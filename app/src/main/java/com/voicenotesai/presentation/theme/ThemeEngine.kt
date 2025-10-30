package com.voicenotesai.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.voicenotesai.domain.model.AccessibilityNeeds
import com.voicenotesai.domain.model.ThemeConfiguration
import com.voicenotesai.domain.model.ThemeModifications
import com.voicenotesai.domain.model.ThemePreferences

/**
 * Interface for dynamic theme generation and management.
 */
interface ThemeEngine {
    /**
     * Generates a dynamic color scheme based on user preferences.
     */
    @Composable
    fun generateDynamicTheme(userPreferences: ThemePreferences): ColorScheme
    
    /**
     * Adapts theme to system settings and device capabilities.
     */
    @Composable
    fun adaptToSystemSettings(): ThemeConfiguration
    
    /**
     * Applies accessibility enhancements to the theme.
     */
    fun applyAccessibilityEnhancements(needs: AccessibilityNeeds): ThemeModifications
    
    /**
     * Generates a color scheme from a seed color.
     */
    fun generateColorSchemeFromSeed(
        seedColor: Color,
        isDark: Boolean,
        contrastLevel: com.voicenotesai.domain.model.ContrastLevel
    ): ColorScheme
    
    /**
     * Adapts colors for color blindness accessibility.
     */
    fun adaptForColorBlindness(
        colorScheme: ColorScheme,
        colorBlindnessType: com.voicenotesai.domain.model.ColorBlindnessType
    ): ColorScheme
    
    /**
     * Validates theme meets accessibility standards.
     */
    fun validateAccessibility(configuration: ThemeConfiguration): Boolean
    
    /**
     * Gets the optimal text color for a given background.
     */
    fun getOptimalTextColor(backgroundColor: Color, highContrast: Boolean = false): Color
}