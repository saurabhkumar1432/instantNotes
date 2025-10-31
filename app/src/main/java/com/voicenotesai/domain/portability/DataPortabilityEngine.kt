package com.voicenotesai.domain.portability

import java.io.File

/**
 * Interface for handling data export, import, and backup operations.
 * Supports multiple formats and ensures data integrity throughout the process.
 */
interface DataPortabilityEngine {
    /**
     * Export notes to the specified format
     */
    suspend fun exportNotes(format: ExportFormat, outputFile: File): ExportResult

    /**
     * Import notes from the provided data
     */
    suspend fun importNotes(data: ImportData): ImportResult

    /**
     * Create a complete backup of all data
     */
    suspend fun createBackup(includeAudio: Boolean = false, outputFile: File): BackupResult

    /**
     * Restore data from a backup file
     */
    suspend fun restoreBackup(backupFile: File): RestoreResult

    /**
     * Validate data integrity of import/backup files
     */
    suspend fun validateDataIntegrity(file: File): IntegrityCheckResult
}

/**
 * Supported export formats
 */
sealed class ExportFormat {
    object JSON : ExportFormat()
    object CSV : ExportFormat()
    object Markdown : ExportFormat()
    object PDF : ExportFormat()
    object Word : ExportFormat()
    object PlainText : ExportFormat()
    data class Custom(val template: String) : ExportFormat()
}

/**
 * Data structure for import operations
 */
data class ImportData(
    val file: File,
    val format: ExportFormat,
    val validateIntegrity: Boolean = true
)

/**
 * Result of export operations
 */
sealed class ExportResult {
    data class Success(
        val file: File,
        val notesExported: Int,
        val fileSizeBytes: Long
    ) : ExportResult()
    
    data class Failure(
        val error: ExportError,
        val message: String
    ) : ExportResult()
}

/**
 * Result of import operations
 */
sealed class ImportResult {
    data class Success(
        val notesImported: Int,
        val duplicatesSkipped: Int,
        val validationPassed: Boolean
    ) : ImportResult()
    
    data class Failure(
        val error: ImportError,
        val message: String,
        val lineNumber: Int? = null
    ) : ImportResult()
}

/**
 * Result of backup operations
 */
sealed class BackupResult {
    data class Success(
        val backupFile: File,
        val notesBackedUp: Int,
        val audioFilesIncluded: Int,
        val totalSizeBytes: Long
    ) : BackupResult()
    
    data class Failure(
        val error: BackupError,
        val message: String
    ) : BackupResult()
}

/**
 * Result of restore operations
 */
sealed class RestoreResult {
    data class Success(
        val notesRestored: Int,
        val audioFilesRestored: Int,
        val validationPassed: Boolean
    ) : RestoreResult()
    
    data class Failure(
        val error: RestoreError,
        val message: String
    ) : RestoreResult()
}

/**
 * Result of data integrity validation
 */
sealed class IntegrityCheckResult {
    data class Valid(
        val checksumMatches: Boolean,
        val structureValid: Boolean,
        val notesCount: Int
    ) : IntegrityCheckResult()
    
    data class Invalid(
        val issues: List<IntegrityIssue>,
        val canProceed: Boolean
    ) : IntegrityCheckResult()
}

/**
 * Types of export errors
 */
enum class ExportError {
    FILE_WRITE_ERROR,
    INSUFFICIENT_STORAGE,
    PERMISSION_DENIED,
    DATA_ACCESS_ERROR,
    FORMAT_NOT_SUPPORTED,
    TEMPLATE_INVALID
}

/**
 * Types of import errors
 */
enum class ImportError {
    FILE_READ_ERROR,
    INVALID_FORMAT,
    CORRUPTED_DATA,
    UNSUPPORTED_VERSION,
    VALIDATION_FAILED,
    DUPLICATE_DETECTION_FAILED
}

/**
 * Types of backup errors
 */
enum class BackupError {
    INSUFFICIENT_STORAGE,
    PERMISSION_DENIED,
    COMPRESSION_FAILED,
    AUDIO_ACCESS_ERROR,
    METADATA_CREATION_FAILED
}

/**
 * Types of restore errors
 */
enum class RestoreError {
    BACKUP_CORRUPTED,
    INCOMPATIBLE_VERSION,
    EXTRACTION_FAILED,
    DATABASE_ERROR,
    AUDIO_RESTORE_FAILED
}

/**
 * Data integrity issues
 */
data class IntegrityIssue(
    val type: IntegrityIssueType,
    val description: String,
    val severity: IssueSeverity
)

enum class IntegrityIssueType {
    CHECKSUM_MISMATCH,
    MISSING_REQUIRED_FIELD,
    INVALID_DATA_TYPE,
    CORRUPTED_AUDIO_FILE,
    TIMESTAMP_OUT_OF_RANGE,
    ENCODING_ERROR
}

enum class IssueSeverity {
    WARNING,  // Can proceed with caution
    ERROR,    // Should not proceed
    CRITICAL  // Data loss risk
}