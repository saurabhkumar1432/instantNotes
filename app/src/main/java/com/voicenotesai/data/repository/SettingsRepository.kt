package com.voicenotesai.data.repository

import com.voicenotesai.data.model.AISettings
import com.voicenotesai.data.model.MultilingualSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    suspend fun saveSettings(settings: AISettings)
    fun getSettings(): Flow<AISettings?>
    suspend fun hasValidSettings(): Boolean
    suspend fun hasValidatedSettings(): Boolean
    suspend fun clearSettings()
    
    // Multilingual settings methods
    suspend fun saveMultilingualSettings(settings: MultilingualSettings)
    fun getMultilingualSettings(): Flow<MultilingualSettings>
    suspend fun getMultilingualSettingsOnce(): MultilingualSettings
}
