package com.kuzmix.ai

import com.kuzmix.ai.data.knowledge.KnowledgePragmas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression guard for the on-device open failure:
 * "Queries can be performed using SQLiteDatabase query or rawQuery
 * methods only" — thrown when a value-returning PRAGMA (mmap_size) was
 * passed to SQLiteDatabase.execSQL(). The store executes every
 * FIRST_OPEN statement through a classifier; these tests pin the
 * classification so a future PRAGMA can never reach execSQL() again.
 */
class KnowledgePragmasTest {

    @Test
    fun mmapSizeReturnsRowsAndMustNotUseExecSql() {
        assertTrue(KnowledgePragmas.returnsRows("PRAGMA mmap_size=268435456"))
    }

    @Test
    fun journalModeReturnsRowsAndMustNotUseExecSql() {
        assertTrue(KnowledgePragmas.returnsRows("PRAGMA journal_mode=DELETE"))
        assertTrue(KnowledgePragmas.returnsRows("PRAGMA journal_mode=WAL"))
        assertTrue(KnowledgePragmas.returnsRows("  pragma journal_mode = MEMORY  "))
    }

    @Test
    fun pureSettersAreExecSqlSafe() {
        assertFalse(KnowledgePragmas.returnsRows("PRAGMA query_only=ON"))
        assertFalse(KnowledgePragmas.returnsRows("PRAGMA temp_store=MEMORY"))
        assertFalse(KnowledgePragmas.returnsRows("PRAGMA foreign_keys=ON"))
        assertFalse(KnowledgePragmas.returnsRows("PRAGMA synchronous=OFF"))
    }

    @Test
    fun nonPragmaStatementsNeverClassifiedAsPragmas() {
        assertFalse(KnowledgePragmas.returnsRows("SELECT 1 FROM knowledge_fts LIMIT 1"))
        assertFalse(KnowledgePragmas.returnsRows("CREATE TABLE x (a)"))
    }

    @Test
    fun pragmaNameParsesVariants() {
        assertEquals("mmap_size", KnowledgePragmas.pragmaName("PRAGMA mmap_size=268435456"))
        assertEquals("journal_mode", KnowledgePragmas.pragmaName("PRAGMA journal_mode=DELETE"))
        assertEquals("query_only", KnowledgePragmas.pragmaName("pragma query_only ON"))
    }

    @Test
    fun firstOpenSequenceKeepsWalConversionAndContractPragmas() {
        assertEquals(
            listOf(
                "PRAGMA journal_mode=DELETE",
                "PRAGMA query_only=ON",
                "PRAGMA temp_store=MEMORY",
                "PRAGMA mmap_size=268435456"
            ),
            KnowledgePragmas.FIRST_OPEN
        )
    }

    @Test
    fun everyFirstOpenStatementIsClassifiedCorrectly() {
        KnowledgePragmas.FIRST_OPEN.forEach { sql ->
            assertTrue("'$sql' returns rows but would be run through execSQL()", KnowledgePragmas.returnsRows(sql) || isPureSetter(sql))
        }
        // The two that historically failed on-device must be query-classified.
        assertTrue(KnowledgePragmas.returnsRows("PRAGMA journal_mode=DELETE"))
        assertTrue(KnowledgePragmas.returnsRows("PRAGMA mmap_size=268435456"))
    }

    private fun isPureSetter(sql: String): Boolean =
        KnowledgePragmas.pragmaName(sql) in setOf("query_only", "temp_store")
}
