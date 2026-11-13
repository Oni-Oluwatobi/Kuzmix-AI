package com.kuzmix.ai

import com.kuzmix.ai.data.knowledge.CoreRow
import com.kuzmix.ai.data.knowledge.KnowledgeCoreMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeCoreMapperTest {

    private fun row(
        id: String = "c_gravity",
        name: String = "Gravity and Gravitation",
        summary: String = "A fundamental interaction.",
        explanation: String = "Long pedagogical explanation.",
        keyProps: String? = null,
        applications: String? = null,
        domainName: String? = "Science and Mathematics",
        categoryName: String? = "Classical Mechanics",
        topicName: String? = "Newtonian Gravity",
        aliases: List<String> = listOf("Gravitational Force"),
        related: List<String> = listOf("c_mass", "c_acceleration"),
        misconceptions: List<Pair<String, String>> = emptyList(),
        titles: List<String> = emptyList(),
        formulaRecord: String? = null,
        confidence: Double = 1.0
    ) = CoreRow(
        id = id,
        name = name,
        domainId = "sci_math",
        summaryDefinition = summary,
        primaryExplanation = explanation,
        keyPropertiesJson = keyProps,
        applicationsJson = applications,
        domainName = domainName,
        categoryName = categoryName,
        topicName = topicName,
        aliases = aliases,
        relatedIds = related,
        misconceptions = misconceptions,
        recordTitles = titles,
        formulaRecordContent = formulaRecord,
        confidenceScore = confidence
    )

    @Test
    fun mapsCoreFields() {
        val record = KnowledgeCoreMapper.toRecord(
            row(applications = """["Orbit calculation.", "Tide modeling."]""")
        )
        assertEquals("c_gravity", record.id)
        assertEquals("concept", record.type)
        assertEquals("sci_math", record.domain)
        assertEquals("Gravity and Gravitation", record.name)
        assertEquals(listOf("Gravitational Force"), record.aliases)
        assertEquals("A fundamental interaction.", record.definition)
        assertEquals(listOf("Orbit calculation.", "Tide modeling."), record.examples)
        assertEquals(listOf("c_mass", "c_acceleration"), record.related)
        assertEquals("HIGH", record.confidence)
    }

    @Test
    fun definitionFallsBackToPrimaryExplanation() {
        val record = KnowledgeCoreMapper.toRecord(row(summary = "", explanation = "Deep explanation."))
        assertEquals("Deep explanation.", record.definition)
    }

    @Test
    fun examplesAreCappedAndBlankEntriesDropped() {
        val apps = """["One", "", "Two", "Three", "Four"]"""
        val record = KnowledgeCoreMapper.toRecord(row(applications = apps))
        assertEquals(listOf("One", "Two", "Three"), record.examples)
    }

    @Test
    fun malformedApplicationsJsonYieldsNoExamples() {
        assertTrue(KnowledgeCoreMapper.toRecord(row(applications = "not json")).examples.isEmpty())
        assertTrue(KnowledgeCoreMapper.toRecord(row(applications = null)).examples.isEmpty())
    }

    @Test
    fun misconceptionsArePairedAndCapped() {
        val misc = (1..6).map { "Belief $it" to "Correction $it" }
        val record = KnowledgeCoreMapper.toRecord(row(misconceptions = misc))
        assertEquals(4, record.misconceptions.size)
        assertEquals("Belief 1 \u2192 Correction 1", record.misconceptions[0])
    }

    @Test
    fun relatedCapsAtEightAndDropsSelf() {
        val related = (1..12).map { "c_$it" } + "c_gravity"
        val record = KnowledgeCoreMapper.toRecord(row(related = related))
        assertEquals(8, record.related.size)
        assertTrue("c_gravity" !in record.related)
    }

    @Test
    fun keywordsAreSingleNormalizedWords() {
        val record = KnowledgeCoreMapper.toRecord(
            row(titles = listOf("Why doesn't the Moon crash into Earth?"))
        )
        assertTrue("mechanics" in record.keywords)
        assertTrue("newtonian" in record.keywords)
        assertTrue("gravity" in record.keywords)
        assertTrue(record.keywords.none { it.contains(" ") })
        assertTrue(record.keywords.none { it in setOf("the", "and", "into", "why") })
    }

    @Test
    fun formulaPrefersEquationProperty() {
        val props = """[{"property": "Mathematical Equation", "value": "V = I * R"}]"""
        val record = KnowledgeCoreMapper.toRecord(row(keyProps = props, formulaRecord = "Ignored record."))
        assertEquals("V = I * R", record.formula)
    }

    @Test
    fun formulaFallsBackToLawRecordAfterColon() {
        val law = "Every particle attracts every other: F = G*(m1*m2)/r^2."
        val record = KnowledgeCoreMapper.toRecord(row(formulaRecord = law))
        assertEquals("F = G*(m1*m2)/r^2", record.formula)
    }

    @Test
    fun formulaSplitsExplanatoryClause() {
        val formula = "A = P * (1 + r/n)^(n*t), where A is future value and P is principal"
        val record = KnowledgeCoreMapper.toRecord(row(formulaRecord = formula))
        assertEquals("A = P * (1 + r/n)^(n*t)", record.formula)
    }

    @Test
    fun formulaEmptyWhenNoEquationExists() {
        assertEquals("", KnowledgeCoreMapper.toRecord(row(formulaRecord = null)).formula)
        assertEquals("", KnowledgeCoreMapper.toRecord(row(formulaRecord = "Pure prose sentence.")).formula)
        assertEquals("", KnowledgeCoreMapper.toRecord(row(keyProps = """[{"property": "Color", "value": "Red"}]""")).formula)
    }

    @Test
    fun confidenceLabels() {
        assertEquals("HIGH", KnowledgeCoreMapper.confidenceLabel(1.0))
        assertEquals("HIGH", KnowledgeCoreMapper.confidenceLabel(0.85))
        assertEquals("MEDIUM", KnowledgeCoreMapper.confidenceLabel(0.7))
        assertEquals("LOW", KnowledgeCoreMapper.confidenceLabel(0.3))
    }
}
