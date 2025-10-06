package com.voicenotesai.presentation.help

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.IntSize

/**
 * Manages contextual help and tooltips throughout the app.
 * Provides a centralized system for showing guided tours and interactive help.
 */
object HelpManager {
    private var currentTour: HelpTour? = null
    private var isHelpEnabled = true
    
    fun startTour(tour: HelpTour) {
        if (isHelpEnabled) {
            currentTour = tour
        }
    }
    
    fun nextStep(): Boolean {
        return currentTour?.nextStep() ?: false
    }
    
    fun skipTour() {
        currentTour = null
    }
    
    fun setHelpEnabled(enabled: Boolean) {
        isHelpEnabled = enabled
    }
    
    fun getCurrentTour(): HelpTour? = currentTour
}

/**
 * Represents a guided tour with multiple steps.
 */
data class HelpTour(
    val id: String,
    val title: String,
    val steps: List<HelpStep>,
    private var currentStepIndex: Int = 0
) {
    fun getCurrentStep(): HelpStep? = steps.getOrNull(currentStepIndex)
    
    fun nextStep(): Boolean {
        return if (currentStepIndex < steps.size - 1) {
            currentStepIndex++
            true
        } else {
            false
        }
    }
    
    fun previousStep(): Boolean {
        return if (currentStepIndex > 0) {
            currentStepIndex--
            true
        } else {
            false
        }
    }
    
    fun getProgress(): Float = if (steps.isNotEmpty()) {
        (currentStepIndex + 1).toFloat() / steps.size.toFloat()
    } else 0f
    
    fun isFirstStep(): Boolean = currentStepIndex == 0
    fun isLastStep(): Boolean = currentStepIndex == steps.size - 1
}

/**
 * Individual step in a help tour.
 */
data class HelpStep(
    val id: String,
    val title: String,
    val description: String,
    val targetKey: String? = null, // Key to identify the target UI element
    val position: TooltipPosition = TooltipPosition.Bottom,
    val action: HelpAction? = null,
    val highlightTarget: Boolean = true
)

/**
 * Position for tooltip placement relative to target.
 */
enum class TooltipPosition {
    Top, Bottom, Left, Right, Center
}

/**
 * Optional action that can be triggered during a help step.
 */
sealed class HelpAction {
    object None : HelpAction()
    data class Navigate(val route: String) : HelpAction()
    data class TriggerAnimation(val animationType: String) : HelpAction()
    data class ShowExample(val exampleType: String) : HelpAction()
}

/**
 * Composable state holder for help system.
 */
@Composable
fun rememberHelpState(): HelpState {
    return remember { HelpState() }
}

/**
 * State management for the help system.
 */
class HelpState {
    var isTooltipVisible by mutableStateOf(false)
        private set
    
    var currentTooltip by mutableStateOf<TooltipData?>(null)
        private set
    
    var targetBounds by mutableStateOf<Rect?>(null)
        private set
    
    fun showTooltip(
        title: String,
        description: String,
        position: TooltipPosition = TooltipPosition.Bottom,
        targetBounds: Rect? = null
    ) {
        this.currentTooltip = TooltipData(title, description, position)
        this.targetBounds = targetBounds
        this.isTooltipVisible = true
    }
    
    fun hideTooltip() {
        isTooltipVisible = false
        currentTooltip = null
        targetBounds = null
    }
}

/**
 * Data class for tooltip content.
 */
data class TooltipData(
    val title: String,
    val description: String,
    val position: TooltipPosition
)

/**
 * Predefined help tours for different app sections.
 */
object HelpTours {
    
    val FIRST_TIME_USER = HelpTour(
        id = "first_time_user",
        title = "Welcome to Voice Notes AI",
        steps = listOf(
            HelpStep(
                id = "welcome",
                title = "🎤 Welcome!",
                description = "Voice Notes AI transforms your voice into polished, AI-enhanced notes. Let me show you how it works!",
                position = TooltipPosition.Center
            ),
            HelpStep(
                id = "record_button",
                title = "Start Recording",
                description = "Tap this button to start recording your voice. Speak naturally - the AI will clean up and organize your thoughts.",
                targetKey = "record_button",
                position = TooltipPosition.Top,
                highlightTarget = true
            ),
            HelpStep(
                id = "ai_processing",
                title = "AI Magic ✨",
                description = "Your voice gets transcribed and enhanced with AI. The result is clean, well-formatted notes ready for use.",
                position = TooltipPosition.Center,
                action = HelpAction.ShowExample("ai_processing")
            ),
            HelpStep(
                id = "notes_section",
                title = "View Your Notes",
                description = "Access all your saved notes here. Search, organize, and export them whenever you need.",
                targetKey = "notes_button",
                position = TooltipPosition.Bottom
            ),
            HelpStep(
                id = "settings",
                title = "Customize Your Experience",
                description = "Configure AI models, adjust settings, and personalize your experience in the settings menu.",
                targetKey = "settings_button",
                position = TooltipPosition.Left
            )
        )
    )
    
    val RECORDING_FEATURES = HelpTour(
        id = "recording_features",
        title = "Recording Features",
        steps = listOf(
            HelpStep(
                id = "voice_visualization",
                title = "Voice Visualization",
                description = "Watch the real-time visualization of your voice as you speak. The colors and patterns respond to your tone and volume.",
                targetKey = "voice_visualizer",
                position = TooltipPosition.Top
            ),
            HelpStep(
                id = "pause_resume",
                title = "Pause & Resume",
                description = "Long pause detected? The app automatically pauses and resumes to capture only your actual speech.",
                targetKey = "pause_indicator",
                position = TooltipPosition.Bottom
            ),
            HelpStep(
                id = "stop_save",
                title = "Stop & Save",
                description = "When finished, tap stop to process your recording. The AI will enhance and save your notes automatically.",
                targetKey = "stop_button",
                position = TooltipPosition.Top
            )
        )
    )
    
    val NOTES_MANAGEMENT = HelpTour(
        id = "notes_management",
        title = "Managing Your Notes",
        steps = listOf(
            HelpStep(
                id = "search_notes",
                title = "Search Everything",
                description = "Search through all your notes instantly. The search includes both original content and transcribed text.",
                targetKey = "search_bar",
                position = TooltipPosition.Bottom
            ),
            HelpStep(
                id = "note_actions",
                title = "Note Actions",
                description = "Tap any note to view details, or use the delete button to remove unwanted notes.",
                targetKey = "note_item",
                position = TooltipPosition.Right
            ),
            HelpStep(
                id = "export_share",
                title = "Export & Share",
                description = "Export your notes in multiple formats or share them directly with other apps.",
                targetKey = "export_button",
                position = TooltipPosition.Top
            )
        )
    )
    
    val SETTINGS_OVERVIEW = HelpTour(
        id = "settings_overview",
        title = "Settings & Configuration",
        steps = listOf(
            HelpStep(
                id = "api_setup",
                title = "AI Configuration",
                description = "Set up your OpenAI API key to enable AI-powered note enhancement. Your key is stored securely on your device.",
                targetKey = "api_key_field",
                position = TooltipPosition.Bottom
            ),
            HelpStep(
                id = "model_selection",
                title = "Choose AI Model",
                description = "Select the AI model that best fits your needs. GPT-4 provides better quality, while GPT-3.5 is faster and more cost-effective.",
                targetKey = "model_dropdown",
                position = TooltipPosition.Top
            ),
            HelpStep(
                id = "validation",
                title = "Validate Settings",
                description = "Always validate your settings to ensure everything works properly before starting to record.",
                targetKey = "validate_button",
                position = TooltipPosition.Top
            )
        )
    )
}