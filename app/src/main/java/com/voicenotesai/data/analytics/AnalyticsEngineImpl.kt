package com.voicenotesai.data.analytics

import android.content.Context
import com.voicenotesai.domain.analytics.*
import com.voicenotesai.data.local.dao.AnalyticsDao
import com.voicenotesai.data.local.entity.AnalyticsEvent as AnalyticsEventEntity
import com.voicenotesai.data.local.entity.AnalyticsSession
import com.voicenotesai.data.local.entity.UserJourney as UserJourneyEntity
import com.voicenotesai.data.local.entity.AnalyticsAggregate
import com.voicenotesai.data.local.entity.AnalyticsPrivacyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.util.*
import java.text.SimpleDateFormat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AnalyticsEngine with privacy-first local analytics.
 * All data is processed and stored locally with configurable privacy settings.
 */
@Singleton
class AnalyticsEngineImpl @Inject constructor(
    private val analyticsDao: AnalyticsDao,
    private val context: Context,
    private val json: Json
) : AnalyticsEngine {
    
    private var currentSessionId: String? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    override suspend fun trackEvent(event: AnalyticsEvent): TrackingResult {
        return try {
            val preferences = getPrivacyPreferences()
            
            // Check if this type of event should be collected
            if (!shouldCollectEvent(event, preferences)) {
                return TrackingResult.Success // Silently skip
            }
            
            // Ensure we have an active session
            ensureActiveSession()
            
            // Convert domain event to entity
            val eventEntity = AnalyticsEventEntity(
                eventType = event::class.simpleName ?: "Unknown",
                timestamp = event.timestamp,
                sessionId = event.sessionId,
                properties = serializeEventProperties(event, preferences.anonymizeData),
                anonymized = preferences.anonymizeData
            )
            
            analyticsDao.insertEvent(eventEntity)
            
            // Update session event count
            updateSessionEventCount(event.sessionId)
            
            TrackingResult.Success
        } catch (e: Exception) {
            TrackingResult.Error("Failed to track event: ${e.message}")
        }
    }
    
    override suspend fun trackUserJourney(journey: UserJourney): JourneyResult {
        return try {
            val preferences = getPrivacyPreferences()
            if (!preferences.collectUsageData) {
                return JourneyResult.Success // Silently skip
            }
            
            val journeyEntity = UserJourneyEntity(
                journeyId = journey.journeyId,
                sessionId = journey.sessionId,
                startTime = journey.startTime,
                endTime = journey.endTime,
                steps = json.encodeToString(journey.steps),
                completed = journey.completed,
                abandonedAt = journey.abandonedAt
            )
            
            analyticsDao.insertJourney(journeyEntity)
            JourneyResult.Success
        } catch (e: Exception) {
            JourneyResult.Error("Failed to track journey: ${e.message}")
        }
    }
    
    override suspend fun generateInsights(): UserInsights {
        val preferences = getPrivacyPreferences()
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        
        return UserInsights(
            recordingPatterns = generateRecordingPatterns(thirtyDaysAgo),
            contentAnalysis = generateContentAnalysis(thirtyDaysAgo, preferences.collectContentInsights),
            productivityTrends = generateProductivityTrends(thirtyDaysAgo),
            recommendations = generateRecommendations(thirtyDaysAgo)
        )
    }
    
    override suspend fun getProductivityMetrics(): ProductivityMetrics {
        val now = System.currentTimeMillis()
        val oneDayAgo = now - (24 * 60 * 60 * 1000L)
        val oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000L)
        val oneMonthAgo = now - (30 * 24 * 60 * 60 * 1000L)
        
        val totalNotes = analyticsDao.getEventCountSince(0L)
        val dailyCounts = analyticsDao.getDailyNoteCounts(oneMonthAgo)
        
        return ProductivityMetrics(
            totalNotes = totalNotes,
            totalRecordingTime = calculateTotalRecordingTime(oneMonthAgo),
            averageNotesPerDay = calculateAverageNotesPerDay(dailyCounts),
            currentStreak = calculateCurrentStreak(dailyCounts),
            longestStreak = calculateLongestStreak(dailyCounts),
            thisWeekNotes = analyticsDao.getEventCountSince(oneWeekAgo),
            lastWeekNotes = analyticsDao.getEventCountSince(oneWeekAgo * 2) - analyticsDao.getEventCountSince(oneWeekAgo),
            thisMonthNotes = analyticsDao.getEventCountSince(oneMonthAgo),
            lastMonthNotes = analyticsDao.getEventCountSince(oneMonthAgo * 2) - analyticsDao.getEventCountSince(oneMonthAgo),
            efficiency = calculateEfficiencyMetrics(oneMonthAgo)
        )
    }
    
    override suspend fun getRecordingPatterns(): RecordingPatterns {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        return generateRecordingPatterns(thirtyDaysAgo)
    }
    
    override suspend fun setPrivacyPreferences(preferences: PrivacyPreferences): ConfigResult {
        return try {
            val entity = AnalyticsPrivacyPreferences(
                collectUsageData = preferences.collectUsageData,
                collectPerformanceData = preferences.collectPerformanceData,
                collectErrorData = preferences.collectErrorData,
                collectContentInsights = preferences.collectContentInsights,
                dataRetentionDays = preferences.dataRetentionDays,
                anonymizeData = preferences.anonymizeData
            )
            
            analyticsDao.insertPrivacyPreferences(entity)
            
            // Clean up old data based on new retention policy
            analyticsDao.cleanupOldData(preferences.dataRetentionDays)
            
            ConfigResult.Success
        } catch (e: Exception) {
            ConfigResult.Error("Failed to update privacy preferences: ${e.message}")
        }
    }
    
    override suspend fun getPrivacyPreferences(): PrivacyPreferences {
        val entity = analyticsDao.getPrivacyPreferences()
        return entity?.let {
            PrivacyPreferences(
                collectUsageData = it.collectUsageData,
                collectPerformanceData = it.collectPerformanceData,
                collectErrorData = it.collectErrorData,
                collectContentInsights = it.collectContentInsights,
                dataRetentionDays = it.dataRetentionDays,
                anonymizeData = it.anonymizeData
            )
        } ?: PrivacyPreferences() // Default preferences
    }
    
    override suspend fun clearAllData(): ClearDataResult {
        return try {
            analyticsDao.deleteAllEvents()
            analyticsDao.cleanupOldData(0) // Delete everything
            ClearDataResult.Success
        } catch (e: Exception) {
            ClearDataResult.Error("Failed to clear analytics data: ${e.message}")
        }
    }
    
    override suspend fun exportAnalyticsData(): AnalyticsExportData {
        val preferences = getPrivacyPreferences()
        val insights = generateInsights()
        val totalEvents = analyticsDao.getTotalEventCount()
        
        return AnalyticsExportData(
            summary = AnalyticsSummary(
                totalEvents = totalEvents,
                dateRange = DateRange(
                    start = System.currentTimeMillis() - (preferences.dataRetentionDays * 24 * 60 * 60 * 1000L),
                    end = System.currentTimeMillis()
                ),
                dataTypes = listOf("events", "sessions", "journeys", "aggregates")
            ),
            insights = insights,
            privacyInfo = PrivacyInfo(
                dataAnonymized = preferences.anonymizeData,
                retentionPolicy = "${preferences.dataRetentionDays} days",
                dataTypes = getCollectedDataTypes(preferences)
            )
        )
    }
    
    override fun getAnalyticsMetricsFlow(): Flow<AnalyticsMetrics> {
        return analyticsDao.getPrivacyPreferencesFlow().map { preferences ->
            val activeSession = analyticsDao.getActiveSession()
            val now = System.currentTimeMillis()
            
            AnalyticsMetrics(
                activeSession = activeSession != null,
                sessionDuration = activeSession?.let { now - it.startTime } ?: 0L,
                eventsInSession = activeSession?.eventCount ?: 0,
                lastEventTime = getLastEventTime(),
                memoryUsage = getAnalyticsMemoryUsage(),
                storageUsage = getAnalyticsStorageUsage()
            )
        }
    }
    
    // Private helper methods
    
    private suspend fun ensureActiveSession() {
        if (currentSessionId == null) {
            currentSessionId = UUID.randomUUID().toString()
            val session = AnalyticsSession(
                sessionId = currentSessionId!!,
                startTime = System.currentTimeMillis(),
                appVersion = getAppVersion(),
                deviceInfo = getAnonymizedDeviceInfo()
            )
            analyticsDao.insertSession(session)
        }
    }
    
    private suspend fun updateSessionEventCount(sessionId: String) {
        val session = analyticsDao.getSession(sessionId)
        session?.let {
            analyticsDao.updateSession(it.copy(eventCount = it.eventCount + 1))
        }
    }
    
    private fun shouldCollectEvent(event: AnalyticsEvent, preferences: PrivacyPreferences): Boolean {
        return when (event) {
            is AnalyticsEvent.ErrorOccurred -> preferences.collectErrorData
            is AnalyticsEvent.RecordingStarted,
            is AnalyticsEvent.RecordingCompleted -> preferences.collectPerformanceData
            is AnalyticsEvent.NoteCreated,
            is AnalyticsEvent.SearchPerformed -> preferences.collectContentInsights
            else -> preferences.collectUsageData
        }
    }
    
    private fun serializeEventProperties(event: AnalyticsEvent, anonymize: Boolean): String {
        val properties = when (event) {
            is AnalyticsEvent.AppLaunched -> mapOf(
                "coldStart" to event.coldStart,
                "startupTime" to event.startupTime
            )
            is AnalyticsEvent.RecordingStarted -> mapOf(
                "recordingMode" to event.recordingMode.name,
                "audioQuality" to event.audioQuality.name
            )
            is AnalyticsEvent.RecordingCompleted -> mapOf(
                "duration" to event.duration,
                "audioSize" to event.audioSize,
                "transcriptionSuccess" to event.transcriptionSuccess,
                "processingTime" to event.processingTime
            )
            is AnalyticsEvent.NoteCreated -> mapOf(
                "noteCategory" to event.noteCategory,
                "wordCount" to event.wordCount,
                "hasEntities" to event.hasEntities,
                "language" to (if (anonymize) "anonymized" else event.language)
            )
            is AnalyticsEvent.NoteViewed -> mapOf(
                "noteId" to (if (anonymize) "anonymized" else event.noteId),
                "viewDuration" to event.viewDuration
            )
            is AnalyticsEvent.SearchPerformed -> mapOf(
                "queryLength" to event.queryLength,
                "resultsCount" to event.resultsCount,
                "searchTime" to event.searchTime
            )
            is AnalyticsEvent.FeatureUsed -> mapOf(
                "featureName" to event.featureName,
                "context" to (if (anonymize) "anonymized" else event.context)
            )
            is AnalyticsEvent.ErrorOccurred -> mapOf(
                "errorType" to event.errorType,
                "errorMessage" to (if (anonymize) "anonymized" else event.errorMessage),
                "context" to (if (anonymize) "anonymized" else event.context)
            )
        }
        
        return json.encodeToString(properties)
    }
    
    private suspend fun generateRecordingPatterns(startTime: Long): RecordingPatterns {
        val recordingEvents = analyticsDao.getEventsByType("RecordingStarted", startTime)
        val completedEvents = analyticsDao.getEventsByType("RecordingCompleted", startTime)
        val hourDistribution = analyticsDao.getRecordingHourDistribution(startTime)
        val dayDistribution = analyticsDao.getRecordingDayDistribution(startTime)
        
        return RecordingPatterns(
            averageRecordingDuration = calculateAverageRecordingDuration(completedEvents),
            mostActiveTimeOfDay = hourDistribution.firstOrNull()?.hour?.toIntOrNull() ?: 12,
            mostActiveDayOfWeek = dayDistribution.firstOrNull()?.dayOfWeek?.toIntOrNull() ?: 1,
            recordingFrequency = calculateRecordingFrequency(recordingEvents),
            preferredRecordingMode = RecordingMode.Standard, // Default for now
            averageProcessingTime = calculateAverageProcessingTime(completedEvents),
            successRate = calculateSuccessRate(recordingEvents, completedEvents)
        )
    }
    
    private suspend fun generateContentAnalysis(startTime: Long, includeContent: Boolean): ContentAnalysis {
        if (!includeContent) {
            return ContentAnalysis(
                mostCommonCategories = emptyList(),
                averageWordCount = 0,
                languageDistribution = emptyMap(),
                entityTypes = emptyMap(),
                sentimentDistribution = SentimentDistribution(0.33f, 0.34f, 0.33f),
                topKeywords = emptyList()
            )
        }
        
        val noteEvents = analyticsDao.getEventsByType("NoteCreated", startTime)
        
        return ContentAnalysis(
            mostCommonCategories = extractCategoryUsage(noteEvents),
            averageWordCount = calculateAverageWordCount(noteEvents),
            languageDistribution = extractLanguageDistribution(noteEvents),
            entityTypes = extractEntityTypes(noteEvents),
            sentimentDistribution = SentimentDistribution(0.4f, 0.4f, 0.2f), // Placeholder
            topKeywords = emptyList() // Would require content analysis
        )
    }
    
    private suspend fun generateProductivityTrends(startTime: Long): ProductivityTrends {
        val dailyCounts = analyticsDao.getDailyNoteCounts(startTime)
        
        return ProductivityTrends(
            dailyNoteCount = dailyCounts.associate { it.date to it.count },
            weeklyTrends = calculateWeeklyTrends(dailyCounts),
            monthlyGrowth = calculateMonthlyGrowth(dailyCounts),
            streakDays = calculateCurrentStreak(dailyCounts),
            peakProductivityHours = calculatePeakHours(startTime),
            goalProgress = null // Would be set by user
        )
    }
    
    private suspend fun generateRecommendations(startTime: Long): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        
        val hourDistribution = analyticsDao.getRecordingHourDistribution(startTime)
        val peakHour = hourDistribution.firstOrNull()?.hour?.toIntOrNull()
        
        if (peakHour != null) {
            recommendations.add(
                Recommendation(
                    type = RecommendationType.OptimalTiming,
                    title = "Optimal Recording Time",
                    description = "You're most productive recording notes around ${peakHour}:00. Consider scheduling important recordings during this time.",
                    actionable = true,
                    priority = RecommendationPriority.Medium
                )
            )
        }
        
        return recommendations
    }
    
    // Additional helper methods for calculations
    private fun calculateAverageRecordingDuration(events: List<AnalyticsEventEntity>): Long {
        if (events.isEmpty()) return 0L
        
        val durations = events.mapNotNull { event ->
            try {
                val properties = json.decodeFromString<Map<String, Any>>(event.properties)
                (properties["duration"] as? Number)?.toLong()
            } catch (e: Exception) {
                null
            }
        }
        
        return if (durations.isNotEmpty()) durations.average().toLong() else 0L
    }
    
    private fun calculateRecordingFrequency(events: List<AnalyticsEventEntity>): RecordingFrequency {
        if (events.isEmpty()) return RecordingFrequency(0f, 0f, 0f)
        
        val dayCount = events.groupBy { 
            dateFormat.format(Date(it.timestamp)) 
        }.size
        
        val weekCount = dayCount / 7f
        val monthCount = dayCount / 30f
        
        return RecordingFrequency(
            notesPerDay = events.size.toFloat() / dayCount,
            notesPerWeek = events.size.toFloat() / maxOf(weekCount, 1f),
            notesPerMonth = events.size.toFloat() / maxOf(monthCount, 1f)
        )
    }
    
    private fun calculateAverageProcessingTime(events: List<AnalyticsEventEntity>): Long {
        if (events.isEmpty()) return 0L
        
        val processingTimes = events.mapNotNull { event ->
            try {
                val properties = json.decodeFromString<Map<String, Any>>(event.properties)
                (properties["processingTime"] as? Number)?.toLong()
            } catch (e: Exception) {
                null
            }
        }
        
        return if (processingTimes.isNotEmpty()) processingTimes.average().toLong() else 0L
    }
    
    private fun calculateSuccessRate(startedEvents: List<AnalyticsEventEntity>, completedEvents: List<AnalyticsEventEntity>): Float {
        if (startedEvents.isEmpty()) return 0f
        return completedEvents.size.toFloat() / startedEvents.size.toFloat()
    }
    
    private suspend fun calculateTotalRecordingTime(startTime: Long): Long {
        val completedEvents = analyticsDao.getEventsByType("RecordingCompleted", startTime)
        return completedEvents.sumOf { event ->
            try {
                val properties = json.decodeFromString<Map<String, Any>>(event.properties)
                (properties["duration"] as? Number)?.toLong() ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }
    
    private fun calculateAverageNotesPerDay(dailyCounts: List<com.voicenotesai.data.local.dao.DailyCount>): Float {
        if (dailyCounts.isEmpty()) return 0f
        return dailyCounts.map { it.count }.average().toFloat()
    }
    
    private fun calculateCurrentStreak(dailyCounts: List<com.voicenotesai.data.local.dao.DailyCount>): Int {
        if (dailyCounts.isEmpty()) return 0
        
        val sortedCounts = dailyCounts.sortedByDescending { it.date }
        var streak = 0
        
        for (dayCount in sortedCounts) {
            if (dayCount.count > 0) {
                streak++
            } else {
                break
            }
        }
        
        return streak
    }
    
    private fun calculateLongestStreak(dailyCounts: List<com.voicenotesai.data.local.dao.DailyCount>): Int {
        if (dailyCounts.isEmpty()) return 0
        
        val sortedCounts = dailyCounts.sortedBy { it.date }
        var maxStreak = 0
        var currentStreak = 0
        
        for (dayCount in sortedCounts) {
            if (dayCount.count > 0) {
                currentStreak++
                maxStreak = maxOf(maxStreak, currentStreak)
            } else {
                currentStreak = 0
            }
        }
        
        return maxStreak
    }
    
    private suspend fun calculateEfficiencyMetrics(startTime: Long): EfficiencyMetrics {
        return EfficiencyMetrics(
            averageRecordingToNoteRatio = 1.0f, // Placeholder
            averageProcessingTime = calculateAverageProcessingTime(
                analyticsDao.getEventsByType("RecordingCompleted", startTime)
            ),
            transcriptionAccuracy = 0.95f, // Placeholder
            retryRate = 0.05f // Placeholder
        )
    }
    
    private fun extractCategoryUsage(events: List<AnalyticsEventEntity>): List<CategoryUsage> {
        val categoryMap = mutableMapOf<String, Int>()
        
        events.forEach { event ->
            try {
                val properties = json.decodeFromString<Map<String, Any>>(event.properties)
                val category = properties["noteCategory"] as? String ?: "General"
                categoryMap[category] = categoryMap.getOrDefault(category, 0) + 1
            } catch (e: Exception) {
                // Skip malformed events
            }
        }
        
        val total = categoryMap.values.sum()
        return categoryMap.map { (category, count) ->
            CategoryUsage(
                category = category,
                count = count,
                percentage = if (total > 0) count.toFloat() / total else 0f
            )
        }.sortedByDescending { it.count }
    }
    
    private fun calculateAverageWordCount(events: List<AnalyticsEventEntity>): Int {
        val wordCounts = events.mapNotNull { event ->
            try {
                val properties = json.decodeFromString<Map<String, Any>>(event.properties)
                (properties["wordCount"] as? Number)?.toInt()
            } catch (e: Exception) {
                null
            }
        }
        
        return if (wordCounts.isNotEmpty()) wordCounts.average().toInt() else 0
    }
    
    private fun extractLanguageDistribution(events: List<AnalyticsEventEntity>): Map<String, Float> {
        val languageMap = mutableMapOf<String, Int>()
        
        events.forEach { event ->
            try {
                val properties = json.decodeFromString<Map<String, Any>>(event.properties)
                val language = properties["language"] as? String
                if (language != null && language != "anonymized") {
                    languageMap[language] = languageMap.getOrDefault(language, 0) + 1
                }
            } catch (e: Exception) {
                // Skip malformed events
            }
        }
        
        val total = languageMap.values.sum()
        return languageMap.mapValues { (_, count) ->
            if (total > 0) count.toFloat() / total else 0f
        }
    }
    
    private fun extractEntityTypes(events: List<AnalyticsEventEntity>): Map<String, Int> {
        // Placeholder - would require actual entity extraction data
        return mapOf(
            "Person" to 10,
            "Date" to 15,
            "Location" to 5,
            "Organization" to 8
        )
    }
    
    private fun calculateWeeklyTrends(dailyCounts: List<com.voicenotesai.data.local.dao.DailyCount>): List<WeeklyTrend> {
        // Group by week and calculate trends
        return emptyList() // Placeholder implementation
    }
    
    private fun calculateMonthlyGrowth(dailyCounts: List<com.voicenotesai.data.local.dao.DailyCount>): Float {
        // Calculate month-over-month growth
        return 0.1f // Placeholder
    }
    
    private suspend fun calculatePeakHours(startTime: Long): List<Int> {
        val hourDistribution = analyticsDao.getRecordingHourDistribution(startTime)
        return hourDistribution.take(3).mapNotNull { it.hour.toIntOrNull() }
    }
    
    private fun getCollectedDataTypes(preferences: PrivacyPreferences): List<String> {
        val types = mutableListOf<String>()
        if (preferences.collectUsageData) types.add("usage")
        if (preferences.collectPerformanceData) types.add("performance")
        if (preferences.collectErrorData) types.add("errors")
        if (preferences.collectContentInsights) types.add("content")
        return types
    }
    
    private suspend fun getLastEventTime(): Long {
        val events = analyticsDao.getEventsByTimeRange(
            System.currentTimeMillis() - 24 * 60 * 60 * 1000L,
            System.currentTimeMillis()
        )
        return events.maxOfOrNull { it.timestamp } ?: 0L
    }
    
    private fun getAnalyticsMemoryUsage(): Long {
        // Estimate memory usage of analytics system
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    }
    
    private suspend fun getAnalyticsStorageUsage(): Long {
        // Estimate storage usage of analytics data
        val eventCount = analyticsDao.getTotalEventCount()
        val sessionCount = analyticsDao.getTotalSessionCount()
        val journeyCount = analyticsDao.getTotalJourneyCount()
        
        // Rough estimate: 1KB per event, 2KB per session, 5KB per journey
        return (eventCount * 1024L) + (sessionCount * 2048L) + (journeyCount * 5120L)
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    private fun getAnonymizedDeviceInfo(): String {
        return json.encodeToString(mapOf(
            "osVersion" to android.os.Build.VERSION.RELEASE,
            "apiLevel" to android.os.Build.VERSION.SDK_INT,
            "deviceType" to if (isTablet()) "tablet" else "phone"
        ))
    }
    
    private fun isTablet(): Boolean {
        val configuration = context.resources.configuration
        return (configuration.screenLayout and android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK) >= 
                android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE
    }
}