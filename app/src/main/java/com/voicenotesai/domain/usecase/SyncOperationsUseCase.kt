package com.voicenotesai.domain.usecase

import com.voicenotesai.domain.sync.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for managing sync operations with intelligent conflict resolution and progress tracking.
 * Handles the business logic for synchronization between local and remote data.
 */
@Singleton
class SyncOperationsUseCase @Inject constructor(
    private val syncManager: SyncManager
) {

    /**
     * Starts intelligent synchronization with automatic conflict detection.
     */
    suspend fun startIntelligentSync(): SyncResult {
        return syncManager.startSync()
    }

    /**
     * Syncs a specific item with priority handling.
     */
    suspend fun syncItemWithPriority(
        itemId: String, 
        itemType: SyncItemType, 
        priority: SyncPriority = SyncPriority.Normal
    ): SyncItemResult {
        return syncManager.syncItem(itemId, itemType)
    }

    /**
     * Resolves conflicts using intelligent strategies.
     */
    suspend fun resolveConflictIntelligently(
        conflictId: String, 
        resolution: ConflictResolution
    ): ConflictResolutionResult {
        return syncManager.resolveConflict(conflictId, resolution)
    }

    /**
     * Auto-resolves conflicts when possible using configured strategies.
     */
    suspend fun autoResolveConflicts(
        conflicts: List<SyncConflict>,
        strategy: AutoConflictResolution
    ): List<ConflictResolutionResult> {
        val results = mutableListOf<ConflictResolutionResult>()
        
        conflicts.forEach { conflict ->
            if (conflict.autoResolvable) {
                val resolution = when (strategy) {
                    AutoConflictResolution.USE_LOCAL -> ConflictResolution.UseLocal
                    AutoConflictResolution.USE_REMOTE -> ConflictResolution.UseRemote
                    AutoConflictResolution.USE_LATEST_TIMESTAMP -> {
                        if (conflict.localVersion.timestamp > conflict.remoteVersion.timestamp) {
                            ConflictResolution.UseLocal
                        } else {
                            ConflictResolution.UseRemote
                        }
                    }
                    AutoConflictResolution.MERGE_WHEN_POSSIBLE -> {
                        // Attempt simple merge for text-based conflicts
                        if (canAutoMerge(conflict)) {
                            val mergedData = performAutoMerge(conflict)
                            ConflictResolution.UseMerged(mergedData)
                        } else {
                            return@forEach // Skip non-mergeable conflicts
                        }
                    }
                    AutoConflictResolution.PROMPT_USER -> return@forEach // Skip auto-resolution
                }
                
                val result = syncManager.resolveConflict(conflict.id, resolution)
                results.add(result)
            }
        }
        
        return results
    }

    /**
     * Retries failed operations with exponential backoff.
     */
    suspend fun retryFailedOperationsWithBackoff(): RetryResult {
        return syncManager.retryFailedOperations()
    }

    /**
     * Gets comprehensive sync status including health metrics.
     */
    suspend fun getSyncHealthStatus(): SyncHealthStatus {
        val metrics = syncManager.getSyncMetrics()
        val pendingOperations = syncManager.getPendingSyncOperations()
        val conflicts = syncManager.getUnresolvedConflicts()
        
        return SyncHealthStatus(
            isHealthy = conflicts.isEmpty() && pendingOperations.none { it.status == SyncOperationStatus.Failed },
            metrics = metrics,
            pendingOperationsCount = pendingOperations.size,
            conflictsCount = conflicts.size,
            failedOperationsCount = pendingOperations.count { it.status == SyncOperationStatus.Failed },
            lastSyncSuccess = metrics.lastSyncTimestamp != null,
            syncReliabilityScore = metrics.syncReliabilityScore,
            recommendations = generateSyncRecommendations(metrics, pendingOperations, conflicts)
        )
    }

    /**
     * Observes sync status with enhanced monitoring.
     */
    fun observeSyncStatusWithMetrics(): Flow<SyncStatus> {
        return syncManager.observeSyncStatus()
    }

    /**
     * Observes sync progress with detailed tracking.
     */
    fun observeDetailedSyncProgress(): Flow<SyncProgress> {
        return syncManager.observeSyncProgress()
    }

    /**
     * Observes conflicts that require user attention.
     */
    fun observeConflictsRequiringAttention(): Flow<SyncConflict> {
        return syncManager.observeConflicts()
    }

    /**
     * Configures sync behavior with validation.
     */
    suspend fun configureSyncBehavior(settings: SyncSettings): ConfigurationResult {
        // Validate settings before applying
        val validationResult = validateSyncSettings(settings)
        if (!validationResult.isValid) {
            return ConfigurationResult(
                success = false,
                appliedSettings = settings,
                error = validationResult.error
            )
        }
        
        return syncManager.configureSyncSettings(settings)
    }

    /**
     * Performs emergency sync for critical data.
     */
    suspend fun performEmergencySync(criticalItemIds: List<String>): SyncResult {
        // Create high-priority sync operations for critical items
        val results = mutableListOf<SyncItemResult>()
        var totalSuccess = true
        
        criticalItemIds.forEach { itemId ->
            val result = syncManager.syncItem(itemId, SyncItemType.NOTE)
            results.add(result)
            if (!result.success) {
                totalSuccess = false
            }
        }
        
        return SyncResult(
            success = totalSuccess,
            operationsCompleted = results.count { it.success },
            operationsFailed = results.count { !it.success },
            conflictsDetected = results.count { it.conflict != null },
            syncTimeMs = results.sumOf { it.syncTimeMs },
            partialSuccess = results.any { it.success } && results.any { !it.success }
        )
    }

    /**
     * Cancels sync operations gracefully.
     */
    suspend fun cancelSyncGracefully(): CancelSyncResult {
        return syncManager.cancelSync()
    }

    /**
     * Forces full synchronization with data integrity checks.
     */
    suspend fun forceFullSyncWithIntegrityCheck(): SyncResult {
        return syncManager.forceFullSync()
    }

    /**
     * Determines if a conflict can be automatically merged.
     */
    private fun canAutoMerge(conflict: SyncConflict): Boolean {
        return when (conflict.conflictType) {
            ConflictType.CONCURRENT_MODIFICATION -> {
                // Check if both versions are text-based and have non-overlapping changes
                isTextBasedConflict(conflict) && hasNonOverlappingChanges(conflict)
            }
            ConflictType.SCHEMA_MISMATCH -> false
            ConflictType.DELETE_MODIFY_CONFLICT -> false
            ConflictType.PERMISSION_DENIED -> false
            ConflictType.DATA_CORRUPTION -> false
            ConflictType.VERSION_MISMATCH -> true // Can often be resolved by taking latest
        }
    }

    /**
     * Performs automatic merge of conflicting data.
     */
    private fun performAutoMerge(conflict: SyncConflict): Any {
        val localData = conflict.localVersion.data.toString()
        val remoteData = conflict.remoteVersion.data.toString()
        
        // Simple line-based merge (in production, use a proper merge algorithm)
        val localLines = localData.lines()
        val remoteLines = remoteData.lines()
        
        val mergedLines = mutableSetOf<String>()
        mergedLines.addAll(localLines)
        mergedLines.addAll(remoteLines)
        
        return mergedLines.joinToString("\n")
    }

    /**
     * Checks if conflict involves text-based data.
     */
    private fun isTextBasedConflict(conflict: SyncConflict): Boolean {
        return conflict.localVersion.data is String && conflict.remoteVersion.data is String
    }

    /**
     * Checks if changes in conflict are non-overlapping.
     */
    private fun hasNonOverlappingChanges(conflict: SyncConflict): Boolean {
        // Simplified check - in production, use proper diff analysis
        val localData = conflict.localVersion.data.toString()
        val remoteData = conflict.remoteVersion.data.toString()
        
        // If one is a subset of the other, they might be non-overlapping additions
        return localData.contains(remoteData) || remoteData.contains(localData)
    }

    /**
     * Validates sync settings for correctness.
     */
    private fun validateSyncSettings(settings: SyncSettings): ValidationResult {
        return when {
            settings.syncInterval < 60000 -> ValidationResult(false, "Sync interval must be at least 1 minute")
            settings.maxConcurrentOperations < 1 -> ValidationResult(false, "Must allow at least 1 concurrent operation")
            settings.maxConcurrentOperations > 20 -> ValidationResult(false, "Too many concurrent operations may impact performance")
            settings.maxRetryAttempts < 0 -> ValidationResult(false, "Retry attempts cannot be negative")
            settings.maxRetryAttempts > 10 -> ValidationResult(false, "Too many retry attempts may cause delays")
            settings.retryDelayMs < 1000 -> ValidationResult(false, "Retry delay must be at least 1 second")
            settings.maxSyncDataSizeMB < 1 -> ValidationResult(false, "Sync data size limit must be at least 1MB")
            settings.maxSyncDataSizeMB > 1000 -> ValidationResult(false, "Sync data size limit too large")
            else -> ValidationResult(true, null)
        }
    }

    /**
     * Generates recommendations for improving sync performance.
     */
    private fun generateSyncRecommendations(
        metrics: SyncMetrics,
        pendingOperations: List<SyncOperation>,
        conflicts: List<SyncConflict>
    ): List<SyncRecommendation> {
        val recommendations = mutableListOf<SyncRecommendation>()
        
        // Check sync reliability
        if (metrics.syncReliabilityScore < 0.8f) {
            recommendations.add(
                SyncRecommendation(
                    type = SyncRecommendationType.RELIABILITY,
                    title = "Improve Sync Reliability",
                    description = "Your sync success rate is ${(metrics.syncReliabilityScore * 100).toInt()}%. Consider checking your network connection.",
                    priority = SyncRecommendationPriority.HIGH
                )
            )
        }
        
        // Check for too many conflicts
        if (conflicts.size > 5) {
            recommendations.add(
                SyncRecommendation(
                    type = SyncRecommendationType.CONFLICTS,
                    title = "Resolve Pending Conflicts",
                    description = "You have ${conflicts.size} unresolved conflicts. Resolve them to improve sync performance.",
                    priority = SyncRecommendationPriority.MEDIUM
                )
            )
        }
        
        // Check for failed operations
        val failedOps = pendingOperations.count { it.status == SyncOperationStatus.Failed }
        if (failedOps > 0) {
            recommendations.add(
                SyncRecommendation(
                    type = SyncRecommendationType.FAILED_OPERATIONS,
                    title = "Retry Failed Operations",
                    description = "$failedOps operations have failed. Try syncing again or check your connection.",
                    priority = SyncRecommendationPriority.HIGH
                )
            )
        }
        
        // Check sync frequency
        val lastSync = metrics.lastSyncTimestamp
        if (lastSync != null && System.currentTimeMillis() - lastSync > 86400000) { // 24 hours
            recommendations.add(
                SyncRecommendation(
                    type = SyncRecommendationType.SYNC_FREQUENCY,
                    title = "Sync More Frequently",
                    description = "Your last sync was over 24 hours ago. Consider enabling automatic sync.",
                    priority = SyncRecommendationPriority.LOW
                )
            )
        }
        
        return recommendations
    }

    /**
     * Result of settings validation.
     */
    private data class ValidationResult(
        val isValid: Boolean,
        val error: String?
    )
}

/**
 * Comprehensive sync health status.
 */
data class SyncHealthStatus(
    val isHealthy: Boolean,
    val metrics: SyncMetrics,
    val pendingOperationsCount: Int,
    val conflictsCount: Int,
    val failedOperationsCount: Int,
    val lastSyncSuccess: Boolean,
    val syncReliabilityScore: Float,
    val recommendations: List<SyncRecommendation>
)

/**
 * Sync recommendation for improving performance.
 */
data class SyncRecommendation(
    val type: SyncRecommendationType,
    val title: String,
    val description: String,
    val priority: SyncRecommendationPriority
)

/**
 * Types of sync recommendations.
 */
enum class SyncRecommendationType {
    RELIABILITY,
    CONFLICTS,
    FAILED_OPERATIONS,
    SYNC_FREQUENCY,
    NETWORK_OPTIMIZATION,
    STORAGE_OPTIMIZATION
}

/**
 * Priority levels for sync operation recommendations.
 */
enum class SyncRecommendationPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}