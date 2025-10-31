package com.voicenotesai.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Example usage of the Material You Theme System
 * Demonstrates how to use ModernColorScheme, GradientSystem, ModernSpacing, and ModernShapes
 */

@Composable
fun GradientHeaderExample() {
    val gradients = MaterialYouTheme.gradients
    val spacing = MaterialYouTheme.spacing
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(gradients.headerGradient())
            .padding(spacing.screenPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "Voice Notes",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ModernCardExample() {
    val spacing = MaterialYouTheme.spacing
    val shapes = MaterialYouTheme.shapes
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacing.screenPadding),
        shape = shapes.cardCorners
    ) {
        Column(
            modifier = Modifier.padding(spacing.cardPadding)
        ) {
            Text(
                text = "Note Title",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "This is a preview of the note content...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = spacing.extraSmall)
            )
        }
    }
}

@Composable
fun WaveformVisualizerExample() {
    val gradients = MaterialYouTheme.gradients
    val spacing = MaterialYouTheme.spacing
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(spacing.screenPadding)
            .clip(RoundedCornerShape(8.dp))
            .background(gradients.waveformGradient()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Waveform Visualization",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun ChipExample() {
    val shapes = MaterialYouTheme.shapes
    val spacing = MaterialYouTheme.spacing
    
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = shapes.chipCorners
            )
            .padding(horizontal = spacing.small, vertical = spacing.extraSmall)
    ) {
        Text(
            text = "Meeting",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MaterialYouThemePreview() {
    MaterialYouTheme {
        Column {
            GradientHeaderExample()
            ModernCardExample()
            WaveformVisualizerExample()
            ChipExample()
        }
    }
}