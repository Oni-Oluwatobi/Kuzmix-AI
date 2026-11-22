package com.kuzmix.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kuzmix.ai.di.AppContainer
import com.kuzmix.ai.domain.model.Conversation
import com.kuzmix.ai.domain.repository.ChatRepository
import com.kuzmix.ai.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val conversations: StateFlow<List<Conversation>> = chatRepository.observeConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userName: StateFlow<String> = settingsRepository.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Oni Oluwatobi")

    val isFirstLaunchDone: StateFlow<Boolean?> = settingsRepository.isFirstLaunchDone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _activeConversationId = MutableStateFlow<Long?>(null)
    val activeConversationId: StateFlow<Long?> = _activeConversationId.asStateFlow()

    // One-shot hand-off: home search -> newly created chat (consumed once).
    private var pendingInitialMessage: String? = null

    fun setPendingInitialMessage(text: String) {
        pendingInitialMessage = text
    }

    fun consumePendingInitialMessage(): String? =
        pendingInitialMessage.also { pendingInitialMessage = null }

    fun markFirstLaunchDone() {
        viewModelScope.launch { settingsRepository.setFirstLaunchDone(true) }
    }

    fun resetFirstLaunch() {
        viewModelScope.launch { settingsRepository.setFirstLaunchDone(false) }
    }

    fun setActiveConversation(id: Long?) {
        _activeConversationId.value = id
    }

    fun createConversation(title: String = "New Chat", isCodeMode: Boolean = false, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = chatRepository.createConversation(title, isCodeMode)
            _activeConversationId.value = id
            onCreated(id)
        }
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch { chatRepository.deleteConversation(id) }
    }
}

class KuzmixViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) ->
                MainViewModel(container.chatRepository, container.settingsRepository) as T
            modelClass.isAssignableFrom(com.kuzmix.ai.ui.screens.chat.ChatViewModel::class.java) ->
                com.kuzmix.ai.ui.screens.chat.ChatViewModel(container.chatRepository, container.aiRepository) as T
            modelClass.isAssignableFrom(com.kuzmix.ai.ui.screens.chats.ChatsViewModel::class.java) ->
                com.kuzmix.ai.ui.screens.chats.ChatsViewModel(container.chatRepository) as T
            modelClass.isAssignableFrom(com.kuzmix.ai.ui.screens.home.HomeViewModel::class.java) ->
                com.kuzmix.ai.ui.screens.home.HomeViewModel(container.chatRepository, container.aiRepository) as T
            modelClass.isAssignableFrom(com.kuzmix.ai.ui.screens.profile.ProfileViewModel::class.java) ->
                com.kuzmix.ai.ui.screens.profile.ProfileViewModel(container.settingsRepository, container.aiRepository) as T
            modelClass.isAssignableFrom(com.kuzmix.ai.ui.screens.auth.AuthViewModel::class.java) ->
                com.kuzmix.ai.ui.screens.auth.AuthViewModel(container.authRepository) as T
            modelClass.isAssignableFrom(com.kuzmix.ai.ui.screens.imagegen.ImageGenViewModel::class.java) ->
                com.kuzmix.ai.ui.screens.imagegen.ImageGenViewModel() as T
            modelClass.isAssignableFrom(com.kuzmix.ai.ui.screens.code.CodeViewModel::class.java) ->
                com.kuzmix.ai.ui.screens.code.CodeViewModel(container.chatRepository, container.aiRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
