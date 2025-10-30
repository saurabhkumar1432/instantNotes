package com.voicenotesai.data.sync

import android.content.Context
import com.voicenotesai.domain.sync.*
import com.voicenotesai.domain.offline.OfflineOperationsQueue
import com.voicenotesai.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SyncManager that provides intelligent synchronization with conflict resolution.
 * Handles partial failures, retry mechanisms, and progress tracking.
 */
@Singleton
class SyncManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val offlineQueue: OfflineOperationsQueue,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SyncManager {

    companion object {
        private const val SYNC_BATCH_SIZE = 10
        private const val MAX_CONCURRENT_OPERATIONS = 5
        private const val CONFLICT_RESOLUTION_TIMEOUT_MS = 30000L
    }

    // State management
    private val syncOperations = ConcurrentHashMap<String, SyncOperation>()
    private val conflicts = ConcurrentHashMap<String, SyncConflict>()
    private val isSyncing = AtomicBoolean(false)
    private var syncJob: Job? = null
    
    // Flow emissions
    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    private val _syncProgress = MutableStateFlow(SyncProgress(0, 0, 0, 0, null, 0f))
    private val _conflicts = MutableSharedFlow<SyncConflict>()
    
    // Configuration
    private var syncSettings = SyncSettings()
    
    // Metrics tracking
    private val metrics = SyncMetricsTracker()

    override suspend fun startSync(): SyncResult = withContext(ioDispatcher) {
        if (isSyncing.get()) {
            return@withContext SyncResult(
                success = false,
                operationsCompleted = 0,
                operationsFailed = 0,
                conflictsDetected = 0,
                syncTimeMs = 0,
                error = "Sync already in progress"
            )
        }

        val startTime = System.currentTimeMillis()
        
        try {
            isSyncing.set(true)
            _syncStatus.value = SyncStatus.SYNCING
            
            // Get pending operations from offline queue
            val pendingOperations = createSyncOperationsFromOfflineQueue()
            
            if (pendingOperations.isEmpty()) {
                _syncStatus.value = SyncStatus.COMPLETED
                return@withContext SyncResult(
                    success = true,
                    operationsCompleted = 0,
                    operationsFailed = 0,
                    conflictsDetected = 0,
                    syncTimeMs = System.currentTimeMillis() - startTime
                )
            }

            // Add operations to sync queue
            pendingOperations.forEach { operation ->
                syncOperations[operation.id] = operation
            }

            // Process operations in batches
            val result = processSyncOperations(pendingOperations)
            
            val syncTimeMs = System.currentTimeMillis() - startTime
            metrics.recordSyncOperation(syncTimeMs, result.operationsCompleted, result.operationsFailed)
            
            // Update final status
            _syncStatus.value = when {
                result.conflictsDetected > 0 -> SyncStatus.CONFLICTS_PENDING
                result.operationsFailed > 0 && result.operationsCompleted > 0 -> SyncStatus.PARTIAL_SUCCESS
                result.operationsFailed > 0 -> SyncStatus.FAILED
                else -> SyncStatus.COMPLETED
            }
            
            result.copy(syncTimeMs = syncTimeMs)

        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.FAILED
            SyncResult(
                success = false,
                operationsCompleted = 0,
                operationsFailed = 0,
                conflictsDetected = 0,
                syncTimeMs = System.currentTimeMillis() - startTime,
                error = "Sync failed: ${e.message}"
            )
        } finally {
            isSyncing.set(false)
        }
    }

    override suspend fun syncItem(itemId: String, itemType: SyncItemType): SyncItemResult = withContext(ioDispatcher) {
        val startTime = System.currentTimeMillis()
        
        try {
            val operation = SyncOperation(
                id = UUID.randomUUID().toString(),
                itemId = itemId,
                itemType = itemType,
                operation = SyncOperationType.UPDATE,
                timestamp = System.currentTimeMillis(),
                priority = SyncPriority.High
            )

            val result = processSingleSyncOperation(operation)
            
            SyncItemResult(
                success = result.success,
                operation = operation,
                syncTimeMs = System.currentTimeMillis() - startTime,
                conflict = result.conflict,
                error = result.error
            )

        } catch (e: Exception) {
            SyncItemResult(
                success = false,
                operation = SyncOperation(
                    id = UUID.randomUUID().toString(),
                    itemId = itemId,
                    itemType = itemType,
                    operation = SyncOperationType.UPDATE,
                    timestamp = System.currentTimeMillis()
                ),
                syncTimeMs = System.currentTimeMillis() - startTime,
                error = "Failed to sync item: ${e.message}"
            )
        }
    }

    override suspend fun resolveConflict(conflictId: String, resolution: ConflictResolution): ConflictResolutionResult = withContext(ioDispatcher) {
        try {
            val conflict = conflicts[conflictId]
                ?: return@withContext ConflictResolutionResult(
                    success = false,
                    conflict = SyncConflict(
                        id = conflictId,
                        itemId = "",
                        itemType = SyncItemType.NOTE,
                        conflictType = ConflictType.CONCURRENT_MODIFICATION,
                        localVersion = ConflictVersion("", 0, "", "", ConflictSource.LOCAL),
                        remoteVersion = ConflictVersion("", 0, "", "", ConflictSource.REMOTE),
                        timestamp = System.currentTimeMillis()
                    ),
                    resolution = resolution,
                    error = "Conflict not found"
                )

            val resolvedData = applyConflictResolution(conflict, resolution)
            
            // Remove conflict from pending list
            conflicts.remove(conflictId)
            
            // Update sync operation status
            val relatedOperation = syncOperations.values.find { it.itemId == conflict.itemId }
            relatedOperation?.let { operation ->
                val updatedOperation = operation.copy(
                    status = SyncOperationStatus.Completed,
                    error = null
                )
                syncOperations[operation.id] = updatedOperation
            }

            ConflictResolutionResult(
                success = true,
                conflict = conflict,
                resolution = resolution,
                resolvedData = resolvedData
            )

        } catch (e: Exception) {
            ConflictResolutionResult(
                success = false,
                conflict = conflicts[conflictId] ?: SyncConflict(
                    id = conflictId,
                    itemId = "",
                    itemType = SyncItemType.NOTE,
                    conflictType = ConflictType.CONCURRENT_MODIFICATION,
                    localVersion = ConflictVersion("", 0, "", "", ConflictSource.LOCAL),
                    remoteVersion = ConflictVersion("", 0, "", "", ConflictSource.REMOTE),
                    timestamp = System.currentTimeMillis()
                ),
                resolution = resolution,
                error = "Failed to resolve conflict: ${e.message}"
            )
        }
    }

    override suspend fun getPendingSyncOperations(): List<SyncOperation> = withContext(ioDispatcher) {
        syncOperations.values
            .filter { it.status == SyncOperationStatus.Pending || it.status == SyncOperationStatus.Retrying }
            .sortedWith(compareByDescending<SyncOperation> { it.priority.ordinal }
                .thenBy { it.timestamp })
    }

    override suspend fun getUnresolvedConflicts(): List<SyncConflict> = withContext(ioDispatcher) {
        conflicts.values.sortedBy { it.timestamp }
    }

    override suspend fun cancelSync(): CancelSyncResult = withContext(ioDispatcher) {
        try {
            syncJob?.cancel()
            
            val cancelledOperations = syncOperations.values.count { 
                it.status == SyncOperationStatus.Pending || it.status == SyncOperationStatus.InProgress 
            }
            
            // Update operation statuses
            syncOperations.values
                .filter { it.status == SyncOperationStatus.Pending || it.status == SyncOperationStatus.InProgress }
                .forEach { operation ->
                    syncOperations[operation.id] = operation.copy(status = SyncOperationStatus.Cancelled)
                }
            
            isSyncing.set(false)
            _syncStatus.value = SyncStatus.CANCELLED
            
            CancelSyncResult(
                success = true,
                operationsCancelled = cancelledOperations
            )

        } catch (e: Exception) {
            CancelSyncResult(
                success = false,
                operationsCancelled = 0,
                error = "Failed to cancel sync: ${e.message}"
            )
        }
    }

    override suspend fun retryFailedOperations(): RetryResult = withContext(ioDispatcher) {
        try {
            val failedOperations = syncOperations.values
                .filter { it.status == SyncOperationStatus.Failed && it.retryAttempts < it.maxRetryAttempts }
            
            if (failedOperations.isEmpty()) {
                return@withContext RetryResult(
                    success = true,
                    operationsRetried = 0,
                    operationsSucceeded = 0,
                    operationsFailed = 0
                )
            }

            var succeeded = 0
            var failed = 0

            failedOperations.forEach { operation ->
                val retryOperation = operation.copy(
                    status = SyncOperationStatus.Retrying,
                    retryAttempts = operation.retryAttempts + 1,
                    lastAttemptTimestamp = System.currentTimeMillis(),
                    error = null
                )
                
                syncOperations[operation.id] = retryOperation
                
                val result = processSingleSyncOperation(retryOperation)
                if (result.success) {
                    succeeded++
                } else {
                    failed++
                }
            }

            RetryResult(
                success = true,
                operationsRetried = failedOperations.size,
                operationsSucceeded = succeeded,
                operationsFailed = failed
            )

        } catch (e: Exception) {
            RetryResult(
                success = false,
                operationsRetried = 0,
                operationsSucceeded = 0,
                operationsFailed = 0,
                error = "Failed to retry operations: ${e.message}"
            )
        }
    }

    override fun observeSyncStatus(): Flow<SyncStatus> = _syncStatus.asStateFlow()

    override fun observeSyncProgress(): Flow<SyncProgress> = _syncProgress.asStateFlow()

    override fun observeConflicts(): Flow<SyncConflict> = _conflicts.asSharedFlow()

    override suspend fun configureSyncSettings(settings: SyncSettings): ConfigurationResult = withContext(ioDispatcher) {
        try {
            syncSettings = settings
            
            ConfigurationResult(
                success = true,
                appliedSettings = settings
            )

        } catch (e: Exception) {
            ConfigurationResult(
                success = false,
                appliedSettings = syncSettings,
                error = "Failed to configure sync settings: ${e.message}"
            )
        }
    }

    override suspend fun getSyncMetrics(): SyncMetrics = withContext(ioDispatcher) {
        metrics.getMetrics()
    }

    override suspend fun forceFullSync(): SyncResult = withContext(ioDispatcher) {
        // Clear existing operations and conflicts
        syncOperations.clear()
        conflicts.clear()
        
        // Start fresh sync
        startSync()
    }

    /**
     * Creates sync operations from the offline operations queue.
     */
    private suspend fun createSyncOperationsFromOfflineQueue(): List<SyncOperation> {
        val offlineOperations = offlineQueue.getPendingOperations()
        
        return offlineOperations.mapNotNull { offlineOp ->
            when (offlineOp.type) {
                com.voicenotesai.domain.offline.OperationType.SYNC_NOTE -> {
                    SyncOperation(
                        id = UUID.randomUUID().toString(),
                        itemId = offlineOp.id,
                        itemType = SyncItemType.NOTE,
                        operation = SyncOperationType.UPDATE,
                        timestamp = offlineOp.timestamp,
                        priority = mapOfflinePriorityToSyncPriority(offlineOp.priority)
                    )
                }
                com.voicenotesai.domain.offline.OperationType.UPLOAD_AUDIO -> {
                    SyncOperation(
                        id = UUID.randomUUID().toString(),
                        itemId = offlineOp.id,
                        itemType = SyncItemType.AUDIO_FILE,
                        operation = SyncOperationType.CREATE,
                        timestamp = offlineOp.timestamp,
                        priority = mapOfflinePriorityToSyncPriority(offlineOp.priority)
                    )
                }
                com.voicenotesai.domain.offline.OperationType.BACKUP_DATA -> {
                    SyncOperation(
                        id = UUID.randomUUID().toString(),
                        itemId = offlineOp.id,
                        itemType = SyncItemType.BACKUP,
                        operation = SyncOperationType.CREATE,
                        timestamp = offlineOp.timestamp,
                        priority = mapOfflinePriorityToSyncPriority(offlineOp.priority)
                    )
                }
                else -> null // Skip non-sync operations
            }
        }
    }

    /**
     * Processes sync operations in batches with concurrency control.
     */
    private suspend fun processSyncOperations(operations: List<SyncOperation>): SyncResult {
        var completed = 0
        var failed = 0
        var conflicted = 0
        val failedOperations = mutableListOf<SyncOperation>()
        val detectedConflicts = mutableListOf<SyncConflict>()

        // Process operations in batches
        operations.chunked(SYNC_BATCH_SIZE).forEach { batch ->
            // Update progress
            _syncProgress.value = SyncProgress(
                totalOperations = operations.size,
                completedOperations = completed,
                failedOperations = failed,
                conflictedOperations = conflicted,
                currentOperation = batch.firstOrNull(),
                progressPercentage = (completed + failed + conflicted).toFloat() / operations.size * 100f
            )

            // Process batch with limited concurrency
            val semaphore = Semaphore(MAX_CONCURRENT_OPERATIONS)
            
            coroutineScope {
                batch.map { operation ->
                    async {
                        semaphore.withPermit {
                            processSingleSyncOperation(operation)
                        }
                    }
                }.awaitAll()
            }.forEach { result ->
                when {
                    result.success -> completed++
                    result.conflict != null -> {
                        conflicted++
                        detectedConflicts.add(result.conflict)
                    }
                    else -> {
                        failed++
                        result.operation?.let { failedOperations.add(it) }
                    }
                }
            }
        }

        return SyncResult(
            success = failed == 0 && conflicted == 0,
            operationsCompleted = completed,
            operationsFailed = failed,
            conflictsDetected = conflicted,
            syncTimeMs = 0, // Will be set by caller
            partialSuccess = completed > 0 && (failed > 0 || conflicted > 0),
            failedOperations = failedOperations,
            conflicts = detectedConflicts
        )
    }

    /**
     * Processes a single sync operation.
     */
    private suspend fun processSingleSyncOperation(operation: SyncOperation): SingleSyncResult {
        try {
            // Update operation status
            val processingOperation = operation.copy(
                status = SyncOperationStatus.InProgress,
                lastAttemptTimestamp = System.currentTimeMillis()
            )
            syncOperations[operation.id] = processingOperation

            // Simulate sync operation based on type
            val result = when (operation.itemType) {
                SyncItemType.NOTE -> syncNote(operation)
                SyncItemType.AUDIO_FILE -> syncAudioFile(operation)
                SyncItemType.SETTINGS -> syncSettings(operation)
                SyncItemType.USER_PREFERENCES -> syncUserPreferences(operation)
                SyncItemType.AI_MODEL -> syncAIModel(operation)
                SyncItemType.BACKUP -> syncBackup(operation)
            }

            // Update operation status based on result
            val finalOperation = when {
                result.conflict != null -> {
                    conflicts[result.conflict.id] = result.conflict
                    _conflicts.emit(result.conflict)
                    operation.copy(status = SyncOperationStatus.ConflictDetected)
                }
                result.success -> operation.copy(status = SyncOperationStatus.Completed, error = null)
                else -> operation.copy(
                    status = if (operation.retryAttempts < operation.maxRetryAttempts) 
                        SyncOperationStatus.Failed else SyncOperationStatus.Failed,
                    error = result.error
                )
            }
            
            syncOperations[operation.id] = finalOperation
            return result.copy(operation = finalOperation)

        } catch (e: Exception) {
            val failedOperation = operation.copy(
                status = SyncOperationStatus.Failed,
                error = "Sync operation failed: ${e.message}"
            )
            syncOperations[operation.id] = failedOperation
            
            return SingleSyncResult(
                success = false,
                operation = failedOperation,
                error = e.message
            )
        }
    }

    /**
     * Syncs a note with conflict detection.
     */
    private suspend fun syncNote(operation: SyncOperation): SingleSyncResult {
        // Simulate network delay
        delay(100)
        
        // Simulate conflict detection (20% chance)
        if (Math.random() < 0.2) {
            val conflict = SyncConflict(
                id = UUID.randomUUID().toString(),
                itemId = operation.itemId,
                itemType = operation.itemType,
                conflictType = ConflictType.CONCURRENT_MODIFICATION,
                localVersion = ConflictVersion(
                    data = "Local note content",
                    timestamp = System.currentTimeMillis() - 60000,
                    version = "1.0",
                    checksum = "local_checksum",
                    source = ConflictSource.LOCAL
                ),
                remoteVersion = ConflictVersion(
                    data = "Remote note content",
                    timestamp = System.currentTimeMillis() - 30000,
                    version = "1.1",
                    checksum = "remote_checksum",
                    source = ConflictSource.REMOTE
                ),
                timestamp = System.currentTimeMillis(),
                autoResolvable = false
            )
            
            return SingleSyncResult(
                success = false,
                conflict = conflict
            )
        }
        
        // Simulate successful sync
        return SingleSyncResult(success = true)
    }

    /**
     * Syncs an audio file.
     */
    private suspend fun syncAudioFile(operation: SyncOperation): SingleSyncResult {
        // Simulate longer upload time for audio files
        delay(500)
        
        // Simulate occasional failure (10% chance)
        if (Math.random() < 0.1) {
            return SingleSyncResult(
                success = false,
                error = "Network timeout during audio upload"
            )
        }
        
        return SingleSyncResult(success = true)
    }

    /**
     * Syncs settings.
     */
    private suspend fun syncSettings(operation: SyncOperation): SingleSyncResult {
        delay(50)
        return SingleSyncResult(success = true)
    }

    /**
     * Syncs user preferences.
     */
    private suspend fun syncUserPreferences(operation: SyncOperation): SingleSyncResult {
        delay(50)
        return SingleSyncResult(success = true)
    }

    /**
     * Syncs AI model.
     */
    private suspend fun syncAIModel(operation: SyncOperation): SingleSyncResult {
        // Simulate long download time
        delay(2000)
        return SingleSyncResult(success = true)
    }

    /**
     * Syncs backup data.
     */
    private suspend fun syncBackup(operation: SyncOperation): SingleSyncResult {
        delay(1000)
        return SingleSyncResult(success = true)
    }

    /**
     * Applies conflict resolution strategy to resolve conflicts.
     */
    private suspend fun applyConflictResolution(conflict: SyncConflict, resolution: ConflictResolution): Any {
        return when (resolution) {
            is ConflictResolution.UseLocal -> conflict.localVersion.data
            is ConflictResolution.UseRemote -> conflict.remoteVersion.data
            is ConflictResolution.UseMerged -> resolution.mergedData
            is ConflictResolution.Skip -> "skipped"
            is ConflictResolution.Custom -> resolution.customData
        }
    }

    /**
     * Maps offline operation priority to sync priority.
     */
    private fun mapOfflinePriorityToSyncPriority(offlinePriority: com.voicenotesai.domain.offline.OperationPriority): SyncPriority {
        return when (offlinePriority) {
            com.voicenotesai.domain.offline.OperationPriority.Low -> SyncPriority.Low
            com.voicenotesai.domain.offline.OperationPriority.Normal -> SyncPriority.Normal
            com.voicenotesai.domain.offline.OperationPriority.High -> SyncPriority.High
            com.voicenotesai.domain.offline.OperationPriority.Critical -> SyncPriority.Critical
        }
    }

    /**
     * Result of processing a single sync operation.
     */
    private data class SingleSyncResult(
        val success: Boolean,
        val operation: SyncOperation? = null,
        val conflict: SyncConflict? = null,
        val error: String? = null
    )

    /**
     * Tracks sync metrics and statistics.
     */
    private class SyncMetricsTracker {
        private val totalOperations = AtomicLong(0)
        private val successfulOperations = AtomicLong(0)
        private val failedOperations = AtomicLong(0)
        private val conflictedOperations = AtomicLong(0)
        private val totalSyncTime = AtomicLong(0)
        private val dataTransferred = AtomicLong(0)
        private var lastSyncTimestamp: Long? = null

        fun recordSyncOperation(syncTimeMs: Long, completed: Int, failed: Int) {
            totalOperations.addAndGet((completed + failed).toLong())
            successfulOperations.addAndGet(completed.toLong())
            failedOperations.addAndGet(failed.toLong())
            totalSyncTime.addAndGet(syncTimeMs)
            lastSyncTimestamp = System.currentTimeMillis()
        }

        fun getMetrics(): SyncMetrics {
            val total = totalOperations.get()
            val successful = successfulOperations.get()
            val failed = failedOperations.get()
            val conflicted = conflictedOperations.get()
            
            return SyncMetrics(
                totalSyncOperations = total,
                successfulOperations = successful,
                failedOperations = failed,
                conflictedOperations = conflicted,
                averageSyncTimeMs = if (total > 0) totalSyncTime.get() / total else 0,
                lastSyncTimestamp = lastSyncTimestamp,
                nextScheduledSyncTimestamp = lastSyncTimestamp?.plus(300000), // 5 minutes
                dataTransferredBytes = dataTransferred.get(),
                conflictResolutionRate = if (conflicted > 0) successful.toFloat() / (successful + conflicted) else 1.0f,
                syncReliabilityScore = if (total > 0) successful.toFloat() / total else 1.0f
            )
        }
    }
}