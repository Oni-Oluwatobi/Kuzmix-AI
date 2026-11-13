# Kuzmix AI — Offline Knowledge Core (`kuzmix_knowledge.sqlite`)

Welcome to the official offline knowledge engine for **Kuzmix AI**.

This project provides a production-grade, structured, queryable SQLite knowledge database designed specifically to run offline on low-RAM mobile devices (such as Android smartphones) and power local Retrieval-Augmented Generation (RAG).

---

## 1. Database Purpose & Core Philosophy

Rather than storing an unstructured dump of Q&A pairs or conversational chatbot answers, the Kuzmix Knowledge Core stores an **interconnected knowledge corpus**:
- **Structured Intellectual Concepts**: Each concept features an exact summary definition, pedagogical explanation, key properties/formulas, practical applications, prerequisites, confidence score, and verified source provenance.
- **RAG-Engineered Semantic Chunks**: Pre-segmented chunks (80–250 tokens) with pre-calculated token estimates allow the on-device AI to assemble targeted prompt contexts within tight mobile RAM limits.
- **Explicit Misconceptions & Corrections**: Provides guardrails against hallucinations by explicitly mapping common false beliefs to verified corrections.
- **Semantic Knowledge Graph**: Directed relationship edges (`prerequisite_of`, `used_for`, `contrasts_with`, `causes`, `part_of`, `related_to`) allow multi-hop concept reasoning.
- **Zero-Cloud Offline Execution**: Fully contained in a single self-contained SQLite file with native FTS5 full-text indexing.

---

## 2. Factual Build Metrics (Massively Expanded Corpus)

| Metric | Verified Count |
| :--- | :--- |
| **Database File** | `kuzmix_knowledge.sqlite` |
| **File Size** | **38.33 MB** (40,189,952 bytes) |
| **Schema Version** | **1.2.0** (journal mode `delete`) |
| **Domains Covered** | **9** |
| **Categories** | **61** |
| **Topics** | **122** |
| **Structured Concepts** | **2,577** |
| **Knowledge Records** | **4,408** |
| **RAG Semantic Chunks** | **8,891** |
| **Knowledge Graph Edges** | **2,617** (incl. **224 `prerequisite_of`** materialized from prerequisites) |
| **Search Aliases / Synonyms** | **6,134** |
| **Misconceptions & Corrections** | **2,587** |
| **Named Entities** | **28** |
| **Disambiguations** | **34** |
| **Structured Comparisons** | **8** |
| **Procedural Workflows** | **4** |
| **Embedding Metadata Rows** | **8** |
| **Attributed Sources** | **16** |
| **FTS5 Indexed Rows** | **7,031** |
| **Integrity Checks** | **100% OK (0 foreign key violations, PRAGMA ok)** |

---

## 3. Knowledge Domains in Seed Corpus

1. **Science and Mathematics**: Gravity, Newton's Laws, Conservation of Energy, Cellular Respiration, Calculus Derivatives.
2. **Engineering and Technology**: Ohm's Law, Bernoulli's Principle, Stress/Strain and Young's Modulus.
3. **Computing and Artificial Intelligence**: Big-O Notation, Hash Tables, Transformer Architecture, Retrieval-Augmented Generation (RAG).
4. **Business and Economics**: Supply and Demand Equilibrium, Opportunity Cost, Compound Interest & Time Value of Money.
5. **Human Knowledge and Humanities**: Falsifiability & Demarcation Problem, Confirmation Bias & Dual-Process Theory, Working Memory Limits.
6. **Society, Civics and Culture**: Separation of Powers, The Rule of Law and Due Process.
7. **Health and Life Sciences**: Human Cardiovascular Circulation, Cardiopulmonary Resuscitation (CPR) & AED Science.
8. **Creative Fields and Arts**: Gestalt Principles of Visual Perception, Typographic Hierarchy & Readability.
9. **Practical and Applied Knowledge**: The Maillard Reaction in Cooking, Leverage & Mechanical Advantage, Domestic Electrical Safety & GFCI.

---

## 4. Directory Structure

```
kuzmix-knowledge/
├── kuzmix_knowledge.sqlite     # Production binary SQLite 3 database
├── schema/
│   ├── 001_initial_schema.sql  # DDL for core tables & constraints
│   ├── 002_fts_and_indexes.sql # FTS5 virtual table & B-Tree performance indices
│   └── 003_views_and_contracts.sql # Android convenience views
├── data/
│   ├── domains.json            # Faculty domains
│   ├── categories.json         # Subject disciplines
│   ├── topics.json             # Thematic topics
│   ├── sources.json            # Genuine reference authorities & OER
│   ├── expansion_pack_astronomy.json # Sample expansion pack
│   └── concepts/               # Modular domain concept files
│       ├── concepts_flagship.json
│       ├── concepts_sci_math.json
│       ├── concepts_eng_tech.json
│       ├── concepts_computing_ai.json
│       ├── concepts_business_econ.json
│       ├── concepts_humanities.json
│       ├── concepts_society_culture.json
│       ├── concepts_health_life.json
│       ├── concepts_creative_arts.json
│       ├── concepts_practical_life.json
│       └── concepts_expansion_astronomy.json  # expansion pack folded in (c_black_holes)
│   └── rewrites/               # Content rewrite batches (ground-truth replacements)
│       ├── batch_001_econ_foundations.json
│       ├── batch_002_cs_science_finance.json
│       ├── batch_003_prob_stats.json
│       └── batch_004_mechanics.json
├── scripts/
│   ├── build_database.ts       # Build entry point (invokes the Python builder)
│   ├── build_sqlite.py         # Deterministic compiler for database
│   └── apply_rewrites.py       # Applies data/rewrites/batch_*.json to concepts (idempotent)
├── tools/
│   ├── query_cli.ts            # CLI search, lookup, and RAG builder
│   ├── import_pack.ts          # Incremental expansion pack importer
│   └── export_dataset.ts       # JSON and structured export utility
├── validation/
│   └── validate_database.ts    # Comprehensive integrity auditor
├── tests/
│   └── test_database.ts        # 14-point automated test suite
└── docs/
    ├── ANDROID_QUERY_CONTRACT.md # Android Room / SQLite integration guide
    └── SCHEMA_ARCHITECTURE.md  # Detailed entity-relationship specifications
```

---

## 5. Tooling & Commands

### A. Rebuild Database from Source Data
To compile all JSON datasets into `kuzmix_knowledge.sqlite`:
```bash
npx tsx kuzmix-knowledge/scripts/build_database.ts
```

### B. Run the Query CLI & RAG Context Builder
```bash
# General search
npx tsx kuzmix-knowledge/tools/query_cli.ts "gravity"

# Technical search
npx tsx kuzmix-knowledge/tools/query_cli.ts "hash table"
```

### C. Run the Validation Suite
```bash
npx tsx kuzmix-knowledge/validation/validate_database.ts
```

### D. Run the Automated Test Suite
```bash
npx tsx kuzmix-knowledge/tests/test_database.ts
```

### E. Import an Expansion Knowledge Pack
```bash
npx tsx kuzmix-knowledge/tools/import_pack.ts kuzmix-knowledge/data/expansion_pack_astronomy.json
```
The importer refreshes `metadata.total_concepts` / `metadata.total_fts_entries` after every import, so the internal metadata always matches the live row counts. (The 2,577-concept corpus = 2,576 concepts in `data/concepts/*.json` + `c_black_holes`, whose full satellites now live in `data/concepts/concepts_expansion_astronomy.json`.)

### F. Apply Content Rewrite Batches
```bash
python kuzmix-knowledge/scripts/apply_rewrites.py   # then rebuild (§A)
```
Rewrite batches in `data/rewrites/` replace template-generated summaries/explanations with
ground-truth content and regenerate records/chunks from the new fields. Regenerated content is
immune to `enrich_corpus.py` re-templating only if enrichment is not re-run — run `apply_rewrites.py`
after any enrichment pass. 54 concepts rewritten so far (batches 001-004: econ foundations,
CS/science/finance, probability & statistics, physics mechanics); remaining template
concepts are scheduled for later batches.

### G. Export SQLite to Clean JSON
```bash
npx tsx kuzmix-knowledge/tools/export_dataset.ts
```

---

## 6. Android Consumption & Query Contract

For complete code examples (Kotlin / Room DAO) and low-memory guidance, refer to `kuzmix-knowledge/docs/ANDROID_QUERY_CONTRACT.md`.

### Quick SQLite Android Example:
```sql
-- 1. Exact Concept Lookup
SELECT * FROM v_concept_details WHERE concept_id = 'c_gravity';

-- 2. Fast BM25 Full-Text Search
SELECT concept_id, title, snippet(knowledge_fts, 1, '<b>', '</b>', '...', 12) AS snippet
FROM knowledge_fts 
WHERE knowledge_fts MATCH 'gravity*'
ORDER BY bm25(knowledge_fts) ASC 
LIMIT 10;

-- 3. RAG Mobile Prompt Chunks
SELECT text_content, token_estimate 
FROM chunks 
WHERE concept_id = 'c_gravity' 
ORDER BY chunk_index ASC;
```

---

## 7. Future Expansion Roadmap

1. **Incremental Knowledge Packs**: Adding new domains or specialty packs using `import_pack.ts` without database rebuilding.
2. **Optional Vector Embeddings**: `chunks.embedding_id` is prepared for vector embeddings (e.g. 256-dim int8 quantized vectors) without schema alterations.
3. **Android Room Packaging**: Bundle directly into Android `assets/kuzmix_knowledge.sqlite` using `Room.databaseBuilder(...).createFromAsset("kuzmix_knowledge.sqlite")`.
