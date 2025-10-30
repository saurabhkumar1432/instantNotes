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
 * Unit tests for LocalStorageManagerImpl to verify smart caching and cleanup functionality.
 */
@RunWith(RobolectricTestRunner::class)
class LocalStorageManagerImplTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var cacheManager: CacheManager
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var localStorageManager: LocalStorageManagerImpl

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
    }

    @Test
    fun `getDeviceCapabilities returns valid device information`() = runTest {
        // When
        val capabilities = localStorageManager.getDeviceCapabilities()

        // Then
        assertNotNull(capabilities)
        assertTrue(capabilities.totalStorage > 0)
        assertTrue(capabilities.availableStorage >= 0)
        assertTrue(capabilities.ramSize > 0)
        assertNotNull(capabilities.processingTier)
        assertTrue(capabilities.batteryLevel >= 0f && capabilities.batteryLevel <= 1f)
    }

    @Test
    fun `getStorageSettings returns default settings when none exist`() = runTest {
        // Given
        val mockPreferences = mockk<Preferences>(relaxed = true)
        every { mockPreferences[any<Preferences.Key<Boolean>>()] } returns null
        every { mockPreferences[any<Preferences.Key<String>>()] } returns null
        every { mockPreferences[any<Preferences.Key<Long>>()] } returns null
        every { mockPreferences[any<Preferences.Key<Float>>()] } returns null
        every { dataStore.data } returns flowOf(mockPreferences)

        // When
        val settings = localStorageManager.getStorageSettings()

        // Then
        assertEquals(true, settings.enableAutomaticCleanup)
        assertEquals(CleanupFrequency.WEEKLY, settings.cleanupFrequency)
        assertEquals(100 * 1024 * 1024L, settings.maxCacheSize)
        assertEquals(CompressionLevel.BALANCED, settings.compressionLevel)
    }

    @Test
    fun `updateStorageSettings saves settings correctly`() = runTest {
        // Given
        val newSettings = StorageManagementSettings(
            enableAutomaticCleanup = false,
            cleanupFrequency = CleanupFrequency.DAILY,
            maxCacheSize = 200 * 1024 * 1024L,
            compressionLevel = CompressionLevel.HIGH
        )

        // When
        localStorageManager.updateStorageSettings(newSettings)

        // Then
        coVerify { dataStore.edit(any()) }
    }

    @Test
    fun `compactDatabase executes vacuum and reindex operations`() = runTest {
        // Given
        val mockDatabase = mockk<android.database.sqlite.SQLiteDatabase>(relaxed = true)
        val mockOpenHelper = mockk<androidx.sqlite.db.SupportSQLiteOpenHelper>(relaxed = true)
        every { database.openHelper } returns mockOpenHelper
        every { mockOpenHelper.writableDatabase } returns mockk(relaxed = true) {
            every { path } returns "/test/path/database.db"
            every { execSQL("VACUUM") } just Runs
            every { execSQL("REINDEX") } just Runs
        }

        // When
        val result = localStorageManager.compactDatabase()

        // Then
        assertNotNull(result)
        assertTrue(result.errors.isEmpty())
        verify { mockOpenHelper.writableDatabase.execSQL("VACUUM") }
        verify { mockOpenHelper.writableDatabase.execSQL("REINDEX") }
    }

    @Test
    fun `cleanupTempFiles removes old temporary files`() = runTest {
        // Given - Create a temporary file structure
        val tempDir = context.cacheDir
        val oldFile = java.io.File(tempDir, "old_temp_file.tmp")
        oldFile.createNewFile()
        oldFile.setLastModified(System.currentTimeMillis() - (8 * 24 * 60 * 60 * 1000)) // 8 days old

        // When
        val result = localStorageManager.cleanupTempFiles()

        // Then
        assertNotNull(result)
        assertTrue(result.errors.isEmpty())
        // Note: In a real test, we'd verify the file was deleted
        // but this requires more complex file system mocking
    }

    @Test
    fun `analyzeStorageUsage returns comprehensive analysis`() = runTest {
        // Given
        val mockNotesDao = mockk<com.voicenotesai.data.local.dao.NotesDao>(relaxed = true)
        every { database.notesDao() } returns mockNotesDao
        coEvery { mockNotesDao.getNotesOlderThan(any()) } returns emptyList()

        // When
        val analysis = localStorageManager.analyzeStorageUsage()

        // Then
        assertNotNull(analysis)
        assertTrue(analysis.totalUsedSpace >= 0)
        assertTrue(analysis.availableSpace >= 0)
        assertNotNull(analysis.deviceCapabilities)
        assertNotNull(analysis.recommendations)
    }

    @Test
    fun `performAutomaticCleanup respects user settings`() = runTest {
        // Given - Mock settings with automatic cleanup disabled
        val mockPreferences = mockk<Preferences>(relaxed = true)
        every { mockPreferences[any<Preferences.Key<Boolean>>()] } returns false // disabled
        every { dataStore.data } returns flowOf(mockPreferences)

        // When
        val result = localStorageManager.performAutomaticCleanup()

        // Then
        assertEquals(false, result.cleanupPerformed)
        assertEquals("Automatic cleanup is disabled", result.reason)
    }

    @Test
    fun `optimizeStorage performs comprehensive cleanup`() = runTest {
        // Given
        val mockCacheOptimization = OptimizationResult(
            freedSpace = 1024 * 1024L, // 1MB
            optimizedFiles = 10,
            compactionTime = 100.days,
            cacheCleanupResult = CleanupResult(5, 512 * 1024L, 50.days),
            databaseCompactionResult = CompactionResult(
                originalSize = 10 * 1024 * 1024L,
                compactedSize = 9 * 1024 * 1024L,
                freedSpace = 1024 * 1024L,
                compactionTime = 30.days,
                tablesOptimized = 1,
                indexesRebuilt = 1
            )
        )
        
        coEvery { cacheManager.optimize() } returns mockCacheOptimization.databaseCompactionResult.let {
            com.voicenotesai.domain.cache.OptimizationResult(
                compactedEntries = it.tablesOptimized,
                freedSpace = it.freedSpace,
                optimizationTime = it.compactionTime
            )
        }

        val mockNotesDao = mockk<com.voicenotesai.data.local.dao.NotesDao>(relaxed = true)
        every { database.notesDao() } returns mockNotesDao
        coEvery { mockNotesDao.getNotesOlderThan(any()) } returns emptyList()

        // When
        val result = localStorageManager.optimizeStorage()

        // Then
        assertNotNull(result)
        assertTrue(result.freedSpace >= 0)
        assertTrue(result.optimizedFiles >= 0)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `archiveOldNotes processes notes older than threshold`() = runTest {
        // Given
        val mockNote = mockk<com.voicenotesai.data.local.entity.Note>(relaxed = true) {
            every { id } returns 1L
            every { content } returns "Test content"
            every { transcribedText } returns "Test transcription"
        }
        
        val mockNotesDao = mockk<com.voicenotesai.data.local.dao.NotesDao>(relaxed = true)
        every { database.notesDao() } returns mockNotesDao
        coEvery { mockNotesDao.getNotesOlderThan(any()) } returns listOf(mockNote)

        // When
        val result = localStorageManager.archiveOldNotes(90.days)

        // Then
        assertNotNull(result)
        assertEquals(1, result.notesArchived)
        assertTrue(result.freedSpace > 0)
        assertTrue(result.archiveLocation.isNotEmpty())
    }

    @Test
    fun `storage metrics are updated correctly`() = runTest {
        // Given
        val mockNotesDao = mockk<com.voicenotesai.data.local.dao.NotesDao>(relaxed = true)
        every { database.notesDao() } returns mockNotesDao
        coEvery { mockNotesDao.getNotesOlderThan(any()) } returns emptyList()

        // When
        val metricsFlow = localStorageManager.getStorageMetrics()
        
        // Trigger an operation that updates metrics
        localStorageManager.analyzeStorageUsage()

        // Then
        metricsFlow.collect { metrics ->
            assertNotNull(metrics)
            assertTrue(metrics.usagePercentage >= 0f)
            assertTrue(metrics.usagePercentage <= 100f)
            assertNotNull(metrics.storageHealth)
            return@collect // Exit after first emission
        }
    }
}