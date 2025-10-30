package com.voicenotesai.data.ai

import com.voicenotesai.data.model.AIProvider
import com.voicenotesai.data.repository.AIRepository
import com.voicenotesai.domain.ai.*
import com.voicenotesai.domain.model.AudioData
import com.voicenotesai.domain.model.Language
import com.voicenotesai.domain.security.EncryptionService
import com.voicenotesai.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of the AI processing engine supporting multiple AI providers
 * and advanced features like speaker identification, content analysis, and entity extraction.
 */
@Singleton
class AIProcessingEngineImpl @Inject constructor(
    private val aiRepository: AIRepository,
    private val encryptionService: EncryptionService,
    private val transcriptionService: TranscriptionService,
    private val entityExtractionService: EntityExtractionService,
    private val contentAnalysisService: ContentAnalysisService,
    private val noteCategorizationService: NoteCategorizationService,
    private val languageService: LanguageService,
    private val fallbackProcessingService: FallbackProcessingService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AIProcessingEngine {

    companion object {
        private const val MAX_AUDIO_SIZE_MB = 25 // OpenAI Whisper limit
        private const val MAX_TEXT_LENGTH = 100000 // Reasonable limit for processing
    }

    override suspend fun transcribeAudio(
        audioData: AudioData,
        config: TranscriptionConfig
    ): TranscriptionResult = withContext(ioDispatcher) {
        try {
            // Validate audio size
            val audioSizeMB = audioData.data.size / (1024 * 1024)
            if (audioSizeMB > MAX_AUDIO_SIZE_MB) {
                return@withContext TranscriptionResult(
                    success = false,
                    error = "Audio file too large. Maximum size is ${MAX_AUDIO_SIZE_MB}MB"
                )
            }

            // Use fallback processing for network-based transcription with local fallback
            return@withContext fallbackProcessingService.transcribeWithFallback(
                audioData = audioData,
                config = config,
                networkTranscription = {
                    // Encrypt audio data for security
                    val encryptedAudio = encryptionService.encryptAudio(audioData.data)
                    
                    val startTime = System.currentTimeMillis()
                    
                    // Perform network-based transcription
                    val result = when (config.model) {
                        is AIModel.Whisper -> transcriptionService.transcribeWithWhisper(
                            encryptedAudio, config
                        )
                        is AIModel.LocalWhisper -> transcriptionService.transcribeWithLocalWhisper(
                            encryptedAudio, config
                        )
                        else -> TranscriptionResult(
                            success = false,
                            error = "Unsupported transcription model: ${config.model}"
                        )
                    }
                    
                    val processingTime = System.currentTimeMillis() - startTime
                    result.copy(processingTimeMs = processingTime)
                }
            )
            
        } catch (e: Exception) {
            TranscriptionResult(
                success = false,
                error = "Transcription failed: ${e.message}"
            )
        }
    }

    override suspend fun generateNotes(
        transcript: String,
        format: NoteFormat
    ): NoteGenerationResult = withContext(ioDispatcher) {
        try {
            if (transcript.length > MAX_TEXT_LENGTH) {
                return@withContext NoteGenerationResult(
                    success = false,
                    format = format,
                    error = "Text too long for processing. Maximum length is $MAX_TEXT_LENGTH characters"
                )
            }

            // Use fallback processing for note generation with local fallback
            return@withContext fallbackProcessingService.generateNotesWithFallback(
                transcript = transcript,
                format = format,
                networkGeneration = {
                    val startTime = System.currentTimeMillis()
                    
                    // Generate prompt based on format
                    val prompt = generatePromptForFormat(format, transcript)
                    
                    // Use the existing AI repository for note generation
                    // This will use the user's configured AI provider and settings
                    val result = aiRepository.generateNotes(
                        provider = AIProvider.OPENAI, // Default, should be configurable
                        apiKey = "", // Should come from settings
                        model = "gpt-4", // Should be configurable
                        transcribedText = transcript,
                        promptTemplate = prompt
                    )
                    
                    val processingTime = System.currentTimeMillis() - startTime
                    
                    if (result.isSuccess) {
                        val notes = result.getOrNull() ?: ""
                        val metadata = generateNoteMetadata(notes, format, processingTime)
                        
                        NoteGenerationResult(
                            success = true,
                            notes = notes,
                            format = format,
                            metadata = metadata
                        )
                    } else {
                        NoteGenerationResult(
                            success = false,
                            format = format,
                            error = result.exceptionOrNull()?.message ?: "Unknown error"
                        )
                    }
                }
            )
            
        } catch (e: Exception) {
            NoteGenerationResult(
                success = false,
                format = format,
                error = "Note generation failed: ${e.message}"
            )
        }
    }

    override suspend fun analyzeContent(content: String): ContentAnalysis = withContext(ioDispatcher) {
        try {
            if (content.length > MAX_TEXT_LENGTH) {
                return@withContext ContentAnalysis(
                    success = false,
                    error = "Content too long for analysis. Maximum length is $MAX_TEXT_LENGTH characters"
                )
            }

            val sentiment = contentAnalysisService.analyzeSentiment(content)
            val themes = contentAnalysisService.extractThemes(content)
            val categories = contentAnalysisService.categorizeContent(content)
            val complexity = contentAnalysisService.assessComplexity(content)

            ContentAnalysis(
                success = true,
                sentiment = sentiment,
                themes = themes,
                categories = categories,
                complexity = complexity
            )
            
        } catch (e: Exception) {
            ContentAnalysis(
                success = false,
                error = "Content analysis failed: ${e.message}"
            )
        }
    }

    override suspend fun extractEntities(text: String): EntityExtractionResult = withContext(ioDispatcher) {
        try {
            if (text.length > MAX_TEXT_LENGTH) {
                return@withContext EntityExtractionResult(
                    success = false,
                    error = "Text too long for entity extraction. Maximum length is $MAX_TEXT_LENGTH characters"
                )
            }

            val entities = entityExtractionService.extractEntities(text)
            
            EntityExtractionResult(
                success = true,
                entities = entities
            )
            
        } catch (e: Exception) {
            EntityExtractionResult(
                success = false,
                error = "Entity extraction failed: ${e.message}"
            )
        }
    }

    override suspend fun detectLanguage(text: String): LanguageDetectionResult = withContext(ioDispatcher) {
        try {
            // Use enhanced language detection for better accuracy
            val result = languageService.detectLanguageEnhanced(text)
            result
            
        } catch (e: Exception) {
            LanguageDetectionResult(
                success = false,
                error = "Language detection failed: ${e.message}"
            )
        }
    }

    override suspend fun translateText(
        text: String,
        targetLanguage: Language
    ): TranslationResult = withContext(ioDispatcher) {
        try {
            if (text.length > MAX_TEXT_LENGTH) {
                return@withContext TranslationResult(
                    success = false,
                    targetLanguage = targetLanguage,
                    error = "Text too long for translation. Maximum length is $MAX_TEXT_LENGTH characters"
                )
            }

            // Use enhanced translation with automatic source language detection
            val result = languageService.translateTextEnhanced(text, targetLanguage)
            result
            
        } catch (e: Exception) {
            TranslationResult(
                success = false,
                targetLanguage = targetLanguage,
                error = "Translation failed: ${e.message}"
            )
        }
    }

    /**
     * Generates appropriate prompt template based on note format.
     */
    private fun generatePromptForFormat(format: NoteFormat, transcript: String): String {
        return when (format) {
            is NoteFormat.BulletPoints -> """
                Convert the following transcription into clear, organized bullet points. Follow these rules:
                1. Group related information under appropriate headings
                2. Use bullet points for main ideas and sub-bullets for details
                3. Fix any grammatical errors from speech-to-text
                4. Preserve all important information
                5. Make it scannable and easy to read
                
                Transcription: {transcription}
            """.trimIndent()
            
            is NoteFormat.Summary -> """
                Create a concise summary of the following transcription. Follow these rules:
                1. Capture the main points and key insights
                2. Use clear, professional language
                3. Organize information logically
                4. Include important details but avoid redundancy
                5. Aim for 20-30% of the original length
                
                Transcription: {transcription}
            """.trimIndent()
            
            is NoteFormat.ActionItems -> """
                Extract and organize action items from the following transcription. Follow these rules:
                1. Identify specific tasks, decisions, and next steps
                2. Include responsible parties when mentioned
                3. Note any deadlines or timeframes
                4. Use clear, actionable language
                5. Prioritize items when possible
                
                Format as:
                ## Action Items
                - [ ] Task description (Owner: Name, Due: Date)
                
                Transcription: {transcription}
            """.trimIndent()
            
            is NoteFormat.MeetingMinutes -> """
                Convert the following transcription into professional meeting minutes. Follow these rules:
                1. Include attendees if mentioned
                2. Organize by agenda items or topics discussed
                3. Capture key decisions and outcomes
                4. List action items with owners and deadlines
                5. Note any follow-up meetings or next steps
                
                Format:
                ## Meeting Summary
                ## Key Discussions
                ## Decisions Made
                ## Action Items
                ## Next Steps
                
                Transcription: {transcription}
            """.trimIndent()
            
            is NoteFormat.Outline -> """
                Create a structured outline from the following transcription. Follow these rules:
                1. Use hierarchical numbering (1., 1.1, 1.1.1)
                2. Organize information logically by topic
                3. Include main points and supporting details
                4. Maintain clear structure and flow
                5. Fix grammatical errors from speech-to-text
                
                Transcription: {transcription}
            """.trimIndent()
            
            is NoteFormat.KeyInsights -> """
                Extract key insights and important takeaways from the following transcription. Follow these rules:
                1. Identify the most valuable information
                2. Highlight patterns, trends, or important observations
                3. Include relevant quotes or specific details
                4. Organize insights by importance or theme
                5. Provide context where helpful
                
                Transcription: {transcription}
            """.trimIndent()
            
            is NoteFormat.Custom -> format.template.replace("{instructions}", format.instructions)
        }
    }

    /**
     * Generates metadata about the generated notes.
     */
    private fun generateNoteMetadata(
        notes: String,
        format: NoteFormat,
        processingTimeMs: Long
    ): NoteMetadata {
        val wordCount = notes.split("\\s+".toRegex()).size
        val estimatedReadingTime = (wordCount / 200).coerceAtLeast(1) // 200 WPM average
        
        // Extract key topics (simple implementation)
        val keyTopics = extractKeyTopics(notes)
        
        // Count action items
        val actionItemsCount = countActionItems(notes)
        
        return NoteMetadata(
            wordCount = wordCount,
            estimatedReadingTimeMinutes = estimatedReadingTime,
            keyTopics = keyTopics,
            actionItemsCount = actionItemsCount,
            generationTimeMs = processingTimeMs
        )
    }

    /**
     * Simple key topic extraction based on common patterns.
     */
    private fun extractKeyTopics(notes: String): List<String> {
        val topics = mutableSetOf<String>()
        
        // Extract headings (lines starting with #)
        val headingRegex = "^#+\\s*(.+)$".toRegex(RegexOption.MULTILINE)
        headingRegex.findAll(notes).forEach { match ->
            topics.add(match.groupValues[1].trim())
        }
        
        // Extract capitalized phrases (potential topics)
        val capitalizedRegex = "\\b[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*\\b".toRegex()
        capitalizedRegex.findAll(notes).take(10).forEach { match ->
            val topic = match.value.trim()
            if (topic.length > 3 && !topic.matches("^(The|This|That|And|But|For|With|From).*".toRegex())) {
                topics.add(topic)
            }
        }
        
        return topics.take(5).toList()
    }

    /**
     * Counts action items in the notes.
     */
    private fun countActionItems(notes: String): Int {
        val actionItemPatterns = listOf(
            "- \\[ \\]".toRegex(), // Checkbox format
            "TODO:".toRegex(RegexOption.IGNORE_CASE),
            "Action:".toRegex(RegexOption.IGNORE_CASE),
            "Next step:".toRegex(RegexOption.IGNORE_CASE)
        )
        
        return actionItemPatterns.sumOf { pattern ->
            pattern.findAll(notes).count()
        }
    }
}