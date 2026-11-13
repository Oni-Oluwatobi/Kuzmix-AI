#!/usr/bin/env python3
import json
import os

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_DIR = os.path.join(BASE_DIR, "data")

with open(os.path.join(DATA_DIR, "categories.json"), "r", encoding="utf-8") as f:
    categories = json.load(f)

topics = []

for cat in categories:
    cid = cat["id"]
    cname = cat["name"]
    # Topic 1: Core Fundamentals
    t1_id = f"top_{cid}_core"
    topics.append({
        "id": t1_id,
        "category_id": cid,
        "name": f"{cname} — Core Principles & Theory",
        "slug": f"{cat['slug']}-core",
        "description": f"Foundational theorems, formal laws, and definitions within {cname}."
    })
    # Topic 2: Applied & Advanced
    t2_id = f"top_{cid}_applied"
    topics.append({
        "id": t2_id,
        "category_id": cid,
        "name": f"{cname} — Applied Methods & Systems",
        "slug": f"{cat['slug']}-applied",
        "description": f"Applied implementations, edge cases, and real-world mechanisms in {cname}."
    })

with open(os.path.join(DATA_DIR, "topics.json"), "w", encoding="utf-8") as f:
    json.dump(topics, f, indent=2)

print(f"✓ Generated {len(topics)} topics across all {len(categories)} categories.")
