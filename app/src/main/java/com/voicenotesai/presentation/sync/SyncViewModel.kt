package com.voicenotesai.presentation.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicenotesai.domain.sync.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing sync operations, status, and conflict resolution.
 */
@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncManager: SyncManager
) : ViewModel() {

    // Sync status and progress
    val syncStatus = syncManager.observeSyncStatus()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SyncStatus.IDLE
        )

    val syncProgress = syncManager.observeSyncProgress()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SyncProgress(0, 0, 0, 0, null, 0f)
        )

    // Conflicts that need resolution
    private val _conflicts = MutableStateFlow<List<SyncConflict>>(emptyList())
    val conflicts = _conflicts.asStateFlow()

    private val _currentConflict = MutableStateFlow<SyncConflict?>(null)
    val currentConflict = _currentConflict.asStateFlow()

    // Sync operations
    private val _pendingOperations = MutableStateFlow<List<SyncOperation>>(emptyList())
    val pendingOperations = _pendingOperations.asStateFlow()

    // Sync metrics
    private val _syncMetrics = MutableStateFlow<SyncMetrics?>(null)
    val syncMetrics = _syncMetrics.asStateFlow()

    // UI state
    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Observe conflicts from sync manager
        viewModelScope.launch {
            syncManager.observeConflicts().collect { conflict ->
                val currentConflicts = _conflicts.value.toMutableList()
                currentConflicts.add(conflict)
                _conflicts.value = currentConflicts
                
                // Show the first unresolved conflict
                if (_currentConflict.value == null) {
                    _currentConflict.value = conflict
                }
            }
        }

        // Load initial data
        loadInitialData()
    }

    /**
     * Starts synchronization of all pending changes.
     */
    fun startSync() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val result = syncManager.startSync()
                
                if (!result.success) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.error ?: "Sync failed"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    refreshData()
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to start sync: ${e.message}"
                )
            }
        }
    }

    /**
     * Syncs a specific item.
     */
    fun syncItem(itemId: String, itemType: SyncItemType) {
        viewModelScope.launch {
            try {
                val result = syncManager.syncItem(itemId, itemType)
                
                if (!result.success) {
                    _uiState.value = _uiState.value.copy(
                        error = result.error ?: "Failed to sync item"
                    )
                }
                
                refreshData()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to sync item: ${e.message}"
                )
            }
        }
    }

    /**
     * Cancels ongoing sync operation.
     */
    fun cancelSync() {
        viewModelScope.launch {
            try {
                val result = syncManager.cancelSync()
                
                if (!result.success) {
                    _uiState.value = _uiState.value.copy(
                        error = result.error ?: "Failed to cancel sync"
                    )
                }
                
                refreshData()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to cancel sync: ${e.message}"
                )
            }
        }
    }

    /**
     * Retries failed sync operations.
     */
    fun retryFailedOperations() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val result = syncManager.retryFailedOperations()
                
                if (!result.success) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.error ?: "Failed to retry operations"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    refreshData()
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to retry operations: ${e.message}"
                )
            }
        }
    }

    /**
     * Resolves a sync conflict.
     */
    fun resolveConflict(conflictId: String, resolution: ConflictResolution) {
        viewModelScope.launch {
            try {
                val result = syncManager.resolveConflict(conflictId, resolution)
                
                if (result.success) {
                    // Remove resolved conflict from list
                    val updatedConflicts = _conflicts.value.filter { it.id != conflictId }
                    _conflicts.value = updatedConflicts
                    
                    // Show next conflict if available
                    _currentConflict.value = updatedConflicts.firstOrNull()
                    
                    refreshData()
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = result.error ?: "Failed to resolve conflict"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to resolve conflict: ${e.message}"
                )
            }
        }
    }

    /**
     * Dismisses the current conflict dialog.
     */
    fun dismissCurrentConflict() {
        _currentConflict.value = null
    }

    /**
     * Shows the next conflict in the queue.
     */
    fun showNextConflict() {
        val conflicts = _conflicts.value
        val currentIndex = conflicts.indexOfFirst { it.id == _currentConflict.value?.id }
        
        if (currentIndex >= 0 && currentIndex < conflicts.size - 1) {
            _currentConflict.value = conflicts[currentIndex + 1]
        } else if (conflicts.isNotEmpty()) {
            _currentConflict.value = conflicts.first()
        }
    }

    /**
     * Configures sync settings.
     */
    fun configureSyncSettings(settings: SyncSettings) {
        viewModelScope.launch {
            try {
                val result = syncManager.configureSyncSettings(settings)
                
                if (!result.success) {
                    _uiState.value = _uiState.value.copy(
                        error = result.error ?: "Failed to configure sync settings"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to configure sync settings: ${e.message}"
                )
            }
        }
    }

    /**
     * Forces a full sync of all data.
     */
    fun forceFullSync() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val result = syncManager.forceFullSync()
                
                if (!result.success) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.error ?: "Full sync failed"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    refreshData()
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to perform full sync: ${e.message}"
                )
            }
        }
    }

    /**
     * Clears any error messages.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Refreshes sync data.
     */
    fun refreshData() {
        viewModelScope.launch {
            try {
                // Load pending operations
                val pendingOps = syncManager.getPendingSyncOperations()
                _pendingOperations.value = pendingOps
                
                // Load unresolved conflicts
                val unresolvedConflicts = syncManager.getUnresolvedConflicts()
                _conflicts.value = unresolvedConflicts
                
                // Update current conflict if needed
                if (_currentConflict.value == null && unresolvedConflicts.isNotEmpty()) {
                    _currentConflict.value = unresolvedConflicts.first()
                }
                
                // Load sync metrics
                val metrics = syncManager.getSyncMetrics()
                _syncMetrics.value = metrics
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to refresh data: ${e.message}"
                )
            }
        }
    }

    /**
     * Loads initial data when ViewModel is created.
     */
    private fun loadInitialData() {
        refreshData()
    }

    /**
     * Gets sync status summary for display.
     */
    fun getSyncStatusSummary(): SyncStatusSummary {
        val status = syncStatus.value
        val progress = syncProgress.value
        val conflictsCount = conflicts.value.size
        val pendingCount = pendingOperations.value.size

        return SyncStatusSummary(
            status = status,
            progress = progress,
            conflictsCount = conflictsCount,
            pendingOperationsCount = pendingCount,
            hasIssues = status == SyncStatus.FAILED || 
                       status == SyncStatus.CONFLICTS_PENDING || 
                       conflictsCount > 0
        )
    }
}

/**
 * UI state for sync operations.
 */
data class SyncUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val showConflictDialog: Boolean = false,
    val showSyncDetails: Boolean = false
)

/**
 * Summary of sync status for UI display.
 */
data class SyncStatusSummary(
    val status: SyncStatus,
    val progress: SyncProgress,
    val conflictsCount: Int,
    val pendingOperationsCount: Int,
    val hasIssues: Boolean
)