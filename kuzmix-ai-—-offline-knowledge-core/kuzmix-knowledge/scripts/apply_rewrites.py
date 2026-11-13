#!/usr/bin/env python3
"""Apply rewrite batches to data/concepts/*.json (idempotent, rerun after every batch).

Batch file: JSON array in data/rewrites/batch_*.json
{
  "id": "c_cat_econ_micro_...",
  "summary_definition": "...",            # real content required
  "primary_explanation": "...",           # real content required
  "key_properties": [{"property": "...", "value": "..."}],
  "applications": ["...", "..."],
  "aliases": [{"alias": "...", "alias_type": "synonym"}],
  "misconceptions": [{"misconception_text": "...", "correction_text": "...", "explanation": "..."}],
  "prerequisites": ["c_..."],             # optional
  "records": [ ... ],                     # optional override; default = 1 definition record
  "relationships": [ ... ]                # optional; merged by target_concept_id
}

Regenerated satellites (records/chunks) are derived from the new core fields so
chunks/records/FTS always stay consistent with the rewritten content.
"""
import json, os, glob, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONCEPTS_DIR = os.path.join(ROOT, "data", "concepts")
REWRITES_DIR = os.path.join(ROOT, "data", "rewrites")

REQUIRED = ("id", "summary_definition", "primary_explanation")

def load_concepts():
    store = {}
    for path in sorted(glob.glob(os.path.join(CONCEPTS_DIR, "concepts_*.json"))):
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        for c in data:
            store[c["id"]] = (path, c)
    return store

def regenerate_satellites(c):
    name = c["name"]
    # Records: single definition record from the real summary
    if "records" not in c or not c.get("__records_custom"):
        c["records"] = [{
            "id": f"rec_{c['id']}_definition",
            "record_type": "definition",
            "title": f"Definition of {name}",
            "content": c["summary_definition"],
            "confidence_score": 1.0,
            "provenance_type": "curated_scientific",
            "source_id": c.get("source_id", "src_feynman_physics"),
        }]
    # Chunks
    chunks = [{
        "chunk_index": 0,
        "chunk_type": "definition",
        "text_content": f"{name}: {c['summary_definition']}",
    }, {
        "chunk_index": 1,
        "chunk_type": "explanation",
        "text_content": c["primary_explanation"],
    }]
    props = c.get("key_properties") or []
    apps = c.get("applications") or []
    if props or apps:
        parts = [f"Key properties and applications of {name}:"]
        for p in props:
            parts.append(f"{p['property']}: {p['value']}.")
        if apps:
            parts.append("Applications: " + " ".join(apps))
        chunks.append({
            "chunk_index": len(chunks),
            "chunk_type": "properties_and_applications",
            "text_content": " ".join(parts),
        })
    for m in c.get("misconceptions") or []:
        chunks.append({
            "chunk_index": len(chunks),
            "chunk_type": "misconceptions",
            "text_content": f"Common misconception about {name}: {m['misconception_text']} Correction: {m['correction_text']}",
        })
    for i, ch in enumerate(chunks):
        ch["chunk_index"] = i
        ch.setdefault("token_estimate", max(1, len(ch["text_content"]) // 4))
    c["chunks"] = chunks

def apply_batch(batch_path, store):
    with open(batch_path, encoding="utf-8") as f:
        batch = json.load(f)
    applied, missing, bad = [], [], []
    dirty_paths = set()
    for entry in batch:
        for k in REQUIRED:
            if not entry.get(k):
                bad.append((entry.get("id"), f"missing {k}"))
                break
        else:
            cid = entry["id"]
            hit = store.get(cid)
            if hit is None:
                missing.append(cid)
                continue
            path, c = hit
            if entry.get("name"):
                c["name"] = entry["name"]
            c["summary_definition"] = entry["summary_definition"]
            c["primary_explanation"] = entry["primary_explanation"]
            c["key_properties"] = entry.get("key_properties", [])
            c["applications"] = entry.get("applications", [])
            c["aliases"] = entry.get("aliases", c.get("aliases", []))
            c["misconceptions"] = entry.get("misconceptions", [])
            if "prerequisites" in entry:
                c["prerequisites"] = entry["prerequisites"]
            if "records" in entry:
                c["records"] = entry["records"]
                c["__records_custom"] = True
            if "relationships" in entry:
                rels = c.get("relationships") or []
                by_target = {r.get("target_concept_id"): r for r in rels}
                for r in entry["relationships"]:
                    by_target[r["target_concept_id"]] = r
                c["relationships"] = list(by_target.values())
            regenerate_satellites(c)
            c.pop("__records_custom", None)
            applied.append(cid)
            dirty_paths.add(path)
    for path in dirty_paths:
        out = [c for (p, c) in store.values() if p == path]
        with open(path, "w", encoding="utf-8") as f:
            json.dump(out, f, ensure_ascii=False, indent=2)
    return applied, missing, bad

def main():
    files = sorted(glob.glob(os.path.join(REWRITES_DIR, "batch_*.json")))
    if not files:
        print("No batch files in data/rewrites/")
        return 1
    store = load_concepts()
    total_a, total_m, total_b = [], [], []
    for bf in files:
        a, m, b = apply_batch(bf, store)
        print(f"{os.path.basename(bf)}: applied={len(a)} missing={len(m)} bad={len(b)}")
        if m: print("   missing:", m)
        if b: print("   bad:", b)
        total_a += a; total_m += m; total_b += b
    print(f"\nTOTAL applied={len(total_a)} missing={len(total_m)} bad={len(total_b)}")
    return 0 if not total_m and not total_b else 2

if __name__ == "__main__":
    sys.exit(main())
