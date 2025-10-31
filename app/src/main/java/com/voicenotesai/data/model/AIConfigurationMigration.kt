package com.voicenotesai.data.model

/**
 * Utility class for migrating between AISettings and AIConfiguration
 * Provides backward compatibility during the transition period
 */
object AIConfigurationMigration {
    
    /**
     * Converts legacy AISettings to new AIConfiguration
     */
    fun fromAISettings(settings: AISettings): AIConfiguration {
        // Convert legacy enum to sealed class
        val provider = when (settings.provider) {
            AIProvider.OPENAI -> AIProviderType.OpenAI
            AIProvider.ANTHROPIC -> AIProviderType.Anthropic
            AIProvider.GOOGLE_AI -> AIProviderType.GoogleAI
        }
        
        return AIConfiguration(
            provider = provider,
            apiKey = settings.apiKey,
            modelName = settings.model,
            isValidated = settings.isValidated,
            // Legacy settings don't have these fields, so use defaults
            baseUrl = provider.getDefaultBaseUrl(),
            customHeaders = emptyMap(),
            lastValidated = if (settings.isValidated) System.currentTimeMillis() else null
        )
    }
    
    /**
     * Converts new AIConfiguration to legacy AISettings for backward compatibility
     */
    fun toAISettings(config: AIConfiguration): AISettings {
        // Convert sealed class back to enum
        val legacyProvider = when (config.provider) {
            is AIProviderType.OpenAI -> AIProvider.OPENAI
            is AIProviderType.Anthropic -> AIProvider.ANTHROPIC
            is AIProviderType.GoogleAI -> AIProvider.GOOGLE_AI
            // For new providers that don't exist in legacy enum, default to OpenAI
            else -> AIProvider.OPENAI
        }
        
        return AISettings(
            provider = legacyProvider,
            apiKey = config.apiKey ?: "",
            model = config.modelName,
            isValidated = config.isValidated,
            promptTemplate = AISettings.DEFAULT_PROMPT_TEMPLATE
        )
    }
    
    /**
     * Checks if an AIConfiguration can be converted to AISettings without data loss
     */
    fun isCompatibleWithLegacy(config: AIConfiguration): Boolean {
        return when (config.provider) {
            is AIProviderType.OpenAI, is AIProviderType.Anthropic, is AIProviderType.GoogleAI -> true
            else -> false
        }
    }
    
    /**
     * Gets the legacy provider name for display purposes
     */
    fun getLegacyProviderName(provider: AIProviderType): String? {
        return when (provider) {
            is AIProviderType.OpenAI -> "OPENAI"
            is AIProviderType.Anthropic -> "ANTHROPIC"
            is AIProviderType.GoogleAI -> "GOOGLE_AI"
            else -> null
        }
    }
}