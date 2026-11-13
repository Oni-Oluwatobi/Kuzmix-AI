package com.kuzmix.ai.domain.ai

class StudyEngine(private val subjects: List<SubjectRecord>) {

    fun startResponse(subject: SubjectRecord): String {
        val startWith = subject.subtopics.take(6)
            .joinLowercaseList()
        return "Absolutely. We can study ${subject.name}. We can start with $startWith, " +
            "or the basics of ${subject.name} itself. Which area interests you most?"
    }

    fun roadmapResponse(subject: SubjectRecord): String {
        val roadmap = subject.roadmap
        if (roadmap.isEmpty()) {
            return "We can start with a subject overview, a specific topic, or a practice question. What are you studying?"
        }
        val foundations = roadmap.take(3).joinLowercaseList()
        val later = roadmap.drop(3).take(4).joinLowercaseList()
        val builder = StringBuilder()
        builder.append("Let's start with the foundations of ${subject.name}: $foundations.")
        if (later.isNotBlank()) {
            builder.append(" Then we can move into $later.")
        }
        if (roadmap.size > 7) {
            builder.append(" After that, we keep building from there.")
        }
        builder.append(" Say the topic you want and we'll dive in.")
        return builder.toString()
    }

    fun genericStartResponse(): String {
        return "We can start with a subject overview, a specific topic, or a practice question. " +
            "What are you studying?"
    }

    fun continueWithSubject(subject: SubjectRecord): String {
        val topic = subject.subtopics.take(3).joinLowercaseList()
        return "We're on ${subject.name}. We can continue with $topic — " +
            "or say \"where should we begin?\" for the full roadmap. Which one?"
    }

    private fun List<String>.joinLowercaseList(): String {
        val items = map { it.trim().trimEnd('?', '.').lowercase() }.filter { it.isNotBlank() }
        return when (items.size) {
            0 -> ""
            1 -> items[0]
            2 -> "${items[0]} and ${items[1]}"
            else -> items.dropLast(1).joinToString(", ") + ", and ${items.last()}"
        }
    }
}
