package com.voicenotesai.data.storage

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.voicenotesai.data.local.AppDatabase
import com.voicenotesai.domain.cache.CacheManager
import com.voicenotesai.domain.storage.*
import com.voicenotesai.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

/**
 * Implementation of LocalStorageManager providing intelligent storage management
 * with device capability awareness and user-configurable settings.
 */
class LocalStorageManagerImpl @Inject constructor(
    private val context: Context,
    private val database: AppDatabase,
    private val cacheManager: CacheManager,
    private val dataStore: DataStore<Preferences>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : LocalStorageManager {

    companion object {
        // Storage settings keys
        private val KEY_ENABLE_AUTO_CLEANUP = booleanPreferencesKey("enable_auto_cleanup")
        private val KEY_CLEANUP_FREQUENCY = stringPreferencesKey("cleanup_frequency")
        private val KEY_MAX_CACHE_SIZE = longPreferencesKey("max_cache_size")
        private val KEY_MAX_AUDIO_RETENTION = longPreferencesKey("max_audio_retention")
        private val KEY_ARCHIVE_OLD_NOTES = booleanPreferencesKey("archive_old_notes")
        private val KEY_ARCHIVE_THRESHOLD = longPreferencesKey("archive_threshold")
        private val KEY_ENABLE_LOW_STORAGE_MODE = booleanPreferencesKey("enable_low_storage_mode")
        private val KEY_LOW_STORAGE_THRESHOLD = floatPreferencesKey("low_storage_threshold")
        private val KEY_ENABLE_BATTERY_OPTIMIZATION = booleanPreferencesKey("enable_battery_optimization")
        private val KEY_ENABLE_THERMAL_OPTIMIZATION = booleanPreferencesKey("enable_thermal_optimization")
        private val KEY_COMPRESSION_LEVEL = stringPreferencesKey("compression_level")
        private val KEY_DELETE_EMPTY_FOLDERS = booleanPreferencesKey("delete_empty_folders")
        private val KEY_OPTIMIZE_DB_ON_STARTUP = booleanPreferencesKey("optimize_db_on_startup")
        private val KEY_MAX_TEMP_FILE_AGE = longPreferencesKey("max_temp_file_age")
        private val KEY_LAST_OPTIMIZATION = longPreferencesKey("last_optimization")
        private val KEY_NEXT_SCHEDULED_CLEANUP = longPreferencesKey("next_scheduled_cleanup")
    }

    private val _storageMetrics = MutableStateFlow(
        StorageMetrics(
            usedSpace = 0L,
            availableSpace = 0L,
            totalSpace = 0L,
            cacheUsage = 0L,
            databaseUsage = 0L,
            audioFilesUsage = 0L,
            usagePercentage = 0f,
            storageHealth = StorageHealth.EXCELLENT,
            lastOptimization = null,
            nextScheduledCleanup = null
        )
    )

    override suspend fun optimizeStorage(): OptimizationResult = withContext(ioDispatcher) {
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<String>()
        var totalFreedSpace = 0L
        var optimizedFiles = 0

        try {
            // 1. Clean up cache
            val cacheCleanupResult = cleanupCache()
            totalFreedSpace += cacheCleanupResult.freedSpace
            optimizedFiles += cacheCleanupResult.filesDeleted

            // 2. Clean up temporary files
            val tempCleanupResult = cleanupTempFiles()
            totalFreedSpace += tempCleanupResult.freedSpace
            optimizedFiles += tempCleanupResult.filesDeleted

            // 3. Compact database
            val compactionResult = compactDatabase()
            totalFreedSpace += compactionResult.freedSpace

            // 4. Archive old notes if enabled
            val settings = getStorageSettings()
            val archiveResult = if (settings.archiveOldNotes) {
                archiveOldNotes(settings.archiveThreshold)
            } else {
                ArchiveResult(0, 0L, Duration.ZERO, "", emptyList())
            }
            totalFreedSpace += archiveResult.freedSpace

            // 5. Delete empty folders if enabled
            if (settings.deleteEmptyFolders) {
                val emptyFoldersResult = deleteEmptyFolders()
                totalFreedSpace += emptyFoldersResult.freedSpace
                optimizedFiles += emptyFoldersResult.filesDeleted
            }

            // Update last optimization timestamp
            updateLastOptimization()
            updateStorageMetrics()

            val optimizationTime = (System.currentTimeMillis() - startTime).milliseconds

            OptimizationResult(
                freedSpace = totalFreedSpace,
                optimizedFiles = optimizedFiles,
                compactionTime = optimizationTime,
                cacheCleanupResult = cacheCleanupResult,
                databaseCompactionResult = compactionResult,
                errors = errors
            )

        } catch (e: Exception) {
            errors.add("Optimization failed: ${e.message}")
            OptimizationResult(
                freedSpace = totalFreedSpace,
                optimizedFiles = optimizedFiles,
                compactionTime = (System.currentTimeMillis() - startTime).milliseconds,
                cacheCleanupResult = CleanupResult(0, 0L, Duration.ZERO, listOf(e.message ?: "Unknown error")),
                databaseCompactionResult = CompactionResult(0L, 0L, 0L, Duration.ZERO, 0, 0, listOf(e.message ?: "Unknown error")),
                errors = errors
            )
        }
    }

    override suspend fun compactDatabase(): CompactionResult = withContext(ioDispatcher) {
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<String>()

        try {
            val dbFile = File(database.openHelper.writableDatabase.path)
            val originalSize = dbFile.length()

            // Execute VACUUM command to compact database
            database.openHelper.writableDatabase.execSQL("VACUUM")
            
            // Rebuild indexes for better performance
            database.openHelper.writableDatabase.execSQL("REINDEX")

            val compactedSize = dbFile.length()
            val freedSpace = originalSize - compactedSize
            val compactionTime = (System.currentTimeMillis() - startTime).milliseconds

            CompactionResult(
                originalSize = originalSize,
                compactedSize = compactedSize,
                freedSpace = maxOf(0L, freedSpace),
                compactionTime = compactionTime,
                tablesOptimized = 1, // Simplified - in real implementation, count actual tables
                indexesRebuilt = 1,  // Simplified - in real implementation, count actual indexes
                errors = errors
            )

        } catch (e: Exception) {
            errors.add("Database compaction failed: ${e.message}")
            CompactionResult(
                originalSize = 0L,
                compactedSize = 0L,
                freedSpace = 0L,
                compactionTime = (System.currentTimeMillis() - startTime).milliseconds,
                tablesOptimized = 0,
                indexesRebuilt = 0,
                errors = errors
            )
        }
    }

    override suspend fun analyzeStorageUsage(): StorageAnalysis = withContext(ioDispatcher) {
        val deviceCapabilities = getDeviceCapabilities()
        val appDir = context.filesDir
        val cacheDir = context.cacheDir
        
        val databaseSize = getDatabaseSize()
        val cacheSize = getCacheSize()
        val audioFilesSize = getAudioFilesSize()
        val tempFilesSize = getTempFilesSize()
        val oldNotesSize = getOldNotesSize()
        
        val totalUsedSpace = databaseSize + cacheSize + audioFilesSize + tempFilesSize
        val availableSpace = deviceCapabilities.availableStorage
        
        val recommendations = generateStorageRecommendations(
            databaseSize, cacheSize, audioFilesSize, tempFilesSize, oldNotesSize, deviceCapabilities
        )

        StorageAnalysis(
            totalUsedSpace = totalUsedSpace,
            availableSpace = availableSpace,
            databaseSize = databaseSize,
            cacheSize = cacheSize,
            audioFilesSize = audioFilesSize,
            tempFilesSize = tempFilesSize,
            oldNotesSize = oldNotesSize,
            recommendations = recommendations,
            deviceCapabilities = deviceCapabilities
        )
    }

    override suspend fun cleanupTempFiles(): CleanupResult = withContext(ioDispatcher) {
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<String>()
        var filesDeleted = 0
        var freedSpace = 0L

        try {
            val settings = getStorageSettings()
            val maxAge = settings.maxTempFileAge
            val cutoffTime = System.currentTimeMillis() - maxAge.inWholeMilliseconds

            val tempDirs = listOf(
                context.cacheDir,
                File(context.filesDir, "temp"),
                File(context.getExternalFilesDir(null), "temp")
            ).filter { it.exists() }

            tempDirs.forEach { dir ->
                dir.walkTopDown().forEach { file ->
                    try {
                        if (file.isFile && file.lastModified() < cutoffTime) {
                            val fileSize = file.length()
                            if (file.delete()) {
                                filesDeleted++
                                freedSpace += fileSize
                            }
                        }
                    } catch (e: Exception) {
                        errors.add("Failed to delete ${file.name}: ${e.message}")
                    }
                }
            }

            CleanupResult(
                filesDeleted = filesDeleted,
                freedSpace = freedSpace,
                cleanupTime = (System.currentTimeMillis() - startTime).milliseconds,
                errors = errors
            )

        } catch (e: Exception) {
            errors.add("Temp file cleanup failed: ${e.message}")
            CleanupResult(
                filesDeleted = filesDeleted,
                freedSpace = freedSpace,
                cleanupTime = (System.currentTimeMillis() - startTime).milliseconds,
                errors = errors
            )
        }
    }

    override suspend fun archiveOldNotes(olderThan: Duration): ArchiveResult = withContext(ioDispatcher) {
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<String>()
        var notesArchived = 0
        var freedSpace = 0L

        try {
            val cutoffTime = System.currentTimeMillis() - olderThan.inWholeMilliseconds
            
            // This would typically involve moving old notes to an archive table or file
            // For now, we'll simulate the operation
            val oldNotes = database.notesDao().getNotesOlderThan(cutoffTime)
            
            oldNotes.forEach { note ->
                try {
                    // Calculate approximate size (simplified)
                    val noteSize = (note.content?.length ?: 0) + (note.transcribedText?.length ?: 0)
                    
                    // In a real implementation, you would:
                    // 1. Export note to archive format
                    // 2. Compress if needed
                    // 3. Store in archive location
                    // 4. Remove from main database
                    
                    notesArchived++
                    freedSpace += noteSize.toLong()
                } catch (e: Exception) {
                    errors.add("Failed to archive note ${note.id}: ${e.message}")
                }
            }

            val archiveLocation = File(context.getExternalFilesDir("archives"), "notes_archive_${System.currentTimeMillis()}.zip").absolutePath

            ArchiveResult(
                notesArchived = notesArchived,
                freedSpace = freedSpace,
                archiveTime = (System.currentTimeMillis() - startTime).milliseconds,
                archiveLocation = archiveLocation,
                errors = errors
            )

        } catch (e: Exception) {
            errors.add("Archive operation failed: ${e.message}")
            ArchiveResult(
                notesArchived = 0,
                freedSpace = 0L,
                archiveTime = (System.currentTimeMillis() - startTime).milliseconds,
                archiveLocation = "",
                errors = errors
            )
        }
    }

    override fun getStorageMetrics(): Flow<StorageMetrics> = _storageMetrics.asStateFlow()

    override suspend fun performAutomaticCleanup(): AutoCleanupResult = withContext(ioDispatcher) {
        val settings = getStorageSettings()
        
        if (!settings.enableAutomaticCleanup) {
            return@withContext AutoCleanupResult(
                cleanupPerformed = false,
                reason = "Automatic cleanup is disabled",
                optimizationResult = null,
                nextScheduledCleanup = calculateNextCleanup(settings.cleanupFrequency)
            )
        }

        val deviceCapabilities = getDeviceCapabilities()
        val storageMetrics = _storageMetrics.value

        // Check if cleanup is needed based on various conditions
        val shouldCleanup = shouldPerformCleanup(settings, deviceCapabilities, storageMetrics)
        
        if (!shouldCleanup.first) {
            return@withContext AutoCleanupResult(
                cleanupPerformed = false,
                reason = shouldCleanup.second,
                optimizationResult = null,
                nextScheduledCleanup = calculateNextCleanup(settings.cleanupFrequency)
            )
        }

        // Perform cleanup
        val optimizationResult = optimizeStorage()
        
        // Schedule next cleanup
        val nextCleanup = calculateNextCleanup(settings.cleanupFrequency)
        updateNextScheduledCleanup(nextCleanup)

        AutoCleanupResult(
            cleanupPerformed = true,
            reason = "Automatic cleanup completed successfully",
            optimizationResult = optimizationResult,
            nextScheduledCleanup = nextCleanup
        )
    }

    override suspend fun getDeviceCapabilities(): DeviceCapabilities = withContext(ioDispatcher) {
        val statFs = StatFs(Environment.getDataDirectory().path)
        val totalStorage = statFs.totalBytes
        val availableStorage = statFs.availableBytes

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val ramSize = memoryInfo.totalMem

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) / 100f
        val isCharging = batteryManager.isCharging

        DeviceCapabilities(
            totalStorage = totalStorage,
            availableStorage = availableStorage,
            ramSize = ramSize,
            processingTier = determineProcessingTier(ramSize, totalStorage),
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            thermalState = getThermalState(),
            networkSpeed = getNetworkSpeed(),
            storageType = determineStorageType()
        )
    }

    override suspend fun updateStorageSettings(settings: StorageManagementSettings): Unit = withContext(ioDispatcher) {
        dataStore.edit { preferences ->
            preferences[KEY_ENABLE_AUTO_CLEANUP] = settings.enableAutomaticCleanup
            preferences[KEY_CLEANUP_FREQUENCY] = settings.cleanupFrequency.name
            preferences[KEY_MAX_CACHE_SIZE] = settings.maxCacheSize
            preferences[KEY_MAX_AUDIO_RETENTION] = settings.maxAudioRetention.inWholeMilliseconds
            preferences[KEY_ARCHIVE_OLD_NOTES] = settings.archiveOldNotes
            preferences[KEY_ARCHIVE_THRESHOLD] = settings.archiveThreshold.inWholeMilliseconds
            preferences[KEY_ENABLE_LOW_STORAGE_MODE] = settings.enableLowStorageMode
            preferences[KEY_LOW_STORAGE_THRESHOLD] = settings.lowStorageThreshold
            preferences[KEY_ENABLE_BATTERY_OPTIMIZATION] = settings.enableBatteryOptimization
            preferences[KEY_ENABLE_THERMAL_OPTIMIZATION] = settings.enableThermalOptimization
            preferences[KEY_COMPRESSION_LEVEL] = settings.compressionLevel.name
            preferences[KEY_DELETE_EMPTY_FOLDERS] = settings.deleteEmptyFolders
            preferences[KEY_OPTIMIZE_DB_ON_STARTUP] = settings.optimizeDatabaseOnStartup
            preferences[KEY_MAX_TEMP_FILE_AGE] = settings.maxTempFileAge.inWholeMilliseconds
        }
    }

    override suspend fun getStorageSettings(): StorageManagementSettings = withContext(ioDispatcher) {
        dataStore.data.map { preferences ->
            StorageManagementSettings(
                enableAutomaticCleanup = preferences[KEY_ENABLE_AUTO_CLEANUP] ?: true,
                cleanupFrequency = try {
                    CleanupFrequency.valueOf(preferences[KEY_CLEANUP_FREQUENCY] ?: "WEEKLY")
                } catch (e: IllegalArgumentException) {
                    CleanupFrequency.WEEKLY
                },
                maxCacheSize = preferences[KEY_MAX_CACHE_SIZE] ?: (100 * 1024 * 1024L),
                maxAudioRetention = Duration.parse("PT${preferences[KEY_MAX_AUDIO_RETENTION] ?: (30L * 24 * 60 * 60 * 1000)}ms"),
                archiveOldNotes = preferences[KEY_ARCHIVE_OLD_NOTES] ?: true,
                archiveThreshold = Duration.parse("PT${preferences[KEY_ARCHIVE_THRESHOLD] ?: (90L * 24 * 60 * 60 * 1000)}ms"),
                enableLowStorageMode = preferences[KEY_ENABLE_LOW_STORAGE_MODE] ?: true,
                lowStorageThreshold = preferences[KEY_LOW_STORAGE_THRESHOLD] ?: 0.85f,
                enableBatteryOptimization = preferences[KEY_ENABLE_BATTERY_OPTIMIZATION] ?: true,
                enableThermalOptimization = preferences[KEY_ENABLE_THERMAL_OPTIMIZATION] ?: true,
                compressionLevel = try {
                    CompressionLevel.valueOf(preferences[KEY_COMPRESSION_LEVEL] ?: "BALANCED")
                } catch (e: IllegalArgumentException) {
                    CompressionLevel.BALANCED
                },
                deleteEmptyFolders = preferences[KEY_DELETE_EMPTY_FOLDERS] ?: true,
                optimizeDatabaseOnStartup = preferences[KEY_OPTIMIZE_DB_ON_STARTUP] ?: false,
                maxTempFileAge = Duration.parse("PT${preferences[KEY_MAX_TEMP_FILE_AGE] ?: (7L * 24 * 60 * 60 * 1000)}ms")
            )
        }.first()
    }

    // Private helper methods

    private suspend fun cleanupCache(): CleanupResult {
        return try {
            val cacheOptimization = cacheManager.optimize()
            CleanupResult(
                filesDeleted = cacheOptimization.compactedEntries,
                freedSpace = cacheOptimization.freedSpace,
                cleanupTime = cacheOptimization.optimizationTime,
                errors = cacheOptimization.errors
            )
        } catch (e: Exception) {
            CleanupResult(0, 0L, Duration.ZERO, listOf("Cache cleanup failed: ${e.message}"))
        }
    }

    private suspend fun deleteEmptyFolders(): CleanupResult {
        val startTime = System.currentTimeMillis()
        var foldersDeleted = 0
        var freedSpace = 0L

        try {
            val appDirs = listOf(
                context.filesDir,
                context.cacheDir,
                context.getExternalFilesDir(null)
            ).filterNotNull()

            appDirs.forEach { dir ->
                dir.walkBottomUp().forEach { file ->
                    if (file.isDirectory && file != dir && file.listFiles()?.isEmpty() == true) {
                        if (file.delete()) {
                            foldersDeleted++
                            // Empty folders don't free significant space, but we count them
                            freedSpace += 4096 // Typical directory entry size
                        }
                    }
                }
            }

            return CleanupResult(
                filesDeleted = foldersDeleted,
                freedSpace = freedSpace,
                cleanupTime = (System.currentTimeMillis() - startTime).milliseconds
            )
        } catch (e: Exception) {
            return CleanupResult(
                filesDeleted = foldersDeleted,
                freedSpace = freedSpace,
                cleanupTime = (System.currentTimeMillis() - startTime).milliseconds,
                errors = listOf("Empty folder cleanup failed: ${e.message}")
            )
        }
    }

    private suspend fun updateStorageMetrics() {
        val deviceCapabilities = getDeviceCapabilities()
        val databaseSize = getDatabaseSize()
        val cacheSize = getCacheSize()
        val audioFilesSize = getAudioFilesSize()
        
        val totalUsed = databaseSize + cacheSize + audioFilesSize
        val usagePercentage = if (deviceCapabilities.totalStorage > 0) {
            (totalUsed.toFloat() / deviceCapabilities.totalStorage) * 100f
        } else 0f

        val storageHealth = when {
            usagePercentage < 50f -> StorageHealth.EXCELLENT
            usagePercentage < 70f -> StorageHealth.GOOD
            usagePercentage < 85f -> StorageHealth.WARNING
            else -> StorageHealth.CRITICAL
        }

        val lastOptimization = dataStore.data.first()[KEY_LAST_OPTIMIZATION]
        val nextScheduledCleanup = dataStore.data.first()[KEY_NEXT_SCHEDULED_CLEANUP]

        _storageMetrics.value = StorageMetrics(
            usedSpace = totalUsed,
            availableSpace = deviceCapabilities.availableStorage,
            totalSpace = deviceCapabilities.totalStorage,
            cacheUsage = cacheSize,
            databaseUsage = databaseSize,
            audioFilesUsage = audioFilesSize,
            usagePercentage = usagePercentage,
            storageHealth = storageHealth,
            lastOptimization = lastOptimization,
            nextScheduledCleanup = nextScheduledCleanup
        )
    }

    private fun getDatabaseSize(): Long {
        return try {
            File(database.openHelper.writableDatabase.path).length()
        } catch (e: Exception) {
            0L
        }
    }

    private fun getCacheSize(): Long {
        return try {
            context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } catch (e: Exception) {
            0L
        }
    }

    private fun getAudioFilesSize(): Long {
        return try {
            val audioDir = File(context.filesDir, "audio")
            if (audioDir.exists()) {
                audioDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            } else 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun getTempFilesSize(): Long {
        return try {
            val tempDirs = listOf(
                File(context.filesDir, "temp"),
                File(context.getExternalFilesDir(null), "temp")
            ).filter { it.exists() }
            
            tempDirs.sumOf { dir ->
                dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            }
        } catch (e: Exception) {
            0L
        }
    }

    private suspend fun getOldNotesSize(): Long {
        return try {
            val settings = getStorageSettings()
            val cutoffTime = System.currentTimeMillis() - settings.archiveThreshold.inWholeMilliseconds
            val oldNotes = database.notesDao().getNotesOlderThan(cutoffTime)
            
            oldNotes.sumOf { note ->
                ((note.content?.length ?: 0) + (note.transcribedText?.length ?: 0)).toLong()
            }
        } catch (e: Exception) {
            0L
        }
    }

    private fun generateStorageRecommendations(
        databaseSize: Long,
        cacheSize: Long,
        audioFilesSize: Long,
        tempFilesSize: Long,
        oldNotesSize: Long,
        deviceCapabilities: DeviceCapabilities
    ): List<StorageRecommendation> {
        val recommendations = mutableListOf<StorageRecommendation>()

        // Cache cleanup recommendation
        if (cacheSize > 50 * 1024 * 1024) { // > 50MB
            recommendations.add(
                StorageRecommendation(
                    type = RecommendationType.CACHE_CLEANUP,
                    title = "Clean up cache",
                    description = "Cache is using ${cacheSize / (1024 * 1024)}MB. Cleaning up can free significant space.",
                    potentialSavings = cacheSize / 2, // Estimate 50% savings
                    priority = StorageRecommendationPriority.MEDIUM,
                    action = RecommendationAction.AutomaticCleanup
                )
            )
        }

        // Old notes archiving recommendation
        if (oldNotesSize > 10 * 1024 * 1024) { // > 10MB
            recommendations.add(
                StorageRecommendation(
                    type = RecommendationType.OLD_NOTES_ARCHIVE,
                    title = "Archive old notes",
                    description = "You have ${oldNotesSize / (1024 * 1024)}MB of old notes that can be archived.",
                    potentialSavings = oldNotesSize,
                    priority = StorageRecommendationPriority.LOW,
                    action = RecommendationAction.ArchiveData(90.days)
                )
            )
        }

        // Database optimization recommendation
        if (databaseSize > 100 * 1024 * 1024) { // > 100MB
            recommendations.add(
                StorageRecommendation(
                    type = RecommendationType.DATABASE_OPTIMIZATION,
                    title = "Optimize database",
                    description = "Database compaction can improve performance and free up space.",
                    potentialSavings = databaseSize / 10, // Estimate 10% savings
                    priority = StorageRecommendationPriority.MEDIUM,
                    action = RecommendationAction.CompactDatabase
                )
            )
        }

        // Temp files cleanup recommendation
        if (tempFilesSize > 5 * 1024 * 1024) { // > 5MB
            recommendations.add(
                StorageRecommendation(
                    type = RecommendationType.TEMP_FILES_CLEANUP,
                    title = "Clean temporary files",
                    description = "Temporary files are using ${tempFilesSize / (1024 * 1024)}MB of space.",
                    potentialSavings = tempFilesSize,
                    priority = StorageRecommendationPriority.HIGH,
                    action = RecommendationAction.AutomaticCleanup
                )
            )
        }

        // Low storage warning
        val usagePercentage = (databaseSize + cacheSize + audioFilesSize + tempFilesSize).toFloat() / deviceCapabilities.totalStorage
        if (usagePercentage > 0.85f) {
            recommendations.add(
                StorageRecommendation(
                    type = RecommendationType.STORAGE_EXPANSION,
                    title = "Storage space critical",
                    description = "Device storage is ${(usagePercentage * 100).toInt()}% full. Consider freeing up space.",
                    potentialSavings = 0L,
                    priority = StorageRecommendationPriority.CRITICAL,
                    action = RecommendationAction.ManualReview
                )
            )
        }

        return recommendations.sortedByDescending { it.priority.ordinal }
    }

    private fun shouldPerformCleanup(
        settings: StorageManagementSettings,
        deviceCapabilities: DeviceCapabilities,
        storageMetrics: StorageMetrics
    ): Pair<Boolean, String> {
        // Check storage threshold
        if (settings.enableLowStorageMode && storageMetrics.usagePercentage > settings.lowStorageThreshold) {
            return true to "Storage usage above threshold (${(storageMetrics.usagePercentage * 100).toInt()}%)"
        }

        // Check battery optimization
        if (settings.enableBatteryOptimization && deviceCapabilities.batteryLevel < 0.2f && !deviceCapabilities.isCharging) {
            return false to "Battery too low for cleanup operations"
        }

        // Check thermal optimization
        if (settings.enableThermalOptimization && deviceCapabilities.thermalState == ThermalState.CRITICAL) {
            return false to "Device thermal state too high for cleanup operations"
        }

        // Check scheduled cleanup
        val now = System.currentTimeMillis()
        val nextScheduled = storageMetrics.nextScheduledCleanup
        if (nextScheduled != null && now >= nextScheduled) {
            return true to "Scheduled cleanup time reached"
        }

        return false to "No cleanup needed at this time"
    }

    private fun calculateNextCleanup(frequency: CleanupFrequency): Long {
        val now = System.currentTimeMillis()
        return when (frequency) {
            CleanupFrequency.DAILY -> now + (24 * 60 * 60 * 1000)
            CleanupFrequency.WEEKLY -> now + (7 * 24 * 60 * 60 * 1000)
            CleanupFrequency.MONTHLY -> now + (30L * 24 * 60 * 60 * 1000)
            CleanupFrequency.MANUAL -> Long.MAX_VALUE
        }
    }

    private suspend fun updateLastOptimization() {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_OPTIMIZATION] = System.currentTimeMillis()
        }
    }

    private suspend fun updateNextScheduledCleanup(nextCleanup: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_NEXT_SCHEDULED_CLEANUP] = nextCleanup
        }
    }

    private fun determineProcessingTier(ramSize: Long, storageSize: Long): ProcessingTier {
        return when {
            ramSize >= 8L * 1024 * 1024 * 1024 && storageSize >= 256L * 1024 * 1024 * 1024 -> ProcessingTier.FLAGSHIP
            ramSize >= 6L * 1024 * 1024 * 1024 && storageSize >= 128L * 1024 * 1024 * 1024 -> ProcessingTier.HIGH_END
            ramSize >= 4L * 1024 * 1024 * 1024 && storageSize >= 64L * 1024 * 1024 * 1024 -> ProcessingTier.MID_RANGE
            else -> ProcessingTier.LOW_END
        }
    }

    private fun getThermalState(): ThermalState {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            when (powerManager.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> ThermalState.NORMAL
                PowerManager.THERMAL_STATUS_LIGHT -> ThermalState.MODERATE
                PowerManager.THERMAL_STATUS_MODERATE -> ThermalState.HIGH
                else -> ThermalState.CRITICAL
            }
        } catch (e: Exception) {
            ThermalState.NORMAL
        }
    }

    private fun getNetworkSpeed(): NetworkSpeed {
        // Simplified implementation - in real app, you'd check actual network speed
        return NetworkSpeed.MODERATE
    }

    private fun determineStorageType(): StorageType {
        // Simplified implementation - in real app, you'd detect actual storage type
        return StorageType.UFS
    }
}