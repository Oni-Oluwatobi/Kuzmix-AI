package com.kuzmix.ai

import com.kuzmix.ai.tools.GrammarChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GrammarCheckerTest {

    @Test
    fun cleanTextHasNoIssues() {
        assertTrue(GrammarChecker.check("Kuzmix AI works offline.").isEmpty())
    }

    @Test
    fun blankTextHasNoIssues() {
        assertTrue(GrammarChecker.check("").isEmpty())
        assertTrue(GrammarChecker.check("   ").isEmpty())
    }

    @Test
    fun repeatedWordIsDetectedAndFixed() {
        val text = "This is is a test"
        val issues = GrammarChecker.check(text)
        assertTrue(issues.any { it.message.contains("Repeated word") })
        assertEquals("This is a test.", GrammarChecker.correct(text))
    }

    @Test
    fun extraSpacesAreDetectedAndFixed() {
        assertEquals("Hello world.", GrammarChecker.correct("Hello  world"))
    }

    @Test
    fun spaceBeforePunctuationIsFixed() {
        assertEquals("Hello, world!", GrammarChecker.correct("Hello , world !"))
    }

    @Test
    fun lowercaseIIsCapitalized() {
        assertEquals("I am ready.", GrammarChecker.correct("i am ready"))
        assertTrue(GrammarChecker.check("i am ready").any { it.message.contains("Lowercase \"i\"") })
    }

    @Test
    fun sentenceCapitalizationIsFixed() {
        assertEquals("Hello there. Nice day.", GrammarChecker.correct("hello there. nice day."))
    }

    @Test
    fun missingSpaceAfterPunctuationIsFixed() {
        assertEquals("End. Next one.", GrammarChecker.correct("End.Next one"))
    }

    @Test
    fun missingTerminalPeriodIsAdded() {
        assertEquals("Let's build it.", GrammarChecker.correct("Let's build it"))
        assertTrue(GrammarChecker.check("Let's build it").any { it.message.contains("ending punctuation") })
    }

    @Test
    fun numbersWithDecimalsAreNotFlagged() {
        assertTrue(GrammarChecker.check("The value is 3.5 today.").isEmpty())
    }
}
