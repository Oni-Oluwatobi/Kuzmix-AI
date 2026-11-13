package com.kuzmix.ai.domain.ai

import com.kuzmix.ai.domain.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class EngineTrace(
    val rawQuery: String,
    val normalizedQuery: String,
    val intent: Intent,
    val entity: String?,
    val activeSubject: String?,
    val activeTopic: String?,
    val candidateId: String?,
    val score: Double?,
    val confidence: String?,
    val provider: String = "LOCAL",
    val result: String
)

/** Lightweight ranked hit for search UIs (home search bar). */
data class KnowledgeHit(
    val id: String,
    val name: String,
    val summary: String,
    val score: Double
)

class KnowledgeEngine(
    private val pack: KnowledgePack,
    conceptSource: ConceptSource? = null,
    private val corpus: CorpusSource? = null
) : AIEngine {

    override val engineName: String = "Kuzmix Knowledge Base"
    override val isAvailable: Boolean = true
    override val isLocal: Boolean = true

    private val index by lazy { KnowledgeIndex(pack, conceptSource) }
    private val resolver by lazy { ContextResolver(pack.subjects) }
    private val study by lazy { StudyEngine(pack.subjects) }

    var lastTrace: EngineTrace? = null
        private set

    private val enabledSubjects: List<SubjectRecord>
        get() = pack.subjects.filter { it.enabled }

    override suspend fun generateResponse(
        query: String,
        history: List<ChatMessage>
    ): String = withContext(Dispatchers.Default) {
        val nq = QueryNormalizer.normalize(query)
        val match = IntentClassifier.classify(nq)
        val ctxBefore = resolver.resolve(history)
        val ctxNow = resolver.applyQuery(ctxBefore, query)

        // a brand-new conversation must not inherit a stale tutoring session
        if (history.isEmpty()) session = null

        var intent = match.intent
        lastCandidateId = null
        lastCandidateScore = null
        lastCandidateConfidence = null
        if (intent == Intent.DEFINITION) {
            val entity = match.entity
            val entry = entity?.let { index.resolveEntity(it) }
            intent = if (entry?.kind == KnowledgeIndex.Kind.SUBJECT) {
                Intent.SUBJECT_DEFINITION
            } else {
                Intent.CONCEPT_DEFINITION
            }
        }

        // A live practice/quiz/reading session gets first pick of the reply:
        // short answers, option letters, "next"/"another"... When the session
        // cannot interpret the message it clears itself and we fall through
        // to the normal intent dispatch below.
        val sessionResponse = session?.let { handleSessionReply(it, intent, nq, match) }

        val response: String = sessionResponse ?: when (intent) {
            Intent.CODE_GENERATION, Intent.CODE_EXPLANATION, Intent.DEBUGGING,
            Intent.FEATURE_REQUEST, Intent.PROJECT_GENERATION, Intent.CODE_ASSISTANT ->
                codeResponse(intent)

            Intent.CAPABILITY_QUERY, Intent.SUBJECT_CAPABILITY -> capabilityResponse()

            Intent.STUDY_ROADMAP -> roadmapResponse(nq, ctxNow)

            Intent.STUDY_HELP, Intent.STUDY_START -> studyHelpResponse(match, nq, ctxNow)

            Intent.SUBJECT_DEFINITION, Intent.CONCEPT_DEFINITION ->
                definitionResponse(nq, match, ctxNow, intent)

            Intent.COMPARISON -> comparisonResponse(match)

            Intent.CALCULATION -> calculationResponse(nq, match)

            Intent.SOLVE_EQUATION -> solveEquationResponse(match)

            Intent.BIBLE_QUERY ->
                bibleResponse(nq, match)
                    ?: retrievalResponse(nq, ctxNow, intent, match.entity)
                    ?: legacyLookup(nq)
                    ?: fallbackFor(ctxBefore)

            Intent.EXAMPLE_REQUEST -> exampleResponse(match, nq, ctxNow)

            Intent.PRACTICE_QUESTION -> practiceResponse(match, ctxNow)

            Intent.IDENTITY -> identityResponse()
            Intent.ABOUT_KUZMIX ->
                if (match.entity == "provider") providerResponse() else aboutResponse()
            Intent.DEVELOPER_INFO -> developerResponse()

            Intent.GREETING -> pack.responses["greeting"] ?: defaultGreeting()
            Intent.FAREWELL -> pack.responses["farewell"] ?: "Goodbye! Wishing you a productive learning session ahead."
            Intent.THANKS -> pack.responses["thanks"] ?: "You're welcome! Feel free to ask if you have more questions."
            Intent.HELP -> helpResponse()

            Intent.SMALL_TALK -> {
                val active = ctxNow.activeSubject?.let { id -> enabledSubjects.firstOrNull { it.id == id } }
                if (match.entity == "__continue__" && active != null) {
                    study.continueWithSubject(active)
                } else {
                    smallTalkResponse(ctxNow)
                }
            }

            Intent.GENERAL_KNOWLEDGE, Intent.UNKNOWN ->
                retrievalResponse(nq, ctxNow, intent, match.entity) ?: legacyLookup(nq)
                    ?: fallbackFor(ctxBefore)

            else ->
                retrievalResponse(nq, ctxNow, intent, match.entity) ?: legacyLookup(nq)
                    ?: fallbackFor(ctxBefore)
        }

        lastTrace = EngineTrace(
            rawQuery = query,
            normalizedQuery = nq.normalized,
            intent = intent,
            entity = match.entity ?: match.entity2,
            activeSubject = ctxNow.activeSubject,
            activeTopic = ctxNow.activeTopic,
            candidateId = lastCandidateId,
            score = lastCandidateScore,
            confidence = lastCandidateConfidence,
            result = if (response.startsWith(ContextResolver.FALLBACK_A) ||
                response.startsWith(ContextResolver.FALLBACK_B)
            ) "fallback" else "success"
        )

        response
    }

    private var lastCandidateId: String? = null
    private var lastCandidateScore: Double? = null
    private var lastCandidateConfidence: String? = null

    // ===================================================================
    // Interactive tutoring state — one live session at a time. The NEXT
    // user message is first interpreted as a reply to the session
    // (answer attempt, "next", "another"...); if it cannot be, the session
    // clears itself and the normal intent dispatch takes over.
    // ===================================================================

    private sealed interface Session {
        /** Student is solving a generated problem; their reply is an attempt. */
        data class EquationPractice(
            val problem: MathSolver.PracticeProblem,
            var misses: Int = 0
        ) : Session

        /** MCQ / short-answer quiz built from the real exam papers. */
        data class Quiz(
            val topic: String,
            val questions: List<QuizQuestion>,
            val keysByConcept: Map<String, Map<Int, String>>,
            val referenceText: String,
            var index: Int = 0
        ) : Session

        /** Stepwise reading of one concept's lesson notes. */
        data class LessonReader(
            val conceptName: String,
            val topic: String,
            val chunks: List<CorpusChunk>,
            var offset: Int = 0
        ) : Session
    }

    private data class QuizQuestion(
        val number: Int,
        val stem: String,
        val options: List<String>,
        val conceptId: String
    )

    private var session: Session? = null

    /** Intents whose message can plausibly be an answer to a live session. */
    private val replyIntents = setOf(Intent.UNKNOWN, Intent.GENERAL_KNOWLEDGE, Intent.CALCULATION)

    private companion object {
        /** Verses shown when the user asks for a whole chapter. */
        private const val CHAPTER_PREVIEW = 40
        /** Max chars of a lesson chunk rendered per teaching part. */
        private const val LESSON_CHUNK_CHARS = 950
        /** Max chars of a corpus-backed answer. */
        private const val CORPUS_ANSWER_CHARS = 650
        /** Max chars of lesson material appended under a definition. */
        private const val DEFINITION_LESSON_CHARS = 550
        /** Questions per generated quiz. */
        private const val QUIZ_SIZE = 5
    }

    /** Simple "x = 5" / "5" typed during an equation practice round. */
    private val trivialAnswerForm = Regex("^(?:[a-z]\\s*=\\s*)?[-+]?\\d+(?:\\.\\d+)?$")

    private fun handleSessionReply(
        active: Session,
        intent: Intent,
        nq: NormalizedQuery,
        match: IntentMatch
    ): String? {
        val raw = nq.raw.trim()
        val continueRequest = intent == Intent.SMALL_TALK && match.entity == "__continue__"
        return when (active) {
            is Session.EquationPractice -> when {
                continueRequest -> {
                    // "next"/"another": swap in a fresh problem of the same shape
                    val variable = Regex("([a-z])\\s*=").find(active.problem.answer)
                        ?.groupValues?.get(1) ?: "x"
                    val fresh = MathSolver.generateSimilar(MathSolver.Kind.LINEAR, variable)
                    if (fresh != null) {
                        session = Session.EquationPractice(fresh)
                        equationPrompt(fresh, repeat = false)
                    } else {
                        equationPrompt(active.problem, repeat = true)
                    }
                }
                intent in replyIntents && Regex("-?\\d").containsMatchIn(raw) ->
                    markEquationReply(active, raw)
                intent == Intent.SOLVE_EQUATION && trivialAnswerForm.matches(raw.lowercase()) ->
                    markEquationReply(active, raw)
                else -> {
                    session = null
                    null
                }
            }

            is Session.Quiz -> when {
                continueRequest -> presentQuizQuestion(active)
                intent == Intent.PRACTICE_QUESTION && sameTopic(match.entity, active) ->
                    presentQuizQuestion(active)
                intent in replyIntents -> markQuizReply(active, raw)
                else -> {
                    session = null
                    null
                }
            }

            is Session.LessonReader -> when {
                continueRequest -> nextLessonChunk(active)
                else -> {
                    session = null
                    null
                }
            }
        }
    }

    private fun sameTopic(entity: String?, quiz: Session.Quiz): Boolean {
        if (entity.isNullOrBlank()) return true          // "more questions" -> same quiz
        val a = entity.trim().lowercase()
        val b = quiz.topic.trim().lowercase()
        return a == b || a in b || b in a
    }

    // ------------------------------------------------------ equation solving

    private fun equationPrompt(problem: MathSolver.PracticeProblem, repeat: Boolean): String = buildString {
        if (repeat) append("Here it is again:\n\n")
        append("🎯 Your turn:\n\n")
        append(problem.question)
        append("\n\nType your answer — I'll mark it straight away.")
    }

    private fun markEquationReply(active: Session.EquationPractice, reply: String): String {
        val problem = active.problem
        if (MathSolver.isCorrectAnswer(reply, problem)) {
            session = null
            val variable = Regex("([a-z])\\s*=").find(problem.answer)?.groupValues?.get(1) ?: "x"
            val next = MathSolver.generateSimilar(MathSolver.Kind.LINEAR, variable)
            val praise = "✅ Correct! ${problem.answer} — the method is mastered."
            return if (next != null) {
                session = Session.EquationPractice(next)
                "$praise\n\n${equationPrompt(next, repeat = false)}"
            } else {
                "$praise\n\nWhat next — another equation, practice questions on a topic, or a new question?"
            }
        }
        active.misses++
        val steps = MathSolver.solve(problem.question)?.steps.orEmpty()
        return if (active.misses >= 2) {
            session = null
            buildString {
                append("The answer is ${problem.answer}.\n\nFull walk-through:\n")
                steps.forEachIndexed { i, s -> append("${i + 1}. $s\n") }
                append("\nAsk for a fresh one anytime, or quiz me on any topic.")
            }
        } else {
            buildString {
                append("Not quite — check your signs and try again.\n")
                steps.getOrNull(1)?.let { append("\nHint: $it\n") }
                append("\nType your answer (e.g. \"${problem.answer}\").")
            }
        }
    }

    private suspend fun solveEquationResponse(match: IntentMatch): String {
        val raw = (match.entity ?: "").trim()
        if (raw.isEmpty()) {
            return "Send me the full equation — for example \"2x - 3 = 34\" or \"x^2 - 5x + 6 = 0\" " +
                "— and I'll work it out step by step."
        }
        val factorFirst = MathSolver.isFactorRequest(raw)
        val solution = (if (factorFirst) MathSolver.factorize(raw) else null)
            ?: MathSolver.solve(raw)
            ?: MathSolver.factorize(raw)
        if (solution == null) {
            session = null
            return "I couldn't parse that as an equation. Write it with the variable and an equals sign — " +
                "like \"2x - 3 = 34\", \"3(x - 2) = 4x + 1\" or \"x^2 - 5x + 6 = 0\"."
        }
        lastCandidateId = null
        lastCandidateScore = null
        lastCandidateConfidence = "HIGH"
        val heading = if (solution.kind == MathSolver.Kind.FACTOR) {
            "Factorising step by step:"
        } else {
            "Solving it step by step:"
        }
        val practice = MathSolver.generateSimilar(solution.kind, solution.variable)
        if (practice != null) session = Session.EquationPractice(practice) else session = null
        return buildString {
            append("$heading\n\n")
            solution.steps.forEachIndexed { i, step -> append("${i + 1}. $step\n") }
            append("\n✅ Answer: ${solution.answer}")
            if (practice != null) {
                append("\n\n").append(equationPrompt(practice, repeat = false))
            } else {
                append("\n\nReady for another? Ask a new question, or say \"quiz me on <topic>\".")
            }
        }
    }

    // ---------------------------------------------------------------- bible

    private suspend fun bibleResponse(nq: NormalizedQuery, match: IntentMatch): String? {
        val src = corpus ?: return null
        val ref = match.entity?.let { BibleRefs.findReference(it) }

        if (ref != null && ref.isChapterOnly) {
            val verses = src.bibleRange(ref.book, ref.chapter, 1, 176)
            if (verses.isEmpty()) return bibleMiss(ref)
            lastCandidateId = null
            lastCandidateScore = null
            lastCandidateConfidence = "HIGH"
            val shown = verses.take(CHAPTER_PREVIEW)
            return buildString {
                append("📖 ${ref.display()} (KJV)\n\n")
                shown.forEach { append("Verse ${it.verse}. ${it.text}\n\n") }
                if (verses.size > shown.size) {
                    append("(First ${shown.size} of ${verses.size} verses — ask for more, e.g. " +
                        "\"${ref.book} ${ref.chapter}:${shown.last().verse + 1}\")")
                } else {
                    append("Quote any verse for an explanation (\"what does ${ref.display()}:${shown.last().verse} mean\"), " +
                        "or say \"quiz me on ${ref.book} ${ref.chapter}\".")
                }
            }
        }

        if (ref != null) {
            val from = ref.firstVerse ?: 1
            val to = ref.lastVerse ?: from
            val verses = src.bibleRange(ref.book, ref.chapter, from, to)
            if (verses.isEmpty()) return bibleMiss(ref)
            lastCandidateId = null
            lastCandidateScore = null
            lastCandidateConfidence = "HIGH"
            val rangeLabel = if (to != from) "${ref.chapter}:$from-$to" else "${ref.chapter}:$from"
            return buildString {
                append("📖 ${ref.book} $rangeLabel (KJV)\n\n")
                verses.forEach { append("Verse ${it.verse}. ${it.text}\n\n") }
                append("Want the surrounding context (whole chapter) or an explanation of how it applies?")
            }
        }

        // No reference — topical search ("verses about love", "what the bible says about forgiveness")
        val terms = queryTerms(nq)
        if (terms.isEmpty()) return null
        val hits = src.bibleSearch(terms, 5)
        if (hits.isEmpty()) return null
        lastCandidateId = null
        lastCandidateScore = null
        lastCandidateConfidence = "MEDIUM"
        return buildString {
            append("📖 Bible matches (KJV):\n\n")
            hits.forEach { append("• ${it.ref} — ${it.text}\n\n") }
            append("Ask for any full reference (\"John 3:16\") to see it on its own — " +
                "or say \"teach me <topic>\" and I'll bring in your lesson notes too.")
        }
    }

    private fun bibleMiss(ref: BibleRef): String =
        "I couldn't find ${ref.display()} in the bundled KJV. Check the chapter/verse numbers " +
            "(for example \"John 3:16\" or \"1 Corinthians 13:4-7\"), or search by topic like \"verses about love\"."

    // ------------------------------------------------------------ quiz flow

    private fun presentQuizQuestion(quiz: Session.Quiz): String {
        val i = quiz.index
        if (i >= quiz.questions.size) {
            session = null
            return "🎉 That's all ${quiz.questions.size} questions on \"${quiz.topic}\". " +
                "Say \"more\" for another set, name a different topic, or \"teach me <topic>\" to revise first."
        }
        val q = quiz.questions[i]
        return buildString {
            append("Question ${i + 1} of ${quiz.questions.size}\n\n")
            append(q.stem)
            if (q.options.isNotEmpty()) {
                append("\n")
                q.options.forEachIndexed { oi, opt -> append("\n${'A' + oi}) $opt") }
                append("\n\nType your answer (A, B, C, D) or explain your choice.")
            } else {
                append("\n\nType your answer in your own words — I'll check it against your notes.")
            }
        }
    }

    private fun markQuizReply(quiz: Session.Quiz, reply: String): String {
        val q = quiz.questions.getOrNull(quiz.index) ?: run { session = null; return "" }
        val key = quiz.keysByConcept[q.conceptId]?.get(q.number)
        val sb = StringBuilder()

        when {
            key != null && q.options.isNotEmpty() -> {
                val chosen = detectOption(reply, q.options)
                    ?: return "I couldn't catch your choice — reply with just the letter (A, B, C or D)."
                val chosenText = q.options.getOrNull(chosen[0] - 'A')
                if (chosen == key) {
                    sb.append("✅ Correct!")
                    chosenText?.let { sb.append(" ($key — $it)") }
                } else {
                    sb.append("❌ Not quite — the answer is $key")
                    q.options.getOrNull(key[0] - 'A')?.let { sb.append(" ($it)") }
                    sb.append(".")
                    nudgeFromNotes(quiz.referenceText, reply)?.let { sb.append("\n\n$it") }
                }
            }

            q.options.isNotEmpty() -> {
                // Objective question, but this paper ships no answer key:
                // check the chosen option against the lesson notes instead.
                val chosen = detectOption(reply, q.options)
                sb.append(
                    if (chosen != null) optionNotesVerdict(chosen, q, quiz)
                    else paragraphVerdict(reply, quiz)
                )
            }

            else -> sb.append(paragraphVerdict(reply, quiz))
        }

        quiz.index++
        if (quiz.index < quiz.questions.size) {
            sb.append("\n\n").append(presentQuizQuestion(quiz))
        } else {
            session = null
            sb.append("\n\n🎉 That's all ${quiz.questions.size} questions on \"${quiz.topic}\". " +
                "Say \"more\" for another set, name a different topic, or \"teach me <topic>\" to revise first.")
        }
        return sb.toString()
    }

    /** "b", "B.", "answer is c", "Field", "cultivating land..." -> "B"/"C". */
    private fun detectOption(reply: String, options: List<String>): String? {
        val r = reply.trim()
        if (r.isEmpty()) return null
        Regex("^([a-dA-D])\\s*[.)]?$").find(r)?.let { return it.groupValues[1].uppercase() }
        Regex("\\b(?:answer|option|choice)\\s*(?:is|:|=)?\\s*([a-dA-D])\\b").find(r)?.let {
            return it.groupValues[1].uppercase()
        }
        // the option text itself (longest match wins: "earthworm" beats "earth")
        val lower = r.lowercase()
        var best: Pair<Int, String>? = null
        options.forEachIndexed { i, opt ->
            val o = opt.trim().lowercase()
            if (o.length >= 3 && lower.contains(o)) {
                if (best == null || o.length > best!!.first) best = o.length to ('A' + i).toString()
            }
        }
        best?.let { return it.second }
        // short reply with a clear standalone letter (uppercase B/C/D, or b/c/d)
        if (r.length <= 60) {
            Regex("\\b([B-D])\\b").find(r)?.let { return it.groupValues[1] }
            Regex("\\b([b-d])\\b").find(r)?.let { return it.groupValues[1].uppercase() }
            // a lone/short "a" is only a real pick, never the article
            if (r.length <= 14 && Regex("\\b[aA]\\b").containsMatchIn(r)) return "A"
        }
        return null
    }

    /** No key: weigh the chosen option against the lesson notes. */
    private fun optionNotesVerdict(chosen: String, q: QuizQuestion, quiz: Session.Quiz): String {
        val chosenText = q.options.getOrNull(chosen[0] - 'A') ?: chosen
        val points = keyPoints(quiz.referenceText)
        if (points.isEmpty()) return "Noted — you chose $chosen. Let's keep going!"
        val lower = chosenText.lowercase()
        val covered = points.filter { lower.contains(it) || it.contains(lower.take(6)) }
        return if (covered.isNotEmpty()) {
            "✅ $chosen fits your notes — it lines up with ${covered.take(3).joinToString(", ")}. " +
                "(This paper has no bundled answer key, so I'm checking against your lesson notes.)"
        } else {
            "📘 Noted — $chosen. Your notes stress: ${points.take(4).joinToString(", ")}." +
                evidenceSnippet(quiz.referenceText, points.firstOrNull())?.let { "\n$it" }.orEmpty() +
                "\n(This paper has no bundled answer key, so I'm checking against your lesson notes.)"
        }
    }

    /** No options (theory): compare the student's own words with the notes. */
    private fun paragraphVerdict(reply: String, quiz: Session.Quiz): String {
        val points = keyPoints(quiz.referenceText)
        if (points.isEmpty()) return "Thanks — noted. Let's keep going!"
        val replyLower = reply.lowercase()
        val covered = points.filter { p -> replyLower.contains(p) || replyLower.contains(p.take(6)) }
        val missing = points.filterNot { it in covered }
        return when {
            covered.size >= maxOf(2, (points.size * 0.6).toInt()) ->
                "✅ Good answer — it covers the key points: ${covered.take(4).joinToString(", ")}."
            covered.isNotEmpty() ->
                "📘 Close! You covered ${covered.take(3).joinToString(", ")}. " +
                    "The notes also stress: ${missing.take(3).joinToString(", ")}." +
                    (evidenceSnippet(quiz.referenceText, missing.firstOrNull())?.let { "\n$it" } ?: "")
            else ->
                "📘 Here's how your notes put it:\n\n" +
                    truncateAtSentence(quiz.referenceText, 320) +
                    "\n\nKey words to include: ${points.take(4).joinToString(", ")}."
        }
    }

    private fun nudgeFromNotes(reference: String, reply: String): String? {
        val missing = keyPoints(reference).filterNot { reply.lowercase().contains(it) }
        if (missing.isEmpty()) return null
        return "Remember the key ideas: ${missing.take(3).joinToString(", ")}."
    }

    /** Most distinctive recurring words in a passage — the "key points". */
    private fun keyPoints(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val words = text.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 5 && it !in QueryNormalizer.stopwords }
        if (words.isEmpty()) return emptyList()
        return words.groupingBy { it }.eachCount().entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenByDescending { it.key.length })
            .map { it.key }
            .distinct()
            .take(6)
    }

    private fun evidenceSnippet(text: String, word: String?): String? {
        if (word.isNullOrBlank() || text.isBlank()) return null
        val sentence = text.split(Regex("(?<=[.!?])\\s+"))
            .firstOrNull { it.lowercase().contains(word) } ?: return null
        val cut = truncateAtSentence(sentence, 240)
        return if (cut.length < sentence.length) "…$cut" else "“$cut”"
    }

    // ---------------------------------------------------------- exam parsing

    private fun stripAnswerKeySections(text: String): String {
        var out = text
        val marker = Regex("(?i)\\[?\\s*ANSWER KEY")
        var guard = 0
        while (guard++ < 8) {
            val m = marker.find(out) ?: break
            val stop = Regex("\\bQ\\d+[.)]").find(out, m.range.last + 1)?.range?.first ?: out.length
            if (stop <= m.range.first) break
            out = out.substring(0, m.range.first) + out.substring(stop)
        }
        return out.replace(Regex("\\bQ\\d+\\s*\\[$"), "")
    }

    private fun parseAnswerKeys(text: String): Map<Int, String> {
        val marker = Regex("(?i)ANSWER KEY").find(text) ?: return emptyMap()
        val from = marker.range.last + 1
        val stop = Regex("\\bQ\\d+[.)]").find(text, from)?.range?.first ?: text.length
        if (stop <= from) return emptyMap()
        val region = text.substring(from, minOf(stop, from + 4000))
        val out = HashMap<Int, String>()
        for (m in Regex("(\\d{1,3})\\s*[-.:]\\s*([A-Da-d])(?![a-z])").findAll(region)) {
            val n = m.groupValues[1].toIntOrNull() ?: continue
            out.putIfAbsent(n, m.groupValues[2].uppercase())
        }
        return out
    }

    private fun parseQuestions(text: String): List<QuizQuestion> {
        val headers = Regex("\\bQ(\\d+)[.)]\\s*").findAll(text).toList()
        if (headers.isEmpty()) return emptyList()
        val out = ArrayList<QuizQuestion>(headers.size)
        for (i in headers.indices) {
            val number = headers[i].groupValues[1].toIntOrNull() ?: continue
            val start = headers[i].range.last + 1
            val end = headers.getOrNull(i + 1)?.range?.first ?: text.length
            val body = text.substring(start, end).trim()
            if (body.isBlank() || body.length > 700) continue
            val (stem, options) = splitOptions(body)
            if (stem.isBlank() || stem.length > 420) continue
            out.add(QuizQuestion(number, stem, options, conceptId = ""))
        }
        return out
    }

    private fun splitOptions(body: String): Pair<String, List<String>> {
        val markers = Regex("(?:^|\\s)([A-D])[.)]\\s*").findAll(body).toList()
        if (markers.size < 2) return body to emptyList()
        val stem = body.substring(0, markers.first().range.first).trim()
        if (stem.isBlank()) return body to emptyList()
        val options = ArrayList<String>(markers.size)
        for (i in markers.indices) {
            val contentStart = markers[i].range.last + 1
            val contentEnd = markers.getOrNull(i + 1)?.range?.first ?: body.length
            options.add(
                body.substring(contentStart, contentEnd).trim()
                    .trimEnd(',', ';', '.', ' ').trim()
            )
        }
        val cleaned = options.filter { it.isNotBlank() }
        return if (cleaned.size < 2) stem to emptyList() else stem to cleaned
    }

    // -------------------------------------------------- lesson notes teaching

    private suspend fun startLesson(terms: List<String>, topic: String, intro: String): String? {
        val src = corpus ?: return null
        if (terms.isEmpty()) return null
        val hit = src.search(terms, 5, "lesson_notes").firstOrNull() ?: return null
        val chunks = src.forConcept(hit.conceptId, 6, "lesson_notes").ifEmpty { listOf(hit) }
        val reader = Session.LessonReader(hit.conceptName, topic, chunks, 0)
        session = reader
        lastCandidateId = hit.conceptId
        lastCandidateScore = 1.0
        lastCandidateConfidence = "HIGH"
        return "$intro\n\n" + renderLessonPart(reader)
    }

    private fun renderLessonPart(reader: Session.LessonReader): String {
        val chunk = reader.chunks[reader.offset]
        return buildString {
            append("📘 ${reader.topic} — part ${reader.offset + 1} of ${reader.chunks.size}\n\n")
            append("${chunk.sectionTitle}:\n\n")
            append(truncateAtSentence(chunk.text, LESSON_CHUNK_CHARS))
            if (reader.offset < reader.chunks.size - 1) {
                append("\n\nSay \"next\" to continue, \"quiz me on ${reader.topic}\" for practice questions, " +
                    "or ask me anything about it.")
            } else {
                append("\n\nThat's the end of the ${reader.topic} notes. " +
                    "Say \"quiz me on ${reader.topic}\" to test yourself, or ask me anything.")
            }
        }
    }

    private fun nextLessonChunk(reader: Session.LessonReader): String {
        if (reader.offset < reader.chunks.size - 1) {
            reader.offset++
            return renderLessonPart(reader)
        }
        session = null
        return "That's the end of the ${reader.topic} notes. " +
            "Say \"quiz me on ${reader.topic}\" to test yourself, or ask me anything."
    }

    // ------------------------------------------------------------ corpus core

    /** Best lesson/exam passage for the query — the "answer from the pdf folder" path. */
    private suspend fun corpusAnswer(nq: NormalizedQuery): String? {
        val src = corpus ?: return null
        val terms = queryTerms(nq)
        if (terms.isEmpty()) return null
        val hit = src.search(terms, 5, "lesson_notes").firstOrNull()
            ?: src.search(terms, 5, "exam_questions").firstOrNull()
            ?: src.search(terms, 5, null).firstOrNull()
            ?: return null
        lastCandidateId = hit.conceptId
        lastCandidateScore = null
        lastCandidateConfidence = "MEDIUM"
        val chunks = src.forConcept(hit.conceptId, 6, hit.chunkType).ifEmpty { listOf(hit) }
        session = Session.LessonReader(hit.conceptName, hit.sectionTitle, chunks, 0)
        return buildString {
            append("Here's what my study pack holds on \"${hit.sectionTitle}\":\n\n")
            append(truncateAtSentence(hit.text, CORPUS_ANSWER_CHARS))
            append("\n\nSay \"next\" to keep reading, \"quiz me on this\" for practice, or ask a follow-up.")
        }
    }

    private suspend fun lessonExcerpt(nq: NormalizedQuery): CorpusChunk? {
        val src = corpus ?: return null
        val terms = queryTerms(nq)
        if (terms.isEmpty()) return null
        return src.search(terms, 3, "lesson_notes").firstOrNull()
    }

    private fun queryTerms(nq: NormalizedQuery): List<String> {
        val content = QueryNormalizer.contentTokens(nq.tokens)
        if (content.isNotEmpty()) return content
        return nq.raw.split(Regex("\\s+"))
            .map { it.lowercase() }
            .filter { it.length > 2 && it !in QueryNormalizer.stopwords }
    }

    private fun truncateAtSentence(text: String, max: Int): String {
        val clean = text.replace(Regex("\\s+"), " ").trim()
        if (clean.length <= max) return clean
        val cut = clean.lastIndexOf('.', max)
        return if (cut > max / 2) {
            clean.substring(0, cut + 1)
        } else {
            clean.substring(0, max).trimEnd() + "…"
        }
    }


    private fun codeResponse(intent: Intent): String = when (intent) {
        Intent.DEBUGGING ->
            "I can help debug that. Paste the error message and the code around it, and we'll work through it step by step: " +
                "first the exact error line, then imports and dependencies, then logic and state. What is the error saying?"
        Intent.CODE_EXPLANATION ->
            "I can walk you through this code. Paste it (or the key part) and I'll explain it section by section: " +
                "what each block does, how the data flows, and any pitfalls to watch for."
        Intent.FEATURE_REQUEST ->
            "I can help add that feature. Share the screen or file it should live in plus your current code, " +
                "and I'll outline the changes: data layer, logic, then UI, with the snippet you need."
        Intent.PROJECT_GENERATION ->
            "I can scaffold that project for you. Tell me the platform (for example Kotlin + Jetpack Compose for Android) " +
                "and the core screens or features, and I'll lay out the structure and starter code step by step."
        else ->
            "I can write code step by step. Tell me the language and what the code should do " +
                "(for example \"a function that sorts a list\" or \"a simple Android app screen\"), " +
                "and I'll break it into snippets you can paste into your project."
    }

    private fun capabilityResponse(): String {
        val joined = joinNames(enabledSubjects.map { it.name })
        return "I can help with $joined, and with supported programming and software topics. " +
            "Ask me for a definition (like \"what is biology\"), say \"I need help on chemistry\" to start studying, " +
            "or \"where should we begin?\" for a full roadmap."
    }

    private fun roadmapResponse(nq: NormalizedQuery, ctx: ConversationContext): String {
        val fromQuery = resolver.detectSubject(nq.normalized)
        val subjectId = fromQuery?.id ?: ctx.activeSubject
        val subject = subjectId?.let { id -> enabledSubjects.firstOrNull { it.id == id } }
        return if (subject != null) study.roadmapResponse(subject) else study.genericStartResponse()
    }

    private suspend fun studyHelpResponse(match: IntentMatch, nq: NormalizedQuery, ctx: ConversationContext): String {
        val fromQuery = resolver.detectSubject(nq.normalized)
        val entitySubject = match.entity?.let { index.resolveEntity(it) }?.takeIf { it.kind == KnowledgeIndex.Kind.SUBJECT }
        // an explicit entity ("teach me photosynthesis") wins over the
        // previously active subject — the student asked about THIS topic
        val subject = fromQuery
            ?: entitySubject?.subject
            ?: if (match.entity == null) {
                ctx.activeSubject?.let { id -> enabledSubjects.firstOrNull { it.id == id } }
            } else null

        if (subject != null) return study.startResponse(subject)
        if (match.entity == null && ctx.activeSubject != null) {
            val active = enabledSubjects.firstOrNull { it.id == ctx.activeSubject }
            if (active != null) return study.continueWithSubject(active)
        }
        // topic-level request: teach straight from the lesson notes
        val topic = match.entity
        if (!topic.isNullOrBlank()) {
            startLesson(entityTerms(topic), topic, "Let's study $topic — I've loaded your lesson notes.")
                ?.let { return it }
        }
        return "I'd love to help. Which subject? I cover ${joinNames(enabledSubjects.map { it.name })}."
    }

    /** Simple whitespace term list for corpus queries. */
    private fun entityTerms(text: String?): List<String> =
        text?.split(Regex("\\s+"))?.map { it.lowercase() }?.filter { it.isNotBlank() } ?: emptyList()

    private suspend fun definitionResponse(
        nq: NormalizedQuery,
        match: IntentMatch,
        ctx: ConversationContext,
        intent: Intent
    ): String {
        val entity = match.entity ?: nq.normalized
        val exact = index.resolveEntity(entity)
        if (exact != null) {
            lastCandidateId = exact.id
            lastCandidateScore = 1.0
            lastCandidateConfidence = "HIGH"
            val base = renderEntry(exact, intent, exact = true)
            // deepen concept answers with the actual lesson-note passage
            val lesson = if (intent == Intent.CONCEPT_DEFINITION) lessonExcerpt(nq) else null
            return if (lesson != null) {
                "$base\n\n📚 From your lesson notes — ${lesson.sectionTitle}:\n" +
                    truncateAtSentence(lesson.text, DEFINITION_LESSON_CHARS)
            } else {
                base
            }
        }

        val results = index.search(nq, ctx, intent, minScore = 0.20)
        val best = results.firstOrNull()
        if (best != null && best.score >= 0.28) {
            lastCandidateId = best.entry.id
            lastCandidateScore = best.score
            lastCandidateConfidence = index.confidenceFor(best.score, exact = false)
            return renderEntry(best.entry, intent, exact = false)
        }

        val broad = index.search(nq, ctx, Intent.GENERAL_KNOWLEDGE, minScore = 0.15)
        val broadBest = broad.firstOrNull()
        if (broadBest != null && broadBest.score >= 0.30) {
            lastCandidateId = broadBest.entry.id
            lastCandidateScore = broadBest.score
            lastCandidateConfidence = index.confidenceFor(broadBest.score, exact = false)
            return "Closest match in my knowledge:\n\n" + renderEntry(broadBest.entry, intent, exact = false)
        }

        lastCandidateId = null
        lastCandidateScore = null
        lastCandidateConfidence = null
        return corpusAnswer(nq) ?: legacyLookup(nq) ?: fallbackFor(ctx)
    }

    private suspend fun renderEntry(entry: KnowledgeIndex.Entry, intent: Intent, exact: Boolean): String {
        return when (entry.kind) {
            KnowledgeIndex.Kind.SUBJECT -> renderSubject(entry.subject!!)
            KnowledgeIndex.Kind.CONCEPT -> renderConcept(entry.concept!!)
            KnowledgeIndex.Kind.FORMULA -> renderFormula(entry.formula!!)
        }
    }

    private fun renderSubject(s: SubjectRecord): String {
        val builder = StringBuilder()
        builder.append(s.definition.trim())
        if (s.subtopics.isNotEmpty()) {
            builder.append("\n\nKey areas in ${s.name}:")
            s.subtopics.take(6).forEach { builder.append("\n• $it") }
        }
        if (s.roadmap.isNotEmpty()) {
            builder.append("\n\nSay \"where should we begin?\" and I'll give you the full ${s.name} roadmap.")
        }
        return builder.toString()
    }

    private suspend fun renderConcept(c: ConceptRecord): String {
        val builder = StringBuilder()
        builder.append(c.definition.trim())
        if (c.formula.isNotBlank()) {
            builder.append("\n\nFormula: ${c.formula}")
        }
        c.examples.firstOrNull()?.let { builder.append("\n\nExample: $it") }
        val relatedNames = c.related.mapNotNull { index.nameOf(it) }.filter { !it.equals(c.name, true) }.take(3)
        if (relatedNames.isNotEmpty()) {
            builder.append("\n\nRelated: ${relatedNames.joinToString(", ")}")
        }
        return builder.toString()
    }

    private fun renderFormula(f: FormulaRecord): String {
        val builder = StringBuilder()
        builder.append("${f.name}: ${f.formula}")
        if (f.symbols.isNotEmpty()) {
            val where = f.symbols.entries.joinToString(", ") { (k, v) -> "$k = $v" }
            builder.append("\nWhere $where")
        }
        if (f.explanation.isNotBlank()) {
            builder.append("\n${f.explanation}")
        }
        return builder.toString()
    }

    private suspend fun comparisonResponse(match: IntentMatch): String {
        val a = match.entity?.let { index.resolveEntity(it) }
        val b = match.entity2?.let { index.resolveEntity(it) }
        return when {
            a != null && b != null -> {
                lastCandidateId = a.id
                lastCandidateScore = 1.0
                lastCandidateConfidence = "HIGH"
                buildString {
                    append("${a.name}: ${a.definition.trim()}")
                    append("\n\n${b.name}: ${b.definition.trim()}")
                    append("\n\nTell me what aspect you're comparing (structure, uses, exam focus) and I'll go deeper on each.")
                }
            }
            a != null -> {
                lastCandidateId = a.id
                lastCandidateScore = 1.0
                lastCandidateConfidence = "HIGH"
                "${a.name}: ${a.definition.trim()}\n\nI don't have a matching entry for \"${match.entity2}\" yet."
            }
            b != null -> {
                lastCandidateId = b.id
                lastCandidateScore = 1.0
                lastCandidateConfidence = "HIGH"
                "${b.name}: ${b.definition.trim()}\n\nI don't have a matching entry for \"${match.entity}\" yet."
            }
            else -> {
                lastCandidateId = null
                lastCandidateScore = null
                lastCandidateConfidence = null
                "I need a bit more detail — tell me two topics (for example, \"difference between mitosis and meiosis\") and I'll break them down side by side."
            }
        }
    }

    private suspend fun calculationResponse(nq: NormalizedQuery, match: IntentMatch): String {
        val percent = Regex("(\\d+(?:\\.\\d+)?)\\s*%\\s*of\\s*(\\d+(?:\\.\\d+)?)").find(nq.normalized)
        if (percent != null) {
            val value = percent.groupValues[1].toDouble()
            val total = percent.groupValues[2].toDouble()
            val result = formatNumber(value / 100.0 * total)
            lastCandidateId = null
            lastCandidateScore = null
            lastCandidateConfidence = "HIGH"
            return "${formatNumber(value)}% of ${formatNumber(total)} = $result"
        }

        val entity = match.entity ?: nq.normalized
        if (entity.any { it.isDigit() }) {
            val cleaned = entity.replace("×", "*").replace("÷", "/")
            val result = ExprEval(cleaned).eval()
            if (result != null) {
                lastCandidateId = null
                lastCandidateScore = null
                lastCandidateConfidence = "HIGH"
                return "$entity = ${formatNumber(result)}"
            }
        }

        val results = index.search(nq, ConversationContext(), Intent.CALCULATION, minScore = 0.20)
        val formula = results.firstOrNull { it.entry.kind == KnowledgeIndex.Kind.FORMULA }
        if (formula != null) {
            lastCandidateId = formula.entry.id
            lastCandidateScore = formula.score
            lastCandidateConfidence = index.confidenceFor(formula.score, false)
            return renderFormula(formula.entry.formula!!) +
                "\n\nSend me the values and I'll work through it step by step."
        }

        val concept = results.firstOrNull()
        if (concept != null) {
            lastCandidateId = concept.entry.id
            lastCandidateScore = concept.score
            lastCandidateConfidence = index.confidenceFor(concept.score, false)
            return renderEntry(concept.entry, Intent.CALCULATION, exact = false) +
                "\n\nSend me the numbers (or the full expression) and I'll compute it."
        }

        lastCandidateId = null
        lastCandidateScore = null
        lastCandidateConfidence = null
        return "I can compute expressions like \"12 * 8\" or \"7 × 8\", or walk you through a formula " +
            "if you give me the values. What are the numbers?"
    }

    private suspend fun exampleResponse(match: IntentMatch, nq: NormalizedQuery, ctx: ConversationContext): String {
        val entity = match.entity
        val entry = entity?.let { index.resolveEntity(it) }
            ?: index.search(nq, ctx, Intent.EXAMPLE_REQUEST, 0.20).firstOrNull()?.entry
        if (entry == null) {
            lastCandidateId = null
            lastCandidateScore = null
            lastCandidateConfidence = null
            return "Tell me the concept you'd like an example of — for instance, \"example of a polygon\" or \"example of osmosis\"."
        }
        lastCandidateId = entry.id
        lastCandidateScore = 1.0
        lastCandidateConfidence = "HIGH"
        val concept = entry.concept
        return when {
            concept != null && concept.examples.isNotEmpty() ->
                "${concept.definition.trim()}\n\n" +
                    concept.examples.mapIndexed { i, ex -> "Example ${i + 1}: $ex" }.joinToString("\n")
            concept != null -> renderConcept(concept)
            entry.subject != null -> renderSubject(entry.subject)
            else -> entry.definition
        }
    }

    private suspend fun practiceResponse(match: IntentMatch, ctx: ConversationContext): String {
        val subject = match.entity?.let { index.resolveEntity(it) }?.subject
            ?: ctx.activeSubject?.let { id -> enabledSubjects.firstOrNull { it.id == id } }

        // 1) real exam questions from the shipped corpus (the pdf folder)
        val label = match.entity ?: subject?.name
        val terms = entityTerms(label).filter { it !in QueryNormalizer.stopwords }
        if (!label.isNullOrBlank() && terms.isNotEmpty()) {
            buildQuizFromCorpus(terms, label)?.let { quiz ->
                session = quiz
                lastCandidateId = quiz.questions.firstOrNull()?.conceptId
                lastCandidateScore = 1.0
                lastCandidateConfidence = "HIGH"
                return "I pulled real questions on \"$label\" from your exam papers:\n\n" +
                    presentQuizQuestion(quiz)
            }
        }

        // 2) fallback: subject subtopics (legacy behaviour)
        if (subject == null) {
            lastCandidateId = null
            lastCandidateScore = null
            lastCandidateConfidence = null
            return "Which subject should I quiz you on? I cover ${joinNames(enabledSubjects.map { it.name })}."
        }
        lastCandidateId = subject.id
        lastCandidateScore = 1.0
        lastCandidateConfidence = "HIGH"
        val topics = subject.subtopics.take(5)
        val builder = StringBuilder("Practice questions for ${subject.name}:\n")
        topics.forEachIndexed { i, topic ->
            builder.append("\n${i + 1}. Define ${topic.lowercase().trimEnd('?')}.")
        }
        if (topics.isNotEmpty()) {
            builder.append("\n\nAnswer one at a time and I'll check your response.")
        }
        return builder.toString()
    }

    /**
     * Builds a quiz from the ingested exam papers: BM25 search for the
     * topic, question/Q-number parsing, answer keys where the paper ships
     * them, and a reference passage for key-point marking where it does not.
     */
    private suspend fun buildQuizFromCorpus(terms: List<String>, label: String): Session.Quiz? {
        val src = corpus ?: return null
        val examHits = src.search(terms, 6, "exam_questions")
        if (examHits.isEmpty()) return null

        val keys = HashMap<String, Map<Int, String>>()
        val questions = ArrayList<QuizQuestion>(QUIZ_SIZE)
        for (hit in examHits) {
            parseAnswerKeys(hit.text).let { parsed -> if (parsed.isNotEmpty()) keys[hit.conceptId] = parsed }
            for (q in parseQuestions(stripAnswerKeySections(hit.text))) {
                if (questions.none { it.stem.equals(q.stem, true) }) {
                    questions.add(q.copy(conceptId = hit.conceptId))
                }
                if (questions.size >= QUIZ_SIZE) break
            }
            if (questions.size >= QUIZ_SIZE) break
        }
        if (questions.isEmpty()) return null

        // reference passage: concise concept definition first, lesson notes second
        val reference = src.search(terms, 1, "definition").firstOrNull()?.text
            ?: src.search(terms, 1, "lesson_notes").firstOrNull()?.text
            ?: ""
        return Session.Quiz(label, questions, keys, reference, 0)
    }

    private fun identityResponse(): String {
        val subjects = joinNames(enabledSubjects.take(8).map { it.name })
        return "I'm Kuzmix AI, an offline educational assistant built by Oni Oluwatobi under The Kreadive Galaxy. " +
            "I run entirely offline on your device — no internet required. " +
            "I can help with $subjects, study roadmaps, and code guidance. What would you like to start with?"
    }

    private fun aboutResponse(): String {
        return "Kuzmix AI is a local-first assistant: an offline knowledge base with definitions and study roadmaps, " +
            "a chat, a code assistant, an image generator, and study tools — all in one app, all private on your device. " +
            "Try \"what is biology\", \"I need help on chemistry\", or \"where should we begin?\"."
    }

    private fun providerResponse(): String {
        return "I work fully offline — no internet, cloud, or external AI service is used for chat. " +
            "Your knowledge, chats, and settings stay on this device."
    }

    private fun developerResponse(): String {
        val creator = pack.responses["creator"] ?: "Kuzmix AI was built by Oni Oluwatobi under The Kreadive Galaxy."
        val galaxy = pack.responses["kreadive_galaxy"]
        return if (galaxy.isNullOrBlank()) creator else "$creator\n\n$galaxy"
    }

    private fun helpResponse(): String {
        return "Here's what I can do:\n" +
            "• Definitions — \"what is photosynthesis\"\n" +
            "• Step-by-step equations — \"2x - 3 = 34\", \"x^2 - 5x + 6 = 0\", \"factorise x^2 + 5x + 6\"\n" +
            "• Bible (full KJV) — \"John 3:16\", \"Psalm 23\", \"verses about love\"\n" +
            "• Teaching from your notes — \"teach me photosynthesis\", then \"next\"\n" +
            "• Exam practice — \"give me practice questions on chemistry\" (real past questions)\n" +
            "• Study plans — \"I need help on biology\", then \"where should we begin?\"\n" +
            "• Code guidance — \"create a simple Android app\"\n" +
            "• Calculations — \"12 * 8\" or a formula with your values\n" +
            "I cover ${joinNames(enabledSubjects.map { it.name })}. What would you like?"
    }

    private fun smallTalkResponse(ctx: ConversationContext): String {
        val subject = ctx.activeSubject?.let { id -> enabledSubjects.firstOrNull { it.id == id } }
        if (subject != null) {
            return "Alright — we're still on ${subject.name}. " +
                subject.subtopics.take(3).joinToString(", ") { it.lowercase().trimEnd('?') } +
                " are good next steps, or say \"where should we begin?\" for the roadmap."
        }
        return pack.responses["status"] ?: "I'm ready when you are — ask me anything, or say \"where should we begin?\"."
    }

    private fun defaultGreeting(): String = "Hello! How can I assist you today?"

    /**
     * Ranked preview of what the knowledge base holds for [query] — used
     * by the home search bar so users see real concepts before starting a
     * chat. Lower minScore than answer retrieval: previews only inform the
     * UI, the engine still gates actual answers at 0.20+.
     */
    suspend fun searchPreview(query: String, limit: Int = 5): List<KnowledgeHit> =
        withContext(Dispatchers.Default) {
            val nq = QueryNormalizer.normalize(query)
            if (QueryNormalizer.contentTokens(nq.tokens).isEmpty()) {
                return@withContext emptyList()
            }
            index.search(nq, ConversationContext(), Intent.GENERAL_KNOWLEDGE, minScore = 0.15)
                .take(limit)
                .map {
                    KnowledgeHit(
                        id = it.entry.id,
                        name = it.entry.name,
                        summary = it.entry.definition.take(160),
                        score = it.score
                    )
                }
        }

    private suspend fun retrievalResponse(
        nq: NormalizedQuery,
        ctx: ConversationContext,
        intent: Intent,
        entity: String?
    ): String? {
        val entityEntry = entity?.let { index.resolveEntity(it) }
        if (entityEntry != null) {
            lastCandidateId = entityEntry.id
            lastCandidateScore = 1.0
            lastCandidateConfidence = "HIGH"
            return renderEntry(entityEntry, intent, exact = true)
        }

        val results = index.search(nq, ctx, intent, minScore = 0.20)
        val best = results.firstOrNull()
        if (best != null && best.score >= 0.30) {
            lastCandidateId = best.entry.id
            lastCandidateScore = best.score
            lastCandidateConfidence = index.confidenceFor(best.score, false)
            return renderEntry(best.entry, intent, exact = false)
        }
        // nothing in the index: answer from the lesson notes / exam corpus
        return corpusAnswer(nq)
    }

    private fun legacyLookup(nq: NormalizedQuery): String? {
        val cleaned = nq.normalized
        if (cleaned.isBlank()) return null
        pack.variations[cleaned]?.let { key -> pack.responses[key]?.let { return it } }
        for ((trigger, key) in pack.variations) {
            if (trigger.isBlank()) continue
            val triggerHit = trigger == cleaned ||
                (trigger.length >= 4 && cleaned.contains(trigger)) ||
                (cleaned.length >= 4 && trigger.contains(cleaned))
            if (triggerHit) {
                pack.responses[key]?.let { return it }
            }
        }
        pack.responses[cleaned]?.let { return it }
        return null
    }

    private fun fallbackFor(ctx: ConversationContext): String {
        lastCandidateId = null
        lastCandidateScore = null
        lastCandidateConfidence = null
        return if (ctx.lastAssistantWasFallback) ContextResolver.FALLBACK_B +
            " — for example, mention the subject (Biology, Physics, Government...) or rephrase your question."
        else ContextResolver.FALLBACK_A +
            " Try rephrasing it or give me the specific topic you're working on."
    }

    private fun joinNames(names: List<String>): String = when (names.size) {
        0 -> ""
        1 -> names[0]
        2 -> "${names[0]} and ${names[1]}"
        else -> names.dropLast(1).joinToString(", ") + ", and ${names.last()}"
    }

    private fun formatNumber(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return value.toString()
        val rounded = Math.round(value * 1_000_000.0) / 1_000_000.0
        return if (rounded == Math.floor(rounded) && Math.abs(rounded) < 1e15) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }

    private class ExprEval(private val input: String) {
        private var pos = 0

        fun eval(): Double? {
            return try {
                pos = 0
                val value = parseExpression()
                skipSpaces()
                if (pos >= input.length && !value.isNaN()) value else null
            } catch (e: Exception) {
                null
            }
        }

        private fun parseExpression(): Double {
            var value = parseTerm()
            skipSpaces()
            while (pos < input.length && (input[pos] == '+' || input[pos] == '-')) {
                val op = input[pos]
                pos++
                val right = parseTerm()
                value = if (op == '+') value + right else value - right
                skipSpaces()
            }
            return value
        }

        private fun parseTerm(): Double {
            var value = parseFactor()
            skipSpaces()
            while (pos < input.length && (input[pos] == '*' || input[pos] == '/')) {
                val op = input[pos]
                pos++
                val right = parseFactor()
                value = if (op == '*') value * right else value / right
                skipSpaces()
            }
            return value
        }

        private fun parseFactor(): Double {
            skipSpaces()
            if (pos < input.length && input[pos] == '+') {
                pos++
                return parseFactor()
            }
            if (pos < input.length && input[pos] == '-') {
                pos++
                return -parseFactor()
            }
            if (pos < input.length && input[pos] == '(') {
                pos++
                val value = parseExpression()
                skipSpaces()
                if (pos < input.length && input[pos] == ')') pos++
                skipSpaces()
                if (pos < input.length && input[pos] == '^') {
                    pos++
                    return Math.pow(value, parseFactor())
                }
                return value
            }
            val start = pos
            while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) pos++
            if (start == pos) throw IllegalArgumentException("no number")
            val number = input.substring(start, pos).toDouble()
            skipSpaces()
            if (pos < input.length && input[pos] == '^') {
                pos++
                return Math.pow(number, parseFactor())
            }
            return number
        }

        private fun skipSpaces() {
            while (pos < input.length && input[pos] == ' ') pos++
        }
    }
}
