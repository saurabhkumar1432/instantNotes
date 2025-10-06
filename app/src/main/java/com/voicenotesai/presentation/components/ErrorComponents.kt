package com.voicenotesai.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicenotesai.domain.model.AppError
import com.voicenotesai.domain.model.canRetry
import com.voicenotesai.domain.model.getActionGuidance
import com.voicenotesai.domain.model.shouldNavigateToSettings
import com.voicenotesai.domain.model.toUserMessage
import com.voicenotesai.presentation.theme.ExtendedTypography
import com.voicenotesai.presentation.theme.Spacing

/**
 * Enhanced error display component with contextual actions and better UX
 */
@Composable
fun EnhancedErrorCard(
    error: AppError,
    onRetry: (() -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.large),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                // Header with icon and dismiss button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        ErrorIcon(
                            error = error,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = getErrorTitle(error),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    
                    onDismiss?.let {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                it()
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .semantics {
                                    contentDescription = "Dismiss error message"
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                
                // Error message
                Text(
                    text = error.toUserMessage(),
                    style = ExtendedTypography.errorText,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    lineHeight = 20.sp
                )
                
                // Action guidance if available
                error.getActionGuidance()?.let { guidance ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(Spacing.medium)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = guidance,
                                style = ExtendedTypography.caption,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small, Alignment.End)
                ) {
                    when {
                        error.shouldNavigateToSettings() && onNavigateToSettings != null -> {
                            FilledTonalButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onNavigateToSettings()
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.semantics {
                                    contentDescription = "Go to settings to fix this issue"
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(Spacing.small))
                                Text(
                                    "Go to Settings",
                                    style = ExtendedTypography.buttonText
                                )
                            }
                        }
                        error.canRetry() && onRetry != null -> {
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onRetry()
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = "Retry the failed operation"
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(Spacing.small))
                                Text(
                                    "Retry",
                                    style = ExtendedTypography.buttonText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Get appropriate icon for different error types
 */
@Composable
fun ErrorIcon(
    error: AppError,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.error
) {
    val icon = when (error) {
        is AppError.NetworkError,
        is AppError.NoInternetConnection,
        is AppError.RequestTimeout -> Icons.Default.Warning
        is AppError.SettingsNotConfigured,
        is AppError.InvalidSettings,
        is AppError.InvalidAPIKey -> Icons.Default.Settings
        is AppError.RecordingFailed,
        is AppError.NoSpeechDetected,
        is AppError.RecordingTimeout,
        is AppError.SpeechRecognizerUnavailable -> Icons.Default.Warning
        else -> Icons.Default.Warning
    }
    
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = modifier
    )
}

/**
 * Get user-friendly title for different error types
 */
fun getErrorTitle(error: AppError): String {
    return when (error) {
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
        is AppError.StorageError -> "Storage Problem"
        is AppError.Unknown -> "Unexpected Error"
    }
}