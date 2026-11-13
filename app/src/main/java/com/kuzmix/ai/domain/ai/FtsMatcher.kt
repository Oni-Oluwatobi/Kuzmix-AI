package com.kuzmix.ai.domain.ai

/**
 * Builds a safe SQLite FTS MATCH expression from query tokens.
 * Every term is wrapped in double quotes so user text can never be parsed
 * as FTS operators (NEAR, AND, OR, *, parentheses, stray quotes...).
 * Terms are ORed for recall — KnowledgeIndex re-ranks the candidates
 * with the existing scoring (spec 4.4).
 */
object FtsMatcher {

    private const val MAX_TERMS = 12

    fun buildMatch(tokens: Collection<String>): String? {
        val terms = tokens
            .map { it.replace("\"", " ").trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(MAX_TERMS)
            .map { "\"$it\"" }
        return terms.takeIf { it.isNotEmpty() }?.joinToString(" OR ")
    }

    /**
     * Single quoted FTS4 phrase query: matches only ADJACENT tokens.
     * Used as the first candidate tier so a multi-word alias like
     * "cell wall" is guaranteed a slot even when many rows share the
     * individual tokens (the OR tier + LIMIT would cut by rowid order).
     */
    fun phraseMatch(text: String): String? {
        val clean = text.replace("\"", " ").trim().replace(Regex("\\s+"), " ")
        return clean.takeIf { it.isNotBlank() }?.let { "\"$it\"" }
    }

    /** Content tokens for a normalized query, ready for [buildMatch]. */
    fun matchFor(nq: NormalizedQuery): String? =
        buildMatch(QueryNormalizer.contentTokens(nq.tokens))

    /**
     * Prefix OR tier: `"mito"* OR "cell"*` — covers abbreviations and
     * typed prefixes ("mito" -> mitochondria). Only tokens of 3+ chars
     * qualify so a stray short token cannot explode the match set.
     */
    fun prefixMatch(tokens: Collection<String>): String? {
        val terms = tokens
            .map { it.replace("\"", " ").trim() }
            .filter { it.isNotBlank() && it.length >= 3 }
            .distinct()
            .take(MAX_TERMS)
            .map { "\"$it\"*" }
        return terms.takeIf { it.isNotEmpty() }?.joinToString(" OR ")
    }

    /**
     * Naive singular/plural variants of a token for the OR fill tier
     * (cell <-> cells, energy <-> frequencies). Imperfect by design:
     * the ORIGINAL token always stays in the base tier, variants only
     * ADD recall, and KnowledgeIndex re-scores every candidate anyway.
     * Latin/Greek endings (-is, -us, -ss) are skipped to avoid garbage
     * like mitosis -> mitosiss.
     */
    fun inflectionVariants(token: String): List<String> {
        val t = token.trim()
        if (t.length < 3) return emptyList()
        if (t.endsWith("is") || t.endsWith("us") || t.endsWith("ss")) return emptyList()
        return when {
            t.endsWith("ies") && t.length > 4 -> listOf(t.dropLast(3) + "y")
            t.endsWith("es") && t.length > 4 &&
                Regex("[sxz]es$|[csz]hes$").containsMatchIn(t) -> listOf(t.dropLast(2))
            t.endsWith("s") -> listOf(t.dropLast(1))
            t.endsWith("y") && t.length > 3 &&
                t[t.length - 2] !in "aeiou" -> listOf(t.dropLast(1) + "ies")
            Regex("[sxz]$|[csz]h$").containsMatchIn(t) -> listOf(t + "es", t + "s")
            else -> listOf(t + "s")
        }.filter { it.length >= 3 }
    }
}
