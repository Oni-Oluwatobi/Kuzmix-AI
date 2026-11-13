package com.kuzmix.ai.tools

data class GrammarIssue(
    val index: Int,
    val message: String,
    val suggestion: String
)

object GrammarChecker {

    private val repeatedWord = Regex("""\b([A-Za-z]+)(\s+)\1\b""")
    private val doubleSpaces = Regex(""" {2,}""")
    private val spaceBeforePunctuation = Regex("""\s+([,.;:!?)])""")
    private val missingSpaceAfterPunctuation = Regex("""([.!?])([A-Za-z])""")
    private val lowercaseI = Regex("""(?<![A-Za-z])i(?![A-Za-z])""")
    private val lowercaseSentenceStart = Regex("""([.!?]\s+)([a-z])""")
    private val lowercaseTextStart = Regex("""^([a-z])""")

    fun check(text: String): List<GrammarIssue> {
        val issues = mutableListOf<GrammarIssue>()
        if (text.isBlank()) return issues

        repeatedWord.replace(text) { match ->
            issues.add(
                GrammarIssue(
                    index = match.range.first,
                    message = "Repeated word \"${match.groupValues[1]}\"",
                    suggestion = "Write it once: \"${match.groupValues[1]}\""
                )
            )
            match.value
        }

        doubleSpaces.findAll(text).forEach { match ->
            issues.add(
                GrammarIssue(
                    index = match.range.first,
                    message = "Extra spaces",
                    suggestion = "Use a single space"
                )
            )
        }

        spaceBeforePunctuation.findAll(text).forEach { match ->
            issues.add(
                GrammarIssue(
                    index = match.range.first,
                    message = "Space before \"${match.groupValues[1]}\"",
                    suggestion = "Remove the space before the punctuation mark"
                )
            )
        }

        missingSpaceAfterPunctuation.findAll(text).forEach { match ->
            issues.add(
                GrammarIssue(
                    index = match.range.first,
                    message = "Missing space after \"${match.groupValues[1]}\"",
                    suggestion = "Add a space: \"${match.groupValues[1]} ${match.groupValues[2]}\""
                )
            )
        }

        lowercaseI.findAll(text).forEach { match ->
            issues.add(
                GrammarIssue(
                    index = match.range.first,
                    message = "Lowercase \"i\"",
                    suggestion = "Capitalize it: \"I\""
                )
            )
        }

        lowercaseSentenceStart.findAll(text).forEach { match ->
            issues.add(
                GrammarIssue(
                    index = match.range.first,
                    message = "Sentence should start with a capital letter",
                    suggestion = "Capitalize \"${match.groupValues[2]}\""
                )
            )
        }

        if (lowercaseTextStart.containsMatchIn(text)) {
            issues.add(
                GrammarIssue(
                    index = 0,
                    message = "Text should start with a capital letter",
                    suggestion = "Capitalize the first letter"
                )
            )
        }

        val trimmed = text.trimEnd()
        if (trimmed.isNotEmpty() && trimmed.last() !in ".!?") {
            issues.add(
                GrammarIssue(
                    index = trimmed.lastIndex,
                    message = "Missing ending punctuation",
                    suggestion = "End the sentence with a period"
                )
            )
        }

        return issues.sortedBy { it.index }
    }

    fun correct(text: String): String {
        if (text.isBlank()) return text
        var out = text

        out = repeatedWord.replace(out) { it.groupValues[1] }
        out = doubleSpaces.replace(out, " ")
        out = spaceBeforePunctuation.replace(out) { it.groupValues[1] }
        out = missingSpaceAfterPunctuation.replace(out) { "${it.groupValues[1]} ${it.groupValues[2]}" }
        out = lowercaseI.replace(out, "I")
        out = lowercaseSentenceStart.replace(out) { "${it.groupValues[1]}${it.groupValues[2].uppercase()}" }
        out = lowercaseTextStart.replace(out) { it.groupValues[1].uppercase() }
        out = out.trim()

        if (out.isNotEmpty() && out.last() !in ".!?") {
            out += "."
        }
        return out
    }
}
