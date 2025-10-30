package com.voicenotesai.presentation.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicenotesai.domain.offline.*
import com.voicenotesai.domain.usecase.OfflineRecordingUseCase
import com.voicenotesai.domain.usecase.OfflineOperationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing offline recording functionality.
 * Handles offline recording, processing, and queue management.
 */
@HiltViewModel
class OfflineRecordingViewModel @Inject constructor(
    private val offlineRecordingUseCase: OfflineRecordingUseCase,
    private val offlineOperationsUseCase: OfflineOperationsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfflineRecordingUiState())
    val uiState: StateFlow<OfflineRecordingUiState> = _uiState.asStateFlow()

    private val _recordingState = MutableStateFlow<OfflineRecordingState>(OfflineRecordingState.Idle)
    val recordingState: StateFlow<OfflineRecordingState> = _recordingState.asStateFlow()

    private val _processingState = MutableStateFlow<BatchProcessingState?>(null)
    val processingState: StateFlow<BatchProcessingState?> = _processingState.asStateFlow()

    init {
        loadPendingRecordings()
        loadQueueStatus()
        observeQueueChanges()
    }

    /**
     * Starts offline recording.
     */
    fun startOfflineRecording() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                offlineRecordingUseCase.startOfflineRecording()
                    .collect { state ->
                        _recordingState.value = state
                        
                        when (state) {
                            is OfflineRecordingState.Saved -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    currentRecording = state.recording
                                )
                                loadPendingRecordings() // Refresh the list
                            }
                            is OfflineRecordingState.Error -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = state.message
                                )
                            }
                            else -> {
                                // Handle other states as needed
                            }
                        }
                    }
                    
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to start recording: ${e.message}"
                )
            }
        }
    }

    /**
     * Stops offline recording.
     */
    fun stopOfflineRecording() {
        viewModelScope.launch {
            try {
                val result = offlineRecordingUseCase.stopOfflineRecording()
                
                when (result) {
                    is OfflineRecordingResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            currentRecording = result.recording,
                            error = null
                        )
                        loadPendingRecordings()
                    }
                    is OfflineRecordingResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = result.message
                        )
                    }
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to stop recording: ${e.message}"
                )
            }
        }
    }

    /**
     * Processes a specific offline recording.
     */
    fun processRecording(recordingId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val result = offlineRecordingUseCase.processOfflineRecording(recordingId)
                
                if (result.success) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                    loadPendingRecordings() // Refresh to show updated processing status
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Processing failed: ${result.error}"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to process recording: ${e.message}"
                )
            }
        }
    }

    /**
     * Processes all pending recordings.
     */
    fun processAllRecordings() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                offlineRecordingUseCase.processAllPendingRecordings()
                    .collect { state ->
                        _processingState.value = state
                        
                        when (state) {
                            is BatchProcessingState.Completed -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = null
                                )
                                loadPendingRecordings()
                                loadQueueStatus()
                            }
                            is BatchProcessingState.Error -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = state.message
                                )
                            }
                            else -> {
                                // Handle other processing states
                            }
                        }
                    }
                    
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to process recordings: ${e.message}"
                )
            }
        }
    }

    /**
     * Deletes an offline recording.
     */
    fun deleteRecording(recordingId: String) {
        viewModelScope.launch {
            try {
                val result = offlineRecordingUseCase.deleteOfflineRecording(recordingId)
                
                if (result.success) {
                    loadPendingRecordings()
                    loadStorageInfo()
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to delete recording: ${result.error}"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete recording: ${e.message}"
                )
            }
        }
    }

    /**
     * Converts a recording to a note.
     */
    fun convertRecordingToNote(recordingId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val result = offlineRecordingUseCase.convertRecordingToNote(recordingId)
                
                when (result) {
                    is com.voicenotesai.domain.usecase.ConversionResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = null,
                            convertedNote = ConvertedNote(
                                content = result.content,
                                transcription = result.transcription,
                                timestamp = result.timestamp,
                                duration = result.duration,
                                audioFilePath = result.audioFilePath
                            )
                        )
                    }
                    is com.voicenotesai.domain.usecase.ConversionResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to convert recording: ${e.message}"
                )
            }
        }
    }

    /**
     * Cleans up old recordings.
     */
    fun cleanupOldRecordings() {
        viewModelScope.launch {
            try {
                val result = offlineRecordingUseCase.cleanupOldRecordings()
                
                if (result.success) {
                    loadPendingRecordings()
                    loadStorageInfo()
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Cleanup failed: ${result.error}"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Cleanup failed: ${e.message}"
                )
            }
        }
    }

    /**
     * Clears the error state.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Loads pending recordings.
     */
    private fun loadPendingRecordings() {
        viewModelScope.launch {
            try {
                val recordings = offlineRecordingUseCase.getPendingRecordings()
                _uiState.value = _uiState.value.copy(pendingRecordings = recordings)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load recordings: ${e.message}"
                )
            }
        }
    }

    /**
     * Loads storage information.
     */
    private fun loadStorageInfo() {
        viewModelScope.launch {
            try {
                val storageInfo = offlineRecordingUseCase.getOfflineStorageInfo()
                _uiState.value = _uiState.value.copy(storageInfo = storageInfo)
            } catch (e: Exception) {
                // Don't show error for storage info loading failure
            }
        }
    }

    /**
     * Loads queue status.
     */
    private fun loadQueueStatus() {
        viewModelScope.launch {
            try {
                val queueStatus = offlineOperationsUseCase.getQueueStatus()
                _uiState.value = _uiState.value.copy(queueStatus = queueStatus)
            } catch (e: Exception) {
                // Don't show error for queue status loading failure
            }
        }
    }

    /**
     * Observes queue changes for real-time updates.
     */
    private fun observeQueueChanges() {
        viewModelScope.launch {
            offlineOperationsUseCase.observeQueueChanges()
                .collect { event ->
                    when (event) {
                        is QueueChangeEvent.OperationCompleted,
                        is QueueChangeEvent.OperationFailed,
                        is QueueChangeEvent.OperationAdded,
                        is QueueChangeEvent.OperationRemoved -> {
                            loadQueueStatus()
                        }
                        is QueueChangeEvent.QueueCleared -> {
                            loadQueueStatus()
                        }
                        else -> {
                            // Handle other events as needed
                        }
                    }
                }
        }
    }

    /**
     * Gets the current recording status.
     */
    fun isRecording(): Boolean {
        return offlineRecordingUseCase.isRecording()
    }
}

/**
 * UI state for offline recording screen.
 */
data class OfflineRecordingUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val pendingRecordings: List<OfflineRecording> = emptyList(),
    val currentRecording: OfflineRecording? = null,
    val storageInfo: OfflineStorageInfo? = null,
    val queueStatus: QueueStatus? = null,
    val convertedNote: ConvertedNote? = null
)

/**
 * Represents a converted note from an offline recording.
 */
data class ConvertedNote(
    val content: String,
    val transcription: String,
    val timestamp: Long,
    val duration: Long,
    val audioFilePath: String
)