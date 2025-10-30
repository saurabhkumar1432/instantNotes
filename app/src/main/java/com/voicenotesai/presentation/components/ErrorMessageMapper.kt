package com.voicenotesai.presentation.components

import androidx.annotation.StringRes
import com.voicenotesai.R
import com.voicenotesai.domain.model.AppError

data class LocalizedMessage(
    @StringRes val resId: Int,
    val args: Array<Any> = emptyArray()
)

/**
 * Map domain AppError to a localized string resource and optional format args.
 * Keep localization concerns in the presentation layer so the domain remains testable.
 */
fun AppError.toLocalizedMessage(): LocalizedMessage {
    return when (this) {
    is AppError.PermissionDenied -> LocalizedMessage(R.string.error_microphone_permission_required)
        is AppError.RecordingFailed -> LocalizedMessage(R.string.error_recording_failed_format, arrayOf(this.reason))
        is AppError.NoSpeechDetected -> LocalizedMessage(R.string.error_no_speech)
        is AppError.RecordingTimeout -> LocalizedMessage(R.string.error_recording_timeout)
        is AppError.SpeechRecognizerUnavailable -> LocalizedMessage(R.string.error_speech_unavailable)
        is AppError.NetworkError -> LocalizedMessage(R.string.error_network_error_format, arrayOf(this.message))
        is AppError.NoInternetConnection -> LocalizedMessage(R.string.error_no_internet)
        is AppError.RequestTimeout -> LocalizedMessage(R.string.error_request_timeout)
        is AppError.ApiError -> {
            when (this.code) {
                401 -> LocalizedMessage(R.string.error_api_invalid_key)
                403 -> LocalizedMessage(R.string.error_api_forbidden)
                429 -> LocalizedMessage(R.string.error_api_rate_limit)
                400 -> LocalizedMessage(R.string.error_api_invalid_request_format, arrayOf(this.message))
                500, 502, 503 -> LocalizedMessage(R.string.error_api_service_unavailable)
                null -> LocalizedMessage(R.string.error_api_generic_format, arrayOf(this.message))
                else -> LocalizedMessage(R.string.error_api_generic_format, arrayOf("${this.code}: ${this.message}"))
            }
        }
        is AppError.InvalidAPIKey -> LocalizedMessage(R.string.error_api_invalid_key)
        is AppError.RateLimitExceeded -> LocalizedMessage(R.string.error_api_rate_limit)
        is AppError.InvalidRequest -> LocalizedMessage(R.string.error_invalid_request)
        is AppError.SettingsNotConfigured -> LocalizedMessage(R.string.error_settings_not_configured)
        is AppError.InvalidSettings -> LocalizedMessage(R.string.error_invalid_settings_format, arrayOf(this.field))
        is AppError.StorageError -> LocalizedMessage(R.string.error_storage_format, arrayOf(this.message))
        is AppError.Unknown -> LocalizedMessage(R.string.error_unknown_format, arrayOf(this.message))
    }
}
