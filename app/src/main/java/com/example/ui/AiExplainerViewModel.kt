package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GeminiRepository
import com.example.data.InteractionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val prompt: String = "Explain how AI works in a few words",
    val selectedModel: String = "gemini-3.8-flash",
    val availableModels: List<String> = listOf("gemini-2.5-flash", "gemini-3.8-flash", "gemini-1.5-flash"),
    val isLoading: Boolean = false,
    val currentResult: InteractionResult? = null,
    val history: List<InteractionResult> = emptyList(),
    val showCodeSnippet: Boolean = false,
    val userNotice: String? = null,
    val showProfileScreen: Boolean = false,
    val fullName: String = "Jahed",
    val callName: String = "Jahed",
    val customPreferences: String = "Ask clarifying questions before giving detailed answers"
)

class AiExplainerViewModel(
    private val repository: GeminiRepository = GeminiRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // Automatically execute initial interaction with default prompt on launch
        executeInteraction()
    }

    fun onPromptChange(newPrompt: String) {
        _uiState.update { it.copy(prompt = newPrompt) }
    }

    fun onSelectModel(model: String) {
        _uiState.update { it.copy(selectedModel = model) }
    }

    fun onSelectPreset(preset: String) {
        _uiState.update { it.copy(prompt = preset) }
        executeInteraction(preset)
    }

    fun toggleCodeSnippet() {
        _uiState.update { it.copy(showCodeSnippet = !it.showCodeSnippet) }
    }

    fun openProfile() {
        _uiState.update { it.copy(showProfileScreen = true) }
    }

    fun closeProfile() {
        _uiState.update { it.copy(showProfileScreen = false) }
    }

    fun updateProfile(fullName: String, callName: String) {
        _uiState.update { it.copy(fullName = fullName, callName = callName) }
    }

    fun updatePreferences(newPreferences: String) {
        _uiState.update { it.copy(customPreferences = newPreferences) }
    }

    fun resetAccount() {
        _uiState.update {
            it.copy(
                fullName = "Jahed",
                callName = "Jahed",
                customPreferences = "Ask clarifying questions before giving detailed answers",
                history = emptyList()
            )
        }
    }

    fun clearNotice() {
        _uiState.update { it.copy(userNotice = null) }
    }

    fun executeInteraction(promptToRun: String? = null) {
        val targetPrompt = (promptToRun ?: _uiState.value.prompt).trim()
        if (targetPrompt.isEmpty()) return

        val targetModel = _uiState.value.selectedModel
        val fullName = _uiState.value.fullName
        val callName = _uiState.value.callName
        val preferences = _uiState.value.customPreferences

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    prompt = targetPrompt,
                    userNotice = null
                )
            }

            val result = repository.runInteraction(
                prompt = targetPrompt,
                preferredModel = targetModel,
                userFullName = fullName,
                userCallName = callName,
                customInstructions = preferences
            )

            _uiState.update { current ->
                val updatedHistory = if (result.isSuccess) {
                    listOf(result) + current.history.filterNot { it.prompt == result.prompt && it.outputText == result.outputText }
                } else {
                    current.history
                }
                current.copy(
                    isLoading = false,
                    currentResult = result,
                    history = updatedHistory.take(20),
                    userNotice = result.errorMessage
                )
            }
        }
    }
}
