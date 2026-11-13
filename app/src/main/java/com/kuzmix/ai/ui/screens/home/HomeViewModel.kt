package com.kuzmix.ai.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuzmix.ai.domain.ai.AIRepository
import com.kuzmix.ai.domain.ai.KnowledgeHit
import com.kuzmix.ai.domain.model.Conversation
import com.kuzmix.ai.domain.repository.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val chatRepository: ChatRepository,
    private val aiRepository: AIRepository
) : ViewModel() {

    val conversations: StateFlow<List<Conversation>> = chatRepository.observeConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val offlineReady: Boolean get() = aiRepository.isOfflineReady

    private val _searchQuery = MutableStateFlow("")

    /** Debounced knowledge-base preview hits for the home search bar. */
    val knowledgeHits: StateFlow<List<KnowledgeHit>> = _searchQuery
        .debounce(SEARCH_DEBOUNCE_MS)
        .mapLatest { raw ->
            val q = raw.trim()
            if (q.length < 2) emptyList() else aiRepository.searchKnowledge(q, 5)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQuery(value: String) {
        _searchQuery.value = value
    }

    fun startNewChat(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = chatRepository.createConversation("New Chat")
            onCreated(id)
        }
    }

    fun startTitledChat(title: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = chatRepository.createConversation(title)
            onCreated(id)
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}
