package com.voicenotesai.data.cache

import com.voicenotesai.domain.cache.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Implementation of access tracking for cache optimization and preloading
 */
class AccessTrackerImpl(
    private val maxHistorySize: Int = 10000
) : AccessTracker {

    private val accessHistory = mutableListOf<AccessRecord>()
    private val accessPatterns = mutableMapOf<String, MutableAccessPattern>()
    private val mutex = Mutex()
    
    private val _accessHistoryFlow = MutableStateFlow<List<AccessRecord>>(emptyList())

    override suspend fun recordAccess(key: CacheKey, accessTime: Long) {
        mutex.withLock {
            val record = AccessRecord(key, accessTime)
            accessHistory.add(record)
            
            // Maintain history size limit
            if (accessHistory.size > maxHistorySize) {
                accessHistory.removeFirst()
            }
            
            // Update access pattern
            val stringKey = key.toStringKey()
            val pattern = accessPatterns.getOrPut(stringKey) {
                MutableAccessPattern(
                    key = key,
                    accessTimes = mutableListOf(),
                    timeOfDayAccesses = mutableMapOf()
                )
            }
            
            pattern.accessTimes.add(accessTime)
            
            // Track time of day pattern
            val hourOfDay = java.time.Instant.ofEpochMilli(accessTime)
                .atZone(java.time.ZoneId.systemDefault())
                .hour
            pattern.timeOfDayAccesses[hourOfDay] = pattern.timeOfDayAccesses.getOrDefault(hourOfDay, 0) + 1
            
            _accessHistoryFlow.value = accessHistory.toList()
        }
    }

    override suspend fun getAccessPattern(key: CacheKey): AccessPattern {
        return mutex.withLock {
            val stringKey = key.toStringKey()
            val mutablePattern = accessPatterns[stringKey] ?: return AccessPattern(
                key = key,
                accessCount = 0,
                firstAccess = 0L,
                lastAccess = 0L,
                averageInterval = Duration.ZERO,
                intervalVariance = 0f,
                timeOfDayPattern = emptyMap()
            )
            
            mutablePattern.toAccessPattern()
        }
    }

    override suspend fun getMostFrequentlyAccessed(limit: Int): List<CacheKey> {
        return mutex.withLock {
            accessPatterns.values
                .sortedByDescending { it.accessTimes.size }
                .take(limit)
                .map { it.key }
        }
    }

    override suspend fun getRecentlyAccessed(limit: Int): List<CacheKey> {
        return mutex.withLock {
            val recentThreshold = System.currentTimeMillis() - Duration.parse("24h").inWholeMilliseconds
            
            accessPatterns.values
                .filter { pattern ->
                    pattern.accessTimes.any { it > recentThreshold }
                }
                .sortedByDescending { pattern ->
                    pattern.accessTimes.maxOrNull() ?: 0L
                }
                .take(limit)
                .map { it.key }
        }
    }

    override fun getAccessHistory(): Flow<List<AccessRecord>> = _accessHistoryFlow.asStateFlow()

    /**
     * Get access statistics for analysis
     */
    suspend fun getAccessStatistics(): AccessStatistics {
        return mutex.withLock {
            val totalAccesses = accessHistory.size
            val uniqueKeys = accessPatterns.size
            val averageAccessesPerKey = if (uniqueKeys > 0) totalAccesses.toFloat() / uniqueKeys else 0f
            
            val now = System.currentTimeMillis()
            val last24h = now - Duration.parse("24h").inWholeMilliseconds
            val recentAccesses = accessHistory.count { it.accessTime > last24h }
            
            val mostAccessedKey = accessPatterns.values.maxByOrNull { it.accessTimes.size }?.key
            val leastAccessedKey = accessPatterns.values.minByOrNull { it.accessTimes.size }?.key
            
            AccessStatistics(
                totalAccesses = totalAccesses,
                uniqueKeys = uniqueKeys,
                averageAccessesPerKey = averageAccessesPerKey,
                recentAccesses = recentAccesses,
                mostAccessedKey = mostAccessedKey,
                leastAccessedKey = leastAccessedKey,
                accessPatternsCount = accessPatterns.size
            )
        }
    }

    /**
     * Clean up old access records to maintain performance
     */
    suspend fun cleanup(olderThan: Duration) {
        mutex.withLock {
            val cutoffTime = System.currentTimeMillis() - olderThan.inWholeMilliseconds
            
            // Remove old access records
            accessHistory.removeAll { it.accessTime < cutoffTime }
            
            // Clean up access patterns
            accessPatterns.values.forEach { pattern ->
                pattern.accessTimes.removeAll { it < cutoffTime }
            }
            
            // Remove patterns with no recent accesses
            accessPatterns.entries.removeAll { (_, pattern) ->
                pattern.accessTimes.isEmpty()
            }
            
            _accessHistoryFlow.value = accessHistory.toList()
        }
    }

    /**
     * Get hottest cache keys based on recent access frequency
     */
    suspend fun getHottestKeys(limit: Int, timeWindow: Duration = Duration.parse("1h")): List<Pair<CacheKey, Int>> {
        return mutex.withLock {
            val cutoffTime = System.currentTimeMillis() - timeWindow.inWholeMilliseconds
            
            accessPatterns.values
                .map { pattern ->
                    val recentAccesses = pattern.accessTimes.count { it > cutoffTime }
                    pattern.key to recentAccesses
                }
                .filter { it.second > 0 }
                .sortedByDescending { it.second }
                .take(limit)
        }
    }
}

/**
 * Mutable access pattern for internal tracking
 */
private data class MutableAccessPattern(
    val key: CacheKey,
    val accessTimes: MutableList<Long>,
    val timeOfDayAccesses: MutableMap<Int, Int>
) {
    fun toAccessPattern(): AccessPattern {
        if (accessTimes.isEmpty()) {
            return AccessPattern(
                key = key,
                accessCount = 0,
                firstAccess = 0L,
                lastAccess = 0L,
                averageInterval = Duration.ZERO,
                intervalVariance = 0f,
                timeOfDayPattern = emptyMap()
            )
        }
        
        val sortedTimes = accessTimes.sorted()
        val intervals = sortedTimes.zipWithNext { a, b -> b - a }
        
        val averageInterval = if (intervals.isNotEmpty()) {
            intervals.average().toLong().milliseconds
        } else {
            Duration.ZERO
        }
        
        val intervalVariance = if (intervals.size > 1) {
            val mean = intervals.average()
            val variance = intervals.map { (it - mean) * (it - mean) }.average()
            kotlin.math.sqrt(variance).toFloat()
        } else {
            0f
        }
        
        return AccessPattern(
            key = key,
            accessCount = accessTimes.size,
            firstAccess = sortedTimes.first(),
            lastAccess = sortedTimes.last(),
            averageInterval = averageInterval,
            intervalVariance = intervalVariance,
            timeOfDayPattern = timeOfDayAccesses.toMap()
        )
    }
}

/**
 * Access statistics for monitoring and analysis
 */
data class AccessStatistics(
    val totalAccesses: Int,
    val uniqueKeys: Int,
    val averageAccessesPerKey: Float,
    val recentAccesses: Int,
    val mostAccessedKey: CacheKey?,
    val leastAccessedKey: CacheKey?,
    val accessPatternsCount: Int
)