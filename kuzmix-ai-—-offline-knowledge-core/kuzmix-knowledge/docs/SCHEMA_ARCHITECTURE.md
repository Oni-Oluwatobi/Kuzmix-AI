# Kuzmix AI — Database Schema Architecture

## Architectural Philosophy

The **Kuzmix AI Offline Knowledge Base** is engineered specifically as a high-density, low-latency relational and full-text knowledge corpus. It avoids the pitfall of unstructured Q&A dumps by decoupling intellectual concepts into structured, attributed nodes connected through a directed semantic graph.

```
+-------------------------------------------------------------+
|                     DOMAINS (9)                             |
+------------------------------+------------------------------+
                               | 1:N
                               v
+-------------------------------------------------------------+
|                   CATEGORIES (61)                           |
+------------------------------+------------------------------+
                               | 1:N
                               v
+-------------------------------------------------------------+
|                     TOPICS (122)                            |
+------------------------------+------------------------------+
                               | 1:N
                               v
+-------------------------------------------------------------+
|                    CONCEPTS (2,577)                         |
+--------+-------------+-------------+------------+-----------+
         | 1:N         | 1:N         | 1:N        | 1:N (Graph)
         v             v             v            v
  +--------------+  +-------+  +------------+  +--------------+
  |  KNOWLEDGE   |  | RAG   |  | ALIASES &  |  | RELATIONSHIP |
  |   RECORDS    |  | CHUNKS|  | MISCONCEPT.|  |    EDGES     |
  +--------------+  +-------+  +------------+  +--------------+
         |
         v Synced via FTS5
  +-----------------------------------------------------------+
  |            SQLite FTS5 VIRTUAL SEARCH TABLE               |
  +-----------------------------------------------------------+
```

---

## 1. Table Specifications

### `domains`
High-level human faculties of knowledge:
- `id` (TEXT, PK): Semantic slug (e.g. `sci_math`, `computing_ai`, `practical_life`).
- `name` (TEXT): Display name.
- `slug` (TEXT, UNIQUE): URL/path friendly identifier.
- `icon` (TEXT): Lucide/Vector icon hint.

### `categories`
Disciplines within domains:
- `id` (TEXT, PK): Unique ID (e.g. `cat_physics`, `cat_microecon`).
- `domain_id` (TEXT, FK): References `domains(id)`.
- `name` (TEXT): Discipline title.

### `topics`
Thematic clusters:
- `id` (TEXT, PK): Unique ID.
- `category_id` (TEXT, FK): References `categories(id)`.

### `sources`
Attribution and provenance:
- `id` (TEXT, PK): Unique reference ID.
- `title` (TEXT): Source document or authority title.
- `source_type` (TEXT): `academic_textbook`, `open_educational_resource`, `institutional_guideline`, etc.

### `concepts`
The core intellectual unit:
- `id` (TEXT, PK): Canonical concept ID (e.g. `c_gravity`, `c_hash_table`).
- `topic_id`, `category_id`, `domain_id`: Hierarchical foreign keys.
- `name` (TEXT): Display title.
- `canonical_name_normalized` (TEXT, Indexed): Lowercase, trimmed text for O(1) B-tree lookup.
- `summary_definition` (TEXT): Terse definition suitable for 1-sentence answering.
- `primary_explanation` (TEXT): Pedagogical, structured explanation.
- `key_properties_json` (TEXT): Structured JSON of properties, constants, formulas.
- `applications_json` (TEXT): JSON array of real-world use cases.
- `prerequisites_json` (TEXT): JSON array of prerequisite concept references.
- `confidence_score` (REAL): Metric bounded between 0.0 and 1.0.
- `provenance_type` (TEXT): Integrity tag (`verified_fact`, `curated_scientific`, etc.).
- `content_hash` (TEXT): SHA256 signature to guarantee deduplication upon re-ingestion.

### `knowledge_records`
Atomic knowledge primitives:
- `id` (TEXT, PK): Unique record identifier.
- `concept_id` (TEXT, FK): Parent concept.
- `record_type` (TEXT): Enumerated type (`law`, `formula`, `theorem`, `procedure`, `example`, `FAQ`, etc.).
- `title` (TEXT): Distinct title.
- `content` (TEXT): Full record text.

### `chunks`
Engineered semantic segments for mobile RAG:
- `id` (TEXT, PK): Chunk ID (`chk_{concept_id}_{index}`).
- `concept_id` (TEXT, FK): Parent concept.
- `chunk_index` (INTEGER): Positional order.
- `chunk_type` (TEXT): Segment role (`definition`, `properties_and_examples`, `misconceptions`).
- `text_content` (TEXT): Self-contained 80-250 token passage.
- `token_estimate` (INTEGER): Pre-calculated token budget.
- `header_context` (TEXT): Breadcrumb text prepended during prompt synthesis.
- `embedding_id` (TEXT, NULL): Placeholder column for future vector integration without schema redesign.

### `relationships`
Knowledge graph edges:
- `id` (TEXT, PK): Unique edge identifier.
- `source_concept_id` (TEXT, FK): Source node.
- `target_concept_id` (TEXT, FK): Target node.
- `relationship_type` (TEXT): `related_to`, `prerequisite_of`, `part_of`, `subtype_of`, `causes`, `contrasts_with`, `used_for`, `associated_with`.
- `description` (TEXT): Semantic meaning of edge.
- `weight` (REAL): Connection strength (0.0 - 1.0).

### `misconceptions`
Pedagogical guardrails:
- `misconception_text`: The false belief commonly held.
- `correction_text`: The scientifically verified correction.
- `explanation`: Detailed explanation of why the misconception occurs and why it is incorrect.

### `knowledge_fts`
SQLite FTS5 virtual table tokenized with `unicode61 remove_diacritics 2` indexing titles, definitions, explanations, keywords, and aliases for rapid BM25 ranking.

---

## 2. Vector Extension Strategy (Future Phase Readiness)

The database is built so vector embeddings can be added seamlessly:
1. `chunks.embedding_id` links directly to an external or internal vector table (such as `sqlite-vec` or an external `.bin` embedding file).
2. Existing text queries and FTS5 continue working with zero breaking changes if vector extensions are omitted.
