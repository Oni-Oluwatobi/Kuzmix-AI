-- =====================================================================
-- KUZMIX AI OFFLINE KNOWLEDGE DATABASE - CORE DDL SCHEMA
-- Target Database: SQLite 3.35+ (Android Room & SQLite Native compatible)
-- =====================================================================

PRAGMA foreign_keys = ON;
PRAGMA page_size = 4096;

-- 1. METADATA & SCHEMA VERSIONING
CREATE TABLE IF NOT EXISTS metadata (
    key TEXT PRIMARY KEY NOT NULL,
    value TEXT NOT NULL,
    updated_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now'))
);

-- 2. DOMAINS (High-level knowledge faculties)
CREATE TABLE IF NOT EXISTS domains (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    slug TEXT UNIQUE NOT NULL,
    description TEXT,
    icon TEXT,
    sort_order INTEGER DEFAULT 0
);

-- 3. CATEGORIES (Subject areas within domains)
CREATE TABLE IF NOT EXISTS categories (
    id TEXT PRIMARY KEY NOT NULL,
    domain_id TEXT NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    slug TEXT NOT NULL,
    description TEXT,
    sort_order INTEGER DEFAULT 0
);

-- 4. TOPICS (Specific thematic clusters within categories)
CREATE TABLE IF NOT EXISTS topics (
    id TEXT PRIMARY KEY NOT NULL,
    category_id TEXT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    slug TEXT NOT NULL,
    description TEXT
);

-- 5. SOURCES & PROVENANCE (Honest, verified attribution)
CREATE TABLE IF NOT EXISTS sources (
    id TEXT PRIMARY KEY NOT NULL,
    title TEXT NOT NULL,
    author_or_org TEXT,
    source_type TEXT CHECK (source_type IN (
        'academic_textbook', 'scientific_standard', 'encyclopedia',
        'open_educational_resource', 'peer_reviewed_paper',
        'institutional_guideline', 'curated_corpus'
    )) NOT NULL,
    uri_or_reference TEXT,
    license TEXT,
    notes TEXT
);

-- 6. CONCEPTS (Core unit of intellectual knowledge)
CREATE TABLE IF NOT EXISTS concepts (
    id TEXT PRIMARY KEY NOT NULL,
    topic_id TEXT REFERENCES topics(id) ON DELETE SET NULL,
    category_id TEXT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    domain_id TEXT NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    canonical_name_normalized TEXT NOT NULL,
    summary_definition TEXT NOT NULL,
    primary_explanation TEXT NOT NULL,
    key_properties_json TEXT, -- JSON Array of properties or key/value pairs
    applications_json TEXT,   -- JSON Array of practical applications
    prerequisites_json TEXT,  -- JSON Array of prerequisite concept identifiers or names
    difficulty_level TEXT CHECK (difficulty_level IN ('introductory', 'intermediate', 'advanced', 'expert')) DEFAULT 'introductory',
    confidence_score REAL DEFAULT 1.0 CHECK (confidence_score >= 0.0 AND confidence_score <= 1.0),
    provenance_type TEXT CHECK (provenance_type IN (
        'verified_fact', 'curated_scientific', 'standard_definition',
        'synthesized_pedagogical', 'historical_record', 'practical_guideline'
    )) DEFAULT 'curated_scientific',
    source_id TEXT REFERENCES sources(id) ON DELETE SET NULL,
    content_hash TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')),
    updated_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now'))
);

-- 7. KNOWLEDGE RECORDS (Fine-grained structured knowledge components)
CREATE TABLE IF NOT EXISTS knowledge_records (
    id TEXT PRIMARY KEY NOT NULL,
    concept_id TEXT REFERENCES concepts(id) ON DELETE CASCADE,
    record_type TEXT NOT NULL CHECK (record_type IN (
        'fact', 'concept', 'definition', 'explanation', 'detailed_explanation',
        'example', 'non_example', 'procedure', 'process', 'principle', 'rule',
        'theorem', 'formula', 'law', 'classification', 'comparison', 'cause_effect',
        'timeline', 'historical_event', 'person', 'place', 'organization',
        'technology', 'invention', 'terminology', 'synonym', 'alias',
        'prerequisite', 'application', 'limitation', 'misconception',
        'correction', 'relationship', 'FAQ', 'practical_knowledge'
    )),
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    structured_payload_json TEXT,
    confidence_score REAL DEFAULT 1.0 CHECK (confidence_score >= 0.0 AND confidence_score <= 1.0),
    provenance_type TEXT DEFAULT 'curated_scientific',
    source_id TEXT REFERENCES sources(id) ON DELETE SET NULL,
    content_hash TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now'))
);

-- 8. CHUNKS (Optimized semantic segments for mobile RAG retrieval)
CREATE TABLE IF NOT EXISTS chunks (
    id TEXT PRIMARY KEY NOT NULL,
    concept_id TEXT NOT NULL REFERENCES concepts(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    chunk_type TEXT NOT NULL,
    text_content TEXT NOT NULL,
    token_estimate INTEGER NOT NULL,
    header_context TEXT,
    embedding_id TEXT, -- Reserved for optional vector extension
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')),
    UNIQUE(concept_id, chunk_index)
);

-- 9. ENTITIES (Real-world and academic entities)
CREATE TABLE IF NOT EXISTS entities (
    id TEXT PRIMARY KEY NOT NULL,
    concept_id TEXT REFERENCES concepts(id) ON DELETE SET NULL,
    entity_type TEXT NOT NULL CHECK (entity_type IN (
        'person', 'place', 'organization', 'technology',
        'invention', 'event', 'law_or_principle', 'standard'
    )),
    name TEXT NOT NULL,
    name_normalized TEXT NOT NULL,
    description TEXT NOT NULL,
    attributes_json TEXT
);

-- 10. RELATIONSHIPS (Knowledge graph directional edges)
CREATE TABLE IF NOT EXISTS relationships (
    id TEXT PRIMARY KEY NOT NULL,
    source_concept_id TEXT NOT NULL REFERENCES concepts(id) ON DELETE CASCADE,
    target_concept_id TEXT NOT NULL REFERENCES concepts(id) ON DELETE CASCADE,
    relationship_type TEXT NOT NULL CHECK (relationship_type IN (
        'related_to', 'prerequisite_of', 'depends_on', 'part_of', 'contains',
        'subtype_of', 'broader_than', 'narrower_than', 'example_of', 'used_for',
        'causes', 'caused_by', 'influenced_by', 'contrasts_with', 'compared_with',
        'synonym_of', 'alias_of', 'analogous_to', 'application_of', 'derived_from',
        'governed_by', 'associated_with'
    )),
    description TEXT,
    weight REAL DEFAULT 1.0,
    is_bidirectional INTEGER DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')),
    UNIQUE(source_concept_id, target_concept_id, relationship_type)
);

-- 11. ALIASES (Synonyms, acronyms, and search terms)
CREATE TABLE IF NOT EXISTS aliases (
    id TEXT PRIMARY KEY NOT NULL,
    concept_id TEXT NOT NULL REFERENCES concepts(id) ON DELETE CASCADE,
    alias TEXT NOT NULL,
    alias_normalized TEXT NOT NULL,
    alias_type TEXT CHECK (alias_type IN ('synonym', 'acronym', 'spelling_variant', 'colloquial', 'scientific_name', 'translation', 'formula_repr', 'symbol')) DEFAULT 'synonym',
    UNIQUE(concept_id, alias_normalized)
);

-- 12. MISCONCEPTIONS (Common false beliefs and pedagogical corrections)
CREATE TABLE IF NOT EXISTS misconceptions (
    id TEXT PRIMARY KEY NOT NULL,
    concept_id TEXT NOT NULL REFERENCES concepts(id) ON DELETE CASCADE,
    misconception_text TEXT NOT NULL,
    correction_text TEXT NOT NULL,
    explanation TEXT NOT NULL,
    reason_for_confusion TEXT, -- Why people commonly make this error
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now'))
);

-- 13. DISAMBIGUATIONS (Polysemous terms spanning multiple domains and concepts)
CREATE TABLE IF NOT EXISTS disambiguations (
    id TEXT PRIMARY KEY NOT NULL,
    term TEXT NOT NULL,
    term_normalized TEXT NOT NULL,
    concept_id TEXT NOT NULL REFERENCES concepts(id) ON DELETE CASCADE,
    domain_id TEXT NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
    category_id TEXT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    distinguishing_context TEXT NOT NULL,
    disambiguation_hint TEXT NOT NULL,
    sort_priority INTEGER DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')),
    UNIQUE(term_normalized, concept_id)
);

-- 14. COMPARISONS (Structured comparative analysis between commonly confused concepts)
CREATE TABLE IF NOT EXISTS comparisons (
    id TEXT PRIMARY KEY NOT NULL,
    concept_a_id TEXT NOT NULL REFERENCES concepts(id) ON DELETE CASCADE,
    concept_b_id TEXT NOT NULL REFERENCES concepts(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    shared_aspect TEXT NOT NULL,
    key_differences_json TEXT NOT NULL, -- JSON array of {criterion, concept_a_val, concept_b_val}
    common_confusion TEXT NOT NULL,
    rule_of_thumb TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')),
    UNIQUE(concept_a_id, concept_b_id)
);

-- 15. PROCEDURES (Step-by-step algorithms, troubleshooting flows, and standard methods)
CREATE TABLE IF NOT EXISTS procedures (
    id TEXT PRIMARY KEY NOT NULL,
    concept_id TEXT NOT NULL REFERENCES concepts(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    goal TEXT NOT NULL,
    prerequisites_json TEXT, -- JSON array of prerequisite concept IDs or skill strings
    steps_json TEXT NOT NULL, -- JSON array of {step_number, title, instruction, expected_outcome, cautionary_note}
    verification_step TEXT NOT NULL,
    troubleshooting_notes TEXT,
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now'))
);

-- 16. EMBEDDING METADATA (Prepared schema for future hybrid/vector retrieval)
CREATE TABLE IF NOT EXISTS embedding_metadata (
    id TEXT PRIMARY KEY NOT NULL,
    target_type TEXT NOT NULL CHECK (target_type IN ('chunk', 'concept', 'disambiguation')),
    target_id TEXT NOT NULL,
    model_name TEXT NOT NULL,
    dimensions INTEGER NOT NULL,
    quantization TEXT NOT NULL CHECK (quantization IN ('float32', 'int8', 'binary', 'none')) DEFAULT 'float32',
    vector_json TEXT, -- JSON array of float values (testing / light local cache)
    version TEXT NOT NULL DEFAULT '1.0.0',
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')),
    UNIQUE(target_type, target_id, model_name)
);
