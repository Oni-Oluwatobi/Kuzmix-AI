package com.kuzmix.ai.domain.ai

import android.net.Uri
import com.kuzmix.ai.domain.model.ChatMessage
import kotlinx.coroutines.flow.StateFlow

class AIRepository(
    private val knowledgeEngine: KnowledgeEngine,
    private val localAIEngine: LocalAIEngine,
    private val secretDocs: SecretDocs = SecretDocs()
) {

    val activeEngineName: String
        get() = if (localAIEngine.isAvailable) localAIEngine.engineName else knowledgeEngine.engineName

    val isOfflineReady: Boolean
        get() = knowledgeEngine.isAvailable || localAIEngine.isAvailable

    val modelStatus: LocalAIEngine.ModelStatus
        get() = localAIEngine.modelStatus

    val modelStatusFlow: StateFlow<LocalAIEngine.ModelStatus>
        get() = localAIEngine.statusFlow

    val modelFileLabel: StateFlow<String?>
        get() = localAIEngine.modelFileLabel

    /**
     * Tappable chips for the chat UI: wizard steps (class/term/subject/
     * topic) or MCQ option letters, re-synced after every reply.
     */
    val pendingChoices: List<String>
        get() = knowledgeEngine.pendingChoices

    /** True while an interactive quiz/reading session holds in the engine. */
    val sessionActive: Boolean
        get() = knowledgeEngine.sessionActive

    /** True when the text is a secret-drawer code (master or document). */
    fun isSecretCode(text: String): Boolean = secretDocs.isCode(text)

    /**
     * Asks the best available engine. With a local model installed this is
     * real generative chat: knowledge chunks are retrieved first (offline
     * FTS), embedded into the prompt, and [onPartial] streams the answer as
     * it is generated. Falls back to the deterministic knowledge engine.
     *
     * A live session (quiz wizard / MCQ round) or an explicit quiz request
     * is routed STRAIGHT to the knowledge engine — the local model would
     * bypass the class-scoped wizard entirely.
     */
    suspend fun askStream(
        query: String,
        history: List<ChatMessage> = emptyList(),
        onPartial: (String) -> Unit = {}
    ): String {
        // Admin drawer first: the codes must never reach the local model or
        // the knowledge engine (which would answer them as ordinary text).
        secretDocs.handle(query)?.let { return it.text }

        val interactive = knowledgeEngine.sessionActive || isQuizRequest(query)
        if (!interactive && localAIEngine.isAvailable) {
            try {
                val hits = knowledgeEngine.searchPreview(query, 5)
                val prompt = PromptBuilder.build(query, history, hits)
                return localAIEngine.generateStream(prompt, onPartial)
            } catch (t: Throwable) {
                // fall back to the knowledge engine below
            }
        }
        return knowledgeEngine.generateResponse(query, history)
    }

    /** "practice/random/shuffle questions..." -> the class-scoped wizard. */
    private fun isQuizRequest(query: String): Boolean =
        IntentClassifier.classify(QueryNormalizer.normalize(query)).intent ==
            Intent.PRACTICE_QUESTION

    suspend fun ask(query: String, history: List<ChatMessage> = emptyList()): String =
        askStream(query, history) {}

    suspend fun searchKnowledge(query: String, limit: Int = 5): List<KnowledgeHit> =
        knowledgeEngine.searchPreview(query, limit)

    suspend fun importModel(uri: Uri): LocalAIEngine.ImportResult =
        localAIEngine.importModel(uri)

    suspend fun removeModel() = localAIEngine.removeModel()

    suspend fun loadModelIfPresent(path: String) = localAIEngine.loadIfPresent(path)
}
