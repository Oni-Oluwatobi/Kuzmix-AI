import { DatabaseSync } from 'node:sqlite';
import * as path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const DB_PATH = path.resolve(__dirname, '..', 'kuzmix_knowledge.sqlite');

export class KuzmixKnowledgeClient {
  private db: DatabaseSync;

  constructor(dbPath: string = DB_PATH) {
    this.db = new DatabaseSync(dbPath, { open: true });
    this.db.exec('PRAGMA foreign_keys = ON;');
  }

  getConceptById(id: string) {
    const stmt = this.db.prepare(`
      SELECT * FROM v_concept_details WHERE concept_id = ?
    `);
    const concept = stmt.get(id);
    if (!concept) return null;

    const aliases = this.db.prepare('SELECT alias, alias_type FROM aliases WHERE concept_id = ?').all(id);
    const misconceptions = this.db.prepare('SELECT misconception_text, correction_text, explanation FROM misconceptions WHERE concept_id = ?').all(id);
    const records = this.db.prepare('SELECT id, record_type, title, content FROM knowledge_records WHERE concept_id = ?').all(id);
    const chunks = this.db.prepare('SELECT chunk_index, chunk_type, text_content, token_estimate FROM chunks WHERE concept_id = ? ORDER BY chunk_index ASC').all(id);
    const relationships = this.db.prepare(`
      SELECT r.relationship_type, r.description, r.weight, tc.id AS target_id, tc.name AS target_name
      FROM relationships r
      JOIN concepts tc ON r.target_concept_id = tc.id
      WHERE r.source_concept_id = ?
    `).all(id);

    // Incoming edges (additive): what points AT this concept — used by the
    // dashboard to render "incoming" and "prerequisite" graph tags.
    const incoming_relationships = this.db.prepare(`
      SELECT r.relationship_type, r.description, r.weight,
             sc.id AS source_id, sc.name AS source_name
      FROM relationships r
      JOIN concepts sc ON r.source_concept_id = sc.id
      WHERE r.target_concept_id = ?
    `).all(id);

    return {
      ...concept,
      aliases,
      misconceptions,
      records,
      chunks,
      relationships,
      incoming_relationships,
      comparisons: this.getComparisons(id),
      procedures: this.getProcedures(id),
      disambiguations: this.getDisambiguationsForConcept(id)
    };
  }

  getConceptByName(name: string) {
    const normalized = name.toLowerCase().trim();
    // Try exact canonical match first
    const direct = this.db.prepare('SELECT id FROM concepts WHERE canonical_name_normalized = ?').get(normalized) as any;
    if (direct) return this.getConceptById(direct.id);

    // Try alias match
    const aliasMatch = this.db.prepare('SELECT concept_id FROM aliases WHERE alias_normalized = ?').get(normalized) as any;
    if (aliasMatch) return this.getConceptById(aliasMatch.concept_id);

    return null;
  }

  getDisambiguation(term: string) {
    const normalized = term.toLowerCase().trim();
    return this.db.prepare(`
      SELECT * FROM v_disambiguation_lookup
      WHERE term_normalized = ?
    `).all(normalized);
  }

  getDisambiguationsForConcept(conceptId: string) {
    return this.db.prepare(`
      SELECT term, distinguishing_context, disambiguation_hint
      FROM disambiguations
      WHERE concept_id = ?
    `).all(conceptId);
  }

  getComparisons(conceptId: string) {
    return this.db.prepare(`
      SELECT * FROM v_concept_comparisons
      WHERE concept_a_id = ? OR concept_b_id = ?
    `).all(conceptId, conceptId);
  }

  getProcedures(conceptId: string) {
    return this.db.prepare(`
      SELECT * FROM v_concept_procedures
      WHERE concept_id = ?
    `).all(conceptId);
  }

  searchFts(query: string, limit: number = 10, domainId?: string, categoryId?: string) {
    const rawTokens = query.replace(/[^\w\s]/g, ' ').trim().split(/\s+/).filter(Boolean);
    if (rawTokens.length === 0) return [];

    const isExplicitBoolean = /\b(OR|AND|NOT)\b/.test(query);

    let ftsQuery = '';
    if (isExplicitBoolean) {
      ftsQuery = query;
    } else {
      const STOP_WORDS = new Set([
        'what', 'is', 'how', 'does', 'do', 'the', 'a', 'an', 'in', 'of',
        'to', 'for', 'why', 'can', 'are', 'tell', 'me', 'about', 'explain'
      ]);
      const contentTokens = rawTokens.filter(t => !STOP_WORDS.has(t.toLowerCase()));
      const tokensToUse = contentTokens.length > 0 ? contentTokens : rawTokens;

      // Join terms with OR so multi-word queries match documents containing any term,
      // while BM25 naturally ranks documents containing all terms highest.
      ftsQuery = tokensToUse.map(t => `${t}*`).join(' OR ');
    }

    let sql = `
      SELECT 
        k.concept_id,
        k.title,
        k.category_name,
        k.domain_name,
        snippet(knowledge_fts, 1, '<b>', '</b>', '...', 12) AS snippet,
        bm25(knowledge_fts) AS rank
      FROM knowledge_fts k
      JOIN concepts c ON k.concept_id = c.id
      WHERE knowledge_fts MATCH ?
    `;
    const params: any[] = [ftsQuery];

    if (domainId) {
      sql += ' AND c.domain_id = ?';
      params.push(domainId);
    }
    if (categoryId) {
      sql += ' AND c.category_id = ?';
      params.push(categoryId);
    }

    sql += ' ORDER BY rank ASC LIMIT ?';
    params.push(limit);

    try {
      return this.db.prepare(sql).all(...params);
    } catch {
      // Fallback exact token search
      params[0] = rawTokens.join(' OR ');
      return this.db.prepare(sql).all(...params);
    }
  }

  buildRagContext(query: string, maxTokens: number = 400) {
    return this.buildSmarterRagContext(query, { maxTokens });
  }

  buildSmarterRagContext(query: string, options: {
    maxTokens?: number;
    maxChunksPerConcept?: number;
    includeMisconceptions?: boolean;
    domainHint?: string;
  } = {}) {
    const maxTokens = options.maxTokens ?? 400;
    const maxPerConcept = options.maxChunksPerConcept ?? 2;
    const isComparisonQuery = /\b(vs|versus|differ|difference|compare|contrasts?)\b/i.test(query);
    const isProcedureQuery = /\b(how to|steps?|procedure|protocol|algorithm|workflow)\b/i.test(query);

    // 1. Check for ambiguous single-word term
    const cleanWords = query.trim().split(/\s+/);
    let disambiguationNotice: string | null = null;
    if (cleanWords.length <= 2) {
      const disambiguations = this.getDisambiguation(cleanWords[0]);
      if (disambiguations.length > 1) {
        disambiguationNotice = `[Disambiguation Warning: "${cleanWords[0]}" has multiple meanings]: ` +
          disambiguations.map((d: any) => `${d.domain_name} (${d.distinguishing_context})`).join('; ');
      }
    }

    // 2. Perform FTS search
    const results = this.searchFts(query, 8);
    const retrievedConceptIds = Array.from(new Set(results.map((r: any) => r.concept_id)));

    // Best (most negative) BM25 rank per concept — additive: surfaced as a
    // per-chunk score in the dashboard's RAG simulator.
    const rankByConcept = new Map<string, number | null>();
    for (const r of results as any[]) {
      const prev = rankByConcept.get(r.concept_id);
      if (prev == null || (typeof r.rank === 'number' && r.rank < prev)) {
        rankByConcept.set(r.concept_id, typeof r.rank === 'number' ? r.rank : null);
      }
    }
    const scoreFor = (conceptId: string): number | null => rankByConcept.get(conceptId) ?? null;

    const contextChunks: any[] = [];
    let accumulatedTokens = 0;

    if (disambiguationNotice) {
      const noticeTokens = Math.ceil(disambiguationNotice.length / 4);
      accumulatedTokens += noticeTokens;
    }

    // 3. If comparison query, check comparisons table first
    if (isComparisonQuery) {
      for (const cid of retrievedConceptIds) {
        const comps = this.getComparisons(cid);
        for (const comp of comps as any[]) {
          const compText = `[Comparison Matrix: ${comp.title}]\nShared Context: ${comp.shared_aspect}\nCommon Confusion: ${comp.common_confusion}\nRule of Thumb: ${comp.rule_of_thumb}`;
          const tokens = Math.ceil(compText.length / 4);
          if (accumulatedTokens + tokens <= maxTokens) {
            contextChunks.push({
              chunk_id: `comp_${comp.comparison_id}`,
              concept_name: comp.title,
              header_context: `Structured Comparison > ${comp.title}`,
              text_content: compText,
              token_estimate: tokens,
              bm25_score: scoreFor(cid)
            });
            accumulatedTokens += tokens;
          }
          if (accumulatedTokens >= maxTokens * 0.7) break;
        }
        if (accumulatedTokens >= maxTokens * 0.7) break;
      }
    }

    // 4. If procedural query, check procedures table first
    if (isProcedureQuery) {
      for (const cid of retrievedConceptIds) {
        const procs = this.getProcedures(cid);
        for (const proc of procs as any[]) {
          const steps = JSON.parse(proc.steps_json || '[]');
          const stepsText = steps.slice(0, 4).map((s: any) => `Step ${s.step_number}: ${s.title} - ${s.instruction}`).join('\n');
          const procText = `[Procedure: ${proc.title}]\nGoal: ${proc.goal}\n${stepsText}\nVerification: ${proc.verification_step}`;
          const tokens = Math.ceil(procText.length / 4);
          if (accumulatedTokens + tokens <= maxTokens) {
            contextChunks.push({
              chunk_id: `proc_${proc.procedure_id}`,
              concept_name: proc.title,
              header_context: `Standard Procedure > ${proc.title}`,
              text_content: procText,
              token_estimate: tokens,
              bm25_score: scoreFor(cid)
            });
            accumulatedTokens += tokens;
          }
          if (accumulatedTokens >= maxTokens * 0.7) break;
        }
        if (accumulatedTokens >= maxTokens * 0.7) break;
      }
    }

    // 5. Retrieve diverse concept chunks with max per concept
    for (const cid of retrievedConceptIds) {
      const chunks = this.db.prepare(`
        SELECT chunk_id, concept_name, domain_name, category_name, header_context, text_content, token_estimate
        FROM v_rag_context
        WHERE concept_id = ?
        ORDER BY chunk_index ASC
      `).all(cid);

      let conceptChunkCount = 0;
      for (const ch of chunks as any[]) {
        if (conceptChunkCount >= maxPerConcept) break;
        if (accumulatedTokens + ch.token_estimate > maxTokens) break;

        contextChunks.push({ ...ch, bm25_score: scoreFor(cid) });
        accumulatedTokens += ch.token_estimate;
        conceptChunkCount++;
      }
      if (accumulatedTokens >= maxTokens) break;
    }

    let formattedContext = contextChunks.map((c, i) => 
      `[Fact ${i + 1} | ${c.header_context}]\n${c.text_content}`
    ).join('\n\n');

    if (disambiguationNotice) {
      formattedContext = `${disambiguationNotice}\n\n${formattedContext}`;
    }

    return {
      query,
      retrievedChunksCount: contextChunks.length,
      estimatedTokens: accumulatedTokens,
      formattedContext,
      chunks: contextChunks,
      disambiguationNotice
    };
  }

  listDomainsWithStats() {
    return this.db.prepare(`
      SELECT 
        d.id, d.name, d.slug, d.description, d.icon,
        COUNT(DISTINCT cat.id) AS category_count,
        COUNT(DISTINCT c.id) AS concept_count
      FROM domains d
      LEFT JOIN categories cat ON d.id = cat.domain_id
      LEFT JOIN concepts c ON d.id = c.domain_id
      GROUP BY d.id
      ORDER BY d.sort_order ASC
    `).all();
  }

  close() {
    this.db.close();
  }
}

// CLI runner if invoked directly
if (process.argv[1]?.includes('query_cli')) {
  const client = new KuzmixKnowledgeClient();
  const query = process.argv[2] || 'gravity';
  console.log(`\n--- Kuzmix FTS Search: "${query}" ---`);
  const results = client.searchFts(query);
  console.log(results);

  console.log(`\n--- RAG Context Simulation for "${query}" ---`);
  const rag = client.buildRagContext(query);
  console.log(rag.formattedContext);
  client.close();
}
