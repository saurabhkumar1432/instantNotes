package com.voicenotesai.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicenotesai.data.model.AIProvider
import com.voicenotesai.data.model.AISettings
import com.voicenotesai.data.repository.AIRepository
import com.voicenotesai.data.repository.SettingsRepository
import com.voicenotesai.domain.model.AppError
import com.voicenotesai.domain.model.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ValidationStatus {
    NONE,
    VALIDATING,
    SUCCESS,
    FAILED
}

data class SettingsUiState(
    val provider: AIProvider = AIProvider.OPENAI,
    val apiKey: String = "",
    val model: String = "",
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val isSaved: Boolean = false,
    val validationStatus: ValidationStatus = ValidationStatus.NONE,
    val validationMessage: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val aiRepository: AIRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                settingsRepository.getSettings().collect { settings ->
                    if (settings != null) {
                        _uiState.update {
                            it.copy(
                                provider = settings.provider,
                                apiKey = settings.apiKey,
                                model = settings.model,
                                isLoading = false,
                                validationStatus = if (settings.isValidated) 
                                    ValidationStatus.SUCCESS 
                                else 
                                    ValidationStatus.NONE
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                val error = AppError.StorageError(e.message ?: "Failed to load settings")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error
                    )
                }
            }
        }
    }

    fun onProviderChanged(provider: AIProvider) {
        _uiState.update { 
            it.copy(
                provider = provider, 
                isSaved = false,
                validationStatus = ValidationStatus.NONE,
                validationMessage = ""
            ) 
        }
    }

    fun onApiKeyChanged(apiKey: String) {
        _uiState.update { 
            it.copy(
                apiKey = apiKey, 
                isSaved = false,
                validationStatus = ValidationStatus.NONE,
                validationMessage = ""
            ) 
        }
    }

    fun onModelChanged(model: String) {
        _uiState.update { 
            it.copy(
                model = model, 
                isSaved = false,
                validationStatus = ValidationStatus.NONE,
                validationMessage = ""
            ) 
        }
    }

    fun saveSettings() {
        val currentState = _uiState.value
        
        // Validation
        val validationError = validateSettings(currentState)
        if (validationError != null) {
            _uiState.update { it.copy(error = validationError) }
            return
        }
        
        // Ensure settings have been validated before saving
        if (currentState.validationStatus != ValidationStatus.SUCCESS) {
            _uiState.update { 
                it.copy(
                    error = AppError.InvalidSettings("Please validate your API key and model before saving")
                ) 
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val settings = AISettings(
                    provider = currentState.provider,
                    apiKey = currentState.apiKey,
                    model = currentState.model,
                    isValidated = true  // Mark as validated when saving
                )
                settingsRepository.saveSettings(settings)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSaved = true,
                        error = null
                    )
                }
            } catch (e: Exception) {
                val error = AppError.StorageError(e.message ?: "Failed to save settings")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error
                    )
                }
            }
        }
    }

    private fun validateSettings(state: SettingsUiState): AppError? {
        return when {
            state.apiKey.isBlank() -> AppError.InvalidSettings("API key")
            state.apiKey.length < 20 -> AppError.InvalidSettings("API key is too short")
            !isValidApiKeyFormat(state.provider, state.apiKey) -> 
                AppError.InvalidSettings("API key format is invalid for ${state.provider.name}")
            state.model.isBlank() -> AppError.InvalidSettings("Model name")
            state.model.length < 3 -> AppError.InvalidSettings("Model name is too short")
            else -> null
        }
    }
    
    /**
     * Validates API key format based on provider.
     */
    private fun isValidApiKeyFormat(provider: AIProvider, apiKey: String): Boolean {
        return when (provider) {
            AIProvider.OPENAI -> {
                // OpenAI keys start with "sk-" and are alphanumeric with some special chars
                apiKey.startsWith("sk-") && apiKey.length >= 40
            }
            AIProvider.ANTHROPIC -> {
                // Anthropic keys start with "sk-ant-" 
                apiKey.startsWith("sk-ant-") && apiKey.length >= 40
            }
            AIProvider.GOOGLE_AI -> {
                // Google AI keys are typically 39 characters, alphanumeric
                apiKey.length >= 30 && apiKey.all { it.isLetterOrDigit() || it == '-' || it == '_' }
            }
        }
    }
    
    fun validateApiKey() {
        val currentState = _uiState.value
        
        // Basic validation first
        val validationError = validateSettings(currentState)
        if (validationError != null) {
            _uiState.update { 
                it.copy(
                    error = validationError,
                    validationStatus = ValidationStatus.FAILED,
                    validationMessage = validationError.toUserMessage()
                ) 
            }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    validationStatus = ValidationStatus.VALIDATING,
                    validationMessage = "Testing API key and model...",
                    error = null
                ) 
            }
            
            try {
                val result = aiRepository.validateApiKeyAndModel(
                    provider = currentState.provider,
                    apiKey = currentState.apiKey,
                    model = currentState.model
                )
                
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            validationStatus = ValidationStatus.SUCCESS,
                            validationMessage = result.getOrNull() ?: "Validation successful"
                        )
                    }
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Validation failed"
                    _uiState.update {
                        it.copy(
                            validationStatus = ValidationStatus.FAILED,
                            validationMessage = errorMessage,
                            error = AppError.ApiError(errorMessage)
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        validationStatus = ValidationStatus.FAILED,
                        validationMessage = e.message ?: "Validation failed",
                        error = AppError.ApiError(e.message ?: "Unknown error")
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSavedState() {
        _uiState.update { it.copy(isSaved = false) }
    }
    
    fun clearValidation() {
        _uiState.update { 
            it.copy(
                validationStatus = ValidationStatus.NONE,
                validationMessage = ""
            ) 
        }
    }
}
