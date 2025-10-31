package com.voicenotesai.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Gradient system for horizontal header gradients and vertical waveform gradients
 * as specified in the UI consolidation modernization requirements.
 */
object GradientSystem {
    
    // Fixed color gradients using the specified Material You colors
    private val primaryColor = Color(0xFF6366F1)  // Indigo
    private val tertiaryColor = Color(0xFF8B5CF6) // Purple
    
    /**
     * Horizontal gradient for headers using primary to tertiary colors
     * Used for gradient headers throughout the app
     */
    @Composable
    fun headerGradient() = Brush.horizontalGradient(
        colors = listOf(primaryColor, tertiaryColor)
    )
    
    /**
     * Vertical gradient for waveforms using primary to tertiary colors
     * Used for animated waveform visualizations
     */
    @Composable
    fun waveformGradient() = Brush.verticalGradient(
        colors = listOf(primaryColor, tertiaryColor)
    )
    
    /**
     * Dynamic horizontal gradient using ColorScheme for theme-aware gradients
     * Adapts to light/dark theme changes
     */
    @Composable
    fun headerGradient(colorScheme: ColorScheme) = Brush.horizontalGradient(
        colors = listOf(
            colorScheme.primary,
            colorScheme.tertiary
        )
    )
    
    /**
     * Dynamic vertical gradient using ColorScheme for theme-aware gradients
     * Adapts to light/dark theme changes
     */
    @Composable
    fun waveformGradient(colorScheme: ColorScheme) = Brush.verticalGradient(
        colors = listOf(
            colorScheme.primary,
            colorScheme.tertiary
        )
    )
    
    /**
     * Additional gradient variations for different use cases
     */
    @Composable
    fun cardGradient() = Brush.linearGradient(
        colors = listOf(
            primaryColor.copy(alpha = 0.1f),
            tertiaryColor.copy(alpha = 0.05f)
        )
    )
    
    @Composable
    fun buttonGradient() = Brush.horizontalGradient(
        colors = listOf(primaryColor, tertiaryColor)
    )
}