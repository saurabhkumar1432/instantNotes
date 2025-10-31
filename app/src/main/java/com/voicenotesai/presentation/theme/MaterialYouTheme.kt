package com.voicenotesai.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Material You Theme System implementation for Voice Notes AI
 * Consolidates ModernColorScheme, GradientSystem, ModernSpacing, and ModernShapes
 * as specified in the UI consolidation modernization requirements.
 */

// Composition locals for accessing theme components
val LocalGradientSystem = staticCompositionLocalOf { GradientSystem }
val LocalModernSpacing = staticCompositionLocalOf { ModernSpacing }
val LocalModernShapes = staticCompositionLocalOf { ModernShapes }

/**
 * Main theme composable that provides the complete Material You theme system
 */
@Composable
fun MaterialYouTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        ModernDarkColorScheme
    } else {
        ModernLightColorScheme
    }
    
    CompositionLocalProvider(
        LocalGradientSystem provides GradientSystem,
        LocalModernSpacing provides ModernSpacing,
        LocalModernShapes provides ModernShapes
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}

/**
 * Extension properties for easy access to theme components
 */
object MaterialYouTheme {
    val gradients: GradientSystem
        @Composable
        get() = LocalGradientSystem.current
    
    val spacing: ModernSpacing
        @Composable
        get() = LocalModernSpacing.current
    
    val shapes: ModernShapes
        @Composable
        get() = LocalModernShapes.current
}

/**
 * Helper composables for accessing specific theme elements
 */
@Composable
fun rememberGradientSystem() = LocalGradientSystem.current

@Composable
fun rememberModernSpacing() = LocalModernSpacing.current

@Composable
fun rememberModernShapes() = LocalModernShapes.current