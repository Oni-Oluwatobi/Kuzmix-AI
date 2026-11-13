import { DatabaseSync } from 'node:sqlite';
import * as fs from 'node:fs';
import * as path from 'node:path';
import * as crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const DB_PATH = path.resolve(__dirname, '..', 'kuzmix_knowledge.sqlite');

function sha256(content: string): string {
  return crypto.createHash('sha256').update(content).digest('hex');
}

export interface KnowledgePack {
  pack_id: string;
  pack_name: string;
  version: string;
  author?: string;
  domains?: Array<{ id: string; name: string; slug: string; description?: string; icon?: string; sort_order?: number }>;
  categories?: Array<{ id: string; domain_id: string; name: string; slug: string; description?: string; sort_order?: number }>;
  topics?: Array<{ id: string; category_id: string; name: string; slug: string; description?: string }>;
  sources?: Array<{ id: string; title: string; author_or_org?: string; source_type: string; uri_or_reference?: string }>;
  concepts: Array<{
    id: string;
    domain_id: string;
    category_id: string;
    topic_id?: string;
    name: string;
    summary_definition: string;
    primary_explanation: string;
    key_properties?: any[];
    applications?: string[];
    prerequisites?: string[];
    difficulty_level?: 'introductory' | 'intermediate' | 'advanced' | 'expert';
    confidence_score?: number;
    provenance_type?: string;
    source_id?: string;
    aliases?: Array<{ alias: string; alias_type?: string }>;
    misconceptions?: Array<{ misconception_text: string; correction_text: string; explanation: string }>;
    records?: Array<{ id: string; record_type: string; title: string; content: string; structured_payload?: any }>;
    chunks?: Array<{ chunk_index: number; chunk_type: string; text_content: string; token_estimate?: number; header_context?: string }>;
    entities?: Array<{ id: string; entity_type: string; name: string; description: string; attributes?: any }>;
    relationships?: Array<{ target_concept_id: string; relationship_type: string; description?: string; weight?: number }>;
  }>;
}

export function importKnowledgePack(packPath: string, dbPath: string = DB_PATH) {
  console.log(`\n--- Importing Knowledge Pack: ${packPath} ---`);
  if (!fs.existsSync(packPath)) {
    throw new Error(`Knowledge pack file not found at ${packPath}`);
  }

  const raw = fs.readFileSync(packPath, 'utf8');
  const pack: KnowledgePack = JSON.parse(raw);

  const db = new DatabaseSync(dbPath, { open: true });
  db.exec('PRAGMA foreign_keys = ON;');
  db.exec('PRAGMA journal_mode = WAL;');

  // Prepared statements
  const insertDomain = db.prepare(`
    INSERT INTO domains (id, name, slug, description, icon, sort_order)
    VALUES (?, ?, ?, ?, ?, ?)
    ON CONFLICT(id) DO UPDATE SET name = excluded.name, description = excluded.description
  `);

  const insertCategory = db.prepare(`
    INSERT INTO categories (id, domain_id, name, slug, description, sort_order)
    VALUES (?, ?, ?, ?, ?, ?)
    ON CONFLICT(id) DO UPDATE SET name = excluded.name, description = excluded.description
  `);

  const insertConcept = db.prepare(`
    INSERT INTO concepts (
      id, topic_id, category_id, domain_id, name, canonical_name_normalized,
      summary_definition, primary_explanation, key_properties_json, applications_json,
      prerequisites_json, difficulty_level, confidence_score, provenance_type,
      source_id, content_hash
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT(id) DO UPDATE SET
      summary_definition = excluded.summary_definition,
      primary_explanation = excluded.primary_explanation,
      content_hash = excluded.content_hash,
      updated_at = strftime('%Y-%m-%dT%H:%M:%SZ', 'now')
  `);

  const insertFts = db.prepare(`
    INSERT INTO knowledge_fts (concept_id, record_id, title, content, aliases, category_name, domain_name, keywords)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
  `);

  let addedConcepts = 0;
  let updatedConcepts = 0;

  // Transaction
  db.exec('BEGIN TRANSACTION;');
  try {
    // Domains
    if (pack.domains) {
      for (const d of pack.domains) {
        insertDomain.run(d.id, d.name, d.slug, d.description || '', d.icon || 'BookOpen', d.sort_order || 0);
      }
    }

    // Categories
    if (pack.categories) {
      for (const c of pack.categories) {
        insertCategory.run(c.id, c.domain_id, c.name, c.slug, c.description || '', c.sort_order || 0);
      }
    }

    // Concepts
    for (const c of pack.concepts) {
      const canonicalNorm = c.name.toLowerCase().trim();
      const contentHash = sha256(`${c.name}:${c.summary_definition}:${c.primary_explanation}`);

      // Check if existing
      const existing = db.prepare('SELECT content_hash FROM concepts WHERE id = ?').get(c.id) as any;
      if (existing) {
        if (existing.content_hash === contentHash) {
          // Identical duplicate, skip
          continue;
        }
        updatedConcepts++;
      } else {
        addedConcepts++;
      }

      insertConcept.run(
        c.id,
        c.topic_id || null,
        c.category_id,
        c.domain_id,
        c.name,
        canonicalNorm,
        c.summary_definition,
        c.primary_explanation,
        c.key_properties ? JSON.stringify(c.key_properties) : null,
        c.applications ? JSON.stringify(c.applications) : null,
        c.prerequisites ? JSON.stringify(c.prerequisites) : null,
        c.difficulty_level || 'introductory',
        c.confidence_score !== undefined ? c.confidence_score : 1.0,
        c.provenance_type || 'curated_scientific',
        c.source_id || null,
        contentHash
      );

      // FTS indexing for concept
      insertFts.run(
        c.id,
        null,
        c.name,
        `${c.summary_definition} ${c.primary_explanation}`,
        (c.aliases || []).map(a => a.alias).join(' '),
        '',
        '',
        (c.applications || []).join(' ')
      );
    }

    db.exec('COMMIT;');

    // Keep the metadata counters truthful after adding/updating rows
    // (they are stamped by build_sqlite.py at build time and would
    // otherwise silently drift after every pack import).
    const totalConcepts = (db.prepare('SELECT COUNT(*) AS n FROM concepts').get() as any).n as number;
    const totalFts = (db.prepare('SELECT COUNT(*) AS n FROM knowledge_fts').get() as any).n as number;
    const refreshCounter = db.prepare(`
      INSERT INTO metadata (key, value)
      VALUES (?, ?)
      ON CONFLICT(key) DO UPDATE SET
        value = excluded.value,
        updated_at = strftime('%Y-%m-%dT%H:%M:%SZ', 'now')
    `);
    refreshCounter.run('total_concepts', String(totalConcepts));
    refreshCounter.run('total_fts_entries', String(totalFts));

    console.log(`✓ Pack "${pack.pack_name}" (v${pack.version}) imported successfully!`);
    console.log(`  Added concepts: ${addedConcepts}`);
    console.log(`  Updated concepts: ${updatedConcepts}`);
    console.log(`  metadata.total_concepts: ${totalConcepts}`);
    console.log(`  metadata.total_fts_entries: ${totalFts}`);
  } catch (err: any) {
    db.exec('ROLLBACK;');
    console.error(`✗ Import failed, transaction rolled back: ${err.message}`);
    throw err;
  } finally {
    db.close();
  }

  return { addedConcepts, updatedConcepts };
}

if (process.argv[1]?.includes('import_pack')) {
  const packFile = process.argv[2];
  if (!packFile) {
    console.log('Usage: npx tsx tools/import_pack.ts <path-to-pack.json>');
    process.exit(1);
  }
  importKnowledgePack(packFile);
}
