package com.kuzmix.ai.data.knowledge

import android.content.Context
import com.kuzmix.ai.R
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

    /**
     * Reads one bundled JSON pack by resource NAME via a static R.raw mapping.
     *
     * getIdentifier() must NOT be used here: the release resource shrinker
     * (shrinkResources) cannot trace reflective lookups and silently strips
     * every JSON pack under res/raw from the APK — which emptied the subject list,
     * the response shell and the Room seed on device (v2.0 field report).
     * A compile-time R.raw reference is always retained by the shrinker.
     * Keep res/raw/keep.xml in sync as a second line of defence.
     */
    private fun readRaw(name: String): String? {
        val id = when (name) {
            "knowledge_base" -> R.raw.knowledge_base
            "kp_subjects" -> R.raw.kp_subjects
            "kp_concepts_science" -> R.raw.kp_concepts_science
            "kp_concepts_tech" -> R.raw.kp_concepts_tech
            "kp_concepts_humanities" -> R.raw.kp_concepts_humanities
            else -> 0
        }
        if (id == 0) return null
        return context.resources.openRawResource(id).bufferedReader().use { it.readText() }
    }
}
