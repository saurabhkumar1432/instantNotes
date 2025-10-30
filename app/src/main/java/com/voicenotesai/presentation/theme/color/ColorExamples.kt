package com.voicenotesai.presentation.theme.color

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voicenotesai.presentation.theme.Spacing

/**
 * Example composables demonstrating the enhanced color system usage.
 */

/**
 * Status indicator using semantic colors.
 */
@Composable
fun StatusIndicator(
    state: ColorState,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val stateColors = ColorHelpers.getStateColors(state)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = stateColors.container
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(stateColors.primary)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = stateColors.onContainer
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = stateColors.onContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Voice level indicator using voice visualization colors.
 */
@Composable
fun VoiceLevelIndicator(
    level: Float,
    modifier: Modifier = Modifier
) {
    val voiceColor = ColorHelpers.getVoiceColor(level)
    val semanticColors = MaterialTheme.colorScheme.semantic
    
    Box(
        modifier = modifier
            .size(width = 100.dp, height = 8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(semanticColors.neutral30)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(level.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            semanticColors.voiceWeak,
                            voiceColor
                        )
                    )
                )
        )
    }
}

/**
 * Glass surface example using enhanced glass colors.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val glassColors = ColorHelpers.getGlassColors()
    
    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                color = glassColors.outline,
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        color = glassColors.surface,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier.padding(Spacing.large)
        ) {
            content()
        }
    }
}

/**
 * Adaptive color text that changes based on background luminance.
 */
@Composable
fun AdaptiveColorText(
    text: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    val textColor = ColorUtils.getOnColor(backgroundColor)
    
    Text(
        text = text,
        color = textColor,
        modifier = modifier
    )
}

/**
 * High contrast button that ensures accessibility.
 */
@Composable
fun AccessibleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary
) {
    val accessibility = MaterialTheme.colorScheme.accessibility
    val adjustedBackgroundColor = if (accessibility.highContrastMode) {
        backgroundColor
    } else {
        backgroundColor.copy(alpha = 0.9f)
    }
    
    val textColor = ColorUtils.getOnColor(adjustedBackgroundColor)
        .let { color ->
            if (accessibility.highContrastMode) {
                color.ensureAccessibility(adjustedBackgroundColor)
            } else {
                color
            }
        }
    
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = adjustedBackgroundColor,
            contentColor = textColor
        )
    ) {
        Text(text = text)
    }
}

/**
 * Color contrast demo for testing accessibility.
 */
@Composable
fun ColorContrastDemo(
    foregroundColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    val contrastRatio = ColorUtils.contrastRatio(foregroundColor, backgroundColor)
    val meetsStandards = ColorUtils.meetsContrastStandards(foregroundColor, backgroundColor)
    val meetsHighStandards = ColorUtils.meetsHighContrastStandards(foregroundColor, backgroundColor)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Text(
                text = stringResource(id = com.voicenotesai.R.string.sample_text),
                color = foregroundColor,
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                text = stringResource(id = com.voicenotesai.R.string.contrast_ratio_format, "%.2f".format(contrastRatio)),
                color = foregroundColor,
                style = MaterialTheme.typography.bodySmall
            )
            
            Text(
                text = when {
                    meetsHighStandards -> stringResource(id = com.voicenotesai.R.string.contrast_aaa)
                    meetsStandards -> stringResource(id = com.voicenotesai.R.string.contrast_aa)
                    else -> stringResource(id = com.voicenotesai.R.string.contrast_below)
                },
                color = foregroundColor,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Extension function to easily access semantic colors.
 */
private fun Color.ensureAccessibility(background: Color): Color {
    return ColorUtils.run { this@ensureAccessibility.ensureAccessibility(background) }
}