package com.voicenotesai.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Task entity for managing action items and todos extracted from voice notes.
 * 
 * Indexes are strategically placed on frequently queried columns:
 * - isCompleted: For filtering completed vs pending tasks
 * - sourceNoteId: For joining with notes and finding tasks from specific notes
 * - createdAt: For chronological ordering
 * - dueDate: For deadline-based queries and reminders
 * - priority: For priority-based sorting
 */
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["isCompleted"], name = "idx_tasks_completed"),
        Index(value = ["sourceNoteId"], name = "idx_tasks_source_note"),
        Index(value = ["createdAt"], name = "idx_tasks_created_at"),
        Index(value = ["dueDate"], name = "idx_tasks_due_date"),
        Index(value = ["priority"], name = "idx_tasks_priority"),
        Index(value = ["isCompleted", "createdAt"], name = "idx_tasks_completed_created"),
        Index(value = ["sourceNoteId", "isCompleted"], name = "idx_tasks_source_completed")
    ]
)
data class Task(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isCompleted: Boolean = false,
    val sourceNoteId: Long? = null, // References Note.id, nullable for manual tasks
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val dueDate: Long? = null,
    val priority: String = "NORMAL", // Stored as string for Room compatibility
    val category: String? = null
)

/**
 * Task priority levels for organizing and sorting tasks.
 */
enum class TaskPriority {
    LOW,
    NORMAL, 
    HIGH,
    URGENT
}