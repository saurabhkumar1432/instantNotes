package com.voicenotesai.data.offline

import android.content.Context
import android.content.pm.PackageManager
import com.voicenotesai.domain.ai.LocalAIEngine
import com.voicenotesai.domain.ai.OfflineProcessingResult
import com.voicenotesai.domain.model.AudioData
import com.voicenotesai.domain.model.AudioFormat
import com.voicenotesai.domain.offline.*
import com.voicenotesai.domain.security.EncryptionService
import com.voicenotesai.domain.security.EncryptedData
import com.voicenotesai.domain.security.EncryptionMetadata
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineRecordingManagerImplTest {

    private lateinit var context: Context
    private lateinit var encryptionService: EncryptionService
    private lateinit var localAIEngine: LocalAIEngine
    private lateinit var offlineRecordingManager: OfflineRecordingManagerImpl
    private lateinit var testDispatcher: UnconfinedTestDispatcher
    private lateinit var tempDir: File

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        encryptionService = mockk(relaxed = true)
        localAIEngine = mockk(relaxed = true)
        testDispatcher = UnconfinedTestDispatcher()

        // Create temporary directory for testing
        tempDir = createTempDir("offline_recording_test")
        
        // Mock context.filesDir to return our temp directory
        every { context.filesDir } returns tempDir
        
        // Mock permission check to return granted
        every { 
            context.checkSelfPermission(any()) 
        } returns PackageManager.PERMISSION_GRANTED

        offlineRecordingManager = OfflineRecordingManagerImpl(
            context = context,
            encryptionService = encryptionService,
            localAIEngine = localAIEngine,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        // Clean up temp directory
        tempDir.deleteRecursively()
        clearAllMocks()
    }

    @Test
    fun `startOfflineRecording should emit Recording state when permission granted`() = runTest {
        // Given
        every { 
            context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) 
        } returns PackageManager.PERMISSION_GRANTED

        // When
        val flow = offlineRecordingManager.startOfflineRecording()
        val firstState = flow.first()

        // Then
        assertTrue("First state should be Recording", firstState is OfflineRecordingState.Recording)
        val recordingState = firstState as OfflineRecordingState.Recording
        assertTrue("Duration should be non-negative", recordingState.duration >= 0)
        assertTrue("File size should be non-negative", recordingState.fileSizeBytes >= 0)
        assertNotNull("Recording ID should not be null", recordingState.recordingId)
    }

    @Test
    fun `startOfflineRecording should emit Error when permission denied`() = runTest {
        // Given
        every { 
            context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) 
        } returns PackageManager.PERMISSION_DENIED

        // When
        val flow = offlineRecordingManager.startOfflineRecording()
        val firstState = flow.first()

        // Then
        assertTrue("Should emit Error state", firstState is OfflineRecordingState.Error)
        val errorState = firstState as OfflineRecordingState.Error
        assertTrue("Error message should mention permission", 
            errorState.message.contains("permission", ignoreCase = true))
    }

    @Test
    fun `stopOfflineRecording should return error when not recording`() = runTest {
        // When
        val result = offlineRecordingManager.stopOfflineRecording()

        // Then
        assertTrue("Should return error result", result is OfflineRecordingResult.Error)
        val errorResult = result as OfflineRecordingResult.Error
        assertTrue("Error message should mention not recording", 
            errorResult.message.contains("not", ignoreCase = true))
    }

    @Test
    fun `getPendingRecordings should return empty list initially`() = runTest {
        // When
        val recordings = offlineRecordingManager.getPendingRecordings()

        // Then
        assertTrue("Should return empty list initially", recordings.isEmpty())
    }

    @Test
    fun `processOfflineRecording should return error for non-existent recording`() = runTest {
        // When
        val result = offlineRecordingManager.processOfflineRecording("non-existent-id")

        // Then
        assertFalse("Should return failure", result.success)
        assertNotNull("Should have error message", result.error)
        assertTrue("Error should mention not found", 
            result.error!!.contains("not found", ignoreCase = true))
    }

    @Test
    fun `processOfflineRecording should process successfully with valid recording`() = runTest {
        // Given
        val recordingId = "test-recording-123"
        val testRecording = createTestRecording(recordingId)
        
        // Create test recording file and metadata
        createTestRecordingFiles(testRecording)
        
        // Mock encryption service
        val encryptedData = EncryptedData(
            encryptedBytes = "test audio data".toByteArray(),
            metadata = EncryptionMetadata(
                algorithm = "AES/GCM/NoPadding",
                keyAlias = "test-key",
                iv = ByteArray(12)
            )
        )
        coEvery { encryptionService.decryptAudio(any()) } returns "decrypted audio data".toByteArray()

        // Mock local AI engine
        val aiResult = OfflineProcessingResult(
            success = true,
            transcription = "Test transcription",
            confidence = 0.85f,
            generatedNotes = "Test generated notes",
            processingTimeMs = 1000,
            modelUsed = null,
            error = null
        )
        coEvery { localAIEngine.processOffline(any()) } returns aiResult

        // When
        val result = offlineRecordingManager.processOfflineRecording(recordingId)

        // Then
        assertTrue("Processing should succeed", result.success)
        assertEquals("Should return transcription", "Test transcription", result.transcription)
        assertEquals("Should return generated notes", "Test generated notes", result.generatedNotes)
        assertTrue("Processing time should be positive", result.processingTimeMs > 0)
        
        // Verify AI engine was called
        coVerify { localAIEngine.processOffline(any()) }
    }

    @Test
    fun `deleteOfflineRecording should remove recording and files`() = runTest {
        // Given
        val recordingId = "test-recording-delete"
        val testRecording = createTestRecording(recordingId)
        createTestRecordingFiles(testRecording)

        // Verify files exist before deletion
        val recordingFile = File(testRecording.filePath)
        val metadataFile = File(tempDir, "offline_recordings/$recordingId.metadata")
        assertTrue("Recording file should exist", recordingFile.exists())
        assertTrue("Metadata file should exist", metadataFile.exists())

        // When
        val result = offlineRecordingManager.deleteOfflineRecording(recordingId)

        // Then
        assertTrue("Deletion should succeed", result.success)
        assertTrue("Should report freed space", result.freedSpaceBytes > 0)
        assertFalse("Recording file should be deleted", recordingFile.exists())
        assertFalse("Metadata file should be deleted", metadataFile.exists())
    }

    @Test
    fun `getStorageInfo should return accurate storage information`() = runTest {
        // Given
        val recording1 = createTestRecording("recording1")
        val recording2 = createTestRecording("recording2")
        createTestRecordingFiles(recording1)
        createTestRecordingFiles(recording2)

        // When
        val storageInfo = offlineRecordingManager.getStorageInfo()

        // Then
        assertEquals("Should report correct number of recordings", 2, storageInfo.totalRecordings)
        assertTrue("Should report total size", storageInfo.totalSizeBytes > 0)
        assertTrue("Should report available space", storageInfo.availableSpaceBytes > 0)
        assertEquals("Should report pending count", 2, storageInfo.pendingCount)
        assertEquals("Should report processed count", 0, storageInfo.processedCount)
    }

    @Test
    fun `cleanupOldRecordings should remove recordings older than retention period`() = runTest {
        // Given
        val oldTimestamp = System.currentTimeMillis() - (31 * 24 * 60 * 60 * 1000L) // 31 days ago
        val recentTimestamp = System.currentTimeMillis() - (1 * 24 * 60 * 60 * 1000L) // 1 day ago
        
        val oldRecording = createTestRecording("old-recording", oldTimestamp)
        val recentRecording = createTestRecording("recent-recording", recentTimestamp)
        
        createTestRecordingFiles(oldRecording)
        createTestRecordingFiles(recentRecording)

        // When
        val result = offlineRecordingManager.cleanupOldRecordings()

        // Then
        assertTrue("Cleanup should succeed", result.success)
        assertEquals("Should delete 1 old recording", 1, result.recordingsDeleted)
        assertTrue("Should report freed space", result.spaceFreedBytes > 0)
        
        // Verify only recent recording remains
        val remainingRecordings = offlineRecordingManager.getPendingRecordings()
        assertEquals("Should have 1 remaining recording", 1, remainingRecordings.size)
        assertEquals("Remaining recording should be recent one", "recent-recording", remainingRecordings[0].id)
    }

    @Test
    fun `isRecording should return false initially`() {
        // When & Then
        assertFalse("Should not be recording initially", offlineRecordingManager.isRecording())
    }

    @Test
    fun `getCurrentRecording should return null initially`() {
        // When & Then
        assertNull("Should have no current recording initially", offlineRecordingManager.getCurrentRecording())
    }

    private fun createTestRecording(
        id: String, 
        timestamp: Long = System.currentTimeMillis()
    ): OfflineRecording {
        val recordingsDir = File(tempDir, "offline_recordings")
        recordingsDir.mkdirs()
        
        return OfflineRecording(
            id = id,
            filePath = File(recordingsDir, "$id.wav").absolutePath,
            timestamp = timestamp,
            duration = 5000, // 5 seconds
            fileSizeBytes = 1024, // 1KB
            audioFormat = "wav",
            sampleRate = 44100,
            channels = 1,
            isProcessed = false,
            encryptionKeyId = "test-key"
        )
    }

    private fun createTestRecordingFiles(recording: OfflineRecording) {
        // Create recording file
        val recordingFile = File(recording.filePath)
        recordingFile.parentFile?.mkdirs()
        recordingFile.writeText("test audio data")

        // Create metadata file
        val metadataFile = File(tempDir, "offline_recordings/${recording.id}.metadata")
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
                "transcription": null,
                "generatedNotes": null,
                "processingError": null,
                "encryptionKeyId": "${recording.encryptionKeyId}"
            }
        """.trimIndent()
        metadataFile.writeText(metadata)
    }
}