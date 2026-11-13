package com.kuzmix.ai.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuzmix.ai.domain.ai.AIRepository
import com.kuzmix.ai.domain.ai.LocalAIEngine
import com.kuzmix.ai.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val userName: String = "Oni Oluwatobi",
    val userEmail: String = "thekreadivegalaxy@gmail.com",
    val appearance: String = "Dark",
    val language: String = "English",
    val notificationsEnabled: Boolean = true,
    val modelStatus: String = "Knowledge Base Ready",
    val isModelInstalled: Boolean = false,
    val engineName: String = "Kuzmix Knowledge Base"
)

class ProfileViewModel(
    private val settingsRepository: SettingsRepository,
    private val aiRepository: AIRepository
) : ViewModel() {

    val userName: StateFlow<String> = settingsRepository.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Oni Oluwatobi")

    val userEmail: StateFlow<String> = settingsRepository.userEmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "thekreadivegalaxy@gmail.com")

    val appearance: StateFlow<String> = settingsRepository.appearance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Dark")

    val language: StateFlow<String> = settingsRepository.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "English")

    val notificationsEnabled: StateFlow<Boolean> = settingsRepository.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val modelStatus: StateFlow<String> = aiRepository.modelStatusFlow
        .map { status ->
            when (status) {
                LocalAIEngine.ModelStatus.READY -> "Local Model Ready"
                LocalAIEngine.ModelStatus.LOADING -> "Loading Model..."
                LocalAIEngine.ModelStatus.ERROR -> "Model Error"
                LocalAIEngine.ModelStatus.NOT_INSTALLED ->
                    "Knowledge Base Ready • Model Not Installed"
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            "Knowledge Base Ready • Model Not Installed"
        )

    val isModelInstalled: StateFlow<Boolean> = aiRepository.modelStatusFlow
        .map { it == LocalAIEngine.ModelStatus.READY }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val modelFileLabel: StateFlow<String?> = aiRepository.modelFileLabel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val engineName: String get() = aiRepository.activeEngineName

    fun setAppearance(value: String) {
        viewModelScope.launch { settingsRepository.setAppearance(value) }
    }

    fun setLanguage(value: String) {
        viewModelScope.launch { settingsRepository.setLanguage(value) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationsEnabled(enabled) }
    }

    fun importModel(uri: Uri) {
        viewModelScope.launch { aiRepository.importModel(uri) }
    }

    fun removeModel() {
        viewModelScope.launch { aiRepository.removeModel() }
    }
}
