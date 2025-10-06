package com.voicenotesai.presentation.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents different steps in the onboarding flow
 */
sealed class OnboardingStep {
    object Welcome : OnboardingStep()
    object PermissionEducation : OnboardingStep()
    object AIProviderSetup : OnboardingStep()
    object FirstRecording : OnboardingStep()
    object Completed : OnboardingStep()
}

/**
 * Data class representing an onboarding page
 */
data class OnboardingPage(
    val step: OnboardingStep,
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector? = null,
    val primaryButtonText: String,
    val secondaryButtonText: String? = null,
    val showProgress: Boolean = true
)

/**
 * Onboarding flow configuration
 */
object OnboardingConfig {
    val pages = listOf(
        OnboardingPage(
            step = OnboardingStep.Welcome,
            title = "Welcome to Instant Notes",
            subtitle = "Capture clear notes from any conversation",
            description = "Record once and receive structured summaries, action items, and highlights in seconds. Instant Notes keeps you focused on the discussion, not on typing.",
            primaryButtonText = "Get Started",
            secondaryButtonText = null,
            showProgress = false
        ),
        OnboardingPage(
            step = OnboardingStep.PermissionEducation,
            title = "Enable Voice Capture",
            subtitle = "Microphone access keeps every idea within reach",
            description = "Audio stays on your device while we transcribe locally. Only the generated text is sent to your connected AI provider—never the original recording.",
            icon = Icons.Default.Warning,
            primaryButtonText = "Allow Microphone Access",
            secondaryButtonText = "Not Now"
        ),
        OnboardingPage(
            step = OnboardingStep.AIProviderSetup,
            title = "Connect Your AI Provider",
            subtitle = "Choose the model that matches your workflow",
            description = "Instant Notes supports OpenAI (GPT), Anthropic (Claude), and Google AI (Gemini). Add your API key to unlock high-quality, structured results.",
            icon = Icons.Default.Settings,
            primaryButtonText = "Configure Provider",
            secondaryButtonText = "I’ll Do This Later"
        ),
        OnboardingPage(
            step = OnboardingStep.FirstRecording,
            title = "Try Your First Recording",
            subtitle = "Run a quick capture to confirm everything is ready",
            description = "Record a short update—project notes, next steps, or even a reminder. We will format it into clean bullet points and summaries instantly.",
            primaryButtonText = "Record a Sample",
            secondaryButtonText = "Skip Demo"
        )
    )
    
    fun getPageForStep(step: OnboardingStep): OnboardingPage? {
        return pages.find { it.step == step }
    }
    
    fun getNextStep(currentStep: OnboardingStep): OnboardingStep {
        return when (currentStep) {
            OnboardingStep.Welcome -> OnboardingStep.PermissionEducation
            OnboardingStep.PermissionEducation -> OnboardingStep.AIProviderSetup
            OnboardingStep.AIProviderSetup -> OnboardingStep.FirstRecording
            OnboardingStep.FirstRecording -> OnboardingStep.Completed
            OnboardingStep.Completed -> OnboardingStep.Completed
        }
    }
    
    fun getPreviousStep(currentStep: OnboardingStep): OnboardingStep? {
        return when (currentStep) {
            OnboardingStep.Welcome -> null
            OnboardingStep.PermissionEducation -> OnboardingStep.Welcome
            OnboardingStep.AIProviderSetup -> OnboardingStep.PermissionEducation
            OnboardingStep.FirstRecording -> OnboardingStep.AIProviderSetup
            OnboardingStep.Completed -> OnboardingStep.FirstRecording
        }
    }
    
    fun getStepIndex(step: OnboardingStep): Int {
        return when (step) {
            OnboardingStep.Welcome -> 0
            OnboardingStep.PermissionEducation -> 1
            OnboardingStep.AIProviderSetup -> 2
            OnboardingStep.FirstRecording -> 3
            OnboardingStep.Completed -> 4
        }
    }
    
    val totalSteps = 4 // Excluding completed state
}

/**
 * UI state for onboarding flow
 */
data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.Welcome,
    val isLoading: Boolean = false,
    val canProceed: Boolean = true,
    val hasPermission: Boolean = false,
    val hasValidSettings: Boolean = false,
    val showSkipDialog: Boolean = false,
    val error: String? = null
)