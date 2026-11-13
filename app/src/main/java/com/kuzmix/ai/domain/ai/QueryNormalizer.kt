package com.kuzmix.ai.domain.ai

data class NormalizedQuery(
    val raw: String,
    val normalized: String,
    val tokens: List<String>
)

object QueryNormalizer {

    private val contractions = mapOf(
        "what's" to "what is", "whats" to "what is", "wats" to "what is", "wat" to "what",
        "who's" to "who is", "whos" to "who is",
        "where's" to "where is", "wheres" to "where is",
        "how's" to "how is", "hows" to "how is",
        "why's" to "why is",
        "i'm" to "i am", "i’m" to "i am",
        "you're" to "you are", "you’re" to "you are", "youre" to "you are",
        "we're" to "we are", "they're" to "they are",
        "it's" to "it is", "that's" to "that is", "there's" to "there is",
        "don't" to "do not", "doesn't" to "does not", "didn't" to "did not",
        "isn't" to "is not", "aren't" to "are not", "wasn't" to "was not",
        "can't" to "cannot", "cant" to "cannot", "won't" to "will not",
        "let's" to "let us", "lets" to "let us",
        "could've" to "could have", "should've" to "should have", "would've" to "would have"
    )

    private val slang = mapOf(
        "u" to "you", "ur" to "your", "urs" to "yours",
        "pls" to "please", "plz" to "please", "plse" to "please",
        "thx" to "thanks", "ty" to "thanks",
        "wetin" to "what", "watin" to "what",
        "sabi" to "know", "fit" to "can",
        "abeg" to "please", "una" to "you all",
        "gonna" to "going to", "wanna" to "want to", "gotta" to "got to"
    )

    private val stripLeadingCodeContext = Regex("^language\\s*:\\s*[^.]*\\.\\s*", RegexOption.IGNORE_CASE)

    fun normalize(raw: String): NormalizedQuery {
        var text = raw.lowercase().trim()
        text = text.replace(stripLeadingCodeContext, "")
        text = text.replace(Regex("[’‘]"), "'")

        for ((from, to) in contractions) {
            text = text.replace(Regex("(?<=\\s|^)${Regex.escape(from)}(?=\\s|$|[?!.])"), to)
        }
        for ((from, to) in slang) {
            text = text.replace(Regex("(?<=\\s|^)${Regex.escape(from)}(?=\\s|$|[?!.])"), to)
        }

        text = text.replace(Regex("o{3,}"), "o")
        text = text.replace('×', '*').replace('÷', '/').replace('−', '-').replace('–', '-')

        text = text.replace(Regex("[^\\w\\s*+/^().%-]"), " ")
        text = text.replace(Regex("\\s+"), " ").trim()
        text = text.replace(Regex("^[\\s?.,!]+"), "").replace(Regex("[\\s?.,!]+$"), "")

        val tokens = text.split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .map { it.trim { c -> c in "?.!,;:\"'()" } }
            .filter { it.isNotEmpty() }

        return NormalizedQuery(raw = raw, normalized = text, tokens = tokens)
    }

    val stopwords = setOf(
        "a", "an", "the", "of", "in", "on", "to", "for", "and", "or", "is", "are", "was",
        "were", "be", "been", "being", "it", "its", "this", "that", "these", "those",
        "do", "does", "did", "can", "could", "will", "would", "shall", "should", "may",
        "might", "must", "you", "your", "yours", "i", "me", "my", "we", "our", "they",
        "them", "he", "she", "his", "her", "with", "as", "at", "by", "from", "about",
        "into", "than", "then", "so", "if", "not", "no", "yes", "please", "tell", "give",
        "show", "get", "got", "have", "has", "had", "what", "which", "who", "whom",
        "how", "when", "where", "why", "there", "here", "out", "up", "down", "over",
        "under", "again", "also", "just", "like", "much", "many", "more", "most",
        "some", "any", "all", "anyone", "something", "anything", "one", "two",
        "kuzmix", "assistant", "ai"
    )

    fun contentTokens(tokens: List<String>): List<String> =
        tokens.filter { it.length > 1 && it !in stopwords && !it.all { c -> c.isDigit() } }

    private val leadingInterrogative = Regex(
        "^(what('s| is| are| was| were)?|who('s| is| was| were)?|where('s| is| are| was| were)?|" +
            "when('s| is| are| was| were)?|why('s| is| are| was| were)?|how('s| does| do| did| is| are| was| were)?|" +
            "which('s| is| are| was| were)?|define|explain|describe|tell me about|what do you mean by|" +
            "difference between|compare|list|give me|show me|name)(?:\\s+|$)",
        RegexOption.IGNORE_CASE
    )
    private val leadingAuxiliary = Regex("^(is|are|was|were|do|does|did|can|could|will|would|should)(?:\\s+|$)")

    /**
     * Phrase text for the FTS adjacency tier: leading question words /
     * auxiliaries stripped, whitespace collapsed — but EVERY other word
     * kept, including stopwords. FTS indexes stopwords and they sit
     * between content words in real text ("laws OF motion"), so the old
     * stopword-stripped phrase ("laws motion") never matched anything.
     * The string is already lowercased/punct-normalized by [normalize].
     */
    fun phraseText(nq: NormalizedQuery): String {
        var t = nq.normalized
        t = t.replace(leadingInterrogative, "")
        t = t.replace(leadingAuxiliary, "")
        t = t.replace(Regex("^[\\s?.,!]+|[\\s?.,!]+$"), "").trim()
        return t
    }

    fun stripPunct(text: String): String =
        text.lowercase().replace(Regex("[^\\w\\s]"), " ").replace(Regex("\\s+"), " ").trim()
}
