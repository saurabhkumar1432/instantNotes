package com.voicenotesai.presentation.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.voicenotesai.presentation.main.MainScreen
import com.voicenotesai.presentation.notes.NoteDetailScreen
import com.voicenotesai.presentation.notes.NotesScreen
import com.voicenotesai.presentation.onboarding.OnboardingScreen
import com.voicenotesai.presentation.settings.SettingsScreen
import com.voicenotesai.presentation.settings.SettingsViewModel

/**
 * Responsive navigation graph that adapts to different screen sizes
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun ResponsiveNavGraph(
    navController: NavHostController,
    layoutConfig: LayoutConfig = rememberLayoutConfig()
) {
    val currentRoute = navController.currentBackStackEntry?.destination?.route ?: "main"
    
    when {
        // Large screens with two-pane layout for notes
        layoutConfig.shouldUseTwoPane && currentRoute.startsWith("notes") -> {
            TwoPaneNotesLayout(
                navController = navController,
                layoutConfig = layoutConfig
            )
        }
        // Adaptive navigation for main app screens
        currentRoute in listOf("main", "notes", "settings") -> {
            AdaptiveScaffold(
                currentRoute = currentRoute,
                onNavigationClick = { route ->
                    if (route != currentRoute.substringBefore("/")) {
                        navController.navigate(route) {
                            // Avoid multiple copies of the same destination
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                layoutConfig = layoutConfig
            ) {
                ResponsiveContentContainer(layoutConfig = layoutConfig) {
                    StandardNavHost(navController = navController)
                }
            }
        }
        // Full-screen layouts for onboarding, setup, etc.
        else -> {
            ResponsiveContentContainer(layoutConfig = layoutConfig) {
                StandardNavHost(navController = navController)
            }
        }
    }
}

/**
 * Two-pane layout specifically for notes screens on large displays
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
private fun TwoPaneNotesLayout(
    navController: NavHostController,
    layoutConfig: LayoutConfig
) {
    val currentRoute = navController.currentBackStackEntry?.destination?.route ?: "notes"
    val noteId = navController.currentBackStackEntry?.arguments?.getLong("noteId")
    
    AdaptiveScaffold(
        currentRoute = "notes", // Always show notes as selected in navigation
        onNavigationClick = { route ->
            if (route != "notes") {
                navController.navigate(route) {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        layoutConfig = layoutConfig
    ) {
        AdaptiveTwoPane(
            layoutConfig = layoutConfig,
            primaryContent = {
                // Notes list in primary pane
                NotesScreen(
                    onNavigateBack = {
                        navController.navigate("main") {
                            popUpTo("notes") { inclusive = true }
                        }
                    },
                    onNoteClick = { selectedNoteId ->
                        navController.navigate("note_detail/$selectedNoteId")
                    }
                )
            },
            secondaryContent = if (noteId != null) {
                {
                    // Note detail in secondary pane
                    NoteDetailScreen(
                        noteId = noteId,
                        onNavigateBack = {
                            navController.navigate("notes") {
                                popUpTo("note_detail/{noteId}") { inclusive = true }
                            }
                        }
                    )
                }
            } else null,
            primaryWeight = 0.4f // Give more space to detail view
        )
    }
}

/**
 * Standard navigation host for single-pane layouts
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
private fun StandardNavHost(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = "setup_check"
    ) {
        // Setup check screen
        composable("setup_check") {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val uiState by settingsViewModel.uiState.collectAsState()
            
            LaunchedEffect(uiState.isLoading) {
                if (!uiState.isLoading) {
                    if (uiState.apiKey.isNotBlank() && 
                        uiState.model.isNotBlank() &&
                        uiState.validationStatus == com.voicenotesai.presentation.settings.ValidationStatus.SUCCESS) {
                        navController.navigate("main") {
                            popUpTo("setup_check") { inclusive = true }
                        }
                    } else {
                        navController.navigate("onboarding") {
                            popUpTo("setup_check") { inclusive = true }
                        }
                    }
                }
            }
            
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        // Onboarding flow
        composable("onboarding") {
            OnboardingScreen(
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToMain = {
                    navController.navigate("main") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                onStartFirstRecording = {
                    navController.navigate("main") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        
        // Main recording screen
        composable("main") {
            MainScreen(
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToNotes = {
                    navController.navigate("notes")
                }
            )
        }
        
        // Settings screen
        composable("settings") {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val uiState by settingsViewModel.uiState.collectAsState()
            
            LaunchedEffect(uiState.isSaved) {
                if (uiState.isSaved && uiState.validationStatus == com.voicenotesai.presentation.settings.ValidationStatus.SUCCESS) {
                    navController.navigate("main") {
                        popUpTo("settings") { inclusive = true }
                    }
                }
            }
            
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = {
                    if (navController.previousBackStackEntry?.destination?.route != null) {
                        navController.popBackStack()
                    }
                }
            )
        }
        
        // Notes list screen
        composable("notes") {
            NotesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNoteClick = { noteId ->
                    navController.navigate("note_detail/$noteId")
                }
            )
        }
        
        // Note detail screen
        composable(
            route = "note_detail/{noteId}",
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: return@composable
            NoteDetailScreen(
                noteId = noteId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}