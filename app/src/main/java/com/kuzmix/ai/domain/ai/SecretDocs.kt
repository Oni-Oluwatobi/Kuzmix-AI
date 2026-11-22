package com.kuzmix.ai.domain.ai

/**
 * The admin-only "secret drawer", driven entirely from the normal chat box.
 *
 * ```
 * user:  CEOTKG@2026        ->  PICk a doc
 * user:  CEOTKG@1 / @2 / @3 ->  an unlock card for the SS1 / SS2 / SS3
 *                               study guide (opens INSIDE the app,
 *                               read-only, screenshots blocked)
 * ```
 *
 * The document codes are only honoured *after* the master code has been
 * entered; any other message closes the drawer again. Unlock cards carry
 * [MARKER] so [com.kuzmix.ai.ui.components.KuzmixChatBubble] renders them
 * read-only, and an `[[study:key]]` marker the bubble turns into the
 * in-app open button.
 *
 * This flow is fully independent of the scheme-of-work corpus — the guides
 * ship as bundled assets, so every class works even when the public quiz
 * wizard reports "couldn't find scheme-of-work material".
 *
 * This object holds no persistent state: closing the app (or typing an
 * unrelated message) relocks the drawer.
 */
/**
 * One piece of a protected guide reply: prose or an embedded diagram.
 */
sealed interface GuideSegment {
    data class Text(val text: String) : GuideSegment
    data class Image(val asset: String) : GuideSegment
}

class SecretDocs {

    data class Reply(val text: String, val protected: Boolean = false)

    private var awaitingDoc = false

    /**
     * Intercepts a raw chat message. Returns null when the message is not
     * part of the secret flow (it then travels through the normal engine).
     */
    fun handle(raw: String): Reply? {
        val code = normalize(raw)
        if (code.isEmpty()) return null

        if (code == MASTER_CODE) {
            awaitingDoc = true
            return Reply(MENU)
        }

        // document codes are meaningless until the master code has been accepted
        if (!awaitingDoc) return null
        awaitingDoc = false

        val label = DOC_LABELS[code] ?: return null
        val key = DOC_KEYS[code] ?: return null
        val body = "Study notes by Kuzmix AI — $label\n" +
            "1st Term Exam · read-only · screenshots blocked\n\n" +
            "[[study:$key]]"
        return Reply(MARKER + body, protected = true)
    }

    /** True for the master code or any document code (used for chat titles). */
    fun isCode(raw: String): Boolean {
        val c = normalize(raw)
        return c == MASTER_CODE || c in DOC_KEYS
    }

    fun reset() {
        awaitingDoc = false
    }

    companion object {
        const val MASTER_CODE = "CEOTKG@2026"
        const val MENU = "PICk a doc"

        /**
         * Invisible prefix: zero-width space + invisible separator. The chat
         * bubble strips it before display and, because of it, refuses to
         * offer the copy action. Surviving a database round-trip is the
         * point — reopened conversations stay read-only too.
         */
        const val MARKER = "\u200B\u2063"

        val DOC_KEYS: Map<String, String> = mapOf(
            "CEOTKG@1" to "ss1",
            "CEOTKG@2" to "ss2",
            "CEOTKG@3" to "ss3"
        )

        val DOC_LABELS: Map<String, String> = mapOf(
            "CEOTKG@1" to "SS 1",
            "CEOTKG@2" to "SS 2",
            "CEOTKG@3" to "SS 3"
        )

        /** "ss1" -> "SS 1" for the viewer's top bar. */
        fun labelFor(key: String): String = when (key.lowercase()) {
            "ss1" -> "SS 1"
            "ss2" -> "SS 2"
            "ss3" -> "SS 3"
            else -> key.uppercase()
        }

        private val IMG_TAG = Regex("\\[\\[img:([^\\]]+)]]")

        /** Splits guide text around `[[img:...]]` markers, in order. */
        fun segments(body: String): List<GuideSegment> {
            val out = mutableListOf<GuideSegment>()
            var last = 0
            for (m in IMG_TAG.findAll(body)) {
                if (m.range.first > last) out.add(GuideSegment.Text(body.substring(last, m.range.first)))
                out.add(GuideSegment.Image(m.groupValues[1].trim()))
                last = m.range.last + 1
            }
            if (last < body.length) out.add(GuideSegment.Text(body.substring(last)))
            return if (out.isEmpty()) listOf(GuideSegment.Text(body)) else out
        }

        /**
         * Splits a long guide into chat-sized parts on line boundaries so a
         * 50k-character exam never lands as one giant bubble on a low-end
         * phone.
         */
        fun split(body: String, maxChars: Int = 4500): List<String> {
            if (body.length <= maxChars) return listOf(body)
            val parts = mutableListOf<String>()
            val current = StringBuilder()
            for (line in body.lineSequence()) {
                if (current.isNotEmpty() && current.length + line.length + 1 > maxChars) {
                    parts.add(current.toString().trimEnd())
                    current.clear()
                }
                current.append(line).append('\n')
            }
            if (current.isNotBlank()) parts.add(current.toString().trimEnd())
            return if (parts.isEmpty()) listOf(body) else parts
        }

        private fun normalize(raw: String): String =
            raw.trim().uppercase().replace(Regex("\\s+"), "")
    }
}
