package com.voicenotesai.di

import com.voicenotesai.data.portability.DataPortabilityEngineImpl
import com.voicenotesai.domain.portability.DataPortabilityEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for data portability components
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataPortabilityModule {
    
    @Binds
    @Singleton
    abstract fun bindDataPortabilityEngine(
        dataPortabilityEngineImpl: DataPortabilityEngineImpl
    ): DataPortabilityEngine
}