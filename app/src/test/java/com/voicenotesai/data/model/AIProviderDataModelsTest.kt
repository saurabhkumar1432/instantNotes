package com.voicenotesai.data.model

import org.junit.Test
import org.junit.Assert.*

class AIProviderDataModelsTest {

    @Test
    fun `AIProviderType should return correct display names`() {
        assertEquals("OpenAI", AIProviderType.OpenAI.getDisplayName())
        assertEquals("Anthropic", AIProviderType.Anthropic.getDisplayName())
        assertEquals("Google AI", AIProviderType.GoogleAI.getDisplayName())
        assertEquals("OpenRouter", AIProviderType.OpenRouter.getDisplayName())
        assertEquals("Ollama", AIProviderType.Ollama.getDisplayName())
        assertEquals("LM Studio", AIProviderType.LMStudio.getDisplayName())
        assertEquals("Custom Provider", AIProviderType.Custom("Custom Provider").getDisplayName())
    }

    @Test
    fun `AIProviderType should return correct API key requirements`() {
        assertTrue(AIProviderType.OpenAI.requiresApiKey())
        assertTrue(AIProviderType.Anthropic.requiresApiKey())
        assertTrue(AIProviderType.GoogleAI.requiresApiKey())
        assertTrue(AIProviderType.OpenRouter.requiresApiKey())
        assertFalse(AIProviderType.Ollama.requiresApiKey())
        assertFalse(AIProviderType.LMStudio.requiresApiKey())
        assertTrue(AIProviderType.Custom("Test").requiresApiKey())
    }

    @Test
    fun `AIProviderType should return correct default base URLs`() {
        assertEquals("https://api.openai.com/v1", AIProviderType.OpenAI.getDefaultBaseUrl())
        assertEquals("https://api.anthropic.com", AIProviderType.Anthropic.getDefaultBaseUrl())
        assertEquals("https://generativelanguage.googleapis.com/v1", AIProviderType.GoogleAI.getDefaultBaseUrl())
        assertEquals("https://openrouter.ai/api/v1", AIProviderType.OpenRouter.getDefaultBaseUrl())
        assertEquals("http://localhost:11434", AIProviderType.Ollama.getDefaultBaseUrl())
        assertEquals("http://localhost:1234", AIProviderType.LMStudio.getDefaultBaseUrl())
        assertNull(AIProviderType.Custom("Test").getDefaultBaseUrl())
    }

    @Test
    fun `AIConfiguration should create default configurations correctly`() {
        val openAIConfig = AIConfiguration.defaultFor(AIProviderType.OpenAI)
        assertEquals(AIProviderType.OpenAI, openAIConfig.provider)
        assertEquals("gpt-3.5-turbo", openAIConfig.modelName)
        assertEquals("https://api.openai.com/v1", openAIConfig.baseUrl)
        assertFalse(openAIConfig.isValidated)

        val ollamaConfig = AIConfiguration.defaultFor(AIProviderType.Ollama)
        assertEquals(AIProviderType.Ollama, ollamaConfig.provider)
        assertEquals("llama2", ollamaConfig.modelName)
        assertEquals("http://localhost:11434", ollamaConfig.baseUrl)
    }

    @Test
    fun `AIConfiguration should validate completeness correctly`() {
        val incompleteConfig = AIConfiguration(
            provider = AIProviderType.OpenAI,
            apiKey = null, // Missing required API key
            modelName = "gpt-3.5-turbo"
        )
        assertFalse(incompleteConfig.isComplete())

        val completeConfig = AIConfiguration(
            provider = AIProviderType.OpenAI,
            apiKey = "test-key",
            modelName = "gpt-3.5-turbo"
        )
        assertTrue(completeConfig.isComplete())

        val localConfig = AIConfiguration(
            provider = AIProviderType.Ollama,
            apiKey = null, // Not required for local providers
            modelName = "llama2"
        )
        assertTrue(localConfig.isComplete())
    }

    @Test
    fun `AIConfiguration should sanitize sensitive data`() {
        val config = AIConfiguration(
            provider = AIProviderType.OpenAI,
            apiKey = "secret-key",
            modelName = "gpt-3.5-turbo",
            customHeaders = mapOf("Authorization" -> "Bearer secret-token")
        )

        val sanitized = config.sanitized()
        assertEquals("***", sanitized.apiKey)
        assertEquals("***", sanitized.customHeaders["Authorization"])
        assertEquals("gpt-3.5-turbo", sanitized.modelName) // Non-sensitive data preserved
    }

    @Test
    fun `AIModel should support capability checking`() {
        val model = AIModel(
            id = "test-model",
            name = "Test Model",
            provider = AIProviderType.OpenAI,
            capabilities = setOf(AICapability.TEXT_GENERATION, AICapability.TASK_EXTRACTION)
        )

        assertTrue(model.supports(AICapability.TEXT_GENERATION))
        assertTrue(model.supports(AICapability.TASK_EXTRACTION))
        assertFalse(model.supports(AICapability.VISION))

        assertTrue(model.supportsAll(setOf(AICapability.TEXT_GENERATION)))
        assertTrue(model.supportsAll(setOf(AICapability.TEXT_GENERATION, AICapability.TASK_EXTRACTION)))
        assertFalse(model.supportsAll(setOf(AICapability.TEXT_GENERATION, AICapability.VISION)))
    }

    @Test
    fun `AIConfigurationMigration should convert between legacy and new formats`() {
        val legacySettings = AISettings(
            provider = AIProvider.OPENAI,
            apiKey = "test-key",
            model = "gpt-3.5-turbo",
            isValidated = true
        )

        val newConfig = AIConfigurationMigration.fromAISettings(legacySettings)
        assertEquals(AIProviderType.OpenAI, newConfig.provider)
        assertEquals("test-key", newConfig.apiKey)
        assertEquals("gpt-3.5-turbo", newConfig.modelName)
        assertTrue(newConfig.isValidated)

        val backToLegacy = AIConfigurationMigration.toAISettings(newConfig)
        assertEquals(AIProvider.OPENAI, backToLegacy.provider)
        assertEquals("test-key", backToLegacy.apiKey)
        assertEquals("gpt-3.5-turbo", backToLegacy.model)
        assertTrue(backToLegacy.isValidated)
    }

    @Test
    fun `AIProviderType should handle ID conversion correctly`() {
        assertEquals("openai", AIProviderType.OpenAI.getId())
        assertEquals("anthropic", AIProviderType.Anthropic.getId())
        assertEquals("google-ai", AIProviderType.GoogleAI.getId())
        assertEquals("custom-test-provider", AIProviderType.Custom("Test Provider").getId())

        assertEquals(AIProviderType.OpenAI, AIProviderType.fromId("openai"))
        assertEquals(AIProviderType.Anthropic, AIProviderType.fromId("anthropic"))
        assertEquals(AIProviderType.GoogleAI, AIProviderType.fromId("google-ai"))
        assertNull(AIProviderType.fromId("unknown"))

        val customProvider = AIProviderType.fromId("custom-test-provider")
        assertTrue(customProvider is AIProviderType.Custom)
        assertEquals("Test Provider", (customProvider as AIProviderType.Custom).name)
    }
}