package com.voicenotesai.di

import com.voicenotesai.data.visualization.AudioVisualizationEngineImpl
import com.voicenotesai.domain.visualization.AudioVisualizationEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing audio visualization dependencies.
 * 
 * Requirements addressed:
 * - 1.3: Real-time audio visualization with smooth animations and contextual UI adaptations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AudioVisualizationModule {
    
    @Binds
    @Singleton
    abstract fun bindAudioVisualizationEngine(
        audioVisualizationEngineImpl: AudioVisualizationEngineImpl
    ): AudioVisualizationEngine
}