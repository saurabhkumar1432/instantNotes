package com.voicenotesai.data.ai

import com.voicenotesai.domain.ai.*
import com.voicenotesai.domain.model.AudioData
import com.voicenotesai.domain.model.AudioFormat
import com.voicenotesai.domain.model.Language
import com.voicenotesai.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service that provides fallback processing capabilities when network is unavailable.
 * Integrates local AI processing with the main AI processing engine.
 */
@Singleton
class FallbackProcessingService @Inject constructor(
    private val localAIEngine: LocalAIEngine,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    companion object {
        private const val NETWORK_TIMEOUT_MS = 10000L // 10 seconds
        private const val LOCAL_PROCESSING_TIMEOUT_MS = 30000L // 30 seconds
    }

    /**
     * Attempts transcription with fallback to local processing.
     */
    suspend fun transcribeWithFallback(
        audioData: AudioData,
        config: TranscriptionConfig,
        networkTranscription: suspend () -> TranscriptionResult
    ): TranscriptionResult = withContext(ioDispatcher) {
        
        // First, try network-based transcription with timeout
        val networkResult = withTimeoutOrNull(NETWORK_TIMEOUT_MS) {
            try {
                networkTranscription()
            } catch (e: Exception) {
                null
            }
        }

        // If network transcription succeeded, return it
        if (networkResult?.success == true) {
            return@withContext networkResult
        }

        // Fallback to local processing
        return@withContext processLocally(audioData, config, networkResult?.error)
    }

    /**
     * Attempts note generation with fallback to local processing.
     */
    suspend fun generateNotesWithFallback(
        transcript: String,
        format: NoteFormat,
        networkGeneration: suspend () -> NoteGenerationResult
    ): NoteGenerationResult = withContext(ioDispatcher) {
        
        // First, try network-based generation with timeout
        val networkResult = withTimeoutOrNull(NETWORK_TIMEOUT_MS) {
            try {
                networkGeneration()
            } catch (e: Exception) {
                null
            }
        }

        // If network generation succeeded, return it
        if (networkResult?.success == true) {
            return@withContext networkResult
        }

        // Fallback to local processing
        return@withContext generateNotesLocally(transcript, format, networkResult?.error)
    }

    /**
     * Checks if fallback processing is available.
     */
    fun isFallbackAvailable(): Boolean {
        return localAIEngine.isLocalProcessingAvailable()
    }

    /**
     * Gets fallback processing capabilities.
     */
    fun getFallbackCapabilities(): FallbackCapabilities {
        val localCapabilities = localAIEngine.getModelCapabilities()
        
        return FallbackCapabilities(
            transcriptionAvailable = localCapabilities.transcriptionSupported,
            noteGenerationAvailable = localCapabilities.noteGenerationSupported,
            supportedLanguages = localCapabilities.supportedLanguages,
            maxAudioDurationSeconds = localCapabilities.maxAudioDurationSeconds,
            estimatedAccuracy = localCapabilities.estimatedAccuracy,
            processingSpeed = ProcessingSpeed.MODERATE
        )
    }

    /**
     * Initializes fallback processing capabilities.
     */
    suspend fun initializeFallback(): FallbackInitializationResult = withContext(ioDispatcher) {
        try {
            val initResult = localAIEngine.initializeLocalModels()
            
            FallbackInitializationResult(
                success = initResult.success,
                capabilities = if (initResult.success) getFallbackCapabilities() else null,
                initializationTimeMs = initResult.initializationTimeMs,
                error = initResult.error
            )
            
        } catch (e: Exception) {
            FallbackInitializationResult(
                success = false,
                error = "Failed to initialize fallback processing: ${e.message}"
            )
        }
    }

    /**
     * Processes audio locally when network is unavailable.
     */
    private suspend fun processLocally(
        audioData: AudioData,
        config: TranscriptionConfig,
        networkError: String?
    ): TranscriptionResult {
        
        if (!localAIEngine.isLocalProcessingAvailable()) {
            return TranscriptionResult(
                success = false,
                error = "Network unavailable and local processing not available. ${networkError ?: ""}"
            )
        }

        return withTimeoutOrNull(LOCAL_PROCESSING_TIMEOUT_MS) {
            try {
                val localResult = localAIEngine.processOffline(audioData)
                
                if (localResult.success) {
                    TranscriptionResult(
                        success = true,
                        transcript = localResult.transcription,
                        confidence = localResult.confidence,
                        language = detectLanguageFromConfig(config),
                        processingTimeMs = localResult.processingTimeMs,
                        error = null
                    )
                } else {
                    TranscriptionResult(
                        success = false,
                        error = "Local processing failed: ${localResult.error}. Network error: ${networkError ?: "Unknown"}"
                    )
                }
                
            } catch (e: Exception) {
                TranscriptionResult(
                    success = false,
                    error = "Local processing exception: ${e.message}. Network error: ${networkError ?: "Unknown"}"
                )
            }
        } ?: TranscriptionResult(
            success = false,
            error = "Local processing timeout. Network error: ${networkError ?: "Unknown"}"
        )
    }

    /**
     * Generates notes locally when network is unavailable.
     */
    private suspend fun generateNotesLocally(
        transcript: String,
        format: NoteFormat,
        networkError: String?
    ): NoteGenerationResult {
        
        if (!localAIEngine.isLocalProcessingAvailable()) {
            return NoteGenerationResult(
                success = false,
                format = format,
                error = "Network unavailable and local processing not available. ${networkError ?: ""}"
            )
        }

        return withTimeoutOrNull(LOCAL_PROCESSING_TIMEOUT_MS) {
            try {
                // Create audio data from transcript for local processing
                // This is a workaround since local engine expects audio data
                val simulatedAudioData = AudioData(
                    data = transcript.toByteArray(),
                    format = AudioFormat.WAV, // Use enum value instead of string
                    sampleRate = 16000,
                    channels = 1,
                    durationMs = transcript.length * 50L // Rough estimation
                )
                
                val localResult = localAIEngine.processOffline(simulatedAudioData)
                
                if (localResult.success && localResult.generatedNotes.isNotEmpty()) {
                    NoteGenerationResult(
                        success = true,
                        notes = localResult.generatedNotes,
                        format = format,
                        metadata = NoteMetadata(
                            wordCount = localResult.generatedNotes.split("\\s+".toRegex()).size,
                            estimatedReadingTimeMinutes = (localResult.generatedNotes.length / 1000).coerceAtLeast(1),
                            keyTopics = extractSimpleTopics(localResult.generatedNotes),
                            actionItemsCount = countActionItems(localResult.generatedNotes),
                            generationTimeMs = localResult.processingTimeMs
                        ),
                        error = null
                    )
                } else {
                    // Fallback to simple formatting
                    val formattedNotes = formatTranscriptAsNotes(transcript, format)
                    NoteGenerationResult(
                        success = true,
                        notes = formattedNotes,
                        format = format,
                        metadata = NoteMetadata(
                            wordCount = formattedNotes.split("\\s+".toRegex()).size,
                            estimatedReadingTimeMinutes = (formattedNotes.length / 1000).coerceAtLeast(1),
                            keyTopics = extractSimpleTopics(formattedNotes),
                            actionItemsCount = countActionItems(formattedNotes),
                            generationTimeMs = 500
                        ),
                        error = "Used basic formatting due to local processing limitations"
                    )
                }
                
            } catch (e: Exception) {
                NoteGenerationResult(
                    success = false,
                    format = format,
                    error = "Local note generation failed: ${e.message}. Network error: ${networkError ?: "Unknown"}"
                )
            }
        } ?: NoteGenerationResult(
            success = false,
            format = format,
            error = "Local note generation timeout. Network error: ${networkError ?: "Unknown"}"
        )
    }

    /**
     * Detects language from transcription config.
     */
    private fun detectLanguageFromConfig(config: TranscriptionConfig): Language? {
        return config.language
    }

    /**
     * Formats transcript as notes based on the requested format.
     */
    private fun formatTranscriptAsNotes(transcript: String, format: NoteFormat): String {
        return when (format) {
            is NoteFormat.BulletPoints -> {
                val sentences = transcript.split(". ")
                val bullets = sentences.map { "• ${it.trim()}" }
                "# Voice Notes (Offline)\n\n${bullets.joinToString("\n")}"
            }
            
            is NoteFormat.Summary -> {
                val words = transcript.split(" ")
                val summary = if (words.size > 50) {
                    words.take(50).joinToString(" ") + "..."
                } else transcript
                "# Summary (Offline)\n\n$summary"
            }
            
            is NoteFormat.ActionItems -> {
                val actionWords = listOf("need to", "should", "must", "will", "todo", "action")
                val sentences = transcript.split(". ")
                val actions = sentences.filter { sentence ->
                    actionWords.any { action -> sentence.lowercase().contains(action) }
                }
                val actionItems = actions.map { "- [ ] ${it.trim()}" }
                "# Action Items (Offline)\n\n${actionItems.joinToString("\n")}"
            }
            
            is NoteFormat.MeetingMinutes -> {
                "# Meeting Minutes (Offline)\n\n## Discussion:\n$transcript\n\n## Action Items:\n- Review and organize notes\n- Add specific action items"
            }
            
            else -> {
                "# Voice Notes (Offline)\n\n$transcript"
            }
        }
    }

    /**
     * Extracts simple topics from text.
     */
    private fun extractSimpleTopics(text: String): List<String> {
        val words = text.split("\\s+".toRegex())
        val capitalizedWords = words.filter { 
            it.length > 3 && it[0].isUpperCase() && it.drop(1).all { c -> c.isLowerCase() }
        }
        return capitalizedWords.distinct().take(5)
    }

    /**
     * Counts action items in text.
     */
    private fun countActionItems(text: String): Int {
        val actionPatterns = listOf("- [ ]", "TODO:", "Action:", "Need to")
        return actionPatterns.sumOf { pattern ->
            text.split(pattern).size - 1
        }
    }
}

/**
 * Capabilities of fallback processing.
 */
data class FallbackCapabilities(
    val transcriptionAvailable: Boolean,
    val noteGenerationAvailable: Boolean,
    val supportedLanguages: List<String>,
    val maxAudioDurationSeconds: Int,
    val estimatedAccuracy: Float,
    val processingSpeed: ProcessingSpeed
)

/**
 * Processing speed levels.
 */
enum class ProcessingSpeed {
    FAST,
    MODERATE,
    SLOW
}

/**
 * Result of fallback initialization.
 */
data class FallbackInitializationResult(
    val success: Boolean,
    val capabilities: FallbackCapabilities? = null,
    val initializationTimeMs: Long = 0,
    val error: String? = null
)