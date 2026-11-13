package com.kuzmix.ai.domain.model

data class KnowledgeEntry(
    val key: String,
    val response: String,
    val keywords: List<String>
)
