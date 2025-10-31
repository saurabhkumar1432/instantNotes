package com.voicenotesai.di

import com.voicenotesai.domain.error.ErrorHandler
import com.voicenotesai.domain.error.ErrorHandlerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for error handling components.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ErrorHandlingModule {
    
    @Binds
    @Singleton
    abstract fun bindErrorHandler(
        errorHandlerImpl: ErrorHandlerImpl
    ): ErrorHandler
}