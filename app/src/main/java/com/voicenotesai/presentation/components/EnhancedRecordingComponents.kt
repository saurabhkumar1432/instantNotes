package com.voicenotesai.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.voicenotesai.presentation.animations.bouncyClickable
import com.voicenotesai.presentation.animations.pulseAnimation
import com.voicenotesai.presentation.theme.ExtendedTypography
import com.voicenotesai.presentation.theme.Spacing
import kotlinx.coroutines.delay

/**
 * Enhanced recording button with multiple states and better visual feedback
 */
@Composable
fun EnhancedRecordButton(
    recordingState: RecordingButtonState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPauseRecording: () -> Unit = {},
    onResumeRecording: () -> Unit = {},
    duration: Long = 0L,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "enhanced-record-transition")
    val haptic = LocalHapticFeedback.current

    // Animated properties based on state
    val animatedScale by infiniteTransition.animateFloat(
        initialValue = when (recordingState) {
            RecordingButtonState.Idle -> 1f
            RecordingButtonState.Recording -> 0.95f
            RecordingButtonState.Paused -> 1f
            RecordingButtonState.Processing -> 1.05f
        },
        targetValue = when (recordingState) {
            RecordingButtonState.Idle -> 1.02f
            RecordingButtonState.Recording -> 1.08f
            RecordingButtonState.Paused -> 1.02f
            RecordingButtonState.Processing -> 0.98f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (recordingState) {
                    RecordingButtonState.Recording -> 800
                    RecordingButtonState.Processing -> 1200
                    else -> 2000
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "record-scale"
    )

    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = when (recordingState) {
            RecordingButtonState.Recording -> 0.6f
            RecordingButtonState.Processing -> 0.3f
            else -> 0.1f
        },
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (recordingState) {
                    RecordingButtonState.Recording -> 1200
                    RecordingButtonState.Processing -> 1600
                    else -> 2400
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple-alpha"
    )

    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = when (recordingState) {
            RecordingButtonState.Recording -> 1.6f
            RecordingButtonState.Processing -> 1.4f
            else -> 1.2f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (recordingState) {
                    RecordingButtonState.Recording -> 1200
                    RecordingButtonState.Processing -> 1600
                    else -> 2400
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple-scale"
    )

    // Color animation
    val buttonColor by animateColorAsState(
        targetValue = when (recordingState) {
            RecordingButtonState.Idle -> MaterialTheme.colorScheme.primary
            RecordingButtonState.Recording -> MaterialTheme.colorScheme.error
            RecordingButtonState.Paused -> MaterialTheme.colorScheme.secondary
            RecordingButtonState.Processing -> MaterialTheme.colorScheme.tertiary
        },
        animationSpec = tween(300),
        label = "button-color"
    )

    val iconColor by animateColorAsState(
        targetValue = when (recordingState) {
            RecordingButtonState.Idle -> Color.White
            RecordingButtonState.Recording -> Color.White
            RecordingButtonState.Paused -> Color.White
            RecordingButtonState.Processing -> Color.White
        },
        animationSpec = tween(300),
        label = "icon-color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = modifier
    ) {
        // Main record button with enhanced animations
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(200.dp)
                .pulseAnimation(
                    enabled = recordingState != RecordingButtonState.Idle,
                    minScale = 0.96f,
                    maxScale = 1.04f,
                    duration = 1000
                )
        ) {
            // Animated ripple effects
            repeat(2) { index ->
                Box(
                    modifier = Modifier
                        .size((150 + index * 18).dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    buttonColor.copy(alpha = rippleAlpha / (index + 1)),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                        .scale(rippleScale + index * 0.1f)
                        .blur((15 + index * 10).dp)
                )
            }

            // Main button
            FilledIconButton(
                onClick = {
                    haptic.performHapticFeedback(
                        when (recordingState) {
                            RecordingButtonState.Idle -> HapticFeedbackType.TextHandleMove
                            RecordingButtonState.Recording -> HapticFeedbackType.LongPress
                            RecordingButtonState.Paused -> HapticFeedbackType.TextHandleMove
                            RecordingButtonState.Processing -> HapticFeedbackType.TextHandleMove
                        }
                    )
                    
                    when (recordingState) {
                        RecordingButtonState.Idle -> onStartRecording()
                        RecordingButtonState.Recording -> onStopRecording()
                        RecordingButtonState.Paused -> onResumeRecording()
                        RecordingButtonState.Processing -> { /* Processing - no action */ }
                    }
                },
                enabled = enabled && recordingState != RecordingButtonState.Processing,
                modifier = Modifier
                    .size(160.dp)
                    .scale(animatedScale)
                    .clearAndSetSemantics {
                        role = Role.Button
                        contentDescription = when (recordingState) {
                            RecordingButtonState.Idle -> "Start voice recording"
                            RecordingButtonState.Recording -> "Stop recording. Currently recording for ${formatDurationForAccessibility(duration)}"
                            RecordingButtonState.Paused -> "Resume recording. Paused at ${formatDurationForAccessibility(duration)}"
                            RecordingButtonState.Processing -> "Processing recording. Please wait."
                        }
                        stateDescription = when (recordingState) {
                            RecordingButtonState.Idle -> "Ready to record"
                            RecordingButtonState.Recording -> "Recording in progress"
                            RecordingButtonState.Paused -> "Recording paused"
                            RecordingButtonState.Processing -> "Processing audio"
                        }
                    },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = buttonColor,
                    disabledContainerColor = buttonColor.copy(alpha = 0.6f)
                ),
                shape = CircleShape
            ) {
                AnimatedContent(
                    targetState = recordingState,
                    transitionSpec = {
                        scaleIn(tween(200)) + fadeIn(tween(200)) togetherWith
                                scaleOut(tween(200)) + fadeOut(tween(200))
                    },
                    label = "button-icon-transition"
                ) { state ->
                    when (state) {
                        RecordingButtonState.Idle -> Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(42.dp)
                        )
                        RecordingButtonState.Recording -> Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(40.dp)
                        )
                        RecordingButtonState.Paused -> Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(42.dp)
                        )
                        RecordingButtonState.Processing -> CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp,
                            color = iconColor
                        )
                    }
                }
            }
        }

        // Duration display
        AnimatedContent(
            targetState = recordingState,
            transitionSpec = {
                fadeIn(tween(300)) + scaleIn(tween(300)) togetherWith
                        fadeOut(tween(300)) + scaleOut(tween(300))
            },
            label = "duration-transition"
        ) { state ->
            when (state) {
                RecordingButtonState.Recording, RecordingButtonState.Paused -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        Text(
                            text = formatDuration(duration),
                            style = ExtendedTypography.timerDisplay,
                            color = buttonColor,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = when (state) {
                                RecordingButtonState.Recording -> "Recording in progress"
                                RecordingButtonState.Paused -> "Recording paused"
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    Text(
                        text = when (state) {
                            RecordingButtonState.Idle -> "Tap to record"
                            RecordingButtonState.Processing -> "Transcribing and structuring"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Secondary controls for recording state
        // Additional inline controls can be introduced here as features expand.
    }
}

/**
 * Enhanced waveform visualization with better animation and responsiveness
 */
@Composable
fun EnhancedWaveformIndicator(
    isActive: Boolean = true,
    intensity: Float = 1f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "enhanced-waveform")
    val barCount = 7
    
    val baseColor = MaterialTheme.colorScheme.primary
    val accentColor = MaterialTheme.colorScheme.secondary
    
    val barBrush = Brush.verticalGradient(
        colors = listOf(
            accentColor,
            baseColor,
            accentColor
        )
    )

    val barHeights = (0 until barCount).map { index ->
        infiniteTransition.animateFloat(
            initialValue = if (isActive) 0.2f else 0.1f,
            targetValue = if (isActive) {
                (0.4f + (kotlin.math.sin(index * 0.5).toFloat() * 0.3f)).coerceIn(0.3f, 1f) * intensity
            } else {
                0.15f
            },
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 400 + (index * 80),
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = androidx.compose.animation.core.StartOffset(index * 60)
            ),
            label = "waveform-bar-$index"
        )
    }

    Row(
        modifier = modifier.height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        barHeights.forEach { heightFraction ->
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight(heightFraction.value)
                    .background(
                        brush = barBrush,
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

/**
 * Recording quality indicator
 */
@Composable
fun RecordingQualityIndicator(
    quality: RecordingQuality,
    modifier: Modifier = Modifier
) {
    val color = when (quality) {
        RecordingQuality.Poor -> MaterialTheme.colorScheme.error
        RecordingQuality.Fair -> MaterialTheme.colorScheme.tertiary
        RecordingQuality.Good -> MaterialTheme.colorScheme.primary
        RecordingQuality.Excellent -> MaterialTheme.colorScheme.secondary
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Audio Quality:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
                .pulseAnimation(
                    enabled = quality == RecordingQuality.Poor,
                    minScale = 0.8f,
                    maxScale = 1.2f,
                    duration = 800
                )
        )
        
        Text(
            text = quality.displayName,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Smart retry button with contextual messaging
 */
@Composable
fun SmartRetryButton(
    retryType: RetryType,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        FilledIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onRetry()
            },
            modifier = Modifier
                .clearAndSetSemantics {
                    role = Role.Button
                    contentDescription = "Retry ${retryType.actionDescription}"
                },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = retryType.icon,
                contentDescription = null,
                tint = Color.White
            )
        }
        
        Text(
            text = retryType.displayText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// Data classes and enums for enhanced recording states
enum class RecordingButtonState {
    Idle,
    Recording, 
    Paused,
    Processing
}

enum class RecordingQuality(val displayName: String) {
    Poor("Poor"),
    Fair("Fair"), 
    Good("Good"),
    Excellent("Excellent")
}

enum class RetryType(
    val displayText: String,
    val actionDescription: String,
    val icon: ImageVector
) {
    RecordAgain("Try Recording Again", "recording", Icons.Filled.Refresh),
    CheckMicrophone("Check Microphone", "microphone check", Icons.Filled.Warning),
    CheckConnection("Check Connection", "connection check", Icons.Filled.Warning)
}

// Helper functions
private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private fun formatDurationForAccessibility(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    
    return when {
        minutes > 0 -> "$minutes minutes and $seconds seconds"
        seconds > 0 -> "$seconds seconds"
        else -> "less than a second"
    }
}