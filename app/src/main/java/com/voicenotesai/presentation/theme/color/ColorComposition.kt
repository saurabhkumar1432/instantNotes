package com.voicenotesai.presentation.theme.color

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Composition locals for enhanced color system integration.
 */

/**
 * CompositionLocal for semantic colors.
 */
val LocalSemanticColors = staticCompositionLocalOf { LightSemanticColors }

/**
 * CompositionLocal for color accessibility state.
 */
val LocalColorAccessibility = staticCompositionLocalOf { ColorAccessibilityState() }

/**
 * State for managing color accessibility features.
 */
@Immutable
data class ColorAccessibilityState(
    val highContrastMode: Boolean = false,
    val colorBlindnessSupport: Boolean = false,
    val reducedTransparency: Boolean = false
)

/**
 * Enhanced theme provider that includes semantic colors and accessibility features.
 */
@Composable
fun EnhancedColorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrastMode: Boolean = false,
    colorBlindnessSupport: Boolean = false,
    reducedTransparency: Boolean = false,
    content: @Composable () -> Unit
) {
    val semanticColors = if (darkTheme) {
        if (highContrastMode) {
            getHighContrastDarkSemanticColors()
        } else {
            DarkSemanticColors
        }
    } else {
        if (highContrastMode) {
            getHighContrastLightSemanticColors()
        } else {
            LightSemanticColors
        }
    }
    
    val accessibilityState = ColorAccessibilityState(
        highContrastMode = highContrastMode,
        colorBlindnessSupport = colorBlindnessSupport,
        reducedTransparency = reducedTransparency
    )
    
    CompositionLocalProvider(
        LocalSemanticColors provides semanticColors,
        LocalColorAccessibility provides accessibilityState
    ) {
        content()
    }
}

/**
 * Extension to access semantic colors from any composable.
 */
val ColorScheme.semantic: SemanticColors
    @Composable get() = LocalSemanticColors.current

/**
 * Extension to access color accessibility state from any composable.
 */
val ColorScheme.accessibility: ColorAccessibilityState
    @Composable get() = LocalColorAccessibility.current

/**
 * High contrast semantic colors for better accessibility.
 */
private fun getHighContrastLightSemanticColors(): SemanticColors {
    return LightSemanticColors.copy(
        // Increase contrast for better accessibility
        success = Color(0xFF1B5E20),         // Darker green
        onSuccess = Color(0xFFFFFFFF),
        warning = Color(0xFFE65100),         // Darker amber
        onWarning = Color(0xFFFFFFFF),
        info = Color(0xFF0D47A1),            // Darker blue
        onInfo = Color(0xFFFFFFFF),
        recording = Color(0xFFB71C1C),       // Darker red
        onRecording = Color(0xFFFFFFFF),
        processing = Color(0xFF4A148C),      // Darker purple
        onProcessing = Color(0xFFFFFFFF),
        
        // More pronounced neutrals
        neutral50 = Color(0xFF757575),
        neutral60 = Color(0xFF616161),
        neutral70 = Color(0xFF424242),
        
        // More opaque glass effects
        glassSurface = Color(0x33FFFFFF),
        glassOutline = Color(0x66FFFFFF)
    )
}

private fun getHighContrastDarkSemanticColors(): SemanticColors {
    return DarkSemanticColors.copy(
        // Brighter colors for dark theme high contrast
        success = Color(0xFF66BB6A),         // Brighter green
        onSuccess = Color(0xFF000000),
        warning = Color(0xFFFFCA28),         // Brighter amber
        onWarning = Color(0xFF000000),
        info = Color(0xFF42A5F5),            // Brighter blue
        onInfo = Color(0xFF000000),
        recording = Color(0xFFEF5350),       // Brighter red
        onRecording = Color(0xFF000000),
        processing = Color(0xFFAB47BC),      // Brighter purple
        onProcessing = Color(0xFF000000),
        
        // Adjusted neutrals for better contrast
        neutral50 = Color(0xFF9E9E9E),
        neutral60 = Color(0xFFBDBDBD),
        neutral70 = Color(0xFFE0E0E0),
        
        // More pronounced glass effects for dark theme
        glassSurface = Color(0x33FFFFFF),
        glassOutline = Color(0x66FFFFFF),
        glassShadow = Color(0x66000000)
    )
}

/**
 * Helper composables for commonly used color combinations.
 */
object ColorHelpers {
    
    /**
     * Gets appropriate colors for different states.
     */
    @Composable
    fun getStateColors(state: ColorState): StateColorSet {
        val semanticColors = LocalSemanticColors.current
        
        return when (state) {
            ColorState.Success -> StateColorSet(
                primary = semanticColors.success,
                onPrimary = semanticColors.onSuccess,
                container = semanticColors.successContainer,
                onContainer = semanticColors.onSuccessContainer
            )
            ColorState.Warning -> StateColorSet(
                primary = semanticColors.warning,
                onPrimary = semanticColors.onWarning,
                container = semanticColors.warningContainer,
                onContainer = semanticColors.onWarningContainer
            )
            ColorState.Error -> StateColorSet(
                primary = semanticColors.recording,
                onPrimary = semanticColors.onRecording,
                container = semanticColors.recordingContainer,
                onContainer = semanticColors.onRecordingContainer
            )
            ColorState.Info -> StateColorSet(
                primary = semanticColors.info,
                onPrimary = semanticColors.onInfo,
                container = semanticColors.infoContainer,
                onContainer = semanticColors.onInfoContainer
            )
            ColorState.Recording -> StateColorSet(
                primary = semanticColors.recording,
                onPrimary = semanticColors.onRecording,
                container = semanticColors.recordingContainer,
                onContainer = semanticColors.onRecordingContainer
            )
            ColorState.Processing -> StateColorSet(
                primary = semanticColors.processing,
                onPrimary = semanticColors.onProcessing,
                container = semanticColors.processingContainer,
                onContainer = semanticColors.onProcessingContainer
            )
        }
    }
    
    /**
     * Gets voice visualization colors based on intensity.
     */
    @Composable
    fun getVoiceColor(intensity: Float): Color {
        val semanticColors = LocalSemanticColors.current
        
        return when {
            intensity < 0.2f -> semanticColors.voiceWeak
            intensity < 0.5f -> semanticColors.voiceMedium
            intensity < 0.8f -> semanticColors.voiceStrong
            else -> semanticColors.voicePeak
        }
    }
    
    /**
     * Gets glass effect colors for different surfaces.
     */
    @Composable
    fun getGlassColors(): GlassColorSet {
        val semanticColors = LocalSemanticColors.current
        val accessibility = LocalColorAccessibility.current
        
        return if (accessibility.reducedTransparency) {
            // Provide more opaque alternatives for reduced transparency
            GlassColorSet(
                surface = semanticColors.glassSurface.copy(alpha = 0.8f),
                outline = semanticColors.glassOutline.copy(alpha = 1f),
                shadow = semanticColors.glassShadow.copy(alpha = 0.5f)
            )
        } else {
            GlassColorSet(
                surface = semanticColors.glassSurface,
                outline = semanticColors.glassOutline,
                shadow = semanticColors.glassShadow
            )
        }
    }
}

/**
 * Enum for different color states.
 */
enum class ColorState {
    Success, Warning, Error, Info, Recording, Processing
}

/**
 * Data class for state color combinations.
 */
@Immutable
data class StateColorSet(
    val primary: Color,
    val onPrimary: Color,
    val container: Color,
    val onContainer: Color
)

/**
 * Data class for glass effect colors.
 */
@Immutable
data class GlassColorSet(
    val surface: Color,
    val outline: Color,
    val shadow: Color
)

/**
 * Preview helpers for testing color combinations.
 */
object ColorPreviews {
    
    /**
     * Generates a list of all semantic colors for preview purposes.
     */
    @Composable
    fun getAllSemanticColors(): List<Pair<String, Color>> {
        val semanticColors = LocalSemanticColors.current
        
        return listOf(
            "Success" to semanticColors.success,
            "On Success" to semanticColors.onSuccess,
            "Success Container" to semanticColors.successContainer,
            "On Success Container" to semanticColors.onSuccessContainer,
            
            "Warning" to semanticColors.warning,
            "On Warning" to semanticColors.onWarning,
            "Warning Container" to semanticColors.warningContainer,
            "On Warning Container" to semanticColors.onWarningContainer,
            
            "Info" to semanticColors.info,
            "On Info" to semanticColors.onInfo,
            "Info Container" to semanticColors.infoContainer,
            "On Info Container" to semanticColors.onInfoContainer,
            
            "Recording" to semanticColors.recording,
            "On Recording" to semanticColors.onRecording,
            "Recording Container" to semanticColors.recordingContainer,
            "On Recording Container" to semanticColors.onRecordingContainer,
            
            "Processing" to semanticColors.processing,
            "On Processing" to semanticColors.onProcessing,
            "Processing Container" to semanticColors.processingContainer,
            "On Processing Container" to semanticColors.onProcessingContainer,
            
            "Voice Weak" to semanticColors.voiceWeak,
            "Voice Medium" to semanticColors.voiceMedium,
            "Voice Strong" to semanticColors.voiceStrong,
            "Voice Peak" to semanticColors.voicePeak,
            
            "Glass Surface" to semanticColors.glassSurface,
            "Glass Outline" to semanticColors.glassOutline,
            "Glass Shadow" to semanticColors.glassShadow
        )
    }
}