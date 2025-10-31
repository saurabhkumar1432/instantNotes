package com.voicenotesai.di

import com.voicenotesai.data.notification.NotificationManagerImpl
import com.voicenotesai.data.repository.ReminderRepository
import com.voicenotesai.data.repository.ReminderRepositoryImpl
import com.voicenotesai.domain.notification.NotificationManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for reminder-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ReminderModule {
    
    @Binds
    @Singleton
    abstract fun bindReminderRepository(
        reminderRepositoryImpl: ReminderRepositoryImpl
    ): ReminderRepository
    
    @Binds
    @Singleton
    abstract fun bindNotificationManager(
        notificationManagerImpl: NotificationManagerImpl
    ): NotificationManager
}