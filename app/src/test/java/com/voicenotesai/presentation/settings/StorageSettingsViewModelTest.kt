package com.voicenotesai.presentation.settings

import com.voicenotesai.domain.storage.*
import com.voicenotesai.domain.usecase.StorageManagementUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

/**
 * Unit tests for StorageSettingsViewModel to verify UI state management
 * and interaction with storage management use case.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StorageSettingsViewModelTest {

    private lateinit var storageManagementUseCase: StorageManagementUseCase
    private lateinit var viewModel: StorageSettingsViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        storageManagementUseCase = mockk(relaxed = true)
        
        // Mock default responses
        coEvery { storageManagementUseCase.getStorageSettings() } returns StorageManagementSettings()
        every { storageManagementUseCase.getStorageMetrics() } returns flowOf(
            StorageMetrics(
                usedSpace = 100 * 1024 * 1024L,
                availableSpace = 900 * 1024 * 1024L,
                totalSpace = 1000 * 1024 * 1024L,
                cacheUsage = 50 * 1024 * 1024L,
                databaseUsage = 30 * 1024 * 1024L,
                audioFilesUsage = 20 * 1024 * 1024L,
                usagePercentage = 10f,
                storageHealth = StorageHealth.EXCELLENT,
                lastOptimization = null,
                nextScheduledCleanup = null
            )
        )
        coEvery { storageManagementUseCase.analyzeStorage() } returns StorageAnalysis(
            totalUsedSpace = 100 * 1024 * 1024L,
            availableSpace = 900 * 1024 * 1024L,
            databaseSize = 30 * 1024 * 1024L,
            cacheSize = 50 * 1024 * 1024L,
            audioFilesSize = 20 * 1024 * 1024L,
            tempFilesSize = 0L,
            oldNotesSize = 0L,
            recommendations = emptyList(),
            deviceCapabilities = DeviceCapabilities(
                totalStorage = 1000 * 1024 * 1024L,
                availableStorage = 900 * 1024 * 1024L,
                ramSize = 4 * 1024 * 1024 * 1024L,
                processingTier = ProcessingTier.MID_RANGE,
                batteryLevel = 0.8f,
                isCharging = false,
                thermalState = ThermalState.NORMAL,
                networkSpeed = NetworkSpeed.FAST,
                storageType = StorageType.UFS
            )
        )

        viewModel = StorageSettingsViewModel(storageManagementUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads storage settings correctly`() = runTest {
        // Given - ViewModel is initialized in setup()

        // When - Initial state is loaded
        val uiState = viewModel.uiState.value

        // Then
        assertFalse(uiState.isLoading)
        assertNotNull(uiState.settings)
        assertEquals(StorageManagementSettings(), uiState.settings)
        coVerify { storageManagementUseCase.getStorageSettings() }
    }

    @Test
    fun `storage metrics are observed correctly`() = runTest {
        // Given - ViewModel is initialized in setup()

        // When - Metrics flow is observed
        val metrics = viewModel.storageMetrics.value

        // Then
        assertNotNull(metrics)
        assertEquals(10f, metrics.usagePercentage)
        assertEquals(StorageHealth.EXCELLENT, metrics.storageHealth)
        verify { storageManagementUseCase.getStorageMetrics() }
    }

    @Test
    fun `updateAutomaticCleanup updates settings correctly`() = runTest {
        // Given
        val newValue = false

        // When
        viewModel.updateAutomaticCleanup(newValue)

        // Then
        coVerify { 
            storageManagementUseCase.updateStorageSettings(
                match { it.enableAutomaticCleanup == newValue }
            )
        }
    }

    @Test
    fun `updateCleanupFrequency updates settings correctly`() = runTest {
        // Given
        val newFrequency = CleanupFrequency.DAILY

        // When
        viewModel.updateCleanupFrequency(newFrequency)

        // Then
        coVerify { 
            storageManagementUseCase.updateStorageSettings(
                match { it.cleanupFrequency == newFrequency }
            )
        }
    }

    @Test
    fun `updateMaxCacheSize converts MB to bytes correctly`() = runTest {
        // Given
        val cacheSizeMB = 150
        val expectedBytes = 150L * 1024 * 1024

        // When
        viewModel.updateMaxCacheSize(cacheSizeMB)

        // Then
        coVerify { 
            storageManagementUseCase.updateStorageSettings(
                match { it.maxCacheSize == expectedBytes }
            )
        }
    }

    @Test
    fun `updateMaxAudioRetention converts days to duration correctly`() = runTest {
        // Given
        val retentionDays = 45
        val expectedDuration = 45.days

        // When
        viewModel.updateMaxAudioRetention(retentionDays)

        // Then
        coVerify { 
            storageManagementUseCase.updateStorageSettings(
                match { it.maxAudioRetention == expectedDuration }
            )
        }
    }

    @Test
    fun `updateLowStorageThreshold converts percentage correctly`() = runTest {
        // Given
        val thresholdPercentage = 75f
        val expectedRatio = 0.75f

        // When
        viewModel.updateLowStorageThreshold(thresholdPercentage)

        // Then
        coVerify { 
            storageManagementUseCase.updateStorageSettings(
                match { it.lowStorageThreshold == expectedRatio }
            )
        }
    }

    @Test
    fun `optimizeStorage triggers optimization and updates state`() = runTest {
        // Given
        val optimizationResult = OptimizationResult(
            freedSpace = 50 * 1024 * 1024L,
            optimizedFiles = 25,
            compactionTime = 5.days,
            cacheCleanupResult = CleanupResult(10, 20 * 1024 * 1024L, 2.days),
            databaseCompactionResult = CompactionResult(
                originalSize = 100 * 1024 * 1024L,
                compactedSize = 80 * 1024 * 1024L,
                freedSpace = 20 * 1024 * 1024L,
                compactionTime = 3.days,
                tablesOptimized = 5,
                indexesRebuilt = 3
            )
        )
        coEvery { storageManagementUseCase.optimizeStorage() } returns optimizationResult

        // When
        viewModel.optimizeStorage()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isOptimizing)
        assertEquals(optimizationResult, uiState.lastOptimizationResult)
        coVerify { storageManagementUseCase.optimizeStorage() }
        coVerify { storageManagementUseCase.analyzeStorage() } // Should refresh analysis
    }

    @Test
    fun `performAutomaticCleanup triggers cleanup and updates state`() = runTest {
        // Given
        val cleanupResult = AutoCleanupResult(
            cleanupPerformed = true,
            reason = "Storage threshold exceeded",
            optimizationResult = OptimizationResult(
                freedSpace = 30 * 1024 * 1024L,
                optimizedFiles = 15,
                compactionTime = 3.days,
                cacheCleanupResult = CleanupResult(8, 15 * 1024 * 1024L, 1.days),
                databaseCompactionResult = CompactionResult(
                    originalSize = 80 * 1024 * 1024L,
                    compactedSize = 65 * 1024 * 1024L,
                    freedSpace = 15 * 1024 * 1024L,
                    compactionTime = 2.days,
                    tablesOptimized = 3,
                    indexesRebuilt = 2
                )
            ),
            nextScheduledCleanup = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
        )
        coEvery { storageManagementUseCase.performAutomaticCleanup() } returns cleanupResult

        // When
        viewModel.performAutomaticCleanup()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isOptimizing)
        assertEquals(cleanupResult, uiState.lastAutomaticCleanupResult)
        coVerify { storageManagementUseCase.performAutomaticCleanup() }
    }

    @Test
    fun `cleanupTempFiles triggers cleanup and refreshes analysis`() = runTest {
        // When
        viewModel.cleanupTempFiles()

        // Then
        coVerify { storageManagementUseCase.cleanupTempFiles() }
        coVerify { storageManagementUseCase.analyzeStorage() }
    }

    @Test
    fun `archiveOldNotes converts days and triggers archiving`() = runTest {
        // Given
        val olderThanDays = 120

        // When
        viewModel.archiveOldNotes(olderThanDays)

        // Then
        coVerify { storageManagementUseCase.archiveOldNotes(120.days) }
        coVerify { storageManagementUseCase.analyzeStorage() }
    }

    @Test
    fun `compactDatabase triggers compaction and updates state`() = runTest {
        // When
        viewModel.compactDatabase()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isOptimizing)
        coVerify { storageManagementUseCase.compactDatabase() }
        coVerify { storageManagementUseCase.analyzeStorage() }
    }

    @Test
    fun `error handling works correctly for failed operations`() = runTest {
        // Given
        val errorMessage = "Storage optimization failed"
        coEvery { storageManagementUseCase.optimizeStorage() } throws RuntimeException(errorMessage)

        // When
        viewModel.optimizeStorage()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isOptimizing)
        assertTrue(uiState.error?.contains("Storage optimization failed") == true)
    }

    @Test
    fun `clearError resets error state`() = runTest {
        // Given - Set an error state first
        coEvery { storageManagementUseCase.optimizeStorage() } throws RuntimeException("Test error")
        viewModel.optimizeStorage()
        assertTrue(viewModel.uiState.value.error != null)

        // When
        viewModel.clearError()

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(null, uiState.error)
    }

    @Test
    fun `clearOptimizationResult resets optimization result`() = runTest {
        // Given - Set an optimization result first
        val optimizationResult = OptimizationResult(
            freedSpace = 1024L,
            optimizedFiles = 1,
            compactionTime = 1.days,
            cacheCleanupResult = CleanupResult(1, 512L, 1.days),
            databaseCompactionResult = CompactionResult(1024L, 512L, 512L, 1.days, 1, 1)
        )
        coEvery { storageManagementUseCase.optimizeStorage() } returns optimizationResult
        viewModel.optimizeStorage()
        assertTrue(viewModel.uiState.value.lastOptimizationResult != null)

        // When
        viewModel.clearOptimizationResult()

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(null, uiState.lastOptimizationResult)
    }

    @Test
    fun `multiple setting updates work correctly`() = runTest {
        // When - Update multiple settings
        viewModel.updateAutomaticCleanup(false)
        viewModel.updateCleanupFrequency(CleanupFrequency.MONTHLY)
        viewModel.updateCompressionLevel(CompressionLevel.HIGH)
        viewModel.updateBatteryOptimization(false)

        // Then - All updates should be called
        coVerify(exactly = 4) { storageManagementUseCase.updateStorageSettings(any()) }
    }
}