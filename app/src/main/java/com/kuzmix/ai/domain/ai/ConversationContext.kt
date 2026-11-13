package com.kuzmix.ai.domain.ai

import com.kuzmix.ai.domain.model.ChatMessage

data class ConversationContext(
    val activeSubject: String? = null,
    val activeSubjectName: String? = null,
    val activeTopic: String? = null,
    val activeMode: String? = null,
    val lastIntent: Intent? = null,
    val educationLevel: String? = null,
    val lastAssistantWasFallback: Boolean = false
)

class ContextResolver(private val subjects: List<SubjectRecord>) {

    private val studyMarkers = listOf(
        "need help", "want help", "help me", "teach me", "learning", "studying",
        "tutor me", "guide me", "study", "learn", "revision", "explain to me"
    )

    private val codeMarkers = listOf(
        "write code", "my code", "this code", "debug", "fix this error",
        "android app", "programming", "function", "compile", "stacktrace"
    )

    private val levelPattern = Regex("\\b(ss[1-3]|jss[1-3]|ss[1-9]|waec|neco|gce|grade\\s*\\d|form\\s*\\d|class\\s*\\d)\\b")

    fun resolve(history: List<ChatMessage>): ConversationContext {
        var ctx = ConversationContext()
        for (message in history) {
            if (message.role == ChatMessage.Role.USER) {
                ctx = applyQuery(ctx, message.content)
            } else if (message.role == ChatMessage.Role.ASSISTANT) {
                val content = message.content.trim()
                ctx = ctx.copy(
                    lastAssistantWasFallback = content.startsWith(FALLBACK_A) || content.startsWith(FALLBACK_B)
                )
            }
        }
        return ctx
    }

    fun applyQuery(ctx: ConversationContext, query: String): ConversationContext {
        val normalized = QueryNormalizer.normalize(query).normalized
        var next = ctx

        levelPattern.find(normalized)?.let { m ->
            next = next.copy(educationLevel = m.value.uppercase())
        }

        val matchedSubject = detectSubject(normalized)
        if (matchedSubject != null) {
            if (next.activeSubject != matchedSubject.id) {
                next = next.copy(activeSubject = matchedSubject.id, activeSubjectName = matchedSubject.name, activeTopic = null)
            }
            next = next.copy(activeSubject = matchedSubject.id, activeSubjectName = matchedSubject.name)
        }

        val mode = when {
            codeMarkers.any { normalized.contains(it) } -> "code"
            studyMarkers.any { normalized.contains(it) } -> "study"
            else -> null
        }
        if (mode != null) {
            next = next.copy(activeMode = mode)
        }

        if (next.activeSubject != null) {
            val subject = subjects.firstOrNull { it.id == next.activeSubject }
            val topic = subject?.subtopics?.firstOrNull { sub ->
                Regex("\\b${Regex.escape(sub.lowercase())}s?\\b").containsMatchIn(normalized)
            }
            if (topic != null && !topic.equals(next.activeTopic, ignoreCase = true)) {
                next = next.copy(activeTopic = topic)
            }
        }

        return next
    }

    fun detectSubject(normalizedQuery: String): SubjectRecord? {
        val text = " $normalizedQuery "

        for (subject in subjects) {
            val names = (listOf(subject.name) + subject.aliases).map { it.lowercase() }
            for (name in names) {
                if (Regex("(?<![a-z0-9])${Regex.escape(name)}(?![a-z0-9])").containsMatchIn(text)) {
                    return subject
                }
            }
        }

        var best: SubjectRecord? = null
        var bestHits = 1
        for (subject in subjects) {
            val hits = subject.detectKeywords.count { kw ->
                Regex("(?<![a-z0-9])${Regex.escape(kw.lowercase())}(?![a-z0-9])").containsMatchIn(text)
            }
            if (hits >= 2 && hits > bestHits) {
                best = subject
                bestHits = hits
            }
        }
        return best
    }

    companion object {
        const val FALLBACK_A = "I couldn't find enough information for that exact request"
        const val FALLBACK_B = "Let me try from another angle"
    }
}
