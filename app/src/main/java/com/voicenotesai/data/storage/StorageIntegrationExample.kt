package com.voicenotesai.data.storage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicenotesai.domain.storage.LocalStorageManager
import com.voicenotesai.domain.storage.StorageManagementSettings
import com.voicenotesai.domain.storage.CleanupFrequency
import com.voicenotesai.domain.storage.CompressionLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

/**
 * Example integration showing how to use the smart caching and cleanup system
 * in a real application context.
 */
@HiltViewModel
class StorageIntegrationExampleViewModel @Inject constructor(
    private val localStorageManager: LocalStorageManager
) : ViewModel() {

    /**
     * Initialize storage management with default settings for new users.
     */
    fun initializeStorageManagement() {
        viewModelScope.launch {
            // Check if settings already exist
            val currentSettings = localStorageManager.getStorageSettings()
            
            // Set up default settings optimized for the device
            val deviceCapabilities = localStorageManager.getDeviceCapabilities()
            val optimizedSettings = optimizeSettingsForDevice(currentSettings, deviceCapabilities)
            
            localStorageManager.updateStorageSettings(optimizedSettings)
        }
    }

    /**
     * Perform startup optimization if enabled in settings.
     */
    fun performStartupOptimization() {
        viewModelScope.launch {
            val settings = localStorageManager.getStorageSettings()
            
            if (settings.optimizeDatabaseOnStartup) {
                try {
                    localStorageManager.compactDatabase()
                } catch (e: Exception) {
                    // Log error but don't block app startup
                    println("Startup database optimization failed: ${e.message}")
                }
            }
        }
    }

    /**
     * Monitor storage health and trigger cleanup when needed.
     */
    fun monitorStorageHealth() {
        viewModelScope.launch {
            localStorageManager.getStorageMetrics().collect { metrics ->
                // Check if storage is getting full
                if (metrics.usagePercentage > 85f) {
                    // Trigger automatic cleanup
                    localStorageManager.performAutomaticCleanup()
                }
                
                // Check if cache is too large
                if (metrics.cacheUsage > 200 * 1024 * 1024) { // 200MB
                    // Optimize cache
                    // This would typically be handled by the CacheManager
                    println("Cache size is large: ${metrics.cacheUsage / (1024 * 1024)}MB")
                }
            }
        }
    }

    /**
     * Handle low memory situations by performing emergency cleanup.
     */
    fun handleLowMemoryWarning() {
        viewModelScope.launch {
            try {
                // Perform immediate cleanup focusing on cache and temp files
                localStorageManager.cleanupTempFiles()
                
                // Get current analysis to see what else can be cleaned
                val analysis = localStorageManager.analyzeStorageUsage()
                
                // Apply high-priority recommendations
                analysis.recommendations
                    .filter { it.priority == com.voicenotesai.domain.storage.StorageRecommendationPriority.HIGH }
                    .forEach { recommendation ->
                        when (recommendation.action) {
                            is com.voicenotesai.domain.storage.RecommendationAction.AutomaticCleanup -> {
                                localStorageManager.optimizeStorage()
                            }
                            is com.voicenotesai.domain.storage.RecommendationAction.CompactDatabase -> {
                                localStorageManager.compactDatabase()
                            }
                            else -> { /* Handle other actions */ }
                        }
                    }
            } catch (e: Exception) {
                println("Emergency cleanup failed: ${e.message}")
            }
        }
    }

    /**
     * Optimize settings based on device capabilities.
     */
    private suspend fun optimizeSettingsForDevice(
        currentSettings: StorageManagementSettings,
        deviceCapabilities: com.voicenotesai.domain.storage.DeviceCapabilities
    ): StorageManagementSettings {
        return when (deviceCapabilities.processingTier) {
            com.voicenotesai.domain.storage.ProcessingTier.LOW_END -> {
                // Conservative settings for low-end devices
                currentSettings.copy(
                    enableAutomaticCleanup = true,
                    cleanupFrequency = CleanupFrequency.DAILY,
                    maxCacheSize = 50 * 1024 * 1024L, // 50MB
                    maxAudioRetention = 14.days, // 2 weeks
                    archiveOldNotes = true,
                    archiveThreshold = 60.days, // 2 months
                    enableLowStorageMode = true,
                    lowStorageThreshold = 0.8f, // 80%
                    enableBatteryOptimization = true,
                    enableThermalOptimization = true,
                    compressionLevel = CompressionLevel.HIGH,
                    optimizeDatabaseOnStartup = false, // Skip to improve startup time
                    maxTempFileAge = 3.days
                )
            }
            
            com.voicenotesai.domain.storage.ProcessingTier.MID_RANGE -> {
                // Balanced settings for mid-range devices
                currentSettings.copy(
                    enableAutomaticCleanup = true,
                    cleanupFrequency = CleanupFrequency.WEEKLY,
                    maxCacheSize = 100 * 1024 * 1024L, // 100MB
                    maxAudioRetention = 30.days, // 1 month
                    archiveOldNotes = true,
                    archiveThreshold = 90.days, // 3 months
                    enableLowStorageMode = true,
                    lowStorageThreshold = 0.85f, // 85%
                    enableBatteryOptimization = true,
                    enableThermalOptimization = true,
                    compressionLevel = CompressionLevel.BALANCED,
                    optimizeDatabaseOnStartup = true,
                    maxTempFileAge = 7.days
                )
            }
            
            com.voicenotesai.domain.storage.ProcessingTier.HIGH_END,
            com.voicenotesai.domain.storage.ProcessingTier.FLAGSHIP -> {
                // Aggressive settings for high-end devices
                currentSettings.copy(
                    enableAutomaticCleanup = true,
                    cleanupFrequency = CleanupFrequency.WEEKLY,
                    maxCacheSize = 200 * 1024 * 1024L, // 200MB
                    maxAudioRetention = 60.days, // 2 months
                    archiveOldNotes = true,
                    archiveThreshold = 120.days, // 4 months
                    enableLowStorageMode = true,
                    lowStorageThreshold = 0.9f, // 90%
                    enableBatteryOptimization = false, // Less conservative
                    enableThermalOptimization = true,
                    compressionLevel = CompressionLevel.BALANCED,
                    optimizeDatabaseOnStartup = true,
                    maxTempFileAge = 14.days
                )
            }
        }
    }
}

/**
 * Application-level integration for storage management.
 */
class StorageManagementIntegration(
    private val context: Context,
    private val localStorageManager: LocalStorageManager
) {
    
    /**
     * Initialize storage management when the app starts.
     */
    suspend fun initialize() {
        // Set up automatic cleanup service
        val cleanupManager = StorageCleanupManager(AutoCleanupService(localStorageManager), localStorageManager)
        cleanupManager.initialize()
        
        // Perform initial storage analysis
        val analysis = localStorageManager.analyzeStorageUsage()
        
        // Log storage status for debugging
        println("Storage Analysis:")
        println("  Total used: ${analysis.totalUsedSpace / (1024 * 1024)}MB")
        println("  Available: ${analysis.availableSpace / (1024 * 1024)}MB")
        println("  Database: ${analysis.databaseSize / (1024 * 1024)}MB")
        println("  Cache: ${analysis.cacheSize / (1024 * 1024)}MB")
        println("  Recommendations: ${analysis.recommendations.size}")
    }
    
    /**
     * Handle app going to background - good time for cleanup.
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
            // Perform emergency cleanup
            val result = localStorageManager.performAutomaticCleanup()
            if (result.cleanupPerformed) {
                println("Emergency cleanup freed ${result.optimizationResult?.freedSpace ?: 0} bytes")
            }
        } catch (e: Exception) {
            println("Emergency cleanup failed: ${e.message}")
        }
    }
    
    /**
     * Update cleanup schedule when settings change.
     */
    suspend fun onSettingsChanged() {
        val cleanupManager = StorageCleanupManager(AutoCleanupService(localStorageManager), localStorageManager)
        cleanupManager.onSettingsChanged()
    }
}

/**
 * Example usage in MainActivity or Application class.
 */
class StorageManagementUsageExample {
    
    fun exampleUsage(
        context: Context,
        localStorageManager: LocalStorageManager
    ) {
        // Initialize storage management
        val integration = StorageManagementIntegration(context, localStorageManager)
        
        // In Application.onCreate()
        /*
        lifecycleScope.launch {
            integration.initialize()
        }
        */
        
        // In Activity.onStop() or similar
        /*
        lifecycleScope.launch {
            integration.onAppBackground()
        }
        */
        
        // In Application.onLowMemory()
        /*
        lifecycleScope.launch {
            integration.onLowMemory()
        }
        */
        
        // When user changes storage settings
        /*
        lifecycleScope.launch {
            integration.onSettingsChanged()
        }
        */
    }
}