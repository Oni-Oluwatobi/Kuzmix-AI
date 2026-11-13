package com.kuzmix.ai.ui.screens.imagegen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ImageGenUiState(
    val prompt: String = "",
    val style: String = "Realistic",
    val aspectRatio: String = "1:1",
    val isGenerating: Boolean = false,
    val generatedImage: String? = null,
    val statusMessage: String? = null
)

class ImageGenViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ImageGenUiState())
    val uiState: StateFlow<ImageGenUiState> = _uiState.asStateFlow()

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value.take(500), statusMessage = null) }
    }

    fun onStyleChange(style: String) {
        _uiState.update { it.copy(style = style) }
    }

    fun onAspectRatioChange(ratio: String) {
        _uiState.update { it.copy(aspectRatio = ratio) }
    }

    fun generate() {
        val prompt = _uiState.value.prompt
        if (prompt.isBlank()) {
            _uiState.update { it.copy(statusMessage = "Describe what you want to create first.") }
            return
        }

        _uiState.update {
            it.copy(
                isGenerating = true,
                statusMessage = null,
                generatedImage = null
            )
        }

        // Local image generation will be plugged in here (on-device model).
        // Until a local diffusion/GGUF image model is bundled, we report status honestly.
        _uiState.update {
            it.copy(
                isGenerating = false,
                statusMessage = "Local image model not installed. Import a model in Settings → AI Model to generate images offline."
            )
        }
    }
}
