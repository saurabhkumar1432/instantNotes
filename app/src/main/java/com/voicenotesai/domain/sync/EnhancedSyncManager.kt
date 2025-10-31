package com.voicenotesai.domain.sync

import kotlinx.coroutines.flow.Flow

/**
 * Enhanced sync manager that includes cloud backup and advanced sync capabilities
 */
interface EnhancedSyncManager : SyncManager {
    
    /**
     * Configure cloud backup providers
     */
    suspend fun configureCloudProvider(
        providerType: CloudProviderType,
        enabled: Boolean
    ): ConfigurationResult
    
    /**
     * Create and upload backup to cloud storage
     */
    suspend fun createCloudBackup(
        includeAudio: Boolean = false,
        providerType: CloudProviderType? = null
    ): CloudBackupResult
    
    /**
     * Restore from cloud backup
     */
    suspend fun restoreFromCloudBackup(
        backupId: String,
        providerType: CloudProviderType
    ): RestoreCloudBackupResult
    
    /**
     * List available cloud backups
     */
    suspend fun listCloudBackups(
        providerType: CloudProviderType? = null
    ): List<CloudBackupInfo>
    
    /**
     * Delete cloud backup
     */
    suspend fun deleteCloudBackup(
        backupId: String,
        providerType: CloudProviderType
    ): CloudBackupResult
    
    /**
     * Enable/disable automatic cloud backup
     */
    suspend fun configureAutoBackup(
        enabled: Boolean,
        intervalHours: Int = 24,
        providerType: CloudProviderType,
        includeAudio: Boolean = false
    ): ConfigurationResult
    
    /**
     * Sync with bandwidth optimization
     */
    suspend fun startOptimizedSync(
        maxBandwidthKbps: Int? = null,
        wifiOnly: Boolean = false
    ): SyncResult
    
    /**
     * Get sync status with cloud information
     */
    suspend fun getEnhancedSyncStatus(): EnhancedSyncStatus
    
    /**
     * Observe cloud backup progress
     */
    fun observeCloudBackupProgress(): Flow<CloudBackupProgress>
    
    /**
     * Observe network status for sync optimization
     */
    fun observeNetworkStatus(): Flow<NetworkStatus>
    
    /**
     * Force sync of specific data types
     */
    suspend fun syncDataType(
        dataType: SyncDataType,
        priority: SyncPriority = SyncPriority.Normal
    ): SyncResult
    
    /**
     * Get detailed sync metrics including cloud operations
     */
    suspend fun getDetailedSyncMetrics(): DetailedSyncMetrics
    
    /**
     * Configure conflict resolution strategy
     */
    suspend fun configureConflictResolution(
        strategy: ConflictResolutionStrategy
    ): ConfigurationResult
    
    /**
     * Perform integrity check on synced data
     */
    suspend fun performIntegrityCheck(): IntegrityCheckResult
}

/**
 * Data types that can be synced individually
 */
enum class SyncDataType {
    NOTES,
    AUDIO_FILES,
    SETTINGS,
    USER_PREFERENCES,
    AI_MODELS,
    CATEGORIES,
    TASKS,
    REMINDERS
}

/**
 * Enhanced sync status with cloud information
 */
data class EnhancedSyncStatus(
    val localSyncStatus: SyncStatus,
    val cloudBackupStatus: CloudBackupStatus,
    val lastCloudBackup: Long? = null,
    val nextScheduledBackup: Long? = null,
    val availableCloudProviders: List<CloudProviderType>,
    val activeCloudProvider: CloudProviderType? = null,
    val cloudStorageUsed: Long = 0,
    val cloudStorageAvailable: Long = 0,
    val pendingCloudOperations: Int = 0,
    val networkStatus: NetworkStatus,
    val bandwidthUsageKbps: Int = 0
)

/**
 * Cloud backup status
 */
enum class CloudBackupStatus {
    IDLE,
    UPLOADING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    AUTHENTICATION_REQUIRED,
    QUOTA_EXCEEDED,
    NETWORK_UNAVAILABLE
}

/**
 * Network status for sync optimization
 */
data class NetworkStatus(
    val isConnected: Boolean,
    val isWifi: Boolean,
    val isMobile: Boolean,
    val isMetered: Boolean,
    val signalStrength: NetworkSignalStrength,
    val estimatedBandwidthKbps: Int? = null
)

/**
 * Network signal strength
 */
enum class NetworkSignalStrength {
    POOR,
    FAIR,
    GOOD,
    EXCELLENT,
    UNKNOWN
}

/**
 * Result of cloud backup restore operation
 */
sealed class RestoreCloudBackupResult {
    data class Success(
        val notesRestored: Int,
        val audioFilesRestored: Int,
        val validationPassed: Boolean,
        val restoredSizeBytes: Long
    ) : RestoreCloudBackupResult()
    
    data class Failure(
        val error: CloudBackupError,
        val message: String
    ) : RestoreCloudBackupResult()
}

/**
 * Detailed sync metrics including cloud operations
 */
data class DetailedSyncMetrics(
    val basicMetrics: SyncMetrics,
    val cloudBackupMetrics: CloudBackupMetrics,
    val networkMetrics: NetworkMetrics,
    val conflictMetrics: ConflictMetrics,
    val performanceMetrics: PerformanceMetrics
)

/**
 * Cloud backup specific metrics
 */
data class CloudBackupMetrics(
    val totalBackupsCreated: Long,
    val totalBackupsRestored: Long,
    val totalCloudStorageUsed: Long,
    val averageBackupSizeMB: Float,
    val averageUploadTimeMs: Long,
    val averageDownloadTimeMs: Long,
    val backupSuccessRate: Float,
    val lastBackupTimestamp: Long? = null
)

/**
 * Network usage metrics
 */
data class NetworkMetrics(
    val totalDataTransferredBytes: Long,
    val wifiDataTransferredBytes: Long,
    val mobileDataTransferredBytes: Long,
    val averageBandwidthKbps: Float,
    val peakBandwidthKbps: Int,
    val dataCompressionRatio: Float
)

/**
 * Conflict resolution metrics
 */
data class ConflictMetrics(
    val totalConflictsDetected: Long,
    val conflictsAutoResolved: Long,
    val conflictsManuallyResolved: Long,
    val conflictsPending: Long,
    val averageResolutionTimeMs: Long,
    val mostCommonConflictType: ConflictType? = null
)

/**
 * Performance metrics
 */
data class PerformanceMetrics(
    val averageSyncLatencyMs: Long,
    val syncThroughputItemsPerSecond: Float,
    val memoryUsageMB: Float,
    val cpuUsagePercent: Float,
    val batteryImpactScore: Float,
    val cacheHitRate: Float
)

/**
 * Conflict resolution strategies
 */
data class ConflictResolutionStrategy(
    val autoResolution: AutoConflictResolution,
    val timeoutMs: Long = 30000,
    val maxRetries: Int = 3,
    val preferLocalForTypes: Set<SyncDataType> = emptySet(),
    val preferRemoteForTypes: Set<SyncDataType> = emptySet(),
    val enableMerging: Boolean = true,
    val notifyUser: Boolean = true
)

/**
 * Integrity check result for synced data
 */
sealed class IntegrityCheckResult {
    data class Passed(
        val itemsChecked: Int,
        val checksumMatches: Int,
        val timestampConsistency: Boolean
    ) : IntegrityCheckResult()
    
    data class Failed(
        val issues: List<IntegrityIssue>,
        val itemsChecked: Int,
        val criticalIssues: Int
    ) : IntegrityCheckResult()
}

/**
 * Integrity issue details
 */
data class IntegrityIssue(
    val itemId: String,
    val itemType: SyncDataType,
    val issueType: IntegrityIssueType,
    val description: String,
    val severity: IssueSeverity,
    val canAutoFix: Boolean = false
)

/**
 * Types of integrity issues
 */
enum class IntegrityIssueType {
    CHECKSUM_MISMATCH,
    TIMESTAMP_INCONSISTENCY,
    MISSING_REFERENCE,
    CORRUPTED_DATA,
    DUPLICATE_ENTRY,
    SCHEMA_VIOLATION
}

/**
 * Severity levels for integrity issues
 */
enum class IssueSeverity {
    LOW,      // Minor inconsistency, no data loss risk
    MEDIUM,   // Moderate issue, may affect functionality
    HIGH,     // Serious issue, data integrity at risk
    CRITICAL  // Severe issue, immediate attention required
}