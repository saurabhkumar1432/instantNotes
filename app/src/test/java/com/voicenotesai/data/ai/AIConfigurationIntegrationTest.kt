package com.voicenotesai.data.ai

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.voicenotesai.data.model.AIConfiguration
import com.voicenotesai.data.model.AIProviderType
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AIConfigurationIntegrationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var validator: AIConfigurationValidatorImpl
    private lateinit var configurationManager: AIConfigurationManagerImpl
    private lateinit var json: Json

    private val testConfig = AIConfiguration(
        provider = AIProviderType.OpenAI,
        apiKey = "sk-test-key",
        modelName = "gpt-3.5-turbo"
    )

    @Before
    fun setup() {
        val testFile = File(tempFolder.newFolder(), "test_preferences.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create { testFile }
        
        json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
        
        val httpClient = OkHttpClient.Builder().build()
        
        validator = AIConfigurationValidatorImpl(
            httpClient = httpClient,
            json = json
        )
        
        configurationManager = AIConfigurationManagerImpl(
            dataStore = dataStore,
            validator = validator,
            httpClient = httpClient,
            json = json
        )
    }

    @Test
    fun `should save and retrieve configuration`() = runTest {
        // When
        val saveResult = configurationManager.saveConfiguration(testConfig)
        
        // Then
        assertTrue(saveResult.isSuccess)
        
        val allConfigs = configurationManager.getAllConfigurations()
        assertEquals(1, allConfigs.size)
        assertEquals(testConfig.provider, allConfigs[0].provider)
        assertEquals(testConfig.modelName, allConfigs[0].modelName)
    }

    @Test
    fun `should set and get current configuration`() = runTest {
        // Given
        configurationManager.saveConfiguration(testConfig)
        
        // When
        val setResult = configurationManager.setActiveConfiguration(testConfig)
        
        // Then
        assertTrue(setResult.isSuccess)
        
        val currentConfig = configurationManager.getCurrentConfiguration()
        assertNotNull(currentConfig)
        assertEquals(testConfig.provider, currentConfig?.provider)
        assertEquals(testConfig.modelName, currentConfig?.modelName)
    }

    @Test
    fun `should return null when no current configuration exists`() = runTest {
        // When
        val currentConfig = configurationManager.getCurrentConfiguration()
        
        // Then
        assertNull(currentConfig)
    }

    @Test
    fun `should delete configuration`() = runTest {
        // Given
        configurationManager.saveConfiguration(testConfig)
        configurationManager.setActiveConfiguration(testConfig)
        
        // When
        val deleteResult = configurationManager.deleteConfiguration(testConfig)
        
        // Then
        assertTrue(deleteResult.isSuccess)
        
        val allConfigs = configurationManager.getAllConfigurations()
        assertTrue(allConfigs.isEmpty())
        
        val currentConfig = configurationManager.getCurrentConfiguration()
        assertNull(currentConfig)
    }

    @Test
    fun `should validate OpenAI configuration correctly`() = runTest {
        // Given
        val validConfig = testConfig.copy(apiKey = "sk-valid-key")
        val invalidConfig = testConfig.copy(apiKey = "invalid-key")
        
        // When
        val validResult = validator.validateConfiguration(validConfig)
        val invalidResult = validator.validateConfiguration(invalidConfig)
        
        // Then
        assertTrue(validResult.isValid)
        assertFalse(invalidResult.isValid)
        assertEquals("OpenAI API key should start with 'sk-'", invalidResult.errorMessage)
    }

    @Test
    fun `should validate Ollama configuration correctly`() = runTest {
        // Given
        val validConfig = AIConfiguration(
            provider = AIProviderType.Ollama,
            baseUrl = "http://localhost:11434",
            modelName = "llama2"
        )
        val invalidConfig = AIConfiguration(
            provider = AIProviderType.Ollama,
            baseUrl = "invalid-url",
            modelName = "llama2"
        )
        
        // When
        val validResult = validator.validateConfiguration(validConfig)
        val invalidResult = validator.validateConfiguration(invalidConfig)
        
        // Then
        assertTrue(validResult.isValid)
        assertFalse(invalidResult.isValid)
        assertEquals("Invalid Ollama base URL format", invalidResult.errorMessage)
    }

    @Test
    fun `should get default configuration for providers`() {
        // When
        val openAIDefault = configurationManager.getDefaultConfiguration(AIProviderType.OpenAI)
        val ollamaDefault = configurationManager.getDefaultConfiguration(AIProviderType.Ollama)
        
        // Then
        assertEquals(AIProviderType.OpenAI, openAIDefault.provider)
        assertEquals("gpt-3.5-turbo", openAIDefault.modelName)
        assertEquals("https://api.openai.com/v1", openAIDefault.getEffectiveBaseUrl())
        
        assertEquals(AIProviderType.Ollama, ollamaDefault.provider)
        assertEquals("llama2", ollamaDefault.modelName)
        assertEquals("http://localhost:11434", ollamaDefault.getEffectiveBaseUrl())
    }

    @Test
    fun `should export configuration without sensitive data`() = runTest {
        // Given
        val configWithSecrets = testConfig.copy(
            apiKey = "secret-key",
            customHeaders = mapOf("Authorization" to "Bearer secret")
        )
        
        // When
        val exportResult = configurationManager.exportConfiguration(configWithSecrets)
        
        // Then
        assertTrue(exportResult.isSuccess)
        val exportedJson = exportResult.getOrNull()
        assertNotNull(exportedJson)
        
        val exportedConfig = json.decodeFromString<AIConfiguration>(exportedJson!!)
        assertNull(exportedConfig.apiKey)
        assertTrue(exportedConfig.customHeaders.isEmpty())
    }

    @Test
    fun `should import configuration successfully`() = runTest {
        // Given
        val configJson = json.encodeToString(AIConfiguration.serializer(), testConfig)
        
        // When
        val importResult = configurationManager.importConfiguration(configJson)
        
        // Then
        assertTrue(importResult.isSuccess)
        val importedConfig = importResult.getOrNull()
        assertNotNull(importedConfig)
        assertEquals(testConfig.provider, importedConfig?.provider)
        assertEquals(testConfig.modelName, importedConfig?.modelName)
        
        // Verify it was saved
        val allConfigs = configurationManager.getAllConfigurations()
        assertEquals(1, allConfigs.size)
    }

    @Test
    fun `should reset to defaults`() = runTest {
        // Given
        configurationManager.saveConfiguration(testConfig)
        configurationManager.setActiveConfiguration(testConfig)
        
        // When
        val resetResult = configurationManager.resetToDefaults()
        
        // Then
        assertTrue(resetResult.isSuccess)
        
        val allConfigs = configurationManager.getAllConfigurations()
        assertTrue(allConfigs.isEmpty())
        
        val currentConfig = configurationManager.getCurrentConfiguration()
        assertNull(currentConfig)
    }
}