package com.voicenotesai.data.local

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import com.voicenotesai.data.local.dao.NotesDao
import com.voicenotesai.data.local.entity.Note
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class PaginationConfigTest {

    private lateinit var notesDao: NotesDao
    private lateinit var paginationConfig: PaginationConfig

    @Before
    fun setup() {
        notesDao = mockk<NotesDao>()
        paginationConfig = PaginationConfig(notesDao)
    }

    @Test
    fun `createStandardConfig returns correct configuration`() {
        // When
        val config = paginationConfig.createStandardConfig()

        // Then
        assertEquals(PaginationConfig.DEFAULT_PAGE_SIZE, config.pageSize)
        assertEquals(PaginationConfig.DEFAULT_PREFETCH_DISTANCE, config.prefetchDistance)
        assertEquals(PaginationConfig.DEFAULT_INITIAL_LOAD_SIZE, config.initialLoadSize)
        assertEquals(PaginationConfig.DEFAULT_MAX_SIZE, config.maxSize)
        assertEquals(PaginationConfig.DEFAULT_ENABLE_PLACEHOLDERS, config.enablePlaceholders)
    }

    @Test
    fun `createSearchConfig returns optimized search configuration`() {
        // When
        val config = paginationConfig.createSearchConfig()

        // Then
        assertEquals(15, config.pageSize)
        assertEquals(5, config.prefetchDistance)
        assertEquals(30, config.initialLoadSize)
        assertEquals(150, config.maxSize)
        assertFalse(config.enablePlaceholders) // Disabled for search
    }

    @Test
    fun `createLowMemoryConfig returns memory-optimized configuration`() {
        // When
        val config = paginationConfig.createLowMemoryConfig()

        // Then
        assertEquals(10, config.pageSize)
        assertEquals(5, config.prefetchDistance)
        assertEquals(20, config.initialLoadSize)
        assertEquals(100, config.maxSize)
        assertTrue(config.enablePlaceholders)
    }

    @Test
    fun `createAdaptiveConfig returns low memory config for low-end device`() {
        // When
        val config = paginationConfig.createAdaptiveConfig(
            availableMemoryMB = 1024,
            isLowEndDevice = true
        )

        // Then
        assertEquals(10, config.pageSize) // Low memory config
        assertEquals(5, config.prefetchDistance)
        assertEquals(20, config.initialLoadSize)
        assertEquals(100, config.maxSize)
    }

    @Test
    fun `createAdaptiveConfig returns low memory config for insufficient memory`() {
        // When
        val config = paginationConfig.createAdaptiveConfig(
            availableMemoryMB = 256,
            isLowEndDevice = false
        )

        // Then
        assertEquals(10, config.pageSize) // Low memory config due to insufficient RAM
        assertEquals(5, config.prefetchDistance)
        assertEquals(20, config.initialLoadSize)
        assertEquals(100, config.maxSize)
    }

    @Test
    fun `createAdaptiveConfig returns medium config for moderate memory`() {
        // When
        val config = paginationConfig.createAdaptiveConfig(
            availableMemoryMB = 768,
            isLowEndDevice = false
        )

        // Then
        assertEquals(15, config.pageSize) // Medium config
        assertEquals(8, config.prefetchDistance)
        assertEquals(30, config.initialLoadSize)
        assertEquals(150, config.maxSize)
    }

    @Test
    fun `createAdaptiveConfig returns standard config for high memory`() {
        // When
        val config = paginationConfig.createAdaptiveConfig(
            availableMemoryMB = 2048,
            isLowEndDevice = false
        )

        // Then
        assertEquals(PaginationConfig.DEFAULT_PAGE_SIZE, config.pageSize) // Standard config
        assertEquals(PaginationConfig.DEFAULT_PREFETCH_DISTANCE, config.prefetchDistance)
        assertEquals(PaginationConfig.DEFAULT_INITIAL_LOAD_SIZE, config.initialLoadSize)
        assertEquals(PaginationConfig.DEFAULT_MAX_SIZE, config.maxSize)
    }
}

class PaginationPerformanceMonitorTest {

    private lateinit var monitor: PaginationPerformanceMonitor

    @Before
    fun setup() {
        monitor = PaginationPerformanceMonitor()
    }

    @Test
    fun `getMetrics returns correct averages`() {
        // Given
        monitor.recordLoadTime(100)
        monitor.recordLoadTime(200)
        monitor.recordLoadTime(300)
        monitor.recordCacheHit()
        monitor.recordCacheHit()
        monitor.recordCacheMiss()
        monitor.recordPageLoad()
        monitor.recordPageLoad()
        monitor.recordError()

        // When
        val metrics = monitor.getMetrics()

        // Then
        assertEquals(200L, metrics.averageLoadTime) // (100 + 200 + 300) / 3
        assertEquals(0.67f, metrics.cacheHitRatio, 0.01f) // 2 hits / 3 total
        assertEquals(2, metrics.totalPagesLoaded)
        assertEquals(0.5f, metrics.errorRate) // 1 error / 2 pages
    }

    @Test
    fun `getMetrics handles empty state`() {
        // When
        val metrics = monitor.getMetrics()

        // Then
        assertEquals(0L, metrics.averageLoadTime)
        assertEquals(0f, metrics.cacheHitRatio)
        assertEquals(0, metrics.totalPagesLoaded)
        assertEquals(0f, metrics.errorRate)
    }

    @Test
    fun `reset clears all metrics`() {
        // Given
        monitor.recordLoadTime(100)
        monitor.recordCacheHit()
        monitor.recordPageLoad()
        monitor.recordError()

        // When
        monitor.reset()
        val metrics = monitor.getMetrics()

        // Then
        assertEquals(0L, metrics.averageLoadTime)
        assertEquals(0f, metrics.cacheHitRatio)
        assertEquals(0, metrics.totalPagesLoaded)
        assertEquals(0f, metrics.errorRate)
    }

    @Test
    fun `recordLoadTime maintains rolling window`() {
        // Given - Record more than 100 load times
        repeat(150) { index ->
            monitor.recordLoadTime(index.toLong())
        }

        // When
        val metrics = monitor.getMetrics()

        // Then - Should only keep the last 100 measurements
        // Average should be around (50 + 149) / 2 = 99.5 for the last 100 values
        assertTrue(metrics.averageLoadTime > 90)
        assertTrue(metrics.averageLoadTime < 110)
    }
}