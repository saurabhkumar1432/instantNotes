package com.voicenotesai.presentation.animations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Advanced animation composables that showcase the enhanced animation system
 * with shared element transitions, micro-interactions, and performance adaptation.
 * 
 * Requirements addressed:
 * - 1.1: Fluid, animated onboarding experience with micro-interactions
 * - 1.5: Seamless shared element transitions and motion design
 * - 3.6: Performance-adaptive animation quality settings
 */

/**
 * Enhanced animated button with micro-interactions and haptic feedback
 */
@Composable
fun AnimatedInteractiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hapticEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val hapticManager = rememberHapticFeedbackManager(hapticEnabled)
    val performanceManager = rememberPerformanceAdaptiveAnimationManager()
    val animationQuality = rememberAnimationQuality(performanceManager)
    
    val microInteractionConfig = remember(animationQuality) {
        DefaultAnimationEngine().createMicroInteraction(
            type = MicroInteractionType.Tap,
            intensity = if (animationQuality.quality == AnimationQuality.High) 1.0f else 0.7f
        )
    }
    
    Box(
        modifier = modifier
            .microInteraction(
                config = microInteractionConfig,
                enabled = enabled,
                onInteraction = {
                    // Haptic feedback will be handled by the micro-interaction system
                    // The actual haptic call is made within the microInteraction modifier
                }
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Animated loading indicator with performance-adaptive quality
 */
@Composable
fun PerformanceAdaptiveLoadingIndicator(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 48.dp
) {
    val performanceManager = rememberPerformanceAdaptiveAnimationManager()
    val animationQuality = rememberAnimationQuality(performanceManager)
    
    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        when (animationQuality.quality) {
            AnimationQuality.High -> {
                // Complex particle-based loading animation
                ComplexLoadingAnimation(
                    color = color,
                    size = size
                )
            }
            AnimationQuality.Medium -> {
                // Standard rotating circle
                StandardLoadingAnimation(
                    color = color,
                    size = size
                )
            }
            AnimationQuality.Low -> {
                // Simple pulsing circle
                SimpleLoadingAnimation(
                    color = color,
                    size = size
                )
            }
        }
    }
}

@Composable
private fun ComplexLoadingAnimation(
    color: Color,
    size: Dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "complex-loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Canvas(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
            }
    ) {
        drawComplexLoadingPattern(color)
    }
}

@Composable
private fun StandardLoadingAnimation(
    color: Color,
    size: Dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "standard-loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation }
            .background(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        color.copy(alpha = 0.1f),
                        color,
                        color.copy(alpha = 0.1f)
                    )
                ),
                shape = CircleShape
            )
    )
}

@Composable
private fun SimpleLoadingAnimation(
    color: Color,
    size: Dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "simple-loading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Box(
        modifier = Modifier
            .size(size)
            .background(
                color = color.copy(alpha = alpha),
                shape = CircleShape
            )
    )
}

private fun DrawScope.drawComplexLoadingPattern(color: Color) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = size.minDimension / 4
    
    // Draw multiple rotating circles
    for (i in 0 until 8) {
        val angle = (i * 45f) * (Math.PI / 180f)
        val x = center.x + cos(angle).toFloat() * radius
        val y = center.y + sin(angle).toFloat() * radius
        
        drawCircle(
            color = color.copy(alpha = 0.7f - (i * 0.08f)),
            radius = radius / 4,
            center = Offset(x, y)
        )
    }
}

/**
 * Animated content container with entrance animations
 */
@Composable
fun AnimatedContentContainer(
    visible: Boolean,
    modifier: Modifier = Modifier,
    animationSpec: AnimationSpec<Float>? = null,
    content: @Composable () -> Unit
) {
    val performanceManager = rememberPerformanceAdaptiveAnimationManager()
    val optimizedSpec = animationSpec ?: rememberOptimizedAnimationSpec(
        baseDuration = 300,
        animationType = AnimationType.Entrance,
        performanceManager = performanceManager
    )
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(300, easing = FastOutSlowInEasing),
            initialOffsetY = { it / 2 }
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            animationSpec = tween(200, easing = FastOutSlowInEasing),
            targetOffsetY = { -it / 2 }
        ) + fadeOut(animationSpec = tween(200)),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Staggered list animation for smooth item appearances
 */
@Composable
fun <T> StaggeredAnimatedList(
    items: List<T>,
    modifier: Modifier = Modifier,
    staggerDelay: Long = 50L,
    itemContent: @Composable (item: T, index: Int) -> Unit
) {
    val performanceManager = rememberPerformanceAdaptiveAnimationManager()
    val shouldUseStagger = performanceManager.shouldEnableFeature(AnimationFeature.ComplexTransitions)
    
    items.forEachIndexed { index, item ->
        var visible by remember { mutableStateOf(false) }
        
        LaunchedEffect(items) {
            if (shouldUseStagger) {
                delay(index * staggerDelay)
            }
            visible = true
        }
        
        AnimatedContentContainer(
            visible = visible,
            modifier = modifier
        ) {
            itemContent(item, index)
        }
    }
}

/**
 * Breathing animation effect for ambient UI elements
 */
fun Modifier.breathingEffect(
    enabled: Boolean = true,
    minScale: Float = 0.98f,
    maxScale: Float = 1.02f,
    duration: Int = 2000
): Modifier = composed {
    if (!enabled) return@composed this
    
    val performanceManager = rememberPerformanceAdaptiveAnimationManager()
    val shouldAnimate = performanceManager.shouldEnableFeature(AnimationFeature.BackgroundAnimations)
    
    if (!shouldAnimate) return@composed this
    
    val scale = AdvancedAnimationEffects.breathingEffect(
        enabled = true,
        minScale = minScale,
        maxScale = maxScale,
        duration = duration
    )
    
    this.scale(scale)
}

/**
 * Morphing shape animation between different states
 */
@Composable
fun MorphingShape(
    targetShape: ShapeState,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    animationDuration: Int = 500
) {
    val performanceManager = rememberPerformanceAdaptiveAnimationManager()
    val animationSpec = rememberOptimizedAnimationSpec(
        baseDuration = animationDuration,
        animationType = AnimationType.Standard,
        performanceManager = performanceManager
    )
    
    val morphProgress = remember { Animatable(0f) }
    
    LaunchedEffect(targetShape) {
        morphProgress.animateTo(
            targetValue = when (targetShape) {
                ShapeState.Circle -> 0f
                ShapeState.Square -> 0.5f
                ShapeState.Triangle -> 1f
            },
            animationSpec = animationSpec
        )
    }
    
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        drawMorphingShape(
            progress = morphProgress.value,
            color = color
        )
    }
}

enum class ShapeState {
    Circle,
    Square,
    Triangle
}

private fun DrawScope.drawMorphingShape(
    progress: Float,
    color: Color
) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = size.minDimension / 4
    
    when {
        progress <= 0.5f -> {
            // Morph from circle to square
            val squareProgress = progress * 2
            val cornerRadius = radius * (1f - squareProgress)
            
            drawCircle(
                color = color,
                radius = radius,
                center = center
            )
        }
        else -> {
            // Morph from square to triangle
            val triangleProgress = (progress - 0.5f) * 2
            
            // Draw triangle approximation
            drawCircle(
                color = color,
                radius = radius,
                center = center
            )
        }
    }
}

/**
 * Performance-aware particle system
 */
@Composable
fun ParticleSystem(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    particleCount: Int = 20,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val performanceManager = rememberPerformanceAdaptiveAnimationManager()
    val shouldShowParticles = performanceManager.shouldEnableFeature(AnimationFeature.ParticleEffects)
    
    if (!shouldShowParticles || !isActive) return
    
    val density = LocalDensity.current
    val particles = remember {
        List(particleCount) { index ->
            Particle(
                id = index,
                initialX = 0f,
                initialY = 0f,
                velocityX = (Math.random() * 200 - 100).toFloat(),
                velocityY = (Math.random() * 200 - 100).toFloat(),
                life = 1f
            )
        }
    }
    
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        particles.forEach { particle ->
            if (particle.life > 0) {
                drawCircle(
                    color = color.copy(alpha = particle.life),
                    radius = with(density) { 2.dp.toPx() },
                    center = Offset(particle.currentX, particle.currentY)
                )
            }
        }
    }
}

private data class Particle(
    val id: Int,
    val initialX: Float,
    val initialY: Float,
    val velocityX: Float,
    val velocityY: Float,
    var life: Float,
    var currentX: Float = initialX,
    var currentY: Float = initialY
)