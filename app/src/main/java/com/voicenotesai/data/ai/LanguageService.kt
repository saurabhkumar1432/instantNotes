package com.voicenotesai.data.ai

import com.voicenotesai.domain.ai.LanguageDetectionResult
import com.voicenotesai.domain.ai.LanguageCandidate
import com.voicenotesai.domain.ai.TranslationResult
import com.voicenotesai.domain.model.Language
import com.voicenotesai.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for language detection and translation operations.
 * Supports automatic language detection and text translation.
 */
@Singleton
class LanguageService @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    companion object {
        // Common words in different languages for basic detection
        private val LANGUAGE_INDICATORS = mapOf(
            Language.ENGLISH to setOf(
                "the", "and", "is", "in", "to", "of", "a", "that", "it", "with",
                "for", "as", "was", "on", "are", "you", "this", "be", "at", "have"
            ),
            Language.SPANISH to setOf(
                "el", "la", "de", "que", "y", "a", "en", "un", "es", "se",
                "no", "te", "lo", "le", "da", "su", "por", "son", "con", "para"
            ),
            Language.FRENCH to setOf(
                "le", "de", "et", "à", "un", "il", "être", "et", "en", "avoir",
                "que", "pour", "dans", "ce", "son", "une", "sur", "avec", "ne", "se"
            ),
            Language.GERMAN to setOf(
                "der", "die", "und", "in", "den", "von", "zu", "das", "mit", "sich",
                "des", "auf", "für", "ist", "im", "dem", "nicht", "ein", "eine", "als"
            ),
            Language.ITALIAN to setOf(
                "il", "di", "che", "e", "la", "per", "un", "in", "con", "del",
                "da", "a", "al", "le", "si", "dei", "sul", "una", "su", "nel"
            ),
            Language.PORTUGUESE to setOf(
                "o", "de", "a", "e", "do", "da", "em", "um", "para", "é",
                "com", "não", "uma", "os", "no", "se", "na", "por", "mais", "as"
            ),
            Language.RUSSIAN to setOf(
                "в", "и", "не", "на", "я", "быть", "тот", "он", "оно", "с",
                "а", "как", "по", "это", "она", "к", "но", "они", "мы", "что"
            ),
            Language.CHINESE_SIMPLIFIED to setOf(
                "的", "一", "是", "在", "不", "了", "有", "和", "人", "这",
                "中", "大", "为", "上", "个", "国", "我", "以", "要", "他"
            ),
            Language.JAPANESE to setOf(
                "の", "に", "は", "を", "た", "が", "で", "て", "と", "し",
                "れ", "さ", "ある", "いる", "も", "する", "から", "な", "こと", "として"
            ),
            Language.KOREAN to setOf(
                "이", "의", "가", "을", "는", "에", "한", "하", "으로", "로",
                "와", "과", "도", "만", "에서", "까지", "부터", "보다", "처럼", "같이"
            ),
            Language.ARABIC to setOf(
                "في", "من", "إلى", "على", "أن", "هذا", "هذه", "التي", "الذي", "كان",
                "لم", "قد", "كل", "بعد", "عند", "حتى", "لكن", "أو", "إذا", "حيث"
            ),
            Language.HINDI to setOf(
                "के", "में", "की", "है", "को", "से", "पर", "एक", "यह", "वह",
                "और", "या", "तो", "भी", "जो", "कि", "था", "थी", "हैं", "होता"
            )
        )
        
        // Character patterns for script detection
        private val SCRIPT_PATTERNS = mapOf(
            Language.CHINESE_SIMPLIFIED to "[\u4e00-\u9fff]".toRegex(),
            Language.CHINESE_TRADITIONAL to "[\u4e00-\u9fff]".toRegex(),
            Language.JAPANESE to "[\u3040-\u309f\u30a0-\u30ff\u4e00-\u9fff]".toRegex(),
            Language.KOREAN to "[\uac00-\ud7af]".toRegex(),
            Language.ARABIC to "[\u0600-\u06ff]".toRegex(),
            Language.RUSSIAN to "[\u0400-\u04ff]".toRegex(),
            Language.GREEK to "[\u0370-\u03ff]".toRegex(),
            Language.HEBREW to "[\u0590-\u05ff]".toRegex(),
            Language.THAI to "[\u0e00-\u0e7f]".toRegex(),
            Language.HINDI to "[\u0900-\u097f]".toRegex()
        )
    }

    /**
     * Detects the language of the given text.
     */
    suspend fun detectLanguage(text: String): LanguageDetectionResult = withContext(ioDispatcher) {
        try {
            if (text.isBlank()) {
                return@withContext LanguageDetectionResult(
                    success = false,
                    error = "Text is empty"
                )
            }
            
            val cleanText = text.lowercase().replace("[^\\p{L}\\s]".toRegex(), " ")
            val words = cleanText.split("\\s+".toRegex()).filter { it.isNotBlank() }
            
            if (words.isEmpty()) {
                return@withContext LanguageDetectionResult(
                    success = false,
                    error = "No valid words found in text"
                )
            }
            
            // First, try script-based detection
            val scriptDetection = detectByScript(text)
            if (scriptDetection != null) {
                return@withContext LanguageDetectionResult(
                    success = true,
                    language = scriptDetection,
                    confidence = 0.9f
                )
            }
            
            // Then try word-based detection for Latin script languages
            val wordDetection = detectByWords(words)
            
            return@withContext if (wordDetection.isNotEmpty()) {
                val topCandidate = wordDetection.first()
                LanguageDetectionResult(
                    success = true,
                    language = topCandidate.language,
                    confidence = topCandidate.confidence,
                    alternativeLanguages = wordDetection.drop(1)
                )
            } else {
                LanguageDetectionResult(
                    success = true,
                    language = Language.ENGLISH, // Default fallback
                    confidence = 0.3f
                )
            }
            
        } catch (e: Exception) {
            LanguageDetectionResult(
                success = false,
                error = "Language detection failed: ${e.message}"
            )
        }
    }

    /**
     * Translates text to the target language.
     * Note: This is a placeholder implementation. In a real app, you would integrate
     * with translation services like Google Translate API, Azure Translator, etc.
     */
    suspend fun translateText(
        text: String,
        targetLanguage: Language
    ): TranslationResult = withContext(ioDispatcher) {
        try {
            if (text.isBlank()) {
                return@withContext TranslationResult(
                    success = false,
                    targetLanguage = targetLanguage,
                    error = "Text is empty"
                )
            }
            
            // Detect source language first
            val sourceDetection = detectLanguage(text)
            val sourceLanguage = sourceDetection.language
            
            // Check if translation is needed
            if (sourceLanguage == targetLanguage) {
                return@withContext TranslationResult(
                    success = true,
                    translatedText = text,
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage,
                    confidence = 1.0f
                )
            }
            
            // TODO: Implement actual translation using external service
            // For now, return a placeholder response
            val translatedText = "[$targetLanguage translation of: $text]"
            
            TranslationResult(
                success = true,
                translatedText = translatedText,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                confidence = 0.8f
            )
            
        } catch (e: Exception) {
            TranslationResult(
                success = false,
                targetLanguage = targetLanguage,
                error = "Translation failed: ${e.message}"
            )
        }
    }

    /**
     * Detects language based on script/character patterns.
     */
    private fun detectByScript(text: String): Language? {
        SCRIPT_PATTERNS.forEach { (language, pattern) ->
            val matches = pattern.findAll(text).count()
            val totalChars = text.replace("\\s".toRegex(), "").length
            
            if (totalChars > 0 && matches.toFloat() / totalChars > 0.3f) {
                return language
            }
        }
        return null
    }

    /**
     * Detects language based on common words.
     */
    private fun detectByWords(words: List<String>): List<LanguageCandidate> {
        val languageScores = mutableMapOf<Language, Int>()
        
        LANGUAGE_INDICATORS.forEach { (language, indicators) ->
            val matchCount = words.count { word -> indicators.contains(word) }
            if (matchCount > 0) {
                languageScores[language] = matchCount
            }
        }
        
        val totalWords = words.size
        return languageScores.entries
            .map { (language, score) ->
                LanguageCandidate(
                    language = language,
                    confidence = (score.toFloat() / totalWords).coerceAtMost(1.0f)
                )
            }
            .sortedByDescending { it.confidence }
            .filter { it.confidence > 0.1f } // Only include candidates with >10% confidence
    }

    /**
     * Detects language using n-gram analysis.
     */
    private fun detectByNgrams(text: String): List<LanguageCandidate> {
        val ngramSize = 3
        val ngrams = mutableMapOf<String, Int>()
        
        // Generate character n-grams
        for (i in 0..text.length - ngramSize) {
            val ngram = text.substring(i, i + ngramSize).lowercase()
            ngrams[ngram] = ngrams.getOrDefault(ngram, 0) + 1
        }
        
        // Compare with language-specific n-gram patterns
        val languageScores = mutableMapOf<Language, Float>()
        
        // Simplified n-gram patterns for major languages
        val ngramPatterns = mapOf(
            Language.ENGLISH to setOf("the", "and", "ing", "ion", "ent", "her", "hat", "his", "tha"),
            Language.SPANISH to setOf("que", "con", "una", "por", "est", "ent", "aci", "ion", "ada"),
            Language.FRENCH to setOf("que", "les", "des", "une", "ent", "ion", "ait", "eur", "ant"),
            Language.GERMAN to setOf("der", "die", "und", "ich", "sch", "ein", "ung", "ent", "ich"),
            Language.ITALIAN to setOf("che", "con", "per", "una", "del", "ent", "are", "ion", "ato")
        )
        
        ngramPatterns.forEach { (language, patterns) ->
            val matchScore = patterns.sumOf { pattern ->
                ngrams.getOrDefault(pattern, 0)
            }.toFloat()
            
            if (matchScore > 0) {
                languageScores[language] = matchScore / ngrams.values.sum()
            }
        }
        
        return languageScores.entries
            .map { (language, score) ->
                LanguageCandidate(language = language, confidence = score)
            }
            .sortedByDescending { it.confidence }
            .filter { it.confidence > 0.05f }
    }

    /**
     * Detects language using character frequency analysis.
     */
    private fun detectByCharacterFrequency(text: String): List<LanguageCandidate> {
        val charFreq = mutableMapOf<Char, Int>()
        text.lowercase().forEach { char ->
            if (char.isLetter()) {
                charFreq[char] = charFreq.getOrDefault(char, 0) + 1
            }
        }
        
        val languageScores = mutableMapOf<Language, Float>()
        
        // Character frequency patterns for different languages
        val charPatterns = mapOf(
            Language.ENGLISH to mapOf('e' to 0.127f, 't' to 0.091f, 'a' to 0.082f, 'o' to 0.075f, 'i' to 0.070f),
            Language.SPANISH to mapOf('e' to 0.137f, 'a' to 0.125f, 'o' to 0.086f, 's' to 0.080f, 'n' to 0.071f),
            Language.FRENCH to mapOf('e' to 0.147f, 's' to 0.081f, 'a' to 0.076f, 'i' to 0.075f, 't' to 0.074f),
            Language.GERMAN to mapOf('e' to 0.174f, 'n' to 0.098f, 'i' to 0.075f, 's' to 0.072f, 'r' to 0.070f),
            Language.ITALIAN to mapOf('e' to 0.118f, 'a' to 0.117f, 'i' to 0.111f, 'o' to 0.098f, 'n' to 0.069f)
        )
        
        val totalChars = charFreq.values.sum().toFloat()
        if (totalChars == 0f) return emptyList()
        
        charPatterns.forEach { (language, expectedFreq) ->
            var score = 0f
            expectedFreq.forEach { (char, expectedRatio) ->
                val actualRatio = charFreq.getOrDefault(char, 0) / totalChars
                score += 1f - kotlin.math.abs(actualRatio - expectedRatio)
            }
            languageScores[language] = score / expectedFreq.size
        }
        
        return languageScores.entries
            .map { (language, score) ->
                LanguageCandidate(language = language, confidence = score)
            }
            .sortedByDescending { it.confidence }
            .filter { it.confidence > 0.6f }
    }

    /**
     * Combines multiple detection results using weighted scoring.
     */
    private fun combineDetectionResults(results: List<LanguageCandidate>): List<LanguageCandidate> {
        val combinedScores = mutableMapOf<Language, Float>()
        
        results.forEach { candidate ->
            combinedScores[candidate.language] = 
                combinedScores.getOrDefault(candidate.language, 0f) + candidate.confidence
        }
        
        return combinedScores.entries
            .map { (language, score) ->
                LanguageCandidate(language = language, confidence = score.coerceAtMost(1.0f))
            }
            .sortedByDescending { it.confidence }
            .filter { it.confidence > 0.3f }
    }

    /**
     * Gets the system language as fallback.
     */
    private fun getSystemLanguage(): Language {
        val systemLocale = Locale.getDefault()
        return Language.fromCode(systemLocale.language) ?: Language.ENGLISH
    }

    /**
     * Preprocesses text for translation based on source language characteristics.
     */
    private fun preprocessTextForTranslation(text: String, sourceLanguage: Language): String {
        return when (sourceLanguage) {
            Language.CHINESE_SIMPLIFIED, Language.CHINESE_TRADITIONAL -> {
                // Add spaces between Chinese characters for better processing
                text.replace("([\\u4e00-\\u9fff])".toRegex(), "$1 ").trim()
            }
            Language.JAPANESE -> {
                // Handle mixed scripts (Hiragana, Katakana, Kanji)
                text.replace("([\\u3040-\\u309f\\u30a0-\\u30ff\\u4e00-\\u9fff])".toRegex(), "$1 ").trim()
            }
            Language.ARABIC, Language.HEBREW -> {
                // Handle RTL text preprocessing
                text.trim()
            }
            Language.THAI -> {
                // Thai doesn't use spaces between words
                text.trim()
            }
            else -> {
                // Standard preprocessing for Latin-based languages
                text.replace("\\s+".toRegex(), " ").trim()
            }
        }
    }

    /**
     * Performs the actual translation (placeholder implementation).
     * In a real app, this would integrate with translation services.
     */
    private suspend fun performTranslation(
        text: String,
        sourceLanguage: Language,
        targetLanguage: Language
    ): String {
        // TODO: Integrate with actual translation service (Google Translate, Azure Translator, etc.)
        // This is a placeholder implementation
        return when {
            sourceLanguage == Language.ENGLISH && targetLanguage == Language.SPANISH -> {
                // Simple word replacements for demo
                text.replace("hello", "hola")
                    .replace("goodbye", "adiós")
                    .replace("thank you", "gracias")
                    .replace("yes", "sí")
                    .replace("no", "no")
            }
            sourceLanguage == Language.SPANISH && targetLanguage == Language.ENGLISH -> {
                text.replace("hola", "hello")
                    .replace("adiós", "goodbye")
                    .replace("gracias", "thank you")
                    .replace("sí", "yes")
            }
            else -> {
                "[$targetLanguage translation of: $text]"
            }
        }
    }

    /**
     * Assesses translation quality based on various factors.
     */
    private fun assessTranslationQuality(
        sourceText: String,
        translatedText: String,
        sourceLanguage: Language,
        targetLanguage: Language
    ): Float {
        var confidence = 0.8f // Base confidence
        
        // Adjust confidence based on language pair difficulty
        val languagePairDifficulty = getLanguagePairDifficulty(sourceLanguage, targetLanguage)
        confidence *= (1f - languagePairDifficulty * 0.3f)
        
        // Adjust based on text length (longer texts may have lower confidence)
        val lengthFactor = when {
            sourceText.length < 50 -> 1.0f
            sourceText.length < 200 -> 0.95f
            sourceText.length < 500 -> 0.9f
            else -> 0.85f
        }
        confidence *= lengthFactor
        
        // Check for obvious translation issues
        if (translatedText.contains("[") && translatedText.contains("translation of:")) {
            confidence = 0.5f // Placeholder translation
        }
        
        return confidence.coerceIn(0f, 1f)
    }

    /**
     * Gets the difficulty score for a language pair (0.0 = easy, 1.0 = very difficult).
     */
    private fun getLanguagePairDifficulty(sourceLanguage: Language, targetLanguage: Language): Float {
        // Same language family = easier
        val languageFamilies = mapOf(
            "Romance" to setOf(Language.SPANISH, Language.FRENCH, Language.ITALIAN, Language.PORTUGUESE),
            "Germanic" to setOf(Language.ENGLISH, Language.GERMAN, Language.DUTCH, Language.SWEDISH, Language.NORWEGIAN),
            "Slavic" to setOf(Language.RUSSIAN, Language.POLISH, Language.CZECH, Language.UKRAINIAN),
            "CJK" to setOf(Language.CHINESE_SIMPLIFIED, Language.CHINESE_TRADITIONAL, Language.JAPANESE, Language.KOREAN),
            "Semitic" to setOf(Language.ARABIC, Language.HEBREW)
        )
        
        val sourceFamily = languageFamilies.entries.find { it.value.contains(sourceLanguage) }?.key
        val targetFamily = languageFamilies.entries.find { it.value.contains(targetLanguage) }?.key
        
        return when {
            sourceFamily == targetFamily -> 0.2f // Same family
            sourceFamily == "Germanic" && targetFamily == "Romance" -> 0.3f // Related families
            sourceFamily == "Romance" && targetFamily == "Germanic" -> 0.3f
            sourceLanguage == Language.ENGLISH || targetLanguage == Language.ENGLISH -> 0.4f // English involved
            sourceFamily == "CJK" || targetFamily == "CJK" -> 0.8f // CJK languages are complex
            sourceFamily == "Semitic" || targetFamily == "Semitic" -> 0.7f // RTL languages
            else -> 0.6f // Default difficulty
        }
    }

    /**
     * Detects language with enhanced accuracy using multiple detection methods.
     */
    suspend fun detectLanguageEnhanced(text: String): LanguageDetectionResult = withContext(ioDispatcher) {
        try {
            if (text.isBlank()) {
                return@withContext LanguageDetectionResult(
                    success = false,
                    error = "Text is empty"
                )
            }
            
            val cleanText = text.lowercase().replace("[^\\p{L}\\s]".toRegex(), " ")
            val words = cleanText.split("\\s+".toRegex()).filter { it.isNotBlank() }
            
            if (words.isEmpty()) {
                return@withContext LanguageDetectionResult(
                    success = false,
                    error = "No valid words found in text"
                )
            }
            
            // Combine multiple detection methods for better accuracy
            val detectionResults = mutableListOf<LanguageCandidate>()
            
            // 1. Script-based detection (highest confidence for non-Latin scripts)
            val scriptDetection = detectByScript(text)
            if (scriptDetection != null) {
                detectionResults.add(LanguageCandidate(scriptDetection, 0.95f))
            }
            
            // 2. Word frequency analysis
            val wordDetection = detectByWords(words)
            detectionResults.addAll(wordDetection.map { it.copy(confidence = it.confidence * 0.8f) })
            
            // 3. N-gram analysis for better accuracy
            val ngramDetection = detectByNgrams(cleanText)
            detectionResults.addAll(ngramDetection.map { it.copy(confidence = it.confidence * 0.7f) })
            
            // 4. Character frequency analysis
            val charFreqDetection = detectByCharacterFrequency(text)
            detectionResults.addAll(charFreqDetection.map { it.copy(confidence = it.confidence * 0.6f) })
            
            // Combine and rank results
            val combinedResults = combineDetectionResults(detectionResults)
            
            return@withContext if (combinedResults.isNotEmpty()) {
                val topCandidate = combinedResults.first()
                LanguageDetectionResult(
                    success = true,
                    language = topCandidate.language,
                    confidence = topCandidate.confidence,
                    alternativeLanguages = combinedResults.drop(1).take(3)
                )
            } else {
                // Fallback to system locale if available
                val systemLanguage = getSystemLanguage()
                LanguageDetectionResult(
                    success = true,
                    language = systemLanguage,
                    confidence = 0.3f
                )
            }
            
        } catch (e: Exception) {
            LanguageDetectionResult(
                success = false,
                error = "Enhanced language detection failed: ${e.message}"
            )
        }
    }

    /**
     * Translates text with automatic source language detection and quality assessment.
     */
    suspend fun translateTextEnhanced(
        text: String,
        targetLanguage: Language,
        sourceLanguage: Language? = null
    ): TranslationResult = withContext(ioDispatcher) {
        try {
            if (text.isBlank()) {
                return@withContext TranslationResult(
                    success = false,
                    targetLanguage = targetLanguage,
                    error = "Text is empty"
                )
            }
            
            // Detect source language if not provided
            val detectedSourceLanguage = sourceLanguage ?: run {
                val detection = detectLanguageEnhanced(text)
                detection.language ?: Language.ENGLISH
            }
            
            // Check if translation is needed
            if (detectedSourceLanguage == targetLanguage) {
                return@withContext TranslationResult(
                    success = true,
                    translatedText = text,
                    sourceLanguage = detectedSourceLanguage,
                    targetLanguage = targetLanguage,
                    confidence = 1.0f
                )
            }
            
            // Apply language-specific preprocessing
            val preprocessedText = preprocessTextForTranslation(text, detectedSourceLanguage)
            
            // Perform translation with quality assessment
            val translatedText = performTranslation(preprocessedText, detectedSourceLanguage, targetLanguage)
            val confidence = assessTranslationQuality(preprocessedText, translatedText, detectedSourceLanguage, targetLanguage)
            
            TranslationResult(
                success = true,
                translatedText = translatedText,
                sourceLanguage = detectedSourceLanguage,
                targetLanguage = targetLanguage,
                confidence = confidence
            )
            
        } catch (e: Exception) {
            TranslationResult(
                success = false,
                targetLanguage = targetLanguage,
                error = "Enhanced translation failed: ${e.message}"
            )
        }
    }

    /**
     * Gets language-specific processing optimizations.
     */
    fun getLanguageProcessingConfig(language: Language): LanguageProcessingConfig {
        return when (language) {
            Language.CHINESE_SIMPLIFIED, Language.CHINESE_TRADITIONAL -> LanguageProcessingConfig(
                requiresSegmentation = true,
                segmentationMethod = SegmentationMethod.CHARACTER_BASED,
                rtlSupport = false,
                complexScript = true,
                preferredFontFamily = "Noto Sans CJK",
                textDirection = TextDirection.LTR
            )
            Language.JAPANESE -> LanguageProcessingConfig(
                requiresSegmentation = true,
                segmentationMethod = SegmentationMethod.MORPHOLOGICAL,
                rtlSupport = false,
                complexScript = true,
                preferredFontFamily = "Noto Sans CJK JP",
                textDirection = TextDirection.LTR
            )
            Language.KOREAN -> LanguageProcessingConfig(
                requiresSegmentation = true,
                segmentationMethod = SegmentationMethod.SYLLABLE_BASED,
                rtlSupport = false,
                complexScript = true,
                preferredFontFamily = "Noto Sans CJK KR",
                textDirection = TextDirection.LTR
            )
            Language.ARABIC -> LanguageProcessingConfig(
                requiresSegmentation = false,
                segmentationMethod = SegmentationMethod.WORD_BASED,
                rtlSupport = true,
                complexScript = true,
                preferredFontFamily = "Noto Sans Arabic",
                textDirection = TextDirection.RTL
            )
            Language.HEBREW -> LanguageProcessingConfig(
                requiresSegmentation = false,
                segmentationMethod = SegmentationMethod.WORD_BASED,
                rtlSupport = true,
                complexScript = true,
                preferredFontFamily = "Noto Sans Hebrew",
                textDirection = TextDirection.RTL
            )
            Language.THAI -> LanguageProcessingConfig(
                requiresSegmentation = true,
                segmentationMethod = SegmentationMethod.DICTIONARY_BASED,
                rtlSupport = false,
                complexScript = true,
                preferredFontFamily = "Noto Sans Thai",
                textDirection = TextDirection.LTR
            )
            Language.HINDI -> LanguageProcessingConfig(
                requiresSegmentation = false,
                segmentationMethod = SegmentationMethod.WORD_BASED,
                rtlSupport = false,
                complexScript = true,
                preferredFontFamily = "Noto Sans Devanagari",
                textDirection = TextDirection.LTR
            )
            else -> LanguageProcessingConfig(
                requiresSegmentation = false,
                segmentationMethod = SegmentationMethod.WORD_BASED,
                rtlSupport = false,
                complexScript = false,
                preferredFontFamily = "Noto Sans",
                textDirection = TextDirection.LTR
            )
        }
    }

    /**
     * Gets supported languages for translation.
     */
    fun getSupportedLanguages(): List<Language> {
        return Language.values().filter { it != Language.AUTO_DETECT }
    }

    /**
     * Checks if a language is supported for translation.
     */
    fun isLanguageSupported(language: Language): Boolean {
        return language != Language.AUTO_DETECT
    }

    /**
     * Gets popular language pairs for translation.
     */
    fun getPopularLanguagePairs(): List<LanguagePair> {
        return listOf(
            LanguagePair(Language.ENGLISH, Language.SPANISH),
            LanguagePair(Language.ENGLISH, Language.FRENCH),
            LanguagePair(Language.ENGLISH, Language.GERMAN),
            LanguagePair(Language.ENGLISH, Language.CHINESE_SIMPLIFIED),
            LanguagePair(Language.ENGLISH, Language.JAPANESE),
            LanguagePair(Language.ENGLISH, Language.KOREAN),
            LanguagePair(Language.SPANISH, Language.ENGLISH),
            LanguagePair(Language.FRENCH, Language.ENGLISH),
            LanguagePair(Language.GERMAN, Language.ENGLISH),
            LanguagePair(Language.CHINESE_SIMPLIFIED, Language.ENGLISH),
            LanguagePair(Language.JAPANESE, Language.ENGLISH),
            LanguagePair(Language.KOREAN, Language.ENGLISH)
        )
    }
}
/**
 * C
onfiguration for language-specific processing optimizations.
 */
data class LanguageProcessingConfig(
    val requiresSegmentation: Boolean,
    val segmentationMethod: SegmentationMethod,
    val rtlSupport: Boolean,
    val complexScript: Boolean,
    val preferredFontFamily: String,
    val textDirection: TextDirection
)

/**
 * Text segmentation methods for different languages.
 */
enum class SegmentationMethod {
    WORD_BASED,
    CHARACTER_BASED,
    SYLLABLE_BASED,
    MORPHOLOGICAL,
    DICTIONARY_BASED
}

/**
 * Text direction for proper rendering.
 */
enum class TextDirection {
    LTR, // Left-to-Right
    RTL  // Right-to-Left
}

/**
 * Language pair for translation.
 */
data class LanguagePair(
    val sourceLanguage: Language,
    val targetLanguage: Language
) {
    override fun toString(): String {
        return "${sourceLanguage.displayName} → ${targetLanguage.displayName}"
    }
}