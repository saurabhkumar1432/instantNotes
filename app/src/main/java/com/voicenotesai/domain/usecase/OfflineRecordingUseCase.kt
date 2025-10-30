package com.voicenotesai.domain.usecase

import com.voicenotesai.domain.offline.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for managing offline recording operations.
 * Handles starting, stopping, and processing offline voice recordings.
 */
class OfflineRecordingUseCase @Inject constructor(
    private val offlineRecordingManager: OfflineRecordingManager,
    private val offlineOperationsQueue: OfflineOperationsQueue
) {

    /**
     * Starts offline recording and returns a flow of recording states.
     */
    suspend fun startOfflineRecording(): Flow<OfflineRecordingState> {
        return offlineRecordingManager.startOfflineRecording()
    }

    /**
     * Stops the current offline recording.
     */
    suspend fun stopOfflineRecording(): OfflineRecordingResult {
        return offlineRecordingManager.stopOfflineRecording()
    }

    /**
     * Gets all pending offline recordings.
     */
    suspend fun getPendingRecordings(): List<OfflineRecording> {
        return offlineRecordingManager.getPendingRecordings()
    }

    /**
     * Processes a specific offline recording.
     */
    suspend fun processOfflineRecording(recordingId: String): ProcessingResult {
        return offlineRecordingManager.processOfflineRecording(recordingId)
    }

    /**
     * Processes all pending offline recordings.
     */
    suspend fun processAllPendingRecordings(): Flow<BatchProcessingState> {
        val pendingRecordings = offlineRecordingManager.getPendingRecordings()
        
        // Queue processing operations for each pending recording
        pendingRecordings.filter { !it.isProcessed }.forEach { recording ->
            val operation = OfflineOperation(
                id = "process_${recording.id}",
                type = OperationType.TRANSCRIBE_AUDIO,
                data = OperationData.AudioTranscription(
                    audioFilePath = recording.filePath,
                    audioData = ByteArray(0), // Will be loaded during processing
                    language = null,
                    modelPreference = null
                ),
                timestamp = System.currentTimeMillis(),
                priority = OperationPriority.High
            )
            
            offlineOperationsQueue.enqueueOperation(operation)
        }
        
        // Process all queued operations
        return offlineOperationsQueue.processAllOperations()
    }

    /**
     * Deletes an offline recording.
     */
    suspend fun deleteOfflineRecording(recordingId: String): DeletionResult {
        return offlineRecordingManager.deleteOfflineRecording(recordingId)
    }

    /**
     * Gets storage information for offline recordings.
     */
    suspend fun getOfflineStorageInfo(): OfflineStorageInfo {
        return offlineRecordingManager.getStorageInfo()
    }

    /**
     * Cleans up old offline recordings based on retention policy.
     */
    suspend fun cleanupOldRecordings(): CleanupResult {
        return offlineRecordingManager.cleanupOldRecordings()
    }

    /**
     * Checks if offline recording is currently active.
     */
    fun isRecording(): Boolean {
        return offlineRecordingManager.isRecording()
    }

    /**
     * Gets the current recording session if active.
     */
    fun getCurrentRecording(): OfflineRecording? {
        return offlineRecordingManager.getCurrentRecording()
    }

    /**
     * Converts an offline recording to a note after processing.
     */
    suspend fun convertRecordingToNote(recordingId: String): ConversionResult {
        return try {
            val recordings = offlineRecordingManager.getPendingRecordings()
            val recording = recordings.find { it.id == recordingId }
                ?: return ConversionResult.Error("Recording not found")

            if (!recording.isProcessed) {
                // Process the recording first
                val processingResult = offlineRecordingManager.processOfflineRecording(recordingId)
                if (!processingResult.success) {
                    return ConversionResult.Error("Failed to process recording: ${processingResult.error}")
                }
            }

            // Get the updated recording with processing results
            val updatedRecordings = offlineRecordingManager.getPendingRecordings()
            val processedRecording = updatedRecordings.find { it.id == recordingId }
                ?: return ConversionResult.Error("Processed recording not found")

            val noteContent = processedRecording.generatedNotes ?: processedRecording.transcription ?: ""
            val transcription = processedRecording.transcription ?: ""

            ConversionResult.Success(
                content = noteContent,
                transcription = transcription,
                timestamp = processedRecording.timestamp,
                duration = processedRecording.duration,
                audioFilePath = processedRecording.filePath
            )

        } catch (e: Exception) {
            ConversionResult.Error("Conversion failed: ${e.message}")
        }
    }
}

/**
 * Result of converting an offline recording to a note.
 */
sealed class ConversionResult {
    data class Success(
        val content: String,
        val transcription: String,
        val timestamp: Long,
        val duration: Long,
        val audioFilePath: String
    ) : ConversionResult()
    
    data class Error(val message: String) : ConversionResult()
}