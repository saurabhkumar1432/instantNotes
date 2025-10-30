package com.voicenotesai.data.storage

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import com.voicenotesai.domain.storage.LocalStorageManager
import com.voicenotesai.domain.storage.CleanupFrequency
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.days

/**
 * Simple background service for automatic storage cleanup using coroutines.
 * Alternative to WorkManager for periodic cleanup operations.
 */
@Singleton
class AutoCleanupService @Inject constructor(
    private val localStorageManager: LocalStorageManager
) {
    private var cleanupJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Start the automatic cleanup service based on user settings.
     */
    suspend fun startCleanupService() {
        stopCleanupService() // Stop any existing service
        
        val settings = localStorageManager.getStorageSettings()
        
        if (!settings.enableAutomaticCleanup || settings.cleanupFrequency == CleanupFrequency.MANUAL) {
            return
        }

        val interval = when (settings.cleanupFrequency) {
            CleanupFrequency.DAILY -> 24.hours
            CleanupFrequency.WEEKLY -> (7 * 24).hours
            CleanupFrequency.MONTHLY -> (30 * 24).hours
            CleanupFrequency.MANUAL -> return
        }

        cleanupJob = serviceScope.launch {
            while (isActive) {
                try {
                    // Wait for the interval
                    delay(interval)
                    
                    // Perform automatic cleanup
                    val result = localStorageManager.performAutomaticCleanup()
                    
                    // Log result for debugging
                    if (result.cleanupPerformed) {
                        println("Automatic cleanup completed: ${result.reason}")
                        result.optimizationResult?.let { optimization ->
                            println("Freed ${optimization.freedSpace / (1024 * 1024)}MB of space")
                        }
                    } else {
                        println("Automatic cleanup skipped: ${result.reason}")
                    }
                    
                } catch (e: CancellationException) {
                    // Service was cancelled, exit gracefully
                    break
                } catch (e: Exception) {
                    // Log error but continue service
                    println("Automatic cleanup failed: ${e.message}")
                    
                    // Wait a bit before retrying to avoid rapid failures
                    delay(1.hours)
                }
            }
        }
    }

    /**
     * Stop the automatic cleanup service.
     */
    fun stopCleanupService() {
        cleanupJob?.cancel()
        cleanupJob = null
    }

    /**
     * Trigger immediate cleanup operation.
     */
    suspend fun triggerImmediateCleanup() {
        serviceScope.launch {
            try {
                val result = localStorageManager.performAutomaticCleanup()
                println("Immediate cleanup result: ${result.reason}")
            } catch (e: Exception) {
                println("Immediate cleanup failed: ${e.message}")
            }
        }
    }

    /**
     * Update the cleanup service when settings change.
     */
    suspend fun onSettingsChanged() {
        startCleanupService() // This will restart with new settings
    }

    /**
     * Check if the cleanup service is currently running.
     */
    fun isServiceRunning(): Boolean {
        return cleanupJob?.isActive == true
    }

    /**
     * Cleanup resources when the service is no longer needed.
     */
    fun cleanup() {
        stopCleanupService()
        serviceScope.cancel()
    }
}

/**
 * Manager for integrating automatic cleanup into the application lifecycle.
 */
@Singleton
class StorageCleanupManager @Inject constructor(
    private val autoCleanupService: AutoCleanupService,
    private val localStorageManager: LocalStorageManager
) {
    
    /**
     * Initialize storage cleanup when the app starts.
     */
    suspend fun initialize() {
        // Start the automatic cleanup service
        autoCleanupService.startCleanupService()
        
        // Perform initial storage analysis
        try {
            val analysis = localStorageManager.analyzeStorageUsage()
            println("Initial storage analysis:")
            println("  Total used: ${analysis.totalUsedSpace / (1024 * 1024)}MB")
            println("  Available: ${analysis.availableSpace / (1024 * 1024)}MB")
            println("  Recommendations: ${analysis.recommendations.size}")
        } catch (e: Exception) {
            println("Initial storage analysis failed: ${e.message}")
        }
    }
    
    /**
     * Handle app going to background - good time for light cleanup.
     */
    suspend fun onAppBackground() {
        try {
            // Perform light cleanup when app goes to background
            localStorageManager.cleanupTempFiles()
        } catch (e: Exception) {
            println("Background cleanup failed: ${e.message}")
        }
    }
    
    /**
     * Handle low memory warnings from the system.
     */
    suspend fun onLowMemory() {
        try {
            // Trigger immediate cleanup for low memory situations
            autoCleanupService.triggerImmediateCleanup()
        } catch (e: Exception) {
            println("Low memory cleanup failed: ${e.message}")
        }
    }
    
    /**
     * Update cleanup service when settings change.
     */
    suspend fun onSettingsChanged() {
        autoCleanupService.onSettingsChanged()
    }
    
    /**
     * Cleanup resources when the app is destroyed.
     */
    fun onAppDestroy() {
        autoCleanupService.cleanup()
    }
}