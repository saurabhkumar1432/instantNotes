package com.voicenotesai.presentation.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.voicenotesai.presentation.theme.Spacing

/**
 * Navigation destinations
 */
import androidx.annotation.StringRes

data class NavDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    @StringRes val contentDescriptionRes: Int
)

/**
 * Adaptive scaffold that switches between bottom navigation and navigation rail
 * based on screen size
 */
@Composable
fun AdaptiveScaffold(
    currentRoute: String,
    onNavigationClick: (String) -> Unit,
    layoutConfig: LayoutConfig = rememberLayoutConfig(),
    topBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    val destinations = remember {
        listOf(
            NavDestination(
                route = "main",
                labelRes = com.voicenotesai.R.string.nav_label_record,
                icon = Icons.Default.Warning, // Using Warning as placeholder for record icon
                contentDescriptionRes = com.voicenotesai.R.string.go_to_recording_nav
            ),
            NavDestination(
                route = "notes",
                labelRes = com.voicenotesai.R.string.nav_label_notes,
                icon = Icons.Default.List,
                contentDescriptionRes = com.voicenotesai.R.string.view_all_notes_nav
            ),
            NavDestination(
                route = "settings",
                labelRes = com.voicenotesai.R.string.nav_label_settings,
                icon = Icons.Default.Settings,
                contentDescriptionRes = com.voicenotesai.R.string.open_settings_nav
            )
        )
    }

    when {
        layoutConfig.shouldUseNavigationRail -> {
            // Large screens: Navigation Rail + Content
            Row(modifier = Modifier.fillMaxSize()) {
                AdaptiveNavigationRail(
                    destinations = destinations,
                    currentRoute = currentRoute,
                    onNavigationClick = onNavigationClick,
                    modifier = Modifier.fillMaxHeight()
                )
                
                Scaffold(
                    modifier = Modifier.weight(1f),
                    containerColor = Color.Transparent,
                    topBar = topBar,
                    floatingActionButton = floatingActionButton
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        content()
                    }
                }
            }
        }
        else -> {
            // Small/Medium screens: Traditional bottom navigation
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                topBar = topBar,
                bottomBar = {
                    AdaptiveBottomNavigation(
                        destinations = destinations,
                        currentRoute = currentRoute,
                        onNavigationClick = onNavigationClick
                    )
                },
                floatingActionButton = floatingActionButton
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Navigation rail for large screens
 */
@Composable
private fun AdaptiveNavigationRail(
    destinations: List<NavDestination>,
    currentRoute: String,
    onNavigationClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    Surface(
        modifier = modifier.width(80.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 3.dp
    ) {
        NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            containerColor = Color.Transparent
        ) {
            Spacer(modifier = Modifier.height(Spacing.large))
            
            destinations.forEach { destination ->
                val navContentDesc = stringResource(id = destination.contentDescriptionRes)
                NavigationRailItem(
                    selected = currentRoute.startsWith(destination.route),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigationClick(destination.route)
                    },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(id = destination.labelRes),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.semantics {
                        // Use precomputed description
                        contentDescription = navContentDesc
                    }
                )
            }
        }
    }
}

/**
 * Bottom navigation for small and medium screens
 */
@Composable
private fun AdaptiveBottomNavigation(
    destinations: List<NavDestination>,
    currentRoute: String,
    onNavigationClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        NavigationBar(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 3.dp
        ) {
            destinations.forEach { destination ->
                val navContentDesc = stringResource(id = destination.contentDescriptionRes)
                NavigationBarItem(
                    selected = currentRoute.startsWith(destination.route),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigationClick(destination.route)
                    },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(id = destination.labelRes),
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    modifier = Modifier.semantics {
                        contentDescription = navContentDesc
                    }
                )
            }
        }
    }
}

/**
 * Two-pane layout for expanded screens
 */
@Composable
fun AdaptiveTwoPane(
    layoutConfig: LayoutConfig = rememberLayoutConfig(),
    primaryContent: @Composable () -> Unit,
    secondaryContent: @Composable (() -> Unit)? = null,
    primaryWeight: Float = 0.6f,
    modifier: Modifier = Modifier
) {
    if (layoutConfig.shouldUseTwoPane && secondaryContent != null) {
        Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            // Primary content pane
            Box(
                modifier = Modifier
                    .weight(primaryWeight)
                    .fillMaxHeight()
            ) {
                primaryContent()
            }
            
            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            )
            
            // Secondary content pane
            Box(
                modifier = Modifier
                    .weight(1f - primaryWeight)
                    .fillMaxHeight()
            ) {
                secondaryContent()
            }
        }
    } else {
        // Single pane layout
        Box(modifier = modifier.fillMaxSize()) {
            primaryContent()
        }
    }
}

/**
 * Responsive content container with proper spacing and maximum width
 */
@Composable
fun ResponsiveContentContainer(
    layoutConfig: LayoutConfig = rememberLayoutConfig(),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val maxWidth = adaptiveContentWidth(layoutConfig = layoutConfig)
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (maxWidth != Dp.Unspecified) {
                        Modifier.widthIn(max = maxWidth)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                )
                .padding(layoutConfig.contentPadding)
        ) {
            content()
        }
    }
}

/**
 * Adaptive card layout for different screen sizes
 */
@Composable
fun AdaptiveCard(
    layoutConfig: LayoutConfig = rememberLayoutConfig(),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val cornerRadius = when (layoutConfig.windowSizeClass) {
        WindowSizeClass.Compact -> 16.dp
        WindowSizeClass.Medium -> 20.dp  
        WindowSizeClass.Expanded -> 24.dp
    }
    
    val elevation = when (layoutConfig.windowSizeClass) {
        WindowSizeClass.Compact -> 2.dp
        WindowSizeClass.Medium -> 4.dp
        WindowSizeClass.Expanded -> 6.dp
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = elevation,
        shadowElevation = elevation
    ) {
        Box(
            modifier = Modifier.padding(
                when (layoutConfig.windowSizeClass) {
                    WindowSizeClass.Compact -> Spacing.medium
                    WindowSizeClass.Medium -> Spacing.large
                    WindowSizeClass.Expanded -> Spacing.extraLarge
                }
            )
        ) {
            content()
        }
    }
}