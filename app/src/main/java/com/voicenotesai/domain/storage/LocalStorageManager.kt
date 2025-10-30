package com.voicenotesai.domain.storage

import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/**
 * Interface for managing local storage optimization and cleanup operations.
 * Provides intelligent storage management based on device capabilities and user preferences.
 */
interface LocalStorageManager {
    /**
     * Optimize storage by cleaning up old data and compacting databases
     */
    suspend fun optimizeStorage(): OptimizationResult

    /**
     * Compact database to reclaim space and improve performance
     */
    suspend fun compactDatabase(): CompactionResult

    /**
     * Analyze current storage usage and provide insights
     */
    suspend fun analyzeStorageUsage(): StorageAnalysis

    /**
     * Clean up temporary files and cached data
     */
    suspend fun cleanupTempFiles(): CleanupResult

    /**
     * Archive old notes based on user preferences
     */
    suspend fun archiveOldNotes(olderThan: Duration): ArchiveResult

    /**
     * Get real-time storage metrics
     */
    fun getStorageMetrics(): Flow<StorageMetrics>

    /**
     * Perform automatic cleanup based on device capabilities and user settings
     */
    suspend fun performAutomaticCleanup(): AutoCleanupResult

    /**
     * Get device storage capabilities
     */
    suspend fun getDeviceCapabilities(): DeviceCapabilities

    /**
     * Configure storage management settings
     */
    suspend fun updateStorageSettings(settings: StorageManagementSettings)

    /**
     * Get current storage management settings
     */
    suspend fun getStorageSettings(): StorageManagementSettings
}

/**
 * Result of storage optimization operations
 */
data class OptimizationResult(
    val freedSpace: Long,
    val optimizedFiles: Int,
    val compactionTime: Duration,
    val cacheCleanupResult: CleanupResult,
    val databaseCompactionResult: CompactionResult,
    val errors: List<String> = emptyList()
)

/**
 * Result of database compaction
 */
data class CompactionResult(
    val originalSize: Long,
    val compactedSize: Long,
    val freedSpace: Long,
    val compactionTime: Duration,
    val tablesOptimized: Int,
    val indexesRebuilt: Int,
    val errors: List<String> = emptyList()
)

/**
 * Analysis of current storage usage
 */
data class StorageAnalysis(
    val totalUsedSpace: Long,
    val availableSpace: Long,
    val databaseSize: Long,
    val cacheSize: Long,
    val audioFilesSize: Long,
    val tempFilesSize: Long,
    val oldNotesSize: Long,
    val recommendations: List<StorageRecommendation>,
    val deviceCapabilities: DeviceCapabilities
)

/**
 * Result of cleanup operations
 */
data class CleanupResult(
    val filesDeleted: Int,
    val freedSpace: Long,
    val cleanupTime: Duration,
    val errors: List<String> = emptyList()
)

/**
 * Result of archiving operations
 */
data class ArchiveResult(
    val notesArchived: Int,
    val freedSpace: Long,
    val archiveTime: Duration,
    val archiveLocation: String,
    val errors: List<String> = emptyList()
)

/**
 * Real-time storage metrics
 */
data class StorageMetrics(
    val usedSpace: Long,
    val availableSpace: Long,
    val totalSpace: Long,
    val cacheUsage: Long,
    val databaseUsage: Long,
    val audioFilesUsage: Long,
    val usagePercentage: Float,
    val storageHealth: StorageHealth,
    val lastOptimization: Long?,
    val nextScheduledCleanup: Long?
)

/**
 * Storage health indicators
 */
enum class StorageHealth {
    EXCELLENT,  // < 50% usage
    GOOD,       // 50-70% usage
    WARNING,    // 70-85% usage
    CRITICAL    // > 85% usage
}

/**
 * Result of automatic cleanup operations
 */
data class AutoCleanupResult(
    val cleanupPerformed: Boolean,
    val reason: String,
    val optimizationResult: OptimizationResult?,
    val nextScheduledCleanup: Long
)

/**
 * Device storage and processing capabilities
 */
data class DeviceCapabilities(
    val totalStorage: Long,
    val availableStorage: Long,
    val ramSize: Long,
    val processingTier: ProcessingTier,
    val batteryLevel: Float,
    val isCharging: Boolean,
    val thermalState: ThermalState,
    val networkSpeed: NetworkSpeed,
    val storageType: StorageType
)

/**
 * Device processing capability tiers
 */
enum class ProcessingTier {
    LOW_END,     // Basic devices with limited resources
    MID_RANGE,   // Average devices with moderate resources
    HIGH_END,    // Premium devices with abundant resources
    FLAGSHIP     // Top-tier devices with maximum resources
}

/**
 * Device thermal states
 */
enum class ThermalState {
    NORMAL,
    MODERATE,
    HIGH,
    CRITICAL
}

/**
 * Network speed categories
 */
enum class NetworkSpeed {
    OFFLINE,
    SLOW,      // < 1 Mbps
    MODERATE,  // 1-10 Mbps
    FAST,      // 10-50 Mbps
    VERY_FAST  // > 50 Mbps
}

/**
 * Storage type categories
 */
enum class StorageType {
    EMMC,      // Embedded MultiMediaCard
    UFS,       // Universal Flash Storage
    NVME,      // NVMe SSD
    UNKNOWN
}

/**
 * Storage management settings configurable by users
 */
data class StorageManagementSettings(
    val enableAutomaticCleanup: Boolean = true,
    val cleanupFrequency: CleanupFrequency = CleanupFrequency.WEEKLY,
    val maxCacheSize: Long = 100 * 1024 * 1024, // 100MB
    val maxAudioRetention: Duration = Duration.parse("P30D"), // 30 days
    val archiveOldNotes: Boolean = true,
    val archiveThreshold: Duration = Duration.parse("P90D"), // 90 days
    val enableLowStorageMode: Boolean = true,
    val lowStorageThreshold: Float = 0.85f, // 85%
    val enableBatteryOptimization: Boolean = true,
    val enableThermalOptimization: Boolean = true,
    val compressionLevel: CompressionLevel = CompressionLevel.BALANCED,
    val deleteEmptyFolders: Boolean = true,
    val optimizeDatabaseOnStartup: Boolean = false,
    val maxTempFileAge: Duration = Duration.parse("P7D") // 7 days
)

/**
 * Cleanup frequency options
 */
enum class CleanupFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    MANUAL
}

/**
 * Compression level options
 */
enum class CompressionLevel {
    NONE,
    LOW,
    BALANCED,
    HIGH,
    MAXIMUM
}

/**
 * Storage optimization recommendations
 */
data class StorageRecommendation(
    val type: RecommendationType,
    val title: String,
    val description: String,
    val potentialSavings: Long,
    val priority: StorageRecommendationPriority,
    val action: RecommendationAction
)

/**
 * Types of storage recommendations
 */
enum class RecommendationType {
    CACHE_CLEANUP,
    OLD_NOTES_ARCHIVE,
    DATABASE_OPTIMIZATION,
    TEMP_FILES_CLEANUP,
    COMPRESSION_IMPROVEMENT,
    STORAGE_EXPANSION
}

/**
 * Priority levels for storage recommendations
 */
enum class StorageRecommendationPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Actions that can be taken for recommendations
 */
sealed class RecommendationAction {
    object AutomaticCleanup : RecommendationAction()
    object ManualReview : RecommendationAction()
    data class ConfigureSettings(val settingKey: String) : RecommendationAction()
    data class ArchiveData(val olderThan: Duration) : RecommendationAction()
    object CompactDatabase : RecommendationAction()
}