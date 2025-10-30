package com.voicenotesai.domain.model

import java.util.UUID

/**
 * Enhanced note model with additional metadata for export/import operations
 */
data class EnhancedNote(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val transcribedText: String,
    val timestamp: Long,
    val lastModified: Long = timestamp,
    val category: NoteCategory = NoteCategory.General,
    val tags: List<String> = emptyList(),
    val entities: List<ExtractedEntity> = emptyList(),
    val sentiment: SentimentScore? = null,
    val language: DetectedLanguage? = null,
    val audioFingerprint: String? = null,
    val isArchived: Boolean = false,
    val encryptionMetadata: EncryptionMetadata? = null,
    val accessLevel: AccessLevel = AccessLevel(SecurityLevel.Internal, setOf(Permission.Read, Permission.Write)),
    val audioFilePath: String? = null,
    val duration: Long? = null,
    val fileSize: Long? = null
)

/**
 * Note categories for organization
 */
enum class NoteCategory {
    General, Meeting, Idea, Task, Reminder, Research, Personal, Business
}

/**
 * Extracted entities from note content
 */
data class ExtractedEntity(
    val text: String,
    val type: EntityType,
    val confidence: Float,
    val startIndex: Int,
    val endIndex: Int
)

/**
 * Types of entities that can be extracted
 */
enum class EntityType {
    Person, Organization, Location, Date, Time, PhoneNumber, Email, URL, Task
}

/**
 * Sentiment analysis result
 */
data class SentimentScore(
    val score: Float, // -1.0 (negative) to 1.0 (positive)
    val confidence: Float,
    val label: SentimentLabel
)

enum class SentimentLabel {
    Positive, Negative, Neutral
}

/**
 * Language detection result
 */
data class DetectedLanguage(
    val code: String, // ISO 639-1 code (e.g., "en", "es")
    val name: String, // Human readable name
    val confidence: Float
)

/**
 * Encryption metadata for secure notes
 */
data class EncryptionMetadata(
    val algorithm: String,
    val keyId: String,
    val iv: ByteArray,
    val authTag: ByteArray,
    val version: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptionMetadata

        if (algorithm != other.algorithm) return false
        if (keyId != other.keyId) return false
        if (!iv.contentEquals(other.iv)) return false
        if (!authTag.contentEquals(other.authTag)) return false
        if (version != other.version) return false

        return true
    }

    override fun hashCode(): Int {
        var result = algorithm.hashCode()
        result = 31 * result + keyId.hashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + authTag.contentHashCode()
        result = 31 * result + version
        return result
    }
}

/**
 * Access level for notes
 */
data class AccessLevel(
    val level: SecurityLevel,
    val permissions: Set<Permission>,
    val expirationTime: Long? = null
)

enum class SecurityLevel {
    Public, Internal, Confidential, Restricted, TopSecret
}

enum class Permission {
    Read, Write, Delete, Export, Share
}

/**
 * Extension function to convert enhanced note to basic note entity
 */
fun EnhancedNote.toBasicNote(): com.voicenotesai.data.local.entity.Note {
    return com.voicenotesai.data.local.entity.Note(
        id = id.hashCode().toLong(), // Convert string ID to long for compatibility
        content = content,
        timestamp = timestamp,
        transcribedText = transcribedText
    )
}

/**
 * Extension function to convert basic note entity to enhanced note
 */
fun com.voicenotesai.data.local.entity.Note.toEnhancedNote(): EnhancedNote {
    return EnhancedNote(
        id = id.toString(),
        content = content,
        transcribedText = transcribedText ?: "",
        timestamp = timestamp,
        lastModified = timestamp
    )
}