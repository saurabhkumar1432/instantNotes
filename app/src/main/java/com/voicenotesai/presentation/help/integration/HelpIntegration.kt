package com.voicenotesai.presentation.help.integration

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.voicenotesai.presentation.help.HelpManager
import com.voicenotesai.presentation.help.HelpPreferences
import com.voicenotesai.presentation.help.HelpState
import com.voicenotesai.presentation.help.HelpTour
import com.voicenotesai.presentation.help.HelpTours
import com.voicenotesai.presentation.help.TooltipPosition
import com.voicenotesai.presentation.help.components.ContextualTooltip
import com.voicenotesai.presentation.help.components.GuidedTour
import com.voicenotesai.presentation.help.components.HelpFloatingButton
import com.voicenotesai.presentation.help.components.QuickTip
import com.voicenotesai.presentation.help.rememberHelpState
import com.voicenotesai.presentation.theme.Spacing
import kotlinx.coroutines.delay

/**
 * Wrapper component that adds help functionality to any screen.
 * Provides guided tours, contextual tooltips, and help integration.
 */
@Composable
fun HelpEnabledScreen(
    helpKey: String,
    enabledTours: List<HelpTour> = emptyList(),
    showHelpButton: Boolean = true,
    autoStartTour: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val helpState = rememberHelpState()
    var currentTour by remember { mutableStateOf<HelpTour?>(null) }
    val context = LocalContext.current
    val helpPrefs = remember { HelpPreferences.getInstance(context) }
    
    // Check if quick tip has been dismissed before using persistent storage
    var quickTipsDismissed by remember(helpKey) { 
        mutableStateOf(helpPrefs.isQuickTipDismissed(helpKey)) 
    }
    
    // Auto-start tour if specified
    LaunchedEffect(autoStartTour, helpKey) {
        if (autoStartTour && enabledTours.isNotEmpty()) {
            val tour = enabledTours.first()
            // Check if user has seen this tour before
            val hasSeenTour = helpPrefs.isTourCompleted(tour.id) || helpPrefs.isTourSkipped(tour.id)
            if (!hasSeenTour) {
                delay(500) // Small delay to let the screen settle
                currentTour = tour
            }
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Main content
        content()
        
        // Contextual tooltip overlay
        ContextualTooltip(helpState = helpState)
        
        // Guided tour overlay
        GuidedTour(
            tour = currentTour,
            onStepComplete = {
                // Tour step completed - could trigger analytics or state updates
            },
            onTourComplete = {
                currentTour?.let { helpPrefs.completeTour(it.id) }
                currentTour = null
            },
            onTourSkip = {
                currentTour?.let { helpPrefs.skipTour(it.id) }
                currentTour = null
            }
        )
        
        // Help button - only show if there are tours available and no tour is active
        if (showHelpButton && currentTour == null && enabledTours.isNotEmpty()) {
            HelpFloatingButton(
                onClick = {
                    currentTour = enabledTours.first()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.large)
                    .helpTarget("help_button")
            )
        }
        
        // Quick tips based on screen - only show if not dismissed and no tour is active
        if (!quickTipsDismissed && currentTour == null) {
            when (helpKey) {
                "main_screen" -> {
                    QuickTip(
                        title = "Pro Tip",
                        description = "Speak clearly and naturally. The AI works best with conversational speech.",
                        isVisible = true,
                        onDismiss = { 
                            quickTipsDismissed = true
                            helpPrefs.dismissQuickTip(helpKey)
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = Spacing.medium)
                    )
                }
                "notes_screen" -> {
                    QuickTip(
                        title = "Search Everything",
                        description = "Use the search bar to find notes by content or transcribed text.",
                        isVisible = true,
                        onDismiss = { 
                            quickTipsDismissed = true
                            helpPrefs.dismissQuickTip(helpKey)
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = Spacing.medium)
                    )
                }
                "settings_screen" -> {
                    QuickTip(
                        title = "Validate Settings",
                        description = "Always test your API settings before recording to ensure everything works properly.",
                        isVisible = true,
                        onDismiss = { 
                            quickTipsDismissed = true
                            helpPrefs.dismissQuickTip(helpKey)
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = Spacing.medium)
                    )
                }
            }
        }
    }
}

/**
 * Modifier extension to mark UI elements as help targets.
 * This allows the help system to identify and highlight specific components.
 */
@Composable
fun Modifier.helpTarget(
    key: String,
    helpState: HelpState? = null,
    tooltip: String? = null
): Modifier {
    var targetBounds by remember { mutableStateOf<Rect?>(null) }
    
    return this
        .onGloballyPositioned { coordinates ->
            targetBounds = coordinates.boundsInWindow()
        }
        .semantics {
            // Add semantic information for accessibility
            contentDescription = tooltip ?: "Help target: $key"
        }
        .then(
            if (helpState != null && tooltip != null) {
                Modifier.onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInWindow()
                    // Store bounds for potential tooltip display
                    targetBounds = bounds
                }
            } else {
                Modifier
            }
        )
}

/**
 * Component that automatically shows contextual help based on user interaction patterns.
 */
@Composable
fun SmartHelpProvider(
    screenKey: String,
    content: @Composable () -> Unit
) {
    val helpState = rememberHelpState()
    var userIdleTime by remember { mutableStateOf(0L) }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Monitor user idle time
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            val currentTime = System.currentTimeMillis()
            userIdleTime = currentTime - lastInteractionTime
            
            // Show contextual help after 10 seconds of inactivity
            if (userIdleTime > 10000 && !helpState.isTooltipVisible) {
                when (screenKey) {
                    "main_screen" -> {
                        helpState.showTooltip(
                            title = "Ready to record?",
                            description = "Tap the microphone button to start capturing your thoughts. The AI will enhance them automatically.",
                            position = TooltipPosition.Center
                        )
                    }
                    "notes_screen" -> {
                        helpState.showTooltip(
                            title = "Browse your notes",
                            description = "Tap any note to view its full content, or use the search bar to find specific information.",
                            position = TooltipPosition.Center
                        )
                    }
                    "settings_screen" -> {
                        helpState.showTooltip(
                            title = "Configure your AI",
                            description = "Enter your OpenAI API key and select a model to get started with AI-enhanced notes.",
                            position = TooltipPosition.Center
                        )
                    }
                }
                lastInteractionTime = currentTime // Reset timer after showing help
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                // Reset idle timer on any layout changes (user interaction)
                lastInteractionTime = System.currentTimeMillis()
            }
    ) {
        content()
        
        ContextualTooltip(helpState = helpState)
    }
}

/**
 * Provides contextual help text based on the current screen and user state.
 */
object ContextualHelpContent {
    
    fun getScreenHelp(screenKey: String): Pair<String, String> {
        return when (screenKey) {
            "main_screen" -> "Recording Interface" to 
                "This is your main recording workspace. Tap the microphone to start recording, and watch the real-time visualization as you speak. The AI will automatically enhance your spoken words into polished notes."
            
            "notes_screen" -> "Your Note Library" to 
                "All your AI-enhanced notes are stored here. Use the search bar to find specific content, tap any note to view details, or use the delete button to remove unwanted notes. You can also export notes in various formats."
            
            "settings_screen" -> "AI Configuration" to 
                "Configure your AI settings here. Enter your OpenAI API key to enable AI processing, select the model that best fits your needs, and validate your settings before recording. Your API key is stored securely on your device."
            
            "note_detail" -> "Note Details" to 
                "View the full content of your note here. This includes both the original transcription and the AI-enhanced version. You can copy the text or share it with other apps."
            
            "onboarding" -> "Welcome Setup" to 
                "Welcome to Voice Notes AI! This guided setup will help you configure the app and take your first recording. Follow the steps to get started with AI-enhanced note-taking."
            
            else -> "Help" to "This screen provides specific functionality for the Voice Notes AI app. Use the help button to learn more about available features."
        }
    }
    
    fun getFeatureHelp(featureKey: String): Pair<String, String> {
        return when (featureKey) {
            "record_button" -> "Recording" to 
                "Tap to start/stop recording. The button changes color to indicate recording status. Red means actively recording, gray means stopped."
            
            "voice_visualizer" -> "Voice Visualization" to 
                "Real-time visual feedback of your voice. The patterns and colors change based on your speech volume and tone, providing immediate feedback while recording."
            
            "pause_detection" -> "Smart Pause" to 
                "The app automatically detects when you pause speaking and temporarily stops recording to avoid capturing silence. It resumes when you start speaking again."
            
            "ai_processing" -> "AI Enhancement" to 
                "Your recorded speech is transcribed and then enhanced by AI to create clean, well-formatted notes. This process happens automatically after you stop recording."
            
            "search_notes" -> "Smart Search" to 
                "Search through all your notes by content. The search includes both the original transcription and the AI-enhanced text, making it easy to find any information."
            
            "export_notes" -> "Export Options" to 
                "Export your notes in multiple formats including plain text, markdown, and JSON. You can also share notes directly with other apps on your device."
            
            "api_settings" -> "API Configuration" to 
                "Enter your OpenAI API key to enable AI processing. Your key is stored securely on your device and never shared. You'll need a valid OpenAI account to use this feature."
            
            "model_selection" -> "AI Model Choice" to 
                "Choose between different AI models. GPT-4 provides higher quality results but costs more, while GPT-3.5 is faster and more economical. Select based on your needs and budget."
            
            else -> "Feature Help" to "This feature enhances your note-taking experience. Explore the interface to discover its capabilities."
        }
    }
}