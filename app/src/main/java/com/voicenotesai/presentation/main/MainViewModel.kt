package com.voicenotesai.presentation.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicenotesai.data.local.entity.Note
import com.voicenotesai.data.repository.NotesRepository
import com.voicenotesai.data.repository.RecordingState
import com.voicenotesai.data.repository.SettingsRepository
import com.voicenotesai.domain.model.AppError
import com.voicenotesai.domain.model.canRetry
import com.voicenotesai.domain.model.shouldNavigateToSettings
import com.voicenotesai.domain.model.toUserMessage
import com.voicenotesai.domain.usecase.GenerateNotesUseCase
import com.voicenotesai.domain.usecase.RecordVoiceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the main screen that handles recording, note generation, and UI state management.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val recordVoiceUseCase: RecordVoiceUseCase,
    private val generateNotesUseCase: GenerateNotesUseCase,
    private val notesRepository: NotesRepository,
    private val settingsRepository: SettingsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val KEY_TRANSCRIBED_TEXT = "transcribed_text"
        private const val KEY_RECORDING_REQUESTED = "recording_requested"
    }

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Idle)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var currentTranscribedText: String?
        get() = savedStateHandle.get<String>(KEY_TRANSCRIBED_TEXT)
        set(value) {
            savedStateHandle[KEY_TRANSCRIBED_TEXT] = value
        }
    
    private var recordingRequested: Boolean
        get() = savedStateHandle.get<Boolean>(KEY_RECORDING_REQUESTED) ?: false
        set(value) {
            savedStateHandle[KEY_RECORDING_REQUESTED] = value
        }

    init {
        checkSettings()
        // Restore state if configuration changed during processing
        currentTranscribedText?.let { text ->
            if (text.isNotBlank()) {
                generateNotes(text)
            }
        }
    }

    /**
     * Called when the ViewModel is destroyed.
     * Cleans up audio resources to prevent memory leaks.
     */
    override fun onCleared() {
        super.onCleared()
        // Clean up audio resources when ViewModel is destroyed
        recordVoiceUseCase.cleanup()
    }

    /**
     * Checks if AI settings are configured on initialization.
     */
    private fun checkSettings() {
        viewModelScope.launch {
            val hasSettings = settingsRepository.hasValidSettings()
            if (!hasSettings) {
                _uiState.value = MainUiState.Error(AppError.SettingsNotConfigured)
            }
        }
    }

    /**
     * Starts the voice recording process.
     */
    fun startRecording() {
        recordingRequested = true
        
        // Check if settings are configured
        viewModelScope.launch {
            if (!settingsRepository.hasValidSettings()) {
                _uiState.value = MainUiState.Error(AppError.SettingsNotConfigured)
                recordingRequested = false
                return@launch
            }

            // Check permission
            if (!recordVoiceUseCase.hasPermission()) {
                _uiState.value = MainUiState.PermissionRequired
                // Don't reset recordingRequested here - we'll start when permission is granted
                return@launch
            }

            // Start recording
            try {
                recordVoiceUseCase.invoke().collect { recordingState ->
                    when (recordingState) {
                        is RecordingState.Idle -> {
                            _uiState.value = MainUiState.Idle
                            recordingRequested = false
                        }
                        is RecordingState.Recording -> {
                            _uiState.value = MainUiState.Recording(recordingState.duration)
                        }
                        is RecordingState.Processing -> {
                            _uiState.value = MainUiState.Processing("Converting speech to text...")
                        }
                        is RecordingState.Success -> {
                            currentTranscribedText = recordingState.transcribedText
                            generateNotes(recordingState.transcribedText)
                            recordingRequested = false
                        }
                        is RecordingState.Error -> {
                            val error = mapRecordingError(recordingState.message)
                            _uiState.value = MainUiState.Error(error)
                            recordingRequested = false
                        }
                    }
                }
            } catch (e: Exception) {
                val error = AppError.RecordingFailed(e.message ?: "Unknown error")
                _uiState.value = MainUiState.Error(error)
                recordingRequested = false
            }
        }
    }

    /**
     * Stops the current recording.
     */
    fun stopRecording() {
        viewModelScope.launch {
            try {
                // Just trigger the stop - the Flow will handle the result
                recordVoiceUseCase.stopRecording()
            } catch (e: Exception) {
                val error = AppError.RecordingFailed(e.message ?: "Failed to stop recording")
                _uiState.value = MainUiState.Error(error)
            }
        }
    }

    /**
     * Generates AI-powered notes from transcribed text.
     */
    private fun generateNotes(transcribedText: String) {
        viewModelScope.launch {
            _uiState.value = MainUiState.Processing("Generating notes with AI...")

            val result = generateNotesUseCase(transcribedText)
            result.onSuccess { generatedNotes ->
                // Save the note automatically
                saveNote(generatedNotes, transcribedText)
            }.onFailure { error ->
                val appError = mapApiError(error)
                _uiState.value = MainUiState.Error(appError)
            }
        }
    }

    /**
     * Saves the generated note to the database.
     */
    private suspend fun saveNote(content: String, transcribedText: String) {
        try {
            val note = Note(
                content = content,
                transcribedText = transcribedText,
                timestamp = System.currentTimeMillis()
            )
            notesRepository.saveNote(note)
            _uiState.value = MainUiState.Success(content)
        } catch (e: Exception) {
            val error = AppError.StorageError(e.message ?: "Failed to save note")
            _uiState.value = MainUiState.Error(error)
        }
    }

    /**
     * Resets the UI state to idle.
     */
    fun resetToIdle() {
        _uiState.value = MainUiState.Idle
        currentTranscribedText = null
        recordingRequested = false
    }

    /**
     * Clears any error messages.
     */
    fun clearError() {
        if (_uiState.value is MainUiState.Error) {
            _uiState.value = MainUiState.Idle
            recordingRequested = false
        }
    }

    /**
     * Handles permission result.
     */
    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            // Permission granted - if user previously requested recording, start it now
            if (recordingRequested && _uiState.value is MainUiState.PermissionRequired) {
                startRecording()
            } else {
                // Just clear permission error state
                if (_uiState.value is MainUiState.PermissionRequired) {
                    _uiState.value = MainUiState.Idle
                }
            }
        } else {
            _uiState.value = MainUiState.Error(AppError.PermissionDenied("RECORD_AUDIO"))
            recordingRequested = false
        }
    }

    /**
     * Maps recording error messages to AppError types.
     */
    private fun mapRecordingError(message: String): AppError {
        return when {
            message.contains("no speech", ignoreCase = true) -> AppError.NoSpeechDetected
            message.contains("timeout", ignoreCase = true) -> AppError.RecordingTimeout
            message.contains("unavailable", ignoreCase = true) -> AppError.SpeechRecognizerUnavailable
            else -> AppError.RecordingFailed(message)
        }
    }

    /**
     * Maps API errors to AppError types.
     */
    private fun mapApiError(error: Throwable): AppError {
        val message = error.message ?: "Unknown error"
        return when {
            message.contains("not configured", ignoreCase = true) -> 
                AppError.SettingsNotConfigured
            message.contains("401") -> 
                AppError.InvalidAPIKey
            message.contains("429") -> 
                AppError.RateLimitExceeded
            message.contains("400") -> 
                AppError.InvalidRequest
            message.contains("network", ignoreCase = true) -> 
                AppError.NetworkError(message)
            message.contains("timeout", ignoreCase = true) -> 
                AppError.RequestTimeout
            message.contains("500") || message.contains("502") || message.contains("503") -> 
                AppError.APIError(500, "Service temporarily unavailable")
            else -> AppError.Unknown(message)
        }
    }
}

/**
 * UI state for the main screen.
 */
sealed class MainUiState {
    object Idle : MainUiState()
    object PermissionRequired : MainUiState()
    data class Recording(val duration: Long) : MainUiState()
    data class Processing(val message: String) : MainUiState()
    data class Success(val notes: String) : MainUiState()
    data class Error(val error: AppError) : MainUiState()
}
