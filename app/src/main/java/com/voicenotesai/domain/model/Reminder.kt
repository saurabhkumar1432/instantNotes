package com.voicenotesai.domain.model

import java.util.UUID

/**
 * Reminder data model representing scheduled notifications for notes and tasks.
 */
data class Reminder(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val triggerTime: Long,
    val sourceNoteId: String? = null,
    val sourceTaskId: String? = null,
    val isCompleted: Boolean = false,
    val reminderType: ReminderType = ReminderType.ONE_TIME,
    val repeatInterval: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

/**
 * Types of reminders supported by the system.
 */
enum class ReminderType {
    ONE_TIME,
    DAILY,
    WEEKLY,
    MONTHLY;
    
    val displayName: String
        get() = when (this) {
            ONE_TIME -> "One Time"
            DAILY -> "Daily"
            WEEKLY -> "Weekly"
            MONTHLY -> "Monthly"
        }
}

/**
 * Reminder with associated note and task information for display purposes.
 */
data class ReminderWithContext(
    val reminder: Reminder,
    val sourceNote: EnhancedNote? = null,
    val sourceTask: Task? = null
)

/**
 * Result of date/time detection operation from note content.
 */
data class DateTimeDetectionResult(
    val success: Boolean,
    val detectedDates: List<DetectedDateTime> = emptyList(),
    val confidence: Float = 0f,
    val error: String? = null
)

/**
 * Detected date/time information from note content.
 */
data class DetectedDateTime(
    val text: String, // Original text that was detected
    val timestamp: Long, // Parsed timestamp
    val type: DateTimeType,
    val confidence: Float,
    val suggestedReminderTime: Long? = null // Suggested time to set reminder before the detected time
)

/**
 * Types of date/time patterns that can be detected.
 */
enum class DateTimeType {
    ABSOLUTE_DATE, // "March 15, 2024"
    RELATIVE_DATE, // "tomorrow", "next week"
    TIME_ONLY, // "3:30 PM"
    DATETIME, // "March 15 at 3:30 PM"
    RECURRING // "every Monday"
}

/**
 * Quick action types for reminder notifications.
 */
enum class ReminderAction {
    MARK_DONE,
    SNOOZE_15MIN,
    SNOOZE_1HOUR,
    SNOOZE_TOMORROW,
    VIEW_NOTE,
    VIEW_TASK
}

/**
 * Snooze duration options for reminders.
 */
enum class SnoozeDuration(val milliseconds: Long, val displayName: String) {
    FIFTEEN_MINUTES(15 * 60 * 1000L, "15 minutes"),
    ONE_HOUR(60 * 60 * 1000L, "1 hour"),
    TOMORROW(24 * 60 * 60 * 1000L, "Tomorrow")
}