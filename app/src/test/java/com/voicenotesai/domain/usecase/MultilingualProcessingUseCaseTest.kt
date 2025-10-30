package com.voicenotesai.domain.usecase

import com.voicenotesai.data.ai.MultilingualProcessingResult
import com.voicenotesai.data.ai.MultilingualProcessingService
import com.voicenotesai.domain.ai.*
import com.voicenotesai.domain.model.AudioData
import com.voicenotesai.domain.model.Language
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MultilingualProcessingUseCaseTest {

    private lateinit var multilingualProcessingUseCase: MultilingualProcessingUseCase
    private val mockAIProcessingEngine = mockk<AIProcessingEngine>()
    private val mockMultilingualProcessingService = mockk<MultilingualProcessingService>()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        multilingualProcessingUseCase = MultilingualProcessingUseCase(
            mockAIProcessingEngine,
            mockMultilingualProcessingService,
            testDispatcher
        )
    }

    @Test
    fun `processMultilingualAudio should handle successful processing`() = runTest(testDispatcher) {
        // Given
        val audioData = AudioData(
            data = byteArrayOf(1, 2, 3, 4), 
            format = com.voicenotesai.domain.model.AudioFormat.WAV, 
            sampleRate = 44100,
            channels = 1,
            durationMs = 1000
        )
        val baseConfig = TranscriptionConfig(
            model = AIModel.Whisper,
            language = null
        )
        
        val transcriptionResult = TranscriptionResult(
            success = true,
            transcript = "Hello world, this is a test.",
            confidence = 0.9f
        )
        
        val languageDetectionResult = LanguageDetectionResult(
            success = true,
            language = Language.ENGLISH,
            confidence = 0.8f
        )
        
        val multilingualResult = MultilingualProcessingResult(
            success = true,
            processedContent = "Hello world, this is a test.",
            detectedLanguages = listOf(Language.ENGLISH)
        )
        
        val noteGenerationResult = NoteGenerationResult(
            success = true,
            notes = "• Hello world\n• This is a test",
            format = NoteFormat.BulletPoints
        )

        coEvery { 
            mockAIProcessingEngine.transcribeAudio(audioData, baseConfig) 
        } returns transcriptionResult
        
        coEvery { 
            mockAIProcessingEngine.detectLanguage(transcriptionResult.transcript) 
        } returns languageDetectionResult
        
        coEvery { 
            mockMultilingualProcessingService.optimizeTranscriptionForLanguage(any(), any()) 
        } returns baseConfig.copy(language = Language.ENGLISH)
        
        coEvery { 
            mockMultilingualProcessingService.processMultilingualContent(any(), any()) 
        } returns multilingualResult
        
        coEvery { 
            mockAIProcessingEngine.generateNotes(any(), any()) 
        } returns noteGenerationResult
        
        coEvery { 
            mockMultilingualProcessingService.formatNotesForLanguage(any(), any(), any()) 
        } returns "• Hello world\n• This is a test"

        // When
        val result = multilingualProcessingUseCase.processMultilingualAudio(
            audioData, baseConfig, null, NoteFormat.BulletPoints
        )

        // Then
        assertTrue(result.success)
        assertEquals(Language.ENGLISH, result.detectedLanguage)
        assertTrue(result.formattedNotes.isNotEmpty())
        assertNull(result.error)
    }

    @Test
    fun `processMultilingualAudio should handle transcription failure`() = runTest(testDispatcher) {
        // Given
        val audioData = AudioData(
            data = byteArrayOf(1, 2, 3, 4), 
            format = com.voicenotesai.domain.model.AudioFormat.WAV, 
            sampleRate = 44100,
            channels = 1,
            durationMs = 1000
        )
        val baseConfig = TranscriptionConfig(
            model = AIModel.Whisper,
            language = null
        )
        
        val failedTranscriptionResult = TranscriptionResult(
            success = false,
            error = "Transcription failed"
        )

        coEvery { 
            mockAIProcessingEngine.transcribeAudio(audioData, baseConfig) 
        } returns failedTranscriptionResult

        // When
        val result = multilingualProcessingUseCase.processMultilingualAudio(
            audioData, baseConfig
        )

        // Then
        assertFalse(result.success)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Initial transcription failed"))
    }

    @Test
    fun `translateNoteContent should handle successful translation`() = runTest(testDispatcher) {
        // Given
        val content = "Hola mundo"
        val targetLanguage = Language.ENGLISH
        
        val languageDetectionResult = LanguageDetectionResult(
            success = true,
            language = Language.SPANISH,
            confidence = 0.9f
        )
        
        val translationResult = TranslationResult(
            success = true,
            translatedText = "Hello world",
            sourceLanguage = Language.SPANISH,
            targetLanguage = Language.ENGLISH,
            confidence = 0.8f
        )
        
        val multilingualResult = MultilingualProcessingResult(
            success = true,
            processedContent = "Hello world",
            detectedLanguages = listOf(Language.ENGLISH)
        )

        coEvery { 
            mockAIProcessingEngine.detectLanguage(content) 
        } returns languageDetectionResult
        
        coEvery { 
            mockAIProcessingEngine.translateText(content, targetLanguage) 
        } returns translationResult
        
        coEvery { 
            mockMultilingualProcessingService.processMultilingualContent(any(), any()) 
        } returns multilingualResult

        // When
        val result = multilingualProcessingUseCase.translateNoteContent(
            content, targetLanguage
        )

        // Then
        assertTrue(result.success)
        assertEquals("Hello world", result.translatedContent)
        assertEquals(Language.SPANISH, result.sourceLanguage)
        assertEquals(Language.ENGLISH, result.targetLanguage)
    }

    @Test
    fun `getLanguageProcessingRecommendations should provide recommendations for complex scripts`() = runTest(testDispatcher) {
        // Given
        val content = "这是一个中文测试"
        
        val multilingualResult = MultilingualProcessingResult(
            success = true,
            processedContent = content,
            detectedLanguages = listOf(Language.CHINESE_SIMPLIFIED)
        )

        coEvery { 
            mockMultilingualProcessingService.processMultilingualContent(content) 
        } returns multilingualResult

        // When
        val result = multilingualProcessingUseCase.getLanguageProcessingRecommendations(content)

        // Then
        assertTrue(result.success)
        assertTrue(result.detectedLanguages.contains(Language.CHINESE_SIMPLIFIED))
        assertTrue(result.recommendations.any { it.type == RecommendationType.SEGMENTATION })
        assertFalse(result.isMultilingual)
    }

    @Test
    fun `getLanguageProcessingRecommendations should detect multilingual content`() = runTest(testDispatcher) {
        // Given
        val content = "Hello world. Hola mundo."
        
        val multilingualResult = MultilingualProcessingResult(
            success = true,
            processedContent = content,
            detectedLanguages = listOf(Language.ENGLISH, Language.SPANISH)
        )

        coEvery { 
            mockMultilingualProcessingService.processMultilingualContent(content) 
        } returns multilingualResult

        // When
        val result = multilingualProcessingUseCase.getLanguageProcessingRecommendations(content)

        // Then
        assertTrue(result.success)
        assertTrue(result.isMultilingual)
        assertTrue(result.recommendations.any { it.type == RecommendationType.TRANSLATION })
    }

    @Test
    fun `processMultilingualAudio should optimize for detected language`() = runTest(testDispatcher) {
        // Given
        val audioData = AudioData(
            data = byteArrayOf(1, 2, 3, 4), 
            format = com.voicenotesai.domain.model.AudioFormat.WAV, 
            sampleRate = 44100,
            channels = 1,
            durationMs = 1000
        )
        val baseConfig = TranscriptionConfig(
            model = AIModel.Whisper,
            language = null
        )
        
        val initialTranscription = TranscriptionResult(
            success = true,
            transcript = "Bonjour le monde",
            confidence = 0.8f
        )
        
        val languageDetection = LanguageDetectionResult(
            success = true,
            language = Language.FRENCH,
            confidence = 0.9f // High confidence should trigger re-transcription
        )
        
        val optimizedConfig = baseConfig.copy(
            language = Language.FRENCH,
            customVocabulary = listOf("bonjour", "monde")
        )
        
        val optimizedTranscription = TranscriptionResult(
            success = true,
            transcript = "Bonjour le monde, comment allez-vous?",
            confidence = 0.95f
        )

        coEvery { 
            mockAIProcessingEngine.transcribeAudio(audioData, baseConfig) 
        } returns initialTranscription
        
        coEvery { 
            mockAIProcessingEngine.detectLanguage(initialTranscription.transcript) 
        } returns languageDetection
        
        coEvery { 
            mockMultilingualProcessingService.optimizeTranscriptionForLanguage(Language.FRENCH, baseConfig) 
        } returns optimizedConfig
        
        coEvery { 
            mockAIProcessingEngine.transcribeAudio(audioData, optimizedConfig) 
        } returns optimizedTranscription
        
        coEvery { 
            mockMultilingualProcessingService.processMultilingualContent(any(), any()) 
        } returns MultilingualProcessingResult(
            success = true,
            processedContent = optimizedTranscription.transcript,
            detectedLanguages = listOf(Language.FRENCH)
        )
        
        coEvery { 
            mockAIProcessingEngine.generateNotes(any(), any()) 
        } returns NoteGenerationResult(
            success = true,
            notes = "Notes in French",
            format = NoteFormat.BulletPoints
        )
        
        coEvery { 
            mockMultilingualProcessingService.formatNotesForLanguage(any(), any(), any()) 
        } returns "Formatted French notes"

        // When
        val result = multilingualProcessingUseCase.processMultilingualAudio(audioData, baseConfig)

        // Then
        assertTrue(result.success)
        assertEquals(Language.FRENCH, result.detectedLanguage)
        assertEquals(optimizedTranscription.transcript, result.optimizedTranscript)
        assertTrue(result.languageConfidence > 0.8f)
    }
}