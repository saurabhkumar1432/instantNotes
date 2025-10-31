package com.voicenotesai.presentation.sharing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicenotesai.data.sharing.CalendarIntegrationService
import com.voicenotesai.data.sharing.ShareableLinkService
import com.voicenotesai.domain.sharing.CalendarEventDetails
import com.voicenotesai.domain.model.EnhancedNote
import com.voicenotesai.domain.sharing.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for handling sharing operations
 */
@HiltViewModel
class SharingViewModel @Inject constructor(
    private val sharingManager: SharingManager,
    private val shareableLinkService: ShareableLinkService,
    private val calendarIntegrationService: CalendarIntegrationService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SharingUiState())
    val uiState: StateFlow<SharingUiState> = _uiState.asStateFlow()
    
    /**
     * Share a note as plain text
     */
    fun shareNoteAsText(note: EnhancedNote) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            when (val result = sharingManager.shareNote(note, ShareFormat.PLAIN_TEXT)) {
                is ShareResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        lastShareResult = result
                    )
                }
                is ShareResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }
    
    /**
     * Share a note in a specific format
     */
    fun shareNoteAsFile(note: EnhancedNote, format: ShareFormat) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            when (val result = sharingManager.shareNote(note, format)) {
                is ShareResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        lastShareResult = result
                    )
                }
                is ShareResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }
    
    /**
     * Share multiple notes
     */
    fun shareMultipleNotes(notes: List<EnhancedNote>, format: ShareFormat) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            when (val result = sharingManager.shareNotes(notes, format)) {
                is ShareResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        lastShareResult = result
                    )
                }
                is ShareResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }
    
    /**
     * Share note to a specific app
     */
    fun shareToApp(note: EnhancedNote, targetApp: TargetApp, format: ShareFormat) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            when (val result = sharingManager.shareToApp(note, targetApp, format)) {
                is ShareResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        lastShareResult = result
                    )
                }
                is ShareResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }
    
    /**
     * Create a shareable link for a note
     */
    fun createShareableLink(noteId: String, options: ShareableLinkOptions) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val shareUrl = shareableLinkService.createShareableLink(
                    noteId = noteId,
                    options = options
                )
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    generatedLink = shareUrl
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to create shareable link"
                )
            }
        }
    }
    
    /**
     * Extract calendar event details from a note
     */
    fun extractCalendarEvent(note: EnhancedNote): CalendarEventDetails? {
        return if (calendarIntegrationService.isMeetingNote(note)) {
            calendarIntegrationService.extractCalendarEvent(note)
        } else {
            null
        }
    }
    
    /**
     * Create a calendar event from a note
     */
    fun createCalendarEvent(note: EnhancedNote, eventDetails: CalendarEventDetails) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            when (val result = sharingManager.createCalendarEvent(note, eventDetails)) {
                is CalendarResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        calendarIntent = result.calendarIntent
                    )
                }
                is CalendarResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }
    
    /**
     * Export notes to cloud storage
     */
    fun exportToCloudStorage(notes: List<EnhancedNote>, cloudProvider: CloudProvider, format: ShareFormat) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            when (val result = sharingManager.exportToCloudStorage(notes, cloudProvider, format)) {
                is CloudExportResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        cloudExportResult = result
                    )
                }
                is CloudExportResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Clear generated link
     */
    fun clearGeneratedLink() {
        _uiState.value = _uiState.value.copy(generatedLink = null)
    }
    
    /**
     * Clear calendar intent
     */
    fun clearCalendarIntent() {
        _uiState.value = _uiState.value.copy(calendarIntent = null)
    }
}

/**
 * UI state for sharing operations
 */
data class SharingUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastShareResult: ShareResult.Success? = null,
    val generatedLink: String? = null,
    val calendarIntent: android.content.Intent? = null,
    val cloudExportResult: CloudExportResult.Success? = null
)