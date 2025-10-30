package com.voicenotesai.presentation.animations

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.max

/**
 * Performance-adaptive animation system that monitors device capabilities
 * and adjusts animation quality to maintain 60fps performance.
 * 
 * Requirements addressed:
 * - 3.6: Maintain 60fps performance through adaptive quality settings
 * - 3.6: Performance-adaptive animation quality settings
 */
interface PerformanceAdaptiveAnimationManager {
    /**
     * Gets current device capabilities
     */
    fun getDeviceCapabilities(): DeviceCapabilities
    
    /**
     * Gets current animation quality configuration
     */
    fun getCurrentAnimationQuality(): StateFlow<AnimationQualityConfig>
    
    /**
     * Updates animation quality based on current performance metrics
     */
    suspend fun updateAnimationQuality(performanceMetrics: PerformanceMetrics)
    
    /**
     * Creates an optimized animation spec based on current quality settings
     */
    fun createOptimizedAnimationSpec(
        baseDuration: Int,
        animationType: AnimationType = AnimationType.Standard
    ): AnimationSpec<Float>
    
    /**
     * Checks if a specific animation feature should be enabled
     */
    fun shouldEnableFeature(feature: AnimationFeature): Boolean
    
    /**
     * Gets the maximum number of concurrent animations allowed
     */
    fun getMaxConcurrentAnimations(): Int
}

/**
 * Default implementation of performance-adaptive animation manager
 */
@Stable
class DefaultPerformanceAdaptiveAnimationManager(
    private val context: Context,
    private val animationEngine: AnimationEngine
) : PerformanceAdaptiveAnimationManager {
    
    private val _currentQuality = MutableStateFlow(
        animationEngine.adaptAnimationQuality(getDeviceCapabilities())
    )
    
    private val performanceMonitor = PerformanceMonitor()
    private var lastQualityUpdate = 0L
    private val qualityUpdateInterval = 5000L // 5 seconds
    
    override fun getDeviceCapabilities(): DeviceCapabilities {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        return DeviceCapabilities(
            totalMemoryMB = getTotalMemoryMB(),
            availableMemoryMB = memoryInfo.availMem / (1024 * 1024),
            cpuCores = Runtime.getRuntime().availableProcessors(),
            gpuTier = determineGpuTier(),
            batteryLevel = getBatteryLevel(),
            thermalState = getThermalState()
        )
    }
    
    override fun getCurrentAnimationQuality(): StateFlow<AnimationQualityConfig> {
        return _currentQuality.asStateFlow()
    }
    
    override suspend fun updateAnimationQuality(performanceMetrics: PerformanceMetrics) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastQualityUpdate < qualityUpdateInterval) return
        
        val deviceCapabilities = getDeviceCapabilities()
        val shouldDowngrade = shouldDowngradeQuality(performanceMetrics, deviceCapabilities)
        val shouldUpgrade = shouldUpgradeQuality(performanceMetrics, deviceCapabilities)
        
        val currentQuality = _currentQuality.value.quality
        val newQuality = when {
            shouldDowngrade -> when (currentQuality) {
                AnimationQuality.High -> AnimationQuality.Medium
                AnimationQuality.Medium -> AnimationQuality.Low
                AnimationQuality.Low -> AnimationQuality.Low
            }
            shouldUpgrade -> when (currentQuality) {
                AnimationQuality.Low -> AnimationQuality.Medium
                AnimationQuality.Medium -> AnimationQuality.High
                AnimationQuality.High -> AnimationQuality.High
            }
            else -> currentQuality
        }
        
        if (newQuality != currentQuality) {
            val newConfig = animationEngine.adaptAnimationQuality(
                deviceCapabilities.copy(
                    // Adjust capabilities based on performance
                    availableMemoryMB = max(
                        deviceCapabilities.availableMemoryMB - performanceMetrics.memoryPressure,
                        1000L
                    )
                )
            ).copy(quality = newQuality)
            
            _currentQuality.value = newConfig
            lastQualityUpdate = currentTime
        }
    }
    
    override fun createOptimizedAnimationSpec(
        baseDuration: Int,
        animationType: AnimationType
    ): AnimationSpec<Float> {
        val quality = _currentQuality.value.quality
        val durationMultiplier = when (quality) {
            AnimationQuality.High -> 1.0f
            AnimationQuality.Medium -> 0.8f
            AnimationQuality.Low -> 0.6f
        }
        
        val adjustedDuration = (baseDuration * durationMultiplier).toInt()
        
        return when (animationType) {
            AnimationType.Standard -> when (quality) {
                AnimationQuality.High -> spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
                AnimationQuality.Medium -> tween(
                    durationMillis = adjustedDuration,
                    easing = FastOutSlowInEasing
                )
                AnimationQuality.Low -> tween(
                    durationMillis = adjustedDuration,
                    easing = LinearEasing
                )
            }
            
            AnimationType.Entrance -> when (quality) {
                AnimationQuality.High -> spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
                AnimationQuality.Medium -> tween(
                    durationMillis = adjustedDuration,
                    easing = FastOutSlowInEasing
                )
                AnimationQuality.Low -> tween(
                    durationMillis = adjustedDuration,
                    easing = LinearEasing
                )
            }
            
            AnimationType.Exit -> when (quality) {
                AnimationQuality.High -> spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
                AnimationQuality.Medium -> tween(
                    durationMillis = (adjustedDuration * 0.7f).toInt(),
                    easing = FastOutSlowInEasing
                )
                AnimationQuality.Low -> tween(
                    durationMillis = (adjustedDuration * 0.5f).toInt(),
                    easing = LinearEasing
                )
            }
            
            AnimationType.Shared -> when (quality) {
                AnimationQuality.High -> spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
                AnimationQuality.Medium -> tween(
                    durationMillis = adjustedDuration,
                    easing = FastOutSlowInEasing
                )
                AnimationQuality.Low -> tween(
                    durationMillis = (adjustedDuration * 0.8f).toInt(),
                    easing = LinearEasing
                )
            }
            
            AnimationType.Micro -> when (quality) {
                AnimationQuality.High -> spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
                AnimationQuality.Medium -> tween(
                    durationMillis = (adjustedDuration * 0.6f).toInt(),
                    easing = FastOutSlowInEasing
                )
                AnimationQuality.Low -> tween(
                    durationMillis = (adjustedDuration * 0.4f).toInt(),
                    easing = LinearEasing
                )
            }
        }
    }
    
    override fun shouldEnableFeature(feature: AnimationFeature): Boolean {
        val quality = _currentQuality.value
        return when (feature) {
            AnimationFeature.ComplexTransitions -> quality.enableComplexAnimations
            AnimationFeature.ParticleEffects -> quality.enableParticleEffects
            AnimationFeature.SharedElements -> quality.quality != AnimationQuality.Low
            AnimationFeature.MicroInteractions -> true // Always enabled
            AnimationFeature.BackgroundAnimations -> quality.quality == AnimationQuality.High
            AnimationFeature.AdvancedEasing -> quality.quality != AnimationQuality.Low
            AnimationFeature.MultiLayerAnimations -> quality.quality == AnimationQuality.High
            AnimationFeature.RealTimeEffects -> quality.quality == AnimationQuality.High
        }
    }
    
    override fun getMaxConcurrentAnimations(): Int {
        return _currentQuality.value.maxConcurrentAnimations
    }
    
    private fun shouldDowngradeQuality(
        metrics: PerformanceMetrics,
        capabilities: DeviceCapabilities
    ): Boolean {
        return metrics.averageFps < 45f ||
                metrics.memoryPressure > capabilities.availableMemoryMB * 0.7f ||
                metrics.cpuUsage > 80f ||
                capabilities.batteryLevel < 0.2f ||
                capabilities.thermalState == ThermalState.Hot
    }
    
    private fun shouldUpgradeQuality(
        metrics: PerformanceMetrics,
        capabilities: DeviceCapabilities
    ): Boolean {
        return metrics.averageFps > 55f &&
                metrics.memoryPressure < capabilities.availableMemoryMB * 0.3f &&
                metrics.cpuUsage < 50f &&
                capabilities.batteryLevel > 0.5f &&
                capabilities.thermalState == ThermalState.Normal
    }
    
    private fun getTotalMemoryMB(): Long {
        return try {
            val memInfo = File("/proc/meminfo")
            val reader = RandomAccessFile(memInfo, "r")
            val line = reader.readLine()
            reader.close()
            
            val memTotal = line.split("\\s+".toRegex())[1].toLong()
            memTotal / 1024 // Convert KB to MB
        } catch (e: Exception) {
            4096L // Default fallback
        }
    }
    
    private fun determineGpuTier(): GpuTier {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> GpuTier.High
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> GpuTier.Medium
            else -> GpuTier.Low
        }
    }
    
    private fun getBatteryLevel(): Float {
        // Simplified battery level detection
        return 1.0f // Would implement actual battery level detection in production
    }
    
    private fun getThermalState(): ThermalState {
        // Simplified thermal state detection
        return ThermalState.Normal // Would implement actual thermal monitoring in production
    }
}

/**
 * Types of animations for different optimization strategies
 */
enum class AnimationType {
    Standard,    // Regular UI animations
    Entrance,    // Entry animations
    Exit,        // Exit animations
    Shared,      // Shared element transitions
    Micro        // Micro-interactions
}

/**
 * Animation features that can be enabled/disabled based on performance
 */
enum class AnimationFeature {
    ComplexTransitions,
    ParticleEffects,
    SharedElements,
    MicroInteractions,
    BackgroundAnimations,
    AdvancedEasing,
    MultiLayerAnimations,
    RealTimeEffects
}

/**
 * Performance metrics for animation quality decisions
 */
@Stable
data class PerformanceMetrics(
    val averageFps: Float,
    val memoryPressure: Long, // MB
    val cpuUsage: Float, // Percentage
    val frameDrops: Int,
    val animationCount: Int
)

/**
 * Performance monitor for tracking animation performance
 */
@Stable
class PerformanceMonitor {
    private val frameTimeHistory = mutableListOf<Long>()
    private val maxHistorySize = 60 // Track last 60 frames
    private var lastFrameTime = 0L
    private var frameDropCount = 0
    private var activeAnimationCount = 0
    
    fun onFrameStart() {
        val currentTime = System.nanoTime()
        if (lastFrameTime != 0L) {
            val frameTime = currentTime - lastFrameTime
            frameTimeHistory.add(frameTime)
            
            if (frameTimeHistory.size > maxHistorySize) {
                frameTimeHistory.removeAt(0)
            }
            
            // Detect frame drops (>16.67ms for 60fps)
            if (frameTime > 16_670_000L) {
                frameDropCount++
            }
        }
        lastFrameTime = currentTime
    }
    
    fun onAnimationStart() {
        activeAnimationCount++
    }
    
    fun onAnimationEnd() {
        activeAnimationCount = maxOf(0, activeAnimationCount - 1)
    }
    
    fun getCurrentMetrics(): PerformanceMetrics {
        val averageFrameTime = if (frameTimeHistory.isNotEmpty()) {
            frameTimeHistory.average()
        } else {
            16_670_000.0 // Default to 60fps
        }
        
        val averageFps = 1_000_000_000.0 / averageFrameTime
        
        return PerformanceMetrics(
            averageFps = averageFps.toFloat(),
            memoryPressure = getMemoryPressure(),
            cpuUsage = getCpuUsage(),
            frameDrops = frameDropCount,
            animationCount = activeAnimationCount
        )
    }
    
    fun reset() {
        frameTimeHistory.clear()
        frameDropCount = 0
        lastFrameTime = 0L
    }
    
    private fun getMemoryPressure(): Long {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return usedMemory / (1024 * 1024) // Convert to MB
    }
    
    private fun getCpuUsage(): Float {
        // Simplified CPU usage estimation
        // In production, would use proper CPU monitoring
        return when (activeAnimationCount) {
            0 -> 10f
            in 1..3 -> 30f
            in 4..6 -> 50f
            in 7..10 -> 70f
            else -> 90f
        }
    }
}

/**
 * Animation quality controller that manages quality transitions
 */
@Stable
class AnimationQualityController(
    private val performanceManager: PerformanceAdaptiveAnimationManager
) {
    private val performanceMonitor = PerformanceMonitor()
    
    suspend fun startPerformanceMonitoring() {
        // Would implement continuous performance monitoring
        // This is a simplified version for the core structure
    }
    
    fun onAnimationStart() {
        performanceMonitor.onAnimationStart()
    }
    
    fun onAnimationEnd() {
        performanceMonitor.onAnimationEnd()
    }
    
    fun onFrame() {
        performanceMonitor.onFrameStart()
    }
    
    suspend fun updateQualityIfNeeded() {
        val metrics = performanceMonitor.getCurrentMetrics()
        performanceManager.updateAnimationQuality(metrics)
    }
}

/**
 * Composable function to create and remember a performance-adaptive animation manager
 */
@Composable
fun rememberPerformanceAdaptiveAnimationManager(
    animationEngine: AnimationEngine = remember { DefaultAnimationEngine() }
): PerformanceAdaptiveAnimationManager {
    val context = LocalContext.current
    return remember(context, animationEngine) {
        DefaultPerformanceAdaptiveAnimationManager(context, animationEngine)
    }
}

/**
 * Composable function to observe current animation quality
 */
@Composable
fun rememberAnimationQuality(
    performanceManager: PerformanceAdaptiveAnimationManager = rememberPerformanceAdaptiveAnimationManager()
): AnimationQualityConfig {
    val quality by performanceManager.getCurrentAnimationQuality().collectAsState()
    return quality
}

/**
 * Composable function to create optimized animation specs
 */
@Composable
fun rememberOptimizedAnimationSpec(
    baseDuration: Int,
    animationType: AnimationType = AnimationType.Standard,
    performanceManager: PerformanceAdaptiveAnimationManager = rememberPerformanceAdaptiveAnimationManager()
): AnimationSpec<Float> {
    return remember(baseDuration, animationType, performanceManager) {
        performanceManager.createOptimizedAnimationSpec(baseDuration, animationType)
    }
}