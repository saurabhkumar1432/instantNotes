package com.voicenotesai.di

import com.voicenotesai.data.ai.*
import com.voicenotesai.domain.ai.AIProcessingEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for AI processing components.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AIProcessingModule {

    @Binds
    @Singleton
    abstract fun bindAIProcessingEngine(
        aiProcessingEngineImpl: AIProcessingEngineImpl
    ): AIProcessingEngine

    @Binds
    @Singleton
    abstract fun bindWhisperApiService(
        whisperApiServiceImpl: WhisperApiServiceImpl
    ): WhisperApiService

    @Binds
    @Singleton
    abstract fun bindLocalWhisperService(
        localWhisperServiceImpl: LocalWhisperServiceImpl
    ): LocalWhisperService

    companion object {
        @Provides
        @Singleton
        fun provideTranscriptionService(
            encryptionService: com.voicenotesai.domain.security.EncryptionService,
            whisperApiService: WhisperApiService,
            localWhisperService: LocalWhisperService,
            @IoDispatcher ioDispatcher: kotlinx.coroutines.CoroutineDispatcher
        ): TranscriptionService {
            return TranscriptionService(
                encryptionService = encryptionService,
                whisperApiService = whisperApiService,
                localWhisperService = localWhisperService,
                ioDispatcher = ioDispatcher
            )
        }

        @Provides
        @Singleton
        fun provideEntityExtractionService(
            @IoDispatcher ioDispatcher: kotlinx.coroutines.CoroutineDispatcher
        ): EntityExtractionService {
            return EntityExtractionService(ioDispatcher)
        }

        @Provides
        @Singleton
        fun provideNoteCategorizationService(
            @IoDispatcher ioDispatcher: kotlinx.coroutines.CoroutineDispatcher
        ): NoteCategorizationService {
            return NoteCategorizationService(ioDispatcher)
        }

        @Provides
        @Singleton
        fun provideEnhancedSentimentAnalysisService(
            @IoDispatcher ioDispatcher: kotlinx.coroutines.CoroutineDispatcher
        ): EnhancedSentimentAnalysisService {
            return EnhancedSentimentAnalysisService(ioDispatcher)
        }

        @Provides
        @Singleton
        fun provideContentAnalysisService(
            noteCategorizationService: NoteCategorizationService,
            enhancedSentimentAnalysisService: EnhancedSentimentAnalysisService,
            @IoDispatcher ioDispatcher: kotlinx.coroutines.CoroutineDispatcher
        ): ContentAnalysisService {
            return ContentAnalysisService(
                noteCategorizationService,
                enhancedSentimentAnalysisService,
                ioDispatcher
            )
        }

        @Provides
        @Singleton
        fun provideLanguageService(
            @IoDispatcher ioDispatcher: kotlinx.coroutines.CoroutineDispatcher
        ): LanguageService {
            return LanguageService(ioDispatcher)
        }
    }
}