package com.kuzmix.ai

import com.kuzmix.ai.domain.ai.BiblePassage
import com.kuzmix.ai.domain.ai.CorpusChunk
import com.kuzmix.ai.domain.ai.CorpusSource
import com.kuzmix.ai.domain.ai.InMemoryQuizHistory
import com.kuzmix.ai.domain.ai.KnowledgeEngine
import com.kuzmix.ai.domain.ai.KnowledgePack
import com.kuzmix.ai.domain.ai.KnowledgePackParser
import com.kuzmix.ai.domain.ai.SchemeSubject
import com.kuzmix.ai.domain.model.ChatMessage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Class-scoped quiz wizard: CLASS -> TERM -> SUBJECT -> TOPIC, delivering
 * 20 objective + 5 theory questions pinned to the chosen class, shuffled,
 * never repeating, with tappable chips at every step.
 */
class QuizWizardTest {

    private lateinit var corpus: FakeCorpus
    private lateinit var quizHistory: InMemoryQuizHistory
    private lateinit var engine: KnowledgeEngine
    private val dialogue = mutableListOf<ChatMessage>()

    @Before
    fun setUp() {
        corpus = FakeCorpus()
        quizHistory = InMemoryQuizHistory()
        engine = KnowledgeEngine(loadPack(), null, corpus, quizHistory)
        dialogue.clear()
    }

    private fun ask(text: String): String = runBlocking {
        dialogue.add(user(text))
        val reply = engine.generateResponse(text, dialogue.toList())
        dialogue.add(assistant(reply))
        reply
    }

    private fun user(text: String) =
        ChatMessage(role = ChatMessage.Role.USER, content = text)

    private fun assistant(text: String) =
        ChatMessage(role = ChatMessage.Role.ASSISTANT, content = text)

    /** Walks the full wizard and returns the first question shown. */
    private fun startQuiz(
        entry: String = "practice questions",
        cls: String = "JSS 1",
        term: String = "2nd Term",
        subject: String = "Chemistry",
        topic: String = "Photosynthesis"
    ): String {
        var reply = ask(entry)
        assertTrue(reply, reply.contains("which class", ignoreCase = true))
        reply = ask(cls)
        assertTrue(reply, reply.contains("Which term"))
        reply = ask(term)
        if (reply.contains("Which subject")) {
            reply = ask(subject)
            assertTrue(reply, reply.contains("Which topic"))
        }
        if (reply.contains("Which topic")) {
            reply = ask(topic)
        }
        assertTrue(reply, reply.contains("Question 1 of 25"))
        return reply
    }

    /** The stem of the question currently on screen. */
    private fun stemOf(reply: String): String =
        Regex("Question \\d+ of \\d+ · (?:Objective|Theory)\\n\\n([^\\n]+)")
            .find(reply)?.groupValues?.get(1)
            ?: error("no question found in: $reply")

    // ------------------------------------------------------------ wizard

    @Test
    fun wizardStartsWithClassChoices() {
        val reply = ask("practice questions")
        assertTrue(reply, reply.contains("which class", ignoreCase = true))
        assertEquals(
            listOf("JSS 1", "JSS 2", "JSS 3", "SS 1", "SS 2", "SS 3"),
            engine.pendingChoices
        )
    }

    @Test
    fun randomPhrasingEntersSameWizard() {
        val reply = ask("random questions")
        assertTrue(reply, reply.contains("which class", ignoreCase = true))
        val shuffle = runBlocking {
            val e = KnowledgeEngine(loadPack(), null, FakeCorpus(), InMemoryQuizHistory())
            e.generateResponse("shuffle my questions", listOf(user("shuffle my questions")))
        }
        assertTrue(shuffle, shuffle.contains("which class", ignoreCase = true))
    }

    @Test
    fun fuzzyClassInputAccepted() {
        ask("practice questions")
        val reply = ask("i am in ss2")
        assertTrue(reply, reply.contains("Got it — SS 2"))
    }

    @Test
    fun stopCancelsWizard() {
        ask("practice questions")
        val reply = ask("stop")
        assertTrue(reply, reply.contains("quiz setup cancelled"))
        assertTrue(engine.pendingChoices.isEmpty())
    }

    @Test
    fun subjectNamedUpFrontSkipsSubjectStage() {
        var reply = ask("practice questions on chemistry")
        assertTrue(reply, reply.contains("which class", ignoreCase = true))
        reply = ask("JSS 1")
        assertTrue(reply, reply.contains("Which term"))
        reply = ask("2nd term")
        assertTrue(reply, reply.contains("Which topic in Chemistry"))
    }

    @Test
    fun topicStageOffersTopicsAndAnyTopic() {
        ask("practice questions")
        ask("JSS 1")
        ask("2nd term")
        ask("Chemistry")
        val chips = engine.pendingChoices
        assertTrue(chips.toString(), chips.contains("Photosynthesis"))
        assertTrue(chips.toString(), chips.contains("Any topic"))
    }

    @Test
    fun examOnlySubjectSkipsTopicStage() {
        corpus.subjects = listOf(SchemeSubject("Chemistry", ""))
        ask("practice questions")
        ask("JSS 1")
        ask("2nd term")
        val reply = ask("Chemistry")
        assertTrue(reply, reply.contains("Question 1 of 25"))
    }

    // ------------------------------------------------------- the quiz

    @Test
    fun fullFlowDelivers20ObjectiveAnd5TheoryInsideClass() {
        val start = startQuiz()
        assertTrue(start, start.contains("20 objective + 5 theory"))
        assertTrue(start, start.contains("JSS 1 · 2nd Term · Chemistry · Photosynthesis"))
        assertEquals(listOf("A", "B", "C", "D"), engine.pendingChoices)
        assertTrue(corpus.examCalls.isNotEmpty())
        assertTrue(
            "class leaked: $corpus.examCalls",
            corpus.examCalls.all { it.cls == "jss1" }
        )

        val next = ask("A")
        assertTrue(next, next.contains("Question 2 of 25 · Objective"))
    }

    @Test
    fun theoryQuestionsFollowTheObjectives() {
        startQuiz()
        var reply = ""
        repeat(20) { reply = ask("A") }   // 20 objectives answered -> Q21
        assertTrue(reply, reply.contains("Question 21 of 25 · Theory"))
        assertFalse(reply, reply.contains("A)"))
        assertTrue("theory must not show option chips", engine.pendingChoices.isEmpty())
    }

    @Test
    fun stopDuringQuizEndsTheRound() {
        startQuiz()
        val reply = ask("stop")
        assertTrue(reply, reply.contains("Quiz ended"))
        assertTrue(engine.pendingChoices.isEmpty())
    }

    @Test
    fun wideningLadderNeverLeavesTheClass() {
        // only the widest rung (any term, any subject) returns questions —
        // the ladder must relax everything EXCEPT the class
        corpus.accept = { _, term, subject -> term == null && subject == null }
        val start = startQuiz()
        assertTrue(start, start.contains("Question 1 of 25"))
        assertTrue("ladder should have widened", corpus.examCalls.size >= 4)
        assertTrue(
            "class leaked: $corpus.examCalls",
            corpus.examCalls.all { it.cls == "jss1" }
        )
    }

    @Test
    fun noRepeatedQuestionsAcrossRounds() {
        val first = startQuiz()
        val firstStem = stemOf(first)
        repeat(20) { ask("A") }                     // finish the objectives
        repeat(5) { ask("my detailed answer") }     // finish the theory
        val more = ask("more")                      // fresh set, same scope
        assertTrue(more, more.contains("Question 1 of 25"))
        val askedStems = runBlocking { quizHistory.askedStems() }
        assertEquals(50, askedStems.size)
        assertFalse(
            "round 2 repeated a round-1 question",
            more.contains(firstStem)
        )
    }

    // ------------------------------------------------------------ fakes

    private class FakeCorpus(
        var subjects: List<SchemeSubject> = listOf(
            SchemeSubject("Chemistry", "c_cur_jss1_chemistry_t2"),
            SchemeSubject("Mathematics", "c_cur_jss1_mathematics_t2")
        ),
        var topics: List<String> = listOf("Photosynthesis", "Respiration"),
        var objectiveCount: Int = 40,
        var theoryCount: Int = 10,
        var accept: (cls: String, term: Int?, subject: String?) -> Boolean = { _, _, _ -> true }
    ) : CorpusSource {

        data class ExamCall(val cls: String, val term: Int?, val subject: String?)

        val examCalls = mutableListOf<ExamCall>()

        override suspend fun search(
            terms: List<String>, limit: Int, chunkType: String?
        ): List<CorpusChunk> = emptyList()

        override suspend fun forConcept(
            conceptId: String, limit: Int, chunkType: String?
        ): List<CorpusChunk> = emptyList()

        override suspend fun bibleRange(
            book: String, chapter: Int, fromVerse: Int, toVerse: Int
        ): List<BiblePassage> = emptyList()

        override suspend fun bibleSearch(
            terms: List<String>, limit: Int
        ): List<BiblePassage> = emptyList()

        override suspend fun schemeSubjects(cls: String, term: Int?): List<SchemeSubject> =
            if (cls == "jss1") subjects else emptyList()

        override suspend fun lessonTopics(conceptId: String, limit: Int): List<String> = topics

        override suspend fun examChunksFor(
            cls: String,
            term: Int?,
            subject: String?,
            terms: List<String>,
            limit: Int
        ): List<CorpusChunk> {
            examCalls.add(ExamCall(cls, term, subject))
            if (!accept(cls, term, subject)) return emptyList()
            val objective = buildString {
                for (i in 1..objectiveCount) {
                    append("Q$i. Objective question number $i about acids and bases? ")
                    append("A) Option one B) Option two C) Option three D) Option four\n")
                }
            }
            val theory = buildString {
                for (i in 1..theoryCount) {
                    append("Q${objectiveCount + i}. ")
                    append("Theory question number $i explain it well. (5 marks)\n")
                }
            }
            return listOf(
                CorpusChunk(
                    conceptId = "c_exam_jss1_chemistry_t2",
                    conceptName = "Chemistry",
                    chunkType = "exam_questions",
                    headerContext = "Science > Chemistry > JSS 1 Chemistry Examination > A: OBJECTIVE",
                    text = objective,
                    chunkIndex = 0
                ),
                CorpusChunk(
                    conceptId = "c_exam_jss1_chemistry_t2",
                    conceptName = "Chemistry",
                    chunkType = "exam_questions",
                    headerContext = "Science > Chemistry > JSS 1 Chemistry Examination > B: THEORY",
                    text = theory,
                    chunkIndex = 1
                )
            )
        }
    }

    // --------------------------------------------------------- loading

    private fun loadPack(): KnowledgePack {
        val base = readRaw("knowledge_base.json")
        val subjects = readRaw("kp_subjects.json")
        val concepts = listOf(
            "kp_concepts_science.json",
            "kp_concepts_tech.json",
            "kp_concepts_humanities.json"
        ).map { readRaw(it) }
        return KnowledgePackParser.merge(base, subjects, concepts)
    }

    private fun readRaw(name: String): String {
        val candidates = listOf(
            File("src/main/res/raw/$name"),
            File("app/src/main/res/raw/$name")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("Test resource not found: $name (cwd=${File(".").absolutePath})")
        return file.readText()
    }
}
