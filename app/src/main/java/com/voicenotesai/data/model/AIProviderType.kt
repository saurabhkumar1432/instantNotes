package com.voicenotesai.data.model

import kotlinx.serialization.Serializable

/**
 * Sealed class representing different AI providers supported by the application.
 * Each provider has specific configuration requirements and capabilities.
 */
@Serializable
sealed class AIProviderType {
    /**
     * OpenAI provider supporting GPT models
     */
    @Serializable
    object OpenAI : AIProviderType()
    
    /**
     * Anthropic provider supporting Claude models
     */
    @Serializable
    object Anthropic : AIProviderType()
    
    /**
     * Google AI provider supporting Gemini models
     */
    @Serializable
    object GoogleAI : AIProviderType()
    
    /**
     * OpenRouter provider for accessing multiple models through a single API
     */
    @Serializable
    object OpenRouter : AIProviderType()
    
    /**
     * Ollama provider for local model hosting
     */
    @Serializable
    object Ollama : AIProviderType()
    
    /**
     * LM Studio provider for local model hosting
     */
    @Serializable
    object LMStudio : AIProviderType()
    
    /**
     * Custom provider for user-defined endpoints
     */
    @Serializable
    data class Custom(val name: String) : AIProviderType()
    
    /**
     * Returns the display name for the provider
     */
    fun getDisplayName(): String = when (this) {
        is OpenAI -> "OpenAI"
        is Anthropic -> "Anthropic"
        is GoogleAI -> "Google AI"
        is OpenRouter -> "OpenRouter"
        is Ollama -> "Ollama"
        is LMStudio -> "LM Studio"
        is Custom -> name
    }
    
    /**
     * Returns whether this provider requires an API key
     */
    fun requiresApiKey(): Boolean = when (this) {
        is OpenAI, is Anthropic, is GoogleAI, is OpenRouter -> true
        is Ollama, is LMStudio -> false
        is Custom -> true // Assume custom providers need API keys by default
    }
    
    /**
     * Returns the default base URL for the provider, if applicable
     */
    fun getDefaultBaseUrl(): String? = when (this) {
        is Ollama -> "http://localhost:11434"
        is LMStudio -> "http://localhost:1234"
        is OpenAI -> "https://api.openai.com/v1"
        is Anthropic -> "https://api.anthropic.com"
        is GoogleAI -> "https://generativelanguage.googleapis.com/v1beta"
        is OpenRouter -> "https://openrouter.ai/api/v1"
        is Custom -> null
    }
    
    /**
     * Returns a unique identifier for this provider type
     */
    fun getId(): String = when (this) {
        is OpenAI -> "openai"
        is Anthropic -> "anthropic"
        is GoogleAI -> "google-ai"
        is OpenRouter -> "openrouter"
        is Ollama -> "ollama"
        is LMStudio -> "lm-studio"
        is Custom -> "custom-${name.lowercase().replace(" ", "-")}"
    }
    
    companion object {
        /**
         * Creates a provider type from its ID
         */
        fun fromId(id: String): AIProviderType? = when (id) {
            "openai" -> OpenAI
            "anthropic" -> Anthropic
            "google-ai" -> GoogleAI
            "openrouter" -> OpenRouter
            "ollama" -> Ollama
            "lm-studio" -> LMStudio
            else -> if (id.startsWith("custom-")) {
                val name = id.removePrefix("custom-").replace("-", " ").replaceFirstChar { it.uppercase() }
                Custom(name)
            } else null
        }
        
        /**
         * Returns all built-in provider types
         */
        fun getAllBuiltInProviders(): List<AIProviderType> = listOf(
            OpenAI,
            Anthropic,
            GoogleAI,
            OpenRouter,
            Ollama,
            LMStudio
        )
    }
}