package com.voicenotesai.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing user-defined custom categories.
 */
@Entity(
    tableName = "custom_categories",
    indices = [
        Index(value = ["name"], name = "idx_custom_categories_name"),
        Index(value = ["createdAt"], name = "idx_custom_categories_created"),
        Index(value = ["usageCount"], name = "idx_custom_categories_usage")
    ]
)
data class CustomCategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val colorValue: Long, // Color.value as Long
    val iconName: String, // Icon identifier
    val createdAt: Long = System.currentTimeMillis(),
    val usageCount: Int = 0
)