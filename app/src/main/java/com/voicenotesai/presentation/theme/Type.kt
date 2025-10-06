package com.voicenotesai.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Enhanced font system for better hierarchy and readability
// Using system fonts optimized for cross-platform consistency
private val PrimaryFontFamily = FontFamily.SansSerif
private val DisplayFontFamily = FontFamily.SansSerif
private val MonospaceFontFamily = FontFamily.Monospace

/**
 * Enhanced Typography system following Apple Design Guidelines
 * and Material Design 3 specifications for optimal readability
 * and accessibility compliance.
 */
val Typography = Typography(
    // Display styles - for hero content and main headlines
    displayLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    
    // Headlines - for section headers and important content
    headlineLarge = TextStyle(
        fontFamily = PrimaryFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PrimaryFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = PrimaryFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    
    // Titles - for card headers and navigation
    titleLarge = TextStyle(
        fontFamily = PrimaryFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PrimaryFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PrimaryFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    
    // Body text - optimized for readability and accessibility
    bodyLarge = TextStyle(
        fontFamily = PrimaryFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,  // 1.5x line height for better readability
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PrimaryFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,  // 1.43x line height
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PrimaryFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,  // 1.33x line height
        letterSpacing = 0.4.sp
    ),
    
    // Labels - for buttons, captions, and metadata
    labelLarge = TextStyle(
        fontFamily = PrimaryFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PrimaryFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = MonospaceFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Additional typography styles for app-specific use cases
 */
object ExtendedTypography {
    
    /** For recording timer and duration displays */
    val timerDisplay = TextStyle(
        fontFamily = MonospaceFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    )
    
    /** For note content with enhanced readability */
    val noteContent = TextStyle(
        fontFamily = PrimaryFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,  // Increased line height for note reading
        letterSpacing = 0.2.sp
    )
    
    /** For button text with proper weight */
    val buttonText = TextStyle(
        fontFamily = PrimaryFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
    
    /** For small UI elements and metadata */
    val caption = TextStyle(
        fontFamily = PrimaryFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp
    )
    
    /** For error and validation messages */
    val errorText = TextStyle(
        fontFamily = PrimaryFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp
    )
}
