#!/usr/bin/env python3
"""
Kuzmix AI - Massive Offline Knowledge Corpus Generator
Generates 2,000+ structured, high-quality concepts across a broad taxonomy.
Outputs modular JSON files to data/concepts/ and updates categories, topics, sources.
"""

import json
import os
import hashlib

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_DIR = os.path.join(BASE_DIR, "data")
CONCEPTS_DIR = os.path.join(DATA_DIR, "concepts")

os.makedirs(CONCEPTS_DIR, exist_ok=True)

def sha256(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()

print("Generating expansive taxonomy and sources...")
