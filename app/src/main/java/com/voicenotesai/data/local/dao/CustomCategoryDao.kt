package com.voicenotesai.data.local.dao

import androidx.room.*
import com.voicenotesai.data.local.entity.CustomCategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for managing user-defined custom categories.
 */
@Dao
interface CustomCategoryDao {

    @Query("SELECT * FROM custom_categories ORDER BY name ASC")
    fun getAllCustomCategories(): Flow<List<CustomCategoryEntity>>

    @Query("SELECT * FROM custom_categories WHERE id = :id")
    suspend fun getCustomCategory(id: String): CustomCategoryEntity?

    @Query("SELECT * FROM custom_categories WHERE name = :name")
    suspend fun getCustomCategoryByName(name: String): CustomCategoryEntity?

    @Query("SELECT * FROM custom_categories ORDER BY usageCount DESC LIMIT :limit")
    suspend fun getMostUsedCustomCategories(limit: Int = 5): List<CustomCategoryEntity>

    @Query("SELECT * FROM custom_categories ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentCustomCategories(limit: Int = 5): List<CustomCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCustomCategory(entity: CustomCategoryEntity)

    @Update
    suspend fun updateCustomCategory(entity: CustomCategoryEntity)

    @Query("DELETE FROM custom_categories WHERE id = :id")
    suspend fun deleteCustomCategory(id: String)

    @Query("DELETE FROM custom_categories")
    suspend fun deleteAllCustomCategories()

    @Query("UPDATE custom_categories SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsageCount(id: String)

    @Query("SELECT COUNT(*) FROM custom_categories")
    suspend fun getCustomCategoriesCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM custom_categories WHERE name = :name)")
    suspend fun categoryNameExists(name: String): Boolean
}