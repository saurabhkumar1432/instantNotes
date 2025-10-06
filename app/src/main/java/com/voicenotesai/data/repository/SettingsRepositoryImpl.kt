package com.voicenotesai.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.voicenotesai.data.model.AIProvider
import com.voicenotesai.data.model.AISettings
import com.voicenotesai.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SettingsRepository {

    companion object {
        private val KEY_PROVIDER = stringPreferencesKey("ai_provider")
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_MODEL = stringPreferencesKey("model")
        private val KEY_IS_VALIDATED = stringPreferencesKey("is_validated")
    }

    override suspend fun saveSettings(settings: AISettings) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[KEY_PROVIDER] = settings.provider.name
                preferences[KEY_API_KEY] = settings.apiKey
                preferences[KEY_MODEL] = settings.model
                preferences[KEY_IS_VALIDATED] = settings.isValidated.toString()
            }
        }
    }

    override fun getSettings(): Flow<AISettings?> {
        return dataStore.data
            .map { preferences ->
                val providerString = preferences[KEY_PROVIDER]
                val apiKey = preferences[KEY_API_KEY]
                val model = preferences[KEY_MODEL]
                val isValidated = preferences[KEY_IS_VALIDATED]?.toBoolean() ?: false

                if (providerString != null && apiKey != null && model != null) {
                    AISettings(
                        provider = AIProvider.valueOf(providerString),
                        apiKey = apiKey,
                        model = model,
                        isValidated = isValidated
                    )
                } else {
                    null
                }
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun hasValidSettings(): Boolean = withContext(ioDispatcher) {
        val settings = getSettings().first()
        return@withContext settings != null && 
               settings.apiKey.isNotBlank() && 
               settings.model.isNotBlank()
    }
    
    override suspend fun hasValidatedSettings(): Boolean = withContext(ioDispatcher) {
        val settings = getSettings().first()
        return@withContext settings != null && 
               settings.apiKey.isNotBlank() && 
               settings.model.isNotBlank() &&
               settings.isValidated
    }
    
    override suspend fun clearSettings() {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences.clear()
            }
        }
    }
}
