package com.kuzmix.ai

import com.kuzmix.ai.data.knowledge.KnowledgeSeeder
import com.kuzmix.ai.data.knowledge.RoomConceptSource
import com.kuzmix.ai.data.knowledge.SeedState
import com.kuzmix.ai.domain.ai.ConceptCache
import com.kuzmix.ai.domain.ai.ConceptRecord
import com.kuzmix.ai.domain.ai.QueryNormalizer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Candidate-gathering behavior of the Room/FTS store (spec 4.4 + the
 * multi-word alias concern): the phrase tier must keep an exact multi-word
 * alias match inside the ~50-candidate window even when the OR tier alone
 * would fill LIMIT with earlier-inserted rows sharing a common token.
 */
class RoomConceptSourceTest {

    private fun record(id: String, name: String, aliases: List<String>, definition: String) =
        ConceptRecord(
            id = id,
            type = "CONCEPT",
            domain = "biology",
            name = name,
            aliases = aliases,
            definition = definition,
            keywords = emptyList(),
            related = emptyList(),
            examples = emptyList(),
            misconceptions = emptyList(),
            formula = "",
            confidence = "HIGH"
        )

    private suspend fun seed(dao: FakeConceptDao) {
        // 60 fillers sharing the token "cell", inserted FIRST (lowest rowid —
        // a plain OR + LIMIT 50 would return these and evict the target).
        val fillers = (1..60).map { i ->
            record("concept.filler.$i", "Filler $i", emptyList(), "A cell structure number $i.")
        }
        // The target: multi-word alias, inserted LAST.
        val target = record(
            "concept.biology.cell_wall",
            "Cell Wall",
            listOf("cell wall"),
            "The outer protective layer of a plant cell."
        )
        val state = object : SeedState {
            override suspend fun getVersion(): Int = 0
            override suspend fun setVersion(version: Int) {}
        }
        KnowledgeSeeder(loadConcepts = { fillers + target }, dao = dao, state = state).seedIfNeeded()
    }

    @Test
    fun multiWordAliasSurvivesLimit_phraseTierFirst() = runBlocking {
        val dao = FakeConceptDao()
        seed(dao)
        val source = RoomConceptSource(dao, ConceptCache(30))

        val ids = source.candidates(QueryNormalizer.normalize("what is cell wall")).map { it.id }

        assertTrue(
            "exact phrase match must survive the 50-candidate window despite 60 earlier fillers",
            ids.contains("concept.biology.cell_wall")
        )
        assertTrue("candidates stay bounded (spec 7)", ids.size <= 50)
    }

    @Test
    fun singleTokenQuery_stillWorks() = runBlocking {
        val dao = FakeConceptDao()
        seed(dao)
        val source = RoomConceptSource(dao, ConceptCache(30))

        val ids = source.candidates(QueryNormalizer.normalize("cell")).map { it.id }

        assertTrue(ids.isNotEmpty())
        assertTrue(ids.size <= 50)
    }

    @Test
    fun stopwordOnlyQuery_returnsEmptyWithoutTouchingFts() = runBlocking {
        val dao = FakeConceptDao()
        seed(dao)
        val source = RoomConceptSource(dao, ConceptCache(30))

        val ids = source.candidates(QueryNormalizer.normalize("what is the"))

        assertTrue(ids.isEmpty())
    }

    @Test
    fun lookup_resolvesExactMultiWordAlias() = runBlocking {
        val dao = FakeConceptDao()
        seed(dao)
        val source = RoomConceptSource(dao, ConceptCache(30))

        val found = source.lookup("cell wall")

        assertEquals("concept.biology.cell_wall", found?.id)
    }

    @Test
    fun candidates_goThroughCache_secondQueryDoesNotRefetch() = runBlocking {
        val dao = FakeConceptDao()
        seed(dao)
        val cache = ConceptCache(60)
        val source = RoomConceptSource(dao, cache)

        val nq = QueryNormalizer.normalize("what is cell wall")
        val first = source.candidates(nq)
        val cachedBefore = cache.size
        assertTrue(cachedBefore > 0)

        source.candidates(nq)
        assertEquals("no re-hydration of cached rows", cachedBefore, cache.size)
        assertTrue(first.isNotEmpty())
    }
}
