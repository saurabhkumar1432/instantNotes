package com.voicenotesai.integration

import com.voicenotesai.data.ai.AIConfigurationManagerImpl
import com.voicenotesai.data.ai.AIConfigurationValidatorImpl
import com.voicenotesai.data.model.AIConfiguration
import com.voicenotesai.data.model.AIProvider
import com.voicenotesai.data.repository.SettingsRepositoryImpl
import com.voicenotesai.domain.ai.AIConfigurationValidator
import com.voicenotesai.domain.model.AppError
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Integration test for AI provider switching and configuration persistence.
 * Tests the complete flow of:
 * 1. Configuring different AI providers
 * 2. Validating configurations
 * 3. Switching between providers
 * 4. Persisting configuration changes
 * 5. Handling provider failures and fallbacks
 */
class AIProviderSwitchingIntegrationTest {

    private lateinit var aiConfigurationManager: AIConfigurationManagerImpl
    private lateinit var aiConfigurationValidator: AIConfigurationValidatorImpl
    private lateinit var settingsRepository: SettingsRepositoryImpl

    @Before
    fun setup() {
        settingsRepository = mockk(relaxed = true)
        aiConfigurationValidator = mockk(relaxed = true)
        aiConfigurationManager = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `switching from OpenAI to Anthropic persists configuration`() = runTest {
        // Given: Current OpenAI configuration
        val openAIConfig = AIConfiguration(
            provider = AIProvider.OpenAI,
            apiKey = "openai-key",
            modelName = "gpt-3.5-turbo",
            isValidated = true,
            lastValidated = System.currentTimeMillis()
        )

        // Given: New Anthropic configuration
        val anthropicConfig = AIConfiguration(
            provider = AIProvider.Anthropic,
            apiKey = "anthropic-key", 
            modelName = "claude-3-sonnet",
            isValidated = false
        )

        val validatedAnthropicConfig = anthropicConfig.copy(
            isValidated = true,
            lastValidated = System.currentTimeMillis()
        )

        // Mock current configuration
        coEvery { aiConfigurationManager.getCurrentConfiguration() } returns openAIConfig

        // Mock validation success
        coEvery { aiConfigurationValidator.validateConfiguration(anthropicConfig) } returns 
            AIConfigurationValidator.ValidationResult(
                isValid = true,
                errorMessage = null,
                supportedCapabilities = setOf(
                    AIConfigurationValidator.AICapability.TEXT_GENERATION,
                    AIConfigurationValidator.AICapability.TASK_EXTRACTION
                )
            )

        // Mock configuration save
        coEvery { aiConfigurationManager.saveConfiguration(any()) } returns Result.success(Unit)
        coEvery { settingsRepository.saveAIConfiguration(any()) } returns Unit

        // When: Switching to Anthropic
        val validationResult = aiConfigurationValidator.validateConfiguration(anthropicConfig)
        assertTrue("Anthropic configuration should be valid", validationResult.isValid)

        val saveResult = aiConfigurationManager.saveConfiguration(validatedAnthropicConfig)
        assertTrue("Configuration save should succeed", saveResult.isSuccess)

        // Then: Verify configuration was persisted
        verify { settingsRepository.saveAIConfiguration(validatedAnthropicConfig) }
        verify { aiConfigurationManager.saveConfiguration(validatedAnthropicConfig) }
    }

    @Test
    fun `switching to local AI provider (Ollama) with custom endpoint`() = runTest {
        // Given: Ollama configuration with custom endpoint
        val ollamaConfig = AIConfiguration(
            provider = AIProvider.Ollama,
            baseUrl = "http://192.168.1.100:11434",
            modelName = "llama2:7b",
            isValidated = false
        )

        val validatedOllamaConfig = ollamaConfig.copy(
            isValidated = true,
            lastValidated = System.currentTimeMillis()
        )

        // Mock validation with connection test
        coEvery { aiConfigurationValidator.validateConfiguration(ollamaConfig) } returns
            AIConfigurationValidator.ValidationResult(
                isValid = true,
                errorMessage = null,
                supportedCapabilities = setOf(
                    AIConfigurationValidator.AICapability.TEXT_GENERATION,
                    AIConfigurationValidator.AICapability.TASK_EXTRACTION
                )
            )

        // Mock model discovery for Ollama
        coEvery { aiConfigurationManager.getAvailableModels(AIProvider.Ollama) } returns listOf(
            AIConfigurationValidator.AIModel(
                id = "llama2:7b",
                name = "Llama 2 7B",
                provider = AIProvider.Ollama,
                capabilities = setOf(AIConfigurationValidator.AICapability.TEXT_GENERATION)
            )
        )

        coEvery { aiConfigurationManager.saveConfiguration(any()) } returns Result.success(Unit)

        // When: Configuring Ollama
        val models = aiConfigurationManager.getAvailableModels(AIProvider.Ollama)
        assertEquals("Should discover Ollama models", 1, models.size)
        assertEquals("llama2:7b", models[0].id)

        val validationResult = aiConfigurationValidator.validateConfiguration(ollamaConfig)
        assertTrue("Ollama configuration should be valid", validationResult.isValid)

        val saveResult = aiConfigurationManager.saveConfiguration(validatedOllamaConfig)
        assertTrue("Ollama configuration save should succeed", saveResult.isSuccess)

        // Then: Verify local AI configuration
        verify { aiConfigurationManager.getAvailableModels(AIProvider.Ollama) }
        verify { aiConfigurationValidator.validateConfiguration(ollamaConfig) }
        verify { aiConfigurationManager.saveConfiguration(validatedOllamaConfig) }
    }

    @Test
    fun `configuration validation fails for invalid API key`() = runTest {
        // Given: Invalid OpenAI configuration
        val invalidConfig = AIConfiguration(
            provider = AIProvider.OpenAI,
            apiKey = "invalid-key",
            modelName = "gpt-3.5-turbo",
            isValidated = false
        )

        // Mock validation failure
        coEvery { aiConfigurationValidator.validateConfiguration(invalidConfig) } returns
            AIConfigurationValidator.ValidationResult(
                isValid = false,
                errorMessage = "Invalid API key: Authentication failed",
                supportedCapabilities = emptySet()
            )

        // When: Validating invalid configuration
        val validationResult = aiConfigurationValidator.validateConfiguration(invalidConfig)

        // Then: Should fail validation
        assertFalse("Invalid configuration should fail validation", validationResult.isValid)
        assertEquals("Should return specific error message", "Invalid API key: Authentication failed", validationResult.errorMessage)
        assertTrue("Should have no supported capabilities", validationResult.supportedCapabilities.isEmpty())

        // Verify configuration was not saved
        verify(exactly = 0) { aiConfigurationManager.saveConfiguration(any()) }
    }

    @Test
    fun `switching providers maintains previous configuration as fallback`() = runTest {
        // Given: Working OpenAI configuration
        val workingOpenAIConfig = AIConfiguration(
            provider = AIProvider.OpenAI,
            apiKey = "working-openai-key",
            modelName = "gpt-3.5-turbo",
            isValidated = true,
            lastValidated = System.currentTimeMillis()
        )

        // Given: Failing Google AI configuration
        val failingGoogleConfig = AIConfiguration(
            provider = AIProvider.GoogleAI,
            apiKey = "invalid-google-key",
            modelName = "gemini-pro",
            isValidated = false
        )

        // Mock current working configuration
        coEvery { aiConfigurationManager.getCurrentConfiguration() } returns workingOpenAIConfig

        // Mock Google AI validation failure
        coEvery { aiConfigurationValidator.validateConfiguration(failingGoogleConfig) } returns
            AIConfigurationValidator.ValidationResult(
                isValid = false,
                errorMessage = "Google AI API key is invalid",
                supportedCapabilities = emptySet()
            )

        // Mock fallback to previous configuration
        coEvery { aiConfigurationManager.getCurrentConfiguration() } returns workingOpenAIConfig

        // When: Attempting to switch to failing provider
        val validationResult = aiConfigurationValidator.validateConfiguration(failingGoogleConfig)
        assertFalse("Google AI validation should fail", validationResult.isValid)

        // When: Falling back to previous configuration
        val fallbackConfig = aiConfigurationManager.getCurrentConfiguration()

        // Then: Should maintain working configuration
        assertEquals("Should fallback to OpenAI", AIProvider.OpenAI, fallbackConfig?.provider)
        assertTrue("Fallback configuration should be validated", fallbackConfig?.isValidated == true)

        // Verify failed configuration was not saved
        verify(exactly = 0) { aiConfigurationManager.saveConfiguration(failingGoogleConfig) }
    }

    @Test
    fun `custom provider configuration with headers`() = runTest {
        // Given: Custom provider with headers
        val customConfig = AIConfiguration(
            provider = AIProvider.Custom("Custom LLM"),
            baseUrl = "https://api.custom-llm.com/v1",
            apiKey = "custom-api-key",
            modelName = "custom-model-v1",
            customHeaders = mapOf(
                "X-Custom-Header" to "custom-value",
                "Authorization" to "Bearer custom-token"
            ),
            isValidated = false
        )

        val validatedCustomConfig = customConfig.copy(
            isValidated = true,
            lastValidated = System.currentTimeMillis()
        )

        // Mock validation with custom headers
        coEvery { aiConfigurationValidator.validateConfiguration(customConfig) } returns
            AIConfigurationValidator.ValidationResult(
                isValid = true,
                errorMessage = null,
                supportedCapabilities = setOf(AIConfigurationValidator.AICapability.TEXT_GENERATION)
            )

        coEvery { aiConfigurationManager.saveConfiguration(any()) } returns Result.success(Unit)

        // When: Configuring custom provider
        val validationResult = aiConfigurationValidator.validateConfiguration(customConfig)
        assertTrue("Custom provider should validate successfully", validationResult.isValid)

        val saveResult = aiConfigurationManager.saveConfiguration(validatedCustomConfig)
        assertTrue("Custom configuration save should succeed", saveResult.isSuccess)

        // Then: Verify custom configuration with headers
        verify { aiConfigurationValidator.validateConfiguration(customConfig) }
        verify { aiConfigurationManager.saveConfiguration(validatedCustomConfig) }
        
        // Verify custom headers are preserved
        assertEquals("Should preserve custom headers", 2, customConfig.customHeaders.size)
        assertEquals("custom-value", customConfig.customHeaders["X-Custom-Header"])
    }

    @Test
    fun `configuration persistence survives app restart`() = runTest {
        // Given: Saved configuration
        val savedConfig = AIConfiguration(
            provider = AIProvider.Anthropic,
            apiKey = "anthropic-key",
            modelName = "claude-3-haiku",
            isValidated = true,
            lastValidated = System.currentTimeMillis() - 3600000 // 1 hour ago
        )

        // Mock configuration retrieval after restart
        coEvery { settingsRepository.getAIConfiguration() } returns flowOf(savedConfig)
        coEvery { aiConfigurationManager.getCurrentConfiguration() } returns savedConfig

        // When: Retrieving configuration after restart
        val retrievedConfig = aiConfigurationManager.getCurrentConfiguration()

        // Then: Should retrieve persisted configuration
        assertNotNull("Should retrieve saved configuration", retrievedConfig)
        assertEquals("Should preserve provider", AIProvider.Anthropic, retrievedConfig?.provider)
        assertEquals("Should preserve model", "claude-3-haiku", retrievedConfig?.modelName)
        assertTrue("Should preserve validation status", retrievedConfig?.isValidated == true)

        verify { settingsRepository.getAIConfiguration() }
    }

    @Test
    fun `multiple rapid provider switches handle correctly`() = runTest {
        // Given: Multiple provider configurations
        val configs = listOf(
            AIConfiguration(provider = AIProvider.OpenAI, apiKey = "openai-key", modelName = "gpt-3.5-turbo"),
            AIConfiguration(provider = AIProvider.Anthropic, apiKey = "anthropic-key", modelName = "claude-3-sonnet"),
            AIConfiguration(provider = AIProvider.GoogleAI, apiKey = "google-key", modelName = "gemini-pro")
        )

        // Mock all validations as successful
        configs.forEach { config ->
            coEvery { aiConfigurationValidator.validateConfiguration(config) } returns
                AIConfigurationValidator.ValidationResult(isValid = true, errorMessage = null, supportedCapabilities = emptySet())
            coEvery { aiConfigurationManager.saveConfiguration(any()) } returns Result.success(Unit)
        }

        // When: Rapidly switching between providers
        configs.forEach { config ->
            val validationResult = aiConfigurationValidator.validateConfiguration(config)
            assertTrue("All configurations should validate", validationResult.isValid)
            
            val saveResult = aiConfigurationManager.saveConfiguration(config.copy(isValidated = true))
            assertTrue("All configurations should save successfully", saveResult.isSuccess)
        }

        // Then: All switches should complete successfully
        verify(exactly = 3) { aiConfigurationValidator.validateConfiguration(any()) }
        verify(exactly = 3) { aiConfigurationManager.saveConfiguration(any()) }
    }

    @Test
    fun `provider switching with network connectivity issues`() = runTest {
        // Given: Network connectivity issues
        val networkConfig = AIConfiguration(
            provider = AIProvider.OpenRouter,
            apiKey = "openrouter-key",
            modelName = "openai/gpt-3.5-turbo",
            isValidated = false
        )

        // Mock network failure during validation
        coEvery { aiConfigurationValidator.validateConfiguration(networkConfig) } returns
            AIConfigurationValidator.ValidationResult(
                isValid = false,
                errorMessage = "Network error: Unable to connect to OpenRouter API",
                supportedCapabilities = emptySet()
            )

        // When: Attempting to validate with network issues
        val validationResult = aiConfigurationValidator.validateConfiguration(networkConfig)

        // Then: Should handle network error gracefully
        assertFalse("Validation should fail due to network", validationResult.isValid)
        assertTrue("Should indicate network error", validationResult.errorMessage?.contains("Network error") == true)

        // Verify configuration was not saved due to validation failure
        verify(exactly = 0) { aiConfigurationManager.saveConfiguration(any()) }
    }
}