package com.voicenotesai.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Analytics event entity for local storage.
 * Optimized for privacy-first analytics with efficient querying.
 */
@Entity(
    tableName = "analytics_events",
    indices = [
        Index(value = ["timestamp"], name = "idx_analytics_timestamp"),
        Index(value = ["eventType"], name = "idx_analytics_event_type"),
        Index(value = ["sessionId"], name = "idx_analytics_session"),
        Index(value = ["timestamp", "eventType"], name = "idx_analytics_timestamp_type")
    ]
)
data class AnalyticsEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventType: String,
    val timestamp: Long,
    val sessionId: String,
    val properties: String, // JSON string of event properties
    val anonymized: Boolean = true
)

/**
 * User journey entity for tracking user flows
 */
@Entity(
    tableName = "user_journeys",
    indices = [
        Index(value = ["journeyId"], name = "idx_journey_id"),
        Index(value = ["sessionId"], name = "idx_journey_session"),
        Index(value = ["startTime"], name = "idx_journey_start_time"),
        Index(value = ["completed"], name = "idx_journey_completed")
    ]
)
data class UserJourney(
    @PrimaryKey
    val journeyId: String,
    val sessionId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val steps: String, // JSON string of journey steps
    val completed: Boolean = false,
    val abandonedAt: String? = null
)

/**
 * Analytics session entity for tracking user sessions
 */
@Entity(
    tableName = "analytics_sessions",
    indices = [
        Index(value = ["sessionId"], name = "idx_session_id"),
        Index(value = ["startTime"], name = "idx_session_start_time"),
        Index(value = ["endTime"], name = "idx_session_end_time")
    ]
)
data class AnalyticsSession(
    @PrimaryKey
    val sessionId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val eventCount: Int = 0,
    val appVersion: String,
    val deviceInfo: String, // Anonymized device information
    val active: Boolean = true
)

/**
 * Aggregated analytics data for performance optimization
 */
@Entity(
    tableName = "analytics_aggregates",
    indices = [
        Index(value = ["date"], name = "idx_aggregate_date"),
        Index(value = ["aggregateType"], name = "idx_aggregate_type"),
        Index(value = ["date", "aggregateType"], name = "idx_aggregate_date_type")
    ]
)
data class AnalyticsAggregate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // YYYY-MM-DD format
    val aggregateType: String, // daily, weekly, monthly
    val data: String, // JSON string of aggregated data
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Privacy preferences entity
 */
@Entity(
    tableName = "analytics_privacy_preferences"
)
data class AnalyticsPrivacyPreferences(
    @PrimaryKey
    val id: Int = 1, // Single row table
    val collectUsageData: Boolean = true,
    val collectPerformanceData: Boolean = true,
    val collectErrorData: Boolean = true,
    val collectContentInsights: Boolean = false,
    val dataRetentionDays: Int = 90,
    val anonymizeData: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)