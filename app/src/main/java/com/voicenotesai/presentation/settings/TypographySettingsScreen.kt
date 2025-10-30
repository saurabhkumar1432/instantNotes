package com.voicenotesai.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.voicenotesai.R
import com.voicenotesai.presentation.accessibility.AccessibilityPreferences
import com.voicenotesai.presentation.accessibility.AccessibleBodyText
import com.voicenotesai.presentation.accessibility.AccessibleHeading
import com.voicenotesai.presentation.components.EnhancedListItem
import com.voicenotesai.presentation.components.ResponsiveContent
import com.voicenotesai.presentation.components.SettingsSection
import com.voicenotesai.presentation.components.TypographyPreview
import com.voicenotesai.presentation.layout.rememberEnhancedLayoutConfig
import com.voicenotesai.presentation.theme.SpacingScale
import com.voicenotesai.presentation.theme.TextSizeScale
import com.voicenotesai.presentation.theme.rememberAdvancedSpacing

/**
 * Typography and accessibility settings screen
 */
@Composable
fun TypographySettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    var textSizeScale by remember { mutableStateOf(TextSizeScale.Default) }
    var spacingScale by remember { mutableStateOf(SpacingScale.Default) }
    var highContrastMode by remember { mutableStateOf(false) }
    var boldText by remember { mutableStateOf(false) }
    var increasedLineSpacing by remember { mutableStateOf(false) }
    var useSystemFonts by remember { mutableStateOf(true) }
    var reducedMotion by remember { mutableStateOf(false) }
    
    val accessibilityPreferences = AccessibilityPreferences(
        textSizeScale = textSizeScale,
        spacingScale = spacingScale,
        highContrastMode = highContrastMode,
        boldText = boldText,
        increasedLineSpacing = increasedLineSpacing,
        useSystemFonts = useSystemFonts,
        reducedMotion = reducedMotion
    )
    
    val layoutConfig = rememberEnhancedLayoutConfig()
    val spacing = rememberAdvancedSpacing(spacingScale)
    
    ResponsiveContent(
        modifier = modifier.fillMaxSize(),
        accessibilityPreferences = accessibilityPreferences
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(spacing.screenMargin),
            verticalArrangement = Arrangement.spacedBy(spacing.sectionSpacing)
        ) {
            AccessibleHeading(
                text = stringResource(R.string.typography_accessibility_settings),
                level = 1,
                accessibilityPreferences = accessibilityPreferences
            )
            
            // Text Size Settings
            SettingsSection(
                title = stringResource(R.string.text_size),
                accessibilityPreferences = accessibilityPreferences
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.componentSpacing)
                ) {
                    AccessibleBodyText(
                        text = stringResource(R.string.text_size_description),
                        accessibilityPreferences = accessibilityPreferences
                    )
                    
                    TextSizeSlider(
                        value = textSizeScale,
                        onValueChange = { textSizeScale = it },
                        accessibilityPreferences = accessibilityPreferences
                    )
                }
            }
            
            // Spacing Settings
            SettingsSection(
                title = stringResource(R.string.spacing_layout),
                accessibilityPreferences = accessibilityPreferences
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.componentSpacing)
                ) {
                    AccessibleBodyText(
                        text = stringResource(R.string.spacing_description),
                        accessibilityPreferences = accessibilityPreferences
                    )
                    
                    SpacingSlider(
                        value = spacingScale,
                        onValueChange = { spacingScale = it },
                        accessibilityPreferences = accessibilityPreferences
                    )
                }
            }
            
            // Accessibility Options
            SettingsSection(
                title = stringResource(R.string.accessibility_options),
                accessibilityPreferences = accessibilityPreferences
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.componentSpacing)
                ) {
                    EnhancedListItem(
                        title = stringResource(R.string.high_contrast_mode),
                        subtitle = stringResource(R.string.high_contrast_description),
                        accessibilityPreferences = accessibilityPreferences,
                        onClick = { highContrastMode = !highContrastMode }
                    )
                    
                    EnhancedListItem(
                        title = stringResource(R.string.bold_text),
                        subtitle = stringResource(R.string.bold_text_description),
                        accessibilityPreferences = accessibilityPreferences,
                        onClick = { boldText = !boldText }
                    )
                    
                    EnhancedListItem(
                        title = stringResource(R.string.increased_line_spacing),
                        subtitle = stringResource(R.string.line_spacing_description),
                        accessibilityPreferences = accessibilityPreferences,
                        onClick = { increasedLineSpacing = !increasedLineSpacing }
                    )
                    
                    EnhancedListItem(
                        title = stringResource(R.string.use_system_fonts),
                        subtitle = stringResource(R.string.system_fonts_description),
                        accessibilityPreferences = accessibilityPreferences,
                        onClick = { useSystemFonts = !useSystemFonts }
                    )
                    
                    EnhancedListItem(
                        title = stringResource(R.string.reduced_motion),
                        subtitle = stringResource(R.string.reduced_motion_description),
                        accessibilityPreferences = accessibilityPreferences,
                        onClick = { reducedMotion = !reducedMotion }
                    )
                }
            }
            
            // Preview Section
            SettingsSection(
                title = stringResource(R.string.preview),
                accessibilityPreferences = accessibilityPreferences
            ) {
                TypographyPreview(
                    accessibilityPreferences = accessibilityPreferences
                )
            }
        }
    }
}

/**
 * Text size slider component
 */
@Composable
private fun TextSizeSlider(
    value: TextSizeScale,
    onValueChange: (TextSizeScale) -> Unit,
    accessibilityPreferences: AccessibilityPreferences,
    modifier: Modifier = Modifier
) {
    val scales = TextSizeScale.values()
    val currentIndex = scales.indexOf(value)
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.small),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.large),
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Slider(
            value = currentIndex.toFloat(),
            onValueChange = { newValue ->
                val newIndex = newValue.toInt().coerceIn(0, scales.size - 1)
                onValueChange(scales[newIndex])
            },
            valueRange = 0f..(scales.size - 1).toFloat(),
            steps = scales.size - 2
        )
        
        Text(
            text = stringResource(R.string.current_size, value.name),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

/**
 * Spacing slider component
 */
@Composable
private fun SpacingSlider(
    value: SpacingScale,
    onValueChange: (SpacingScale) -> Unit,
    accessibilityPreferences: AccessibilityPreferences,
    modifier: Modifier = Modifier
) {
    val scales = SpacingScale.values()
    val currentIndex = scales.indexOf(value)
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.compact),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.spacious),
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Slider(
            value = currentIndex.toFloat(),
            onValueChange = { newValue ->
                val newIndex = newValue.toInt().coerceIn(0, scales.size - 1)
                onValueChange(scales[newIndex])
            },
            valueRange = 0f..(scales.size - 1).toFloat(),
            steps = scales.size - 2
        )
        
        Text(
            text = stringResource(R.string.current_spacing, value.name),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}