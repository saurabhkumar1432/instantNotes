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
    val spacing = rememberAdvancedSpacing(accessibilityPreferences.spacingScale)
    
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
                .padding(spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(spacing.componentSpacing)
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
                    accessibilityPreferences = accessibilityPreferences
                )
                
                Spacer(modifier = Modifier.width(spacing.small))
                
                AccessibleCaption(
                    text = timestamp,
                    accessibilityPreferences = accessibilityPreferences
                )
            }
            
            AccessibleBodyText(
                text = content,
                modifier = Modifier.fillMaxWidth(),
                accessibilityPreferences = accessibilityPreferences
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
    val spacing = rememberAdvancedSpacing(accessibilityPreferences.spacingScale)
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        AccessibleHeading(
            text = title,
            level = 2,
            accessibilityPreferences = accessibilityPreferences
        )
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = if (accessibilityPreferences.highContrastMode) 1.0f else 0.7f
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(spacing.contentPadding),
                verticalArrangement = Arrangement.spacedBy(spacing.componentSpacing)
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
    val spacing = rememberAdvancedSpacing(accessibilityPreferences.spacingScale)
    
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
                .padding(spacing.contentPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.itemSpacing)
            ) {
                AccessibleHeading(
                    text = title,
                    level = 4,
                    accessibilityPreferences = accessibilityPreferences
                )
                
                subtitle?.let { sub ->
                    AccessibleCaption(
                        text = sub,
                        accessibilityPreferences = accessibilityPreferences
                    )
                }
            }
            
            trailing?.let { trailingText ->
                Spacer(modifier = Modifier.width(spacing.medium))
                AccessibleCaption(
                    text = trailingText,
                    accessibilityPreferences = accessibilityPreferences
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
    val spacing = rememberAdvancedSpacing(accessibilityPreferences.spacingScale)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.contentPadding),
            verticalArrangement = Arrangement.spacedBy(spacing.componentSpacing)
        ) {
            AccessibleHeading(
                text = "Typography Preview",
                level = 2,
                accessibilityPreferences = accessibilityPreferences
            )
            
            AccessibleHeading(
                text = "This is a heading",
                level = 3,
                accessibilityPreferences = accessibilityPreferences
            )
            
            AccessibleBodyText(
                text = "This is body text that demonstrates how the typography system adapts to different accessibility settings. The line height, letter spacing, and font size all scale appropriately.",
                accessibilityPreferences = accessibilityPreferences
            )
            
            AccessibleCaption(
                text = "This is caption text for metadata and secondary information",
                accessibilityPreferences = accessibilityPreferences
            )
        }
    }
}