package com.kuzmix.ai

import com.kuzmix.ai.domain.ai.BibleRefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The shipped KJV lookup depends on reference parsing being exactly right:
 * canonical book names (as the DB stores them), abbreviations, verse ranges,
 * chapter-only refs, and hard NO's for math/noise ("2x - 3 = 34",
 * "exercise 5.2").
 */
class BibleRefsTest {

    // -------------------------------------------------------- verse refs

    @Test
    fun parsesPlainVerseReference() {
        val ref = BibleRefs.findReference("what does John 3:16 say")
        assertNotNull(ref)
        assertEquals("John", ref!!.book)
        assertEquals(3, ref.chapter)
        assertEquals(16, ref.firstVerse)
        assertEquals("John 3:16", ref.display())
    }

    @Test
    fun parsesGenesisQuotedTypically() {
        val ref = BibleRefs.findReference("Genesis 1:1")
        assertNotNull(ref)
        assertEquals("Genesis", ref!!.book)
        assertEquals(1, ref.firstVerse)
    }

    @Test
    fun parsesPrefixedBookAndOrdinalAbbreviation() {
        val ref = BibleRefs.findReference("1 cor 13:4-7")
        assertNotNull(ref)
        assertEquals("1 Corinthians", ref!!.book)
        assertEquals(13, ref.chapter)
        assertEquals(4, ref.firstVerse)
        assertEquals(7, ref.lastVerse)
        assertEquals("1 Corinthians 13:4-7", ref.display())
    }

    @Test
    fun parsesAttachedForms() {
        val attached = BibleRefs.findReference("read John3:16 now")
        assertNotNull(attached)
        assertEquals("John", attached!!.book)
        assertEquals(16, attached.firstVerse)

        val compact = BibleRefs.findReference("1cor13:4")
        assertNotNull(compact)
        assertEquals("1 Corinthians", compact!!.book)
        assertEquals(13, compact.chapter)
    }

    @Test
    fun chapterOnlyReference() {
        val ref = BibleRefs.findReference("Psalm 23")
        assertNotNull(ref)
        assertEquals("Psalms", ref!!.book)     // canonical DB spelling
        assertEquals(23, ref.chapter)
        assertEquals(null, ref.firstVerse)
        assertEquals("Psalms 23", ref.display())
        assertEquals(true, ref.isChapterOnly)
    }

    @Test
    fun wholeChapterWithVerseRangeInside() {
        val ref = BibleRefs.findReference("psalm 23:1-5")
        assertNotNull(ref)
        assertEquals("Psalms", ref!!.book)
        assertEquals(1, ref.firstVerse)
        assertEquals(5, ref.lastVerse)
    }

    @Test
    fun leadWordsAreCapturedForGating() {
        val ref = BibleRefs.findReference("in John 3 we see grace")
        assertNotNull(ref)
        assertEquals("in", ref!!.leadText)
        assertEquals(true, BibleRefs.startsNearBeginning(ref, "in John 3 we see grace"))

        val noisy = BibleRefs.findReference("the final 3 pages of chapter 9") ?: return
        // "pages" is no book -> nothing resolves (defensive: fail test if it does)
        assertNull(noisy)
    }

    // ------------------------------------------------------------ no-gos

    @Test
    fun mathEquationsNeverResolveAsReferences() {
        assertNull(BibleRefs.findReference("2x - 3 = 34"))
        assertNull(BibleRefs.findReference("1x - 2 = 32"))
        assertNull(BibleRefs.findReference("x^2 - 5x + 6 = 0"))
        assertNull(BibleRefs.findReference("solve 3(x - 2) = 4x + 1"))
        assertNull(BibleRefs.findReference("what is 12 * 8"))
    }

    @Test
    fun nonBookPhrasesNeverResolve() {
        assertNull(BibleRefs.findReference("exercise 5.2 is hard"))
        assertNull(BibleRefs.findReference("the answer is 42"))
        assertNull(BibleRefs.findReference("i scored 3 goals in 5 matches"))
    }

    // ------------------------------------------------------- canonicalisation

    @Test
    fun canonicalisesCommonAbbreviationsToDbSpellings() {
        assertEquals("Psalms", BibleRefs.canonicalizeBook("ps"))
        assertEquals("Psalms", BibleRefs.canonicalizeBook("PSA"))
        assertEquals("Matthew", BibleRefs.canonicalizeBook("mt"))
        assertEquals("Song of Solomon", BibleRefs.canonicalizeBook("song of songs"))
        assertEquals("1 Corinthians", BibleRefs.canonicalizeBook("1 COR"))
        assertEquals("Revelation", BibleRefs.canonicalizeBook("rev"))
        assertEquals("1 John", BibleRefs.canonicalizeBook("1 jn"))
        // all 66 canonical names self-resolve
        BibleRefs.BOOKS.forEach { book ->
            assertEquals(book, BibleRefs.canonicalizeBook(book))
        }
        assertNull(BibleRefs.canonicalizeBook("photosynthesis"))
        assertNull(BibleRefs.canonicalizeBook("the book"))
    }
}
