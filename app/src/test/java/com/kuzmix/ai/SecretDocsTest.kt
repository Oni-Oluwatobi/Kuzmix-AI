package com.kuzmix.ai

import com.kuzmix.ai.domain.ai.SecretDocs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Admin drawer contracts: the master code opens the menu, document codes
 * only work after it, anything else relocks the drawer, and unlock cards
 * carry the invisible read-only marker plus an in-app study-notes action.
 */
class SecretDocsTest {

    private fun docs() = SecretDocs()

    @Test
    fun masterCodeRepliesWithTheMenu() {
        val docs = docs()
        val reply = docs.handle("CEOTKG@2026")
        assertEquals(SecretDocs.MENU, reply?.text)
        assertFalse(reply?.protected ?: true)
        assertTrue(docs.isCode("CEOTKG@2026"))
    }

    @Test
    fun documentCodesUnlockAfterTheMasterCode() {
        val docs = docs()
        docs.handle("CEOTKG@2026")

        val ss1 = docs.handle("CEOTKG@1")
        assertTrue(ss1!!.protected)
        assertTrue(ss1.text.startsWith(SecretDocs.MARKER))
        assertTrue(ss1.text.contains("Study notes by Kuzmix AI"))
        assertTrue(ss1.text.contains("[[study:ss1]]"))

        docs.handle("CEOTKG@2026")
        val ss3 = docs.handle("CEOTKG@3")!!
        assertTrue(ss3.text.contains("[[study:ss3]]"))
        assertTrue(ss3.text.contains("SS 3"))
    }

    @Test
    fun documentCodesAreDeadWithoutTheMasterCode() {
        val docs = docs()
        assertNull(docs.handle("CEOTKG@1"))
        assertNull(docs.handle("CEOTKG@3"))
    }

    @Test
    fun anyOtherMessageRelocksTheDrawer() {
        val docs = docs()
        docs.handle("CEOTKG@2026")
        assertNull(docs.handle("what is photosynthesis"))
        // drawer is closed again - the next doc code does nothing
        assertNull(docs.handle("CEOTKG@2"))
    }

    @Test
    fun codesTolerateCaseAndSurroundingSpaces() {
        val docs = docs()
        val reply = docs.handle("  ceotkg@2026 ")
        assertEquals(SecretDocs.MENU, reply?.text)
        docs.handle("ceotkg@2026")
        assertTrue(docs.handle(" Ceotkg@2 ")!!.text.contains("[[study:ss2]]"))
    }

    @Test
    fun labelsMapToReadableClassNames() {
        assertEquals("SS 1", SecretDocs.labelFor("ss1"))
        assertEquals("SS 2", SecretDocs.labelFor("ss2"))
        assertEquals("SS 3", SecretDocs.labelFor("ss3"))
    }

    @Test
    fun longGuidesSplitIntoChatSizedParts() {
        val body = (1..400).joinToString("\n") { "Line $it of the study guide" }
        val parts = SecretDocs.split(body, maxChars = 1000)
        assertTrue(parts.size > 1)
        assertTrue(parts.all { it.length <= 1000 })
        val rejoined = parts.joinToString("\n")
        assertTrue(rejoined.contains("Line 1 of the study guide"))
        assertTrue(rejoined.contains("Line 400 of the study guide"))
    }

    @Test
    fun shortGuideStaysOnePart() {
        val parts = SecretDocs.split("short guide", maxChars = 1000)
        assertEquals(listOf("short guide"), parts)
    }

    @Test
    fun guidesRenderProseAndImagesInDocumentOrder() {
        val body = "Before diagram\n[[img:secret/img/ss3_image1.png]]\nAfter diagram"
        val segments = SecretDocs.segments(body)
        assertEquals(3, segments.size)
        assertTrue(segments[0] is com.kuzmix.ai.domain.ai.GuideSegment.Text)
        val image = segments[1] as com.kuzmix.ai.domain.ai.GuideSegment.Image
        assertTrue(image.asset.endsWith("ss3_image1.png"))
        assertTrue(segments[2] is com.kuzmix.ai.domain.ai.GuideSegment.Text)
    }

    @Test
    fun textWithoutImagesIsASingleTextSegment() {
        val segments = SecretDocs.segments("plain guide")
        assertEquals(1, segments.size)
        assertEquals("plain guide", (segments[0] as com.kuzmix.ai.domain.ai.GuideSegment.Text).text)
    }
}
