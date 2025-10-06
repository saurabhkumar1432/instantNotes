package com.voicenotesai.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import com.voicenotesai.presentation.navigation.NavGraph
import com.voicenotesai.presentation.theme.NeonBackdrop
import com.voicenotesai.presentation.theme.VoiceNotesAITheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity that hosts the navigation graph and applies the app theme.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VoiceNotesAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    NeonBackdrop {
                        VoiceNotesApp()
                    }
                }
            }
        }
    }
}

/**
 * Main app composable that sets up navigation.
 */
@Composable
private fun VoiceNotesApp() {
    val navController = rememberNavController()
    
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
