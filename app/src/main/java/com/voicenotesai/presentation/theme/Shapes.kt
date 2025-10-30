package com.voicenotesai.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Centralized shape tokens used across the app to ensure consistent corner radii
 * and adaptable shapes for different components.
 */
val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),   // Buttons, small chips
    medium = RoundedCornerShape(16.dp), // Cards, dialogs
    large = RoundedCornerShape(24.dp)   // Large surfaces and containers
)
