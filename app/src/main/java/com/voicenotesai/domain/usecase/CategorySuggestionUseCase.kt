package com.voicenotesai.domain.usecase

import com.voicenotesai.domain.ai.CategoryManager
import com.voicenotesai.domain.ai.CategorySuggestion
import com.voicenotesai.domain.ai.ContentCategory
import com.voicenotesai.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for getting category suggestions for notes and learning from user selections.
 */
@Singleton
class CategorySuggestionUseCase @Inject constructor(
    private val categoryManager: CategoryManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    /**
     * Get category suggestions for a note based on its content.
     */
    suspend fun getCategorySuggestions(
        content: String,
        transcribedText: String = "",
        limit: Int = 3
    ): Result<List<CategorySuggestion>> = withContext(ioDispatcher) {
        try {
            val suggestions = categoryManager.getCategorySuggestions(content, transcribedText, limit)
            Result.success(suggestions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get the best category suggestion for a note.
     */
    suspend fun getBestCategorySuggestion(
        content: String,
        transcribedText: String = ""
    ): Result<CategorySuggestion> = withContext(ioDispatcher) {
        try {
            val userHistory = categoryManager.getCategoryUsageStats().first()
            val suggestion = categoryManager.categorizeNote(content, transcribedText, userHistory)
            Result.success(suggestion)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Apply a category to a note and record the usage for learning.
     */
    suspend fun applyCategoryToNote(
        noteContent: String,
        selectedCategory: ContentCategory,
        confidence: Float = 1.0f
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            categoryManager.recordCategoryUsage(noteContent, selectedCategory, confidence)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get category usage statistics for analytics.
     */
    suspend fun getCategoryUsageStats() = categoryManager.getCategoryUsageStats()

    /**
     * Get category distribution for the current notes.
     */
    suspend fun getCategoryDistribution(): Result<Map<ContentCategory, Int>> = withContext(ioDispatcher) {
        try {
            val distribution = categoryManager.getCategoryDistribution()
            Result.success(distribution)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}