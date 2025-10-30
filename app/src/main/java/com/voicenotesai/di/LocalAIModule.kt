package com.voicenotesai.di

import com.voicenotesai.data.ai.FallbackProcessingService
import com.voicenotesai.data.ai.LocalAIEngineImpl
import com.voicenotesai.data.ai.ModelManagementService
import com.voicenotesai.domain.ai.LocalAIEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for local AI processing components.
 * Provides bindings for offline AI capabilities and model management.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LocalAIModule {

    /**
     * Note: LocalAIEngine binding is now provided by OfflineModule 
     * using EnhancedLocalAIEngine for better offline capabilities.
     */

    /**
     * Note: FallbackProcessingService and ModelManagementService are provided
     * as concrete classes since they don't need interface abstractions at this time.
     * They are automatically available for injection due to @Singleton and @Inject annotations.
     */
}