package com.voicenotesai.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicenotesai.domain.storage.StorageManagementSettings
import com.voicenotesai.domain.storage.StorageMetrics
import com.voicenotesai.domain.storage.StorageAnalysis
import com.voicenotesai.domain.storage.OptimizationResult
import com.voicenotesai.domain.storage.CleanupFrequency
import com.voicenotesai.domain.storage.CompressionLevel
import com.voicenotesai.domain.usecase.StorageManagementUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * ViewModel for managing storage settings and operations.
 */
@HiltViewModel
class StorageSettingsViewModel @Inject constructor(
    private val storageManagementUseCase: StorageManagementUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageSettingsUiState())
    val uiState: StateFlow<StorageSettingsUiState> = _uiState.asStateFlow()

    private val _storageMetrics = MutableStateFlow<StorageMetrics?>(null)
    val storageMetrics: StateFlow<StorageMetrics?> = _storageMetrics.asStateFlow()

    private val _storageAnalysis = MutableStateFlow<StorageAnalysis?>(null)
    val storageAnalysis: StateFlow<StorageAnalysis?> = _storageAnalysis.asStateFlow()

    init {
        loadStorageSettings()
        observeStorageMetrics()
        analyzeStorage()
    }

    private fun loadStorageSettings() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val settings = storageManagementUseCase.getStorageSettings()
                _uiState.value = _uiState.value.copy(
                    settings = settings,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load storage settings: ${e.message}"
                )
            }
        }
    }

    private fun observeStorageMetrics() {
        viewModelScope.launch {
            storageManagementUseCase.getStorageMetrics()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to load storage metrics: ${e.message}"
                    )
                }
                .collect { metrics ->
                    _storageMetrics.value = metrics
                }
        }
    }

    private fun analyzeStorage() {
        viewModelScope.launch {
            try {
                val analysis = storageManagementUseCase.analyzeStorage()
                _storageAnalysis.value = analysis
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to analyze storage: ${e.message}"
                )
            }
        }
    }

    fun updateAutomaticCleanup(enabled: Boolean) {
        updateSettings { it.copy(enableAutomaticCleanup = enabled) }
    }

    fun updateCleanupFrequency(frequency: CleanupFrequency) {
        updateSettings { it.copy(cleanupFrequency = frequency) }
    }

    fun updateMaxCacheSize(sizeInMB: Int) {
        val sizeInBytes = sizeInMB * 1024L * 1024L
        updateSettings { it.copy(maxCacheSize = sizeInBytes) }
    }

    fun updateMaxAudioRetention(days: Int) {
        val duration = days.days
        updateSettings { it.copy(maxAudioRetention = duration) }
    }

    fun updateArchiveOldNotes(enabled: Boolean) {
        updateSettings { it.copy(archiveOldNotes = enabled) }
    }

    fun updateArchiveThreshold(days: Int) {
        val duration = days.days
        updateSettings { it.copy(archiveThreshold = duration) }
    }

    fun updateLowStorageMode(enabled: Boolean) {
        updateSettings { it.copy(enableLowStorageMode = enabled) }
    }

    fun updateLowStorageThreshold(percentage: Float) {
        updateSettings { it.copy(lowStorageThreshold = percentage / 100f) }
    }

    fun updateBatteryOptimization(enabled: Boolean) {
        updateSettings { it.copy(enableBatteryOptimization = enabled) }
    }

    fun updateThermalOptimization(enabled: Boolean) {
        updateSettings { it.copy(enableThermalOptimization = enabled) }
    }

    fun updateCompressionLevel(level: CompressionLevel) {
        updateSettings { it.copy(compressionLevel = level) }
    }

    fun updateDeleteEmptyFolders(enabled: Boolean) {
        updateSettings { it.copy(deleteEmptyFolders = enabled) }
    }

    fun updateOptimizeDatabaseOnStartup(enabled: Boolean) {
        updateSettings { it.copy(optimizeDatabaseOnStartup = enabled) }
    }

    fun updateMaxTempFileAge(days: Int) {
        val duration = days.days
        updateSettings { it.copy(maxTempFileAge = duration) }
    }

    private fun updateSettings(update: (StorageManagementSettings) -> StorageManagementSettings) {
        viewModelScope.launch {
            try {
                val currentSettings = _uiState.value.settings
                val newSettings = update(currentSettings)
                
                storageManagementUseCase.updateStorageSettings(newSettings)
                _uiState.value = _uiState.value.copy(settings = newSettings)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update settings: ${e.message}"
                )
            }
        }
    }

    fun optimizeStorage() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isOptimizing = true)
                val result = storageManagementUseCase.optimizeStorage()
                _uiState.value = _uiState.value.copy(
                    isOptimizing = false,
                    lastOptimizationResult = result
                )
                // Refresh analysis after optimization
                analyzeStorage()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isOptimizing = false,
                    error = "Storage optimization failed: ${e.message}"
                )
            }
        }
    }

    fun performAutomaticCleanup() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isOptimizing = true)
                val result = storageManagementUseCase.performAutomaticCleanup()
                _uiState.value = _uiState.value.copy(
                    isOptimizing = false,
                    lastAutomaticCleanupResult = result
                )
                // Refresh analysis after cleanup
                analyzeStorage()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isOptimizing = false,
                    error = "Automatic cleanup failed: ${e.message}"
                )
            }
        }
    }

    fun cleanupTempFiles() {
        viewModelScope.launch {
            try {
                storageManagementUseCase.cleanupTempFiles()
                analyzeStorage() // Refresh analysis
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Temp file cleanup failed: ${e.message}"
                )
            }
        }
    }

    fun archiveOldNotes(olderThanDays: Int) {
        viewModelScope.launch {
            try {
                val duration = olderThanDays.days
                storageManagementUseCase.archiveOldNotes(duration)
                analyzeStorage() // Refresh analysis
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Archive operation failed: ${e.message}"
                )
            }
        }
    }

    fun compactDatabase() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isOptimizing = true)
                storageManagementUseCase.compactDatabase()
                _uiState.value = _uiState.value.copy(isOptimizing = false)
                analyzeStorage() // Refresh analysis
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isOptimizing = false,
                    error = "Database compaction failed: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearOptimizationResult() {
        _uiState.value = _uiState.value.copy(lastOptimizationResult = null)
    }

    fun clearAutomaticCleanupResult() {
        _uiState.value = _uiState.value.copy(lastAutomaticCleanupResult = null)
    }
}

/**
 * UI state for storage settings screen.
 */
data class StorageSettingsUiState(
    val settings: StorageManagementSettings = StorageManagementSettings(),
    val isLoading: Boolean = false,
    val isOptimizing: Boolean = false,
    val error: String? = null,
    val lastOptimizationResult: OptimizationResult? = null,
    val lastAutomaticCleanupResult: com.voicenotesai.domain.storage.AutoCleanupResult? = null
)