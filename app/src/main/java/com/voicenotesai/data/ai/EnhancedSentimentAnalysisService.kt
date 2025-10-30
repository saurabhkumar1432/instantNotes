package com.voicenotesai.data.ai

import com.voicenotesai.domain.ai.SentimentScore
import com.voicenotesai.domain.ai.Theme
import com.voicenotesai.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced sentiment analysis service that provides detailed sentiment insights
 * and key theme identification with emotional context.
 */
@Singleton
class EnhancedSentimentAnalysisService @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    companion object {
        // Positive emotion indicators with intensity weights
        private val POSITIVE_EMOTIONS = mapOf(
            // Joy and happiness
            "happy" to 2.5f,
            "joy" to 2.5f,
            "joyful" to 2.5f,
            "delighted" to 2.8f,
            "thrilled" to 3.0f,
            "ecstatic" to 3.2f,
            "elated" to 3.0f,
            "cheerful" to 2.3f,
            "upbeat" to 2.2f,
            "optimistic" to 2.4f,
            
            // Satisfaction and contentment
            "satisfied" to 2.2f,
            "content" to 2.0f,
            "pleased" to 2.1f,
            "fulfilled" to 2.6f,
            "accomplished" to 2.7f,
            "proud" to 2.5f,
            "successful" to 2.4f,
            "achievement" to 2.3f,
            
            // Love and affection
            "love" to 2.8f,
            "adore" to 2.9f,
            "cherish" to 2.7f,
            "appreciate" to 2.2f,
            "grateful" to 2.4f,
            "thankful" to 2.3f,
            
            // Excitement and enthusiasm
            "excited" to 2.6f,
            "enthusiastic" to 2.7f,
            "passionate" to 2.8f,
            "energetic" to 2.4f,
            "motivated" to 2.5f,
            "inspired" to 2.6f,
            
            // Positive descriptors
            "amazing" to 2.4f,
            "awesome" to 2.5f,
            "fantastic" to 2.6f,
            "wonderful" to 2.5f,
            "excellent" to 2.3f,
            "outstanding" to 2.7f,
            "brilliant" to 2.6f,
            "perfect" to 2.8f,
            "great" to 2.0f,
            "good" to 1.8f,
            "nice" to 1.6f,
            "fine" to 1.4f
        )
        
        // Negative emotion indicators with intensity weights
        private val NEGATIVE_EMOTIONS = mapOf(
            // Sadness and depression
            "sad" to -2.3f,
            "depressed" to -3.0f,
            "miserable" to -3.2f,
            "devastated" to -3.5f,
            "heartbroken" to -3.3f,
            "disappointed" to -2.4f,
            "discouraged" to -2.5f,
            "hopeless" to -3.1f,
            "despair" to -3.4f,
            
            // Anger and frustration
            "angry" to -2.6f,
            "furious" to -3.2f,
            "rage" to -3.4f,
            "frustrated" to -2.4f,
            "annoyed" to -2.0f,
            "irritated" to -2.1f,
            "outraged" to -3.0f,
            "livid" to -3.3f,
            
            // Fear and anxiety
            "afraid" to -2.5f,
            "scared" to -2.4f,
            "terrified" to -3.1f,
            "anxious" to -2.3f,
            "worried" to -2.2f,
            "nervous" to -2.0f,
            "panic" to -3.0f,
            "stressed" to -2.4f,
            
            // Disgust and contempt
            "disgusted" to -2.7f,
            "revolted" to -2.9f,
            "appalled" to -2.8f,
            "contempt" to -2.6f,
            "disdain" to -2.5f,
            
            // Negative descriptors
            "terrible" to -2.6f,
            "awful" to -2.7f,
            "horrible" to -2.8f,
            "bad" to -1.8f,
            "poor" to -1.6f,
            "disappointing" to -2.2f,
            "failed" to -2.4f,
            "failure" to -2.5f,
            "problem" to -1.8f,
            "issue" to -1.6f,
            "trouble" to -2.0f,
            "difficult" to -1.7f,
            "challenging" to -1.4f, // Can be positive in some contexts
            "struggle" to -2.1f,
            "crisis" to -2.8f,
            "disaster" to -3.0f
        )
        
        // Neutral emotion indicators
        private val NEUTRAL_EMOTIONS = mapOf(
            "okay" to 0.0f,
            "fine" to 0.1f,
            "normal" to 0.0f,
            "average" to 0.0f,
            "typical" to 0.0f,
            "usual" to 0.0f,
            "standard" to 0.0f,
            "regular" to 0.0f
        )
        
        // Emotional intensity modifiers
        private val INTENSITY_MODIFIERS = mapOf(
            "very" to 1.3f,
            "extremely" to 1.5f,
            "incredibly" to 1.4f,
            "absolutely" to 1.4f,
            "completely" to 1.3f,
            "totally" to 1.3f,
            "really" to 1.2f,
            "quite" to 1.1f,
            "somewhat" to 0.8f,
            "slightly" to 0.7f,
            "a bit" to 0.6f,
            "kind of" to 0.7f,
            "sort of" to 0.7f
        )
        
        // Negation words that flip sentiment
        private val NEGATION_WORDS = setOf(
            "not", "no", "never", "nothing", "nobody", "nowhere", "neither", "nor",
            "none", "hardly", "scarcely", "barely", "doesn't", "don't", "won't",
            "wouldn't", "couldn't", "shouldn't", "can't", "isn't", "aren't", "wasn't", "weren't"
        )
    }

    /**
     * Performs enhanced sentiment analysis with emotional context and intensity.
     */
    suspend fun analyzeEnhancedSentiment(content: String): EnhancedSentimentResult = withContext(ioDispatcher) {
        val words = content.lowercase().split("\\s+".toRegex())
        val sentences = content.split("[.!?]+".toRegex()).filter { it.isNotBlank() }
        
        var totalSentimentScore = 0f
        var emotionCounts: MutableMap<EmotionCategory, Int>
        var sentimentDistribution = mutableListOf<Float>()
        val emotionalPhrases = mutableListOf<EmotionalPhrase>()
        
        // Analyze each sentence for sentiment
        sentences.forEach { sentence ->
            val sentenceScore = analyzeSentenceSentiment(sentence.lowercase())
            sentimentDistribution.add(sentenceScore)
            totalSentimentScore += sentenceScore
            
            // Extract emotional phrases from sentence
            emotionalPhrases.addAll(extractEmotionalPhrases(sentence))
        }
        
        // Calculate overall sentiment metrics
        val averageSentiment = if (sentences.isNotEmpty()) totalSentimentScore / sentences.size else 0f
        val sentimentVariance = calculateVariance(sentimentDistribution, averageSentiment)
        val emotionalStability = 1f - (sentimentVariance / 4f).coerceAtMost(1f) // Normalize to 0-1
        
        // Categorize emotions
        emotionCounts = categorizeEmotions(words)
        
        // Calculate confidence based on emotional indicators found
        val emotionalIndicatorCount = emotionalPhrases.size
        val confidence = (emotionalIndicatorCount.toFloat() / words.size * 10).coerceAtMost(1f)
        
        // Determine dominant emotion
        val dominantEmotion = determineDominantEmotion(emotionCounts, averageSentiment)
        
        // Extract key themes with emotional context
        val emotionalThemes = extractEmotionalThemes(content, emotionalPhrases)
        
        EnhancedSentimentResult(
            overallSentiment = SentimentScore(
                overall = averageSentiment,
                positive = sentimentDistribution.count { it > 0.1f }.toFloat() / sentences.size,
                negative = sentimentDistribution.count { it < -0.1f }.toFloat() / sentences.size,
                neutral = sentimentDistribution.count { it >= -0.1f && it <= 0.1f }.toFloat() / sentences.size,
                confidence = confidence
            ),
            emotionalStability = emotionalStability,
            dominantEmotion = dominantEmotion,
            emotionDistribution = emotionCounts,
            emotionalPhrases = emotionalPhrases,
            emotionalThemes = emotionalThemes,
            sentimentProgression = sentimentDistribution
        )
    }

    /**
     * Analyzes sentiment of a single sentence with context awareness.
     */
    private fun analyzeSentenceSentiment(sentence: String): Float {
        val words = sentence.split("\\s+".toRegex())
        var sentimentScore = 0f
        var negationActive = false
        var intensityMultiplier = 1f
        
        for (i in words.indices) {
            val word = words[i].replace("[^a-zA-Z]".toRegex(), "")
            
            // Check for negation
            if (NEGATION_WORDS.contains(word)) {
                negationActive = true
                continue
            }
            
            // Check for intensity modifiers
            if (INTENSITY_MODIFIERS.containsKey(word)) {
                intensityMultiplier = INTENSITY_MODIFIERS[word] ?: 1f
                continue
            }
            
            // Check for emotional words
            var wordSentiment = 0f
            when {
                POSITIVE_EMOTIONS.containsKey(word) -> wordSentiment = POSITIVE_EMOTIONS[word] ?: 0f
                NEGATIVE_EMOTIONS.containsKey(word) -> wordSentiment = NEGATIVE_EMOTIONS[word] ?: 0f
                NEUTRAL_EMOTIONS.containsKey(word) -> wordSentiment = NEUTRAL_EMOTIONS[word] ?: 0f
            }
            
            // Apply intensity and negation
            if (wordSentiment != 0f) {
                wordSentiment *= intensityMultiplier
                if (negationActive) {
                    wordSentiment *= -0.8f // Negation reduces but doesn't completely flip
                }
                sentimentScore += wordSentiment
                
                // Reset modifiers after applying to emotional word
                negationActive = false
                intensityMultiplier = 1f
            }
        }
        
        // Normalize by sentence length
        return if (words.isNotEmpty()) sentimentScore / words.size else 0f
    }

    /**
     * Extracts emotional phrases with their context and intensity.
     */
    private fun extractEmotionalPhrases(sentence: String): List<EmotionalPhrase> {
        val phrases = mutableListOf<EmotionalPhrase>()
        val words = sentence.split("\\s+".toRegex())
        
        for (i in words.indices) {
            val word = words[i].lowercase().replace("[^a-zA-Z]".toRegex(), "")
            
            if (POSITIVE_EMOTIONS.containsKey(word) || NEGATIVE_EMOTIONS.containsKey(word)) {
                val intensity = POSITIVE_EMOTIONS[word] ?: NEGATIVE_EMOTIONS[word] ?: 0f
                val context = extractPhraseContext(words, i)
                
                phrases.add(
                    EmotionalPhrase(
                        phrase = context,
                        emotion = categorizeWordEmotion(word),
                        intensity = kotlin.math.abs(intensity),
                        sentiment = if (intensity > 0) "positive" else "negative"
                    )
                )
            }
        }
        
        return phrases
    }

    /**
     * Extracts context around an emotional word.
     */
    private fun extractPhraseContext(words: List<String>, index: Int): String {
        val start = (index - 2).coerceAtLeast(0)
        val end = (index + 3).coerceAtMost(words.size)
        return words.subList(start, end).joinToString(" ")
    }

    /**
     * Categorizes emotions into broader categories.
     */
    private fun categorizeEmotions(words: List<String>): MutableMap<EmotionCategory, Int> {
        val emotionCounts = mutableMapOf<EmotionCategory, Int>()
        
        words.forEach { word ->
            val cleanWord = word.lowercase().replace("[^a-zA-Z]".toRegex(), "")
            val emotion = categorizeWordEmotion(cleanWord)
            if (emotion != EmotionCategory.NEUTRAL) {
                emotionCounts[emotion] = emotionCounts.getOrDefault(emotion, 0) + 1
            }
        }
        
        return emotionCounts
    }

    /**
     * Categorizes a word into an emotion category.
     */
    private fun categorizeWordEmotion(word: String): EmotionCategory {
        return when {
            word in listOf("happy", "joy", "joyful", "delighted", "thrilled", "ecstatic", "elated", "cheerful") -> EmotionCategory.JOY
            word in listOf("love", "adore", "cherish", "appreciate", "grateful", "thankful") -> EmotionCategory.LOVE
            word in listOf("excited", "enthusiastic", "passionate", "energetic", "motivated", "inspired") -> EmotionCategory.EXCITEMENT
            word in listOf("satisfied", "content", "pleased", "fulfilled", "accomplished", "proud") -> EmotionCategory.SATISFACTION
            word in listOf("sad", "depressed", "miserable", "devastated", "heartbroken", "disappointed") -> EmotionCategory.SADNESS
            word in listOf("angry", "furious", "rage", "frustrated", "annoyed", "irritated", "outraged") -> EmotionCategory.ANGER
            word in listOf("afraid", "scared", "terrified", "anxious", "worried", "nervous", "panic") -> EmotionCategory.FEAR
            word in listOf("disgusted", "revolted", "appalled", "contempt", "disdain") -> EmotionCategory.DISGUST
            POSITIVE_EMOTIONS.containsKey(word) -> EmotionCategory.POSITIVE_OTHER
            NEGATIVE_EMOTIONS.containsKey(word) -> EmotionCategory.NEGATIVE_OTHER
            else -> EmotionCategory.NEUTRAL
        }
    }

    /**
     * Determines the dominant emotion from the emotion distribution.
     */
    private fun determineDominantEmotion(emotionCounts: Map<EmotionCategory, Int>, averageSentiment: Float): EmotionCategory {
        if (emotionCounts.isEmpty()) {
            return when {
                averageSentiment > 0.5f -> EmotionCategory.POSITIVE_OTHER
                averageSentiment < -0.5f -> EmotionCategory.NEGATIVE_OTHER
                else -> EmotionCategory.NEUTRAL
            }
        }
        
        return emotionCounts.maxByOrNull { it.value }?.key ?: EmotionCategory.NEUTRAL
    }

    /**
     * Extracts themes with emotional context.
     */
    private fun extractEmotionalThemes(content: String, emotionalPhrases: List<EmotionalPhrase>): List<Theme> {
        val themes = mutableListOf<Theme>()
        
        // Group emotional phrases by similar emotions
        val emotionGroups = emotionalPhrases.groupBy { it.emotion }
        
        emotionGroups.forEach { (emotion, phrases) ->
            if (phrases.size >= 2) { // Only create themes with multiple instances
                val keywords = phrases.map { phrase ->
                    phrase.phrase.split("\\s+".toRegex())
                        .filter { it.length > 3 }
                        .take(2)
                }.flatten().distinct()
                
                val relevance = phrases.map { it.intensity }.sum() / content.length * 100
                
                themes.add(
                    Theme(
                        name = emotion.displayName,
                        relevance = relevance,
                        keywords = keywords.take(5)
                    )
                )
            }
        }
        
        return themes.sortedByDescending { it.relevance }.take(5)
    }

    /**
     * Calculates variance of sentiment scores.
     */
    private fun calculateVariance(scores: List<Float>, mean: Float): Float {
        if (scores.isEmpty()) return 0f
        
        val sumSquaredDifferences = scores.map { (it - mean) * (it - mean) }.sum()
        return (sumSquaredDifferences / scores.size).toFloat()
    }
}

/**
 * Enhanced sentiment analysis result with detailed emotional insights.
 */
data class EnhancedSentimentResult(
    val overallSentiment: SentimentScore,
    val emotionalStability: Float, // 0-1, higher = more stable emotions
    val dominantEmotion: EmotionCategory,
    val emotionDistribution: Map<EmotionCategory, Int>,
    val emotionalPhrases: List<EmotionalPhrase>,
    val emotionalThemes: List<Theme>,
    val sentimentProgression: List<Float> // Sentiment score per sentence
)

/**
 * Emotional phrase with context and intensity.
 */
data class EmotionalPhrase(
    val phrase: String,
    val emotion: EmotionCategory,
    val intensity: Float,
    val sentiment: String // "positive", "negative", "neutral"
)

/**
 * Emotion categories for detailed analysis.
 */
enum class EmotionCategory(val displayName: String) {
    JOY("Joy & Happiness"),
    LOVE("Love & Affection"),
    EXCITEMENT("Excitement & Enthusiasm"),
    SATISFACTION("Satisfaction & Pride"),
    SADNESS("Sadness & Disappointment"),
    ANGER("Anger & Frustration"),
    FEAR("Fear & Anxiety"),
    DISGUST("Disgust & Contempt"),
    POSITIVE_OTHER("Other Positive"),
    NEGATIVE_OTHER("Other Negative"),
    NEUTRAL("Neutral")
}