#!/usr/bin/env python3
"""
KUZMIX AI :: FINALIZE CORPUS FOR THE ANDROID APP
=================================================
  1. Build chunks_fts - FTS5 index over the 51k lesson-note + exam-question
     chunks (external content: index only, text stays in `chunks`).
  2. Add the covering index for per-concept lesson/exam retrieval.
  3. Refresh truthful metadata counters.
  4. Verify integrity, lock journal to DELETE, VACUUM.
  5. Copy the finished DB to BOTH destinations (knowledge core + APK asset).

Run LAST, after every ingestion script - chunk writes after this would
desync chunks_fts (no triggers on read-only shipped data).
"""

import os
import shutil
import sqlite3
import sys
import time

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

WORKSPACE = "C:/Users/The Kreadive Galaxy/Desktop/My Products/Kuzmix AI"
PROJECT = os.path.join(WORKSPACE, "kuzmix-ai-" + chr(8212) + "-offline-knowledge-core")
DB = os.path.join(PROJECT, "kuzmix-knowledge", "kuzmix_knowledge.sqlite")
ASSET = os.path.join(WORKSPACE, "app", "src", "main", "assets", "kuzmix_knowledge.sqlite")


def log(msg=""):
    print(msg, flush=True)


def run():
    t0 = time.time()
    log("=" * 78)
    log("KUZMIX AI :: CORPUS FINALIZE (chunks_fts + asset copy)")
    log("=" * 78)
    log(f"  Database: {DB}")
    if not os.path.exists(DB):
        log("!! database not found")
        return 1

    con = sqlite3.connect(DB)
    cur = con.cursor()

    chunks = cur.execute("SELECT COUNT(*) FROM chunks").fetchone()[0]
    log(f"  chunks rows: {chunks:,}")

    # ---- 1. chunks_fts (external content, index-only) ----------------------
    # NOTE: COUNT(*) on an external-content FTS5 table reads the *content*
    # table, so it always equals COUNT(chunks) even when the index itself is
    # stale. The shadow %_docsize table holds one row per indexed document -
    # that is the real staleness signal (probe: 51,074 docsize vs 51,369
    # chunks after an ingest that never touched the index).
    have = cur.execute(
        "SELECT 1 FROM sqlite_master WHERE name = 'chunks_fts'").fetchone()
    if have:
        try:
            n = cur.execute("SELECT COUNT(*) FROM chunks_fts_docsize").fetchone()[0]
        except sqlite3.OperationalError:
            n = -1                     # docsize shadow missing -> rebuild
        if n == chunks:
            log(f"  chunks_fts already built ({n:,} rows) - skipping")
        else:
            log(f"  chunks_fts stale ({n:,} vs {chunks:,}) - rebuilding")
            cur.execute("DROP TABLE chunks_fts")
            have = None
    if not have or cur.execute(
            "SELECT COUNT(*) FROM chunks_fts_docsize").fetchone()[0] != chunks:
        cur.execute("DROP TABLE IF EXISTS chunks_fts")
        cur.execute("""
            CREATE VIRTUAL TABLE chunks_fts USING fts5(
                text_content, header_context,
                content='chunks', content_rowid='rowid',
                tokenize='unicode61 remove_diacritics 2')""")
        cur.execute("""
            INSERT INTO chunks_fts(rowid, text_content, header_context)
            SELECT rowid, text_content, header_context FROM chunks""")
        cur.execute("INSERT INTO chunks_fts(chunks_fts) VALUES('optimize')")
        cur.fetchall()
        log("  chunks_fts built + optimized")

    n = cur.execute("SELECT COUNT(*) FROM chunks_fts_docsize").fetchone()[0]
    assert n == chunks, f"chunks_fts {n} != chunks {chunks}"
    log(f"  chunks_fts rows : {n:,} (matches)")

    # ---- 2. covering index for concept-ordered retrieval -------------------
    cur.execute("CREATE INDEX IF NOT EXISTS idx_chunks_concept "
                "ON chunks(concept_id, chunk_type, chunk_index)")
    cur.execute("CREATE INDEX IF NOT EXISTS idx_chunks_type "
                "ON chunks(chunk_type, concept_id)")

    # ---- 3. metadata -------------------------------------------------------
    stamp = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
    by_type = dict(cur.execute(
        "SELECT chunk_type, COUNT(*) FROM chunks GROUP BY 1").fetchall())
    meta = [
        ("chunks_fts_entries", str(n)),
        ("lesson_note_chunks", str(by_type.get("lesson_notes", 0))),
        ("exam_question_chunks", str(by_type.get("exam_questions", 0))),
        ("total_chunks", str(chunks)),
        ("corpus_finalize_timestamp", stamp),
        ("last_ingest_timestamp", stamp),
    ]
    for key, val in meta:
        cur.execute("INSERT INTO metadata (key, value, updated_at) VALUES (?, ?, ?) "
                    "ON CONFLICT(key) DO UPDATE SET value = excluded.value, "
                    "updated_at = excluded.updated_at", (key, val, stamp))
    con.commit()
    log(f"  lesson chunks   : {by_type.get('lesson_notes', 0):,}")
    log(f"  exam chunks     : {by_type.get('exam_questions', 0):,}")

    # ---- 4. optimize + verify --------------------------------------------
    log("Optimizing + vacuuming...")
    cur.execute("PRAGMA optimize;")
    cur.fetchall()
    con.commit()
    cur.execute("PRAGMA journal_mode=DELETE;")
    cur.fetchall()
    con.execute("VACUUM;")

    integrity = cur.execute("PRAGMA integrity_check;").fetchall()
    fk = cur.execute("PRAGMA foreign_key_check;").fetchall()
    log(f"  integrity_check  : {integrity}")
    log(f"  foreign_key_check: {'ok' if not fk else fk[:5]}")
    if integrity != [("ok",)] or fk:
        log("!! VERIFICATION FAILED")
        con.close()
        return 3

    # smoke: search an exam chunk and a lesson chunk via FTS
    for probe in ('"photosynthesis"', '"agriculture is derived"'):
        row = cur.execute(
            "SELECT c.concept_id, c.chunk_type, substr(c.header_context,1,70) "
            "FROM chunks_fts f JOIN chunks c ON c.rowid = f.rowid "
            "WHERE chunks_fts MATCH ? ORDER BY bm25(chunks_fts, 1.0, 4.0) LIMIT 1",
            (probe,)).fetchone()
        log(f"  FTS probe {probe:24s}: {row}")

    # bible quick probes (proves the whole stack in one pass)
    b = cur.execute("SELECT COUNT(*) FROM bible_verses").fetchone()[0]
    log(f"  bible_verses    : {b:,}")

    journal = cur.execute("PRAGMA journal_mode;").fetchone()[0]
    size = cur.execute("PRAGMA page_count;").fetchone()[0] * \
        cur.execute("PRAGMA page_size;").fetchone()[0]
    con.close()

    # ---- 5. copy to APK asset --------------------------------------------
    log("\nCopying to APK asset...")
    shutil.copy2(DB, ASSET)
    for suffix in ("-wal", "-shm"):
        stale = ASSET + suffix
        if os.path.exists(stale):
            os.remove(stale)
    asset_size = os.path.getsize(ASSET)
    same = open(DB, "rb").read() == open(ASSET, "rb").read()
    log(f"  asset: {ASSET}")
    log(f"  asset size: {asset_size:,} bytes ({asset_size / 1_048_576:.1f} MB)  identical={same}")

    log("\n" + "-" * 78)
    log("FINALIZE COMPLETE")
    log("-" * 78)
    log(f"  journal={journal}  db_size={size:,} bytes ({size / 1_048_576:.1f} MB)")
    log(f"  Elapsed: {time.time() - t0:.1f}s")
    log("  REMINDER: bump KnowledgeCoreStore.DB_VERSION so the app re-copies.")
    return 0 if same else 4


if __name__ == "__main__":
    sys.exit(run())
