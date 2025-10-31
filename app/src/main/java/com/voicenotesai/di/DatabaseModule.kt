package com.voicenotesai.di

import android.content.Context
import androidx.room.Room
import com.voicenotesai.data.local.AppDatabase
import com.voicenotesai.data.local.DatabaseMigrations
import com.voicenotesai.data.local.DatabaseOptimizer
import com.voicenotesai.data.local.PaginationConfig
import com.voicenotesai.data.local.dao.NotesDao
import com.voicenotesai.data.local.dao.TaskDao
import com.voicenotesai.data.local.dao.ReminderDao
import com.voicenotesai.data.local.dao.CategoryUsageDao
import com.voicenotesai.data.local.dao.CustomCategoryDao
import com.voicenotesai.data.local.dao.ShareableLinkDao
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
            .addMigrations(
                DatabaseMigrations.MIGRATION_1_2,
                DatabaseMigrations.MIGRATION_2_3,
                DatabaseMigrations.MIGRATION_3_4,
                DatabaseMigrations.MIGRATION_4_5,
                DatabaseMigrations.MIGRATION_5_6,
                DatabaseMigrations.MIGRATION_6_7
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideNotesDao(database: AppDatabase): NotesDao {
        return database.notesDao()
    }

    @Provides
    @Singleton
    fun provideTaskDao(database: AppDatabase): TaskDao {
        return database.taskDao()
    }

    @Provides
    @Singleton
    fun provideReminderDao(database: AppDatabase): ReminderDao {
        return database.reminderDao()
    }

    @Provides
    @Singleton
    fun provideCategoryUsageDao(database: AppDatabase): CategoryUsageDao {
        return database.categoryUsageDao()
    }

    @Provides
    @Singleton
    fun provideCustomCategoryDao(database: AppDatabase): CustomCategoryDao {
        return database.customCategoryDao()
    }

    @Provides
    @Singleton
    fun provideShareableLinkDao(database: AppDatabase): ShareableLinkDao {
        return database.shareableLinkDao()
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
