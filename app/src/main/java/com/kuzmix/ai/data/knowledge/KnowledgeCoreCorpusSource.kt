package com.kuzmix.ai.data.knowledge

import android.util.Log
import com.kuzmix.ai.domain.ai.CorpusChunk
import com.kuzmix.ai.domain.ai.CorpusSource
import com.kuzmix.ai.domain.ai.BiblePassage
import com.kuzmix.ai.domain.ai.FtsMatcher
import com.kuzmix.ai.domain.ai.QueryNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * CorpusSource over the shipped kuzmix_knowledge.sqlite.
 *
 * [store] is the same memoized suspend open used by KnowledgeCoreConceptSource:
 * it resolves once the asset copy/verify finished and returns null when the
 * core is unavailable — every method then returns emptyList() so the engine
 * degrades to its existing behaviour instead of crashing.
 *
 * MATCH expressions are built with FtsMatcher (every term double-quoted,
 * ORed) after dropping stopwords plus corpus-container words ("bible",
 * "verse"...) so a search for "bible verses about love" ranks on "love".
 * All queries run on Dispatchers.IO — the engine itself stays on Default.
 */
class KnowledgeCoreCorpusSource(
    private val store: suspend () -> KnowledgeCoreStore?
) : CorpusSource {

    override suspend fun search(
        terms: List<String>,
        limit: Int,
        chunkType: String?
    ): List<CorpusChunk> {
        val match = matchExpr(terms) ?: return emptyList()
        val s = store() ?: return emptyList()
        return withContext(Dispatchers.IO) {
            s.searchChunks(match, limit, chunkType)
                .map { it.toCorpusChunk() }
                .also { Log.i(TAG, "searchChunks type=$chunkType terms='${terms.joinToString(" ")}' -> ${it.size}") }
        }
    }

    override suspend fun forConcept(
        conceptId: String,
        limit: Int,
        chunkType: String?
    ): List<CorpusChunk> {
        val s = store() ?: return emptyList()
        return withContext(Dispatchers.IO) {
            s.chunksForConcept(conceptId, limit, chunkType).map { it.toCorpusChunk() }
        }
    }

    override suspend fun bibleRange(
        book: String,
        chapter: Int,
        fromVerse: Int,
        toVerse: Int
    ): List<BiblePassage> {
        val s = store() ?: return emptyList()
        return withContext(Dispatchers.IO) {
            s.bibleRange(book, chapter, fromVerse, toVerse).map { it.toPassage() }
        }
    }

    override suspend fun bibleSearch(terms: List<String>, limit: Int): List<BiblePassage> {
        // container words say WHAT kind of search it is, not WHAT about
        val content = terms.filter { it.lowercase() !in BIBLE_CONTAINER_WORDS }
        val match = matchExpr(content) ?: return emptyList()
        val s = store() ?: return emptyList()
        return withContext(Dispatchers.IO) {
            s.bibleSearch(match, limit).map { it.toPassage() }
        }
    }

    private fun matchExpr(terms: List<String>): String? {
        val clean = terms
            .map { QueryNormalizer.stripPunct(it).lowercase() }
            .filter { it.length > 1 && it !in QueryNormalizer.stopwords }
        return FtsMatcher.buildMatch(clean)
    }

    private fun KnowledgeCoreStore.ChunkResult.toCorpusChunk() = CorpusChunk(
        conceptId = conceptId,
        conceptName = conceptName,
        chunkType = chunkType,
        headerContext = headerContext,
        text = textContent,
        chunkIndex = chunkIndex
    )

    private fun KnowledgeCoreStore.BibleResult.toPassage() = BiblePassage(
        book = book,
        chapter = chapter,
        verse = verse,
        ref = ref,
        text = text
    )

    companion object {
        private const val TAG = "KuzmixKB"

        /** Query-framing words that carry no topical signal for the KJV index. */
        private val BIBLE_CONTAINER_WORDS = setOf(
            "bible", "verse", "verses", "kjv", "scripture", "scriptures",
            "testament", "chapter", "bible verse", "king james", "version"
        )
    }
}
