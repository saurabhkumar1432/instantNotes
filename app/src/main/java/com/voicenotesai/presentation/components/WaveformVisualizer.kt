package com.voicenotesai.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.voicenotesai.R
import kotlin.random.Random

/**
 * Waveform visualizer component matching the recording screen mockup.
 * Displays animated bars representing audio levels.
 */
@Composable
fun WaveformVisualizer(
    isActive: Boolean,
    barCount: Int = 20,
    modifier: Modifier = Modifier,
    audioLevels: List<Float> = emptyList()
) {
    val activityDescription = if (isActive) {
        stringResource(R.string.active_state)
    } else {
        stringResource(R.string.inactive_state)
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(128.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .semantics {
                contentDescription = "Waveform visualizer: $activityDescription"
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(barCount) { index ->
                WaveformBar(
                    isActive = isActive,
                    animationDelay = index * 50,
                    height = if (audioLevels.isNotEmpty() && index < audioLevels.size) {
                        audioLevels[index]
                    } else {
                        Random.nextFloat()
                    }
                )
            }
        }
    }
}

@Composable
private fun RowScope.WaveformBar(
    isActive: Boolean,
    animationDelay: Int,
    height: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    
    val animatedHeight by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (isActive) height else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing,
                delayMillis = animationDelay
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar_height"
    )
    
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(if (isActive) animatedHeight else 0.2f)
            .clip(RoundedCornerShape(2.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            )
    )
}

/**
 * Simple static waveform for non-recording states
 */
@Composable
fun StaticWaveform(
    audioData: FloatArray,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val sampledData = audioData.toList().chunked(audioData.size / 50).map { it.average().toFloat() }
        
        sampledData.forEach { amplitude ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(amplitude.coerceIn(0.1f, 1f))
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}
