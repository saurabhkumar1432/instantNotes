package com.voicenotesai.presentation.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.voicenotesai.presentation.accessibility.AccessibleCaption
import com.voicenotesai.presentation.accessibility.AccessibleHeading
import com.voicenotesai.presentation.components.EnhancedNoteCard
import com.voicenotesai.presentation.components.ResponsiveContent
import com.voicenotesai.presentation.components.TypographyPreview
import com.voicenotesai.presentation.layout.AdaptiveTwoPane
import com.voicenotesai.presentation.layout.ResponsiveContainer
import com.voicenotesai.presentation.layout.rememberEnhancedLayoutConfig
import com.voicenotesai.presentation.theme.SpacingScale
import com.voicenotesai.presentation.theme.TextSizeScale
import com.voicenotesai.presentation.theme.rememberAdvancedSpacing

/**
 * Demo screen showcasing enhanced typography and responsive layouts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypographyDemoScreen(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var textSizeScale by remember { mutableStateOf(TextSizeScale.Default) }
    var spacingScale by remember { mutableStateOf(SpacingScale.Default) }
    var highContrastMode by remember { mutableStateOf(false) }
    var boldText by remember { mutableStateOf(false) }
    var increasedLineSpacing by remember { mutableStateOf(false) }
    var reducedMotion by remember { mutableStateOf(false) }
    
    val accessibilityPreferences = AccessibilityPreferences(
        textSizeScale = textSizeScale,
        spacingScale = spacingScale,
        highContrastMode = highContrastMode,
        boldText = boldText,
        increasedLineSpacing = increasedLineSpacing,
        reducedMotion = reducedMotion
    )
    
    val layoutConfig = rememberEnhancedLayoutConfig()
    val spacing = rememberAdvancedSpacing(spacingScale)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AccessibleHeading(
                        text = "Typography & Layout Demo",
                        level = 1,
                        accessibilityPreferences = accessibilityPreferences
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.go_back_description)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        AdaptiveTwoPane(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            layoutConfig = layoutConfig,
            primaryPane = {
                // Settings Panel
                SettingsPanel(
                    textSizeScale = textSizeScale,
                    onTextSizeScaleChange = { textSizeScale = it },
                    spacingScale = spacingScale,
                    onSpacingScaleChange = { spacingScale = it },
                    highContrastMode = highContrastMode,
                    onHighContrastModeChange = { highContrastMode = it },
                    boldText = boldText,
                    onBoldTextChange = { boldText = it },
                    increasedLineSpacing = increasedLineSpacing,
                    onIncreasedLineSpacingChange = { increasedLineSpacing = it },
                    reducedMotion = reducedMotion,
                    onReducedMotionChange = { reducedMotion = it },
                    accessibilityPreferences = accessibilityPreferences
                )
            },
            secondaryPane = {
                // Preview Panel
                PreviewPanel(
                    accessibilityPreferences = accessibilityPreferences,
                    layoutConfig = layoutConfig
                )
            },
            singlePane = {
                // Single pane layout for smaller screens
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(spacing.screenMargin),
                    verticalArrangement = Arrangement.spacedBy(spacing.sectionSpacing)
                ) {
                    SettingsPanel(
                        textSizeScale = textSizeScale,
                        onTextSizeScaleChange = { textSizeScale = it },
                        spacingScale = spacingScale,
                        onSpacingScaleChange = { spacingScale = it },
                        highContrastMode = highContrastMode,
                        onHighContrastModeChange = { highContrastMode = it },
                        boldText = boldText,
                        onBoldTextChange = { boldText = it },
                        increasedLineSpacing = increasedLineSpacing,
                        onIncreasedLineSpacingChange = { increasedLineSpacing = it },
                        reducedMotion = reducedMotion,
                        onReducedMotionChange = { reducedMotion = it },
                        accessibilityPreferences = accessibilityPreferences
                    )
                    
                    PreviewPanel(
                        accessibilityPreferences = accessibilityPreferences,
                        layoutConfig = layoutConfig
                    )
                }
            }
        )
    }
}

/**
 * Settings panel for adjusting typography and accessibility options
 */
@Composable
private fun SettingsPanel(
    textSizeScale: TextSizeScale,
    onTextSizeScaleChange: (TextSizeScale) -> Unit,
    spacingScale: SpacingScale,
    onSpacingScaleChange: (SpacingScale) -> Unit,
    highContrastMode: Boolean,
    onHighContrastModeChange: (Boolean) -> Unit,
    boldText: Boolean,
    onBoldTextChange: (Boolean) -> Unit,
    increasedLineSpacing: Boolean,
    onIncreasedLineSpacingChange: (Boolean) -> Unit,
    reducedMotion: Boolean,
    onReducedMotionChange: (Boolean) -> Unit,
    accessibilityPreferences: AccessibilityPreferences,
    modifier: Modifier = Modifier
) {
    val spacing = rememberAdvancedSpacing(spacingScale)
    
    ResponsiveContainer(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(spacing.contentPadding),
            verticalArrangement = Arrangement.spacedBy(spacing.sectionSpacing)
        ) {
            AccessibleHeading(
                text = "Settings",
                level = 2,
                accessibilityPreferences = accessibilityPreferences
            )
            
            // Text Size Control
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(spacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(spacing.componentSpacing)
                ) {
                    AccessibleHeading(
                        text = stringResource(R.string.text_size),
                        level = 3,
                        accessibilityPreferences = accessibilityPreferences
                    )
                    
                    TextSizeSlider(
                        value = textSizeScale,
                        onValueChange = onTextSizeScaleChange
                    )
                }
            }
            
            // Spacing Control
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(spacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(spacing.componentSpacing)
                ) {
                    AccessibleHeading(
                        text = stringResource(R.string.spacing_layout),
                        level = 3,
                        accessibilityPreferences = accessibilityPreferences
                    )
                    
                    SpacingSlider(
                        value = spacingScale,
                        onValueChange = onSpacingScaleChange
                    )
                }
            }
            
            // Accessibility Options
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(spacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(spacing.componentSpacing)
                ) {
                    AccessibleHeading(
                        text = stringResource(R.string.accessibility_options),
                        level = 3,
                        accessibilityPreferences = accessibilityPreferences
                    )
                    
                    ToggleOption(
                        title = stringResource(R.string.high_contrast_mode),
                        checked = highContrastMode,
                        onCheckedChange = onHighContrastModeChange,
                        accessibilityPreferences = accessibilityPreferences
                    )
                    
                    ToggleOption(
                        title = stringResource(R.string.bold_text),
                        checked = boldText,
                        onCheckedChange = onBoldTextChange,
                        accessibilityPreferences = accessibilityPreferences
                    )
                    
                    ToggleOption(
                        title = stringResource(R.string.increased_line_spacing),
                        checked = increasedLineSpacing,
                        onCheckedChange = onIncreasedLineSpacingChange,
                        accessibilityPreferences = accessibilityPreferences
                    )
                    
                    ToggleOption(
                        title = stringResource(R.string.reduced_motion),
                        checked = reducedMotion,
                        onCheckedChange = onReducedMotionChange,
                        accessibilityPreferences = accessibilityPreferences
                    )
                }
            }
        }
    }
}

/**
 * Preview panel showing the effects of typography and layout changes
 */
@Composable
private fun PreviewPanel(
    accessibilityPreferences: AccessibilityPreferences,
    layoutConfig: com.voicenotesai.presentation.layout.EnhancedLayoutConfig,
    modifier: Modifier = Modifier
) {
    val spacing = rememberAdvancedSpacing(accessibilityPreferences.spacingScale)
    
    ResponsiveContainer(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(spacing.contentPadding),
            verticalArrangement = Arrangement.spacedBy(spacing.sectionSpacing)
        ) {
            AccessibleHeading(
                text = "Preview",
                level = 2,
                accessibilityPreferences = accessibilityPreferences
            )
            
            // Typography Preview
            TypographyPreview(
                accessibilityPreferences = accessibilityPreferences
            )
            
            // Sample Note Cards
            AccessibleHeading(
                text = "Sample Notes",
                level = 3,
                accessibilityPreferences = accessibilityPreferences
            )
            
            EnhancedNoteCard(
                title = "Meeting Notes",
                content = "Discussed project timeline and deliverables. Need to follow up with the design team about the new mockups.",
                timestamp = "2 hours ago",
                accessibilityPreferences = accessibilityPreferences,
                layoutConfig = layoutConfig
            )
            
            EnhancedNoteCard(
                title = "Ideas for App Enhancement",
                content = "Consider adding voice commands for hands-free operation. Also explore integration with calendar apps for automatic meeting notes.",
                timestamp = "Yesterday",
                accessibilityPreferences = accessibilityPreferences,
                layoutConfig = layoutConfig
            )
            
            // Layout Information
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(spacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(spacing.componentSpacing)
                ) {
                    AccessibleHeading(
                        text = "Layout Information",
                        level = 3,
                        accessibilityPreferences = accessibilityPreferences
                    )
                    
                    AccessibleCaption(
                        text = "Window Size: ${layoutConfig.windowSizeClass}",
                        accessibilityPreferences = accessibilityPreferences
                    )
                    
                    AccessibleCaption(
                        text = "Device Type: ${layoutConfig.deviceType}",
                        accessibilityPreferences = accessibilityPreferences
                    )
                    
                    AccessibleCaption(
                        text = "Screen: ${layoutConfig.screenWidth} × ${layoutConfig.screenHeight}",
                        accessibilityPreferences = accessibilityPreferences
                    )
                    
                    AccessibleCaption(
                        text = "Two Pane: ${if (layoutConfig.shouldUseTwoPane) "Yes" else "No"}",
                        accessibilityPreferences = accessibilityPreferences
                    )
                }
            }
        }
    }
}

/**
 * Toggle option component
 */
@Composable
private fun ToggleOption(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accessibilityPreferences: AccessibilityPreferences,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AccessibleBodyText(
            text = title,
            modifier = Modifier.weight(1f),
            accessibilityPreferences = accessibilityPreferences
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Text size slider component
 */
@Composable
private fun TextSizeSlider(
    value: TextSizeScale,
    onValueChange: (TextSizeScale) -> Unit,
    modifier: Modifier = Modifier
) {
    val scales = TextSizeScale.values()
    val currentIndex = scales.indexOf(value)
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
            text = "Current: ${value.name}",
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
    modifier: Modifier = Modifier
) {
    val scales = SpacingScale.values()
    val currentIndex = scales.indexOf(value)
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
            text = "Current: ${value.name}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}