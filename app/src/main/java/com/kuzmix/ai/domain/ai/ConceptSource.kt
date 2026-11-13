package com.kuzmix.ai.domain.ai

/**
 * Seam between KnowledgeIndex and where concept data comes from (spec 5.
 * interface-strategy note): tests construct KnowledgeIndex without a source
 * and keep the legacy full-scan behavior; production passes a Room-backed
 * source that fetches only FTS candidates (bounded, spec 4.4).
 */
interface ConceptSource {

    /** Candidate concepts for one query — at most ~50 for the Room impl (spec 4.4). */
    suspend fun candidates(nq: NormalizedQuery): List<ConceptRecord>

    /** Resolve an entity string (normalized name or alias) to a concept. */
    suspend fun lookup(entity: String): ConceptRecord?

    /** Fetch a single concept by its id, via cache first. */
    suspend fun byId(id: String): ConceptRecord?
}
