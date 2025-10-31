package com.voicenotesai.data.ai

import com.voicenotesai.data.model.AICapability
import com.voicenotesai.data.model.AIConfiguration
import com.voicenotesai.data.model.AIModel
import com.voicenotesai.data.model.AIProviderType
import com.voicenotesai.domain.ai.AIConfigurationValidator
import com.voicenotesai.domain.ai.ConnectionResult
import com.voicenotesai.domain.ai.ValidationResult
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AIConfigurationValidator that validates AI configurations and tests connections.
 * Supports provider-specific validation logic for each AI provider type.
 */
@Singleton
class AIConfigurationValidatorImpl @Inject constructor(
    private val httpClient: OkHttpClient,
    private val json: Json
) : AIConfigurationValidator {

    companion object {
        private const val CONNECTION_TIMEOUT_MS = 10000L
        private const val TEST_PROMPT = "Hello"
    }

    override suspend fun validateConfiguration(config: AIConfiguration): ValidationResult {
        return try {
            // Basic validation
            if (config.modelName.isBlank()) {
                return ValidationResult.failure("Model name cannot be empty")
            }

            // Provider-specific validation
            when (config.provider) {
                is AIProviderType.OpenAI -> validateOpenAIConfig(config)
                is AIProviderType.Anthropic -> validateAnthropicConfig(config)
                is AIProviderType.GoogleAI -> validateGoogleAIConfig(config)
                is AIProviderType.OpenRouter -> validateOpenRouterConfig(config)
                is AIProviderType.Ollama -> validateOllamaConfig(config)
                is AIProviderType.LMStudio -> validateLMStudioConfig(config)
                is AIProviderType.Custom -> validateCustomConfig(config)
            }
        } catch (e: Exception) {
            ValidationResult.failure("Validation error: ${e.message}")
        }
    }

    override suspend fun testConnection(config: AIConfiguration): ConnectionResult {
        return try {
            val startTime = System.currentTimeMillis()
            
            val result = withTimeoutOrNull(CONNECTION_TIMEOUT_MS) {
                when (config.provider) {
                    is AIProviderType.OpenAI -> testOpenAIConnection(config)
                    is AIProviderType.Anthropic -> testAnthropicConnection(config)
                    is AIProviderType.GoogleAI -> testGoogleAIConnection(config)
                    is AIProviderType.OpenRouter -> testOpenRouterConnection(config)
                    is AIProviderType.Ollama -> testOllamaConnection(config)
                    is AIProviderType.LMStudio -> testLMStudioConnection(config)
                    is AIProviderType.Custom -> testCustomConnection(config)
                }
            }
            
            val responseTime = System.currentTimeMillis() - startTime
            
            result?.let { 
                if (it.isConnected) {
                    ConnectionResult.success(responseTime, it.details)
                } else {
                    it
                }
            } ?: ConnectionResult.failure("Connection timeout after ${CONNECTION_TIMEOUT_MS}ms")
        } catch (e: Exception) {
            ConnectionResult.failure("Connection error: ${e.message}")
        }
    }

    override suspend fun getAvailableModels(config: AIConfiguration): List<AIModel> {
        return try {
            when (config.provider) {
                is AIProviderType.OpenAI -> AIModel.getOpenAIModels()
                is AIProviderType.Anthropic -> AIModel.getAnthropicModels()
                is AIProviderType.GoogleAI -> AIModel.getGoogleAIModels()
                is AIProviderType.OpenRouter -> getOpenRouterModels(config)
                is AIProviderType.Ollama -> getOllamaModels(config)
                is AIProviderType.LMStudio -> getLMStudioModels(config)
                is AIProviderType.Custom -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun validateModel(config: AIConfiguration, modelId: String): ValidationResult {
        return try {
            val availableModels = getAvailableModels(config)
            val modelExists = availableModels.any { it.id == modelId }
            
            if (modelExists) {
                val model = availableModels.first { it.id == modelId }
                ValidationResult.success(
                    capabilities = model.capabilities,
                    details = mapOf(
                        "model_name" to model.name,
                        "context_window" to (model.contextWindow ?: 0),
                        "cost_tier" to model.costTier
                    )
                )
            } else {
                ValidationResult.failure("Model '$modelId' not found or not available")
            }
        } catch (e: Exception) {
            ValidationResult.failure("Model validation error: ${e.message}")
        }
    }

    override suspend fun fullValidation(config: AIConfiguration): ValidationResult {
        // First validate configuration
        val configValidation = validateConfiguration(config)
        if (!configValidation.isValid) {
            return configValidation
        }

        // Then test connection
        val connectionResult = testConnection(config)
        if (!connectionResult.isConnected) {
            return ValidationResult.failure(
                errorMessage = connectionResult.errorMessage ?: "Connection failed",
                details = connectionResult.details
            )
        }

        // Finally validate model
        val modelValidation = validateModel(config, config.modelName)
        return modelValidation
    }

    // Provider-specific validation methods
    private fun validateOpenAIConfig(config: AIConfiguration): ValidationResult {
        if (config.apiKey.isNullOrBlank()) {
            return ValidationResult.failure("OpenAI API key is required")
        }
        
        if (!config.apiKey.startsWith("sk-")) {
            return ValidationResult.failure("OpenAI API key should start with 'sk-'")
        }

        return ValidationResult.success(AIModel.ADVANCED_CAPABILITIES)
    }

    private fun validateAnthropicConfig(config: AIConfiguration): ValidationResult {
        if (config.apiKey.isNullOrBlank()) {
            return ValidationResult.failure("Anthropic API key is required")
        }
        
        if (!config.apiKey.startsWith("sk-ant-")) {
            return ValidationResult.failure("Anthropic API key should start with 'sk-ant-'")
        }

        return ValidationResult.success(AIModel.ADVANCED_CAPABILITIES)
    }

    private fun validateGoogleAIConfig(config: AIConfiguration): ValidationResult {
        if (config.apiKey.isNullOrBlank()) {
            return ValidationResult.failure("Google AI API key is required")
        }

        return ValidationResult.success(AIModel.ADVANCED_CAPABILITIES)
    }

    private fun validateOpenRouterConfig(config: AIConfiguration): ValidationResult {
        if (config.apiKey.isNullOrBlank()) {
            return ValidationResult.failure("OpenRouter API key is required")
        }
        
        if (!config.apiKey.startsWith("sk-or-")) {
            return ValidationResult.failure("OpenRouter API key should start with 'sk-or-'")
        }

        return ValidationResult.success(AIModel.NOTE_PROCESSING_CAPABILITIES)
    }

    private fun validateOllamaConfig(config: AIConfiguration): ValidationResult {
        val baseUrl = config.getEffectiveBaseUrl()
        if (baseUrl.isNullOrBlank()) {
            return ValidationResult.failure("Ollama base URL is required")
        }

        try {
            URL(baseUrl)
        } catch (e: Exception) {
            return ValidationResult.failure("Invalid Ollama base URL format")
        }

        return ValidationResult.success(AIModel.NOTE_PROCESSING_CAPABILITIES)
    }

    private fun validateLMStudioConfig(config: AIConfiguration): ValidationResult {
        val baseUrl = config.getEffectiveBaseUrl()
        if (baseUrl.isNullOrBlank()) {
            return ValidationResult.failure("LM Studio base URL is required")
        }

        try {
            URL(baseUrl)
        } catch (e: Exception) {
            return ValidationResult.failure("Invalid LM Studio base URL format")
        }

        return ValidationResult.success(AIModel.NOTE_PROCESSING_CAPABILITIES)
    }

    private fun validateCustomConfig(config: AIConfiguration): ValidationResult {
        val baseUrl = config.getEffectiveBaseUrl()
        if (baseUrl.isNullOrBlank()) {
            return ValidationResult.failure("Custom provider base URL is required")
        }

        try {
            URL(baseUrl)
        } catch (e: Exception) {
            return ValidationResult.failure("Invalid custom provider base URL format")
        }

        return ValidationResult.success(AIModel.BASIC_TEXT_CAPABILITIES)
    }

    // Connection testing methods
    private suspend fun testOpenAIConnection(config: AIConfiguration): ConnectionResult {
        return try {
            val requestBody = buildJsonObject {
                put("model", config.modelName)
                putJsonArray("messages") {
                    addJsonObject {
                        put("role", "user")
                        put("content", TEST_PROMPT)
                    }
                }
                put("max_tokens", 5)
            }.toString()

            val request = Request.Builder()
                .url("${config.getEffectiveBaseUrl()}/chat/completions")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .header("Authorization", "Bearer ${config.apiKey}")
                .build()

            val startTime = System.currentTimeMillis()
            val response = httpClient.newCall(request).execute()
            val responseTime = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                ConnectionResult.success(responseTime)
            } else {
                val errorBody = response.body?.string()
                ConnectionResult.failure("HTTP ${response.code}: $errorBody")
            }
        } catch (e: Exception) {
            ConnectionResult.failure("OpenAI connection error: ${e.message}")
        }
    }

    private suspend fun testAnthropicConnection(config: AIConfiguration): ConnectionResult {
        return try {
            val requestBody = buildJsonObject {
                put("model", config.modelName)
                put("max_tokens", 5)
                putJsonArray("messages") {
                    addJsonObject {
                        put("role", "user")
                        put("content", TEST_PROMPT)
                    }
                }
            }.toString()

            val request = Request.Builder()
                .url("${config.getEffectiveBaseUrl()}/v1/messages")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .header("x-api-key", config.apiKey!!)
                .header("anthropic-version", "2023-06-01")
                .build()

            val startTime = System.currentTimeMillis()
            val response = httpClient.newCall(request).execute()
            val responseTime = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                ConnectionResult.success(responseTime)
            } else {
                val errorBody = response.body?.string()
                ConnectionResult.failure("HTTP ${response.code}: $errorBody")
            }
        } catch (e: Exception) {
            ConnectionResult.failure("Anthropic connection error: ${e.message}")
        }
    }

    private suspend fun testGoogleAIConnection(config: AIConfiguration): ConnectionResult {
        return try {
            val requestBody = buildJsonObject {
                putJsonArray("contents") {
                    addJsonObject {
                        putJsonArray("parts") {
                            addJsonObject {
                                put("text", TEST_PROMPT)
                            }
                        }
                        put("role", "user")
                    }
                }
                putJsonObject("generationConfig") {
                    put("maxOutputTokens", 5)
                }
            }.toString()

            val request = Request.Builder()
                .url("${config.getEffectiveBaseUrl()}/models/${config.modelName}:generateContent?key=${config.apiKey}")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val startTime = System.currentTimeMillis()
            val response = httpClient.newCall(request).execute()
            val responseTime = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                ConnectionResult.success(responseTime)
            } else {
                val errorBody = response.body?.string()
                ConnectionResult.failure("HTTP ${response.code}: $errorBody")
            }
        } catch (e: Exception) {
            ConnectionResult.failure("Google AI connection error: ${e.message}")
        }
    }

    private suspend fun testOpenRouterConnection(config: AIConfiguration): ConnectionResult {
        return try {
            val requestBody = buildJsonObject {
                put("model", config.modelName)
                putJsonArray("messages") {
                    addJsonObject {
                        put("role", "user")
                        put("content", TEST_PROMPT)
                    }
                }
                put("max_tokens", 5)
            }.toString()

            val request = Request.Builder()
                .url("${config.getEffectiveBaseUrl()}/chat/completions")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .header("Authorization", "Bearer ${config.apiKey}")
                .build()

            val startTime = System.currentTimeMillis()
            val response = httpClient.newCall(request).execute()
            val responseTime = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                ConnectionResult.success(responseTime)
            } else {
                val errorBody = response.body?.string()
                ConnectionResult.failure("HTTP ${response.code}: $errorBody")
            }
        } catch (e: Exception) {
            ConnectionResult.failure("OpenRouter connection error: ${e.message}")
        }
    }

    private suspend fun testOllamaConnection(config: AIConfiguration): ConnectionResult {
        return try {
            val request = Request.Builder()
                .url("${config.getEffectiveBaseUrl()}/api/tags")
                .get()
                .build()

            val startTime = System.currentTimeMillis()
            val response = httpClient.newCall(request).execute()
            val responseTime = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                ConnectionResult.success(responseTime)
            } else {
                ConnectionResult.failure("HTTP ${response.code}: Ollama server not responding")
            }
        } catch (e: Exception) {
            ConnectionResult.failure("Ollama connection error: ${e.message}")
        }
    }

    private suspend fun testLMStudioConnection(config: AIConfiguration): ConnectionResult {
        return try {
            val request = Request.Builder()
                .url("${config.getEffectiveBaseUrl()}/v1/models")
                .get()
                .build()

            val startTime = System.currentTimeMillis()
            val response = httpClient.newCall(request).execute()
            val responseTime = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                ConnectionResult.success(responseTime)
            } else {
                ConnectionResult.failure("HTTP ${response.code}: LM Studio server not responding")
            }
        } catch (e: Exception) {
            ConnectionResult.failure("LM Studio connection error: ${e.message}")
        }
    }

    private suspend fun testCustomConnection(config: AIConfiguration): ConnectionResult {
        return try {
            val requestBuilder = Request.Builder()
                .url(config.getEffectiveBaseUrl()!!)
                .get()

            // Add custom headers if provided
            config.customHeaders.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }

            // Add API key if provided
            if (!config.apiKey.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer ${config.apiKey}")
            }

            val request = requestBuilder.build()

            val startTime = System.currentTimeMillis()
            val response = httpClient.newCall(request).execute()
            val responseTime = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                ConnectionResult.success(responseTime)
            } else {
                ConnectionResult.failure("HTTP ${response.code}: Custom provider not responding")
            }
        } catch (e: Exception) {
            ConnectionResult.failure("Custom provider connection error: ${e.message}")
        }
    }

    // Model discovery methods
    private suspend fun getOpenRouterModels(config: AIConfiguration): List<AIModel> {
        return try {
            val request = Request.Builder()
                .url("${config.getEffectiveBaseUrl()}/models")
                .header("Authorization", "Bearer ${config.apiKey}")
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

    private suspend fun getOllamaModels(config: AIConfiguration): List<AIModel> {
        return try {
            val request = Request.Builder()
                .url("${config.getEffectiveBaseUrl()}/api/tags")
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

    private suspend fun getLMStudioModels(config: AIConfiguration): List<AIModel> {
        return try {
            val request = Request.Builder()
                .url("${config.getEffectiveBaseUrl()}/v1/models")
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