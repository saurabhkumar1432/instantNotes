package com.voicenotesai.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier annotations for different dispatchers.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

/**
 * Hilt module that provides coroutine dispatchers for dependency injection.
 * This ensures proper threading and prevents blocking the main thread.
 * 
 * For testing, you can replace these dispatchers using @TestInstallIn to provide
 * TestDispatchers instead:
 * 
 * @TestInstallIn(
 *     components = [SingletonComponent::class],
 *     replaces = [DispatcherModule::class]
 * )
 * object TestDispatcherModule {
 *     @Provides
 *     @IoDispatcher
 *     fun provideIoDispatcher(): CoroutineDispatcher = TestCoroutineDispatcher()
 * }
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    /**
     * Provides IO dispatcher for disk and network operations.
     * Override in tests with TestCoroutineDispatcher.
     */
    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /**
     * Provides Default dispatcher for CPU-intensive work.
     * Override in tests with TestCoroutineDispatcher.
     */
    @Provides
    @Singleton
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    /**
     * Provides Main dispatcher for UI updates.
     * Override in tests with TestCoroutineDispatcher.
     */
    @Provides
    @Singleton
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
