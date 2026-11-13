package com.kuzmix.ai.data.knowledge

import com.kuzmix.ai.domain.ai.ConceptRecord
import com.kuzmix.ai.domain.ai.QueryNormalizer
import org.json.JSONArray
import org.json.JSONObject

/**
 * One concept hydrated from the shipped kuzmix_knowledge.sqlite corpus —
 * plain values (no Android Cursor) so mapping is unit-testable on the JVM.
 */
data class CoreRow(
    val id: String,
    val name: String,
    val domainId: String,
    val summaryDefinition: String,
    val primaryExplanation: String,
    val keyPropertiesJson: String?,
    val applicationsJson: String?,
    val domainName: String?,
    val categoryName: String?,
    val topicName: String?,
    val aliases: List<String>,
    val relatedIds: List<String>,
    val misconceptions: List<Pair<String, String>>,
    val recordTitles: List<String>,
    val formulaRecordContent: String?,
    val confidenceScore: Double
)

/**
 * Maps knowledge-core rows onto the engine's ConceptRecord contract.
 * definition = summary_definition (the one-sentence answer per the query
 * contract), examples = applications_json, related = relationship target
 * ids (nameOf resolves them), formula = first clean equation from
 * key_properties or the formula/law record.
 */
object KnowledgeCoreMapper {

    const val TYPE = "concept"
    private const val MAX_EXAMPLES = 3
    private const val MAX_RELATED = 8
    private const val MAX_MISCONCEPTIONS = 4
    private const val MAX_KEYWORDS = 40
    private const val MAX_RECORD_TITLES = 5

    private val formulaProperty = Regex("(?i)formula|equation")
    private val formulaClauseSplit =
        Regex(",\\s*(where|representing|stating|with|when|and)\\b|\\.\\s*$")
    private val equationMarkers = charArrayOf('=', '∝', '^')

    fun toRecord(row: CoreRow): ConceptRecord =
        ConceptRecord(
            id = row.id,
            type = TYPE,
            domain = row.domainId,
            name = row.name,
            aliases = row.aliases,
            definition = row.summaryDefinition.ifBlank { row.primaryExplanation },
            keywords = keywords(row),
            related = row.relatedIds.filter { it != row.id }.distinct().take(MAX_RELATED),
            examples = examples(row),
            misconceptions = row.misconceptions.take(MAX_MISCONCEPTIONS)
                .map { (belief, correction) -> "$belief \u2192 $correction" },
            formula = formula(row),
            confidence = confidenceLabel(row.confidenceScore)
        )

    internal fun examples(row: CoreRow): List<String> =
        jsonArray(row.applicationsJson)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(MAX_EXAMPLES)

    internal fun formula(row: CoreRow): String {
        jsonArrayObjects(row.keyPropertiesJson).forEach { obj ->
            val property = obj.optString("property")
            if (property.isNotEmpty() && formulaProperty.containsMatchIn(property)) {
                val value = obj.optString("value").trim()
                if (value.isNotBlank()) return value
            }
        }
        val record = row.formulaRecordContent?.trim().orEmpty()
        if (record.isEmpty()) return ""
        val colon = record.lastIndexOf(':')
        if (colon >= 0) {
            val afterColon = record.substring(colon + 1).trim()
            if (afterColon.any { it in equationMarkers }) return afterColon.trimEnd('.')
        }
        val firstClause = record.split(formulaClauseSplit).first().trim()
        if (firstClause.any { it in equationMarkers } && firstClause.length <= 120) return firstClause
        return ""
    }

    internal fun confidenceLabel(score: Double): String = when {
        score >= 0.85 -> "HIGH"
        score >= 0.60 -> "MEDIUM"
        else -> "LOW"
    }

    private fun keywords(row: CoreRow): List<String> {
        val words = LinkedHashSet<String>()
        listOfNotNull(row.categoryName, row.topicName, row.domainName)
            .forEach { addWordTokens(words, it) }
        row.recordTitles.take(MAX_RECORD_TITLES).forEach { addWordTokens(words, it) }
        return words.take(MAX_KEYWORDS).toList()
    }

    private fun addWordTokens(into: MutableSet<String>, text: String) {
        QueryNormalizer.stripPunct(text).split(" ").forEach { token ->
            if (token.length > 1 && token !in QueryNormalizer.stopwords) into.add(token)
        }
    }

    private fun jsonArray(text: String?): List<String> {
        if (text.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(text)
            (0 until array.length()).mapNotNull { i -> array.optString(i).takeIf { s -> s.isNotBlank() } }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun jsonArrayObjects(text: String?): List<JSONObject> {
        if (text.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(text)
            (0 until array.length()).mapNotNull { i ->
                array.optJSONObject(i)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
