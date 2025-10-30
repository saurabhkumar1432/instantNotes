package com.voicenotesai.data.local

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.voicenotesai.data.local.dao.DatabaseStats
import com.voicenotesai.data.local.dao.NotesDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.io.File

class DatabaseOptimizerTest {

    private lateinit var notesDao: NotesDao
    private lateinit var database: AppDatabase
    private lateinit var context: Context
    private lateinit var databaseOptimizer: DatabaseOptimizer
    private lateinit var mockDbFile: File

    @Before
    fun setup() {
        notesDao = mockk()
        database = mockk()
        context = mockk()
        mockDbFile = mockk()
        databaseOptimizer = DatabaseOptimizer(notesDao, database, context)
    }

    @Test
    fun `optimizeDatabase performs all optimization steps successfully`() = runTest {
        // Given
        val initialStats = DatabaseStats(
            totalNotes = 1000,
            activeNotes = 800,
            archivedNotes = 200,
            avgContentLength = 150.0,
            latestTimestamp = System.currentTimeMillis(),
            oldestTimestamp = System.currentTimeMillis() - (400L * 24 * 60 * 60 * 1000) // 400 days ago
        )
        val finalStats = initialStats.copy(
            totalNotes = 950,
            archivedNotes = 150
        )

        val mockOpenHelper = mockk<SupportSQLiteOpenHelper>()
        val mockSQLiteDb = mockk<SupportSQLiteDatabase>()
        
        every { database.openHelper } returns mockOpenHelper
        every { mockOpenHelper.writableDatabase } returns mockSQLiteDb
        every { mockSQLiteDb.execSQL("VACUUM") } returns Unit
        every { mockSQLiteDb.execSQL("ANALYZE") } returns Unit
        
        coEvery { notesDao.getDatabaseStats() } returnsMany listOf(initialStats, finalStats)
        coEvery { notesDao.deleteArchivedNotesOlderThan(any()) } returns 50

        // When
        val result = databaseOptimizer.optimizeDatabase()

        // Then
        assertTrue("Result should be Success", result is OptimizationResult.Success)
        val successResult = result as OptimizationResult.Success
        assertEquals(50, successResult.deletedArchivedNotes)
        assertEquals(initialStats, successResult.initialStats)
        assertEquals(finalStats, successResult.finalStats)
        assertTrue(successResult.duration > 0)

        coVerify { notesDao.deleteArchivedNotesOlderThan(any()) }
        coVerify { mockSQLiteDb.execSQL("VACUUM") }
        coVerify { mockSQLiteDb.execSQL("ANALYZE") }
    }

    @Test
    fun `optimizeDatabase handles errors gracefully`() = runTest {
        // Given
        coEvery { notesDao.getDatabaseStats() } throws RuntimeException("Database error")

        // When
        val result = databaseOptimizer.optimizeDatabase()

        // Then
        assertTrue("Result should be Error", result is OptimizationResult.Error)
        val errorResult = result as OptimizationResult.Error
        assertEquals("Database error", errorResult.error)
        assertTrue(errorResult.duration > 0)
    }

    @Test
    fun `performQuickMaintenance executes analyze successfully`() = runTest {
        // Given
        val stats = DatabaseStats(
            totalNotes = 500,
            activeNotes = 450,
            archivedNotes = 50,
            avgContentLength = 120.0,
            latestTimestamp = System.currentTimeMillis(),
            oldestTimestamp = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        )

        val mockOpenHelper = mockk<SupportSQLiteOpenHelper>()
        val mockSQLiteDb = mockk<SupportSQLiteDatabase>()
        
        every { database.openHelper } returns mockOpenHelper
        every { mockOpenHelper.writableDatabase } returns mockSQLiteDb
        every { mockSQLiteDb.execSQL("ANALYZE") } returns Unit
        
        coEvery { notesDao.getDatabaseStats() } returns stats

        // When
        val result = databaseOptimizer.performQuickMaintenance()

        // Then
        assertTrue("Result should be Success", result is MaintenanceResult.Success)
        val successResult = result as MaintenanceResult.Success
        assertEquals(stats, successResult.stats)
        assertTrue(successResult.duration >= 0)

        coVerify { mockSQLiteDb.execSQL("ANALYZE") }
    }

    @Test
    fun `checkOptimizationNeeded recommends optimization for high archived ratio`() = runTest {
        // Given
        val stats = DatabaseStats(
            totalNotes = 1000,
            activeNotes = 600,
            archivedNotes = 400, // 40% archived
            avgContentLength = 150.0,
            latestTimestamp = System.currentTimeMillis(),
            oldestTimestamp = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        )

        every { context.getDatabasePath("voice_notes_database") } returns mockDbFile
        every { mockDbFile.exists() } returns true
        every { mockDbFile.length() } returns 50 * 1024 * 1024L // 50MB

        coEvery { notesDao.getDatabaseStats() } returns stats

        // When
        val recommendation = databaseOptimizer.checkOptimizationNeeded()

        // Then
        assertTrue(recommendation.isOptimizationNeeded)
        assertEquals(OptimizationPriority.MEDIUM, recommendation.priority)
        assertTrue(recommendation.recommendations.any { it.contains("archived notes") })
    }

    @Test
    fun `checkOptimizationNeeded recommends optimization for large dataset`() = runTest {
        // Given
        val stats = DatabaseStats(
            totalNotes = 15000, // Large dataset
            activeNotes = 14000,
            archivedNotes = 1000,
            avgContentLength = 150.0,
            latestTimestamp = System.currentTimeMillis(),
            oldestTimestamp = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        )

        every { context.getDatabasePath("voice_notes_database") } returns mockDbFile
        every { mockDbFile.exists() } returns true
        every { mockDbFile.length() } returns 200 * 1024 * 1024L // 200MB

        coEvery { notesDao.getDatabaseStats() } returns stats

        // When
        val recommendation = databaseOptimizer.checkOptimizationNeeded()

        // Then
        assertTrue(recommendation.isOptimizationNeeded)
        assertEquals(OptimizationPriority.HIGH, recommendation.priority)
        assertTrue(recommendation.recommendations.any { it.contains("Large dataset") })
        assertTrue(recommendation.recommendations.any { it.contains("Database size is large") })
    }

    @Test
    fun `checkOptimizationNeeded returns no optimization needed for healthy database`() = runTest {
        // Given
        val stats = DatabaseStats(
            totalNotes = 500,
            activeNotes = 450,
            archivedNotes = 50, // 10% archived
            avgContentLength = 150.0,
            latestTimestamp = System.currentTimeMillis(),
            oldestTimestamp = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        )

        every { context.getDatabasePath("voice_notes_database") } returns mockDbFile
        every { mockDbFile.exists() } returns true
        every { mockDbFile.length() } returns 10 * 1024 * 1024L // 10MB

        coEvery { notesDao.getDatabaseStats() } returns stats

        // When
        val recommendation = databaseOptimizer.checkOptimizationNeeded()

        // Then
        assertTrue(recommendation.recommendations.isEmpty())
        assertEquals(OptimizationPriority.LOW, recommendation.priority)
    }

    @Test
    fun `getPerformanceMetrics calculates correct performance estimates`() = runTest {
        // Given
        val stats = DatabaseStats(
            totalNotes = 8000,
            activeNotes = 7500,
            archivedNotes = 500,
            avgContentLength = 200.0,
            latestTimestamp = System.currentTimeMillis(),
            oldestTimestamp = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)
        )

        every { context.getDatabasePath("voice_notes_database") } returns mockDbFile
        every { mockDbFile.exists() } returns true
        every { mockDbFile.length() } returns 80 * 1024 * 1024L // 80MB

        coEvery { notesDao.getDatabaseStats() } returns stats

        // When
        val metrics = databaseOptimizer.getPerformanceMetrics()

        // Then
        assertEquals(8000, metrics.totalNotes)
        assertEquals(7500, metrics.activeNotes)
        assertEquals(500, metrics.archivedNotes)
        assertEquals(80 * 1024 * 1024L, metrics.databaseSizeBytes)
        assertEquals(200.0, metrics.avgContentLength)
        assertEquals(QueryPerformance.FAIR, metrics.estimatedSearchPerformance) // 8k notes = FAIR
        assertEquals(IndexEfficiency.GOOD, metrics.indexEfficiency)
    }
}