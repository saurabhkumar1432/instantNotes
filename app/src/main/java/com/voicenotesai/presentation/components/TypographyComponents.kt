package com.voicenotesai.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.voicenotesai.presentation.accessibility.AccessibilityPreferences
import com.voicenotesai.presentation.accessibility.AccessibleBodyText
import com.voicenotesai.presentation.accessibility.AccessibleCaption
import com.voicenotesai.presentation.accessibility.AccessibleHeading
import com.voicenotesai.presentation.layout.EnhancedLayoutConfig
import com.voicenotesai.presentation.layout.ResponsiveContainer
import com.voicenotesai.presentation.layout.rememberEnhancedLayoutConfig
import com.voicenotesai.presentation.theme.SpacingScale
import com.voicenotesai.presentation.theme.rememberAdvancedSpacing

/**
 * Enhanced note card with responsive typography and layout
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EnhancedNoteCard(
    title: String,
    content: String,
    timestamp: String,
    modifier: Modifier = Modifier,
    accessibilityPreferences: AccessibilityPreferences = AccessibilityPreferences(),
    layoutConfig: EnhancedLayoutConfig = rememberEnhancedLayoutConfig(),
    onClick: () -> Unit = {}
) {
    val spacing = rememberAdvancedSpacing(SpacingScale.Default)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(layoutConfig.listItemHeight),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (accessibilityPreferences.highContrastMode) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (accessibilityPreferences.reducedMotion) 2.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                AccessibleHeading(
                    text = title,
                    level = 3,
                    modifier = Modifier.weight(1f),
                    preferences = accessibilityPreferences
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                AccessibleCaption(
                    text = timestamp,
                    preferences = accessibilityPreferences
                )
            }
            
            AccessibleBodyText(
                text = content,
                modifier = Modifier.fillMaxWidth(),
                preferences = accessibilityPreferences
            )
        }
    }
}

/**
 * Responsive settings section with proper typography hierarchy
 */
@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    accessibilityPreferences: AccessibilityPreferences = AccessibilityPreferences(),
    content: @Composable () -> Unit
) {
    val spacing = rememberAdvancedSpacing(SpacingScale.Default)
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AccessibleHeading(
            text = title,
            level = 2,
            preferences = accessibilityPreferences
        )
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = if (accessibilityPreferences.highContrastMode) 1.0f else 0.7f
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * Responsive content container with proper typography scaling
 */
@Composable
fun ResponsiveContent(
    modifier: Modifier = Modifier,
    accessibilityPreferences: AccessibilityPreferences = AccessibilityPreferences(),
    content: @Composable () -> Unit
) {
    ResponsiveContainer(
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Enhanced list item with accessibility features
 */
@Composable
fun EnhancedListItem(
    title: String,
    subtitle: String? = null,
    trailing: String? = null,
    modifier: Modifier = Modifier,
    accessibilityPreferences: AccessibilityPreferences = AccessibilityPreferences(),
    layoutConfig: EnhancedLayoutConfig = rememberEnhancedLayoutConfig(),
    onClick: () -> Unit = {}
) {
    val spacing = rememberAdvancedSpacing(SpacingScale.Default)
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(layoutConfig.listItemHeight),
        onClick = onClick,
        color = if (accessibilityPreferences.highContrastMode) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.background
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AccessibleHeading(
                    text = title,
                    level = 4,
                    preferences = accessibilityPreferences
                )
                
                subtitle?.let { sub ->
                    AccessibleCaption(
                        text = sub,
                        preferences = accessibilityPreferences
                    )
                }
            }
            
            trailing?.let { trailingText ->
                Spacer(modifier = Modifier.width(16.dp))
                AccessibleCaption(
                    text = trailingText,
                    preferences = accessibilityPreferences
                )
            }
        }
    }
}

/**
 * Typography preview component for settings
 */
@Composable
fun TypographyPreview(
    accessibilityPreferences: AccessibilityPreferences = AccessibilityPreferences(),
    modifier: Modifier = Modifier
) {
    val spacing = rememberAdvancedSpacing(SpacingScale.Default)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AccessibleHeading(
                text = "Typography Preview",
                level = 2,
                preferences = accessibilityPreferences
            )
            
            AccessibleHeading(
                text = "This is a heading",
                level = 3,
                preferences = accessibilityPreferences
            )
            
            AccessibleBodyText(
                text = "This is body text that demonstrates how the typography system adapts to different accessibility settings. The line height, letter spacing, and font size all scale appropriately.",
                preferences = accessibilityPreferences
            )
            
            AccessibleCaption(
                text = "This is caption text for metadata and secondary information",
                preferences = accessibilityPreferences
            )
        }
    }
}