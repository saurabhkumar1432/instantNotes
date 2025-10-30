package com.voicenotesai.domain.sync

import kotlinx.coroutines.flow.Flow

/**
 * Manages intelligent synchronization of data with conflict resolution and progress tracking.
 * Handles offline-to-online sync scenarios with partial failure recovery.
 */
interface SyncManager {
    
    /**
     * Starts synchronization of all pending changes.
     */
    suspend fun startSync(): SyncResult
    
    /**
     * Synchronizes a specific item by ID.
     */
    suspend fun syncItem(itemId: String, itemType: SyncItemType): SyncItemResult
    
    /**
     * Resolves a conflict for a specific item.
     */
    suspend fun resolveConflict(conflictId: String, resolution: ConflictResolution): ConflictResolutionResult
    
    /**
     * Gets all pending sync operations.
     */
    suspend fun getPendingSyncOperations(): List<SyncOperation>
    
    /**
     * Gets all unresolved conflicts.
     */
    suspend fun getUnresolvedConflicts(): List<SyncConflict>
    
    /**
     * Cancels an ongoing sync operation.
     */
    suspend fun cancelSync(): CancelSyncResult
    
    /**
     * Retries failed sync operations.
     */
    suspend fun retryFailedOperations(): RetryResult
    
    /**
     * Observes sync status changes in real-time.
     */
    fun observeSyncStatus(): Flow<SyncStatus>
    
    /**
     * Observes sync progress for ongoing operations.
     */
    fun observeSyncProgress(): Flow<SyncProgress>
    
    /**
     * Observes conflict events that require user resolution.
     */
    fun observeConflicts(): Flow<SyncConflict>
    
    /**
     * Configures sync settings and preferences.
     */
    suspend fun configureSyncSettings(settings: SyncSettings): ConfigurationResult
    
    /**
     * Gets current sync statistics and metrics.
     */
    suspend fun getSyncMetrics(): SyncMetrics
    
    /**
     * Forces a full sync of all data (use with caution).
     */
    suspend fun forceFullSync(): SyncResult
}

/**
 * Types of items that can be synchronized.
 */
enum class SyncItemType {
    NOTE,
    AUDIO_FILE,
    SETTINGS,
    USER_PREFERENCES,
    AI_MODEL,
    BACKUP
}

/**
 * Represents a sync operation to be performed.
 */
data class SyncOperation(
    val id: String,
    val itemId: String,
    val itemType: SyncItemType,
    val operation: SyncOperationType,
    val timestamp: Long,
    val priority: SyncPriority = SyncPriority.Normal,
    val retryAttempts: Int = 0,
    val maxRetryAttempts: Int = 3,
    val lastAttemptTimestamp: Long? = null,
    val status: SyncOperationStatus = SyncOperationStatus.Pending,
    val error: String? = null,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Types of sync operations.
 */
enum class SyncOperationType {
    CREATE,
    UPDATE,
    DELETE,
    MERGE
}

/**
 * Priority levels for sync operations.
 */
enum class SyncPriority {
    Low,
    Normal,
    High,
    Critical
}

/**
 * Status of sync operations.
 */
enum class SyncOperationStatus {
    Pending,
    InProgress,
    Completed,
    Failed,
    Cancelled,
    ConflictDetected,
    Retrying
}

/**
 * Represents a sync conflict that requires resolution.
 */
data class SyncConflict(
    val id: String,
    val itemId: String,
    val itemType: SyncItemType,
    val conflictType: ConflictType,
    val localVersion: ConflictVersion,
    val remoteVersion: ConflictVersion,
    val timestamp: Long,
    val autoResolvable: Boolean = false,
    val suggestedResolution: ConflictResolution? = null,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Types of conflicts that can occur during sync.
 */
enum class ConflictType {
    CONCURRENT_MODIFICATION,
    DELETE_MODIFY_CONFLICT,
    SCHEMA_MISMATCH,
    PERMISSION_DENIED,
    DATA_CORRUPTION,
    VERSION_MISMATCH
}

/**
 * Represents a version of data in a conflict.
 */
data class ConflictVersion(
    val data: Any,
    val timestamp: Long,
    val version: String,
    val checksum: String,
    val source: ConflictSource,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Source of conflicting data.
 */
enum class ConflictSource {
    LOCAL,
    REMOTE,
    MERGED
}

/**
 * Resolution strategy for conflicts.
 */
sealed class ConflictResolution {
    object UseLocal : ConflictResolution()
    object UseRemote : ConflictResolution()
    data class UseMerged(val mergedData: Any) : ConflictResolution()
    object Skip : ConflictResolution()
    data class Custom(val customData: Any, val strategy: String) : ConflictResolution()
}

/**
 * Overall sync status.
 */
enum class SyncStatus {
    IDLE,
    SYNCING,
    COMPLETED,
    FAILED,
    CANCELLED,
    CONFLICTS_PENDING,
    PARTIAL_SUCCESS
}

/**
 * Progress information for sync operations.
 */
data class SyncProgress(
    val totalOperations: Int,
    val completedOperations: Int,
    val failedOperations: Int,
    val conflictedOperations: Int,
    val currentOperation: SyncOperation? = null,
    val progressPercentage: Float,
    val estimatedTimeRemainingMs: Long? = null,
    val throughputItemsPerSecond: Float = 0f
)

/**
 * Configuration settings for sync behavior.
 */
data class SyncSettings(
    val autoSyncEnabled: Boolean = true,
    val syncInterval: Long = 300000, // 5 minutes
    val maxConcurrentOperations: Int = 5,
    val retryDelayMs: Long = 5000,
    val maxRetryAttempts: Int = 3,
    val conflictResolutionStrategy: AutoConflictResolution = AutoConflictResolution.PROMPT_USER,
    val syncOnlyOnWifi: Boolean = false,
    val syncOnlyWhenCharging: Boolean = false,
    val maxSyncDataSizeMB: Int = 100,
    val enablePartialSync: Boolean = true,
    val compressionEnabled: Boolean = true
)

/**
 * Automatic conflict resolution strategies.
 */
enum class AutoConflictResolution {
    PROMPT_USER,
    USE_LOCAL,
    USE_REMOTE,
    USE_LATEST_TIMESTAMP,
    MERGE_WHEN_POSSIBLE
}

/**
 * Metrics and statistics for sync operations.
 */
data class SyncMetrics(
    val totalSyncOperations: Long,
    val successfulOperations: Long,
    val failedOperations: Long,
    val conflictedOperations: Long,
    val averageSyncTimeMs: Long,
    val lastSyncTimestamp: Long?,
    val nextScheduledSyncTimestamp: Long?,
    val dataTransferredBytes: Long,
    val conflictResolutionRate: Float,
    val syncReliabilityScore: Float
)

/**
 * Result of a sync operation.
 */
data class SyncResult(
    val success: Boolean,
    val operationsCompleted: Int,
    val operationsFailed: Int,
    val conflictsDetected: Int,
    val syncTimeMs: Long,
    val error: String? = null,
    val partialSuccess: Boolean = false,
    val failedOperations: List<SyncOperation> = emptyList(),
    val conflicts: List<SyncConflict> = emptyList()
)

/**
 * Result of syncing a specific item.
 */
data class SyncItemResult(
    val success: Boolean,
    val operation: SyncOperation,
    val syncTimeMs: Long,
    val conflict: SyncConflict? = null,
    val error: String? = null
)

/**
 * Result of conflict resolution.
 */
data class ConflictResolutionResult(
    val success: Boolean,
    val conflict: SyncConflict,
    val resolution: ConflictResolution,
    val resolvedData: Any? = null,
    val error: String? = null
)

/**
 * Result of canceling sync.
 */
data class CancelSyncResult(
    val success: Boolean,
    val operationsCancelled: Int,
    val error: String? = null
)

/**
 * Result of retrying failed operations.
 */
data class RetryResult(
    val success: Boolean,
    val operationsRetried: Int,
    val operationsSucceeded: Int,
    val operationsFailed: Int,
    val error: String? = null
)

/**
 * Result of configuration changes.
 */
data class ConfigurationResult(
    val success: Boolean,
    val appliedSettings: SyncSettings,
    val error: String? = null
)

/**
 * Result of processing a single sync operation.
 */
data class SingleSyncResult(
    val success: Boolean,
    val operation: SyncOperation? = null,
    val conflict: SyncConflict? = null,
    val error: String? = null
)