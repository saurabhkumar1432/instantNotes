package com.voicenotesai.data.model

import com.voicenotesai.domain.model.Language

/**
 * Settings for multilingual processing and translation features.
 */
data class MultilingualSettings(
    val autoDetectLanguage: Boolean = true,
    val preferredLanguage: Language = Language.ENGLISH,
    val autoTranslate: Boolean = false,
    val targetTranslationLanguage: Language = Language.ENGLISH,
    val enableLanguageSpecificOptimizations: Boolean = true,
    val showLanguageConfidence: Boolean = false,
    val enableMultilingualNotes: Boolean = true,
    val preserveOriginalLanguage: Boolean = true,
    val rtlTextSupport: Boolean = true,
    val complexScriptSupport: Boolean = true,
    val preferredTranslationProvider: TranslationProvider = TranslationProvider.AUTO,
    val enableOfflineTranslation: Boolean = false,
    val languageDetectionSensitivity: LanguageDetectionSensitivity = LanguageDetectionSensitivity.MEDIUM
)

/**
 * Available translation providers.
 */
enum class TranslationProvider {
    AUTO,
    GOOGLE_TRANSLATE,
    AZURE_TRANSLATOR,
    AWS_TRANSLATE,
    LOCAL_MODELS,
    OPENAI_GPT
}

/**
 * Language detection sensitivity levels.
 */
enum class LanguageDetectionSensitivity {
    LOW,    // Only detect with high confidence (>0.8)
    MEDIUM, // Detect with moderate confidence (>0.6)
    HIGH    // Detect with low confidence (>0.4)
}

/**
 * Language processing preferences for specific languages.
 */
data class LanguageProcessingPreferences(
    val language: Language,
    val enableSpecialProcessing: Boolean = true,
    val preferredFont: String? = null,
    val customVocabulary: List<String> = emptyList(),
    val processingOptimizations: Map<String, Boolean> = emptyMap()
)

/**
 * Translation quality settings.
 */
data class TranslationQualitySettings(
    val enableQualityAssessment: Boolean = true,
    val minimumConfidenceThreshold: Float = 0.7f,
    val enableContextAwareTranslation: Boolean = true,
    val preserveFormatting: Boolean = true,
    val enableGlossary: Boolean = false,
    val customGlossary: Map<String, String> = emptyMap()
)