package com.voicenotesai.domain.usecase

import com.voicenotesai.domain.storage.LocalStorageManager
import com.voicenotesai.domain.storage.StorageManagementSettings
import com.voicenotesai.domain.storage.StorageMetrics
import com.voicenotesai.domain.storage.StorageAnalysis
import com.voicenotesai.domain.storage.OptimizationResult
import com.voicenotesai.domain.storage.AutoCleanupResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Use case for managing storage operations including cleanup, optimization,
 * and user-configurable storage settings.
 */
class StorageManagementUseCase @Inject constructor(
    private val localStorageManager: LocalStorageManager
) {
    /**
     * Get real-time storage metrics
     */
    fun getStorageMetrics(): Flow<StorageMetrics> {
        return localStorageManager.getStorageMetrics()
    }

    /**
     * Analyze current storage usage and get recommendations
     */
    suspend fun analyzeStorage(): StorageAnalysis {
        return localStorageManager.analyzeStorageUsage()
    }

    /**
     * Perform manual storage optimization
     */
    suspend fun optimizeStorage(): OptimizationResult {
        return localStorageManager.optimizeStorage()
    }

    /**
     * Perform automatic cleanup based on settings and device state
     */
    suspend fun performAutomaticCleanup(): AutoCleanupResult {
        return localStorageManager.performAutomaticCleanup()
    }

    /**
     * Clean up temporary files
     */
    suspend fun cleanupTempFiles() {
        localStorageManager.cleanupTempFiles()
    }

    /**
     * Archive old notes based on age threshold
     */
    suspend fun archiveOldNotes(olderThan: Duration) {
        localStorageManager.archiveOldNotes(olderThan)
    }

    /**
     * Compact database to improve performance and free space
     */
    suspend fun compactDatabase() {
        localStorageManager.compactDatabase()
    }

    /**
     * Get current storage management settings
     */
    suspend fun getStorageSettings(): StorageManagementSettings {
        return localStorageManager.getStorageSettings()
    }

    /**
     * Update storage management settings
     */
    suspend fun updateStorageSettings(settings: StorageManagementSettings) {
        localStorageManager.updateStorageSettings(settings)
    }

    /**
     * Get device capabilities for adaptive storage management
     */
    suspend fun getDeviceCapabilities() {
        localStorageManager.getDeviceCapabilities()
    }
}