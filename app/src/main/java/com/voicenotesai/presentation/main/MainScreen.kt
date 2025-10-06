package com.voicenotesai.presentation.main

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.voicenotesai.domain.model.AppError
import com.voicenotesai.domain.model.canRetry
import com.voicenotesai.domain.model.getActionGuidance
import com.voicenotesai.domain.model.shouldNavigateToSettings
import com.voicenotesai.domain.model.toUserMessage
import com.voicenotesai.presentation.theme.Spacing

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit,
    onNavigateToNotes: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Permission state
    val microphonePermissionState = rememberPermissionState(
        permission = Manifest.permission.RECORD_AUDIO
    )

    // Handle permission rationale dialog
    var showPermissionRationale by remember { mutableStateOf(false) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    
    // Track permission denial state
    var previousShouldShowRationale by remember { mutableStateOf(microphonePermissionState.status.shouldShowRationale) }
    var permissionPermanentlyDenied by remember { mutableStateOf(false) }
    
    // Detect when user clicks "Don't ask again"
    LaunchedEffect(microphonePermissionState.status) {
        val currentShouldShow = microphonePermissionState.status.shouldShowRationale
        val isGranted = microphonePermissionState.status.isGranted
        
        // If permission was previously shouldShowRationale=true, but now it's false AND not granted,
        // then user clicked "Don't ask again"
        if (previousShouldShowRationale && !currentShouldShow && !isGranted) {
            permissionPermanentlyDenied = true
            showPermissionDeniedDialog = true
        }
        
        previousShouldShowRationale = currentShouldShow
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Notes AI") },
                actions = {
                    IconButton(onClick = onNavigateToNotes) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "View all saved notes"
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open settings to configure AI provider and API key"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    val initial = initialState
                    val target = targetState
                    if (initial is MainUiState.Recording && target is MainUiState.Recording) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        val enter = fadeIn(animationSpec = tween(300)) +
                            slideInVertically(animationSpec = tween(300)) { it / 4 }
                        val exit = fadeOut(animationSpec = tween(300)) +
                            slideOutVertically(animationSpec = tween(300)) { -it / 4 }
                        enter togetherWith exit
                    }
                },
                label = "stateTransition"
            ) { state ->
                when (state) {
                    is MainUiState.Idle -> {
                        IdleContent(
                            onRecordClick = {
                                when {
                                    microphonePermissionState.status.isGranted -> {
                                        viewModel.startRecording()
                                    }
                                    microphonePermissionState.status.shouldShowRationale -> {
                                        showPermissionRationale = true
                                    }
                                    else -> {
                                        microphonePermissionState.launchPermissionRequest()
                                    }
                                }
                            }
                        )
                    }
                    is MainUiState.PermissionRequired -> {
                        PermissionRequiredContent(
                            onRequestPermission = {
                                when {
                                    microphonePermissionState.status.shouldShowRationale -> {
                                        showPermissionRationale = true
                                    }
                                    else -> {
                                        microphonePermissionState.launchPermissionRequest()
                                    }
                                }
                            },
                            onOpenSettings = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                    is MainUiState.Recording -> {
                        RecordingContent(
                            duration = state.duration,
                            onStopClick = { viewModel.stopRecording() },
                            enabled = true
                        )
                    }
                    is MainUiState.Processing -> {
                        ProcessingContent(message = state.message)
                    }
                    is MainUiState.Success -> {
                        SuccessContent(
                            notes = state.notes,
                            onCopy = { 
                                copyToClipboard(context, state.notes)
                            },
                            onShare = { shareNotes(context, state.notes) },
                            onNewRecording = { viewModel.resetToIdle() }
                        )
                    }
                    is MainUiState.Error -> {
                        // Show idle content in background
                        IdleContent(
                            onRecordClick = {
                                when {
                                    microphonePermissionState.status.isGranted -> {
                                        viewModel.startRecording()
                                    }
                                    microphonePermissionState.status.shouldShowRationale -> {
                                        showPermissionRationale = true
                                    }
                                    else -> {
                                        microphonePermissionState.launchPermissionRequest()
                                    }
                                }
                            }
                        )
                    }
                }
            }
            
            // Show error dialog on top of content
            if (uiState is MainUiState.Error) {
                ErrorDialog(
                    error = (uiState as MainUiState.Error).error,
                    onDismiss = { viewModel.clearError() },
                    onRetry = {
                        viewModel.clearError()
                        if (microphonePermissionState.status.isGranted) {
                            viewModel.startRecording()
                        }
                    },
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        }
    }

    // Permission rationale dialog
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Microphone Permission Required") },
            text = {
                Text("This app needs access to your microphone to record voice notes. " +
                     "Your recordings are processed locally and only the transcribed text is sent to the AI service.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionRationale = false
                        microphonePermissionState.launchPermissionRequest()
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Permission permanently denied dialog
    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("Permission Required") },
            text = {
                Text("Microphone permission is required to record voice notes. " +
                     "Please enable it in the app settings.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDeniedDialog = false
                        // Open app settings
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Handle permission result
    LaunchedEffect(microphonePermissionState.status) {
        if (microphonePermissionState.status.isGranted) {
            // Permission granted - just notify ViewModel, don't auto-start recording
            viewModel.onPermissionResult(true)
        } else if (!microphonePermissionState.status.shouldShowRationale && 
                   !microphonePermissionState.status.isGranted) {
            // Permission was permanently denied
            showPermissionDeniedDialog = true
        }
    }
}

@Composable
private fun IdleContent(
    onRecordClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Tap to record your voice",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = Spacing.huge)
        )
        
        RecordButton(
            isRecording = false,
            onClick = onRecordClick,
            enabled = true
        )
        
        Text(
            text = "Your voice will be converted into organized notes",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.huge)
        )
    }
}

@Composable
private fun PermissionRequiredContent(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Microphone Permission Required",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = Spacing.medium)
        )
        
        Text(
            text = "This app needs access to your microphone to record voice notes. " +
                  "Your recordings are processed locally and only the transcribed text is sent to the AI service.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = Spacing.extraLarge)
        )
        
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Grant Permission")
        }
        
        Spacer(modifier = Modifier.height(Spacing.small))
        
        TextButton(onClick = onOpenSettings) {
            Text("Open App Settings")
        }
    }
}

@Composable
private fun ErrorDialog(
    error: AppError,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val userMessage = error.toUserMessage()
    val actionGuidance = error.getActionGuidance()
    val canRetry = error.canRetry()
    val shouldGoToSettings = error.shouldNavigateToSettings()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Error",
                color = MaterialTheme.colorScheme.error
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                Text(text = userMessage)
                actionGuidance?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            when {
                shouldGoToSettings -> {
                    TextButton(onClick = {
                        onDismiss()
                        onNavigateToSettings()
                    }) {
                        Text("Go to Settings")
                    }
                }
                canRetry -> {
                    TextButton(onClick = {
                        onDismiss()
                        onRetry()
                    }) {
                        Text("Retry")
                    }
                }
                else -> {
                    TextButton(onClick = onDismiss) {
                        Text("OK")
                    }
                }
            }
        },
        dismissButton = {
            if (shouldGoToSettings || canRetry) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun RecordingContent(
    duration: Long,
    onStopClick: () -> Unit,
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Recording...",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = Spacing.large)
        )
        
        Text(
            text = formatDuration(duration),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = Spacing.large)
        )
        
        // Waveform indicator
        WaveformIndicator(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(60.dp)
                .padding(bottom = Spacing.extraLarge)
        )
        
        RecordButton(
            isRecording = true,
            onClick = onStopClick,
            enabled = enabled
        )
        
        Text(
            text = "Tap to stop recording",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.huge)
        )
    }
}

@Composable
private fun ProcessingContent(
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(64.dp)
                .padding(bottom = Spacing.large),
            // Note: CircularProgressIndicator doesn't have contentDescription parameter
            // Screen readers will announce "Progress" by default
        )
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SuccessContent(
    notes: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onNewRecording: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.medium)
    ) {
        Text(
            text = "Generated Notes",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = Spacing.medium)
        )
        
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.medium)
            )
        }
        
        Spacer(modifier = Modifier.height(Spacing.medium))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            OutlinedButton(
                onClick = onCopy,
                modifier = Modifier.weight(1f)
            ) {
                Text("Copy")
            }
            
            OutlinedButton(
                onClick = onShare,
                modifier = Modifier.weight(1f)
            ) {
                Text("Share")
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.small))
        
        Button(
            onClick = onNewRecording,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("New Recording")
        }
    }
}



@Composable
private fun RecordButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    // Pulsing animation for recording state
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // Ripple effect animation
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = if (isRecording) 0.6f else 0f,
        targetValue = if (isRecording) 0f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleAlpha"
    )
    
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = if (isRecording) 1f else 1f,
        targetValue = if (isRecording) 1.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleScale"
    )
    
    val color = if (isRecording) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(140.dp)
    ) {
        // Ripple effect when recording
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(rippleScale)
                    .background(
                        color = color.copy(alpha = rippleAlpha),
                        shape = CircleShape
                    )
            )
        }
        
        // Main button
        FilledIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(120.dp)
                .scale(scale),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = color
            ),
            shape = CircleShape
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Color.White,
                        shape = if (isRecording) MaterialTheme.shapes.small else CircleShape
                    )
            )
        }
    }
}

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Generated Notes", text)
    clipboard.setPrimaryClip(clip)
}

private fun shareNotes(context: Context, notes: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, notes)
        putExtra(Intent.EXTRA_SUBJECT, "Voice Notes")
    }
    context.startActivity(Intent.createChooser(intent, "Share Notes"))
}

@Composable
private fun WaveformIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    
    // Create 5 bars with different animation offsets
    val barCount = 5
    val barHeights = (0 until barCount).map { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 600 + (index * 100),
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(index * 100)
            ),
            label = "bar$index"
        )
    }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        barHeights.forEach { heightFraction ->
            Box(
                modifier = Modifier
                    .width(Spacing.small)
                    .fillMaxHeight(heightFraction.value)
                    .background(
                        color = MaterialTheme.colorScheme.error,
                        shape = MaterialTheme.shapes.small
                    )
            )
        }
    }
}
