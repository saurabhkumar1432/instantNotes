package com.voicenotesai.domain.ai

import com.voicenotesai.data.model.AICapability
import com.voicenotesai.data.model.AIConfiguration
import com.voicenotesai.data.model.AIModel

/**
 * Result of validating an AI configuration
 */
data class ValidationResult(
    /**
     * Whether the configuration is valid
     */
    val isValid: Boolean,
    
    /**
     * Error message if validation failed
     */
    val errorMessage: String? = null,
    
    /**
     * Set of capabilities supported by this configuration
     */
    val supportedCapabilities: Set<AICapability> = emptySet(),
    
    /**
     * Additional details about the validation
     */
    val details: Map<String, Any> = emptyMap()
) {
    companion object {
        fun success(capabilities: Set<AICapability>, details: Map<String, Any> = emptyMap()) = 
            ValidationResult(
                isValid = true,
                supportedCapabilities = capabilities,
                details = details
            )
        
        fun failure(errorMessage: String, details: Map<String, Any> = emptyMap()) = 
            ValidationResult(
                isValid = false,
                errorMessage = errorMessage,
                details = details
            )
    }
}

/**
 * Result of testing connection to an AI provider
 */
data class ConnectionResult(
    /**
     * Whether the connection was successful
     */
    val isConnected: Boolean,
    
    /**
     * Response time in milliseconds
     */
    val responseTimeMs: Long? = null,
    
    /**
     * Error message if connection failed
     */
    val errorMessage: String? = null,
    
    /**
     * Additional connection details
     */
    val details: Map<String, Any> = emptyMap()
) {
    companion object {
        fun success(responseTimeMs: Long, details: Map<String, Any> = emptyMap()) = 
            ConnectionResult(
                isConnected = true,
                responseTimeMs = responseTimeMs,
                details = details
            )
        
        fun failure(errorMessage: String, details: Map<String, Any> = emptyMap()) = 
            ConnectionResult(
                isConnected = false,
                errorMessage = errorMessage,
                details = details
            )
    }
}

/**
 * Interface for validating AI configurations and testing connections
 */
interface AIConfigurationValidator {
    /**
     * Validates an AI configuration without making network calls
     * Checks for required fields, format validation, etc.
     */
    suspend fun validateConfiguration(config: AIConfiguration): ValidationResult
    
    /**
     * Tests the connection to an AI provider with the given configuration
     * Makes actual network calls to verify connectivity and authentication
     */
    suspend fun testConnection(config: AIConfiguration): ConnectionResult
    
    /**
     * Gets available models for a given configuration
     * Returns empty list if models cannot be retrieved
     */
    suspend fun getAvailableModels(config: AIConfiguration): List<AIModel>
    
    /**
     * Validates that a specific model is available for the configuration
     */
    suspend fun validateModel(config: AIConfiguration, modelId: String): ValidationResult
    
    /**
     * Performs a comprehensive validation including connection test and model validation
     */
    suspend fun fullValidation(config: AIConfiguration): ValidationResult
}