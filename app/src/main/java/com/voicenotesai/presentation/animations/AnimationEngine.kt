package com.voicenotesai.presentation.animations

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * Advanced animation engine providing shared element transitions, micro-interactions,
 * and performance-adaptive animation quality settings.
 * 
 * Requirements addressed:
 * - 1.1: Fluid, animated onboarding experience with micro-interactions
 * - 1.5: Seamless shared element transitions and motion design
 * - 3.6: Maintain 60fps performance through adaptive quality settings
 */
interface AnimationEngine {
    /**
     * Creates shared element transition configuration
     */
    fun createSharedElementTransition(
        key: String,
        animationSpec: AnimationSpec<Float>? = null
    ): SharedElementTransitionConfig
    
    /**
     * Generates micro-interactions for UI components
     */
    fun createMicroInteraction(
        type: MicroInteractionType,
        intensity: Float = 1.0f
    ): MicroInteractionConfig
    
    /**
     * Adapts animation quality based on device performance
     */
    fun adaptAnimationQuality(
        deviceCapabilities: DeviceCapabilities
    ): AnimationQualityConfig
    
    /**
     * Creates performance-optimized animation specs
     */
    fun createOptimizedAnimationSpec(
        duration: Int,
        quality: AnimationQuality = AnimationQuality.High
    ): AnimationSpec<Float>
}

/**
 * Default implementation of the AnimationEngine
 */
@Stable
class DefaultAnimationEngine : AnimationEngine {
    
    override fun createSharedElementTransition(
        key: String,
        animationSpec: AnimationSpec<Float>?
    ): SharedElementTransitionConfig {
        return SharedElementTransitionConfig(
            key = key,
            animationSpec = animationSpec ?: spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }
    
    override fun createMicroInteraction(
        type: MicroInteractionType,
        intensity: Float
    ): MicroInteractionConfig {
        return when (type) {
            MicroInteractionType.Tap -> MicroInteractionConfig.Tap(
                scaleDown = 0.96f * intensity,
                hapticType = HapticFeedbackType.LongPress,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            MicroInteractionType.LongPress -> MicroInteractionConfig.LongPress(
                scaleDown = 0.92f * intensity,
                hapticType = HapticFeedbackType.LongPress,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            MicroInteractionType.Hover -> MicroInteractionConfig.Hover(
                scaleUp = 1.02f * intensity,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            MicroInteractionType.Focus -> MicroInteractionConfig.Focus(
                glowIntensity = 0.8f * intensity,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
            MicroInteractionType.Success -> MicroInteractionConfig.Success(
                bounceScale = 1.1f * intensity,
                hapticType = HapticFeedbackType.LongPress,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            MicroInteractionType.Error -> MicroInteractionConfig.Error(
                shakeIntensity = 10f * intensity,
                hapticType = HapticFeedbackType.LongPress,
                animationSpec = tween(300)
            )
        }
    }
    
    override fun adaptAnimationQuality(
        deviceCapabilities: DeviceCapabilities
    ): AnimationQualityConfig {
        val quality = when {
            deviceCapabilities.isHighEnd() -> AnimationQuality.High
            deviceCapabilities.isMidRange() -> AnimationQuality.Medium
            else -> AnimationQuality.Low
        }
        
        return AnimationQualityConfig(
            quality = quality,
            enableComplexAnimations = quality != AnimationQuality.Low,
            enableParticleEffects = quality == AnimationQuality.High,
            maxConcurrentAnimations = when (quality) {
                AnimationQuality.High -> 10
                AnimationQuality.Medium -> 6
                AnimationQuality.Low -> 3
            },
            frameRateTarget = when (quality) {
                AnimationQuality.High -> 60
                AnimationQuality.Medium -> 30
                AnimationQuality.Low -> 24
            }
        )
    }
    
    override fun createOptimizedAnimationSpec(
        duration: Int,
        quality: AnimationQuality
    ): AnimationSpec<Float> {
        return when (quality) {
            AnimationQuality.High -> spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
            AnimationQuality.Medium -> tween(
                durationMillis = duration,
                easing = FastOutSlowInEasing
            )
            AnimationQuality.Low -> tween(
                durationMillis = (duration * 0.7f).toInt(),
                easing = LinearEasing
            )
        }
    }
}

/**
 * Configuration for shared element transitions
 */
@Stable
data class SharedElementTransitionConfig(
    val key: String,
    val animationSpec: AnimationSpec<Float>
)

/**
 * Types of micro-interactions available
 */
enum class MicroInteractionType {
    Tap,
    LongPress,
    Hover,
    Focus,
    Success,
    Error
}

/**
 * Configuration for different micro-interactions
 */
@Stable
sealed class MicroInteractionConfig {
    data class Tap(
        val scaleDown: Float,
        val hapticType: HapticFeedbackType,
        val animationSpec: AnimationSpec<Float>
    ) : MicroInteractionConfig()
    
    data class LongPress(
        val scaleDown: Float,
        val hapticType: HapticFeedbackType,
        val animationSpec: AnimationSpec<Float>
    ) : MicroInteractionConfig()
    
    data class Hover(
        val scaleUp: Float,
        val animationSpec: AnimationSpec<Float>
    ) : MicroInteractionConfig()
    
    data class Focus(
        val glowIntensity: Float,
        val animationSpec: AnimationSpec<Float>
    ) : MicroInteractionConfig()
    
    data class Success(
        val bounceScale: Float,
        val hapticType: HapticFeedbackType,
        val animationSpec: AnimationSpec<Float>
    ) : MicroInteractionConfig()
    
    data class Error(
        val shakeIntensity: Float,
        val hapticType: HapticFeedbackType,
        val animationSpec: AnimationSpec<Float>
    ) : MicroInteractionConfig()
}

/**
 * Animation quality levels for performance adaptation
 */
enum class AnimationQuality {
    High,    // Full animations with complex effects
    Medium,  // Standard animations with reduced complexity
    Low      // Minimal animations for performance
}

/**
 * Device capabilities for performance assessment
 */
@Stable
data class DeviceCapabilities(
    val totalMemoryMB: Long,
    val availableMemoryMB: Long,
    val cpuCores: Int,
    val gpuTier: GpuTier,
    val batteryLevel: Float,
    val thermalState: ThermalState
) {
    fun isHighEnd(): Boolean = totalMemoryMB >= 6000 && cpuCores >= 8 && gpuTier == GpuTier.High
    fun isMidRange(): Boolean = totalMemoryMB >= 4000 && cpuCores >= 6 && gpuTier != GpuTier.Low
}

enum class GpuTier { Low, Medium, High }
enum class ThermalState { Normal, Warm, Hot, Critical }

/**
 * Configuration for animation quality settings
 */
@Stable
data class AnimationQualityConfig(
    val quality: AnimationQuality,
    val enableComplexAnimations: Boolean,
    val enableParticleEffects: Boolean,
    val maxConcurrentAnimations: Int,
    val frameRateTarget: Int
)

/**
 * Composable wrapper for shared element transitions
 * Note: This is a simplified implementation. Full shared element transitions
 * would require the experimental SharedTransition APIs when they become stable.
 */
@Composable
fun AnimatedSharedElement(
    config: SharedElementTransitionConfig,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Simplified implementation without experimental APIs
    Box(modifier = modifier) {
        content()
    }
}

/**
 * Modifier for applying micro-interactions to any composable
 */
fun Modifier.microInteraction(
    config: MicroInteractionConfig,
    enabled: Boolean = true,
    onInteraction: (() -> Unit)? = null
): Modifier = composed {
    if (!enabled) return@composed this
    
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    when (config) {
        is MicroInteractionConfig.Tap -> {
            val scale = remember { Animatable(1f) }
            
            LaunchedEffect(isPressed) {
                if (isPressed) {
                    scale.animateTo(config.scaleDown, config.animationSpec)
                    haptic.performHapticFeedback(config.hapticType)
                    onInteraction?.invoke()
                } else {
                    scale.animateTo(1f, config.animationSpec)
                }
            }
            
            this.scale(scale.value)
        }
        
        is MicroInteractionConfig.LongPress -> {
            val scale = remember { Animatable(1f) }
            var isLongPressed by remember { mutableStateOf(false) }
            
            this
                .scale(scale.value)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            isLongPressed = true
                            haptic.performHapticFeedback(config.hapticType)
                            onInteraction?.invoke()
                        },
                        onPress = {
                            scale.animateTo(config.scaleDown, config.animationSpec)
                            tryAwaitRelease()
                            scale.animateTo(1f, config.animationSpec)
                            isLongPressed = false
                        }
                    )
                }
        }
        
        is MicroInteractionConfig.Success -> {
            val scale = remember { Animatable(1f) }
            
            LaunchedEffect(Unit) {
                haptic.performHapticFeedback(config.hapticType)
                scale.animateTo(config.bounceScale, config.animationSpec)
                scale.animateTo(1f, config.animationSpec)
                onInteraction?.invoke()
            }
            
            this.scale(scale.value)
        }
        
        is MicroInteractionConfig.Error -> {
            val offsetX = remember { Animatable(0f) }
            
            LaunchedEffect(Unit) {
                haptic.performHapticFeedback(config.hapticType)
                repeat(3) {
                    offsetX.animateTo(config.shakeIntensity, config.animationSpec)
                    offsetX.animateTo(-config.shakeIntensity, config.animationSpec)
                }
                offsetX.animateTo(0f, config.animationSpec)
                onInteraction?.invoke()
            }
            
            this.graphicsLayer { translationX = offsetX.value }
        }
        
        else -> this
    }
}

/**
 * Advanced animation effects for enhanced user experience
 */
object AdvancedAnimationEffects {
    
    /**
     * Creates a breathing animation effect
     */
    @Composable
    fun breathingEffect(
        enabled: Boolean = true,
        minScale: Float = 0.98f,
        maxScale: Float = 1.02f,
        duration: Int = 2000
    ): Float {
        if (!enabled) return 1f
        
        val infiniteTransition = rememberInfiniteTransition(label = "breathing")
        val scale by infiniteTransition.animateFloat(
            initialValue = minScale,
            targetValue = maxScale,
            animationSpec = infiniteRepeatable(
                animation = tween(duration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathing-scale"
        )
        return scale
    }
    
    /**
     * Creates a wave animation effect
     */
    @Composable
    fun waveEffect(
        enabled: Boolean = true,
        amplitude: Float = 5f,
        frequency: Float = 1f,
        duration: Int = 2000
    ): Float {
        if (!enabled) return 0f
        
        val infiniteTransition = rememberInfiniteTransition(label = "wave")
        val time by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(duration, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wave-time"
        )
        
        return amplitude * sin(time * frequency * 2 * Math.PI).toFloat()
    }
    
    /**
     * Creates a particle burst effect
     */
    @Composable
    fun ParticleBurst(
        trigger: Boolean,
        particleCount: Int = 12,
        maxRadius: Dp = 100.dp,
        duration: Int = 800,
        modifier: Modifier = Modifier
    ) {
        if (!trigger) return
        
        Box(modifier = modifier.fillMaxSize()) {
            repeat(particleCount) { index ->
                val angle = (360f / particleCount) * index
                val animatable = remember { Animatable(0f) }
                
                LaunchedEffect(trigger) {
                    animatable.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(duration, easing = FastOutSlowInEasing)
                    )
                    delay(100)
                    animatable.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(200)
                    )
                }
                
                // Individual particle implementation would go here
                // This is a simplified version for the core structure
            }
        }
    }
}

/**
 * Performance monitoring for animations
 */
@Stable
class AnimationPerformanceMonitor {
    private var frameCount = 0
    private var lastFrameTime = 0L
    private var averageFrameTime = 0f
    
    fun onFrame() {
        val currentTime = System.nanoTime()
        if (lastFrameTime != 0L) {
            val frameTime = (currentTime - lastFrameTime) / 1_000_000f // Convert to ms
            averageFrameTime = (averageFrameTime * frameCount + frameTime) / (frameCount + 1)
            frameCount++
        }
        lastFrameTime = currentTime
    }
    
    fun getCurrentFPS(): Float {
        return if (averageFrameTime > 0) 1000f / averageFrameTime else 0f
    }
    
    fun reset() {
        frameCount = 0
        lastFrameTime = 0L
        averageFrameTime = 0f
    }
}

/**
 * Creates a performance monitor for tracking animation performance
 */
@Composable
fun rememberAnimationPerformanceMonitor(): AnimationPerformanceMonitor {
    return remember { AnimationPerformanceMonitor() }
}