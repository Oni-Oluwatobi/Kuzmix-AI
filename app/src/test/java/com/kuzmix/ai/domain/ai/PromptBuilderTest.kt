package com.kuzmix.ai.domain.ai

import com.kuzmix.ai.domain.model.ChatMessage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    private fun hit(name: String, summary: String) =
        KnowledgeHit(id = "c_$name", name = name, summary = summary, score = 0.9)

    @Test
    fun `includes persona context and question`() {
        val prompt = PromptBuilder.build(
            query = "What is photosynthesis?",
            ragContext = listOf(hit("Photosynthesis", "Plants convert light to food"))
        )
        assertTrue(prompt.contains("Kuzmix AI"))
        assertTrue(prompt.contains("Nigerian curriculum"))
        assertTrue(prompt.contains("[knowledge]"))
        assertTrue(prompt.contains("Plants convert light to food"))
        assertTrue(prompt.contains("Student: What is photosynthesis?"))
        assertTrue(prompt.trimEnd().endsWith("Kuzmix:"))
    }

    @Test
    fun `omits knowledge block when no context`() {
        val prompt = PromptBuilder.build(query = "Hello")
        assertFalse(prompt.contains("[knowledge]"))
        assertTrue(prompt.contains("Student: Hello"))
    }

    @Test
    fun `truncates long history to last turns`() {
        val history = (1..12).map { i ->
            ChatMessage(
                id = i.toLong(),
                role = if (i % 2 == 0) ChatMessage.Role.ASSISTANT else ChatMessage.Role.USER,
                content = "msg $i " + "x".repeat(400)
            )
        }
        val prompt = PromptBuilder.build(query = "Next", history = history)
        assertTrue(prompt.contains("[conversation]"))
        assertTrue(prompt.contains("msg 12"))
        assertFalse(prompt.contains("msg 1\n"))
        assertFalse(prompt.contains("msg 4 "))
        // per-turn cap: no single history line over ~300 chars of content
        val lines = prompt.lines().filter { it.startsWith("Student: ") || it.startsWith("Kuzmix: ") }
        assertTrue(lines.all { it.length <= 320 })
    }

    @Test
    fun `numbers context entries from 1`() {
        val prompt = PromptBuilder.build(
            query = "q",
            ragContext = listOf(hit("A", "first"), hit("B", "second"))
        )
        assertTrue(prompt.contains("[1] A - first"))
        assertTrue(prompt.contains("[2] B - second"))
    }
}
