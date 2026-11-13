package com.kuzmix.ai.data.knowledge

/**
 * Single source of truth for the PRAGMA statements used when opening the
 * embedded knowledge database.
 *
 * Android's SQLiteDatabase.execSQL() rejects any statement that produces
 * a result set with the error "Queries can be performed using SQLiteDatabase
 * query or rawQuery methods only." Several PRAGMAs that LOOK like setters
 * still return a value row (journal_mode, mmap_size), so they must be run
 * through rawQuery() with a closed cursor. Genuine setters (query_only,
 * temp_store) go through execSQL().
 *
 * Keep this object free of Android imports so the classification is
 * unit-testable on the JVM.
 */
object KnowledgePragmas {

    /** PRAGMAs that return a result row even when assigned a value. */
    private val RETURNS_ROWS = setOf(
        "journal_mode",
        "mmap_size"
    )

    /**
     * First-open sequence for the shipped WAL database: convert to plain
     * DELETE journal (a read-only connection cannot create the -shm file a
     * WAL DB needs), then apply the Android query-contract pragmas.
     * Order matters: journal conversion first, settings after.
     */
    val FIRST_OPEN: List<String> = listOf(
        "PRAGMA journal_mode=DELETE",
        "PRAGMA query_only=ON",
        "PRAGMA temp_store=MEMORY",
        "PRAGMA mmap_size=268435456"
    )

    /** The pragma name, lowercased ("PRAGMA mmap_size=268435456" → "mmap_size"). */
    fun pragmaName(sql: String): String =
        sql.trim()
            .lowercase()
            .removePrefix("pragma")
            .trim()
            .substringBefore("=")
            .substringBefore(" ")
            .trim()

    /**
     * True when [sql] is a PRAGMA that returns a value row and therefore
     * must NOT be executed through SQLiteDatabase.execSQL().
     */
    fun returnsRows(sql: String): Boolean =
        sql.trim().lowercase().startsWith("pragma") && pragmaName(sql) in RETURNS_ROWS
}
