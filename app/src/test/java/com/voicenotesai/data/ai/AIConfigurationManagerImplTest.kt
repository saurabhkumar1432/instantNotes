package com.voicenotesai.data.ai

import app.cash.turbine.test
import com.voicenotesai.data.model.AIConfiguration
import com.voicenotesai.data.model.AIProviderType
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.voicenotesai.domain.ai.AIConfigurationValidator
import com.voicenotesai.domain.ai.ConnectionResult
import com.voicenotesai.domain.ai.ValidationResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AIConfigurationManagerImplTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var validator: AIConfigurationValidator
    private lateinit var httpClient: OkHttpClient
    private lateinit var json: Json
    private lateinit var configurationManager: AIConfigurationManagerImpl

    private val testConfig = AIConfiguration(
        provider = AIProviderType.OpenAI,
        apiKey = "sk-test-key",
        modelName = "gpt-3.5-turbo"
    )

    @Before
    fun setup() {
        dataStore = mockk(relaxed = true)
        validator = mockk(relaxed = true)
        httpClient = mockk(relaxed = true)
        json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
        
        configurationManager = AIConfigurationManagerImpl(
            dataStore = dataStore,
            validator = validator,
            httpClient = httpClient,
            json = json
        )
    }

    @Test
    fun `saveConfiguration should save config successfully`() = runTest {
        // Given
        every { dataStore.data } returns flowOf(mockk(relaxed = true))
        coEvery { dataStore.edit(any()) } returns mockk(relaxed = true)

        // When
        val result = configurationManager.saveConfiguration(testConfig)

        // Then
        assertTrue(result.isSuccess)
        coVerify { dataStore.edit(any()) }
    }

    @Test
    fun `getCurrentConfiguration should return null when no config exists`() = runTest {
        // Given
        every { settingsRepository.getString("current_ai_configuration") } returns flowOf("")

        // When
        val result = configurationManager.getCurrentConfiguration()

        // Then
        assertNull(result)
    }

    @Test
    fun `getCurrentConfiguration should return saved config`() = runTest {
        // Given
        val configJson = json.encodeToString(AIConfiguration.serializer(), testConfig)
        every { settingsRepository.getString("current_ai_configuration") } returns flowOf(configJson)

        // When
        val result = configurationManager.getCurrentConfiguration()

        // Then
        assertNotNull(result)
        assertEquals(testConfig.provider, result?.provider)
        assertEquals(testConfig.modelName, result?.modelName)
    }

    @Test
    fun `setActiveConfiguration should update current config`() = runTest {
        // Given
        coEvery { settingsRepository.saveString(any(), any()) } returns Unit

        // When
        val result = configurationManager.setActiveConfiguration(testConfig)

        // Then
        assertTrue(result.isSuccess)
        coVerify { settingsRepository.saveString("current_ai_configuration", any()) }
    }

    @Test
    fun `observeCurrentConfiguration should emit config changes`() = runTest {
        // Given
        coEvery { settingsRepository.saveString(any(), any()) } returns Unit

        // When & Then
        configurationManager.observeCurrentConfiguration().test {
            // Initial value should be null
            assertNull(awaitItem())

            // Set a configuration
            configurationManager.setActiveConfiguration(testConfig)
            
            // Should emit the new configuration
            val emittedConfig = awaitItem()
            assertNotNull(emittedConfig)
            assertEquals(testConfig.provider, emittedConfig?.provider)
        }
    }

    @Test
    fun `validateConfiguration should delegate to validator`() = runTest {
        // Given
        val expectedResult = ValidationResult.success(emptySet())
        coEvery { validator.validateConfiguration(testConfig) } returns expectedResult

        // When
        val result = configurationManager.validateConfiguration(testConfig)

        // Then
        assertEquals(expectedResult, result)
        coVerify { validator.validateConfiguration(testConfig) }
    }

    @Test
    fun `testConnection should delegate to validator`() = runTest {
        // Given
        val expectedResult = ConnectionResult.success(100L)
        coEvery { validator.testConnection(testConfig) } returns expectedResult

        // When
        val result = configurationManager.testConnection(testConfig)

        // Then
        assertEquals(expectedResult, result)
        coVerify { validator.testConnection(testConfig) }
    }

    @Test
    fun `getDefaultConfiguration should return correct default for OpenAI`() {
        // When
        val result = configurationManager.getDefaultConfiguration(AIProviderType.OpenAI)

        // Then
        assertEquals(AIProviderType.OpenAI, result.provider)
        assertEquals("gpt-3.5-turbo", result.modelName)
        assertEquals("https://api.openai.com/v1", result.getEffectiveBaseUrl())
    }

    @Test
    fun `getDefaultConfiguration should return correct default for Ollama`() {
        // When
        val result = configurationManager.getDefaultConfiguration(AIProviderType.Ollama)

        // Then
        assertEquals(AIProviderType.Ollama, result.provider)
        assertEquals("llama2", result.modelName)
        assertEquals("http://localhost:11434", result.getEffectiveBaseUrl())
    }

    @Test
    fun `deleteConfiguration should remove config from list`() = runTest {
        // Given
        val configList = listOf(testConfig)
        val configsJson = json.encodeToString(configList)
        every { settingsRepository.getString("all_ai_configurations") } returns flowOf(configsJson)
        coEvery { settingsRepository.saveString(any(), any()) } returns Unit

        // When
        val result = configurationManager.deleteConfiguration(testConfig)

        // Then
        assertTrue(result.isSuccess)
        coVerify { settingsRepository.saveString("all_ai_configurations", "[]") }
    }

    @Test
    fun `resetToDefaults should clear all configurations`() = runTest {
        // Given
        coEvery { settingsRepository.saveString(any(), any()) } returns Unit

        // When
        val result = configurationManager.resetToDefaults()

        // Then
        assertTrue(result.isSuccess)
        coVerify { settingsRepository.saveString("all_ai_configurations", "") }
        coVerify { settingsRepository.saveString("current_ai_configuration", "") }
    }

    @Test
    fun `exportConfiguration should return sanitized config JSON`() = runTest {
        // Given
        val configWithSecrets = testConfig.copy(
            apiKey = "secret-key",
            customHeaders = mapOf("Authorization" to "Bearer secret")
        )

        // When
        val result = configurationManager.exportConfiguration(configWithSecrets)

        // Then
        assertTrue(result.isSuccess)
        val exportedJson = result.getOrNull()
        assertNotNull(exportedJson)
        
        // Verify sensitive data is not included
        val exportedConfig = json.decodeFromString<AIConfiguration>(exportedJson!!)
        assertNull(exportedConfig.apiKey)
        assertTrue(exportedConfig.customHeaders.isEmpty())
    }

    @Test
    fun `importConfiguration should parse and save config`() = runTest {
        // Given
        val configJson = json.encodeToString(AIConfiguration.serializer(), testConfig)
        every { settingsRepository.getString("all_ai_configurations") } returns flowOf("")
        coEvery { settingsRepository.saveString(any(), any()) } returns Unit

        // When
        val result = configurationManager.importConfiguration(configJson)

        // Then
        assertTrue(result.isSuccess)
        val importedConfig = result.getOrNull()
        assertNotNull(importedConfig)
        assertEquals(testConfig.provider, importedConfig?.provider)
        assertEquals(testConfig.modelName, importedConfig?.modelName)
    }
}