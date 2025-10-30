package com.voicenotesai.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import com.voicenotesai.domain.model.AccessibilityNeeds
import com.voicenotesai.domain.model.ColorBlindnessType
import com.voicenotesai.domain.model.ContrastLevel
import com.voicenotesai.domain.model.DarkThemePreference
import com.voicenotesai.domain.model.ThemeConfiguration
import com.voicenotesai.domain.model.ThemeModifications
import com.voicenotesai.domain.model.ThemePreferences
import com.voicenotesai.presentation.theme.color.DarkSemanticColors
import com.voicenotesai.presentation.theme.color.EnhancedDarkColorScheme
import com.voicenotesai.presentation.theme.color.EnhancedLightColorScheme
import com.voicenotesai.presentation.theme.color.LightSemanticColors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Implementation of ThemeEngine with Material You dynamic color generation,
 * accessibility enhancements, and color blindness support.
 */
@Singleton
class ThemeEngineImpl @Inject constructor() : ThemeEngine {

    @Composable
    override fun generateDynamicTheme(userPreferences: ThemePreferences): ColorScheme {
        val context = LocalContext.current
        val isSystemDark = isSystemInDarkTheme()
        
        val isDark = when (userPreferences.darkTheme) {
            DarkThemePreference.Light -> false
            DarkThemePreference.Dark -> true
            DarkThemePreference.FollowSystem -> isSystemDark
        }

        // Generate base color scheme
        val baseColorScheme = when {
            userPreferences.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (isDark) {
                    dynamicDarkColorScheme(context)
                } else {
                    dynamicLightColorScheme(context)
                }
            }
            userPreferences.colorSeed != null -> {
                generateColorSchemeFromSeed(
                    seedColor = userPreferences.colorSeed,
                    isDark = isDark,
                    contrastLevel = userPreferences.contrastLevel
                )
            }
            isDark -> EnhancedDarkColorScheme
            else -> EnhancedLightColorScheme
        }

        // Apply accessibility modifications
        var finalColorScheme = baseColorScheme

        if (userPreferences.highContrast) {
            finalColorScheme = enhanceContrast(finalColorScheme, userPreferences.contrastLevel)
        }

        if (userPreferences.colorBlindnessType != null) {
            finalColorScheme = adaptForColorBlindness(finalColorScheme, userPreferences.colorBlindnessType)
        }

        return finalColorScheme
    }

    @Composable
    override fun adaptToSystemSettings(): ThemeConfiguration {
        val context = LocalContext.current
        val isSystemDark = isSystemInDarkTheme()
        
        // Get system accessibility settings
        val accessibilityManager = androidx.core.content.ContextCompat.getSystemService(
            context, 
            android.view.accessibility.AccessibilityManager::class.java
        )
        
        val isHighContrastEnabled = false // TODO: Implement proper accessibility detection
        val animationScale = try {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
        } catch (e: Exception) {
            1.0f
        }

        val textScale = try {
            android.provider.Settings.System.getFloat(
                context.contentResolver,
                android.provider.Settings.System.FONT_SCALE,
                1.0f
            )
        } catch (e: Exception) {
            1.0f
        }

        // Create default preferences based on system settings
        val systemPreferences = ThemePreferences(
            highContrast = isHighContrastEnabled,
            reducedMotion = animationScale < 0.5f,
            largeText = textScale > 1.2f,
            extraLargeText = textScale > 1.5f,
            darkTheme = if (isSystemDark) DarkThemePreference.Dark else DarkThemePreference.Light
        )

        val colorScheme = generateDynamicTheme(systemPreferences)
        val semanticColors = if (isSystemDark) DarkSemanticColors else LightSemanticColors

        return ThemeConfiguration(
            colorScheme = colorScheme,
            semanticColors = semanticColors,
            isDarkTheme = isSystemDark,
            animationScale = animationScale,
            textScale = textScale,
            contrastRatio = if (isHighContrastEnabled) 7.0f else 4.5f,
            reducedTransparency = animationScale < 0.5f
        )
    }

    override fun applyAccessibilityEnhancements(needs: AccessibilityNeeds): ThemeModifications {
        var contrastBoost = 0f
        var textSizeMultiplier = 1f
        var animationScale = 1f
        var transparencyReduction = 0f
        val colorAdjustments = mutableMapOf<String, Color>()

        if (needs.highContrast) {
            contrastBoost = 0.3f
            transparencyReduction = 0.8f
        }

        if (needs.largeText) {
            textSizeMultiplier = 1.3f
        }

        if (needs.reducedMotion) {
            animationScale = 0.5f
        }

        if (needs.screenReader) {
            // Ensure high contrast for screen reader users
            contrastBoost = max(contrastBoost, 0.2f)
            transparencyReduction = max(transparencyReduction, 0.6f)
        }

        return ThemeModifications(
            contrastBoost = contrastBoost,
            textSizeMultiplier = textSizeMultiplier,
            animationScale = animationScale,
            transparencyReduction = transparencyReduction,
            colorAdjustments = colorAdjustments
        )
    }

    override fun generateColorSchemeFromSeed(
        seedColor: Color,
        isDark: Boolean,
        contrastLevel: ContrastLevel
    ): ColorScheme {
        // Convert to HSL for better color manipulation
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(seedColor.toArgb(), hsl)
        
        val hue = hsl[0]
        val baseSaturation = hsl[1]
        val baseLightness = hsl[2]

        // Generate primary colors
        val primaryHue = hue
        val primarySaturation = when (contrastLevel) {
            ContrastLevel.Standard -> baseSaturation
            ContrastLevel.Medium -> min(baseSaturation + 0.1f, 1f)
            ContrastLevel.High -> min(baseSaturation + 0.2f, 1f)
        }

        // Generate secondary colors (30 degrees offset)
        val secondaryHue = (hue + 30f) % 360f
        
        // Generate tertiary colors (60 degrees offset)
        val tertiaryHue = (hue + 60f) % 360f

        return if (isDark) {
            createDarkColorScheme(primaryHue, secondaryHue, tertiaryHue, primarySaturation, contrastLevel)
        } else {
            createLightColorScheme(primaryHue, secondaryHue, tertiaryHue, primarySaturation, contrastLevel)
        }
    }

    override fun adaptForColorBlindness(
        colorScheme: ColorScheme,
        colorBlindnessType: ColorBlindnessType
    ): ColorScheme {
        return when (colorBlindnessType) {
            ColorBlindnessType.Protanopia, ColorBlindnessType.Protanomaly -> {
                // Red-blind: Shift reds to oranges/yellows
                adaptColorsForRedBlindness(colorScheme)
            }
            ColorBlindnessType.Deuteranopia, ColorBlindnessType.Deuteranomaly -> {
                // Green-blind: Enhance blue-yellow contrast
                adaptColorsForGreenBlindness(colorScheme)
            }
            ColorBlindnessType.Tritanopia, ColorBlindnessType.Tritanomaly -> {
                // Blue-blind: Shift blues to greens
                adaptColorsForBlueBlindness(colorScheme)
            }
        }
    }

    override fun validateAccessibility(configuration: ThemeConfiguration): Boolean {
        val colorScheme = configuration.colorScheme
        
        // Check key color combinations meet WCAG standards
        val criticalCombinations = listOf(
            colorScheme.onPrimary to colorScheme.primary,
            colorScheme.onSecondary to colorScheme.secondary,
            colorScheme.onSurface to colorScheme.surface,
            colorScheme.onBackground to colorScheme.background,
            colorScheme.onError to colorScheme.error
        )

        val requiredRatio = if (configuration.contrastRatio >= 7.0f) 7.0f else 4.5f
        
        return criticalCombinations.all { (foreground, background) ->
            calculateContrastRatio(foreground, background) >= requiredRatio
        }
    }

    override fun getOptimalTextColor(backgroundColor: Color, highContrast: Boolean): Color {
        val backgroundLuminance = backgroundColor.luminance()
        
        return if (highContrast) {
            // Use pure black or white for maximum contrast
            if (backgroundLuminance > 0.5f) Color.Black else Color.White
        } else {
            // Use slightly softer colors for better readability
            if (backgroundLuminance > 0.5f) {
                Color(0xFF1C1B1F) // Dark gray
            } else {
                Color(0xFFFFFBFE) // Off-white
            }
        }
    }

    // Private helper methods

    private fun enhanceContrast(colorScheme: ColorScheme, contrastLevel: ContrastLevel): ColorScheme {
        val contrastMultiplier = when (contrastLevel) {
            ContrastLevel.Standard -> 1.0f
            ContrastLevel.Medium -> 1.2f
            ContrastLevel.High -> 1.5f
        }

        return colorScheme.copy(
            primary = adjustColorContrast(colorScheme.primary, colorScheme.surface, contrastMultiplier),
            secondary = adjustColorContrast(colorScheme.secondary, colorScheme.surface, contrastMultiplier),
            tertiary = adjustColorContrast(colorScheme.tertiary, colorScheme.surface, contrastMultiplier),
            error = adjustColorContrast(colorScheme.error, colorScheme.surface, contrastMultiplier)
        )
    }

    private fun adjustColorContrast(color: Color, background: Color, multiplier: Float): Color {
        val currentRatio = calculateContrastRatio(color, background)
        val targetRatio = currentRatio * multiplier
        
        if (currentRatio >= targetRatio) return color
        
        val backgroundLuminance = background.luminance()
        val shouldDarken = backgroundLuminance > 0.5f
        
        var adjustedColor = color
        var iterations = 0
        
        while (calculateContrastRatio(adjustedColor, background) < targetRatio && iterations < 20) {
            adjustedColor = if (shouldDarken) {
                darkenColor(adjustedColor, 0.05f)
            } else {
                lightenColor(adjustedColor, 0.05f)
            }
            iterations++
        }
        
        return adjustedColor
    }

    private fun createLightColorScheme(
        primaryHue: Float,
        secondaryHue: Float,
        tertiaryHue: Float,
        saturation: Float,
        contrastLevel: ContrastLevel
    ): ColorScheme {
        val lightnessFactor = when (contrastLevel) {
            ContrastLevel.Standard -> 1.0f
            ContrastLevel.Medium -> 0.9f
            ContrastLevel.High -> 0.8f
        }

        return androidx.compose.material3.lightColorScheme(
            primary = createColorFromHSL(primaryHue, saturation, 0.4f * lightnessFactor),
            onPrimary = Color.White,
            primaryContainer = createColorFromHSL(primaryHue, saturation * 0.3f, 0.9f),
            onPrimaryContainer = createColorFromHSL(primaryHue, saturation, 0.2f),
            
            secondary = createColorFromHSL(secondaryHue, saturation * 0.8f, 0.5f * lightnessFactor),
            onSecondary = Color.White,
            secondaryContainer = createColorFromHSL(secondaryHue, saturation * 0.3f, 0.9f),
            onSecondaryContainer = createColorFromHSL(secondaryHue, saturation * 0.8f, 0.2f),
            
            tertiary = createColorFromHSL(tertiaryHue, saturation * 0.6f, 0.5f * lightnessFactor),
            onTertiary = Color.White,
            tertiaryContainer = createColorFromHSL(tertiaryHue, saturation * 0.3f, 0.9f),
            onTertiaryContainer = createColorFromHSL(tertiaryHue, saturation * 0.6f, 0.2f),
            
            background = Color(0xFFFFFBFE),
            onBackground = Color(0xFF1C1B1F),
            surface = Color(0xFFFFFBFE),
            onSurface = Color(0xFF1C1B1F)
        )
    }

    private fun createDarkColorScheme(
        primaryHue: Float,
        secondaryHue: Float,
        tertiaryHue: Float,
        saturation: Float,
        contrastLevel: ContrastLevel
    ): ColorScheme {
        val lightnessFactor = when (contrastLevel) {
            ContrastLevel.Standard -> 1.0f
            ContrastLevel.Medium -> 1.1f
            ContrastLevel.High -> 1.2f
        }

        return androidx.compose.material3.darkColorScheme(
            primary = createColorFromHSL(primaryHue, saturation, 0.7f * lightnessFactor),
            onPrimary = createColorFromHSL(primaryHue, saturation, 0.2f),
            primaryContainer = createColorFromHSL(primaryHue, saturation, 0.3f),
            onPrimaryContainer = createColorFromHSL(primaryHue, saturation * 0.3f, 0.9f),
            
            secondary = createColorFromHSL(secondaryHue, saturation * 0.8f, 0.6f * lightnessFactor),
            onSecondary = createColorFromHSL(secondaryHue, saturation * 0.8f, 0.2f),
            secondaryContainer = createColorFromHSL(secondaryHue, saturation * 0.8f, 0.3f),
            onSecondaryContainer = createColorFromHSL(secondaryHue, saturation * 0.3f, 0.9f),
            
            tertiary = createColorFromHSL(tertiaryHue, saturation * 0.6f, 0.6f * lightnessFactor),
            onTertiary = createColorFromHSL(tertiaryHue, saturation * 0.6f, 0.2f),
            tertiaryContainer = createColorFromHSL(tertiaryHue, saturation * 0.6f, 0.3f),
            onTertiaryContainer = createColorFromHSL(tertiaryHue, saturation * 0.3f, 0.9f),
            
            background = Color(0xFF10101A),
            onBackground = Color(0xFFE6E1E5),
            surface = Color(0xFF10101A),
            onSurface = Color(0xFFE6E1E5)
        )
    }

    private fun createColorFromHSL(hue: Float, saturation: Float, lightness: Float): Color {
        val hsl = floatArrayOf(hue, saturation.coerceIn(0f, 1f), lightness.coerceIn(0f, 1f))
        return Color(ColorUtils.HSLToColor(hsl))
    }

    private fun adaptColorsForRedBlindness(colorScheme: ColorScheme): ColorScheme {
        return colorScheme.copy(
            primary = shiftRedToOrange(colorScheme.primary),
            error = shiftRedToOrange(colorScheme.error),
            tertiary = enhanceBlueYellowContrast(colorScheme.tertiary)
        )
    }

    private fun adaptColorsForGreenBlindness(colorScheme: ColorScheme): ColorScheme {
        return colorScheme.copy(
            secondary = enhanceBlueYellowContrast(colorScheme.secondary),
            tertiary = enhanceBlueYellowContrast(colorScheme.tertiary)
        )
    }

    private fun adaptColorsForBlueBlindness(colorScheme: ColorScheme): ColorScheme {
        return colorScheme.copy(
            primary = shiftBlueToGreen(colorScheme.primary),
            tertiary = shiftBlueToGreen(colorScheme.tertiary)
        )
    }

    private fun shiftRedToOrange(color: Color): Color {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color.toArgb(), hsl)
        
        // Shift red hues (0-30) towards orange (30-60)
        if (hsl[0] <= 30f) {
            hsl[0] = (hsl[0] + 20f).coerceAtMost(60f)
        }
        
        return Color(ColorUtils.HSLToColor(hsl))
    }

    private fun shiftBlueToGreen(color: Color): Color {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color.toArgb(), hsl)
        
        // Shift blue hues (200-280) towards green (80-160)
        if (hsl[0] in 200f..280f) {
            hsl[0] = ((hsl[0] - 200f) * 0.4f + 80f).coerceIn(80f, 160f)
        }
        
        return Color(ColorUtils.HSLToColor(hsl))
    }

    private fun enhanceBlueYellowContrast(color: Color): Color {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color.toArgb(), hsl)
        
        // Enhance saturation for blue (200-280) and yellow (40-80) hues
        if (hsl[0] in 40f..80f || hsl[0] in 200f..280f) {
            hsl[1] = min(hsl[1] + 0.2f, 1f)
        }
        
        return Color(ColorUtils.HSLToColor(hsl))
    }

    private fun calculateContrastRatio(color1: Color, color2: Color): Float {
        val luminance1 = color1.luminance() + 0.05f
        val luminance2 = color2.luminance() + 0.05f
        
        return max(luminance1, luminance2) / min(luminance1, luminance2)
    }

    private fun lightenColor(color: Color, factor: Float): Color {
        return Color(
            red = (color.red + (1f - color.red) * factor).coerceIn(0f, 1f),
            green = (color.green + (1f - color.green) * factor).coerceIn(0f, 1f),
            blue = (color.blue + (1f - color.blue) * factor).coerceIn(0f, 1f),
            alpha = color.alpha
        )
    }

    private fun darkenColor(color: Color, factor: Float): Color {
        return Color(
            red = (color.red * (1f - factor)).coerceIn(0f, 1f),
            green = (color.green * (1f - factor)).coerceIn(0f, 1f),
            blue = (color.blue * (1f - factor)).coerceIn(0f, 1f),
            alpha = color.alpha
        )
    }
}