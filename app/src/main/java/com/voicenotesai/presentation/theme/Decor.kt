package com.voicenotesai.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Applies a frosted glass surface treatment that pairs well with neon gradients.
 */
fun Modifier.glassLayer(shape: Shape = RoundedCornerShape(28.dp)): Modifier = composed {
    clip(shape)
        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
        .border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            shape = shape
        )
}

/**
 * Provides a vibrant gradient backdrop plus a subtle overlay to keep content legible.
 */
@Composable
fun NeonBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val gradient = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.45f))
        )
        content()
    }
}
