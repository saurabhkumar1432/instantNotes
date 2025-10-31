package com.voicenotesai.data.ai

import com.voicenotesai.data.model.AICapability
import com.voicenotesai.data.model.AIConfiguration
import com.voicenotesai.data.model.AIModel
import com.voicenotesai.data.model.AIProviderType
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.voicenotesai.domain.ai.AIConfigurationManager
import com.voicenotesai.domain.ai.AIConfigurationValidator
import com.voicenotesai.domain.ai.ConnectionResult
import com.voicenotesai.domain.ai.ValidationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AIConfigurationManager that handles saving, validating, and testing AI configurations.
 * Supports multiple providers including OpenAI, Anthropic, Google AI, OpenRouter, Ollama, LM Studio, and Custom.
 */
@Singleton
class AIConfigurationManagerImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val validator: AIConfigurationValidator,
    private val httpClient: OkHttpClient,
    private val json: Json
) : AIConfigurationManager {

    private val _currentConfiguration = MutableStateFlow<AIConfiguration?>(null)
    
    companion object {
        private val CURRENT_CONFIG_KEY = stringPreferencesKey("current_ai_configuration")
        private val ALL_CONFIGS_KEY = stringPreferencesKey("all_ai_configurations")
        private const val CONNECTION_TIMEOUT_MS = 10000L
    }

    init {
        // Load current configuration on initialization
        CoroutineScope(Dispatchers.IO).launch {
            loadCurrentConfiguration()
        }
    }

    override suspend fun saveConfiguration(config: AIConfiguration): Result<Unit> {
        return try {
            val allConfigs = getAllConfigurations().toMutableList()
            
            // Remove existing config with same provider and model if it exists
            allConfigs.removeAll { 
                it.provider == config.provider && it.modelName == config.modelName 
            }
            
            // Add the new/updated config
            allConfigs.add(config)
            
            // Save all configurations
            val configsJson = json.encodeToString(allConfigs)
            dataStore.edit { preferences ->
                preferences[ALL_CONFIGS_KEY] = configsJson
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentConfiguration(): AIConfiguration? {
        return _currentConfiguration.value ?: loadCurrentConfiguration()
    }

    override suspend fun getAllConfigurations(): List<AIConfiguration> {
        return try {
            val configsJson = dataStore.data.map { preferences ->
                preferences[ALL_CONFIGS_KEY]
            }.first()
            if (configsJson.isNullOrEmpty()) {
                emptyList()
            } else {
                json.decodeFromString<List<AIConfiguration>>(configsJson)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getConfigurationsForProvider(provider: AIProviderType): List<AIConfiguration> {
        return getAllConfigurations().filter { it.provider == provider }
    }

    override suspend fun deleteConfiguration(config: AIConfiguration): Result<Unit> {
        return try {
            val allConfigs = getAllConfigurations().toMutableList()
            allConfigs.removeAll { 
                it.provider == config.provider && it.modelName == config.modelName 
            }
            
            val configsJson = json.encodeToString(allConfigs)
            dataStore.edit { preferences ->
                preferences[ALL_CONFIGS_KEY] = configsJson
            }
            
            // If this was the current configuration, clear it
            if (_currentConfiguration.value == config) {
                _currentConfiguration.value = null
                dataStore.edit { preferences ->
                    preferences[CURRENT_CONFIG_KEY] = ""
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setActiveConfiguration(config: AIConfiguration): Result<Unit> {
        return try {
            _currentConfiguration.value = config
            val configJson = json.encodeToString(config)
            dataStore.edit { preferences ->
                preferences[CURRENT_CONFIG_KEY] = configJson
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeCurrentConfiguration(): Flow<AIConfiguration?> {
        return _currentConfiguration.asStateFlow()
    }

    override suspend fun validateConfiguration(config: AIConfiguration): ValidationResult {
        return validator.validateConfiguration(config)
    }

    override suspend fun testConnection(config: AIConfiguration): ConnectionResult {
        return validator.testConnection(config)
    }

    override suspend fun getAvailableModels(provider: AIProviderType, baseUrl: String?): List<AIModel> {
        return when (provider) {
            is AIProviderType.OpenAI -> AIModel.getOpenAIModels()
            is AIProviderType.Anthropic -> AIModel.getAnthropicModels()
            is AIProviderType.GoogleAI -> AIModel.getGoogleAIModels()
            is AIProviderType.OpenRouter -> getOpenRouterModels(baseUrl)
            is AIProviderType.Ollama -> discoverLocalModels(provider, baseUrl ?: provider.getDefaultBaseUrl()!!)
            is AIProviderType.LMStudio -> discoverLocalModels(provider, baseUrl ?: provider.getDefaultBaseUrl()!!)
            is AIProviderType.Custom -> emptyList() // Custom providers need manual model specification
        }
    }

    override suspend fun discoverLocalModels(provider: AIProviderType, baseUrl: String): List<AIModel> {
        return try {
            when (provider) {
                is AIProviderType.Ollama -> discoverOllamaModels(baseUrl)
                is AIProviderType.LMStudio -> discoverLMStudioModels(baseUrl)
                else -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun importConfiguration(configData: String): Result<AIConfiguration> {
        return try {
            val config = json.decodeFromString<AIConfiguration>(configData)
            saveConfiguration(config)
            Result.success(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportConfiguration(config: AIConfiguration): Result<String> {
        return try {
            val sanitizedConfig = config.copy(
                apiKey = null, // Don't export sensitive data
                customHeaders = emptyMap()
            )
            val configJson = json.encodeToString(sanitizedConfig)
            Result.success(configJson)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetToDefaults(): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences[ALL_CONFIGS_KEY] = ""
                preferences[CURRENT_CONFIG_KEY] = ""
            }
            _currentConfiguration.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getDefaultConfiguration(provider: AIProviderType): AIConfiguration {
        return AIConfiguration.defaultFor(provider)
    }

    private suspend fun loadCurrentConfiguration(): AIConfiguration? {
        return try {
            val configJson = dataStore.data.map { preferences ->
                preferences[CURRENT_CONFIG_KEY]
            }.first()
            if (configJson.isNullOrEmpty()) {
                null
            } else {
                val config = json.decodeFromString<AIConfiguration>(configJson)
                _currentConfiguration.value = config
                config
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getOpenRouterModels(baseUrl: String?): List<AIModel> {
        return try {
            val url = "${baseUrl ?: "https://openrouter.ai/api/v1"}/models"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    parseOpenRouterModels(responseBody)
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun discoverOllamaModels(baseUrl: String): List<AIModel> {
        return try {
            val url = "$baseUrl/api/tags"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    parseOllamaModels(responseBody)
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun discoverLMStudioModels(baseUrl: String): List<AIModel> {
        return try {
            val url = "$baseUrl/v1/models"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    parseLMStudioModels(responseBody)
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseOpenRouterModels(responseBody: String): List<AIModel> {
        return try {
            val jsonElement = json.parseToJsonElement(responseBody)
            val modelsArray = jsonElement.jsonObject["data"]?.jsonArray
            
            modelsArray?.mapNotNull { modelElement ->
                try {
                    val modelObj = modelElement.jsonObject
                    val id = modelObj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val name = modelObj["name"]?.jsonPrimitive?.content ?: id
                    
                    AIModel(
                        id = id,
                        name = name,
                        provider = AIProviderType.OpenRouter,
                        capabilities = AIModel.NOTE_PROCESSING_CAPABILITIES,
                        description = "OpenRouter model: $name"
                    )
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseOllamaModels(responseBody: String): List<AIModel> {
        return try {
            val jsonElement = json.parseToJsonElement(responseBody)
            val modelsArray = jsonElement.jsonObject["models"]?.jsonArray
            
            modelsArray?.mapNotNull { modelElement ->
                try {
                    val modelObj = modelElement.jsonObject
                    val name = modelObj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    
                    AIModel(
                        id = name,
                        name = name,
                        provider = AIProviderType.Ollama,
                        capabilities = AIModel.NOTE_PROCESSING_CAPABILITIES,
                        description = "Local Ollama model"
                    )
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseLMStudioModels(responseBody: String): List<AIModel> {
        return try {
            val jsonElement = json.parseToJsonElement(responseBody)
            val modelsArray = jsonElement.jsonObject["data"]?.jsonArray
            
            modelsArray?.mapNotNull { modelElement ->
                try {
                    val modelObj = modelElement.jsonObject
                    val id = modelObj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    
                    AIModel(
                        id = id,
                        name = id,
                        provider = AIProviderType.LMStudio,
                        capabilities = AIModel.NOTE_PROCESSING_CAPABILITIES,
                        description = "Local LM Studio model"
                    )
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}