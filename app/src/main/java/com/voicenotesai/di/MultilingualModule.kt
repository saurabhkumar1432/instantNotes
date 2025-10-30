package com.voicenotesai.di

import com.voicenotesai.data.ai.MultilingualProcessingService
import com.voicenotesai.domain.usecase.MultilingualProcessingUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for multilingual processing components.
 */
@Module
@InstallIn(SingletonComponent::class)
object MultilingualModule {

    @Provides
    @Singleton
    fun provideMultilingualProcessingService(
        languageService: com.voicenotesai.data.ai.LanguageService,
        @IoDispatcher ioDispatcher: kotlinx.coroutines.CoroutineDispatcher
    ): MultilingualProcessingService {
        return MultilingualProcessingService(languageService, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideMultilingualProcessingUseCase(
        aiProcessingEngine: com.voicenotesai.domain.ai.AIProcessingEngine,
        multilingualProcessingService: MultilingualProcessingService,
        @IoDispatcher ioDispatcher: kotlinx.coroutines.CoroutineDispatcher
    ): MultilingualProcessingUseCase {
        return MultilingualProcessingUseCase(
            aiProcessingEngine,
            multilingualProcessingService,
            ioDispatcher
        )
    }
}