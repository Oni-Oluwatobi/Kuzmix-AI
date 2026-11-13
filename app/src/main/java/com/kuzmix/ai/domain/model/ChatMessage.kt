package com.kuzmix.ai.domain.model

data class ChatMessage(
    val id: Long = 0,
    val conversationId: Long = 0,
    val role: Role,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class Role { USER, ASSISTANT }
}
