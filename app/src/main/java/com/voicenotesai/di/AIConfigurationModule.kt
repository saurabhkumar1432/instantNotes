package com.voicenotesai.di

import com.voicenotesai.data.ai.AIConfigurationManagerImpl
import com.voicenotesai.data.ai.AIConfigurationValidatorImpl
import com.voicenotesai.domain.ai.AIConfigurationManager
import com.voicenotesai.domain.ai.AIConfigurationValidator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AIConfigurationModule {

    @Binds
    @Singleton
    abstract fun bindAIConfigurationManager(
        aiConfigurationManagerImpl: AIConfigurationManagerImpl
    ): AIConfigurationManager

    @Binds
    @Singleton
    abstract fun bindAIConfigurationValidator(
        aiConfigurationValidatorImpl: AIConfigurationValidatorImpl
    ): AIConfigurationValidator

    companion object {
        @Provides
        @Singleton
        fun provideJson(): Json {
            return Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            }
        }
    }
}