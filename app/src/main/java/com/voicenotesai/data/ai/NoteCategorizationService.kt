package com.voicenotesai.data.ai

import com.voicenotesai.domain.ai.ContentCategory
import com.voicenotesai.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for automatically categorizing notes based on content analysis.
 * Uses pattern matching and keyword analysis to determine note types.
 */
@Singleton
class NoteCategorizationService @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    companion object {
        // Meeting indicators with weights
        private val MEETING_INDICATORS = mapOf(
            "meeting" to 3.0f,
            "agenda" to 2.5f,
            "attendees" to 2.5f,
            "discussion" to 2.0f,
            "decision" to 2.0f,
            "action item" to 2.5f,
            "follow up" to 2.0f,
            "next steps" to 2.0f,
            "minutes" to 3.0f,
            "conference" to 2.0f,
            "call" to 1.5f,
            "presentation" to 2.0f,
            "review" to 1.5f,
            "status" to 1.5f,
            "update" to 1.5f,
            "progress" to 1.5f,
            "deadline" to 2.0f,
            "timeline" to 2.0f,
            "stakeholder" to 2.0f,
            "team" to 1.5f,
            "project" to 1.5f
        )
        
        // Brainstorming indicators
        private val BRAINSTORMING_INDICATORS = mapOf(
            "brainstorm" to 3.0f,
            "idea" to 2.5f,
            "creative" to 2.0f,
            "innovation" to 2.5f,
            "concept" to 2.0f,
            "suggestion" to 2.0f,
            "possibility" to 2.0f,
            "option" to 1.5f,
            "alternative" to 2.0f,
            "solution" to 2.0f,
            "approach" to 1.5f,
            "strategy" to 2.0f,
            "think" to 1.0f,
            "imagine" to 2.0f,
            "consider" to 1.0f,
            "explore" to 1.5f,
            "develop" to 1.5f,
            "generate" to 2.0f,
            "inspiration" to 2.5f,
            "breakthrough" to 2.5f
        )
        
        // Task planning indicators
        private val TASK_INDICATORS = mapOf(
            "task" to 3.0f,
            "todo" to 3.0f,
            "action" to 2.0f,
            "plan" to 2.5f,
            "schedule" to 2.5f,
            "organize" to 2.0f,
            "prepare" to 2.0f,
            "complete" to 2.0f,
            "finish" to 2.0f,
            "start" to 1.5f,
            "begin" to 1.5f,
            "priority" to 2.5f,
            "urgent" to 2.5f,
            "important" to 2.0f,
            "deadline" to 2.5f,
            "due" to 2.5f,
            "assign" to 2.0f,
            "responsible" to 2.0f,
            "owner" to 2.0f,
            "deliverable" to 2.5f,
            "milestone" to 2.0f,
            "checklist" to 3.0f
        )
        
        // Research indicators
        private val RESEARCH_INDICATORS = mapOf(
            "research" to 3.0f,
            "study" to 2.5f,
            "analyze" to 2.5f,
            "investigate" to 2.5f,
            "examine" to 2.0f,
            "explore" to 1.5f,
            "data" to 2.0f,
            "findings" to 2.5f,
            "results" to 2.0f,
            "conclusion" to 2.5f,
            "hypothesis" to 2.5f,
            "theory" to 2.0f,
            "evidence" to 2.0f,
            "source" to 2.0f,
            "reference" to 2.0f,
            "citation" to 2.5f,
            "methodology" to 2.5f,
            "analysis" to 2.0f,
            "experiment" to 2.5f,
            "observation" to 2.0f
        )
        
        // Personal reflection indicators
        private val PERSONAL_INDICATORS = mapOf(
            "feel" to 2.0f,
            "think" to 1.5f,
            "believe" to 2.0f,
            "opinion" to 2.0f,
            "perspective" to 2.0f,
            "experience" to 2.0f,
            "learn" to 1.5f,
            "realize" to 2.0f,
            "understand" to 1.5f,
            "reflect" to 3.0f,
            "consider" to 1.0f,
            "remember" to 1.5f,
            "personal" to 2.5f,
            "myself" to 2.0f,
            "journey" to 2.5f,
            "growth" to 2.0f,
            "insight" to 2.5f,
            "wisdom" to 2.5f,
            "emotion" to 2.0f,
            "feeling" to 2.0f
        )
        
        // Interview indicators
        private val INTERVIEW_INDICATORS = mapOf(
            "interview" to 3.0f,
            "candidate" to 2.5f,
            "interviewer" to 2.5f,
            "question" to 2.0f,
            "answer" to 2.0f,
            "qualification" to 2.0f,
            "experience" to 1.5f,
            "skill" to 2.0f,
            "background" to 2.0f,
            "position" to 2.0f,
            "role" to 1.5f,
            "hire" to 2.5f,
            "hiring" to 2.5f,
            "resume" to 2.5f,
            "cv" to 2.5f,
            "portfolio" to 2.0f,
            "reference" to 2.0f,
            "salary" to 2.0f,
            "compensation" to 2.0f
        )
        
        // Lecture indicators
        private val LECTURE_INDICATORS = mapOf(
            "lecture" to 3.0f,
            "professor" to 2.5f,
            "class" to 2.0f,
            "course" to 2.5f,
            "student" to 2.0f,
            "lesson" to 2.5f,
            "chapter" to 2.0f,
            "textbook" to 2.0f,
            "assignment" to 2.0f,
            "homework" to 2.0f,
            "exam" to 2.0f,
            "test" to 1.5f,
            "grade" to 2.0f,
            "semester" to 2.0f,
            "university" to 2.0f,
            "college" to 2.0f,
            "school" to 1.5f,
            "education" to 2.0f,
            "academic" to 2.0f
        )
        
        // Phone call indicators
        private val PHONE_CALL_INDICATORS = mapOf(
            "phone" to 2.5f,
            "call" to 2.0f,
            "spoke with" to 2.5f,
            "talked to" to 2.0f,
            "conversation" to 2.0f,
            "discussed" to 1.5f,
            "caller" to 2.5f,
            "voicemail" to 3.0f,
            "dial" to 2.0f,
            "ring" to 2.0f,
            "hang up" to 2.0f,
            "callback" to 2.5f,
            "telephone" to 2.5f,
            "mobile" to 1.5f,
            "cell" to 1.5f
        )
        
        // Reminder indicators
        private val REMINDER_INDICATORS = mapOf(
            "remind" to 3.0f,
            "reminder" to 3.0f,
            "don't forget" to 3.0f,
            "remember" to 2.5f,
            "note to self" to 3.0f,
            "important" to 2.0f,
            "urgent" to 2.0f,
            "later" to 1.5f,
            "tomorrow" to 2.0f,
            "next week" to 2.0f,
            "follow up" to 2.0f,
            "check" to 1.5f,
            "verify" to 1.5f,
            "confirm" to 1.5f
        )
        
        // Idea capture indicators
        private val IDEA_INDICATORS = mapOf(
            "idea" to 3.0f,
            "thought" to 2.5f,
            "inspiration" to 2.5f,
            "concept" to 2.0f,
            "innovation" to 2.0f,
            "creative" to 2.0f,
            "invention" to 2.5f,
            "breakthrough" to 2.5f,
            "eureka" to 3.0f,
            "lightbulb" to 2.5f,
            "spark" to 2.0f,
            "vision" to 2.0f,
            "dream" to 1.5f,
            "imagine" to 2.0f,
            "what if" to 2.5f,
            "maybe" to 1.0f,
            "could" to 1.0f,
            "potential" to 1.5f
        )
    }

    /**
     * Automatically categorizes a note based on its content.
     */
    suspend fun categorizeNote(content: String, transcribedText: String = ""): NoteCategorization = withContext(ioDispatcher) {
        val fullText = "$content $transcribedText".lowercase()
        
        // Calculate scores for each category
        val categoryScores = mutableMapOf<ContentCategory, Float>()
        
        categoryScores[ContentCategory.MEETING] = calculateCategoryScore(fullText, MEETING_INDICATORS)
        categoryScores[ContentCategory.BRAINSTORMING] = calculateCategoryScore(fullText, BRAINSTORMING_INDICATORS)
        categoryScores[ContentCategory.TASK_PLANNING] = calculateCategoryScore(fullText, TASK_INDICATORS)
        categoryScores[ContentCategory.RESEARCH_NOTES] = calculateCategoryScore(fullText, RESEARCH_INDICATORS)
        categoryScores[ContentCategory.PERSONAL_REFLECTION] = calculateCategoryScore(fullText, PERSONAL_INDICATORS)
        categoryScores[ContentCategory.INTERVIEW] = calculateCategoryScore(fullText, INTERVIEW_INDICATORS)
        categoryScores[ContentCategory.LECTURE] = calculateCategoryScore(fullText, LECTURE_INDICATORS)
        categoryScores[ContentCategory.PHONE_CALL] = calculateCategoryScore(fullText, PHONE_CALL_INDICATORS)
        categoryScores[ContentCategory.REMINDER] = calculateCategoryScore(fullText, REMINDER_INDICATORS)
        categoryScores[ContentCategory.IDEA_CAPTURE] = calculateCategoryScore(fullText, IDEA_INDICATORS)
        
        // Find the category with the highest score
        val topCategory = categoryScores.maxByOrNull { it.value }
        val confidence = topCategory?.value ?: 0f
        
        // Set minimum confidence threshold
        val primaryCategory = if (confidence > 0.5f) {
            topCategory?.key ?: ContentCategory.OTHER
        } else {
            ContentCategory.OTHER
        }
        
        // Get secondary categories (scores > 0.3)
        val secondaryCategories = categoryScores
            .filter { it.value > 0.3f && it.key != primaryCategory }
            .map { CategoryScore(it.key, it.value) }
            .sortedByDescending { it.score }
            .take(2)
        
        NoteCategorization(
            primaryCategory = primaryCategory,
            confidence = confidence,
            secondaryCategories = secondaryCategories,
            categoryScores = categoryScores.mapValues { it.value }
        )
    }

    /**
     * Calculates the score for a specific category based on keyword matches.
     */
    private fun calculateCategoryScore(text: String, indicators: Map<String, Float>): Float {
        var totalScore = 0f
        var matchCount = 0
        
        indicators.forEach { (keyword, weight) ->
            val matches = countKeywordMatches(text, keyword)
            if (matches > 0) {
                totalScore += weight * matches
                matchCount++
            }
        }
        
        // Normalize score based on text length and number of matches
        val textLength = text.split("\\s+".toRegex()).size
        val normalizedScore = if (textLength > 0) {
            (totalScore / textLength) * (1 + matchCount * 0.1f)
        } else 0f
        
        return normalizedScore.coerceAtMost(5.0f) // Cap at 5.0
    }

    /**
     * Counts keyword matches in text, including partial matches and variations.
     */
    private fun countKeywordMatches(text: String, keyword: String): Int {
        var count = 0
        
        // Exact phrase match
        if (text.contains(keyword)) {
            count += text.split(keyword).size - 1
        }
        
        // Word boundary matches for single words
        if (!keyword.contains(" ")) {
            val wordPattern = "\\b${keyword}\\b".toRegex()
            count += wordPattern.findAll(text).count()
        }
        
        return count
    }

    /**
     * Suggests tags based on the categorization and content analysis.
     */
    suspend fun suggestTags(
        content: String,
        transcribedText: String = "",
        categorization: NoteCategorization
    ): List<String> = withContext(ioDispatcher) {
        val tags = mutableSetOf<String>()
        val fullText = "$content $transcribedText".lowercase()
        
        // Add category-based tags
        when (categorization.primaryCategory) {
            ContentCategory.MEETING -> {
                tags.addAll(listOf("meeting", "discussion", "team"))
                if (fullText.contains("action")) tags.add("action-items")
                if (fullText.contains("decision")) tags.add("decisions")
            }
            ContentCategory.BRAINSTORMING -> {
                tags.addAll(listOf("brainstorming", "ideas", "creative"))
                if (fullText.contains("innovation")) tags.add("innovation")
            }
            ContentCategory.TASK_PLANNING -> {
                tags.addAll(listOf("tasks", "planning", "todo"))
                if (fullText.contains("priority")) tags.add("priority")
                if (fullText.contains("urgent")) tags.add("urgent")
            }
            ContentCategory.RESEARCH_NOTES -> {
                tags.addAll(listOf("research", "analysis", "study"))
                if (fullText.contains("data")) tags.add("data")
            }
            ContentCategory.PERSONAL_REFLECTION -> {
                tags.addAll(listOf("personal", "reflection", "thoughts"))
                if (fullText.contains("growth")) tags.add("growth")
            }
            ContentCategory.INTERVIEW -> {
                tags.addAll(listOf("interview", "candidate", "hiring"))
            }
            ContentCategory.LECTURE -> {
                tags.addAll(listOf("lecture", "education", "learning"))
            }
            ContentCategory.PHONE_CALL -> {
                tags.addAll(listOf("phone-call", "conversation"))
            }
            ContentCategory.REMINDER -> {
                tags.addAll(listOf("reminder", "important"))
            }
            ContentCategory.IDEA_CAPTURE -> {
                tags.addAll(listOf("idea", "inspiration", "concept"))
            }
            ContentCategory.OTHER -> {
                tags.add("general")
            }
        }
        
        // Add time-based tags
        val currentHour = java.time.LocalTime.now().hour
        when (currentHour) {
            in 6..11 -> tags.add("morning")
            in 12..17 -> tags.add("afternoon")
            in 18..21 -> tags.add("evening")
            else -> tags.add("night")
        }
        
        // Add urgency tags based on keywords
        if (fullText.contains("urgent") || fullText.contains("asap") || fullText.contains("immediately")) {
            tags.add("urgent")
        }
        
        if (fullText.contains("important") || fullText.contains("critical") || fullText.contains("priority")) {
            tags.add("important")
        }
        
        return@withContext tags.take(8).toList() // Limit to 8 tags
    }
}

/**
 * Result of note categorization analysis.
 */
data class NoteCategorization(
    val primaryCategory: ContentCategory,
    val confidence: Float,
    val secondaryCategories: List<CategoryScore>,
    val categoryScores: Map<ContentCategory, Float>
)

/**
 * Category score for secondary categories.
 */
data class CategoryScore(
    val category: ContentCategory,
    val score: Float
)