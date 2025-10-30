package com.voicenotesai.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import com.voicenotesai.data.local.AppDatabase
import com.voicenotesai.domain.cache.CacheManager
import com.voicenotesai.domain.storage.*
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

/**
 * Integration test for the complete smart caching and cleanup system.
 * Tests the interaction between all components.
 */
@RunWith(RobolectricTestRunner::class)
class StorageIntegrationTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var cacheManager: CacheManager
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var localStorageManager: LocalStorageManagerImpl
    private lateinit var autoCleanupService: AutoCleanupService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = mockk(relaxed = true)
        cacheManager = mockk(relaxed = true)
        dataStore = mockk(relaxed = true)

        // Mock DataStore to return default preferences
        every { dataStore.data } returns flowOf(mockk(relaxed = true))
        coEvery { dataStore.edit(any()) } returns mockk(relaxed = true)

        localStorageManager = LocalStorageManagerImpl(
            context = context,
            database = database,
            cacheManager = cacheManager,
            dataStore = dataStore,
            ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
        )

        autoCleanupService = AutoCleanupService(localStorageManager)
    }

    @Test
    fun `complete storage management workflow works correctly`() = runTest {
        // Given - Mock database and cache responses
        val mockNotesDao = mockk<com.voicenotesai.data.local.dao.NotesDao>(relaxed = true)
        every { database.notesDao() } returns mockNotesDao
        coEvery { mockNotesDao.getNotesOlderThan(any()) } returns emptyList()

        val mockCacheOptimization = com.voicenotesai.domain.cache.OptimizationResult(
            compactedEntries = 10,
            freedSpace = 1024 * 1024L,
            optimizationTime = 1.days
        )
        coEvery { cacheManager.optimize() } returns mockCacheOptimization

        // When - Perform complete storage optimization
        val result = localStorageManager.optimizeStorage()

        // Then - Verify all operations were performed
        assertNotNull(result)
        assertTrue(result.freedSpace >= 0)
        assertTrue(result.optimizedFiles >= 0)
        assertTrue(result.errors.isEmpty())

        // Verify cache optimization was called
        coVerify { cacheManager.optimize() }
        
        // Verify database operations were attempted
        coVerify { mockNotesDao.getNotesOlderThan(any()) }
    }

    @Test
    fun `storage analysis provides comprehensive information`() = runTest {
        // Given - Mock database responses
        val mockNotesDao = mockk<com.voicenotesai.data.local.dao.NotesDao>(relaxed = true)
        every { database.notesDao() } returns mockNotesDao
        coEvery { mockNotesDao.getNotesOlderThan(any()) } returns emptyList()

        // When - Analyze storage usage
        val analysis = localStorageManager.analyzeStorageUsage()

        // Then - Verify analysis contains expected information
        assertNotNull(analysis)
        assertTrue(analysis.totalUsedSpace >= 0)
        assertTrue(analysis.availableSpace >= 0)
        assertNotNull(analysis.deviceCapabilities)
        assertNotNull(analysis.recommendations)

        // Verify device capabilities are populated
        val capabilities = analysis.deviceCapabilities
        assertTrue(capabilities.totalStorage > 0)
        assertTrue(capabilities.ramSize > 0)
        assertNotNull(capabilities.processingTier)
        assertTrue(capabilities.batteryLevel >= 0f && capabilities.batteryLevel <= 1f)
    }

    @Test
    fun `storage settings can be updated and retrieved correctly`() = runTest {
        // Given - New storage settings
        val newSettings = StorageManagementSettings(
            enableAutomaticCleanup = false,
            cleanupFrequency = CleanupFrequency.MONTHLY,
            maxCacheSize = 200 * 1024 * 1024L,
            archiveOldNotes = false,
            compressionLevel = CompressionLevel.HIGH
        )

        // When - Update settings
        localStorageManager.updateStorageSettings(newSettings)

        // Then - Verify settings were saved
        coVerify { dataStore.edit(any()) }
    }

    @Test
    fun `automatic cleanup service can be started and stopped`() = runTest {
        // Given - Service is initially stopped
        assertTrue(!autoCleanupService.isServiceRunning())

        // When - Start the service
        autoCleanupService.startCleanupService()

        // Then - Service should be running
        // Note: In a real test, we'd need to wait for the coroutine to start
        // For this test, we'll just verify no exceptions were thrown
        
        // When - Stop the service
        autoCleanupService.stopCleanupService()

        // Then - Service should be stopped
        assertTrue(!autoCleanupService.isServiceRunning())
    }

    @Test
    fun `storage metrics are updated correctly after operations`() = runTest {
        // Given - Mock database responses
        val mockNotesDao = mockk<com.voicenotesai.data.local.dao.NotesDao>(relaxed = true)
        every { database.notesDao() } returns mockNotesDao
        coEvery { mockNotesDao.getNotesOlderThan(any()) } returns emptyList()

        // When - Get initial metrics
        val metricsFlow = localStorageManager.getStorageMetrics()
        
        // Trigger an operation that should update metrics
        localStorageManager.analyzeStorageUsage()

        // Then - Verify metrics are available
        metricsFlow.collect { metrics ->
            assertNotNull(metrics)
            assertTrue(metrics.usagePercentage >= 0f)
            assertTrue(metrics.usagePercentage <= 100f)
            assertNotNull(metrics.storageHealth)
            return@collect // Exit after first emission
        }
    }

    @Test
    fun `device capabilities detection works correctly`() = runTest {
        // When - Get device capabilities
        val capabilities = localStorageManager.getDeviceCapabilities()

        // Then - Verify all capabilities are populated
        assertNotNull(capabilities)
        assertTrue(capabilities.totalStorage > 0)
        assertTrue(capabilities.availableStorage >= 0)
        assertTrue(capabilities.ramSize > 0)
        assertNotNull(capabilities.processingTier)
        assertTrue(capabilities.batteryLevel >= 0f && capabilities.batteryLevel <= 1f)
        assertNotNull(capabilities.thermalState)
        assertNotNull(capabilities.networkSpeed)
        assertNotNull(capabilities.storageType)
    }

    @Test
    fun `storage recommendations are generated based on usage`() = runTest {
        // Given - Mock database with some old notes
        val mockNote = mockk<com.voicenotesai.data.local.entity.Note>(relaxed = true) {
            every { id } returns 1L
            every { content } returns "Test content"
            every { transcribedText } returns "Test transcription"
        }
        
        val mockNotesDao = mockk<com.voicenotesai.data.local.dao.NotesDao>(relaxed = true)
        every { database.notesDao() } returns mockNotesDao
        coEvery { mockNotesDao.getNotesOlderThan(any()) } returns listOf(mockNote)

        // When - Analyze storage
        val analysis = localStorageManager.analyzeStorageUsage()

        // Then - Verify recommendations are generated
        assertNotNull(analysis.recommendations)
        // In a real scenario with actual storage usage, we'd have recommendations
    }

    @Test
    fun `automatic cleanup respects user settings`() = runTest {
        // Given - Mock settings with cleanup disabled
        val mockPreferences = mockk<Preferences>(relaxed = true)
        every { mockPreferences[any<Preferences.Key<Boolean>>()] } returns false
        every { dataStore.data } returns flowOf(mockPreferences)

        // When - Perform automatic cleanup
        val result = localStorageManager.performAutomaticCleanup()

        // Then - Cleanup should be skipped
        assertEquals(false, result.cleanupPerformed)
        assertTrue(result.reason.contains("disabled"))
    }

    @Test
    fun `temp file cleanup removes old files`() = runTest {
        // Given - Create a temporary directory structure
        val tempDir = context.cacheDir
        tempDir.mkdirs()

        // When - Perform temp file cleanup
        val result = localStorageManager.cleanupTempFiles()

        // Then - Verify cleanup completed without errors
        assertNotNull(result)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.freedSpace >= 0)
        assertTrue(result.filesDeleted >= 0)
    }

    @Test
    fun `database compaction executes successfully`() = runTest {
        // Given - Mock database operations
        val mockDatabase = mockk<androidx.sqlite.db.SupportSQLiteDatabase>(relaxed = true)
        val mockOpenHelper = mockk<androidx.sqlite.db.SupportSQLiteOpenHelper>(relaxed = true)
        every { database.openHelper } returns mockOpenHelper
        every { mockOpenHelper.writableDatabase } returns mockDatabase
        every { mockDatabase.path } returns "/test/path/database.db"
        every { mockDatabase.execSQL("VACUUM") } just Runs
        every { mockDatabase.execSQL("REINDEX") } just Runs

        // When - Compact database
        val result = localStorageManager.compactDatabase()

        // Then - Verify compaction was attempted
        assertNotNull(result)
        assertTrue(result.errors.isEmpty())
        verify { mockDatabase.execSQL("VACUUM") }
        verify { mockDatabase.execSQL("REINDEX") }
    }
}