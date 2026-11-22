package com.kuzmix.ai.data.knowledge

import android.util.Log
import com.kuzmix.ai.domain.ai.ConceptCache
import com.kuzmix.ai.domain.ai.ConceptRecord
import com.kuzmix.ai.domain.ai.ConceptSource
import com.kuzmix.ai.domain.ai.FtsMatcher
import com.kuzmix.ai.domain.ai.NormalizedQuery
import com.kuzmix.ai.domain.ai.QueryNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Production ConceptSource backed by the shipped kuzmix_knowledge.sqlite
 * (FTS5 + BM25, contract queries). [store] is a memoized, suspend open —
 * it resolves once the asset has been copied/verified, and returns null
 * when the core is unavailable, in which case every call delegates to the
 * existing Room/JSON [fallback] (unchanged legacy architecture).
 * Candidate gathering mirrors RoomConceptSource: phrase tier first, OR
 * fill second, then the shared cache hydrates only misses.
 *
 * Every path logs under the KuzmixKB tag so on-device validation can
 * prove whether a query was served by the SQLite core or the fallback.
 */
class KnowledgeCoreConceptSource(
    private val store: suspend () -> KnowledgeCoreStore?,
    private val cache: ConceptCache,
    private val fallback: ConceptSource
) : ConceptSource {

    override suspend fun candidates(nq: NormalizedQuery): List<ConceptRecord> {
        val s = store()
        if (s == null) {
            Log.w(TAG, "candidates: knowledge core unavailable -> JSON/Room fallback")
            return fallback.candidates(nq)
        }
        val tokens = QueryNormalizer.contentTokens(nq.tokens)
        if (tokens.isEmpty()) return emptyList()
        return withContext(Dispatchers.IO) {
            val ids = candidateIds(s, QueryNormalizer.phraseText(nq), tokens, CANDIDATE_LIMIT)
            Log.i(TAG, "FTS5 candidates=${ids.size} query='${tokens.joinToString(" ")}'")
            if (ids.isEmpty()) emptyList() else hydrate(s, ids)
        }
    }

    override suspend fun lookup(entity: String): ConceptRecord? {
        val s = store()
        if (s == null) {
            Log.w(TAG, "lookup: knowledge core unavailable -> JSON/Room fallback")
            return fallback.lookup(entity)
        }
        val n = QueryNormalizer.stripPunct(entity)
        if (n.isBlank()) return null
        cache.get(entity)?.let { return it }

        return withContext(Dispatchers.IO) {
            val exactId = s.lookupExact(entity.trim().lowercase())
                ?: s.lookupExact(n)
            val exact = exactId?.let { id -> hydrate(s, listOf(id)).firstOrNull() }
            if (exact != null) {
                Log.i(TAG, "exact lookup hit id=${exact.id} for '$entity'")
                exact
            } else {
                // JSON/Room concepts (Debugging, Thermodynamics, ...) resolve
                // BEFORE the sqlite fuzzy pass: LooseEntityMatcher only accepts
                // near-exact names/aliases, so a hit here is a true name match —
                // while the fuzzy pass would hand back a thematically-close PDF
                // chunk concept instead of the concept actually asked for.
                val fromJson = fallback.lookup(entity)
                if (fromJson != null) {
                    Log.i(TAG, "JSON/Room lookup hit id=${fromJson.id} for '$entity'")
                    fromJson
                } else {
                    val entityTokens = QueryNormalizer.contentTokens(n.split(" "))
                    val ids = candidateIds(s, n, entityTokens.ifEmpty { listOf(n) }, LOOKUP_LIMIT)
                    Log.i(TAG, "lookup fuzzy candidates=${ids.size} for '$entity'")
                    if (ids.isEmpty()) null else LooseEntityMatcher.firstMatch(hydrate(s, ids), entity)
                }
            }
        }
    }

    override suspend fun byId(id: String): ConceptRecord? {
        cache.get(id)?.let { return it }
        val s = store()
        if (s == null) {
            Log.w(TAG, "byId: knowledge core unavailable -> JSON/Room fallback")
            return fallback.byId(id)
        }
        return withContext(Dispatchers.IO) {
            s.rowById(id)?.let { row ->
                KnowledgeCoreMapper.toRecord(row).also { cache.put(it.id, it) }
            }
        }
    }

    private fun candidateIds(
        s: KnowledgeCoreStore,
        phraseText: String,
        tokens: List<String>,
        limit: Int
    ): List<String> {
        if (tokens.isEmpty()) return emptyList()

        // Tier 1: raw adjacency phrase WITH stopwords kept
        // ("laws of motion" — the stopword-stripped form matched 0 rows).
        val found = LinkedHashSet<String>()
        FtsMatcher.phraseMatch(phraseText)?.let { found.addAll(s.searchIds(it, limit)) }

        // Tier 2: OR of content tokens (recall workhorse).
        if (found.size < limit) {
            FtsMatcher.buildMatch(tokens)?.let { found.addAll(s.searchIds(it, limit)) }
        }

        // Tier 3 (only when still starved): singular/plural variants.
        if (found.size < limit / 2) {
            val variants = tokens.flatMap { FtsMatcher.inflectionVariants(it) }.distinct()
            FtsMatcher.buildMatch(variants)?.let { found.addAll(s.searchIds(it, limit)) }
        }

        // Tier 4 (still starved): prefix OR — abbreviations/typed prefixes.
        if (found.size < limit / 2) {
            FtsMatcher.prefixMatch(tokens)?.let { found.addAll(s.searchIds(it, limit)) }
        }

        return found.toList().take(limit)
    }

    private fun hydrate(s: KnowledgeCoreStore, ids: List<String>): List<ConceptRecord> {
        val missing = ids.filter { !cache.contains(it) }
        if (missing.isNotEmpty()) {
            s.rowsByIds(missing).forEach { row ->
                val record = KnowledgeCoreMapper.toRecord(row)
                cache.put(record.id, record)
            }
        }
        return ids.mapNotNull { cache.get(it) }
    }

    companion object {
        private const val TAG = "KuzmixKB"
        const val CANDIDATE_LIMIT = 50
        const val LOOKUP_LIMIT = 25
    }
}
