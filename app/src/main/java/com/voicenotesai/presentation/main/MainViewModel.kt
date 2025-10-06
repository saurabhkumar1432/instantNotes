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
import com.voicenotesai.presentation.performance.OptimizedViewModel
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
) : OptimizedViewModel() {

    companion object {
        private const val KEY_TRANSCRIBED_TEXT = "transcribed_text"
        private const val KEY_RECORDING_REQUESTED = "recording_requested"
    }

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Idle)
    val uiState: StateFlow<MainUiState> = _uiState.asOptimizedStateFlow()

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
        safeLaunch(
            onError = { error ->
                _uiState.value = MainUiState.Error(AppError.SettingsNotConfigured)
            }
        ) {
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
        safeLaunch(
            onError = { error ->
                _uiState.value = MainUiState.Error(AppError.SettingsNotConfigured)
                recordingRequested = false
            }
        ) {
            if (!settingsRepository.hasValidSettings()) {
                _uiState.value = MainUiState.Error(AppError.SettingsNotConfigured)
                recordingRequested = false
                return@safeLaunch
            }

            // Check permission
            if (!recordVoiceUseCase.hasPermission()) {
                _uiState.value = MainUiState.PermissionRequired
                // Don't reset recordingRequested here - we'll start when permission is granted
                return@safeLaunch
            }

            // Start recording with comprehensive error handling
            try {
                recordVoiceUseCase.invoke().collect { recordingState ->
                    // Defensive check - ensure we're in a valid state for this transition
                    when (recordingState) {
                        is RecordingState.Idle -> {
                            _uiState.value = MainUiState.Idle
                            recordingRequested = false
                        }
                        is RecordingState.Recording -> {
                            // Validate duration is not negative
                            val safeDuration = recordingState.duration.coerceAtLeast(0)
                            _uiState.value = MainUiState.Recording(safeDuration)
                        }
                        is RecordingState.Processing -> {
                            _uiState.value = MainUiState.Processing("Converting speech to text...")
                        }
                        is RecordingState.Success -> {
                            // Validate transcribed text is not empty
                            val text = recordingState.transcribedText.trim()
                            if (text.isNotEmpty()) {
                                currentTranscribedText = text
                                generateNotes(text)
                            } else {
                                // Handle edge case: success with empty text
                                _uiState.value = MainUiState.Error(AppError.NoSpeechDetected)
                            }
                            recordingRequested = false
                        }
                        is RecordingState.Error -> {
                            val error = mapRecordingError(recordingState.message)
                            _uiState.value = MainUiState.Error(error)
                            recordingRequested = false
                        }
                    }
                }
            } catch (e: IllegalStateException) {
                // Handle state-related errors (e.g., recording already in progress)
                val error = AppError.RecordingFailed("Recording in invalid state. Please try again.")
                _uiState.value = MainUiState.Error(error)
                recordingRequested = false
            } catch (e: SecurityException) {
                // Permission was revoked during recording
                _uiState.value = MainUiState.Error(AppError.PermissionDenied("RECORD_AUDIO"))
                recordingRequested = false
            } catch (e: Exception) {
                // Catch any other unexpected errors
                val error = AppError.RecordingFailed(e.message ?: "An unexpected error occurred. Please try again.")
                _uiState.value = MainUiState.Error(error)
                recordingRequested = false
            }
        }
    }

    /**
     * Stops the current recording.
     * Handles edge cases gracefully to prevent crashes.
     */
    fun stopRecording() {
        viewModelScope.launch {
            try {
                // Check if actually recording before stopping
                if (_uiState.value is MainUiState.Recording) {
                    // Just trigger the stop - the Flow will handle the result
                    recordVoiceUseCase.stopRecording()
                } else {
                    // Not recording - ignore stop request gracefully
                    // This can happen if user taps stop multiple times quickly
                }
            } catch (e: IllegalStateException) {
                // Not currently recording - this is not an error, just ignore
                // User might have tapped stop after recording already ended
            } catch (e: Exception) {
                // Only show error if it's a real problem
                val error = AppError.RecordingFailed(e.message ?: "Failed to stop recording. Recording may have already ended.")
                _uiState.value = MainUiState.Error(error)
            }
        }
    }

    /**
     * Generates AI-powered notes from transcribed text.
     * Validates input before processing.
     */
    private fun generateNotes(transcribedText: String) {
        viewModelScope.launch {
            try {
                // Defensive validation
                val cleanedText = transcribedText.trim()
                if (cleanedText.isEmpty()) {
                    _uiState.value = MainUiState.Error(AppError.NoSpeechDetected)
                    recordingRequested = false
                    return@launch
                }
                
                _uiState.value = MainUiState.Processing("Generating notes with AI...")

                val result = generateNotesUseCase(cleanedText)
                result.onSuccess { generatedNotes ->
                    // Validate generated content
                    val trimmedNotes = generatedNotes.trim()
                    if (trimmedNotes.isEmpty()) {
                        _uiState.value = MainUiState.Error(AppError.Unknown("AI generated empty content. Please try again."))
                        recordingRequested = false
                    } else {
                        // Save the note automatically
                        saveNote(trimmedNotes, cleanedText)
                    }
                }.onFailure { error ->
                    // Check for specific error types
                    when (error) {
                        is IllegalArgumentException -> {
                            // Blank text error from use case
                            _uiState.value = MainUiState.Error(AppError.NoSpeechDetected)
                        }
                        is IllegalStateException -> {
                            // Settings not configured
                            val message = error.message ?: "Settings not configured"
                            if (message.contains("not configured", ignoreCase = true)) {
                                _uiState.value = MainUiState.Error(AppError.SettingsNotConfigured)
                            } else {
                                _uiState.value = MainUiState.Error(AppError.Unknown(message))
                            }
                        }
                        else -> {
                            // Other errors - map to appropriate AppError
                            val appError = mapApiError(error)
                            _uiState.value = MainUiState.Error(appError)
                        }
                    }
                    recordingRequested = false
                }
            } catch (e: IllegalArgumentException) {
                // Catch blank text exceptions
                _uiState.value = MainUiState.Error(AppError.NoSpeechDetected)
                recordingRequested = false
            } catch (e: IllegalStateException) {
                // Catch settings errors
                _uiState.value = MainUiState.Error(AppError.SettingsNotConfigured)
                recordingRequested = false
            } catch (e: Exception) {
                // Catch any unexpected errors in note generation
                val error = AppError.Unknown(e.message ?: "Failed to generate notes. Please try again.")
                _uiState.value = MainUiState.Error(error)
                recordingRequested = false
            }
        }
    }

    /**
     * Saves the generated note to the database.
     * Validates data before saving.
     */
    private suspend fun saveNote(content: String, transcribedText: String) {
        try {
            // Defensive validation before saving
            val trimmedContent = content.trim()
            val trimmedTranscription = transcribedText.trim()
            
            if (trimmedContent.isEmpty()) {
                _uiState.value = MainUiState.Error(AppError.Unknown("Cannot save empty note content."))
                recordingRequested = false
                return
            }
            
            if (trimmedTranscription.isEmpty()) {
                _uiState.value = MainUiState.Error(AppError.NoSpeechDetected)
                recordingRequested = false
                return
            }
            
            val note = Note(
                content = trimmedContent,
                transcribedText = trimmedTranscription,
                timestamp = System.currentTimeMillis()
            )
            notesRepository.saveNote(note)
            _uiState.value = MainUiState.Success(trimmedContent)
        } catch (e: IllegalArgumentException) {
            // Invalid note data
            val error = AppError.StorageError("Invalid note data: ${e.message}")
            _uiState.value = MainUiState.Error(error)
            recordingRequested = false
        } catch (e: Exception) {
            // Database or other errors
            val error = AppError.StorageError(e.message ?: "Failed to save note")
            _uiState.value = MainUiState.Error(error)
            recordingRequested = false
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
     * Provides graceful handling for all edge cases.
     */
    private fun mapRecordingError(message: String): AppError {
        return when {
            // No speech detected cases
            message.contains("no speech", ignoreCase = true) -> AppError.NoSpeechDetected
            message.contains("didn't catch", ignoreCase = true) -> AppError.NoSpeechDetected
            message.contains("couldn't understand", ignoreCase = true) -> AppError.NoSpeechDetected
            
            // Timeout cases
            message.contains("timeout", ignoreCase = true) -> AppError.RecordingTimeout
            message.contains("timed out", ignoreCase = true) -> AppError.RecordingTimeout
            
            // Permission issues
            message.contains("permission", ignoreCase = true) -> AppError.PermissionDenied("RECORD_AUDIO")
            
            // Network issues
            message.contains("network", ignoreCase = true) -> AppError.NetworkError(message)
            message.contains("internet", ignoreCase = true) -> AppError.NetworkError("Speech recognition requires internet connection.")
            message.contains("connection", ignoreCase = true) -> AppError.NetworkError("Please check your internet connection.")
            
            // Service availability
            message.contains("unavailable", ignoreCase = true) -> AppError.SpeechRecognizerUnavailable
            message.contains("busy", ignoreCase = true) -> AppError.RecordingFailed("Speech recognition service is busy. Please try again in a moment.")
            message.contains("server error", ignoreCase = true) -> AppError.RecordingFailed("Speech recognition service error. Please try again.")
            
            // Audio/microphone issues
            message.contains("audio", ignoreCase = true) -> AppError.RecordingFailed("Audio recording error. Please check your microphone.")
            message.contains("microphone", ignoreCase = true) -> AppError.RecordingFailed("Microphone error. Please check device settings.")
            
            // Generic fallback
            else -> AppError.RecordingFailed(message)
        }
    }

    /**
     * Maps API errors to AppError types.
     * Handles all error scenarios gracefully.
     */
    private fun mapApiError(error: Throwable): AppError {
        // Check exception type first
        when (error) {
            is IllegalArgumentException -> {
                return AppError.NoSpeechDetected
            }
            is IllegalStateException -> {
                val message = error.message ?: ""
                return when {
                    message.contains("not configured", ignoreCase = true) -> AppError.SettingsNotConfigured
                    message.contains("API key", ignoreCase = true) -> AppError.InvalidAPIKey
                    message.contains("incomplete", ignoreCase = true) -> AppError.SettingsNotConfigured
                    else -> AppError.Unknown(message)
                }
            }
        }
        
        // Check error message
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
            message.contains("500") -> 
                AppError.ApiError("Service temporarily unavailable", code = 500)
            message.contains("502") -> 
                AppError.ApiError("Service temporarily unavailable", code = 502)
            message.contains("503") -> 
                AppError.ApiError("Service temporarily unavailable", code = 503)
            message.contains("empty", ignoreCase = true) || message.contains("blank", ignoreCase = true) ->
                AppError.NoSpeechDetected
            else -> AppError.ApiError(message)
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
