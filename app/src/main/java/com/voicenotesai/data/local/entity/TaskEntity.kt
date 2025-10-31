package com.voicenotesai.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Task entity for Room database storage.
 * 
 * Indexes are strategically placed on frequently queried columns:
 * - isCompleted: For filtering completed vs pending tasks
 * - sourceNoteId: For finding tasks associated with specific notes
 * - createdAt: For chronological ordering
 * - dueDate: For deadline-based queries
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
        Index(value = ["isCompleted", "createdAt"], name = "idx_tasks_completed_created")
    ],
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["sourceNoteId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val text: String,
    val isCompleted: Boolean = false,
    val sourceNoteId: Long? = null, // Foreign key to Note.id
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val dueDate: Long? = null,
    val priority: String = "NORMAL" // Stored as string for Room compatibility
)