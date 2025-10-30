package com.voicenotesai.di

import com.voicenotesai.data.ai.EnhancedLocalAIEngine
import com.voicenotesai.data.offline.OfflineOperationsQueueImpl
import com.voicenotesai.data.offline.OfflineRecordingManagerImpl
import com.voicenotesai.domain.ai.LocalAIEngine
import com.voicenotesai.domain.offline.OfflineOperationsQueue
import com.voicenotesai.domain.offline.OfflineRecordingManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for offline functionality components.
 * Provides bindings for offline recording, operations queue, and enhanced local AI processing.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class OfflineModule {

    /**
     * Provides the offline recording manager implementation.
     */
    @Binds
    @Singleton
    abstract fun bindOfflineRecordingManager(
        offlineRecordingManagerImpl: OfflineRecordingManagerImpl
    ): OfflineRecordingManager

    /**
     * Provides the offline operations queue implementation.
     */
    @Binds
    @Singleton
    abstract fun bindOfflineOperationsQueue(
        offlineOperationsQueueImpl: OfflineOperationsQueueImpl
    ): OfflineOperationsQueue

    /**
     * Provides the enhanced local AI engine implementation.
     * This replaces the basic LocalAIEngine with enhanced capabilities.
     */
    @Binds
    @Singleton
    abstract fun bindEnhancedLocalAIEngine(
        enhancedLocalAIEngine: EnhancedLocalAIEngine
    ): LocalAIEngine
}