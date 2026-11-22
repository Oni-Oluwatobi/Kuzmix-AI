package com.kuzmix.ai.domain.ai

class KnowledgeIndex(
    private val pack: KnowledgePack,
    private val source: ConceptSource? = null
) {

    enum class Kind { SUBJECT, CONCEPT, FORMULA }

    data class Entry(
        val id: String,
        val kind: Kind,
        val name: String,
        val normalizedName: String,
        val aliases: List<String>,
        val domain: String,
        val definition: String,
        val nameTokens: Set<String>,
        val keywordTokens: Set<String>,
        val definitionTokens: Set<String>,
        val subject: SubjectRecord? = null,
        val concept: ConceptRecord? = null,
        val formula: FormulaRecord? = null
    )

    data class Scored(val entry: Entry, val score: Double)

    val entries: List<Entry>
    private val byName: Map<String, List<Entry>>
    private val byAlias: Map<String, List<Entry>>

    init {
        val list = mutableListOf<Entry>()

        for (s in pack.subjects) {
            if (!s.enabled) continue
            list.add(
                Entry(
                    id = s.id,
                    kind = Kind.SUBJECT,
                    name = s.name,
                    normalizedName = norm(s.name),
                    aliases = s.aliases,
                    domain = s.id,
                    definition = s.definition,
                    nameTokens = tokens(s.name),
                    keywordTokens = s.detectKeywords.mapNotNull { normOrNull(it) }.toSet() + s.subtopics.flatMap { tokens(it) },
                    definitionTokens = tokens(s.definition),
                    subject = s
                )
            )
        }

        for (c in pack.concepts) {
            list.add(conceptEntry(c))
        }

        for (f in pack.formulas) {
            list.add(
                Entry(
                    id = f.id,
                    kind = Kind.FORMULA,
                    name = f.name,
                    normalizedName = norm(f.name),
                    aliases = listOf(f.formula),
                    domain = f.domain,
                    definition = f.explanation,
                    nameTokens = tokens(f.name),
                    keywordTokens = tokens(f.explanation),
                    definitionTokens = tokens(f.explanation),
                    formula = f
                )
            )
        }

        entries = list

        val nameMap = HashMap<String, MutableList<Entry>>()
        val aliasMap = HashMap<String, MutableList<Entry>>()
        for (e in list) {
            nameMap.getOrPut(e.normalizedName) { mutableListOf() }.add(e)
            for (a in e.aliases) {
                val key = norm(a)
                if (key.isNotBlank()) aliasMap.getOrPut(key) { mutableListOf() }.add(e)
            }
        }
        byName = nameMap
        byAlias = aliasMap
    }

    suspend fun resolveEntity(entity: String): Entry? {
        val n = norm(entity)
        if (n.isBlank()) return null

        byName[n]?.let { return prefer(it) }
        byAlias[n]?.let { return prefer(it) }

        if (source == null) {
            val entityTokens = tokens(entity).filter { it !in QueryNormalizer.stopwords }
            if (entityTokens.isNotEmpty()) {
                byName.entries.firstOrNull { (key, _) ->
                    val keyTokens = key.split(" ").filter { it.isNotBlank() }
                    keyTokens.isNotEmpty() && keyTokens.all { it in entityTokens } && keyTokens.size >= entityTokens.size - 1
                }?.value?.let { return prefer(it) }
            }

            if (entityTokens.size == 1) {
                val single = entityTokens[0]
                byName.keys.firstOrNull { it.split(" ").contains(single) }?.let { key ->
                    byName[key]?.let { return prefer(it) }
                }
                byAlias.keys.firstOrNull { it.split(" ").contains(single) }?.let { key ->
                    byAlias[key]?.let { return prefer(it) }
                }
            }
            return fuzzyResolve(n)
        }

        // Disk-backed mode: subjects/formulas resolved above; concepts go
        // through the store (FTS + cache), which applies the same
        // exact/fuzzy name and alias logic over its hydrated candidates.
        // The typo pass runs last so exact hits always win.
        return source.lookup(entity)?.let { conceptEntry(it) } ?: fuzzyResolve(n)
    }

    /**
     * Typo tolerance for direct lookups ("buology" -> Biology): the best
     * name/alias key within a tiny edit budget. Subjects (and formulas)
     * live in the static shell in BOTH modes, so a misspelled subject
     * request resolves here even when the disk concept source misses.
     * Budget is 1 edit for short words, 2 for longer ones — tight enough
     * that "physics" can never swallow an unrelated subject.
     */
    private fun fuzzyResolve(n: String): Entry? {
        if (n.length < 4) return null
        val budget = if (n.length <= 5) 1 else 2
        var bestDist = budget + 1
        var best: Entry? = null
        fun consider(key: String, list: List<Entry>) {
            if (key.isEmpty() || kotlin.math.abs(key.length - n.length) > budget) return
            val d = QueryNormalizer.editDistanceAtMost(n, key, budget)
            if (d > budget) return
            val e = prefer(list)
            val cur = best
            if (cur == null || d < bestDist || (d == bestDist && kindRank(e) < kindRank(cur))) {
                bestDist = d
                best = e
            }
        }
        for ((key, list) in byName) consider(key, list)
        if (best == null) {
            for ((key, list) in byAlias) consider(key, list)
        }
        return best
    }

    suspend fun search(
        nq: NormalizedQuery,
        context: ConversationContext,
        intent: Intent,
        minScore: Double = 0.20
    ): List<Scored> {
        val qTokens = QueryNormalizer.contentTokens(nq.tokens).toSet()
        if (qTokens.isEmpty()) return emptyList()
        val normalized = nq.normalized

        // Candidate gathering (spec 4.4): legacy walks every resident entry;
        // disk-backed mode scores subjects/formulas plus only the FTS
        // candidates (~50), never the full corpus. Deduped — a concept in
        // both the static pack and the FTS list keeps its static slot but
        // still earns the FTS prior. FTS candidate order IS the weighted
        // BM25 rank (searchIds returns concepts best-row-first), used as a
        // small rank prior instead of being discarded.
        val ftsRank = HashMap<String, Double>()
        val candidates = if (source == null) {
            entries
        } else {
            val dynamic = source.candidates(nq).map { conceptEntry(it) }
            dynamic.forEachIndexed { i, e ->
                ftsRank.putIfAbsent(e.id, 1.0 - i.toDouble() / dynamic.size)
            }
            if (dynamic.isEmpty()) {
                entries
            } else {
                val seen = HashSet<String>(entries.size + dynamic.size)
                ArrayList<Entry>(entries.size + dynamic.size).also { merged ->
                    entries.forEach { if (seen.add(it.id)) merged.add(it) }
                    dynamic.forEach { if (seen.add(it.id)) merged.add(it) }
                }
            }
        }

        val scored = ArrayList<Scored>(candidates.size)
        for (e in candidates) {
            val nameHit = e.nameTokens.any { it in qTokens }
            val aliasHit = e.aliases.any { alias ->
                val a = norm(alias)
                a.isNotBlank() && Regex("(?<![a-z0-9])${Regex.escape(a)}(?![a-z0-9])").containsMatchIn(normalized)
            }
            val keywordHit = e.keywordTokens.any { it in qTokens }
            val definitionHit = e.definitionTokens.any { it in qTokens }

            if (!nameHit && !aliasHit && !keywordHit && !definitionHit) continue

            val semantic = (qTokens intersect (e.definitionTokens + e.keywordTokens + e.nameTokens)).size.toDouble() / qTokens.size

            // Coverage gate: for multi-token queries a candidate covering
            // <1/3 of the query tokens that is not even a name/alias hit is
            // weak context-channel noise — drop before it can accumulate
            // intent/context points and clear minScore. Single-token
            // queries are unaffected (coverage is 0 or 1).
            if (semantic < 0.33 && !nameHit && !aliasHit) continue

            val lexical = (qTokens intersect (e.nameTokens + e.keywordTokens)).size.toDouble() / qTokens.size
            val aliasScore = when {
                aliasHit -> 1.0
                e.nameTokens.isNotEmpty() && e.normalizedName in normalized -> 1.0
                else -> 0.0
            }

            val contextScore = when {
                context.activeSubject != null && isActiveDomain(context.activeSubject, e.domain) -> 1.0
                context.activeSubject != null && e.subject?.subtopics?.any {
                    norm(it) in normalized
                } == true -> 0.6
                else -> 0.0
            }

            val intentScore = when (intent) {
                Intent.SUBJECT_DEFINITION -> if (e.kind == Kind.SUBJECT) 1.0 else 0.35
                Intent.DEFINITION, Intent.CONCEPT_DEFINITION, Intent.EXAMPLE_REQUEST ->
                    when (e.kind) {
                        Kind.CONCEPT -> 1.0
                        Kind.SUBJECT -> 0.85
                        Kind.FORMULA -> 0.25
                    }
                Intent.CALCULATION -> if (e.kind == Kind.FORMULA) 1.0 else 0.4
                Intent.STUDY_ROADMAP, Intent.STUDY_START, Intent.STUDY_HELP ->
                    if (e.kind == Kind.SUBJECT) 1.0 else 0.3
                Intent.COMPARISON -> 0.7
                Intent.GENERAL_KNOWLEDGE, Intent.UNKNOWN -> 0.5
                else -> 0.5
            }

            // Weighted sum (thresholds unchanged). fts = FTS rank prior,
            // 0 in legacy mode where no FTS candidates exist.
            val fts = ftsRank[e.id] ?: 0.0
            val score = lexical * 0.24 + aliasScore * 0.18 + semantic * 0.28 +
                contextScore * 0.14 + intentScore * 0.10 + fts * 0.06

            if (score >= minScore) {
                scored.add(Scored(e, score))
            }
        }

        return scored.sortedByDescending { it.score }
    }

    fun confidenceFor(score: Double, exact: Boolean): String = when {
        exact -> "HIGH"
        score >= 0.55 -> "HIGH"
        score >= 0.32 -> "MEDIUM"
        else -> "LOW"
    }

    suspend fun nameOf(idOrName: String): String? {
        entries.firstOrNull { it.id == idOrName }?.let { return it.name }
        source?.byId(idOrName)?.let { return it.name }
        resolveEntity(idOrName)?.let { return it.name }
        return null
    }

    private fun conceptEntry(c: ConceptRecord): Entry = Entry(
        id = c.id,
        kind = Kind.CONCEPT,
        name = c.name,
        normalizedName = norm(c.name),
        aliases = c.aliases,
        domain = c.domain,
        definition = c.definition,
        nameTokens = tokens(c.name),
        keywordTokens = c.keywords.mapNotNull { normOrNull(it) }.toSet(),
        definitionTokens = tokens(c.definition),
        concept = c
    )

    private fun kindRank(e: Entry): Int = when (e.kind) {
        Kind.SUBJECT -> 0
        Kind.CONCEPT -> 1
        Kind.FORMULA -> 2
    }

    private fun prefer(list: List<Entry>): Entry =
        list.minByOrNull { kindRank(it) } ?: list.first()

    private fun isActiveDomain(activeSubject: String, domain: String): Boolean =
        DomainBridge.matches(activeSubject, domain)

    private fun norm(text: String): String = QueryNormalizer.stripPunct(text)

    private fun normOrNull(text: String): String? = norm(text).takeIf { it.isNotBlank() }

    private fun tokens(text: String): Set<String> =
        QueryNormalizer.stripPunct(text).split(" ")
            .filter { it.isNotBlank() && it !in QueryNormalizer.stopwords }
            .toSet()
}
