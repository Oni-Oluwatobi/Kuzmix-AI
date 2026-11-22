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
    private val corpus: CorpusSource? = null,
    private val quizHistory: QuizHistory = InMemoryQuizHistory(),
    /**
     * Fired when a quiz pins its final scope (class + term). The host uses
     * it to cache the student's class locally and mirror it to Firestore
     * (`users/{uid}`) — the engine itself never touches storage.
     */
    private val onScopePinned: ((cls: String, className: String, term: Int?) -> Unit)? = null
) : AIEngine {

    override val engineName: String = "Kuzmix Knowledge Base"
    override val isAvailable: Boolean = true
    override val isLocal: Boolean = true

    private val index by lazy { KnowledgeIndex(pack, conceptSource) }
    private val resolver by lazy { ContextResolver(pack.subjects) }
    private val study by lazy { StudyEngine(pack.subjects) }

    var lastTrace: EngineTrace? = null
        private set

    /**
     * Tappable option chips for the chat UI: wizard steps while the quiz
     * setup runs (class/term/subject/topic), answer letters (A/B/C/D)
     * during an MCQ, empty otherwise. Re-synced after every reply so a
     * stale tap can never be shown.
     */
    var pendingChoices: List<String> = emptyList()
        private set

    /** True while an interactive session (quiz setup / quiz / reader) holds. */
    val sessionActive: Boolean
        get() = session != null

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

        // chips always mirror the state AFTER this reply (wizard step,
        // MCQ options, or none) — never a stale tap target
        pendingChoices = currentChoices()

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
            var index: Int = 0,
            /** Pinned scope — "more" reshuffles THIS class, never another. */
            val cls: String? = null,
            val term: Int? = null,
            val subject: String? = null
        ) : Session

        /** Stepwise reading of one concept's lesson notes. */
        data class LessonReader(
            val conceptName: String,
            val topic: String,
            val chunks: List<CorpusChunk>,
            var offset: Int = 0
        ) : Session

        /**
         * CLASS -> TERM -> SUBJECT -> TOPIC setup wizard. Every quiz starts
         * here so questions can never drift outside the chosen class: the
         * class is a hard filter on every rung of the widening ladder in
         * [buildScopedQuiz]. Lists are snapshotted per stage (so a reply
         * can never be matched against another stage's options) and [page]
         * drives "More…" paging for long subject/topic lists.
         */
        data class QuizSetup(
            var stage: Stage = Stage.CLASS,
            var cls: String? = null,
            var term: Int? = null,
            var subject: String? = null,
            var subjectConceptId: String? = null,
            var topic: String? = null,
            var subjects: List<SchemeSubject> = emptyList(),
            var topics: List<String> = emptyList(),
            var page: Int = 0,
            /** Topic/subject named up front ("questions on chemistry"). */
            var prefill: String? = null
        ) : Session {
            enum class Stage { CLASS, TERM, SUBJECT, TOPIC }
        }
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
        /** Objective (tap-to-answer) questions per random quiz. */
        private const val OBJECTIVE_SIZE = 20
        /** Theory (type-your-answer) questions per random quiz. */
        private const val THEORY_SIZE = 5
        /**
         * Exam chunks fetched per widening rung. Measured on the shipped
         * corpus: the class needing the most chunks to reach 20 objective
         * + 5 theory in paper order is JSS3 at 106 — 160 leaves headroom
         * even after stem dedup.
         */
        private const val EXAM_FETCH = 160
        /** Wizard choices per page before a "More…" chip appears. */
        private const val CHOICE_PAGE = 6
        private const val MORE_CHOICE = "More…"
        private const val ANY_TOPIC_LABEL = "Any topic"
        private val CLASS_KEYS = setOf("jss1", "jss2", "jss3", "ss1", "ss2", "ss3")
        private val CLASS_CHOICES = listOf("JSS 1", "JSS 2", "JSS 3", "SS 1", "SS 2", "SS 3")
        private val TERM_CHOICES = listOf("1st Term", "2nd Term", "3rd Term")
        private val ANY_TOPIC_CHOICES = setOf(
            "any topic", "any", "all topics", "everything", "mix", "mixed"
        )
        /** Cancel words (normalised) — an exact stage choice beats these. */
        private val QUIZ_CANCEL_WORDS = setOf(
            "stop", "quit", "exit", "cancel", "abort", "forget it", "never mind",
            "nevermind", "leave it", "no thanks", "that s all", "thats all", "done"
        )
        /**
         * Intents that stay inside the wizard (re-prompt) instead of falling
         * through to normal dispatch: anything not here ends the setup.
         */
        private val WIZARD_KEEP_INTENTS = setOf(
            Intent.UNKNOWN, Intent.GENERAL_KNOWLEDGE, Intent.PRACTICE_QUESTION,
            Intent.SMALL_TALK, Intent.GREETING, Intent.THANKS
        )
    }

    /** Simple "x = 5" / "5" typed during an equation practice round. */
    private val trivialAnswerForm = Regex("^(?:[a-z]\\s*=\\s*)?[-+]?\\d+(?:\\.\\d+)?$")

    private suspend fun handleSessionReply(
        active: Session,
        intent: Intent,
        nq: NormalizedQuery,
        match: IntentMatch
    ): String? {
        val raw = nq.raw.trim()
        val continueRequest = intent == Intent.SMALL_TALK && match.entity == "__continue__"
        return when (active) {
            is Session.QuizSetup -> handleQuizSetup(active, intent, nq, match)
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
                // a clean exit word ends the round before answer-marking
                // ("stop" during a quiz must not be marked as an answer)
                normalizeChoice(raw) in QUIZ_CANCEL_WORDS -> {
                    session = null
                    "Quiz ended. Ask \"practice questions\" or \"random questions\" " +
                        "anytime for another set in the same class."
                }
                // round finished: "more"/"another" reshuffles the SAME
                // pinned scope — the class never changes silently
                active.index >= active.questions.size -> when {
                    continueRequest ||
                        (intent == Intent.PRACTICE_QUESTION &&
                            (match.entity.isNullOrBlank() ||
                                match.entity.equals(active.topic, true))) ->
                        continueQuiz(active)
                    else -> {
                        session = null
                        null
                    }
                }
                continueRequest -> presentQuizQuestion(active)
                intent == Intent.PRACTICE_QUESTION -> {
                    // same topic (or unnamed) -> keep the current round;
                    // a NEW topic/subject request -> fresh class-scoped setup
                    val ent = match.entity
                    if (ent.isNullOrBlank() || ent.equals(active.topic, true)) {
                        presentQuizQuestion(active)
                    } else {
                        session = null
                        null
                    }
                }
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

    // -------------------------------------------------- quiz setup wizard
    // CLASS -> TERM -> SUBJECT -> TOPIC. The class picked here becomes a
    // hard SQL filter inside buildScopedQuiz, so random/shuffled questions
    // can never leave it. Each stage snaps its own choice list, and the
    // chat UI mirrors the current step's choices as tappable chips.

    private suspend fun handleQuizSetup(
        setup: Session.QuizSetup,
        intent: Intent,
        nq: NormalizedQuery,
        match: IntentMatch
    ): String? {
        val nc = normalizeChoice(nq.raw)

        // 1) an exact tap (or exact typed choice) beats every exit word
        stageChoice(setup, nc, exact = true)?.let { return it }

        // 2) "More…" pages long subject/topic lists
        if (nc == "more" && hasMorePages(setup)) {
            setup.page++
            return quizStagePrompt(setup)
        }

        // 3) explicit cancellation — after exact choices, before fuzzy ones
        if (intent == Intent.FAREWELL || nc in QUIZ_CANCEL_WORDS) {
            session = null
            return "No problem — quiz setup cancelled. Say \"practice questions\" or " +
                "\"random questions\" any time and I'll pin your class first."
        }

        // 4) fuzzy match for sloppily typed input — loses to exit words above
        stageChoice(setup, nc, exact = false)?.let { return it }

        // 5) an unrelated question hands control back to normal dispatch
        if (intent !in WIZARD_KEEP_INTENTS) {
            session = null
            return null
        }

        // 6) anything else: gently re-ask the current step
        return "Let's finish setting up your quiz first.\n\n" + quizStagePrompt(setup)
    }

    /** Applies one stage's choice; null = this input is not a valid pick. */
    private suspend fun stageChoice(
        setup: Session.QuizSetup,
        nc: String,
        exact: Boolean
    ): String? {
        when (setup.stage) {
            Session.QuizSetup.Stage.CLASS -> {
                val key = nc.replace(" ", "")
                val cls = if (exact) key.takeIf { it in CLASS_KEYS } else parseClass(nc)
                if (cls != null) {
                    setup.cls = cls
                    setup.stage = Session.QuizSetup.Stage.TERM
                    setup.page = 0
                    return quizStagePrompt(setup)
                }
            }

            Session.QuizSetup.Stage.TERM -> {
                val term = if (exact) parseTermExact(nc) else parseTerm(nc)
                if (term != null) {
                    setup.term = term
                    return openSubjectStage(setup)
                }
            }

            Session.QuizSetup.Stage.SUBJECT -> {
                val hit = if (exact) {
                    setup.subjects.firstOrNull { normalizeChoice(it.subject) == nc }
                } else {
                    setup.subjects.firstOrNull { sameSubject(nc, it.subject) }
                }
                if (hit != null) {
                    if (setup.prefill?.let { sameSubject(it, hit.subject) } == true) {
                        setup.prefill = null   // consumed (a topic prefill may still apply)
                    }
                    setup.subject = hit.subject
                    setup.subjectConceptId = hit.conceptId
                    return openTopicStage(setup)
                }
            }

            Session.QuizSetup.Stage.TOPIC -> {
                if (exact && nc in ANY_TOPIC_CHOICES) {
                    setup.topic = null
                    return startScopedQuiz(setup)
                }
                val topic = if (exact) {
                    setup.topics.firstOrNull { normalizeChoice(it) == nc }
                } else {
                    setup.topics.firstOrNull { topicMatches(nc, it) }
                }
                if (topic != null) {
                    setup.topic = topic
                    return startScopedQuiz(setup)
                }
            }
        }
        return null
    }

    /** Loads the scheme-of-work subjects for the pinned class (+ term). */
    private suspend fun openSubjectStage(setup: Session.QuizSetup): String {
        val cls = setup.cls ?: return quizStagePrompt(setup)
        val src = corpus
        var subjects = src?.schemeSubjects(cls, setup.term).orEmpty()
        if (subjects.isEmpty() && setup.term != null) {
            // no term-tagged entries -> any term for this class (class stays pinned)
            subjects = src?.schemeSubjects(cls, null).orEmpty()
            if (subjects.isNotEmpty()) setup.term = null
        }

        // a subject named up front ("questions on chemistry") jumps to its topics
        setup.prefill?.let { pre ->
            subjects.firstOrNull { sameSubject(pre, it.subject) }?.let { hit ->
                setup.subject = hit.subject
                setup.subjectConceptId = hit.conceptId
                setup.prefill = null
                return openTopicStage(setup)
            }
        }

        if (subjects.isEmpty()) {
            session = null
            return "I couldn't find scheme-of-work material for ${classLabel(cls)} in the shipped " +
                "books. Try another class by asking for a quiz again, ask me anything else, or " +
                "say \"teach me <topic>\" to study instead."
        }
        setup.subjects = subjects
        setup.stage = Session.QuizSetup.Stage.SUBJECT
        setup.page = 0
        return quizStagePrompt(setup)
    }

    /** Loads the weekly lesson topics of the chosen subject. */
    private suspend fun openTopicStage(setup: Session.QuizSetup): String {
        val conceptId = setup.subjectConceptId
        val topics = if (conceptId.isNullOrBlank()) {
            emptyList()
        } else {
            corpus?.lessonTopics(conceptId, 40).orEmpty()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        }
        setup.topics = topics
        setup.stage = Session.QuizSetup.Stage.TOPIC
        setup.page = 0

        // exam-only subject (no lesson notes for this class): quiz the lot
        if (topics.isEmpty()) return startScopedQuiz(setup)

        // a topic named up front matches exactly only — fuzzy could mis-jump
        setup.prefill?.let { pre ->
            val npc = normalizeChoice(pre)
            topics.firstOrNull { normalizeChoice(it) == npc }?.let { t ->
                setup.topic = t
                return startScopedQuiz(setup)
            }
        }
        return quizStagePrompt(setup)
    }

    /** Builds and opens the class-pinned, shuffled, repeat-free quiz. */
    private suspend fun startScopedQuiz(setup: Session.QuizSetup): String {
        val cls = setup.cls
        val subject = setup.subject
        if (cls == null || subject == null) {
            // defensive: a quiz can never start without a pinned class
            val fresh = Session.QuizSetup(prefill = setup.prefill)
            session = fresh
            return quizStagePrompt(fresh)
        }
        val quiz = buildScopedQuiz(cls, setup.term, subject, setup.topic)
        if (quiz == null) {
            session = null
            return "I couldn't find exam questions for ${scopeLine(setup)} in the shipped exam " +
                "papers. Try a different topic, say \"teach me <topic>\" to revise first, or ask " +
                "for a quiz again."
        }
        session = quiz
        lastCandidateId = quiz.questions.firstOrNull()?.conceptId
        lastCandidateScore = 1.0
        lastCandidateConfidence = "HIGH"
        // remember this class on-device and mirror it to Firestore
        onScopePinned?.invoke(cls, classLabel(cls), setup.term)
        return "🎯 ${quizSplit(quiz)} for ${scopeLine(setup)} — shuffled, no repeats:\n\n" +
            presentQuizQuestion(quiz)
    }

    /** "20 objective + 5 theory" — the actual split of a built quiz. */
    private fun quizSplit(quiz: Session.Quiz): String {
        val objective = quiz.questions.count { it.options.isNotEmpty() }
        return "$objective objective + ${quiz.questions.size - objective} theory questions"
    }

    private fun quizStagePrompt(setup: Session.QuizSetup): String = buildString {
        when (setup.stage) {
            Session.QuizSetup.Stage.CLASS -> {
                append("Let's set up your quiz — which class are you in? " +
                    "Every question stays inside that class.")
                setup.prefill?.let { append(" (You asked for \"$it\".)") }
            }
            Session.QuizSetup.Stage.TERM -> {
                append("Got it — ${classLabel(setup.cls ?: "")}. Which term?")
            }
            Session.QuizSetup.Stage.SUBJECT -> {
                val scope = listOfNotNull(
                    setup.cls?.let { classLabel(it) },
                    setup.term?.let { termLabel(it) }
                ).joinToString(" ")
                append("Which subject do you want questions for — $scope?")
            }
            Session.QuizSetup.Stage.TOPIC -> {
                val scope = buildList {
                    setup.cls?.let { add(classLabel(it)) }
                    setup.term?.let { add(termLabel(it)) }
                }.joinToString(" ")
                append("Which topic in ${setup.subject ?: "that subject"} ($scope)?")
            }
        }
        append("\n")
        quizStageChoices(setup).forEach { append("\n• $it") }
        append("\n\n")
        append(
            when (setup.stage) {
                Session.QuizSetup.Stage.CLASS ->
                    "Tap a class or type it — you can say \"stop\" anytime."
                Session.QuizSetup.Stage.TERM ->
                    "Tap a term or type it."
                Session.QuizSetup.Stage.SUBJECT ->
                    "Tap a subject — you'll pick a topic next."
                Session.QuizSetup.Stage.TOPIC ->
                    "Tap a topic, or \"Any topic\" to mix the whole subject. " +
                        "You'll get 20 objective + 5 theory questions — shuffled, never repeated."
            }
        )
    }

    private fun quizStageChoices(setup: Session.QuizSetup): List<String> = when (setup.stage) {
        Session.QuizSetup.Stage.CLASS -> CLASS_CHOICES
        Session.QuizSetup.Stage.TERM -> TERM_CHOICES
        Session.QuizSetup.Stage.SUBJECT ->
            pagedChoices(setup.subjects.map { it.subject }, setup.page)
        Session.QuizSetup.Stage.TOPIC ->
            pagedChoices(setup.topics, setup.page) +
                (if (setup.topics.isNotEmpty()) listOf(ANY_TOPIC_LABEL) else emptyList())
    }

    private fun pagedChoices(items: List<String>, page: Int): List<String> {
        if (items.isEmpty()) return emptyList()
        val from = page * CHOICE_PAGE
        if (from >= items.size) return emptyList()
        val out = items.subList(from, minOf(from + CHOICE_PAGE, items.size)).toMutableList()
        if (from + CHOICE_PAGE < items.size) out.add(MORE_CHOICE)
        return out
    }

    private fun hasMorePages(setup: Session.QuizSetup): Boolean = when (setup.stage) {
        Session.QuizSetup.Stage.SUBJECT ->
            (setup.page + 1) * CHOICE_PAGE < setup.subjects.size
        Session.QuizSetup.Stage.TOPIC ->
            (setup.page + 1) * CHOICE_PAGE < setup.topics.size
        else -> false
    }

    /**
     * Tappable chips mirroring the live state: the current wizard step's
     * choices, or the MCQ option letters during a quiz, empty otherwise.
     */
    private fun currentChoices(): List<String> = when (val s = session) {
        is Session.QuizSetup -> quizStageChoices(s)
        is Session.Quiz -> s.questions.getOrNull(s.index)
            ?.takeIf { it.options.isNotEmpty() }
            ?.options
            ?.mapIndexed { i, _ -> ('A' + i).toString() }
            ?: emptyList()
        else -> emptyList()
    }

    // ------------------------------------------------------ stage parsers

    private fun normalizeChoice(raw: String): String =
        raw.trim().lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

    /** "J.S.S 1" / "jss1" / "i'm in ss2" -> "jss1" / "ss2"; null when absent. */
    private fun parseClass(text: String): String? {
        val stripped = text.replace(Regex("[^a-z0-9]"), "")
        CLASS_KEYS.firstOrNull { stripped == it }?.let { return it }
        // word-bounded so "class 1" or "questions" never parse as a class
        Regex("\\b(j\\s*s\\s*s|s\\s*s)\\s*([1-3])\\b").find(text)?.let { m ->
            return m.groupValues[1].replace(" ", "") + m.groupValues[2]
        }
        return null
    }

    /** Exact wizard chips: "1", "1st", "first", "term 2", "2nd Term"... */
    private fun parseTermExact(nc: String): Int? {
        val s = nc.replace("term", "").trim()
        return when (s) {
            "1", "1st", "first" -> 1
            "2", "2nd", "second" -> 2
            "3", "3rd", "third" -> 3
            else -> null
        }
    }

    /** Fuzzy: "i'm in 2nd term", "second term please" -> 2. */
    private fun parseTerm(text: String): Int? {
        if (parseClass(text) != null) return null     // a class name, not a term
        if (Regex("\\b(first|1st)\\b").containsMatchIn(text)) return 1
        if (Regex("\\b(second|2nd)\\b").containsMatchIn(text)) return 2
        if (Regex("\\b(third|3rd)\\b").containsMatchIn(text)) return 3
        if (Regex("\\b1\\b").containsMatchIn(text)) return 1
        if (Regex("\\b2\\b").containsMatchIn(text)) return 2
        if (Regex("\\b3\\b").containsMatchIn(text)) return 3
        return null
    }

    /** Generic words dropped from both sides of a subject comparison. */
    private val wizardSubjectStop = setOf(
        "and", "or", "of", "for", "the", "a", "an", "in", "on",
        "language", "languages", "studies", "study", "practice",
        "subject", "paper", "questions"
    )

    private fun wizardSubjectTokens(s: String): List<String> =
        s.lowercase()
            .replace('&', ' ')
            .replace(Regex("\\([^)]*\\)"), " ")
            .replace(Regex("[^a-z0-9]+"), " ")
            .split(' ')
            .filter { it.length > 1 && it !in wizardSubjectStop }
            .map { if (it.length > 3 && it.endsWith("s")) it.dropLast(1) else it }

    /**
     * Same rule as KnowledgeCoreStore.sameSubject: equal token sets match
     * outright, otherwise BOTH sides must overlap by >= 60% — so
     * "English Studies" ~ "English Language" passes while "Mathematics"
     * never swallows "Further Mathematics". Single-word subjects get two
     * extra allowances: a prefix ("math" -> Mathematics) and a small edit
     * budget ("buology" -> Biology), both gated on >= 4 characters so
     * short noise can never pin a subject.
     */
    private fun sameSubject(a: String, b: String): Boolean {
        val ta = wizardSubjectTokens(a)
        val tb = wizardSubjectTokens(b)
        if (ta.isEmpty() || tb.isEmpty()) return false
        if (ta == tb) return true
        if (ta.size == 1 && tb.size == 1) {
            val x = ta[0]
            val y = tb[0]
            if (x.length >= 4 && y.length >= 4) {
                if (y.startsWith(x) || x.startsWith(y)) return true
                return QueryNormalizer.withinEditDistance(x, y, 2)
            }
        }
        val sa = ta.toSet()
        val sb = tb.toSet()
        val inter = sa.intersect(sb).size
        return inter.toDouble() / ta.size >= 0.6 && inter.toDouble() / tb.size >= 0.6
    }

    /** Loose containment for topic names (the list is already subject-scoped). */
    private fun topicMatches(typed: String, topic: String): Boolean {
        val a = normalizeChoice(typed)
        val b = normalizeChoice(topic)
        if (a.isEmpty() || b.isEmpty()) return false
        if (a == b) return true
        if (a.length >= 4 && b.contains(a)) return true
        if (b.length >= 4 && a.contains(b)) return true
        return false
    }

    private fun classLabel(cls: String): String {
        val c = cls.trim().lowercase()
        return if (c.length >= 3 && c.last().isDigit()) {
            c.dropLast(1).uppercase() + " " + c.last()
        } else {
            c.uppercase()
        }
    }

    private fun termLabel(term: Int): String = when (term) {
        1 -> "1st Term"
        2 -> "2nd Term"
        3 -> "3rd Term"
        else -> "$term Term"
    }

    /** "JSS 1 · 1st Term · Chemistry · Acids" — shown with the quiz. */
    private fun scopeLine(setup: Session.QuizSetup): String =
        scopeLine(setup.cls, setup.term, setup.subject, setup.topic)

    private fun scopeLine(cls: String?, term: Int?, subject: String?, topic: String?): String =
        buildList {
            cls?.let { add(classLabel(it)) }
            term?.let { add(termLabel(it)) }
            subject?.let { add(it) }
            topic?.let { add(it) }
        }.joinToString(" · ")

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
            // session stays alive (finished): "more" reshuffles this scope
            return finishMessage(quiz)
        }
        val q = quiz.questions[i]
        return buildString {
            append("Question ${i + 1} of ${quiz.questions.size}")
            append(if (q.options.isNotEmpty()) " · Objective" else " · Theory")
            append("\n\n")
            append(q.stem)
            if (q.options.isNotEmpty()) {
                append("\n")
                q.options.forEachIndexed { oi, opt -> append("\n${'A' + oi}) $opt") }
                append("\n\nTap your answer below — or type just the letter (A, B, C, D).")
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
                    ?: return "I couldn't catch your choice — tap an option below " +
                        "(or reply with just the letter A, B, C or D)."
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
            // keep the (finished) session alive: "more" reshuffles this scope
            sb.append("\n\n").append(finishMessage(quiz))
        }
        return sb.toString()
    }

    /** End-of-round text; the finished session stays so "more" can continue. */
    private fun finishMessage(quiz: Session.Quiz): String {
        val scope = quiz.cls?.let {
            " for ${scopeLine(quiz.cls, quiz.term, quiz.subject, null)}"
        } ?: ""
        return "🎉 That's all ${quiz.questions.size} questions (${quizSplit(quiz)})$scope. " +
            "Say \"more\" for another shuffled set, name a different topic, " +
            "or \"teach me <topic>\" to revise first."
    }

    /** "more" after a finished round: a fresh shuffle of the SAME scope. */
    private suspend fun continueQuiz(quiz: Session.Quiz): String? {
        val cls = quiz.cls
        if (cls != null) {
            val fresh = buildScopedQuiz(cls, quiz.term, quiz.subject, quiz.topic)
            if (fresh != null) {
                session = fresh
                lastCandidateId = fresh.questions.firstOrNull()?.conceptId
                lastCandidateScore = 1.0
                lastCandidateConfidence = "HIGH"
                return "🔄 Another shuffled set — ${quizSplit(fresh)} for " +
                    scopeLine(quiz.cls, quiz.term, quiz.subject, null) + ":\n\n" +
                    presentQuizQuestion(fresh)
            }
        }
        session = null
        return "🎉 That's all the \"${quiz.topic}\" questions I have in this scope — " +
            "you've seen every one. Name another topic, or say \"teach me <topic>\" to revise."
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

    /**
     * Repairs a stale-seed arrow artifact: some devices still carry Room
     * rows from an older pack where " → " was mangled into " ’n ".
     * The SEED_VERSION bump re-seeds clean data; this keeps any copy that
     * escaped the re-seed readable in the meantime.
     */
    private fun fixArrowArtifacts(text: String): String =
        text.replace(Regex(" ['’]n "), " → ")

    private suspend fun renderConcept(c: ConceptRecord): String {
        val builder = StringBuilder()
        builder.append(fixArrowArtifacts(c.definition.trim()))
        if (c.formula.isNotBlank()) {
            builder.append("\n\nFormula: ${c.formula}")
        }
        c.examples.firstOrNull()?.let { builder.append("\n\nExample: ${fixArrowArtifacts(it)}") }
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
                "${fixArrowArtifacts(concept.definition.trim())}\n\n" +
                    concept.examples.mapIndexed { i, ex ->
                        "Example ${i + 1}: ${fixArrowArtifacts(ex)}"
                    }.joinToString("\n")
            concept != null -> renderConcept(concept)
            entry.subject != null -> renderSubject(entry.subject)
            else -> entry.definition
        }
    }

    private suspend fun practiceResponse(match: IntentMatch, ctx: ConversationContext): String {
        // 1) class-scoped wizard (default): pin CLASS -> TERM -> SUBJECT ->
        //    TOPIC first, so questions (random/shuffled or not) can never
        //    leave the student's class. "random questions" / "shuffle
        //    questions" phrasing lands here too via IntentClassifier.
        if (corpus != null) {
            val setup = Session.QuizSetup(prefill = match.entity?.takeIf { it.isNotBlank() })
            session = setup
            return quizStagePrompt(setup)
        }

        // 2) legacy fallback (unit tests / DB unavailable): subject subtopics
        val subject = match.entity?.let { index.resolveEntity(it) }?.subject
            ?: ctx.activeSubject?.let { id -> enabledSubjects.firstOrNull { it.id == id } }
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
     * Builds a quiz pinned to ONE class from the ingested exam papers.
     *
     * The class key is a hard filter on EVERY rung of the widening ladder
     * (drop topic, then term, then subject — the class never drops), so
     * randomisation can never leave the chosen class. Questions are then
     * shuffled inside that pinned scope, and stems already shown
     * (QuizHistory) are skipped — fresh questions first, least-recently
     * asked ones reused only as a last resort for a tiny scope.
     */
    private suspend fun buildScopedQuiz(
        cls: String,
        term: Int?,
        subject: String?,
        topic: String?
    ): Session.Quiz? {
        val src = corpus ?: return null
        val topicTerms = entityTerms(topic).filter { it !in QueryNormalizer.stopwords }
        val rungs = buildList {
            add(Triple(term, subject, topicTerms))
            add(Triple(term, subject, emptyList<String>()))
            add(Triple(null, subject, emptyList<String>()))
            add(Triple(null, null, emptyList<String>()))
        }

        val asked = quizHistory.askedStems()
        val keys = HashMap<String, Map<Int, String>>()
        val freshObj = ArrayList<QuizQuestion>(OBJECTIVE_SIZE)
        val freshThy = ArrayList<QuizQuestion>(THEORY_SIZE)
        val staleObj = ArrayList<QuizQuestion>()
        val staleThy = ArrayList<QuizQuestion>()
        val seenStems = HashSet<String>()
        run@ for ((rTerm, rSubject, rTerms) in rungs) {
            val hits = src.examChunksFor(cls, rTerm, rSubject, rTerms, EXAM_FETCH)
            for (hit in hits) {
                parseAnswerKeys(hit.text).let { parsed ->
                    if (parsed.isNotEmpty()) keys[hit.conceptId] = parsed
                }
                // "B: THEORY" sections are typed answers even when a stray
                // "A." in the prose looks like an option marker
                val theorySection = hit.headerContext.contains("THEORY", ignoreCase = true)
                for (q in parseQuestions(stripAnswerKeySections(hit.text))) {
                    val item = if (theorySection) {
                        q.copy(conceptId = hit.conceptId, options = emptyList())
                    } else {
                        q.copy(conceptId = hit.conceptId)
                    }
                    if (!seenStems.add(item.stem.lowercase())) continue
                    val objective = item.options.isNotEmpty()
                    if (QuizHistory.stemHash(item.stem) in asked) {
                        if (objective) staleObj.add(item) else staleThy.add(item)
                    } else {
                        if (objective) freshObj.add(item) else freshThy.add(item)
                    }
                    if (freshObj.size >= OBJECTIVE_SIZE && freshThy.size >= THEORY_SIZE) break@run
                }
            }
        }
        // objectives first (shuffled), then theory (shuffled) — both inside
        // the pinned class; stale stems only fill slots no fresh one reached
        val objectives = (freshObj + staleObj).take(OBJECTIVE_SIZE).shuffled()
        val theories = (freshThy + staleThy).take(THEORY_SIZE).shuffled()
        val questions = objectives + theories
        if (questions.isEmpty()) return null

        // remember everything shown this round -> never asked again
        quizHistory.remember(questions.map { QuizHistory.stemHash(it.stem) })

        // reference passage: the topic's definition/notes first, the
        // subject's next (used to mark answers where a paper has no key)
        val refTerms = topicTerms.ifEmpty {
            entityTerms(subject).filter { it !in QueryNormalizer.stopwords }
        }
        val reference = if (refTerms.isEmpty()) {
            ""
        } else {
            src.search(refTerms, 1, "definition").firstOrNull()?.text
                ?: src.search(refTerms, 1, "lesson_notes").firstOrNull()?.text
                ?: ""
        }
        val label = topic ?: subject ?: classLabel(cls)
        return Session.Quiz(label, questions, keys, reference, 0, cls, term, subject)
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
            "• Exam practice — \"give me practice questions on chemistry\" " +
                "(20 objective + 5 theory, real past questions from your class)\n" +
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
