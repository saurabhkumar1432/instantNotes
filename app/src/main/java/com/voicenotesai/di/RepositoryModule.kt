package com.voicenotesai.di

import com.voicenotesai.data.repository.AIRepository
import com.voicenotesai.data.repository.AIRepositoryImpl
import com.voicenotesai.data.repository.AudioRepository
import com.voicenotesai.data.repository.AudioRepositoryImpl
import com.voicenotesai.data.repository.NotesRepository
import com.voicenotesai.data.repository.NotesRepositoryImpl
import com.voicenotesai.data.repository.SettingsRepository
import com.voicenotesai.data.repository.SettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindNotesRepository(
        notesRepositoryImpl: NotesRepositoryImpl
    ): NotesRepository

    @Binds
    @Singleton
    abstract fun bindAudioRepository(
        audioRepositoryImpl: AudioRepositoryImpl
    ): AudioRepository

    @Binds
    @Singleton
    abstract fun bindAIRepository(
        aiRepositoryImpl: AIRepositoryImpl
    ): AIRepository
}
