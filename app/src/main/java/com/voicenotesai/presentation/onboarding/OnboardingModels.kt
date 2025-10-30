package com.voicenotesai.presentation.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.StringRes

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
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector? = null,
    @StringRes val primaryButtonTextRes: Int,
    @StringRes val secondaryButtonTextRes: Int? = null,
    val showProgress: Boolean = true
)

/**
 * Onboarding flow configuration
 */
object OnboardingConfig {
    val pages = listOf(
        OnboardingPage(
            step = OnboardingStep.Welcome,
            titleRes = com.voicenotesai.R.string.onboarding_welcome_title,
            subtitleRes = com.voicenotesai.R.string.onboarding_welcome_subtitle,
            descriptionRes = com.voicenotesai.R.string.onboarding_welcome_desc,
            primaryButtonTextRes = com.voicenotesai.R.string.onboarding_get_started,
            secondaryButtonTextRes = null,
            showProgress = false
        ),
        OnboardingPage(
            step = OnboardingStep.PermissionEducation,
            titleRes = com.voicenotesai.R.string.onboarding_permission_title,
            subtitleRes = com.voicenotesai.R.string.onboarding_permission_subtitle,
            descriptionRes = com.voicenotesai.R.string.onboarding_permission_desc,
            icon = Icons.Default.Warning,
            primaryButtonTextRes = com.voicenotesai.R.string.onboarding_allow_microphone,
            secondaryButtonTextRes = com.voicenotesai.R.string.not_now
        ),
        OnboardingPage(
            step = OnboardingStep.AIProviderSetup,
            titleRes = com.voicenotesai.R.string.onboarding_provider_title,
            subtitleRes = com.voicenotesai.R.string.onboarding_provider_subtitle,
            descriptionRes = com.voicenotesai.R.string.onboarding_provider_desc,
            icon = Icons.Default.Settings,
            primaryButtonTextRes = com.voicenotesai.R.string.onboarding_configure_provider,
            secondaryButtonTextRes = com.voicenotesai.R.string.onboarding_provider_later
        ),
        OnboardingPage(
            step = OnboardingStep.FirstRecording,
            titleRes = com.voicenotesai.R.string.onboarding_first_title,
            subtitleRes = com.voicenotesai.R.string.onboarding_first_subtitle,
            descriptionRes = com.voicenotesai.R.string.onboarding_first_desc,
            primaryButtonTextRes = com.voicenotesai.R.string.onboarding_record_sample,
            secondaryButtonTextRes = com.voicenotesai.R.string.onboarding_skip_demo
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