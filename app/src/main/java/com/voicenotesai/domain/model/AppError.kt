package com.voicenotesai.domain.model

/**
 * Sealed class representing all possible errors in the application.
 * Each error type contains relevant information for user feedback.
 */
sealed class AppError {
    /**
     * Permission-related errors
     */
    data class PermissionDenied(val permission: String) : AppError()

    /**
     * Recording-related errors
     */
    data class RecordingFailed(val reason: String) : AppError()
    object NoSpeechDetected : AppError()
    object RecordingTimeout : AppError()
    object SpeechRecognizerUnavailable : AppError()

    /**
     * Network-related errors
     */
    data class NetworkError(val message: String) : AppError()
    object NoInternetConnection : AppError()
    object RequestTimeout : AppError()

    /**
     * API-related errors
     */
    data class ApiError(val message: String, val code: Int? = null) : AppError()
    object InvalidAPIKey : AppError()
    object RateLimitExceeded : AppError()
    object InvalidRequest : AppError()

    /**
     * Configuration-related errors
     */
    object SettingsNotConfigured : AppError()
    data class InvalidSettings(val field: String) : AppError()

    /**
     * Storage-related errors
     */
    data class StorageError(val message: String) : AppError()

    /**
     * AI processing errors
     */
    data class AIProcessingError(val message: String) : AppError()
    data class LocalModelError(val message: String, val modelId: String? = null) : AppError()
    data class ModelDownloadError(val message: String, val modelName: String? = null) : AppError()
    object LocalProcessingUnavailable : AppError()
    object ModelNotFound : AppError()

    /**
     * Data portability errors
     */
    data class ExportError(val message: String, val format: String? = null) : AppError()
    data class ImportError(val message: String, val lineNumber: Int? = null) : AppError()
    data class BackupError(val message: String) : AppError()
    data class RestoreError(val message: String) : AppError()
    data class DataIntegrityError(val message: String) : AppError()

    /**
     * Generic errors
     */
    data class Unknown(val message: String) : AppError()
}

/**
 * Extension function to convert AppError to user-friendly message.
 * Maps technical errors to actionable, understandable messages for users.
 */
fun AppError.toUserMessage(): String {
    return when (this) {
        // Permission errors
        is AppError.PermissionDenied -> 
            "Microphone permission is required to record audio. Please grant permission in settings."

        // Recording errors
        is AppError.RecordingFailed -> 
            "Recording failed: $reason. Please try again."
        is AppError.NoSpeechDetected -> 
            "No speech detected. Please speak clearly and try again."
        is AppError.RecordingTimeout -> 
            "Recording stopped automatically after 5 minutes. Please start a new recording."
        is AppError.SpeechRecognizerUnavailable -> 
            "Speech recognition is not available on this device. Please check your device settings."

        // Network errors
        is AppError.NetworkError -> 
            "Network error: $message. Please check your internet connection and try again."
        is AppError.NoInternetConnection -> 
            "No internet connection. Please check your network settings and try again."
        is AppError.RequestTimeout -> 
            "Request timed out. Please check your connection and try again."

        // API errors
        is AppError.ApiError -> {
            when (code) {
                401 -> "Invalid API key. Please check your settings and ensure your API key is correct."
                403 -> "Access forbidden. Please verify your API key has the necessary permissions."
                429 -> "Rate limit exceeded. Please wait a moment and try again."
                400 -> "Invalid request: $message. Please try again or check your settings."
                500, 502, 503 -> "AI service is temporarily unavailable. Please try again later."
                null -> "API error: $message"
                else -> "API error ($code): $message. Please try again."
            }
        }
        is AppError.InvalidAPIKey -> 
            "Invalid API key. Please check your settings and ensure your API key is correct."
        is AppError.RateLimitExceeded -> 
            "Rate limit exceeded. You've made too many requests. Please wait a moment and try again."
        is AppError.InvalidRequest -> 
            "Invalid request. Please check your settings and try again."

        // Configuration errors
        is AppError.SettingsNotConfigured -> 
            "AI settings not configured. Please go to Settings and configure your AI provider and API key."
        is AppError.InvalidSettings -> 
            "Invalid $field. Please check your settings and ensure all fields are filled correctly."

        // Storage errors
        is AppError.StorageError -> 
            "Storage error: $message. Please try again."

        // AI processing errors
        is AppError.AIProcessingError -> 
            "AI processing failed: $message. Please try again or check your settings."
        is AppError.LocalModelError -> 
            "Local model error: $message. ${modelId?.let { "Model: $it" } ?: ""}"
        is AppError.ModelDownloadError -> 
            "Model download failed: $message. ${modelName?.let { "Model: $it" } ?: ""}"
        is AppError.LocalProcessingUnavailable -> 
            "Local AI processing is not available. Please download models or check your internet connection."
        is AppError.ModelNotFound -> 
            "Required AI model not found. Please download the necessary models in Settings."

        // Data portability errors
        is AppError.ExportError -> 
            "Export failed: $message. ${format?.let { "Format: $it" } ?: ""}"
        is AppError.ImportError -> 
            "Import failed: $message. ${lineNumber?.let { "Line: $it" } ?: ""}"
        is AppError.BackupError -> 
            "Backup failed: $message. Please check available storage and try again."
        is AppError.RestoreError -> 
            "Restore failed: $message. Please verify the backup file and try again."
        is AppError.DataIntegrityError -> 
            "Data integrity check failed: $message. The file may be corrupted."

        // Unknown errors
        is AppError.Unknown -> 
            "An unexpected error occurred: $message. Please try again."
    }
}

/**
 * Extension function to get actionable guidance for the error.
 * Provides specific next steps the user can take.
 */
fun AppError.getActionGuidance(): String? {
    return when (this) {
        is AppError.PermissionDenied -> 
            "Tap 'Open Settings' to grant microphone permission."
        is AppError.SettingsNotConfigured -> 
            "Tap 'Go to Settings' to configure your AI provider."
        is AppError.InvalidAPIKey, is AppError.ApiError -> 
            "Check your API key in Settings."
        is AppError.NoInternetConnection -> 
            "Connect to Wi-Fi or mobile data and try again."
        is AppError.RateLimitExceeded -> 
            "Wait a few minutes before trying again."
        is AppError.LocalProcessingUnavailable, is AppError.ModelNotFound -> 
            "Go to Settings to download AI models for offline processing."
        is AppError.ModelDownloadError -> 
            "Check your internet connection and try downloading again."
        else -> null
    }
}

/**
 * Extension function to determine if the error should show a retry option.
 */
fun AppError.canRetry(): Boolean {
    return when (this) {
        is AppError.NetworkError,
        is AppError.NoInternetConnection,
        is AppError.RequestTimeout,
        is AppError.RecordingFailed,
        is AppError.NoSpeechDetected,
        is AppError.RateLimitExceeded,
        is AppError.ApiError,
        is AppError.ExportError,
        is AppError.ImportError,
        is AppError.BackupError,
        is AppError.RestoreError,
        is AppError.AIProcessingError,
        is AppError.LocalModelError,
        is AppError.ModelDownloadError -> true
        else -> false
    }
}

/**
 * Extension function to determine if the error should navigate to settings.
 */
fun AppError.shouldNavigateToSettings(): Boolean {
    return when (this) {
        is AppError.SettingsNotConfigured,
        is AppError.InvalidSettings,
        is AppError.InvalidAPIKey,
        is AppError.LocalProcessingUnavailable,
        is AppError.ModelNotFound -> true
        else -> false
    }
}
