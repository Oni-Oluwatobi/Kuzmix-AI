package com.kuzmix.ai.ui.screens.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuzmix.ai.domain.model.Conversation
import com.kuzmix.ai.domain.repository.ChatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatsViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    val conversations: StateFlow<List<Conversation>> = chatRepository.observeConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteConversation(id: Long) {
        viewModelScope.launch { chatRepository.deleteConversation(id) }
    }
}
