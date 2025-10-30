package com.voicenotesai.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Text size scale for accessibility support
 */
enum class TextSizeScale(val multiplier: Float) {
    Small(0.85f),
    Default(1.0f),
    Large(1.15f),
    ExtraLarge(1.3f),
    Huge(1.5f)
}

/**
 * Advanced typography system with customizable text sizes and proper spacing
 * Implements Material Design 3 typography with accessibility enhancements
 */
@Stable
class AdvancedTypography(
    private val textSizeScale: TextSizeScale = TextSizeScale.Default,
    private val useSystemFonts: Boolean = true,
    private val highContrastMode: Boolean = false
) {
    
    private val primaryFontFamily = if (useSystemFonts) FontFamily.SansSerif else FontFamily.Default
    private val displayFontFamily = if (useSystemFonts) FontFamily.SansSerif else FontFamily.Default
    private val monospaceFontFamily = FontFamily.Monospace
    
    /**
     * Scale text size based on accessibility preferences
     */
    private fun TextUnit.scaled(): TextUnit = this * textSizeScale.multiplier
    
    /**
     * Enhanced line height calculation for better readability
     */
    private fun calculateLineHeight(fontSize: TextUnit): TextUnit {
        val baseRatio = when {
            fontSize.value <= 12f -> 1.5f
            fontSize.value <= 16f -> 1.4f
            fontSize.value <= 24f -> 1.3f
            else -> 1.2f
        }
        
        // Increase line height for accessibility
        val accessibilityMultiplier = if (textSizeScale != TextSizeScale.Default) 1.1f else 1.0f
        
        return fontSize * baseRatio * accessibilityMultiplier
    }
    
    /**
     * Enhanced letter spacing for improved readability
     */
    private fun calculateLetterSpacing(fontSize: TextUnit): TextUnit {
        return when {
            fontSize.value <= 12f -> 0.4.sp
            fontSize.value <= 16f -> 0.25.sp
            fontSize.value <= 24f -> 0.15.sp
            fontSize.value <= 32f -> 0.1.sp
            else -> 0.sp
        }
    }
    
    /**
     * Material Design 3 Typography with accessibility enhancements
     */
    val typography = Typography(
        // Display styles - for hero content and main headlines
        displayLarge = TextStyle(
            fontFamily = displayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 57.sp.scaled(),
            lineHeight = calculateLineHeight(57.sp.scaled()),
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontFamily = displayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 45.sp.scaled(),
            lineHeight = calculateLineHeight(45.sp.scaled()),
            letterSpacing = 0.sp
        ),
        displaySmall = TextStyle(
            fontFamily = displayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 36.sp.scaled(),
            lineHeight = calculateLineHeight(36.sp.scaled()),
            letterSpacing = 0.sp
        ),
        
        // Headlines - for section headers and important content
        headlineLarge = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp.scaled(),
            lineHeight = calculateLineHeight(32.sp.scaled()),
            letterSpacing = calculateLetterSpacing(32.sp.scaled())
        ),
        headlineMedium = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp.scaled(),
            lineHeight = calculateLineHeight(28.sp.scaled()),
            letterSpacing = calculateLetterSpacing(28.sp.scaled())
        ),
        headlineSmall = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp.scaled(),
            lineHeight = calculateLineHeight(24.sp.scaled()),
            letterSpacing = calculateLetterSpacing(24.sp.scaled())
        ),
        
        // Titles - for card headers and navigation
        titleLarge = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp.scaled(),
            lineHeight = calculateLineHeight(22.sp.scaled()),
            letterSpacing = calculateLetterSpacing(22.sp.scaled())
        ),
        titleMedium = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp.scaled(),
            lineHeight = calculateLineHeight(16.sp.scaled()),
            letterSpacing = calculateLetterSpacing(16.sp.scaled())
        ),
        titleSmall = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp.scaled(),
            lineHeight = calculateLineHeight(14.sp.scaled()),
            letterSpacing = calculateLetterSpacing(14.sp.scaled())
        ),
        
        // Body text - optimized for readability and accessibility
        bodyLarge = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp.scaled(),
            lineHeight = calculateLineHeight(16.sp.scaled()),
            letterSpacing = calculateLetterSpacing(16.sp.scaled())
        ),
        bodyMedium = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp.scaled(),
            lineHeight = calculateLineHeight(14.sp.scaled()),
            letterSpacing = calculateLetterSpacing(14.sp.scaled())
        ),
        bodySmall = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp.scaled(),
            lineHeight = calculateLineHeight(12.sp.scaled()),
            letterSpacing = calculateLetterSpacing(12.sp.scaled())
        ),
        
        // Labels - for buttons, captions, and metadata
        labelLarge = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp.scaled(),
            lineHeight = calculateLineHeight(14.sp.scaled()),
            letterSpacing = calculateLetterSpacing(14.sp.scaled())
        ),
        labelMedium = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp.scaled(),
            lineHeight = calculateLineHeight(12.sp.scaled()),
            letterSpacing = calculateLetterSpacing(12.sp.scaled())
        ),
        labelSmall = TextStyle(
            fontFamily = monospaceFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp.scaled(),
            lineHeight = calculateLineHeight(11.sp.scaled()),
            letterSpacing = calculateLetterSpacing(11.sp.scaled())
        )
    )
    
    /**
     * Extended typography styles for app-specific use cases
     */
    object Extended {
        
        @Composable
        fun timerDisplay(typography: AdvancedTypography): TextStyle = with(typography) {
            TextStyle(
                fontFamily = monospaceFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp.scaled(),
                lineHeight = calculateLineHeight(28.sp.scaled()),
                letterSpacing = 0.sp
            )
        }
        
        @Composable
        fun noteContent(typography: AdvancedTypography): TextStyle = with(typography) {
            TextStyle(
                fontFamily = primaryFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp.scaled(),
                lineHeight = calculateLineHeight(16.sp.scaled()) * 1.1f, // Extra line height for note reading
                letterSpacing = calculateLetterSpacing(16.sp.scaled())
            )
        }
        
        @Composable
        fun buttonText(typography: AdvancedTypography): TextStyle = with(typography) {
            TextStyle(
                fontFamily = primaryFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp.scaled(),
                lineHeight = calculateLineHeight(14.sp.scaled()),
                letterSpacing = calculateLetterSpacing(14.sp.scaled())
            )
        }
        
        @Composable
        fun caption(typography: AdvancedTypography): TextStyle = with(typography) {
            TextStyle(
                fontFamily = primaryFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp.scaled(),
                lineHeight = calculateLineHeight(11.sp.scaled()),
                letterSpacing = calculateLetterSpacing(11.sp.scaled())
            )
        }
        
        @Composable
        fun errorText(typography: AdvancedTypography): TextStyle = with(typography) {
            TextStyle(
                fontFamily = primaryFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp.scaled(),
                lineHeight = calculateLineHeight(12.sp.scaled()),
                letterSpacing = calculateLetterSpacing(12.sp.scaled())
            )
        }
    }
}

/**
 * Create advanced typography with accessibility settings
 */
@Composable
fun rememberAdvancedTypography(
    textSizeScale: TextSizeScale = TextSizeScale.Default,
    useSystemFonts: Boolean = true,
    highContrastMode: Boolean = false
): AdvancedTypography {
    return AdvancedTypography(
        textSizeScale = textSizeScale,
        useSystemFonts = useSystemFonts,
        highContrastMode = highContrastMode
    )
}