#!/usr/bin/env python3
"""
Kuzmix AI - curriculum ingestion (PDF + DOCX lesson notes)
==========================================================
Extracts the local e-lesson-note corpus (JSS 1 - SS 3, in <workspace>/pdf)
and folds it into kuzmix_knowledge.sqlite as a provenance-linked curriculum
layer. Reads BOTH formats found in the corpus:
  * .pdf  - the 143 lesson-note PDFs
  * .docx - lesson-note Word files that live beside the PDFs (e.g.
            "SS2 PHYSICS LESSON NOTES.docx"). Exam papers (any file with
            EXAMINATION in its name) are excluded here - they belong to
            ingest_docx_exams.py.
Every topic outline is kept: sections as short as 40 characters are
ingested (a WEEK/LESSON heading with a topic list is real scheme-of-work
content and must not be dropped), while pure calendar filler such as
"MIDTERM BREAK" stays excluded via RE_SKIP_TITLE.

Goals:
  SMART    - structure-aware parsing (term markers / WEEK headings / TOPIC
             fallbacks), heading-aware 80-250 token chunking, near-duplicate
             section detection per subject, taxonomy mapping into the existing
             domain/category tree, graph cross-links from lesson topics to
             existing core concepts, FTS5 indexing, truthful metadata counters.
  LIGHT    - byte-identical duplicate files skipped, page furniture (page
             numbers, running headers/footers, scheme-of-work tables of
             contents) stripped, near-identical sections de-duplicated, no raw
             PDF payload stored, VACUUM + FTS optimize at the end.
  SAFE     - idempotent (content-hash gated), single transaction, pre-ingest
             backup, integrity + foreign-key checks, journal left in DELETE
             mode.

Usage:
    python ingest_pdf_corpus.py --dry-run      # measure only, no writes
    python ingest_pdf_corpus.py                # ingest into the real DB
    python ingest_pdf_corpus.py --limit 3      # first 3 unique files only
    python ingest_pdf_corpus.py --db <path>    # ingest into another DB
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
import zlib
import xml.etree.ElementTree as ET

try:
    import pymupdf  # type: ignore
except Exception:  # pragma: no cover - legacy alias
    import fitz as pymupdf  # type: ignore

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

# --------------------------------------------------------------------------
# Paths.  The workspace folder name contains U+2012, which cannot survive argv
# encoding, so it is assembled from chr() rather than embedded literally.
# --------------------------------------------------------------------------
WORKSPACE = "C:/Users/The Kreadive Galaxy/Desktop/My Products/Kuzmix AI"
PROJECT = os.path.join(WORKSPACE, "kuzmix-ai-" + chr(8212) + "-offline-knowledge-core")
DB_DEFAULT = os.path.join(PROJECT, "kuzmix-knowledge", "kuzmix_knowledge.sqlite")
PDF_ROOT_DEFAULT = os.path.join(WORKSPACE, "pdf")
BACKUP_DEFAULT = os.path.join(PROJECT, "kuzmix-knowledge",
                              "kuzmix_knowledge.pre-pdf-ingest.bak.sqlite")

SOURCE_ID = "src_curriculum_elesson_notes"
SOURCE_TITLE = "Nigerian Basic Education E-Lesson Notes (JSS 1 - SS 3)"
SOURCE_ORG = "The Kreadive Galaxy"

REUSE, NEWCAT = "reuse", "new"

# subject_key -> (domain_id, category_id, mode, (new_name, new_description) if NEW)
SUBJECT_MAP = {
    # ---- sci_math ----------------------------------------------------------
    "mathematics": ("sci_math", "cat_math_algebra", REUSE, None),
    "further mathematics": ("sci_math", "cat_math_calculus", REUSE, None),
    "biology": ("sci_math", "cat_bio_cellular", REUSE, None),
    "chemistry": ("sci_math", "cat_chem_general", REUSE, None),
    "physics": ("sci_math", "cat_physics_mechanics", REUSE, None),
    "intermediate science": ("sci_math", "cat_cur_integrated_science", NEWCAT,
        ("Integrated & Intermediate Science",
         "General science taught at JSS level spanning biology, chemistry and physics fundamentals.")),
    # ---- computing_ai ------------------------------------------------------
    "digital technologies": ("computing_ai", "cat_cs_foundations", REUSE, None),
    "computer hardwares and repairs": ("computing_ai", "cat_cs_systems", REUSE, None),
    "computer hardware and gsm repairs": ("computing_ai", "cat_cs_systems", REUSE, None),
    # ---- creative_arts -----------------------------------------------------
    "visual arts": ("creative_arts", "cat_art_design", REUSE, None),
    "cultural and creative arts": ("creative_arts", "cat_art_design", REUSE, None),
    "literature in english": ("creative_arts", "cat_art_narrative", REUSE, None),
    "fashion design and garment making": ("creative_arts", "cat_cur_fashion_beauty", NEWCAT,
        ("Fashion, Garment Making & Beauty Studies",
         "Vocational creative-arts studies covering pattern drafting, garment construction, styling and cosmetology.")),
    "beauty and cosmetology": ("creative_arts", "cat_cur_fashion_beauty", NEWCAT, None),
    # ---- humanities --------------------------------------------------------
    "english studies": ("humanities", "cat_hum_linguistics", REUSE, None),
    "english language": ("humanities", "cat_hum_linguistics", REUSE, None),
    "french": ("humanities", "cat_hum_linguistics", REUSE, None),
    "nigerian history": ("humanities", "cat_hum_history", REUSE, None),
    # ---- society_culture ---------------------------------------------------
    "government": ("society_culture", "cat_soc_civics", REUSE, None),
    "social and citizenship studies": ("society_culture", "cat_soc_civics", REUSE, None),
    "citizenship and heritage studies": ("society_culture", "cat_soc_civics", REUSE, None),
    "geography": ("society_culture", "cat_cur_geography", NEWCAT,
        ("Geography & Environmental Studies",
         "Physical and human geography: landforms, climate, population, resources and map reading.")),
    "christian religious studies": ("society_culture", "cat_cur_religious_studies", NEWCAT,
        ("Religious Studies (CRS & IRS)",
         "Christian and Islamic religious studies taught in Nigerian secondary schools: beliefs, practices, ethics.")),
    "islamic religious studies": ("society_culture", "cat_cur_religious_studies", NEWCAT, None),
    # ---- health_life -------------------------------------------------------
    "food and nutrition": ("health_life", "cat_health_nutrition", REUSE, None),
    "physical and health education": ("health_life", "cat_cur_physical_education", NEWCAT,
        ("Physical & Health Education",
         "School physical education and health education: fitness, games, hygiene and disease prevention.")),
    # ---- practical_life ----------------------------------------------------
    "agricultural science": ("practical_life", "cat_cur_agriculture", NEWCAT,
        ("Agriculture, Crop & Livestock Production",
         "Agricultural science and vocational agriculture: soil, crops, livestock and farm management.")),
    "horticulture and crop production": ("practical_life", "cat_cur_agriculture", NEWCAT, None),
    "livestock farming": ("practical_life", "cat_cur_agriculture", NEWCAT, None),
    "catering and craft practice": ("practical_life", "cat_prac_food_sci", REUSE, None),
    "catering and craft": ("practical_life", "cat_prac_food_sci", REUSE, None),
    # ---- business_econ -----------------------------------------------------
    "economics": ("business_econ", "cat_econ_micro", REUSE, None),
    "commerce": ("business_econ", "cat_biz_marketing", REUSE, None),
    "marketing": ("business_econ", "cat_biz_marketing", REUSE, None),
    "financial accounting": ("business_econ", "cat_biz_accounting", REUSE, None),
    "business studies": ("business_econ", "cat_cur_business_studies", NEWCAT,
        ("Business Studies & Office Practice",
         "Secondary-school business studies: entrepreneurship, office practice and commerce fundamentals.")),
    # ---- eng_tech ----------------------------------------------------------
    "technical drawing": ("eng_tech", "cat_eng_mechanical", REUSE, None),
    "solar installation": ("eng_tech", "cat_eng_electrical", REUSE, None),
    "solar photovoltaic installation": ("eng_tech", "cat_eng_electrical", REUSE, None),
    "solar photovoltaic installation and maintenance": ("eng_tech", "cat_eng_electrical", REUSE, None),
}

DIFFICULTY = {"jss1": "introductory", "jss2": "introductory", "jss3": "introductory",
              "ss1": "intermediate", "ss2": "intermediate", "ss3": "advanced"}
CLASS_LABEL = {"jss1": "JSS 1", "jss2": "JSS 2", "jss3": "JSS 3",
               "ss1": "SS 1", "ss2": "SS 2", "ss3": "SS 3"}
CLASS_ORDER = {"jss1": 1, "jss2": 2, "jss3": 3, "ss1": 4, "ss2": 5, "ss3": 6}
TERM_LABEL = {1: "First Term", 2: "Second Term", 3: "Third Term"}

# Chunking budget (tokens, len//4 convention). Body packed to ~150-220;
# the breadcrumb prefix (domain > category > concept > lesson) adds ~30-60,
# keeping final passages inside the schema's 80-250 token contract.
CH_TARGET, CH_MAX, CH_MIN = 150, 220, 60

# --------------------------------------------------------------------------
# Regexes
# --------------------------------------------------------------------------
WEEK_NUM = (r"(?:ONE|TWO|THREE|FOUR|FIVE|SIX|SEVEN|EIGHT|NINE|TEN|ELEVEN|TWELVE|"
            r"THIRTEEN|FOURTEEN|FIFTEEN|SIXTEEN|SEVENTEEN|EIGHTEEN|NINETEEN|TWENTY|\d{1,2})")
RE_WEEK = re.compile(rf"^\s*WEEK\s+({WEEK_NUM})\b\s*([-–—:.]\s*)?(.*?)\s*$", re.I)
RE_LESSON = re.compile(r"^\s*LESSON\s+(\d{1,2})\b\s*([-–—:.]\s*)?(.*?)\s*$", re.I)
RE_TOPIC_START = re.compile(r"^\s*TOPIC\s+(\d{1,2})\b\s*([-–—:.]\s*)?(.*?)\s*$", re.I)
RE_TOPIC_LINE = re.compile(r"^\s*TOPIC\s*[-–—:]\s*(.+?)\s*$", re.I)
RE_TERM = re.compile(r"^\s*(?:(FIRST|SECOND|THIRD|FOURTH|1ST|2ND|3RD|4TH)\s+TERM|TERM\s*([123]))\s*$", re.I)
RE_SCHEME = re.compile(r"^\s*(SCHEME\s+OF\s+WORK|WEEKLY\s+SCHEME|CONTENTS)\b", re.I)
RE_NUMBERED = re.compile(r"^\s*\d{1,2}\s*[.)]\s+\S")
RE_PAGENUM = re.compile(r"^\s*\d{1,4}\s*$")
RE_SKIP_TITLE = re.compile(r"(?i)\b(mid\s*-?\s*term|break|final examination|closing|"
                           r"term examination|end of term)\b")

BULLET_CHARS = "\uf0b7\u25cf\u25aa\u2022\u25a0"
STRIP_CHARS = re.compile(r"[\u0000-\u0008\u000b-\u001f\u007f-\u009f\u2500-\u25ff\uf000-\uf8ff]")


# --------------------------------------------------------------------------
# Helpers
# --------------------------------------------------------------------------
def log(msg: str = "") -> None:
    print(msg, flush=True)


def sha256_text(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def token_est(text: str) -> int:
    return max(1, len(text) // 4)          # same convention as build_sqlite.py


def slugify(text: str) -> str:
    s = text.lower().replace("&", " and ")
    s = re.sub(r"[^a-z0-9]+", "_", s)
    return re.sub(r"_+", "_", s).strip("_")


def norm_text(text: str) -> str:
    s = text.lower()
    s = re.sub(r"[^a-z0-9]+", " ", s)
    return re.sub(r"\s+", " ", s).strip()


def phrase_pad(s: str) -> str:
    return " " + re.sub(r"\s+", " ", s).strip() + " "


def term_from_marker(word, digit) -> int:
    if digit:
        return int(digit)
    return {"FIRST": 1, "SECOND": 2, "THIRD": 3, "FOURTH": 4,
            "1ST": 1, "2ND": 2, "3RD": 3, "4TH": 4}.get((word or "").upper(), 1)


def display_subject(subject: str) -> str:
    s = re.sub(r"\s+", " ", subject).strip().title()
    s = re.sub(r"\b(And|In|Of|The)\b", lambda m: m.group(0).lower(), s)
    return s


def subject_key(subject: str) -> str:
    s = subject.lower().replace("&", " and ")
    s = re.sub(r"\([^)]*\)", " ", s)
    s = re.sub(r"[^a-z0-9]+", " ", s)
    return re.sub(r"\s+", " ", s).strip()


# --------------------------------------------------------------------------
# Extraction / cleaning
# --------------------------------------------------------------------------
def extract_pdf(path: str):
    doc = pymupdf.open(path)
    pages = []
    for i in range(len(doc)):
        try:
            pages.append(doc[i].get_text())
        except Exception:
            pages.append("")
    doc.close()
    return pages


# DOCX lesson notes: a .docx is a ZIP of XML, so the standard library is
# enough (no python-docx dependency). Paragraph breaks are preserved so the
# WEEK/LESSON/TOPIC heading parser sees one heading per line.
W_NS = "{http://schemas.openxmlformats.org/wordprocessingml/2006/main}"


def extract_docx(path: str):
    """-> [text] - one pseudo-page holding the whole document."""
    with zipfile.ZipFile(path) as z:
        xml = z.read("word/document.xml")
    root = ET.fromstring(xml)
    lines = []

    def para_text(p):
        parts = []
        for node in p.iter():
            if node.tag == W_NS + "t":
                parts.append(node.text or "")
            elif node.tag == W_NS + "tab":
                parts.append("\t")
            elif node.tag == W_NS + "br":
                parts.append("\n")
        return "".join(parts)

    body = root.find(W_NS + "body")
    if body is None:
        return [""]
    for child in body:
        if child.tag == W_NS + "p":
            lines.append(para_text(child))
        elif child.tag == W_NS + "tbl":
            for row in child.iter(W_NS + "tr"):
                cells = [para_text(p) for p in row.iter(W_NS + "p")]
                lines.append(" | ".join(c for c in cells if c))
    return ["\n".join(lines)]


def extract_pages(path: str):
    lower = path.lower()
    if lower.endswith(".docx"):
        return extract_docx(path)
    if lower.endswith(".pdf"):
        return extract_pdf(path)
    return []


def clean_page(text: str) -> str:
    text = text.replace("\r\n", "\n").replace("\r", "\n")
    for b in BULLET_CHARS:
        text = text.replace(b, " - ")
    text = re.sub(r"(\w)-\n(\w)", r"\1\2", text)     # de-hyphenate wrapped words
    text = text.replace("\u00a0", " ")
    text = text.replace("\u2018", "'").replace("\u2019", "'")
    text = text.replace("\u201c", '"').replace("\u201d", '"')
    return STRIP_CHARS.sub("", text)


def drop_page_furniture(pages):
    """Remove per-page page-numbers and document-wide running headers/footers."""
    cleaned = [clean_page(p) for p in pages]
    n = max(1, len(cleaned))
    freq = {}
    for p in cleaned:
        seen = set()
        for line in p.splitlines():
            key = re.sub(r"\s+", " ", line).strip().lower()
            if not key or key in seen or len(key) > 80:
                continue
            seen.add(key)
            freq[key] = freq.get(key, 0) + 1
    repeated = {k for k, v in freq.items() if v >= max(3, 0.15 * n)}
    out = []
    for p in cleaned:
        keep = []
        for line in p.splitlines():
            if RE_PAGENUM.match(line):
                continue
            key = re.sub(r"\s+", " ", line).strip().lower()
            if key in repeated:
                continue
            keep.append(line.rstrip())
        out.append("\n".join(keep))
    return out


# --------------------------------------------------------------------------
# Structure parsing
# --------------------------------------------------------------------------
def parse_sections(pages, fallback_title: str):
    text = "\n".join(pages)
    lines = text.splitlines()

    sections = []
    preamble = []
    cur = None                 # {"term":, "title":, "text": ""}
    cur_term = None
    first_term = None

    def close():
        nonlocal cur
        if cur is not None:
            cur["text"] = "\n".join(cur.pop("lines")).strip()
            sections.append(cur)
            cur = None

    def start(term, title, hint):
        nonlocal cur
        close()
        cur = {"term": term, "title": (title or hint or "Lesson").strip(), "lines": []}

    i = 0
    while i < len(lines):
        stripped = lines[i].strip()

        mterm = RE_TERM.match(stripped)
        if mterm:
            t = term_from_marker(mterm.group(1), mterm.group(2))
            cur_term = t
            first_term = first_term or t
            close()
            i += 1
            continue

        if RE_SCHEME.match(stripped):
            i += 1
            continue

        m, kind = RE_WEEK.match(stripped), "week"
        if not m:
            m, kind = RE_LESSON.match(stripped), "lesson"
        if not m:
            m, kind = RE_TOPIC_START.match(stripped), "topic"

        if m:
            sep, title = m.group(2), (m.group(3) or "").strip()
            if title and not sep and not title[0].isupper():
                m = None                          # prose mentioning "week 3 ..."
            else:
                if not title:
                    j = i + 1
                    while j < len(lines) and not lines[j].strip():
                        j += 1
                    if j < len(lines):
                        mt = RE_TOPIC_LINE.match(lines[j].strip())
                        if mt:
                            title = mt.group(1).strip()
                            i = j                  # consume the TOPIC: line
                    if not title:
                        title = (f"Week {m.group(1)}" if kind == "week"
                                 else f"{kind.title()} {m.group(1)}")
                else:
                    title = re.sub(r"^\d+\s*[).]\s*", "", title)
                start(cur_term, title, f"Week {m.group(1)}")
                i += 1
                continue

        if cur is None:
            preamble.append(stripped)
        else:
            cur["lines"].append(lines[i])
        i += 1

    close()

    if sections:
        # scheme-of-work / TOC lines before the first heading duplicate section
        # titles: drop numbered entries, keep any genuine prose.
        kept = [l for l in preamble if not (RE_NUMBERED.match(l) or RE_SCHEME.match(l))]
        if kept:
            first = sections[0]
            first["text"] = ("\n".join(kept) + "\n" + first["text"]).strip()
        if first_term is not None:
            for s in sections:
                if s["term"] is None:
                    s["term"] = first_term
    else:
        whole = "\n".join(preamble).strip()
        if whole:
            sections = [{"term": cur_term or first_term,
                         "title": fallback_title, "text": whole}]
    return sections


# --------------------------------------------------------------------------
# Heading-aware chunking
# --------------------------------------------------------------------------
def is_heading_line(line: str) -> bool:
    b = line.strip()
    if len(b) < 3 or len(b) > 90:
        return False
    if re.search(r"[.!?]$", b) and not b.endswith(":"):
        return False
    letters = [c for c in b if c.isalpha()]
    if len(letters) < 3:
        return False
    return sum(1 for c in letters if c.isupper()) / len(letters) >= 0.7


def section_blocks(text: str):
    """Yield (heading, block_text); heading persists until the next heading."""
    heading, buf = None, []
    for raw in text.splitlines():
        line = raw.strip()
        if not line:
            if buf:
                yield heading, " ".join(buf).strip()
                buf = []
            continue
        if is_heading_line(line):
            if buf:
                yield heading, " ".join(buf).strip()
                buf = []
            heading = re.sub(r"\s+", " ", line).strip(" :;-")
            continue
        buf.append(line)
    if buf:
        yield heading, " ".join(buf).strip()


def split_sentences(text: str):
    parts = re.split(r"(?<=[.!?;])\s+(?=[A-Z0-9\"'(])|\s+-\s+(?=[A-Z0-9])", text)
    return [re.sub(r"\s+", " ", p).strip() for p in parts if p.strip()]


def hard_split(sentence: str, max_toks: int):
    if token_est(sentence) <= max_toks:
        return [sentence]
    out, buf = [], []
    for w in sentence.split():
        buf.append(w)
        if token_est(" ".join(buf)) >= max_toks:
            out.append(" ".join(buf))
            buf = []
    if buf:
        out.append(" ".join(buf))
    return out


def chunk_section(section):
    """Return [{text, token_estimate, chunk_type}] - self-contained passages."""
    title = section["title"]
    atoms = []
    for heading, block in section_blocks(section["text"]):
        if not block:
            continue
        for sent in split_sentences(block):
            for piece in hard_split(sent, CH_MAX):
                atoms.append((heading, piece))
    if not atoms:
        return []

    chunks = []
    body, toks = [], 0
    pending = None
    last_heading = object()

    def ctx_for(heading):
        return title if not heading else f"{title} | {heading}"

    def flush():
        nonlocal body, toks, pending
        if not body:
            return
        text = ((pending + ": ") if pending else "") + " ".join(body)
        chunks.append({"text": text, "token_estimate": token_est(text),
                       "chunk_type": "lesson_notes"})
        body, toks, pending = [], 0, None

    for heading, sent in atoms:
        if heading != last_heading:
            if body:
                flush()
            pending = ctx_for(heading)
            last_heading = heading
        if toks and toks + token_est(sent) > CH_MAX:
            flush()
            if pending is None:
                pending = ctx_for(heading)
        if not body and pending is None:
            pending = ctx_for(heading)
        body.append(sent)
        toks += token_est(sent)
        if toks >= CH_TARGET:
            flush()
    if body:
        flush()
        if len(chunks) >= 2 and chunks[-1]["token_estimate"] < CH_MIN:
            tail = chunks.pop()
            merged = chunks[-1]["text"] + " " + tail["text"]
            if token_est(merged) <= 250:
                chunks[-1] = {"text": merged, "token_estimate": token_est(merged),
                              "chunk_type": "lesson_notes"}
            else:
                chunks.append(tail)
    return chunks


# --------------------------------------------------------------------------
# Duplicate detection
# --------------------------------------------------------------------------
def shingles(words, k=5, cap=400):
    if not words:
        return set()
    if len(words) < k:
        return {zlib.crc32(" ".join(words).encode())}
    step = max(1, (len(words) - k + 1) // cap)
    return {zlib.crc32(" ".join(words[i:i + k]).encode())
            for i in range(0, len(words) - k + 1, step)}


def jaccard(a: set, b: set) -> float:
    if not a or not b:
        return 0.0
    inter = len(a & b)
    return 0.0 if not inter else inter / (len(a) + len(b) - inter)


# --------------------------------------------------------------------------
# Corpus walk
# --------------------------------------------------------------------------
def classify_filename(filename: str):
    stem = os.path.splitext(filename)[0]
    stem = re.sub(r"\(\d+\)$", "", stem).strip()
    m = re.match(r"^(JSS|SS)\s*([123])\s*(.*)$", stem, re.I)
    if not m:
        return None, None, stem
    cls = (m.group(1) + m.group(2)).lower()
    subject = m.group(3)
    subject = re.sub(r"(?i)\s*LESSON\s+NOTES\s*\d*$", "", subject)
    subject = re.sub(r"(?i)\s+NOTES\s*\d*$", "", subject)
    subject = re.sub(r"\s+", " ", subject).strip(" -")
    return cls, subject, stem


def collect_pdfs(pdf_root: str):
    """Unique (hash, path) lesson-note files - PDF and DOCX alike.

    Exam papers (.docx with EXAMINATION in the name) are left to
    ingest_docx_exams.py so the two ingesters never double-count them.
    """
    exam_stem = re.compile(r"(?i)\bEXAMINATION\b")
    files = []
    for dp, _dn, fn in os.walk(pdf_root):
        for f in fn:
            low = f.lower()
            if low.endswith(".pdf"):
                files.append(os.path.join(dp, f))
            elif low.endswith(".docx") and not exam_stem.search(os.path.splitext(f)[0]):
                files.append(os.path.join(dp, f))
    files.sort()
    seen, unique, dupes = set(), [], []
    for p in files:
        h = hashlib.sha256(open(p, "rb").read()).hexdigest()
        if h in seen:
            dupes.append(p)
            continue
        seen.add(h)
        unique.append((h, p))
    return unique, dupes


def build_concept_id(cls: str, subject: str, term, used_ids: set) -> str:
    base = f"c_cur_{cls}_{slugify(subject)}" + (f"_t{term}" if term else "")
    cid, n = base, 2
    while cid in used_ids:
        cid = f"{base}_{n}"
        n += 1
    used_ids.add(cid)
    return cid


# --------------------------------------------------------------------------
# Main
# --------------------------------------------------------------------------
def run(args) -> int:
    t0 = time.time()
    db_path = args.db or DB_DEFAULT
    pdf_root = args.pdf_root or PDF_ROOT_DEFAULT
    dry = args.dry_run

    log("=" * 78)
    log("KUZMIX AI :: PDF CURRICULUM INGESTION" + ("   [DRY RUN]" if dry else ""))
    log("=" * 78)
    log(f"  PDF root : {pdf_root}")
    log(f"  Database : {db_path}")

    if not os.path.exists(db_path):
        log("!! database not found")
        return 1
    if not os.path.isdir(pdf_root):
        log("!! pdf root not found")
        return 1

    # Pre-ingest backup (before the connection is opened).
    if not dry and not os.path.exists(BACKUP_DEFAULT):
        shutil.copy2(db_path, BACKUP_DEFAULT)
        log(f"  Backup   : {os.path.basename(BACKUP_DEFAULT)}")

    unique, dupes = collect_pdfs(pdf_root)
    if args.limit:
        unique = unique[:args.limit]
    log(f"\nFiles: {len(unique) + len(dupes)} total | "
        f"{len(dupes)} byte-identical duplicates skipped | {len(unique)} to process\n")

    con = sqlite3.connect(db_path)
    cur = con.cursor()
    cur.execute("PRAGMA foreign_keys = ON")
    if not dry:
        con.execute("BEGIN")

    # ---- existing taxonomy + concept lookup tables --------------------------
    existing_categories = {r[0] for r in cur.execute("SELECT id FROM categories")}
    domain_ids = {r[0] for r in cur.execute("SELECT id FROM domains")}
    canon = dict(cur.execute("SELECT canonical_name_normalized, id FROM concepts "
                             "WHERE id NOT LIKE 'c_cur\\_%' ESCAPE '\\'"))
    alias_map = dict(cur.execute("SELECT alias_normalized, concept_id FROM aliases "
                                 "WHERE concept_id NOT LIKE 'c_cur\\_%' ESCAPE '\\'"))
    # ids are deterministic per (class, subject, term) and prefixed c_cur_,
    # so a base-id match means "our own prior ingest" -> reuse (upsert).
    # used_ids only guards against intra-run slug collisions.
    used_ids = set()
    canon_padded = [(phrase_pad(k), v) for k, v in canon.items() if k]
    log(f"Existing: {len(canon)} concepts, {len(existing_categories)} categories, "
        f"{len(alias_map)} aliases loaded")

    # ---- new categories (declared in SUBJECT_MAP) ---------------------------
    new_categories = {}
    mapped = {}
    for key, (dom, cat, mode, meta) in SUBJECT_MAP.items():
        mapped[key] = (dom, cat)
        if mode == NEWCAT and cat not in existing_categories and cat not in new_categories:
            if dom not in domain_ids:
                raise RuntimeError(f"unknown domain: {dom}")
            new_categories[cat] = (dom, meta[0], slugify(meta[0]), meta[1])

    # ---- corpus walk --------------------------------------------------------
    stats = {"sections": 0, "chunks": 0, "tokens": 0, "skipped_short": 0,
             "skipped_dupe": 0, "raw_chars": 0, "clean_chars": 0, "skipped": []}
    subject_shingles = {}        # subject_key -> [(hash, shingle_set)]
    groups = {}                  # (cls, subject_key, term) -> group
    unmapped = set()
    cross_links = []             # (cls, subject_key, term, target_id, title)

    for idx, (_fh, path) in enumerate(unique, 1):
        base = os.path.basename(path)
        cls, subject, stem = classify_filename(base)
        if cls is None:
            stats["skipped"].append((base, "unclassified filename"))
            continue
        skey = subject_key(subject)
        if skey not in SUBJECT_MAP:
            unmapped.add(skey)
            stats["skipped"].append((base, f"unmapped subject: '{skey}'"))
            continue

        pages = extract_pages(path)
        raw = sum(len(p) for p in pages)
        stats["raw_chars"] += raw
        if raw < 400:
            stats["skipped"].append((base, "no extractable text"))
            continue
        sections = parse_sections(drop_page_furniture(pages), stem)
        if not sections:
            stats["skipped"].append((base, "no sections parsed"))
            continue

        kept = 0
        for s in sections:
            clean = re.sub(r"\s+", " ", s["text"]).strip()
            stats["clean_chars"] += len(clean)
            # 40 chars (~10 tokens): a WEEK/LESSON heading plus a topic list
            # is genuine scheme-of-work content - keep it. Calendar filler
            # ("MIDTERM BREAK") is still dropped by RE_SKIP_TITLE below.
            if len(clean) < 40:
                stats["skipped_short"] += 1
                continue
            if RE_SKIP_TITLE.search(s["title"]) and token_est(clean) < 200:
                stats["skipped_short"] += 1
                continue

            nrm = norm_text(clean)
            h = sha256_text(nrm)
            sh = shingles(nrm.split())
            dup = False
            for _ph, prior in subject_shingles.get(skey, []):
                if jaccard(sh, prior) >= 0.95:
                    dup = True
                    break
            if dup:
                stats["skipped_dupe"] += 1
                continue
            subject_shingles.setdefault(skey, []).append((h, sh))

            gkey = (cls, skey, s["term"])
            g = groups.get(gkey)
            if g is None:
                g = groups[gkey] = {"cls": cls, "subject_key": skey,
                                    "subject": subject, "term": s["term"],
                                    "sections": [], "hashes": []}
            g["sections"].append({"title": s["title"], "text": clean, "hash": h})
            g["hashes"].append(h)
            stats["sections"] += 1
            kept += 1

        for s in sections:
            nt = norm_text(s["title"])
            if len(nt) < 14:
                continue
            target = canon.get(nt) or alias_map.get(nt)
            if target is None:
                ntp = phrase_pad(nt)
                for cp, cid in canon_padded:
                    if len(cp) - 2 >= len(nt) and ntp in cp:
                        target = cid
                        break
            if target:
                cross_links.append((cls, skey, s["term"], target, s["title"]))

        if idx % 10 == 0 or idx == len(unique):
            log(f"  [{idx:>3}/{len(unique)}] {base[:56]:56s} sections={kept}")

    if unmapped:
        log("\n!! UNMAPPED SUBJECTS (add to SUBJECT_MAP):")
        for u in sorted(unmapped):
            log(f"   - {u}")
        con.close()
        return 2

    # ---- assemble concepts --------------------------------------------------
    concepts = []
    key_to_id = {}
    for gkey in sorted(groups, key=lambda k: (groups[k]["subject_key"],
                                              CLASS_ORDER[k[0]], k[2] or 0)):
        g = groups[gkey]
        cls, skey, term = g["cls"], g["subject_key"], g["term"]
        dom, cat = mapped[skey]
        subj = display_subject(g["subject"])
        label = CLASS_LABEL[cls]
        tlabel = TERM_LABEL.get(term)
        name = f"{label} {subj}" + (f" ({tlabel})" if tlabel else "")

        cid = build_concept_id(cls, g["subject"], term, used_ids)
        key_to_id[gkey] = cid
        titles = [s["title"] for s in g["sections"]]
        n = len(titles)

        summary = (f"{tlabel or 'Full-year'} lesson notes for {label} {subj}: "
                   f"{n} structured lesson{'s' if n != 1 else ''} covering "
                   f"\"{titles[0]}\"" + (f" through \"{titles[-1]}\"" if n > 1 else "") + ".")
        title_list = ", ".join(titles[:60]) + (" ..." if n > 60 else "")
        explanation = (
            f"{label} {subj}" + (f", {tlabel}" if tlabel else "") +
            f" curriculum notes from the Kuzmix Basic Education e-lesson corpus "
            f"({n} weekly lessons). Topics covered: {title_list}. "
            "Each lesson preserves the source objectives, explanations, worked "
            "examples and evaluation questions, organized for offline full-text "
            "and RAG retrieval.")

        prereq = None
        if term and term > 1:
            cand = f"c_cur_{cls}_{slugify(g['subject'])}_t{term - 1}"
            if cand in used_ids:
                prereq = cand
        else:
            ci = CLASS_ORDER[cls] - 1
            if ci > 0:
                order = ["jss1", "jss2", "jss3", "ss1", "ss2", "ss3"]
                prev_cls = order[ci - 1]
                for t in (3, 2, 1, None):
                    cand = (f"c_cur_{prev_cls}_{slugify(g['subject'])}" +
                            (f"_t{t}" if t else ""))
                    if cand in used_ids:
                        prereq = cand
                        break

        content_hash = sha256_text("|".join(g["hashes"]))
        cur.execute("SELECT 1 FROM concepts WHERE id = ? AND content_hash = ?",
                    (cid, content_hash))
        unchanged = cur.fetchone() is not None

        concepts.append({
            "id": cid, "domain": dom, "category": cat, "name": name,
            "canonical": name.lower().strip(), "summary": summary,
            "explanation": explanation, "prereq": prereq,
            "difficulty": DIFFICULTY[cls], "hash": content_hash,
            "sections": g["sections"], "cls": cls, "term": term,
            "subject": g["subject"], "titles": titles, "unchanged": unchanged,
        })

    # ---- chunk everything (also in dry run, for accurate counts) ------------
    total_chunks = total_tokens = 0
    for c in concepts:
        c["chunks"] = []
        for s in c["sections"]:
            for ch in chunk_section(s):
                ch["section_title"] = s["title"]
                ch["chunk_index"] = len(c["chunks"])
                c["chunks"].append(ch)
        if not c["unchanged"]:
            total_chunks += len(c["chunks"])
            total_tokens += sum(ch["token_estimate"] for ch in c["chunks"])
        else:
            total_chunks += len(c["chunks"])
            total_tokens += sum(ch["token_estimate"] for ch in c["chunks"])

    new_concepts = [c for c in concepts if not c["unchanged"]]

    log("\n" + "-" * 78)
    log("PARSE REPORT" + (" (dry run)" if dry else ""))
    log("-" * 78)
    log(f"  Files processed             : {len(unique):>10,}")
    log(f"  Duplicate files skipped     : {len(dupes):>10,}")
    log(f"  Raw extracted characters    : {stats['raw_chars']:>10,}  (~{stats['raw_chars']//6:,} words)")
    log(f"  Cleaned characters          : {stats['clean_chars']:>10,}")
    log(f"  Sections kept               : {stats['sections']:>10,}")
    log(f"  Sections skipped (too short): {stats['skipped_short']:>10,}")
    log(f"  Sections skipped (near-dup) : {stats['skipped_dupe']:>10,}")
    log(f"  Concept groups              : {len(concepts):>10,}  (new/changed: {len(new_concepts)})")
    log(f"  Chunks (total)              : {total_chunks:>10,}")
    log(f"  Chunk tokens (est.)         : {total_tokens:>10,}")
    log(f"  New categories              : {len(new_categories):>10,}")
    log(f"  Cross-links to core         : {len(cross_links):>10,}")
    if stats["skipped"]:
        log("  Skipped files:")
        for f, why in stats["skipped"]:
            log(f"    - {f[:68]:68s} ({why})")
    if args.show_groups:
        log("\n  Concept groups:")
        for c in concepts:
            log(f"    {c['id']:46s} {len(c['chunks']):>4d} chunks  {c['name']}")

    if dry:
        log("\nDRY RUN complete - no database changes were made.")
        con.close()
        return 0

    # ---- write --------------------------------------------------------------
    log("\nWriting...")
    if not new_concepts:
        log("  All concept groups unchanged - finalize only.")

    for cat_id, (dom, cname, cslug, cdesc) in new_categories.items():
        cur.execute("INSERT OR IGNORE INTO categories "
                    "(id, domain_id, name, slug, description, sort_order) "
                    "VALUES (?,?,?,?,?,?)", (cat_id, dom, cname, cslug, cdesc, 90))

    if not cur.execute("SELECT 1 FROM sources WHERE id = ?", (SOURCE_ID,)).fetchone():
        cur.execute("INSERT INTO sources (id, title, author_or_org, source_type, "
                    "uri_or_reference, license, notes) VALUES (?,?,?,?,?,?,?)",
                    (SOURCE_ID, SOURCE_TITLE, SOURCE_ORG, "open_educational_resource",
                     pdf_root, "Local corpus (user-provided)",
                     "Ingested from local PDF e-lesson notes."))

    ins_concept = """
        INSERT INTO concepts (id, topic_id, category_id, domain_id, name,
            canonical_name_normalized, summary_definition, primary_explanation,
            key_properties_json, applications_json, prerequisites_json,
            difficulty_level, confidence_score, provenance_type, source_id,
            content_hash, created_at, updated_at)
        VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                'synthesized_pedagogical', ?, ?,
                strftime('%Y-%m-%dT%H:%M:%SZ','now'),
                strftime('%Y-%m-%dT%H:%M:%SZ','now'))
        ON CONFLICT(id) DO UPDATE SET
            summary_definition = excluded.summary_definition,
            primary_explanation = excluded.primary_explanation,
            key_properties_json = excluded.key_properties_json,
            applications_json = excluded.applications_json,
            prerequisites_json = excluded.prerequisites_json,
            difficulty_level = excluded.difficulty_level,
            content_hash = excluded.content_hash,
            source_id = excluded.source_id,
            updated_at = strftime('%Y-%m-%dT%H:%M:%SZ','now')
    """
    ins_chunk = """
        INSERT OR REPLACE INTO chunks (id, concept_id, chunk_index, chunk_type,
            text_content, token_estimate, header_context, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, strftime('%Y-%m-%dT%H:%M:%SZ','now'))
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
        VALUES (?, ?, ?, ?, ?, ?, 0, strftime('%Y-%m-%dT%H:%M:%SZ','now'))
    """

    dom_name = dict(cur.execute("SELECT id, name FROM domains"))
    cat_name = dict(cur.execute("SELECT id, name FROM categories"))
    for cid_cat, (_d, cname, _s, _x) in new_categories.items():
        cat_name[cid_cat] = cname

    w_concepts = w_chunks = w_aliases = w_rels = 0

    for c in concepts:
        if c["unchanged"]:
            continue
        key_props = [
            {"property": "Level",
             "value": "Junior Secondary" if c["cls"].startswith("jss") else "Senior Secondary"},
            {"property": "Class", "value": CLASS_LABEL[c["cls"]]},
            {"property": "Term", "value": TERM_LABEL.get(c["term"], "Full year")},
            {"property": "Weekly lessons", "value": str(len(c["titles"]))},
        ]
        applications = [
            f"Study and revision notes for {CLASS_LABEL[c['cls']]} {display_subject(c['subject'])}",
            "Teacher lesson-planning and classroom reference",
            "Terminal and common examination preparation",
        ]
        cur.execute(ins_concept, (
            c["id"], c["category"], c["domain"], c["name"], c["canonical"],
            c["summary"], c["explanation"], json.dumps(key_props),
            json.dumps(applications),
            json.dumps([c["prereq"]]) if c["prereq"] else None,
            c["difficulty"], 0.85, SOURCE_ID, c["hash"],
        ))
        w_concepts += 1

        cur.execute("DELETE FROM chunks WHERE concept_id = ?", (c["id"],))
        breadcrumb = (f"{dom_name[c['domain']]} > {cat_name[c['category']]} > "
                      f"{c['name']}")
        for ch in c["chunks"]:
            cur.execute(ins_chunk, (
                f"chk_{c['id']}_{ch['chunk_index']}", c["id"], ch["chunk_index"],
                ch["chunk_type"], ch["text"], ch["token_estimate"],
                f"{breadcrumb} > {ch['section_title']}"))
            w_chunks += 1

        aliases = [f"{c['subject']} {CLASS_LABEL[c['cls']]}",
                   f"{CLASS_LABEL[c['cls']]} {c['subject']}"]
        if c["term"]:
            aliases.append(f"{c['subject']} {CLASS_LABEL[c['cls']]} "
                           f"{TERM_LABEL[c['term']]}")
        alias_texts = []
        for i, a in enumerate(aliases):
            an = re.sub(r"\s+", " ", a).strip().lower()
            if not an:
                continue
            cur.execute(ins_alias, (f"al_{c['id']}_{i}", c["id"], a, an))
            alias_texts.append(a)
            w_aliases += 1

        cur.execute("DELETE FROM knowledge_fts WHERE concept_id = ? "
                    "AND record_id IS NULL", (c["id"],))
        cur.execute(ins_fts, (
            c["id"], c["name"], f"{c['summary']} {c['explanation']}",
            " ".join(alias_texts), cat_name.get(c["category"], ""),
            dom_name.get(c["domain"], ""), "; ".join(c["titles"])))

    # prerequisite chains: earlier term/class -> later term/class
    for c in concepts:
        if c["unchanged"] or not c["prereq"]:
            continue
        cur.execute(ins_rel, (
            f"rel_{c['prereq']}_{c['id']}_prereq", c["prereq"], c["id"],
            "prerequisite_of",
            f"Earlier work leads into {c['name']}", 0.9))
        w_rels += 1

    # cross-links: lesson topics -> existing core concepts
    seen_pairs = set()
    for cls, skey, term, target, title in cross_links:
        cid = key_to_id.get((cls, skey, term))
        if not cid or cid == target or (cid, target) in seen_pairs:
            continue
        seen_pairs.add((cid, target))
        cur.execute(ins_rel, (
            f"rel_{cid}_{target}_related", cid, target, "related_to",
            f'Lesson topic "{title}" aligns with this knowledge concept', 0.85))
        w_rels += 1

    # ---- truthful metadata counters ---------------------------------------
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
    cur_concepts = cur.execute(
        "SELECT COUNT(*) FROM concepts WHERE id LIKE 'c\\_cur\\_%' ESCAPE '\\'"
    ).fetchone()[0]
    for key, val in [("curriculum_ingest_timestamp", stamp),
                     ("curriculum_source_files", str(len(unique))),
                     ("curriculum_concepts", str(cur_concepts)),
                     ("curriculum_chunks", str(total_chunks)),
                     ("last_ingest_timestamp", stamp)]:
        cur.execute("INSERT INTO metadata (key, value, updated_at) VALUES (?, ?, ?) "
                    "ON CONFLICT(key) DO UPDATE SET value = excluded.value, "
                    "updated_at = excluded.updated_at", (key, val, stamp))

    con.commit()

    # ---- optimize + verify --------------------------------------------------
    log("Optimizing FTS5 + vacuuming...")
    cur.execute("INSERT INTO knowledge_fts(knowledge_fts) VALUES('optimize');")
    cur.fetchall()                     # drain FTS optimize result
    cur.execute("PRAGMA optimize;")
    cur.fetchall()                     # drain ANALYZE result
    con.commit()                       # FTS optimize / ANALYZE are transactional
    cur.execute("PRAGMA journal_mode=DELETE;")
    cur.fetchall()                     # pragma returns the mode row
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
    log("INGEST COMPLETE")
    log("-" * 78)
    log(f"  Concepts written : {w_concepts:,}")
    log(f"  Chunks written   : {w_chunks:,}")
    log(f"  Aliases written  : {w_aliases:,}")
    log(f"  Relationships    : {w_rels:,}")
    log(f"  New categories   : {len(new_categories)}")
    log(f"  Database size    : {size:,} bytes ({size / 1_048_576:.1f} MB)")
    log(f"  Elapsed          : {time.time() - t0:.1f}s")
    con.close()
    return 0


def main():
    ap = argparse.ArgumentParser(
        description="Ingest the local PDF lesson-note corpus into kuzmix_knowledge.sqlite")
    ap.add_argument("--dry-run", action="store_true",
                    help="parse and report only; never write")
    ap.add_argument("--db", default=None,
                    help="target database (default: project kuzmix_knowledge.sqlite)")
    ap.add_argument("--pdf-root", default=None,
                    help="PDF corpus root (default: <workspace>/pdf)")
    ap.add_argument("--limit", type=int, default=0,
                    help="process only the first N unique files")
    ap.add_argument("--show-groups", action="store_true",
                    help="list every concept group")
    args = ap.parse_args()
    try:
        sys.exit(run(args))
    except Exception:
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()
