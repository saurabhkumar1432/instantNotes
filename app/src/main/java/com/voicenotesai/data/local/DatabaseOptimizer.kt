package com.voicenotesai.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.voicenotesai.data.local.dao.DatabaseStats
import com.voicenotesai.data.local.dao.NotesDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Database optimization utility for maintaining performance with large datasets.
 * Provides methods for database compaction, cleanup, and performance monitoring.
 */
@Singleton
class DatabaseOptimizer @Inject constructor(
    private val notesDao: NotesDao,
    private val database: AppDatabase,
    private val context: Context
) {
    
    /**
     * Performs comprehensive database optimization.
     * Should be called periodically (e.g., weekly) or when performance degrades.
     * 
     * @return OptimizationResult with details about the optimization process
     */
    suspend fun optimizeDatabase(): OptimizationResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val initialStats = notesDao.getDatabaseStats()
        
        try {
            // Step 1: Clean up old archived notes (older than 1 year)
            val oneYearAgo = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000)
            val deletedCount = notesDao.deleteArchivedNotesOlderThan(oneYearAgo)
            
            // Step 2: Vacuum the database to reclaim space
            performVacuum()
            
            // Step 3: Analyze tables to update query planner statistics
            performAnalyze()
            
            val finalStats = notesDao.getDatabaseStats()
            val duration = System.currentTimeMillis() - startTime
            
            OptimizationResult.Success(
                duration = duration,
                deletedArchivedNotes = deletedCount,
                initialStats = initialStats,
                finalStats = finalStats
            )
        } catch (e: Exception) {
            OptimizationResult.Error(
                duration = System.currentTimeMillis() - startTime,
                error = e.message ?: "Unknown optimization error"
            )
        }
    }
    
    /**
     * Performs quick database maintenance without heavy operations.
     * Can be called more frequently (e.g., daily).
     * 
     * @return MaintenanceResult with details about the maintenance process
     */
    suspend fun performQuickMaintenance(): MaintenanceResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        try {
            // Update query planner statistics
            performAnalyze()
            
            val stats = notesDao.getDatabaseStats()
            val duration = System.currentTimeMillis() - startTime
            
            MaintenanceResult.Success(
                duration = duration,
                stats = stats
            )
        } catch (e: Exception) {
            MaintenanceResult.Error(
                duration = System.currentTimeMillis() - startTime,
                error = e.message ?: "Unknown maintenance error"
            )
        }
    }
    
    /**
     * Checks if database optimization is recommended based on current statistics.
     * 
     * @return OptimizationRecommendation with analysis and suggestions
     */
    suspend fun checkOptimizationNeeded(): OptimizationRecommendation = withContext(Dispatchers.IO) {
        val stats = notesDao.getDatabaseStats()
        val dbFile = context.getDatabasePath("voice_notes_database")
        val dbSizeBytes = if (dbFile.exists()) dbFile.length() else 0L
        
        val recommendations = mutableListOf<String>()
        var priority = OptimizationPriority.LOW
        
        // Check archived notes ratio
        val archivedRatio = if (stats.totalNotes > 0) {
            stats.archivedNotes.toFloat() / stats.totalNotes
        } else 0f
        
        if (archivedRatio > 0.3f) {
            recommendations.add("High number of archived notes (${(archivedRatio * 100).toInt()}%). Consider cleanup.")
            priority = maxOf(priority, OptimizationPriority.MEDIUM)
        }
        
        // Check database size
        val dbSizeMB = dbSizeBytes / (1024 * 1024)
        if (dbSizeMB > 100) {
            recommendations.add("Database size is large (${dbSizeMB}MB). Vacuum recommended.")
            priority = maxOf(priority, OptimizationPriority.MEDIUM)
        }
        
        // Check total notes count
        if (stats.totalNotes > 10000) {
            recommendations.add("Large dataset detected (${stats.totalNotes} notes). Regular optimization recommended.")
            priority = maxOf(priority, OptimizationPriority.HIGH)
        }
        
        // Check for very old notes
        val sixMonthsAgo = System.currentTimeMillis() - (180L * 24 * 60 * 60 * 1000)
        if (stats.oldestTimestamp < sixMonthsAgo && stats.archivedNotes > 100) {
            recommendations.add("Old archived notes detected. Cleanup recommended.")
            priority = maxOf(priority, OptimizationPriority.MEDIUM)
        }
        
        OptimizationRecommendation(
            isOptimizationNeeded = recommendations.isNotEmpty(),
            priority = priority,
            recommendations = recommendations,
            currentStats = stats,
            databaseSizeBytes = dbSizeBytes
        )
    }
    
    /**
     * Gets detailed database performance metrics.
     * 
     * @return DatabasePerformanceMetrics with comprehensive statistics
     */
    suspend fun getPerformanceMetrics(): DatabasePerformanceMetrics = withContext(Dispatchers.IO) {
        val stats = notesDao.getDatabaseStats()
        val dbFile = context.getDatabasePath("voice_notes_database")
        val dbSizeBytes = if (dbFile.exists()) dbFile.length() else 0L
        
        // Calculate estimated query performance based on dataset size
        val estimatedSearchTime = when {
            stats.totalNotes < 1000 -> QueryPerformance.EXCELLENT
            stats.totalNotes < 5000 -> QueryPerformance.GOOD
            stats.totalNotes < 10000 -> QueryPerformance.FAIR
            else -> QueryPerformance.NEEDS_OPTIMIZATION
        }
        
        DatabasePerformanceMetrics(
            totalNotes = stats.totalNotes,
            activeNotes = stats.activeNotes,
            archivedNotes = stats.archivedNotes,
            databaseSizeBytes = dbSizeBytes,
            avgContentLength = stats.avgContentLength,
            estimatedSearchPerformance = estimatedSearchTime,
            indexEfficiency = calculateIndexEfficiency(stats),
            lastOptimizationTime = getLastOptimizationTime()
        )
    }
    
    private fun calculateIndexEfficiency(stats: DatabaseStats): IndexEfficiency {
        // Simple heuristic based on dataset size and structure
        return when {
            stats.totalNotes < 1000 -> IndexEfficiency.OPTIMAL
            stats.totalNotes < 5000 -> IndexEfficiency.GOOD
            stats.totalNotes < 15000 -> IndexEfficiency.MODERATE
            else -> IndexEfficiency.NEEDS_ATTENTION
        }
    }
    
    private suspend fun getLastOptimizationTime(): Long {
        // This would typically be stored in preferences or a separate table
        // For now, return 0 to indicate no previous optimization
        return 0L
    }
    
    /**
     * Performs VACUUM operation on the database to reclaim space.
     */
    private suspend fun performVacuum() = withContext(Dispatchers.IO) {
        database.openHelper.writableDatabase.execSQL("VACUUM")
    }
    
    /**
     * Performs ANALYZE operation on the database to update query planner statistics.
     */
    private suspend fun performAnalyze() = withContext(Dispatchers.IO) {
        database.openHelper.writableDatabase.execSQL("ANALYZE")
    }
}

/**
 * Result of database optimization operation.
 */
sealed class OptimizationResult {
    data class Success(
        val duration: Long,
        val deletedArchivedNotes: Int,
        val initialStats: DatabaseStats,
        val finalStats: DatabaseStats
    ) : OptimizationResult()
    
    data class Error(
        val duration: Long,
        val error: String
    ) : OptimizationResult()
}

/**
 * Result of quick maintenance operation.
 */
sealed class MaintenanceResult {
    data class Success(
        val duration: Long,
        val stats: DatabaseStats
    ) : MaintenanceResult()
    
    data class Error(
        val duration: Long,
        val error: String
    ) : MaintenanceResult()
}

/**
 * Recommendation for database optimization.
 */
data class OptimizationRecommendation(
    val isOptimizationNeeded: Boolean,
    val priority: OptimizationPriority,
    val recommendations: List<String>,
    val currentStats: DatabaseStats,
    val databaseSizeBytes: Long
)

/**
 * Priority level for optimization.
 */
enum class OptimizationPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * Comprehensive database performance metrics.
 */
data class DatabasePerformanceMetrics(
    val totalNotes: Int,
    val activeNotes: Int,
    val archivedNotes: Int,
    val databaseSizeBytes: Long,
    val avgContentLength: Double,
    val estimatedSearchPerformance: QueryPerformance,
    val indexEfficiency: IndexEfficiency,
    val lastOptimizationTime: Long
)

/**
 * Query performance estimation.
 */
enum class QueryPerformance {
    EXCELLENT, GOOD, FAIR, NEEDS_OPTIMIZATION
}

/**
 * Index efficiency rating.
 */
enum class IndexEfficiency {
    OPTIMAL, GOOD, MODERATE, NEEDS_ATTENTION
}