package com.voicenotesai.domain.offline

import com.voicenotesai.domain.model.AudioData
import kotlinx.coroutines.flow.Flow

/**
 * Manager for offline audio recording and local storage.
 * Handles recording audio when network is unavailable and stores it locally for later processing.
 */
interface OfflineRecordingManager {
    
    /**
     * Starts offline recording with local storage.
     * Records audio directly to local storage without requiring network connectivity.
     */
    suspend fun startOfflineRecording(): Flow<OfflineRecordingState>
    
    /**
     * Stops offline recording and returns the recorded audio data.
     */
    suspend fun stopOfflineRecording(): OfflineRecordingResult
    
    /**
     * Gets all pending offline recordings that haven't been processed yet.
     */
    suspend fun getPendingRecordings(): List<OfflineRecording>
    
    /**
     * Processes a specific offline recording using local AI capabilities.
     */
    suspend fun processOfflineRecording(recordingId: String): ProcessingResult
    
    /**
     * Deletes an offline recording and its associated files.
     */
    suspend fun deleteOfflineRecording(recordingId: String): DeletionResult
    
    /**
     * Gets storage information for offline recordings.
     */
    suspend fun getStorageInfo(): OfflineStorageInfo
    
    /**
     * Cleans up old offline recordings based on retention policy.
     */
    suspend fun cleanupOldRecordings(): CleanupResult
    
    /**
     * Checks if offline recording is currently active.
     */
    fun isRecording(): Boolean
    
    /**
     * Gets the current recording session if active.
     */
    fun getCurrentRecording(): OfflineRecording?
}

/**
 * States during offline recording.
 */
sealed class OfflineRecordingState {
    object Idle : OfflineRecordingState()
    data class Recording(
        val duration: Long,
        val fileSizeBytes: Long,
        val recordingId: String
    ) : OfflineRecordingState()
    data class Saving(val recordingId: String) : OfflineRecordingState()
    data class Saved(val recording: OfflineRecording) : OfflineRecordingState()
    data class Error(val message: String, val recordingId: String? = null) : OfflineRecordingState()
}

/**
 * Result of stopping offline recording.
 */
sealed class OfflineRecordingResult {
    data class Success(val recording: OfflineRecording) : OfflineRecordingResult()
    data class Error(val message: String) : OfflineRecordingResult()
}

/**
 * Represents an offline recording stored locally.
 */
data class OfflineRecording(
    val id: String,
    val filePath: String,
    val timestamp: Long,
    val duration: Long,
    val fileSizeBytes: Long,
    val audioFormat: String,
    val sampleRate: Int,
    val channels: Int,
    val isProcessed: Boolean = false,
    val transcription: String? = null,
    val generatedNotes: String? = null,
    val processingError: String? = null,
    val encryptionKeyId: String? = null
)

/**
 * Result of processing an offline recording.
 */
data class ProcessingResult(
    val success: Boolean,
    val transcription: String? = null,
    val generatedNotes: String? = null,
    val processingTimeMs: Long = 0,
    val error: String? = null
)

/**
 * Result of deleting an offline recording.
 */
data class DeletionResult(
    val success: Boolean,
    val freedSpaceBytes: Long = 0,
    val error: String? = null
)

/**
 * Information about offline storage usage.
 */
data class OfflineStorageInfo(
    val totalRecordings: Int,
    val totalSizeBytes: Long,
    val availableSpaceBytes: Long,
    val oldestRecordingTimestamp: Long?,
    val newestRecordingTimestamp: Long?,
    val processedCount: Int,
    val pendingCount: Int
)

/**
 * Result of cleanup operation.
 */
data class CleanupResult(
    val success: Boolean,
    val recordingsDeleted: Int = 0,
    val spaceFreedBytes: Long = 0,
    val error: String? = null
)

/**
 * Configuration for offline recording.
 */
data class OfflineRecordingConfig(
    val maxRecordingDurationMs: Long = 300000, // 5 minutes
    val maxFileSizeBytes: Long = 50 * 1024 * 1024, // 50MB
    val audioFormat: String = "wav",
    val sampleRate: Int = 44100,
    val channels: Int = 1,
    val enableEncryption: Boolean = true,
    val retentionDays: Int = 30,
    val maxStorageBytes: Long = 500 * 1024 * 1024 // 500MB
)