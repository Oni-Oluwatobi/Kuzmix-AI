package com.kuzmix.ai.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey
import com.kuzmix.ai.domain.ai.ConceptRecord

/**
 * List fields are joined with the ASCII unit separator (0x1F) instead of a
 * comma, because examples/misconceptions are full sentences containing commas.
 * Fields are always read as a whole (never filtered into), so a flat string
 * column beats a child table + join on weak CPUs (spec 4.1).
 */
const val LIST_SEPARATOR = "\u001F"

@Entity(tableName = "concepts")
data class ConceptEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val domain: String,
    val name: String,
    val definition: String,
    val aliasesCsv: String,
    val keywordsCsv: String,
    val relatedCsv: String,
    val examplesCsv: String,
    val misconceptionsCsv: String,
    val formula: String,
    val confidence: String
)

@Fts4
@Entity(tableName = "concepts_fts")
data class ConceptFtsEntity(
    val conceptId: String,
    val name: String,
    val definition: String,
    val aliasesCsv: String
)

fun ConceptRecord.toEntity(): ConceptEntity = ConceptEntity(
    id = id,
    type = type,
    domain = domain,
    name = name,
    definition = definition,
    aliasesCsv = aliases.joinToString(LIST_SEPARATOR),
    keywordsCsv = keywords.joinToString(LIST_SEPARATOR),
    relatedCsv = related.joinToString(LIST_SEPARATOR),
    examplesCsv = examples.joinToString(LIST_SEPARATOR),
    misconceptionsCsv = misconceptions.joinToString(LIST_SEPARATOR),
    formula = formula,
    confidence = confidence
)

fun ConceptEntity.toDomain(): ConceptRecord = ConceptRecord(
    id = id,
    type = type,
    domain = domain,
    name = name,
    aliases = aliasesCsv.splitList(),
    definition = definition,
    keywords = keywordsCsv.splitList(),
    related = relatedCsv.splitList(),
    examples = examplesCsv.splitList(),
    misconceptions = misconceptionsCsv.splitList(),
    formula = formula,
    confidence = confidence
)

fun ConceptRecord.toFts(): ConceptFtsEntity = ConceptFtsEntity(
    conceptId = id,
    name = name,
    definition = definition,
    aliasesCsv = aliases.joinToString(LIST_SEPARATOR)
)

private fun String.splitList(): List<String> =
    if (isEmpty()) emptyList() else split(LIST_SEPARATOR).filter { it.isNotBlank() }
