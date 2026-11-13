package com.kuzmix.ai.data.knowledge

import com.kuzmix.ai.data.local.dao.ConceptDao
import com.kuzmix.ai.data.local.entity.toDomain
import com.kuzmix.ai.domain.ai.ConceptCache
import com.kuzmix.ai.domain.ai.ConceptRecord
import com.kuzmix.ai.domain.ai.ConceptSource
import com.kuzmix.ai.domain.ai.FtsMatcher
import com.kuzmix.ai.domain.ai.NormalizedQuery
import com.kuzmix.ai.domain.ai.QueryNormalizer

/**
 * Room/FTS-backed concept source (spec 3, 4.4). All reads pass through
 * [ConceptCache] and batch their DAO calls (spec 1: fewer, larger reads).
 * [awaitReady] gates queries until the one-time seed has completed so a
 * first-launch query never sees an empty store.
 */
class RoomConceptSource(
    private val dao: ConceptDao,
    private val cache: ConceptCache,
    private val awaitReady: suspend () -> Unit = {}
) : ConceptSource {

    override suspend fun candidates(nq: NormalizedQuery): List<ConceptRecord> {
        awaitReady()
        val ids = candidateIds(QueryNormalizer.contentTokens(nq.tokens), CANDIDATE_LIMIT)
        if (ids.isEmpty()) return emptyList()
        return hydrate(ids)
    }

    override suspend fun lookup(entity: String): ConceptRecord? {
        awaitReady()
        val n = QueryNormalizer.stripPunct(entity)
        if (n.isBlank()) return null
        byId(entity)?.let { return it }

        val entityTokens = QueryNormalizer.contentTokens(n.split(" "))
        val ids = candidateIds(entityTokens.ifEmpty { listOf(n) }, LOOKUP_LIMIT)
        if (ids.isEmpty()) return null
        return LooseEntityMatcher.firstMatch(hydrate(ids), entity)
    }

    override suspend fun byId(id: String): ConceptRecord? {
        awaitReady()
        cache.get(id)?.let { return it }
        val entity = dao.getById(id) ?: return null
        return entity.toDomain().also { cache.put(it.id, it) }
    }

    /**
     * Two-tier candidate ids (see FtsMatcher.phraseMatch): tier 1 guarantees
     * multi-word/phrase matches (aliases like "cell wall") a slot; tier 2
     * fills the remaining limit with broader OR recall. Without tier 1 the
     * OR tier's LIMIT would cut in rowid order and could evict the exact
     * phrase match on a large corpus.
     */
    private suspend fun candidateIds(tokens: List<String>, limit: Int): List<String> {
        if (tokens.isEmpty()) return emptyList()

        val phraseIds = if (tokens.size > 1) {
            FtsMatcher.phraseMatch(tokens.joinToString(" "))
                ?.let { dao.searchFts(it, limit) }
                .orEmpty()
        } else {
            emptyList()
        }

        val remaining = limit - phraseIds.size
        if (remaining <= 0) return phraseIds

        val broader = FtsMatcher.buildMatch(tokens)
            ?.let { dao.searchFts(it, remaining) }
            .orEmpty()

        val seen = HashSet<String>(phraseIds.size + broader.size)
        phraseIds.forEach { seen.add(it) }
        return phraseIds + broader.filter { seen.add(it) }
    }

    /** One batched DAO read for all cache misses (spec 1: batch, don't chatter). */
    private suspend fun hydrate(ids: List<String>): List<ConceptRecord> {
        val missing = ids.filter { !cache.contains(it) }
        if (missing.isNotEmpty()) {
            dao.getByIds(missing).forEach { e -> cache.put(e.id, e.toDomain()) }
        }
        return ids.mapNotNull { cache.get(it) }
    }

    companion object {
        const val CANDIDATE_LIMIT = 50
        const val LOOKUP_LIMIT = 25
    }
}
