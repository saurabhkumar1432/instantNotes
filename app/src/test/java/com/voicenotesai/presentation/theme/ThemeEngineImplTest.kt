package com.voicenotesai.presentation.theme

import androidx.compose.ui.graphics.Color
import com.voicenotesai.domain.model.AccessibilityNeeds
import com.voicenotesai.domain.model.ColorBlindnessType
import com.voicenotesai.domain.model.ContrastLevel
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ThemeEngineImpl.
 */
class ThemeEngineImplTest {

    private lateinit var themeEngine: ThemeEngineImpl

    @Before
    fun setup() {
        themeEngine = ThemeEngineImpl()
    }

    @Test
    fun `generateColorSchemeFromSeed creates valid color scheme`() {
        // Given
        val seedColor = Color(0xFF6750A4)
        val isDark = false
        val contrastLevel = ContrastLevel.Standard

        // When
        val colorScheme = themeEngine.generateColorSchemeFromSeed(seedColor, isDark, contrastLevel)

        // Then
        assertNotNull(colorScheme)
        assertNotNull(colorScheme.primary)
        assertNotNull(colorScheme.secondary)
        assertNotNull(colorScheme.tertiary)
    }

    @Test
    fun `adaptForColorBlindness modifies color scheme appropriately`() {
        // Given
        val originalColorScheme = themeEngine.generateColorSchemeFromSeed(
            Color(0xFF6750A4), 
            false, 
            ContrastLevel.Standard
        )
        val colorBlindnessType = ColorBlindnessType.Deuteranopia

        // When
        val adaptedColorScheme = themeEngine.adaptForColorBlindness(originalColorScheme, colorBlindnessType)

        // Then
        assertNotNull(adaptedColorScheme)
        // Colors should be different after adaptation
        assertTrue(adaptedColorScheme.secondary != originalColorScheme.secondary)
    }

    @Test
    fun `applyAccessibilityEnhancements increases contrast for high contrast needs`() {
        // Given
        val accessibilityNeeds = AccessibilityNeeds(
            highContrast = true,
            largeText = true,
            reducedMotion = true
        )

        // When
        val modifications = themeEngine.applyAccessibilityEnhancements(accessibilityNeeds)

        // Then
        assertTrue(modifications.contrastBoost > 0f)
        assertTrue(modifications.textSizeMultiplier > 1f)
        assertTrue(modifications.animationScale < 1f)
        assertTrue(modifications.transparencyReduction > 0f)
    }

    @Test
    fun `getOptimalTextColor returns appropriate contrast`() {
        // Given
        val lightBackground = Color.White
        val darkBackground = Color.Black

        // When
        val textOnLight = themeEngine.getOptimalTextColor(lightBackground, false)
        val textOnDark = themeEngine.getOptimalTextColor(darkBackground, false)
        val highContrastTextOnLight = themeEngine.getOptimalTextColor(lightBackground, true)

        // Then
        // Text on light background should be dark
        assertTrue(textOnLight.red < 0.5f && textOnLight.green < 0.5f && textOnLight.blue < 0.5f)
        
        // Text on dark background should be light
        assertTrue(textOnDark.red > 0.5f && textOnDark.green > 0.5f && textOnDark.blue > 0.5f)
        
        // High contrast should be pure black or white
        assertTrue(
            (highContrastTextOnLight == Color.Black) || 
            (highContrastTextOnLight == Color.White)
        )
    }

    @Test
    fun `high contrast level increases color contrast`() {
        // Given
        val seedColor = Color(0xFF6750A4)
        val standardScheme = themeEngine.generateColorSchemeFromSeed(
            seedColor, false, ContrastLevel.Standard
        )
        val highContrastScheme = themeEngine.generateColorSchemeFromSeed(
            seedColor, false, ContrastLevel.High
        )

        // When & Then
        // High contrast scheme should have different colors than standard
        assertTrue(standardScheme.primary != highContrastScheme.primary)
    }

    @Test
    fun `dark theme generates appropriate colors`() {
        // Given
        val seedColor = Color(0xFF6750A4)

        // When
        val lightScheme = themeEngine.generateColorSchemeFromSeed(seedColor, false, ContrastLevel.Standard)
        val darkScheme = themeEngine.generateColorSchemeFromSeed(seedColor, true, ContrastLevel.Standard)

        // Then
        // Dark theme should have different background colors
        assertTrue(lightScheme.background != darkScheme.background)
        assertTrue(lightScheme.surface != darkScheme.surface)
        
        // Dark theme background should be darker
        assertTrue(darkScheme.background.red < lightScheme.background.red)
    }
}