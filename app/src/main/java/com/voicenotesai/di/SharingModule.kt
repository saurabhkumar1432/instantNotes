package com.voicenotesai.di

import com.voicenotesai.data.sharing.SharingManagerImpl
import com.voicenotesai.domain.sharing.SharingManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for sharing functionality
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SharingModule {
    
    @Binds
    @Singleton
    abstract fun bindSharingManager(
        sharingManagerImpl: SharingManagerImpl
    ): SharingManager
}