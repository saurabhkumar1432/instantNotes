package com.voicenotesai.data.repository

import com.google.gson.Gson
import com.voicenotesai.data.model.AIProvider
import com.voicenotesai.data.remote.api.AnthropicService
import com.voicenotesai.data.remote.api.GoogleAIService
import com.voicenotesai.data.remote.api.OpenAIService
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AIRepositoryImplTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: AIRepositoryImpl
    private lateinit var openAIService: OpenAIService
    private lateinit var anthropicService: AnthropicService
    private lateinit var googleAIService: GoogleAIService
    private val testDispatcher = StandardTestDispatcher()
    private val gson = Gson()

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .writeTimeout(1, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        openAIService = retrofit.create(OpenAIService::class.java)
        anthropicService = retrofit.create(AnthropicService::class.java)
        googleAIService = retrofit.create(GoogleAIService::class.java)

        repository = AIRepositoryImpl(
            openAIService = openAIService,
            anthropicService = anthropicService,
            googleAIService = googleAIService,
            gson = gson,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // OpenAI Tests
    @Test
    fun `generateNotes with OpenAI should return success when API responds correctly`() = runTest(testDispatcher) {
        // Given
        val mockResponse = """
            {
                "id": "chatcmpl-123",
                "choices": [
                    {
                        "index": 0,
                        "message": {
                            "role": "assistant",
                            "content": "• First bullet point\n• Second bullet point\n• Third bullet point"
                        },
                        "finish_reason": "stop"
                    }
                ]
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json")
        )

        // When
        val result = repository.generateNotes(
            provider = AIProvider.OPENAI,
            apiKey = "test-api-key",
            model = "gpt-4",
            transcribedText = "This is a test transcription"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(result.isSuccess)
        assertEquals("• First bullet point\n• Second bullet point\n• Third bullet point", result.getOrNull())
    }

    @Test
    fun `generateNotes with OpenAI should return failure when API returns 401`() = runTest(testDispatcher) {
        // Given
        val errorResponse = """
            {
                "error": {
                    "message": "Invalid API key",
                    "type": "invalid_request_error",
                    "code": "invalid_api_key"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody(errorResponse)
                .addHeader("Content-Type", "application/json")
        )

        // When
        val result = repository.generateNotes(
            provider = AIProvider.OPENAI,
            apiKey = "invalid-key",
            model = "gpt-4",
            transcribedText = "Test"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid API key") == true)
    }

    @Test
    fun `generateNotes with OpenAI should return failure when API returns 429`() = runTest(testDispatcher) {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody("{}")
                .addHeader("Content-Type", "application/json")
        )

        // When
        val result = repository.generateNotes(
            provider = AIProvider.OPENAI,
            apiKey = "test-key",
            model = "gpt-4",
            transcribedText = "Test"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Rate limit exceeded") == true)
    }

    @Test
    fun `generateNotes with OpenAI should return failure when API returns 400`() = runTest(testDispatcher) {
        // Given
        val errorResponse = """
            {
                "error": {
                    "message": "Invalid request format",
                    "type": "invalid_request_error"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody(errorResponse)
                .addHeader("Content-Type", "application/json")
        )

        // When
        val result = repository.generateNotes(
            provider = AIProvider.OPENAI,
            apiKey = "test-key",
            model = "gpt-4",
            transcribedText = "Test"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid request format") == true)
    }

    // Anthropic Tests
    @Test
    fun `generateNotes with Anthropic should return success when API responds correctly`() = runTest(testDispatcher) {
        // Given
        val mockResponse = """
            {
                "id": "msg_123",
                "type": "message",
                "role": "assistant",
                "content": [
                    {
                        "type": "text",
                        "text": "• Key point one\n• Key point two\n• Key point three"
                    }
                ],
                "model": "claude-3-opus-20240229",
                "stop_reason": "end_turn"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json")
        )

        // When
        val result = repository.generateNotes(
            provider = AIProvider.ANTHROPIC,
            apiKey = "test-api-key",
            model = "claude-3-opus-20240229",
            transcribedText = "This is a test transcription"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(result.isSuccess)
        assertEquals("• Key point one\n• Key point two\n• Key point three", result.getOrNull())
    }

    @Test
    fun `generateNotes with Anthropic should return failure when API returns 401`() = runTest(testDispatcher) {
        // Given
        val errorResponse = """
            {
                "type": "error",
                "error": {
                    "type": "authentication_error",
                    "message": "Invalid API key"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody(errorResponse)
                .addHeader("Content-Type", "application/json")
        )

        // When
        val result = repository.generateNotes(
            provider = AIProvider.ANTHROPIC,
            apiKey = "invalid-key",
            model = "claude-3-opus-20240229",
            transcribedText = "Test"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid API key") == true)
    }

    @Test
    fun `generateNotes with Anthropic should return failure when API returns 429`() = runTest(testDispatcher) {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody("{}")
                .addHeader("Content-Type", "application/json")
        )

        // When
        val result = repository.generateNotes(
            provider = AIProvider.ANTHROPIC,
            apiKey = "test-key",
            model = "claude-3-opus-20240229",
            transcribedText = "Test"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Rate limit exceeded") == true)
    }

    @Test
    fun `generateNotes with Anthropic should return failure when API returns 400`() = runTest(testDispatcher) {
        // Given
        val errorResponse = """
            {
                "type": "error",
                "error": {
                    "type": "invalid_request_error",
                    "message": "Invalid request parameters"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody(errorResponse)
                .addHeader("Content-Type", "application/json")
        )

        // When
        val result = repository.generateNotes(
            provider = AIProvider.ANTHROPIC,
            apiKey = "test-key",
            model = "claude-3-opus-20240229",
            transcribedText = "Test"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid request parameters") == true)
    }

    // Google AI Tests
    @Test
    fun `generateNotes with GoogleAI should return success when API responds correctly`() = runTest(testDispatcher) {
        // Given
        val mockResponse = """
            {
                "candidates": [
                    {
                        "content": {
                            "parts": [
                                {
                                    "text": "• Important detail one\n• Important detail two\n• Important detail three"
                                }
                            ],
                            "role": "model"
                        },
                        "finishReason": "STOP",
                        "index": 0
                    }
                ]
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json")
        )

        // When
        val result = repository.generateNotes(
            provider = AIProvider.GOOGLE_AI,
            apiKey = "test-api-key",
            model = "gemini-pro",
            transcribedText = "This is a test transcription"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(result.isSuccess)
        assertEquals("• Important detail one\n• Important detail two\n• Important detail three", result.getOrNull())
    }

    @Test
    fun `generateNotes with GoogleAI should return failure when API returns 401`() = runTest(testDispatcher) {
        // Given
        val errorResponse = """
            {
                "error": {
                    "code": 401,
                    "message": "API key not valid",
                    "status": "UNAUTHENTICATED"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody(errorResponse)
                .addHeader("Content-Type", "application/json")
        )

        // When
        val result = repository.generateNotes(
            provider = AIProvider.GOOGLE_AI,
            apiKey = "invalid-key",
            model = "gemini-pro",
            transcribedText = "Test"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid API key") == true)
    }

    @Test
    fun `generateNotes with GoogleAI should return failure when API returns 429`() = runTest(testDispatcher) {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody("{}")
                .addHeader("Content-Type", "application/json")
        )

        // When
        val result = repository.generateNotes(
            provider = AIProvider.GOOGLE_AI,
            apiKey = "test-key",
            model = "gemini-pro",
            transcribedText = "Test"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Rate limit exceeded") == true)
    }

    @Test
    fun `generateNotes with GoogleAI should return failure when API returns 400`() = runTest(testDispatcher) {
        // Given
        val errorResponse = """
            {
                "error": {
                    "code": 400,
                    "message": "Invalid request body",
                    "status": "INVALID_ARGUMENT"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody(errorResponse)
                .addHeader("Content-Type", "application/json")
        )

        // When
        val result = repository.generateNotes(
            provider = AIProvider.GOOGLE_AI,
            apiKey = "test-key",
            model = "gemini-pro",
            transcribedText = "Test"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid request body") == true)
    }

    // Network Error Tests
    @Test
    fun `generateNotes should handle network errors gracefully`() = runTest(testDispatcher) {
        // Given - server will not respond (connection refused)
        mockWebServer.shutdown()

        // When
        val result = repository.generateNotes(
            provider = AIProvider.OPENAI,
            apiKey = "test-key",
            model = "gpt-4",
            transcribedText = "Test"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Network error") == true)
    }

    @Test
    fun `generateNotes should handle empty response body`() = runTest(testDispatcher) {
        // Given
        val mockResponse = """
            {
                "id": "chatcmpl-123",
                "choices": []
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json")
        )

        // When
        val result = repository.generateNotes(
            provider = AIProvider.OPENAI,
            apiKey = "test-key",
            model = "gpt-4",
            transcribedText = "Test"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Empty response") == true)
    }

    @Test
    fun `generateNotes should handle malformed JSON response`() = runTest(testDispatcher) {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{ invalid json }")
                .addHeader("Content-Type", "application/json")
        )

        // When
        val result = repository.generateNotes(
            provider = AIProvider.OPENAI,
            apiKey = "test-key",
            model = "gpt-4",
            transcribedText = "Test"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `generateNotes should handle server errors (500)`() = runTest(testDispatcher) {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
        )

        // When
        val result = repository.generateNotes(
            provider = AIProvider.OPENAI,
            apiKey = "test-key",
            model = "gpt-4",
            transcribedText = "Test"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("API error: 500") == true)
    }

    @Test
    fun `generateNotes should verify correct request headers for OpenAI`() = runTest(testDispatcher) {
        // Given
        val mockResponse = """
            {
                "id": "chatcmpl-123",
                "choices": [
                    {
                        "index": 0,
                        "message": {
                            "role": "assistant",
                            "content": "Test response"
                        },
                        "finish_reason": "stop"
                    }
                ]
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json")
        )

        // When
        repository.generateNotes(
            provider = AIProvider.OPENAI,
            apiKey = "test-api-key",
            model = "gpt-4",
            transcribedText = "Test"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val request = mockWebServer.takeRequest()
        assertEquals("Bearer test-api-key", request.getHeader("Authorization"))
    }

    @Test
    fun `generateNotes should verify correct request headers for Anthropic`() = runTest(testDispatcher) {
        // Given
        val mockResponse = """
            {
                "id": "msg_123",
                "type": "message",
                "role": "assistant",
                "content": [
                    {
                        "type": "text",
                        "text": "Test response"
                    }
                ],
                "model": "claude-3-opus-20240229",
                "stop_reason": "end_turn"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json")
        )

        // When
        repository.generateNotes(
            provider = AIProvider.ANTHROPIC,
            apiKey = "test-api-key",
            model = "claude-3-opus-20240229",
            transcribedText = "Test"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val request = mockWebServer.takeRequest()
        assertEquals("test-api-key", request.getHeader("x-api-key"))
        assertEquals("2023-06-01", request.getHeader("anthropic-version"))
    }
}
