package com.voicenotesai.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.voicenotesai.domain.visualization.*
import kotlin.math.*

/**
 * Real-time audio visualization components providing waveform display,
 * spectral analysis, and contextual UI adaptations during recording.
 * 
 * Requirements addressed:
 * - 1.3: Real-time audio visualization with smooth animations and contextual UI adaptations
 */

/**
 * Main audio visualization component that adapts based on recording state
 */
@Composable
fun AudioVisualizationDisplay(
    visualizationData: VisualizationData?,
    audioState: AudioState,
    uiAdaptations: UIAdaptationConfig,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp
) {
    val density = LocalDensity.current
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = uiAdaptations.colorScheme.gradientColors,
                    startY = 0f,
                    endY = with(density) { height.toPx() }
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Background pulse animation
        uiAdaptations.pulseAnimation?.let { pulseConfig ->
            PulseBackground(
                pulseConfig = pulseConfig,
                color = uiAdaptations.colorScheme.backgroundColor,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Main visualization content
        when {
            uiAdaptations.showWaveform && visualizationData != null -> {
                WaveformVisualization(
                    waveformData = visualizationData.waveform,
                    colorScheme = uiAdaptations.colorScheme,
                    modifier = Modifier.fillMaxSize()
                )
            }
            uiAdaptations.showSpectralBars && visualizationData != null -> {
                SpectralBarsVisualization(
                    spectralData = visualizationData.spectral,
                    colorScheme = uiAdaptations.colorScheme,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                IdleVisualization(
                    colorScheme = uiAdaptations.colorScheme,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // Audio level meter overlay
        if (uiAdaptations.showLevelMeter && visualizationData != null) {
            AudioLevelMeter(
                audioLevel = visualizationData.audioLevel,
                colorScheme = uiAdaptations.colorScheme,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp)
            )
        }
    }
}

/**
 * Waveform visualization component
 */
@Composable
fun WaveformVisualization(
    waveformData: WaveformData,
    colorScheme: VisualizationColorScheme,
    modifier: Modifier = Modifier
) {
    val animatedAmplitudes by animateFloatArrayAsState(
        targetValue = waveformData.amplitudes.toFloatArray(),
        animationSpec = tween(100, easing = FastOutSlowInEasing)
    )
    
    Canvas(modifier = modifier) {
        drawWaveform(
            amplitudes = animatedAmplitudes.toList(),
            color = colorScheme.primaryColor,
            strokeWidth = 3.dp.toPx(),
            size = size
        )
    }
}

/**
 * Spectral bars visualization component
 */
@Composable
fun SpectralBarsVisualization(
    spectralData: SpectralData,
    colorScheme: VisualizationColorScheme,
    modifier: Modifier = Modifier
) {
    val animatedMagnitudes by animateFloatArrayAsState(
        targetValue = spectralData.magnitudes.take(32).toFloatArray(), // Limit to 32 bars
        animationSpec = tween(150, easing = FastOutSlowInEasing)
    )
    
    Canvas(modifier = modifier) {
        drawSpectralBars(
            magnitudes = animatedMagnitudes.toList(),
            colors = colorScheme.gradientColors,
            size = size
        )
    }
}

/**
 * Idle state visualization
 */
@Composable
fun IdleVisualization(
    colorScheme: VisualizationColorScheme,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "idle")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idle-phase"
    )
    
    Canvas(modifier = modifier) {
        drawIdleWave(
            phase = phase,
            color = colorScheme.secondaryColor,
            size = size
        )
    }
}

/**
 * Audio level meter component
 */
@Composable
fun AudioLevelMeter(
    audioLevel: AudioLevel,
    colorScheme: VisualizationColorScheme,
    modifier: Modifier = Modifier
) {
    val animatedLevel by animateFloatAsState(
        targetValue = audioLevel.rms,
        animationSpec = tween(50, easing = FastOutSlowInEasing),
        label = "audio-level"
    )
    
    Column(
        modifier = modifier.width(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(10) { index ->
            val threshold = (9 - index) / 10f
            val isActive = animatedLevel > threshold
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (isActive) {
                            when {
                                index < 3 -> colorScheme.accentColor // Red zone
                                index < 6 -> Color.Yellow // Yellow zone
                                else -> colorScheme.primaryColor // Green zone
                            }
                        } else {
                            colorScheme.backgroundColor.copy(alpha = 0.3f)
                        }
                    )
            )
        }
    }
}

/**
 * Pulse background animation
 */
@Composable
fun PulseBackground(
    pulseConfig: PulseConfig,
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f + pulseConfig.intensity * 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (1000f / pulseConfig.frequency).toInt(),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse-scale"
    )
    
    Box(
        modifier = modifier
            .scale(scale)
            .background(
                color = color.copy(alpha = 0.2f),
                shape = CircleShape
            )
    )
}

/**
 * Recording indicator with pulsing animation
 */
@Composable
fun RecordingIndicator(
    isRecording: Boolean,
    audioLevel: AudioLevel,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recording-alpha"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isRecording) 1f + (audioLevel.rms * 0.3f) else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "recording-scale"
    )
    
    Box(
        modifier = modifier
            .size(16.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (isRecording) {
                    Color.Red.copy(alpha = alpha)
                } else {
                    MaterialTheme.colorScheme.outline
                }
            )
    )
}

// Drawing functions for Canvas components

private fun DrawScope.drawWaveform(
    amplitudes: List<Float>,
    color: Color,
    strokeWidth: Float,
    size: Size
) {
    if (amplitudes.isEmpty()) return
    
    val path = Path()
    val centerY = size.height / 2f
    val stepX = size.width / (amplitudes.size - 1).coerceAtLeast(1)
    
    // Create waveform path
    amplitudes.forEachIndexed { index, amplitude ->
        val x = index * stepX
        val y = centerY + (amplitude * centerY * 0.8f)
        
        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    
    // Draw the waveform
    drawPath(
        path = path,
        color = color,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    
    // Draw reflection (mirrored below center)
    val reflectionPath = Path()
    amplitudes.forEachIndexed { index, amplitude ->
        val x = index * stepX
        val y = centerY - (amplitude * centerY * 0.8f)
        
        if (index == 0) {
            reflectionPath.moveTo(x, y)
        } else {
            reflectionPath.lineTo(x, y)
        }
    }
    
    drawPath(
        path = reflectionPath,
        color = color.copy(alpha = 0.3f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth * 0.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawSpectralBars(
    magnitudes: List<Float>,
    colors: List<Color>,
    size: Size
) {
    if (magnitudes.isEmpty()) return
    
    val barWidth = size.width / magnitudes.size
    val maxMagnitude = magnitudes.maxOrNull() ?: 1f
    
    magnitudes.forEachIndexed { index, magnitude ->
        val normalizedMagnitude = (magnitude / maxMagnitude).coerceIn(0f, 1f)
        val barHeight = normalizedMagnitude * size.height * 0.9f
        
        val x = index * barWidth
        val y = size.height - barHeight
        
        // Create gradient for each bar
        val gradient = Brush.verticalGradient(
            colors = colors,
            startY = y,
            endY = size.height
        )
        
        drawRect(
            brush = gradient,
            topLeft = Offset(x + barWidth * 0.1f, y),
            size = Size(barWidth * 0.8f, barHeight)
        )
    }
}

private fun DrawScope.drawIdleWave(
    phase: Float,
    color: Color,
    size: Size
) {
    val path = Path()
    val centerY = size.height / 2f
    val amplitude = size.height * 0.1f
    val frequency = 2f
    
    val steps = 100
    val stepX = size.width / steps
    
    for (i in 0..steps) {
        val x = i * stepX
        val y = centerY + amplitude * sin(frequency * (x / size.width) * 2 * PI + phase).toFloat()
        
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    
    drawPath(
        path = path,
        color = color.copy(alpha = 0.6f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )
}

// Helper function to animate float arrays
@Composable
private fun animateFloatArrayAsState(
    targetValue: FloatArray,
    animationSpec: AnimationSpec<Float> = spring()
): State<FloatArray> {
    val animatedValues = remember { mutableStateListOf<Animatable<Float, AnimationVector1D>>() }
    
    // Adjust the size of animated values to match target
    LaunchedEffect(targetValue.size) {
        while (animatedValues.size < targetValue.size) {
            animatedValues.add(Animatable(0f))
        }
        while (animatedValues.size > targetValue.size) {
            animatedValues.removeAt(animatedValues.size - 1)
        }
    }
    
    // Animate each value
    LaunchedEffect(targetValue.contentHashCode()) {
        targetValue.forEachIndexed { index, target ->
            if (index < animatedValues.size) {
                animatedValues[index].animateTo(target, animationSpec)
            }
        }
    }
    
    return derivedStateOf {
        animatedValues.map { it.value }.toFloatArray()
    }
}