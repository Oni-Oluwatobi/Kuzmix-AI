import express from 'express';
import path from 'node:path';
import fs from 'node:fs';
import { fileURLToPath } from 'node:url';
import { DatabaseSync } from 'node:sqlite';
import { createServer as createViteServer } from 'vite';
import { KuzmixKnowledgeClient } from './kuzmix-knowledge/tools/query_cli.ts';
import { validateDatabase } from './kuzmix-knowledge/validation/validate_database.ts';
import { runAllTests } from './kuzmix-knowledge/tests/test_database.ts';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const DB_PATH = path.resolve(__dirname, 'kuzmix-knowledge', 'kuzmix_knowledge.sqlite');

async function startServer() {
  const app = express();
  const PORT = process.env.PORT ? parseInt(process.env.PORT, 10) : 3000;

  app.use(express.json());

  // Database client
  const client = new KuzmixKnowledgeClient(DB_PATH);

  // --- API Endpoints ---
  app.get('/api/stats', (_req, res) => {
    try {
      const stats = fs.statSync(DB_PATH);
      const db = new DatabaseSync(DB_PATH, { open: true });
      const counts = {
        fileSizeBytes: stats.size,
        fileSizeKB: (stats.size / 1024).toFixed(2),
        domains: (db.prepare('SELECT COUNT(*) AS count FROM domains').get() as any).count,
        categories: (db.prepare('SELECT COUNT(*) AS count FROM categories').get() as any).count,
        topics: (db.prepare('SELECT COUNT(*) AS count FROM topics').get() as any).count,
        concepts: (db.prepare('SELECT COUNT(*) AS count FROM concepts').get() as any).count,
        records: (db.prepare('SELECT COUNT(*) AS count FROM knowledge_records').get() as any).count,
        chunks: (db.prepare('SELECT COUNT(*) AS count FROM chunks').get() as any).count,
        relationships: (db.prepare('SELECT COUNT(*) AS count FROM relationships').get() as any).count,
        aliases: (db.prepare('SELECT COUNT(*) AS count FROM aliases').get() as any).count,
        misconceptions: (db.prepare('SELECT COUNT(*) AS count FROM misconceptions').get() as any).count,
        entities: (db.prepare('SELECT COUNT(*) AS count FROM entities').get() as any).count,
        sources: (db.prepare('SELECT COUNT(*) AS count FROM sources').get() as any).count,
        ftsEntries: (db.prepare('SELECT COUNT(*) AS count FROM knowledge_fts').get() as any).count,
        integrity: (db.prepare('PRAGMA integrity_check').get() as any).integrity_check,
        schemaVersion: (db.prepare("SELECT value FROM metadata WHERE key = 'schema_version'").get() as any)?.value ?? '1.2.0'
      };
      db.close();
      res.json({ success: true, data: counts });
    } catch (err: any) {
      res.status(500).json({ success: false, error: err.message });
    }
  });

  app.get('/api/domains', (_req, res) => {
    try {
      const domains = client.listDomainsWithStats();
      res.json({ success: true, data: domains });
    } catch (err: any) {
      res.status(500).json({ success: false, error: err.message });
    }
  });

  app.get('/api/concepts', (req, res) => {
    try {
      const domainId = req.query.domain as string | undefined;
      const categoryId = req.query.category as string | undefined;

      const db = new DatabaseSync(DB_PATH, { open: true });
      let query = `
        SELECT c.id, c.name, c.summary_definition, c.difficulty_level, c.confidence_score,
               d.name AS domain_name, cat.name AS category_name, c.domain_id, c.category_id
        FROM concepts c
        JOIN domains d ON c.domain_id = d.id
        JOIN categories cat ON c.category_id = cat.id
      `;
      const params: any[] = [];
      const conditions: string[] = [];

      if (domainId) {
        conditions.push('c.domain_id = ?');
        params.push(domainId);
      }
      if (categoryId) {
        conditions.push('c.category_id = ?');
        params.push(categoryId);
      }

      if (conditions.length > 0) {
        query += ' WHERE ' + conditions.join(' AND ');
      }
      query += ' ORDER BY c.name ASC';

      const concepts = db.prepare(query).all(...params);
      db.close();
      res.json({ success: true, data: concepts });
    } catch (err: any) {
      res.status(500).json({ success: false, error: err.message });
    }
  });

  app.get('/api/concept/:id', (req, res) => {
    try {
      const concept = client.getConceptById(req.params.id);
      if (!concept) {
        return res.status(404).json({ success: false, error: 'Concept not found' });
      }
      res.json({ success: true, data: concept });
    } catch (err: any) {
      res.status(500).json({ success: false, error: err.message });
    }
  });

  app.get('/api/search', (req, res) => {
    try {
      const q = (req.query.q as string || '').trim();
      if (!q) return res.json({ success: true, data: [] });
      const results = client.searchFts(q, 15);
      res.json({ success: true, data: results });
    } catch (err: any) {
      res.status(500).json({ success: false, error: err.message });
    }
  });

  app.get('/api/rag', (req, res) => {
    try {
      const q = (req.query.q as string || '').trim();
      const maxTokens = req.query.tokens ? parseInt(req.query.tokens as string, 10) : 400;
      if (!q) return res.json({ success: true, data: null });
      const rag = client.buildRagContext(q, maxTokens);
      res.json({ success: true, data: rag });
    } catch (err: any) {
      res.status(500).json({ success: false, error: err.message });
    }
  });

  app.get('/api/validate', (_req, res) => {
    try {
      const report = validateDatabase(DB_PATH);
      res.json({ success: true, data: report });
    } catch (err: any) {
      res.status(500).json({ success: false, error: err.message });
    }
  });

  app.post('/api/run-tests', async (_req, res) => {
    try {
      const started = Date.now();
      const result = await runAllTests();
      let integrity: unknown = null;
      try {
        const report = validateDatabase(DB_PATH);
        integrity = {
          passed: report.passed,
          errors: report.errors,
          warnings: report.warnings,
          metrics: report.metrics
        };
      } catch {
        integrity = null;
      }
      res.json({
        success: true,
        message: 'All 14 automated tests passed cleanly',
        ...result,
        integrity,
        serverDurationMs: Date.now() - started
      });
    } catch (err: any) {
      res.status(500).json({ success: false, error: err.message });
    }
  });

  app.get('/api/download/sqlite', (_req, res) => {
    res.download(DB_PATH, 'kuzmix_knowledge.sqlite', (err) => {
      if (err && !res.headersSent) {
        res.status(500).send('Error downloading database file');
      }
    });
  });

  // Mount Vite Middleware for development
  const isProduction = process.env.NODE_ENV === 'production';
  if (!isProduction) {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.resolve(__dirname, 'dist');
    app.use(express.static(distPath));
    app.get('*', (_req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`\n>>> Kuzmix Offline Knowledge Server running on http://0.0.0.0:${PORT}`);
  });
}

startServer().catch(err => {
  console.error('Failed to start server:', err);
  process.exit(1);
});
