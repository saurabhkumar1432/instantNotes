package com.voicenotesai.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Enhanced Note entity optimized for large datasets (10k+ notes).
 * 
 * Indexes are strategically placed on frequently queried columns:
 * - timestamp: For chronological ordering and date-based queries
 * - content: For full-text search operations
 * - transcribedText: For searching transcribed content
 * - isArchived: For filtering archived vs active notes
 */
@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["timestamp"], name = "idx_notes_timestamp"),
        Index(value = ["content"], name = "idx_notes_content"),
        Index(value = ["transcribedText"], name = "idx_notes_transcribed_text"),
        Index(value = ["isArchived"], name = "idx_notes_archived"),
        Index(value = ["category"], name = "idx_notes_category"),
        Index(value = ["timestamp", "isArchived"], name = "idx_notes_timestamp_archived")
    ]
)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val lastModified: Long = timestamp,
    val transcribedText: String? = null,
    val category: String = "General",
    val tags: String = "", // Comma-separated tags for simple storage
    val isArchived: Boolean = false,
    val audioFingerprint: String? = null,
    val language: String? = null,
    val wordCount: Int = 0,
    val duration: Long = 0 // Audio duration in milliseconds
)
