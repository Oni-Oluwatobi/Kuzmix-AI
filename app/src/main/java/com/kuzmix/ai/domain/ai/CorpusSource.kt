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
 * One subject available to a class (and term) in the scheme of work —
 * backed by the ingested lesson notes (and exam papers), so a quiz can
 * only ever offer subjects the student actually reads for that class.
 *
 * [conceptId] is the curriculum concept ("c_cur_ss1_chemistry_t2") used to
 * load its weekly lesson topics; blank when the subject only exists as an
 * exam paper (no lesson notes for that class/term).
 */
data class SchemeSubject(
    val subject: String,
    val conceptId: String
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

    /**
     * Subjects offered for a class + term in the scheme of work (lesson
     * notes first, exam-only subjects appended). [cls] is the lowercase
     * class key ("jss1".."ss3"), [term] 1..3 or null for any term. Empty
     * when the DB is unavailable — callers then fall back to the JSON
     * subject pack.
     */
    suspend fun schemeSubjects(cls: String, term: Int?): List<SchemeSubject> = emptyList()

    /**
     * Weekly lesson topics ("WEEK 1: Photosynthesis" -> "Photosynthesis")
     * for one curriculum concept, in scheme-of-work order.
     */
    suspend fun lessonTopics(conceptId: String, limit: Int = 40): List<String> = emptyList()

    /**
     * Exam-question chunks for one class (+ term) + subject, BM25-ranked by
     * [terms] (the chosen topic's words; empty = paper order). [cls] is the
     * lowercase class key ("jss1".."ss3"), [term] 1..3 or null for any
     * term, [subject] null for any subject of the class. Callers widen the
     * filters (drop topic, then term, then subject) when a scoped search
     * comes back empty.
     */
    suspend fun examChunksFor(
        cls: String,
        term: Int?,
        subject: String?,
        terms: List<String>,
        limit: Int
    ): List<CorpusChunk> = emptyList()
}
