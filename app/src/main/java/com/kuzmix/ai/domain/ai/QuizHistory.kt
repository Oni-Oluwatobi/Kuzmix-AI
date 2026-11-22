package com.kuzmix.ai.domain.ai

import java.security.MessageDigest

/**
 * Remembers which quiz stems (questions) have already been shown so the
 * same question never comes up twice — across rounds, topics and app
 * restarts. Implementations must be safe to call from any dispatcher.
 */
interface QuizHistory {

    /** Hashes of every stem ever shown, oldest first (insertion order). */
    suspend fun askedStems(): Set<String>

    /** Record stems as shown; re-asked stems move to the end (most recent). */
    suspend fun remember(stems: Collection<String>)

    companion object {
        /**
         * Stable 128-bit hash of a stem: lowercased, whitespace collapsed
         * and punctuated text normalised so "What is  photosynthesis?" and
         * "what is photosynthesis" hash identically.
         */
        fun stemHash(stem: String): String {
            val normalized = stem.lowercase()
                .replace(Regex("[^a-z0-9]+"), " ")
                .trim()
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(normalized.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }.take(32)
        }
    }
}

/**
 * Process-local history — used in unit tests and as a safety net when no
 * stored preferences are available. Keeps the last [cap] stems in
 * insertion order so the least-recently-asked question can be reused only
 * as a last resort when a tiny topic has nothing fresh left.
 */
class InMemoryQuizHistory(private val cap: Int = 1200) : QuizHistory {

    private val asked = LinkedHashSet<String>()

    override suspend fun askedStems(): Set<String> =
        synchronized(asked) { asked.toSet() }

    override suspend fun remember(stems: Collection<String>) {
        synchronized(asked) {
            for (stem in stems) {
                asked.remove(stem)
                asked.add(stem)
            }
            while (asked.size > cap) {
                asked.remove(asked.first())
            }
        }
    }
}
