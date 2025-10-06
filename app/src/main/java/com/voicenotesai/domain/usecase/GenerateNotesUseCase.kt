package com.voicenotesai.domain.usecase

import com.voicenotesai.data.repository.AIRepository
import com.voicenotesai.data.repository.SettingsRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Use case that generates AI-powered bullet-point notes from transcribed text.
 * Retrieves AI settings and calls the appropriate AI service.
 */
class GenerateNotesUseCase @Inject constructor(
    private val aiRepository: AIRepository,
    private val settingsRepository: SettingsRepository
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
