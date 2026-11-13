#!/usr/bin/env python3
"""
Full Corpus Assembler: Combines all group generators into modular concept JSON files.
Generates 2,500+ structured, authentic concepts across all 61 categories.
"""

import sys
import os
import json
import glob

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_DIR = os.path.join(BASE_DIR, "data")
CONCEPTS_DIR = os.path.join(DATA_DIR, "concepts")

sys.path.append(os.path.join(os.path.dirname(os.path.abspath(__file__)), "generators"))
from gen_group_sci_math import generate_group_sci_math
from gen_group_eng_comp import generate_group_eng_comp
from gen_group_biz_hum_soc import generate_group_biz_hum_soc
from gen_group_health_arts_prac import generate_group_health_arts_prac

def assemble_corpus():
    print("==========================================================")
    print("  KUZMIX AI - MASSIVE KNOWLEDGE CORPUS GENERATOR (2,500+)")
    print("==========================================================")

    # 1. Clean existing concept JSON files
    for old_file in glob.glob(os.path.join(CONCEPTS_DIR, "*.json")):
        os.remove(old_file)
    print("✓ Cleared prior concept files.")

    # 2. Generate concepts from all 4 generator groups
    print("\nExecuting Group 1: Science & Mathematics...")
    c1 = generate_group_sci_math()
    print(f"  ✓ Group 1 produced {len(c1)} concepts.")

    print("\nExecuting Group 2: Engineering & Computing...")
    c2 = generate_group_eng_comp()
    print(f"  ✓ Group 2 produced {len(c2)} concepts.")

    print("\nExecuting Group 3: Business, Humanities & Society...")
    c3 = generate_group_biz_hum_soc()
    print(f"  ✓ Group 3 produced {len(c3)} concepts.")

    print("\nExecuting Group 4: Health, Arts & Practical Knowledge...")
    c4 = generate_group_health_arts_prac()
    print(f"  ✓ Group 4 produced {len(c4)} concepts.")

    all_concepts = c1 + c2 + c3 + c4
    total_count = len(all_concepts)
    print(f"\nTotal Concepts Synthesized: {total_count}")

    # 3. Group by domain and write modular files
    by_domain = {}
    for c in all_concepts:
        dom = c["domain_id"]
        if dom not in by_domain:
            by_domain[dom] = []
        by_domain[dom].append(c)

    print("\nWriting modular domain JSON files:")
    for dom, items in by_domain.items():
        out_path = os.path.join(CONCEPTS_DIR, f"concepts_{dom}.json")
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(items, f, indent=2)
        size_kb = os.path.getsize(out_path) / 1024
        print(f"  ✓ {out_path} ({len(items)} concepts, {size_kb:.1f} KB)")

    print("\n==========================================================")
    print(f"  CORPUS GENERATION COMPLETE: {total_count} CONCEPTS READY")
    print("==========================================================\n")
    return total_count

if __name__ == "__main__":
    assemble_corpus()
