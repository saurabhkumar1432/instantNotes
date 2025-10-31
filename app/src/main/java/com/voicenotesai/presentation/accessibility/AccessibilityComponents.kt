package com.voicenotesai.presentation.accessibility

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Accessibility preferences for typography and layout
 */
data class AccessibilityPreferences(
    val fontSizeScale: Float = 1.0f,
    val spacingScale: Float = 1.0f,
    val highContrast: Boolean = false,
    val highContrastMode: Boolean = false,
    val reducedMotion: Boolean = false,
    val screenReaderEnabled: Boolean = false
) {
    companion object {
        val Default = AccessibilityPreferences()
    }
}

/**
 * Accessible heading component with proper semantics
 */
@Composable
fun AccessibleHeading(
    text: String,
    modifier: Modifier = Modifier,
    level: Int = 1,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    preferences: AccessibilityPreferences = AccessibilityPreferences.Default
) {
    Text(
        text = text,
        modifier = modifier.semantics {
            heading()
            contentDescription = text
        },
        style = style.copy(
            fontSize = style.fontSize * preferences.fontSizeScale,
            color = if (preferences.highContrast) {
                MaterialTheme.colorScheme.onSurface
            } else {
                color
            }
        )
    )
}

/**
 * Accessible body text component
 */
@Composable
fun AccessibleBodyText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    preferences: AccessibilityPreferences = AccessibilityPreferences.Default
) {
    Text(
        text = text,
        modifier = modifier.semantics {
            contentDescription = text
        },
        style = style.copy(
            fontSize = style.fontSize * preferences.fontSizeScale,
            color = if (preferences.highContrast) {
                MaterialTheme.colorScheme.onSurface
            } else {
                color
            }
        )
    )
}

/**
 * Accessible caption component
 */
@Composable
fun AccessibleCaption(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    preferences: AccessibilityPreferences = AccessibilityPreferences.Default
) {
    Text(
        text = text,
        modifier = modifier.semantics {
            contentDescription = text
        },
        style = style.copy(
            fontSize = style.fontSize * preferences.fontSizeScale,
            color = if (preferences.highContrast) {
                MaterialTheme.colorScheme.onSurface
            } else {
                color
            }
        )
    )
}

/**
 * Remember accessibility preferences
 */
@Composable
fun rememberAccessibilityPreferences(): AccessibilityPreferences {
    // In a real app, this would come from user settings
    return remember { AccessibilityPreferences.Default }
}

/**
 * Accessibility helper for headings
 */
val headingAccessibility = AccessibilityPreferences.Default

/**
 * Accessibility helper for cards
 */
val cardAccessibility = AccessibilityPreferences.Default

/**
 * Accessibility helper for toggles
 */
val toggleAccessibility = AccessibilityPreferences.Default