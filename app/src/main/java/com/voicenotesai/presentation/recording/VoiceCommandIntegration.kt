package com.voicenotesai.presentation.recording

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.voicenotesai.domain.model.VoiceCommand
import com.voicenotesai.presentation.main.QuickCaptureViewModel
import com.voicenotesai.presentation.main.VoiceCommandEvent

/**
 * Integration component for handling voice commands in the recording screen.
 */
@Composable
fun VoiceCommandIntegration(
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onSaveNote: () -> Unit,
    onCreateReminder: () -> Unit,
    viewModel: QuickCaptureViewModel = hiltViewModel()
) {
    val voiceCommandEvent by viewModel.voiceCommandEvents.collectAsState()
    
    // Handle voice command events
    LaunchedEffect(voiceCommandEvent) {
        when (val event = voiceCommandEvent) {
            is VoiceCommandEvent.CommandRecognized -> {
                when (event.command) {
                    VoiceCommand.START_RECORDING -> {
                        if (!isRecording) {
                            onStartRecording()
                            viewModel.reportShortcutUsed("quick_record")
                        }
                    }
                    VoiceCommand.STOP_RECORDING -> {
                        if (isRecording) {
                            onStopRecording()
                        }
                    }
                    VoiceCommand.PAUSE_RECORDING -> {
                        // Handle pause recording
                    }
                    VoiceCommand.RESUME_RECORDING -> {
                        // Handle resume recording
                    }
                    VoiceCommand.SAVE_NOTE -> {
                        onSaveNote()
                    }
                    VoiceCommand.DELETE_NOTE -> {
                        // Handle delete note
                    }
                    VoiceCommand.SHARE_NOTE -> {
                        // Handle share note
                    }
                    VoiceCommand.GO_HOME -> {
                        // Handle go home
                    }
                    VoiceCommand.OPEN_NOTES -> {
                        // Handle open notes
                    }
                    VoiceCommand.OPEN_TASKS -> {
                        // Handle open tasks
                    }
                    VoiceCommand.OPEN_SETTINGS -> {
                        // Handle open settings
                    }
                    VoiceCommand.GO_BACK -> {
                        // Handle go back
                    }
                    VoiceCommand.CREATE_TASK -> {
                        // Handle create task
                    }
                    VoiceCommand.COMPLETE_TASK -> {
                        // Handle complete task
                    }
                    VoiceCommand.CREATE_REMINDER -> {
                        onCreateReminder()
                    }
                    VoiceCommand.SEARCH_NOTES -> {
                        // Handle search notes
                    }
                    VoiceCommand.CLEAR_SEARCH -> {
                        // Handle clear search
                    }
                    VoiceCommand.FILTER_NOTES -> {
                        // Handle filter notes
                    }
                    VoiceCommand.READ_SCREEN -> {
                        // Handle read screen
                    }
                    VoiceCommand.REPEAT_LAST -> {
                        // Handle repeat last
                    }
                    VoiceCommand.HELP -> {
                        // Handle help
                    }
                    VoiceCommand.PLAY_NOTE -> {
                        // Handle play note
                    }
                    VoiceCommand.STOP_PLAYBACK -> {
                        // Handle stop playback
                    }
                }
                viewModel.clearVoiceCommandEvent()
            }
            is VoiceCommandEvent.CommandNotRecognized -> {
                // Could show a brief message that command wasn't recognized
                viewModel.clearVoiceCommandEvent()
            }
            null -> { /* No event */ }
        }
    }
    
    // Start voice command listening when recording screen is active
    LaunchedEffect(Unit) {
        viewModel.startVoiceCommandListening()
    }
    
    // Stop voice command listening when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopVoiceCommandListening()
        }
    }
}

/**
 * Composable that provides voice command feedback UI.
 */
@Composable
fun VoiceCommandFeedback(
    modifier: Modifier = Modifier,
    viewModel: QuickCaptureViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    if (uiState.isVoiceCommandListening) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice commands active",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Voice commands active",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}