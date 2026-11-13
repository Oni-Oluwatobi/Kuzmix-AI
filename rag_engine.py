import os
import pickle
import numpy as np
from sentence_transformers import SentenceTransformer
from typing import List, Dict, Any

MODEL_NAME = "all-MiniLM-L6-v2"
DB_PATH = "./kuzmix_rag_db.pkl"

_embedder = None
_index = None


def get_embedder():
    global _embedder
    if _embedder is None:
        _embedder = SentenceTransformer(MODEL_NAME)
    return _embedder


def load_index() -> Dict[str, Any]:
    global _index
    if _index is None:
        if os.path.exists(DB_PATH):
            with open(DB_PATH, "rb") as f:
                _index = pickle.load(f)
        else:
            _index = {"embeddings": [], "texts": [], "metadatas": []}
    return _index


def save_index(index: Dict[str, Any]):
    global _index
    _index = index
    with open(DB_PATH, "wb") as f:
        pickle.dump(index, f)


def chunk_code(filepath: str, chunk_size: int = 500, overlap: int = 50) -> List[Dict[str, Any]]:
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    lines = content.split("\n")
    chunks = []
    start = 0
    chunk_id = 0

    while start < len(lines):
        end = min(start + chunk_size, len(lines))
        chunk_lines = lines[start:end]
        chunk_text = "\n".join(chunk_lines)

        if chunk_text.strip():
            chunks.append({
                "id": f"{os.path.basename(filepath)}_chunk_{chunk_id}",
                "text": chunk_text,
                "metadata": {
                    "file": filepath,
                    "start_line": start + 1,
                    "end_line": end
                }
            })
            chunk_id += 1

        start += chunk_size - overlap

    return chunks


def ingest_codebase(root_dir: str = ".") -> int:
    embedder = get_embedder()
    index = load_index()

    py_files = []
    for dirpath, _, filenames in os.walk(root_dir):
        for fname in filenames:
            if fname.endswith(".py") and not fname.startswith(".") and "__pycache__" not in dirpath:
                py_files.append(os.path.join(dirpath, fname))

    existing_ids = set()
    for meta in index["metadatas"]:
        existing_ids.add(meta.get("id", ""))

    total_new = 0
    for filepath in py_files:
        chunks = chunk_code(filepath)
        if not chunks:
            continue

        new_chunks = [c for c in chunks if c["id"] not in existing_ids]
        if not new_chunks:
            continue

        texts = [c["text"] for c in new_chunks]
        embeddings = embedder.encode(texts, show_progress_bar=False)

        index["embeddings"].extend(embeddings)
        index["texts"].extend(texts)
        index["metadatas"].extend([c["metadata"] | {"id": c["id"]} for c in new_chunks])
        total_new += len(new_chunks)

    save_index(index)
    return total_new


def search_codebase(query: str, top_k: int = 5) -> List[Dict[str, Any]]:
    index = load_index()
    if not index["embeddings"]:
        return []

    embedder = get_embedder()
    query_embedding = embedder.encode([query], show_progress_bar=False)[0]

    embeddings = np.array(index["embeddings"])
    query_norm = query_embedding / (np.linalg.norm(query_embedding) + 1e-8)
    emb_norms = embeddings / (np.linalg.norm(embeddings, axis=1, keepdims=True) + 1e-8)

    scores = np.dot(emb_norms, query_norm)
    top_indices = np.argsort(scores)[::-1][:top_k]

    matches = []
    for idx in top_indices:
        matches.append({
            "text": index["texts"][idx],
            "metadata": index["metadatas"][idx],
            "score": float(scores[idx])
        })

    return matches


def format_results(matches: List[Dict[str, Any]]) -> str:
    if not matches:
        return "No relevant code found in codebase."

    out = ["=== Relevant Code Sections ===\n"]
    for i, m in enumerate(matches, 1):
        meta = m["metadata"]
        out.append(f"--- Match {i} (score: {m['score']:.3f}) ---")
        out.append(f"File: {meta['file']} (lines {meta['start_line']}-{meta['end_line']})")
        out.append("```python")
        out.append(m["text"])
        out.append("```\n")

    return "\n".join(out)


if __name__ == "__main__":
    print("Ingesting codebase...")
    count = ingest_codebase(".")
    print(f"Ingested {count} new chunks from codebase.")

    while True:
        q = input("\nSearch query (or 'exit'): ").strip()
        if q.lower() in ("exit", "quit"):
            break
        matches = search_codebase(q, top_k=3)
        print(format_results(matches))