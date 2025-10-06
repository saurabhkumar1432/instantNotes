package com.voicenotesai.presentation.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.platform.LocalContext

/**
 * Window size class enumeration following Material Design 3 breakpoints
 */
enum class WindowSizeClass {
    Compact,    // < 600dp width
    Medium,     // 600dp - 840dp width  
    Expanded    // > 840dp width
}

/**
 * Screen orientation types
 */
enum class ScreenOrientation {
    Portrait,
    Landscape
}

/**
 * Device type classification
 */
enum class DeviceType {
    Phone,
    Tablet,
    Desktop
}

/**
 * Responsive breakpoint configuration
 */
data class ResponsiveBreakpoints(
    val compactMax: Dp = 599.dp,
    val mediumMax: Dp = 839.dp,
    val expandedMin: Dp = 840.dp
)

/**
 * Layout configuration based on window size and device characteristics
 */
data class LayoutConfig(
    val windowSizeClass: WindowSizeClass,
    val screenOrientation: ScreenOrientation,
    val deviceType: DeviceType,
    val screenWidth: Dp,
    val screenHeight: Dp,
    val isLargeScreen: Boolean,
    val shouldUseNavigationRail: Boolean,
    val shouldUseTwoPane: Boolean,
    val contentPadding: PaddingValues,
    val maxContentWidth: Dp
)

/**
 * Composable that provides responsive layout configuration
 */
@Composable
fun rememberLayoutConfig(
    breakpoints: ResponsiveBreakpoints = ResponsiveBreakpoints()
): LayoutConfig {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    val screenWidthDp = with(density) { configuration.screenWidthDp.dp }
    val screenHeightDp = with(density) { configuration.screenHeightDp.dp }
    
    val windowSizeClass = remember(screenWidthDp) {
        when {
            screenWidthDp < breakpoints.compactMax -> WindowSizeClass.Compact
            screenWidthDp < breakpoints.mediumMax -> WindowSizeClass.Medium
            else -> WindowSizeClass.Expanded
        }
    }
    
    val screenOrientation = remember(configuration.orientation) {
        if (configuration.screenWidthDp > configuration.screenHeightDp) {
            ScreenOrientation.Landscape
        } else {
            ScreenOrientation.Portrait
        }
    }
    
    val deviceType = remember(screenWidthDp, screenHeightDp) {
        when {
            screenWidthDp >= 840.dp -> DeviceType.Desktop
            screenWidthDp >= 600.dp -> DeviceType.Tablet
            else -> DeviceType.Phone
        }
    }
    
    val isLargeScreen = windowSizeClass != WindowSizeClass.Compact
    val shouldUseNavigationRail = windowSizeClass == WindowSizeClass.Expanded
    val shouldUseTwoPane = windowSizeClass == WindowSizeClass.Expanded
    
    val contentPadding = remember(windowSizeClass, screenOrientation) {
        when (windowSizeClass) {
            WindowSizeClass.Compact -> PaddingValues(16.dp)
            WindowSizeClass.Medium -> PaddingValues(
                horizontal = if (screenOrientation == ScreenOrientation.Landscape) 32.dp else 24.dp,
                vertical = 16.dp
            )
            WindowSizeClass.Expanded -> PaddingValues(
                horizontal = 48.dp,
                vertical = 24.dp
            )
        }
    }
    
    val maxContentWidth = remember(windowSizeClass) {
        when (windowSizeClass) {
            WindowSizeClass.Compact -> Dp.Unspecified
            WindowSizeClass.Medium -> 720.dp
            WindowSizeClass.Expanded -> 1200.dp
        }
    }
    
    return LayoutConfig(
        windowSizeClass = windowSizeClass,
        screenOrientation = screenOrientation,
        deviceType = deviceType,
        screenWidth = screenWidthDp,
        screenHeight = screenHeightDp,
        isLargeScreen = isLargeScreen,
        shouldUseNavigationRail = shouldUseNavigationRail,
        shouldUseTwoPane = shouldUseTwoPane,
        contentPadding = contentPadding,
        maxContentWidth = maxContentWidth
    )
}

/**
 * Adaptive spacing based on screen size
 */
@Composable
fun adaptiveSpacing(
    compact: Dp = 8.dp,
    medium: Dp = 12.dp,
    expanded: Dp = 16.dp,
    layoutConfig: LayoutConfig = rememberLayoutConfig()
): Dp {
    return when (layoutConfig.windowSizeClass) {
        WindowSizeClass.Compact -> compact
        WindowSizeClass.Medium -> medium
        WindowSizeClass.Expanded -> expanded
    }
}

/**
 * Adaptive padding values
 */
@Composable
fun adaptivePadding(
    compactHorizontal: Dp = 16.dp,
    compactVertical: Dp = 16.dp,
    mediumHorizontal: Dp = 24.dp,
    mediumVertical: Dp = 20.dp,
    expandedHorizontal: Dp = 32.dp,
    expandedVertical: Dp = 24.dp,
    layoutConfig: LayoutConfig = rememberLayoutConfig()
): PaddingValues {
    return when (layoutConfig.windowSizeClass) {
        WindowSizeClass.Compact -> PaddingValues(
            horizontal = compactHorizontal,
            vertical = compactVertical
        )
        WindowSizeClass.Medium -> PaddingValues(
            horizontal = mediumHorizontal,
            vertical = mediumVertical
        )
        WindowSizeClass.Expanded -> PaddingValues(
            horizontal = expandedHorizontal,
            vertical = expandedVertical
        )
    }
}

/**
 * Get number of columns for grid layouts based on screen size
 */
@Composable
fun adaptiveColumns(
    compact: Int = 1,
    medium: Int = 2,
    expanded: Int = 3,
    layoutConfig: LayoutConfig = rememberLayoutConfig()
): Int {
    return when (layoutConfig.windowSizeClass) {
        WindowSizeClass.Compact -> compact
        WindowSizeClass.Medium -> medium
        WindowSizeClass.Expanded -> expanded
    }
}

/**
 * Adaptive content width with maximum constraints
 */
@Composable
fun adaptiveContentWidth(
    fillWidth: Boolean = true,
    maxWidth: Dp = Dp.Unspecified,
    layoutConfig: LayoutConfig = rememberLayoutConfig()
): Dp {
    return when {
        !fillWidth -> Dp.Unspecified
        layoutConfig.maxContentWidth != Dp.Unspecified && maxWidth != Dp.Unspecified -> 
            minOf(layoutConfig.maxContentWidth, maxWidth)
        layoutConfig.maxContentWidth != Dp.Unspecified -> layoutConfig.maxContentWidth
        maxWidth != Dp.Unspecified -> maxWidth
        else -> Dp.Unspecified
    }
}

private fun minOf(dp1: Dp, dp2: Dp): Dp {
    return if (dp1.value <= dp2.value) dp1 else dp2
}