package com.voicenotesai.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for tracking category usage patterns to improve AI suggestions.
 */
@Entity(
    tableName = "category_usage",
    indices = [
        Index(value = ["category"], name = "idx_category_usage_category"),
        Index(value = ["lastUsed"], name = "idx_category_usage_last_used"),
        Index(value = ["usageCount"], name = "idx_category_usage_count")
    ]
)
data class CategoryUsageEntity(
    @PrimaryKey
    val category: String,
    val usageCount: Int = 1,
    val lastUsed: Long = System.currentTimeMillis(),
    val averageConfidence: Float = 0f,
    val commonKeywords: String = "", // Comma-separated keywords
    val totalConfidence: Float = 0f // For calculating average
)