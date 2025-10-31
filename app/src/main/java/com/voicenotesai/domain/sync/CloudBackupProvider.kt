package com.voicenotesai.domain.sync

import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Interface for cloud backup providers (Google Drive, Dropbox, iCloud)
 */
interface CloudBackupProvider {
    
    /**
     * Authenticate with the cloud service
     */
    suspend fun authenticate(): AuthenticationResult
    
    /**
     * Check if user is authenticated
     */
    suspend fun isAuthenticated(): Boolean
    
    /**
     * Upload a backup file to cloud storage
     */
    suspend fun uploadBackup(
        backupFile: File,
        fileName: String,
        metadata: BackupMetadata
    ): CloudBackupResult
    
    /**
     * Download a backup file from cloud storage
     */
    suspend fun downloadBackup(
        backupId: String,
        destinationFile: File
    ): CloudBackupResult
    
    /**
     * List available backups in cloud storage
     */
    suspend fun listBackups(): List<CloudBackupInfo>
    
    /**
     * Delete a backup from cloud storage
     */
    suspend fun deleteBackup(backupId: String): CloudBackupResult
    
    /**
     * Get available storage space
     */
    suspend fun getStorageInfo(): CloudStorageInfo
    
    /**
     * Observe upload/download progress
     */
    fun observeProgress(): Flow<CloudBackupProgress>
    
    /**
     * Cancel ongoing operation
     */
    suspend fun cancelOperation(): Boolean
    
    /**
     * Get provider name
     */
    val providerName: String
    
    /**
     * Get provider type
     */
    val providerType: CloudProviderType
}

/**
 * Types of cloud providers
 */
enum class CloudProviderType {
    GOOGLE_DRIVE,
    DROPBOX,
    ICLOUD,
    ONEDRIVE,
    CUSTOM
}

/**
 * Authentication result
 */
sealed class AuthenticationResult {
    object Success : AuthenticationResult()
    data class Failure(val error: String) : AuthenticationResult()
    object Cancelled : AuthenticationResult()
}

/**
 * Cloud backup operation result
 */
sealed class CloudBackupResult {
    data class Success(
        val backupId: String,
        val url: String? = null,
        val sizeBytes: Long = 0
    ) : CloudBackupResult()
    
    data class Failure(
        val error: CloudBackupError,
        val message: String
    ) : CloudBackupResult()
    
    data class Progress(
        val bytesTransferred: Long,
        val totalBytes: Long,
        val percentage: Float
    ) : CloudBackupResult()
}

/**
 * Cloud backup errors
 */
enum class CloudBackupError {
    AUTHENTICATION_FAILED,
    INSUFFICIENT_STORAGE,
    NETWORK_ERROR,
    FILE_NOT_FOUND,
    PERMISSION_DENIED,
    QUOTA_EXCEEDED,
    SERVICE_UNAVAILABLE,
    UPLOAD_FAILED,
    DOWNLOAD_FAILED,
    OPERATION_CANCELLED
}

/**
 * Information about a cloud backup
 */
data class CloudBackupInfo(
    val id: String,
    val name: String,
    val createdAt: Long,
    val sizeBytes: Long,
    val metadata: BackupMetadata,
    val url: String? = null,
    val checksum: String? = null
)

/**
 * Cloud storage information
 */
data class CloudStorageInfo(
    val totalSpaceBytes: Long,
    val usedSpaceBytes: Long,
    val availableSpaceBytes: Long,
    val quotaExceeded: Boolean = false
)

/**
 * Progress information for cloud operations
 */
data class CloudBackupProgress(
    val operationType: CloudOperationType,
    val fileName: String,
    val bytesTransferred: Long,
    val totalBytes: Long,
    val percentage: Float,
    val speedBytesPerSecond: Long = 0,
    val estimatedTimeRemainingMs: Long? = null
)

/**
 * Types of cloud operations
 */
enum class CloudOperationType {
    UPLOAD,
    DOWNLOAD,
    DELETE,
    LIST
}

/**
 * Backup metadata for cloud storage
 */
data class BackupMetadata(
    val version: String,
    val appVersion: String,
    val createdAt: Long,
    val deviceId: String,
    val notesCount: Int,
    val audioFilesCount: Int,
    val totalSizeBytes: Long,
    val includesAudio: Boolean,
    val compressionType: String = "zip",
    val encryptionEnabled: Boolean = false,
    val tags: List<String> = emptyList(),
    val description: String? = null
)