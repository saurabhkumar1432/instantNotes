package com.voicenotesai.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.voicenotesai.data.notification.QuickCaptureService
import com.voicenotesai.data.shortcuts.ShortcutManager
import com.voicenotesai.presentation.navigation.NavGraph
import com.voicenotesai.presentation.theme.NeonBackdrop
import com.voicenotesai.presentation.theme.VoiceNotesAITheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main activity that hosts the navigation graph and applies the app theme.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var shortcutManager: ShortcutManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create app shortcuts
        shortcutManager.createShortcuts()
        
        setContent {
            VoiceNotesAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    NeonBackdrop {
                        VoiceNotesApp(intent = intent)
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

/**
 * Main app composable that sets up navigation.
 */
@Composable
private fun VoiceNotesApp(intent: Intent?) {
    val navController = rememberNavController()
    
    // Handle quick capture intents
    LaunchedEffect(intent) {
        handleQuickCaptureIntent(intent, navController)
    }
    
    // Handle system back button
    BackHandler {
        if (!navController.popBackStack()) {
            // If we can't pop back stack, we're at the root, so finish the activity
            val activity = navController.context as? ComponentActivity
            activity?.finish() ?: run {
                // Fallback: If cast fails, try to find activity from context
                var context = navController.context
                while (context is android.content.ContextWrapper) {
                    if (context is ComponentActivity) {
                        context.finish()
                        return@BackHandler
                    }
                    context = context.baseContext
                }
            }
        }
    }
    
    NavGraph(navController = navController)
}

/**
 * Handles quick capture intents from shortcuts, widgets, and notifications.
 */
private fun handleQuickCaptureIntent(intent: Intent?, navController: NavHostController) {
    when (intent?.action) {
        QuickCaptureService.ACTION_QUICK_RECORD -> {
            // Navigate to recording screen
            navController.navigate("recording") {
                popUpTo("home") { inclusive = false }
            }
        }
        QuickCaptureService.ACTION_VIEW_RECENT_NOTES -> {
            // Navigate to notes screen
            navController.navigate("notes") {
                popUpTo("home") { inclusive = false }
            }
        }
        QuickCaptureService.ACTION_VIEW_TASKS -> {
            // Navigate to tasks screen
            navController.navigate("tasks") {
                popUpTo("home") { inclusive = false }
            }
        }
    }
}
