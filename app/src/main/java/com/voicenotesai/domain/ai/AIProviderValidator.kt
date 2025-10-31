package com.voicenotesai.domain.ai

import com.voicenotesai.data.model.AIConfiguration
import com.voicenotesai.data.model.AIModel
import com.voicenotesai.data.model.AIProviderType

/**
 * Interface for provider-specific validation logic
 * Each AI provider may have different requirements and validation rules
 */
interface AIProviderValidator {
    /**
     * The provider this validator handles
     */
    val provider: AIProviderType
    
    /**
     * Validates provider-specific configuration requirements
     */
    suspend fun validateProviderConfig(config: AIConfiguration): ValidationResult
    
    /**
     * Tests connection to the provider's API
     */
    suspend fun testProviderConnection(config: AIConfiguration): ConnectionResult
    
    /**
     * Gets available models from the provider
     */
    suspend fun getProviderModels(config: AIConfiguration): List<AIModel>
    
    /**
     * Validates that a specific model exists and is accessible
     */
    suspend fun validateProviderModel(config: AIConfiguration, modelId: String): ValidationResult
    
    /**
     * Gets provider-specific validation rules and requirements
     */
    fun getValidationRules(): ProviderValidationRules
}

/**
 * Data class containing validation rules for a specific provider
 */
data class ProviderValidationRules(
    /**
     * Whether API key is required
     */
    val requiresApiKey: Boolean,
    
    /**
     * Whether base URL is required
     */
    val requiresBaseUrl: Boolean,
    
    /**
     * Whether model name is required
     */
    val requiresModelName: Boolean,
    
    /**
     * Pattern for validating API key format (if applicable)
     */
    val apiKeyPattern: Regex? = null,
    
    /**
     * Pattern for validating base URL format (if applicable)
     */
    val baseUrlPattern: Regex? = null,
    
    /**
     * List of supported model prefixes or patterns
     */
    val supportedModelPatterns: List<Regex> = emptyList(),
    
    /**
     * Default models for this provider
     */
    val defaultModels: List<String> = emptyList(),
    
    /**
     * Additional validation requirements
     */
    val customValidations: Map<String, String> = emptyMap()
)

/**
 * Factory for creating provider-specific validators
 */
interface AIProviderValidatorFactory {
    /**
     * Creates a validator for the specified provider
     */
    fun createValidator(provider: AIProviderType): AIProviderValidator
    
    /**
     * Gets all supported providers
     */
    fun getSupportedProviders(): List<AIProviderType>
    
    /**
     * Checks if a provider is supported
     */
    fun isProviderSupported(provider: AIProviderType): Boolean
}