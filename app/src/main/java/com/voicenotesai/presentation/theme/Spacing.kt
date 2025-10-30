package com.voicenotesai.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.voicenotesai.presentation.layout.WindowSizeClass
import com.voicenotesai.presentation.layout.rememberLayoutConfig

/**
 * Spacing scale for accessibility support
 */
enum class SpacingScale(val multiplier: Float) {
    Compact(0.8f),
    Default(1.0f),
    Comfortable(1.2f),
    Spacious(1.5f)
}

/**
 * Advanced spacing system with responsive and accessibility support
 */
@Stable
class AdvancedSpacing(
    private val spacingScale: SpacingScale = SpacingScale.Default,
    private val windowSizeClass: WindowSizeClass = WindowSizeClass.Compact
) {
    
    /**
     * Scale spacing based on accessibility preferences
     */
    private fun Dp.scaled(): Dp = this * spacingScale.multiplier
    
    /**
     * Adapt spacing based on screen size
     */
    private fun Dp.adaptive(): Dp {
        val sizeMultiplier = when (windowSizeClass) {
            WindowSizeClass.Compact -> 1.0f
            WindowSizeClass.Medium -> 1.2f
            WindowSizeClass.Expanded -> 1.4f
        }
        return this * sizeMultiplier
    }
    
    /**
     * Base spacing values
     */
    val none = 0.dp
    val extraSmall = 4.dp.scaled().adaptive()
    val small = 8.dp.scaled().adaptive()
    val medium = 16.dp.scaled().adaptive()
    val large = 24.dp.scaled().adaptive()
    val extraLarge = 32.dp.scaled().adaptive()
    val huge = 48.dp.scaled().adaptive()
    val massive = 64.dp.scaled().adaptive()
    
    /**
     * Semantic spacing for specific use cases
     */
    val contentPadding = medium
    val cardPadding = medium
    val sectionSpacing = large
    val componentSpacing = small
    val itemSpacing = extraSmall
    
    /**
     * Touch target spacing for accessibility
     */
    val touchTargetSpacing = when (spacingScale) {
        SpacingScale.Compact -> 8.dp
        SpacingScale.Default -> 12.dp
        SpacingScale.Comfortable -> 16.dp
        SpacingScale.Spacious -> 20.dp
    }.adaptive()
    
    /**
     * Text spacing for improved readability
     */
    val textSpacing = when (spacingScale) {
        SpacingScale.Compact -> 4.dp
        SpacingScale.Default -> 6.dp
        SpacingScale.Comfortable -> 8.dp
        SpacingScale.Spacious -> 12.dp
    }.adaptive()
    
    /**
     * Layout margins based on screen size
     */
    val screenMargin = when (windowSizeClass) {
        WindowSizeClass.Compact -> 16.dp
        WindowSizeClass.Medium -> 24.dp
        WindowSizeClass.Expanded -> 32.dp
    }.scaled()
    
    /**
     * Content max width for readability
     */
    val maxContentWidth = when (windowSizeClass) {
        WindowSizeClass.Compact -> Dp.Unspecified
        WindowSizeClass.Medium -> 720.dp
        WindowSizeClass.Expanded -> 1200.dp
    }
}

/**
 * Create advanced spacing with accessibility and responsive settings
 */
@Composable
fun rememberAdvancedSpacing(
    spacingScale: SpacingScale = SpacingScale.Default
): AdvancedSpacing {
    val layoutConfig = rememberLayoutConfig()
    return AdvancedSpacing(
        spacingScale = spacingScale,
        windowSizeClass = layoutConfig.windowSizeClass
    )
}

/**
 * Legacy spacing object for backward compatibility
 */
object Spacing {
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val extraLarge = 32.dp
    val huge = 48.dp
}
