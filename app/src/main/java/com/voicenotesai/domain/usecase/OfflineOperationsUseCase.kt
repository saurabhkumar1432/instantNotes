package com.voicenotesai.domain.usecase

import com.voicenotesai.domain.offline.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for managing offline operations queue.
 * Handles queuing, processing, and monitoring of operations that need to be executed offline or when connectivity is restored.
 */
class OfflineOperationsUseCase @Inject constructor(
    private val offlineOperationsQueue: OfflineOperationsQueue
) {

    /**
     * Enqueues an operation for offline processing.
     */
    suspend fun enqueueOperation(operation: OfflineOperation): EnqueueResult {
        return offlineOperationsQueue.enqueueOperation(operation)
    }

    /**
     * Enqueues an audio transcription operation.
     */
    suspend fun enqueueAudioTranscription(
        audioFilePath: String,
        audioData: ByteArray,
        language: String? = null,
        priority: OperationPriority = OperationPriority.Normal
    ): EnqueueResult {
        val operation = OfflineOperation(
            id = "transcribe_${System.currentTimeMillis()}",
            type = OperationType.TRANSCRIBE_AUDIO,
            data = OperationData.AudioTranscription(
                audioFilePath = audioFilePath,
                audioData = audioData,
                language = language
            ),
            timestamp = System.currentTimeMillis(),
            priority = priority
        )
        
        return offlineOperationsQueue.enqueueOperation(operation)
    }

    /**
     * Enqueues a note generation operation.
     */
    suspend fun enqueueNoteGeneration(
        transcription: String,
        noteFormat: String = "bullets",
        priority: OperationPriority = OperationPriority.Normal
    ): EnqueueResult {
        val operation = OfflineOperation(
            id = "generate_notes_${System.currentTimeMillis()}",
            type = OperationType.GENERATE_NOTES,
            data = OperationData.NoteGeneration(
                transcription = transcription,
                noteFormat = noteFormat
            ),
            timestamp = System.currentTimeMillis(),
            priority = priority
        )
        
        return offlineOperationsQueue.enqueueOperation(operation)
    }

    /**
     * Enqueues an AI processing operation.
     */
    suspend fun enqueueAIProcessing(
        requestType: String,
        inputData: String,
        parameters: Map<String, Any> = emptyMap(),
        priority: OperationPriority = OperationPriority.Normal
    ): EnqueueResult {
        val operation = OfflineOperation(
            id = "ai_process_${System.currentTimeMillis()}",
            type = OperationType.PROCESS_AI_REQUEST,
            data = OperationData.AIProcessing(
                requestType = requestType,
                inputData = inputData,
                parameters = parameters
            ),
            timestamp = System.currentTimeMillis(),
            priority = priority
        )
        
        return offlineOperationsQueue.enqueueOperation(operation)
    }

    /**
     * Gets all pending operations in the queue.
     */
    suspend fun getPendingOperations(): List<OfflineOperation> {
        return offlineOperationsQueue.getPendingOperations()
    }

    /**
     * Processes the next operation in the queue.
     */
    suspend fun processNextOperation(): ProcessOperationResult {
        return offlineOperationsQueue.processNextOperation()
    }

    /**
     * Processes all pending operations in the queue.
     */
    suspend fun processAllOperations(): Flow<BatchProcessingState> {
        return offlineOperationsQueue.processAllOperations()
    }

    /**
     * Removes a specific operation from the queue.
     */
    suspend fun removeOperation(operationId: String): RemovalResult {
        return offlineOperationsQueue.removeOperation(operationId)
    }

    /**
     * Clears all operations from the queue.
     */
    suspend fun clearQueue(): ClearResult {
        return offlineOperationsQueue.clearQueue()
    }

    /**
     * Gets the current status of the queue.
     */
    suspend fun getQueueStatus(): QueueStatus {
        return offlineOperationsQueue.getQueueStatus()
    }

    /**
     * Retries a failed operation.
     */
    suspend fun retryOperation(operationId: String): RetryResult {
        return offlineOperationsQueue.retryOperation(operationId)
    }

    /**
     * Retries all failed operations in the queue.
     */
    suspend fun retryAllFailedOperations(): RetryAllResult {
        return try {
            val queueStatus = offlineOperationsQueue.getQueueStatus()
            val pendingOperations = offlineOperationsQueue.getPendingOperations()
            
            val failedOperations = pendingOperations.filter { 
                it.status == OperationStatus.Failed 
            }
            
            var successCount = 0
            var failureCount = 0
            val errors = mutableListOf<String>()
            
            failedOperations.forEach { operation ->
                val retryResult = offlineOperationsQueue.retryOperation(operation.id)
                if (retryResult.success) {
                    successCount++
                } else {
                    failureCount++
                    retryResult.error?.let { errors.add(it) }
                }
            }
            
            RetryAllResult(
                success = failureCount == 0,
                totalOperations = failedOperations.size,
                successfulRetries = successCount,
                failedRetries = failureCount,
                errors = errors
            )
            
        } catch (e: Exception) {
            RetryAllResult(
                success = false,
                totalOperations = 0,
                successfulRetries = 0,
                failedRetries = 0,
                errors = listOf("Failed to retry operations: ${e.message}")
            )
        }
    }

    /**
     * Observes queue changes for real-time updates.
     */
    fun observeQueueChanges(): Flow<QueueChangeEvent> {
        return offlineOperationsQueue.observeQueueChanges()
    }

    /**
     * Sets the maximum number of retry attempts for operations.
     */
    fun setMaxRetryAttempts(maxAttempts: Int) {
        offlineOperationsQueue.setMaxRetryAttempts(maxAttempts)
    }

    /**
     * Gets operations by type.
     */
    suspend fun getOperationsByType(type: OperationType): List<OfflineOperation> {
        val allOperations = offlineOperationsQueue.getPendingOperations()
        return allOperations.filter { it.type == type }
    }

    /**
     * Gets operations by status.
     */
    suspend fun getOperationsByStatus(status: OperationStatus): List<OfflineOperation> {
        val allOperations = offlineOperationsQueue.getPendingOperations()
        return allOperations.filter { it.status == status }
    }

    /**
     * Gets operations by priority.
     */
    suspend fun getOperationsByPriority(priority: OperationPriority): List<OfflineOperation> {
        val allOperations = offlineOperationsQueue.getPendingOperations()
        return allOperations.filter { it.priority == priority }
    }

    /**
     * Cancels an operation (marks it as cancelled).
     */
    suspend fun cancelOperation(operationId: String): CancelResult {
        return try {
            val pendingOperations = offlineOperationsQueue.getPendingOperations()
            val operation = pendingOperations.find { it.id == operationId }
                ?: return CancelResult(false, "Operation not found")

            if (operation.status == OperationStatus.Processing) {
                return CancelResult(false, "Cannot cancel operation that is currently processing")
            }

            val cancelledOperation = operation.copy(status = OperationStatus.Cancelled)
            
            // Remove the operation from the queue
            val removeResult = offlineOperationsQueue.removeOperation(operationId)
            
            CancelResult(
                success = removeResult.success,
                error = removeResult.error
            )
            
        } catch (e: Exception) {
            CancelResult(false, "Failed to cancel operation: ${e.message}")
        }
    }

    /**
     * Gets queue performance metrics.
     */
    suspend fun getQueueMetrics(): QueueMetrics {
        val status = offlineOperationsQueue.getQueueStatus()
        
        return QueueMetrics(
            totalOperations = status.totalOperations,
            pendingOperations = status.pendingOperations,
            processingOperations = status.processingOperations,
            completedOperations = status.completedOperations,
            failedOperations = status.failedOperations,
            averageProcessingTimeMs = status.averageProcessingTimeMs,
            queueSizeBytes = status.queueSizeBytes,
            successRate = if (status.totalOperations > 0) {
                status.completedOperations.toFloat() / status.totalOperations
            } else 0f,
            failureRate = if (status.totalOperations > 0) {
                status.failedOperations.toFloat() / status.totalOperations
            } else 0f
        )
    }
}

/**
 * Result of retrying all failed operations.
 */
data class RetryAllResult(
    val success: Boolean,
    val totalOperations: Int,
    val successfulRetries: Int,
    val failedRetries: Int,
    val errors: List<String>
)

/**
 * Result of cancelling an operation.
 */
data class CancelResult(
    val success: Boolean,
    val error: String? = null
)

/**
 * Queue performance metrics.
 */
data class QueueMetrics(
    val totalOperations: Int,
    val pendingOperations: Int,
    val processingOperations: Int,
    val completedOperations: Int,
    val failedOperations: Int,
    val averageProcessingTimeMs: Long,
    val queueSizeBytes: Long,
    val successRate: Float,
    val failureRate: Float
)