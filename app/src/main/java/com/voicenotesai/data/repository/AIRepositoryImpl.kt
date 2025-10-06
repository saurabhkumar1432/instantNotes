package com.voicenotesai.data.repository

import com.google.gson.Gson
import com.voicenotesai.data.model.AIProvider
import com.voicenotesai.data.remote.api.AnthropicService
import com.voicenotesai.data.remote.api.GoogleAIService
import com.voicenotesai.data.remote.api.OpenAIService
import com.voicenotesai.data.remote.model.AnthropicContent
import com.voicenotesai.data.remote.model.AnthropicErrorResponse
import com.voicenotesai.data.remote.model.AnthropicMessage
import com.voicenotesai.data.remote.model.AnthropicRequest
import com.voicenotesai.data.remote.model.GoogleAIContent
import com.voicenotesai.data.remote.model.GoogleAIErrorResponse
import com.voicenotesai.data.remote.model.GoogleAIPart
import com.voicenotesai.data.remote.model.GoogleAIRequest
import com.voicenotesai.data.remote.model.OpenAIErrorResponse
import com.voicenotesai.data.remote.model.OpenAIMessage
import com.voicenotesai.data.remote.model.OpenAIRequest
import com.voicenotesai.di.IoDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class AIRepositoryImpl @Inject constructor(
    private val openAIService: OpenAIService,
    private val anthropicService: AnthropicService,
    private val googleAIService: GoogleAIService,
    private val gson: Gson,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AIRepository {

    // Track in-flight requests to prevent duplicates
    private val inFlightRequests = mutableMapOf<String, kotlinx.coroutines.Deferred<Result<String>>>()
    private val requestMutex = kotlinx.coroutines.sync.Mutex()
    
    // Rate limiting
    private var lastRequestTime = 0L
    private val rateLimitMutex = kotlinx.coroutines.sync.Mutex()

    companion object {
        private const val VALIDATION_TIMEOUT_MILLIS = 15_000L
        private const val VALIDATION_TEST_TEXT = "Hello, this is a test."
        private const val MIN_REQUEST_INTERVAL_MS = 1000L // Minimum 1 second between requests
        
        // Retry configuration
        private const val MAX_RETRIES = 3
        private const val INITIAL_BACKOFF_MS = 1000L
        private val RETRYABLE_ERROR_CODES = setOf(500, 502, 503, 504)
    }

    override suspend fun generateNotes(
        provider: AIProvider,
        apiKey: String,
        model: String,
        transcribedText: String,
        promptTemplate: String
    ): Result<String> = withContext(ioDispatcher) {
        // Apply rate limiting
        rateLimitMutex.lock()
        val currentTime = System.currentTimeMillis()
        val timeSinceLastRequest = currentTime - lastRequestTime
        if (timeSinceLastRequest < MIN_REQUEST_INTERVAL_MS) {
            val delayTime = MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest
            rateLimitMutex.unlock()
            kotlinx.coroutines.delay(delayTime)
            rateLimitMutex.lock()
        }
        lastRequestTime = System.currentTimeMillis()
        rateLimitMutex.unlock()
        
        // Create a unique key for this request
        val requestKey = "${provider}_${model}_${transcribedText.hashCode()}"
        
        // Check if same request is already in flight
        requestMutex.lock()
        val existingRequest = inFlightRequests[requestKey]
        if (existingRequest != null && existingRequest.isActive) {
            requestMutex.unlock()
            return@withContext existingRequest.await()
        }
        requestMutex.unlock()
        
        // Create new deferred for this request
        val deferred = CoroutineScope(ioDispatcher).async {
            try {
                // Format prompt with transcription
                val formattedPrompt = promptTemplate.replace("{transcription}", transcribedText)
                
                // Retry logic for transient failures
                retryWithExponentialBackoff {
                    when (provider) {
                        AIProvider.OPENAI -> generateWithOpenAI(apiKey, model, formattedPrompt)
                        AIProvider.ANTHROPIC -> generateWithAnthropic(apiKey, model, formattedPrompt)
                        AIProvider.GOOGLE_AI -> generateWithGoogleAI(apiKey, model, formattedPrompt)
                    }
                }
            } catch (e: IOException) {
                Result.failure(Exception("Network error: ${e.message}"))
            } catch (e: Exception) {
                Result.failure(Exception("Error generating notes: ${e.message}"))
            } finally {
                // Clean up request tracking
                requestMutex.lock()
                inFlightRequests.remove(requestKey)
                requestMutex.unlock()
            }
        }
        
        // Track this request
        requestMutex.lock()
        inFlightRequests[requestKey] = deferred
        requestMutex.unlock()
        
        return@withContext deferred.await()
    }

    private suspend fun generateWithOpenAI(
        apiKey: String,
        model: String,
        formattedPrompt: String
    ): Result<String> {
        val request = OpenAIRequest(
            model = model,
            messages = listOf(
                OpenAIMessage(role = "system", content = "You are a helpful assistant that converts transcribed speech into organized notes."),
                OpenAIMessage(role = "user", content = formattedPrompt)
            ),
            temperature = 0.7
        )

        val response = openAIService.generateCompletion(
            authorization = "Bearer $apiKey",
            request = request
        )

        return handleOpenAIResponse(response)
    }

    private suspend fun generateWithAnthropic(
        apiKey: String,
        model: String,
        formattedPrompt: String
    ): Result<String> {
        val request = AnthropicRequest(
            model = model,
            messages = listOf(
                AnthropicMessage(role = "user", content = formattedPrompt)
            ),
            maxTokens = 1024,
            temperature = 0.7
        )

        val response = anthropicService.generateMessage(
            apiKey = apiKey,
            request = request
        )

        return handleAnthropicResponse(response)
    }

    private suspend fun generateWithGoogleAI(
        apiKey: String,
        model: String,
        formattedPrompt: String
    ): Result<String> {
        val request = GoogleAIRequest(
            contents = listOf(
                GoogleAIContent(
                    parts = listOf(GoogleAIPart(text = formattedPrompt)),
                    role = "user"
                )
            )
        )

        val response = googleAIService.generateContent(
            model = model,
            apiKey = apiKey,
            request = request
        )

        return handleGoogleAIResponse(response)
    }

    private fun handleOpenAIResponse(response: Response<com.voicenotesai.data.remote.model.OpenAIResponse>): Result<String> {
        return when {
            response.isSuccessful -> {
                val body = response.body()
                val content = body?.choices?.firstOrNull()?.message?.content
                if (content != null) {
                    Result.success(content)
                } else {
                    Result.failure(Exception("Empty response from OpenAI"))
                }
            }
            response.code() == 401 -> {
                Result.failure(Exception("Invalid API key. Please check your settings."))
            }
            response.code() == 429 -> {
                Result.failure(Exception("Rate limit exceeded. Please try again later."))
            }
            response.code() == 400 -> {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    gson.fromJson(errorBody, OpenAIErrorResponse::class.java).error.message
                } catch (e: Exception) {
                    "Bad request"
                }
                Result.failure(Exception("API error: $errorMessage"))
            }
            else -> {
                Result.failure(Exception("API error: ${response.code()} - ${response.message()}"))
            }
        }
    }

    private fun handleAnthropicResponse(response: Response<com.voicenotesai.data.remote.model.AnthropicResponse>): Result<String> {
        return when {
            response.isSuccessful -> {
                val body = response.body()
                val content = body?.content?.firstOrNull { it.type == "text" }?.text
                if (content != null) {
                    Result.success(content)
                } else {
                    Result.failure(Exception("Empty response from Anthropic"))
                }
            }
            response.code() == 401 -> {
                Result.failure(Exception("Invalid API key. Please check your settings."))
            }
            response.code() == 429 -> {
                Result.failure(Exception("Rate limit exceeded. Please try again later."))
            }
            response.code() == 400 -> {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    gson.fromJson(errorBody, AnthropicErrorResponse::class.java).error.message
                } catch (e: Exception) {
                    "Bad request"
                }
                Result.failure(Exception("API error: $errorMessage"))
            }
            else -> {
                Result.failure(Exception("API error: ${response.code()} - ${response.message()}"))
            }
        }
    }

    private fun handleGoogleAIResponse(response: Response<com.voicenotesai.data.remote.model.GoogleAIResponse>): Result<String> {
        return when {
            response.isSuccessful -> {
                val body = response.body()
                val content = body?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (content != null) {
                    Result.success(content)
                } else {
                    Result.failure(Exception("Empty response from Google AI"))
                }
            }
            response.code() == 401 -> {
                Result.failure(Exception("Invalid API key. Please check your settings."))
            }
            response.code() == 429 -> {
                Result.failure(Exception("Rate limit exceeded. Please try again later."))
            }
            response.code() == 400 -> {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    gson.fromJson(errorBody, GoogleAIErrorResponse::class.java).error.message
                } catch (e: Exception) {
                    "Bad request"
                }
                Result.failure(Exception("API error: $errorMessage"))
            }
            else -> {
                Result.failure(Exception("API error: ${response.code()} - ${response.message()}"))
            }
        }
    }
    
    override suspend fun validateApiKeyAndModel(
        provider: AIProvider,
        apiKey: String,
        model: String
    ): Result<String> = withContext(ioDispatcher) {
        return@withContext try {
            // Use shorter timeout for validation
            withTimeout(VALIDATION_TIMEOUT_MILLIS) {
                when (provider) {
                    AIProvider.OPENAI -> validateOpenAI(apiKey, model)
                    AIProvider.ANTHROPIC -> validateAnthropic(apiKey, model)
                    AIProvider.GOOGLE_AI -> validateGoogleAI(apiKey, model)
                }
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: Please check your internet connection"))
        } catch (e: Exception) {
            Result.failure(Exception("Validation failed: ${e.message}"))
        }
    }
    
    private suspend fun validateOpenAI(apiKey: String, model: String): Result<String> {
        val request = OpenAIRequest(
            model = model,
            messages = listOf(
                OpenAIMessage(role = "user", content = VALIDATION_TEST_TEXT)
            ),
            temperature = 0.7,
            maxTokens = 10
        )

        val response = openAIService.generateCompletion(
            authorization = "Bearer $apiKey",
            request = request
        )

        return when {
            response.isSuccessful -> {
                val body = response.body()
                if (body?.choices?.isNotEmpty() == true) {
                    Result.success("✓ API key and model validated successfully")
                } else {
                    Result.failure(Exception("Invalid response from API"))
                }
            }
            response.code() == 401 -> {
                Result.failure(Exception("Invalid API key"))
            }
            response.code() == 404 -> {
                Result.failure(Exception("Model '$model' not found or not accessible"))
            }
            response.code() == 429 -> {
                Result.failure(Exception("Rate limit exceeded. Please try again later"))
            }
            response.code() == 400 -> {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    gson.fromJson(errorBody, OpenAIErrorResponse::class.java).error.message
                } catch (e: Exception) {
                    "Invalid model name or request format"
                }
                Result.failure(Exception(errorMessage))
            }
            else -> {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        }
    }
    
    private suspend fun validateAnthropic(apiKey: String, model: String): Result<String> {
        val request = AnthropicRequest(
            model = model,
            messages = listOf(
                AnthropicMessage(role = "user", content = VALIDATION_TEST_TEXT)
            ),
            maxTokens = 10,
            temperature = 0.7
        )

        val response = anthropicService.generateMessage(
            apiKey = apiKey,
            request = request
        )

        return when {
            response.isSuccessful -> {
                val body = response.body()
                if (body?.content?.isNotEmpty() == true) {
                    Result.success("✓ API key and model validated successfully")
                } else {
                    Result.failure(Exception("Invalid response from API"))
                }
            }
            response.code() == 401 -> {
                Result.failure(Exception("Invalid API key"))
            }
            response.code() == 404 -> {
                Result.failure(Exception("Model '$model' not found or not accessible"))
            }
            response.code() == 429 -> {
                Result.failure(Exception("Rate limit exceeded. Please try again later"))
            }
            response.code() == 400 -> {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    gson.fromJson(errorBody, AnthropicErrorResponse::class.java).error.message
                } catch (e: Exception) {
                    "Invalid model name or request format"
                }
                Result.failure(Exception(errorMessage))
            }
            else -> {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        }
    }
    
    private suspend fun validateGoogleAI(apiKey: String, model: String): Result<String> {
        val request = GoogleAIRequest(
            contents = listOf(
                GoogleAIContent(
                    parts = listOf(GoogleAIPart(text = VALIDATION_TEST_TEXT)),
                    role = "user"
                )
            )
        )

        val response = googleAIService.generateContent(
            model = model,
            apiKey = apiKey,
            request = request
        )

        return when {
            response.isSuccessful -> {
                val body = response.body()
                if (body?.candidates?.isNotEmpty() == true) {
                    Result.success("✓ API key and model validated successfully")
                } else {
                    Result.failure(Exception("Invalid response from API"))
                }
            }
            response.code() == 401 || response.code() == 403 -> {
                Result.failure(Exception("Invalid API key"))
            }
            response.code() == 404 -> {
                Result.failure(Exception("Model '$model' not found or not accessible"))
            }
            response.code() == 429 -> {
                Result.failure(Exception("Rate limit exceeded. Please try again later"))
            }
            response.code() == 400 -> {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    gson.fromJson(errorBody, GoogleAIErrorResponse::class.java).error.message
                } catch (e: Exception) {
                    "Invalid model name or request format"
                }
                Result.failure(Exception(errorMessage))
            }
            else -> {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        }
    }
    
    /**
     * Retries the given block with exponential backoff for transient failures.
     * Retries on network errors (5xx) and timeouts, but not on client errors (4xx).
     */
    private suspend fun <T> retryWithExponentialBackoff(block: suspend () -> Result<T>): Result<T> {
        var currentDelay = INITIAL_BACKOFF_MS
        var lastException: Exception? = null
        
        repeat(MAX_RETRIES) { attempt ->
            try {
                val result = block()
                
                // If successful or client error (4xx), return immediately
                if (result.isSuccess) {
                    return result
                }
                
                // Check if error is retryable
                val exception = result.exceptionOrNull()
                val errorMessage = exception?.message ?: ""
                val isRetryable = errorMessage.contains("500") ||
                                errorMessage.contains("502") ||
                                errorMessage.contains("503") ||
                                errorMessage.contains("timeout", ignoreCase = true) ||
                                errorMessage.contains("Network error")
                
                if (!isRetryable || attempt == MAX_RETRIES - 1) {
                    return result
                }
                
                lastException = exception as? Exception
                
            } catch (e: Exception) {
                lastException = e
                
                // Don't retry on last attempt
                if (attempt == MAX_RETRIES - 1) {
                    return Result.failure(e)
                }
            }
            
            // Wait before retry with exponential backoff
            kotlinx.coroutines.delay(currentDelay)
            currentDelay *= 2
        }
        
        return Result.failure(lastException ?: Exception("Unknown error after retries"))
    }
}
