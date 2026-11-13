package com.kuzmix.ai

import com.kuzmix.ai.domain.ai.Intent
import com.kuzmix.ai.domain.ai.IntentClassifier
import com.kuzmix.ai.domain.ai.QueryNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Classification contracts for the v2.0 features — equations must beat
 * CALCULATION/DEFINITION, the Bible must beat DEFINITION, soft keywords
 * must not hijack teaching requests, and the session controls
 * ("more", "another") must route as continue.
 */
class IntentV2Test {

    private fun classify(raw: String) = IntentClassifier.classify(QueryNormalizer.normalize(raw))

    // ------------------------------------------------------ equations

    @Test
    fun equationBeatsCalculationAndDefinition() {
        // "solve" would otherwise be CALCULATION
        assertEquals(Intent.SOLVE_EQUATION, classify("solve 1x - 2 = 32").intent)
        // "what is ..." would otherwise be DEFINITION
        assertEquals(Intent.SOLVE_EQUATION, classify("what is 2x - 3 = 34").intent)
        assertEquals(Intent.SOLVE_EQUATION, classify("2x - 3 = 34").intent)
        assertEquals(Intent.SOLVE_EQUATION, classify("x^2 - 5x + 6 = 0").intent)
        assertEquals(Intent.SOLVE_EQUATION, classify("2(x - 3) = 4x + 1").intent)
        assertEquals(Intent.SOLVE_EQUATION, classify("solve for y: 2y + 4 = 10").intent)
    }

    @Test
    fun equationCarriesExtractedExpressionAsEntity() {
        assertEquals("2x - 3 = 34", classify("what is 2x - 3 = 34").entity)
        assertEquals("x = 5", classify("x = 5").entity)     // practice auto-mark reply
    }

    @Test
    fun factorRequestClassifiedAsEquation() {
        val m = classify("factorise x^2 + 5x + 6")
        assertEquals(Intent.SOLVE_EQUATION, m.intent)
        assertTrue(m.entity!!.contains("x^2"))
    }

    @Test
    fun plainArithmeticStaysCalculation() {
        assertEquals(Intent.CALCULATION, classify("calculate 12 * 8").intent)
        assertEquals(Intent.CALCULATION, classify("what is 5% of 200").intent)
    }

    // ---------------------------------------------------------- bible

    @Test
    fun verseReferenceIsBibleQueryWithCanonicalEntity() {
        assertEquals(Intent.BIBLE_QUERY, classify("John 3:16").intent)
        assertEquals("John 3:16", classify("John 3:16").entity)

        val says = classify("what does John 3:16 say")
        assertEquals(Intent.BIBLE_QUERY, says.intent)
        assertEquals("John 3:16", says.entity)

        assertEquals("1 Corinthians 13:4-7", classify("1 cor 13:4-7").entity)
        assertEquals("Psalms 23", classify("Psalm 23").entity)
    }

    @Test
    fun strongKeywordSearchIsBible_withoutReference() {
        val m = classify("bible verses about love")
        assertEquals(Intent.BIBLE_QUERY, m.intent)
        assertNull(m.entity)                    // no reference -> topical FTS search
    }

    @Test
    fun softKeywordLosesToTeachingRequests() {
        assertEquals(Intent.DEFINITION, classify("explain what a psalm is").intent)
        assertEquals(Intent.DEFINITION, classify("what is prayer").intent)
        // but a chapter reference still wins
        assertEquals(Intent.BIBLE_QUERY, classify("Psalm 23").intent)
    }

    @Test
    fun taskInstructionsAreNotBibleReferences() {
        assertTrue(classify("mark 10 questions").intent != Intent.BIBLE_QUERY)
        assertTrue(classify("give me 5 examples of polygons").intent != Intent.BIBLE_QUERY)
    }

    @Test
    fun equationsInsideBibleCheck_doNotResolve() {
        assertTrue(classify("2x - 3 = 34").intent != Intent.BIBLE_QUERY)
    }

    // ------------------------------------------------- study + practice

    @Test
    fun teachMeExtractsTopicEntity() {
        val m = classify("teach me photosynthesis")
        assertEquals(Intent.STUDY_HELP, m.intent)
        assertEquals("photosynthesis", m.entity)
    }

    @Test
    fun helpWithExtractsSubjectEntity() {
        val m = classify("help me with biology")
        assertEquals(Intent.STUDY_HELP, m.intent)
        assertEquals("biology", m.entity)
    }

    @Test
    fun practiceQuestionExtractsTopic() {
        val m = classify("give me practice questions on agriculture")
        assertEquals(Intent.PRACTICE_QUESTION, m.intent)
        assertEquals("agriculture", m.entity)
    }

    // ------------------------------------------------------ sessions

    @Test
    fun sessionControlsClassifyAsContinue() {
        for (raw in listOf("more", "another", "one more", "next", "again")) {
            val m = classify(raw)
            assertEquals("'$raw' must continue the live session", Intent.SMALL_TALK, m.intent)
            assertEquals("__continue__", m.entity)
        }
    }

    @Test
    fun definitionIntentStillWorksForNormalQuestions() {
        assertEquals(Intent.DEFINITION, classify("what is photosynthesis").intent)
        assertEquals(Intent.IDENTITY, classify("who are you").intent)
        assertEquals(Intent.GREETING, classify("hello").intent)
    }
}
