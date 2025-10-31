package com.voicenotesai.di

import com.voicenotesai.data.repository.TaskRepository
import com.voicenotesai.data.repository.TaskRepositoryImpl
import com.voicenotesai.domain.usecase.TaskManager
import com.voicenotesai.domain.usecase.TaskManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for task management components.
 * 
 * Provides bindings for task-related repositories and use cases.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TaskModule {
    
    /**
     * Binds TaskRepository implementation.
     */
    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        taskRepositoryImpl: TaskRepositoryImpl
    ): TaskRepository
    
    /**
     * Binds TaskManager implementation.
     */
    @Binds
    @Singleton
    abstract fun bindTaskManager(
        taskManagerImpl: TaskManagerImpl
    ): TaskManager
}