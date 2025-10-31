package com.voicenotesai.domain.error

import com.voicenotesai.domain.model.AppError
import kotlinx.coroutines.flow.Flow

/**
 * Central error handling interface for the application.
 * Provides unified error handling, recovery strategies, and user guidance.
 */
interface ErrorHandler {
    
    /**
     * Handle an error and return appropriate user message and actions.
     */
    suspend fun handleError(error: AppError): ErrorHandlingResult
    
    /**
     * Determine if an error can be recovered from automatically.
     */
    fun canRecover(error: AppError): Boolean
    
    /**
     * Attempt automatic recovery from an error.
     */
    suspend fun attemptRecovery(error: AppError): RecoveryResult
    
    /**
     * Get user-friendly error message with context.
     */
    fun getUserMessage(error: AppError): UserMessage
    
    /**
     * Get available actions for an error.
     */
    fun getAvailableActions(error: AppError): List<ErrorAction>
    
    /**
     * Log error for debugging and analytics.
     */
    suspend fun logError(error: AppError, context: ErrorContext? = null)
    
    /**
     * Observe error events for UI updates.
     */
    fun observeErrors(): Flow<ErrorEvent>
    
    /**
     * Clear error state.
     */
    suspend fun clearError(errorId: String)
    
    /**
     * Get error recovery suggestions based on error history.
     */
    suspend fun getRecoverySuggestions(error: AppError): List<RecoverySuggestion>
}

/**
 * Result of error handling operation.
 */
data class ErrorHandlingResult(
    val userMessage: UserMessage,
    val actions: List<ErrorAction>,
    val shouldShowDialog: Boolean = true,
    val shouldNavigateToSettings: Boolean = false,
    val canRetry: Boolean = false,
    val recoveryAttempted: Boolean = false,
    val errorId: String
)

/**
 * Result of error recovery attempt.
 */
data class RecoveryResult(
    val success: Boolean,
    val message: String? = null,
    val newError: AppError? = null,
    val recoveryActions: List<String> = emptyList()
)

/**
 * User-friendly error message with context.
 */
data class UserMessage(
    val title: String,
    val message: String,
    val guidance: String? = null,
    val severity: ErrorSeverity = ErrorSeverity.ERROR
)

/**
 * Available actions for error handling.
 */
sealed class ErrorAction {
    data class Retry(val label: String = "Retry") : ErrorAction()
    data class NavigateToSettings(val label: String = "Open Settings") : ErrorAction()
    data class OpenPermissions(val label: String = "Grant Permission") : ErrorAction()
    data class Dismiss(val label: String = "Dismiss") : ErrorAction()
    data class ViewDetails(val label: String = "View Details") : ErrorAction()
    data class ContactSupport(val label: String = "Contact Support") : ErrorAction()
    data class Custom(val label: String, val action: String) : ErrorAction()
}

/**
 * Error severity levels.
 */
enum class ErrorSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

/**
 * Context information for error logging.
 */
data class ErrorContext(
    val screen: String? = null,
    val action: String? = null,
    val userId: String? = null,
    val deviceInfo: Map<String, String> = emptyMap(),
    val additionalData: Map<String, Any> = emptyMap()
)

/**
 * Error events for UI observation.
 */
sealed class ErrorEvent {
    data class ErrorOccurred(val error: AppError, val result: ErrorHandlingResult) : ErrorEvent()
    data class ErrorRecovered(val error: AppError, val result: RecoveryResult) : ErrorEvent()
    data class ErrorCleared(val errorId: String) : ErrorEvent()
    data class RecoveryFailed(val error: AppError, val reason: String) : ErrorEvent()
}

/**
 * Recovery suggestions based on error patterns.
 */
data class RecoverySuggestion(
    val title: String,
    val description: String,
    val action: ErrorAction,
    val priority: Int = 0
)