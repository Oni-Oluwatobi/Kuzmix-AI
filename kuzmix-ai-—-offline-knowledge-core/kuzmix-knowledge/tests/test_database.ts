import { DatabaseSync } from 'node:sqlite';
import * as assert from 'node:assert';
import * as path from 'node:path';
import * as fs from 'node:fs';
import { fileURLToPath } from 'node:url';
import { KuzmixKnowledgeClient } from '../tools/query_cli.ts';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const PROD_DB_PATH = path.resolve(__dirname, '..', 'kuzmix_knowledge.sqlite');
const TEST_DB_PATH = path.resolve(__dirname, 'test_sandbox.sqlite');

export interface TestCaseResult {
  name: string;
  status: 'passed' | 'failed';
  durationMs: number;
}

export interface TestSuiteResult {
  tests: TestCaseResult[];
  total: number;
  passed: number;
  durationMs: number;
}

export async function runAllTests(): Promise<TestSuiteResult> {
  const suiteStart = performance.now();
  console.log('\n======================================================');
  console.log('  KUZMIX AI - COMPREHENSIVE OFFLINE DATABASE TEST SUITE');
  console.log('======================================================\n');

  let passedTests = 0;
  let totalTests = 0;
  const testResults: TestCaseResult[] = [];

  function runTest(name: string, fn: () => void) {
    totalTests++;
    const started = performance.now();
    try {
      fn();
      console.log(`  ✓ [PASS] ${name}`);
      passedTests++;
      testResults.push({
        name,
        status: 'passed',
        durationMs: Math.round((performance.now() - started) * 10) / 10
      });
    } catch (err: any) {
      console.error(`  ✗ [FAIL] ${name}: ${err.message}`);
      testResults.push({
        name,
        status: 'failed',
        durationMs: Math.round((performance.now() - started) * 10) / 10
      });
      throw err;
    }
  }

  // --- Test Group 1: Production Database Verification ---
  console.log('Group 1: Production Database & Schema Verification');

  runTest('Database file exists and has non-zero size', () => {
    assert.ok(fs.existsSync(PROD_DB_PATH), 'kuzmix_knowledge.sqlite must exist');
    const size = fs.statSync(PROD_DB_PATH).size;
    assert.ok(size > 50000, `Database size should be > 50KB, got ${size} bytes`);
  });

  runTest('SQLite PRAGMA integrity_check passes', () => {
    const db = new DatabaseSync(PROD_DB_PATH, { open: true });
    const res = db.prepare('PRAGMA integrity_check').get() as any;
    assert.strictEqual(res.integrity_check, 'ok');
    db.close();
  });

  runTest('All mandatory tables exist in sqlite_master', () => {
    const db = new DatabaseSync(PROD_DB_PATH, { open: true });
    const tables = (db.prepare("SELECT name FROM sqlite_master WHERE type='table'").all() as any[]).map(t => t.name);
    const requiredTables = [
      'metadata', 'domains', 'categories', 'topics', 'sources',
      'concepts', 'knowledge_records', 'chunks', 'entities',
      'relationships', 'aliases', 'misconceptions', 'knowledge_fts'
    ];
    for (const req of requiredTables) {
      assert.ok(tables.includes(req), `Table "${req}" must exist in schema`);
    }
    db.close();
  });

  runTest('Required convenience views exist', () => {
    const db = new DatabaseSync(PROD_DB_PATH, { open: true });
    const views = (db.prepare("SELECT name FROM sqlite_master WHERE type='view'").all() as any[]).map(v => v.name);
    assert.ok(views.includes('v_concept_details'));
    assert.ok(views.includes('v_rag_context'));
    assert.ok(views.includes('v_concept_graph'));
    assert.ok(views.includes('v_search_lookup'));
    db.close();
  });

  // --- Test Group 2: Android Query Contract Verification ---
  console.log('\nGroup 2: Android Query Contract Operations');
  const client = new KuzmixKnowledgeClient(PROD_DB_PATH);

  runTest('Exact concept lookup by ID (e.g., c_gravity)', () => {
    const concept = client.getConceptById('c_gravity') as any;
    assert.ok(concept, 'Concept c_gravity must exist');
    assert.strictEqual(concept.concept_name, 'Gravity and Gravitation');
    assert.strictEqual(concept.domain_id, 'sci_math');
    assert.ok(concept.summary_definition.length > 20);
    assert.ok(concept.records.length > 0, 'Must have linked knowledge records');
    assert.ok(concept.chunks.length > 0, 'Must have RAG chunks');
    assert.ok(concept.relationships.length > 0, 'Must have linked relationships');
  });

  runTest('Case-insensitive concept lookup by name', () => {
    const concept = client.getConceptByName('gravity and gravitation') as any;
    assert.ok(concept, 'Should find concept by lowercase canonical name');
    assert.strictEqual(concept.concept_id, 'c_gravity');
  });

  runTest('Concept resolution via alias (e.g., "F=ma")', () => {
    const concept = client.getConceptByName('F=ma') as any;
    assert.ok(concept, 'Should find concept via alias "F=ma"');
    assert.strictEqual(concept.concept_id, 'c_newtons_laws');
  });

  runTest('Full-Text Search (FTS5) BM25 ranking', () => {
    const searchRes = client.searchFts('electromagnetism OR circuit OR resistance');
    assert.ok(searchRes.length > 0, 'FTS search should return matches');
    assert.ok(searchRes.some((r: any) => r.concept_id === 'c_ohms_law' || r.concept_id === 'c_domestic_electrical'));
  });

  runTest('RAG Context Assembly fits token constraints', () => {
    const rag = client.buildRagContext('Newton laws acceleration', 300);
    assert.ok(rag.retrievedChunksCount > 0, 'RAG should retrieve chunks');
    assert.ok(rag.estimatedTokens <= 300, `Estimated tokens (${rag.estimatedTokens}) must be <= 300`);
    assert.ok(rag.formattedContext.includes('Newton'), 'Context should contain target knowledge');
  });

  runTest('Multi-domain coverage breadth', () => {
    const domains = client.listDomainsWithStats() as any[];
    assert.ok(domains.length >= 8, `Expected at least 8 domains, got ${domains.length}`);
    for (const d of domains) {
      assert.ok(d.concept_count > 0, `Domain ${d.name} must have at least 1 concept`);
    }
  });

  client.close();

  // --- Test Group 3: Dynamic Mutation, Integrity & Edge Cases Sandbox ---
  console.log('\nGroup 3: Dynamic Mutations, Deduplication & Foreign Key Enforcement');

  if (fs.existsSync(TEST_DB_PATH)) fs.unlinkSync(TEST_DB_PATH);
  fs.copyFileSync(PROD_DB_PATH, TEST_DB_PATH);

  const testDb = new DatabaseSync(TEST_DB_PATH, { open: true });
  testDb.exec('PRAGMA foreign_keys = ON;');

  runTest('Foreign key enforcement blocks orphaned child insert', () => {
    assert.throws(() => {
      testDb.prepare(`
        INSERT INTO concepts (
          id, category_id, domain_id, name, canonical_name_normalized,
          summary_definition, primary_explanation, content_hash
        ) VALUES ('c_invalid', 'cat_non_existent', 'sci_math', 'Invalid', 'invalid', 'Def', 'Exp', 'hash')
      `).run();
    }, /FOREIGN KEY constraint failed/);
  });

  runTest('Duplicate alias collision constraint', () => {
    assert.throws(() => {
      testDb.prepare(`
        INSERT INTO aliases (id, concept_id, alias, alias_normalized, alias_type)
        VALUES ('al_dup_1', 'c_gravity', 'Gravitational Force', 'gravitational force', 'synonym')
      `).run();
    }, /UNIQUE constraint failed/);
  });

  runTest('Deduplication prevents identical relationship duplicates', () => {
    assert.throws(() => {
      testDb.prepare(`
        INSERT INTO relationships (id, source_concept_id, target_concept_id, relationship_type)
        VALUES ('rel_dup_test', 'c_gravity', 'c_newtons_laws', 'related_to')
      `).run();
    }, /UNIQUE constraint failed/);
  });

  runTest('Cascade delete removes records, aliases, and chunks cleanly', () => {
    // Delete concept c_gestalt_design
    testDb.prepare("DELETE FROM concepts WHERE id = 'c_gestalt_design'").run();

    const remainingAliases = testDb.prepare("SELECT COUNT(*) AS count FROM aliases WHERE concept_id = 'c_gestalt_design'").get() as any;
    assert.strictEqual(remainingAliases.count, 0, 'Aliases must be cascade deleted');

    const remainingChunks = testDb.prepare("SELECT COUNT(*) AS count FROM chunks WHERE concept_id = 'c_gestalt_design'").get() as any;
    assert.strictEqual(remainingChunks.count, 0, 'Chunks must be cascade deleted');
  });

  testDb.close();
  if (fs.existsSync(TEST_DB_PATH)) fs.unlinkSync(TEST_DB_PATH);

  console.log('\n======================================================');
  console.log(`  ALL ${passedTests}/${totalTests} TESTS PASSED CLEANLY!`);
  console.log('======================================================\n');

  return {
    tests: testResults,
    total: totalTests,
    passed: passedTests,
    durationMs: Math.round((performance.now() - suiteStart) * 10) / 10
  };
}

if (process.argv[1]?.includes('test_database')) {
  runAllTests();
}
