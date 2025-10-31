package com.voicenotesai.data.local.dao

import androidx.room.*
import com.voicenotesai.data.local.entity.CategoryUsageEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for managing category usage statistics and pattern learning.
 */
@Dao
interface CategoryUsageDao {

    @Query("SELECT * FROM category_usage ORDER BY usageCount DESC")
    fun getAllUsageStats(): Flow<List<CategoryUsageEntity>>

    @Query("SELECT * FROM category_usage WHERE category = :category")
    suspend fun getUsageStats(category: String): CategoryUsageEntity?

    @Query("SELECT * FROM category_usage ORDER BY lastUsed DESC LIMIT :limit")
    suspend fun getRecentlyUsedCategories(limit: Int = 5): List<CategoryUsageEntity>

    @Query("SELECT * FROM category_usage ORDER BY usageCount DESC LIMIT :limit")
    suspend fun getMostUsedCategories(limit: Int = 5): List<CategoryUsageEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUsageStats(entity: CategoryUsageEntity)

    @Update
    suspend fun updateUsageStats(entity: CategoryUsageEntity)

    @Transaction
    suspend fun recordUsage(
        category: String,
        keywords: String,
        confidence: Float,
        timestamp: Long
    ) {
        val existing = getUsageStats(category)
        
        if (existing != null) {
            // Update existing record
            val newUsageCount = existing.usageCount + 1
            val newTotalConfidence = existing.totalConfidence + confidence
            val newAverageConfidence = newTotalConfidence / newUsageCount
            
            // Merge keywords (simple approach - could be improved)
            val existingKeywords = existing.commonKeywords.split(",").filter { it.isNotBlank() }
            val newKeywords = keywords.split(",").filter { it.isNotBlank() }
            val mergedKeywords = (existingKeywords + newKeywords).distinct().take(20)
            
            updateUsageStats(
                existing.copy(
                    usageCount = newUsageCount,
                    lastUsed = timestamp,
                    averageConfidence = newAverageConfidence,
                    commonKeywords = mergedKeywords.joinToString(","),
                    totalConfidence = newTotalConfidence
                )
            )
        } else {
            // Insert new record
            insertUsageStats(
                CategoryUsageEntity(
                    category = category,
                    usageCount = 1,
                    lastUsed = timestamp,
                    averageConfidence = confidence,
                    commonKeywords = keywords,
                    totalConfidence = confidence
                )
            )
        }
    }

    @Query("DELETE FROM category_usage WHERE category = :category")
    suspend fun deleteUsageStats(category: String)

    @Query("DELETE FROM category_usage")
    suspend fun clearAllUsageStats()

    @Query("SELECT COUNT(*) FROM category_usage")
    suspend fun getUsageStatsCount(): Int
}