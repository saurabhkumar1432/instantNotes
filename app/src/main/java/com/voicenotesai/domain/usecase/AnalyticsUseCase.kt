package com.voicenotesai.domain.usecase

import com.voicenotesai.domain.analytics.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for analytics operations.
 * Provides business logic for analytics tracking and insights generation.
 */
class AnalyticsUseCase @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) {
    
    /**
     * Track an analytics event with privacy compliance
     */
    suspend fun trackEvent(event: AnalyticsEvent): TrackingResult {
        return analyticsRepository.trackEvent(event)
    }
    
    /**
     * Get comprehensive user insights
     */
    suspend fun getUserInsights(): UserInsights {
        return analyticsRepository.getUserInsights()
    }
    
    /**
     * Get productivity metrics for dashboard
     */
    suspend fun getProductivityMetrics(): ProductivityMetrics {
        return analyticsRepository.getProductivityMetrics()
    }
    
    /**
     * Get recording patterns for optimization recommendations
     */
    suspend fun getRecordingPatterns(): RecordingPatterns {
        return analyticsRepository.getRecordingPatterns()
    }
    
    /**
     * Update privacy preferences with validation
     */
    suspend fun updatePrivacyPreferences(preferences: PrivacyPreferences): ConfigResult {
        // Validate preferences
        val validatedPreferences = validatePrivacyPreferences(preferences)
        return analyticsRepository.setPrivacyPreferences(validatedPreferences)
    }
    
    /**
     * Get current privacy preferences
     */
    suspend fun getPrivacyPreferences(): PrivacyPreferences {
        return analyticsRepository.getPrivacyPreferences()
    }
    
    /**
     * Get privacy preferences as a flow for reactive UI
     */
    fun getPrivacyPreferencesFlow(): Flow<PrivacyPreferences> {
        return analyticsRepository.getPrivacyPreferencesFlow()
    }
    
    /**
     * Clear all analytics data (for privacy compliance)
     */
    suspend fun clearAllAnalyticsData(): ClearDataResult {
        return analyticsRepository.clearAllAnalyticsData()
    }
    
    /**
     * Export analytics data for user download
     */
    suspend fun exportAnalyticsData(): AnalyticsExportData {
        return analyticsRepository.exportAnalyticsData()
    }
    
    /**
     * Get real-time analytics metrics
     */
    fun getAnalyticsMetricsFlow(): Flow<AnalyticsMetrics> {
        return analyticsRepository.getAnalyticsMetricsFlow()
    }
    
    /**
     * Start a new analytics session
     */
    suspend fun startSession(): String {
        return analyticsRepository.startSession()
    }
    
    /**
     * End current analytics session
     */
    suspend fun endSession(sessionId: String) {
        analyticsRepository.endSession(sessionId)
    }
    
    /**
     * Generate personalized recommendations based on analytics
     */
    suspend fun getPersonalizedRecommendations(): List<Recommendation> {
        val insights = getUserInsights()
        return insights.recommendations
    }
    
    /**
     * Get analytics summary for quick overview
     */
    suspend fun getAnalyticsSummary(): AnalyticsSummary {
        val exportData = exportAnalyticsData()
        return exportData.summary
    }
    
    /**
     * Check if analytics collection is enabled
     */
    suspend fun isAnalyticsEnabled(): Boolean {
        val preferences = getPrivacyPreferences()
        return preferences.collectUsageData || 
               preferences.collectPerformanceData || 
               preferences.collectErrorData
    }
    
    /**
     * Get data retention status
     */
    suspend fun getDataRetentionStatus(): DataRetentionStatus {
        val preferences = getPrivacyPreferences()
        val metrics = analyticsRepository.getAnalyticsMetricsFlow()
        
        return DataRetentionStatus(
            retentionDays = preferences.dataRetentionDays,
            currentDataAge = calculateDataAge(),
            storageUsed = 0L, // Would be calculated from metrics
            estimatedCleanupDate = System.currentTimeMillis() + (preferences.dataRetentionDays * 24 * 60 * 60 * 1000L)
        )
    }
    
    /**
     * Validate privacy preferences before saving
     */
    private fun validatePrivacyPreferences(preferences: PrivacyPreferences): PrivacyPreferences {
        return preferences.copy(
            dataRetentionDays = preferences.dataRetentionDays.coerceIn(1, 365), // 1 day to 1 year
            // Ensure at least error collection is enabled for app stability
            collectErrorData = true
        )
    }
    
    /**
     * Calculate the age of the oldest analytics data
     */
    private suspend fun calculateDataAge(): Long {
        // This would query the oldest event timestamp
        return System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L) // Placeholder: 30 days
    }
}

/**
 * Data class for retention status information
 */
data class DataRetentionStatus(
    val retentionDays: Int,
    val currentDataAge: Long,
    val storageUsed: Long,
    val estimatedCleanupDate: Long
)