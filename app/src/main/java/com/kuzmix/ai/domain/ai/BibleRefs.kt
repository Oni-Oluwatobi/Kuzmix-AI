package com.kuzmix.ai.domain.ai

/**
 * Parsed Bible reference: "John 3:16", "1 cor 13:4-7", "Psalm 23".
 * [book] is always the canonical name exactly as stored in the shipped DB
 * ("John", "1 Corinthians", "Psalms", "Song of Solomon"...)
 */
data class BibleRef(
    val book: String,
    val chapter: Int,
    /** null when the user asked for a whole chapter ("Psalm 23") */
    val firstVerse: Int? = null,
    val lastVerse: Int? = null,
    /** original casing/spelling found in the query, for display + start gating */
    val matchedText: String = "",
    /** words appearing BEFORE the book in the query ("read ", "in the book of ") */
    val leadText: String = ""
) {
    val isChapterOnly: Boolean get() = firstVerse == null

    fun display(): String = buildString {
        append(book).append(' ').append(chapter)
        val v = firstVerse
        if (v != null) {
            append(':').append(v)
            val v2 = lastVerse
            if (v2 != null && v2 != v) {
                append('-').append(v2)
            }
        }
    }
}

/**
 * Offline Bible reference finder. Scans free text for
 *   <book> <chapter>[:.<verse>[-<verse>]]
 * where <book> is any of the 66 canonical books OR one of the common
 * abbreviations ("gen", "1 cor", "ps", "mt", "rev"...).
 *
 * Detection works backwards from each digit run: the 1..4 words before the
 * chapter number are tried longest-first ("1 john" must beat "john"), then
 * shortened until one resolves to a canonical book. This keeps false
 * positives out — "exercise 5.2" resolves nothing, "1 cor 13:4" resolves
 * "1 cor" -> "1 Corinthians".
 *
 * Pure Kotlin (no Android, no DB) so it is fully unit-testable.
 */
object BibleRefs {

    /** The 66 books, spelled exactly as the shipped KJV table stores them. */
    val BOOKS: List<String> = listOf(
        "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy",
        "Joshua", "Judges", "Ruth", "1 Samuel", "2 Samuel", "1 Kings", "2 Kings",
        "1 Chronicles", "2 Chronicles", "Ezra", "Nehemiah", "Esther", "Job",
        "Psalms", "Proverbs", "Ecclesiastes", "Song of Solomon", "Isaiah",
        "Jeremiah", "Lamentations", "Ezekiel", "Daniel", "Hosea", "Joel",
        "Amos", "Obadiah", "Jonah", "Micah", "Nahum", "Habakkuk", "Zephaniah",
        "Haggai", "Zechariah", "Malachi",
        "Matthew", "Mark", "Luke", "John", "Acts", "Romans",
        "1 Corinthians", "2 Corinthians", "Galatians", "Ephesians",
        "Philippians", "Colossians", "1 Thessalonians", "2 Thessalonians",
        "1 Timothy", "2 Timothy", "Titus", "Philemon", "Hebrews", "James",
        "1 Peter", "2 Peter", "1 John", "2 John", "3 John", "Jude", "Revelation"
    )

    private val ALIASES: Map<String, String> = buildMap {
        fun alias(canonical: String, vararg alts: String) {
            val target = BOOKS.firstOrNull { it.equals(canonical, true) } ?: return
            put(norm(target), target)
            alts.forEach { put(norm(it), target) }
        }
        // every canonical name under its own normalised form
        BOOKS.forEach { put(norm(it), it) }

        alias("Genesis", "gen", "ge", "gn")
        alias("Exodus", "exo", "exod", "ex")
        alias("Leviticus", "lev", "le", "lv")
        alias("Numbers", "num", "nu", "nm")
        alias("Deuteronomy", "deut", "deu", "dt")
        alias("Joshua", "josh", "jos", "jsh")
        alias("Judges", "judg", "jdg", "jg")
        alias("Ruth", "rut", "ru")
        alias("1 Samuel", "1 sam", "1sa", "1 sm", "i samuel", "first samuel")
        alias("2 Samuel", "2 sam", "2sa", "2 sm", "ii samuel", "second samuel")
        alias("1 Kings", "1 ki", "1k", "i kings", "first kings")
        alias("2 Kings", "2 ki", "2k", "ii kings", "second kings")
        alias("1 Chronicles", "1 ch", "1ch", "1 chron", "i chronicles", "first chronicles")
        alias("2 Chronicles", "2 ch", "2ch", "2 chron", "ii chronicles", "second chronicles")
        alias("Ezra", "ezr")
        alias("Nehemiah", "neh")
        alias("Esther", "est", "esth")
        alias("Psalms", "psalm", "ps", "psa", "psm")
        alias("Proverbs", "prov", "pro", "prv")
        alias("Ecclesiastes", "ecc", "eccl")
        alias("Song of Solomon", "song of songs", "song", "sng", "sos")
        alias("Isaiah", "isa")
        alias("Jeremiah", "jer", "jr")
        alias("Lamentations", "lam")
        alias("Ezekiel", "ezek", "ezk")
        alias("Daniel", "dan", "dn")
        alias("Hosea", "hos")
        alias("Joel", "joe", "jl")
        alias("Amos", "amo")
        alias("Obadiah", "obad", "oba")
        alias("Jonah", "jon", "jnh")
        alias("Micah", "mic", "mc")
        alias("Nahum", "nah")
        alias("Habakkuk", "hab", "hb")
        alias("Zephaniah", "zeph", "zep")
        alias("Haggai", "hag", "hg")
        alias("Zechariah", "zech", "zec")
        alias("Malachi", "mal")
        alias("Matthew", "matt", "mat", "mt")
        alias("Mark", "mrk", "mk")
        alias("Luke", "luk", "lk")
        alias("John", "joh", "jn")
        alias("Acts", "act")
        alias("Romans", "rom", "rm")
        alias("1 Corinthians", "1 cor", "1 co", "1cor", "1co", "i corinthians", "first corinthians")
        alias("2 Corinthians", "2 cor", "2 co", "2cor", "2co", "ii corinthians", "second corinthians")
        alias("Galatians", "gal")
        alias("Ephesians", "eph")
        alias("Philippians", "phil", "php")
        alias("Colossians", "col")
        alias("1 Thessalonians", "1 thess", "1 th", "1th", "i thessalonians", "first thessalonians")
        alias("2 Thessalonians", "2 thess", "2 th", "2th", "ii thessalonians", "second thessalonians")
        alias("1 Timothy", "1 tim", "1 ti", "1ti", "i timothy", "first timothy")
        alias("2 Timothy", "2 tim", "2 ti", "2ti", "ii timothy", "second timothy")
        alias("Titus", "tit")
        alias("Philemon", "phlm", "phm")
        alias("Hebrews", "heb")
        alias("James", "jam", "jas")
        alias("1 Peter", "1 pe", "1pe", "i peter", "first peter")
        alias("2 Peter", "2 pe", "2pe", "ii peter", "second peter")
        alias("1 John", "1 jn", "1jo", "i john", "first john")
        alias("2 John", "2 jn", "2jo", "ii john", "second john")
        alias("3 John", "3 jn", "3jo", "iii john", "third john")
        alias("Jude", "jud")
        alias("Revelation", "rev", "rv")
    }

    /** "1  COR." -> "1 cor" (lowercase, no dots, single spaces). */
    private fun norm(s: String): String =
        s.lowercase()
            .replace(".", "")
            .replace("\u2019", "'")
            .trim()
            .replace(Regex("\\s+"), " ")

    /** Canonical DB book name for a free-form book phrase, or null. */
    fun canonicalizeBook(part: String): String? = ALIASES[norm(part)]

    /**
     * chapter digits + optional ":verse" or ".verse" + optional "-verse".
     * A separator is REQUIRED before the chapter's own digits, so "2x" or
     * version numbers never look like a chapter.
     */
    private val digitToken = Regex(
        "(\\d{1,3})(?:\\s*[:.]\\s*(\\d{1,3})(?:\\s*[-\u2013\u2014]\\s*(\\d{1,3}))?)?"
    )

    /** First real reference in [text], or null. */
    fun findReference(text: String): BibleRef? {
        if (text.isBlank()) return null
        val src = text.replace('\u2019', '\'')
        for (m in digitToken.findAll(src)) {
            // a book phrase must precede the chapter number; only digits can't
            // (the canonicalize gate below rejects all non-book phrases, so
            // "John3:16" and "1cor13:4" are accepted while "2x - 3 = 34",
            // "3.16" and "exercise 5.2" resolve nothing)
            if (m.range.first == 0 || src[m.range.first - 1].isDigit()) continue

            val chapter = m.groupValues[1].toIntOrNull() ?: continue
            val v1 = m.groupValues[2].toIntOrNull()
            val v2 = m.groupValues[3].toIntOrNull()
            if (chapter < 1 || chapter > 180) continue
            if (v1 != null && (v1 < 1 || v1 > 200)) continue
            if (v2 != null && (v2 < 1 || v2 > 200)) continue

            val before = src.substring(0, m.range.first)
            if (before.isBlank()) continue
            val words = before.trimEnd().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.isEmpty()) continue

            // longest-first: "1 john" must win over "john"
            val maxK = minOf(4, words.size)
            for (k in maxK downTo 1) {
                val candidate = words.takeLast(k).joinToString(" ")
                val book = canonicalizeBook(candidate) ?: continue
                val lead = words.dropLast(k).joinToString(" ")
                val matched = src.substring(m.range.first, m.range.last + 1)
                return BibleRef(
                    book = book,
                    chapter = chapter,
                    firstVerse = v1,
                    lastVerse = if (v1 != null) (v2 ?: v1) else null,
                    matchedText = "$candidate $matched",
                    leadText = lead
                )
            }
        }
        return null
    }

    /** Parse a clean reference string ("John 3:16", "Psalms 23"). */
    fun parse(ref: String): BibleRef? = findReference(ref.trim())

    /** Words allowed before a chapter-only reference at the "start" of a query. */
    private val leadingFunctionWords = Regex(
        "^(?:(?:in|at|from|to|read|open|see|check|go|look|turn|about|the|book of|psalm of)\\s*)+$",
        RegexOption.IGNORE_CASE
    )

    /**
     * True when [ref] sits at the head of [raw] (ignoring small lead-ins
     * like "read ", "in ", "open ") — used to accept chapter-only
     * references such as "read john 3" without accepting
     * "the final 3 pages of chapter 9" style noise.
     */
    fun startsNearBeginning(ref: BibleRef, raw: String): Boolean {
        val lead = ref.leadText.trim()
        if (lead.isEmpty()) return true
        if (leadingFunctionWords.matches(lead)) return true
        val probe = raw.trim()
        return probe.startsWith(ref.matchedText, ignoreCase = true)
    }
}
