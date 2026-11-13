package com.kuzmix.ai.domain.ai

enum class Intent {
    GREETING,
    FAREWELL,
    THANKS,
    SMALL_TALK,
    HELP,
    CAPABILITY_QUERY,
    SUBJECT_CAPABILITY,
    STUDY_HELP,
    STUDY_START,
    STUDY_ROADMAP,
    DEFINITION,
    SUBJECT_DEFINITION,
    CONCEPT_DEFINITION,
    EXAMPLE_REQUEST,
    COMPARISON,
    PRACTICE_QUESTION,
    CALCULATION,
    SOLVE_EQUATION,
    BIBLE_QUERY,
    CODE_ASSISTANT,
    CODE_GENERATION,
    CODE_EXPLANATION,
    DEBUGGING,
    FEATURE_REQUEST,
    PROJECT_GENERATION,
    IDENTITY,
    ABOUT_KUZMIX,
    DEVELOPER_INFO,
    GENERAL_KNOWLEDGE,
    UNKNOWN
}

data class IntentMatch(
    val intent: Intent,
    val entity: String? = null,
    val entity2: String? = null
)

object IntentClassifier {

    private val codePatterns = listOf(
        Regex("\\bcreate\\b.*\\b(app|application|project|program|website)\\b"),
        Regex("\\bbuild\\b.*\\b(app|application|project|website)\\b"),
        Regex("\\bfix\\b.*\\b(error|bug|issue|crash|problem|mistake)\\b"),
        Regex("\\b(debug|troubleshoot)\\b"),
        Regex("\\b(explain|review|read)\\b.*\\b(this\\s+code|my\\s+code|code\\s+below|following\\s+code)\\b"),
        Regex("\\b(this\\s+code|my\\s+code|code\\s+below|the\\s+code)\\b"),
        Regex("\\badd\\b.*\\b(feature|function|screen|button|page|module)\\b"),
        Regex("\\bgenerate\\b.*\\b(project|app|code|scaffold|boilerplate)\\b"),
        Regex("\\bwrite\\b.*\\b(code|function|class|program|script|app|module|api)\\b"),
        Regex("\\bimplement\\b"),
        Regex("\\bconvert\\b.*\\b(code|this|it)\\b.*\\b(to|into)\\b"),
        Regex("\\b(refactor|optimize)\\b.*\\b(code|function|class)?"),
        Regex("\\bwhy\\b.*\\b(not\\s+work|isn't working|does not work|crash|error|fail)\\b"),
        Regex("\\b(my|the)\\b.*\\b(app|code)\\b.*\\b(crash|fail|error|broken|not\\s+work)\\b"),
        Regex("\\bquick\\s*prompt\\b")
    )

    private val codeQuickPrompts = listOf(
        "create a simple android app",
        "fix this error in my code",
        "explain this code",
        "add a new feature",
        "generate a full project"
    )

    private val capabilityPatterns = listOf(
        Regex("\\bwhich subjects?\\b"),
        Regex("\\bwhat subjects?\\b"),
        Regex("\\bwhat can you (teach|help|do|cover|handle|tutor)\\b"),
        Regex("\\bwhat (can|do) you (know|teach)\\b(?!\\s+(about|on|in|for)\\b)"),
        Regex("\\bwhat you (can|know)\\b"),
        Regex("\\bwhat are you capable\\b"),
        Regex("\\b(your|the) capabilities?\\b"),
        Regex("\\bwhat are your (skills|strengths|features|abilities)\\b"),
        Regex("\\bwhich (areas?|topics?|fields?|courses?)\\b.*\\b(can|could|do|will)\\b.*\\b(you|u)\\b"),
        Regex("\\bwhich (areas?|topics?)\\b.*\\bfit\\b"),
        Regex("\\bwhat can i ask\\b"),
        Regex("\\bwhat do you cover\\b"),
        Regex("\\bsupported? (subjects|topics)\\b"),
        Regex("\\b subjects? (are|do|can) (you|u)\\b"),
        Regex("\\byou fit (teach|help|cover|handle)\\b"),
        Regex("\\bwetin you (fit|dey) (teach|help|cover|sabi)\\b")
    )

    private val roadmapPatterns = listOf(
        Regex("\\b(where|how) should we (begin|start)\\b"),
        Regex("\\bwhere do we (begin|start)\\b"),
        Regex("\\bwhat should we (begin|start) with\\b"),
        Regex("\\bstart from the (beginning|start|top)\\b"),
        Regex("\\bwhat should i (learn|start|study|begin) first\\b"),
        Regex("\\bwhich topic should i start\\b"),
        Regex("\\bwhere (can|do) i (begin|start)\\b"),
        Regex("\\blet'?s (begin|start)\\b.*\\b(subject|studying|learning|from)\\b")
    )

    private val studyHelpPatterns = listOf(
        Regex("\\b(i|we) (need|want) help (on|with|in)\\b"),
        Regex("\\bhelp me (with|on|in|about)\\b"),
        Regex("\\bhelp me (study|learn|understand)\\b"),
        Regex("\\bteach me\\b"),
        Regex("\\b(i|we) (am|are) (learning|studying|reading|revising)\\b"),
        Regex("\\b(i|we) (am|are) (now )?(learning|studying)\\b"),
        Regex("\\btutor me\\b"),
        Regex("\\bguide me (through|on|with)\\b"),
        Regex("\\b(i|we) want to (learn|study|understand)\\b"),
        Regex("\\bcan you (teach|tutor) me\\b"),
        Regex("\\b(i|we) need (a )?(tutor|guide|helper)\\b"),
        Regex("\\bassistance? (with|on|in)\\b")
    )

    private val definitionPatterns = listOf(
        Regex("^(what is|what are|what was|what were)\\b"),
        Regex("^define\\b"),
        Regex("^definition of\\b"),
        Regex("^meaning of\\b"),
        Regex("^(explain|describe|state|mention)\\b"),
        Regex("^(tell me about|tell me)\\b"),
        Regex("^(can you (explain|describe|tell me about))\\b"),
        Regex("^(who is|who was|who were|who are)\\b"),
        Regex("^what does\\s+(.+)\\s+mean$"),
        Regex("^(an?\\s+)?(.+?)\\s+means what$"),
        Regex("^how would you define\\b")
    )

    private val comparisonPatterns = listOf(
        Regex("^(?:what is |what's |compare |difference between |differentiate )?(.+?)\\s+(?:vs|versus)\\s+(.+)$"),
        Regex("^(?:what (?:is|are) (?:the )?|what'?s (?:the )?|show me the |tell me the )?difference between (.+?) and (.+)$"),
        Regex("^differentiate (?:between )?(.+?) and (.+)$"),
        Regex("^(?:compare|comparison between|compare and contrast) (.+?)(?:\\s+and|\\s+with|\\s+to) (.+)$")
    )

    private val examplePatterns = listOf(
        Regex("^give (me )?(an? )?example"),
        Regex("^examples? of\\b"),
        Regex("^show me (an? )?example"),
        Regex("^can you give (me )?(an? )?example")
    )

    private val calculationPatterns = listOf(
        Regex("\\b(calculate|compute|solve|evaluate|work out)\\b"),
        Regex("\\d\\s*[*+/^]\\s*\\d"),
        Regex("\\d\\s*(?:×|÷)\\s*\\d"),
        Regex("\\b\\d+(?:\\.\\d+)?\\s*%\\s*of\\s*\\d+"),
        Regex("^what is\\s+[-+]?\\d+(?:\\.\\d+)?\\s*[-+*/×÷^]\\s*"),
        Regex("\\bformula\\b.*\\b(for|of)\\b")
    )

    // ---- Bible -----------------------------------------------------------
    // Strong keywords are unambiguous on their own ("bible verses about
    // love" is a Bible query even without a reference). Soft keywords
    // (psalm, prayer, gospel...) only trigger when the sentence is not a
    // study/teaching request — "explain what a psalm is" must stay a
    // DEFINITION, "psalm 23" must be a BIBLE_QUERY.
    private val bibleStrongKeywords = listOf(
        Regex("\\b(holy )?bible\\b"), Regex("\\bkjv\\b"),
        Regex("\\bking james( version)?\\b"), Regex("\\bscriptures?\\b"),
        Regex("\\bbible verses?\\b|\\bverses? (about|from|on)\\b|\\bverse of the day\\b"),
        Regex("\\b(old|new) testament\\b"), Regex("\\btestament\\b"),
        Regex("\\b(god|the lord|jesus|christ|paul|moses|david) (said|says|told|spake|spoke|promised|commanded|spoke)\\b")
    )

    private val bibleSoftKeywords = listOf(
        Regex("\\bpray|prayer\\b"), Regex("\\bgospel\\b"),
        Regex("\\bpsalm[s]?\\b"), Regex("\\bproverb[s]?\\b"),
        Regex("\\bdisciple[s]?\\b"),
        Regex("\\bcreation (story|account)\\b"), Regex("\\bnoah'?s ark\\b")
    )

    /** Sentence shapes that mean "teach/define", not "quote scripture". */
    private val teachingPatterns = listOf(
        Regex("^what (?:is|are|was|were)\\b"),
        Regex("^(?:explain|describe|define|list|mention|state|compare|differentiate)\\b"),
        Regex("^(?:what does|what do)\\b(?!.*\\bbible\\b)"),
        Regex("^(?:why|how|when|where)\\b"),
        Regex("^(?:summari[sz]e|summary|meaning|definition|teach|tutor)\\b"),
        Regex("^(?:tell me about|tell me)\\b"),
        Regex("^(?:give me a (?:summary|list|definition|explanation))\\b")
    )

    /** "...10 questions", "3 marks", "5 pages" - task words, never chapter refs. */
    private val chapterOnlyNoise = Regex(
        "\\b(questions?|marks?|pages?|problems?|examples?|items?|students?|candidates?" +
            "|points?|seconds?|minutes?|hours?|days?|weeks?|months?|years?|figures?" +
            "|tables?|diagrams?|objects?|words?|lines?|topics?|sections?)\\b"
    )

    private val practicePatterns = listOf(
        Regex("\\b(practice|quiz|test)\\s*(me|questions?|problems?)\\b"),
        Regex("\\bgive (me )?(some |a few )?(practice )?questions\\b"),
        Regex("\\bexam questions?\\b"),
        Regex("\\bquestions? (on|for|about)\\b"),
        Regex("\\bmark my (answer|work)\\b"),
        Regex("\\bsolve this question\\b"),
        Regex("\\bexamine me\\b")
    )

    private val greetingPatterns = listOf(
        Regex("^(hi+|hello+|hey+|yo+|howzit|greetings?|good (morning|afternoon|evening|day)|how far(?: na)?|how e dey|shaku shaku)$")
    )

    private val farewellPatterns = listOf(
        Regex("^(bye+|goodbye|see you(?: later)?|see ya|good night|later|cya|take care|ttyl|until next time)$")
    )

    private val thanksPatterns = listOf(
        Regex("^(thanks|thank you|thank u|thx|thanks a lot|thanks so much|appreciate(?: it)?|cheers|nice one)$")
    )

    private val helpPatterns = listOf(
        Regex("^(help|i need help|assist me|can you help|can you help me|help me)$")
    )

    private val identityPatterns = listOf(
        Regex("\\bwho are you\\b"),
        Regex("\\bwhat is your name\\b"),
        Regex("\\bwhat'?s your name\\b"),
        Regex("\\bare you (human|real|a bot|a robot|alive|conscious)\\b"),
        Regex("\\bare you real\\b"),
        Regex("\\byou na (human|person|robot)\\b")
    )

    private val developerPatterns = listOf(
        Regex("\\bwho (made|created|built|developed|designed) (you|this|kuzmix|the app)\\b"),
        Regex("\\byour (creator|developer|maker|author|owner)\\b"),
        Regex("\\bkreadive\\b"),
        Regex("\\boni oluwatobi\\b"),
        Regex("\\bwho (is|was) (oni|the developer|the creator)\\b")
    )

    private val aboutKuzmixPatterns = listOf(
        Regex("\\bwhat is kuzmix\\b"),
        Regex("\\bwho is kuzmix\\b"),
        Regex("\\bkuzmix ai\\b.*\\b(what|about)\\b"),
        Regex("\\btell me about kuzmix\\b"),
        Regex("\\babout (this app|the app|kuzmix)\\b"),
        Regex("\\bwhat is this app\\b")
    )

    private val providerPatterns = listOf(
        Regex("\\b(use|using|do you use|are you using)\\b.*\\b(gemini|openai|chatgpt|claude|internet|online|cloud|api|server|remote)\\b"),
        Regex("\\b(do you work|work) (locally|offline|online)\\b"),
        Regex("\\bare you (offline|online)\\b"),
        Regex("\\b(need|require|internet|wifi|data)\\b.*\\b(to work|to function|connection)\\b"),
        Regex("\\bhow do you (work|operate)\\b")
    )

    private val smallTalkPatterns = listOf(
        Regex("^(how are you|how('s| is) it going|what'?s up|whats up|how you dey|you dey there|how na|you okay|are you okay|fine|okay|ok|alright|cool|great|nice|good|hm+h*|k|kk)$")
    )

    private val continuePatterns = listOf(
        Regex("^(begin|start|continue|go ahead|go on|next|proceed|okay then|ok then|alright then|yeah|yes please|sure|sounds good|another|another one|one more|more|next one|new one|again|go)$")
    )

    fun classify(nq: NormalizedQuery): IntentMatch {
        val text = nq.normalized.trim()

        if (codeQuickPrompts.any { text == it }) {
            return IntentMatch(matchCodeSubintent(text))
        }
        if (codePatterns.any { it.containsMatchIn(text) }) {
            return IntentMatch(matchCodeSubintent(text))
        }

        // Bible first - "what does John 3:16 say" must not fall into
        // DEFINITION, and "= " math must not fall into CALCULATION.
        // NOTE: detection runs on nq.raw because QueryNormalizer strips
        // '=' and ':' (line: [^\w\s*+/^().%-]).
        if (looksLikeBible(nq.raw)) {
            return IntentMatch(Intent.BIBLE_QUERY, bibleEntity(nq.raw))
        }

        // Equations - linear "2x - 3 = 34", "1x - 2 = 32", quadratics,
        // factorisation. Checked before CALCULATION (which the word
        // "solve" would otherwise grab) and before DEFINITION (which
        // "what is ..." would grab).
        if (MathSolver.isEquation(nq.raw)) {
            return IntentMatch(Intent.SOLVE_EQUATION, MathSolver.extractEquation(nq.raw))
        }
        if (MathSolver.isFactorRequest(nq.raw)) {
            return IntentMatch(Intent.SOLVE_EQUATION, nq.raw.trim())
        }

        for (pattern in capabilityPatterns) {
            val m = pattern.find(text)
            if (m != null) {
                val intent = if (pattern.pattern.contains("subjects")) Intent.SUBJECT_CAPABILITY else Intent.CAPABILITY_QUERY
                return IntentMatch(intent)
            }
        }

        if (identityPatterns.any { it.containsMatchIn(text) }) {
            return IntentMatch(Intent.IDENTITY)
        }
        if (developerPatterns.any { it.containsMatchIn(text) }) {
            return IntentMatch(Intent.DEVELOPER_INFO)
        }
        if (aboutKuzmixPatterns.any { it.containsMatchIn(text) }) {
            return IntentMatch(Intent.ABOUT_KUZMIX)
        }
        if (providerPatterns.any { it.containsMatchIn(text) }) {
            return IntentMatch(Intent.ABOUT_KUZMIX, entity = "provider")
        }

        val knowAbout = Regex("^(?:what do you know|what can you tell me|tell me)\\s+(?:about|on)\\s+(.+)$").find(text)
        if (knowAbout != null && knowAbout.groupValues[1].isNotBlank()) {
            return IntentMatch(Intent.DEFINITION, cleanEntity(knowAbout.groupValues[1]))
        }

        if (roadmapPatterns.any { it.containsMatchIn(text) }) {
            return IntentMatch(Intent.STUDY_ROADMAP)
        }

        for (pattern in studyHelpPatterns) {
            if (pattern.containsMatchIn(text)) {
                val subjectEntity = extractSubjectEntity(text)
                return IntentMatch(Intent.STUDY_HELP, subjectEntity)
            }
        }
        if (Regex("^(let'?s|we should|i should) (study|learn|start|begin)\\b").containsMatchIn(text)) {
            return IntentMatch(Intent.STUDY_START, extractSubjectEntity(text))
        }

        if (continuePatterns.any { it.matches(text) }) {
            return IntentMatch(Intent.SMALL_TALK, entity = "__continue__")
        }

        if (calculationPatterns.any { it.containsMatchIn(text) }) {
            return IntentMatch(Intent.CALCULATION, extractCalculationEntity(text))
        }

        for (pattern in comparisonPatterns) {
            val m = pattern.find(text)
            if (m != null && m.groupValues.size >= 3 && m.groupValues[1].isNotBlank() && m.groupValues[2].isNotBlank()) {
                return IntentMatch(
                    Intent.COMPARISON,
                    cleanEntity(m.groupValues[1]),
                    cleanEntity(m.groupValues[2])
                )
            }
        }

        for (pattern in definitionPatterns) {
            val m = pattern.find(text)
            if (m != null) {
                val entity = when {
                    m.groupValues.size > 2 && m.groupValues[2].isNotBlank() -> m.groupValues[2]
                    else -> text.removePrefix(m.value).trim()
                }
                val cleaned = cleanEntity(entity)
                if (cleaned.isNotBlank()) {
                    return IntentMatch(Intent.DEFINITION, cleaned)
                }
            }
        }

        if (examplePatterns.any { it.containsMatchIn(text) }) {
            return IntentMatch(Intent.EXAMPLE_REQUEST, extractExampleEntity(text))
        }

        if (practicePatterns.any { it.containsMatchIn(text) }) {
            return IntentMatch(Intent.PRACTICE_QUESTION, extractSubjectEntity(text))
        }

        if (greetingPatterns.any { it.matches(text) }) return IntentMatch(Intent.GREETING)
        if (farewellPatterns.any { it.matches(text) }) return IntentMatch(Intent.FAREWELL)
        if (thanksPatterns.any { it.matches(text) }) return IntentMatch(Intent.THANKS)
        if (helpPatterns.any { it.matches(text) }) return IntentMatch(Intent.HELP)

        if (smallTalkPatterns.any { it.matches(text) }) return IntentMatch(Intent.SMALL_TALK)

        if (text.startsWith("what") || text.startsWith("how") || text.startsWith("why") ||
            text.startsWith("when") || text.startsWith("where") || text.startsWith("who")
        ) {
            return IntentMatch(Intent.GENERAL_KNOWLEDGE, cleanEntity(text))
        }

        return IntentMatch(Intent.UNKNOWN, cleanEntity(text))
    }

    private fun matchCodeSubintent(text: String): Intent = when {
        Regex("\\b(fix|debug|troubleshoot)\\b").containsMatchIn(text) ||
            Regex("\\b(error|bug|crash|issue|not working|fail)\\b").containsMatchIn(text) -> Intent.DEBUGGING
        Regex("\\bexplain\\b.*\\bcode\\b").containsMatchIn(text) ||
            Regex("\\b(this|my|the) code\\b").containsMatchIn(text) -> Intent.CODE_EXPLANATION
        Regex("\\badd\\b.*\\bfeature\\b").containsMatchIn(text) -> Intent.FEATURE_REQUEST
        Regex("\\bgenerate\\b.*\\b(full )?project\\b").containsMatchIn(text) -> Intent.PROJECT_GENERATION
        Regex("\\b(create|build|write)\\b").containsMatchIn(text) -> Intent.CODE_GENERATION
        else -> Intent.CODE_ASSISTANT
    }

    // ---- Bible detection helpers -----------------------------------------

    /**
     * True when the message is a Bible lookup/search. Runs on the RAW text
     * because QueryNormalizer strips ':' and '='. Acceptance rules:
     *  1. A resolvable verse reference ("John 3:16", "1 cor 13:4-7") wins.
     *  2. A chapter-only reference ("Psalm 23", "read John 3") needs a
     *     supporting signal: a bible keyword, the word "chapter", the
     *     reference at the head of the message, or a very short message.
     *  3. No reference at all: strong keywords (bible/kjv/scripture) always
     *     win; soft keywords (psalm/prayer/gospel...) lose to study or
     *     teaching requests so "explain what a psalm is" stays a DEFINITION.
     */
    private fun looksLikeBible(raw: String): Boolean {
        val probe = raw.trim()
        if (probe.isBlank()) return false
        val lower = probe.lowercase()

        val ref = BibleRefs.findReference(probe)
        if (ref != null) {
            if (ref.firstVerse != null) return true
            // chapter-only: an instruction containing "...10 questions/pages/
            // marks..." is a task, not a reference ("mark 10 questions"
            // must not become Mark chapter 10)
            if (chapterOnlyNoise.containsMatchIn(lower)) return false
            if (bibleStrongKeywords.any { it.containsMatchIn(lower) }) return true
            if (bibleSoftKeywords.any { it.containsMatchIn(lower) }) return true
            if (Regex("\\bchapter\\b").containsMatchIn(lower)) return true
            if (BibleRefs.startsNearBeginning(ref, probe)) return true
            if (probe.length <= 16) return true
            return false
        }

        val strong = bibleStrongKeywords.any { it.containsMatchIn(lower) }
        if (strong) return true
        if (!bibleSoftKeywords.any { it.containsMatchIn(lower) }) return false
        // soft keyword only: don't hijack teach/define/study requests
        val trimmed = lower.trim()
        if (teachingPatterns.any { it.containsMatchIn(trimmed) }) return false
        if (studyHelpPatterns.any { it.containsMatchIn(trimmed) }) return false
        if (practicePatterns.any { it.containsMatchIn(trimmed) }) return false
        return true
    }

    /** Canonical reference string for the entity slot ("John 3:16"), or null. */
    private fun bibleEntity(raw: String): String? =
        BibleRefs.findReference(raw.trim())?.display()


    private fun cleanEntity(entity: String): String {
        var e = entity.lowercase().trim()
        e = e.replace(Regex("^[?\\s]+"), "").replace(Regex("[?\\s]+$"), "")
        val filler = Regex("^(the|a|an|is|are|about|of|on|in|for)\\s+")
        while (filler.containsMatchIn(e)) {
            val before = e
            e = e.replaceFirst(filler, "")
            if (e == before) break
        }
        return e.trim()
    }

    private fun extractSubjectEntity(text: String): String? {
        val patterns = listOf(
            Regex("\\b(?:on|with|in|about|for)\\s+(.+)$"),
            Regex("^(?:i|we) (?:need|want) help (?:on|with|in)\\s+(.+)$"),
            Regex("^(?:i|we) (?:am|are) (?:learning|studying)\\s+(.+)$"),
            Regex("^(?:teach|tutor) me\\s+(?:about\\s+|on\\s+)?(.+)$"),
            Regex("^(?:i|we) (?:want to|need to|would like to|should) (?:learn|study|understand|know)\\s+(?:about\\s+|on\\s+)?(.+)$"),
            Regex("^(?:help|guide) me\\s+(?:with|on|about|through)\\s+(?:the\\s+|a\\s+)?(.+)$"),
            Regex("^(?:i|we) (?:am|are) (?:now\\s+)?(?:learning|studying)\\s+(?:about\\s+)?(.+)$")
        )
        for (p in patterns) {
            val m = p.find(text)
            if (m != null) {
                val cleaned = cleanEntity(m.groupValues[1])
                if (cleaned.isNotBlank() && cleaned.length <= 60) return cleaned
            }
        }
        return null
    }

    private fun extractExampleEntity(text: String): String? {
        val m = Regex("(?:example|examples)\\s+(?:of|for)\\s+(.+)$").find(text)
            ?: Regex("^give (?:me )?(?:an? )?example\\s+(?:of|for)\\s+(.+)$").find(text)
        return m?.let { cleanEntity(it.groupValues[1]) }
    }

    private fun extractCalculationEntity(text: String): String? {
        val m = Regex("^what is\\s+(.+)$").find(text)
        if (m != null) {
            val expr = m.groupValues[1]
            if (expr.any { it.isDigit() }) return expr.trim()
        }
        return cleanEntity(text)
    }
}
