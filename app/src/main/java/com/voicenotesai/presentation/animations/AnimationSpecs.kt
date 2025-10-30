package com.voicenotesai.presentation.animations

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.ui.graphics.Color

/**
 * Central animation timing tokens to ensure consistent motion across the app.
 */
object AnimationSpecs {
    const val SHORT = 180
    const val MEDIUM = 320
    const val LONG = 600

    const val RECORDING_PULSE = 1200
    const val PROCESSING_PULSE = 1600
    const val IDLE_PULSE = 2000

    val StandardEasing: Easing = FastOutSlowInEasing

    fun shortTween(): FiniteAnimationSpec<Float> = tween(durationMillis = SHORT, easing = StandardEasing)
    fun mediumTween(): FiniteAnimationSpec<Float> = tween(durationMillis = MEDIUM, easing = StandardEasing)
    fun longTween(): FiniteAnimationSpec<Float> = tween(durationMillis = LONG, easing = StandardEasing)

    // Color-specific helpers (useful for animateColorAsState)
    fun shortColorTween(): FiniteAnimationSpec<Color> = tween(durationMillis = SHORT, easing = StandardEasing)
    fun mediumColorTween(): FiniteAnimationSpec<Color> = tween(durationMillis = MEDIUM, easing = StandardEasing)
    fun longColorTween(): FiniteAnimationSpec<Color> = tween(durationMillis = LONG, easing = StandardEasing)
}
