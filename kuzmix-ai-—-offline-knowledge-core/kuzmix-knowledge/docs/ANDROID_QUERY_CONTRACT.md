# Kuzmix AI — Android Offline Query Contract

This specification defines the lightweight, low-RAM query contract for the Kuzmix AI engine on Android.

---

## 1. High-Level Design Principles for Android

1. **Zero Full-Table Scans**: All lookups must resolve through targeted B-Tree indices or SQLite FTS5.
2. **Never Cache Full Rows in Memory**: Retrieve only requested columns. For RAG prompts, query pre-computed `chunks` rather than hydrating full concepts and child tables.
3. **Memory Mapped I/O (`PRAGMA mmap_size`)**: Enables direct zero-copy page cache access from Android flash storage into process virtual memory without duplicate RAM allocations.
4. **Read-Heavy Architecture**: Run with `PRAGMA query_only = ON;` in production Android application instances.

---

## 2. Standard Android SQLite Pragmas

Execute these immediately upon opening `kuzmix_knowledge.sqlite` in Android:

```sql
PRAGMA foreign_keys = ON;
PRAGMA temp_store = MEMORY;
PRAGMA query_only = ON;          -- Prevents accidental writes & locking
PRAGMA mmap_size = 268435456;   -- 256MB memory-mapped I/O (kernel manages page cache)
```

---

## 3. Query Contract Matrix

### A. Lookup Concept by ID
```sql
SELECT 
    concept_id, concept_name, summary_definition, primary_explanation,
    difficulty_level, confidence_score, provenance_type,
    domain_name, category_name, source_title
FROM v_concept_details
WHERE concept_id = :conceptId;
```

### B. Lookup Concept by Exact Name or Alias
```sql
SELECT 
    c.id AS concept_id,
    c.name AS concept_name,
    c.summary_definition,
    c.primary_explanation,
    c.domain_id,
    c.category_id
FROM concepts c
WHERE c.canonical_name_normalized = lower(trim(:inputName))
UNION
SELECT 
    c.id AS concept_id,
    c.name AS concept_name,
    c.summary_definition,
    c.primary_explanation,
    c.domain_id,
    c.category_id
FROM aliases a
JOIN concepts c ON a.concept_id = c.id
WHERE a.alias_normalized = lower(trim(:inputName))
LIMIT 1;
```

### C. Retrieve Exact Definition Only (Ultra Low Memory)
```sql
SELECT summary_definition 
FROM concepts 
WHERE id = :conceptId;
```

### D. Full-Text Search (FTS5 BM25 Ranked — Revised)
Row-level `LIMIT` starves recall: concepts with many indexed rows never
get a slot, so broad queries returned fewer unique concepts than asked
for. The revised form opens an inner BM25 row window (`:window` =
`min(limit*12, 600)`), then groups to ONE best row per concept.
Column weights: title x10, aliases x8, keywords x4 (content x1, others 0).
`MIN(bm25(...))` directly is illegal in SQLite — bm25 is only usable in a
plain ORDER BY context — hence the subquery.
```sql
SELECT concept_id FROM (
    SELECT 
        concept_id,
        bm25(knowledge_fts, 0, 0, 10.0, 1.0, 8.0, 1.0, 1.0, 4.0) AS r
    FROM knowledge_fts
    WHERE knowledge_fts MATCH :sanitizedFtsQuery
    ORDER BY r ASC
    LIMIT :window
)
GROUP BY concept_id
ORDER BY MIN(r) ASC
LIMIT :limit;
```

### E. Mobile RAG Chunk Retrieval (Context Window Assembly)
```sql
SELECT 
    chunk_index,
    header_context,
    text_content,
    token_estimate
FROM chunks
WHERE concept_id = :conceptId
ORDER BY chunk_index ASC;
```

### F. Misconceptions & Corrections (Anti-Hallucination Tutoring)
```sql
SELECT 
    misconception_text,
    correction_text,
    explanation
FROM misconceptions
WHERE concept_id = :conceptId;
```

### G. Relationship Traversal (Graph Edges)
```sql
-- Outgoing relationships (what this concept leads to or depends on)
SELECT 
    r.relationship_type,
    tc.id AS target_concept_id,
    tc.name AS target_concept_name,
    r.description,
    r.weight
FROM relationships r
JOIN concepts tc ON r.target_concept_id = tc.id
WHERE r.source_concept_id = :conceptId

UNION ALL

-- Incoming prerequisites (what must be learned before this concept)
SELECT 
    'prerequisite_for' AS relationship_type,
    sc.id AS target_concept_id,
    sc.name AS target_concept_name,
    r.description,
    r.weight
FROM relationships r
JOIN concepts sc ON r.source_concept_id = sc.id
WHERE r.target_concept_id = :conceptId 
  AND r.relationship_type = 'prerequisite_of';
```

### H. Domain and Category Filtering
```sql
SELECT 
    id, name, summary_definition, difficulty_level
FROM concepts
WHERE domain_id = :domainId AND category_id = :categoryId
ORDER BY name ASC
LIMIT :pageSize OFFSET :offset;
```

---

## 4. Kotlin Android Room / SQLite Implementation Example

```kotlin
package ai.kuzmix.knowledge

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "concepts")
data class ConceptEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "summary_definition") val summaryDefinition: String,
    @ColumnInfo(name = "primary_explanation") val primaryExplanation: String,
    @ColumnInfo(name = "domain_id") val domainId: String,
    @ColumnInfo(name = "category_id") val categoryId: String,
    @ColumnInfo(name = "difficulty_level") val difficultyLevel: String,
    @ColumnInfo(name = "confidence_score") val confidenceScore: Float
)

data class RagChunkItem(
    @ColumnInfo(name = "chunk_index") val chunkIndex: Int,
    @ColumnInfo(name = "header_context") val headerContext: String,
    @ColumnInfo(name = "text_content") val textContent: String,
    @ColumnInfo(name = "token_estimate") val tokenEstimate: Int
)

@Dao
interface KuzmixKnowledgeDao {
    @Query("SELECT * FROM concepts WHERE id = :id LIMIT 1")
    suspend fun getConceptById(id: String): ConceptEntity?

    @Query("""
        SELECT chunk_index, header_context, text_content, token_estimate 
        FROM chunks 
        WHERE concept_id = :conceptId 
        ORDER BY chunk_index ASC
    """)
    suspend fun getRagChunks(conceptId: String): List<RagChunkItem>

    @Query("""
        SELECT concept_id 
        FROM knowledge_fts 
        WHERE knowledge_fts MATCH :ftsQuery 
        ORDER BY bm25(knowledge_fts) ASC 
        LIMIT :limit
    """)
    suspend fun searchFts(ftsQuery: String, limit: Int = 10): List<String>
}
```

---

## 5. Memory Management Checklist for Low-End Android Devices

- [x] **Cursor Management**: Always consume `Cursor` instances within `use { ... }` blocks to prevent native memory leaks.
- [x] **Projections**: Never execute `SELECT *` in production UI threads.
- [x] **Pagination**: Keep result sets to `<= 20` items using `LIMIT` and `OFFSET`.
- [x] **Pre-calculated Tokens**: Rely on the `token_estimate` column rather than loading heavy tokenizers into device RAM.
