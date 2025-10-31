package com.voicenotesai.data.local.dao

import androidx.room.*
import com.voicenotesai.data.local.entity.AnalyticsEvent
import com.voicenotesai.data.local.entity.AnalyticsSession
import com.voicenotesai.data.local.entity.UserJourney
import com.voicenotesai.data.local.entity.AnalyticsAggregate
import com.voicenotesai.data.local.entity.AnalyticsPrivacyPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for analytics operations.
 * Optimized for privacy-first local analytics with efficient querying.
 */
@Dao
interface AnalyticsDao {
    
    // Event operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: AnalyticsEvent): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<AnalyticsEvent>)
    
    @Query("SELECT * FROM analytics_events WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getEventsByTimeRange(startTime: Long, endTime: Long): List<AnalyticsEvent>
    
    @Query("SELECT * FROM analytics_events WHERE eventType = :eventType AND timestamp >= :startTime ORDER BY timestamp DESC")
    suspend fun getEventsByType(eventType: String, startTime: Long): List<AnalyticsEvent>
    
    @Query("SELECT * FROM analytics_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getEventsBySession(sessionId: String): List<AnalyticsEvent>
    
    @Query("SELECT COUNT(*) FROM analytics_events WHERE timestamp >= :startTime")
    suspend fun getEventCountSince(startTime: Long): Int
    
    @Query("DELETE FROM analytics_events WHERE timestamp < :cutoffTime")
    suspend fun deleteOldEvents(cutoffTime: Long): Int
    
    @Query("DELETE FROM analytics_events")
    suspend fun deleteAllEvents()
    
    // Session operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AnalyticsSession)
    
    @Update
    suspend fun updateSession(session: AnalyticsSession)
    
    @Query("SELECT * FROM analytics_sessions WHERE sessionId = :sessionId")
    suspend fun getSession(sessionId: String): AnalyticsSession?
    
    @Query("SELECT * FROM analytics_sessions WHERE active = 1 LIMIT 1")
    suspend fun getActiveSession(): AnalyticsSession?
    
    @Query("SELECT * FROM analytics_sessions WHERE startTime >= :startTime ORDER BY startTime DESC")
    suspend fun getSessionsSince(startTime: Long): List<AnalyticsSession>
    
    @Query("UPDATE analytics_sessions SET active = 0, endTime = :endTime WHERE sessionId = :sessionId")
    suspend fun endSession(sessionId: String, endTime: Long)
    
    @Query("DELETE FROM analytics_sessions WHERE startTime < :cutoffTime")
    suspend fun deleteOldSessions(cutoffTime: Long): Int
    
    // Journey operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJourney(journey: UserJourney)
    
    @Update
    suspend fun updateJourney(journey: UserJourney)
    
    @Query("SELECT * FROM user_journeys WHERE journeyId = :journeyId")
    suspend fun getJourney(journeyId: String): UserJourney?
    
    @Query("SELECT * FROM user_journeys WHERE sessionId = :sessionId")
    suspend fun getJourneysBySession(sessionId: String): List<UserJourney>
    
    @Query("SELECT * FROM user_journeys WHERE completed = :completed AND startTime >= :startTime")
    suspend fun getJourneysByCompletion(completed: Boolean, startTime: Long): List<UserJourney>
    
    @Query("DELETE FROM user_journeys WHERE startTime < :cutoffTime")
    suspend fun deleteOldJourneys(cutoffTime: Long): Int
    
    // Aggregate operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAggregate(aggregate: AnalyticsAggregate)
    
    @Query("SELECT * FROM analytics_aggregates WHERE date = :date AND aggregateType = :type")
    suspend fun getAggregate(date: String, type: String): AnalyticsAggregate?
    
    @Query("SELECT * FROM analytics_aggregates WHERE aggregateType = :type AND date >= :startDate ORDER BY date DESC")
    suspend fun getAggregatesByType(type: String, startDate: String): List<AnalyticsAggregate>
    
    @Query("DELETE FROM analytics_aggregates WHERE date < :cutoffDate")
    suspend fun deleteOldAggregates(cutoffDate: String): Int
    
    // Privacy preferences
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrivacyPreferences(preferences: AnalyticsPrivacyPreferences)
    
    @Query("SELECT * FROM analytics_privacy_preferences WHERE id = 1")
    suspend fun getPrivacyPreferences(): AnalyticsPrivacyPreferences?
    
    @Query("SELECT * FROM analytics_privacy_preferences WHERE id = 1")
    fun getPrivacyPreferencesFlow(): Flow<AnalyticsPrivacyPreferences?>
    
    // Analytics queries for insights
    @Query("""
        SELECT eventType, COUNT(*) as count 
        FROM analytics_events 
        WHERE timestamp >= :startTime 
        GROUP BY eventType 
        ORDER BY count DESC
    """)
    suspend fun getEventTypeCounts(startTime: Long): List<EventTypeCount>
    
    @Query("""
        SELECT sessionId, COUNT(*) as eventCount, MIN(timestamp) as startTime, MAX(timestamp) as endTime
        FROM analytics_events 
        WHERE timestamp >= :startTime 
        GROUP BY sessionId 
        ORDER BY startTime DESC
    """)
    suspend fun getSessionSummaries(startTime: Long): List<SessionSummary>
    
    @Query("""
        SELECT strftime('%H', datetime(timestamp/1000, 'unixepoch', 'localtime')) as hour, 
               COUNT(*) as count
        FROM analytics_events 
        WHERE eventType = 'RecordingStarted' AND timestamp >= :startTime
        GROUP BY hour 
        ORDER BY count DESC
    """)
    suspend fun getRecordingHourDistribution(startTime: Long): List<HourCount>
    
    @Query("""
        SELECT strftime('%w', datetime(timestamp/1000, 'unixepoch', 'localtime')) as dayOfWeek, 
               COUNT(*) as count
        FROM analytics_events 
        WHERE eventType = 'RecordingStarted' AND timestamp >= :startTime
        GROUP BY dayOfWeek 
        ORDER BY count DESC
    """)
    suspend fun getRecordingDayDistribution(startTime: Long): List<DayCount>
    
    @Query("""
        SELECT date(timestamp/1000, 'unixepoch', 'localtime') as date, 
               COUNT(*) as count
        FROM analytics_events 
        WHERE eventType = 'NoteCreated' AND timestamp >= :startTime
        GROUP BY date 
        ORDER BY date DESC
    """)
    suspend fun getDailyNoteCounts(startTime: Long): List<DailyCount>
    
    // Cleanup operations
    @Query("SELECT COUNT(*) FROM analytics_events")
    suspend fun getTotalEventCount(): Int
    
    @Query("SELECT COUNT(*) FROM analytics_sessions")
    suspend fun getTotalSessionCount(): Int
    
    @Query("SELECT COUNT(*) FROM user_journeys")
    suspend fun getTotalJourneyCount(): Int
    
    @Transaction
    suspend fun cleanupOldData(retentionDays: Int) {
        val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
        val cutoffDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date(cutoffTime))
        
        deleteOldEvents(cutoffTime)
        deleteOldSessions(cutoffTime)
        deleteOldJourneys(cutoffTime)
        deleteOldAggregates(cutoffDate)
    }
}

// Data classes for query results
data class EventTypeCount(
    val eventType: String,
    val count: Int
)

data class SessionSummary(
    val sessionId: String,
    val eventCount: Int,
    val startTime: Long,
    val endTime: Long
)

data class HourCount(
    val hour: String,
    val count: Int
)

data class DayCount(
    val dayOfWeek: String,
    val count: Int
)

data class DailyCount(
    val date: String,
    val count: Int
)