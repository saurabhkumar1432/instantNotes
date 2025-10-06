package com.voicenotesai.presentation.main

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
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
import com.voicenotesai.presentation.animations.AnimationConfig
import com.voicenotesai.presentation.animations.SlideInContent
import com.voicenotesai.presentation.animations.SlideDirection
import com.voicenotesai.presentation.animations.bouncyClickable
import com.voicenotesai.presentation.animations.pulseAnimation
import com.voicenotesai.presentation.components.EnhancedErrorCard
import com.voicenotesai.presentation.components.EnhancedRecordButton
import com.voicenotesai.presentation.components.EnhancedWaveformIndicator
import com.voicenotesai.presentation.components.RecordingButtonState
import com.voicenotesai.presentation.components.RecordingQuality
import com.voicenotesai.presentation.components.RecordingQualityIndicator
import com.voicenotesai.presentation.components.RetryType
import com.voicenotesai.presentation.components.SmartRetryButton
import com.voicenotesai.presentation.theme.ExtendedTypography
import com.voicenotesai.presentation.theme.Spacing
import com.voicenotesai.presentation.theme.glassLayer
import com.voicenotesai.presentation.help.HelpTours
import com.voicenotesai.presentation.help.integration.HelpEnabledScreen
import com.voicenotesai.presentation.help.integration.helpTarget

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
	viewModel: MainViewModel = hiltViewModel(),
	onNavigateToSettings: () -> Unit,
	onNavigateToNotes: () -> Unit
) {
	val uiState by viewModel.uiState.collectAsState()
	val context = LocalContext.current

	val microphonePermissionState = rememberPermissionState(
		permission = Manifest.permission.RECORD_AUDIO
	)

	var showPermissionRationale by remember { mutableStateOf(false) }
	var showPermissionDeniedDialog by remember { mutableStateOf(false) }
	var previousShouldShowRationale by remember { mutableStateOf(microphonePermissionState.status.shouldShowRationale) }

	LaunchedEffect(microphonePermissionState.status) {
		val currentShouldShow = microphonePermissionState.status.shouldShowRationale
		val isGranted = microphonePermissionState.status.isGranted

		if (previousShouldShowRationale && !currentShouldShow && !isGranted) {
			showPermissionDeniedDialog = true
		}

		previousShouldShowRationale = currentShouldShow

		if (isGranted) {
			viewModel.onPermissionResult(true)
		} else if (!currentShouldShow && !isGranted) {
			showPermissionDeniedDialog = true
		}
	}

	Scaffold(
		modifier = Modifier.fillMaxSize(),
		containerColor = Color.Transparent,
		topBar = {
			TopAppBar(
				title = {
					Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
						Text(
							text = "Instant Notes",
							style = MaterialTheme.typography.titleLarge,
							fontWeight = FontWeight.SemiBold
						)
						Text(
							text = "Drop thoughts. Get structured notes.",
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
						)
					}
				},
				colors = TopAppBarDefaults.topAppBarColors(
					containerColor = Color.Transparent,
					titleContentColor = MaterialTheme.colorScheme.onBackground,
					actionIconContentColor = MaterialTheme.colorScheme.onBackground
				),
				actions = {
					val haptic = LocalHapticFeedback.current
					IconButton(
						onClick = {
							haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
							onNavigateToNotes()
						},
						modifier = Modifier
							.semantics {
								contentDescription = "View all saved notes"
							}
							.helpTarget("notes_button")
					) {
						Icon(
							imageVector = Icons.Default.List,
							contentDescription = null // Already provided by parent
						)
					}
					IconButton(
						onClick = {
							haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
							onNavigateToSettings()
						},
						modifier = Modifier
							.semantics {
								contentDescription = "Open AI settings and configuration"
							}
							.helpTarget("settings_button")
					) {
						Icon(
							imageVector = Icons.Default.Settings,
							contentDescription = null // Already provided by parent
						)
					}
				}
			)
		}
	) { paddingValues ->
		HelpEnabledScreen(
			helpKey = "main_screen",
			enabledTours = listOf(HelpTours.FIRST_TIME_USER, HelpTours.RECORDING_FEATURES),
			showHelpButton = true,
			autoStartTour = false,
			modifier = Modifier
				.fillMaxSize()
				.padding(paddingValues)
				.padding(horizontal = Spacing.large, vertical = Spacing.extraLarge)
		) {
			AnimatedContent(
				modifier = Modifier.fillMaxSize(),
				targetState = uiState,
				transitionSpec = {
					val initial = initialState
					val target = targetState
					// Avoid jitter while timer updates inside Recording state.
					if (initial is MainUiState.Recording && target is MainUiState.Recording) {
						EnterTransition.None togetherWith ExitTransition.None
					} else {
						val enter = fadeIn(animationSpec = tween(320)) +
							slideInVertically(animationSpec = tween(320)) { it / 6 }
						val exit = fadeOut(animationSpec = tween(320)) +
							slideOutVertically(animationSpec = tween(320)) { -it / 6 }
						enter togetherWith exit
					}
				},
				label = "main-state-transition"
			) { state ->
				when (state) {
					is MainUiState.Idle -> IdleContent(
						onRecordClick = {
							when {
								microphonePermissionState.status.isGranted -> viewModel.startRecording()
								microphonePermissionState.status.shouldShowRationale -> showPermissionRationale = true
								else -> microphonePermissionState.launchPermissionRequest()
							}
						},
						modifier = Modifier.fillMaxSize()
					)

					is MainUiState.PermissionRequired -> PermissionRequiredContent(
						onRequestPermission = {
							if (microphonePermissionState.status.shouldShowRationale) {
								showPermissionRationale = true
							} else {
								microphonePermissionState.launchPermissionRequest()
							}
						},
						onOpenSettings = {
							val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
								data = Uri.fromParts("package", context.packageName, null)
							}
							context.startActivity(intent)
						}
					)

					is MainUiState.Recording -> RecordingContent(
						duration = state.duration,
						onStopClick = { viewModel.stopRecording() }
					)

					is MainUiState.Processing -> ProcessingContent(message = state.message)

					is MainUiState.Success -> SuccessContent(
						notes = state.notes,
						onCopy = { copyToClipboard(context, state.notes) },
						onShare = { shareNotes(context, state.notes) },
						onNewRecording = { viewModel.resetToIdle() }
					)

					is MainUiState.Error -> IdleContent(
						onRecordClick = {
							when {
								microphonePermissionState.status.isGranted -> viewModel.startRecording()
								microphonePermissionState.status.shouldShowRationale -> showPermissionRationale = true
								else -> microphonePermissionState.launchPermissionRequest()
							}
						}
					)
				}
			}

			if (uiState is MainUiState.Error) {
				val error = (uiState as MainUiState.Error).error
				
				Column(
					modifier = Modifier
						.align(Alignment.BottomCenter)
						.padding(Spacing.medium),
					verticalArrangement = Arrangement.spacedBy(Spacing.medium),
					horizontalAlignment = Alignment.CenterHorizontally
				) {
					// Smart retry suggestion based on error type
					val errorMessage = error.toUserMessage()
					val retryType = when {
						errorMessage.contains("microphone", ignoreCase = true) || 
						errorMessage.contains("audio", ignoreCase = true) -> RetryType.CheckMicrophone
						errorMessage.contains("network", ignoreCase = true) || 
						errorMessage.contains("connection", ignoreCase = true) -> RetryType.CheckConnection
						else -> RetryType.RecordAgain
					}
					
					SmartRetryButton(
						retryType = retryType,
						onRetry = {
							viewModel.clearError()
							if (microphonePermissionState.status.isGranted) {
								viewModel.startRecording()
							}
						}
					)

					EnhancedErrorCard(
						error = error,
						onDismiss = { viewModel.clearError() },
						onRetry = {
							viewModel.clearError()
							if (microphonePermissionState.status.isGranted) {
								viewModel.startRecording()
							}
						},
						onNavigateToSettings = onNavigateToSettings,
						modifier = Modifier.fillMaxWidth()
					)
				}
			}
		}
	}

	if (showPermissionRationale) {
		AlertDialog(
			onDismissRequest = { showPermissionRationale = false },
			title = { Text("Microphone Permission Needed") },
			text = {
				Text(
					"We only use your mic while you record. The raw audio never leaves your device; only the " +
						"transcribed text reaches your AI provider."
				)
			},
			confirmButton = {
				TextButton(
					onClick = {
						showPermissionRationale = false
						microphonePermissionState.launchPermissionRequest()
					}
				) {
					Text("Grant Access")
				}
			},
			dismissButton = {
				TextButton(onClick = { showPermissionRationale = false }) {
					Text("Not Now")
				}
			}
		)
	}

	if (showPermissionDeniedDialog) {
		AlertDialog(
			onDismissRequest = { showPermissionDeniedDialog = false },
			title = { Text("Turn On Microphone") },
			text = {
				Text(
					"Recording is disabled because the microphone permission is off. You can enable it from the app settings."
				)
			},
			confirmButton = {
				TextButton(
					onClick = {
						showPermissionDeniedDialog = false
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
}

@Composable
private fun IdleContent(
	onRecordClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.verticalScroll(rememberScrollState()),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Spacer(modifier = Modifier.height(Spacing.extraLarge))

		Column(
			modifier = Modifier.fillMaxWidth(),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(Spacing.medium)
		) {
			Text(
				text = "✨ Drop that thought ✨",
				style = MaterialTheme.typography.headlineLarge,
				fontWeight = FontWeight.ExtraBold,
				textAlign = TextAlign.Center
			)
			Text(
				text = "Hit record, spill your mind, watch AI work its magic 🎯",
				style = MaterialTheme.typography.bodyLarge,
				textAlign = TextAlign.Center,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				fontWeight = FontWeight.Medium
			)
		}

		Spacer(modifier = Modifier.height(Spacing.extraLarge))

		Box(
			modifier = Modifier
				.fillMaxWidth()
				.border(
					width = 3.dp,
					brush = Brush.linearGradient(
						colors = listOf(
							MaterialTheme.colorScheme.primary,
							MaterialTheme.colorScheme.secondary,
							MaterialTheme.colorScheme.tertiary
						)
					),
					shape = RoundedCornerShape(24.dp)
				)
				.background(
					color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
					shape = RoundedCornerShape(24.dp)
				)
				.padding(horizontal = Spacing.large, vertical = Spacing.large)
		) {
			Column(
				verticalArrangement = Arrangement.spacedBy(Spacing.medium),
				horizontalAlignment = Alignment.Start
			) {
				Text(
					text = "🚀 What you get:",
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.primary
				)
				Text(
					text = "✓ Instant AI structuring (bullets + summary)",
					style = MaterialTheme.typography.bodyLarge,
					fontWeight = FontWeight.Medium
				)
				Text(
					text = "✓ Your choice of AI brain (OpenAI, Claude, Gemini)",
					style = MaterialTheme.typography.bodyLarge,
					fontWeight = FontWeight.Medium
				)
				Text(
					text = "✓ Audio never leaves your device 🔒",
					style = MaterialTheme.typography.bodyLarge,
					fontWeight = FontWeight.Medium
				)
			}
		}

		Spacer(modifier = Modifier.height(Spacing.huge))

		EnhancedRecordButton(
			recordingState = RecordingButtonState.Idle,
			onStartRecording = onRecordClick,
			onStopRecording = {},
			duration = 0L,
			modifier = Modifier.helpTarget("record_button")
		)

		Spacer(modifier = Modifier.height(Spacing.large))

		Text(
			text = "No typing. No fuss. Just vibes → notes 💭",
			style = MaterialTheme.typography.bodyLarge,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			fontWeight = FontWeight.Medium
		)

		Spacer(modifier = Modifier.height(Spacing.extraLarge))
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
			.verticalScroll(rememberScrollState()),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(Spacing.large)
	) {
		Spacer(modifier = Modifier.height(Spacing.extraLarge))

		Box(
			modifier = Modifier
				.fillMaxWidth()
				.glassLayer(RoundedCornerShape(32.dp))
				.padding(Spacing.large)
		) {
			Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
				Text(
					text = "Microphone permission required",
					style = MaterialTheme.typography.headlineSmall,
					fontWeight = FontWeight.SemiBold
				)
				Text(
					text = "We only capture audio while you're recording and instantly turn it into notes.",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}

		Button(
			onClick = onRequestPermission,
			modifier = Modifier.fillMaxWidth()
		) {
			Text("Grant Microphone Access")
		}

		OutlinedButton(
			onClick = onOpenSettings,
			modifier = Modifier.fillMaxWidth()
		) {
			Text("Open App Settings")
		}

		Spacer(modifier = Modifier.height(Spacing.extraLarge))
	}
}

@Composable
private fun RecordingContent(
	duration: Long,
	onStopClick: () -> Unit
) {
	// Simulate recording quality based on duration (in real app, this would come from audio analysis)
	val recordingQuality = remember(duration) {
		when {
			duration < 1000 -> RecordingQuality.Poor
			duration < 3000 -> RecordingQuality.Fair
			duration < 10000 -> RecordingQuality.Good
			else -> RecordingQuality.Excellent
		}
	}

	Column(
		modifier = Modifier
			.fillMaxSize()
			.verticalScroll(rememberScrollState()),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(Spacing.large)
	) {
		Spacer(modifier = Modifier.height(Spacing.medium))

		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(Spacing.small)
		) {
			Text(
				text = "🎤 Listening...",
				style = MaterialTheme.typography.headlineLarge,
				fontWeight = FontWeight.ExtraBold
			)

			RecordingQualityIndicator(
				quality = recordingQuality,
				modifier = Modifier.padding(horizontal = Spacing.medium)
			)
		}

		// Enhanced waveform visualization
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.height(140.dp)
				.border(
					width = 3.dp,
					brush = Brush.horizontalGradient(
						colors = listOf(
							MaterialTheme.colorScheme.primary,
							MaterialTheme.colorScheme.secondary
						)
					),
					shape = RoundedCornerShape(28.dp)
				)
				.background(
					color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
					shape = RoundedCornerShape(28.dp)
				)
				.padding(Spacing.large)
		) {
			EnhancedWaveformIndicator(
				isActive = true,
				intensity = when (recordingQuality) {
					RecordingQuality.Poor -> 0.3f
					RecordingQuality.Fair -> 0.6f
					RecordingQuality.Good -> 0.8f
					RecordingQuality.Excellent -> 1f
				},
				modifier = Modifier.fillMaxWidth()
			)
		}

		// Enhanced record button with stop functionality
		EnhancedRecordButton(
			recordingState = RecordingButtonState.Recording,
			onStartRecording = {},
			onStopRecording = onStopClick,
			duration = duration
		)

		Text(
			text = "Speak clearly for best results ✨\nTap stop when you're done",
			style = MaterialTheme.typography.bodyLarge,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			fontWeight = FontWeight.Medium,
			textAlign = TextAlign.Center,
			lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3
		)

		Spacer(modifier = Modifier.height(Spacing.medium))
	}
}

@Composable
private fun ProcessingContent(
	message: String
) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.verticalScroll(rememberScrollState()),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		// Enhanced processing button (non-interactive)
		EnhancedRecordButton(
			recordingState = RecordingButtonState.Processing,
			onStartRecording = {},
			onStopRecording = {},
			duration = 0L,
			enabled = false
		)

		Spacer(modifier = Modifier.height(Spacing.large))

		Box(
			modifier = Modifier
				.fillMaxWidth()
				.glassLayer(RoundedCornerShape(28.dp))
				.padding(Spacing.large)
		) {
			Column(
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(Spacing.medium)
			) {
				Text(
					text = message,
					style = MaterialTheme.typography.bodyLarge,
					textAlign = TextAlign.Center,
					fontWeight = FontWeight.Medium
				)
				
				Text(
					text = "🧠 AI is working its magic...",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					textAlign = TextAlign.Center
				)
			}
		}
	}
}

@Composable
private fun SuccessContent(
	notes: String,
	onCopy: () -> Unit,
	onShare: () -> Unit,
	onNewRecording: () -> Unit
) {
	val haptic = LocalHapticFeedback.current
	
	SlideInContent(
		visible = true,
		slideDirection = SlideDirection.Bottom
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.verticalScroll(rememberScrollState()),
			verticalArrangement = Arrangement.spacedBy(Spacing.large)
		) {
		Spacer(modifier = Modifier.height(Spacing.small))

		Text(
			text = "🔥 Your AI-powered notes",
			style = MaterialTheme.typography.headlineLarge,
			fontWeight = FontWeight.ExtraBold
		)

		val noteScrollState = rememberScrollState()

		Box(
			modifier = Modifier
				.fillMaxWidth()
				.heightIn(min = 200.dp, max = 420.dp)
				.border(
					width = 3.dp,
					brush = Brush.linearGradient(
						colors = listOf(
							MaterialTheme.colorScheme.secondary,
							MaterialTheme.colorScheme.primary
						)
					),
					shape = RoundedCornerShape(24.dp)
				)
				.background(
					color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
					shape = RoundedCornerShape(24.dp)
				)
		) {
			Text(
				text = notes,
				style = ExtendedTypography.noteContent,
				color = MaterialTheme.colorScheme.onSurface,
				modifier = Modifier
					.fillMaxWidth()
					.verticalScroll(noteScrollState)
					.padding(Spacing.large),
				fontWeight = FontWeight.Medium
			)
		}

		Button(
			onClick = {
				haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
				onCopy()
			},
			modifier = Modifier
				.fillMaxWidth()
				.semantics {
					contentDescription = "Copy generated notes to clipboard"
				},
			colors = ButtonDefaults.buttonColors(
				containerColor = MaterialTheme.colorScheme.secondaryContainer,
				contentColor = MaterialTheme.colorScheme.onSecondaryContainer
			)
		) {
			Text(
				"📋 Copy to clipboard", 
				style = ExtendedTypography.buttonText
			)
		}

		Button(
			onClick = {
				haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
				onShare()
			},
			modifier = Modifier
				.fillMaxWidth()
				.semantics {
					contentDescription = "Share generated notes with other apps"
				},
			colors = ButtonDefaults.buttonColors(
				containerColor = MaterialTheme.colorScheme.tertiaryContainer,
				contentColor = MaterialTheme.colorScheme.onTertiaryContainer
			)
		) {
			Text(
				"📤 Share with friends", 
				style = ExtendedTypography.buttonText
			)
		}

		Button(
			onClick = {
				haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
				onNewRecording()
			},
			modifier = Modifier
				.fillMaxWidth()
				.semantics {
					contentDescription = "Start a new voice recording"
				}
		) {
			Text(
				"🎙️ Record another banger", 
				style = ExtendedTypography.buttonText
			)
		}

		Spacer(modifier = Modifier.height(Spacing.small))
	}
	}
}

private fun formatDuration(milliseconds: Long): String {
	val totalSeconds = milliseconds / 1000
	val minutes = totalSeconds / 60
	val seconds = totalSeconds % 60
	return String.format("%02d:%02d", minutes, seconds)
}

private fun formatDurationForAccessibility(milliseconds: Long): String {
	val totalSeconds = milliseconds / 1000
	val minutes = totalSeconds / 60
	val seconds = totalSeconds % 60
	
	return when {
		minutes > 0 -> "$minutes minutes and $seconds seconds"
		seconds > 0 -> "$seconds seconds"
		else -> "less than a second"
	}
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
