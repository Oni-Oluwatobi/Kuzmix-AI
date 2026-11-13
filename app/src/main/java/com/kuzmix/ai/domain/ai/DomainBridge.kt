package com.kuzmix.ai.domain.ai

/**
 * Bridges the knowledge-core domain slugs (9 broad faculties such as
 * "sci_math") to the app's 13 subject ids ("subject.physics") so the
 * context boost in KnowledgeIndex.search keeps firing when the concept
 * corpus comes from the shipped SQLite database instead of the JSON pack.
 * Legacy exact matching (activeSubject == domain, "subject.x" == domain)
 * is preserved first, so JSON-pack behavior is byte-identical.
 */
object DomainBridge {

    private val allDomains = setOf(
        "sci_math", "eng_tech", "computing_ai", "business_econ",
        "humanities", "society_culture", "health_life", "creative_arts",
        "practical_life"
    )

    private val subjectDomains: Map<String, Set<String>> = mapOf(
        "biology" to setOf("health_life", "sci_math"),
        "chemistry" to setOf("sci_math"),
        "physics" to setOf("sci_math", "eng_tech"),
        "mathematics" to setOf("sci_math"),
        "computer_science" to setOf("computing_ai", "eng_tech"),
        "economics" to setOf("business_econ"),
        "government" to setOf("society_culture", "humanities"),
        "geography" to setOf("sci_math", "society_culture"),
        "english_language" to setOf("humanities"),
        "literature" to setOf("humanities", "creative_arts"),
        "agricultural_science" to setOf("health_life", "practical_life", "sci_math"),
        "crs" to setOf("humanities", "society_culture"),
        "general_knowledge" to allDomains
    )

    fun matches(activeSubject: String, domain: String): Boolean {
        if (activeSubject == domain) return true
        val key = activeSubject.substringAfter("subject.", "")
        if (key.isNotEmpty() && key == domain) return true
        if (key.isEmpty()) return false
        return subjectDomains[key]?.contains(domain) == true
    }
}
