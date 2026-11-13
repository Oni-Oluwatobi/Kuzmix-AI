#!/usr/bin/env python3
"""
KUZMIX AI :: DOCX EXAMINATION-PAPER INGESTION

Ingests the local DOCX examination-question corpus (JSS 1 - SS 3, all
subjects) into kuzmix_knowledge.sqlite.

Design:
  * one concept per (class, subject, term) with id c_exam_{cls}_{slug}[_tN]
  * chunk_type 'exam_questions' - renumbered questions grouped into
    self-contained passages targeting ~150 tokens (hard ceiling 250)
  * source: src_curriculum_exam_papers (open_educational_resource)
  * related_to edges from each exam concept to the matching curriculum
    lesson-note concepts (c_cur_*) of the same class + subject
  * one FTS row per exam concept; truthful metadata counters
  * idempotent: content_hash of the parsed question stream; upserts only
  * pre-ingest backup, single transaction, FTS optimize + VACUUM,
    journal left in DELETE, integrity + foreign-key verification

Parsing uses only the Python standard library (a .docx is a ZIP of XML):
word/document.xml is walked for paragraphs (w:p/w:t) and table rows.
"""

import argparse
import hashlib
import json
import os
import re
import shutil
import sqlite3
import sys
import time
import zipfile
import xml.etree.ElementTree as ET

# Reuse helpers/taxonomy from the PDF ingester (same directory).
import ingest_pdf_corpus as base

W = "{http://schemas.openxmlformats.org/wordprocessingml/2006/main}"

WORKSPACE = "C:/Users/The Kreadive Galaxy/Desktop/My Products/Kuzmix AI"
PROJECT = os.path.join(WORKSPACE, "kuzmix-ai-" + chr(8212) + "-offline-knowledge-core")
DB_DEFAULT = os.path.join(PROJECT, "kuzmix-knowledge", "kuzmix_knowledge.sqlite")
ROOT_DEFAULT = os.path.join(WORKSPACE, "pdf")
BACKUP_DEFAULT = os.path.join(PROJECT, "kuzmix-knowledge",
                              "kuzmix_knowledge.pre-exam-ingest.bak.sqlite")

SOURCE_ID = "src_curriculum_exam_papers"
SOURCE_TITLE = "Nigerian Basic Education Examination Questions (JSS 1 - SS 3)"
SOURCE_ORG = "The Kreadive Galaxy"

CH_TARGET, CH_MAX = 150, 220

# filename-subject variants not already keyed in SUBJECT_MAP
EXAM_SUBJECT_ALIASES = {
    "french language": "french",
    "catering craft": "catering and craft",
    "catering craft practice": "catering and craft practice",
}

RE_EXAM_STEM = re.compile(r"(?i)\bEXAMINATION\b")
RE_HEADER = re.compile(
    r"^(Subject|Class|Term|Examination\s+Type|Time\s+Allowed)\s*:\s*(.+)$", re.I)
RE_HEADER_ANY = re.compile(
    r"^(Subject|Class|Term|Examination\s+Type|Time\s+Allowed)\s*:", re.I)
RE_SECTION = re.compile(r"^SECTION\s+([A-Z0-9]{1,3})\s*[:.\-]?\s*(.*)$", re.I)
RE_OPTION = re.compile(r"^\s*([A-Da-d])\s*[.)]\s+(.*)$")
RE_QNUM = re.compile(r"^\s*(?:Q(?:uestion)?\s*)?(\d{1,3})\s*[.)]\s+")
RE_QNUM2 = re.compile(r"^\s*Question\s+\d{1,3}\s*", re.I)
RE_INSTR = re.compile(
    r"(?i)^\s*[\(\[]?\s*(instructions?\b|note\b|directions?\b|answer\s+(all|any|the)\b|"
    r"show\s+all\b|use\s+|choose\s+the\s+most\b|from\s+the\s+options\b|"
    r"each\s+question\b|this\s+paper\b|time\s+allowed\b|do\s+not\b|"
    r"read\s+the\s+|read\s+all\b|study\s+the\s+|fill\s+|pick\s+|select\s+|"
    r"write\s+your\b|non[-\s]?programmable\b|all\s+questions\b|"
    r"calculators?\b|marks?\s+will\s+be|answer\s+sheet\b|answer\s+booklet\b)")
RE_TERM_WORD = re.compile(r"(?i)\b(FIRST|SECOND|THIRD|1ST|2ND|3RD)\b")


def log(msg=""):
    print(msg, flush=True)


def sha256_text(text):
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def token_est(text):
    return max(1, len(text) // 4)


def norm_text(text):
    s = text.lower()
    s = re.sub(r"[^a-z0-9]+", " ", s)
    return re.sub(r"\s+", " ", s).strip()


# --------------------------------------------------------------------------
# DOCX text extraction (stdlib only)
# --------------------------------------------------------------------------
def docx_lines(path):
    with zipfile.ZipFile(path) as z:
        xml = z.read("word/document.xml")
    root = ET.fromstring(xml)
    body = root.find(W + "body")
    out = []

    def para_text(p):
        parts = []
        for node in p.iter():
            if node.tag == W + "t":
                parts.append(node.text or "")
            elif node.tag == W + "tab":
                parts.append("\t")
            elif node.tag == W + "br":
                parts.append("\n")
        return "".join(parts)

    def walk(el):
        for child in el:
            if child.tag == W + "p":
                out.append(para_text(child))
            elif child.tag == W + "tbl":
                for tr in child.findall(W + "tr"):
                    cells = []
                    for tc in tr.findall(W + "tc"):
                        cell = " ".join(
                            para_text(p).strip() for p in tc.iter(W + "p")).strip()
                        cells.append(cell)
                    out.append(" | ".join(cells))
            else:
                walk(child)

    walk(body)
    return [re.sub(r"[  \t]+", " ", ln).replace(" ", " ").strip()
            for ln in out]


# --------------------------------------------------------------------------
# Corpus walk
# --------------------------------------------------------------------------
def collect_docx(root):
    files = []
    for dp, _dn, fn in os.walk(root):
        for f in fn:
            if f.lower().endswith(".docx") and not f.startswith("~$"):
                files.append(os.path.join(dp, f))
    files.sort()
    seen, unique, dupes = set(), [], []
    for p in files:
        h = hashlib.sha256(open(p, "rb").read()).hexdigest()
        if h in seen:
            dupes.append(p)
            continue
        seen.add(h)
        unique.append(p)
    return unique, dupes


def classify_exam(filename):
    """-> (cls, subject_raw, stem) or (None, None, stem)."""
    stem = os.path.splitext(filename)[0].strip()
    m = re.match(r"^(JSS|SS)\s*([123])\s*(.*)$", stem, re.I)
    if not m:
        return None, None, stem
    cls = (m.group(1) + m.group(2)).lower()
    rest = m.group(3)
    if not RE_EXAM_STEM.search(rest):
        return None, None, stem          # lesson-note docx etc.
    rest = re.sub(r"(?i)\s*EXAMINATION(\s+QUESTIONS?)?\s*$", "", rest)
    rest = re.sub(r"(?i)\s+QUESTIONS?\s*$", "", rest)
    rest = rest.replace("&", " and ")
    rest = re.sub(r"\s*\((IRS|CCA|SS|JS|BECE|JSCE)\)\s*", " ", rest, flags=re.I)
    rest = re.sub(r"\s+", " ", rest).strip(" -")
    return cls, rest, stem


def subject_key(subject):
    s = subject.lower()
    s = s.replace("-", " ")
    s = re.sub(r"\s+", " ", s).strip()
    if s in EXAM_SUBJECT_ALIASES:
        s = EXAM_SUBJECT_ALIASES[s]
    return s


def term_from_header(lines):
    for ln in lines[:20]:
        m = re.search(r"Term\s*:\s*(FIRST|SECOND|THIRD|1ST|2ND|3RD)\b", ln, re.I)
        if m:
            return base.term_from_marker(m.group(1), None)
        m = re.search(r"Term\s*:\s*([123])\b", ln, re.I)
        if m:
            return int(m.group(1))
    return None


# --------------------------------------------------------------------------
# Exam paper parsing
# --------------------------------------------------------------------------
def is_subsection_line(ln):
    b = ln.strip()
    if len(b) < 4 or len(b) > 70:
        return False
    if "?" in b or re.search(r"[.!?]\s*$", b):
        return False
    letters = [c for c in b if c.isalpha()]
    if len(letters) < 4:
        return False
    return sum(1 for c in letters if c.isupper()) / len(letters) >= 0.8


def parse_exam(lines):
    """-> {term, questions: [{section, text, options[]}]}"""
    questions = []
    section = "Questions"
    subsection = None
    cur = None

    def close():
        nonlocal cur
        if cur is not None:
            cur["text"] = re.sub(r"\s+", " ", cur["text"]).strip(" ?.:")
            if len(cur["text"]) >= 8:
                questions.append(cur)
            cur = None

    for raw in lines:
        for ln in [x.strip() for x in raw.split(" | ")]:
            if not ln:
                continue
            if RE_HEADER_ANY.match(ln):
                close()
                continue
            m = RE_SECTION.match(ln)
            if m and len(m.group(1)) <= 3:
                close()
                name = (m.group(1).upper() + (
                    ": " + m.group(2).strip() if m.group(2).strip() else ""))
                section = re.sub(r"\s*\(\d+[^)]*\)\s*", " ", name).strip() or name
                subsection = None
                continue
            if is_subsection_line(ln) and not RE_OPTION.match(ln):
                close()
                subsection = re.sub(r"\s*\(\d+[^)]*\)\s*$", "", ln).strip()
                continue
            if RE_INSTR.match(ln) and not RE_OPTION.match(ln):
                close()
                continue
            mo = RE_OPTION.match(ln)
            if mo and cur is not None:
                cur["options"].append(
                    mo.group(1).upper() + ". " + mo.group(2).strip())
                continue
            if mo and cur is None:
                continue                                    # orphan option
            close()
            text = RE_QNUM.sub("", ln)
            text = RE_QNUM2.sub("", text)
            cur = {"section": section, "subsection": subsection,
                   "text": text, "options": []}
    close()

    # renumber per section; attach section labels
    counters = {}
    out = []
    seen_q = set()
    for q in questions:
        key = norm_text(q["text"])[:180]
        if key in seen_q:
            continue
        seen_q.add(key)
        n = counters.get(q["section"], 0) + 1
        counters[q["section"]] = n
        q["number"] = n
        out.append(q)
    return out


def question_block(q):
    label = f"Q{q['number']}"
    if q.get("subsection"):
        label += f" [{q['subsection']}]"
    text = f"{label}. {q['text']}"
    if q["options"]:
        text += " " + " | ".join(q["options"])
    return text


def chunk_questions(questions):
    chunks, buf, toks, cur_sec = [], [], 0, None

    def flush():
        nonlocal buf, toks
        if buf:
            chunks.append((cur_sec, " ".join(buf)))
            buf, toks = [], 0

    for q in questions:
        block = question_block(q)
        bt = token_est(block)
        if q["section"] != cur_sec:
            flush()
            cur_sec = q["section"]
        if bt > CH_MAX:                    # oversized single question
            flush()
            words, part, pt = block.split(), [], 0
            for w in words:
                if pt + token_est(w) > CH_MAX and part:
                    chunks.append((cur_sec, " ".join(part)))
                    part, pt = [], 0
                part.append(w)
                pt += token_est(w)
            if part:
                chunks.append((cur_sec, " ".join(part)))
            continue
        if buf and toks + bt > CH_TARGET:
            flush()
        buf.append(block)
        toks += bt
    flush()
    return chunks


# --------------------------------------------------------------------------
# Main
# --------------------------------------------------------------------------
def run(args):
    t0 = time.time()
    db_path = args.db or DB_DEFAULT
    root = args.root or ROOT_DEFAULT
    dry = args.dry_run

    log("=" * 78)
    log("KUZMIX AI :: DOCX EXAMINATION-PAPER INGESTION" + ("   [DRY RUN]" if dry else ""))
    log("=" * 78)
    log(f"  DOCX root: {root}")
    log(f"  Database : {db_path}")

    if not os.path.exists(db_path):
        log("!! database not found")
        return 1
    if not os.path.isdir(root):
        log("!! docx root not found")
        return 1

    if not dry and not os.path.exists(BACKUP_DEFAULT):
        shutil.copy2(db_path, BACKUP_DEFAULT)
        log(f"  Backup   : {os.path.basename(BACKUP_DEFAULT)}")

    unique, dupes = collect_docx(root)
    if args.limit:
        unique = unique[:args.limit]
    log(f"\nFiles: {len(unique) + len(dupes)} docx | "
        f"{len(dupes)} byte-identical duplicates skipped | {len(unique)} unique\n")

    con = sqlite3.connect(db_path)
    cur = con.cursor()
    cur.execute("PRAGMA foreign_keys = ON")
    if not dry:
        con.execute("BEGIN")

    existing_categories = {r[0] for r in cur.execute("SELECT id FROM categories")}
    domain_ids = {r[0] for r in cur.execute("SELECT id FROM domains")}

    mapped = {}
    new_categories = {}
    for k, (dom, cat, mode, meta) in base.SUBJECT_MAP.items():
        mapped[k] = (dom, cat)
        if mode == base.NEWCAT and cat not in existing_categories and cat not in new_categories:
            new_categories[cat] = (dom, meta[0], base.slugify(meta[0]), meta[1])
    for alias, target in EXAM_SUBJECT_ALIASES.items():
        if target in mapped:
            mapped[alias] = mapped[target]

    stats = {"files": 0, "skipped_nonexam": 0, "skipped_small": 0,
             "questions": 0, "skipped_dupq": 0, "raw_chars": 0}
    groups = {}                    # (cls, skey, term) -> group
    unmapped = set()

    for idx, path in enumerate(unique, 1):
        base_name = os.path.basename(path)
        cls, subject, stem = classify_exam(base_name)
        if cls is None:
            stats["skipped_nonexam"] += 1
            continue
        skey = subject_key(subject)
        if skey not in mapped:
            unmapped.add(skey)
            continue
        try:
            lines = docx_lines(path)
        except Exception as e:                       # corrupt zip etc.
            log(f"  !! unreadable: {base_name} ({e})")
            continue
        raw = sum(len(l) for l in lines)
        stats["raw_chars"] += raw
        if raw < 200:
            stats["skipped_small"] += 1
            continue
        term = term_from_header(lines)
        questions = parse_exam(lines)
        if not questions:
            stats["skipped_small"] += 1
            continue

        gkey = (cls, skey, term)
        g = groups.get(gkey)
        if g is None:
            g = groups[gkey] = {"cls": cls, "subject_key": skey,
                                "subject": subject, "term": term,
                                "questions": [], "files": [],
                                "sections": set()}
        g["files"].append(base_name)
        g["questions"].extend(questions)
        g["sections"].update(q["section"] for q in questions)
        stats["files"] += 1
        stats["questions"] += len(questions)
        if idx % 20 == 0 or idx == len(unique):
            log(f"  [{idx:>3}/{len(unique)}] {base_name[:56]:56s} q={len(questions)}")

    if unmapped:
        log("\n!! UNMAPPED EXAM SUBJECTS (add to SUBJECT_MAP / EXAM_SUBJECT_ALIASES):")
        for u in sorted(unmapped):
            log(f"   - {u}")
        con.close()
        return 2

    # ---- assemble concepts --------------------------------------------------
    concepts = []
    for gkey in sorted(groups, key=lambda k: (groups[k]["subject_key"],
                                              base.CLASS_ORDER[k[0]], k[2] or 0)):
        g = groups[gkey]
        cls, skey, term = g["cls"], g["subject_key"], g["term"]
        dom, cat = mapped[skey]
        subj = g["subject"].title().replace(" And ", " and ")
        label = base.CLASS_LABEL[cls]
        tlabel = base.TERM_LABEL.get(term)
        name = f"{label} {subj} Examination" + (f" ({tlabel})" if tlabel else "")

        cid = f"c_exam_{cls}_{base.slugify(g['subject'])}" + (f"_t{term}" if term else "")
        sec_list = sorted(g["sections"])
        n = len(g["questions"])
        summary = (f"{label} {subj} examination question bank"
                   + (f" ({tlabel})" if tlabel else "") +
                   f": {n} practice questions across {len(sec_list)} section"
                   f"{'s' if len(sec_list) != 1 else ''} "
                   f"({', '.join(sec_list[:4])}"
                   f"{', ...' if len(sec_list) > 4 else ''}), patterned on "
                   "BECE/JSCE-style secondary-school assessments.")
        explanation = (
            f"Examination preparation material for {label} {subj}"
            + (f", {tlabel}" if tlabel else "") +
            f" drawn from the Kuzmix Basic Education e-assessment corpus "
            f"({n} questions in {len(sec_list)} sections: "
            f"{'; '.join(sec_list)}). Questions are stored renumbered with "
            "their answer options so students can practise under exam "
            "conditions and teachers can build revision quizzes offline. "
            "Pairs with the corresponding lesson-note concepts for study "
            "then self-testing.")

        qhashes = [sha256_text(norm_text(question_block(q))) for q in g["questions"]]
        content_hash = sha256_text("|".join(qhashes))
        cur.execute("SELECT 1 FROM concepts WHERE id = ? AND content_hash = ?",
                    (cid, content_hash))
        unchanged = cur.fetchone() is not None

        concepts.append({
            "id": cid, "domain": dom, "category": cat, "name": name,
            "canonical": name.lower().strip(), "summary": summary,
            "explanation": explanation, "difficulty": base.DIFFICULTY[cls],
            "hash": content_hash, "questions": g["questions"],
            "sections": sec_list, "cls": cls, "subject": g["subject"],
            "files": g["files"], "unchanged": unchanged,
        })

    total_q = total_chunks = total_tokens = 0
    for c in concepts:
        c["chunks"] = chunk_questions(c["questions"])
        total_q += len(c["questions"])
        total_chunks += len(c["chunks"])
        total_tokens += sum(token_est(t) for _s, t in c["chunks"])

    new_concepts = [c for c in concepts if not c["unchanged"]]

    log("\n" + "-" * 78)
    log("PARSE REPORT" + (" (dry run)" if dry else ""))
    log("-" * 78)
    log(f"  Exam files processed       : {stats['files']:>10,}")
    log(f"  Non-exam docx skipped      : {stats['skipped_nonexam']:>10,}")
    log(f"  Unreadable / empty skipped : {stats['skipped_small']:>10,}")
    log(f"  Byte-identical duplicates  : {len(dupes):>10,}")
    log(f"  Raw extracted characters    : {stats['raw_chars']:>10,}")
    log(f"  Questions kept (deduped)    : {total_q:>10,}")
    log(f"  Concept groups              : {len(concepts):>10,}  (new/changed: {len(new_concepts)})")
    log(f"  Chunks (total)              : {total_chunks:>10,}")
    log(f"  Chunk tokens (est.)         : {total_tokens:>10,}")
    log(f"  New categories              : {len(new_categories):>10,}")
    if args.show_groups:
        log("\n  Concept groups:")
        for c in concepts:
            log(f"    {c['id']:44s} {len(c['questions']):>4d}q "
                f"{len(c['chunks']):>3d}ch  {c['name']}")

    if dry:
        log("\nDRY RUN complete - no database changes were made.")
        con.close()
        return 0

    # ---- write --------------------------------------------------------------
    log("\nWriting...")
    if not new_concepts:
        log("  All exam groups unchanged - finalize only.")

    for cat_id, (dom, cname, cslug, cdesc) in new_categories.items():
        cur.execute("INSERT OR IGNORE INTO categories "
                    "(id, domain_id, name, slug, description, sort_order) "
                    "VALUES (?,?,?,?,?,?)", (cat_id, dom, cname, cslug, cdesc, 90))

    if not cur.execute("SELECT 1 FROM sources WHERE id = ?", (SOURCE_ID,)).fetchone():
        cur.execute("INSERT INTO sources (id, title, author_or_org, source_type, "
                    "uri_or_reference, license, notes) VALUES (?,?,?,?,?,?,?)",
                    (SOURCE_ID, SOURCE_TITLE, SOURCE_ORG,
                     "open_educational_resource", root,
                     "Local corpus (user-provided)",
                     "Ingested from local DOCX examination papers."))

    ins_concept = """
        INSERT INTO concepts (id, topic_id, category_id, domain_id, name,
            canonical_name_normalized, summary_definition, primary_explanation,
            key_properties_json, applications_json, prerequisites_json,
            difficulty_level, confidence_score, provenance_type, source_id,
            content_hash, created_at, updated_at)
        VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?,
                'synthesized_pedagogical', ?, ?,
                strftime('%Y-%m-%dT%H:%M:%SZ','now'),
                strftime('%Y-%m-%dT%H:%M:%SZ','now'))
        ON CONFLICT(id) DO UPDATE SET
            summary_definition = excluded.summary_definition,
            primary_explanation = excluded.primary_explanation,
            key_properties_json = excluded.key_properties_json,
            applications_json = excluded.applications_json,
            difficulty_level = excluded.difficulty_level,
            content_hash = excluded.content_hash,
            source_id = excluded.source_id,
            updated_at = strftime('%Y-%m-%dT%H:%M:%SZ','now')
    """
    ins_chunk = """
        INSERT OR REPLACE INTO chunks (id, concept_id, chunk_index, chunk_type,
            text_content, token_estimate, header_context, created_at)
        VALUES (?, ?, ?, 'exam_questions', ?, ?, ?,
                strftime('%Y-%m-%dT%H:%M:%SZ','now'))
    """
    ins_alias = """
        INSERT OR IGNORE INTO aliases (id, concept_id, alias, alias_normalized, alias_type)
        VALUES (?, ?, ?, ?, 'synonym')
    """
    ins_fts = """
        INSERT INTO knowledge_fts (concept_id, record_id, title, content, aliases,
            category_name, domain_name, keywords)
        VALUES (?, NULL, ?, ?, ?, ?, ?, ?)
    """
    ins_rel = """
        INSERT OR IGNORE INTO relationships (id, source_concept_id, target_concept_id,
            relationship_type, description, weight, is_bidirectional, created_at)
        VALUES (?, ?, ?, 'related_to', ?, ?, 0,
                strftime('%Y-%m-%dT%H:%M:%SZ','now'))
    """

    dom_name = dict(cur.execute("SELECT id, name FROM domains"))
    cat_name = dict(cur.execute("SELECT id, name FROM categories"))
    for cid_cat, (_d, cname, _s, _x) in new_categories.items():
        cat_name[cid_cat] = cname

    w_concepts = w_chunks = w_aliases = w_rels = 0
    for c in concepts:
        if c["unchanged"]:
            continue
        label = base.CLASS_LABEL[c["cls"]]
        subj = c["subject"]
        key_props = [
            {"property": "Class", "value": label},
            {"property": "Subject", "value": subj.title()},
            {"property": "Questions", "value": str(len(c["questions"]))},
            {"property": "Sections", "value": "; ".join(c["sections"])},
            {"property": "Source files", "value": "; ".join(c["files"])},
        ]
        applications = [
            "Examination revision and practice for students",
            "Teacher test-building and revision-quiz drafting",
            "BECE/JSCE patterned assessment preparation",
        ]
        cur.execute(ins_concept, (
            c["id"], c["category"], c["domain"], c["name"], c["canonical"],
            c["summary"], c["explanation"], json.dumps(key_props),
            json.dumps(applications), c["difficulty"], 0.85, SOURCE_ID, c["hash"],
        ))
        w_concepts += 1

        cur.execute("DELETE FROM chunks WHERE concept_id = ?", (c["id"],))
        breadcrumb = (f"{dom_name[c['domain']]} > {cat_name[c['category']]} > "
                      f"{c['name']}")
        for i, (sec, text) in enumerate(c["chunks"]):
            cur.execute(ins_chunk, (
                f"chk_{c['id']}_{i}", c["id"], i, text,
                token_est(text), f"{breadcrumb} > {sec}"))
            w_chunks += 1

        aliases = [f"{label} {subj} exam",
                   f"{subj} examination questions {label}",
                   f"{label} {subj} past questions"]
        alias_texts = []
        for i, a in enumerate(aliases):
            an = re.sub(r"\s+", " ", a).strip().lower()
            cur.execute(ins_alias, (f"al_{c['id']}_{i}", c["id"], a, an))
            alias_texts.append(a)
            w_aliases += 1

        stems = " ".join(q["text"][:90] for q in c["questions"][:40])
        cur.execute("DELETE FROM knowledge_fts WHERE concept_id = ? "
                    "AND record_id IS NULL", (c["id"],))
        cur.execute(ins_fts, (
            c["id"], c["name"], f"{c['summary']} {c['explanation']} {stems}",
            " ".join(alias_texts), cat_name.get(c["category"], ""),
            dom_name.get(c["domain"], ""), "; ".join(c["sections"])))

    # related_to: exam concept -> matching lesson-note concepts
    for c in concepts:
        slug = base.slugify(c["subject"])
        rows = cur.execute(
            "SELECT id FROM concepts WHERE id LIKE ? ESCAPE '\\' "
            "ORDER BY id", (f"c\\_cur\\_{c['cls']}\\_{slug}\\_%",)).fetchall()
        for (target,) in rows:
            if target == c["id"]:
                continue
            cur.execute(ins_rel, (
                f"rel_{c['id']}_{target}_exam", c["id"], target,
                f"Practice questions for the {c['name']} lesson content",
                0.8))
            w_rels += 1

    # ---- truthful metadata counters ----------------------------------------
    counters = [("total_domains", "domains"), ("total_categories", "categories"),
                ("total_topics", "topics"), ("total_concepts", "concepts"),
                ("total_records", "knowledge_records"), ("total_chunks", "chunks"),
                ("total_relationships", "relationships"), ("total_aliases", "aliases"),
                ("total_misconceptions", "misconceptions"),
                ("total_disambiguations", "disambiguations"),
                ("total_comparisons", "comparisons"), ("total_procedures", "procedures"),
                ("total_embeddings", "embedding_metadata"), ("total_sources", "sources"),
                ("total_fts_entries", "knowledge_fts")]
    for key, table in counters:
        n = cur.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0]
        cur.execute("INSERT INTO metadata (key, value, updated_at) VALUES (?, ?, "
                    "strftime('%Y-%m-%dT%H:%M:%SZ','now')) "
                    "ON CONFLICT(key) DO UPDATE SET value = excluded.value, "
                    "updated_at = excluded.updated_at", (key, str(n)))

    stamp = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
    exam_concepts = cur.execute(
        "SELECT COUNT(*) FROM concepts WHERE id LIKE 'c\\_exam\\_%' ESCAPE '\\'"
    ).fetchone()[0]
    for key, val in [("exam_ingest_timestamp", stamp),
                     ("exam_source_files", str(stats["files"])),
                     ("exam_concepts", str(exam_concepts)),
                     ("exam_questions_total", str(total_q)),
                     ("last_ingest_timestamp", stamp)]:
        cur.execute("INSERT INTO metadata (key, value, updated_at) VALUES (?, ?, ?) "
                    "ON CONFLICT(key) DO UPDATE SET value = excluded.value, "
                    "updated_at = excluded.updated_at", (key, val, stamp))

    con.commit()

    # ---- optimize + verify --------------------------------------------------
    log("Optimizing FTS5 + vacuuming...")
    cur.execute("INSERT INTO knowledge_fts(knowledge_fts) VALUES('optimize');")
    cur.fetchall()
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

    size = os.path.getsize(db_path)
    log("\n" + "-" * 78)
    log("EXAM INGEST COMPLETE")
    log("-" * 78)
    log(f"  Concepts written : {w_concepts:,}")
    log(f"  Chunks written   : {w_chunks:,}")
    log(f"  Aliases written  : {w_aliases:,}")
    log(f"  Relationships    : {w_rels:,}")
    log(f"  Database size    : {size:,} bytes ({size / 1_048_576:.1f} MB)")
    log(f"  Elapsed          : {time.time() - t0:.1f}s")
    con.close()
    return 0


def main():
    ap = argparse.ArgumentParser(
        description="Ingest local DOCX examination papers into kuzmix_knowledge.sqlite")
    ap.add_argument("--dry-run", action="store_true",
                    help="parse and report only; never write")
    ap.add_argument("--db", default=None)
    ap.add_argument("--root", default=None,
                    help="DOCX corpus root (default: <workspace>/pdf)")
    ap.add_argument("--limit", type=int, default=0)
    ap.add_argument("--show-groups", action="store_true")
    args = ap.parse_args()
    try:
        sys.exit(run(args))
    except Exception:
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()
