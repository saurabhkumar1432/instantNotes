package com.voicenotesai.domain.ai

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Content categories for automatic note classification.
 * Each category has associated colors and icons for visual organization.
 */
enum class ContentCategory(
    val displayName: String,
    val description: String,
    val defaultColor: Color,
    val icon: ImageVector,
    val isCustom: Boolean = false
) {
    WORK(
        displayName = "Work",
        description = "Work-related notes, meetings, and tasks",
        defaultColor = Color(0xFF6366F1), // Indigo
        icon = Icons.Default.Work
    ),
    PERSONAL(
        displayName = "Personal",
        description = "Personal thoughts, reflections, and private notes",
        defaultColor = Color(0xFF10B981), // Green
        icon = Icons.Default.Person
    ),
    IDEAS(
        displayName = "Ideas",
        description = "Creative ideas, brainstorming, and inspiration",
        defaultColor = Color(0xFF8B5CF6), // Purple
        icon = Icons.Default.Lightbulb
    ),
    MEETINGS(
        displayName = "Meetings",
        description = "Meeting notes, discussions, and action items",
        defaultColor = Color(0xFF3B82F6), // Blue
        icon = Icons.Default.Groups
    ),
    SHOPPING(
        displayName = "Shopping",
        description = "Shopping lists, purchases, and product notes",
        defaultColor = Color(0xFFF59E0B), // Amber
        icon = Icons.Default.ShoppingCart
    ),
    TASKS(
        displayName = "Tasks",
        description = "Task planning, todo lists, and action items",
        defaultColor = Color(0xFFEF4444), // Red
        icon = Icons.Default.Task
    ),
    RESEARCH(
        displayName = "Research",
        description = "Research notes, studies, and analysis",
        defaultColor = Color(0xFF06B6D4), // Cyan
        icon = Icons.Default.Science
    ),
    EDUCATION(
        displayName = "Education",
        description = "Learning notes, lectures, and educational content",
        defaultColor = Color(0xFF84CC16), // Lime
        icon = Icons.Default.School
    ),
    HEALTH(
        displayName = "Health",
        description = "Health-related notes, appointments, and wellness",
        defaultColor = Color(0xFFEC4899), // Pink
        icon = Icons.Default.LocalHospital
    ),
    TRAVEL(
        displayName = "Travel",
        description = "Travel plans, itineraries, and trip notes",
        defaultColor = Color(0xFF14B8A6), // Teal
        icon = Icons.Default.Flight
    ),
    
    // Legacy categories for backward compatibility
    MEETING(
        displayName = "Meeting",
        description = "Meeting notes and discussions",
        defaultColor = Color(0xFF3B82F6),
        icon = Icons.Default.Groups
    ),
    BRAINSTORMING(
        displayName = "Brainstorming",
        description = "Creative brainstorming sessions",
        defaultColor = Color(0xFF8B5CF6),
        icon = Icons.Default.Psychology
    ),
    TASK_PLANNING(
        displayName = "Task Planning",
        description = "Task and project planning",
        defaultColor = Color(0xFFEF4444),
        icon = Icons.Default.Assignment
    ),
    RESEARCH_NOTES(
        displayName = "Research Notes",
        description = "Research and analysis notes",
        defaultColor = Color(0xFF06B6D4),
        icon = Icons.Default.Science
    ),
    PERSONAL_REFLECTION(
        displayName = "Personal Reflection",
        description = "Personal thoughts and reflections",
        defaultColor = Color(0xFF10B981),
        icon = Icons.Default.SelfImprovement
    ),
    INTERVIEW(
        displayName = "Interview",
        description = "Interview notes and candidate evaluations",
        defaultColor = Color(0xFF6366F1),
        icon = Icons.Default.RecordVoiceOver
    ),
    LECTURE(
        displayName = "Lecture",
        description = "Educational lectures and learning notes",
        defaultColor = Color(0xFF84CC16),
        icon = Icons.Default.School
    ),
    PHONE_CALL(
        displayName = "Phone Call",
        description = "Phone call notes and conversations",
        defaultColor = Color(0xFF8B5CF6),
        icon = Icons.Default.Phone
    ),
    REMINDER(
        displayName = "Reminder",
        description = "Reminders and important notes",
        defaultColor = Color(0xFFF59E0B),
        icon = Icons.Default.Alarm
    ),
    IDEA_CAPTURE(
        displayName = "Idea Capture",
        description = "Quick idea captures and inspiration",
        defaultColor = Color(0xFF8B5CF6),
        icon = Icons.Default.Lightbulb
    ),
    OTHER(
        displayName = "Other",
        description = "Uncategorized or general notes",
        defaultColor = Color(0xFF6B7280), // Gray
        icon = Icons.Default.Note
    );

    companion object {
        /**
         * Get primary categories for the modern UI (excludes legacy categories)
         */
        fun getPrimaryCategories(): List<ContentCategory> = listOf(
            WORK, PERSONAL, IDEAS, MEETINGS, SHOPPING, TASKS, 
            RESEARCH, EDUCATION, HEALTH, TRAVEL, OTHER
        )
        
        /**
         * Get all categories including legacy ones
         */
        fun getAllCategories(): List<ContentCategory> = values().toList()
        
        /**
         * Map legacy categories to modern equivalents
         */
        fun mapLegacyToModern(category: ContentCategory): ContentCategory = when (category) {
            MEETING -> MEETINGS
            BRAINSTORMING -> IDEAS
            TASK_PLANNING -> TASKS
            RESEARCH_NOTES -> RESEARCH
            PERSONAL_REFLECTION -> PERSONAL
            INTERVIEW -> WORK
            LECTURE -> EDUCATION
            PHONE_CALL -> WORK
            REMINDER -> TASKS
            IDEA_CAPTURE -> IDEAS
            else -> category
        }
    }
}

/**
 * Custom category definition for user-created categories
 */
data class CustomCategory(
    val id: String,
    val name: String,
    val description: String,
    val color: Color,
    val icon: ImageVector,
    val createdAt: Long = System.currentTimeMillis(),
    val usageCount: Int = 0
)

/**
 * Category suggestion based on user patterns
 */
data class CategorySuggestion(
    val category: ContentCategory,
    val confidence: Float,
    val reason: String,
    val keywords: List<String> = emptyList()
)

/**
 * Category usage statistics for pattern learning
 */
data class CategoryUsageStats(
    val category: ContentCategory,
    val usageCount: Int,
    val lastUsed: Long,
    val averageConfidence: Float,
    val commonKeywords: List<String>
)