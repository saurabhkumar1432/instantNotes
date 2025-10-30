package com.voicenotesai.data.ai

import com.voicenotesai.domain.ai.*
import com.voicenotesai.domain.model.Language
import com.voicenotesai.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for multilingual content processing with language-specific optimizations.
 * Handles automatic language detection, translation, and language-aware processing.
 */
@Singleton
class MultilingualProcessingService @Inject constructor(
    private val languageService: LanguageService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    /**
     * Processes multilingual content with automatic language detection and optimization.
     */
    suspend fun processMultilingualContent(
        content: String,
        targetLanguage: Language? = null
    ): MultilingualProcessingResult = withContext(ioDispatcher) {
        try {
            if (content.isBlank()) {
                return@withContext MultilingualProcessingResult(
                    success = false,
                    error = "Content is empty"
                )
            }

            // Detect content languages
            val languageSegments = detectLanguageSegments(content)
            
            // Process each segment with language-specific optimizations
            val processedSegments = languageSegments.map { segment ->
                processLanguageSegment(segment, targetLanguage)
            }

            // Combine results
            val combinedContent = if (targetLanguage != null) {
                // Translate all segments to target language
                processedSegments.joinToString("\n") { it.translatedText ?: it.originalSegment.originalText }
            } else {
                // Keep original content with optimizations
                processedSegments.joinToString("\n") { it.optimizedText }
            }

            MultilingualProcessingResult(
                success = true,
                processedContent = combinedContent,
                detectedLanguages = languageSegments.map { it.language }.distinct(),
                languageSegments = processedSegments,
                targetLanguage = targetLanguage
            )

        } catch (e: Exception) {
            MultilingualProcessingResult(
                success = false,
                error = "Multilingual processing failed: ${e.message}"
            )
        }
    }

    /**
     * Optimizes transcription configuration based on detected language.
     */
    suspend fun optimizeTranscriptionForLanguage(
        language: Language,
        baseConfig: TranscriptionConfig
    ): TranscriptionConfig {
        val processingConfig = languageService.getLanguageProcessingConfig(language)
        
        return baseConfig.copy(
            language = language,
            // Adjust settings based on language characteristics
            noiseReduction = when (language) {
                Language.CHINESE_SIMPLIFIED, Language.CHINESE_TRADITIONAL,
                Language.JAPANESE, Language.KOREAN -> true // Tonal languages benefit from noise reduction
                else -> baseConfig.noiseReduction
            },
            customVocabulary = getLanguageSpecificVocabulary(language)
        )
    }

    /**
     * Generates language-aware note formatting.
     */
    suspend fun formatNotesForLanguage(
        notes: String,
        language: Language,
        format: NoteFormat
    ): String = withContext(ioDispatcher) {
        val processingConfig = languageService.getLanguageProcessingConfig(language)
        
        return@withContext when {
            processingConfig.rtlSupport -> formatRTLNotes(notes, format)
            processingConfig.complexScript -> formatComplexScriptNotes(notes, format, language)
            else -> formatStandardNotes(notes, format)
        }
    }

    /**
     * Detects language segments in mixed-language content.
     */
    private suspend fun detectLanguageSegments(content: String): List<LanguageSegment> {
        val segments = mutableListOf<LanguageSegment>()
        
        // Split content into sentences/paragraphs for analysis
        val textSegments = content.split("\n").filter { it.isNotBlank() }
        
        for ((index, segment) in textSegments.withIndex()) {
            val detection = languageService.detectLanguageEnhanced(segment)
            
            segments.add(
                LanguageSegment(
                    id = index,
                    originalText = segment,
                    language = detection.language ?: Language.ENGLISH,
                    confidence = detection.confidence,
                    startIndex = content.indexOf(segment),
                    endIndex = content.indexOf(segment) + segment.length
                )
            )
        }
        
        return segments
    }

    /**
     * Processes a single language segment with optimizations.
     */
    private suspend fun processLanguageSegment(
        segment: LanguageSegment,
        targetLanguage: Language?
    ): ProcessedLanguageSegment {
        val processingConfig = languageService.getLanguageProcessingConfig(segment.language)
        
        // Apply language-specific text processing
        val optimizedText = applyLanguageOptimizations(segment.originalText, processingConfig)
        
        // Translate if target language is specified
        val translatedText = if (targetLanguage != null && targetLanguage != segment.language) {
            val translation = languageService.translateTextEnhanced(
                segment.originalText,
                targetLanguage,
                segment.language
            )
            if (translation.success) translation.translatedText else null
        } else null

        return ProcessedLanguageSegment(
            originalSegment = segment,
            optimizedText = optimizedText,
            translatedText = translatedText,
            processingConfig = processingConfig
        )
    }

    /**
     * Applies language-specific text optimizations.
     */
    private fun applyLanguageOptimizations(
        text: String,
        config: LanguageProcessingConfig
    ): String {
        var optimizedText = text

        when (config.segmentationMethod) {
            SegmentationMethod.CHARACTER_BASED -> {
                // Add proper spacing for character-based languages
                optimizedText = optimizedText.replace("([\\u4e00-\\u9fff])".toRegex(), "$1 ")
            }
            SegmentationMethod.SYLLABLE_BASED -> {
                // Optimize for syllable-based languages like Korean
                optimizedText = optimizedText.trim()
            }
            SegmentationMethod.DICTIONARY_BASED -> {
                // Apply dictionary-based segmentation for languages like Thai
                optimizedText = optimizedText.trim()
            }
            SegmentationMethod.MORPHOLOGICAL -> {
                // Apply morphological analysis for languages like Japanese
                optimizedText = optimizedText.trim()
            }
            else -> {
                // Standard word-based processing
                optimizedText = optimizedText.replace("\\s+".toRegex(), " ").trim()
            }
        }

        return optimizedText
    }

    /**
     * Gets language-specific vocabulary for improved transcription accuracy.
     */
    private fun getLanguageSpecificVocabulary(language: Language): List<String> {
        return when (language) {
            Language.ENGLISH -> listOf(
                "meeting", "action", "item", "deadline", "project", "task", "note", "reminder"
            )
            Language.SPANISH -> listOf(
                "reunión", "acción", "elemento", "fecha límite", "proyecto", "tarea", "nota", "recordatorio"
            )
            Language.FRENCH -> listOf(
                "réunion", "action", "élément", "échéance", "projet", "tâche", "note", "rappel"
            )
            Language.GERMAN -> listOf(
                "Besprechung", "Aktion", "Element", "Frist", "Projekt", "Aufgabe", "Notiz", "Erinnerung"
            )
            Language.JAPANESE -> listOf(
                "会議", "アクション", "項目", "締切", "プロジェクト", "タスク", "ノート", "リマインダー"
            )
            Language.CHINESE_SIMPLIFIED -> listOf(
                "会议", "行动", "项目", "截止日期", "项目", "任务", "笔记", "提醒"
            )
            Language.KOREAN -> listOf(
                "회의", "액션", "항목", "마감일", "프로젝트", "작업", "노트", "알림"
            )
            else -> emptyList()
        }
    }

    /**
     * Formats notes for right-to-left languages.
     */
    private fun formatRTLNotes(notes: String, format: NoteFormat): String {
        // Add RTL formatting markers and proper text direction
        val rtlMarker = "\u202E" // Right-to-Left Override
        val ltrMarker = "\u202D" // Left-to-Right Override
        val popMarker = "\u202C" // Pop Directional Formatting
        
        return when (format) {
            is NoteFormat.BulletPoints -> {
                notes.lines().joinToString("\n") { line ->
                    if (line.trim().startsWith("-") || line.trim().startsWith("•")) {
                        "$rtlMarker${line.trim()}$popMarker"
                    } else {
                        line
                    }
                }
            }
            else -> "$rtlMarker$notes$popMarker"
        }
    }

    /**
     * Formats notes for complex script languages.
     */
    private fun formatComplexScriptNotes(
        notes: String,
        format: NoteFormat,
        language: Language
    ): String {
        return when (language) {
            Language.CHINESE_SIMPLIFIED, Language.CHINESE_TRADITIONAL -> {
                // Add proper punctuation and spacing for Chinese
                notes.replace("。", "。\n")
                    .replace("？", "？\n")
                    .replace("！", "！\n")
                    .trim()
            }
            Language.JAPANESE -> {
                // Format Japanese text with proper line breaks
                notes.replace("。", "。\n")
                    .replace("？", "？\n")
                    .replace("！", "！\n")
                    .trim()
            }
            Language.KOREAN -> {
                // Format Korean text with proper spacing
                notes.replace("다.", "다.\n")
                    .replace("요.", "요.\n")
                    .trim()
            }
            else -> notes
        }
    }

    /**
     * Formats notes using standard formatting.
     */
    private fun formatStandardNotes(notes: String, format: NoteFormat): String {
        return when (format) {
            is NoteFormat.BulletPoints -> {
                notes.lines().joinToString("\n") { line ->
                    if (line.isNotBlank() && !line.trim().startsWith("-") && !line.trim().startsWith("•")) {
                        "• $line"
                    } else {
                        line
                    }
                }
            }
            else -> notes
        }
    }
}

/**
 * Represents a segment of text in a specific language.
 */
data class LanguageSegment(
    val id: Int,
    val originalText: String,
    val language: Language,
    val confidence: Float,
    val startIndex: Int,
    val endIndex: Int
)

/**
 * Represents a processed language segment with optimizations and translations.
 */
data class ProcessedLanguageSegment(
    val originalSegment: LanguageSegment,
    val optimizedText: String,
    val translatedText: String?,
    val processingConfig: LanguageProcessingConfig
)

/**
 * Result of multilingual content processing.
 */
data class MultilingualProcessingResult(
    val success: Boolean,
    val processedContent: String = "",
    val detectedLanguages: List<Language> = emptyList(),
    val languageSegments: List<ProcessedLanguageSegment> = emptyList(),
    val targetLanguage: Language? = null,
    val error: String? = null
)