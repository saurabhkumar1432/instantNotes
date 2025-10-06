package com.voicenotesai.presentation.theme.color

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Enhanced color system with semantic colors, improved contrast ratios,
 * and comprehensive dark mode support.
 */

/**
 * Custom semantic colors for the application.
 */
@Stable
data class SemanticColors(
    // Success states
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    
    // Warning states
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    
    // Info states
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    
    // Recording states
    val recording: Color,
    val onRecording: Color,
    val recordingContainer: Color,
    val onRecordingContainer: Color,
    
    // Processing states
    val processing: Color,
    val onProcessing: Color,
    val processingContainer: Color,
    val onProcessingContainer: Color,
    
    // Neutral variants
    val neutral10: Color,
    val neutral20: Color,
    val neutral30: Color,
    val neutral40: Color,
    val neutral50: Color,
    val neutral60: Color,
    val neutral70: Color,
    val neutral80: Color,
    val neutral90: Color,
    val neutral95: Color,
    val neutral99: Color,
    
    // Voice visualization colors
    val voiceWeak: Color,
    val voiceMedium: Color,
    val voiceStrong: Color,
    val voicePeak: Color,
    
    // Glass effect colors
    val glassSurface: Color,
    val glassOutline: Color,
    val glassShadow: Color
)

/**
 * Light theme semantic colors with proper contrast ratios.
 */
val LightSemanticColors = SemanticColors(
    // Success - Green palette
    success = Color(0xFF2E7D32),              // Green 800
    onSuccess = Color(0xFFFFFFFF),            // White
    successContainer = Color(0xFFC8E6C9),     // Green 100
    onSuccessContainer = Color(0xFF1B5E20),   // Green 900
    
    // Warning - Amber palette
    warning = Color(0xFFF57C00),              // Amber 800
    onWarning = Color(0xFFFFFFFF),            // White
    warningContainer = Color(0xFFFFE0B2),     // Amber 100
    onWarningContainer = Color(0xFFE65100),   // Amber 900
    
    // Info - Blue palette
    info = Color(0xFF1976D2),                 // Blue 700
    onInfo = Color(0xFFFFFFFF),               // White
    infoContainer = Color(0xFFBBDEFB),        // Blue 100
    onInfoContainer = Color(0xFF0D47A1),      // Blue 900
    
    // Recording - Red palette
    recording = Color(0xFFD32F2F),            // Red 700
    onRecording = Color(0xFFFFFFFF),          // White
    recordingContainer = Color(0xFFFFCDD2),   // Red 100
    onRecordingContainer = Color(0xFFB71C1C), // Red 900
    
    // Processing - Purple palette
    processing = Color(0xFF7B1FA2),           // Purple 700
    onProcessing = Color(0xFFFFFFFF),         // White
    processingContainer = Color(0xFFE1BEE7),  // Purple 100
    onProcessingContainer = Color(0xFF4A148C), // Purple 900
    
    // Neutral variants for sophisticated grays
    neutral10 = Color(0xFFFCFCFC),
    neutral20 = Color(0xFFF5F5F5),
    neutral30 = Color(0xFFEEEEEE),
    neutral40 = Color(0xFFE0E0E0),
    neutral50 = Color(0xFFBDBDBD),
    neutral60 = Color(0xFF9E9E9E),
    neutral70 = Color(0xFF757575),
    neutral80 = Color(0xFF616161),
    neutral90 = Color(0xFF424242),
    neutral95 = Color(0xFF212121),
    neutral99 = Color(0xFF000000),
    
    // Voice visualization colors
    voiceWeak = Color(0xFF81C784),     // Light green
    voiceMedium = Color(0xFF4CAF50),   // Medium green
    voiceStrong = Color(0xFF2E7D32),   // Strong green
    voicePeak = Color(0xFFFF5722),     // Peak red-orange
    
    // Glass effect colors
    glassSurface = Color(0x1AFFFFFF),   // Translucent white
    glassOutline = Color(0x33FFFFFF),   // Light white border
    glassShadow = Color(0x1A000000)     // Subtle shadow
)

/**
 * Dark theme semantic colors with proper contrast ratios.
 */
val DarkSemanticColors = SemanticColors(
    // Success - Green palette (adjusted for dark theme)
    success = Color(0xFF4CAF50),              // Green 500
    onSuccess = Color(0xFF000000),            // Black
    successContainer = Color(0xFF2E7D32),     // Green 800
    onSuccessContainer = Color(0xFFC8E6C9),   // Green 100
    
    // Warning - Amber palette (adjusted for dark theme)
    warning = Color(0xFFFFC107),              // Amber 500
    onWarning = Color(0xFF000000),            // Black
    warningContainer = Color(0xFFF57C00),     // Amber 800
    onWarningContainer = Color(0xFFFFE0B2),   // Amber 100
    
    // Info - Blue palette (adjusted for dark theme)
    info = Color(0xFF2196F3),                 // Blue 500
    onInfo = Color(0xFFFFFFFF),               // White
    infoContainer = Color(0xFF1976D2),        // Blue 700
    onInfoContainer = Color(0xFFBBDEFB),      // Blue 100
    
    // Recording - Red palette (adjusted for dark theme)
    recording = Color(0xFFF44336),            // Red 500
    onRecording = Color(0xFFFFFFFF),          // White
    recordingContainer = Color(0xFFD32F2F),   // Red 700
    onRecordingContainer = Color(0xFFFFCDD2), // Red 100
    
    // Processing - Purple palette (adjusted for dark theme)
    processing = Color(0xFF9C27B0),           // Purple 500
    onProcessing = Color(0xFFFFFFFF),         // White
    processingContainer = Color(0xFF7B1FA2),  // Purple 700
    onProcessingContainer = Color(0xFFE1BEE7), // Purple 100
    
    // Neutral variants for dark theme
    neutral10 = Color(0xFF000000),
    neutral20 = Color(0xFF121212),
    neutral30 = Color(0xFF1E1E1E),
    neutral40 = Color(0xFF2D2D2D),
    neutral50 = Color(0xFF424242),
    neutral60 = Color(0xFF616161),
    neutral70 = Color(0xFF757575),
    neutral80 = Color(0xFF9E9E9E),
    neutral90 = Color(0xFFBDBDBD),
    neutral95 = Color(0xFFEEEEEE),
    neutral99 = Color(0xFFFFFFFF),
    
    // Voice visualization colors (enhanced for dark theme)
    voiceWeak = Color(0xFF66BB6A),     // Light green
    voiceMedium = Color(0xFF4CAF50),   // Medium green  
    voiceStrong = Color(0xFF388E3C),   // Strong green
    voicePeak = Color(0xFFFF7043),     // Peak orange
    
    // Glass effect colors (adjusted for dark theme)
    glassSurface = Color(0x1AFFFFFF),   // Translucent white
    glassOutline = Color(0x33FFFFFF),   // Light white border
    glassShadow = Color(0x33000000)     // More pronounced shadow
)

/**
 * Enhanced Material 3 color schemes with improved contrast.
 */
val EnhancedLightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    
    scrim = Color(0xFF000000),
    
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Color(0xFFD0BCFF)
)

val EnhancedDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    
    background = Color(0xFF10101A),
    onBackground = Color(0xFFE6E1E5),
    
    surface = Color(0xFF10101A),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    
    scrim = Color(0xFF000000),
    
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = Color(0xFF6750A4)
)

/**
 * Utility functions for color operations.
 */
object ColorUtils {
    
    /**
     * Calculates the contrast ratio between two colors.
     * Returns a value between 1 and 21, where higher values indicate better contrast.
     */
    fun contrastRatio(color1: Color, color2: Color): Float {
        val luminance1 = color1.luminance() + 0.05f
        val luminance2 = color2.luminance() + 0.05f
        
        return maxOf(luminance1, luminance2) / minOf(luminance1, luminance2)
    }
    
    /**
     * Checks if the contrast ratio meets WCAG AA standards (4.5:1 for normal text).
     */
    fun meetsContrastStandards(foreground: Color, background: Color): Boolean {
        return contrastRatio(foreground, background) >= 4.5f
    }
    
    /**
     * Checks if the contrast ratio meets WCAG AAA standards (7:1 for normal text).
     */
    fun meetsHighContrastStandards(foreground: Color, background: Color): Boolean {
        return contrastRatio(foreground, background) >= 7.0f
    }
    
    /**
     * Creates a color with adjusted alpha for glass effects.
     */
    fun Color.withGlassAlpha(alpha: Float = 0.1f): Color {
        return this.copy(alpha = alpha)
    }
    
    /**
     * Creates a color that adapts to the theme (lighter in dark theme, darker in light theme).
     */
    fun Color.adaptToTheme(isDark: Boolean, factor: Float = 0.1f): Color {
        return if (isDark) {
            this.lighten(factor)
        } else {
            this.darken(factor)
        }
    }
    
    /**
     * Lightens a color by the specified factor.
     */
    fun Color.lighten(factor: Float): Color {
        return Color(
            red = (red + (1f - red) * factor).coerceIn(0f, 1f),
            green = (green + (1f - green) * factor).coerceIn(0f, 1f),
            blue = (blue + (1f - blue) * factor).coerceIn(0f, 1f),
            alpha = alpha
        )
    }
    
    /**
     * Darkens a color by the specified factor.
     */
    fun Color.darken(factor: Float): Color {
        return Color(
            red = (red * (1f - factor)).coerceIn(0f, 1f),
            green = (green * (1f - factor)).coerceIn(0f, 1f),
            blue = (blue * (1f - factor)).coerceIn(0f, 1f),
            alpha = alpha
        )
    }
    
    /**
     * Gets the appropriate on-color (text color) for a given background color.
     */
    fun getOnColor(backgroundColor: Color): Color {
        return if (backgroundColor.luminance() > 0.5f) {
            Color.Black
        } else {
            Color.White
        }
    }
    
    /**
     * Creates a color with improved accessibility.
     */
    fun Color.ensureAccessibility(background: Color): Color {
        var adjustedColor = this
        var iterations = 0
        
        while (!meetsContrastStandards(adjustedColor, background) && iterations < 10) {
            adjustedColor = if (background.luminance() > 0.5f) {
                adjustedColor.darken(0.1f)
            } else {
                adjustedColor.lighten(0.1f)
            }
            iterations++
        }
        
        return adjustedColor
    }
}