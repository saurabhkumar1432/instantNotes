package com.voicenotesai.data.cache

import com.voicenotesai.data.local.entity.Note
import com.voicenotesai.domain.cache.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlin.time.Duration.Companion.hours

/**
 * Integration test for the caching system
 */
class CacheIntegrationTest {

    private lateinit var accessTracker: AccessTracker
    private lateinit var preloadingStrategy: PreloadingStrategy
    private lateinit var cacheManager: CacheManager

    @Before
    fun setup() {
        accessTracker = AccessTrackerImpl()
        preloadingStrategy = FrequencyBasedPreloadingStrategy(accessTracker)
        cacheManager = CacheManagerImpl(
            maxMemorySize = 1024 * 1024, // 1MB
            preloadingStrategy = preloadingStrategy,
            accessTracker = accessTracker
        )
    }

    @Test
    fun `cache system integration test`() = runTest {
        // Given
        val note = Note(
            id = 1,
            content = "Test note for caching",
            timestamp = System.currentTimeMillis(),
            transcribedText = "Transcribed text"
        )
        
        val cacheKey = CacheKey(CacheType.NOTE_CONTENT, "1")
        val cacheableNote = CacheableNote(note)
        val policy = CachePolicy(
            ttl = 1.hours,
            maxSize = 1024,
            evictionStrategy = EvictionStrategy.LRU,
            compressionEnabled = true
        )

        // When - Cache the note
        val cacheResult = cacheManager.cache(cacheKey, cacheableNote, policy)
        
        // Then - Verify caching succeeded
        assertEquals(CacheResult.Success, cacheResult)

        // When - Retrieve the note
        val retrievedData = cacheManager.retrieve(cacheKey)
        
        // Then - Verify retrieval succeeded
        assertNotNull(retrievedData)
        assertTrue(retrievedData is CacheableNote)
        val retrievedNote = (retrievedData as CacheableNote).note
        assertEquals(note.content, retrievedNote.content)
        assertEquals(note.transcribedText, retrievedNote.transcribedText)

        // When - Check metrics
        val metrics = cacheManager.getMetrics().first()
        
        // Then - Verify metrics are updated
        assertTrue(metrics.hitRate > 0f)
        assertEquals(1, metrics.totalEntries)
        assertTrue(metrics.totalSize > 0L)
        assertTrue(metrics.compressionRatio > 0f && metrics.compressionRatio <= 1f)
    }

    @Test
    fun `preloading strategy works correctly`() = runTest {
        // Given
        val key1 = CacheKey(CacheType.NOTE_CONTENT, "1")
        val key2 = CacheKey(CacheType.NOTE_CONTENT, "2")
        val baseTime = System.currentTimeMillis()

        // When - Record multiple accesses for key1
        repeat(5) { i ->
            accessTracker.recordAccess(key1, baseTime + i * 1000)
        }
        
        // Record fewer accesses for key2
        repeat(2) { i ->
            accessTracker.recordAccess(key2, baseTime + i * 1000)
        }

        // Then - Get preload candidates
        val candidates = preloadingStrategy.getPreloadCandidates()
        
        // Verify key1 is prioritized (more frequent)
        assertTrue(candidates.isNotEmpty())
        assertEquals(key1, candidates.first())
    }

    @Test
    fun `cache compression works`() = runTest {
        // Given - Large content that should compress well
        val largeContent = "This is a test note with repetitive content. ".repeat(100)
        val note = Note(
            id = 1,
            content = largeContent,
            timestamp = System.currentTimeMillis()
        )
        
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
        val retrievedData = cacheManager.retrieve(cacheKey)
        val metrics = cacheManager.getMetrics().first()

        // Then
        assertNotNull(retrievedData)
        assertTrue(retrievedData is CacheableNote)
        assertEquals(largeContent, (retrievedData as CacheableNote).note.content)
        
        // Verify compression occurred
        assertTrue(metrics.compressionRatio < 1.0f)
        assertTrue(metrics.compressionRatio > 0.0f)
    }

    @Test
    fun `cache invalidation works`() = runTest {
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

        // Cache both items
        cacheManager.cache(key1, CacheableNote(note1), policy)
        cacheManager.cache(key2, CacheableSearchResults("query", listOf(note2), 1), policy)

        // When - Invalidate only NOTE_CONTENT type
        val result = cacheManager.invalidate(CachePattern.ByType(CacheType.NOTE_CONTENT))

        // Then
        assertEquals(1, result.invalidatedCount)
        assertNull(cacheManager.retrieve(key1)) // Should be removed
        assertNotNull(cacheManager.retrieve(key2)) // Should remain
    }
}