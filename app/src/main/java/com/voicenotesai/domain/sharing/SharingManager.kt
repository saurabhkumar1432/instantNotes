package com.voicenotesai.domain.sharing

import android.content.Intent
import com.voicenotesai.domain.model.EnhancedNote
import java.io.File

/**
 * Interface for managing note sharing and export operations
 */
interface SharingManager {
    /**
     * Share a single note via Android's share intent
     */
    suspend fun shareNote(note: EnhancedNote, format: ShareFormat): ShareResult
    
    /**
     * Share multiple notes via Android's share intent
     */
    suspend fun shareNotes(notes: List<EnhancedNote>, format: ShareFormat): ShareResult
    
    /**
     * Create a shareable link for a note with optional expiration
     */
    suspend fun createShareableLink(noteId: String, expirationHours: Int? = null): ShareLinkResult
    
    /**
     * Share note directly to specific apps
     */
    suspend fun shareToApp(note: EnhancedNote, targetApp: TargetApp, format: ShareFormat): ShareResult
    
    /**
     * Create calendar event from meeting note
     */
    suspend fun createCalendarEvent(note: EnhancedNote, eventDetails: CalendarEventDetails): CalendarResult
    
    /**
     * Export note to cloud storage
     */
    suspend fun exportToCloudStorage(notes: List<EnhancedNote>, cloudProvider: CloudProvider, format: ShareFormat): CloudExportResult
}

/**
 * Supported sharing formats
 */
enum class ShareFormat {
    PLAIN_TEXT,
    MARKDOWN,
    PDF,
    RTF,
    JSON
}

/**
 * Target applications for direct sharing
 */
enum class TargetApp(val packageName: String) {
    EMAIL("com.android.email"),
    GMAIL("com.google.android.gm"),
    WHATSAPP("com.whatsapp"),
    TELEGRAM("org.telegram.messenger"),
    SLACK("com.Slack"),
    MICROSOFT_TEAMS("com.microsoft.teams"),
    GOOGLE_DRIVE("com.google.android.apps.docs"),
    DROPBOX("com.dropbox.android"),
    ONEDRIVE("com.microsoft.skydrive")
}

/**
 * Cloud storage providers
 */
enum class CloudProvider {
    GOOGLE_DRIVE,
    DROPBOX,
    ONEDRIVE,
    ICLOUD
}

/**
 * Calendar event details
 */
data class CalendarEventDetails(
    val title: String,
    val description: String,
    val startTime: Long,
    val endTime: Long,
    val location: String? = null,
    val attendees: List<String> = emptyList()
)

/**
 * Result of sharing operations
 */
sealed class ShareResult {
    data class Success(
        val sharedContent: String,
        val targetApp: String? = null,
        val filePath: String? = null
    ) : ShareResult()
    
    data class Failure(
        val error: ShareError,
        val message: String
    ) : ShareResult()
}

/**
 * Result of shareable link creation
 */
sealed class ShareLinkResult {
    data class Success(
        val shareUrl: String,
        val expiresAt: Long? = null,
        val accessCount: Int = 0
    ) : ShareLinkResult()
    
    data class Failure(
        val error: ShareError,
        val message: String
    ) : ShareLinkResult()
}

/**
 * Result of calendar event creation
 */
sealed class CalendarResult {
    data class Success(
        val eventId: String,
        val calendarIntent: Intent
    ) : CalendarResult()
    
    data class Failure(
        val error: ShareError,
        val message: String
    ) : CalendarResult()
}

/**
 * Result of cloud export operations
 */
sealed class CloudExportResult {
    data class Success(
        val cloudUrl: String,
        val fileName: String,
        val fileSize: Long
    ) : CloudExportResult()
    
    data class Failure(
        val error: ShareError,
        val message: String
    ) : CloudExportResult()
}

/**
 * Types of sharing errors
 */
enum class ShareError {
    APP_NOT_INSTALLED,
    PERMISSION_DENIED,
    NETWORK_ERROR,
    FILE_CREATION_FAILED,
    CLOUD_AUTHENTICATION_FAILED,
    CALENDAR_ACCESS_DENIED,
    LINK_CREATION_FAILED,
    UNSUPPORTED_FORMAT,
    CONTENT_TOO_LARGE
}