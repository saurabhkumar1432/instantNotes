package com.voicenotesai.presentation.help.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.voicenotesai.presentation.help.HelpState
import com.voicenotesai.presentation.help.HelpStep
import com.voicenotesai.presentation.help.HelpTour
import com.voicenotesai.presentation.help.TooltipData
import com.voicenotesai.presentation.help.TooltipPosition
import com.voicenotesai.presentation.theme.Spacing
import kotlinx.coroutines.delay

/**
 * Main tooltip component that displays contextual help information.
 */
@Composable
fun ContextualTooltip(
    helpState: HelpState,
    modifier: Modifier = Modifier
) {
    val tooltipData = helpState.currentTooltip
    val targetBounds = helpState.targetBounds
    
    AnimatedVisibility(
        visible = helpState.isTooltipVisible && tooltipData != null,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(200)) + scaleOut(animationSpec = tween(200))
    ) {
        tooltipData?.let { data ->
            Box(modifier = modifier.fillMaxSize()) {
                // Semi-transparent overlay
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { helpState.hideTooltip() }
                ) {
                    drawRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        size = size
                    )
                    
                    // Cut out hole for target element if bounds provided
                    targetBounds?.let { bounds ->
                        val expandedBounds = Rect(
                            offset = Offset(
                                bounds.left - 8.dp.toPx(),
                                bounds.top - 8.dp.toPx()
                            ),
                            size = Size(
                                bounds.width + 16.dp.toPx(),
                                bounds.height + 16.dp.toPx()
                            )
                        )
                        
                        drawRoundRect(
                            color = Color.Transparent,
                            topLeft = expandedBounds.topLeft,
                            size = expandedBounds.size,
                            cornerRadius = CornerRadius(12.dp.toPx()),
                            blendMode = BlendMode.Clear
                        )
                    }
                }
                
                // Tooltip content
                TooltipContent(
                    data = data,
                    targetBounds = targetBounds,
                    onDismiss = { helpState.hideTooltip() }
                )
            }
        }
    }
}

/**
 * Tooltip content with title, description, and dismiss button.
 */
@Composable
private fun TooltipContent(
    data: TooltipData,
    targetBounds: Rect?,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    
    val tooltipPosition = remember(targetBounds, data.position) {
        calculateTooltipPosition(targetBounds, data.position, density)
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Card(
            modifier = Modifier
                .padding(Spacing.medium)
                .then(
                    if (tooltipPosition.offset != IntOffset.Zero) {
                        Modifier.offset { tooltipPosition.offset }
                    } else {
                        Modifier.align(Alignment.Center)
                    }
                )
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .semantics {
                    contentDescription = "Help tooltip: ${data.title}. ${data.description}"
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(Spacing.large),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close tooltip",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Description
                Text(
                    text = data.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                )
                
                // Action button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Got it!")
                }
            }
        }
        
        // Draw arrow pointing to target if applicable
        targetBounds?.let { bounds ->
            TooltipArrow(
                targetBounds = bounds,
                tooltipPosition = data.position
            )
        }
    }
}

/**
 * Arrow pointing from tooltip to target element.
 */
@Composable
private fun TooltipArrow(
    targetBounds: Rect,
    tooltipPosition: TooltipPosition
) {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val arrowSize = 12.dp.toPx()
        val arrowPath = Path()
        
        when (tooltipPosition) {
            TooltipPosition.Top -> {
                val arrowTip = Offset(
                    targetBounds.center.x,
                    targetBounds.top - 8.dp.toPx()
                )
                arrowPath.apply {
                    moveTo(arrowTip.x, arrowTip.y)
                    lineTo(arrowTip.x - arrowSize / 2, arrowTip.y - arrowSize)
                    lineTo(arrowTip.x + arrowSize / 2, arrowTip.y - arrowSize)
                    close()
                }
            }
            TooltipPosition.Bottom -> {
                val arrowTip = Offset(
                    targetBounds.center.x,
                    targetBounds.bottom + 8.dp.toPx()
                )
                arrowPath.apply {
                    moveTo(arrowTip.x, arrowTip.y)
                    lineTo(arrowTip.x - arrowSize / 2, arrowTip.y + arrowSize)
                    lineTo(arrowTip.x + arrowSize / 2, arrowTip.y + arrowSize)
                    close()
                }
            }
            TooltipPosition.Left -> {
                val arrowTip = Offset(
                    targetBounds.left - 8.dp.toPx(),
                    targetBounds.center.y
                )
                arrowPath.apply {
                    moveTo(arrowTip.x, arrowTip.y)
                    lineTo(arrowTip.x - arrowSize, arrowTip.y - arrowSize / 2)
                    lineTo(arrowTip.x - arrowSize, arrowTip.y + arrowSize / 2)
                    close()
                }
            }
            TooltipPosition.Right -> {
                val arrowTip = Offset(
                    targetBounds.right + 8.dp.toPx(),
                    targetBounds.center.y
                )
                arrowPath.apply {
                    moveTo(arrowTip.x, arrowTip.y)
                    lineTo(arrowTip.x + arrowSize, arrowTip.y - arrowSize / 2)
                    lineTo(arrowTip.x + arrowSize, arrowTip.y + arrowSize / 2)
                    close()
                }
            }
            TooltipPosition.Center -> {
                // No arrow for center position
                return@Canvas
            }
        }
        
        drawPath(
            path = arrowPath,
            color = Color.White
        )
    }
}

/**
 * Guided tour component that manages step-by-step help tours.
 */
@Composable
fun GuidedTour(
    tour: HelpTour?,
    onStepComplete: () -> Unit,
    onTourComplete: () -> Unit,
    onTourSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    tour?.let { currentTour ->
        val currentStep = currentTour.getCurrentStep()
        
        currentStep?.let { step ->
            Box(modifier = modifier.fillMaxSize()) {
                // Dark overlay
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    drawRect(
                        color = Color.Black.copy(alpha = 0.7f),
                        size = size
                    )
                }
                
                // Tour step content
                TourStepContent(
                    tour = currentTour,
                    step = step,
                    onNext = {
                        if (currentTour.nextStep()) {
                            onStepComplete()
                        } else {
                            onTourComplete()
                        }
                    },
                    onPrevious = {
                        currentTour.previousStep()
                        onStepComplete()
                    },
                    onSkip = onTourSkip
                )
            }
        }
    }
}

/**
 * Individual tour step content.
 */
@Composable
private fun TourStepContent(
    tour: HelpTour,
    step: HelpStep,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.large),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(Spacing.extraLarge),
                verticalArrangement = Arrangement.spacedBy(Spacing.large)
            ) {
                // Progress indicator
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = tour.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${tour.getCurrentStep()?.let { tour.steps.indexOf(it) + 1 } ?: 1} of ${tour.steps.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    LinearProgressIndicator(
                        progress = tour.getProgress(),
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                
                // Step content
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                )
                
                // Navigation buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skip button
                    TextButton(onClick = onSkip) {
                        Text("Skip tour")
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                    ) {
                        // Previous button
                        if (!tour.isFirstStep()) {
                            OutlinedButton(
                                onClick = onPrevious,
                                modifier = Modifier.semantics {
                                    contentDescription = "Previous step"
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(Spacing.small))
                                Text("Previous")
                            }
                        }
                        
                        // Next/Finish button
                        Button(
                            onClick = onNext,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.semantics {
                                contentDescription = if (tour.isLastStep()) "Finish tour" else "Next step"
                            }
                        ) {
                            Text(
                                text = if (tour.isLastStep()) "Finish" else "Next"
                            )
                            if (!tour.isLastStep()) {
                                Spacer(modifier = Modifier.width(Spacing.small))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Floating help button that can trigger tours or show help info.
 */
@Composable
fun HelpFloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .semantics {
                contentDescription = "Show help and guidance"
            },
        containerColor = MaterialTheme.colorScheme.tertiary,
        contentColor = MaterialTheme.colorScheme.onTertiary
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null
        )
    }
}

/**
 * Quick tip component for showing brief helpful hints.
 */
@Composable
fun QuickTip(
    title: String,
    description: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Card(
            modifier = modifier
                .padding(Spacing.medium)
                .semantics {
                    contentDescription = "Quick tip: $title"
                },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(Spacing.medium),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss tip",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Calculate tooltip position based on target bounds and desired position.
 */
private fun calculateTooltipPosition(
    targetBounds: Rect?,
    position: TooltipPosition,
    density: androidx.compose.ui.unit.Density
): TooltipPositioning {
    targetBounds ?: return TooltipPositioning(IntOffset.Zero)
    
    with(density) {
        val tooltipOffset = when (position) {
            TooltipPosition.Top -> IntOffset(
                x = (targetBounds.center.x - 150.dp.toPx()).toInt(),
                y = (targetBounds.top - 200.dp.toPx()).toInt()
            )
            TooltipPosition.Bottom -> IntOffset(
                x = (targetBounds.center.x - 150.dp.toPx()).toInt(),
                y = (targetBounds.bottom + 16.dp.toPx()).toInt()
            )
            TooltipPosition.Left -> IntOffset(
                x = (targetBounds.left - 300.dp.toPx()).toInt(),
                y = (targetBounds.center.y - 100.dp.toPx()).toInt()
            )
            TooltipPosition.Right -> IntOffset(
                x = (targetBounds.right + 16.dp.toPx()).toInt(),
                y = (targetBounds.center.y - 100.dp.toPx()).toInt()
            )
            TooltipPosition.Center -> IntOffset.Zero
        }
        
        return TooltipPositioning(tooltipOffset)
    }
}

/**
 * Data class for tooltip positioning.
 */
private data class TooltipPositioning(
    val offset: IntOffset
)