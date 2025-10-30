package com.voicenotesai.data.ai

import com.voicenotesai.domain.model.Language
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LanguageServiceTest {

    private lateinit var languageService: LanguageService
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        languageService = LanguageService(testDispatcher)
    }

    @Test
    fun `detectLanguage should detect English correctly`() = runTest(testDispatcher) {
        // Given
        val englishText = "Hello world, this is a test message in English language."

        // When
        val result = languageService.detectLanguage(englishText)

        // Then
        assertTrue(result.success)
        assertEquals(Language.ENGLISH, result.language)
        assertTrue("Confidence should be > 0.1f, was ${result.confidence}", result.confidence > 0.1f)
    }

    @Test
    fun `detectLanguage should detect Spanish correctly`() = runTest(testDispatcher) {
        // Given
        val spanishText = "Hola mundo, este es un mensaje de prueba en español."

        // When
        val result = languageService.detectLanguage(spanishText)

        // Then
        assertTrue(result.success)
        assertEquals(Language.SPANISH, result.language)
        assertTrue(result.confidence > 0.3f)
    }

    @Test
    fun `detectLanguageEnhanced should provide better accuracy`() = runTest(testDispatcher) {
        // Given
        val mixedText = "Hello world. Bonjour le monde. Hola mundo."

        // When
        val result = languageService.detectLanguageEnhanced(mixedText)

        // Then
        assertTrue(result.success)
        assertNotNull(result.language)
        assertTrue(result.alternativeLanguages.isNotEmpty())
    }

    @Test
    fun `detectLanguage should handle empty text gracefully`() = runTest(testDispatcher) {
        // Given
        val emptyText = ""

        // When
        val result = languageService.detectLanguage(emptyText)

        // Then
        assertFalse(result.success)
        assertNotNull(result.error)
    }

    @Test
    fun `translateText should handle same language translation`() = runTest(testDispatcher) {
        // Given
        val text = "Hello world"
        val targetLanguage = Language.ENGLISH

        // When
        val result = languageService.translateText(text, targetLanguage)

        // Then
        assertTrue(result.success)
        assertEquals(text, result.translatedText)
        assertEquals(1.0f, result.confidence)
    }

    @Test
    fun `translateTextEnhanced should detect source language automatically`() = runTest(testDispatcher) {
        // Given
        val spanishText = "Hola mundo"
        val targetLanguage = Language.ENGLISH

        // When
        val result = languageService.translateTextEnhanced(spanishText, targetLanguage)

        // Then
        assertTrue(result.success)
        assertNotNull(result.sourceLanguage)
        assertNotNull(result.translatedText)
    }

    @Test
    fun `getSupportedLanguages should return all languages except AUTO_DETECT`() = runTest {
        // When
        val supportedLanguages = languageService.getSupportedLanguages()

        // Then
        assertTrue(supportedLanguages.isNotEmpty())
        assertFalse(supportedLanguages.contains(Language.AUTO_DETECT))
        assertTrue(supportedLanguages.contains(Language.ENGLISH))
        assertTrue(supportedLanguages.contains(Language.SPANISH))
    }

    @Test
    fun `isLanguageSupported should return correct values`() = runTest {
        // Then
        assertTrue(languageService.isLanguageSupported(Language.ENGLISH))
        assertTrue(languageService.isLanguageSupported(Language.SPANISH))
        assertFalse(languageService.isLanguageSupported(Language.AUTO_DETECT))
    }

    @Test
    fun `getLanguageProcessingConfig should return correct config for Chinese`() = runTest {
        // When
        val config = languageService.getLanguageProcessingConfig(Language.CHINESE_SIMPLIFIED)

        // Then
        assertTrue(config.requiresSegmentation)
        assertEquals(SegmentationMethod.CHARACTER_BASED, config.segmentationMethod)
        assertFalse(config.rtlSupport)
        assertTrue(config.complexScript)
        assertEquals(TextDirection.LTR, config.textDirection)
    }

    @Test
    fun `getLanguageProcessingConfig should return correct config for Arabic`() = runTest {
        // When
        val config = languageService.getLanguageProcessingConfig(Language.ARABIC)

        // Then
        assertFalse(config.requiresSegmentation)
        assertEquals(SegmentationMethod.WORD_BASED, config.segmentationMethod)
        assertTrue(config.rtlSupport)
        assertTrue(config.complexScript)
        assertEquals(TextDirection.RTL, config.textDirection)
    }

    @Test
    fun `getPopularLanguagePairs should return common translation pairs`() = runTest {
        // When
        val pairs = languageService.getPopularLanguagePairs()

        // Then
        assertTrue(pairs.isNotEmpty())
        assertTrue(pairs.any { it.sourceLanguage == Language.ENGLISH && it.targetLanguage == Language.SPANISH })
        assertTrue(pairs.any { it.sourceLanguage == Language.SPANISH && it.targetLanguage == Language.ENGLISH })
    }
}