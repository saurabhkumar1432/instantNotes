package com.voicenotesai.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.voicenotesai.data.model.LayoutSettings
import com.voicenotesai.data.model.TypographySettings
import com.voicenotesai.data.model.UIPreferences
import com.voicenotesai.presentation.theme.SpacingScale
import com.voicenotesai.presentation.theme.TextSizeScale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing typography and accessibility settings
 */
@Singleton
class TypographyRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    
    companion object {
        private val TEXT_SIZE_SCALE = stringPreferencesKey("text_size_scale")
        private val SPACING_SCALE = stringPreferencesKey("spacing_scale")
        private val USE_SYSTEM_FONTS = booleanPreferencesKey("use_system_fonts")
        private val HIGH_CONTRAST_MODE = booleanPreferencesKey("high_contrast_mode")
        private val BOLD_TEXT = booleanPreferencesKey("bold_text")
        private val INCREASED_LINE_SPACING = booleanPreferencesKey("increased_line_spacing")
        private val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        private val FOLLOW_SYSTEM_SETTINGS = booleanPreferencesKey("follow_system_settings")
        
        // Layout settings
        private val PREFER_TWO_PANE = booleanPreferencesKey("prefer_two_pane")
        private val PREFER_NAVIGATION_RAIL = booleanPreferencesKey("prefer_navigation_rail")
        private val ENABLE_ADAPTIVE_LAYOUTS = booleanPreferencesKey("enable_adaptive_layouts")
        
        // Theme settings
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        private val REDUCED_TRANSPARENCY = booleanPreferencesKey("reduced_transparency")
    }
    
    /**
     * Get typography settings as a flow
     */
    val typographySettings: Flow<TypographySettings> = dataStore.data.map { preferences ->
        TypographySettings(
            textSizeScale = TextSizeScale.valueOf(
                preferences[TEXT_SIZE_SCALE] ?: TextSizeScale.Default.name
            ),
            spacingScale = SpacingScale.valueOf(
                preferences[SPACING_SCALE] ?: SpacingScale.Default.name
            ),
            useSystemFonts = preferences[USE_SYSTEM_FONTS] ?: true,
            highContrastMode = preferences[HIGH_CONTRAST_MODE] ?: false,
            boldText = preferences[BOLD_TEXT] ?: false,
            increasedLineSpacing = preferences[INCREASED_LINE_SPACING] ?: false,
            reducedMotion = preferences[REDUCED_MOTION] ?: false,
            followSystemSettings = preferences[FOLLOW_SYSTEM_SETTINGS] ?: true
        )
    }
    
    /**
     * Get layout settings as a flow
     */
    val layoutSettings: Flow<LayoutSettings> = dataStore.data.map { preferences ->
        LayoutSettings(
            preferTwoPane = preferences[PREFER_TWO_PANE] ?: false,
            preferNavigationRail = preferences[PREFER_NAVIGATION_RAIL] ?: false,
            enableAdaptiveLayouts = preferences[ENABLE_ADAPTIVE_LAYOUTS] ?: true
        )
    }
    
    /**
     * Get complete UI preferences as a flow
     */
    val uiPreferences: Flow<UIPreferences> = dataStore.data.map { preferences ->
        UIPreferences(
            typography = TypographySettings(
                textSizeScale = TextSizeScale.valueOf(
                    preferences[TEXT_SIZE_SCALE] ?: TextSizeScale.Default.name
                ),
                spacingScale = SpacingScale.valueOf(
                    preferences[SPACING_SCALE] ?: SpacingScale.Default.name
                ),
                useSystemFonts = preferences[USE_SYSTEM_FONTS] ?: true,
                highContrastMode = preferences[HIGH_CONTRAST_MODE] ?: false,
                boldText = preferences[BOLD_TEXT] ?: false,
                increasedLineSpacing = preferences[INCREASED_LINE_SPACING] ?: false,
                reducedMotion = preferences[REDUCED_MOTION] ?: false,
                followSystemSettings = preferences[FOLLOW_SYSTEM_SETTINGS] ?: true
            ),
            layout = LayoutSettings(
                preferTwoPane = preferences[PREFER_TWO_PANE] ?: false,
                preferNavigationRail = preferences[PREFER_NAVIGATION_RAIL] ?: false,
                enableAdaptiveLayouts = preferences[ENABLE_ADAPTIVE_LAYOUTS] ?: true
            ),
            theme = preferences[THEME_MODE] ?: "system",
            dynamicColors = preferences[DYNAMIC_COLORS] ?: true,
            reducedTransparency = preferences[REDUCED_TRANSPARENCY] ?: false
        )
    }
    
    /**
     * Update typography settings
     */
    suspend fun updateTypographySettings(settings: TypographySettings) {
        dataStore.edit { preferences ->
            preferences[TEXT_SIZE_SCALE] = settings.textSizeScale.name
            preferences[SPACING_SCALE] = settings.spacingScale.name
            preferences[USE_SYSTEM_FONTS] = settings.useSystemFonts
            preferences[HIGH_CONTRAST_MODE] = settings.highContrastMode
            preferences[BOLD_TEXT] = settings.boldText
            preferences[INCREASED_LINE_SPACING] = settings.increasedLineSpacing
            preferences[REDUCED_MOTION] = settings.reducedMotion
            preferences[FOLLOW_SYSTEM_SETTINGS] = settings.followSystemSettings
        }
    }
    
    /**
     * Update layout settings
     */
    suspend fun updateLayoutSettings(settings: LayoutSettings) {
        dataStore.edit { preferences ->
            preferences[PREFER_TWO_PANE] = settings.preferTwoPane
            preferences[PREFER_NAVIGATION_RAIL] = settings.preferNavigationRail
            preferences[ENABLE_ADAPTIVE_LAYOUTS] = settings.enableAdaptiveLayouts
        }
    }
    
    /**
     * Update text size scale
     */
    suspend fun updateTextSizeScale(scale: TextSizeScale) {
        dataStore.edit { preferences ->
            preferences[TEXT_SIZE_SCALE] = scale.name
        }
    }
    
    /**
     * Update spacing scale
     */
    suspend fun updateSpacingScale(scale: SpacingScale) {
        dataStore.edit { preferences ->
            preferences[SPACING_SCALE] = scale.name
        }
    }
    
    /**
     * Update high contrast mode
     */
    suspend fun updateHighContrastMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[HIGH_CONTRAST_MODE] = enabled
        }
    }
    
    /**
     * Update bold text preference
     */
    suspend fun updateBoldText(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[BOLD_TEXT] = enabled
        }
    }
    
    /**
     * Update increased line spacing preference
     */
    suspend fun updateIncreasedLineSpacing(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[INCREASED_LINE_SPACING] = enabled
        }
    }
    
    /**
     * Update reduced motion preference
     */
    suspend fun updateReducedMotion(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[REDUCED_MOTION] = enabled
        }
    }
    
    /**
     * Reset all typography settings to defaults
     */
    suspend fun resetToDefaults() {
        dataStore.edit { preferences ->
            preferences.remove(TEXT_SIZE_SCALE)
            preferences.remove(SPACING_SCALE)
            preferences.remove(USE_SYSTEM_FONTS)
            preferences.remove(HIGH_CONTRAST_MODE)
            preferences.remove(BOLD_TEXT)
            preferences.remove(INCREASED_LINE_SPACING)
            preferences.remove(REDUCED_MOTION)
            preferences.remove(FOLLOW_SYSTEM_SETTINGS)
            preferences.remove(PREFER_TWO_PANE)
            preferences.remove(PREFER_NAVIGATION_RAIL)
            preferences.remove(ENABLE_ADAPTIVE_LAYOUTS)
        }
    }
}