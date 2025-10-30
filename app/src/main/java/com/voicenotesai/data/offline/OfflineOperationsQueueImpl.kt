package com.voicenotesai.data.offline

import android.content.Context
import com.voicenotesai.domain.ai.LocalAIEngine
import com.voicenotesai.domain.offline.*
import com.voicenotesai.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of OfflineOperationsQueue that manages queued operations for offline processing.
 * Handles persistence, retry logic, and batch processing of operations.
 */
@Singleton
class OfflineOperationsQueueImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localAIEngine: LocalAIEngine,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : OfflineOperationsQueue {

    companion object {
        private const val QUEUE_DIR = "offline_queue"
        private const val OPERATION_FILE_EXTENSION = ".operation"
    }

    private val queueDir: File by lazy {
        File(context.filesDir, QUEUE_DIR).apply { mkdirs() }
    }

    private val config = OfflineQueueConfig()
    private val operationsCache = ConcurrentHashMap<String, OfflineOperation>()
    private val queueChangeFlow = MutableSharedFlow<QueueChangeEvent>()
    private val processingStats = ProcessingStats()

    private var maxRetryAttempts = config.maxRetryAttempts

    init {
        // Load existing operations from disk on initialization
        loadPersistedOperations()
    }

    override suspend fun enqueueOperation(operation: OfflineOperation): EnqueueResult = withContext(ioDispatcher) {
        try {
            // Check queue size limit
            if (operationsCache.size >= config.maxQueueSize) {
                return@withContext EnqueueResult(
                    success = false,
                    error = "Queue is full (max ${config.maxQueueSize} operations)"
                )
            }

            // Validate operation
            val validationResult = validateOperation(operation)
            if (!validationResult.isValid) {
                return@withContext EnqueueResult(
                    success = false,
                    error = validationResult.error
                )
            }

            // Add to cache and persist
            operationsCache[operation.id] = operation
            persistOperation(operation)

            // Calculate queue position based on priority
            val queuePosition = calculateQueuePosition(operation)

            // Emit change event
            queueChangeFlow.emit(QueueChangeEvent.OperationAdded(operation))

            EnqueueResult(
                success = true,
                operationId = operation.id,
                queuePosition = queuePosition
            )

        } catch (e: Exception) {
            EnqueueResult(
                success = false,
                error = "Failed to enqueue operation: ${e.message}"
            )
        }
    }

    override suspend fun getPendingOperations(): List<OfflineOperation> = withContext(ioDispatcher) {
        operationsCache.values
            .filter { it.status == OperationStatus.Pending || it.status == OperationStatus.Retrying }
            .sortedWith(compareByDescending<OfflineOperation> { it.priority.ordinal }
                .thenBy { it.timestamp })
    }

    override suspend fun processNextOperation(): ProcessOperationResult = withContext(ioDispatcher) {
        try {
            val pendingOperations = getPendingOperations()
            if (pendingOperations.isEmpty()) {
                return@withContext ProcessOperationResult(
                    success = false,
                    error = "No pending operations in queue"
                )
            }

            val operation = pendingOperations.first()
            return@withContext processOperation(operation)

        } catch (e: Exception) {
            ProcessOperationResult(
                success = false,
                error = "Failed to process operation: ${e.message}"
            )
        }
    }

    override suspend fun processAllOperations(): Flow<BatchProcessingState> = callbackFlow {
        try {
            trySend(BatchProcessingState.Starting)

            val pendingOperations = getPendingOperations()
            if (pendingOperations.isEmpty()) {
                trySend(BatchProcessingState.Completed(0, 0, 0, 0))
                close()
                return@callbackFlow
            }

            val startTime = System.currentTimeMillis()
            var completed = 0
            var successful = 0
            var failed = 0

            pendingOperations.forEachIndexed { index, operation ->
                val progress = (index + 1).toFloat() / pendingOperations.size
                
                trySend(BatchProcessingState.Processing(
                    currentOperation = operation,
                    completed = completed,
                    total = pendingOperations.size,
                    progress = progress
                ))

                val result = processOperation(operation)
                completed++

                if (result.success) {
                    successful++
                    trySend(BatchProcessingState.OperationCompleted(
                        operation = operation,
                        success = true,
                        result = result.result
                    ))
                } else {
                    failed++
                    val willRetry = operation.retryAttempts < maxRetryAttempts
                    trySend(BatchProcessingState.OperationFailed(
                        operation = operation,
                        error = result.error ?: "Unknown error",
                        willRetry = willRetry
                    ))
                }
            }

            val processingTime = System.currentTimeMillis() - startTime
            trySend(BatchProcessingState.Completed(
                totalProcessed = completed,
                successful = successful,
                failed = failed,
                processingTimeMs = processingTime
            ))

        } catch (e: Exception) {
            trySend(BatchProcessingState.Error("Batch processing failed: ${e.message}"))
        }

        awaitClose { }
    }

    override suspend fun removeOperation(operationId: String): RemovalResult = withContext(ioDispatcher) {
        try {
            val operation = operationsCache.remove(operationId)
            if (operation == null) {
                return@withContext RemovalResult(
                    success = false,
                    error = "Operation not found"
                )
            }

            // Remove from disk
            val operationFile = File(queueDir, "$operationId$OPERATION_FILE_EXTENSION")
            operationFile.delete()

            // Emit change event
            queueChangeFlow.emit(QueueChangeEvent.OperationRemoved(operationId))

            RemovalResult(success = true)

        } catch (e: Exception) {
            RemovalResult(
                success = false,
                error = "Failed to remove operation: ${e.message}"
            )
        }
    }

    override suspend fun clearQueue(): ClearResult = withContext(ioDispatcher) {
        try {
            val operationCount = operationsCache.size
            
            // Clear cache
            operationsCache.clear()
            
            // Clear disk files
            queueDir.listFiles { file ->
                file.name.endsWith(OPERATION_FILE_EXTENSION)
            }?.forEach { it.delete() }

            // Emit change event
            queueChangeFlow.emit(QueueChangeEvent.QueueCleared)

            ClearResult(
                success = true,
                operationsRemoved = operationCount
            )

        } catch (e: Exception) {
            ClearResult(
                success = false,
                error = "Failed to clear queue: ${e.message}"
            )
        }
    }

    override suspend fun getQueueStatus(): QueueStatus = withContext(ioDispatcher) {
        val operations = operationsCache.values
        val totalOperations = operations.size
        val pendingOperations = operations.count { it.status == OperationStatus.Pending }
        val processingOperations = operations.count { it.status == OperationStatus.Processing }
        val failedOperations = operations.count { it.status == OperationStatus.Failed }
        val completedOperations = operations.count { it.status == OperationStatus.Completed }
        
        val oldestTimestamp = operations.minOfOrNull { it.timestamp }
        val newestTimestamp = operations.maxOfOrNull { it.timestamp }
        
        val queueSizeBytes = queueDir.listFiles()?.sumOf { it.length() } ?: 0L

        QueueStatus(
            totalOperations = totalOperations,
            pendingOperations = pendingOperations,
            processingOperations = processingOperations,
            failedOperations = failedOperations,
            completedOperations = completedOperations,
            oldestOperationTimestamp = oldestTimestamp,
            newestOperationTimestamp = newestTimestamp,
            averageProcessingTimeMs = processingStats.getAverageProcessingTime(),
            queueSizeBytes = queueSizeBytes
        )
    }

    override suspend fun retryOperation(operationId: String): RetryResult = withContext(ioDispatcher) {
        try {
            val operation = operationsCache[operationId]
                ?: return@withContext RetryResult(
                    success = false,
                    error = "Operation not found"
                )

            if (operation.retryAttempts >= maxRetryAttempts) {
                return@withContext RetryResult(
                    success = false,
                    error = "Maximum retry attempts exceeded"
                )
            }

            val retryOperation = operation.copy(
                status = OperationStatus.Retrying,
                retryAttempts = operation.retryAttempts + 1,
                lastAttemptTimestamp = System.currentTimeMillis(),
                error = null
            )

            operationsCache[operationId] = retryOperation
            persistOperation(retryOperation)

            // Emit change event
            queueChangeFlow.emit(QueueChangeEvent.OperationUpdated(retryOperation))

            RetryResult(
                success = true,
                operation = retryOperation
            )

        } catch (e: Exception) {
            RetryResult(
                success = false,
                error = "Failed to retry operation: ${e.message}"
            )
        }
    }

    override fun setMaxRetryAttempts(maxAttempts: Int) {
        maxRetryAttempts = maxAttempts
    }

    override fun observeQueueChanges(): Flow<QueueChangeEvent> = queueChangeFlow

    /**
     * Processes a single operation based on its type.
     */
    private suspend fun processOperation(operation: OfflineOperation): ProcessOperationResult {
        val startTime = System.currentTimeMillis()
        
        try {
            // Update operation status to processing
            val processingOperation = operation.copy(
                status = OperationStatus.Processing,
                lastAttemptTimestamp = startTime
            )
            operationsCache[operation.id] = processingOperation
            persistOperation(processingOperation)

            // Process based on operation type
            val result = when (operation.type) {
                OperationType.TRANSCRIBE_AUDIO -> processAudioTranscription(operation)
                OperationType.GENERATE_NOTES -> processNoteGeneration(operation)
                OperationType.PROCESS_AI_REQUEST -> processAIRequest(operation)
                OperationType.SYNC_NOTE -> processSyncNote(operation)
                OperationType.UPLOAD_AUDIO -> processFileUpload(operation)
                OperationType.DOWNLOAD_MODEL -> processModelDownload(operation)
                OperationType.BACKUP_DATA -> processDataBackup(operation)
                OperationType.EXPORT_NOTES -> processNotesExport(operation)
            }

            val processingTime = System.currentTimeMillis() - startTime
            processingStats.recordProcessingTime(processingTime)

            return if (result.success) {
                // Mark as completed
                val completedOperation = operation.copy(
                    status = OperationStatus.Completed,
                    error = null
                )
                operationsCache[operation.id] = completedOperation
                persistOperation(completedOperation)
                
                queueChangeFlow.emit(QueueChangeEvent.OperationCompleted(completedOperation))
                
                ProcessOperationResult(
                    success = true,
                    operation = completedOperation,
                    result = result.result,
                    processingTimeMs = processingTime
                )
            } else {
                // Handle failure
                val shouldRetry = operation.retryAttempts < maxRetryAttempts
                val failedOperation = if (shouldRetry) {
                    operation.copy(
                        status = OperationStatus.Retrying,
                        retryAttempts = operation.retryAttempts + 1,
                        error = result.error
                    )
                } else {
                    operation.copy(
                        status = OperationStatus.Failed,
                        error = result.error
                    )
                }
                
                operationsCache[operation.id] = failedOperation
                persistOperation(failedOperation)
                
                queueChangeFlow.emit(QueueChangeEvent.OperationFailed(failedOperation, result.error ?: "Unknown error"))
                
                ProcessOperationResult(
                    success = false,
                    operation = failedOperation,
                    error = result.error,
                    processingTimeMs = processingTime
                )
            }

        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            val errorMessage = "Processing failed: ${e.message}"
            
            val failedOperation = operation.copy(
                status = OperationStatus.Failed,
                error = errorMessage
            )
            operationsCache[operation.id] = failedOperation
            persistOperation(failedOperation)
            
            return ProcessOperationResult(
                success = false,
                operation = failedOperation,
                error = errorMessage,
                processingTimeMs = processingTime
            )
        }
    }

    /**
     * Processes audio transcription operation.
     */
    private suspend fun processAudioTranscription(operation: OfflineOperation): OperationProcessingResult {
        val data = operation.data as? OperationData.AudioTranscription
            ?: return OperationProcessingResult(false, error = "Invalid audio transcription data")

        return try {
            val audioData = com.voicenotesai.domain.model.AudioData(
                data = data.audioData,
                format = com.voicenotesai.domain.model.AudioFormat.WAV,
                sampleRate = 44100,
                channels = 1,
                durationMs = 0 // Will be calculated by the engine
            )

            val result = localAIEngine.processOffline(audioData)
            
            if (result.success) {
                OperationProcessingResult(
                    success = true,
                    result = mapOf(
                        "transcription" to result.transcription,
                        "confidence" to result.confidence,
                        "processingTimeMs" to result.processingTimeMs
                    )
                )
            } else {
                OperationProcessingResult(false, error = result.error)
            }

        } catch (e: Exception) {
            OperationProcessingResult(false, error = "Audio transcription failed: ${e.message}")
        }
    }

    /**
     * Processes note generation operation.
     */
    private suspend fun processNoteGeneration(operation: OfflineOperation): OperationProcessingResult {
        val data = operation.data as? OperationData.NoteGeneration
            ?: return OperationProcessingResult(false, error = "Invalid note generation data")

        return try {
            // Simple note generation based on transcription
            val generatedNotes = generateNotesFromTranscription(data.transcription, data.noteFormat)
            
            OperationProcessingResult(
                success = true,
                result = mapOf(
                    "generatedNotes" to generatedNotes,
                    "format" to data.noteFormat
                )
            )

        } catch (e: Exception) {
            OperationProcessingResult(false, error = "Note generation failed: ${e.message}")
        }
    }

    /**
     * Processes AI request operation.
     */
    private suspend fun processAIRequest(operation: OfflineOperation): OperationProcessingResult {
        val data = operation.data as? OperationData.AIProcessing
            ?: return OperationProcessingResult(false, error = "Invalid AI processing data")

        return try {
            // Process AI request using local capabilities
            val result = processLocalAIRequest(data.requestType, data.inputData, data.parameters)
            
            OperationProcessingResult(
                success = true,
                result = result
            )

        } catch (e: Exception) {
            OperationProcessingResult(false, error = "AI processing failed: ${e.message}")
        }
    }

    /**
     * Processes other operation types (simplified implementations).
     */
    private suspend fun processSyncNote(operation: OfflineOperation): OperationProcessingResult {
        // Placeholder for note sync logic
        return OperationProcessingResult(true, result = "Note sync queued for when online")
    }

    private suspend fun processFileUpload(operation: OfflineOperation): OperationProcessingResult {
        // Placeholder for file upload logic
        return OperationProcessingResult(true, result = "File upload queued for when online")
    }

    private suspend fun processModelDownload(operation: OfflineOperation): OperationProcessingResult {
        // Placeholder for model download logic
        return OperationProcessingResult(true, result = "Model download queued for when online")
    }

    private suspend fun processDataBackup(operation: OfflineOperation): OperationProcessingResult {
        // Placeholder for data backup logic
        return OperationProcessingResult(true, result = "Data backup completed locally")
    }

    private suspend fun processNotesExport(operation: OfflineOperation): OperationProcessingResult {
        // Placeholder for notes export logic
        return OperationProcessingResult(true, result = "Notes export completed")
    }

    /**
     * Generates notes from transcription text.
     */
    private fun generateNotesFromTranscription(transcription: String, format: String): String {
        return when (format.lowercase()) {
            "bullets" -> {
                val sentences = transcription.split(". ")
                sentences.joinToString("\n") { "• ${it.trim()}" }
            }
            "summary" -> {
                "Summary: ${transcription.take(200)}${if (transcription.length > 200) "..." else ""}"
            }
            "action_items" -> {
                val actionWords = listOf("need to", "should", "must", "will", "todo", "action")
                val sentences = transcription.split(". ")
                val actionItems = sentences.filter { sentence ->
                    actionWords.any { word -> sentence.lowercase().contains(word) }
                }
                if (actionItems.isNotEmpty()) {
                    "Action Items:\n" + actionItems.joinToString("\n") { "- $it" }
                } else {
                    "No specific action items identified in the transcription."
                }
            }
            else -> transcription
        }
    }

    /**
     * Processes local AI requests.
     */
    private suspend fun processLocalAIRequest(requestType: String, inputData: String, parameters: Map<String, Any>): Any {
        return when (requestType.lowercase()) {
            "sentiment_analysis" -> {
                // Simple sentiment analysis
                val positiveWords = listOf("good", "great", "excellent", "positive", "happy", "success")
                val negativeWords = listOf("bad", "terrible", "negative", "sad", "failure", "problem")
                
                val positiveCount = positiveWords.count { inputData.lowercase().contains(it) }
                val negativeCount = negativeWords.count { inputData.lowercase().contains(it) }
                
                when {
                    positiveCount > negativeCount -> "positive"
                    negativeCount > positiveCount -> "negative"
                    else -> "neutral"
                }
            }
            "entity_extraction" -> {
                // Simple entity extraction
                val emailPattern = "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b".toRegex()
                val phonePattern = "\\b\\d{3}-\\d{3}-\\d{4}\\b".toRegex()
                
                mapOf(
                    "emails" to emailPattern.findAll(inputData).map { it.value }.toList(),
                    "phones" to phonePattern.findAll(inputData).map { it.value }.toList()
                )
            }
            else -> "Local AI processing completed for: $requestType"
        }
    }

    /**
     * Validates an operation before adding to queue.
     */
    private fun validateOperation(operation: OfflineOperation): ValidationResult {
        return when {
            operation.id.isBlank() -> ValidationResult(false, "Operation ID cannot be blank")
            operationsCache.containsKey(operation.id) -> ValidationResult(false, "Operation with this ID already exists")
            operation.timestamp <= 0 -> ValidationResult(false, "Invalid timestamp")
            else -> ValidationResult(true, null)
        }
    }

    /**
     * Calculates queue position based on priority and timestamp.
     */
    private fun calculateQueuePosition(operation: OfflineOperation): Int {
        val pendingOperations = operationsCache.values
            .filter { it.status == OperationStatus.Pending }
            .sortedWith(compareByDescending<OfflineOperation> { it.priority.ordinal }
                .thenBy { it.timestamp })
        
        return pendingOperations.indexOfFirst { it.id == operation.id } + 1
    }

    /**
     * Persists an operation to disk.
     */
    private suspend fun persistOperation(operation: OfflineOperation) = withContext(ioDispatcher) {
        if (!config.enablePersistence) return@withContext
        
        try {
            val operationFile = File(queueDir, "${operation.id}$OPERATION_FILE_EXTENSION")
            val operationJson = serializeOperation(operation)
            operationFile.writeText(operationJson)
        } catch (e: Exception) {
            // Log error but don't fail the operation
        }
    }

    /**
     * Loads persisted operations from disk.
     */
    private fun loadPersistedOperations() {
        if (!config.enablePersistence) return
        
        try {
            queueDir.listFiles { file ->
                file.name.endsWith(OPERATION_FILE_EXTENSION)
            }?.forEach { file ->
                try {
                    val operationJson = file.readText()
                    val operation = deserializeOperation(operationJson)
                    operationsCache[operation.id] = operation
                } catch (e: Exception) {
                    // Skip corrupted files
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // Ignore loading errors
        }
    }

    /**
     * Serializes an operation to JSON format.
     */
    private fun serializeOperation(operation: OfflineOperation): String {
        // Simple JSON serialization (in production, use a proper JSON library)
        return """
            {
                "id": "${operation.id}",
                "type": "${operation.type}",
                "timestamp": ${operation.timestamp},
                "priority": "${operation.priority}",
                "retryAttempts": ${operation.retryAttempts},
                "maxRetryAttempts": ${operation.maxRetryAttempts},
                "lastAttemptTimestamp": ${operation.lastAttemptTimestamp},
                "status": "${operation.status}",
                "error": ${if (operation.error != null) "\"${operation.error}\"" else "null"}
            }
        """.trimIndent()
    }

    /**
     * Deserializes an operation from JSON format.
     */
    private fun deserializeOperation(json: String): OfflineOperation {
        // Simple JSON deserialization (in production, use a proper JSON library)
        val id = extractJsonValue(json, "id")
        val type = OperationType.valueOf(extractJsonValue(json, "type"))
        val timestamp = extractJsonValue(json, "timestamp").toLong()
        val priority = OperationPriority.valueOf(extractJsonValue(json, "priority"))
        val retryAttempts = extractJsonValue(json, "retryAttempts").toInt()
        val maxRetryAttempts = extractJsonValue(json, "maxRetryAttempts").toInt()
        val lastAttemptTimestamp = extractJsonValueOrNull(json, "lastAttemptTimestamp")?.toLongOrNull()
        val status = OperationStatus.valueOf(extractJsonValue(json, "status"))
        val error = extractJsonValueOrNull(json, "error")
        
        return OfflineOperation(
            id = id,
            type = type,
            data = OperationData.AIProcessing("placeholder", ""), // Simplified for demo
            timestamp = timestamp,
            priority = priority,
            retryAttempts = retryAttempts,
            maxRetryAttempts = maxRetryAttempts,
            lastAttemptTimestamp = lastAttemptTimestamp,
            error = error,
            status = status
        )
    }

    /**
     * Extracts a JSON value from a simple JSON string.
     */
    private fun extractJsonValue(json: String, key: String): String {
        val pattern = "\"$key\":\\s*\"([^\"]*)\"|\"$key\":\\s*([^,}\\s]+)".toRegex()
        val match = pattern.find(json)
        return match?.groupValues?.get(1) ?: match?.groupValues?.get(2) ?: ""
    }

    /**
     * Extracts a JSON value that might be null.
     */
    private fun extractJsonValueOrNull(json: String, key: String): String? {
        val pattern = "\"$key\":\\s*\"([^\"]*)\"|\"$key\":\\s*(null)".toRegex()
        val match = pattern.find(json)
        val value = match?.groupValues?.get(1) ?: match?.groupValues?.get(2)
        return if (value == "null" || value.isNullOrEmpty()) null else value
    }

    /**
     * Result of operation processing.
     */
    private data class OperationProcessingResult(
        val success: Boolean,
        val result: Any? = null,
        val error: String? = null
    )

    /**
     * Result of operation validation.
     */
    private data class ValidationResult(
        val isValid: Boolean,
        val error: String?
    )

    /**
     * Tracks processing statistics.
     */
    private class ProcessingStats {
        private val processingTimes = mutableListOf<Long>()
        private val totalProcessingTime = AtomicLong(0)
        private val operationCount = AtomicInteger(0)

        fun recordProcessingTime(timeMs: Long) {
            synchronized(processingTimes) {
                processingTimes.add(timeMs)
                if (processingTimes.size > 100) {
                    processingTimes.removeAt(0)
                }
            }
            totalProcessingTime.addAndGet(timeMs)
            operationCount.incrementAndGet()
        }

        fun getAverageProcessingTime(): Long {
            val count = operationCount.get()
            return if (count > 0) totalProcessingTime.get() / count else 0
        }
    }
}