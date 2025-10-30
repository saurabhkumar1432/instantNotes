package com.voicenotesai.domain.usecase

import com.voicenotesai.domain.ai.AIProcessingEngine
import com.voicenotesai.domain.ai.LanguageDetectionResult
import com.voicenotesai.domain.ai.TranslationResult
import com.voicenotesai.domain.model.Language
import javax.inject.Inject

/**
 * Use case for language detection and translation using AI processing engine.
 */
class LanguageProcessingUseCase @Inject constructor(
    private val aiProcessingEngine: AIProcessingEngine
) {
    
    /**
     * Detects the language of the given text.
     */
    suspend fun detectLanguage(text: String): LanguageDetectionResult {
        return aiProcessingEngine.detectLanguage(text)
    }
    
    /**
     * Translates text to the target language.
     */
    suspend fun translateText(
        text: String,
        targetLanguage: Language
    ): TranslationResult {
        return aiProcessingEngine.translateText(text, targetLanguage)
    }
    
    /**
     * Translates text to multiple languages.
     */
    suspend fun translateToMultipleLanguages(
        text: String,
        targetLanguages: List<Language>
    ): Map<Language, TranslationResult> {
        val results = mutableMapOf<Language, TranslationResult>()
        
        targetLanguages.forEach { language ->
            val result = aiProcessingEngine.translateText(text, language)
            results[language] = result
        }
        
        return results
    }
}