package com.kuzmix.ai

import com.kuzmix.ai.domain.ai.FtsMatcher
import com.kuzmix.ai.domain.ai.NormalizedQuery
import com.kuzmix.ai.domain.ai.QueryNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FtsMatcherTest {

    @Test
    fun emptyOrStopwordOnlyInputProducesNoMatch() {
        assertNull(FtsMatcher.buildMatch(emptyList()))
        assertNull(FtsMatcher.buildMatch(listOf("", "  ")))

        val nq = QueryNormalizer.normalize("what is the")
        assertNull("stopword-only query must not MATCH everything", FtsMatcher.matchFor(nq))
    }

    @Test
    fun everyTermIsQuoted_soUserTextCannotBecomeFtsSyntax() {
        val match = FtsMatcher.buildMatch(listOf("near", "and", "photosynthesis"))
        assertEquals("\"near\" OR \"and\" OR \"photosynthesis\"", match)
    }

    @Test
    fun embeddedQuotesAndOperatorsAreNeutralised() {
        val match = FtsMatcher.buildMatch(listOf("a\"b", "(x*y)", "don't"))
        assertTrue(match != null)
        assertTrue(match!!.startsWith("\""))
        assertTrue("operators must sit inside quotes", match.contains("\"(x*y)\""))
        assertTrue(!match.contains("\"a\""))
    }

    @Test
    fun termsAreDeduplicatedAndCapped() {
        val match = FtsMatcher.buildMatch(listOf("cell") + (1..20).map { "t$it" })
        val parts = match!!.split(" OR ")
        assertTrue("capped at 12 terms, got ${parts.size}", parts.size <= 12)
        assertEquals("\"cell\"", parts.first())
        assertEquals(1, parts.count { it == "\"cell\"" })
    }

    @Test
    fun matchForUsesContentTokensFromNormalizedQuery() {
        val nq = QueryNormalizer.normalize("what is photosynthesis")
        val match = FtsMatcher.matchFor(nq)
        assertEquals("\"photosynthesis\"", match)
    }

    @Test
    fun multiWordQuery_orTierKeepsTokensSeparate() {
        val nq = QueryNormalizer.normalize("what is cell wall")
        assertEquals("\"cell\" OR \"wall\"", FtsMatcher.matchFor(nq))
    }

    @Test
    fun phraseMatch_quotesMultiWordAdjacency() {
        assertEquals("\"cell wall\"", FtsMatcher.phraseMatch("cell wall"))
        assertEquals("\"cell wall\"", FtsMatcher.phraseMatch("  cell   wall  "))
        assertEquals("\"cell wall\"", FtsMatcher.phraseMatch("cell \" wall"))
        assertEquals("\"nucleus\"", FtsMatcher.phraseMatch("nucleus"))
        assertNull(FtsMatcher.phraseMatch("   "))
        assertNull(FtsMatcher.phraseMatch(""))
    }

    @Test
    fun atSampleQueriesProduceMatchableExpressions() {
        val samples = listOf(
            "what is government",
            "explain encryption in cybersecurity",
            "difference between mitosis and meiosis",
            "what is a polygon"
        )
        for (sample in samples) {
            val nq = QueryNormalizer.normalize(sample)
            val match = FtsMatcher.matchFor(nq)
            assertTrue("no match for: $sample", !match.isNullOrBlank())
            assertTrue(match!!.startsWith("\""))
        }
    }

    @Test
    fun prefixMatch_quotesTermsWithStar_andSkipsShortTokens() {
        assertEquals("\"mito\"*", FtsMatcher.prefixMatch(listOf("mito")))
        assertEquals("\"mito\"* OR \"cell\"*", FtsMatcher.prefixMatch(listOf("mito", "cell")))
        assertNull(FtsMatcher.prefixMatch(listOf("a", "to")))
        assertNull(FtsMatcher.prefixMatch(emptyList()))
        val capped = FtsMatcher.prefixMatch((1..20).map { "tok$it" })
        assertTrue("capped at 12 terms", capped!!.split(" OR ").size <= 12)
    }

    @Test
    fun inflectionVariants_coverCommonPluralForms() {
        assertEquals(listOf("cells"), FtsMatcher.inflectionVariants("cell"))
        assertEquals(listOf("cell"), FtsMatcher.inflectionVariants("cells"))
        assertEquals(listOf("frequency"), FtsMatcher.inflectionVariants("frequencies"))
        assertTrue(FtsMatcher.inflectionVariants("energy").contains("energies"))
        assertTrue(FtsMatcher.inflectionVariants("box").contains("boxes"))
    }

    @Test
    fun inflectionVariants_skipLatinEndings_andShortTokens() {
        assertTrue(FtsMatcher.inflectionVariants("mitosis").isEmpty())
        assertTrue(FtsMatcher.inflectionVariants("bus").isEmpty())
        assertTrue(FtsMatcher.inflectionVariants("it").isEmpty())
        for (t in listOf("cell", "cells", "energy", "boxes")) {
            FtsMatcher.inflectionVariants(t).forEach { assertTrue(it != t) }
        }
    }
}
