package com.kuzmix.ai

import com.kuzmix.ai.data.knowledge.KnowledgeSeeder
import com.kuzmix.ai.data.knowledge.SeedState
import com.kuzmix.ai.data.local.dao.ConceptDao
import com.kuzmix.ai.data.local.entity.ConceptEntity
import com.kuzmix.ai.data.local.entity.ConceptFtsEntity
import com.kuzmix.ai.data.local.entity.toDomain
import com.kuzmix.ai.domain.ai.ConceptRecord
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeSeederTest {

    private class FakeSeedState(var version: Int = 0) : SeedState {
        override suspend fun getVersion(): Int = version
        override suspend fun setVersion(version: Int) { this.version = version }
    }

    private fun concept(id: String, name: String): ConceptRecord = ConceptRecord(
        id = id,
        type = "CONCEPT",
        domain = "biology",
        name = name,
        aliases = listOf("$name alias"),
        definition = "The definition of $name.",
        keywords = listOf(name),
        related = emptyList(),
        examples = listOf("An example of $name, which includes a comma."),
        misconceptions = emptyList(),
        formula = "",
        confidence = "HIGH"
    )

    private val fixture = listOf(
        concept("concept.biology.cell", "Cell"),
        concept("concept.biology.dna", "DNA"),
        concept("concept.physics.force", "Force"),
        ConceptRecord(
            id = "concept.biology.cell_wall",
            type = "CONCEPT",
            domain = "biology",
            name = "Cell Wall",
            aliases = listOf("cell wall"),
            definition = "The outer protective layer of a plant cell.",
            keywords = listOf("cell wall"),
            related = emptyList(),
            examples = emptyList(),
            misconceptions = emptyList(),
            formula = "",
            confidence = "HIGH"
        ),
        ConceptRecord(
            id = "concept.biology.protoplasm",
            type = "CONCEPT",
            domain = "biology",
            name = "Protoplasm",
            aliases = listOf("living matter"),
            definition = "The living content of a cell, bounded by a wall.",
            keywords = emptyList(),
            related = emptyList(),
            examples = emptyList(),
            misconceptions = emptyList(),
            formula = "",
            confidence = "HIGH"
        )
    )

    private fun seeder(dao: ConceptDao, state: SeedState) =
        KnowledgeSeeder(loadConcepts = { fixture }, dao = dao, state = state)

    @Test
    fun seedIfNeeded_importsAllRowsOnce() = runBlocking {
        val dao = FakeConceptDao()
        val state = FakeSeedState()

        seeder(dao, state).seedIfNeeded()

        assertEquals(fixture.size, dao.count())
        assertEquals(fixture.size, dao.fts.size)
        assertEquals(KnowledgeSeeder.SEED_VERSION, state.version)
    }

    @Test
    fun seedIfNeeded_isIdempotent_runningTwiceDoesNotDuplicate() = runBlocking {
        val dao = FakeConceptDao()
        val state = FakeSeedState()

        val seeder = seeder(dao, state)
        seeder.seedIfNeeded()
        seeder.seedIfNeeded() // version already current — must be a no-op

        assertEquals(fixture.size, dao.count())
        assertEquals(fixture.size, dao.fts.size)
    }

    @Test
    fun reseed_replacesRowsWithoutDuplicates() = runBlocking {
        val dao = FakeConceptDao()
        val state = FakeSeedState()

        val seeder = seeder(dao, state)
        seeder.seedIfNeeded()
        seeder.seedConcepts(fixture) // full replace path, as on a SEED_VERSION bump

        assertEquals(fixture.size, dao.count())
        assertEquals(fixture.size, dao.fts.size)
        assertEquals(1, dao.concepts.count { it.id == "concept.biology.cell" })
    }

    @Test
    fun roundTrip_preservesListFieldsWithCommas() = runBlocking {
        val dao = FakeConceptDao()
        seeder(dao, FakeSeedState()).seedIfNeeded()

        val row = dao.concepts.first { it.id == "concept.biology.cell" }
        assertTrue(row.examplesCsv.contains(", which includes a comma."))

        val back = row.toDomain()
        assertEquals(listOf("An example of Cell, which includes a comma."), back.examples)
        assertEquals(listOf("Cell alias"), back.aliases)
    }

    @Test
    fun ftsIndexFindsConcepts_byNameAndAlias() = runBlocking {
        val dao = FakeConceptDao()
        seeder(dao, FakeSeedState()).seedIfNeeded()

        val byName = dao.searchFts("\"dna\"", 50)
        assertEquals(listOf("concept.biology.dna"), byName)

        val byAlias = dao.searchFts("\"force alias\"", 50)
        assertEquals(listOf("concept.physics.force"), byAlias)
    }

    @Test
    fun multiWordAlias_phraseMatchesAdjacency_only() = runBlocking {
        val dao = FakeConceptDao()
        seeder(dao, FakeSeedState()).seedIfNeeded()

        // Quoted phrase = adjacent tokens: matches the "cell wall" alias...
        val phraseHits = dao.searchFts("\"cell wall\"", 50)
        assertTrue(phraseHits.contains("concept.biology.cell_wall"))

        // ...but NOT a row that merely has "cell" and "wall" non-adjacent.
        assertTrue(!phraseHits.contains("concept.biology.protoplasm"))

        // The broader OR tier does match scattered tokens (that is its job —
        // recall; KnowledgeIndex rerank applies the adjacency scoring).
        val broaderHits = dao.searchFts("\"cell\" OR \"wall\"", 50)
        assertTrue(broaderHits.contains("concept.biology.cell_wall"))
        assertTrue(broaderHits.contains("concept.biology.protoplasm"))
    }
}

/**
 * In-memory stand-in for the Room FTS4 table. Quoted multi-word terms are
 * treated as adjacency (substring) matches, mirroring FTS4 phrase queries;
 * OR-separated terms mirror the token-OR tier.
 */
internal class FakeConceptDao : ConceptDao {
    val concepts = mutableListOf<ConceptEntity>()
    val fts = mutableListOf<ConceptFtsEntity>()

    override suspend fun searchFts(match: String, limit: Int): List<String> {
        val terms = match.split(" OR ").map { it.trim().trim('"').lowercase() }
        return fts.filter { row ->
            val haystack = "${row.name} ${row.definition} ${row.aliasesCsv}".lowercase()
            terms.any { haystack.contains(it) }
        }.take(limit).map { it.conceptId }
    }

    override suspend fun getByIds(ids: List<String>): List<ConceptEntity> =
        concepts.filter { it.id in ids }

    override suspend fun getById(id: String): ConceptEntity? = concepts.firstOrNull { it.id == id }

    override suspend fun insertConcepts(concepts: List<ConceptEntity>) {
        this.concepts.addAll(concepts)
    }

    override suspend fun insertFts(rows: List<ConceptFtsEntity>) {
        fts.addAll(rows)
    }

    override suspend fun deleteAllConcepts() { concepts.clear() }
    override suspend fun deleteAllFts() { fts.clear() }
    override suspend fun count(): Int = concepts.size
}
