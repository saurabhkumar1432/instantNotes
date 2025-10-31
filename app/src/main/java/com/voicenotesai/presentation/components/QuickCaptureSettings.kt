package com.voicenotesai.presentation.components

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Shortcut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.voicenotesai.data.notification.QuickCaptureService
import com.voicenotesai.domain.model.VoiceCommand
import com.voicenotesai.presentation.main.QuickCaptureViewModel
import com.voicenotesai.presentation.main.VoiceCommandEvent

/**
 * Composable for managing quick capture settings and features.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCaptureSettings(
    modifier: Modifier = Modifier,
    viewModel: QuickCaptureViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val voiceCommandEvent by viewModel.voiceCommandEvents.collectAsState()
    
    // Handle voice command events
    LaunchedEffect(voiceCommandEvent) {
        when (voiceCommandEvent) {
            is VoiceCommandEvent.CommandRecognized -> {
                // Handle the recognized command
                // This would typically trigger navigation or actions
                viewModel.clearVoiceCommandEvent()
            }
            is VoiceCommandEvent.CommandNotRecognized -> {
                // Show feedback that command wasn't recognized
                viewModel.clearVoiceCommandEvent()
            }
            null -> { /* No event */ }
        }
    }
    
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "Quick Capture Features",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Voice Commands Section
        item {
            QuickCaptureCard(
                title = "Voice Commands",
                description = "Control the app with voice commands",
                icon = if (uiState.isVoiceCommandListening) Icons.Default.Mic else Icons.Default.MicOff,
                isEnabled = uiState.isVoiceCommandEnabled,
                isActive = uiState.isVoiceCommandListening,
                onToggle = { viewModel.toggleVoiceCommandListening() }
            )
        }
        
        // Show available commands when voice commands are enabled
        if (uiState.isVoiceCommandEnabled) {
            item {
                VoiceCommandsList(
                    commands = viewModel.getAvailableCommands(),
                    onTestCommand = { command ->
                        viewModel.processTextCommand(command)
                    }
                )
            }
        }
        
        // Persistent Notification Section
        item {
            QuickCaptureCard(
                title = "Quick Capture Notification",
                description = "Persistent notification for quick access",
                icon = if (uiState.isPersistentNotificationEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                isEnabled = true,
                isActive = uiState.isPersistentNotificationEnabled,
                onToggle = { enabled ->
                    if (enabled) {
                        QuickCaptureService.startService(context)
                    } else {
                        QuickCaptureService.stopService(context)
                    }
                }
            )
        }
        
        // App Shortcuts Section
        item {
            QuickCaptureCard(
                title = "App Shortcuts",
                description = "Long-press app icon for quick actions",
                icon = Icons.Default.Shortcut,
                isEnabled = true,
                isActive = true,
                onToggle = { /* Shortcuts are always enabled */ }
            )
        }
        
        // Error display
        uiState.error?.let { error ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { viewModel.clearError() }
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickCaptureCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isEnabled: Boolean,
    isActive: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            Switch(
                checked = isActive,
                onCheckedChange = onToggle,
                enabled = isEnabled
            )
        }
    }
}

@Composable
private fun VoiceCommandsList(
    commands: Map<VoiceCommand, List<String>>,
    onTestCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Available Commands",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            commands.forEach { (command, phrases) ->
                Column(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = command.name.replace("_", " ").lowercase()
                            .replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    phrases.take(2).forEach { phrase ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "\"$phrase\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { onTestCommand(phrase) }
                            ) {
                                Text("Test")
                            }
                        }
                    }
                }
                if (command != commands.keys.last()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}