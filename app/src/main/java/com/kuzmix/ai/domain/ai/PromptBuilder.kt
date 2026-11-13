package com.kuzmix.ai.domain.ai

import com.kuzmix.ai.domain.model.ChatMessage

/**
 * Builds the single offline prompt for the local generative model:
 * tutor persona + retrieved knowledge context + conversation + question.
 * Pure Kotlin — unit-testable, no Android dependencies.
 */
object PromptBuilder {

    private const val SYSTEM = (
        "You are Kuzmix AI, a friendly offline tutor for the Nigerian " +
            "curriculum (JSS1-SS3). Answer using the KNOWLEDGE CONTEXT below " +
            "whenever it is relevant. Be clear, simple, and step by step. " +
            "If the context does not contain the answer, say you do not have " +
            "that in your offline knowledge base - never invent facts. " +
            "When you use the context, end your answer with a short " +
            "'Sources:' list of the context headings you used."
        )

    private const val MAX_HISTORY_TURNS = 6
    private const val MAX_TURN_CHARS = 300

    fun build(
        query: String,
        history: List<ChatMessage> = emptyList(),
        ragContext: List<KnowledgeHit> = emptyList()
    ): String {
        val sb = StringBuilder()
        sb.append("[system]\n").append(SYSTEM).append('\n')

        if (ragContext.isNotEmpty()) {
            sb.append("\n[knowledge]\n")
            ragContext.forEachIndexed { i, hit ->
                sb.append('[').append(i + 1).append("] ")
                    .append(hit.name).append(" - ").append(hit.summary).append('\n')
            }
        }

        val recent = history.takeLast(MAX_HISTORY_TURNS)
        if (recent.isNotEmpty()) {
            sb.append("\n[conversation]\n")
            recent.forEach { msg ->
                val who = if (msg.role == ChatMessage.Role.USER) "Student" else "Kuzmix"
                val text = msg.content.take(MAX_TURN_CHARS)
                sb.append(who).append(": ").append(text).append('\n')
            }
        }

        sb.append("\n[question]\nStudent: ").append(query.trim())
            .append("\nKuzmix:")
        return sb.toString()
    }
}
