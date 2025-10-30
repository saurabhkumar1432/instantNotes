package com.voicenotesai.presentation.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Enhanced layout configuration with accessibility and responsive features
 */
@Stable
data class EnhancedLayoutConfig(
    val windowSizeClass: WindowSizeClass,
    val screenOrientation: ScreenOrientation,
    val deviceType: DeviceType,
    val screenWidth: Dp,
    val screenHeight: Dp,
    val isLargeScreen: Boolean,
    val shouldUseNavigationRail: Boolean,
    val shouldUseTwoPane: Boolean,
    val shouldUseBottomSheet: Boolean,
    val contentPadding: PaddingValues,
    val maxContentWidth: Dp,
    val gridColumns: Int,
    val listItemHeight: Dp,
    val fabPosition: FabPosition,
    val navigationStyle: NavigationStyle
)

/**
 * FAB position based on screen size and orientation
 */
enum class FabPosition {
    BottomEnd,
    BottomCenter,
    EndCenter
}

/**
 * Navigation style based on screen size
 */
enum class NavigationStyle {
    BottomNavigation,
    NavigationRail,
    NavigationDrawer
}

/**
 * Enhanced responsive layout configuration
 */
@Composable
fun rememberEnhancedLayoutConfig(): EnhancedLayoutConfig {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    val screenWidthDp = with(density) { configuration.screenWidthDp.dp }
    val screenHeightDp = with(density) { configuration.screenHeightDp.dp }
    
    val windowSizeClass = when {
        screenWidthDp < 600.dp -> WindowSizeClass.Compact
        screenWidthDp < 840.dp -> WindowSizeClass.Medium
        else -> WindowSizeClass.Expanded
    }
    
    val screenOrientation = if (configuration.screenWidthDp > configuration.screenHeightDp) {
        ScreenOrientation.Landscape
    } else {
        ScreenOrientation.Portrait
    }
    
    val deviceType = when {
        screenWidthDp >= 840.dp -> DeviceType.Desktop
        screenWidthDp >= 600.dp -> DeviceType.Tablet
        else -> DeviceType.Phone
    }
    
    val isLargeScreen = windowSizeClass != WindowSizeClass.Compact
    val shouldUseNavigationRail = windowSizeClass == WindowSizeClass.Expanded
    val shouldUseTwoPane = windowSizeClass == WindowSizeClass.Expanded && 
                          screenOrientation == ScreenOrientation.Landscape
    val shouldUseBottomSheet = windowSizeClass == WindowSizeClass.Compact
    
    val contentPadding = when (windowSizeClass) {
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
    
    val maxContentWidth = when (windowSizeClass) {
        WindowSizeClass.Compact -> Dp.Unspecified
        WindowSizeClass.Medium -> 720.dp
        WindowSizeClass.Expanded -> 1200.dp
    }
    
    val gridColumns = when (windowSizeClass) {
        WindowSizeClass.Compact -> if (screenOrientation == ScreenOrientation.Landscape) 2 else 1
        WindowSizeClass.Medium -> if (screenOrientation == ScreenOrientation.Landscape) 3 else 2
        WindowSizeClass.Expanded -> if (screenOrientation == ScreenOrientation.Landscape) 4 else 3
    }
    
    val listItemHeight = when (windowSizeClass) {
        WindowSizeClass.Compact -> 72.dp
        WindowSizeClass.Medium -> 80.dp
        WindowSizeClass.Expanded -> 88.dp
    }
    
    val fabPosition = when {
        windowSizeClass == WindowSizeClass.Expanded -> FabPosition.EndCenter
        screenOrientation == ScreenOrientation.Landscape -> FabPosition.BottomEnd
        else -> FabPosition.BottomEnd
    }
    
    val navigationStyle = when (windowSizeClass) {
        WindowSizeClass.Compact -> NavigationStyle.BottomNavigation
        WindowSizeClass.Medium -> if (screenOrientation == ScreenOrientation.Landscape) {
            NavigationStyle.NavigationRail
        } else {
            NavigationStyle.BottomNavigation
        }
        WindowSizeClass.Expanded -> NavigationStyle.NavigationRail
    }
    
    return EnhancedLayoutConfig(
        windowSizeClass = windowSizeClass,
        screenOrientation = screenOrientation,
        deviceType = deviceType,
        screenWidth = screenWidthDp,
        screenHeight = screenHeightDp,
        isLargeScreen = isLargeScreen,
        shouldUseNavigationRail = shouldUseNavigationRail,
        shouldUseTwoPane = shouldUseTwoPane,
        shouldUseBottomSheet = shouldUseBottomSheet,
        contentPadding = contentPadding,
        maxContentWidth = maxContentWidth,
        gridColumns = gridColumns,
        listItemHeight = listItemHeight,
        fabPosition = fabPosition,
        navigationStyle = navigationStyle
    )
}

/**
 * Responsive container that adapts content width and padding
 */
@Composable
fun ResponsiveContainer(
    modifier: Modifier = Modifier,
    layoutConfig: EnhancedLayoutConfig = rememberEnhancedLayoutConfig(),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .let { mod ->
                    if (layoutConfig.maxContentWidth != Dp.Unspecified) {
                        mod.widthIn(max = layoutConfig.maxContentWidth)
                    } else {
                        mod
                    }
                }
                .padding(layoutConfig.contentPadding)
        ) {
            content()
        }
    }
}

/**
 * Adaptive two-pane layout for large screens
 */
@Composable
fun AdaptiveTwoPane(
    modifier: Modifier = Modifier,
    layoutConfig: EnhancedLayoutConfig = rememberEnhancedLayoutConfig(),
    primaryPane: @Composable () -> Unit,
    secondaryPane: @Composable () -> Unit,
    singlePane: @Composable () -> Unit = primaryPane
) {
    if (layoutConfig.shouldUseTwoPane) {
        Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxSize()
            ) {
                primaryPane()
            }
            Box(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxSize()
            ) {
                secondaryPane()
            }
        }
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            singlePane()
        }
    }
}

/**
 * Adaptive grid layout with responsive columns
 */
@Composable
fun AdaptiveGrid(
    modifier: Modifier = Modifier,
    layoutConfig: EnhancedLayoutConfig = rememberEnhancedLayoutConfig(),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    content: @Composable () -> Unit
) {
    when (layoutConfig.gridColumns) {
        1 -> {
            Column(
                modifier = modifier,
                verticalArrangement = verticalArrangement
            ) {
                content()
            }
        }
        else -> {
            // For multi-column layouts, we'd typically use LazyVerticalGrid
            // This is a simplified version for demonstration
            Column(
                modifier = modifier,
                verticalArrangement = verticalArrangement
            ) {
                content()
            }
        }
    }
}

/**
 * Responsive text scaling based on screen size
 */
@Composable
fun adaptiveTextScale(
    layoutConfig: EnhancedLayoutConfig = rememberEnhancedLayoutConfig()
): Float {
    return when (layoutConfig.windowSizeClass) {
        WindowSizeClass.Compact -> 1.0f
        WindowSizeClass.Medium -> 1.1f
        WindowSizeClass.Expanded -> 1.2f
    }
}

/**
 * Adaptive spacing for different screen densities
 */
@Composable
fun adaptiveSpacingForDensity(
    baseSpacing: Dp,
    layoutConfig: EnhancedLayoutConfig = rememberEnhancedLayoutConfig()
): Dp {
    val densityMultiplier = when (layoutConfig.deviceType) {
        DeviceType.Phone -> 1.0f
        DeviceType.Tablet -> 1.2f
        DeviceType.Desktop -> 1.4f
    }
    return baseSpacing * densityMultiplier
}

/**
 * Responsive button size based on touch target requirements
 */
@Composable
fun adaptiveButtonSize(
    layoutConfig: EnhancedLayoutConfig = rememberEnhancedLayoutConfig()
): Dp {
    return when (layoutConfig.windowSizeClass) {
        WindowSizeClass.Compact -> 48.dp // Minimum touch target
        WindowSizeClass.Medium -> 52.dp
        WindowSizeClass.Expanded -> 56.dp
    }
}