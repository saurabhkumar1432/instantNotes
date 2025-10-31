package com.voicenotesai.domain.ai

import com.voicenotesai.data.model.AIConfiguration
import com.voicenotesai.data.model.AIModel
import com.voicenotesai.data.model.AIProviderType
import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing AI configurations
 * Handles saving, loading, and managing AI provider configurations
 */
interface AIConfigurationManager {
    /**
     * Saves an AI configuration
     */
    suspend fun saveConfiguration(config: AIConfiguration): Result<Unit>
    
    /**
     * Gets the current active AI configuration
     */
    suspend fun getCurrentConfiguration(): AIConfiguration?
    
    /**
     * Gets all saved configurations
     */
    suspend fun getAllConfigurations(): List<AIConfiguration>
    
    /**
     * Gets configurations for a specific provider
     */
    suspend fun getConfigurationsForProvider(provider: AIProviderType): List<AIConfiguration>
    
    /**
     * Deletes a configuration
     */
    suspend fun deleteConfiguration(config: AIConfiguration): Result<Unit>
    
    /**
     * Sets the active configuration
     */
    suspend fun setActiveConfiguration(config: AIConfiguration): Result<Unit>
    
    /**
     * Observes changes to the current configuration
     */
    fun observeCurrentConfiguration(): Flow<AIConfiguration?>
    
    /**
     * Validates a configuration using the validator
     */
    suspend fun validateConfiguration(config: AIConfiguration): ValidationResult
    
    /**
     * Tests connection for a configuration
     */
    suspend fun testConnection(config: AIConfiguration): ConnectionResult
    
    /**
     * Gets available models for a provider configuration
     */
    suspend fun getAvailableModels(provider: AIProviderType, baseUrl: String? = null): List<AIModel>
    
    /**
     * Discovers models for local providers (Ollama, LM Studio)
     */
    suspend fun discoverLocalModels(provider: AIProviderType, baseUrl: String): List<AIModel>
    
    /**
     * Imports configuration from a JSON string or file
     */
    suspend fun importConfiguration(configData: String): Result<AIConfiguration>
    
    /**
     * Exports configuration to JSON string
     */
    suspend fun exportConfiguration(config: AIConfiguration): Result<String>
    
    /**
     * Resets all configurations to defaults
     */
    suspend fun resetToDefaults(): Result<Unit>
    
    /**
     * Gets default configuration for a provider
     */
    fun getDefaultConfiguration(provider: AIProviderType): AIConfiguration
}