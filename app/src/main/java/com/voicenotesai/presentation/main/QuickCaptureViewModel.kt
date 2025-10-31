package com.voicenotesai.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicenotesai.data.notification.QuickCaptureService
import com.voicenotesai.data.shortcuts.ShortcutManager
import com.voicenotesai.data.voice.VoiceCommandService
import com.voicenotesai.domain.model.VoiceCommand
import com.voicenotesai.domain.model.VoiceCommandResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing quick capture features including voice commands,
 * persistent notifications, and shortcuts.
 */
@HiltViewModel
class QuickCaptureViewModel @Inject constructor(
    private val voiceCommandService: VoiceCommandService,
    private val shortcutManager: ShortcutManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(QuickCaptureUiState())
    val uiState: StateFlow<QuickCaptureUiState> = _uiState.asStateFlow()
    
    private val _voiceCommandEvents = MutableStateFlow<VoiceCommandEvent?>(null)
    val voiceCommandEvents: StateFlow<VoiceCommandEvent?> = _voiceCommandEvents.asStateFlow()
    
    init {
        observeVoiceCommands()
    }
    
    /**
     * Starts listening for voice commands.
     */
    fun startVoiceCommandListening() {
        if (!voiceCommandService.isVoiceRecognitionAvailable()) {
            _uiState.value = _uiState.value.copy(
                isVoiceCommandEnabled = false,
                error = "Voice recognition not available on this device"
            )
            return
        }
        
        voiceCommandService.startListening()
        _uiState.value = _uiState.value.copy(
            isVoiceCommandListening = true,
            isVoiceCommandEnabled = true,
            error = null
        )
    }
    
    /**
     * Stops listening for voice commands.
     */
    fun stopVoiceCommandListening() {
        voiceCommandService.stopListening()
        _uiState.value = _uiState.value.copy(
            isVoiceCommandListening = false
        )
    }
    
    /**
     * Toggles voice command listening on/off.
     */
    fun toggleVoiceCommandListening() {
        if (_uiState.value.isVoiceCommandListening) {
            stopVoiceCommandListening()
        } else {
            startVoiceCommandListening()
        }
    }
    
    /**
     * Processes a text command as if it were spoken.
     */
    fun processTextCommand(text: String) {
        viewModelScope.launch {
            val result = voiceCommandService.processTextCommand(text)
            handleVoiceCommandResult(result)
        }
    }
    
    /**
     * Reports shortcut usage to help Android prioritize frequently used shortcuts.
     */
    fun reportShortcutUsed(shortcutId: String) {
        shortcutManager.reportShortcutUsed(shortcutId)
    }
    
    /**
     * Gets all available voice commands.
     */
    fun getAvailableCommands(): Map<VoiceCommand, List<String>> {
        return voiceCommandService.getAvailableCommands()
    }
    
    /**
     * Clears the current voice command event.
     */
    fun clearVoiceCommandEvent() {
        _voiceCommandEvents.value = null
    }
    
    /**
     * Clears any error messages.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    private fun observeVoiceCommands() {
        viewModelScope.launch {
            voiceCommandService.recognizedCommands.collect { command ->
                _voiceCommandEvents.value = VoiceCommandEvent.CommandRecognized(command)
            }
        }
        
        viewModelScope.launch {
            voiceCommandService.commandResults.collect { result ->
                handleVoiceCommandResult(result)
            }
        }
    }
    
    private fun handleVoiceCommandResult(result: VoiceCommandResult) {
        when (result) {
            is VoiceCommandResult.Success -> {
                _uiState.value = _uiState.value.copy(error = null)
            }
            is VoiceCommandResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    error = result.message,
                    isVoiceCommandListening = false
                )
            }
            is VoiceCommandResult.NotRecognized -> {
                _voiceCommandEvents.value = VoiceCommandEvent.CommandNotRecognized
            }
            is VoiceCommandResult.NotAvailable -> {
                _uiState.value = _uiState.value.copy(
                    isVoiceCommandEnabled = false,
                    isVoiceCommandListening = false,
                    error = "Voice commands not available"
                )
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        voiceCommandService.cleanup()
    }
}

/**
 * UI state for quick capture features.
 */
data class QuickCaptureUiState(
    val isVoiceCommandEnabled: Boolean = false,
    val isVoiceCommandListening: Boolean = false,
    val isPersistentNotificationEnabled: Boolean = false,
    val error: String? = null
)

/**
 * Events related to voice command processing.
 */
sealed class VoiceCommandEvent {
    data class CommandRecognized(val command: VoiceCommand) : VoiceCommandEvent()
    object CommandNotRecognized : VoiceCommandEvent()
}