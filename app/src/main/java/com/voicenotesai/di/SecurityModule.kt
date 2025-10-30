package com.voicenotesai.di

import com.voicenotesai.data.security.BiometricAuthManagerImpl
import com.voicenotesai.data.security.EncryptionServiceImpl
import com.voicenotesai.data.security.SecureStorageManagerImpl
import com.voicenotesai.domain.security.BiometricAuthManager
import com.voicenotesai.domain.security.EncryptionService
import com.voicenotesai.domain.security.SecureStorageManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindEncryptionService(
        encryptionServiceImpl: EncryptionServiceImpl
    ): EncryptionService

    @Binds
    @Singleton
    abstract fun bindSecureStorageManager(
        secureStorageManagerImpl: SecureStorageManagerImpl
    ): SecureStorageManager

    @Binds
    @Singleton
    abstract fun bindBiometricAuthManager(
        biometricAuthManagerImpl: BiometricAuthManagerImpl
    ): BiometricAuthManager
}