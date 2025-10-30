package com.voicenotesai.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.voicenotesai.data.repository.TypographyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for typography and accessibility features
 */
@Module
@InstallIn(SingletonComponent::class)
object TypographyModule {
    
    @Provides
    @Singleton
    fun provideTypographyRepository(
        dataStore: DataStore<Preferences>
    ): TypographyRepository {
        return TypographyRepository(dataStore)
    }
}