package com.voicenotesai.data.ai

import com.voicenotesai.data.model.AIConfiguration
import com.voicenotesai.data.model.AIProviderType
import com.voicenotesai.domain.ai.ConnectionResult
import com.voicenotesai.domain.ai.ValidationResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for AI configuration validation and connection testing.
 * 
 * Tests validation logic for different AI providers and connection testing.
 */
class AIConfigurationValidationTest {
    
    private lateinit var validator: AIConfigurationValidatorImpl
    private lateinit var httpClient: OkHttpClient
    private lateinit var call: Call
    private lateinit var response: Response
    private lateinit var responseBody: ResponseBody
    
    @Before
    fun setup() {
        httpClient = mockk()
        call = mockk()
        response = mockk()
        responseBody = mockk()
        
        validator = AIConfigurationValidatorImpl(httpClient)
    }
    
    @Test
    fun `validateConfiguration should validate OpenAI config correctly`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.OpenAI,
            apiKey = "sk-test-key-123",
            modelName = "gpt-3.5-turbo"
        )
        
        // When
        val result = validator.validateConfiguration(config)
        
        // Then
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
        assertTrue(result.supportedCapabilities.isNotEmpty())
    }
    
    @Test
    fun `validateConfiguration should reject OpenAI config with invalid API key format`() = runTest {
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
        assertTrue(result.errorMessage?.contains("API key") == true)
    }
    
    @Test
    fun `validateConfiguration should reject OpenAI config with empty API key`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.OpenAI,
            apiKey = "",
            modelName = "gpt-3.5-turbo"
        )
        
        // When
        val result = validator.validateConfiguration(config)
        
        // Then
        assertFalse(result.isValid)
        assertTrue(result.errorMessage?.contains("API key is required") == true)
    }
    
    @Test
    fun `validateConfiguration should validate Anthropic config correctly`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.Anthropic,
            apiKey = "sk-ant-test-key",
            modelName = "claude-3-sonnet"
        )
        
        // When
        val result = validator.validateConfiguration(config)
        
        // Then
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }
    
    @Test
    fun `validateConfiguration should validate Google AI config correctly`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.GoogleAI,
            apiKey = "AIza-test-key",
            modelName = "gemini-pro"
        )
        
        // When
        val result = validator.validateConfiguration(config)
        
        // Then
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }
    
    @Test
    fun `validateConfiguration should validate Ollama config correctly`() = runTest {
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
        assertNull(result.errorMessage)
    }
    
    @Test
    fun `validateConfiguration should reject Ollama config with invalid URL`() = runTest {
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
        assertTrue(result.errorMessage?.contains("Invalid base URL") == true)
    }
    
    @Test
    fun `validateConfiguration should validate LM Studio config correctly`() = runTest {
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
        assertNull(result.errorMessage)
    }
    
    @Test
    fun `validateConfiguration should validate Custom config correctly`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.Custom("MyProvider"),
            baseUrl = "https://api.example.com",
            apiKey = "custom-key",
            modelName = "custom-model",
            customHeaders = mapOf("X-Custom-Header" to "value")
        )
        
        // When
        val result = validator.validateConfiguration(config)
        
        // Then
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }
    
    @Test
    fun `testConnection should return success for valid OpenAI connection`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.OpenAI,
            apiKey = "sk-test-key",
            modelName = "gpt-3.5-turbo"
        )
        
        coEvery { httpClient.newCall(any()) } returns call
        coEvery { call.execute() } returns response
        coEvery { response.isSuccessful } returns true
        coEvery { response.body } returns responseBody
        coEvery { responseBody.string() } returns """{"data": [{"id": "gpt-3.5-turbo"}]}"""
        
        // When
        val result = validator.testConnection(config)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.responseTimeMs > 0)
        assertNull(result.errorMessage)
    }
    
    @Test
    fun `testConnection should return failure for invalid API key`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.OpenAI,
            apiKey = "invalid-key",
            modelName = "gpt-3.5-turbo"
        )
        
        coEvery { httpClient.newCall(any()) } returns call
        coEvery { call.execute() } returns response
        coEvery { response.isSuccessful } returns false
        coEvery { response.code } returns 401
        coEvery { response.body } returns responseBody
        coEvery { responseBody.string() } returns """{"error": {"message": "Invalid API key"}}"""
        
        // When
        val result = validator.testConnection(config)
        
        // Then
        assertFalse(result.isSuccess)
        assertTrue(result.errorMessage?.contains("Invalid API key") == true)
    }
    
    @Test
    fun `testConnection should handle network timeout`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.OpenAI,
            apiKey = "sk-test-key",
            modelName = "gpt-3.5-turbo"
        )
        
        coEvery { httpClient.newCall(any()) } returns call
        coEvery { call.execute() } throws java.net.SocketTimeoutException("Connection timeout")
        
        // When
        val result = validator.testConnection(config)
        
        // Then
        assertFalse(result.isSuccess)
        assertTrue(result.errorMessage?.contains("timeout") == true)
    }
    
    @Test
    fun `testConnection should test Ollama connection correctly`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.Ollama,
            baseUrl = "http://localhost:11434",
            modelName = "llama2"
        )
        
        coEvery { httpClient.newCall(any()) } returns call
        coEvery { call.execute() } returns response
        coEvery { response.isSuccessful } returns true
        coEvery { response.body } returns responseBody
        coEvery { responseBody.string() } returns """{"models": [{"name": "llama2"}]}"""
        
        // When
        val result = validator.testConnection(config)
        
        // Then
        assertTrue(result.isSuccess)
        assertNull(result.errorMessage)
    }
    
    @Test
    fun `testConnection should handle Ollama server not running`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.Ollama,
            baseUrl = "http://localhost:11434",
            modelName = "llama2"
        )
        
        coEvery { httpClient.newCall(any()) } returns call
        coEvery { call.execute() } throws java.net.ConnectException("Connection refused")
        
        // When
        val result = validator.testConnection(config)
        
        // Then
        assertFalse(result.isSuccess)
        assertTrue(result.errorMessage?.contains("Connection refused") == true)
    }
    
    @Test
    fun `testConnection should test LM Studio connection correctly`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.LMStudio,
            baseUrl = "http://localhost:1234",
            modelName = "local-model"
        )
        
        coEvery { httpClient.newCall(any()) } returns call
        coEvery { call.execute() } returns response
        coEvery { response.isSuccessful } returns true
        coEvery { response.body } returns responseBody
        coEvery { responseBody.string() } returns """{"data": [{"id": "local-model"}]}"""
        
        // When
        val result = validator.testConnection(config)
        
        // Then
        assertTrue(result.isSuccess)
        assertNull(result.errorMessage)
    }
    
    @Test
    fun `testConnection should handle custom provider correctly`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.Custom("TestProvider"),
            baseUrl = "https://api.test.com",
            apiKey = "test-key",
            modelName = "test-model",
            customHeaders = mapOf("Authorization" to "Bearer test-key")
        )
        
        coEvery { httpClient.newCall(any()) } returns call
        coEvery { call.execute() } returns response
        coEvery { response.isSuccessful } returns true
        coEvery { response.body } returns responseBody
        coEvery { responseBody.string() } returns """{"status": "ok"}"""
        
        // When
        val result = validator.testConnection(config)
        
        // Then
        assertTrue(result.isSuccess)
        assertNull(result.errorMessage)
    }
    
    @Test
    fun `validateConfiguration should reject config with empty model name`() = runTest {
        // Given
        val config = AIConfiguration(
            provider = AIProviderType.OpenAI,
            apiKey = "sk-test-key",
            modelName = ""
        )
        
        // When
        val result = validator.validateConfiguration(config)
        
        // Then
        assertFalse(result.isValid)
        assertTrue(result.errorMessage?.contains("Model name is required") == true)
    }
    
    @Test
    fun `validateConfiguration should validate OpenRouter config correctly`() = runTest {
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
        assertNull(result.errorMessage)
    }
}