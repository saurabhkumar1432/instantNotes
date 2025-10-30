package com.voicenotesai.di

import android.content.Context
import com.voicenotesai.presentation.animations.AnimationEngine
import com.voicenotesai.presentation.animations.DefaultAnimationEngine
import com.voicenotesai.presentation.animations.DefaultPerformanceAdaptiveAnimationManager
import com.voicenotesai.presentation.animations.PerformanceAdaptiveAnimationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for animation-related components.
 * Provides centralized configuration for the advanced animation system.
 */
@Module
@InstallIn(SingletonComponent::class)
object AnimationModule {
    
    /**
     * Provides the main animation engine for the application
     */
    @Provides
    @Singleton
    fun provideAnimationEngine(): AnimationEngine {
        return DefaultAnimationEngine()
    }
    
    /**
     * Provides the performance-adaptive animation manager
     */
    @Provides
    @Singleton
    fun providePerformanceAdaptiveAnimationManager(
        @ApplicationContext context: Context,
        animationEngine: AnimationEngine
    ): PerformanceAdaptiveAnimationManager {
        return DefaultPerformanceAdaptiveAnimationManager(context, animationEngine)
    }
}