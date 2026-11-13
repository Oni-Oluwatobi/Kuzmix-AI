package com.kuzmix.ai.ui.screens.code

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuzmix.ai.domain.ai.AIRepository
import com.kuzmix.ai.domain.model.ChatMessage
import com.kuzmix.ai.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CodeUiState(
    val selectedLanguage: String = "Android (Kotlin)",
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isLoading: Boolean = false,
    val activeTab: Int = 0
)

class CodeViewModel(
    private val chatRepository: ChatRepository,
    private val aiRepository: AIRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CodeUiState())
    val uiState: StateFlow<CodeUiState> = _uiState.asStateFlow()

    private var conversationId: Long = 0

    init {
        viewModelScope.launch {
            conversationId = chatRepository.createConversation("Code Assistant", isCodeMode = true)
        }
    }

    fun onLanguageSelected(language: String) {
        _uiState.update { it.copy(selectedLanguage = language) }
    }

    fun onTabChange(index: Int) {
        _uiState.update { it.copy(activeTab = index) }
    }

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun sendPrompt(text: String) {
        if (text.isBlank() || _uiState.value.isLoading) return
        _uiState.update { it.copy(input = "", isLoading = true) }

        viewModelScope.launch {
            chatRepository.addMessage(conversationId, ChatMessage.Role.USER, text)
            val history = chatRepository.getMessages(conversationId)

            val context = "Language: ${_uiState.value.selectedLanguage}. $text"
            val response = withContext(Dispatchers.Default) {
                aiRepository.ask(context, history)
            }

            chatRepository.addMessage(conversationId, ChatMessage.Role.ASSISTANT, response)
            val updated = chatRepository.getMessages(conversationId)
            _uiState.update { it.copy(messages = updated, isLoading = false) }
        }
    }

    fun useQuickPrompt(prompt: String) {
        _uiState.update { it.copy(input = prompt) }
    }
}
