package com.voicenotesai.data.ai

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.voicenotesai.domain.ai.*
import com.voicenotesai.data.local.dao.CategoryUsageDao
import com.voicenotesai.data.local.dao.CustomCategoryDao
import com.voicenotesai.data.local.dao.NotesDao
import com.voicenotesai.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of CategoryManager that provides AI-powered categorization
 * with user pattern learning and custom category support.
 */
@Singleton
class CategoryManagerImpl @Inject constructor(
    private val noteCategorizationService: NoteCategorizationService,
    private val categoryUsageDao: CategoryUsageDao,
    private val customCategoryDao: CustomCategoryDao,
    private val noteDao: NotesDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CategoryManager {

    companion object {
        private const val MIN_CONFIDENCE_THRESHOLD = 0.3f
        private const val HIGH_CONFIDENCE_THRESHOLD = 0.7f
        private const val PATTERN_LEARNING_WEIGHT = 0.2f
    }

    override suspend fun categorizeNote(
        content: String,
        transcribedText: String,
        userHistory: List<CategoryUsageStats>
    ): CategorySuggestion = withContext(ioDispatcher) {
        
        // Get AI categorization
        val aiCategorization = noteCategorizationService.categorizeNote(content, transcribedText)
        
        // Apply user pattern learning
        val adjustedCategory = applyUserPatterns(
            aiCategorization.primaryCategory,
            content,
            transcribedText,
            userHistory
        )
        
        // Calculate final confidence
        val baseConfidence = aiCategorization.confidence
        val patternBoost = calculatePatternBoost(adjustedCategory, content, userHistory)
        val finalConfidence = (baseConfidence + patternBoost * PATTERN_LEARNING_WEIGHT)
            .coerceIn(0f, 1f)
        
        // Generate explanation
        val reason = generateCategoryReason(adjustedCategory, aiCategorization, finalConfidence)
        
        // Extract relevant keywords
        val keywords = extractRelevantKeywords(content, transcribedText, adjustedCategory)
        
        CategorySuggestion(
            category = adjustedCategory,
            confidence = finalConfidence,
            reason = reason,
            keywords = keywords
        )
    }

    override suspend fun getCategorySuggestions(
        content: String,
        transcribedText: String,
        limit: Int
    ): List<CategorySuggestion> = withContext(ioDispatcher) {
        
        val aiCategorization = noteCategorizationService.categorizeNote(content, transcribedText)
        val userHistory = getCategoryUsageStats().first()
        
        val suggestions = mutableListOf<CategorySuggestion>()
        
        // Primary suggestion
        val primarySuggestion = categorizeNote(content, transcribedText, userHistory)
        suggestions.add(primarySuggestion)
        
        // Secondary suggestions from AI analysis
        aiCategorization.secondaryCategories.forEach { categoryScore ->
            if (categoryScore.score > MIN_CONFIDENCE_THRESHOLD && suggestions.size < limit) {
                val patternBoost = calculatePatternBoost(categoryScore.category, content, userHistory)
                val adjustedConfidence = (categoryScore.score + patternBoost * PATTERN_LEARNING_WEIGHT)
                    .coerceIn(0f, 1f)
                
                suggestions.add(
                    CategorySuggestion(
                        category = categoryScore.category,
                        confidence = adjustedConfidence,
                        reason = "Alternative categorization based on content analysis",
                        keywords = extractRelevantKeywords(content, transcribedText, categoryScore.category)
                    )
                )
            }
        }
        
        // Add user's most frequently used categories if confidence is low
        if (primarySuggestion.confidence < HIGH_CONFIDENCE_THRESHOLD && suggestions.size < limit) {
            val frequentCategories = userHistory
                .sortedByDescending { it.usageCount }
                .take(2)
                .filter { stats -> !suggestions.any { suggestion -> suggestion.category == stats.category } }
            
            for (stats in frequentCategories) {
                if (suggestions.size < limit) {
                    suggestions.add(
                        CategorySuggestion(
                            category = stats.category,
                            confidence = 0.4f, // Moderate confidence for frequent categories
                            reason = "Frequently used category (${stats.usageCount} times)",
                            keywords = stats.commonKeywords
                        )
                    )
                }
            }
        }
        
        suggestions.take(limit).sortedByDescending { it.confidence }
    }

    override suspend fun recordCategoryUsage(
        content: String,
        selectedCategory: ContentCategory,
        confidence: Float
    ) = withContext(ioDispatcher) {
        
        // Extract keywords from content for pattern learning
        val keywords = extractKeywordsForLearning(content)
        
        // Update or create usage statistics
        categoryUsageDao.recordUsage(
            category = selectedCategory.name,
            keywords = keywords.joinToString(","),
            confidence = confidence,
            timestamp = System.currentTimeMillis()
        )
    }

    override suspend fun getCategoryUsageStats(): Flow<List<CategoryUsageStats>> {
        return categoryUsageDao.getAllUsageStats().map { entities ->
            entities.map { entity ->
                CategoryUsageStats(
                    category = ContentCategory.valueOf(entity.category),
                    usageCount = entity.usageCount,
                    lastUsed = entity.lastUsed,
                    averageConfidence = entity.averageConfidence,
                    commonKeywords = entity.commonKeywords.split(",").filter { it.isNotBlank() }
                )
            }
        }
    }

    override suspend fun createCustomCategory(customCategory: CustomCategory): Result<Unit> = 
        withContext(ioDispatcher) {
            try {
                customCategoryDao.insertCustomCategory(customCategory.toEntity())
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun updateCustomCategory(customCategory: CustomCategory): Result<Unit> = 
        withContext(ioDispatcher) {
            try {
                customCategoryDao.updateCustomCategory(customCategory.toEntity())
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun deleteCustomCategory(categoryId: String): Result<Unit> = 
        withContext(ioDispatcher) {
            try {
                customCategoryDao.deleteCustomCategory(categoryId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getCustomCategories(): Flow<List<CustomCategory>> {
        return customCategoryDao.getAllCustomCategories().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllAvailableCategories(): List<ContentCategory> {
        val builtInCategories = ContentCategory.getPrimaryCategories()
        // Note: Custom categories would need to be converted to ContentCategory enum
        // For now, returning built-in categories
        return builtInCategories
    }

    override fun getCategoryColor(category: ContentCategory): androidx.compose.ui.graphics.Color {
        return category.defaultColor
    }

    override fun getCategoryIcon(category: ContentCategory): androidx.compose.ui.graphics.vector.ImageVector {
        return category.icon
    }

    override suspend fun getNotesForCategory(category: ContentCategory): Flow<List<Long>> {
        return noteDao.getNotesByCategory(category.name).map { notes ->
            notes.map { it.id }
        }
    }

    override suspend fun getCategoryDistribution(): Map<ContentCategory, Int> = 
        withContext(ioDispatcher) {
            val distribution = mutableMapOf<ContentCategory, Int>()
            
            ContentCategory.getPrimaryCategories().forEach { category ->
                val count = noteDao.getNotesCountByCategory(category.name)
                if (count > 0) {
                    distribution[category] = count
                }
            }
            
            distribution
        }

    // Private helper methods

    private fun applyUserPatterns(
        aiCategory: ContentCategory,
        content: String,
        transcribedText: String,
        userHistory: List<CategoryUsageStats>
    ): ContentCategory {
        
        // Find user patterns that match current content
        val contentKeywords = extractKeywordsForLearning("$content $transcribedText")
        
        val matchingPatterns = userHistory.filter { stats ->
            val commonKeywords = stats.commonKeywords.intersect(contentKeywords.toSet())
            commonKeywords.isNotEmpty() && stats.usageCount > 2 // Minimum usage threshold
        }
        
        // If we have strong user patterns, consider overriding AI suggestion
        val strongPattern = matchingPatterns
            .filter { it.averageConfidence > HIGH_CONFIDENCE_THRESHOLD }
            .maxByOrNull { it.usageCount }
        
        return strongPattern?.category ?: aiCategory
    }

    private fun calculatePatternBoost(
        category: ContentCategory,
        content: String,
        userHistory: List<CategoryUsageStats>
    ): Float {
        val contentKeywords = extractKeywordsForLearning(content)
        
        val categoryStats = userHistory.find { it.category == category }
            ?: return 0f
        
        val keywordMatches = categoryStats.commonKeywords.intersect(contentKeywords.toSet()).size
        val totalKeywords = categoryStats.commonKeywords.size.coerceAtLeast(1)
        
        val keywordScore = keywordMatches.toFloat() / totalKeywords
        val usageScore = (categoryStats.usageCount / 10f).coerceAtMost(1f) // Normalize usage count
        
        return (keywordScore * 0.7f + usageScore * 0.3f).coerceAtMost(1f)
    }

    private fun generateCategoryReason(
        category: ContentCategory,
        aiCategorization: NoteCategorization,
        confidence: Float
    ): String {
        return when {
            confidence > HIGH_CONFIDENCE_THRESHOLD -> 
                "High confidence match based on content analysis and user patterns"
            confidence > MIN_CONFIDENCE_THRESHOLD -> 
                "Moderate confidence match with ${category.displayName.lowercase()} indicators"
            else -> 
                "Low confidence suggestion based on available context"
        }
    }

    private fun extractRelevantKeywords(
        content: String,
        transcribedText: String,
        category: ContentCategory
    ): List<String> {
        val fullText = "$content $transcribedText".lowercase()
        val words = fullText.split("\\s+".toRegex())
            .filter { it.length > 3 }
            .distinct()
        
        // Return category-relevant keywords
        return when (category) {
            ContentCategory.WORK, ContentCategory.MEETINGS -> 
                words.filter { it in listOf("meeting", "project", "team", "deadline", "task", "work") }
            ContentCategory.IDEAS -> 
                words.filter { it in listOf("idea", "creative", "innovation", "concept", "brainstorm") }
            ContentCategory.SHOPPING -> 
                words.filter { it in listOf("buy", "purchase", "store", "price", "shopping", "list") }
            ContentCategory.PERSONAL -> 
                words.filter { it in listOf("personal", "feel", "think", "reflection", "myself") }
            else -> words.take(5)
        }.take(3)
    }

    private fun extractKeywordsForLearning(content: String): List<String> {
        return content.lowercase()
            .split("\\s+".toRegex())
            .filter { it.length > 3 && it.matches("[a-zA-Z]+".toRegex()) }
            .distinct()
            .take(10)
    }
}

// Extension functions for entity conversion
private fun CustomCategory.toEntity(): com.voicenotesai.data.local.entity.CustomCategoryEntity {
    return com.voicenotesai.data.local.entity.CustomCategoryEntity(
        id = id,
        name = name,
        description = description,
        colorValue = color.value.toLong(),
        iconName = icon.name,
        createdAt = createdAt,
        usageCount = usageCount
    )
}

private fun com.voicenotesai.data.local.entity.CustomCategoryEntity.toDomain(): CustomCategory {
    return CustomCategory(
        id = id,
        name = name,
        description = description,
        color = androidx.compose.ui.graphics.Color(colorValue.toULong()),
        icon = Icons.Default.Folder, // Default icon
        createdAt = createdAt,
        usageCount = usageCount
    )
}