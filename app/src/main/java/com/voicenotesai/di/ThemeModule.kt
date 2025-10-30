package com.voicenotesai.di

import com.voicenotesai.presentation.theme.ThemeEngine
import com.voicenotesai.presentation.theme.ThemeEngineImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for theme-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ThemeModule {

    @Binds
    @Singleton
    abstract fun bindThemeEngine(
        themeEngineImpl: ThemeEngineImpl
    ): ThemeEngine
}