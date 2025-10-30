package com.voicenotesai.di

import com.voicenotesai.data.cache.AccessTrackerImpl
import com.voicenotesai.data.cache.CacheManagerImpl
import com.voicenotesai.domain.cache.AccessTracker
import com.voicenotesai.domain.cache.CacheManager
import com.voicenotesai.domain.cache.FrequencyBasedPreloadingStrategy
import com.voicenotesai.domain.cache.PreloadingStrategy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for caching components
 */
@Module
@InstallIn(SingletonComponent::class)
object CacheModule {

    @Provides
    @Singleton
    fun provideAccessTracker(): AccessTracker {
        return AccessTrackerImpl(maxHistorySize = 10000)
    }

    @Provides
    @Singleton
    fun providePreloadingStrategy(accessTracker: AccessTracker): PreloadingStrategy {
        return FrequencyBasedPreloadingStrategy(
            accessTracker = accessTracker,
            maxPreloadItems = 50
        )
    }

    @Provides
    @Singleton
    fun provideCacheManager(
        preloadingStrategy: PreloadingStrategy,
        accessTracker: AccessTracker
    ): CacheManager {
        return CacheManagerImpl(
            maxMemorySize = 50 * 1024 * 1024, // 50MB
            preloadingStrategy = preloadingStrategy,
            accessTracker = accessTracker
        )
    }
}