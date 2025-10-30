package com.voicenotesai.data.ai

import com.voicenotesai.domain.ai.NoteFormat
import com.voicenotesai.domain.model.Language
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MultilingualProcessingServiceTest {

    private lateinit var multilingualProcessingService: MultilingualProcessingService
    private val mockLanguageService = mockk<LanguageService>()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        multilingualProcessingService = MultilingualProcessingService(
            mockLanguageService,
            testDispatcher
        )
    }

    @Test
    fun `processMultilingualContent should handle single language content`() = runTest(testDispatcher) {
        // Given
        val content = "This is a test message in English."
        
        coEvery { 
            mockLanguageService.detectLanguageEnhanced(any()) 
        } returns com.voicenotesai.domain.ai.LanguageDetectionResult(
            success = true,
            language = Language.ENGLISH,
            confidence = 0.9f
        )

        // When
        val result = multilingualProcessingService.processMultilingualContent(content)

        // Then
        assertTrue(result.success)
        assertEquals(content.trim(), result.processedContent.trim())
        assertTrue(result.detectedLanguages.contains(Language.ENGLISH))
    }

    @Test
    fun `processMultilingualContent should handle empty content`() = runTest(testDispatcher) {
        // Given
        val emptyContent = ""

        // When
        val result = multilingualProcessingService.processMultilingualContent(emptyContent)

        // Then
        assertFalse(result.success)
        assertNotNull(result.error)
    }

    @Test
    fun `processMultilingualContent should translate to target language`() = runTest(testDispatcher) {
        // Given
        val content = "Hola mundo"
        val targetLanguage = Language.ENGLISH
        
        coEvery { 
            mockLanguageService.detectLanguageEnhanced(any()) 
        } returns com.voicenotesai.domain.ai.LanguageDetectionResult(
            success = true,
            language = Language.SPANISH,
            confidence = 0.8f
        )
        
        coEvery { 
            mockLanguageService.translateTextEnhanced(any(), any(), any()) 
        } returns com.voicenotesai.domain.ai.TranslationResult(
            success = true,
            translatedText = "Hello world",
            sourceLanguage = Language.SPANISH,
            targetLanguage = Language.ENGLISH,
            confidence = 0.9f
        )

        // When
        val result = multilingualProcessingService.processMultilingualContent(content, targetLanguage)

        // Then
        assertTrue(result.success)
        assertEquals(targetLanguage, result.targetLanguage)
        assertTrue(result.processedContent.contains("Hello world"))
    }

    @Test
    fun `formatNotesForLanguage should handle RTL languages`() = runTest(testDispatcher) {
        // Given
        val notes = "This is a test note"
        val format = NoteFormat.BulletPoints
        
        coEvery { 
            mockLanguageService.getLanguageProcessingConfig(Language.ARABIC) 
        } returns LanguageProcessingConfig(
            requiresSegmentation = false,
            segmentationMethod = SegmentationMethod.WORD_BASED,
            rtlSupport = true,
            complexScript = true,
            preferredFontFamily = "Noto Sans Arabic",
            textDirection = TextDirection.RTL
        )

        // When
        val result = multilingualProcessingService.formatNotesForLanguage(
            notes, Language.ARABIC, format
        )

        // Then
        assertNotNull(result)
        // RTL formatting should be applied
        assertTrue(result.contains("\u202E") || result.contains("\u202C"))
    }

    @Test
    fun `formatNotesForLanguage should handle complex scripts`() = runTest(testDispatcher) {
        // Given
        val notes = "这是一个测试笔记。"
        val format = NoteFormat.Summary
        
        coEvery { 
            mockLanguageService.getLanguageProcessingConfig(Language.CHINESE_SIMPLIFIED) 
        } returns LanguageProcessingConfig(
            requiresSegmentation = true,
            segmentationMethod = SegmentationMethod.CHARACTER_BASED,
            rtlSupport = false,
            complexScript = true,
            preferredFontFamily = "Noto Sans CJK",
            textDirection = TextDirection.LTR
        )

        // When
        val result = multilingualProcessingService.formatNotesForLanguage(
            notes, Language.CHINESE_SIMPLIFIED, format
        )

        // Then
        assertNotNull(result)
        // Should preserve Chinese characters
        assertTrue(result.contains("测试"))
    }

    @Test
    fun `optimizeTranscriptionForLanguage should enhance config for tonal languages`() = runTest(testDispatcher) {
        // Given
        val baseConfig = com.voicenotesai.domain.ai.TranscriptionConfig(
            model = com.voicenotesai.domain.ai.AIModel.Whisper,
            language = null,
            noiseReduction = false
        )
        
        coEvery { 
            mockLanguageService.getLanguageProcessingConfig(Language.CHINESE_SIMPLIFIED) 
        } returns LanguageProcessingConfig(
            requiresSegmentation = true,
            segmentationMethod = SegmentationMethod.CHARACTER_BASED,
            rtlSupport = false,
            complexScript = true,
            preferredFontFamily = "Noto Sans CJK",
            textDirection = TextDirection.LTR
        )

        // When
        val optimizedConfig = multilingualProcessingService.optimizeTranscriptionForLanguage(
            Language.CHINESE_SIMPLIFIED, baseConfig
        )

        // Then
        assertEquals(Language.CHINESE_SIMPLIFIED, optimizedConfig.language)
        assertTrue(optimizedConfig.noiseReduction) // Should be enabled for tonal languages
        assertTrue(optimizedConfig.customVocabulary.isNotEmpty())
    }
}