package com.kuzmix.ai

import com.kuzmix.ai.domain.ai.QueryNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QueryNormalizerTest {

    private fun phrase(raw: String): String = QueryNormalizer.phraseText(QueryNormalizer.normalize(raw))

    @Test
    fun stopwordsAreKept_soMotionPhraseStillMatchesFts() {
        assertEquals("laws of motion", phrase("laws of motion"))
        assertEquals("a polygon", phrase("what is a polygon"))
    }

    @Test
    fun leadingQuestionWordsAreStripped() {
        assertEquals("photosynthesis", phrase("what is photosynthesis"))
        assertEquals("mitosis and meiosis", phrase("difference between mitosis and meiosis"))
        assertEquals("the water cycle work", phrase("how does the water cycle work"))
        assertEquals("formulated newton laws", phrase("who formulated newton laws"))
    }

    @Test
    fun leadingAuxiliaryIsStripped() {
        assertEquals("the sun a star", phrase("is the sun a star"))
    }

    @Test
    fun trailingPunctuationIsGone_alreadyNormalized() {
        assertEquals("cell wall", phrase("Cell wall?"))
        assertEquals("photosynthesis", phrase("explain photosynthesis!"))
    }

    @Test
    fun emptyInputYieldsEmptyPhrase() {
        assertTrue(phrase("").isEmpty())
        assertTrue(phrase("what").isEmpty())
    }
}
