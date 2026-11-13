package com.kuzmix.ai.domain.model

data class Conversation(
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isCodeMode: Boolean = false,
    val lastMessagePreview: String = ""
)
