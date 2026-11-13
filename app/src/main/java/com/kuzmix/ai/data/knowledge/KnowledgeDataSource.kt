package com.kuzmix.ai.data.knowledge

import android.content.Context
import com.kuzmix.ai.domain.ai.ConceptRecord
import com.kuzmix.ai.domain.ai.KnowledgePack
import com.kuzmix.ai.domain.ai.KnowledgePackParser

/**
 * Reads the raw JSON seed files. Since the Room migration (spec v1) this is
 * NOT the runtime source of truth:
 *  - loadShell(): the small always-resident pack (responses + subjects +
 *    formulas) — ~40 KB, loaded on every cold start.
 *  - loadConcepts(): the concept corpus — parsed ONLY during one-time seeding.
 */
class KnowledgeDataSource(private val context: Context) {

    fun loadShell(): KnowledgePack = KnowledgePackParser.merge(
        baseJson = readRaw("knowledge_base") ?: "{}",
        subjectsJson = readRaw("kp_subjects") ?: "{}",
        conceptsJsons = emptyList()
    )

    fun loadConcepts(): List<ConceptRecord> =
        conceptFiles().flatMap { name ->
            readRaw(name)?.let { KnowledgePackParser.parseConcepts(it) }.orEmpty()
        }

    fun loadAll(): KnowledgePack = KnowledgePackParser.merge(
        baseJson = readRaw("knowledge_base") ?: "{}",
        subjectsJson = readRaw("kp_subjects") ?: "{}",
        conceptsJsons = conceptFiles().mapNotNull { name -> readRaw(name) }
    )

    private fun conceptFiles() = listOf(
        "kp_concepts_science",
        "kp_concepts_tech",
        "kp_concepts_humanities"
    )

    private fun readRaw(name: String): String? {
        val id = context.resources.getIdentifier(name, "raw", context.packageName)
        if (id == 0) return null
        return context.resources.openRawResource(id).bufferedReader().use { it.readText() }
    }
}
