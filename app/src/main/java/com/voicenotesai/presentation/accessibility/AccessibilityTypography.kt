package com.voicenotesai.presentation.accessibility

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicenotesai.presentation.theme.AdvancedTypography
import com.voicenotesai.presentation.theme.SpacingScale
import com.voicenotesai.presentation.theme.TextSizeScale
import com.voicenotesai.presentation.theme.rememberAdvancedSpacing
import com.voicenotesai.presentation.theme.rememberAdvancedTypography

/**
 * Accessibility preferences for typography and layout
 */
@Stable
data class AccessibilityPreferences(
    val textSizeScale: TextSizeScale = TextSizeScale.Default,
    val spacingScale: SpacingScale = SpacingScale.Default,
    val highContrastMode: Boolean = false,
    val reducedMotion: Boolean = false,
    val useSystemFonts: Boolean = true,
    val boldText: Boolean = false,
    val increasedLineSpacing: Boolean = false
)

/**
 * Enhanced text component with accessibility features
 */
@Composable
fun AccessibleText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    accessibilityPreferences: AccessibilityPreferences = AccessibilityPreferences(),
    semanticLabel: String? = null
) {
    val advancedTypography = rememberAdvancedTypography(
        textSizeScale = accessibilityPreferences.textSizeScale,
        useSystemFonts = accessibilityPreferences.useSystemFonts,
        highContrastMode = accessibilityPreferences.highContrastMode
    )
    
    val spacing = rememberAdvancedSpacing(
        spacingScale = accessibilityPreferences.spacingScale
    )
    
    // Apply accessibility modifications to text style
    val accessibleStyle = style.copy(
        fontSize = if (accessibilityPreferences.textSizeScale != TextSizeScale.Default) {
            style.fontSize * accessibilityPreferences.textSizeScale.multiplier
        } else {
            style.fontSize
        },
        fontWeight = if (accessibilityPreferences.boldText) {
            when (style.fontWeight) {
                FontWeight.Normal -> FontWeight.Medium
                FontWeight.Medium -> FontWeight.SemiBold
                FontWeight.SemiBold -> FontWeight.Bold
                else -> style.fontWeight
            }
        } else {
            style.fontWeight
        },
        lineHeight = if (accessibilityPreferences.increasedLineSpacing) {
            style.lineHeight * 1.2f
        } else {
            style.lineHeight
        }
    )
    
    Text(
        text = text,
        modifier = modifier.padding(vertical = spacing.textSpacing),
        style = accessibleStyle,
        color = if (accessibilityPreferences.highContrastMode && color == Color.Unspecified) {
            MaterialTheme.colorScheme.onSurface
        } else {
            color
        }
    )
}

/**
 * Accessible heading component with proper semantic structure
 */
@Composable
fun AccessibleHeading(
    text: String,
    level: Int = 1,
    modifier: Modifier = Modifier,
    accessibilityPreferences: AccessibilityPreferences = AccessibilityPreferences()
) {
    val advancedTypography = rememberAdvancedTypography(
        textSizeScale = accessibilityPreferences.textSizeScale,
        useSystemFonts = accessibilityPreferences.useSystemFonts,
        highContrastMode = accessibilityPreferences.highContrastMode
    )
    
    val style = when (level) {
        1 -> advancedTypography.typography.headlineLarge
        2 -> advancedTypography.typography.headlineMedium
        3 -> advancedTypography.typography.headlineSmall
        4 -> advancedTypography.typography.titleLarge
        5 -> advancedTypography.typography.titleMedium
        else -> advancedTypography.typography.titleSmall
    }
    
    AccessibleText(
        text = text,
        modifier = modifier,
        style = style,
        accessibilityPreferences = accessibilityPreferences,
        semanticLabel = "Heading level $level: $text"
    )
}

/**
 * Accessible body text with optimal reading settings
 */
@Composable
fun AccessibleBodyText(
    text: String,
    modifier: Modifier = Modifier,
    accessibilityPreferences: AccessibilityPreferences = AccessibilityPreferences()
) {
    val advancedTypography = rememberAdvancedTypography(
        textSizeScale = accessibilityPreferences.textSizeScale,
        useSystemFonts = accessibilityPreferences.useSystemFonts,
        highContrastMode = accessibilityPreferences.highContrastMode
    )
    
    AccessibleText(
        text = text,
        modifier = modifier,
        style = AdvancedTypography.Extended.noteContent(advancedTypography),
        accessibilityPreferences = accessibilityPreferences
    )
}

/**
 * Accessible caption text for metadata and secondary information
 */
@Composable
fun AccessibleCaption(
    text: String,
    modifier: Modifier = Modifier,
    accessibilityPreferences: AccessibilityPreferences = AccessibilityPreferences()
) {
    val advancedTypography = rememberAdvancedTypography(
        textSizeScale = accessibilityPreferences.textSizeScale,
        useSystemFonts = accessibilityPreferences.useSystemFonts,
        highContrastMode = accessibilityPreferences.highContrastMode
    )
    
    AccessibleText(
        text = text,
        modifier = modifier,
        style = AdvancedTypography.Extended.caption(advancedTypography),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        accessibilityPreferences = accessibilityPreferences
    )
}

/**
 * Accessible button text with proper contrast and sizing
 */
@Composable
fun AccessibleButtonText(
    text: String,
    modifier: Modifier = Modifier,
    accessibilityPreferences: AccessibilityPreferences = AccessibilityPreferences()
) {
    val advancedTypography = rememberAdvancedTypography(
        textSizeScale = accessibilityPreferences.textSizeScale,
        useSystemFonts = accessibilityPreferences.useSystemFonts,
        highContrastMode = accessibilityPreferences.highContrastMode
    )
    
    AccessibleText(
        text = text,
        modifier = modifier,
        style = AdvancedTypography.Extended.buttonText(advancedTypography),
        accessibilityPreferences = accessibilityPreferences
    )
}

/**
 * Accessible error text with proper styling and contrast
 */
@Composable
fun AccessibleErrorText(
    text: String,
    modifier: Modifier = Modifier,
    accessibilityPreferences: AccessibilityPreferences = AccessibilityPreferences()
) {
    val advancedTypography = rememberAdvancedTypography(
        textSizeScale = accessibilityPreferences.textSizeScale,
        useSystemFonts = accessibilityPreferences.useSystemFonts,
        highContrastMode = accessibilityPreferences.highContrastMode
    )
    
    AccessibleText(
        text = text,
        modifier = modifier,
        style = AdvancedTypography.Extended.errorText(advancedTypography),
        color = MaterialTheme.colorScheme.error,
        accessibilityPreferences = accessibilityPreferences,
        semanticLabel = "Error: $text"
    )
}

/**
 * Composable that provides accessibility preferences to child components
 */
@Composable
fun AccessibilityProvider(
    preferences: AccessibilityPreferences,
    content: @Composable () -> Unit
) {
    val advancedTypography = rememberAdvancedTypography(
        textSizeScale = preferences.textSizeScale,
        useSystemFonts = preferences.useSystemFonts,
        highContrastMode = preferences.highContrastMode
    )
    
    CompositionLocalProvider(
        LocalTextStyle provides advancedTypography.typography.bodyMedium
    ) {
        content()
    }
}

/**
 * Get system accessibility preferences
 */
@Composable
fun rememberSystemAccessibilityPreferences(): AccessibilityPreferences {
    val context = LocalContext.current
    
    // In a real implementation, you would read from system settings
    // For now, return default preferences
    return AccessibilityPreferences()
}