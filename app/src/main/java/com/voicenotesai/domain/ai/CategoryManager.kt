package com.voicenotesai.domain.ai

import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing note categories, including custom categories and user patterns.
 */
interface CategoryManager {
    
    /**
     * Automatically categorize a note based on its content
     */
    suspend fun categorizeNote(
        content: String,
        transcribedText: String = "",
        userHistory: List<CategoryUsageStats> = emptyList()
    ): CategorySuggestion
    
    /**
     * Get category suggestions based on user patterns and content
     */
    suspend fun getCategorySuggestions(
        content: String,
        transcribedText: String = "",
        limit: Int = 3
    ): List<CategorySuggestion>
    
    /**
     * Learn from user's category selection to improve future suggestions
     */
    suspend fun recordCategoryUsage(
        content: String,
        selectedCategory: ContentCategory,
        confidence: Float
    )
    
    /**
     * Get user's category usage statistics
     */
    suspend fun getCategoryUsageStats(): Flow<List<CategoryUsageStats>>
    
    /**
     * Create a custom category
     */
    suspend fun createCustomCategory(customCategory: CustomCategory): Result<Unit>
    
    /**
     * Update an existing custom category
     */
    suspend fun updateCustomCategory(customCategory: CustomCategory): Result<Unit>
    
    /**
     * Delete a custom category
     */
    suspend fun deleteCustomCategory(categoryId: String): Result<Unit>
    
    /**
     * Get all custom categories
     */
    suspend fun getCustomCategories(): Flow<List<CustomCategory>>
    
    /**
     * Get all available categories (built-in + custom)
     */
    suspend fun getAllAvailableCategories(): List<ContentCategory>
    
    /**
     * Get category color for UI display
     */
    fun getCategoryColor(category: ContentCategory): androidx.compose.ui.graphics.Color
    
    /**
     * Get category icon for UI display
     */
    fun getCategoryIcon(category: ContentCategory): androidx.compose.ui.graphics.vector.ImageVector
    
    /**
     * Filter notes by category
     */
    suspend fun getNotesForCategory(category: ContentCategory): Flow<List<Long>> // Note IDs
    
    /**
     * Get category distribution statistics
     */
    suspend fun getCategoryDistribution(): Map<ContentCategory, Int>
}