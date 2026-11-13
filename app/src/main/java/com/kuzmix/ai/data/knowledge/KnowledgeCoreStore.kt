package com.kuzmix.ai.data.knowledge

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log
import java.io.File

/**
 * Read-only handle to the shipped kuzmix_knowledge.sqlite corpus
 * (2,577 concepts, FTS5 BM25 index). The DB is copied from assets once
 * (version-gated by UserPreferences) and opened with the Android query
 * contract pragmas (docs/ANDROID_QUERY_CONTRACT.md). All cursors run in
 * `use { }` blocks and every projection is explicit — never SELECT *.
 *
 * Returns null from [open] when the asset is missing, the copy fails, or
 * the device SQLite has no FTS5 — callers then fall back to the existing
 * Room/JSON source.
 */
class KnowledgeCoreStore private constructor(
    private val db: SQLiteDatabase
) {

    /**
     * BM25-ranked concept ids, contract query D (revised).
     *
     * Inner window: top `limit*12` FTS rows by weighted BM25 (title x10,
     * aliases x8, keywords x4 — other columns x1), outer query groups to
     * ONE best row per concept. The old form (ORDER BY bm25 LIMIT limit*2
     * + Kotlin dedup) was starved by row-level LIMIT — concepts with many
     * indexed rows never got a slot and broad queries returned fewer than
     * `limit` unique concepts (probe: "water" -> 39 vs 50).
     * MIN(bm25(...)) directly is rejected by SQLite ("unable to use
     * function bm25 in the requested context"), hence the subquery.
     */
    fun searchIds(match: String, limit: Int): List<String> {
        if (limit <= 0) return emptyList()
        return try {
            val window = (limit * 12).coerceAtMost(MAX_ROW_WINDOW)
            db.rawQuery(
                "SELECT concept_id FROM (" +
                    "SELECT concept_id, bm25(knowledge_fts, 0, 0, 10.0, 1.0, 8.0, 1.0, 1.0, 4.0) AS r " +
                    "FROM knowledge_fts WHERE knowledge_fts MATCH ? ORDER BY r ASC LIMIT ?" +
                    ") GROUP BY concept_id ORDER BY MIN(r) ASC LIMIT ?",
                arrayOf(match, window.toString(), limit.toString())
            ).use { cursor ->
                val out = ArrayList<String>(limit)
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    if (id.isNotBlank()) out.add(id)
                }
                out
            }
        } catch (e: SQLiteException) {
            Log.w(TAG, "fts search failed: ${e.message}")
            emptyList()
        }
    }

    /** Exact name/alias resolution, contract query B. [normalized] = lower(trim(input)). */
    fun lookupExact(normalized: String): String? {
        if (normalized.isBlank()) return null
        return try {
            db.rawQuery(
                "SELECT id FROM concepts WHERE canonical_name_normalized = ? " +
                    "UNION SELECT concept_id FROM aliases WHERE alias_normalized = ? LIMIT 1",
                arrayOf(normalized, normalized)
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: SQLiteException) {
            Log.w(TAG, "exact lookup failed: ${e.message}")
            null
        }
    }

    /** Batched hydration for up to ~50 candidate ids — five grouped reads. */
    fun rowsByIds(ids: List<String>): List<CoreRow> {
        if (ids.isEmpty()) return emptyList()
        val placeholders = ids.joinToString(",") { "?" }
        val args = ids.toTypedArray()

        val base = HashMap<String, BaseCols>()
        db.rawQuery(
            "SELECT c.id, c.name, c.domain_id, c.summary_definition, c.primary_explanation, " +
                "c.key_properties_json, c.applications_json, c.confidence_score, " +
                "d.name, cat.name, t.name " +
                "FROM concepts c " +
                "JOIN domains d ON d.id = c.domain_id " +
                "JOIN categories cat ON cat.id = c.category_id " +
                "LEFT JOIN topics t ON t.id = c.topic_id " +
                "WHERE c.id IN ($placeholders)",
            args
        ).use { cursor ->
            while (cursor.moveToNext()) {
                base[cursor.getString(0)] = BaseCols(
                    name = cursor.getString(1),
                    domainId = cursor.getString(2),
                    summary = cursor.getString(3) ?: "",
                    explanation = cursor.getString(4) ?: "",
                    keyProps = cursor.getString(5),
                    applications = cursor.getString(6),
                    confidence = if (cursor.isNull(7)) 1.0 else cursor.getDouble(7),
                    domainName = cursor.getString(8),
                    categoryName = cursor.getString(9),
                    topicName = cursor.getString(10)
                )
            }
        }
        if (base.isEmpty()) return emptyList()

        val aliases = HashMap<String, MutableList<String>>()
        db.rawQuery(
            "SELECT concept_id, alias FROM aliases WHERE concept_id IN ($placeholders)",
            args
        ).use { cursor ->
            while (cursor.moveToNext()) {
                aliases.getOrPut(cursor.getString(0)) { mutableListOf() }.add(cursor.getString(1))
            }
        }

        val related = HashMap<String, MutableList<String>>()
        db.rawQuery(
            "SELECT source_concept_id, target_concept_id FROM relationships " +
                "WHERE source_concept_id IN ($placeholders) ORDER BY weight DESC",
            args
        ).use { cursor ->
            while (cursor.moveToNext()) {
                related.getOrPut(cursor.getString(0)) { mutableListOf() }.add(cursor.getString(1))
            }
        }

        val misconceptions = HashMap<String, MutableList<Pair<String, String>>>()
        db.rawQuery(
            "SELECT concept_id, misconception_text, correction_text FROM misconceptions " +
                "WHERE concept_id IN ($placeholders)",
            args
        ).use { cursor ->
            while (cursor.moveToNext()) {
                misconceptions.getOrPut(cursor.getString(0)) { mutableListOf() }
                    .add(cursor.getString(1) to (cursor.getString(2) ?: ""))
            }
        }

        val titles = HashMap<String, MutableList<String>>()
        val formulas = HashMap<String, String>()
        val formulasRank = HashMap<String, Int>()
        val rank = mapOf("formula" to 0, "law" to 1, "theorem" to 2)
        db.rawQuery(
            "SELECT concept_id, record_type, title, content FROM knowledge_records " +
                "WHERE concept_id IN ($placeholders)",
            args
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val conceptId = cursor.getString(0)
                val type = cursor.getString(1) ?: ""
                titles.getOrPut(conceptId) { mutableListOf() }.add(cursor.getString(2) ?: "")
                if (type in rank) {
                    val currentRank = formulasRank[conceptId] ?: Int.MAX_VALUE
                    if (rank.getValue(type) < currentRank) {
                        formulas[conceptId] = cursor.getString(3) ?: ""
                        formulasRank[conceptId] = rank.getValue(type)
                    }
                }
            }
        }

        return ids.mapNotNull { id ->
            val cols = base[id] ?: return@mapNotNull null
            CoreRow(
                id = id,
                name = cols.name,
                domainId = cols.domainId,
                summaryDefinition = cols.summary,
                primaryExplanation = cols.explanation,
                keyPropertiesJson = cols.keyProps,
                applicationsJson = cols.applications,
                domainName = cols.domainName,
                categoryName = cols.categoryName,
                topicName = cols.topicName,
                aliases = aliases[id].orEmpty(),
                relatedIds = related[id].orEmpty(),
                misconceptions = misconceptions[id].orEmpty(),
                recordTitles = titles[id].orEmpty(),
                formulaRecordContent = formulas[id],
                confidenceScore = cols.confidence
            )
        }
    }

    fun rowById(id: String): CoreRow? = rowsByIds(listOf(id)).firstOrNull()

    // ---------------------------------------------------------------- corpus
    // Lesson-note / exam-question retrieval over chunks_fts (BM25,
    // header weighted 4x because it carries the section title) and exact
    // KJV verse access. [match] is a ready FTS5 MATCH expression built by
    // the caller (see KnowledgeCoreCorpusSource.matchExpr).

    /** BM25-ranked corpus chunks, optionally restricted to one chunk_type. */
    fun searchChunks(match: String, limit: Int, chunkType: String? = null): List<ChunkResult> {
        if (limit <= 0) return emptyList()
        return try {
            val typeClause = if (chunkType != null) " AND ch.chunk_type = ?" else ""
            val args = if (chunkType != null) arrayOf(match, chunkType, limit.toString())
            else arrayOf(match, limit.toString())
            db.rawQuery(
                "SELECT ch.concept_id, co.name, ch.chunk_type, ch.header_context, " +
                    "ch.text_content, ch.chunk_index " +
                    "FROM chunks_fts f " +
                    "JOIN chunks ch ON ch.rowid = f.rowid " +
                    "JOIN concepts co ON co.id = ch.concept_id " +
                    "WHERE chunks_fts MATCH ?$typeClause " +
                    "ORDER BY bm25(chunks_fts, 1.0, 4.0) LIMIT ?",
                args
            ).use { cursor -> readChunks(cursor) }
        } catch (e: SQLiteException) {
            Log.w(TAG, "chunk search failed: ${e.message}")
            emptyList()
        }
    }

    /** All chunks of one concept in reading order (definition, explanation, notes...). */
    fun chunksForConcept(conceptId: String, limit: Int, chunkType: String? = null): List<ChunkResult> {
        if (conceptId.isBlank() || limit <= 0) return emptyList()
        return try {
            val typeClause = if (chunkType != null) " AND ch.chunk_type = ?" else ""
            val args = if (chunkType != null) arrayOf(conceptId, chunkType, limit.toString())
            else arrayOf(conceptId, limit.toString())
            db.rawQuery(
                "SELECT ch.concept_id, co.name, ch.chunk_type, ch.header_context, " +
                    "ch.text_content, ch.chunk_index " +
                    "FROM chunks ch JOIN concepts co ON co.id = ch.concept_id " +
                    "WHERE ch.concept_id = ?$typeClause " +
                    "ORDER BY ch.chunk_index LIMIT ?",
                args
            ).use { cursor -> readChunks(cursor) }
        } catch (e: SQLiteException) {
            Log.w(TAG, "concept chunks failed: ${e.message}")
            emptyList()
        }
    }

    private fun readChunks(cursor: android.database.Cursor): List<ChunkResult> {
        val out = ArrayList<ChunkResult>()
        while (cursor.moveToNext()) {
            out.add(
                ChunkResult(
                    conceptId = cursor.getString(0) ?: "",
                    conceptName = cursor.getString(1) ?: "",
                    chunkType = cursor.getString(2) ?: "",
                    headerContext = cursor.getString(3) ?: "",
                    textContent = cursor.getString(4) ?: "",
                    chunkIndex = cursor.getInt(5)
                )
            )
        }
        return out
    }

    /** Exact verses for a book/chapter/verse range (from..to inclusive). */
    fun bibleRange(book: String, chapter: Int, fromVerse: Int, toVerse: Int): List<BibleResult> {
        if (book.isBlank() || chapter < 1) return emptyList()
        return try {
            db.rawQuery(
                "SELECT book, chapter, verse, ref, text FROM bible_verses " +
                    "WHERE book = ? COLLATE NOCASE AND chapter = ? " +
                    "AND verse BETWEEN ? AND ? ORDER BY verse",
                arrayOf(book, chapter.toString(), fromVerse.toString(), toVerse.toString())
            ).use { cursor ->
                val out = ArrayList<BibleResult>()
                while (cursor.moveToNext()) {
                    out.add(
                        BibleResult(
                            book = cursor.getString(0) ?: "",
                            chapter = cursor.getInt(1),
                            verse = cursor.getInt(2),
                            ref = cursor.getString(3) ?: "",
                            text = cursor.getString(4) ?: ""
                        )
                    )
                }
                out
            }
        } catch (e: SQLiteException) {
            Log.w(TAG, "bible range failed: ${e.message}")
            emptyList()
        }
    }

    /** BM25 search across the KJV (ref x1, text x5, book x2). */
    fun bibleSearch(match: String, limit: Int): List<BibleResult> {
        if (limit <= 0) return emptyList()
        return try {
            db.rawQuery(
                "SELECT v.book, v.chapter, v.verse, v.ref, v.text " +
                    "FROM bible_fts f JOIN bible_verses v ON v.rowid = f.rowid " +
                    "WHERE bible_fts MATCH ? " +
                    "ORDER BY bm25(bible_fts, 1.0, 5.0, 2.0) LIMIT ?",
                arrayOf(match, limit.toString())
            ).use { cursor ->
                val out = ArrayList<BibleResult>()
                while (cursor.moveToNext()) {
                    out.add(
                        BibleResult(
                            book = cursor.getString(0) ?: "",
                            chapter = cursor.getInt(1),
                            verse = cursor.getInt(2),
                            ref = cursor.getString(3) ?: "",
                            text = cursor.getString(4) ?: ""
                        )
                    )
                }
                out
            }
        } catch (e: SQLiteException) {
            Log.w(TAG, "bible search failed: ${e.message}")
            emptyList()
        }
    }

    /** One lesson/exam chunk as returned by the corpus queries above. */
    data class ChunkResult(
        val conceptId: String,
        val conceptName: String,
        val chunkType: String,
        val headerContext: String,
        val textContent: String,
        val chunkIndex: Int
    )

    /** One KJV verse row. */
    data class BibleResult(
        val book: String,
        val chapter: Int,
        val verse: Int,
        val ref: String,
        val text: String
    )

    fun close() {
        try {
            db.close()
        } catch (_: Exception) {
        }
    }

    private data class BaseCols(
        val name: String,
        val domainId: String,
        val summary: String,
        val explanation: String,
        val keyProps: String?,
        val applications: String?,
        val confidence: Double,
        val domainName: String?,
        val categoryName: String?,
        val topicName: String?
    )

    companion object {
        private const val TAG = "KuzmixKB"

        /** Bump together with the bundled asset to force a re-copy on update. */
        const val DB_VERSION = 8
        const val ASSET_NAME = "kuzmix_knowledge.sqlite"

        /** Upper bound for the BM25 row window inside [searchIds]. */
        private const val MAX_ROW_WINDOW = 600

        /**
         * Copy the asset if needed, open, convert the shipped WAL journal
         * to plain DELETE (a read-only connection cannot create the -shm
         * file a WAL DB needs, so the first open is read-write and then
         * locked down with query_only per contract), apply contract
         * pragmas, and probe FTS5. Returns null (caller falls back to
         * Room/JSON) whenever anything fails.
         */
        fun open(context: Context, forceCopy: Boolean): KnowledgeCoreStore? = try {
            val dest = File(File(context.filesDir, "knowledge"), ASSET_NAME)
            if (forceCopy || !dest.exists()) copyAsset(context, dest)
            val db = SQLiteDatabase.openDatabase(dest.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
            KnowledgePragmas.FIRST_OPEN.forEach { sql -> execPragma(db, sql) }
            if (!supportsFts5(db)) {
                Log.w(TAG, "device SQLite has no FTS5; using JSON/Room fallback")
                db.close()
                null
            } else {
                Log.i(TAG, "knowledge core opened (${dest.length()} bytes)")
                KnowledgeCoreStore(db)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "knowledge core open failed", t)
            null
        }

        /**
         * Runs one PRAGMA the correct way: value-returning PRAGMAs
         * (journal_mode, mmap_size) go through rawQuery() with a closed
         * cursor because execSQL() throws "Queries can be performed using
         * SQLiteDatabase query or rawQuery methods only" for any statement
         * with a result set; genuine setters use execSQL().
         */
        internal fun execPragma(db: SQLiteDatabase, sql: String) {
            if (KnowledgePragmas.returnsRows(sql)) {
                db.rawQuery(sql, null).use { it.moveToFirst() }
            } else {
                db.execSQL(sql)
            }
        }

        private fun copyAsset(context: Context, dest: File) {
            dest.parentFile?.mkdirs()
            val tmp = File(dest.parentFile, "${dest.name}.tmp")
            context.assets.open(ASSET_NAME).use { input ->
                tmp.outputStream().use { output -> input.copyTo(output) }
            }
            if (dest.exists()) dest.delete()
            if (!tmp.renameTo(dest)) throw IllegalStateException("rename of $tmp failed")
        }

        private fun supportsFts5(db: SQLiteDatabase): Boolean = try {
            db.rawQuery(
                "SELECT 1 FROM knowledge_fts WHERE knowledge_fts MATCH 'kuzmix' LIMIT 1",
                null
            ).use { it.moveToFirst() }
            true
        } catch (_: SQLiteException) {
            false
        }
    }
}
