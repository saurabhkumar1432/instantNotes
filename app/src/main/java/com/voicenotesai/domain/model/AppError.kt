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
    is AppError.ApiError -> true
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
        is AppError.InvalidAPIKey -> true
        else -> false
    }
}
