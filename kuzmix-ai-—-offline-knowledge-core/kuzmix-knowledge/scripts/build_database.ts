import { execSync } from 'node:child_process';
import * as fs from 'node:fs';
import * as path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const ROOT_DIR = path.resolve(__dirname, '..');
const DB_PATH = path.join(ROOT_DIR, 'kuzmix_knowledge.sqlite');
const PY_BUILDER = path.join(__dirname, 'build_sqlite.py');

export function buildDatabase() {
  console.log('Compiling Kuzmix AI SQLite database with high-performance builder...');
  const output = execSync(`python3 "${PY_BUILDER}"`, { stdio: 'inherit' });

  const stats = fs.statSync(DB_PATH);
  return {
    fileSizeKB: (stats.size / 1024).toFixed(2),
    fileSizeBytes: stats.size,
    dbPath: DB_PATH
  };
}

buildDatabase();
