package com.voicenotesai.di

import android.content.Context
import androidx.room.Room
import com.voicenotesai.data.local.AppDatabase
import com.voicenotesai.data.local.DatabaseMigrations
import com.voicenotesai.data.local.DatabaseOptimizer
import com.voicenotesai.data.local.PaginationConfig
import com.voicenotesai.data.local.dao.NotesDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "voice_notes_database"
        )
            .addMigrations(DatabaseMigrations.MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton
    fun provideNotesDao(database: AppDatabase): NotesDao {
        return database.notesDao()
    }
    
    @Provides
    @Singleton
    fun provideDatabaseOptimizer(
        notesDao: NotesDao,
        database: AppDatabase,
        @ApplicationContext context: Context
    ): DatabaseOptimizer {
        return DatabaseOptimizer(notesDao, database, context)
    }
    
    @Provides
    @Singleton
    fun providePaginationConfig(notesDao: NotesDao): PaginationConfig {
        return PaginationConfig(notesDao)
    }
}
