package com.kuzmix.ai.domain.ai

import org.json.JSONObject

object KnowledgePackParser {

    data class SubjectsJson(
        val knowledgeVersion: String,
        val subjects: List<SubjectRecord>,
        val formulas: List<FormulaRecord>,
        val misconceptions: List<MisconceptionRecord>,
        val relations: List<RelationRecord>
    )

    fun parseBase(json: String): Pair<Map<String, String>, Map<String, String>> {
        val root = JSONObject(json)
        val responses = mutableMapOf<String, String>()
        val variations = mutableMapOf<String, String>()

        root.optJSONObject("responses")?.let { obj ->
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                responses[key] = obj.getString(key)
            }
        }
        root.optJSONObject("variations")?.let { obj ->
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                variations[key] = obj.getString(key)
            }
        }
        return responses to variations
    }

    fun parseSubjects(json: String): SubjectsJson {
        val root = JSONObject(json)
        val version = root.optString("knowledgeVersion", "6.0")

        val subjects = mutableListOf<SubjectRecord>()
        root.optJSONArray("subjects")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                subjects.add(
                    SubjectRecord(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        aliases = o.stringList("aliases"),
                        enabled = o.optBoolean("enabled", true),
                        definition = o.optString("definition", ""),
                        subtopics = o.stringList("subtopics"),
                        roadmap = o.stringList("roadmap"),
                        detectKeywords = o.stringList("detectKeywords")
                    )
                )
            }
        }

        val formulas = mutableListOf<FormulaRecord>()
        root.optJSONArray("formulas")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val symbols = mutableMapOf<String, String>()
                o.optJSONObject("symbols")?.let { sym ->
                    val keys = sym.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        symbols[k] = sym.getString(k)
                    }
                }
                formulas.add(
                    FormulaRecord(
                        id = o.getString("id"),
                        domain = o.optString("domain", ""),
                        name = o.getString("name"),
                        formula = o.getString("formula"),
                        symbols = symbols,
                        explanation = o.optString("explanation", "")
                    )
                )
            }
        }

        val misconceptions = mutableListOf<MisconceptionRecord>()
        root.optJSONArray("misconceptions")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                misconceptions.add(
                    MisconceptionRecord(
                        id = o.getString("id"),
                        domain = o.optString("domain", ""),
                        misconception = o.getString("misconception"),
                        correction = o.getString("correction")
                    )
                )
            }
        }

        val relations = mutableListOf<RelationRecord>()
        root.optJSONArray("relations")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                relations.add(
                    RelationRecord(
                        from = o.getString("from"),
                        type = o.optString("type", "RELATED_TO"),
                        to = o.getString("to")
                    )
                )
            }
        }

        return SubjectsJson(version, subjects, formulas, misconceptions, relations)
    }

    fun parseConcepts(json: String): List<ConceptRecord> {
        val root = JSONObject(json)
        val result = mutableListOf<ConceptRecord>()
        root.optJSONArray("concepts")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                result.add(
                    ConceptRecord(
                        id = o.getString("id"),
                        type = o.optString("type", "CONCEPT"),
                        domain = o.optString("domain", ""),
                        name = o.getString("name"),
                        aliases = o.stringList("aliases"),
                        definition = o.getString("definition"),
                        keywords = o.stringList("keywords"),
                        related = o.stringList("related"),
                        examples = o.stringList("examples"),
                        misconceptions = o.stringList("misconceptions"),
                        formula = o.optString("formula", ""),
                        confidence = o.optString("confidence", "HIGH")
                    )
                )
            }
        }
        return result
    }

    fun merge(
        baseJson: String,
        subjectsJson: String,
        conceptsJsons: List<String>
    ): KnowledgePack {
        val (responses, variations) = parseBase(baseJson)
        val subjectsPart = parseSubjects(subjectsJson)
        val concepts = conceptsJsons.flatMap { parseConcepts(it) }
        return KnowledgePack(
            knowledgeVersion = subjectsPart.knowledgeVersion,
            subjects = subjectsPart.subjects,
            concepts = concepts,
            formulas = subjectsPart.formulas,
            misconceptions = subjectsPart.misconceptions,
            relations = subjectsPart.relations,
            responses = responses,
            variations = variations
        )
    }

    private fun JSONObject.stringList(key: String): List<String> {
        val arr = optJSONArray(key) ?: return emptyList()
        val list = ArrayList<String>(arr.length())
        for (i in 0 until arr.length()) {
            val value = arr.optString(i, "")
            if (value.isNotBlank()) list.add(value)
        }
        return list
    }
}
