package com.voicenotesai.domain.analytics

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for analytics operations.
 * Provides a clean abstraction over the analytics data layer.
 */
interface AnalyticsRepository {
    /**
     * Track an analytics event
     */
    suspend fun trackEvent(event: AnalyticsEvent): TrackingResult
    
    /**
     * Track a user journey
     */
    suspend fun trackUserJourney(journey: UserJourney): JourneyResult
    
    /**
     * Get user insights based on analytics data
     */
    suspend fun getUserInsights(): UserInsights
    
    /**
     * Get productivity metrics
     */
    suspend fun getProductivityMetrics(): ProductivityMetrics
    
    /**
     * Get recording patterns analysis
     */
    suspend fun getRecordingPatterns(): RecordingPatterns
    
    /**
     * Set privacy preferences
     */
    suspend fun setPrivacyPreferences(preferences: PrivacyPreferences): ConfigResult
    
    /**
     * Get current privacy preferences
     */
    suspend fun getPrivacyPreferences(): PrivacyPreferences
    
    /**
     * Get privacy preferences as a flow
     */
    fun getPrivacyPreferencesFlow(): Flow<PrivacyPreferences>
    
    /**
     * Clear all analytics data
     */
    suspend fun clearAllAnalyticsData(): ClearDataResult
    
    /**
     * Export analytics data for user
     */
    suspend fun exportAnalyticsData(): AnalyticsExportData
    
    /**
     * Get real-time analytics metrics
     */
    fun getAnalyticsMetricsFlow(): Flow<AnalyticsMetrics>
    
    /**
     * Start a new analytics session
     */
    suspend fun startSession(): String
    
    /**
     * End the current analytics session
     */
    suspend fun endSession(sessionId: String)
    
    /**
     * Clean up old analytics data based on retention policy
     */
    suspend fun cleanupOldData()
}