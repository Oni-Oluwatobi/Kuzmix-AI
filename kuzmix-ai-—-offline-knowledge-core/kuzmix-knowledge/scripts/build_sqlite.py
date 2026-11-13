#!/usr/bin/env python3
"""
Kuzmix AI Offline Knowledge Database Builder
Compiles all schemas, taxonomy tables, 2,570+ concepts, FTS5 index, chunks, aliases,
misconceptions, and relationships deterministically into kuzmix_knowledge.sqlite.
"""

import os
import sys
import glob
import json
import time
import shutil
import sqlite3
import hashlib

def sha256(text: str) -> str:
    return hashlib.sha256(text.encode('utf-8')).hexdigest()

def build_database():
    start_time = time.time()
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    schema_dir = os.path.join(base_dir, 'schema')
    data_dir = os.path.join(base_dir, 'data')
    concepts_dir = os.path.join(data_dir, 'concepts')
    
    target_db = os.path.join(base_dir, 'kuzmix_knowledge.sqlite')
    root_target_db = os.path.join(os.path.dirname(base_dir), 'kuzmix_knowledge.sqlite')
    tmp_db = '/tmp/kuzmix_knowledge_building.sqlite'

    print("====================================================")
    print("  KUZMIX AI - OFFLINE KNOWLEDGE DATABASE BUILDER")
    print("====================================================")
    print(f"Building temporary database at: {tmp_db}")

    # Clean any previous temp files
    for f in [tmp_db, tmp_db + '-shm', tmp_db + '-wal']:
        if os.path.exists(f):
            os.remove(f)

    conn = sqlite3.connect(tmp_db)
    conn.isolation_level = None  # Autocommit control
    cursor = conn.cursor()

    # Pragmas for fast, reliable ingestion
    cursor.execute('PRAGMA page_size = 4096;')
    cursor.execute('PRAGMA foreign_keys = ON;')
    cursor.execute('PRAGMA journal_mode = WAL;')
    cursor.execute('PRAGMA synchronous = NORMAL;')

    # 1. Apply Schema DDLs
    print("\nApplying schema DDL files...")
    schema_files = [
        '001_initial_schema.sql',
        '002_fts_and_indexes.sql',
        '003_views_and_contracts.sql'
    ]
    for sfile in schema_files:
        path = os.path.join(schema_dir, sfile)
        with open(path, 'r', encoding='utf-8') as f:
            cursor.executescript(f.read())
        print(f"  ✓ Applied {sfile}")

    cursor.execute('BEGIN TRANSACTION;')

    # 2. Ingest Sources
    with open(os.path.join(data_dir, 'sources.json'), 'r', encoding='utf-8') as f:
        sources = json.load(f)
    for s in sources:
        cursor.execute(
            '''INSERT INTO sources (id, title, author_or_org, source_type, uri_or_reference, license, notes)
               VALUES (?, ?, ?, ?, ?, ?, ?)''',
            (s['id'], s['title'], s.get('author_or_org'), s['source_type'], s.get('uri_or_reference'), s.get('license'), s.get('notes'))
        )
    print(f"\n✓ Ingested {len(sources)} sources")

    # 3. Ingest Domains
    with open(os.path.join(data_dir, 'domains.json'), 'r', encoding='utf-8') as f:
        domains = json.load(f)
    for d in domains:
        cursor.execute(
            '''INSERT INTO domains (id, name, slug, description, icon, sort_order)
               VALUES (?, ?, ?, ?, ?, ?)''',
            (d['id'], d['name'], d['slug'], d['description'], d.get('icon'), d['sort_order'])
        )
    domain_map = {d['id']: d['name'] for d in domains}
    print(f"✓ Ingested {len(domains)} domains")

    # 4. Ingest Categories
    with open(os.path.join(data_dir, 'categories.json'), 'r', encoding='utf-8') as f:
        categories = json.load(f)
    for c in categories:
        cursor.execute(
            '''INSERT INTO categories (id, domain_id, name, slug, description, sort_order)
               VALUES (?, ?, ?, ?, ?, ?)''',
            (c['id'], c['domain_id'], c['name'], c['slug'], c['description'], c['sort_order'])
        )
    category_map = {c['id']: c['name'] for c in categories}
    print(f"✓ Ingested {len(categories)} categories")

    # 5. Ingest Topics
    with open(os.path.join(data_dir, 'topics.json'), 'r', encoding='utf-8') as f:
        topics = json.load(f)
    for t in topics:
        cursor.execute(
            '''INSERT INTO topics (id, category_id, name, slug, description)
               VALUES (?, ?, ?, ?, ?)''',
            (t['id'], t['category_id'], t['name'], t['slug'], t.get('description'))
        )
    print(f"✓ Ingested {len(topics)} topics")

    # 6. Ingest Concepts across all JSON files
    concept_files = sorted(glob.glob(os.path.join(concepts_dir, '*.json')))
    total_concepts = 0
    total_records = 0
    total_chunks = 0
    total_aliases = 0
    total_misconceptions = 0
    total_entities = 0
    total_fts_entries = 0
    delayed_relationships = []
    prereq_edges = []
    seen_concept_ids = set()

    for cfile in concept_files:
        with open(cfile, 'r', encoding='utf-8') as f:
            concepts = json.load(f)
        fname = os.path.basename(cfile)
        print(f"  Ingesting {len(concepts)} concepts from {fname}...")

        for c in concepts:
            cid = c['id']
            if cid in seen_concept_ids:
                continue
            seen_concept_ids.add(cid)

            canonical_norm = c['name'].lower().strip()
            content_hash = sha256(f"{c['name']}:{c['summary_definition']}:{c['primary_explanation']}")

            cursor.execute('''
                INSERT INTO concepts (
                    id, topic_id, category_id, domain_id, name, canonical_name_normalized,
                    summary_definition, primary_explanation, key_properties_json, applications_json,
                    prerequisites_json, difficulty_level, confidence_score, provenance_type,
                    source_id, content_hash
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (
                cid,
                c.get('topic_id'),
                c['category_id'],
                c['domain_id'],
                c['name'],
                canonical_norm,
                c['summary_definition'],
                c['primary_explanation'],
                json.dumps(c.get('key_properties')) if c.get('key_properties') else None,
                json.dumps(c.get('applications')) if c.get('applications') else None,
                json.dumps(c.get('prerequisites')) if c.get('prerequisites') else None,
                c.get('difficulty_level', 'introductory'),
                c.get('confidence_score', 1.0),
                c.get('provenance_type', 'curated_scientific'),
                c.get('source_id', 'src_feynman_physics'),
                content_hash
            ))
            total_concepts += 1

            # Aliases
            alias_texts = []
            for i, a in enumerate(c.get('aliases', [])):
                cursor.execute(
                    '''INSERT INTO aliases (id, concept_id, alias, alias_normalized, alias_type)
                       VALUES (?, ?, ?, ?, ?)''',
                    (f"al_{cid}_{i}", cid, a['alias'], a['alias'].lower().strip(), a.get('alias_type', 'synonym'))
                )
                alias_texts.append(a['alias'])
                total_aliases += 1

            # Misconceptions
            for i, m in enumerate(c.get('misconceptions', [])):
                cursor.execute(
                    '''INSERT INTO misconceptions (id, concept_id, misconception_text, correction_text, explanation)
                       VALUES (?, ?, ?, ?, ?)''',
                    (f"misc_{cid}_{i}", cid, m['misconception_text'], m['correction_text'], m.get('explanation', ''))
                )
                total_misconceptions += 1

            # Knowledge Records
            for r in c.get('records', []):
                rhash = sha256(f"{r['title']}:{r['content']}")
                cursor.execute('''
                    INSERT INTO knowledge_records (
                        id, concept_id, record_type, title, content, structured_payload_json,
                        confidence_score, provenance_type, source_id, content_hash
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ''', (
                    r['id'], cid, r['record_type'], r['title'], r['content'],
                    json.dumps(r.get('structured_payload')) if r.get('structured_payload') else None,
                    r.get('confidence_score', 1.0),
                    r.get('provenance_type', 'curated_scientific'),
                    r.get('source_id') or c.get('source_id'),
                    rhash
                ))
                total_records += 1

                # Record FTS entry
                cursor.execute('''
                    INSERT INTO knowledge_fts (concept_id, record_id, title, content, aliases, category_name, domain_name, keywords)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ''', (
                    cid, r['id'], r['title'], r['content'], '',
                    category_map.get(c['category_id'], ''),
                    domain_map.get(c['domain_id'], ''),
                    r['record_type']
                ))
                total_fts_entries += 1

            # Chunks
            for ch in c.get('chunks', []):
                cursor.execute('''
                    INSERT INTO chunks (id, concept_id, chunk_index, chunk_type, text_content, token_estimate, header_context)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                ''', (
                    f"chk_{cid}_{ch['chunk_index']}", cid, ch['chunk_index'], ch['chunk_type'],
                    ch['text_content'], ch.get('token_estimate', max(1, len(ch['text_content']) // 4)),
                    ch.get('header_context', f"{domain_map.get(c['domain_id'])} > {category_map.get(c['category_id'])} > {c['name']}")
                ))
                total_chunks += 1

            # Entities
            for ent in c.get('entities', []):
                cursor.execute('''
                    INSERT INTO entities (id, concept_id, entity_type, name, name_normalized, description, attributes_json)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                ''', (
                    ent['id'], cid, ent['entity_type'], ent['name'], ent['name'].lower().strip(),
                    ent.get('description', ''), json.dumps(ent.get('attributes')) if ent.get('attributes') else None
                ))
                total_entities += 1

            # Delayed relationships
            for rel in c.get('relationships', []):
                delayed_relationships.append((
                    cid, rel['target_concept_id'], rel['relationship_type'],
                    rel.get('description', ''), rel.get('weight', 1.0), 1 if rel.get('is_bidirectional') else 0
                ))

            # Prerequisites -> prerequisite_of edges (source = prerequisite)
            for p in (c.get('prerequisites') or []):
                prereq_edges.append((p, cid, c['name']))

            # Primary Concept FTS Entry
            cursor.execute('''
                INSERT INTO knowledge_fts (concept_id, record_id, title, content, aliases, category_name, domain_name, keywords)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ''', (
                cid, None, c['name'], f"{c['summary_definition']} {c['primary_explanation']}",
                ' '.join(alias_texts),
                category_map.get(c['category_id'], ''),
                domain_map.get(c['domain_id'], ''),
                ' '.join(c.get('applications', []))
            ))
            total_fts_entries += 1

    # 7. Insert Relationships (delayed second pass + cross-domain links)
    # Load cross-domain links
    cross_links_path = os.path.join(data_dir, 'cross_domain_links.json')
    if os.path.exists(cross_links_path):
        with open(cross_links_path, 'r', encoding='utf-8') as f:
            for cl in json.load(f):
                delayed_relationships.append((
                    cl["source_concept_id"], cl["target_concept_id"], cl["relationship_type"],
                    cl.get("description", ""), cl.get("weight", 0.95), 0
                ))

    # Materialize prerequisites as prerequisite_of edges
    for p, cid, cname in prereq_edges:
        delayed_relationships.append((
            p, cid, 'prerequisite_of',
            f'Prerequisite concept for {cname}', 0.95, 0
        ))

    print(f"\nInserting {len(delayed_relationships)} semantic relationships...")
    total_relationships = 0
    rel_seen = set()
    for idx, (src, tgt, rtype, desc, wt, bidi) in enumerate(delayed_relationships):
        # Enforce target exists and uniqueness
        if tgt not in seen_concept_ids or src not in seen_concept_ids:
            continue
        rel_key = (src, tgt, rtype)
        if rel_key in rel_seen:
            continue
        rel_seen.add(rel_key)

        cursor.execute('''
            INSERT INTO relationships (id, source_concept_id, target_concept_id, relationship_type, description, weight, is_bidirectional)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        ''', (f"rel_{src}_{tgt}_{idx}", src, tgt, rtype, desc, wt, bidi))
        total_relationships += 1

    # 8. Ingest Disambiguations
    total_disambiguations = 0
    dis_path = os.path.join(data_dir, 'disambiguations.json')
    if os.path.exists(dis_path):
        with open(dis_path, 'r', encoding='utf-8') as f:
            disambiguations = json.load(f)
        for dis in disambiguations:
            if dis['concept_id'] in seen_concept_ids:
                cursor.execute('''
                    INSERT INTO disambiguations (id, term, term_normalized, concept_id, domain_id, category_id, distinguishing_context, disambiguation_hint, sort_priority)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ''', (
                    dis['id'], dis['term'], dis['term_normalized'], dis['concept_id'],
                    dis['domain_id'], dis['category_id'], dis['distinguishing_context'],
                    dis['disambiguation_hint'], dis.get('sort_priority', 0)
                ))
                total_disambiguations += 1

                # Disambiguation FTS entry for instant discovery
                cursor.execute('''
                    INSERT INTO knowledge_fts (concept_id, record_id, title, content, aliases, category_name, domain_name, keywords)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ''', (
                    dis['concept_id'], None, f"Disambiguation: {dis['term']}",
                    f"{dis['distinguishing_context']} {dis['disambiguation_hint']}",
                    dis['term'], category_map.get(dis['category_id'], ''),
                    domain_map.get(dis['domain_id'], ''), "disambiguation polysemy"
                ))
                total_fts_entries += 1
    print(f"✓ Ingested {total_disambiguations} disambiguation entries")

    # 9. Ingest Comparisons
    total_comparisons = 0
    comp_path = os.path.join(data_dir, 'comparisons.json')
    if os.path.exists(comp_path):
        with open(comp_path, 'r', encoding='utf-8') as f:
            comparisons = json.load(f)
        for comp in comparisons:
            if comp['concept_a_id'] in seen_concept_ids and comp['concept_b_id'] in seen_concept_ids:
                cursor.execute('''
                    INSERT INTO comparisons (id, concept_a_id, concept_b_id, title, shared_aspect, key_differences_json, common_confusion, rule_of_thumb)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ''', (
                    comp['id'], comp['concept_a_id'], comp['concept_b_id'], comp['title'],
                    comp['shared_aspect'], comp['key_differences_json'], comp['common_confusion'],
                    comp['rule_of_thumb']
                ))
                total_comparisons += 1

                # Comparison FTS entry
                cursor.execute('''
                    INSERT INTO knowledge_fts (concept_id, record_id, title, content, aliases, category_name, domain_name, keywords)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ''', (
                    comp['concept_a_id'], None, f"Comparison: {comp['title']}",
                    f"{comp['shared_aspect']} {comp['common_confusion']} Rule: {comp['rule_of_thumb']}",
                    comp['title'], "Comparisons", "Multi-Domain", "comparison vs differences"
                ))
                total_fts_entries += 1
    print(f"✓ Ingested {total_comparisons} structured comparisons")

    # 10. Ingest Procedures
    total_procedures = 0
    proc_path = os.path.join(data_dir, 'procedures.json')
    if os.path.exists(proc_path):
        with open(proc_path, 'r', encoding='utf-8') as f:
            procedures = json.load(f)
        for proc in procedures:
            if proc['concept_id'] in seen_concept_ids:
                cursor.execute('''
                    INSERT INTO procedures (id, concept_id, title, goal, prerequisites_json, steps_json, verification_step, troubleshooting_notes)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ''', (
                    proc['id'], proc['concept_id'], proc['title'], proc['goal'],
                    proc.get('prerequisites_json'), proc['steps_json'],
                    proc['verification_step'], proc.get('troubleshooting_notes')
                ))
                total_procedures += 1

                # Procedure FTS entry
                cursor.execute('''
                    INSERT INTO knowledge_fts (concept_id, record_id, title, content, aliases, category_name, domain_name, keywords)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ''', (
                    proc['concept_id'], None, f"Procedure: {proc['title']}",
                    f"Goal: {proc['goal']} Verification: {proc['verification_step']}",
                    proc['title'], "Procedures", "Methodology", "procedure step-by-step how-to algorithm"
                ))
                total_fts_entries += 1
    print(f"✓ Ingested {total_procedures} procedural workflows")

    # 11. Ingest Embedding Metadata
    total_embeddings = 0
    emb_path = os.path.join(data_dir, 'embedding_metadata.json')
    if os.path.exists(emb_path):
        with open(emb_path, 'r', encoding='utf-8') as f:
            embeddings = json.load(f)
        for emb in embeddings:
            cursor.execute('''
                INSERT INTO embedding_metadata (id, target_type, target_id, model_name, dimensions, quantization, vector_json, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ''', (
                emb['id'], emb['target_type'], emb['target_id'], emb['model_name'],
                emb['dimensions'], emb['quantization'], emb.get('vector_json'), emb.get('version', '1.0.0')
            ))
            total_embeddings += 1
    print(f"✓ Ingested {total_embeddings} embedding metadata entries")

    cursor.execute('COMMIT;')

    # 12. Store Metadata
    cursor.execute('BEGIN TRANSACTION;')
    meta = [
        ('database_name', 'kuzmix_knowledge.sqlite'),
        ('corpus_title', 'Kuzmix AI Offline Knowledge Corpus'),
        ('schema_version', '1.2.0'),
        ('build_timestamp', time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime())),
        ('engine_target', 'Android SQLite / Room 2.5+ / Node:sqlite'),
        ('min_android_api_level', '28 (Android 9.0 Pie+)'),
        ('fts_module', 'FTS5 unicode61'),
        ('total_domains', str(len(domains))),
        ('total_categories', str(len(categories))),
        ('total_topics', str(len(topics))),
        ('total_concepts', str(total_concepts)),
        ('total_records', str(total_records)),
        ('total_chunks', str(total_chunks)),
        ('total_relationships', str(total_relationships)),
        ('total_aliases', str(total_aliases)),
        ('total_misconceptions', str(total_misconceptions)),
        ('total_disambiguations', str(total_disambiguations)),
        ('total_comparisons', str(total_comparisons)),
        ('total_procedures', str(total_procedures)),
        ('total_embeddings', str(total_embeddings)),
        ('total_sources', str(len(sources))),
        ('total_fts_entries', str(total_fts_entries))
    ]
    for k, v in meta:
        cursor.execute('INSERT INTO metadata (key, value) VALUES (?, ?)', (k, v))
    cursor.execute('COMMIT;')

    # 9. Optimize & Checkpoint
    print("\nOptimizing FTS5 index and checkpointing WAL...")
    cursor.execute("INSERT INTO knowledge_fts(knowledge_fts) VALUES('optimize');")
    cursor.execute('PRAGMA optimize;')
    cursor.execute('PRAGMA wal_checkpoint(TRUNCATE);')
    # Ship in DELETE journal mode (app contract: WAL -> DELETE on first open)
    jm = cursor.execute('PRAGMA journal_mode=DELETE;').fetchone()[0]
    print(f"  ✓ Journal mode: {jm}")

    # 10. Integrity Checks
    cursor.execute('PRAGMA integrity_check;')
    integrity = cursor.fetchall()
    print(f"  ✓ Integrity Check: {integrity}")
    assert integrity == [('ok',)], f"Integrity check failed: {integrity}"

    cursor.execute('PRAGMA foreign_key_check;')
    fk = cursor.fetchall()
    print(f"  ✓ Foreign Key Violations: {len(fk)}")
    assert len(fk) == 0, f"Foreign key violations: {fk}"

    conn.close()

    # Move clean DB into place (atomic copy)
    for dest in [target_db, root_target_db]:
        # Remove any lingering shm/wal at destination
        for ext in ['', '-shm', '-wal']:
            fpath = dest + ext
            if os.path.exists(fpath):
                try:
                    os.remove(fpath)
                except Exception:
                    pass
        shutil.copy2(tmp_db, dest)
        size_mb = os.path.getsize(dest) / (1024 * 1024)
        print(f"✓ Installed production database at {dest} ({size_mb:.2f} MB)")

    elapsed = time.time() - start_time
    print(f"\n====================================================")
    print(f"  BUILD COMPLETED SUCCESSFULLY IN {elapsed:.2f}s")
    print(f"  Total Concepts:      {total_concepts}")
    print(f"  Knowledge Records:   {total_records}")
    print(f"  RAG Chunks:          {total_chunks}")
    print(f"  Relationships:       {total_relationships}")
    print(f"  Aliases:             {total_aliases}")
    print(f"  Misconceptions:      {total_misconceptions}")
    print(f"  FTS5 Entries:        {total_fts_entries}")
    print(f"====================================================\n")

if __name__ == '__main__':
    build_database()
