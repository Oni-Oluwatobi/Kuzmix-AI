package com.kuzmix.ai.data.knowledge

import com.kuzmix.ai.domain.ai.ConceptRecord
import com.kuzmix.ai.domain.ai.QueryNormalizer

/**
 * Loose entity resolution shared by both concept sources: after exact
 * name/alias lookups fail, match a normalized entity against hydrated
 * candidates by token containment (name first, then aliases), mirroring
 * the original RoomConceptSource behavior.
 */
internal object LooseEntityMatcher {

    fun firstMatch(hydrated: List<ConceptRecord>, entity: String): ConceptRecord? {
        val n = QueryNormalizer.stripPunct(entity)
        if (n.isBlank()) return null
        val entityTokens = QueryNormalizer.contentTokens(n.split(" "))

        hydrated.firstOrNull { QueryNormalizer.stripPunct(it.name) == n }?.let { return it }
        hydrated.firstOrNull { c -> c.aliases.any { QueryNormalizer.stripPunct(it) == n } }?.let { return it }

        if (entityTokens.isNotEmpty()) {
            hydrated.firstOrNull { c ->
                val keyTokens = QueryNormalizer.stripPunct(c.name)
                    .split(" ")
                    .filter { it.isNotBlank() && it !in QueryNormalizer.stopwords }
                keyTokens.isNotEmpty() &&
                    keyTokens.all { it in entityTokens } &&
                    keyTokens.size >= entityTokens.size - 1
            }?.let { return it }
        }

        if (entityTokens.size == 1) {
            val single = entityTokens[0]
            hydrated.firstOrNull { QueryNormalizer.stripPunct(it.name).split(" ").contains(single) }
                ?.let { return it }
            hydrated.firstOrNull { c -> c.aliases.any { QueryNormalizer.stripPunct(it).split(" ").contains(single) } }
                ?.let { return it }
        }
        return null
    }
}
