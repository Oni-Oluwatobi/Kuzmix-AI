package com.kuzmix.ai.domain.ai

import android.net.Uri
import com.kuzmix.ai.domain.model.ChatMessage
import kotlinx.coroutines.flow.StateFlow

class AIRepository(
    private val knowledgeEngine: KnowledgeEngine,
    private val localAIEngine: LocalAIEngine
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
     * Asks the best available engine. With a local model installed this is
     * real generative chat: knowledge chunks are retrieved first (offline
     * FTS), embedded into the prompt, and [onPartial] streams the answer as
     * it is generated. Falls back to the deterministic knowledge engine.
     */
    suspend fun askStream(
        query: String,
        history: List<ChatMessage> = emptyList(),
        onPartial: (String) -> Unit = {}
    ): String {
        if (localAIEngine.isAvailable) {
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

    suspend fun ask(query: String, history: List<ChatMessage> = emptyList()): String =
        askStream(query, history) {}

    suspend fun searchKnowledge(query: String, limit: Int = 5): List<KnowledgeHit> =
        knowledgeEngine.searchPreview(query, limit)

    suspend fun importModel(uri: Uri): LocalAIEngine.ImportResult =
        localAIEngine.importModel(uri)

    suspend fun removeModel() = localAIEngine.removeModel()

    suspend fun loadModelIfPresent(path: String) = localAIEngine.loadIfPresent(path)
}
