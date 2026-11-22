package com.kuzmix.ai.data.knowledge

import com.kuzmix.ai.data.local.dao.ConceptDao
import com.kuzmix.ai.data.local.entity.toEntity
import com.kuzmix.ai.data.local.entity.toFts
import com.kuzmix.ai.domain.ai.ConceptRecord

/** Minimal persistence for the seed version — fakeable in unit tests (spec 5.8). */
interface SeedState {
    suspend fun getVersion(): Int
    suspend fun setVersion(version: Int)
}

/**
 * One-time JSON -> Room import (spec 4.3). Reuses the existing parser via
 * [loadConcepts] (KnowledgeDataSource in production, a fixture in tests).
 * Re-seeding deletes old rows first, so running it twice (or after a
 * SEED_VERSION bump) never duplicates rows.
 */
class KnowledgeSeeder(
    private val loadConcepts: () -> List<ConceptRecord>,
    private val dao: ConceptDao,
    private val state: SeedState
) {
    companion object {
        /**
         * v1 -> v2: the v2.0 release APK shipped with the res/raw JSON packs stripped
         * by the resource shrinker, so devices seeded Room with ZERO concepts
         * and recorded version 1. Bumping forces a full re-seed now that the
         * packs are statically referenced and survive shrinking.
         *
         * v2 -> v3: pick up the new Thermodynamics concept AND repair devices
         * whose older seed stored mangled example arrows (" → " stored as
         * " ’n ") — a full delete + re-import from the clean packs.
         */
        const val SEED_VERSION = 3
        private const val INSERT_CHUNK = 250
    }

    suspend fun seedIfNeeded() {
        if (state.getVersion() >= SEED_VERSION) return
        seedConcepts(loadConcepts())
        state.setVersion(SEED_VERSION)
    }

    /** Full replace-import. Idempotent: clears both tables before inserting. */
    suspend fun seedConcepts(concepts: List<ConceptRecord>) {
        dao.deleteAllFts()
        dao.deleteAllConcepts()
        concepts.chunked(INSERT_CHUNK).forEach { chunk ->
            dao.insertConcepts(chunk.map { it.toEntity() })
            dao.insertFts(chunk.map { it.toFts() })
        }
    }
}
