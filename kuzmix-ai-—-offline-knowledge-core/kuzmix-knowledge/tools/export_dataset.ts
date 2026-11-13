import { DatabaseSync } from 'node:sqlite';
import * as fs from 'node:fs';
import * as path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const DB_PATH = path.resolve(__dirname, '..', 'kuzmix_knowledge.sqlite');
const EXPORT_DIR = path.resolve(__dirname, '..', 'exports');

export function exportDatabaseToJson(outputPath?: string) {
  const db = new DatabaseSync(DB_PATH, { open: true });
  const targetPath = outputPath || path.join(EXPORT_DIR, 'kuzmix_knowledge_export.json');

  if (!fs.existsSync(EXPORT_DIR)) {
    fs.mkdirSync(EXPORT_DIR, { recursive: true });
  }

  console.log(`\nExporting SQLite database to JSON: ${targetPath}`);

  const metadata = db.prepare('SELECT key, value FROM metadata').all();
  const domains = db.prepare('SELECT * FROM domains ORDER BY sort_order ASC').all();
  const categories = db.prepare('SELECT * FROM categories ORDER BY sort_order ASC').all();
  const topics = db.prepare('SELECT * FROM topics').all();
  const concepts = db.prepare('SELECT * FROM concepts').all();
  const records = db.prepare('SELECT * FROM knowledge_records').all();
  const chunks = db.prepare('SELECT * FROM chunks ORDER BY concept_id, chunk_index ASC').all();
  const relationships = db.prepare('SELECT * FROM relationships').all();
  const aliases = db.prepare('SELECT * FROM aliases').all();
  const misconceptions = db.prepare('SELECT * FROM misconceptions').all();
  const entities = db.prepare('SELECT * FROM entities').all();
  const sources = db.prepare('SELECT * FROM sources').all();

  const exportPayload = {
    exported_at: new Date().toISOString(),
    source_database: 'kuzmix_knowledge.sqlite',
    counts: {
      domains: domains.length,
      categories: categories.length,
      topics: topics.length,
      concepts: concepts.length,
      records: records.length,
      chunks: chunks.length,
      relationships: relationships.length,
      aliases: aliases.length,
      misconceptions: misconceptions.length,
      entities: entities.length,
      sources: sources.length
    },
    metadata,
    domains,
    categories,
    topics,
    sources,
    concepts,
    records,
    chunks,
    relationships,
    aliases,
    misconceptions,
    entities
  };

  fs.writeFileSync(targetPath, JSON.stringify(exportPayload, null, 2), 'utf8');
  const sizeMB = (fs.statSync(targetPath).size / (1024 * 1024)).toFixed(2);
  console.log(`✓ Export completed: ${targetPath} (${sizeMB} MB)`);

  db.close();
  return exportPayload;
}

if (process.argv[1]?.includes('export_dataset')) {
  exportDatabaseToJson();
}
