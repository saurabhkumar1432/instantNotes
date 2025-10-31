package com.voicenotesai.data.ai

import com.voicenotesai.data.model.AIConfiguration
import com.voicenotesai.data.model.AIProviderType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AIConfigurationValidatorImplTest {

    private lateinit var httpClient: OkHttpClient
    private lateinit var json: Json
    private lateinit var validator: AIConfigurationValidatorImpl

    @Before
    fun setup() {
        httpClient = mockk(relaxed = true)
        json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
        
        validator = AIConfigurationValidatorImpl(
            httpClient = httpClient,
            json = json
        )
    }

    @Test
    fun `validateConfiguration should fail for empty model name`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.OpenAI,
            apiKey = "sk-test",
            modelName = ""
        )

        // When
        val result = validator.validateConfiguration(config)

        // Then
        assertFalse(result.isValid)
        assertEquals("Model name cannot be empty", result.errorMessage)
    }

    @Test
    fun `validateConfiguration should fail for OpenAI without API key`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.OpenAI,
            apiKey = null,
            modelName = "gpt-3.5-turbo"
        )

        // When
        val result = validator.validateConfiguration(config)

        // Then
        assertFalse(result.isValid)
        assertEquals("OpenAI API key is required", result.errorMessage)
    }

    @Test
    fun `validateConfiguration should fail for OpenAI with invalid API key format`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.OpenAI,
            apiKey = "invalid-key",
            modelName = "gpt-3.5-turbo"
        )

        // When
        val result = validator.validateConfiguration(config)

        // Then
        assertFalse(result.isValid)
        assertEquals("OpenAI API key should start with 'sk-'", result.errorMessage)
    }

    @Test
    fun `validateConfiguration should succeed for valid OpenAI config`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.OpenAI,
            apiKey = "sk-test-key",
            modelName = "gpt-3.5-turbo"
        )

        // When
        val result = validator.validateConfiguration(config)

        // Then
        assertTrue(result.isValid)
        assertTrue(result.supportedCapabilities.isNotEmpty())
    }

    @Test
    fun `validateConfiguration should fail for Anthropic without API key`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.Anthropic,
            apiKey = null,
            modelName = "claude-3-haiku"
        )

        // When
        val result = validator.validateConfiguration(config)

        // Then
        assertFalse(result.isValid)
        assertEquals("Anthropic API key is required", result.errorMessage)
    }

    @Test
    fun `validateConfiguration should fail for Anthropic with invalid API key format`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.Anthropic,
            apiKey = "invalid-key",
            modelName = "claude-3-haiku"
        )

        // When
        val result = validator.validateConfiguration(config)

        // Then
        assertFalse(result.isValid)
        assertEquals("Anthropic API key should start with 'sk-ant-'", result.errorMessage)
    }

    @Test
    fun `validateConfiguration should succeed for valid Anthropic config`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.Anthropic,
            apiKey = "sk-ant-test-key",
            modelName = "claude-3-haiku"
        )

        // When
        val result = validator.validateConfiguration(config)

        // Then
        assertTrue(result.isValid)
        assertTrue(result.supportedCapabilities.isNotEmpty())
    }

    @Test
    fun `validateConfiguration should fail for OpenRouter with invalid API key format`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.OpenRouter,
            apiKey = "invalid-key",
            modelName = "openai/gpt-3.5-turbo"
        )

        // When
        val result = validator.validateConfiguration(config)

        // Then
        assertFalse(result.isValid)
        assertEquals("OpenRouter API key should start with 'sk-or-'", result.errorMessage)
    }

    @Test
    fun `validateConfiguration should succeed for valid OpenRouter config`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.OpenRouter,
            apiKey = "sk-or-test-key",
            modelName = "openai/gpt-3.5-turbo"
        )

        // When
        val result = validator.validateConfiguration(config)

        // Then
        assertTrue(result.isValid)
        assertTrue(result.supportedCapabilities.isNotEmpty())
    }

    @Test
    fun `validateConfiguration should fail for Ollama with invalid base URL`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.Ollama,
            baseUrl = "invalid-url",
            modelName = "llama2"
        )

        // When
        val result = validator.validateConfiguration(config)

        // Then
        assertFalse(result.isValid)
        assertEquals("Invalid Ollama base URL format", result.errorMessage)
    }

    @Test
    fun `validateConfiguration should succeed for valid Ollama config`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.Ollama,
            baseUrl = "http://localhost:11434",
            modelName = "llama2"
        )

        // When
        val result = validator.validateConfiguration(config)

        // Then
        assertTrue(result.isValid)
        assertTrue(result.supportedCapabilities.isNotEmpty())
    }

    @Test
    fun `validateConfiguration should succeed for valid LM Studio config`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.LMStudio,
            baseUrl = "http://localhost:1234",
            modelName = "local-model"
        )

        // When
        val result = validator.validateConfiguration(config)

        // Then
        assertTrue(result.isValid)
        assertTrue(result.supportedCapabilities.isNotEmpty())
    }

    @Test
    fun `validateConfiguration should succeed for valid Custom config`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.Custom("My Custom Provider"),
            baseUrl = "https://api.example.com",
            modelName = "custom-model"
        )

        // When
        val result = validator.validateConfiguration(config)

        // Then
        assertTrue(result.isValid)
        assertTrue(result.supportedCapabilities.isNotEmpty())
    }

    @Test
    fun `testConnection should return success for successful HTTP response`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.Ollama,
            baseUrl = "http://localhost:11434",
            modelName = "llama2"
        )

        val mockCall = mockk<Call>()
        val mockResponse = Response.Builder()
            .request(Request.Builder().url("http://localhost:11434/api/tags").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("{}".toResponseBody())
            .build()

        every { httpClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns mockResponse

        // When
        val result = validator.testConnection(config)

        // Then
        assertTrue(result.isConnected)
        assertTrue(result.responseTimeMs!! > 0)
    }

    @Test
    fun `testConnection should return failure for HTTP error response`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.Ollama,
            baseUrl = "http://localhost:11434",
            modelName = "llama2"
        )

        val mockCall = mockk<Call>()
        val mockResponse = Response.Builder()
            .request(Request.Builder().url("http://localhost:11434/api/tags").build())
            .protocol(Protocol.HTTP_1_1)
            .code(404)
            .message("Not Found")
            .body("Not Found".toResponseBody())
            .build()

        every { httpClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns mockResponse

        // When
        val result = validator.testConnection(config)

        // Then
        assertFalse(result.isConnected)
        assertTrue(result.errorMessage!!.contains("404"))
    }

    @Test
    fun `testConnection should return failure for network exception`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.Ollama,
            baseUrl = "http://localhost:11434",
            modelName = "llama2"
        )

        val mockCall = mockk<Call>()
        every { httpClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } throws RuntimeException("Network error")

        // When
        val result = validator.testConnection(config)

        // Then
        assertFalse(result.isConnected)
        assertTrue(result.errorMessage!!.contains("Network error"))
    }

    @Test
    fun `getAvailableModels should return predefined models for OpenAI`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.OpenAI,
            apiKey = "sk-test",
            modelName = "gpt-3.5-turbo"
        )

        // When
        val result = validator.getAvailableModels(config)

        // Then
        assertTrue(result.isNotEmpty())
        assertTrue(result.any { it.id == "gpt-3.5-turbo" })
        assertTrue(result.any { it.id == "gpt-4" })
    }

    @Test
    fun `validateModel should succeed for existing OpenAI model`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.OpenAI,
            apiKey = "sk-test",
            modelName = "gpt-3.5-turbo"
        )

        // When
        val result = validator.validateModel(config, "gpt-3.5-turbo")

        // Then
        assertTrue(result.isValid)
        assertTrue(result.supportedCapabilities.isNotEmpty())
    }

    @Test
    fun `validateModel should fail for non-existing model`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.OpenAI,
            apiKey = "sk-test",
            modelName = "gpt-3.5-turbo"
        )

        // When
        val result = validator.validateModel(config, "non-existing-model")

        // Then
        assertFalse(result.isValid)
        assertTrue(result.errorMessage!!.contains("not found"))
    }
}