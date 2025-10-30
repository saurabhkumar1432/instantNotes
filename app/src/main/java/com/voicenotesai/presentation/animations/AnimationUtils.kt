package com.voicenotesai.presentation.animations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/**
 * Animation configurations for consistent behavior across the app
 */
object AnimationConfig {
    val SpringyEnter = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    val SpringyExit = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    val FastFade = tween<Float>(150)
    val MediumFade = tween<Float>(300)
    val SlowFade = tween<Float>(500)
    
    val QuickSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    
    val SmoothSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
}

/**
 * Enhanced bouncy clickable modifier with haptic feedback and scale animation
 * Now integrates with the advanced animation system for performance adaptation
 */
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    hapticFeedback: Boolean = true,
    scaleDown: Float = 0.96f,
    animationSpec: AnimationSpec<Float>? = null,
    onClick: () -> Unit
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Use performance-adaptive animation spec if none provided
    val performanceManager = rememberPerformanceAdaptiveAnimationManager()
    val adaptiveSpec = animationSpec ?: performanceManager.createOptimizedAnimationSpec(
        baseDuration = 150,
        animationType = AnimationType.Micro
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = adaptiveSpec,
        label = "bouncy-scale"
    )
    
    this
        .scale(scale)
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = rememberRipple(),
            onClick = {
                if (hapticFeedback) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onClick()
            }
        )
}

/**
 * Smooth slide-in animation for content that appears from different directions
 */
@Composable
fun SlideInContent(
    visible: Boolean,
    modifier: Modifier = Modifier,
    slideDirection: SlideDirection = SlideDirection.Bottom,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = when (slideDirection) {
            SlideDirection.Top -> slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = AnimationConfig.MediumFade)
            SlideDirection.Bottom -> slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = AnimationConfig.MediumFade)
        },
        exit = when (slideDirection) {
            SlideDirection.Top -> slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = AnimationConfig.FastFade)
            SlideDirection.Bottom -> slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = AnimationConfig.FastFade)
        },
        modifier = modifier
    ) {
        content()
    }
}

enum class SlideDirection {
    Top, Bottom
}

/**
 * Expandable content with smooth animation
 */
@Composable
fun ExpandableContent(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = AnimationConfig.MediumFade),
        exit = shrinkVertically(
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = AnimationConfig.FastFade),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Animated loading skeleton for better perceived performance
 */
@Composable
fun LoadingSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(
        label = "skeleton-shimmer"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(1000),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "skeleton-alpha"
    )
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha * 0.7f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
                    )
                )
            )
    )
}

/**
 * Animated color transition for state changes
 */
@Composable
fun animatedStateColor(
    targetColor: Color,
    animationSpec: AnimationSpec<Color> = tween(300)
): androidx.compose.runtime.State<Color> {
    return animateColorAsState(
        targetValue = targetColor,
        animationSpec = animationSpec,
        label = "state-color"
    )
}

/**
 * Pulse animation for attention-grabbing elements
 * Now performance-aware and adapts to device capabilities
 */
fun Modifier.pulseAnimation(
    enabled: Boolean = true,
    minScale: Float = 0.95f,
    maxScale: Float = 1.05f,
    duration: Int = 1000
): Modifier = composed {
    if (!enabled) return@composed this
    
    // Check if background animations should be enabled based on performance
    val performanceManager = rememberPerformanceAdaptiveAnimationManager()
    val shouldAnimate = performanceManager.shouldEnableFeature(AnimationFeature.BackgroundAnimations)
    
    if (!shouldAnimate) return@composed this
    
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(
        label = "pulse-animation"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(duration),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulse-scale"
    )
    
    this.scale(scale)
}

/**
 * Shake animation for error states or invalid inputs
 */
fun Modifier.shakeAnimation(
    enabled: Boolean,
    strength: Float = 10f,
    duration: Int = 300
): Modifier = composed {
    if (!enabled) return@composed this
    
    val offsetX = remember { Animatable(0f) }
    
    LaunchedEffect(enabled) {
        if (enabled) {
            repeat(3) {
                offsetX.animateTo(
                    targetValue = strength,
                    animationSpec = tween(duration / 6)
                )
                offsetX.animateTo(
                    targetValue = -strength,
                    animationSpec = tween(duration / 6)
                )
            }
            offsetX.animateTo(
                targetValue = 0f,
                animationSpec = tween(duration / 6)
            )
        }
    }
    
    this.graphicsLayer {
        translationX = offsetX.value
    }
}

/**
 * Simple animation item helper for lists
 */
@Composable
fun <T> AnimatedListItem(
    item: T,
    index: Int,
    modifier: Modifier = Modifier,
    delayBetweenItems: Int = 50,
    content: @Composable (item: T) -> Unit
) {
    SlideInContent(
        visible = true,
        slideDirection = SlideDirection.Bottom,
        modifier = modifier
    ) {
        // Add staggered delay
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay((index * delayBetweenItems).toLong())
        }
        content(item)
    }
}