package com.voicenotesai.presentation.accessibility

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * Accessibility utilities for enhanced user experience
 */
object AccessibilityUtils {
    
    /**
     * Standard minimum touch target size as per accessibility guidelines
     */
    val MinTouchTargetSize = 48.dp
    
    /**
     * Enhanced touch target size for primary actions
     */
    val EnhancedTouchTargetSize = 56.dp
}

/**
 * Enhanced clickable modifier with haptic feedback and accessibility support
 */
@Composable
fun Modifier.accessibleClickable(
    contentDescription: String,
    role: Role = Role.Button,
    stateDescription: String? = null,
    enabled: Boolean = true,
    hapticFeedback: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    
    return this
        .size(AccessibilityUtils.MinTouchTargetSize)
        .clip(CircleShape)
        .semantics {
            this.contentDescription = contentDescription
            this.role = role
            stateDescription?.let { this.stateDescription = it }
        }
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = rememberRipple(
                bounded = false,
                radius = AccessibilityUtils.MinTouchTargetSize / 2,
                color = MaterialTheme.colorScheme.primary
            ),
            onClick = {
                if (hapticFeedback) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onClick()
            }
        )
}

/**
 * Recording button specific accessibility with state announcements
 */
@Composable
fun Modifier.recordingAccessibility(
    isRecording: Boolean,
    duration: Long = 0L
): Modifier {
    val contentDesc = if (isRecording) {
        stringResource(id = com.voicenotesai.R.string.recording_stop_desc, formatDurationForAccessibility(duration))
    } else {
        stringResource(id = com.voicenotesai.R.string.start_voice_recording)
    }

    val stateDesc = if (isRecording) {
        stringResource(id = com.voicenotesai.R.string.recording_in_progress)
    } else {
        stringResource(id = com.voicenotesai.R.string.ready_to_record)
    }
    
    return this.semantics {
        contentDescription = contentDesc
        stateDescription = stateDesc
        role = Role.Button
    }
}

/**
 * Format duration for accessibility announcement using localized strings.
 */
@Composable
private fun formatDurationForAccessibility(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return when {
        minutes > 0 -> stringResource(id = com.voicenotesai.R.string.duration_minutes_seconds_format, minutes, seconds)
        seconds > 0 -> stringResource(id = com.voicenotesai.R.string.duration_seconds_format, seconds)
        else -> stringResource(id = com.voicenotesai.R.string.less_than_a_second)
    }
}

/**
 * Note item accessibility with content preview
 */
@Composable
fun Modifier.noteItemAccessibility(
    timestamp: String,
    content: String
): Modifier {
    val preview = content.take(100).replace("\n", " ")
    val contentDesc = stringResource(id = com.voicenotesai.R.string.note_card_desc_format, timestamp, preview)
    
    return this.semantics {
        contentDescription = contentDesc
        role = Role.Button
    }
}

/**
 * Settings field accessibility with validation state
 */
@Composable
fun Modifier.settingsFieldAccessibility(
    value: String,
    isValid: Boolean = true,
    isRequired: Boolean = false
): Modifier {
    val statusDesc = when {
        !isValid -> stringResource(id = com.voicenotesai.R.string.invalid_input)
        isRequired && value.isBlank() -> stringResource(id = com.voicenotesai.R.string.required_field_not_filled)
        value.isNotBlank() -> stringResource(id = com.voicenotesai.R.string.field_completed)
        else -> stringResource(id = com.voicenotesai.R.string.field_empty)
    }
    
    return this.semantics {
        stateDescription = statusDesc
    }
}