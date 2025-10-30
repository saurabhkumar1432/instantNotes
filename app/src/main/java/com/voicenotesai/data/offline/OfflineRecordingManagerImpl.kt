package com.voicenotesai.data.offline

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.voicenotesai.data.ai.LocalAIEngineImpl
import com.voicenotesai.domain.ai.LocalAIEngine
import com.voicenotesai.domain.model.AudioData
import com.voicenotesai.domain.offline.*
import com.voicenotesai.domain.security.EncryptionService
import com.voicenotesai.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of OfflineRecordingManager that handles local audio recording and storage.
 * Records audio directly to device storage without requiring network connectivity.
 */
@Singleton
class OfflineRecordingManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionService: EncryptionService,
    private val localAIEngine: LocalAIEngine,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : OfflineRecordingManager {

    companion object {
        private const val RECORDINGS_DIR = "offline_recordings"
        private const val BUFFER_SIZE = 8192
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private val recordingsDir: File by lazy {
        File(context.filesDir, RECORDINGS_DIR).apply { mkdirs() }
    }

    private val isRecording = AtomicBoolean(false)
    private val recordingStartTime = AtomicLong(0)
    private var audioRecord: AudioRecord? = null
    private var currentRecording: OfflineRecording? = null
    private var recordingJob: Job? = null

    private val config = OfflineRecordingConfig()

    override suspend fun startOfflineRecording(): Flow<OfflineRecordingState> = callbackFlow {
        if (!hasAudioPermission()) {
            trySend(OfflineRecordingState.Error("Audio recording permission not granted"))
            close()
            return@callbackFlow
        }

        if (isRecording.get()) {
            trySend(OfflineRecordingState.Error("Recording already in progress"))
            close()
            return@callbackFlow
        }

        val recordingId = UUID.randomUUID().toString()
        val recordingFile = File(recordingsDir, "$recordingId.wav")
        
        try {
            // Initialize AudioRecord
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val bufferSize = maxOf(minBufferSize, BUFFER_SIZE)
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                trySend(OfflineRecordingState.Error("Failed to initialize audio recorder"))
                close()
                return@callbackFlow
            }

            // Start recording
            audioRecord?.startRecording()
            isRecording.set(true)
            recordingStartTime.set(System.currentTimeMillis())

            // Create recording metadata
            currentRecording = OfflineRecording(
                id = recordingId,
                filePath = recordingFile.absolutePath,
                timestamp = recordingStartTime.get(),
                duration = 0,
                fileSizeBytes = 0,
                audioFormat = "wav",
                sampleRate = SAMPLE_RATE,
                channels = 1,
                encryptionKeyId = if (config.enableEncryption) "offline_recording_key" else null
            )

            trySend(OfflineRecordingState.Recording(0, 0, recordingId))

            // Start recording loop
            recordingJob = launch {
                recordAudioToFile(recordingFile, recordingId)
            }

            // Duration tracking loop
            launch {
                while (isRecording.get()) {
                    val duration = System.currentTimeMillis() - recordingStartTime.get()
                    val fileSize = if (recordingFile.exists()) recordingFile.length() else 0
                    
                    trySend(OfflineRecordingState.Recording(duration, fileSize, recordingId))
                    
                    // Check limits
                    if (duration >= config.maxRecordingDurationMs) {
                        stopRecordingInternal()
                        trySend(OfflineRecordingState.Error("Maximum recording duration reached", recordingId))
                        break
                    }
                    
                    if (fileSize >= config.maxFileSizeBytes) {
                        stopRecordingInternal()
                        trySend(OfflineRecordingState.Error("Maximum file size reached", recordingId))
                        break
                    }
                    
                    kotlinx.coroutines.delay(1000)
                }
            }

        } catch (e: Exception) {
            cleanup()
            trySend(OfflineRecordingState.Error("Failed to start recording: ${e.message}"))
            close()
        }

        awaitClose {
            cleanup()
        }
    }

    override suspend fun stopOfflineRecording(): OfflineRecordingResult = withContext(ioDispatcher) {
        try {
            if (!isRecording.get()) {
                return@withContext OfflineRecordingResult.Error("No recording in progress")
            }

            stopRecordingInternal()
            
            val recording = currentRecording
            if (recording == null) {
                return@withContext OfflineRecordingResult.Error("Recording metadata not found")
            }

            val recordingFile = File(recording.filePath)
            if (!recordingFile.exists()) {
                return@withContext OfflineRecordingResult.Error("Recording file not found")
            }

            // Update recording metadata
            val duration = System.currentTimeMillis() - recordingStartTime.get()
            val finalRecording = recording.copy(
                duration = duration,
                fileSizeBytes = recordingFile.length()
            )

            // Encrypt the recording if enabled
            if (config.enableEncryption) {
                encryptRecordingFile(recordingFile)
            }

            // Save recording metadata
            saveRecordingMetadata(finalRecording)
            
            currentRecording = null
            
            OfflineRecordingResult.Success(finalRecording)

        } catch (e: Exception) {
            cleanup()
            OfflineRecordingResult.Error("Failed to stop recording: ${e.message}")
        }
    }

    override suspend fun getPendingRecordings(): List<OfflineRecording> = withContext(ioDispatcher) {
        try {
            val metadataFiles = recordingsDir.listFiles { file ->
                file.name.endsWith(".metadata")
            } ?: emptyArray()

            metadataFiles.mapNotNull { metadataFile ->
                try {
                    loadRecordingMetadata(metadataFile)
                } catch (e: Exception) {
                    null // Skip corrupted metadata files
                }
            }.sortedByDescending { it.timestamp }

        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun processOfflineRecording(recordingId: String): ProcessingResult = withContext(ioDispatcher) {
        try {
            val recordings = getPendingRecordings()
            val recording = recordings.find { it.id == recordingId }
                ?: return@withContext ProcessingResult(false, error = "Recording not found")

            if (recording.isProcessed) {
                return@withContext ProcessingResult(
                    success = true,
                    transcription = recording.transcription,
                    generatedNotes = recording.generatedNotes
                )
            }

            val startTime = System.currentTimeMillis()
            
            // Load and decrypt audio data
            val audioData = loadAudioData(recording)
            
            // Process with local AI engine
            val processingResult = localAIEngine.processOffline(audioData)
            
            val processingTime = System.currentTimeMillis() - startTime
            
            if (processingResult.success) {
                // Update recording with results
                val updatedRecording = recording.copy(
                    isProcessed = true,
                    transcription = processingResult.transcription,
                    generatedNotes = processingResult.generatedNotes
                )
                
                saveRecordingMetadata(updatedRecording)
                
                ProcessingResult(
                    success = true,
                    transcription = processingResult.transcription,
                    generatedNotes = processingResult.generatedNotes,
                    processingTimeMs = processingTime
                )
            } else {
                // Update recording with error
                val updatedRecording = recording.copy(
                    processingError = processingResult.error
                )
                
                saveRecordingMetadata(updatedRecording)
                
                ProcessingResult(
                    success = false,
                    error = processingResult.error,
                    processingTimeMs = processingTime
                )
            }

        } catch (e: Exception) {
            ProcessingResult(false, error = "Processing failed: ${e.message}")
        }
    }

    override suspend fun deleteOfflineRecording(recordingId: String): DeletionResult = withContext(ioDispatcher) {
        try {
            val recordings = getPendingRecordings()
            val recording = recordings.find { it.id == recordingId }
                ?: return@withContext DeletionResult(false, error = "Recording not found")

            val recordingFile = File(recording.filePath)
            val metadataFile = File(recordingsDir, "$recordingId.metadata")
            
            var freedSpace = 0L
            
            if (recordingFile.exists()) {
                freedSpace += recordingFile.length()
                recordingFile.delete()
            }
            
            if (metadataFile.exists()) {
                freedSpace += metadataFile.length()
                metadataFile.delete()
            }

            DeletionResult(success = true, freedSpaceBytes = freedSpace)

        } catch (e: Exception) {
            DeletionResult(false, error = "Deletion failed: ${e.message}")
        }
    }

    override suspend fun getStorageInfo(): OfflineStorageInfo = withContext(ioDispatcher) {
        try {
            val recordings = getPendingRecordings()
            val totalSize = recordings.sumOf { it.fileSizeBytes }
            val availableSpace = recordingsDir.freeSpace
            val processedCount = recordings.count { it.isProcessed }
            val pendingCount = recordings.count { !it.isProcessed }

            OfflineStorageInfo(
                totalRecordings = recordings.size,
                totalSizeBytes = totalSize,
                availableSpaceBytes = availableSpace,
                oldestRecordingTimestamp = recordings.minOfOrNull { it.timestamp },
                newestRecordingTimestamp = recordings.maxOfOrNull { it.timestamp },
                processedCount = processedCount,
                pendingCount = pendingCount
            )

        } catch (e: Exception) {
            OfflineStorageInfo(0, 0, 0, null, null, 0, 0)
        }
    }

    override suspend fun cleanupOldRecordings(): CleanupResult = withContext(ioDispatcher) {
        try {
            val recordings = getPendingRecordings()
            val cutoffTime = System.currentTimeMillis() - (config.retentionDays * 24 * 60 * 60 * 1000L)
            
            val oldRecordings = recordings.filter { it.timestamp < cutoffTime }
            var deletedCount = 0
            var freedSpace = 0L
            
            oldRecordings.forEach { recording ->
                val result = deleteOfflineRecording(recording.id)
                if (result.success) {
                    deletedCount++
                    freedSpace += result.freedSpaceBytes
                }
            }

            CleanupResult(
                success = true,
                recordingsDeleted = deletedCount,
                spaceFreedBytes = freedSpace
            )

        } catch (e: Exception) {
            CleanupResult(false, error = "Cleanup failed: ${e.message}")
        }
    }

    override fun isRecording(): Boolean = isRecording.get()

    override fun getCurrentRecording(): OfflineRecording? = currentRecording

    /**
     * Records audio data to file in a background coroutine.
     */
    private suspend fun recordAudioToFile(file: File, recordingId: String) = withContext(ioDispatcher) {
        try {
            FileOutputStream(file).use { outputStream ->
                val buffer = ByteArray(BUFFER_SIZE)
                
                while (isRecording.get()) {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    
                    if (bytesRead > 0) {
                        outputStream.write(buffer, 0, bytesRead)
                        outputStream.flush()
                    }
                }
            }
        } catch (e: Exception) {
            // Recording will be stopped by error handling in the main flow
        }
    }

    /**
     * Stops the recording process and cleans up resources.
     */
    private fun stopRecordingInternal() {
        isRecording.set(false)
        recordingJob?.cancel()
        recordingJob = null
        
        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            // Ignore stop errors
        }
        
        cleanup()
    }

    /**
     * Cleans up audio recording resources.
     */
    private fun cleanup() {
        try {
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore cleanup errors
        } finally {
            audioRecord = null
            isRecording.set(false)
        }
    }

    /**
     * Checks if audio recording permission is granted.
     */
    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Encrypts a recording file if encryption is enabled.
     */
    private suspend fun encryptRecordingFile(file: File) {
        if (!config.enableEncryption) return
        
        try {
            val audioData = file.readBytes()
            val encryptedData = encryptionService.encryptAudio(audioData)
            
            // Write encrypted data back to file
            file.writeBytes(encryptedData.encryptedBytes)
            
        } catch (e: Exception) {
            // Log error but don't fail the recording
        }
    }

    /**
     * Loads audio data from a recording, handling decryption if needed.
     */
    private suspend fun loadAudioData(recording: OfflineRecording): AudioData {
        val file = File(recording.filePath)
        val audioBytes = file.readBytes()
        
        val decryptedBytes = if (recording.encryptionKeyId != null) {
            val encryptedData = com.voicenotesai.domain.security.EncryptedData(
                encryptedBytes = audioBytes,
                metadata = com.voicenotesai.domain.security.EncryptionMetadata(
                    algorithm = "AES/GCM/NoPadding",
                    keyAlias = recording.encryptionKeyId,
                    iv = ByteArray(12) // This should be stored with the recording
                )
            )
            encryptionService.decryptAudio(encryptedData)
        } else {
            audioBytes
        }

        return AudioData(
            data = decryptedBytes,
            format = com.voicenotesai.domain.model.AudioFormat.WAV,
            sampleRate = recording.sampleRate,
            channels = recording.channels,
            durationMs = recording.duration,
            file = file
        )
    }

    /**
     * Saves recording metadata to a file.
     */
    private suspend fun saveRecordingMetadata(recording: OfflineRecording) = withContext(ioDispatcher) {
        val metadataFile = File(recordingsDir, "${recording.id}.metadata")
        
        // Simple JSON-like format for metadata
        val metadata = """
            {
                "id": "${recording.id}",
                "filePath": "${recording.filePath}",
                "timestamp": ${recording.timestamp},
                "duration": ${recording.duration},
                "fileSizeBytes": ${recording.fileSizeBytes},
                "audioFormat": "${recording.audioFormat}",
                "sampleRate": ${recording.sampleRate},
                "channels": ${recording.channels},
                "isProcessed": ${recording.isProcessed},
                "transcription": ${if (recording.transcription != null) "\"${recording.transcription}\"" else "null"},
                "generatedNotes": ${if (recording.generatedNotes != null) "\"${recording.generatedNotes}\"" else "null"},
                "processingError": ${if (recording.processingError != null) "\"${recording.processingError}\"" else "null"},
                "encryptionKeyId": ${if (recording.encryptionKeyId != null) "\"${recording.encryptionKeyId}\"" else "null"}
            }
        """.trimIndent()
        
        metadataFile.writeText(metadata)
    }

    /**
     * Loads recording metadata from a file.
     */
    private fun loadRecordingMetadata(metadataFile: File): OfflineRecording {
        val content = metadataFile.readText()
        
        // Simple parsing (in production, use a proper JSON library)
        val id = extractJsonValue(content, "id")
        val filePath = extractJsonValue(content, "filePath")
        val timestamp = extractJsonValue(content, "timestamp").toLong()
        val duration = extractJsonValue(content, "duration").toLong()
        val fileSizeBytes = extractJsonValue(content, "fileSizeBytes").toLong()
        val audioFormat = extractJsonValue(content, "audioFormat")
        val sampleRate = extractJsonValue(content, "sampleRate").toInt()
        val channels = extractJsonValue(content, "channels").toInt()
        val isProcessed = extractJsonValue(content, "isProcessed").toBoolean()
        val transcription = extractJsonValueOrNull(content, "transcription")
        val generatedNotes = extractJsonValueOrNull(content, "generatedNotes")
        val processingError = extractJsonValueOrNull(content, "processingError")
        val encryptionKeyId = extractJsonValueOrNull(content, "encryptionKeyId")
        
        return OfflineRecording(
            id = id,
            filePath = filePath,
            timestamp = timestamp,
            duration = duration,
            fileSizeBytes = fileSizeBytes,
            audioFormat = audioFormat,
            sampleRate = sampleRate,
            channels = channels,
            isProcessed = isProcessed,
            transcription = transcription,
            generatedNotes = generatedNotes,
            processingError = processingError,
            encryptionKeyId = encryptionKeyId
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
}