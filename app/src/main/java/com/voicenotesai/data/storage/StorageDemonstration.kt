package com.voicenotesai.data.storage

import android.content.Context
import com.voicenotesai.domain.storage.*
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.days

/**
 * Demonstration class showing how the smart caching and cleanup system works.
 * This provides examples of all the key functionality implemented.
 */
class StorageDemonstration(
    private val localStorageManager: LocalStorageManager
) {
    
    /**
     * Demonstrate the complete storage management workflow.
     */
    suspend fun demonstrateCompleteWorkflow() {
        println("=== Smart Caching and Cleanup Demonstration ===\n")
        
        // 1. Get device capabilities
        println("1. Device Capabilities Analysis:")
        val capabilities = localStorageManager.getDeviceCapabilities()
        println("   - Total Storage: ${formatBytes(capabilities.totalStorage)}")
        println("   - Available Storage: ${formatBytes(capabilities.availableStorage)}")
        println("   - RAM Size: ${formatBytes(capabilities.ramSize)}")
        println("   - Processing Tier: ${capabilities.processingTier}")
        println("   - Battery Level: ${(capabilities.batteryLevel * 100).toInt()}%")
        println("   - Thermal State: ${capabilities.thermalState}")
        println("   - Storage Type: ${capabilities.storageType}")
        println()
        
        // 2. Analyze current storage usage
        println("2. Storage Usage Analysis:")
        val analysis = localStorageManager.analyzeStorageUsage()
        println("   - Total Used: ${formatBytes(analysis.totalUsedSpace)}")
        println("   - Available: ${formatBytes(analysis.availableSpace)}")
        println("   - Database Size: ${formatBytes(analysis.databaseSize)}")
        println("   - Cache Size: ${formatBytes(analysis.cacheSize)}")
        println("   - Audio Files: ${formatBytes(analysis.audioFilesSize)}")
        println("   - Temp Files: ${formatBytes(analysis.tempFilesSize)}")
        println("   - Old Notes: ${formatBytes(analysis.oldNotesSize)}")
        println("   - Recommendations: ${analysis.recommendations.size}")
        println()
        
        // 3. Show storage recommendations
        if (analysis.recommendations.isNotEmpty()) {
            println("3. Storage Recommendations:")
            analysis.recommendations.forEachIndexed { index, recommendation ->
                println("   ${index + 1}. ${recommendation.title}")
                println("      - ${recommendation.description}")
                println("      - Priority: ${recommendation.priority}")
                println("      - Potential Savings: ${formatBytes(recommendation.potentialSavings)}")
                println("      - Action: ${recommendation.action::class.simpleName}")
            }
            println()
        }
        
        // 4. Get current storage metrics
        println("4. Current Storage Metrics:")
        val metrics = localStorageManager.getStorageMetrics().first()
        println("   - Usage Percentage: ${metrics.usagePercentage.toInt()}%")
        println("   - Storage Health: ${metrics.storageHealth}")
        println("   - Cache Usage: ${formatBytes(metrics.cacheUsage)}")
        println("   - Database Usage: ${formatBytes(metrics.databaseUsage)}")
        println("   - Audio Files Usage: ${formatBytes(metrics.audioFilesUsage)}")
        println()
        
        // 5. Show current settings
        println("5. Current Storage Settings:")
        val settings = localStorageManager.getStorageSettings()
        println("   - Automatic Cleanup: ${settings.enableAutomaticCleanup}")
        println("   - Cleanup Frequency: ${settings.cleanupFrequency}")
        println("   - Max Cache Size: ${formatBytes(settings.maxCacheSize)}")
        println("   - Audio Retention: ${settings.maxAudioRetention.inWholeDays} days")
        println("   - Archive Old Notes: ${settings.archiveOldNotes}")
        println("   - Archive Threshold: ${settings.archiveThreshold.inWholeDays} days")
        println("   - Low Storage Mode: ${settings.enableLowStorageMode}")
        println("   - Low Storage Threshold: ${(settings.lowStorageThreshold * 100).toInt()}%")
        println("   - Compression Level: ${settings.compressionLevel}")
        println()
        
        // 6. Demonstrate optimization
        println("6. Performing Storage Optimization:")
        val optimizationResult = localStorageManager.optimizeStorage()
        println("   - Space Freed: ${formatBytes(optimizationResult.freedSpace)}")
        println("   - Files Optimized: ${optimizationResult.optimizedFiles}")
        println("   - Optimization Time: ${optimizationResult.compactionTime}")
        println("   - Cache Cleanup: ${optimizationResult.cacheCleanupResult.filesDeleted} files, ${formatBytes(optimizationResult.cacheCleanupResult.freedSpace)} freed")
        println("   - Database Compaction: ${formatBytes(optimizationResult.databaseCompactionResult.freedSpace)} freed")
        if (optimizationResult.errors.isNotEmpty()) {
            println("   - Errors: ${optimizationResult.errors.joinToString(", ")}")
        }
        println()
        
        // 7. Demonstrate automatic cleanup
        println("7. Testing Automatic Cleanup:")
        val autoCleanupResult = localStorageManager.performAutomaticCleanup()
        println("   - Cleanup Performed: ${autoCleanupResult.cleanupPerformed}")
        println("   - Reason: ${autoCleanupResult.reason}")
        if (autoCleanupResult.optimizationResult != null) {
            println("   - Space Freed: ${formatBytes(autoCleanupResult.optimizationResult.freedSpace)}")
        }
        println()
        
        // 8. Demonstrate individual operations
        println("8. Individual Operations:")
        
        // Temp file cleanup
        val tempCleanup = localStorageManager.cleanupTempFiles()
        println("   - Temp Files Cleanup: ${tempCleanup.filesDeleted} files, ${formatBytes(tempCleanup.freedSpace)} freed")
        
        // Database compaction
        val dbCompaction = localStorageManager.compactDatabase()
        println("   - Database Compaction: ${formatBytes(dbCompaction.freedSpace)} freed, ${dbCompaction.tablesOptimized} tables optimized")
        
        // Archive old notes
        val archiveResult = localStorageManager.archiveOldNotes(90.days)
        println("   - Archive Old Notes: ${archiveResult.notesArchived} notes archived, ${formatBytes(archiveResult.freedSpace)} freed")
        println()
        
        // 9. Show updated metrics after operations
        println("9. Updated Storage Metrics After Operations:")
        val updatedMetrics = localStorageManager.getStorageMetrics().first()
        println("   - Usage Percentage: ${updatedMetrics.usagePercentage.toInt()}%")
        println("   - Storage Health: ${updatedMetrics.storageHealth}")
        println("   - Total Used: ${formatBytes(updatedMetrics.usedSpace)}")
        println("   - Available: ${formatBytes(updatedMetrics.availableSpace)}")
        println()
        
        println("=== Demonstration Complete ===")
    }
    
    /**
     * Demonstrate adaptive settings based on device capabilities.
     */
    suspend fun demonstrateAdaptiveSettings() {
        println("=== Adaptive Settings Demonstration ===\n")
        
        val capabilities = localStorageManager.getDeviceCapabilities()
        val currentSettings = localStorageManager.getStorageSettings()
        
        println("Current Device: ${capabilities.processingTier}")
        println("Current Settings:")
        printSettings(currentSettings)
        
        // Show optimized settings for different device tiers
        val optimizedSettings = when (capabilities.processingTier) {
            ProcessingTier.LOW_END -> currentSettings.copy(
                maxCacheSize = 50 * 1024 * 1024L, // 50MB
                maxAudioRetention = 14.days,
                compressionLevel = CompressionLevel.HIGH,
                lowStorageThreshold = 0.8f
            )
            ProcessingTier.MID_RANGE -> currentSettings.copy(
                maxCacheSize = 100 * 1024 * 1024L, // 100MB
                maxAudioRetention = 30.days,
                compressionLevel = CompressionLevel.BALANCED,
                lowStorageThreshold = 0.85f
            )
            ProcessingTier.HIGH_END, ProcessingTier.FLAGSHIP -> currentSettings.copy(
                maxCacheSize = 200 * 1024 * 1024L, // 200MB
                maxAudioRetention = 60.days,
                compressionLevel = CompressionLevel.BALANCED,
                lowStorageThreshold = 0.9f
            )
        }
        
        println("\nOptimized Settings for ${capabilities.processingTier}:")
        printSettings(optimizedSettings)
        
        println("\n=== Adaptive Settings Complete ===")
    }
    
    private fun printSettings(settings: StorageManagementSettings) {
        println("  - Max Cache Size: ${formatBytes(settings.maxCacheSize)}")
        println("  - Audio Retention: ${settings.maxAudioRetention.inWholeDays} days")
        println("  - Compression Level: ${settings.compressionLevel}")
        println("  - Low Storage Threshold: ${(settings.lowStorageThreshold * 100).toInt()}%")
        println("  - Cleanup Frequency: ${settings.cleanupFrequency}")
    }
    
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024 * 1024)}GB"
            bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
            bytes >= 1024 -> "${bytes / 1024}KB"
            else -> "${bytes}B"
        }
    }
}

/**
 * Example usage of the storage demonstration.
 */
class StorageDemonstrationExample {
    
    suspend fun runDemonstration(localStorageManager: LocalStorageManager) {
        val demonstration = StorageDemonstration(localStorageManager)
        
        try {
            // Run complete workflow demonstration
            demonstration.demonstrateCompleteWorkflow()
            
            // Run adaptive settings demonstration
            demonstration.demonstrateAdaptiveSettings()
            
        } catch (e: Exception) {
            println("Demonstration failed: ${e.message}")
            e.printStackTrace()
        }
    }
}