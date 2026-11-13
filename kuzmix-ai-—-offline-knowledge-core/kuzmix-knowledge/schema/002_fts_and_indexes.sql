-- =====================================================================
-- KUZMIX AI OFFLINE KNOWLEDGE DATABASE - FTS5 & PERFORMANCE INDEXES
-- Designed for ultra-low latency reads on low-RAM Android devices
-- =====================================================================

-- B-TREE INDEXES FOR FAST EXACT LOOKUPS AND FOREIGN KEYS
CREATE INDEX IF NOT EXISTS idx_concepts_canonical ON concepts(canonical_name_normalized);
CREATE INDEX IF NOT EXISTS idx_concepts_category ON concepts(category_id);
CREATE INDEX IF NOT EXISTS idx_concepts_domain ON concepts(domain_id);
CREATE INDEX IF NOT EXISTS idx_concepts_topic ON concepts(topic_id);
CREATE INDEX IF NOT EXISTS idx_concepts_difficulty ON concepts(difficulty_level);

CREATE INDEX IF NOT EXISTS idx_records_concept_type ON knowledge_records(concept_id, record_type);
CREATE INDEX IF NOT EXISTS idx_records_type ON knowledge_records(record_type);

CREATE INDEX IF NOT EXISTS idx_chunks_concept_order ON chunks(concept_id, chunk_index);
CREATE INDEX IF NOT EXISTS idx_chunks_type ON chunks(chunk_type);

CREATE INDEX IF NOT EXISTS idx_relationships_source ON relationships(source_concept_id, relationship_type);
CREATE INDEX IF NOT EXISTS idx_relationships_target ON relationships(target_concept_id, relationship_type);

CREATE INDEX IF NOT EXISTS idx_aliases_normalized ON aliases(alias_normalized);
CREATE INDEX IF NOT EXISTS idx_aliases_concept ON aliases(concept_id);

CREATE INDEX IF NOT EXISTS idx_misconceptions_concept ON misconceptions(concept_id);
CREATE INDEX IF NOT EXISTS idx_entities_concept ON entities(concept_id);
CREATE INDEX IF NOT EXISTS idx_entities_type ON entities(entity_type);
CREATE INDEX IF NOT EXISTS idx_entities_normalized ON entities(name_normalized);

CREATE INDEX IF NOT EXISTS idx_disambiguations_term ON disambiguations(term_normalized);
CREATE INDEX IF NOT EXISTS idx_disambiguations_concept ON disambiguations(concept_id);
CREATE INDEX IF NOT EXISTS idx_disambiguations_domain ON disambiguations(domain_id);

CREATE INDEX IF NOT EXISTS idx_comparisons_concept_a ON comparisons(concept_a_id);
CREATE INDEX IF NOT EXISTS idx_comparisons_concept_b ON comparisons(concept_b_id);

CREATE INDEX IF NOT EXISTS idx_procedures_concept ON procedures(concept_id);

CREATE INDEX IF NOT EXISTS idx_embedding_target ON embedding_metadata(target_type, target_id);

-- =====================================================================
-- SQLite FTS5 FULL-TEXT SEARCH VIRTUAL TABLE
-- Indexed columns allow multi-field weighted queries for offline RAG
-- =====================================================================
CREATE VIRTUAL TABLE IF NOT EXISTS knowledge_fts USING fts5(
    concept_id UNINDEXED,
    record_id UNINDEXED,
    title,
    content,
    aliases,
    category_name,
    domain_name,
    keywords,
    tokenize='unicode61 remove_diacritics 2'
);
