package com.voicenotesai.data.local

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.voicenotesai.data.local.dao.NotesDao
import com.voicenotesai.data.local.entity.Note
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Configuration and utilities for efficient pagination of large note datasets.
 * Optimized for smooth scrolling and memory efficiency with 10k+ notes.
 */
@Singleton
class PaginationConfig @Inject constructor(
    private val notesDao: NotesDao
) {
    
    companion object {
        // Optimal page size for note lists - balances memory usage and scroll performance
        const val DEFAULT_PAGE_SIZE = 20
        
        // Prefetch distance - how many items to load ahead of current position
        const val DEFAULT_PREFETCH_DISTANCE = 10
        
        // Initial load size - larger first load for better initial experience
        const val DEFAULT_INITIAL_LOAD_SIZE = 40
        
        // Maximum size in memory before dropping pages
        const val DEFAULT_MAX_SIZE = 200
        
        // Enable placeholders for consistent scroll bar behavior
        const val DEFAULT_ENABLE_PLACEHOLDERS = true
    }
    
    /**
     * Creates a standard paging configuration optimized for note lists.
     */
    fun createStandardConfig(): PagingConfig {
        return PagingConfig(
            pageSize = DEFAULT_PAGE_SIZE,
            prefetchDistance = DEFAULT_PREFETCH_DISTANCE,
            initialLoadSize = DEFAULT_INITIAL_LOAD_SIZE,
            maxSize = DEFAULT_MAX_SIZE,
            enablePlaceholders = DEFAULT_ENABLE_PLACEHOLDERS
        )
    }
    
    /**
     * Creates a paging configuration optimized for search results.
     * Uses smaller pages for faster search response.
     */
    fun createSearchConfig(): PagingConfig {
        return PagingConfig(
            pageSize = 15,
            prefetchDistance = 5,
            initialLoadSize = 30,
            maxSize = 150,
            enablePlaceholders = false // Disable for search to avoid empty spaces
        )
    }
    
    /**
     * Creates a paging configuration for low-memory devices.
     */
    fun createLowMemoryConfig(): PagingConfig {
        return PagingConfig(
            pageSize = 10,
            prefetchDistance = 5,
            initialLoadSize = 20,
            maxSize = 100,
            enablePlaceholders = true
        )
    }
    
    /**
     * Gets all notes with standard pagination.
     */
    fun getAllNotesPaged(): Flow<PagingData<Note>> {
        return Pager(
            config = createStandardConfig(),
            pagingSourceFactory = { notesDao.getAllNotesPaged() }
        ).flow
    }
    
    /**
     * Gets archived notes with pagination.
     */
    fun getArchivedNotesPaged(): Flow<PagingData<Note>> {
        return Pager(
            config = createStandardConfig(),
            pagingSourceFactory = { notesDao.getArchivedNotesPaged() }
        ).flow
    }
    
    /**
     * Gets notes by category with pagination.
     */
    fun getNotesByCategoryPaged(category: String): Flow<PagingData<Note>> {
        return Pager(
            config = createStandardConfig(),
            pagingSourceFactory = { notesDao.getNotesByCategoryPaged(category) }
        ).flow
    }
    
    /**
     * Searches notes with optimized pagination for search results.
     */
    fun searchNotesPaged(query: String): Flow<PagingData<Note>> {
        return Pager(
            config = createSearchConfig(),
            pagingSourceFactory = { notesDao.searchNotesPaged(query) }
        ).flow
    }
    
    /**
     * Gets notes in date range with pagination.
     */
    fun getNotesInDateRangePaged(startTimestamp: Long, endTimestamp: Long): Flow<PagingData<Note>> {
        return Pager(
            config = createStandardConfig(),
            pagingSourceFactory = { notesDao.getNotesInDateRangePaged(startTimestamp, endTimestamp) }
        ).flow
    }
    
    /**
     * Creates adaptive paging configuration based on device capabilities.
     */
    fun createAdaptiveConfig(
        availableMemoryMB: Long,
        isLowEndDevice: Boolean
    ): PagingConfig {
        return when {
            isLowEndDevice || availableMemoryMB < 512 -> createLowMemoryConfig()
            availableMemoryMB < 1024 -> PagingConfig(
                pageSize = 15,
                prefetchDistance = 8,
                initialLoadSize = 30,
                maxSize = 150,
                enablePlaceholders = true
            )
            else -> createStandardConfig()
        }
    }
}

/**
 * Extension functions for common pagination operations.
 */
object PaginationExtensions {
    
    /**
     * Creates a pager with custom configuration.
     */
    fun NotesDao.createPager(
        config: PagingConfig,
        sourceFactory: () -> androidx.paging.PagingSource<Int, Note>
    ): Flow<PagingData<Note>> {
        return Pager(
            config = config,
            pagingSourceFactory = sourceFactory
        ).flow
    }
}

/**
 * Performance monitoring for pagination operations.
 */
data class PaginationMetrics(
    val averageLoadTime: Long,
    val cacheHitRatio: Float,
    val memoryUsageMB: Float,
    val totalPagesLoaded: Int,
    val errorRate: Float
)

/**
 * Pagination performance monitor.
 */
class PaginationPerformanceMonitor {
    private val loadTimes = mutableListOf<Long>()
    private var cacheHits = 0
    private var cacheMisses = 0
    private var totalPages = 0
    private var errors = 0
    
    fun recordLoadTime(timeMs: Long) {
        loadTimes.add(timeMs)
        if (loadTimes.size > 100) {
            loadTimes.removeAt(0) // Keep only recent measurements
        }
    }
    
    fun recordCacheHit() {
        cacheHits++
    }
    
    fun recordCacheMiss() {
        cacheMisses++
    }
    
    fun recordPageLoad() {
        totalPages++
    }
    
    fun recordError() {
        errors++
    }
    
    fun getMetrics(): PaginationMetrics {
        val avgLoadTime = if (loadTimes.isNotEmpty()) {
            loadTimes.average().toLong()
        } else 0L
        
        val totalCacheOperations = cacheHits + cacheMisses
        val cacheHitRatio = if (totalCacheOperations > 0) {
            cacheHits.toFloat() / totalCacheOperations
        } else 0f
        
        val errorRate = if (totalPages > 0) {
            errors.toFloat() / totalPages
        } else 0f
        
        return PaginationMetrics(
            averageLoadTime = avgLoadTime,
            cacheHitRatio = cacheHitRatio,
            memoryUsageMB = 0f, // Would need actual memory measurement
            totalPagesLoaded = totalPages,
            errorRate = errorRate
        )
    }
    
    fun reset() {
        loadTimes.clear()
        cacheHits = 0
        cacheMisses = 0
        totalPages = 0
        errors = 0
    }
}