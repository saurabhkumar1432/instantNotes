package com.voicenotesai.data.repository

import com.voicenotesai.data.model.AISettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    suspend fun saveSettings(settings: AISettings)
    fun getSettings(): Flow<AISettings?>
    suspend fun hasValidSettings(): Boolean
    suspend fun hasValidatedSettings(): Boolean
    suspend fun clearSettings()
}
