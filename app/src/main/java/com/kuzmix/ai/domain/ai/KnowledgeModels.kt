package com.kuzmix.ai.domain.ai

data class SubjectRecord(
    val id: String,
    val name: String,
    val aliases: List<String>,
    val enabled: Boolean,
    val definition: String,
    val subtopics: List<String>,
    val roadmap: List<String>,
    val detectKeywords: List<String>
)

data class ConceptRecord(
    val id: String,
    val type: String,
    val domain: String,
    val name: String,
    val aliases: List<String>,
    val definition: String,
    val keywords: List<String>,
    val related: List<String>,
    val examples: List<String>,
    val misconceptions: List<String>,
    val formula: String,
    val confidence: String
)

data class FormulaRecord(
    val id: String,
    val domain: String,
    val name: String,
    val formula: String,
    val symbols: Map<String, String>,
    val explanation: String
)

data class MisconceptionRecord(
    val id: String,
    val domain: String,
    val misconception: String,
    val correction: String
)

data class RelationRecord(
    val from: String,
    val type: String,
    val to: String
)

data class KnowledgePack(
    val knowledgeVersion: String,
    val subjects: List<SubjectRecord>,
    val concepts: List<ConceptRecord>,
    val formulas: List<FormulaRecord>,
    val misconceptions: List<MisconceptionRecord>,
    val relations: List<RelationRecord>,
    val responses: Map<String, String>,
    val variations: Map<String, String>
)
