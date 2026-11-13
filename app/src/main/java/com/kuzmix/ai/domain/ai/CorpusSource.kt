package com.kuzmix.ai.domain.ai

/**
 * One lesson-note / exam-question chunk from the shipped corpus
 * (the ingested `pdf\` folder: 138 lesson-note PDFs + 132 exam papers).
 * Returned by [CorpusSource]; pure data, no Android types.
 */
data class CorpusChunk(
    val conceptId: String,
    val conceptName: String,
    /** "lesson_notes" | "exam_questions" | "definition" | "explanation" | ... */
    val chunkType: String,
    /** "Domain > Category > Source file > Section" */
    val headerContext: String,
    val text: String,
    val chunkIndex: Int
) {
    /** Short section label for display ("MEANING AND IMPORTANCE OF AGRICULTURE"). */
    val sectionTitle: String
        get() = headerContext.substringAfterLast('>').trim().ifBlank { conceptName }
}

/** One verse from the bundled KJV (31,102 verses, 66 books). */
data class BiblePassage(
    val book: String,
    val chapter: Int,
    val verse: Int,
    val ref: String,
    val text: String
)

/**
 * Read-only gateway to the corpus inside the shipped knowledge DB
 * (chunks_fts + bible_fts). Implemented over KnowledgeCoreStore; the
 * engine only ever sees this interface, so every method must degrade to
 * emptyList() when the DB could not be opened — never throw.
 */
interface CorpusSource {

    /** BM25 search over lesson notes / exam questions / all chunks. */
    suspend fun search(terms: List<String>, limit: Int, chunkType: String? = null): List<CorpusChunk>

    /** Ordered chunks (chunk_index ASC) for one concept — used for stepwise teaching. */
    suspend fun forConcept(conceptId: String, limit: Int, chunkType: String? = null): List<CorpusChunk>

    /** Exact verse range; fromVerse..toVerse (use 1..176 for a chapter). */
    suspend fun bibleRange(book: String, chapter: Int, fromVerse: Int, toVerse: Int): List<BiblePassage>

    /** BM25 keyword search across the KJV. */
    suspend fun bibleSearch(terms: List<String>, limit: Int): List<BiblePassage>
}
