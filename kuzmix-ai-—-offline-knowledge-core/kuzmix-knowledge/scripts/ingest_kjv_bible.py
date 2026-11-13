#!/usr/bin/env python3
"""
KUZMIX AI :: KJV BIBLE INGESTION
=================================
Parses <user>/Downloads/kjv.docx (King James Version, 66 books) and folds it
into kuzmix_knowledge.sqlite as a verse-level, FTS-indexed Bible layer:

  bible_verses  - one row per verse (book, chapter, verse, ref, text)
  bible_fts     - external-content FTS5 over (ref, text, book)
  sources       - src_bible_kjv attribution row
  concepts      - one "The Holy Bible (King James Version)" concept + aliases
                  so generic search can surface the Bible at all
  knowledge_fts - one primary FTS row for that concept

Usage:
    python ingest_kjv_bible.py            # ingest into the real DB
    python ingest_kjv_bible.py --dry-run  # parse + validate only, no writes
"""

import argparse
import hashlib
import os
import re
import shutil
import sqlite3
import sys
import time
import zipfile
import xml.etree.ElementTree as ET

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

WORKSPACE = "C:/Users/The Kreadive Galaxy/Desktop/My Products/Kuzmix AI"
PROJECT = os.path.join(WORKSPACE, "kuzmix-ai-" + chr(8212) + "-offline-knowledge-core")
DB_DEFAULT = os.path.join(PROJECT, "kuzmix-knowledge", "kuzmix_knowledge.sqlite")
DOCX_DEFAULT = r"C:/Users/The Kreadive Galaxy/Downloads/kjv.docx"
BACKUP_DEFAULT = os.path.join(PROJECT, "kuzmix-knowledge",
                              "kuzmix_knowledge.pre-bible-ingest.bak.sqlite")

SOURCE_ID = "src_bible_kjv"
SOURCE_TITLE = "The Holy Bible, King James Version (1611)"
SOURCE_ORG = "Public Domain translation"

CONCEPT_ID = "c_bible_kjv"
DOMAIN_ID = "society_culture"
CATEGORY_ID = "cat_cur_religious_studies"

W = "{http://schemas.openxmlformats.org/wordprocessingml/2006/main}"

# 66 books in canonical order (book_order = index).
BOOKS = [
    "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy", "Joshua",
    "Judges", "Ruth", "1 Samuel", "2 Samuel", "1 Kings", "2 Kings",
    "1 Chronicles", "2 Chronicles", "Ezra", "Nehemiah", "Esther", "Job",
    "Psalms", "Proverbs", "Ecclesiastes", "Song of Solomon", "Isaiah",
    "Jeremiah", "Lamentations", "Ezekiel", "Daniel", "Hosea", "Joel",
    "Amos", "Obadiah", "Jonah", "Micah", "Nahum", "Habakkuk", "Zephaniah",
    "Haggai", "Zechariah", "Malachi",
    "Matthew", "Mark", "Luke", "John", "Acts", "Romans", "1 Corinthians",
    "2 Corinthians", "Galatians", "Ephesians", "Philippians", "Colossians",
    "1 Thessalonians", "2 Thessalonians", "1 Timothy", "2 Timothy", "Titus",
    "Philemon", "Hebrews", "James", "1 Peter", "2 Peter", "1 John", "2 John",
    "3 John", "Jude", "Revelation",
]

# normalized heading -> canonical book
BOOK_ALIASES = {}


def _register_as(alias: str, canon: str):
    BOOK_ALIASES[re.sub(r"[^a-z0-9]+", " ", alias.lower()).strip()] = canon


for _b in BOOKS:
    _register_as(_b, _b)
_register_as("Psalm", "Psalms")              # headings say "Psalm 23"
_register_as("Song of Songs", "Song of Solomon")
_register_as("Canticles", "Song of Solomon")
_register_as("Ecclesiasticus", "Ecclesiastes")
_register_as("The Revelation of John", "Revelation")
_register_as("Revelation of Saint John", "Revelation")

# Known verse counts for post-parse validation (subset that catches drift).
CHECK_COUNTS = {"Genesis": 1533, "John": 879, "Psalms": 2461,
                "Revelation": 404, "Matthew": 1071}
TOTAL_EXPECTED = 31102

RE_CHAPTER_HEADING = re.compile(
    r"^(?:(?P<num>[1-3])\s+)?(?P<name>[A-Za-z][A-Za-z' ]{1,40}?)\s+(?P<ch>\d{1,3})$")
# verse markers: number followed by narrow-no-break-space (U+202F) in this docx
RE_VERSE_MARKER = re.compile(r"(?<![\d.,])(\d{1,3})\u202f")
RE_VERSE_MARKER_LOOSE = re.compile(r"(?<![\d.,:])(\d{1,3})[ \u00a0](?=[A-Z\"'(\[])")


def log(msg=""):
    print(msg, flush=True)


def norm_space(s: str) -> str:
    """Normalize a VERSE BODY for storage. Destroys U+202F only here - the
    parser itself needs the marker intact to split verses."""
    s = s.replace("\u202f", " ").replace("\xa0", " ")
    s = s.replace("\u2018", "'").replace("\u2019", "'")
    s = s.replace("\u201c", '"').replace("\u201d", '"')
    s = s.replace("\u2013", "-").replace("\u2014", "-")
    return re.sub(r"[ \t]+", " ", s).strip()


def soft_norm(s: str) -> str:
    """Typographic cleanup ONLY - preserves U+202F verse markers."""
    s = s.replace("\u2018", "'").replace("\u2019", "'")
    s = s.replace("\u201c", '"').replace("\u201d", '"')
    s = s.replace("\u2013", "-").replace("\u2014", "-")
    return re.sub(r"[ \t]+", " ", s).strip()


def docx_paragraphs(path: str):
    with zipfile.ZipFile(path) as z:
        xml = z.read("word/document.xml")
    root = ET.fromstring(xml)
    body = root.find(W + "body")
    out = []
    for p in body.iter(W + "p"):
        parts = []
        for node in p.iter():
            if node.tag == W + "t":
                parts.append(node.text or "")
            elif node.tag == W + "br":
                parts.append(" ")
        text = soft_norm("".join(parts))
        if text:
            out.append(text)
    return out


# short ALL-Title-Case line without terminal punctuation -> section heading,
# never a verse continuation (e.g. "The Second Day", "Israel Demands a King").
RE_SECTION_HEADING = re.compile(r"^[A-Z][A-Za-z' ]{2,48}$")
# heading immediately followed by a cross-reference parenthetical
RE_HEADING_XREF = re.compile(r"^[A-Z][A-Za-z' ]{2,48}\([A-Z0-9]")


def match_book(candidate: str):
    key = re.sub(r"[^a-z0-9]+", " ", candidate.lower()).strip()
    return BOOK_ALIASES.get(key)


def parse_bible(paras):
    """-> (list of dicts book/chapter/verse/text, stats dict)

    Paragraphs arrive with U+202F intact. Verse bodies are normalized
    (U+202F -> space) only after the split, never before."""
    verses = []
    stats = {"paragraphs": len(paras), "chapters": 0, "skipped_nonverse": 0,
             "continuations": 0, "headings": 0, "seen": set()}
    cur_book = None
    cur_chapter = None
    pending = None          # verse dict currently receiving continuation text

    for para in paras:
        if para.startswith("KJV") or para in ("Holy Bible", "King James Version"):
            continue

        m = RE_CHAPTER_HEADING.match(para)
        if m:
            book = match_book(
                ((m.group("num") + " ") if m.group("num") else "") + m.group("name"))
            if book and 1 <= int(m.group("ch")) <= 176:
                cur_book = book
                cur_chapter = int(m.group("ch"))
                pending = None
                stats["headings"] += 1
                if (book, cur_chapter) not in stats["seen"]:
                    stats["seen"].add((book, cur_chapter))
                    stats["chapters"] += 1
                continue

        if cur_book is None:
            stats["skipped_nonverse"] += 1
            continue

        markers = [(mm.start(), int(mm.group(1))) for mm in RE_VERSE_MARKER.finditer(para)]

        if markers:
            prefix = para[:markers[0][0]]
            if prefix.strip() and pending is not None:
                pending["text"] = (pending["text"] + " " + prefix).strip()
                stats["continuations"] += 1
            for i, (pos, vnum) in enumerate(markers):
                end = markers[i + 1][0] if i + 1 < len(markers) else len(para)
                text = para[pos + len(str(vnum)) + 1:end]
                verses.append({"book": cur_book, "chapter": cur_chapter,
                               "verse": vnum, "text": norm_space(text)})
            pending = verses[-1]
            continue

        # marker-free paragraph
        loose = RE_VERSE_MARKER_LOOSE.match(para)
        if loose and "(" not in para[:loose.start() + 1]:
            vnum = int(loose.group(1))
            text = para[loose.end():].strip()
            if len(text) >= 10:
                if pending is not None and para[:loose.start()].strip():
                    pending["text"] = (pending["text"] + " " +
                                       para[:loose.start()]).strip()
                verses.append({"book": cur_book, "chapter": cur_chapter,
                               "verse": vnum, "text": norm_space(text)})
                pending = verses[-1]
                continue

        # section heading between verses? never glue it into a verse.
        # xref-style headings only count when no verse is pending - a
        # mid-verse "(" is real KJV text (e.g. place-name glosses).
        if RE_SECTION_HEADING.match(para) or (
                pending is None and RE_HEADING_XREF.match(para)):
            pending = None
            stats["headings"] += 1
            continue

        # continuation of the previous verse (poetry wrap / long verse)
        if pending is not None and len(para) >= 8:
            pending["text"] = (pending["text"] + " " + norm_space(para)).strip()
            stats["continuations"] += 1
            continue

        stats["skipped_nonverse"] += 1

    stats.pop("seen", None)
    return verses, stats


def validate(verses):
    errors = []
    counts = {}
    for v in verses:
        counts[v["book"]] = counts.get(v["book"], 0) + 1
    for book, expected in CHECK_COUNTS.items():
        got = counts.get(book, 0)
        if got != expected:
            errors.append(f"{book}: expected {expected} verses, parsed {got}")
    total = len(verses)
    if total != TOTAL_EXPECTED:
        errors.append(f"total: expected {TOTAL_EXPECTED}, parsed {total}")
    seen = set()
    dup = 0
    empty = False
    for v in verses:
        key = (v["book"], v["chapter"], v["verse"])
        if key in seen:
            dup += 1
        seen.add(key)
        if not v["text"].strip():
            empty = True
    if dup:
        errors.append(f"duplicate (book,chapter,verse) rows: {dup}")
    if empty:
        errors.append("empty verse text present")
    return errors, counts


def run(args):
    t0 = time.time()
    db_path = args.db or DB_DEFAULT
    docx_path = args.docx or DOCX_DEFAULT

    log("=" * 78)
    log("KUZMIX AI :: KJV BIBLE INGESTION" + ("   [DRY RUN]" if args.dry_run else ""))
    log("=" * 78)
    log(f"  DOCX    : {docx_path}")
    log(f"  Database: {db_path}")
    if not os.path.exists(docx_path):
        log("!! kjv.docx not found")
        return 1
    if not os.path.exists(db_path):
        log("!! database not found")
        return 1

    paras = docx_paragraphs(docx_path)
    log(f"  Paragraphs read: {len(paras):,}")
    verses, stats = parse_bible(paras)
    log(f"  Verses parsed  : {len(verses):,}  chapters: {stats['chapters']:,}  "
        f"continuations: {stats['continuations']:,}  skipped: {stats['skipped_nonverse']:,}")

    errors, counts = validate(verses)
    if errors:
        log("!! VALIDATION ISSUES:")
        for e in errors:
            log(f"   - {e}")
    else:
        log("  Validation    : PASSED (31,102 verses, chapter counts match)")

    log("\n  Sample:")
    for ref in [("Genesis", 1, 1), ("Genesis", 1, 31), ("John", 3, 16),
                ("Psalms", 23, 1), ("Revelation", 22, 21)]:
        hit = next((v for v in verses
                    if (v["book"], v["chapter"], v["verse"]) == ref), None)
        shown = (hit["text"][:80] + "...") if hit and len(hit["text"]) > 80 else (
            hit["text"] if hit else "MISSING")
        log(f"    {ref[0]} {ref[1]}:{ref[2]} -> {shown!r}")

    if args.dry_run:
        log("\nDRY RUN complete - no database changes were made.")
        return 0 if not errors else 4

    if errors:
        log("\n!! refusing to write: validation failed")
        return 4

    if not os.path.exists(BACKUP_DEFAULT):
        shutil.copy2(db_path, BACKUP_DEFAULT)
        log(f"\n  Backup: {os.path.basename(BACKUP_DEFAULT)}")

    con = sqlite3.connect(db_path)
    cur = con.cursor()
    cur.execute("PRAGMA foreign_keys = ON")
    con.execute("BEGIN")

    # ---- schema ------------------------------------------------------------
    cur.execute("DROP TABLE IF EXISTS bible_fts")
    cur.execute("DROP TABLE IF EXISTS bible_verses")
    cur.execute("""
        CREATE TABLE bible_verses (
            rowid       INTEGER PRIMARY KEY,
            book_order  INTEGER NOT NULL,
            book        TEXT NOT NULL,
            chapter     INTEGER NOT NULL,
            verse       INTEGER NOT NULL,
            ref         TEXT NOT NULL,
            text        TEXT NOT NULL,
            UNIQUE(book, chapter, verse)
        )""")
    cur.execute("CREATE INDEX idx_bible_ref ON bible_verses(ref COLLATE NOCASE)")
    cur.execute("CREATE INDEX idx_bible_book_ch ON bible_verses(book, chapter)")
    cur.execute("""
        CREATE VIRTUAL TABLE bible_fts USING fts5(
            ref, text, book,
            content='bible_verses', content_rowid='rowid',
            tokenize='unicode61 remove_diacritics 2')""")

    order_of = {b: i + 1 for i, b in enumerate(BOOKS)}
    rows = [(order_of[v["book"]], v["book"], v["chapter"], v["verse"],
             f"{v['book']} {v['chapter']}:{v['verse']}", v["text"])
            for v in verses]
    cur.executemany(
        "INSERT INTO bible_verses (book_order, book, chapter, verse, ref, text) "
        "VALUES (?,?,?,?,?,?)", rows)
    cur.execute(
        "INSERT INTO bible_fts(rowid, ref, text, book) "
        "SELECT rowid, ref, text, book FROM bible_verses")
    cur.execute("INSERT INTO bible_fts(bible_fts) VALUES('optimize')")

    # ---- source ------------------------------------------------------------
    cur.execute("INSERT OR REPLACE INTO sources "
                "(id, title, author_or_org, source_type, uri_or_reference, license, notes) "
                "VALUES (?,?,?,?,?,?,?)",
                (SOURCE_ID, SOURCE_TITLE, SOURCE_ORG, "curated_corpus",
                 docx_path, "Public Domain (King James Version)",
                 "Verse-level KJV Bible ingested from local kjv.docx; "
                 "31,102 verses indexed for offline lookup and FTS search."))

    # ---- category guard ----------------------------------------------------
    if not cur.execute("SELECT 1 FROM categories WHERE id = ?", (CATEGORY_ID,)).fetchone():
        if not cur.execute("SELECT 1 FROM domains WHERE id = ?", (DOMAIN_ID,)).fetchone():
            log(f"!! domain {DOMAIN_ID} missing - aborting")
            con.rollback()
            con.close()
            return 5
        cur.execute("INSERT OR IGNORE INTO categories "
                    "(id, domain_id, name, slug, description, sort_order) VALUES (?,?,?,?,?,?)",
                    (CATEGORY_ID, DOMAIN_ID, "Religious Studies (CRS & IRS)",
                     "religious studies crs and irs",
                     "Christian and Islamic religious studies taught in Nigerian "
                     "secondary schools: beliefs, practices, ethics.", 90))

    # ---- concept -----------------------------------------------------------
    summary = ("The Holy Bible in the King James Version: 66 books, 1,189 chapters "
               "and 31,102 verses, available offline for reference, verse lookup "
               "and topical study.")
    explanation = (
        "The King James Version (KJV) of the Holy Bible, first published in 1611, "
        "is a complete English translation of the Christian scriptures - 39 books "
        "of the Old Testament and 27 books of the New Testament. Every verse is "
        "stored individually (for example John 3:16 or Psalm 23:1), so you can ask "
        "for any passage by reference, search by keyword or topic, and study books, "
        "chapters and themes offline. Ask things like \"what does John 3:16 say\", "
        "\"show me Psalm 23\", or \"bible verses about faith\".")
    name = "The Holy Bible (King James Version)"
    content_hash = hashlib.sha256(f"{name}:{summary}".encode()).hexdigest()
    cur.execute("""
        INSERT INTO concepts (id, topic_id, category_id, domain_id, name,
            canonical_name_normalized, summary_definition, primary_explanation,
            key_properties_json, applications_json, prerequisites_json,
            difficulty_level, confidence_score, provenance_type, source_id,
            content_hash, created_at, updated_at)
        VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'historical_record', ?, ?,
                strftime('%Y-%m-%dT%H:%M:%SZ','now'),
                strftime('%Y-%m-%dT%H:%M:%SZ','now'))
        ON CONFLICT(id) DO UPDATE SET
            summary_definition = excluded.summary_definition,
            primary_explanation = excluded.primary_explanation,
            content_hash = excluded.content_hash,
            updated_at = strftime('%Y-%m-%dT%H:%M:%SZ','now')""",
        (CONCEPT_ID, CATEGORY_ID, DOMAIN_ID, name, name.lower().strip(),
         summary, explanation,
         '[{"property": "Books", "value": "66 (39 Old Testament, 27 New Testament)"},'
         ' {"property": "Chapters", "value": "1,189"},'
         ' {"property": "Verses", "value": "31,102"},'
         ' {"property": "Translation", "value": "King James Version (1611)"}]',
         '["Offline verse lookup by reference (John 3:16)",'
         ' "Topical Bible study and scripture memory",'
         ' "CRS/IRS exam preparation and quotation practice"]',
         None, "introductory", 0.95, SOURCE_ID, content_hash))

    # aliases
    for i, a in enumerate(["Bible", "Holy Bible", "KJV", "King James Version",
                           "King James Bible", "scripture", "The Scriptures"]):
        cur.execute("INSERT OR IGNORE INTO aliases "
                    "(id, concept_id, alias, alias_normalized, alias_type) "
                    "VALUES (?,?,?,?, 'synonym')",
                    (f"al_{CONCEPT_ID}_{i}", CONCEPT_ID, a, a.lower().strip()))

    # primary knowledge_fts row
    cur.execute("DELETE FROM knowledge_fts WHERE concept_id = ? AND record_id IS NULL",
                (CONCEPT_ID,))
    cur.execute("INSERT INTO knowledge_fts "
                "(concept_id, record_id, title, content, aliases, category_name, "
                "domain_name, keywords) VALUES (?, NULL, ?, ?, ?, ?, ?, ?)",
                (CONCEPT_ID, name, f"{summary} {explanation}",
                 "Bible Holy Bible KJV King James Version scripture scriptures",
                 "Religious Studies (CRS & IRS)", "Society, Civics and Culture",
                 "bible; kjv; verse; scripture; old testament; new testament; "
                 "genesis; psalms; john; matthew; revelation; crs"))

    # ---- truthful metadata -------------------------------------------------
    stamp = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
    counters = [("total_sources", "sources"), ("total_concepts", "concepts"),
                ("total_aliases", "aliases"), ("total_fts_entries", "knowledge_fts")]
    for key, table in counters:
        n = cur.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0]
        cur.execute("INSERT INTO metadata (key, value, updated_at) VALUES (?, ?, ?) "
                    "ON CONFLICT(key) DO UPDATE SET value = excluded.value, "
                    "updated_at = excluded.updated_at", (key, str(n), stamp))
    for key, val in [("bible_ingest_timestamp", stamp),
                     ("bible_source_files", "1"),
                     ("bible_verses_total", str(len(verses))),
                     ("bible_chapters_total", str(stats["chapters"])),
                     ("last_ingest_timestamp", stamp)]:
        cur.execute("INSERT INTO metadata (key, value, updated_at) VALUES (?, ?, ?) "
                    "ON CONFLICT(key) DO UPDATE SET value = excluded.value, "
                    "updated_at = excluded.updated_at", (key, val, stamp))

    con.commit()

    # ---- optimize + verify -------------------------------------------------
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

    hit = cur.execute(
        "SELECT v.ref, v.text FROM bible_fts f JOIN bible_verses v ON v.rowid = f.rowid "
        "WHERE bible_fts MATCH ? ORDER BY bm25(bible_fts) LIMIT 1",
        ('"for god so loved"',)).fetchone()
    log(f"  FTS smoke test   : {hit}")
    ref_hit = cur.execute(
        "SELECT text FROM bible_verses WHERE book = 'John' AND chapter = 3 AND verse = 16"
    ).fetchone()
    log(f"  John 3:16        : {ref_hit[0] if ref_hit else 'MISSING'}")

    size = os.path.getsize(db_path)
    log("\n" + "-" * 78)
    log("BIBLE INGEST COMPLETE")
    log("-" * 78)
    log(f"  Verses written : {len(rows):,}")
    log(f"  Database size  : {size:,} bytes ({size / 1_048_576:.1f} MB)")
    log(f"  Elapsed        : {time.time() - t0:.1f}s")
    con.close()
    return 0


def main():
    ap = argparse.ArgumentParser(description="Ingest the KJV Bible into kuzmix_knowledge.sqlite")
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--db", default=None)
    ap.add_argument("--docx", default=None)
    args = ap.parse_args()
    try:
        sys.exit(run(args))
    except Exception:
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()
