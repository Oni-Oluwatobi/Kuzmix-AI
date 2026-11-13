-- =====================================================================
-- KUZMIX AI OFFLINE KNOWLEDGE DATABASE - ANDROID CONVENIENCE VIEWS
-- Standardized views matching the Android Query Contract
-- =====================================================================

-- 1. Full Concept Details with Domain & Category Hierarchy
CREATE VIEW IF NOT EXISTS v_concept_details AS
SELECT 
    c.id AS concept_id,
    c.name AS concept_name,
    c.canonical_name_normalized,
    c.summary_definition,
    c.primary_explanation,
    c.key_properties_json,
    c.applications_json,
    c.prerequisites_json,
    c.difficulty_level,
    c.confidence_score,
    c.provenance_type,
    c.content_hash,
    c.created_at,
    c.updated_at,
    d.id AS domain_id,
    d.name AS domain_name,
    d.slug AS domain_slug,
    cat.id AS category_id,
    cat.name AS category_name,
    t.id AS topic_id,
    t.name AS topic_name,
    s.title AS source_title,
    s.source_type,
    s.uri_or_reference AS source_ref
FROM concepts c
JOIN domains d ON c.domain_id = d.id
JOIN categories cat ON c.category_id = cat.id
LEFT JOIN topics t ON c.topic_id = t.id
LEFT JOIN sources s ON c.source_id = s.id;

-- 2. Compact RAG Context Delivery View
CREATE VIEW IF NOT EXISTS v_rag_context AS
SELECT 
    ch.id AS chunk_id,
    ch.concept_id,
    c.name AS concept_name,
    d.name AS domain_name,
    cat.name AS category_name,
    ch.chunk_index,
    ch.chunk_type,
    ch.header_context,
    ch.text_content,
    ch.token_estimate
FROM chunks ch
JOIN concepts c ON ch.concept_id = c.id
JOIN domains d ON c.domain_id = d.id
JOIN categories cat ON c.category_id = cat.id
ORDER BY ch.concept_id, ch.chunk_index ASC;

-- 3. Knowledge Graph Traverse View
CREATE VIEW IF NOT EXISTS v_concept_graph AS
SELECT 
    r.id AS relationship_id,
    r.source_concept_id,
    sc.name AS source_concept_name,
    r.relationship_type,
    r.target_concept_id,
    tc.name AS target_concept_name,
    r.description,
    r.weight
FROM relationships r
JOIN concepts sc ON r.source_concept_id = sc.id
JOIN concepts tc ON r.target_concept_id = tc.id;

-- 4. Unified Fast Search View (combines direct concept, aliases, and entities)
CREATE VIEW IF NOT EXISTS v_search_lookup AS
SELECT 
    c.id AS concept_id,
    c.name AS match_term,
    'concept_name' AS match_type,
    c.summary_definition,
    c.difficulty_level,
    c.domain_id,
    d.name AS domain_name,
    cat.name AS category_name
FROM concepts c
JOIN domains d ON c.domain_id = d.id
JOIN categories cat ON c.category_id = cat.id
UNION ALL
SELECT 
    a.concept_id,
    a.alias AS match_term,
    'alias' AS match_type,
    c.summary_definition,
    c.difficulty_level,
    c.domain_id,
    d.name AS domain_name,
    cat.name AS category_name
FROM aliases a
JOIN concepts c ON a.concept_id = c.id
JOIN domains d ON c.domain_id = d.id
JOIN categories cat ON c.category_id = cat.id;

-- 5. Disambiguation Lookup View
CREATE VIEW IF NOT EXISTS v_disambiguation_lookup AS
SELECT 
    dis.id AS disambiguation_id,
    dis.term,
    dis.term_normalized,
    dis.concept_id,
    c.name AS concept_name,
    dis.domain_id,
    d.name AS domain_name,
    dis.category_id,
    cat.name AS category_name,
    dis.distinguishing_context,
    dis.disambiguation_hint,
    c.summary_definition
FROM disambiguations dis
JOIN concepts c ON dis.concept_id = c.id
JOIN domains d ON dis.domain_id = d.id
JOIN categories cat ON dis.category_id = cat.id;

-- 6. Structured Comparisons View
CREATE VIEW IF NOT EXISTS v_concept_comparisons AS
SELECT 
    comp.id AS comparison_id,
    comp.title,
    comp.shared_aspect,
    comp.concept_a_id,
    ca.name AS concept_a_name,
    comp.concept_b_id,
    cb.name AS concept_b_name,
    comp.key_differences_json,
    comp.common_confusion,
    comp.rule_of_thumb
FROM comparisons comp
JOIN concepts ca ON comp.concept_a_id = ca.id
JOIN concepts cb ON comp.concept_b_id = cb.id;

-- 7. Step-by-Step Procedures View
CREATE VIEW IF NOT EXISTS v_concept_procedures AS
SELECT 
    p.id AS procedure_id,
    p.concept_id,
    c.name AS concept_name,
    p.title,
    p.goal,
    p.prerequisites_json,
    p.steps_json,
    p.verification_step,
    p.troubleshooting_notes
FROM procedures p
JOIN concepts c ON p.concept_id = c.id;

