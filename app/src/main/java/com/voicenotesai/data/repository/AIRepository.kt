package com.voicenotesai.data.repository

import com.voicenotesai.data.model.AIProvider

/**
 * Repository interface for AI text generation operations.
 * 
 * Handles communication with various AI providers (OpenAI, Anthropic, Google AI)
 * to convert voice transcriptions into structured notes.
 */
interface AIRepository {
    /**
     * Generates formatted notes from transcribed text using the specified AI provider.
     * 
     * This method includes:
     * - Request deduplication to prevent redundant API calls
     * - Rate limiting to prevent API quota waste
     * - Exponential backoff retry logic for transient failures
     * - Custom prompt template support
     * 
     * @param provider The AI provider to use (OpenAI, Anthropic, or Google AI)
     * @param apiKey The API key for authentication
     * @param model The specific model to use (e.g., "gpt-4", "claude-3")
     * @param transcribedText The raw text transcribed from voice input
     * @param promptTemplate Custom prompt template with {transcription} placeholder
     * @return Result containing the generated notes or error details
     */
    suspend fun generateNotes(
        provider: AIProvider,
        apiKey: String,
        model: String,
        transcribedText: String,
        promptTemplate: String
    ): Result<String>
    
    /**
     * Validates API key and model by making a test API call.
     * 
     * Uses minimal tokens (10-20) to test credentials without significant cost.
     * Validates that:
     * - The API key is valid and authorized
     * - The specified model exists and is accessible
     * - Network connectivity is working
     * 
     * @param provider The AI provider to validate
     * @param apiKey The API key to test
     * @param model The model to validate
     * @return Result with success message or error
     */
    suspend fun validateApiKeyAndModel(
        provider: AIProvider,
        apiKey: String,
        model: String
    ): Result<String>
}
