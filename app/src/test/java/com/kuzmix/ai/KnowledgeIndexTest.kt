package com.kuzmix.ai

import com.kuzmix.ai.domain.ai.ConceptRecord
import com.kuzmix.ai.domain.ai.ConceptSource
import com.kuzmix.ai.domain.ai.Intent
import com.kuzmix.ai.domain.ai.KnowledgeIndex
import com.kuzmix.ai.domain.ai.KnowledgePack
import com.kuzmix.ai.domain.ai.NormalizedQuery
import com.kuzmix.ai.domain.ai.QueryNormalizer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Disk-mode KnowledgeIndex behavior: FTS rank prior, coverage gate,
 * static/dynamic dedupe, and legacy parity when no source is wired.
 */
class KnowledgeIndexTest {

    private fun concept(
        id: String,
        name: String,
        definition: String = "",
        keywords: List<String> = emptyList(),
        aliases: List<String> = emptyList()
    ) = ConceptRecord(
        id = id,
        type = "concept",
        domain = "sci",
        name = name,
        aliases = aliases,
        definition = definition,
        keywords = keywords,
        related = emptyList(),
        examples = emptyList(),
        misconceptions = emptyList(),
        formula = "",
        confidence = "HIGH"
    )

    private val pack = KnowledgePack(
        knowledgeVersion = "test",
        subjects = emptyList(),
        concepts = listOf(
            concept("c_static", "Photosynthesis", "plants convert light into chemical energy")
        ),
        formulas = emptyList(),
        misconceptions = emptyList(),
        relations = emptyList(),
        responses = emptyMap(),
        variations = emptyMap()
    )

    private class StubSource(private val result: List<ConceptRecord>) : ConceptSource {
        override suspend fun candidates(nq: NormalizedQuery): List<ConceptRecord> = result
        override suspend fun lookup(entity: String): ConceptRecord? = null
        override suspend fun byId(id: String): ConceptRecord? = result.firstOrNull { it.id == id }
    }

    private fun nq(text: String): NormalizedQuery = QueryNormalizer.normalize(text)

    @Test
    fun ftsRankPrior_ordersOtherwiseEqualCandidates_andRaisesScore() = runBlocking {
        val alpha = concept("c_a", "Alpha Basin", "the reservoir holds water")
        val beta = concept("c_b", "Beta Basin", "the reservoir holds water")
        val index = KnowledgeIndex(pack, StubSource(listOf(alpha, beta)))

        val results = index.search(
            nq("water"),
            com.kuzmix.ai.domain.ai.ConversationContext(),
            Intent.GENERAL_KNOWLEDGE,
            minScore = 0.0
        )

        val alphaResult = results.first { it.entry.id == "c_a" }
        val betaResult = results.first { it.entry.id == "c_b" }
        assertTrue("FTS-first candidate must outscore the second", alphaResult.score > betaResult.score)
        assertEquals("c_a", results.first().entry.id)
    }

    @Test
    fun coverageGate_dropsWeakContextNoise_keepsNameHit() = runBlocking {
        val noise = concept(
            "c_noise", "Totally Unrelated Plant",
            definition = "a leaf of grass in a meadow"
        )
        val index = KnowledgeIndex(pack, StubSource(listOf(noise)))

        val results = index.search(
            nq("photosynthesis light chlorophyll leaf"),
            com.kuzmix.ai.domain.ai.ConversationContext(),
            Intent.GENERAL_KNOWLEDGE,
            minScore = 0.0
        )

        assertTrue(results.any { it.entry.id == "c_static" })
        assertTrue("candidate matching 1/4 tokens with no name/alias hit must be gated", results.none { it.entry.id == "c_noise" })
    }

    @Test
    fun singleTokenQuery_isNeverCoverageGated() = runBlocking {
        val candidate = concept("c_leaf", "Leaf Blade", definition = "a flat outgrowth of a leaf")
        val index = KnowledgeIndex(pack, StubSource(listOf(candidate)))

        val results = index.search(nq("leaf"), com.kuzmix.ai.domain.ai.ConversationContext(), Intent.GENERAL_KNOWLEDGE)

        assertTrue(results.any { it.entry.id == "c_leaf" })
    }

    @Test
    fun staticPackAndFtsCandidate_areDedupedToSingleResult() = runBlocking {
        val dup = concept("c_static", "Photosynthesis", "plants convert light into chemical energy")
        val index = KnowledgeIndex(pack, StubSource(listOf(dup)))

        val results = index.search(nq("photosynthesis"), com.kuzmix.ai.domain.ai.ConversationContext(), Intent.GENERAL_KNOWLEDGE)

        assertEquals(1, results.count { it.entry.id == "c_static" })
    }

    @Test
    fun legacyMode_withoutSource_hasNoFtsPrior_equalEntriesTie() = runBlocking {
        val a = concept("c_a", "Twin One", "shares the exact definition words here")
        val b = concept("c_b", "Twin Two", "shares the exact definition words here")
        val legacyPack = pack.copy(concepts = listOf(a, b))
        val index = KnowledgeIndex(legacyPack, source = null)

        val results = index.search(nq("shares definition"), com.kuzmix.ai.domain.ai.ConversationContext(), Intent.GENERAL_KNOWLEDGE)

        assertEquals(2, results.size)
        assertEquals(results[0].score, results[1].score, 1e-9)
    }
}
