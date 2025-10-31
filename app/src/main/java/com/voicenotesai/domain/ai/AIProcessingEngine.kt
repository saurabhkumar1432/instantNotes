package com.voicenotesai.domain.ai

import com.voicenotesai.domain.model.AudioData
import com.voicenotesai.domain.model.Language

/**
 * Advanced AI processing engine interface for multi-model AI operations.
 * Supports transcription, note generation, content analysis, and entity extraction.
 */
interface AIProcessingEngine {
    
    /**
     * Transcribes audio data to text using the specified configuration.
     */
    suspend fun transcribeAudio(
        audioData: AudioData, 
        config: TranscriptionConfig
    ): TranscriptionResult
    
    /**
     * Generates formatted notes from transcript using the specified format.
     */
    suspend fun generateNotes(
        transcript: String, 
        format: NoteFormat
    ): NoteGenerationResult
    
    /**
     * Analyzes content for themes, sentiment, and patterns.
     */
    suspend fun analyzeContent(content: String): ContentAnalysis
    
    /**
     * Extracts entities like dates, names, locations, and tasks from text.
     */
    suspend fun extractEntities(text: String): EntityExtractionResult
    
    /**
     * Detects the language of the given text.
     */
    suspend fun detectLanguage(text: String): LanguageDetectionResult
    
    /**
     * Translates text to the target language.
     */
    suspend fun translateText(
        text: String, 
        targetLanguage: Language
    ): TranslationResult
}

/**
 * Configuration for audio transcription.
 */
data class TranscriptionConfig(
    val model: AIModel,
    val language: Language? = null,
    val speakerIdentification: Boolean = false,
    val realTimeProcessing: Boolean = false,
    val noiseReduction: Boolean = true,
    val customVocabulary: List<String> = emptyList()
)

/**
 * Available AI models for different operations.
 */
sealed class AIModel {
    // OpenAI Models
    object Whisper : AIModel()
    object GPT4 : AIModel()
    object GPT4Turbo : AIModel()
    object GPT35Turbo : AIModel()
    
    // Anthropic Models
    object Claude3Opus : AIModel()
    object Claude3Sonnet : AIModel()
    object Claude3Haiku : AIModel()
    
    // Google AI Models
    object GeminiPro : AIModel()
    object GeminiProVision : AIModel()
    
    // Local Models
    object LocalWhisper : AIModel()
    object LocalLLM : AIModel()
    
    data class Custom(val modelName: String, val provider: String) : AIModel()
}

/**
 * Different note generation formats.
 */
sealed class NoteFormat {
    object BulletPoints : NoteFormat()
    object Summary : NoteFormat()
    object ActionItems : NoteFormat()
    object MeetingMinutes : NoteFormat()
    object Outline : NoteFormat()
    object KeyInsights : NoteFormat()
    data class Custom(val template: String, val instructions: String) : NoteFormat()
}

/**
 * Result of audio transcription operation.
 */
data class TranscriptionResult(
    val success: Boolean,
    val transcript: String = "",
    val speakers: List<SpeakerSegment> = emptyList(),
    val confidence: Float = 0f,
    val language: Language? = null,
    val processingTimeMs: Long = 0,
    val error: String? = null
)

/**
 * Speaker segment for speaker identification.
 */
data class SpeakerSegment(
    val speakerId: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val confidence: Float
)

/**
 * Result of note generation operation.
 */
data class NoteGenerationResult(
    val success: Boolean,
    val notes: String = "",
    val format: NoteFormat,
    val metadata: NoteMetadata = NoteMetadata(),
    val error: String? = null
)

/**
 * Metadata about generated notes.
 */
data class NoteMetadata(
    val wordCount: Int = 0,
    val estimatedReadingTimeMinutes: Int = 0,
    val keyTopics: List<String> = emptyList(),
    val actionItemsCount: Int = 0,
    val generationTimeMs: Long = 0
)

/**
 * Result of content analysis operation.
 */
data class ContentAnalysis(
    val success: Boolean,
    val sentiment: SentimentScore? = null,
    val themes: List<Theme> = emptyList(),
    val categories: List<ContentCategory> = emptyList(),
    val complexity: ComplexityScore? = null,
    val error: String? = null
)

/**
 * Sentiment analysis score.
 */
data class SentimentScore(
    val overall: Float, // -1.0 to 1.0
    val positive: Float,
    val negative: Float,
    val neutral: Float,
    val confidence: Float
)

/**
 * Identified theme in content.
 */
data class Theme(
    val name: String,
    val relevance: Float,
    val keywords: List<String>
)



/**
 * Content complexity assessment.
 */
data class ComplexityScore(
    val readabilityScore: Float, // 0-100, higher = more readable
    val technicalLevel: TechnicalLevel,
    val vocabularyComplexity: Float
)

enum class TechnicalLevel {
    BASIC,
    INTERMEDIATE,
    ADVANCED,
    EXPERT
}

/**
 * Result of entity extraction operation.
 */
data class EntityExtractionResult(
    val success: Boolean,
    val entities: List<ExtractedEntity> = emptyList(),
    val error: String? = null
)

/**
 * Extracted entity from text.
 */
data class ExtractedEntity(
    val text: String,
    val type: EntityType,
    val confidence: Float,
    val startIndex: Int,
    val endIndex: Int,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Types of entities that can be extracted.
 */
enum class EntityType {
    PERSON,
    ORGANIZATION,
    LOCATION,
    DATE,
    TIME,
    PHONE_NUMBER,
    EMAIL,
    URL,
    TASK,
    DEADLINE,
    MONEY,
    PERCENTAGE,
    PRODUCT,
    EVENT
}

/**
 * Result of language detection operation.
 */
data class LanguageDetectionResult(
    val success: Boolean,
    val language: Language? = null,
    val confidence: Float = 0f,
    val alternativeLanguages: List<LanguageCandidate> = emptyList(),
    val error: String? = null
)

/**
 * Alternative language candidate.
 */
data class LanguageCandidate(
    val language: Language,
    val confidence: Float
)

/**
 * Result of translation operation.
 */
data class TranslationResult(
    val success: Boolean,
    val translatedText: String = "",
    val sourceLanguage: Language? = null,
    val targetLanguage: Language,
    val confidence: Float = 0f,
    val error: String? = null
)