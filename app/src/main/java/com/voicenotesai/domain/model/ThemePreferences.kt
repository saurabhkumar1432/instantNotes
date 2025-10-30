package com.voicenotesai.domain.model

import androidx.compose.ui.graphics.Color

/**
 * User preferences for theme customization and accessibility.
 */
data class ThemePreferences(
    val colorSeed: Color? = null,
    val contrastLevel: ContrastLevel = ContrastLevel.Standard,
    val reducedMotion: Boolean = false,
    val highContrast: Boolean = false,
    val colorBlindnessType: ColorBlindnessType? = null,
    val dynamicColor: Boolean = true,
    val darkTheme: DarkThemePreference = DarkThemePreference.FollowSystem,
    val reducedTransparency: Boolean = false,
    val largeText: Boolean = false,
    val extraLargeText: Boolean = false
)

/**
 * Contrast levels for accessibility.
 */
enum class ContrastLevel {
    Standard,
    Medium,
    High
}

/**
 * Types of color blindness for color scheme adaptation.
 */
enum class ColorBlindnessType {
    Protanopia,     // Red-blind
    Deuteranopia,   // Green-blind
    Tritanopia,     // Blue-blind
    Protanomaly,    // Red-weak
    Deuteranomaly,  // Green-weak
    Tritanomaly     // Blue-weak
}

/**
 * Dark theme preference options.
 */
enum class DarkThemePreference {
    Light,
    Dark,
    FollowSystem
}

/**
 * Theme configuration generated from user preferences.
 */
data class ThemeConfiguration(
    val colorScheme: androidx.compose.material3.ColorScheme,
    val semanticColors: com.voicenotesai.presentation.theme.color.SemanticColors,
    val isDarkTheme: Boolean,
    val animationScale: Float,
    val textScale: Float,
    val contrastRatio: Float,
    val reducedTransparency: Boolean
)

/**
 * Accessibility needs assessment.
 */
data class AccessibilityNeeds(
    val highContrast: Boolean = false,
    val largeText: Boolean = false,
    val reducedMotion: Boolean = false,
    val colorBlindness: ColorBlindnessType? = null,
    val screenReader: Boolean = false,
    val voiceControl: Boolean = false
)

/**
 * Theme modifications for accessibility compliance.
 */
data class ThemeModifications(
    val contrastBoost: Float = 0f,
    val textSizeMultiplier: Float = 1f,
    val animationScale: Float = 1f,
    val transparencyReduction: Float = 0f,
    val colorAdjustments: Map<String, Color> = emptyMap()
)