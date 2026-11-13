import { Smartphone, ShieldCheck } from "lucide-react";
import { Card, CodeBlock, Pill, SectionTitle } from "./ui";

/* Verbatim from kuzmix-knowledge/docs/ANDROID_QUERY_CONTRACT.md —        */
/* Query Contract Matrix A–H plus the Room DAO + low-RAM pragma blocks.   */

const QUERY_A = `SELECT
    concept_id, concept_name, summary_definition, primary_explanation,
    difficulty_level, confidence_score, provenance_type,
    domain_name, category_name, source_title
FROM v_concept_details
WHERE concept_id = :conceptId;`;

const QUERY_B = `SELECT
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
LIMIT 1;`;

const QUERY_C = `SELECT summary_definition
FROM concepts
WHERE id = :conceptId;`;

const QUERY_D = `SELECT concept_id FROM (
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
LIMIT :limit;`;

const QUERY_E = `SELECT
    chunk_index,
    header_context,
    text_content,
    token_estimate
FROM chunks
WHERE concept_id = :conceptId
ORDER BY chunk_index ASC;`;

const QUERY_F = `SELECT
    misconception_text,
    correction_text,
    explanation
FROM misconceptions
WHERE concept_id = :conceptId;`;

const QUERY_G = `-- Outgoing relationships (what this concept leads to or depends on)
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
  AND r.relationship_type = 'prerequisite_of';`;

const QUERY_H = `SELECT
    id, name, summary_definition, difficulty_level
FROM concepts
WHERE domain_id = :domainId AND category_id = :categoryId
ORDER BY name ASC
LIMIT :pageSize OFFSET :offset;`;

const ROOM_DAO = `@Dao
interface KuzmixKnowledgeDao {
  // 1. O(1) Fast Exact Lookup
  @Query("SELECT * FROM concepts WHERE id = :id LIMIT 1")
  suspend fun getConceptById(id: String): ConceptEntity?

  // 2. FTS5 BM25 Full Text Search
  @Query("""
    SELECT concept_id
    FROM knowledge_fts
    WHERE knowledge_fts MATCH :query
    ORDER BY bm25(knowledge_fts) ASC
    LIMIT :limit
  """)
  suspend fun searchFts(query: String, limit: Int = 10): List<String>

  // 3. Low-Memory RAG Chunk Retrieval
  @Query("""
    SELECT text_content, token_estimate
    FROM chunks
    WHERE concept_id = :conceptId
    ORDER BY chunk_index ASC
  """)
  suspend fun getRagChunks(conceptId: String): List<RagChunkItem>
}`;

const PRAGMAS = `-- Enforce relational foreign keys
PRAGMA foreign_keys = ON;

-- Use memory for temporary tables & sorts
PRAGMA temp_store = MEMORY;

-- Lock database into read-only mode to prevent locks
PRAGMA query_only = ON;

-- Memory mapped I/O: Zero-copy reads from flash storage
PRAGMA mmap_size = 268435456; -- 256MB mmap

-- Optimize FTS5 performance
PRAGMA cache_size = -2000; -- 2MB RAM cache budget`;

const QUERIES: { id: string; title: string; note: string; code: string }[] = [
  { id: "A", title: "Lookup Concept by ID", note: "Primary key B-Tree lookup", code: QUERY_A },
  { id: "B", title: "Lookup by Exact Name or Alias", note: "Canonical + alias union", code: QUERY_B },
  { id: "C", title: "Exact Definition Only", note: "Ultra low-memory projection", code: QUERY_C },
  { id: "D", title: "FTS5 BM25 Ranked Search", note: "Windowed, one row per concept", code: QUERY_D },
  { id: "E", title: "Mobile RAG Chunk Retrieval", note: "Context window assembly", code: QUERY_E },
  { id: "F", title: "Misconceptions & Corrections", note: "Anti-hallucination tutoring", code: QUERY_F },
  { id: "G", title: "Relationship Traversal", note: "Outgoing + incoming prerequisite edges", code: QUERY_G },
  { id: "H", title: "Domain & Category Filtering", note: "Paginated filtered listing", code: QUERY_H }
];

export default function AndroidContractTab() {
  return (
    <div className="space-y-6">
      <Card className="p-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-start gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-[#FF8C00]/15">
              <Smartphone className="h-4 w-4 text-[#FF8C00]" />
            </div>
            <div>
              <h2 className="text-base font-bold text-white">
                Android Offline Query Contract &amp; Room DAO
              </h2>
              <p className="mt-1 max-w-3xl text-xs leading-relaxed text-[#9CA3AF]">
                Kuzmix AI on low-end Android devices accesses{" "}
                <code className="rounded bg-[#0A0A0F] px-1.5 py-0.5 text-[#FFC107]">
                  kuzmix_knowledge.sqlite
                </code>{" "}
                directly through SQLite Native or Android Room with zero runtime cloud
                dependencies. Every statement below is indexed — no full-table scans.
              </p>
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            <Pill tone="orange">Queries A – H</Pill>
            <Pill tone="yellow">Room 2.5+</Pill>
            <Pill tone="green">Zero cloud</Pill>
          </div>
        </div>
      </Card>

      <div>
        <SectionTitle hint="Click copy on any block to paste it straight into your DAO or cursor query">
          Standard Android SQL Retrieval Matrix
        </SectionTitle>
        <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
          {QUERIES.map((q) => (
            <CodeBlock key={q.id} title={`${q.id}. ${q.title}`} badge={q.note} code={q.code} />
          ))}
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <CodeBlock title="KuzmixKnowledgeDao.kt" badge="Room 2.5+" code={ROOM_DAO} language="kotlin" />
        <CodeBlock title="Android SQLite Pragmas" badge="Low-RAM safe" code={PRAGMAS} />
      </div>

      <Card className="flex items-start gap-3 p-5">
        <ShieldCheck className="mt-0.5 h-4 w-4 shrink-0 text-emerald-400" />
        <p className="text-xs leading-relaxed text-[#9CA3AF]">
          <span className="font-semibold text-white">Memory management checklist:</span> consume
          every cursor inside <code className="text-[#FFC107]">use {"{ }"}</code> blocks, never{" "}
          <code className="text-[#FFC107]">SELECT *</code> on UI threads, keep pages at 20 items
          or fewer, and always rely on the pre-computed{" "}
          <code className="text-[#FFC107]">token_estimate</code> column instead of loading a
          tokenizer into device RAM.
        </p>
      </Card>
    </div>
  );
}
