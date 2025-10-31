package com.voicenotesai.data.model

import kotlinx.serialization.Serializable

/**
 * Data class representing the configuration for an AI provider.
 * Contains all necessary information to connect to and use an AI service.
 */
@Serializable
data class AIConfiguration(
    /**
     * The AI provider type
     */
    val provider: AIProviderType,
    
    /**
     * API key for authentication (optional for local providers)
     */
    val apiKey: String? = null,
    
    /**
     * Base URL for the API endpoint (optional, uses provider default if not specified)
     */
    val baseUrl: String? = null,
    
    /**
     * Name of the model to use
     */
    val modelName: String,
    
    /**
     * Custom headers to include in API requests
     */
    val customHeaders: Map<String, String> = emptyMap(),
    
    /**
     * Whether this configuration has been validated
     */
    val isValidated: Boolean = false,
    
    /**
     * Timestamp of last validation attempt
     */
    val lastValidated: Long? = null,
    
    /**
     * Error message from last validation attempt, if any
     */
    val validationError: String? = null
) {
    /**
     * Returns the effective base URL, using provider default if not specified
     */
    fun getEffectiveBaseUrl(): String? = baseUrl ?: provider.getDefaultBaseUrl()
    
    /**
     * Returns whether this configuration is complete and ready for validation
     */
    fun isComplete(): Boolean {
        return when {
            provider.requiresApiKey() && apiKey.isNullOrBlank() -> false
            modelName.isBlank() -> false
            else -> true
        }
    }
    
    /**
     * Returns a copy of this configuration with validation status updated
     */
    fun withValidationResult(isValid: Boolean, error: String? = null): AIConfiguration {
        return copy(
            isValidated = isValid,
            lastValidated = System.currentTimeMillis(),
            validationError = error
        )
    }
    
    /**
     * Returns a sanitized version of this configuration for logging (without sensitive data)
     */
    fun sanitized(): AIConfiguration {
        return copy(
            apiKey = if (apiKey != null) "***" else null,
            customHeaders = customHeaders.mapValues { "***" }
        )
    }
    
    companion object {
        /**
         * Creates a default configuration for the specified provider
         */
        fun defaultFor(provider: AIProviderType): AIConfiguration {
            val defaultModel = when (provider) {
                is AIProviderType.OpenAI -> "gpt-3.5-turbo"
                is AIProviderType.Anthropic -> "claude-3-haiku-20240307"
                is AIProviderType.GoogleAI -> "gemini-pro"
                is AIProviderType.OpenRouter -> "openai/gpt-3.5-turbo"
                is AIProviderType.Ollama -> "llama2"
                is AIProviderType.LMStudio -> "local-model"
                is AIProviderType.Custom -> "custom-model"
            }
            
            return AIConfiguration(
                provider = provider,
                modelName = defaultModel,
                baseUrl = provider.getDefaultBaseUrl()
            )
        }
    }
}