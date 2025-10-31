package com.voicenotesai.di

import android.content.Context
import com.voicenotesai.data.shortcuts.ShortcutManager
import com.voicenotesai.data.voice.VoiceCommandService
import com.voicenotesai.domain.usecase.VoiceCommandUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for quick capture features.
 */
@Module
@InstallIn(SingletonComponent::class)
object QuickCaptureModule {
    
    @Provides
    @Singleton
    fun provideVoiceCommandUseCase(): VoiceCommandUseCase {
        return VoiceCommandUseCase()
    }
    
    @Provides
    @Singleton
    fun provideVoiceCommandService(
        @ApplicationContext context: Context,
        voiceCommandUseCase: VoiceCommandUseCase
    ): VoiceCommandService {
        return VoiceCommandService(context, voiceCommandUseCase)
    }
    
    @Provides
    @Singleton
    fun provideShortcutManager(
        @ApplicationContext context: Context
    ): ShortcutManager {
        return ShortcutManager(context)
    }
}