package com.voicenotesai.data.ai

import com.voicenotesai.domain.ai.*
import com.voicenotesai.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for analyzing content sentiment, themes, categories, and complexity.
 * Provides insights into the content structure and meaning.
 */
@Singleton
class ContentAnalysisService @Inject constructor(
    private val noteCategorizationService: NoteCategorizationService,
    private val enhancedSentimentAnalysisService: EnhancedSentimentAnalysisService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    companion object {
        // Positive sentiment words
        private val POSITIVE_WORDS = setOf(
            "good", "great", "excellent", "amazing", "wonderful", "fantastic", "awesome",
            "love", "like", "enjoy", "happy", "pleased", "satisfied", "delighted",
            "success", "successful", "achievement", "accomplish", "win", "victory",
            "positive", "optimistic", "confident", "excited", "thrilled", "grateful",
            "perfect", "brilliant", "outstanding", "impressive", "remarkable"
        )
        
        // Negative sentiment words
        private val NEGATIVE_WORDS = setOf(
            "bad", "terrible", "awful", "horrible", "disappointing", "frustrating",
            "hate", "dislike", "angry", "upset", "sad", "worried", "concerned",
            "problem", "issue", "trouble", "difficulty", "challenge", "obstacle",
            "fail", "failure", "mistake", "error", "wrong", "broken", "damaged",
            "negative", "pessimistic", "doubtful", "skeptical", "critical"
        )
        
        // Meeting-related keywords
        private val MEETING_KEYWORDS = setOf(
            "meeting", "agenda", "attendees", "discussion", "decision", "action item",
            "follow up", "next steps", "minutes", "conference", "call", "presentation",
            "review", "status", "update", "progress", "deadline", "timeline"
        )
        
        // Brainstorming keywords
        private val BRAINSTORMING_KEYWORDS = setOf(
            "idea", "brainstorm", "creative", "innovation", "concept", "suggestion",
            "possibility", "option", "alternative", "solution", "approach", "strategy",
            "think", "imagine", "consider", "explore", "develop", "generate"
        )
        
        // Task planning keywords
        private val TASK_KEYWORDS = setOf(
            "task", "todo", "action", "plan", "schedule", "organize", "prepare",
            "complete", "finish", "start", "begin", "priority", "urgent", "important",
            "deadline", "due", "assign", "responsible", "owner", "deliverable"
        )
        
        // Research keywords
        private val RESEARCH_KEYWORDS = setOf(
            "research", "study", "analyze", "investigate", "examine", "explore",
            "data", "findings", "results", "conclusion", "hypothesis", "theory",
            "evidence", "source", "reference", "citation", "methodology", "analysis"
        )
        
        // Personal reflection keywords
        private val PERSONAL_KEYWORDS = setOf(
            "feel", "think", "believe", "opinion", "perspective", "experience",
            "learn", "realize", "understand", "reflect", "consider", "remember",
            "personal", "myself", "journey", "growth", "insight", "wisdom"
        )
        
        // Technical complexity indicators
        private val TECHNICAL_WORDS = setOf(
            "algorithm", "implementation", "architecture", "framework", "protocol",
            "database", "server", "client", "API", "interface", "configuration",
            "deployment", "infrastructure", "security", "authentication", "encryption",
            "optimization", "performance", "scalability", "integration", "migration"
        )
        
        // Complex vocabulary indicators
        private val COMPLEX_WORDS = setOf(
            "subsequently", "consequently", "furthermore", "nevertheless", "notwithstanding",
            "comprehensive", "sophisticated", "intricate", "elaborate", "multifaceted",
            "paradigm", "methodology", "systematic", "analytical", "theoretical",
            "empirical", "quantitative", "qualitative", "substantial", "significant"
        )
    }

    /**
     * Analyzes sentiment of the content using enhanced sentiment analysis.
     */
    suspend fun analyzeSentiment(content: String): SentimentScore = withContext(ioDispatcher) {
        val enhancedResult = enhancedSentimentAnalysisService.analyzeEnhancedSentiment(content)
        return@withContext enhancedResult.overallSentiment
    }

    /**
     * Performs comprehensive enhanced sentiment analysis.
     */
    suspend fun analyzeEnhancedSentiment(content: String): EnhancedSentimentResult = withContext(ioDispatcher) {
        return@withContext enhancedSentimentAnalysisService.analyzeEnhancedSentiment(content)
    }

    /**
     * Extracts themes from the content.
     */
    suspend fun extractThemes(content: String): List<Theme> = withContext(ioDispatcher) {
        val themes = mutableListOf<Theme>()
        val words = content.lowercase().split("\\s+".toRegex())
        val wordFrequency = mutableMapOf<String, Int>()
        
        // Count word frequency (excluding common words)
        val commonWords = setOf(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "up", "about", "into", "through", "during",
            "before", "after", "above", "below", "between", "among", "is", "are",
            "was", "were", "be", "been", "being", "have", "has", "had", "do", "does",
            "did", "will", "would", "could", "should", "may", "might", "must", "can"
        )
        
        words.forEach { word ->
            val cleanWord = word.replace("[^a-zA-Z]".toRegex(), "")
            if (cleanWord.length > 3 && !commonWords.contains(cleanWord)) {
                wordFrequency[cleanWord] = wordFrequency.getOrDefault(cleanWord, 0) + 1
            }
        }
        
        // Extract top themes based on frequency
        val topWords = wordFrequency.entries
            .sortedByDescending { it.value }
            .take(10)
        
        topWords.forEach { (word, frequency) ->
            val relevance = frequency.toFloat() / words.size
            if (relevance > 0.01f) { // Only include themes with >1% relevance
                themes.add(
                    Theme(
                        name = word.replaceFirstChar { it.uppercase() },
                        relevance = relevance,
                        keywords = findRelatedKeywords(word, content)
                    )
                )
            }
        }
        
        themes
    }

    /**
     * Categorizes content using the enhanced note categorization service.
     */
    suspend fun categorizeContent(content: String): List<ContentCategory> = withContext(ioDispatcher) {
        val categorization = noteCategorizationService.categorizeNote(content)
        
        val categories = mutableListOf<ContentCategory>()
        categories.add(categorization.primaryCategory)
        categories.addAll(categorization.secondaryCategories.map { it.category })
        
        return@withContext categories.ifEmpty { listOf(ContentCategory.OTHER) }
    }

    /**
     * Performs detailed note categorization with confidence scores.
     */
    suspend fun categorizeNoteDetailed(content: String, transcribedText: String = ""): NoteCategorization = withContext(ioDispatcher) {
        return@withContext noteCategorizationService.categorizeNote(content, transcribedText)
    }

    /**
     * Suggests tags based on content analysis and categorization.
     */
    suspend fun suggestTags(content: String, transcribedText: String = ""): List<String> = withContext(ioDispatcher) {
        val categorization = noteCategorizationService.categorizeNote(content, transcribedText)
        return@withContext noteCategorizationService.suggestTags(content, transcribedText, categorization)
    }

    /**
     * Assesses content complexity.
     */
    suspend fun assessComplexity(content: String): ComplexityScore = withContext(ioDispatcher) {
        val words = content.split("\\s+".toRegex())
        val sentences = content.split("[.!?]+".toRegex()).filter { it.isNotBlank() }
        
        // Calculate average words per sentence
        val avgWordsPerSentence = if (sentences.isNotEmpty()) {
            words.size.toFloat() / sentences.size
        } else 0f
        
        // Count syllables (rough approximation)
        val avgSyllablesPerWord = words.map { countSyllables(it) }.average().toFloat()
        
        // Calculate Flesch Reading Ease Score (approximation)
        val fleschScore = if (sentences.isNotEmpty() && words.isNotEmpty()) {
            206.835f - (1.015f * avgWordsPerSentence) - (84.6f * avgSyllablesPerWord)
        } else 50f
        
        // Determine technical level
        val technicalWordCount = words.count { word ->
            TECHNICAL_WORDS.contains(word.lowercase().replace("[^a-zA-Z]".toRegex(), ""))
        }
        val technicalRatio = technicalWordCount.toFloat() / words.size
        
        val technicalLevel = when {
            technicalRatio > 0.1f -> TechnicalLevel.EXPERT
            technicalRatio > 0.05f -> TechnicalLevel.ADVANCED
            technicalRatio > 0.02f -> TechnicalLevel.INTERMEDIATE
            else -> TechnicalLevel.BASIC
        }
        
        // Calculate vocabulary complexity
        val complexWordCount = words.count { word ->
            val cleanWord = word.lowercase().replace("[^a-zA-Z]".toRegex(), "")
            COMPLEX_WORDS.contains(cleanWord) || cleanWord.length > 8
        }
        val vocabularyComplexity = complexWordCount.toFloat() / words.size
        
        ComplexityScore(
            readabilityScore = fleschScore.coerceIn(0f, 100f),
            technicalLevel = technicalLevel,
            vocabularyComplexity = vocabularyComplexity
        )
    }

    /**
     * Finds related keywords for a given theme word.
     */
    private fun findRelatedKeywords(themeWord: String, content: String): List<String> {
        val words = content.lowercase().split("\\s+".toRegex())
        val relatedWords = mutableSetOf<String>()
        
        // Find words that appear near the theme word
        words.forEachIndexed { index, word ->
            val cleanWord = word.replace("[^a-zA-Z]".toRegex(), "")
            if (cleanWord == themeWord) {
                // Look at surrounding words (±3 positions)
                for (i in (index - 3).coerceAtLeast(0)..(index + 3).coerceAtMost(words.size - 1)) {
                    if (i != index) {
                        val nearbyWord = words[i].replace("[^a-zA-Z]".toRegex(), "")
                        if (nearbyWord.length > 3) {
                            relatedWords.add(nearbyWord)
                        }
                    }
                }
            }
        }
        
        return relatedWords.take(5).toList()
    }

    /**
     * Counts syllables in a word (rough approximation).
     */
    private fun countSyllables(word: String): Int {
        val cleanWord = word.lowercase().replace("[^a-zA-Z]".toRegex(), "")
        if (cleanWord.isEmpty()) return 0
        
        var syllables = 0
        var previousWasVowel = false
        
        cleanWord.forEach { char ->
            val isVowel = char in "aeiouy"
            if (isVowel && !previousWasVowel) {
                syllables++
            }
            previousWasVowel = isVowel
        }
        
        // Handle silent 'e'
        if (cleanWord.endsWith("e") && syllables > 1) {
            syllables--
        }
        
        return syllables.coerceAtLeast(1)
    }
}