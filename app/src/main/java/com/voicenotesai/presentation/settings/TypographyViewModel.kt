package com.voicenotesai.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicenotesai.data.model.TypographySettings
import com.voicenotesai.data.model.UIPreferences
import com.voicenotesai.data.repository.TypographyRepository
import com.voicenotesai.presentation.accessibility.AccessibilityPreferences
import com.voicenotesai.presentation.theme.SpacingScale
import com.voicenotesai.presentation.theme.TextSizeScale
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing typography and accessibility settings
 */
@HiltViewModel
class TypographyViewModel @Inject constructor(
    private val typographyRepository: TypographyRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TypographyUiState())
    val uiState: StateFlow<TypographyUiState> = _uiState.asStateFlow()
    
    init {
        observeSettings()
    }
    
    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                typographyRepository.typographySettings,
                typographyRepository.layoutSettings
            ) { typography, layout ->
                TypographyUiState(
                    typographySettings = typography,
                    accessibilityPreferences = AccessibilityPreferences(
                        textSizeScale = typography.textSizeScale,
                        spacingScale = typography.spacingScale,
                        highContrastMode = typography.highContrastMode,
                        boldText = typography.boldText,
                        increasedLineSpacing = typography.increasedLineSpacing,
                        useSystemFonts = typography.useSystemFonts,
                        reducedMotion = typography.reducedMotion
                    ),
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }
    
    /**
     * Update text size scale
     */
    fun updateTextSizeScale(scale: TextSizeScale) {
        viewModelScope.launch {
            typographyRepository.updateTextSizeScale(scale)
        }
    }
    
    /**
     * Update spacing scale
     */
    fun updateSpacingScale(scale: SpacingScale) {
        viewModelScope.launch {
            typographyRepository.updateSpacingScale(scale)
        }
    }
    
    /**
     * Update high contrast mode
     */
    fun updateHighContrastMode(enabled: Boolean) {
        viewModelScope.launch {
            typographyRepository.updateHighContrastMode(enabled)
        }
    }
    
    /**
     * Update bold text preference
     */
    fun updateBoldText(enabled: Boolean) {
        viewModelScope.launch {
            typographyRepository.updateBoldText(enabled)
        }
    }
    
    /**
     * Update increased line spacing preference
     */
    fun updateIncreasedLineSpacing(enabled: Boolean) {
        viewModelScope.launch {
            typographyRepository.updateIncreasedLineSpacing(enabled)
        }
    }
    
    /**
     * Update reduced motion preference
     */
    fun updateReducedMotion(enabled: Boolean) {
        viewModelScope.launch {
            typographyRepository.updateReducedMotion(enabled)
        }
    }
    
    /**
     * Reset all settings to defaults
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            typographyRepository.resetToDefaults()
        }
    }
    
    /**
     * Update complete typography settings
     */
    fun updateTypographySettings(settings: TypographySettings) {
        viewModelScope.launch {
            typographyRepository.updateTypographySettings(settings)
        }
    }
}

/**
 * UI state for typography settings screen
 */
data class TypographyUiState(
    val typographySettings: TypographySettings = TypographySettings.DEFAULT,
    val accessibilityPreferences: AccessibilityPreferences = AccessibilityPreferences(),
    val isLoading: Boolean = true,
    val error: String? = null
)