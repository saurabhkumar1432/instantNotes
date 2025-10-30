package com.voicenotesai.domain.usecase

import com.voicenotesai.data.repository.AIRepository
import com.voicenotesai.data.repository.SettingsRepository
import com.voicenotesai.domain.ai.AIProcessingEngine
import com.voicenotesai.domain.ai.NoteFormat
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Use case that generates AI-powered bullet-point notes from transcribed text.
 * Enhanced with advanced AI processing engine for better note generation.
 * Retrieves AI settings and calls the appropriate AI service.
 */
class GenerateNotesUseCase @Inject constructor(
    private val aiRepository: AIRepository,
    private val settingsRepository: SettingsRepository,
    private val aiProcessingEngine: AIProcessingEngine
) {
    companion object {
        // Maximum characters to send to API (approximately 10,000 tokens)
        private const val MAX_INPUT_LENGTH = 40000
        // Warning threshold for large inputs
        private const val WARNING_THRESHOLD = 30000
    }
    
    /**
     * Generates formatted bullet-point notes from transcribed text using the configured AI provider.
     * 
     * @param transcribedText The text transcribed from voice recording
     * @return Result<String> containing the formatted notes or an error
     */
    suspend operator fun invoke(transcribedText: String): Result<String> {
        return generateNotes(transcribedText, NoteFormat.BulletPoints)
    }
    
    /**
     * Generates formatted notes from transcribed text using the specified format.
     * 
     * @param transcribedText The text transcribed from voice recording
     * @param format The desired note format
     * @return Result<String> containing the formatted notes or an error
     */
    suspend fun generateNotes(transcribedText: String, format: NoteFormat): Result<String> {
        // Validate input
        if (transcribedText.isBlank()) {
            return Result.failure(IllegalArgumentException("Transcribed text cannot be empty"))
        }
        
        // Check and truncate if input is too long
        val processedText = if (transcribedText.length > MAX_INPUT_LENGTH) {
            transcribedText.take(MAX_INPUT_LENGTH) + "\n\n[Note: Text was truncated due to length limitations]"
        } else {
            transcribedText
        }

        // Try using the enhanced AI processing engine first
        try {
            val result = aiProcessingEngine.generateNotes(processedText, format)
            if (result.success) {
                return Result.success(result.notes)
            }
        } catch (e: Exception) {
            // Fall back to legacy implementation if enhanced engine fails
        }

        // Fallback to legacy AI repository implementation
        return generateNotesLegacy(processedText)
    }
    
    /**
     * Legacy note generation using the original AI repository.
     */
    private suspend fun generateNotesLegacy(processedText: String): Result<String> {
        // Retrieve AI settings
        val settings = settingsRepository.getSettings().firstOrNull()
            ?: return Result.failure(IllegalStateException("AI settings not configured. Please configure settings first."))

        // Validate settings
        if (!settingsRepository.hasValidSettings()) {
            return Result.failure(IllegalStateException("AI settings are incomplete. Please check your configuration."))
        }

        // Validate individual fields
        if (settings.apiKey.isBlank()) {
            return Result.failure(IllegalStateException("API key is missing. Please configure your API key in settings."))
        }

        if (settings.model.isBlank()) {
            return Result.failure(IllegalStateException("Model name is missing. Please configure your model in settings."))
        }

        // Call AI repository to generate notes
        return aiRepository.generateNotes(
            provider = settings.provider,
            apiKey = settings.apiKey,
            model = settings.model,
            transcribedText = processedText,
            promptTemplate = settings.promptTemplate
        )
    }
}
