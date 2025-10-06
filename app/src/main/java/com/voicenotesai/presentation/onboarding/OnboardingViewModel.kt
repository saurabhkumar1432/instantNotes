package com.voicenotesai.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.voicenotesai.data.repository.SettingsRepository
import com.voicenotesai.presentation.settings.ValidationStatus
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        // Check initial state
        viewModelScope.launch {
            settingsRepository.getSettings().collect { settings ->
                _uiState.value = _uiState.value.copy(
                    hasValidSettings = settings?.apiKey?.isNotBlank() == true &&
                                      settings.model.isNotBlank()
                )
            }
        }
    }

    fun updatePermissionStatus(hasPermission: Boolean) {
        _uiState.value = _uiState.value.copy(
            hasPermission = hasPermission,
            canProceed = true
        )
    }

    fun nextStep() {
        val currentStep = _uiState.value.currentStep
        val nextStep = OnboardingConfig.getNextStep(currentStep)
        
        // Check if we can proceed to the next step
        val canProceed = when (nextStep) {
            OnboardingStep.AIProviderSetup -> true // Always allow, but show warning if no permission
            OnboardingStep.FirstRecording -> _uiState.value.hasValidSettings
            OnboardingStep.Completed -> true
            else -> true
        }

        _uiState.value = _uiState.value.copy(
            currentStep = nextStep,
            canProceed = canProceed,
            error = if (!canProceed && nextStep == OnboardingStep.FirstRecording) {
                "Please set up your AI provider first"
            } else null
        )
    }

    fun previousStep() {
        val currentStep = _uiState.value.currentStep
        val previousStep = OnboardingConfig.getPreviousStep(currentStep)
        
        previousStep?.let {
            _uiState.value = _uiState.value.copy(
                currentStep = it,
                canProceed = true,
                error = null
            )
        }
    }

    fun skipToStep(step: OnboardingStep) {
        _uiState.value = _uiState.value.copy(
            currentStep = step,
            canProceed = true,
            error = null
        )
    }

    fun showSkipDialog() {
        _uiState.value = _uiState.value.copy(showSkipDialog = true)
    }

    fun hideSkipDialog() {
        _uiState.value = _uiState.value.copy(showSkipDialog = false)
    }

    fun skipOnboarding() {
        _uiState.value = _uiState.value.copy(
            currentStep = OnboardingStep.Completed,
            showSkipDialog = false
        )
    }

    fun setLoading(loading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = loading)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            // Mark onboarding as completed in preferences
            // This could be stored in DataStore for persistence
            _uiState.value = _uiState.value.copy(
                currentStep = OnboardingStep.Completed
            )
        }
    }

    fun restartOnboarding() {
        _uiState.value = OnboardingUiState()
    }
}