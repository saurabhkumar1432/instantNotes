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
            title = "Welcome to Instant Notes! 🎉",
            subtitle = "Turn your thoughts into AI-powered notes instantly",
            description = "Drop a voice memo, get structured bullet points and summaries. No typing, no stress—just pure productivity vibes.",
            primaryButtonText = "Let's Get Started",
            secondaryButtonText = null,
            showProgress = false
        ),
        OnboardingPage(
            step = OnboardingStep.PermissionEducation,
            title = "Let's Set Up Voice Recording 🎤",
            subtitle = "We need microphone access to capture your brilliant ideas",
            description = "Your audio stays on your device and gets converted to text locally. Only the transcribed text reaches your AI provider—never the raw audio.",
            icon = Icons.Default.Warning,
            primaryButtonText = "Grant Microphone Access",
            secondaryButtonText = "Skip for Now"
        ),
        OnboardingPage(
            step = OnboardingStep.AIProviderSetup,
            title = "Choose Your AI Brain 🧠",
            subtitle = "Connect your preferred AI service for note generation",
            description = "Pick from OpenAI (GPT), Anthropic (Claude), or Google AI (Gemini). You'll need an API key from your chosen provider.",
            icon = Icons.Default.Settings,
            primaryButtonText = "Set Up AI Provider",
            secondaryButtonText = "I'll Do This Later"
        ),
        OnboardingPage(
            step = OnboardingStep.FirstRecording,
            title = "Ready to Drop Your First Note? 🚀",
            subtitle = "Let's test everything with a quick recording",
            description = "Try recording a short voice memo about anything—your weekend plans, a project idea, or just say hello. Watch the AI magic happen!",
            primaryButtonText = "Record My First Note",
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