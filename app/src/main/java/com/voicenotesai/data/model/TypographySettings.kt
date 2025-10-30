package com.voicenotesai.data.model

import com.voicenotesai.presentation.theme.SpacingScale
import com.voicenotesai.presentation.theme.TextSizeScale

/**
 * Typography and accessibility settings for the application
 */
data class TypographySettings(
    val textSizeScale: TextSizeScale = TextSizeScale.Default,
    val spacingScale: SpacingScale = SpacingScale.Default,
    val useSystemFonts: Boolean = true,
    val highContrastMode: Boolean = false,
    val boldText: Boolean = false,
    val increasedLineSpacing: Boolean = false,
    val reducedMotion: Boolean = false,
    val followSystemSettings: Boolean = true
) {
    companion object {
        val DEFAULT = TypographySettings()
    }
}

/**
 * Layout preferences for responsive design
 */
data class LayoutSettings(
    val preferTwoPane: Boolean = false,
    val preferNavigationRail: Boolean = false,
    val maxContentWidth: Int? = null, // in dp, null for system default
    val gridColumns: Int? = null, // null for adaptive
    val listItemHeight: Int? = null, // in dp, null for system default
    val enableAdaptiveLayouts: Boolean = true
) {
    companion object {
        val DEFAULT = LayoutSettings()
    }
}

/**
 * Combined UI preferences
 */
data class UIPreferences(
    val typography: TypographySettings = TypographySettings.DEFAULT,
    val layout: LayoutSettings = LayoutSettings.DEFAULT,
    val theme: String = "system", // "light", "dark", "system"
    val dynamicColors: Boolean = true,
    val reducedTransparency: Boolean = false
) {
    companion object {
        val DEFAULT = UIPreferences()
    }
}