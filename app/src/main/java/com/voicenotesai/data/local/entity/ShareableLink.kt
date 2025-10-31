package com.voicenotesai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Entity representing a shareable link for a note
 */
@Entity(
    tableName = "shareable_links",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["noteId"]),
        Index(value = ["shareUrl"], unique = true),
        Index(value = ["expiresAt"]),
        Index(value = ["isActive"])
    ]
)
data class ShareableLink(
    @PrimaryKey
    val id: String,
    
    val noteId: String,
    
    val shareUrl: String,
    
    val createdAt: Long,
    
    val expiresAt: Long? = null,
    
    val accessCount: Int = 0,
    
    val maxAccessCount: Int? = null,
    
    val password: String? = null,
    
    val allowDownload: Boolean = true,
    
    val isActive: Boolean = true,
    
    val createdByUserId: String? = null,
    
    val lastAccessedAt: Long? = null,
    
    val accessLog: String? = null // JSON string of access history
) {
    val isExpired: Boolean
        get() = expiresAt?.let { it < System.currentTimeMillis() } ?: false
    
    val isAccessLimitReached: Boolean
        get() = maxAccessCount?.let { accessCount >= it } ?: false
    
    val isValid: Boolean
        get() = isActive && !isExpired && !isAccessLimitReached
}