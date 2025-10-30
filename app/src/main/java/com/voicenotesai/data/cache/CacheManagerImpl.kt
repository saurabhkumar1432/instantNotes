package com.voicenotesai.data.cache

import com.voicenotesai.domain.cache.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * High-performance cache manager implementation with LRU eviction and compression
 */
class CacheManagerImpl(
    private val maxMemorySize: Long = 50 * 1024 * 1024, // 50MB default
    private val preloadingStrategy: PreloadingStrategy,
    private val accessTracker: AccessTracker
) : CacheManager {

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val accessOrder = LinkedHashMap<String, Long>()
    private val cacheMutex = Mutex()
    
    private val _metrics = MutableStateFlow(
        CacheMetrics(
            hitRate = 0f,
            missRate = 0f,
            totalEntries = 0,
            totalSize = 0L,
            maxSize = maxMemorySize,
            evictionCount = 0L,
            compressionRatio = 0f,
            averageAccessTime = Duration.ZERO,
            memoryPressure = MemoryPressure.LOW,
            performanceScore = 1f
        )
    )

    private var totalHits = 0L
    private var totalMisses = 0L
    private var totalEvictions = 0L
    private val accessTimes = mutableListOf<Long>()

    override suspend fun cache(key: CacheKey, data: CacheableData, policy: CachePolicy): CacheResult {
        return try {
            cacheMutex.withLock {
                val stringKey = key.toStringKey()
                val compressedData = if (policy.compressionEnabled) data.compress() else null
                val entry = CacheEntry(
                    key = key,
                    data = data,
                    compressedData = compressedData,
                    policy = policy,
                    createdAt = System.currentTimeMillis(),
                    lastAccessed = System.currentTimeMillis(),
                    accessCount = 0
                )

                // Check if we need to evict entries
                val entrySize = compressedData?.size?.toLong() ?: data.size
                ensureCapacity(entrySize)

                cache[stringKey] = entry
                accessOrder[stringKey] = System.currentTimeMillis()
                
                updateMetrics()
                CacheResult.Success
            }
        } catch (e: Exception) {
            CacheResult.Error("Failed to cache data: ${e.message}", e)
        }
    }

    override suspend fun retrieve(key: CacheKey): CacheableData? {
        val startTime = System.currentTimeMillis()
        
        return try {
            cacheMutex.withLock {
                val stringKey = key.toStringKey()
                val entry = cache[stringKey]

                if (entry == null) {
                    totalMisses++
                    updateMetrics()
                    return null
                }

                // Check TTL
                if (isExpired(entry)) {
                    cache.remove(stringKey)
                    accessOrder.remove(stringKey)
                    totalMisses++
                    updateMetrics()
                    return null
                }

                // Update access information
                val updatedEntry = entry.copy(
                    lastAccessed = System.currentTimeMillis(),
                    accessCount = entry.accessCount + 1
                )
                cache[stringKey] = updatedEntry
                accessOrder[stringKey] = System.currentTimeMillis()

                // Record access for preloading strategy
                accessTracker.recordAccess(key, System.currentTimeMillis())

                totalHits++
                val accessTime = System.currentTimeMillis() - startTime
                accessTimes.add(accessTime)
                if (accessTimes.size > 1000) {
                    accessTimes.removeFirst()
                }

                updateMetrics()
                
                // Return decompressed data if compressed
                if (updatedEntry.compressedData != null) {
                    updatedEntry.data.decompress(updatedEntry.compressedData)
                } else {
                    updatedEntry.data
                }
            }
        } catch (e: Exception) {
            totalMisses++
            updateMetrics()
            null
        }
    }

    override suspend fun invalidate(pattern: CachePattern): InvalidationResult {
        return cacheMutex.withLock {
            val keysToRemove = mutableListOf<String>()
            val errors = mutableListOf<String>()

            cache.keys.forEach { stringKey ->
                try {
                    val shouldRemove = when (pattern) {
                        is CachePattern.ByType -> {
                            cache[stringKey]?.key?.type == pattern.type
                        }
                        is CachePattern.ByPrefix -> {
                            stringKey.startsWith(pattern.prefix)
                        }
                        is CachePattern.ByRegex -> {
                            pattern.pattern.matches(stringKey)
                        }
                        is CachePattern.All -> true
                    }

                    if (shouldRemove) {
                        keysToRemove.add(stringKey)
                    }
                } catch (e: Exception) {
                    errors.add("Error processing key $stringKey: ${e.message}")
                }
            }

            keysToRemove.forEach { key ->
                cache.remove(key)
                accessOrder.remove(key)
            }

            updateMetrics()
            InvalidationResult(keysToRemove.size, errors)
        }
    }

    override suspend fun preload(keys: List<CacheKey>): PreloadResult {
        val startTime = System.currentTimeMillis()
        var loadedCount = 0
        var failedCount = 0
        val errors = mutableListOf<String>()

        keys.forEach { key ->
            try {
                // Check if already cached
                if (retrieve(key) == null) {
                    // This would typically load from repository
                    // For now, we'll just track the attempt
                    loadedCount++
                }
            } catch (e: Exception) {
                failedCount++
                errors.add("Failed to preload ${key.toStringKey()}: ${e.message}")
            }
        }

        val totalTime = (System.currentTimeMillis() - startTime).milliseconds
        return PreloadResult(loadedCount, failedCount, totalTime, errors)
    }

    override fun getMetrics(): Flow<CacheMetrics> = _metrics.asStateFlow()

    override suspend fun clearAll(): ClearResult {
        return cacheMutex.withLock {
            val clearedCount = cache.size
            val freedSpace = cache.values.sumOf { entry ->
                entry.compressedData?.size?.toLong() ?: entry.data.size
            }

            cache.clear()
            accessOrder.clear()
            
            updateMetrics()
            ClearResult(clearedCount, freedSpace)
        }
    }

    override suspend fun optimize(): OptimizationResult {
        val startTime = System.currentTimeMillis()
        
        return cacheMutex.withLock {
            var compactedEntries = 0
            var freedSpace = 0L

            // Remove expired entries
            val expiredKeys = cache.entries.filter { (_, entry) ->
                isExpired(entry)
            }.map { it.key }

            expiredKeys.forEach { key ->
                cache[key]?.let { entry ->
                    freedSpace += entry.compressedData?.size?.toLong() ?: entry.data.size
                }
                cache.remove(key)
                accessOrder.remove(key)
                compactedEntries++
            }

            // Compress uncompressed entries if beneficial
            cache.entries.forEach { (key, entry) ->
                if (entry.compressedData == null && entry.policy.compressionEnabled) {
                    try {
                        val compressed = entry.data.compress()
                        if (compressed.size < entry.data.size) {
                            val updatedEntry = entry.copy(compressedData = compressed)
                            cache[key] = updatedEntry
                            freedSpace += entry.data.size - compressed.size
                            compactedEntries++
                        }
                    } catch (e: Exception) {
                        // Compression failed, continue
                    }
                }
            }

            updateMetrics()
            val optimizationTime = (System.currentTimeMillis() - startTime).milliseconds
            OptimizationResult(compactedEntries, freedSpace, optimizationTime)
        }
    }

    private fun ensureCapacity(requiredSize: Long) {
        var currentSize = getCurrentSize()
        
        while (currentSize + requiredSize > maxMemorySize && cache.isNotEmpty()) {
            // Find LRU entry
            val lruKey = accessOrder.entries.minByOrNull { it.value }?.key
            if (lruKey != null) {
                cache[lruKey]?.let { entry ->
                    currentSize -= entry.compressedData?.size?.toLong() ?: entry.data.size
                }
                cache.remove(lruKey)
                accessOrder.remove(lruKey)
                totalEvictions++
            } else {
                break
            }
        }
    }

    private fun getCurrentSize(): Long {
        return cache.values.sumOf { entry ->
            entry.compressedData?.size?.toLong() ?: entry.data.size
        }
    }

    private fun isExpired(entry: CacheEntry): Boolean {
        val now = System.currentTimeMillis()
        return now - entry.createdAt > entry.policy.ttl.inWholeMilliseconds
    }

    private fun updateMetrics() {
        val totalRequests = totalHits + totalMisses
        val hitRate = if (totalRequests > 0) totalHits.toFloat() / totalRequests else 0f
        val missRate = if (totalRequests > 0) totalMisses.toFloat() / totalRequests else 0f
        
        val currentSize = getCurrentSize()
        val memoryPressure = when {
            currentSize < maxMemorySize * 0.5 -> MemoryPressure.LOW
            currentSize < maxMemorySize * 0.7 -> MemoryPressure.MODERATE
            currentSize < maxMemorySize * 0.9 -> MemoryPressure.HIGH
            else -> MemoryPressure.CRITICAL
        }

        val compressionRatio = calculateCompressionRatio()
        val averageAccessTime = if (accessTimes.isNotEmpty()) {
            accessTimes.average().toLong().milliseconds
        } else {
            Duration.ZERO
        }

        val performanceScore = calculatePerformanceScore(hitRate, averageAccessTime, memoryPressure)

        _metrics.value = CacheMetrics(
            hitRate = hitRate,
            missRate = missRate,
            totalEntries = cache.size,
            totalSize = currentSize,
            maxSize = maxMemorySize,
            evictionCount = totalEvictions,
            compressionRatio = compressionRatio,
            averageAccessTime = averageAccessTime,
            memoryPressure = memoryPressure,
            performanceScore = performanceScore
        )
    }

    private fun calculateCompressionRatio(): Float {
        val entriesWithCompression = cache.values.filter { it.compressedData != null }
        if (entriesWithCompression.isEmpty()) return 0f

        val originalSize = entriesWithCompression.sumOf { it.data.size }
        val compressedSize = entriesWithCompression.sumOf { it.compressedData!!.size }

        return if (originalSize > 0) compressedSize.toFloat() / originalSize else 0f
    }

    private fun calculatePerformanceScore(
        hitRate: Float,
        averageAccessTime: Duration,
        memoryPressure: MemoryPressure
    ): Float {
        val hitRateScore = hitRate
        val accessTimeScore = when {
            averageAccessTime < 10.milliseconds -> 1f
            averageAccessTime < 50.milliseconds -> 0.8f
            averageAccessTime < 100.milliseconds -> 0.6f
            else -> 0.4f
        }
        val memoryScore = when (memoryPressure) {
            MemoryPressure.LOW -> 1f
            MemoryPressure.MODERATE -> 0.8f
            MemoryPressure.HIGH -> 0.6f
            MemoryPressure.CRITICAL -> 0.4f
        }

        return (hitRateScore * 0.4f + accessTimeScore * 0.3f + memoryScore * 0.3f)
    }
}

/**
 * Internal cache entry representation
 */
private data class CacheEntry(
    val key: CacheKey,
    val data: CacheableData,
    val compressedData: ByteArray? = null,
    val policy: CachePolicy,
    val createdAt: Long,
    val lastAccessed: Long,
    val accessCount: Int
)