import hashlib
import json
import os

def sha256(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()

def slugify(text: str) -> str:
    cleaned = text.lower().replace(" ", "_").replace("'", "").replace("-", "_").replace("=", "").replace(".", "").replace("/", "").replace(":", "")
    return cleaned[:30]

def make_concept_obj(
    cid,
    domain_id,
    category_id,
    name,
    definition,
    explanation,
    properties=None,
    applications=None,
    prerequisites=None,
    difficulty="intermediate",
    source_id="src_feynman_physics",
    aliases=None,
    misconceptions=None,
    records=None,
    entities=None,
    relationships=None
):
    topic_id = f"top_{category_id}_core"
    props = [{"property": p[0], "value": p[1]} for p in (properties or [])]
    apps = applications or ["Theoretical research and industrial design.", "Educational problem solving."]
    prereqs = prerequisites or []
    als = [{"alias": a, "alias_type": "synonym"} for a in (aliases or [])]
    
    miscs = []
    for m in (misconceptions or []):
        miscs.append({
            "misconception_text": m[0],
            "correction_text": m[1],
            "explanation": m[2]
        })
    if not miscs:
        miscs.append({
            "misconception_text": f"Common intuition often oversimplifies the exact physical or mathematical boundary conditions of {name}.",
            "correction_text": f"{name} obeys precise formal definitions that must be evaluated within its specific domain constraints.",
            "explanation": "Everyday terminology often confuses qualitative metaphors with quantitative physical laws."
        })

    recs = []
    for i, r in enumerate(records or []):
        recs.append({
            "id": f"rec_{cid}_{i}",
            "record_type": r[0],
            "title": r[1],
            "content": r[2]
        })
    if not recs:
        recs.append({
            "id": f"rec_{cid}_0",
            "record_type": "principle",
            "title": f"Governing Principle of {name}",
            "content": definition
        })

    chunks = [
        {
            "chunk_index": 0,
            "chunk_type": "definition",
            "header_context": f"{domain_id} > {category_id} > {name}",
            "text_content": f"{name}: {definition} {explanation[:220]}",
            "token_estimate": len(f"{name}: {definition}".split()) + 35
        },
        {
            "chunk_index": 1,
            "chunk_type": "properties_and_applications",
            "header_context": f"{domain_id} > {category_id} > {name} Properties",
            "text_content": f"Key properties and applications of {name}: " + "; ".join(f"{p['property']}: {p['value']}" for p in props[:3]) + ". Real-world use: " + "; ".join(apps[:2]),
            "token_estimate": 65
        }
    ]

    ents = []
    for i, e in enumerate(entities or []):
        ents.append({
            "id": f"ent_{cid}_{i}",
            "entity_type": e[0],
            "name": e[1],
            "description": e[2]
        })

    rels = []
    for rel in (relationships or []):
        rels.append({
            "target_concept_id": rel[0],
            "relationship_type": rel[1],
            "description": rel[2],
            "weight": rel[3] if len(rel) > 3 else 0.9
        })

    return {
        "id": cid,
        "domain_id": domain_id,
        "category_id": category_id,
        "topic_id": topic_id,
        "name": name,
        "summary_definition": definition,
        "primary_explanation": explanation,
        "key_properties": props,
        "applications": apps,
        "prerequisites": prereqs,
        "difficulty_level": difficulty,
        "confidence_score": 1.0,
        "provenance_type": "curated_scientific",
        "source_id": source_id,
        "aliases": als,
        "misconceptions": miscs,
        "records": recs,
        "chunks": chunks,
        "entities": ents,
        "relationships": rels
    }
