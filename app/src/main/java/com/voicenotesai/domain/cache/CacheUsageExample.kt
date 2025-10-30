package com.voicenotesai.domain.cache

import com.voicenotesai.data.cache.AccessTrackerImpl
import com.voicenotesai.data.cache.CacheManagerImpl
import com.voicenotesai.data.local.entity.Note
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.hours

/**
 * Example demonstrating how to use the intelligent caching system
 */
class CacheUsageExample {

    suspend fun demonstrateCaching() {
        // Initialize the caching system
        val accessTracker = AccessTrackerImpl()
        val preloadingStrategy = FrequencyBasedPreloadingStrategy(accessTracker)
        val cacheManager = CacheManagerImpl(
            maxMemorySize = 50 * 1024 * 1024, // 50MB
            preloadingStrategy = preloadingStrategy,
            accessTracker = accessTracker
        )

        // Create a sample note
        val note = Note(
            id = 1,
            content = "This is a sample note for caching demonstration",
            timestamp = System.currentTimeMillis(),
            transcribedText = "Transcribed version of the note"
        )

        // Define cache key and policy
        val cacheKey = CacheKey(CacheType.NOTE_CONTENT, note.id.toString())
        val cachePolicy = CachePolicy(
            ttl = 24.hours,
            maxSize = 1024 * 1024, // 1MB
            evictionStrategy = EvictionStrategy.LRU,
            compressionEnabled = true,
            priority = CachePriority.HIGH
        )

        // Cache the note
        val cacheableNote = CacheableNote(note)
        val cacheResult = cacheManager.cache(cacheKey, cacheableNote, cachePolicy)
        
        println("Cache result: $cacheResult")

        // Retrieve the note (this will be a cache hit)
        val retrievedData = cacheManager.retrieve(cacheKey)
        if (retrievedData is CacheableNote) {
            println("Retrieved note: ${retrievedData.note.content}")
        }

        // Get cache metrics
        val metrics = cacheManager.getMetrics().first()
        println("Cache metrics:")
        println("  Hit rate: ${metrics.hitRate}")
        println("  Total entries: ${metrics.totalEntries}")
        println("  Total size: ${metrics.totalSize} bytes")
        println("  Compression ratio: ${metrics.compressionRatio}")
        println("  Performance score: ${metrics.performanceScore}")

        // Demonstrate preloading
        val context = PreloadContext(
            userActivity = UserActivity.BROWSING_NOTES,
            deviceState = DeviceState(
                cpuUsage = 0.3f,
                memoryPressure = MemoryPressure.LOW,
                thermalState = ThermalState.NORMAL,
                storageAvailable = 1024 * 1024 * 1024 // 1GB
            ),
            networkState = NetworkState(
                isConnected = true,
                connectionType = ConnectionType.WIFI
            ),
            batteryLevel = 0.8f,
            availableMemory = 512 * 1024 * 1024 // 512MB
        )

        val recommendations = preloadingStrategy.getRecommendations(context)
        println("Preload recommendations: ${recommendations.size}")
        recommendations.forEach { recommendation ->
            println("  ${recommendation.key.toStringKey()}: ${recommendation.priority} (${recommendation.confidence})")
        }

        // Demonstrate cache optimization
        val optimizationResult = cacheManager.optimize()
        println("Optimization result:")
        println("  Compacted entries: ${optimizationResult.compactedEntries}")
        println("  Freed space: ${optimizationResult.freedSpace} bytes")
        println("  Optimization time: ${optimizationResult.optimizationTime}")
    }

    suspend fun demonstrateSearchCaching() {
        val accessTracker = AccessTrackerImpl()
        val preloadingStrategy = FrequencyBasedPreloadingStrategy(accessTracker)
        val cacheManager = CacheManagerImpl(
            maxMemorySize = 50 * 1024 * 1024,
            preloadingStrategy = preloadingStrategy,
            accessTracker = accessTracker
        )

        // Simulate search results
        val searchQuery = "meeting notes"
        val searchResults = listOf(
            Note(1, "Meeting with team about project", System.currentTimeMillis()),
            Note(2, "Notes from client meeting", System.currentTimeMillis()),
            Note(3, "Meeting agenda for next week", System.currentTimeMillis())
        )

        // Cache search results
        val searchCacheKey = CacheKey(CacheType.SEARCH_RESULTS, searchQuery.hashCode().toString())
        val cacheableSearchResults = CacheableSearchResults(
            query = searchQuery,
            results = searchResults,
            totalCount = searchResults.size
        )

        val searchCachePolicy = CachePolicy(
            ttl = 30.hours, // Search results expire faster
            maxSize = 512 * 1024, // 512KB
            evictionStrategy = EvictionStrategy.LRU,
            compressionEnabled = true,
            priority = CachePriority.NORMAL
        )

        cacheManager.cache(searchCacheKey, cacheableSearchResults, searchCachePolicy)

        // Retrieve search results
        val cachedSearchResults = cacheManager.retrieve(searchCacheKey)
        if (cachedSearchResults is CacheableSearchResults) {
            println("Cached search results for '$searchQuery':")
            cachedSearchResults.results.forEach { note ->
                println("  - ${note.content}")
            }
        }
    }
}