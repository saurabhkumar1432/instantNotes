package com.voicenotesai.di

import com.voicenotesai.data.ai.CategoryManagerImpl
import com.voicenotesai.domain.ai.CategoryManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for category management.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CategoryModule {

    @Binds
    @Singleton
    abstract fun bindCategoryManager(
        categoryManagerImpl: CategoryManagerImpl
    ): CategoryManager
}