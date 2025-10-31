package com.voicenotesai.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

/**
 * Modern color scheme matching the UI consolidation design specifications.
 * Primary: #6366F1 (Indigo), Tertiary: #8B5CF6 (Purple), Secondary: #10B981 (Green)
 */
@Stable
data class ModernColorScheme(
    val primary: Color = Color(0xFF6366F1),      // Indigo
    val tertiary: Color = Color(0xFF8B5CF6),     // Purple
    val secondary: Color = Color(0xFF10B981),    // Green
    val surface: Color,                          // Card backgrounds
    val background: Color,                       // Screen backgrounds
    val outline: Color,                          // Border colors (20% opacity)
    val onSurface: Color,                        // Primary text
    val onSurfaceVariant: Color,                 // Secondary text
    val onPrimary: Color = Color.White           // Text on gradients
)





/**
 * Light theme color scheme matching design specifications
 */
val ModernLightColorScheme = lightColorScheme(
    primary = Color(0xFF6366F1),           // Indigo
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),  // Light indigo
    onPrimaryContainer = Color(0xFF312E81), // Dark indigo
    
    secondary = Color(0xFF10B981),         // Green
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5), // Light green
    onSecondaryContainer = Color(0xFF064E3B), // Dark green
    
    tertiary = Color(0xFF8B5CF6),          // Purple
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE9FE), // Light purple
    onTertiaryContainer = Color(0xFF581C87), // Dark purple
    
    error = Color(0xFFEF4444),             // Red
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),    // Light red
    onErrorContainer = Color(0xFF991B1B),  // Dark red
    
    background = Color(0xFFFAFAFA),        // Very light gray
    onBackground = Color(0xFF1F2937),      // Dark gray
    
    surface = Color.White,                 // Pure white for cards
    onSurface = Color(0xFF1F2937),         // Dark gray
    surfaceVariant = Color(0xFFF3F4F6),    // Light gray
    onSurfaceVariant = Color(0xFF6B7280),  // Medium gray
    
    outline = Color(0xFFD1D5DB),           // Light border
    outlineVariant = Color(0xFFE5E7EB),    // Very light border
    
    scrim = Color(0xFF000000),
    
    inverseSurface = Color(0xFF1F2937),
    inverseOnSurface = Color(0xFFF9FAFB),
    inversePrimary = Color(0xFF818CF8)     // Light indigo
)

/**
 * Dark theme color scheme matching design specifications
 */
val ModernDarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8),           // Light indigo
    onPrimary = Color(0xFF312E81),         // Dark indigo
    primaryContainer = Color(0xFF4338CA),  // Medium indigo
    onPrimaryContainer = Color(0xFFE0E7FF), // Very light indigo
    
    secondary = Color(0xFF34D399),         // Light green
    onSecondary = Color(0xFF064E3B),       // Dark green
    secondaryContainer = Color(0xFF059669), // Medium green
    onSecondaryContainer = Color(0xFFD1FAE5), // Very light green
    
    tertiary = Color(0xFFA78BFA),          // Light purple
    onTertiary = Color(0xFF581C87),        // Dark purple
    tertiaryContainer = Color(0xFF7C3AED), // Medium purple
    onTertiaryContainer = Color(0xFFEDE9FE), // Very light purple
    
    error = Color(0xFFF87171),             // Light red
    onError = Color(0xFF991B1B),           // Dark red
    errorContainer = Color(0xFFDC2626),    // Medium red
    onErrorContainer = Color(0xFFFEE2E2),  // Very light red
    
    background = Color(0xFF111827),        // Very dark gray
    onBackground = Color(0xFFF9FAFB),      // Very light gray
    
    surface = Color(0xFF1F2937),           // Dark gray for cards
    onSurface = Color(0xFFF9FAFB),         // Very light gray
    surfaceVariant = Color(0xFF374151),    // Medium dark gray
    onSurfaceVariant = Color(0xFF9CA3AF),  // Medium light gray
    
    outline = Color(0xFF4B5563),           // Medium gray border
    outlineVariant = Color(0xFF374151),    // Dark gray border
    
    scrim = Color(0xFF000000),
    
    inverseSurface = Color(0xFFF9FAFB),
    inverseOnSurface = Color(0xFF1F2937),
    inversePrimary = Color(0xFF6366F1)     // Original indigo
)