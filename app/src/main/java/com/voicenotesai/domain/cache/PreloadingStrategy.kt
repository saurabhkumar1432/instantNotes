package com.voicenotesai.domain.cache

import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/**
 * Interface for intelligent preloading strategies
 */
interface PreloadingStrategy {
    /**
     * Determine which notes should be preloaded based on usage patterns
     */
    suspend fun getPreloadCandidates(): List<CacheKey>

    /**
     * Update usage patterns for learning
     */
    suspend fun recordAccess(key: CacheKey, accessTime: Long)

    /**
     * Get preloading recommendations based on current context
     */
    suspend fun getRecommendations(context: PreloadContext): List<PreloadRecommendation>
}

/**
 * Context information for preloading decisions
 */
data class PreloadContext(
    val currentTime: Long = System.currentTimeMillis(),
    val userActivity: UserActivity,
    val deviceState: DeviceState,
    val networkState: NetworkState,
    val batteryLevel: Float,
    val availableMemory: Long
)

/**
 * User activity patterns
 */
enum class UserActivity {
    BROWSING_NOTES,
    SEARCHING,
    RECORDING,
    EDITING,
    IDLE,
    BACKGROUND
}

/**
 * Device performance state
 */
data class DeviceState(
    val cpuUsage: Float,
    val memoryPressure: MemoryPressure,
    val thermalState: ThermalState,
    val storageAvailable: Long
)

/**
 * Thermal state of device
 */
enum class ThermalState {
    NORMAL,
    LIGHT,
    MODERATE,
    SEVERE,
    CRITICAL
}

/**
 * Network connectivity state
 */
data class NetworkState(
    val isConnected: Boolean,
    val connectionType: ConnectionType,
    val bandwidth: Long? = null
)

/**
 * Network connection types
 */
enum class ConnectionType {
    WIFI,
    CELLULAR,
    ETHERNET,
    NONE
}

/**
 * Preload recommendation with priority and reasoning
 */
data class PreloadRecommendation(
    val key: CacheKey,
    val priority: PreloadPriority,
    val confidence: Float,
    val reasoning: String,
    val estimatedBenefit: Duration
)

/**
 * Priority levels for preloading
 */
enum class PreloadPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

/**
 * Frequency-based preloading strategy
 */
class FrequencyBasedPreloadingStrategy(
    private val accessTracker: AccessTracker,
    private val maxPreloadItems: Int = 50
) : PreloadingStrategy {

    override suspend fun getPreloadCandidates(): List<CacheKey> {
        val frequentlyAccessed = accessTracker.getMostFrequentlyAccessed(maxPreloadItems)
        val recentlyAccessed = accessTracker.getRecentlyAccessed(maxPreloadItems / 2)
        
        return (frequentlyAccessed + recentlyAccessed)
            .distinctBy { it.toStringKey() }
            .take(maxPreloadItems)
    }

    override suspend fun recordAccess(key: CacheKey, accessTime: Long) {
        accessTracker.recordAccess(key, accessTime)
    }

    override suspend fun getRecommendations(context: PreloadContext): List<PreloadRecommendation> {
        val candidates = getPreloadCandidates()
        
        return candidates.mapNotNull { key ->
            val accessPattern = accessTracker.getAccessPattern(key)
            val priority = calculatePriority(accessPattern, context)
            val confidence = calculateConfidence(accessPattern)
            
            if (confidence > 0.3f) { // Only recommend if confidence is reasonable
                PreloadRecommendation(
                    key = key,
                    priority = priority,
                    confidence = confidence,
                    reasoning = generateReasoning(accessPattern, context),
                    estimatedBenefit = estimateBenefit(accessPattern)
                )
            } else null
        }.sortedByDescending { it.confidence }
    }

    private fun calculatePriority(pattern: AccessPattern, context: PreloadContext): PreloadPriority {
        return when {
            pattern.accessCount > 20 && pattern.averageInterval < Duration.parse("1h") -> PreloadPriority.URGENT
            pattern.accessCount > 10 && context.userActivity == UserActivity.BROWSING_NOTES -> PreloadPriority.HIGH
            pattern.accessCount > 5 -> PreloadPriority.MEDIUM
            else -> PreloadPriority.LOW
        }
    }

    private fun calculateConfidence(pattern: AccessPattern): Float {
        val frequencyScore = minOf(pattern.accessCount / 20f, 1f)
        val recencyScore = if (pattern.lastAccess > System.currentTimeMillis() - Duration.parse("24h").inWholeMilliseconds) 1f else 0.5f
        val consistencyScore = if (pattern.averageInterval > Duration.ZERO) 1f / (pattern.intervalVariance + 1f) else 0f
        
        return (frequencyScore * 0.4f + recencyScore * 0.3f + consistencyScore * 0.3f)
    }

    private fun generateReasoning(pattern: AccessPattern, context: PreloadContext): String {
        return buildString {
            append("Accessed ${pattern.accessCount} times")
            if (pattern.averageInterval < Duration.parse("1h")) {
                append(", frequently used")
            }
            if (pattern.lastAccess > System.currentTimeMillis() - Duration.parse("24h").inWholeMilliseconds) {
                append(", recently accessed")
            }
            if (context.userActivity == UserActivity.BROWSING_NOTES) {
                append(", user currently browsing")
            }
        }
    }

    private fun estimateBenefit(pattern: AccessPattern): Duration {
        // Estimate time saved based on access frequency and typical load time
        val baseLoadTime = Duration.parse("100ms")
        val frequencyMultiplier = minOf(pattern.accessCount / 10, 3).toDouble()
        return baseLoadTime * frequencyMultiplier
    }
}

/**
 * Tracks access patterns for cache keys
 */
interface AccessTracker {
    suspend fun recordAccess(key: CacheKey, accessTime: Long)
    suspend fun getAccessPattern(key: CacheKey): AccessPattern
    suspend fun getMostFrequentlyAccessed(limit: Int): List<CacheKey>
    suspend fun getRecentlyAccessed(limit: Int): List<CacheKey>
    fun getAccessHistory(): Flow<List<AccessRecord>>
}

/**
 * Access pattern analysis
 */
data class AccessPattern(
    val key: CacheKey,
    val accessCount: Int,
    val firstAccess: Long,
    val lastAccess: Long,
    val averageInterval: Duration,
    val intervalVariance: Float,
    val timeOfDayPattern: Map<Int, Int> // Hour -> access count
)

/**
 * Individual access record
 */
data class AccessRecord(
    val key: CacheKey,
    val accessTime: Long,
    val context: String? = null
)