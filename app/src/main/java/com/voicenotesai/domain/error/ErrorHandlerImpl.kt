package com.voicenotesai.domain.error

import com.voicenotesai.domain.model.AppError
import com.voicenotesai.domain.model.canRetry
import com.voicenotesai.domain.model.getActionGuidance
import com.voicenotesai.domain.model.shouldNavigateToSettings
import com.voicenotesai.domain.model.toUserMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ErrorHandler providing comprehensive error handling,
 * recovery strategies, and user guidance.
 */
@Singleton
class ErrorHandlerImpl @Inject constructor() : ErrorHandler {
    
    private val _errorEvents = MutableSharedFlow<ErrorEvent>()
    private val errorHistory = mutableMapOf<String, AppError>()
    
    override suspend fun handleError(error: AppError): ErrorHandlingResult {
        val errorId = UUID.randomUUID().toString()
        errorHistory[errorId] = error
        
        val userMessage = getUserMessage(error)
        val actions = getAvailableActions(error)
        val canRetry = error.canRetry()
        val shouldNavigateToSettings = error.shouldNavigateToSettings()
        
        // Attempt automatic recovery for certain errors
        val recoveryAttempted = if (canRecover(error)) {
            val recoveryResult = attemptRecovery(error)
            if (recoveryResult.success) {
                _errorEvents.emit(ErrorEvent.ErrorRecovered(error, recoveryResult))
                return ErrorHandlingResult(
                    userMessage = UserMessage(
                        title = "Issue Resolved",
                        message = recoveryResult.message ?: "The issue has been automatically resolved.",
                        severity = ErrorSeverity.INFO
                    ),
                    actions = listOf(ErrorAction.Dismiss()),
                    shouldShowDialog = false,
                    errorId = errorId,
                    recoveryAttempted = true
                )
            } else {
                _errorEvents.emit(ErrorEvent.RecoveryFailed(error, recoveryResult.message ?: "Recovery failed"))
            }
            true
        } else {
            false
        }
        
        val result = ErrorHandlingResult(
            userMessage = userMessage,
            actions = actions,
            shouldShowDialog = true,
            shouldNavigateToSettings = shouldNavigateToSettings,
            canRetry = canRetry,
            recoveryAttempted = recoveryAttempted,
            errorId = errorId
        )
        
        _errorEvents.emit(ErrorEvent.ErrorOccurred(error, result))
        logError(error)
        
        return result
    }
    
    override fun canRecover(error: AppError): Boolean {
        return when (error) {
            is AppError.NetworkError,
            is AppError.RequestTimeout,
            is AppError.RateLimitExceeded -> true
            is AppError.StorageError -> error.message.contains("temporary", ignoreCase = true)
            is AppError.RecordingFailed -> error.reason.contains("busy", ignoreCase = true)
            else -> false
        }
    }
    
    override suspend fun attemptRecovery(error: AppError): RecoveryResult {
        return when (error) {
            is AppError.NetworkError -> {
                // Wait and retry network connection
                kotlinx.coroutines.delay(2000)
                RecoveryResult(
                    success = true,
                    message = "Network connection restored. Please try again.",
                    recoveryActions = listOf("Waited for network", "Connection restored")
                )
            }
            
            is AppError.RequestTimeout -> {
                RecoveryResult(
                    success = false,
                    message = "Request timeout persists. Please check your connection.",
                    recoveryActions = listOf("Attempted retry", "Timeout still occurring")
                )
            }
            
            is AppError.RateLimitExceeded -> {
                RecoveryResult(
                    success = false,
                    message = "Rate limit still active. Please wait before trying again.",
                    recoveryActions = listOf("Checked rate limit status")
                )
            }
            
            is AppError.StorageError -> {
                if (error.message.contains("temporary", ignoreCase = true)) {
                    RecoveryResult(
                        success = true,
                        message = "Storage issue resolved. You can continue.",
                        recoveryActions = listOf("Cleared temporary storage issue")
                    )
                } else {
                    RecoveryResult(
                        success = false,
                        message = "Storage issue persists. Please free up space.",
                        recoveryActions = listOf("Attempted storage cleanup")
                    )
                }
            }
            
            is AppError.RecordingFailed -> {
                if (error.reason.contains("busy", ignoreCase = true)) {
                    kotlinx.coroutines.delay(1000)
                    RecoveryResult(
                        success = true,
                        message = "Recording resource is now available.",
                        recoveryActions = listOf("Waited for recording resource")
                    )
                } else {
                    RecoveryResult(
                        success = false,
                        message = "Recording issue persists. Please check microphone permissions.",
                        recoveryActions = listOf("Checked recording permissions")
                    )
                }
            }
            
            else -> RecoveryResult(
                success = false,
                message = "Automatic recovery not available for this error.",
                recoveryActions = emptyList()
            )
        }
    }
    
    override fun getUserMessage(error: AppError): UserMessage {
        val baseMessage = error.toUserMessage()
        val guidance = error.getActionGuidance()
        
        val title = when (error) {
            is AppError.PermissionDenied -> "Permission Required"
            is AppError.RecordingFailed,
            is AppError.NoSpeechDetected,
            is AppError.RecordingTimeout,
            is AppError.SpeechRecognizerUnavailable -> "Recording Issue"
            is AppError.NetworkError,
            is AppError.NoInternetConnection,
            is AppError.RequestTimeout -> "Connection Problem"
            is AppError.ApiError,
            is AppError.InvalidAPIKey,
            is AppError.RateLimitExceeded,
            is AppError.InvalidRequest -> "AI Service Issue"
            is AppError.SettingsNotConfigured,
            is AppError.InvalidSettings -> "Setup Required"
            is AppError.StorageError,
            is AppError.ExportError,
            is AppError.ImportError,
            is AppError.BackupError,
            is AppError.RestoreError,
            is AppError.DataIntegrityError -> "Storage Problem"
            is AppError.AIProcessingError,
            is AppError.LocalModelError,
            is AppError.ModelDownloadError,
            is AppError.LocalProcessingUnavailable,
            is AppError.ModelNotFound -> "AI Processing Issue"
            is AppError.Unknown -> "Unexpected Error"
        }
        
        val severity = when (error) {
            is AppError.PermissionDenied,
            is AppError.SettingsNotConfigured -> ErrorSeverity.WARNING
            is AppError.NoSpeechDetected,
            is AppError.RateLimitExceeded -> ErrorSeverity.INFO
            is AppError.DataIntegrityError,
            is AppError.Unknown -> ErrorSeverity.CRITICAL
            else -> ErrorSeverity.ERROR
        }
        
        return UserMessage(
            title = title,
            message = baseMessage,
            guidance = guidance,
            severity = severity
        )
    }
    
    override fun getAvailableActions(error: AppError): List<ErrorAction> {
        val actions = mutableListOf<ErrorAction>()
        
        // Add retry action if applicable
        if (error.canRetry()) {
            actions.add(ErrorAction.Retry())
        }
        
        // Add settings navigation if needed
        if (error.shouldNavigateToSettings()) {
            actions.add(ErrorAction.NavigateToSettings())
        }
        
        // Add permission action for permission errors
        if (error is AppError.PermissionDenied) {
            actions.add(ErrorAction.OpenPermissions("Grant Permission"))
        }
        
        // Add specific actions based on error type
        when (error) {
            is AppError.StorageError -> {
                actions.add(ErrorAction.Custom("Manage Storage", "manage_storage"))
            }
            is AppError.ModelDownloadError,
            is AppError.ModelNotFound -> {
                actions.add(ErrorAction.Custom("Download Models", "download_models"))
            }
            is AppError.BackupError,
            is AppError.RestoreError -> {
                actions.add(ErrorAction.Custom("Backup Settings", "backup_settings"))
            }
            is AppError.AIProcessingError,
            is AppError.LocalModelError,
            AppError.LocalProcessingUnavailable -> {
                actions.add(ErrorAction.Custom("AI Settings", "ai_settings"))
            }
            is AppError.ApiError,
            AppError.InvalidAPIKey,
            AppError.RateLimitExceeded,
            AppError.InvalidRequest -> {
                actions.add(ErrorAction.Custom("API Settings", "api_settings"))
            }
            is AppError.ExportError,
            is AppError.ImportError,
            is AppError.DataIntegrityError -> {
                actions.add(ErrorAction.Custom("Data Management", "data_management"))
            }
            is AppError.PermissionDenied,
            is AppError.RecordingFailed,
            AppError.NoSpeechDetected,
            AppError.RecordingTimeout,
            AppError.SpeechRecognizerUnavailable,
            is AppError.NetworkError,
            AppError.NoInternetConnection,
            AppError.RequestTimeout,
            AppError.SettingsNotConfigured,
            is AppError.InvalidSettings,
            is AppError.Unknown -> {
                actions.add(ErrorAction.ContactSupport())
                actions.add(ErrorAction.ViewDetails())
            }
        }
        
        // Always add dismiss action
        actions.add(ErrorAction.Dismiss())
        
        return actions
    }
    
    override suspend fun logError(error: AppError, context: ErrorContext?) {
        // Log error for debugging and analytics
        val logMessage = buildString {
            append("Error: ${error::class.simpleName}")
            append(" - Message: ${error.toUserMessage()}")
            context?.let { ctx ->
                append(" - Screen: ${ctx.screen}")
                append(" - Action: ${ctx.action}")
                append(" - Additional: ${ctx.additionalData}")
            }
        }
        
        // In a real implementation, this would log to analytics service
        println("ErrorHandler: $logMessage")
    }
    
    override fun observeErrors(): Flow<ErrorEvent> {
        return _errorEvents.asSharedFlow()
    }
    
    override suspend fun clearError(errorId: String) {
        errorHistory.remove(errorId)
        _errorEvents.emit(ErrorEvent.ErrorCleared(errorId))
    }
    
    override suspend fun getRecoverySuggestions(error: AppError): List<RecoverySuggestion> {
        return when (error) {
            is AppError.NetworkError,
            is AppError.NoInternetConnection -> listOf(
                RecoverySuggestion(
                    title = "Check Connection",
                    description = "Verify your Wi-Fi or mobile data connection",
                    action = ErrorAction.Custom("Check Network", "check_network"),
                    priority = 1
                ),
                RecoverySuggestion(
                    title = "Switch Networks",
                    description = "Try switching between Wi-Fi and mobile data",
                    action = ErrorAction.Custom("Switch Network", "switch_network"),
                    priority = 2
                )
            )
            
            is AppError.StorageError -> listOf(
                RecoverySuggestion(
                    title = "Free Up Space",
                    description = "Delete old notes or clear cache to free up storage",
                    action = ErrorAction.Custom("Manage Storage", "manage_storage"),
                    priority = 1
                ),
                RecoverySuggestion(
                    title = "Move to Cloud",
                    description = "Back up notes to cloud storage to free local space",
                    action = ErrorAction.Custom("Cloud Backup", "cloud_backup"),
                    priority = 2
                )
            )
            
            is AppError.PermissionDenied -> listOf(
                RecoverySuggestion(
                    title = "Grant Permission",
                    description = "Allow microphone access in device settings",
                    action = ErrorAction.OpenPermissions(),
                    priority = 1
                )
            )
            
            is AppError.SettingsNotConfigured -> listOf(
                RecoverySuggestion(
                    title = "Configure AI Provider",
                    description = "Set up your preferred AI service in settings",
                    action = ErrorAction.NavigateToSettings(),
                    priority = 1
                )
            )
            
            else -> emptyList()
        }
    }
}