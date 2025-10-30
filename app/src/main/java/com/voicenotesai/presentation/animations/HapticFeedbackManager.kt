package com.voicenotesai.presentation.animations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay

/**
 * Advanced haptic feedback manager that provides contextual haptic responses
 * for different UI interactions and states.
 * 
 * Requirements addressed:
 * - 1.1: Immediate haptic and visual feedback with Material You dynamic theming
 * - 1.5: Enhanced micro-interactions with haptic feedback
 */
interface HapticFeedbackManager {
    /**
     * Performs haptic feedback for different interaction types
     */
    suspend fun performHaptic(type: HapticInteractionType, intensity: Float = 1.0f)
    
    /**
     * Performs a sequence of haptic feedback for complex interactions
     */
    suspend fun performHapticSequence(sequence: List<HapticSequenceItem>)
    
    /**
     * Checks if haptic feedback is available and enabled
     */
    fun isHapticAvailable(): Boolean
    
    /**
     * Sets the global haptic intensity
     */
    fun setGlobalIntensity(intensity: Float)
}

/**
 * Default implementation of haptic feedback manager
 */
@Stable
class DefaultHapticFeedbackManager(
    private val hapticFeedback: HapticFeedback,
    private val isEnabled: Boolean = true
) : HapticFeedbackManager {
    
    private var globalIntensity: Float = 1.0f
    
    override suspend fun performHaptic(type: HapticInteractionType, intensity: Float) {
        if (!isEnabled || !isHapticAvailable()) return
        
        val adjustedIntensity = intensity * globalIntensity
        if (adjustedIntensity <= 0f) return
        
        when (type) {
            HapticInteractionType.LightTap -> {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            
            HapticInteractionType.MediumTap -> {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            
            HapticInteractionType.HeavyTap -> {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                if (adjustedIntensity > 0.7f) {
                    delay(50)
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
            
            HapticInteractionType.Success -> {
                // Double tap pattern for success
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(100)
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            
            HapticInteractionType.Error -> {
                // Triple tap pattern for error
                repeat(3) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    delay(80)
                }
            }
            
            HapticInteractionType.Warning -> {
                // Long press followed by short tap
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(150)
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            
            HapticInteractionType.Selection -> {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            
            HapticInteractionType.Navigation -> {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            
            HapticInteractionType.RecordingStart -> {
                // Ascending pattern for recording start
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                delay(100)
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            
            HapticInteractionType.RecordingStop -> {
                // Descending pattern for recording stop
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(100)
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            
            HapticInteractionType.Processing -> {
                // Gentle pulse for processing
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            
            HapticInteractionType.Completion -> {
                // Satisfying completion pattern
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(80)
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(80)
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }
    
    override suspend fun performHapticSequence(sequence: List<HapticSequenceItem>) {
        if (!isEnabled || !isHapticAvailable()) return
        
        for (item in sequence) {
            performHaptic(item.type, item.intensity)
            if (item.delayAfter > 0) {
                delay(item.delayAfter)
            }
        }
    }
    
    override fun isHapticAvailable(): Boolean {
        return isEnabled
    }
    
    override fun setGlobalIntensity(intensity: Float) {
        globalIntensity = intensity.coerceIn(0f, 1f)
    }
}

/**
 * Types of haptic interactions with different feedback patterns
 */
enum class HapticInteractionType {
    LightTap,        // Subtle feedback for light interactions
    MediumTap,       // Standard feedback for normal interactions
    HeavyTap,        // Strong feedback for important interactions
    Success,         // Positive feedback pattern
    Error,           // Negative feedback pattern
    Warning,         // Cautionary feedback pattern
    Selection,       // Feedback for item selection
    Navigation,      // Feedback for navigation actions
    RecordingStart,  // Feedback for starting recording
    RecordingStop,   // Feedback for stopping recording
    Processing,      // Feedback during processing
    Completion       // Feedback for task completion
}

/**
 * Item in a haptic feedback sequence
 */
@Stable
data class HapticSequenceItem(
    val type: HapticInteractionType,
    val intensity: Float = 1.0f,
    val delayAfter: Long = 0L
)

/**
 * Predefined haptic patterns for common interactions
 */
object HapticPatterns {
    
    val ButtonPress = listOf(
        HapticSequenceItem(HapticInteractionType.MediumTap)
    )
    
    val SuccessfulAction = listOf(
        HapticSequenceItem(HapticInteractionType.Success)
    )
    
    val ErrorAction = listOf(
        HapticSequenceItem(HapticInteractionType.Error)
    )
    
    val RecordingWorkflow = listOf(
        HapticSequenceItem(HapticInteractionType.RecordingStart),
        HapticSequenceItem(HapticInteractionType.Processing, delayAfter = 2000),
        HapticSequenceItem(HapticInteractionType.Completion)
    )
    
    val NavigationTransition = listOf(
        HapticSequenceItem(HapticInteractionType.Navigation),
        HapticSequenceItem(HapticInteractionType.LightTap, delayAfter = 150)
    )
    
    val SelectionFeedback = listOf(
        HapticSequenceItem(HapticInteractionType.Selection),
        HapticSequenceItem(HapticInteractionType.LightTap, intensity = 0.5f, delayAfter = 100)
    )
    
    val ProcessingPulse = listOf(
        HapticSequenceItem(HapticInteractionType.Processing),
        HapticSequenceItem(HapticInteractionType.Processing, intensity = 0.7f, delayAfter = 500),
        HapticSequenceItem(HapticInteractionType.Processing, intensity = 0.5f, delayAfter = 500)
    )
}

/**
 * Contextual haptic feedback provider that adapts to different app states
 */
@Stable
class ContextualHapticProvider(
    private val hapticManager: HapticFeedbackManager
) {
    
    /**
     * Provides haptic feedback based on current app context
     */
    suspend fun provideContextualFeedback(
        context: AppContext,
        action: UserAction,
        intensity: Float = 1.0f
    ) {
        val hapticType = when (context) {
            AppContext.Recording -> when (action) {
                UserAction.Start -> HapticInteractionType.RecordingStart
                UserAction.Stop -> HapticInteractionType.RecordingStop
                UserAction.Pause -> HapticInteractionType.MediumTap
                UserAction.Resume -> HapticInteractionType.LightTap
                else -> HapticInteractionType.LightTap
            }
            
            AppContext.Processing -> when (action) {
                UserAction.Start -> HapticInteractionType.Processing
                UserAction.Complete -> HapticInteractionType.Completion
                UserAction.Error -> HapticInteractionType.Error
                else -> HapticInteractionType.Processing
            }
            
            AppContext.Navigation -> when (action) {
                UserAction.Navigate -> HapticInteractionType.Navigation
                UserAction.Select -> HapticInteractionType.Selection
                UserAction.Back -> HapticInteractionType.LightTap
                else -> HapticInteractionType.MediumTap
            }
            
            AppContext.Editing -> when (action) {
                UserAction.Save -> HapticInteractionType.Success
                UserAction.Delete -> HapticInteractionType.Warning
                UserAction.Select -> HapticInteractionType.Selection
                else -> HapticInteractionType.LightTap
            }
            
            AppContext.Settings -> when (action) {
                UserAction.Toggle -> HapticInteractionType.MediumTap
                UserAction.Save -> HapticInteractionType.Success
                UserAction.Reset -> HapticInteractionType.Warning
                else -> HapticInteractionType.LightTap
            }
        }
        
        hapticManager.performHaptic(hapticType, intensity)
    }
}

/**
 * App contexts for contextual haptic feedback
 */
enum class AppContext {
    Recording,
    Processing,
    Navigation,
    Editing,
    Settings
}

/**
 * User actions for contextual haptic feedback
 */
enum class UserAction {
    Start,
    Stop,
    Pause,
    Resume,
    Complete,
    Error,
    Navigate,
    Select,
    Back,
    Save,
    Delete,
    Toggle,
    Reset
}

/**
 * Composable function to create and remember a haptic feedback manager
 */
@Composable
fun rememberHapticFeedbackManager(
    isEnabled: Boolean = true
): HapticFeedbackManager {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(hapticFeedback, isEnabled) {
        DefaultHapticFeedbackManager(hapticFeedback, isEnabled)
    }
}

/**
 * Composable function to create and remember a contextual haptic provider
 */
@Composable
fun rememberContextualHapticProvider(
    hapticManager: HapticFeedbackManager = rememberHapticFeedbackManager()
): ContextualHapticProvider {
    return remember(hapticManager) {
        ContextualHapticProvider(hapticManager)
    }
}