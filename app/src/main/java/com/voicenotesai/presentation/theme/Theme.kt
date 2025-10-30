package com.voicenotesai.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.voicenotesai.domain.model.ColorBlindnessType
import com.voicenotesai.domain.model.ContrastLevel
import com.voicenotesai.domain.model.DarkThemePreference
import com.voicenotesai.domain.model.ThemePreferences
import com.voicenotesai.presentation.theme.color.EnhancedColorTheme
import javax.inject.Inject

/**
 * Enhanced VoiceNotesAI theme with dynamic theming engine integration.
 */
@Composable
fun VoiceNotesAITheme(
    themePreferences: ThemePreferences? = null,
    themeEngine: ThemeEngine? = null,
    textSizeScale: TextSizeScale = TextSizeScale.Default,
    spacingScale: SpacingScale = SpacingScale.Default,
    content: @Composable () -> Unit
) {
    // Use provided theme engine or get from DI
    val engine = themeEngine ?: hiltViewModel<ThemeViewModel>().themeEngine
    
    // Use provided preferences or create default ones
    val preferences = themePreferences ?: remember {
        ThemePreferences(
            darkTheme = DarkThemePreference.FollowSystem,
            dynamicColor = true,
            contrastLevel = ContrastLevel.Standard
        )
    }
    
    // Generate theme configuration
    val themeConfiguration = engine.adaptToSystemSettings()
    val colorScheme = engine.generateDynamicTheme(preferences)
    
    // Determine dark theme state
    val isDarkTheme = when (preferences.darkTheme) {
        DarkThemePreference.Light -> false
        DarkThemePreference.Dark -> true
        DarkThemePreference.FollowSystem -> isSystemInDarkTheme()
    }
    
    // Create advanced typography with accessibility settings
    val advancedTypography = rememberAdvancedTypography(
        textSizeScale = textSizeScale,
        useSystemFonts = true,
        highContrastMode = preferences.highContrast
    )
    
    EnhancedColorTheme(
        darkTheme = isDarkTheme,
        highContrastMode = preferences.highContrast,
        colorBlindnessSupport = preferences.colorBlindnessType != null,
        reducedTransparency = preferences.reducedTransparency
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = advancedTypography.typography,
            shapes = AppShapes,
            content = content
        )
    }
}

/**
 * Simplified theme composable for backward compatibility.
 */
@Composable
fun VoiceNotesAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    highContrastMode: Boolean = false,
    colorBlindnessSupport: Boolean = false,
    reducedTransparency: Boolean = false,
    colorSeed: Color? = null,
    content: @Composable () -> Unit
) {
    val preferences = remember(
        darkTheme, dynamicColor, highContrastMode, 
        colorBlindnessSupport, reducedTransparency, colorSeed
    ) {
        ThemePreferences(
            colorSeed = colorSeed,
            contrastLevel = if (highContrastMode) ContrastLevel.High else ContrastLevel.Standard,
            reducedMotion = false,
            highContrast = highContrastMode,
            colorBlindnessType = if (colorBlindnessSupport) ColorBlindnessType.Deuteranopia else null,
            dynamicColor = dynamicColor,
            darkTheme = if (darkTheme) DarkThemePreference.Dark else DarkThemePreference.Light,
            reducedTransparency = reducedTransparency
        )
    }
    
    VoiceNotesAITheme(
        themePreferences = preferences,
        content = content
    )
}
