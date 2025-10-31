package com.voicenotesai.presentation.settings

import com.voicenotesai.data.model.AICapability
import com.voicenotesai.data.model.AIConfiguration
import com.voicenotesai.data.model.AIModel
import com.voicenotesai.data.model.AIProviderType
import com.voicenotesai.domain.ai.AIConfigurationManager
import com.voicenotesai.domain.ai.ValidationResult
import com.voicenotesai.domain.ai.ConnectionResult
import com.voicenotesai.domain.model.AppError
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for SettingsViewModel, focusing on model selection functionality.
 * Tests cover:
 * - Loading available models for cloud providers
 * - Discovering models for local providers
 * - Model selection and validation
 * - Error handling during model discovery
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var viewModel: SettingsViewModel
    private lateinit var aiConfigurationManager: AIConfigurationManager
    private val testDispatcher = StandardTestDispatcher()

    private val testOpenAIModels = listOf(
        AIModel(
            id = "gpt-3.5-turbo",
            name = "GPT-3.5 Turbo",
            provider = AIProviderType.OpenAI,
            capabilities = setOf(AICapability.TEXT_GENERATION),
            contextWindow = 16385
        ),
        AIModel(
            id = "gpt-4",
            name = "GPT-4",
            provider = AIProviderType.OpenAI,
            capabilities = setOf(AICapability.TEXT_GENERATION),
            contextWindow = 8192
        )
    )

    private val testOllamaModels = listOf(
        AIModel(
            id = "llama2",
            name = "llama2",
            provider = AIProviderType.Ollama,
            capabilities = setOf(AICapability.TEXT_GENERATION),
            description = "Local Ollama model"
        ),
        AIModel(
            id = "mistral",
            name = "mistral",
            provider = AIProviderType.Ollama,
            capabilities = setOf(AICapability.TEXT_GENERATION),
            description = "Local Ollama model"
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        aiConfigurationManager = mockk(relaxed = true)
        
        // Default mock behavior
        coEvery { aiConfigurationManager.getCurrentConfiguration() } returns null
        coEvery { aiConfigurationManager.observeCurrentConfiguration() } returns flowOf(null)
        
        viewModel = SettingsViewModel(aiConfigurationManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `loadAvailableModels should load models for OpenAI provider`() = runTest {
        // Given
        coEvery { 
            aiConfigurationManager.getAvailableModels(AIProviderType.OpenAI, any()) 
        } returns testOpenAIModels

        // When
        viewModel.onProviderChanged(AIProviderType.OpenAI)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(AIProviderType.OpenAI, state.provider)
        assertEquals(testOpenAIModels, state.availableModels)
        assertFalse(state.isLoadingModels)
    }

    @Test
    fun `loadAvailableModels should load models for Anthropic provider`() = runTest {
        // Given
        val anthropicModels = listOf(
            AIModel(
                id = "claude-3-sonnet",
                name = "Claude 3 Sonnet",
                provider = AIProviderType.Anthropic,
                capabilities = setOf(AICapability.TEXT_GENERATION)
            )
        )
        coEvery { 
            aiConfigurationManager.getAvailableModels(AIProviderType.Anthropic, any()) 
        } returns anthropicModels

        // When
        viewModel.onProviderChanged(AIProviderType.Anthropic)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(anthropicModels, state.availableModels)
    }

    @Test
    fun `loadAvailableModels should not load models for Ollama provider`() = runTest {
        // When
        viewModel.onProviderChanged(AIProviderType.Ollama)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(AIProviderType.Ollama, state.provider)
        assertTrue(state.availableModels.isEmpty())
        coVerify(exactly = 0) { 
            aiConfigurationManager.getAvailableModels(AIProviderType.Ollama, any()) 
        }
    }

    @Test
    fun `discoverModels should discover and store local models for Ollama`() = runTest {
        // Given
        val baseUrl = "http://localhost:11434"
        viewModel.onProviderChanged(AIProviderType.Ollama)
        viewModel.onBaseUrlChanged(baseUrl)
        testDispatcher.scheduler.advanceUntilIdle()
        
        coEvery { 
            aiConfigurationManager.discoverLocalModels(AIProviderType.Ollama, baseUrl) 
        } returns testOllamaModels

        // When
        viewModel.discoverModels()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(testOllamaModels, state.discoveredModels)
        assertEquals(testOllamaModels, state.availableModels)
        assertFalse(state.isDiscoveringModels)
    }

    @Test
    fun `discoverModels should show error when base URL is blank`() = runTest {
        // Given
        viewModel.onProviderChanged(AIProviderType.Ollama)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.discoverModels()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertTrue(state.error is AppError.InvalidSettings)
    }

    @Test
    fun `discoverModels should handle discovery failure gracefully`() = runTest {
        // Given
        val baseUrl = "http://localhost:11434"
        viewModel.onProviderChanged(AIProviderType.Ollama)
        viewModel.onBaseUrlChanged(baseUrl)
        testDispatcher.scheduler.advanceUntilIdle()
        
        coEvery { 
            aiConfigurationManager.discoverLocalModels(AIProviderType.Ollama, baseUrl) 
        } throws Exception("Connection failed")

        // When
        viewModel.discoverModels()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isDiscoveringModels)
        assertNotNull(state.error)
        assertTrue(state.error is AppError.NetworkError)
        assertTrue(state.discoveredModels.isEmpty())
    }

    @Test
    fun `onProviderChanged should clear previous models and load new ones`() = runTest {
        // Given - Start with OpenAI
        coEvery { 
            aiConfigurationManager.getAvailableModels(AIProviderType.OpenAI, any()) 
        } returns testOpenAIModels
        
        viewModel.onProviderChanged(AIProviderType.OpenAI)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify OpenAI models are loaded
        assertEquals(testOpenAIModels, viewModel.uiState.value.availableModels)

        // When - Switch to Ollama
        viewModel.onProviderChanged(AIProviderType.Ollama)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Models should be cleared
        val state = viewModel.uiState.value
        assertEquals(AIProviderType.Ollama, state.provider)
        assertTrue(state.availableModels.isEmpty())
        assertTrue(state.modelName.isEmpty())
    }

    @Test
    fun `loadAvailableModels should handle network errors gracefully`() = runTest {
        // Given
        coEvery { 
            aiConfigurationManager.getAvailableModels(AIProviderType.OpenAI, any()) 
        } throws Exception("Network error")

        // When
        viewModel.onProviderChanged(AIProviderType.OpenAI)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertTrue(state.error is AppError.NetworkError)
        assertFalse(state.isLoadingModels)
    }

    @Test
    fun `model selection should update model name in state`() = runTest {
        // Given
        val modelId = "gpt-4"

        // When
        viewModel.onModelChanged(modelId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(modelId, viewModel.uiState.value.modelName)
    }

    @Test
    fun `initial configuration load should load available models`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.OpenAI,
            apiKey = "test-key",
            modelName = "gpt-4",
            isValidated = true
        )
        
        coEvery { aiConfigurationManager.getCurrentConfiguration() } returns config
        coEvery { 
            aiConfigurationManager.getAvailableModels(AIProviderType.OpenAI, any()) 
        } returns testOpenAIModels

        // When
        val newViewModel = SettingsViewModel(aiConfigurationManager)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = newViewModel.uiState.value
        assertEquals(config.provider, state.provider)
        assertEquals(config.modelName, state.modelName)
        assertEquals(testOpenAIModels, state.availableModels)
    }

    @Test
    fun `changing model name should clear validation status`() = runTest {
        // Given - Start with validated configuration
        viewModel.onProviderChanged(AIProviderType.OpenAI)
        viewModel.onApiKeyChanged("test-key")
        viewModel.onModelChanged("gpt-4")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Simulate successful validation
        coEvery { 
            aiConfigurationManager.validateConfiguration(any()) 
        } returns ValidationResult(isValid = true)
        coEvery { 
            aiConfigurationManager.testConnection(any()) 
        } returns ConnectionResult(isConnected = true)
        
        viewModel.validateConfiguration()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify validation succeeded
        assertEquals(ValidationStatus.SUCCESS, viewModel.uiState.value.validationStatus)

        // When - Change model
        viewModel.onModelChanged("gpt-3.5-turbo")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Validation should be cleared
        assertEquals(ValidationStatus.NONE, viewModel.uiState.value.validationStatus)
        assertTrue(viewModel.uiState.value.validationMessage.isEmpty())
    }

    @Test
    fun `discoverModels should set isDiscoveringModels flag during discovery`() = runTest {
        // Given
        val baseUrl = "http://localhost:11434"
        viewModel.onProviderChanged(AIProviderType.Ollama)
        viewModel.onBaseUrlChanged(baseUrl)
        testDispatcher.scheduler.advanceUntilIdle()
        
        coEvery { 
            aiConfigurationManager.discoverLocalModels(AIProviderType.Ollama, baseUrl) 
        } coAnswers {
            kotlinx.coroutines.delay(100)
            testOllamaModels
        }

        // When
        viewModel.discoverModels()
        
        // Then - Flag should be set during discovery
        assertTrue(viewModel.uiState.value.isDiscoveringModels)
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Flag should be cleared after discovery
        assertFalse(viewModel.uiState.value.isDiscoveringModels)
    }
}
