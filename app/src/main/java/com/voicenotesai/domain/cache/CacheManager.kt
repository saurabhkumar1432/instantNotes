package com.voicenotesai.domain.cache

import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/**
 * Interface for managing intelligent caching with LRU eviction and compression.
 * Provides high-performance caching for notes and related data.
 */
interface CacheManager {
    /**
     * Cache data with specified policy
     */
    suspend fun cache(key: CacheKey, data: CacheableData, policy: CachePolicy): CacheResult

    /**
     * Retrieve cached data
     */
    suspend fun retrieve(key: CacheKey): CacheableData?

    /**
     * Invalidate cache entries matching pattern
     */
    suspend fun invalidate(pattern: CachePattern): InvalidationResult

    /**
     * Preload frequently accessed data
     */
    suspend fun preload(keys: List<CacheKey>): PreloadResult

    /**
     * Get real-time cache metrics
     */
    fun getMetrics(): Flow<CacheMetrics>

    /**
     * Clear all cache entries
     */
    suspend fun clearAll(): ClearResult

    /**
     * Optimize cache storage (compact, cleanup)
     */
    suspend fun optimize(): OptimizationResult
}

/**
 * Represents a cache key with type information
 */
data class CacheKey(
    val type: CacheType,
    val identifier: String,
    val version: Int = 1
) {
    fun toStringKey(): String = "${type.name}_${identifier}_v$version"
}

/**
 * Types of cacheable data
 */
enum class CacheType {
    NOTE_CONTENT,
    NOTE_METADATA,
    SEARCH_RESULTS,
    AI_PROCESSING_RESULT,
    AUDIO_TRANSCRIPTION,
    USER_PREFERENCES,
    ANALYTICS_DATA
}

/**
 * Base interface for cacheable data
 */
interface CacheableData {
    val size: Long
    val lastAccessed: Long
    val accessCount: Int
    fun compress(): ByteArray
    fun decompress(data: ByteArray): CacheableData
}

/**
 * Cache policy configuration
 */
data class CachePolicy(
    val ttl: Duration,
    val maxSize: Long,
    val evictionStrategy: EvictionStrategy,
    val compressionEnabled: Boolean = true,
    val priority: CachePriority = CachePriority.NORMAL
)

/**
 * Cache eviction strategies
 */
enum class EvictionStrategy {
    LRU,        // Least Recently Used
    LFU,        // Least Frequently Used
    FIFO,       // First In, First Out
    TTL_BASED   // Time To Live based
}

/**
 * Cache priority levels
 */
enum class CachePriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}

/**
 * Cache pattern for bulk operations
 */
sealed class CachePattern {
    data class ByType(val type: CacheType) : CachePattern()
    data class ByPrefix(val prefix: String) : CachePattern()
    data class ByRegex(val pattern: Regex) : CachePattern()
    object All : CachePattern()
}

/**
 * Result of cache operations
 */
sealed class CacheResult {
    object Success : CacheResult()
    data class Error(val message: String, val cause: Throwable? = null) : CacheResult()
    data class PartialSuccess(val successCount: Int, val failureCount: Int) : CacheResult()
}

/**
 * Result of cache invalidation
 */
data class InvalidationResult(
    val invalidatedCount: Int,
    val errors: List<String> = emptyList()
)

/**
 * Result of cache preloading
 */
data class PreloadResult(
    val loadedCount: Int,
    val failedCount: Int,
    val totalTime: Duration,
    val errors: List<String> = emptyList()
)

/**
 * Result of cache clearing
 */
data class ClearResult(
    val clearedCount: Int,
    val freedSpace: Long,
    val errors: List<String> = emptyList()
)

/**
 * Result of cache optimization
 */
data class OptimizationResult(
    val compactedEntries: Int,
    val freedSpace: Long,
    val optimizationTime: Duration,
    val errors: List<String> = emptyList()
)

/**
 * Real-time cache metrics
 */
data class CacheMetrics(
    val hitRate: Float,
    val missRate: Float,
    val totalEntries: Int,
    val totalSize: Long,
    val maxSize: Long,
    val evictionCount: Long,
    val compressionRatio: Float,
    val averageAccessTime: Duration,
    val memoryPressure: MemoryPressure,
    val performanceScore: Float
)

/**
 * Memory pressure levels
 */
enum class MemoryPressure {
    LOW,
    MODERATE,
    HIGH,
    CRITICAL
}