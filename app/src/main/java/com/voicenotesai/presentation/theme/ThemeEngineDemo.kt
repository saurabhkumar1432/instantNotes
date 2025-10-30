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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.voicenotesai.R
import com.voicenotesai.domain.model.ColorBlindnessType
import com.voicenotesai.domain.model.ContrastLevel
import com.voicenotesai.domain.model.DarkThemePreference
import com.voicenotesai.domain.model.ThemePreferences

/**
 * Demo composable showcasing the dynamic theming engine capabilities.
 */
@Composable
fun ThemeEngineDemo(
    modifier: Modifier = Modifier
) {
    var themePreferences by remember {
        mutableStateOf(
            ThemePreferences(
                darkTheme = DarkThemePreference.FollowSystem,
                dynamicColor = true,
                contrastLevel = ContrastLevel.Standard,
                highContrast = false,
                colorBlindnessType = null,
                reducedTransparency = false
            )
        )
    }

    VoiceNotesAITheme(themePreferences = themePreferences) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = stringResource(R.string.app_name) + " - Theme Engine Demo",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Theme Controls
                ThemeControlsCard(
                    themePreferences = themePreferences,
                    onThemePreferencesChanged = { themePreferences = it }
                )

                // Color Showcase
                ColorShowcaseCard()

                // Accessibility Features
                AccessibilityShowcaseCard()

                // Voice Visualization Demo
                VoiceVisualizationCard()
            }
        }
    }
}

@Composable
private fun ThemeControlsCard(
    themePreferences: ThemePreferences,
    onThemePreferencesChanged: (ThemePreferences) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Theme Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Dynamic Color Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dynamic Color",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = themePreferences.dynamicColor,
                    onCheckedChange = { 
                        onThemePreferencesChanged(
                            themePreferences.copy(dynamicColor = it)
                        )
                    }
                )
            }

            // High Contrast Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "High Contrast",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = themePreferences.highContrast,
                    onCheckedChange = { 
                        onThemePreferencesChanged(
                            themePreferences.copy(
                                highContrast = it,
                                contrastLevel = if (it) ContrastLevel.High else ContrastLevel.Standard
                            )
                        )
                    }
                )
            }

            // Reduced Transparency Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reduced Transparency",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = themePreferences.reducedTransparency,
                    onCheckedChange = { 
                        onThemePreferencesChanged(
                            themePreferences.copy(reducedTransparency = it)
                        )
                    }
                )
            }

            // Theme Mode Buttons
            Text(
                text = "Theme Mode",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { 
                        onThemePreferencesChanged(
                            themePreferences.copy(darkTheme = DarkThemePreference.Light)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Light")
                }
                Button(
                    onClick = { 
                        onThemePreferencesChanged(
                            themePreferences.copy(darkTheme = DarkThemePreference.Dark)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Dark")
                }
                Button(
                    onClick = { 
                        onThemePreferencesChanged(
                            themePreferences.copy(darkTheme = DarkThemePreference.FollowSystem)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Auto")
                }
            }

            // Color Blindness Support
            Text(
                text = "Color Blindness Support",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { 
                        onThemePreferencesChanged(
                            themePreferences.copy(colorBlindnessType = null)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("None", maxLines = 1)
                }
                Button(
                    onClick = { 
                        onThemePreferencesChanged(
                            themePreferences.copy(colorBlindnessType = ColorBlindnessType.Deuteranopia)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Green", maxLines = 1)
                }
                Button(
                    onClick = { 
                        onThemePreferencesChanged(
                            themePreferences.copy(colorBlindnessType = ColorBlindnessType.Protanopia)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Red", maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ColorShowcaseCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Color Showcase",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
                        Column(
                            modifier = Modifier.weight(1f),
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
                    // Fill remaining space if row is not complete
                    repeat(3 - rowColors.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun AccessibilityShowcaseCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Accessibility Features",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val (glassSurface, glassOutline, glassShadow) = ThemeUtils.getThemeAwareGlassColors()

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AccessibilityFeatureRow(
                    label = "Glass Surface Alpha",
                    value = "${(glassSurface.alpha * 100).toInt()}%"
                )
                
                AccessibilityFeatureRow(
                    label = "Adaptive Text Color",
                    value = "Dynamic"
                )
                
                AccessibilityFeatureRow(
                    label = "Contrast Optimization",
                    value = "Active"
                )
            }

            // Glass Effect Demo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(glassSurface)
            ) {
                Text(
                    text = "Glass Effect Demo",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeUtils.getAdaptiveTextColor(glassSurface)
                )
            }
        }
    }
}

@Composable
private fun VoiceVisualizationCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Voice Visualization",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
                                .background(ThemeUtils.getAccessibleVoiceColor(intensity))
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
private fun AccessibilityFeatureRow(
    label: String,
    value: String
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
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(name = "Theme Engine Demo")
@Composable
private fun ThemeEngineDemoPreview() {
    ThemeEngineDemo()
}