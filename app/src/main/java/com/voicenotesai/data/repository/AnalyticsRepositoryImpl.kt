package com.voicenotesai.data.repository

import com.voicenotesai.domain.analytics.*
import com.voicenotesai.data.analytics.AnalyticsEngineImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AnalyticsRepository that delegates to AnalyticsEngine.
 * Provides additional repository-level functionality and caching if needed.
 */
@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val analyticsEngine: AnalyticsEngineImpl
) : AnalyticsRepository {
    
    private var currentSessionId: String? = null
    
    override suspend fun trackEvent(event: AnalyticsEvent): TrackingResult {
        return analyticsEngine.trackEvent(event)
    }
    
    override suspend fun trackUserJourney(journey: UserJourney): JourneyResult {
        return analyticsEngine.trackUserJourney(journey)
    }
    
    override suspend fun getUserInsights(): UserInsights {
        return analyticsEngine.generateInsights()
    }
    
    override suspend fun getProductivityMetrics(): ProductivityMetrics {
        return analyticsEngine.getProductivityMetrics()
    }
    
    override suspend fun getRecordingPatterns(): RecordingPatterns {
        return analyticsEngine.getRecordingPatterns()
    }
    
    override suspend fun setPrivacyPreferences(preferences: PrivacyPreferences): ConfigResult {
        return analyticsEngine.setPrivacyPreferences(preferences)
    }
    
    override suspend fun getPrivacyPreferences(): PrivacyPreferences {
        return analyticsEngine.getPrivacyPreferences()
    }
    
    override fun getPrivacyPreferencesFlow(): Flow<PrivacyPreferences> {
        return analyticsEngine.getAnalyticsMetricsFlow().map { 
            analyticsEngine.getPrivacyPreferences()
        }
    }
    
    override suspend fun clearAllAnalyticsData(): ClearDataResult {
        return analyticsEngine.clearAllData()
    }
    
    override suspend fun exportAnalyticsData(): AnalyticsExportData {
        return analyticsEngine.exportAnalyticsData()
    }
    
    override fun getAnalyticsMetricsFlow(): Flow<AnalyticsMetrics> {
        return analyticsEngine.getAnalyticsMetricsFlow()
    }
    
    override suspend fun startSession(): String {
        currentSessionId = UUID.randomUUID().toString()
        
        // Track session start event
        val sessionStartEvent = AnalyticsEvent.AppLaunched(
            sessionId = currentSessionId!!,
            coldStart = true, // This would be determined by actual app state
            startupTime = 0L // This would be measured
        )
        
        analyticsEngine.trackEvent(sessionStartEvent)
        return currentSessionId!!
    }
    
    override suspend fun endSession(sessionId: String) {
        if (currentSessionId == sessionId) {
            currentSessionId = null
        }
        // Session end is handled automatically by the analytics engine
    }
    
    override suspend fun cleanupOldData() {
        val preferences = analyticsEngine.getPrivacyPreferences()
        // Cleanup is handled automatically based on retention policy
        // This method can be used for manual cleanup if needed
    }
    
    /**
     * Convenience method to track common app events
     */
    suspend fun trackAppLaunched(coldStart: Boolean, startupTime: Long): TrackingResult {
        val sessionId = currentSessionId ?: startSession()
        
        return trackEvent(
            AnalyticsEvent.AppLaunched(
                sessionId = sessionId,
                coldStart = coldStart,
                startupTime = startupTime
            )
        )
    }
    
    /**
     * Convenience method to track recording events
     */
    suspend fun trackRecordingStarted(
        recordingMode: RecordingMode = RecordingMode.Standard,
        audioQuality: AudioQuality = AudioQuality.Medium
    ): TrackingResult {
        val sessionId = currentSessionId ?: startSession()
        
        return trackEvent(
            AnalyticsEvent.RecordingStarted(
                sessionId = sessionId,
                recordingMode = recordingMode,
                audioQuality = audioQuality
            )
        )
    }
    
    /**
     * Convenience method to track recording completion
     */
    suspend fun trackRecordingCompleted(
        duration: Long,
        audioSize: Long,
        transcriptionSuccess: Boolean,
        processingTime: Long
    ): TrackingResult {
        val sessionId = currentSessionId ?: startSession()
        
        return trackEvent(
            AnalyticsEvent.RecordingCompleted(
                sessionId = sessionId,
                duration = duration,
                audioSize = audioSize,
                transcriptionSuccess = transcriptionSuccess,
                processingTime = processingTime
            )
        )
    }
    
    /**
     * Convenience method to track note creation
     */
    suspend fun trackNoteCreated(
        noteCategory: String,
        wordCount: Int,
        hasEntities: Boolean,
        language: String?
    ): TrackingResult {
        val sessionId = currentSessionId ?: startSession()
        
        return trackEvent(
            AnalyticsEvent.NoteCreated(
                sessionId = sessionId,
                noteCategory = noteCategory,
                wordCount = wordCount,
                hasEntities = hasEntities,
                language = language
            )
        )
    }
    
    /**
     * Convenience method to track feature usage
     */
    suspend fun trackFeatureUsed(featureName: String, context: String? = null): TrackingResult {
        val sessionId = currentSessionId ?: startSession()
        
        return trackEvent(
            AnalyticsEvent.FeatureUsed(
                sessionId = sessionId,
                featureName = featureName,
                context = context
            )
        )
    }
    
    /**
     * Convenience method to track errors
     */
    suspend fun trackError(
        errorType: String,
        errorMessage: String,
        context: String? = null
    ): TrackingResult {
        val sessionId = currentSessionId ?: startSession()
        
        return trackEvent(
            AnalyticsEvent.ErrorOccurred(
                sessionId = sessionId,
                errorType = errorType,
                errorMessage = errorMessage,
                context = context
            )
        )
    }
    
    /**
     * Convenience method to track search operations
     */
    suspend fun trackSearchPerformed(
        queryLength: Int,
        resultsCount: Int,
        searchTime: Long
    ): TrackingResult {
        val sessionId = currentSessionId ?: startSession()
        
        return trackEvent(
            AnalyticsEvent.SearchPerformed(
                sessionId = sessionId,
                queryLength = queryLength,
                resultsCount = resultsCount,
                searchTime = searchTime
            )
        )
    }
    
    /**
     * Get current session ID
     */
    fun getCurrentSessionId(): String? = currentSessionId
}