package com.voicenotesai.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.voicenotesai.data.local.AppDatabase
import com.voicenotesai.data.storage.LocalStorageManagerImpl
import com.voicenotesai.domain.cache.CacheManager
import com.voicenotesai.domain.storage.LocalStorageManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing storage management dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideLocalStorageManager(
        @ApplicationContext context: Context,
        database: AppDatabase,
        cacheManager: CacheManager,
        dataStore: DataStore<Preferences>,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): LocalStorageManager {
        return LocalStorageManagerImpl(
            context = context,
            database = database,
            cacheManager = cacheManager,
            dataStore = dataStore,
            ioDispatcher = ioDispatcher
        )
    }
}