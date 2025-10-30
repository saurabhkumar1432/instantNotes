package com.voicenotesai.data.cache

import com.voicenotesai.domain.cache.CacheKey
import com.voicenotesai.domain.cache.CacheType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlin.time.Duration.Companion.hours

class AccessTrackerImplTest {

    private lateinit var accessTracker: AccessTrackerImpl

    @Before
    fun setup() {
        accessTracker = AccessTrackerImpl(maxHistorySize = 100)
    }

    @Test
    fun `recordAccess adds access record`() = runTest {
        // Given
        val key = CacheKey(CacheType.NOTE_CONTENT, "1")
        val accessTime = System.currentTimeMillis()

        // When
        accessTracker.recordAccess(key, accessTime)

        // Then
        val history = accessTracker.getAccessHistory().first()
        assertEquals(1, history.size)
        assertEquals(key, history.first().key)
        assertEquals(accessTime, history.first().accessTime)
    }

    @Test
    fun `getAccessPattern returns correct pattern for single access`() = runTest {
        // Given
        val key = CacheKey(CacheType.NOTE_CONTENT, "1")
        val accessTime = System.currentTimeMillis()

        // When
        accessTracker.recordAccess(key, accessTime)
        val pattern = accessTracker.getAccessPattern(key)

        // Then
        assertEquals(key, pattern.key)
        assertEquals(1, pattern.accessCount)
        assertEquals(accessTime, pattern.firstAccess)
        assertEquals(accessTime, pattern.lastAccess)
    }

    @Test
    fun `getAccessPattern calculates intervals correctly`() = runTest {
        // Given
        val key = CacheKey(CacheType.NOTE_CONTENT, "1")
        val baseTime = System.currentTimeMillis()
        val interval = 1000L // 1 second

        // When
        accessTracker.recordAccess(key, baseTime)
        accessTracker.recordAccess(key, baseTime + interval)
        accessTracker.recordAccess(key, baseTime + interval * 2)
        
        val pattern = accessTracker.getAccessPattern(key)

        // Then
        assertEquals(3, pattern.accessCount)
        assertEquals(baseTime, pattern.firstAccess)
        assertEquals(baseTime + interval * 2, pattern.lastAccess)
        assertTrue(pattern.averageInterval.inWholeMilliseconds > 0)
    }

    @Test
    fun `getMostFrequentlyAccessed returns keys by frequency`() = runTest {
        // Given
        val key1 = CacheKey(CacheType.NOTE_CONTENT, "1")
        val key2 = CacheKey(CacheType.NOTE_CONTENT, "2")
        val key3 = CacheKey(CacheType.NOTE_CONTENT, "3")
        val baseTime = System.currentTimeMillis()

        // Key1: 3 accesses, Key2: 2 accesses, Key3: 1 access
        repeat(3) { accessTracker.recordAccess(key1, baseTime + it * 1000) }
        repeat(2) { accessTracker.recordAccess(key2, baseTime + it * 1000) }
        repeat(1) { accessTracker.recordAccess(key3, baseTime + it * 1000) }

        // When
        val mostFrequent = accessTracker.getMostFrequentlyAccessed(3)

        // Then
        assertEquals(3, mostFrequent.size)
        assertEquals(key1, mostFrequent[0]) // Most frequent
        assertEquals(key2, mostFrequent[1])
        assertEquals(key3, mostFrequent[2]) // Least frequent
    }

    @Test
    fun `getRecentlyAccessed returns recent keys`() = runTest {
        // Given
        val key1 = CacheKey(CacheType.NOTE_CONTENT, "1")
        val key2 = CacheKey(CacheType.NOTE_CONTENT, "2")
        val now = System.currentTimeMillis()
        val oldTime = now - 25 * 60 * 60 * 1000 // 25 hours ago (not recent)
        val recentTime = now - 1 * 60 * 60 * 1000 // 1 hour ago (recent)

        // When
        accessTracker.recordAccess(key1, oldTime)
        accessTracker.recordAccess(key2, recentTime)
        
        val recentKeys = accessTracker.getRecentlyAccessed(10)

        // Then
        assertEquals(1, recentKeys.size)
        assertEquals(key2, recentKeys[0])
    }

    @Test
    fun `cleanup removes old records`() = runTest {
        // Given
        val key = CacheKey(CacheType.NOTE_CONTENT, "1")
        val now = System.currentTimeMillis()
        val oldTime = now - 25 * 60 * 60 * 1000 // 25 hours ago
        val recentTime = now - 1 * 60 * 60 * 1000 // 1 hour ago

        accessTracker.recordAccess(key, oldTime)
        accessTracker.recordAccess(key, recentTime)

        // When
        accessTracker.cleanup(24.hours)

        // Then
        val history = accessTracker.getAccessHistory().first()
        assertEquals(1, history.size) // Only recent record should remain
        assertEquals(recentTime, history.first().accessTime)
    }

    @Test
    fun `getAccessStatistics returns correct statistics`() = runTest {
        // Given
        val key1 = CacheKey(CacheType.NOTE_CONTENT, "1")
        val key2 = CacheKey(CacheType.NOTE_CONTENT, "2")
        val now = System.currentTimeMillis()

        // Add some accesses
        repeat(3) { accessTracker.recordAccess(key1, now + it * 1000) }
        repeat(2) { accessTracker.recordAccess(key2, now + it * 1000) }

        // When
        val stats = accessTracker.getAccessStatistics()

        // Then
        assertEquals(5, stats.totalAccesses)
        assertEquals(2, stats.uniqueKeys)
        assertEquals(2.5f, stats.averageAccessesPerKey)
        assertEquals(key1, stats.mostAccessedKey)
        assertEquals(key2, stats.leastAccessedKey)
    }

    @Test
    fun `getHottestKeys returns keys with recent high activity`() = runTest {
        // Given
        val key1 = CacheKey(CacheType.NOTE_CONTENT, "1")
        val key2 = CacheKey(CacheType.NOTE_CONTENT, "2")
        val now = System.currentTimeMillis()
        val recentTime = now - 30 * 60 * 1000 // 30 minutes ago

        // Key1: 5 recent accesses, Key2: 2 recent accesses
        repeat(5) { accessTracker.recordAccess(key1, recentTime + it * 1000) }
        repeat(2) { accessTracker.recordAccess(key2, recentTime + it * 1000) }

        // When
        val hottestKeys = accessTracker.getHottestKeys(2, 1.hours)

        // Then
        assertEquals(2, hottestKeys.size)
        assertEquals(key1, hottestKeys[0].first)
        assertEquals(5, hottestKeys[0].second)
        assertEquals(key2, hottestKeys[1].first)
        assertEquals(2, hottestKeys[1].second)
    }

    @Test
    fun `maxHistorySize is respected`() = runTest {
        // Given
        val smallTracker = AccessTrackerImpl(maxHistorySize = 3)
        val key = CacheKey(CacheType.NOTE_CONTENT, "1")
        val baseTime = System.currentTimeMillis()

        // When - Add more records than max size
        repeat(5) { 
            smallTracker.recordAccess(key, baseTime + it * 1000)
        }

        // Then
        val history = smallTracker.getAccessHistory().first()
        assertEquals(3, history.size) // Should be limited to max size
        
        // Should contain the most recent records
        assertEquals(baseTime + 2000, history[0].accessTime)
        assertEquals(baseTime + 3000, history[1].accessTime)
        assertEquals(baseTime + 4000, history[2].accessTime)
    }

    @Test
    fun `timeOfDayPattern is tracked correctly`() = runTest {
        // Given
        val key = CacheKey(CacheType.NOTE_CONTENT, "1")
        val calendar = java.util.Calendar.getInstance()
        
        // Set to specific hours
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 9)
        val morning = calendar.timeInMillis
        
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 14)
        val afternoon = calendar.timeInMillis

        // When
        accessTracker.recordAccess(key, morning)
        accessTracker.recordAccess(key, morning + 1000) // Same hour
        accessTracker.recordAccess(key, afternoon)
        
        val pattern = accessTracker.getAccessPattern(key)

        // Then
        assertEquals(2, pattern.timeOfDayPattern[9]) // 2 accesses at 9 AM
        assertEquals(1, pattern.timeOfDayPattern[14]) // 1 access at 2 PM
    }
}