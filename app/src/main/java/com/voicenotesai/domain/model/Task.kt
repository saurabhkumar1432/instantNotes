package com.voicenotesai.domain.model

import java.util.UUID

/**
 * Task data model representing action items extracted from notes or created manually.
 */
data class Task(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isCompleted: Boolean = false,
    val sourceNoteId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val dueDate: Long? = null,
    val priority: TaskPriority = TaskPriority.NORMAL
)

/**
 * Task priority levels for organization and sorting.
 */
enum class TaskPriority {
    LOW, NORMAL, HIGH, URGENT
}

/**
 * Task with associated note information for display purposes.
 */
data class TaskWithNote(
    val task: Task,
    val sourceNote: EnhancedNote?
)

/**
 * Filter options for task lists.
 */
enum class TaskFilter {
    ALL,
    PENDING,
    COMPLETED;
    
    val displayName: String
        get() = when (this) {
            ALL -> "All"
            PENDING -> "Pending"
            COMPLETED -> "Completed"
        }
}

/**
 * Result of task extraction operation from note content.
 */
data class TaskExtractionResult(
    val success: Boolean,
    val extractedTasks: List<String> = emptyList(),
    val confidence: Float = 0f,
    val error: String? = null
)