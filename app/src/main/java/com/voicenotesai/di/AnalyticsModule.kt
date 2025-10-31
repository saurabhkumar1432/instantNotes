package com.voicenotesai.di

import android.content.Context
import com.voicenotesai.data.analytics.AnalyticsEngineImpl
import com.voicenotesai.data.local.AppDatabase
import com.voicenotesai.data.local.dao.AnalyticsDao
import com.voicenotesai.data.repository.AnalyticsRepositoryImpl
import com.voicenotesai.domain.analytics.AnalyticsEngine
import com.voicenotesai.domain.analytics.AnalyticsRepository
import com.voicenotesai.domain.usecase.AnalyticsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Dagger Hilt module for analytics dependencies.
 * Provides all analytics-related dependencies with proper scoping.
 */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {
    
    /**
     * Provides the analytics DAO from the database
     */
    @Provides
    @Singleton
    fun provideAnalyticsDao(database: AppDatabase): AnalyticsDao {
        return database.analyticsDao()
    }
    

    
    /**
     * Provides the analytics engine implementation
     */
    @Provides
    @Singleton
    fun provideAnalyticsEngine(
        analyticsDao: AnalyticsDao,
        @ApplicationContext context: Context,
        json: Json
    ): AnalyticsEngine {
        return AnalyticsEngineImpl(analyticsDao, context, json)
    }
    
    /**
     * Provides the analytics repository implementation
     */
    @Provides
    @Singleton
    fun provideAnalyticsRepository(
        analyticsEngine: AnalyticsEngineImpl
    ): AnalyticsRepository {
        return AnalyticsRepositoryImpl(analyticsEngine)
    }
    
    /**
     * Provides the analytics use case
     */
    @Provides
    @Singleton
    fun provideAnalyticsUseCase(
        analyticsRepository: AnalyticsRepository
    ): AnalyticsUseCase {
        return AnalyticsUseCase(analyticsRepository)
    }
}