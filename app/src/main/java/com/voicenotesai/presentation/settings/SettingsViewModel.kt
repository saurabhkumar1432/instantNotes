package com.voicenotesai.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicenotesai.data.model.AIConfiguration
import com.voicenotesai.data.model.AIModel
import com.voicenotesai.data.model.AIProviderType
import com.voicenotesai.domain.ai.AIConfigurationManager
import com.voicenotesai.domain.ai.ValidationResult
import com.voicenotesai.domain.ai.ConnectionResult
import com.voicenotesai.domain.model.AppError
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
    val provider: AIProviderType = AIProviderType.OpenAI,
    val apiKey: String = "",
    val baseUrl: String = "",
    val modelName: String = "",
    val customHeaders: Map<String, String> = emptyMap(),

    val isLoading: Boolean = false,
    val isDiscoveringModels: Boolean = false,
    val error: AppError? = null,
    val isSaved: Boolean = false,
    val validationStatus: ValidationStatus = ValidationStatus.NONE,
    val validationMessage: String = ""
) {
    fun isConfigurationComplete(): Boolean {
        return when {
            provider.requiresApiKey() && apiKey.isBlank() -> false
            modelName.isBlank() -> false
            (provider is AIProviderType.Ollama || provider is AIProviderType.LMStudio || provider is AIProviderType.Custom) && baseUrl.isBlank() -> false
            else -> true
        }
    }
    
    fun toAIConfiguration(): AIConfiguration {
        return AIConfiguration(
            provider = provider,
            apiKey = apiKey.takeIf { it.isNotBlank() },
            baseUrl = baseUrl.takeIf { it.isNotBlank() },
            modelName = modelName,
            customHeaders = customHeaders,
            isValidated = validationStatus == ValidationStatus.SUCCESS
        )
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val aiConfigurationManager: AIConfigurationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadCurrentConfiguration()
    }

    private fun loadCurrentConfiguration() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val currentConfig = aiConfigurationManager.getCurrentConfiguration()
                if (currentConfig != null) {
                    _uiState.update {
                        it.copy(
                            provider = currentConfig.provider,
                            apiKey = currentConfig.apiKey ?: "",
                            baseUrl = currentConfig.baseUrl ?: currentConfig.provider.getDefaultBaseUrl() ?: "",
                            modelName = currentConfig.modelName,
                            customHeaders = currentConfig.customHeaders,
                            isLoading = false,
                            validationStatus = if (currentConfig.isValidated) 
                                ValidationStatus.SUCCESS 
                            else 
                                ValidationStatus.NONE,
                            validationMessage = if (currentConfig.isValidated) "Configuration validated" else ""
                        )
                    }

                } else {
                    // Set default configuration
                    val defaultProvider = AIProviderType.OpenAI
                    _uiState.update { 
                        it.copy(
                            provider = defaultProvider,
                            baseUrl = defaultProvider.getDefaultBaseUrl() ?: "",
                            isLoading = false
                        ) 
                    }

                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = AppError.StorageError(e.message ?: "Failed to load configuration")
                    )
                }
            }
        }
    }

    fun onProviderChanged(provider: AIProviderType) {
        _uiState.update { 
            it.copy(
                provider = provider,
                baseUrl = provider.getDefaultBaseUrl() ?: "",
                apiKey = if (provider.requiresApiKey()) it.apiKey else "",
                modelName = "",
                customHeaders = emptyMap(),

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

    fun onBaseUrlChanged(baseUrl: String) {
        _uiState.update { 
            it.copy(
                baseUrl = baseUrl, 
                isSaved = false,
                validationStatus = ValidationStatus.NONE,
                validationMessage = ""
            ) 
        }
    }

    fun onModelChanged(modelName: String) {
        _uiState.update { 
            it.copy(
                modelName = modelName, 
                isSaved = false,
                validationStatus = ValidationStatus.NONE,
                validationMessage = ""
            ) 
        }
    }

    fun onCustomHeadersChanged(headers: Map<String, String>) {
        _uiState.update { 
            it.copy(
                customHeaders = headers, 
                isSaved = false,
                validationStatus = ValidationStatus.NONE,
                validationMessage = ""
            ) 
        }
    }

    fun saveConfiguration() {
        val currentState = _uiState.value
        
        // Ensure configuration is complete and validated
        if (!currentState.isConfigurationComplete()) {
            _uiState.update { 
                it.copy(error = AppError.InvalidSettings("Please complete all required fields")) 
            }
            return
        }
        
        if (currentState.validationStatus != ValidationStatus.SUCCESS) {
            _uiState.update { 
                it.copy(error = AppError.InvalidSettings("Please validate your configuration before saving")) 
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val configuration = currentState.toAIConfiguration()
                val result = aiConfigurationManager.saveConfiguration(configuration)
                
                if (result.isSuccess) {
                    // Set as active configuration
                    aiConfigurationManager.setActiveConfiguration(configuration)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSaved = true,
                            error = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = AppError.StorageError(result.exceptionOrNull()?.message ?: "Failed to save configuration")
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = AppError.StorageError(e.message ?: "Failed to save configuration")
                    )
                }
            }
        }
    }

    fun validateConfiguration() {
        val currentState = _uiState.value
        
        if (!currentState.isConfigurationComplete()) {
            _uiState.update { 
                it.copy(
                    validationStatus = ValidationStatus.FAILED,
                    validationMessage = "Please complete all required fields",
                    error = AppError.InvalidSettings("Configuration incomplete")
                ) 
            }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    validationStatus = ValidationStatus.VALIDATING,
                    validationMessage = "Testing configuration...",
                    error = null
                ) 
            }
            
            try {
                val configuration = currentState.toAIConfiguration()
                val validationResult = aiConfigurationManager.validateConfiguration(configuration)
                
                if (validationResult.isValid) {
                    // Also test connection
                    val connectionResult = aiConfigurationManager.testConnection(configuration)
                    
                    if (connectionResult.isConnected) {
                        _uiState.update {
                            it.copy(
                                validationStatus = ValidationStatus.SUCCESS,
                                validationMessage = "Configuration validated successfully"
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                validationStatus = ValidationStatus.FAILED,
                                validationMessage = connectionResult.errorMessage ?: "Connection test failed",
                                error = AppError.NetworkError(connectionResult.errorMessage ?: "Connection failed")
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            validationStatus = ValidationStatus.FAILED,
                            validationMessage = validationResult.errorMessage ?: "Validation failed",
                            error = AppError.InvalidSettings(validationResult.errorMessage ?: "Invalid configuration")
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        validationStatus = ValidationStatus.FAILED,
                        validationMessage = e.message ?: "Validation failed",
                        error = AppError.NetworkError(e.message ?: "Unknown error")
                    )
                }
            }
        }
    }

    fun discoverModels() {
        val currentState = _uiState.value
        
        if (currentState.baseUrl.isBlank()) {
            _uiState.update { 
                it.copy(error = AppError.InvalidSettings("Please enter a base URL first")) 
            }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isDiscoveringModels = true, error = null) }
            try {
                aiConfigurationManager.discoverLocalModels(
                    provider = currentState.provider,
                    baseUrl = currentState.baseUrl
                )
                _uiState.update {
                    it.copy(
                        isDiscoveringModels = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isDiscoveringModels = false,
                        error = AppError.NetworkError(e.message ?: "Failed to discover models")
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
