package com.kuzmix.ai.domain.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.kuzmix.ai.domain.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device generative engine backed by MediaPipe Tasks GenAI (llama.cpp
 * under the hood). Loads a user-imported .task / .gguf model from private
 * storage and runs inference fully offline. When no model is installed it
 * reports unavailable so [AIRepository] falls back to the knowledge engine.
 */
class LocalAIEngine(
    context: Context,
    private val modelFileManager: ModelFileManager,
    private val persistPath: suspend (String?) -> Unit = {}
) : AIEngine {

    private val appContext = context.applicationContext
    private val mutex = Mutex()

    @Volatile
    private var llm: LlmInference? = null

    private val _statusFlow = MutableStateFlow(ModelStatus.NOT_INSTALLED)
    val statusFlow: StateFlow<ModelStatus> = _statusFlow.asStateFlow()

    private val _modelFileLabel = MutableStateFlow<String?>(null)
    val modelFileLabel: StateFlow<String?> = _modelFileLabel.asStateFlow()

    val modelStatus: ModelStatus
        get() = _statusFlow.value

    var modelPath: String? = null
        private set

    override val engineName: String = "Local Model"
    override val isLocal: Boolean = true
    override val isAvailable: Boolean
        get() = llm != null && _statusFlow.value == ModelStatus.READY

    /** Restore a previously imported model at app start. */
    suspend fun loadIfPresent(path: String) {
        if (llm != null) return
        if (!modelFileManager.exists(path)) return
        loadModel(path)
    }

    suspend fun loadModel(path: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (llm != null && modelPath == path) return@withLock
            _statusFlow.value = ModelStatus.LOADING
            _modelFileLabel.value = modelFileManager.describe(path)
            try {
                unloadLocked()
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelAssetPath(path)
                    .setMaxTokens(MAX_TOKENS)
                    .setTemperature(TEMPERATURE)
                    .setTopK(TOP_K)
                    .setRandomSeed(SEED)
                    .build()
                llm = LlmInference.createFromOptions(appContext, options)
                modelPath = path
                _statusFlow.value = ModelStatus.READY
                persistPath(path)
                Log.i(TAG, "model loaded: $path")
            } catch (t: Throwable) {
                Log.e(TAG, "model load failed", t)
                llm = null
                modelPath = null
                _statusFlow.value = ModelStatus.ERROR
            }
        }
    }

    suspend fun importModel(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        try {
            val file = modelFileManager.importFromUri(uri)
            loadModel(file.absolutePath)
            if (_statusFlow.value == ModelStatus.READY) {
                ImportResult.Success(modelFileManager.describe(file.absolutePath) ?: file.name)
            } else {
                ImportResult.Failure("The model failed to load. Use a Gemma .task bundle or a Qwen/Llama .gguf file.")
            }
        } catch (io: IOException) {
            ImportResult.Failure(io.message ?: "Could not copy the model file")
        } catch (t: Throwable) {
            Log.e(TAG, "import failed", t)
            ImportResult.Failure("Import failed: ${t.message}")
        }
    }

    suspend fun removeModel() = withContext(Dispatchers.IO) {
        mutex.withLock {
            unloadLocked()
            modelPath?.let { modelFileManager.delete(it) }
            modelPath = null
            _modelFileLabel.value = null
            _statusFlow.value = ModelStatus.NOT_INSTALLED
            persistPath(null)
        }
    }

    override suspend fun generateResponse(
        query: String,
        history: List<ChatMessage>
    ): String = withContext(Dispatchers.Default) {
        val model = llm ?: return@withContext ""
        mutex.withLock {
            model.generateResponse(query)
        }
    }

    /** Streaming generation; [onPartial] receives the answer built so far. */
    suspend fun generateStream(prompt: String, onPartial: (String) -> Unit): String {
        val model = llm ?: throw IllegalStateException("Local model is not loaded")
        return mutex.withLock {
            suspendCancellableCoroutine { cont ->
                model.generateResponseAsync(
                    prompt,
                    object : LlmInference.LlmInferenceResponseListener {
                        override fun onPartialResponse(partialResult: String) {
                            if (cont.isActive) onPartial(partialResult)
                        }

                        override fun onResponse(response: String) {
                            if (cont.isActive) cont.resume(response)
                        }

                        override fun onError(error: LlmInference.LlmInferenceResponseListener.Error) {
                            if (cont.isActive) {
                                cont.resumeWithException(
                                    RuntimeException(error.message())
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    private fun unloadLocked() {
        try {
            llm?.close()
        } catch (t: Throwable) {
            Log.w(TAG, "llm close failed", t)
        }
        llm = null
    }

    sealed class ImportResult {
        data class Success(val modelLabel: String) : ImportResult()
        data class Failure(val reason: String) : ImportResult()
    }

    enum class ModelStatus {
        NOT_INSTALLED,
        LOADING,
        READY,
        ERROR
    }

    companion object {
        private const val TAG = "LocalAIEngine"
        private const val MAX_TOKENS = 1024
        private const val TEMPERATURE = 0.7f
        private const val TOP_K = 40
        private const val SEED = 101L
    }
}
