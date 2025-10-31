package com.voicenotesai.domain.analytics

import kotlinx.coroutines.flow.Flow

/**
 * Analytics engine interface for privacy-first local analytics.
 * All analytics data is processed and stored locally with no external transmission.
 */
interface AnalyticsEngine {
    /**
     * Track a user event with optional properties
     */
    suspend fun trackEvent(event: AnalyticsEvent): TrackingResult
    
    /**
     * Track a user journey step
     */
    suspend fun trackUserJourney(journey: UserJourney): JourneyResult
    
    /**
     * Generate insights based on collected analytics data
     */
    suspend fun generateInsights(): UserInsights
    
    /**
     * Get productivity metrics for the user
     */
    suspend fun getProductivityMetrics(): ProductivityMetrics
    
    /**
     * Get recording pattern analysis
     */
    suspend fun getRecordingPatterns(): RecordingPatterns
    
    /**
     * Set privacy preferences for analytics collection
     */
    suspend fun setPrivacyPreferences(preferences: PrivacyPreferences): ConfigResult
    
    /**
     * Get current privacy preferences
     */
    suspend fun getPrivacyPreferences(): PrivacyPreferences
    
    /**
     * Clear all analytics data (for privacy compliance)
     */
    suspend fun clearAllData(): ClearDataResult
    
    /**
     * Get analytics data for export (privacy compliant)
     */
    suspend fun exportAnalyticsData(): AnalyticsExportData
    
    /**
     * Get real-time analytics metrics as a flow
     */
    fun getAnalyticsMetricsFlow(): Flow<AnalyticsMetrics>
}

/**
 * Represents different types of analytics events
 */
sealed class AnalyticsEvent {
    abstract val timestamp: Long
    abstract val sessionId: String
    
    data class AppLaunched(
        override val timestamp: Long = System.currentTimeMillis(),
        override val sessionId: String,
        val coldStart: Boolean,
        val startupTime: Long
    ) : AnalyticsEvent()
    
    data class RecordingStarted(
        override val timestamp: Long = System.currentTimeMillis(),
        override val sessionId: String,
        val recordingMode: RecordingMode,
        val audioQuality: AudioQuality
    ) : AnalyticsEvent()
    
    data class RecordingCompleted(
        override val timestamp: Long = System.currentTimeMillis(),
        override val sessionId: String,
        val duration: Long,
        val audioSize: Long,
        val transcriptionSuccess: Boolean,
        val processingTime: Long
    ) : AnalyticsEvent()
    
    data class NoteCreated(
        override val timestamp: Long = System.currentTimeMillis(),
        override val sessionId: String,
        val noteCategory: String,
        val wordCount: Int,
        val hasEntities: Boolean,
        val language: String?
    ) : AnalyticsEvent()
    
    data class NoteViewed(
        override val timestamp: Long = System.currentTimeMillis(),
        override val sessionId: String,
        val noteId: String,
        val viewDuration: Long
    ) : AnalyticsEvent()
    
    data class SearchPerformed(
        override val timestamp: Long = System.currentTimeMillis(),
        override val sessionId: String,
        val queryLength: Int,
        val resultsCount: Int,
        val searchTime: Long
    ) : AnalyticsEvent()
    
    data class FeatureUsed(
        override val timestamp: Long = System.currentTimeMillis(),
        override val sessionId: String,
        val featureName: String,
        val context: String?
    ) : AnalyticsEvent()
    
    data class ErrorOccurred(
        override val timestamp: Long = System.currentTimeMillis(),
        override val sessionId: String,
        val errorType: String,
        val errorMessage: String,
        val context: String?
    ) : AnalyticsEvent()
}

/**
 * User journey tracking for understanding user flows
 */
data class UserJourney(
    val journeyId: String,
    val sessionId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val steps: List<JourneyStep>,
    val completed: Boolean = false,
    val abandonedAt: String? = null
)

data class JourneyStep(
    val stepName: String,
    val timestamp: Long,
    val duration: Long? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Comprehensive user insights generated from analytics data
 */
data class UserInsights(
    val recordingPatterns: RecordingPatterns,
    val contentAnalysis: ContentAnalysis,
    val productivityTrends: ProductivityTrends,
    val recommendations: List<Recommendation>,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Recording pattern analysis
 */
data class RecordingPatterns(
    val averageRecordingDuration: Long,
    val mostActiveTimeOfDay: Int, // Hour of day (0-23)
    val mostActiveDayOfWeek: Int, // Day of week (1-7)
    val recordingFrequency: RecordingFrequency,
    val preferredRecordingMode: RecordingMode,
    val averageProcessingTime: Long,
    val successRate: Float
)

/**
 * Content analysis insights
 */
data class ContentAnalysis(
    val mostCommonCategories: List<CategoryUsage>,
    val averageWordCount: Int,
    val languageDistribution: Map<String, Float>,
    val entityTypes: Map<String, Int>,
    val sentimentDistribution: SentimentDistribution,
    val topKeywords: List<String>
)

/**
 * Productivity trends and metrics
 */
data class ProductivityTrends(
    val dailyNoteCount: Map<String, Int>, // Date to count mapping
    val weeklyTrends: List<WeeklyTrend>,
    val monthlyGrowth: Float,
    val streakDays: Int,
    val peakProductivityHours: List<Int>,
    val goalProgress: GoalProgress?
)

/**
 * Productivity metrics
 */
data class ProductivityMetrics(
    val totalNotes: Int,
    val totalRecordingTime: Long,
    val averageNotesPerDay: Float,
    val currentStreak: Int,
    val longestStreak: Int,
    val thisWeekNotes: Int,
    val lastWeekNotes: Int,
    val thisMonthNotes: Int,
    val lastMonthNotes: Int,
    val efficiency: EfficiencyMetrics
)

/**
 * Privacy preferences for analytics
 */
data class PrivacyPreferences(
    val collectUsageData: Boolean = true,
    val collectPerformanceData: Boolean = true,
    val collectErrorData: Boolean = true,
    val collectContentInsights: Boolean = false, // More sensitive
    val dataRetentionDays: Int = 90,
    val anonymizeData: Boolean = true
)

// Supporting data classes
enum class RecordingMode {
    Standard, HighQuality, LowLatency
}

enum class AudioQuality {
    Low, Medium, High
}

data class RecordingFrequency(
    val notesPerDay: Float,
    val notesPerWeek: Float,
    val notesPerMonth: Float
)

data class CategoryUsage(
    val category: String,
    val count: Int,
    val percentage: Float
)

data class SentimentDistribution(
    val positive: Float,
    val neutral: Float,
    val negative: Float
)

data class WeeklyTrend(
    val weekStart: Long,
    val noteCount: Int,
    val totalDuration: Long,
    val averageWordCount: Int
)

data class GoalProgress(
    val goalType: GoalType,
    val target: Int,
    val current: Int,
    val percentage: Float
)

enum class GoalType {
    DailyNotes, WeeklyNotes, MonthlyNotes, RecordingTime
}

data class EfficiencyMetrics(
    val averageRecordingToNoteRatio: Float,
    val averageProcessingTime: Long,
    val transcriptionAccuracy: Float,
    val retryRate: Float
)

data class Recommendation(
    val type: RecommendationType,
    val title: String,
    val description: String,
    val actionable: Boolean,
    val priority: RecommendationPriority
)

enum class RecommendationType {
    OptimalTiming, CategoryOrganization, FeatureUsage, Performance
}

enum class RecommendationPriority {
    Low, Medium, High
}

// Result types
sealed class TrackingResult {
    object Success : TrackingResult()
    data class Error(val message: String) : TrackingResult()
}

sealed class JourneyResult {
    object Success : JourneyResult()
    data class Error(val message: String) : JourneyResult()
}

sealed class ConfigResult {
    object Success : ConfigResult()
    data class Error(val message: String) : ConfigResult()
}

sealed class ClearDataResult {
    object Success : ClearDataResult()
    data class Error(val message: String) : ClearDataResult()
}

/**
 * Real-time analytics metrics
 */
data class AnalyticsMetrics(
    val activeSession: Boolean,
    val sessionDuration: Long,
    val eventsInSession: Int,
    val lastEventTime: Long,
    val memoryUsage: Long,
    val storageUsage: Long
)

/**
 * Analytics data for export
 */
data class AnalyticsExportData(
    val summary: AnalyticsSummary,
    val insights: UserInsights,
    val privacyInfo: PrivacyInfo,
    val exportedAt: Long = System.currentTimeMillis()
)

data class AnalyticsSummary(
    val totalEvents: Int,
    val dateRange: DateRange,
    val dataTypes: List<String>
)

data class DateRange(
    val start: Long,
    val end: Long
)

data class PrivacyInfo(
    val dataAnonymized: Boolean,
    val retentionPolicy: String,
    val dataTypes: List<String>
)