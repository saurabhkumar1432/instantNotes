package com.voicenotesai.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.voicenotesai.data.model.AIProvider
import com.voicenotesai.data.model.AISettings
import com.voicenotesai.data.model.LanguageDetectionSensitivity
import com.voicenotesai.data.model.MultilingualSettings
import com.voicenotesai.data.model.TranslationProvider
import com.voicenotesai.domain.model.Language
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
        // AI Settings keys
        private val KEY_PROVIDER = stringPreferencesKey("ai_provider")
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_MODEL = stringPreferencesKey("model")
        private val KEY_IS_VALIDATED = stringPreferencesKey("is_validated")
        
        // Multilingual Settings keys
        private val KEY_AUTO_DETECT_LANGUAGE = booleanPreferencesKey("auto_detect_language")
        private val KEY_PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
        private val KEY_AUTO_TRANSLATE = booleanPreferencesKey("auto_translate")
        private val KEY_TARGET_TRANSLATION_LANGUAGE = stringPreferencesKey("target_translation_language")
        private val KEY_ENABLE_LANGUAGE_OPTIMIZATIONS = booleanPreferencesKey("enable_language_optimizations")
        private val KEY_SHOW_LANGUAGE_CONFIDENCE = booleanPreferencesKey("show_language_confidence")
        private val KEY_ENABLE_MULTILINGUAL_NOTES = booleanPreferencesKey("enable_multilingual_notes")
        private val KEY_PRESERVE_ORIGINAL_LANGUAGE = booleanPreferencesKey("preserve_original_language")
        private val KEY_RTL_TEXT_SUPPORT = booleanPreferencesKey("rtl_text_support")
        private val KEY_COMPLEX_SCRIPT_SUPPORT = booleanPreferencesKey("complex_script_support")
        private val KEY_TRANSLATION_PROVIDER = stringPreferencesKey("translation_provider")
        private val KEY_ENABLE_OFFLINE_TRANSLATION = booleanPreferencesKey("enable_offline_translation")
        private val KEY_LANGUAGE_DETECTION_SENSITIVITY = stringPreferencesKey("language_detection_sensitivity")
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

    // Multilingual Settings Methods
    override suspend fun saveMultilingualSettings(settings: MultilingualSettings) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[KEY_AUTO_DETECT_LANGUAGE] = settings.autoDetectLanguage
                preferences[KEY_PREFERRED_LANGUAGE] = settings.preferredLanguage.code
                preferences[KEY_AUTO_TRANSLATE] = settings.autoTranslate
                preferences[KEY_TARGET_TRANSLATION_LANGUAGE] = settings.targetTranslationLanguage.code
                preferences[KEY_ENABLE_LANGUAGE_OPTIMIZATIONS] = settings.enableLanguageSpecificOptimizations
                preferences[KEY_SHOW_LANGUAGE_CONFIDENCE] = settings.showLanguageConfidence
                preferences[KEY_ENABLE_MULTILINGUAL_NOTES] = settings.enableMultilingualNotes
                preferences[KEY_PRESERVE_ORIGINAL_LANGUAGE] = settings.preserveOriginalLanguage
                preferences[KEY_RTL_TEXT_SUPPORT] = settings.rtlTextSupport
                preferences[KEY_COMPLEX_SCRIPT_SUPPORT] = settings.complexScriptSupport
                preferences[KEY_TRANSLATION_PROVIDER] = settings.preferredTranslationProvider.name
                preferences[KEY_ENABLE_OFFLINE_TRANSLATION] = settings.enableOfflineTranslation
                preferences[KEY_LANGUAGE_DETECTION_SENSITIVITY] = settings.languageDetectionSensitivity.name
            }
        }
    }

    override fun getMultilingualSettings(): Flow<MultilingualSettings> {
        return dataStore.data
            .map { preferences ->
                MultilingualSettings(
                    autoDetectLanguage = preferences[KEY_AUTO_DETECT_LANGUAGE] ?: true,
                    preferredLanguage = Language.fromCode(preferences[KEY_PREFERRED_LANGUAGE] ?: "en") ?: Language.ENGLISH,
                    autoTranslate = preferences[KEY_AUTO_TRANSLATE] ?: false,
                    targetTranslationLanguage = Language.fromCode(preferences[KEY_TARGET_TRANSLATION_LANGUAGE] ?: "en") ?: Language.ENGLISH,
                    enableLanguageSpecificOptimizations = preferences[KEY_ENABLE_LANGUAGE_OPTIMIZATIONS] ?: true,
                    showLanguageConfidence = preferences[KEY_SHOW_LANGUAGE_CONFIDENCE] ?: false,
                    enableMultilingualNotes = preferences[KEY_ENABLE_MULTILINGUAL_NOTES] ?: true,
                    preserveOriginalLanguage = preferences[KEY_PRESERVE_ORIGINAL_LANGUAGE] ?: true,
                    rtlTextSupport = preferences[KEY_RTL_TEXT_SUPPORT] ?: true,
                    complexScriptSupport = preferences[KEY_COMPLEX_SCRIPT_SUPPORT] ?: true,
                    preferredTranslationProvider = try {
                        TranslationProvider.valueOf(preferences[KEY_TRANSLATION_PROVIDER] ?: "AUTO")
                    } catch (e: IllegalArgumentException) {
                        TranslationProvider.AUTO
                    },
                    enableOfflineTranslation = preferences[KEY_ENABLE_OFFLINE_TRANSLATION] ?: false,
                    languageDetectionSensitivity = try {
                        LanguageDetectionSensitivity.valueOf(preferences[KEY_LANGUAGE_DETECTION_SENSITIVITY] ?: "MEDIUM")
                    } catch (e: IllegalArgumentException) {
                        LanguageDetectionSensitivity.MEDIUM
                    }
                )
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun getMultilingualSettingsOnce(): MultilingualSettings = withContext(ioDispatcher) {
        getMultilingualSettings().first()
    }
}
