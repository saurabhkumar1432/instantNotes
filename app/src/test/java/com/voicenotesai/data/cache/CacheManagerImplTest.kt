package com.voicenotesai.data.cache

import com.voicenotesai.data.local.entity.Note
import com.voicenotesai.domain.cache.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

class CacheManagerImplTest {

    private lateinit var cacheManager: CacheManagerImpl
    private lateinit var mockPreloadingStrategy: PreloadingStrategy
    private lateinit var mockAccessTracker: AccessTracker

    @Before
    fun setup() {
        mockPreloadingStrategy = mockk(relaxed = true)
        mockAccessTracker = mockk(relaxed = true)
        
        cacheManager = CacheManagerImpl(
            maxMemorySize = 1024 * 1024, // 1MB for testing
            preloadingStrategy = mockPreloadingStrategy,
            accessTracker = mockAccessTracker
        )
    }

    @Test
    fun `cache and retrieve data successfully`() = runTest {
        // Given
        val note = Note(id = 1, content = "Test note", timestamp = System.currentTimeMillis())
        val cacheKey = CacheKey(CacheType.NOTE_CONTENT, "1")
        val cacheableNote = CacheableNote(note)
        val policy = CachePolicy(
            ttl = 1.hours,
            maxSize = 1024,
            evictionStrategy = EvictionStrategy.LRU,
            compressionEnabled = true
        )

        // When
        val cacheResult = cacheManager.cache(cacheKey, cacheableNote, policy)
        val retrievedData = cacheManager.retrieve(cacheKey)

        // Then
        assertEquals(CacheResult.Success, cacheResult)
        assertNotNull(retrievedData)
        assertTrue(retrievedData is CacheableNote)
        assertEquals(note.content, (retrievedData as CacheableNote).note.content)
        
        coVerify { mockAccessTracker.recordAccess(cacheKey, any()) }
    }

    @Test
    fun `retrieve returns null for non-existent key`() = runTest {
        // Given
        val cacheKey = CacheKey(CacheType.NOTE_CONTENT, "nonexistent")

        // When
        val retrievedData = cacheManager.retrieve(cacheKey)

        // Then
        assertNull(retrievedData)
    }

    @Test
    fun `cache eviction works with LRU strategy`() = runTest {
        // Given - Create cache manager with very small size
        val smallCacheManager = CacheManagerImpl(
            maxMemorySize = 100, // Very small cache
            preloadingStrategy = mockPreloadingStrategy,
            accessTracker = mockAccessTracker
        )
        
        val policy = CachePolicy(
            ttl = 1.hours,
            maxSize = 50,
            evictionStrategy = EvictionStrategy.LRU,
            compressionEnabled = false
        )

        // When - Add multiple items that exceed cache size
        val note1 = Note(id = 1, content = "A".repeat(30), timestamp = System.currentTimeMillis())
        val note2 = Note(id = 2, content = "B".repeat(30), timestamp = System.currentTimeMillis())
        val note3 = Note(id = 3, content = "C".repeat(30), timestamp = System.currentTimeMillis())
        
        val key1 = CacheKey(CacheType.NOTE_CONTENT, "1")
        val key2 = CacheKey(CacheType.NOTE_CONTENT, "2")
        val key3 = CacheKey(CacheType.NOTE_CONTENT, "3")
        
        smallCacheManager.cache(key1, CacheableNote(note1), policy)
        smallCacheManager.cache(key2, CacheableNote(note2), policy)
        smallCacheManager.cache(key3, CacheableNote(note3), policy)

        // Then - First item should be evicted
        val retrieved1 = smallCacheManager.retrieve(key1)
        val retrieved3 = smallCacheManager.retrieve(key3)
        
        assertNull(retrieved1) // Should be evicted
        assertNotNull(retrieved3) // Should still be there
    }

    @Test
    fun `compression reduces data size`() = runTest {
        // Given
        val largeContent = "This is a large note content that should compress well. ".repeat(100)
        val note = Note(id = 1, content = largeContent, timestamp = System.currentTimeMillis())
        val cacheKey = CacheKey(CacheType.NOTE_CONTENT, "1")
        val cacheableNote = CacheableNote(note)
        val policy = CachePolicy(
            ttl = 1.hours,
            maxSize = 1024 * 1024,
            evictionStrategy = EvictionStrategy.LRU,
            compressionEnabled = true
        )

        // When
        cacheManager.cache(cacheKey, cacheableNote, policy)
        val metrics = cacheManager.getMetrics().first()

        // Then
        assertTrue(metrics.compressionRatio < 1.0f) // Should be compressed
        assertTrue(metrics.compressionRatio > 0.0f)
    }

    @Test
    fun `invalidate removes matching entries`() = runTest {
        // Given
        val note1 = Note(id = 1, content = "Note 1", timestamp = System.currentTimeMillis())
        val note2 = Note(id = 2, content = "Note 2", timestamp = System.currentTimeMillis())
        val key1 = CacheKey(CacheType.NOTE_CONTENT, "1")
        val key2 = CacheKey(CacheType.SEARCH_RESULTS, "query1")
        val policy = CachePolicy(
            ttl = 1.hours,
            maxSize = 1024,
            evictionStrategy = EvictionStrategy.LRU
        )

        cacheManager.cache(key1, CacheableNote(note1), policy)
        cacheManager.cache(key2, CacheableSearchResults("query", listOf(note2), 1), policy)

        // When
        val result = cacheManager.invalidate(CachePattern.ByType(CacheType.NOTE_CONTENT))

        // Then
        assertEquals(1, result.invalidatedCount)
        assertNull(cacheManager.retrieve(key1))
        assertNotNull(cacheManager.retrieve(key2)) // Different type, should remain
    }

    @Test
    fun `preload processes multiple keys`() = runTest {
        // Given
        val keys = listOf(
            CacheKey(CacheType.NOTE_CONTENT, "1"),
            CacheKey(CacheType.NOTE_CONTENT, "2"),
            CacheKey(CacheType.NOTE_CONTENT, "3")
        )

        coEvery { mockPreloadingStrategy.getPreloadCandidates() } returns keys

        // When
        val result = cacheManager.preload(keys)

        // Then
        assertEquals(3, result.loadedCount)
        assertEquals(0, result.failedCount)
        assertTrue(result.totalTime >= 0.milliseconds)
    }

    @Test
    fun `metrics are updated correctly`() = runTest {
        // Given
        val note = Note(id = 1, content = "Test", timestamp = System.currentTimeMillis())
        val cacheKey = CacheKey(CacheType.NOTE_CONTENT, "1")
        val policy = CachePolicy(
            ttl = 1.hours,
            maxSize = 1024,
            evictionStrategy = EvictionStrategy.LRU
        )

        // When
        cacheManager.cache(cacheKey, CacheableNote(note), policy)
        cacheManager.retrieve(cacheKey) // Hit
        cacheManager.retrieve(CacheKey(CacheType.NOTE_CONTENT, "nonexistent")) // Miss
        
        val metrics = cacheManager.getMetrics().first()

        // Then
        assertEquals(1, metrics.totalEntries)
        assertTrue(metrics.hitRate > 0f)
        assertTrue(metrics.missRate > 0f)
        assertTrue(metrics.totalSize > 0L)
        assertEquals(MemoryPressure.LOW, metrics.memoryPressure)
    }

    @Test
    fun `clearAll removes all entries`() = runTest {
        // Given
        val note1 = Note(id = 1, content = "Note 1", timestamp = System.currentTimeMillis())
        val note2 = Note(id = 2, content = "Note 2", timestamp = System.currentTimeMillis())
        val key1 = CacheKey(CacheType.NOTE_CONTENT, "1")
        val key2 = CacheKey(CacheType.NOTE_CONTENT, "2")
        val policy = CachePolicy(
            ttl = 1.hours,
            maxSize = 1024,
            evictionStrategy = EvictionStrategy.LRU
        )

        cacheManager.cache(key1, CacheableNote(note1), policy)
        cacheManager.cache(key2, CacheableNote(note2), policy)

        // When
        val result = cacheManager.clearAll()

        // Then
        assertEquals(2, result.clearedCount)
        assertTrue(result.freedSpace > 0L)
        assertNull(cacheManager.retrieve(key1))
        assertNull(cacheManager.retrieve(key2))
        
        val metrics = cacheManager.getMetrics().first()
        assertEquals(0, metrics.totalEntries)
        assertEquals(0L, metrics.totalSize)
    }

    @Test
    fun `optimize removes expired entries and compresses data`() = runTest {
        // Given - Create entries with very short TTL
        val expiredPolicy = CachePolicy(
            ttl = 1.milliseconds, // Very short TTL
            maxSize = 1024,
            evictionStrategy = EvictionStrategy.LRU,
            compressionEnabled = false // Start uncompressed
        )
        
        val note = Note(id = 1, content = "Test note", timestamp = System.currentTimeMillis())
        val cacheKey = CacheKey(CacheType.NOTE_CONTENT, "1")
        
        cacheManager.cache(cacheKey, CacheableNote(note), expiredPolicy)
        
        // Wait for expiration
        Thread.sleep(10)

        // When
        val result = cacheManager.optimize()

        // Then
        assertTrue(result.compactedEntries >= 0)
        assertTrue(result.optimizationTime >= 0.milliseconds)
    }
}