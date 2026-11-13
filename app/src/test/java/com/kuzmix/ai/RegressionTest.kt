package com.kuzmix.ai

import com.kuzmix.ai.domain.ai.ContextResolver
import com.kuzmix.ai.domain.ai.Intent
import com.kuzmix.ai.domain.ai.KnowledgeEngine
import com.kuzmix.ai.domain.ai.KnowledgePack
import com.kuzmix.ai.domain.ai.KnowledgePackParser
import com.kuzmix.ai.domain.model.ChatMessage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class RegressionTest {

    private lateinit var engine: KnowledgeEngine

    @Before
    fun setUp() {
        engine = KnowledgeEngine(loadPack())
    }

    private fun ask(query: String, history: List<ChatMessage> = emptyList()): String =
        runBlocking { engine.generateResponse(query, history) }

    private fun isFallback(response: String): Boolean =
        response.startsWith(ContextResolver.FALLBACK_A) ||
            response.startsWith(ContextResolver.FALLBACK_B)

    private fun user(text: String) = ChatMessage(role = ChatMessage.Role.USER, content = text)
    private fun assistant(text: String) = ChatMessage(role = ChatMessage.Role.ASSISTANT, content = text)

    // ---- spec 41: required regression tests ----

    @Test
    fun test1_biologyIsSubjectDefinition() {
        val response = ask("what is biology")
        assertTrue(response, response.contains("living organisms", ignoreCase = true))
        assertEquals(Intent.SUBJECT_DEFINITION, engine.lastTrace?.intent)
        assertEquals("LOCAL", engine.lastTrace?.provider)
        assertFalse(response, isFallback(response))
    }

    @Test
    fun test2_bioAliasResolves() {
        val response = ask("what's bio?")
        assertTrue(response, response.contains("living organisms", ignoreCase = true))
        assertFalse(response, isFallback(response))
    }

    @Test
    fun test3_capabilityQueryListsSubjects() {
        val response = ask("which subjects are u capable of covering?")
        assertTrue(response, response.contains("Biology"))
        assertTrue(response, response.contains("Chemistry"))
        assertTrue(response, response.contains("supported programming"))
        assertTrue(
            response,
            engine.lastTrace?.intent == Intent.SUBJECT_CAPABILITY ||
                engine.lastTrace?.intent == Intent.CAPABILITY_QUERY
        )
    }

    @Test
    fun test4_helpOnBiologyActivatesStudy() {
        val response = ask("I need help on biology")
        assertTrue(response, response.contains("study Biology"))
        assertEquals(Intent.STUDY_HELP, engine.lastTrace?.intent)
        assertEquals("biology", engine.lastTrace?.activeSubject?.removePrefix("subject."))
    }

    @Test
    fun test5_roadmapFollowUpUsesContext() {
        val history = listOf(
            user("I need help on biology"),
            assistant("Absolutely. We can study Biology.")
        )
        val response = ask("where should we begin?", history)
        assertTrue(response, response.startsWith("Let's start with the foundations of Biology"))
        assertTrue(response, response.contains("genetics"))
        assertEquals(Intent.STUDY_ROADMAP, engine.lastTrace?.intent)
    }

    @Test
    fun test6_governmentDefinition() {
        val response = ask("what is government?")
        assertTrue(response, response.contains("political organization", ignoreCase = true))
        assertFalse(response, isFallback(response))
    }

    @Test
    fun test7_quickPromptsNeverKnowledgeMiss() {
        val prompts = listOf(
            "Create a simple Android app" to Intent.CODE_GENERATION,
            "Fix this error in my code" to Intent.DEBUGGING,
            "Explain this code" to Intent.CODE_EXPLANATION,
            "Add a new feature" to Intent.FEATURE_REQUEST,
            "Generate a full project" to Intent.PROJECT_GENERATION
        )
        for ((prompt, expected) in prompts) {
            val response = ask(prompt)
            assertFalse("fallback for: $prompt -> $response", isFallback(response))
            assertEquals("intent for: $prompt", expected, engine.lastTrace?.intent)
        }
    }

    @Test
    fun test9_arithmeticIsLocal() {
        val response = ask("What is 7 × 8?")
        assertTrue(response, response.contains("56"))
        assertEquals(Intent.CALCULATION, engine.lastTrace?.intent)
        assertFalse(response, isFallback(response))
    }

    // ---- spec AT: knowledge pack acceptance ----

    @Test
    fun at_photosynthesis() {
        val response = ask("what is photosynthesis")
        assertFalse(response, isFallback(response))
        assertTrue(response, response.contains("carbon dioxide", ignoreCase = true))
    }

    @Test
    fun at_differenceBetweenMitosisAndMeiosis() {
        val response = ask("difference between mitosis and meiosis")
        assertEquals(Intent.COMPARISON, engine.lastTrace?.intent)
        assertTrue(response, response.contains("Mitosis"))
        assertTrue(response, response.contains("Meiosis"))
    }

    @Test
    fun at_atomIonForce() {
        for (q in listOf("what is an atom", "what is an ion", "what is force")) {
            val response = ask(q)
            assertFalse("$q -> $response", isFallback(response))
        }
    }

    @Test
    fun at_newtonsSecondLaw() {
        val response = ask("state Newton's second law")
        assertTrue(response, response.contains("F = ma"))
        assertFalse(response, isFallback(response))
    }

    @Test
    fun at_mathematicsComputerScienceProbability() {
        for (q in listOf("what is mathematics", "what is computer science", "what is probability")) {
            val response = ask(q)
            assertFalse("$q -> $response", isFallback(response))
        }
    }

    @Test
    fun at_algorithmDatabaseApi() {
        for (q in listOf("what is an algorithm", "what is a database", "what is an API")) {
            val response = ask(q)
            assertFalse("$q -> $response", isFallback(response))
        }
        assertTrue(ask("what is an algorithm").contains("step", ignoreCase = true))
    }

    @Test
    fun at_languagesAndPlatforms() {
        for (q in listOf("what is Python", "what is JavaScript", "what is Android")) {
            val response = ask(q)
            assertFalse("$q -> $response", isFallback(response))
        }
    }

    @Test
    fun at_humanities() {
        for (q in listOf("what is democracy", "what is demand", "what is a metaphor", "what is climate", "what is soil")) {
            val response = ask(q)
            assertFalse("$q -> $response", isFallback(response))
        }
    }

    @Test
    fun at_cybersecurityEncryption() {
        for (q in listOf("what is cybersecurity", "what is encryption")) {
            val response = ask(q)
            assertFalse("$q -> $response", isFallback(response))
        }
    }

    // ---- spec 41/54: conversation behavior ----

    @Test
    fun greetingMatchesTargetBehavior() {
        assertEquals("Hello! How can I assist you today?", ask("hello"))
    }

    @Test
    fun identityIsDynamicAndLocal() {
        val response = ask("who are you")
        assertTrue(response, response.contains("Kuzmix AI"))
        assertTrue(response, response.contains("Oni Oluwatobi"))
        assertTrue(response, response.contains("offline", ignoreCase = true))
    }

    @Test
    fun nigerianEnglishCapability() {
        val response = ask("wetin you fit teach?")
        assertTrue(response, response.contains("Biology"))
    }

    @Test
    fun fallbackLoopProtection() {
        val first = ask("what is glorbnap zzzqxy")
        assertTrue(first, first.startsWith(ContextResolver.FALLBACK_A))

        val history = listOf(
            user("what is glorbnap zzzqxy"),
            assistant(first)
        )
        val second = ask("blorptastic fizzwump", history)
        assertTrue(second, second.startsWith(ContextResolver.FALLBACK_B))
    }

    @Test
    fun arithmeticExpressions() {
        assertTrue(ask("12 * 8").contains("96"))
        assertTrue(ask("what is 100 / 4").contains("25"))
    }

    @Test
    fun helpListsCapabilities() {
        val response = ask("help")
        assertTrue(response, response.contains("Definitions"))
        assertTrue(response, response.contains("Biology"))
    }

    // ---- loading ----

    private fun loadPack(): KnowledgePack {
        val base = readRaw("knowledge_base.json")
        val subjects = readRaw("kp_subjects.json")
        val concepts = listOf(
            "kp_concepts_science.json",
            "kp_concepts_tech.json",
            "kp_concepts_humanities.json"
        ).map { readRaw(it) }
        return KnowledgePackParser.merge(base, subjects, concepts)
    }

    private fun readRaw(name: String): String {
        val candidates = listOf(
            File("src/main/res/raw/$name"),
            File("app/src/main/res/raw/$name")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("Test resource not found: $name (cwd=${File(".").absolutePath})")
        return file.readText()
    }
}
