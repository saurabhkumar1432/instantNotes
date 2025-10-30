package com.voicenotesai.domain.offline

import kotlinx.coroutines.flow.Flow

/**
 * Queue manager for offline operations that need to be processed when connectivity is restored.
 * Handles queuing, persistence, and execution of operations that failed due to network issues.
 */
interface OfflineOperationsQueue {
    
    /**
     * Adds an operation to the offline queue.
     */
    suspend fun enqueueOperation(operation: OfflineOperation): EnqueueResult
    
    /**
     * Gets all pending operations in the queue.
     */
    suspend fun getPendingOperations(): List<OfflineOperation>
    
    /**
     * Processes the next operation in the queue.
     */
    suspend fun processNextOperation(): ProcessOperationResult
    
    /**
     * Processes all pending operations in the queue.
     */
    suspend fun processAllOperations(): Flow<BatchProcessingState>
    
    /**
     * Removes an operation from the queue.
     */
    suspend fun removeOperation(operationId: String): RemovalResult
    
    /**
     * Clears all operations from the queue.
     */
    suspend fun clearQueue(): ClearResult
    
    /**
     * Gets queue statistics and status.
     */
    suspend fun getQueueStatus(): QueueStatus
    
    /**
     * Retries a failed operation.
     */
    suspend fun retryOperation(operationId: String): RetryResult
    
    /**
     * Sets the maximum number of retry attempts for operations.
     */
    fun setMaxRetryAttempts(maxAttempts: Int)
    
    /**
     * Observes queue changes for real-time updates.
     */
    fun observeQueueChanges(): Flow<QueueChangeEvent>
}

/**
 * Represents an offline operation that needs to be processed.
 */
data class OfflineOperation(
    val id: String,
    val type: OperationType,
    val data: OperationData,
    val timestamp: Long,
    val priority: OperationPriority = OperationPriority.Normal,
    val retryAttempts: Int = 0,
    val maxRetryAttempts: Int = 3,
    val lastAttemptTimestamp: Long? = null,
    val error: String? = null,
    val status: OperationStatus = OperationStatus.Pending
)

/**
 * Types of offline operations.
 */
enum class OperationType {
    TRANSCRIBE_AUDIO,
    GENERATE_NOTES,
    SYNC_NOTE,
    PROCESS_AI_REQUEST,
    UPLOAD_AUDIO,
    DOWNLOAD_MODEL,
    BACKUP_DATA,
    EXPORT_NOTES
}

/**
 * Priority levels for operations.
 */
enum class OperationPriority {
    Low,
    Normal,
    High,
    Critical
}

/**
 * Status of an operation.
 */
enum class OperationStatus {
    Pending,
    Processing,
    Completed,
    Failed,
    Cancelled,
    Retrying
}

/**
 * Data payload for operations.
 */
sealed class OperationData {
    data class AudioTranscription(
        val audioFilePath: String,
        val audioData: ByteArray,
        val language: String? = null,
        val modelPreference: String? = null
    ) : OperationData() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as AudioTranscription
            if (audioFilePath != other.audioFilePath) return false
            if (!audioData.contentEquals(other.audioData)) return false
            if (language != other.language) return false
            if (modelPreference != other.modelPreference) return false
            return true
        }
        
        override fun hashCode(): Int {
            var result = audioFilePath.hashCode()
            result = 31 * result + audioData.contentHashCode()
            result = 31 * result + (language?.hashCode() ?: 0)
            result = 31 * result + (modelPreference?.hashCode() ?: 0)
            return result
        }
    }
    
    data class NoteGeneration(
        val transcription: String,
        val noteFormat: String,
        val additionalContext: Map<String, String> = emptyMap()
    ) : OperationData()
    
    data class NoteSync(
        val noteId: String,
        val noteData: String,
        val syncAction: String // "create", "update", "delete"
    ) : OperationData()
    
    data class AIProcessing(
        val requestType: String,
        val inputData: String,
        val parameters: Map<String, Any> = emptyMap()
    ) : OperationData()
    
    data class FileUpload(
        val filePath: String,
        val destination: String,
        val metadata: Map<String, String> = emptyMap()
    ) : OperationData()
    
    data class ModelDownload(
        val modelId: String,
        val downloadUrl: String,
        val expectedSize: Long,
        val checksum: String? = null
    ) : OperationData()
    
    data class DataBackup(
        val backupType: String,
        val includeAudio: Boolean,
        val destination: String
    ) : OperationData()
    
    data class NotesExport(
        val noteIds: List<String>,
        val format: String,
        val destination: String
    ) : OperationData()
}

/**
 * Result of enqueuing an operation.
 */
data class EnqueueResult(
    val success: Boolean,
    val operationId: String? = null,
    val queuePosition: Int? = null,
    val error: String? = null
)

/**
 * Result of processing an operation.
 */
data class ProcessOperationResult(
    val success: Boolean,
    val operation: OfflineOperation? = null,
    val result: Any? = null,
    val processingTimeMs: Long = 0,
    val error: String? = null
)

/**
 * States during batch processing.
 */
sealed class BatchProcessingState {
    object Starting : BatchProcessingState()
    data class Processing(
        val currentOperation: OfflineOperation,
        val completed: Int,
        val total: Int,
        val progress: Float
    ) : BatchProcessingState()
    data class OperationCompleted(
        val operation: OfflineOperation,
        val success: Boolean,
        val result: Any? = null
    ) : BatchProcessingState()
    data class OperationFailed(
        val operation: OfflineOperation,
        val error: String,
        val willRetry: Boolean
    ) : BatchProcessingState()
    data class Completed(
        val totalProcessed: Int,
        val successful: Int,
        val failed: Int,
        val processingTimeMs: Long
    ) : BatchProcessingState()
    data class Error(val message: String) : BatchProcessingState()
}

/**
 * Result of removing an operation.
 */
data class RemovalResult(
    val success: Boolean,
    val error: String? = null
)

/**
 * Result of clearing the queue.
 */
data class ClearResult(
    val success: Boolean,
    val operationsRemoved: Int = 0,
    val error: String? = null
)

/**
 * Status and statistics of the queue.
 */
data class QueueStatus(
    val totalOperations: Int,
    val pendingOperations: Int,
    val processingOperations: Int,
    val failedOperations: Int,
    val completedOperations: Int,
    val oldestOperationTimestamp: Long?,
    val newestOperationTimestamp: Long?,
    val averageProcessingTimeMs: Long,
    val queueSizeBytes: Long
)

/**
 * Result of retrying an operation.
 */
data class RetryResult(
    val success: Boolean,
    val operation: OfflineOperation? = null,
    val error: String? = null
)

/**
 * Events for queue changes.
 */
sealed class QueueChangeEvent {
    data class OperationAdded(val operation: OfflineOperation) : QueueChangeEvent()
    data class OperationUpdated(val operation: OfflineOperation) : QueueChangeEvent()
    data class OperationRemoved(val operationId: String) : QueueChangeEvent()
    data class OperationCompleted(val operation: OfflineOperation) : QueueChangeEvent()
    data class OperationFailed(val operation: OfflineOperation, val error: String) : QueueChangeEvent()
    object QueueCleared : QueueChangeEvent()
}

/**
 * Configuration for the offline operations queue.
 */
data class OfflineQueueConfig(
    val maxQueueSize: Int = 1000,
    val maxRetryAttempts: Int = 3,
    val retryDelayMs: Long = 5000,
    val maxOperationAgeHours: Int = 72,
    val enablePersistence: Boolean = true,
    val processingTimeoutMs: Long = 300000, // 5 minutes
    val batchSize: Int = 10
)