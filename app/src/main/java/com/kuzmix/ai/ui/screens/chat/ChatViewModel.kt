package com.kuzmix.ai.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuzmix.ai.domain.ai.AIRepository
import com.kuzmix.ai.domain.ai.SecretDocs
import com.kuzmix.ai.domain.model.ChatMessage
import com.kuzmix.ai.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatUiState(
    val conversationId: Long = 0,
    val title: String = "New Chat",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val streamingText: String? = null,
    val engineLabel: String = "Kuzmix Knowledge Engine (Offline)",
    /** Tappable chips: quiz-wizard steps or MCQ letters (A/B/C/D). */
    val chips: List<String> = emptyList()
)

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val aiRepository: AIRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var history: List<ChatMessage> = emptyList()
    private var observeJob: Job? = null

    fun loadConversation(conversationId: Long, initialMessage: String? = null) {
        if (_uiState.value.conversationId == conversationId && _uiState.value.messages.isNotEmpty()) {
            if (!initialMessage.isNullOrBlank()) sendMessage(initialMessage)
            return
        }

        viewModelScope.launch {
            observeJob?.cancel()
            val conversation = chatRepository.getConversation(conversationId)
            val messages = chatRepository.getMessages(conversationId)
            history = messages
            _uiState.update {
                it.copy(
                    conversationId = conversationId,
                    title = conversation?.title ?: "Chat",
                    messages = messages,
                    isLoading = false,
                    streamingText = null,
                    engineLabel = aiRepository.activeEngineName + " (Offline)",
                    chips = aiRepository.pendingChoices
                )
            }

            observeJob = viewModelScope.launch {
                chatRepository.observeMessages(conversationId).collect { msgs ->
                    history = msgs
                    _uiState.update { it.copy(messages = msgs) }
                }
            }

            // Home-search hand-off: send the typed query once the
            // conversation is loaded (state must hold the id first).
            if (!initialMessage.isNullOrBlank()) {
                sendMessage(initialMessage)
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val conversationId = _uiState.value.conversationId

        viewModelScope.launch {
            chatRepository.addMessage(conversationId, ChatMessage.Role.USER, text)

            if (_uiState.value.title == "New Chat") {
                val title = when {
                    aiRepository.isSecretCode(text) -> "Secret docs"
                    else -> text.take(40).ifBlank { "New Chat" }
                }
                chatRepository.renameConversation(conversationId, title)
                _uiState.update { it.copy(title = title) }
            }

            // chips are cleared while an answer streams — a tap can never
            // target a stale wizard step or MCQ option
            _uiState.update { it.copy(isLoading = true, streamingText = null, chips = emptyList()) }

            val response = withContext(Dispatchers.Default) {
                aiRepository.askStream(text, history) { partial ->
                    _uiState.update { it.copy(streamingText = partial) }
                }
            }

            // Protected guides arrive marker-prefixed; split the long ones so
            // every bubble stays light on low-end phones.
            val replies = if (response.startsWith(SecretDocs.MARKER)) {
                SecretDocs.split(response.removePrefix(SecretDocs.MARKER))
                    .map { SecretDocs.MARKER + it }
            } else {
                listOf(response)
            }
            replies.forEach { reply ->
                chatRepository.addMessage(conversationId, ChatMessage.Role.ASSISTANT, reply)
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    streamingText = null,
                    chips = aiRepository.pendingChoices
                )
            }
        }
    }

    fun deleteActiveConversation() {
        val id = _uiState.value.conversationId
        viewModelScope.launch {
            observeJob?.cancel()
            observeJob = null
            if (id > 0) {
                chatRepository.deleteConversation(id)
            }
            history = emptyList()
            _uiState.update { ChatUiState() }
        }
    }
}
