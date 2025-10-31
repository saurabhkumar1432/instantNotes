package com.voicenotesai.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Reminder entity for Room database storage.
 * 
 * Indexes are strategically placed on frequently queried columns:
 * - triggerTime: For finding reminders that need to be triggered
 * - isCompleted: For filtering completed vs pending reminders
 * - sourceNoteId: For finding reminders associated with specific notes
 * - sourceTaskId: For finding reminders associated with specific tasks
 * - reminderType: For filtering by reminder type
 */
@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["triggerTime"], name = "idx_reminders_trigger_time"),
        Index(value = ["isCompleted"], name = "idx_reminders_completed"),
        Index(value = ["sourceNoteId"], name = "idx_reminders_source_note"),
        Index(value = ["sourceTaskId"], name = "idx_reminders_source_task"),
        Index(value = ["reminderType"], name = "idx_reminders_type"),
        Index(value = ["triggerTime", "isCompleted"], name = "idx_reminders_trigger_completed")
    ],
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["sourceNoteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceTaskId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ReminderEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String? = null,
    val triggerTime: Long,
    val sourceNoteId: Long? = null, // Foreign key to Note.id
    val sourceTaskId: String? = null, // Foreign key to TaskEntity.id
    val isCompleted: Boolean = false,
    val reminderType: String = "ONE_TIME", // Stored as string for Room compatibility
    val repeatInterval: Long? = null, // Interval in milliseconds for repeating reminders
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)