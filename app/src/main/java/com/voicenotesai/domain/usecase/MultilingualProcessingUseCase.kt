package com.voicenotesai.domain.usecase

import com.voicenotesai.data.ai.MultilingualProcessingResult
import com.voicenotesai.data.ai.MultilingualProcessingService
import com.voicenotesai.domain.ai.AIProcessingEngine
import com.voicenotesai.domain.ai.NoteFormat
import com.voicenotesai.domain.ai.TranscriptionConfig
import com.voicenotesai.domain.model.AudioData
import com.voicenotesai.domain.model.Language
import com.voicenotesai.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for handling multilingual voice note processing with automatic language detection,
 * translation, and language-specific optimizations.
 */
@Singleton
class MultilingualProcessingUseCase @Inject constructor(
    private val aiProcessingEngine: AIProcessingEngine,
    private val multilingualProcessingService: MultilingualProcessingService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    /**
     * Processes audio with automatic language detection and multilingual support.
     */
    suspend fun processMultilingualAudio(
        audioData: AudioData,
        baseConfig: TranscriptionConfig,
        targetLanguage: Language? = null,
        noteFormat: NoteFormat = NoteFormat.BulletPoints
    ): MultilingualAudioProcessingResult = withContext(ioDispatcher) {
        try {
            // Step 1: Initial transcription with language detection
            val initialTranscription = aiProcessingEngine.transcribeAudio(audioData, baseConfig)
            
            if (!initialTranscription.success) {
                return@withContext MultilingualAudioProcessingResult(
                    success = false,
                    error = "Initial transcription failed: ${initialTranscription.error}"
                )
            }

            // Step 2: Detect language from transcription
            val languageDetection = aiProcessingEngine.detectLanguage(initialTranscription.transcript)
            val detectedLanguage = languageDetection.language ?: Language.ENGLISH

            // Step 3: Optimize transcription config for detected language
            val optimizedConfig = multilingualProcessingService.optimizeTranscriptionForLanguage(
                detectedLanguage, baseConfig
            )

            // Step 4: Re-transcribe with optimized config if language was detected with high confidence
            val finalTranscription = if (languageDetection.confidence > 0.7f && detectedLanguage != Language.ENGLISH) {
                aiProcessingEngine.transcribeAudio(audioData, optimizedConfig)
            } else {
                initialTranscription
            }

            if (!finalTranscription.success) {
                return@withContext MultilingualAudioProcessingResult(
                    success = false,
                    error = "Optimized transcription failed: ${finalTranscription.error}"
                )
            }

            // Step 5: Process multilingual content
            val multilingualResult = multilingualProcessingService.processMultilingualContent(
                finalTranscription.transcript,
                targetLanguage
            )

            if (!multilingualResult.success) {
                return@withContext MultilingualAudioProcessingResult(
                    success = false,
                    error = "Multilingual processing failed: ${multilingualResult.error}"
                )
            }

            // Step 6: Generate notes with language-aware formatting
            val contentForNotes = multilingualResult.processedContent
            val notesLanguage = targetLanguage ?: detectedLanguage
            
            val noteGeneration = aiProcessingEngine.generateNotes(contentForNotes, noteFormat)
            
            if (!noteGeneration.success) {
                return@withContext MultilingualAudioProcessingResult(
                    success = false,
                    error = "Note generation failed: ${noteGeneration.error}"
                )
            }

            // Step 7: Apply language-specific formatting
            val formattedNotes = multilingualProcessingService.formatNotesForLanguage(
                noteGeneration.notes,
                notesLanguage,
                noteFormat
            )

            MultilingualAudioProcessingResult(
                success = true,
                originalTranscript = initialTranscription.transcript,
                optimizedTranscript = finalTranscription.transcript,
                detectedLanguage = detectedLanguage,
                languageConfidence = languageDetection.confidence,
                processedContent = multilingualResult.processedContent,
                formattedNotes = formattedNotes,
                detectedLanguages = multilingualResult.detectedLanguages,
                targetLanguage = targetLanguage,
                noteFormat = noteFormat,
                processingTimeMs = finalTranscription.processingTimeMs + noteGeneration.metadata.generationTimeMs
            )

        } catch (e: Exception) {
            MultilingualAudioProcessingResult(
                success = false,
                error = "Multilingual audio processing failed: ${e.message}"
            )
        }
    }

    /**
     * Translates existing note content to a target language.
     */
    suspend fun translateNoteContent(
        content: String,
        targetLanguage: Language,
        sourceLanguage: Language? = null
    ): NoteTranslationResult = withContext(ioDispatcher) {
        try {
            // Detect source language if not provided
            val detectedSourceLanguage = sourceLanguage ?: run {
                val detection = aiProcessingEngine.detectLanguage(content)
                detection.language ?: Language.ENGLISH
            }

            // Translate content
            val translation = aiProcessingEngine.translateText(content, targetLanguage)
            
            if (!translation.success) {
                return@withContext NoteTranslationResult(
                    success = false,
                    error = "Translation failed: ${translation.error}"
                )
            }

            // Process multilingual aspects
            val multilingualResult = multilingualProcessingService.processMultilingualContent(
                translation.translatedText ?: content,
                targetLanguage
            )

            NoteTranslationResult(
                success = true,
                originalContent = content,
                translatedContent = translation.translatedText,
                sourceLanguage = detectedSourceLanguage,
                targetLanguage = targetLanguage,
                confidence = translation.confidence,
                processedContent = multilingualResult.processedContent
            )

        } catch (e: Exception) {
            NoteTranslationResult(
                success = false,
                error = "Note translation failed: ${e.message}"
            )
        }
    }

    /**
     * Gets language-specific processing recommendations.
     */
    suspend fun getLanguageProcessingRecommendations(
        content: String
    ): LanguageProcessingRecommendations = withContext(ioDispatcher) {
        try {
            // Detect content languages
            val multilingualResult = multilingualProcessingService.processMultilingualContent(content)
            
            if (!multilingualResult.success) {
                return@withContext LanguageProcessingRecommendations(
                    success = false,
                    error = "Language analysis failed: ${multilingualResult.error}"
                )
            }

            val detectedLanguages = multilingualResult.detectedLanguages
            val recommendations = mutableListOf<ProcessingRecommendation>()

            // Generate recommendations based on detected languages
            detectedLanguages.forEach { language ->
                when (language) {
                    Language.CHINESE_SIMPLIFIED, Language.CHINESE_TRADITIONAL -> {
                        recommendations.add(
                            ProcessingRecommendation(
                                type = RecommendationType.SEGMENTATION,
                                language = language,
                                description = "Consider using character-based segmentation for better processing",
                                priority = MultilingualRecommendationPriority.HIGH
                            )
                        )
                    }
                    Language.JAPANESE -> {
                        recommendations.add(
                            ProcessingRecommendation(
                                type = RecommendationType.SCRIPT_HANDLING,
                                language = language,
                                description = "Mixed script content detected (Hiragana/Katakana/Kanji). Consider morphological analysis",
                                priority = MultilingualRecommendationPriority.HIGH
                            )
                        )
                    }
                    Language.ARABIC, Language.HEBREW -> {
                        recommendations.add(
                            ProcessingRecommendation(
                                type = RecommendationType.TEXT_DIRECTION,
                                language = language,
                                description = "Right-to-left text detected. Enable RTL support for proper display",
                                priority = MultilingualRecommendationPriority.MEDIUM
                            )
                        )
                    }
                    Language.THAI -> {
                        recommendations.add(
                            ProcessingRecommendation(
                                type = RecommendationType.WORD_BOUNDARY,
                                language = language,
                                description = "Thai text has no word boundaries. Use dictionary-based segmentation",
                                priority = MultilingualRecommendationPriority.HIGH
                            )
                        )
                    }
                    else -> {
                        // No specific recommendations for Latin-based languages
                    }
                }
            }

            // Add translation recommendations if multiple languages detected
            if (detectedLanguages.size > 1) {
                recommendations.add(
                    ProcessingRecommendation(
                        type = RecommendationType.TRANSLATION,
                        language = null,
                        description = "Multiple languages detected. Consider translating to a single target language",
                        priority = MultilingualRecommendationPriority.MEDIUM
                    )
                )
            }

            LanguageProcessingRecommendations(
                success = true,
                detectedLanguages = detectedLanguages,
                recommendations = recommendations,
                isMultilingual = detectedLanguages.size > 1
            )

        } catch (e: Exception) {
            LanguageProcessingRecommendations(
                success = false,
                error = "Language processing analysis failed: ${e.message}"
            )
        }
    }
}

/**
 * Result of multilingual audio processing.
 */
data class MultilingualAudioProcessingResult(
    val success: Boolean,
    val originalTranscript: String = "",
    val optimizedTranscript: String = "",
    val detectedLanguage: Language? = null,
    val languageConfidence: Float = 0f,
    val processedContent: String = "",
    val formattedNotes: String = "",
    val detectedLanguages: List<Language> = emptyList(),
    val targetLanguage: Language? = null,
    val noteFormat: NoteFormat? = null,
    val processingTimeMs: Long = 0,
    val error: String? = null
)

/**
 * Result of note translation.
 */
data class NoteTranslationResult(
    val success: Boolean,
    val originalContent: String = "",
    val translatedContent: String = "",
    val sourceLanguage: Language? = null,
    val targetLanguage: Language? = null,
    val confidence: Float = 0f,
    val processedContent: String = "",
    val error: String? = null
)

/**
 * Language processing recommendations.
 */
data class LanguageProcessingRecommendations(
    val success: Boolean,
    val detectedLanguages: List<Language> = emptyList(),
    val recommendations: List<ProcessingRecommendation> = emptyList(),
    val isMultilingual: Boolean = false,
    val error: String? = null
)

/**
 * Processing recommendation for specific language characteristics.
 */
data class ProcessingRecommendation(
    val type: RecommendationType,
    val language: Language?,
    val description: String,
    val priority: MultilingualRecommendationPriority
)

/**
 * Types of processing recommendations.
 */
enum class RecommendationType {
    SEGMENTATION,
    SCRIPT_HANDLING,
    TEXT_DIRECTION,
    WORD_BOUNDARY,
    TRANSLATION,
    FONT_SELECTION,
    INPUT_METHOD
}

/**
 * Priority levels for multilingual processing recommendations.
 */
enum class MultilingualRecommendationPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}