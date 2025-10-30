package com.voicenotesai.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.voicenotesai.domain.model.AccessibilityNeeds
import com.voicenotesai.domain.model.ColorBlindnessType
import com.voicenotesai.domain.model.ThemePreferences
import com.voicenotesai.presentation.theme.color.LocalColorAccessibility
import com.voicenotesai.presentation.theme.color.LocalSemanticColors

/**
 * Utility functions and composables for theme management.
 */
object ThemeUtils {
    
    /**
     * Detects system accessibility settings and creates AccessibilityNeeds.
     */
    @Composable
    fun detectAccessibilityNeeds(): AccessibilityNeeds {
        val context = LocalContext.current
        
        return remember {
            val accessibilityManager = androidx.core.content.ContextCompat.getSystemService(
                context, 
                android.view.accessibility.AccessibilityManager::class.java
            )
            
            val isHighContrastEnabled = false // TODO: Implement proper accessibility detection
            val isTouchExplorationEnabled = accessibilityManager?.isTouchExplorationEnabled ?: false
            
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
            
            AccessibilityNeeds(
                highContrast = isHighContrastEnabled,
                largeText = textScale > 1.2f,
                reducedMotion = animationScale < 0.5f,
                screenReader = isTouchExplorationEnabled,
                voiceControl = false // This would need additional detection
            )
        }
    }
    
    /**
     * Creates theme preferences from system settings.
     */
    @Composable
    fun createSystemThemePreferences(): ThemePreferences {
        val accessibilityNeeds = detectAccessibilityNeeds()
        
        return remember(accessibilityNeeds) {
            ThemePreferences(
                highContrast = accessibilityNeeds.highContrast,
                reducedMotion = accessibilityNeeds.reducedMotion,
                largeText = accessibilityNeeds.largeText,
                reducedTransparency = accessibilityNeeds.reducedMotion
            )
        }
    }
    
    /**
     * Validates if current theme meets accessibility standards.
     */
    @Composable
    fun validateCurrentThemeAccessibility(colorScheme: ColorScheme): Boolean {
        val accessibilityState = LocalColorAccessibility.current
        
        return remember(colorScheme, accessibilityState) {
            val criticalCombinations = listOf(
                colorScheme.onPrimary to colorScheme.primary,
                colorScheme.onSecondary to colorScheme.secondary,
                colorScheme.onSurface to colorScheme.surface,
                colorScheme.onBackground to colorScheme.background,
                colorScheme.onError to colorScheme.error
            )
            
            val requiredRatio = if (accessibilityState.highContrastMode) 7.0f else 4.5f
            
            criticalCombinations.all { (foreground, background) ->
                calculateContrastRatio(foreground, background) >= requiredRatio
            }
        }
    }
    
    /**
     * Gets the appropriate text color for any background color.
     */
    @Composable
    fun getAdaptiveTextColor(backgroundColor: Color): Color {
        val accessibilityState = LocalColorAccessibility.current
        
        return remember(backgroundColor, accessibilityState) {
            val backgroundLuminance = backgroundColor.luminance()
            
            if (accessibilityState.highContrastMode) {
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
    }
    
    /**
     * Creates a color with accessibility-compliant contrast.
     */
    @Composable
    fun ensureAccessibleColor(
        color: Color, 
        backgroundColor: Color,
        minimumRatio: Float = 4.5f
    ): Color {
        return remember(color, backgroundColor, minimumRatio) {
            var adjustedColor = color
            var iterations = 0
            
            while (calculateContrastRatio(adjustedColor, backgroundColor) < minimumRatio && iterations < 20) {
                val backgroundLuminance = backgroundColor.luminance()
                adjustedColor = if (backgroundLuminance > 0.5f) {
                    // Darken the color
                    Color(
                        red = (adjustedColor.red * 0.9f).coerceAtLeast(0f),
                        green = (adjustedColor.green * 0.9f).coerceAtLeast(0f),
                        blue = (adjustedColor.blue * 0.9f).coerceAtLeast(0f),
                        alpha = adjustedColor.alpha
                    )
                } else {
                    // Lighten the color
                    Color(
                        red = (adjustedColor.red + (1f - adjustedColor.red) * 0.1f).coerceAtMost(1f),
                        green = (adjustedColor.green + (1f - adjustedColor.green) * 0.1f).coerceAtMost(1f),
                        blue = (adjustedColor.blue + (1f - adjustedColor.blue) * 0.1f).coerceAtMost(1f),
                        alpha = adjustedColor.alpha
                    )
                }
                iterations++
            }
            
            adjustedColor
        }
    }
    
    /**
     * Gets voice visualization color based on intensity and accessibility needs.
     */
    @Composable
    fun getAccessibleVoiceColor(intensity: Float): Color {
        val semanticColors = LocalSemanticColors.current
        val accessibilityState = LocalColorAccessibility.current
        
        return remember(intensity, accessibilityState) {
            val baseColor = when {
                intensity < 0.2f -> semanticColors.voiceWeak
                intensity < 0.5f -> semanticColors.voiceMedium
                intensity < 0.8f -> semanticColors.voiceStrong
                else -> semanticColors.voicePeak
            }
            
            if (accessibilityState.highContrastMode) {
                // Enhance saturation for better visibility
                val hsl = FloatArray(3)
                androidx.core.graphics.ColorUtils.colorToHSL(baseColor.toArgb(), hsl)
                hsl[1] = kotlin.math.min(hsl[1] + 0.2f, 1f) // Increase saturation
                Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
            } else {
                baseColor
            }
        }
    }
    
    /**
     * Creates theme-aware glass effect colors.
     */
    @Composable
    fun getThemeAwareGlassColors(): Triple<Color, Color, Color> {
        val semanticColors = LocalSemanticColors.current
        val accessibilityState = LocalColorAccessibility.current
        
        return remember(accessibilityState) {
            if (accessibilityState.reducedTransparency) {
                // Provide more opaque alternatives
                Triple(
                    semanticColors.glassSurface.copy(alpha = 0.8f), // surface
                    semanticColors.glassOutline.copy(alpha = 1f),   // outline
                    semanticColors.glassShadow.copy(alpha = 0.5f)   // shadow
                )
            } else {
                Triple(
                    semanticColors.glassSurface,
                    semanticColors.glassOutline,
                    semanticColors.glassShadow
                )
            }
        }
    }
    
    /**
     * Suggests color blindness type based on user interaction patterns.
     * This is a placeholder for future ML-based detection.
     */
    fun suggestColorBlindnessType(
        redGreenConfusion: Int,
        blueYellowConfusion: Int,
        totalInteractions: Int
    ): ColorBlindnessType? {
        if (totalInteractions < 50) return null // Not enough data
        
        val redGreenRatio = redGreenConfusion.toFloat() / totalInteractions
        val blueYellowRatio = blueYellowConfusion.toFloat() / totalInteractions
        
        return when {
            redGreenRatio > 0.3f -> ColorBlindnessType.Deuteranopia
            blueYellowRatio > 0.2f -> ColorBlindnessType.Tritanopia
            else -> null
        }
    }
    
    private fun calculateContrastRatio(color1: Color, color2: Color): Float {
        val luminance1 = color1.luminance() + 0.05f
        val luminance2 = color2.luminance() + 0.05f
        
        return kotlin.math.max(luminance1, luminance2) / kotlin.math.min(luminance1, luminance2)
    }
}