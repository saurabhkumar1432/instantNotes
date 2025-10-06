package com.voicenotesai.presentation.navigation

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
 * Navigation routes for the app.
 */
object NavRoutes {
    const val SETUP_CHECK = "setup_check"
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val NOTES = "notes"
    const val NOTE_DETAIL = "note_detail/{noteId}"
    
    fun noteDetail(noteId: Long): String = "note_detail/$noteId"
}

/**
 * Sets up the navigation graph for the app with responsive layout support.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.SETUP_CHECK
    ) {
        // Setup check screen - validates settings before allowing app access
        composable(NavRoutes.SETUP_CHECK) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val uiState by settingsViewModel.uiState.collectAsState()
            
            LaunchedEffect(uiState.isLoading) {
                // Wait for settings to load
                if (!uiState.isLoading) {
                    // Check if settings exist and are validated
                    if (uiState.apiKey.isNotBlank() && 
                        uiState.model.isNotBlank() &&
                        uiState.validationStatus == com.voicenotesai.presentation.settings.ValidationStatus.SUCCESS) {
                        // Settings are valid, navigate to main
                        navController.navigate(NavRoutes.MAIN) {
                            popUpTo(NavRoutes.SETUP_CHECK) { inclusive = true }
                        }
                    } else {
                        // First time user - show onboarding
                        navController.navigate(NavRoutes.ONBOARDING) {
                            popUpTo(NavRoutes.SETUP_CHECK) { inclusive = true }
                        }
                    }
                }
            }
            
            // Show a simple loading screen while checking
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        // Onboarding flow
        composable(NavRoutes.ONBOARDING) {
            OnboardingScreen(
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.SETTINGS)
                },
                onNavigateToMain = {
                    navController.navigate(NavRoutes.MAIN) {
                        popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                    }
                },
                onStartFirstRecording = {
                    navController.navigate(NavRoutes.MAIN) {
                        popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        
        // Main screen - recording interface
        composable(NavRoutes.MAIN) {
            MainScreen(
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.SETTINGS)
                },
                onNavigateToNotes = {
                    navController.navigate(NavRoutes.NOTES)
                }
            )
        }
        
        // Settings screen - AI configuration
        composable(NavRoutes.SETTINGS) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val uiState by settingsViewModel.uiState.collectAsState()
            
            // Navigate to main screen after successful save
            LaunchedEffect(uiState.isSaved) {
                if (uiState.isSaved && uiState.validationStatus == com.voicenotesai.presentation.settings.ValidationStatus.SUCCESS) {
                    navController.navigate(NavRoutes.MAIN) {
                        popUpTo(NavRoutes.SETTINGS) { inclusive = true }
                    }
                }
            }
            
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = {
                    // Only allow back navigation if we're not in initial setup
                    if (navController.previousBackStackEntry?.destination?.route != null) {
                        navController.popBackStack()
                    }
                }
            )
        }
        
        // Notes list screen
        composable(NavRoutes.NOTES) {
            NotesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNoteClick = { noteId ->
                    navController.navigate(NavRoutes.noteDetail(noteId))
                }
            )
        }
        
        // Note detail screen
        composable(
            route = NavRoutes.NOTE_DETAIL,
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
