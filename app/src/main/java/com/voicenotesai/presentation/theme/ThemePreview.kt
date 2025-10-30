package com.voicenotesai.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.voicenotesai.domain.model.ColorBlindnessType
import com.voicenotesai.domain.model.ContrastLevel
import com.voicenotesai.domain.model.DarkThemePreference
import com.voicenotesai.domain.model.ThemePreferences
import com.voicenotesai.presentation.theme.color.ColorHelpers
import com.voicenotesai.presentation.theme.color.ColorPreviews
import com.voicenotesai.presentation.theme.color.ColorState

/**
 * Preview composables for demonstrating theme engine functionality.
 */

@Composable
fun ThemeEnginePreview(
    themePreferences: ThemePreferences = ThemePreferences(),
    modifier: Modifier = Modifier
) {
    VoiceNotesAITheme(themePreferences = themePreferences) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Theme Engine Preview",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                item {
                    ColorSchemePreview()
                }
                
                item {
                    SemanticColorsPreview()
                }
                
                item {
                    StateColorsPreview()
                }
                
                item {
                    VoiceVisualizationPreview()
                }
                
                item {
                    AccessibilityPreview()
                }
            }
        }
    }
}

@Composable
private fun ColorSchemePreview() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Material 3 Color Scheme",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val colorScheme = MaterialTheme.colorScheme
            val colors = listOf(
                "Primary" to colorScheme.primary,
                "Secondary" to colorScheme.secondary,
                "Tertiary" to colorScheme.tertiary,
                "Error" to colorScheme.error,
                "Surface" to colorScheme.surface,
                "Background" to colorScheme.background
            )
            
            colors.chunked(3).forEach { rowColors ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowColors.forEach { (name, color) ->
                        ColorSwatch(
                            name = name,
                            color = color,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill remaining space if row is not complete
                    repeat(3 - rowColors.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SemanticColorsPreview() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Semantic Colors",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val semanticColors = ColorPreviews.getAllSemanticColors()
            
            semanticColors.chunked(3).forEach { rowColors ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowColors.forEach { (name, color) ->
                        ColorSwatch(
                            name = name,
                            color = color,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill remaining space if row is not complete
                    repeat(3 - rowColors.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun StateColorsPreview() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "State Colors",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val states = listOf(
                ColorState.Success,
                ColorState.Warning,
                ColorState.Error,
                ColorState.Info,
                ColorState.Recording,
                ColorState.Processing
            )
            
            states.chunked(2).forEach { rowStates ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowStates.forEach { state ->
                        StateColorCard(
                            state = state,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowStates.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun VoiceVisualizationPreview() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Voice Visualization Colors",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val intensities = listOf(0.1f, 0.3f, 0.6f, 0.9f)
                val labels = listOf("Weak", "Medium", "Strong", "Peak")
                
                intensities.forEachIndexed { index, intensity ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ColorHelpers.getVoiceColor(intensity))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = labels[index],
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccessibilityPreview() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Accessibility Features",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val isAccessible = ThemeUtils.validateCurrentThemeAccessibility(MaterialTheme.colorScheme)
            val (glassSurface, glassOutline, glassShadow) = ThemeUtils.getThemeAwareGlassColors()
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AccessibilityItem(
                    label = "WCAG Compliance",
                    value = if (isAccessible) "✓ Passed" else "✗ Failed",
                    isPositive = isAccessible
                )
                
                AccessibilityItem(
                    label = "Glass Surface Alpha",
                    value = "${(glassSurface.alpha * 100).toInt()}%",
                    isPositive = true
                )
                
                AccessibilityItem(
                    label = "Adaptive Text Color",
                    value = "Dynamic",
                    isPositive = true
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    name: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun StateColorCard(
    state: ColorState,
    modifier: Modifier = Modifier
) {
    val stateColors = ColorHelpers.getStateColors(state)
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = stateColors.container
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(stateColors.primary)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.name,
                style = MaterialTheme.typography.bodySmall,
                color = stateColors.onContainer
            )
        }
    }
}

@Composable
private fun AccessibilityItem(
    label: String,
    value: String,
    isPositive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isPositive) {
                ColorHelpers.getStateColors(ColorState.Success).primary
            } else {
                ColorHelpers.getStateColors(ColorState.Error).primary
            },
            fontWeight = FontWeight.Medium
        )
    }
}

// Preview functions for different theme configurations

@Preview(name = "Light Theme - Standard")
@Composable
private fun LightThemePreview() {
    ThemeEnginePreview(
        themePreferences = ThemePreferences(
            darkTheme = DarkThemePreference.Light,
            contrastLevel = ContrastLevel.Standard
        )
    )
}

@Preview(name = "Dark Theme - Standard")
@Composable
private fun DarkThemePreview() {
    ThemeEnginePreview(
        themePreferences = ThemePreferences(
            darkTheme = DarkThemePreference.Dark,
            contrastLevel = ContrastLevel.Standard
        )
    )
}

@Preview(name = "High Contrast Light")
@Composable
private fun HighContrastLightPreview() {
    ThemeEnginePreview(
        themePreferences = ThemePreferences(
            darkTheme = DarkThemePreference.Light,
            contrastLevel = ContrastLevel.High,
            highContrast = true
        )
    )
}

@Preview(name = "Color Blindness Support")
@Composable
private fun ColorBlindnessPreview() {
    ThemeEnginePreview(
        themePreferences = ThemePreferences(
            darkTheme = DarkThemePreference.Light,
            colorBlindnessType = ColorBlindnessType.Deuteranopia
        )
    )
}

@Preview(name = "Custom Seed Color")
@Composable
private fun CustomSeedColorPreview() {
    ThemeEnginePreview(
        themePreferences = ThemePreferences(
            darkTheme = DarkThemePreference.Light,
            colorSeed = Color(0xFF6750A4),
            dynamicColor = false
        )
    )
}