package com.kuzmix.ai.domain.ai

import com.kuzmix.ai.domain.model.ChatMessage

interface AIEngine {
    val engineName: String
    val isAvailable: Boolean
    val isLocal: Boolean

    suspend fun generateResponse(
        query: String,
        history: List<ChatMessage> = emptyList()
    ): String
}
