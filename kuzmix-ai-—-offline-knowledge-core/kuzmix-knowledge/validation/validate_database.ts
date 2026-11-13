import { DatabaseSync } from 'node:sqlite';
import * as path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const DB_PATH = path.resolve(__dirname, '..', 'kuzmix_knowledge.sqlite');

export interface ValidationReport {
  passed: boolean;
  errors: string[];
  warnings: string[];
  metrics: Record<string, any>;
}

export function validateDatabase(dbPath: string = DB_PATH): ValidationReport {
  console.log('\n====================================================');
  console.log('  KUZMIX AI - DATABASE VALIDATION SUITE');
  console.log('====================================================');
  console.log(`Auditing database: ${dbPath}\n`);

  const db = new DatabaseSync(dbPath, { open: true });
  db.exec('PRAGMA foreign_keys = ON;');

  const errors: string[] = [];
  const warnings: string[] = [];

  // 1. SQLite Pragma Integrity
  const integrity = db.prepare('PRAGMA integrity_check').get() as any;
  if (integrity.integrity_check !== 'ok') {
    errors.push(`PRAGMA integrity_check failed: ${JSON.stringify(integrity)}`);
  } else {
    console.log('  ✓ SQLite Low-Level B-Tree Integrity: OK');
  }

  // 2. Foreign Key Check
  const fkViolations = db.prepare('PRAGMA foreign_key_check').all();
  if (fkViolations.length > 0) {
    errors.push(`Foreign key violations found: ${fkViolations.length}`);
  } else {
    console.log('  ✓ Relational Foreign Key Constraints: 0 Violations');
  }

  // 3. Check for Orphaned Categories
  const orphanCats = db.prepare(`
    SELECT c.id, c.name FROM categories c
    LEFT JOIN domains d ON c.domain_id = d.id
    WHERE d.id IS NULL
  `).all();
  if (orphanCats.length > 0) {
    errors.push(`Orphaned categories detected: ${orphanCats.map((c: any) => c.id).join(', ')}`);
  }

  // 4. Check for Orphaned Concepts
  const orphanConcepts = db.prepare(`
    SELECT c.id, c.name FROM concepts c
    LEFT JOIN categories cat ON c.category_id = cat.id
    WHERE cat.id IS NULL
  `).all();
  if (orphanConcepts.length > 0) {
    errors.push(`Orphaned concepts detected: ${orphanConcepts.map((c: any) => c.id).join(', ')}`);
  }

  // 5. Relationship Targets & Self-references
  const badRelationships = db.prepare(`
    SELECT r.id, r.source_concept_id, r.target_concept_id
    FROM relationships r
    LEFT JOIN concepts sc ON r.source_concept_id = sc.id
    LEFT JOIN concepts tc ON r.target_concept_id = tc.id
    WHERE sc.id IS NULL OR tc.id IS NULL
  `).all();
  if (badRelationships.length > 0) {
    errors.push(`Dangling relationships with missing concept endpoints: ${badRelationships.length}`);
  }

  const selfLoops = db.prepare(`
    SELECT id, source_concept_id FROM relationships WHERE source_concept_id = target_concept_id
  `).all();
  if (selfLoops.length > 0) {
    warnings.push(`Self-referential relationship loop detected in ${selfLoops.length} edge(s)`);
  }

  // 6. Content Emptiness and Quality Checks
  const emptyDefs = db.prepare(`
    SELECT id, name FROM concepts 
    WHERE length(trim(summary_definition)) < 10 OR length(trim(primary_explanation)) < 20
  `).all();
  if (emptyDefs.length > 0) {
    errors.push(`Concepts with insufficient definition/explanation length: ${emptyDefs.map((c: any) => c.id).join(', ')}`);
  }

  // 7. Confidence Score Boundary Check (0.0 to 1.0)
  const badConfidence = db.prepare(`
    SELECT id FROM concepts WHERE confidence_score < 0.0 OR confidence_score > 1.0
  `).all();
  if (badConfidence.length > 0) {
    errors.push(`Concepts with out-of-bounds confidence scores: ${badConfidence.length}`);
  }

  // 8. FTS5 Index Coverage Check
  const ftsCount = (db.prepare('SELECT COUNT(*) AS count FROM knowledge_fts').get() as any).count;
  const conceptCount = (db.prepare('SELECT COUNT(*) AS count FROM concepts').get() as any).count;
  if (ftsCount < conceptCount) {
    errors.push(`FTS index entries (${ftsCount}) are fewer than concepts (${conceptCount})`);
  } else {
    console.log(`  ✓ FTS5 Search Coverage: ${ftsCount} index entries covering ${conceptCount} concepts`);
  }

  // 9. Chunk Continuity Check
  const chunksPerConcept = db.prepare(`
    SELECT concept_id, COUNT(*) AS count, MIN(chunk_index) AS min_idx, MAX(chunk_index) AS max_idx
    FROM chunks
    GROUP BY concept_id
  `).all();

  for (const c of chunksPerConcept as any[]) {
    if (c.min_idx !== 0 || c.max_idx !== c.count - 1) {
      warnings.push(`Concept ${c.concept_id} has non-sequential chunk indices (0..${c.count - 1} expected, got ${c.min_idx}..${c.max_idx})`);
    }
  }

  // Collect Metrics
  const metrics = {
    total_domains: (db.prepare('SELECT COUNT(*) AS count FROM domains').get() as any).count,
    total_categories: (db.prepare('SELECT COUNT(*) AS count FROM categories').get() as any).count,
    total_topics: (db.prepare('SELECT COUNT(*) AS count FROM topics').get() as any).count,
    total_concepts: conceptCount,
    total_records: (db.prepare('SELECT COUNT(*) AS count FROM knowledge_records').get() as any).count,
    total_chunks: (db.prepare('SELECT COUNT(*) AS count FROM chunks').get() as any).count,
    total_relationships: (db.prepare('SELECT COUNT(*) AS count FROM relationships').get() as any).count,
    total_misconceptions: (db.prepare('SELECT COUNT(*) AS count FROM misconceptions').get() as any).count,
    total_aliases: (db.prepare('SELECT COUNT(*) AS count FROM aliases').get() as any).count,
    total_fts_entries: ftsCount
  };

  db.close();

  const passed = errors.length === 0;

  console.log(`\nValidation Status: ${passed ? '✓ PASSED (All checks clear)' : '✗ FAILED'}`);
  if (errors.length > 0) {
    console.log('\nErrors:');
    errors.forEach(e => console.log(`  ✗ ${e}`));
  }
  if (warnings.length > 0) {
    console.log('\nWarnings:');
    warnings.forEach(w => console.log(`  ! ${w}`));
  }

  return {
    passed,
    errors,
    warnings,
    metrics
  };
}

if (process.argv[1]?.includes('validate_database')) {
  validateDatabase();
}
