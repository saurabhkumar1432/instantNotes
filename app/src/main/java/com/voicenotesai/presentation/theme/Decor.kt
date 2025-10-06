package com.voicenotesai.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Gen-Z glassmorphism 2.0 - bolder borders, enhanced blur vibes
 */
fun Modifier.glassLayer(shape: Shape = RoundedCornerShape(24.dp)): Modifier = composed {
    val surfaceColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
    clip(shape)
        .background(surfaceColor)
        .border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
            shape = shape
        )
}

/**
 * Neo-brutalism shadow effect for cards and buttons
 */
fun Modifier.brutalistShadow(
    shape: Shape = RoundedCornerShape(24.dp)
): Modifier = composed {
    border(
        width = 3.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        shape = shape
    )
}

/**
 * Vibrant gradient backdrop with Gen-Z energy
 */
@Composable
fun NeonBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
            MaterialTheme.colorScheme.background
        ),
        startY = 0f,
        endY = 1400f
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        content()
    }
}

/**
 * Colorful gradient overlay for emphasis
 */
fun Modifier.rainbowGradient(): Modifier = composed {
    background(
        brush = Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.tertiary
            )
        )
    )
}
