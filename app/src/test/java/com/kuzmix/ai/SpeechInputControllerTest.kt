package com.kuzmix.ai

import com.kuzmix.ai.voice.SpeechInputController
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechInputControllerTest {

    @Test
    fun dictationFillsEmptyComposer() {
        assertEquals("hello world", SpeechInputController.mergeDictation("", "hello world"))
    }

    @Test
    fun dictationAppendsToExistingTextWithoutDestroyingIt() {
        assertEquals(
            "write a function hello",
            SpeechInputController.mergeDictation("write a function", "hello")
        )
    }

    @Test
    fun dictationTrimsWhitespaceSensibly() {
        assertEquals("existing noted", SpeechInputController.mergeDictation("existing   ", "  noted "))
    }

    @Test
    fun blankDictationLeavesComposerUntouched() {
        assertEquals("keep me", SpeechInputController.mergeDictation("keep me", "   "))
        assertEquals("", SpeechInputController.mergeDictation("", ""))
    }
}
