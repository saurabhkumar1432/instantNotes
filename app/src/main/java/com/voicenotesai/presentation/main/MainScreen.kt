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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.voicenotesai.presentation.animations.SlideInContent
import com.voicenotesai.presentation.animations.SlideDirection
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
							text = "Voice-first capture for structured, shareable notes.",
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
		Box(
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
					"Instant Notes only requests access while you record. Raw audio stays on this device; " +
						"only the transcription is shared with your configured AI provider."
				)
			},
			confirmButton = {
				TextButton(
					onClick = {
						showPermissionRationale = false
						microphonePermissionState.launchPermissionRequest()
					}
				) {
					Text("Allow access")
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
					"Recording is unavailable because microphone access is disabled. Enable the permission from Settings to continue."
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
					Text("Open settings")
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
		modifier = modifier
			.fillMaxSize()
			.verticalScroll(rememberScrollState()),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Top
	) {
		Spacer(modifier = Modifier.height(Spacing.extraLarge))

		Text(
			text = "Capture ideas instantly",
			style = MaterialTheme.typography.headlineLarge,
			fontWeight = FontWeight.SemiBold,
			textAlign = TextAlign.Center
		)

		Spacer(modifier = Modifier.height(Spacing.small))

		Text(
			text = "Press record and let Instant Notes transform the conversation into polished summaries, decisions, and action items.",
			style = MaterialTheme.typography.bodyLarge,
			textAlign = TextAlign.Center,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)

		Spacer(modifier = Modifier.height(Spacing.large))

		Surface(
			modifier = Modifier.fillMaxWidth(),
			shape = RoundedCornerShape(28.dp),
			tonalElevation = 6.dp
		) {
			Column(
				modifier = Modifier.padding(Spacing.large),
				verticalArrangement = Arrangement.spacedBy(Spacing.medium)
			) {
				FeatureRow(
					icon = Icons.Filled.Description,
					title = "Structured output",
					subtitle = "Concise summaries with highlights and follow-ups ready to share."
				)
				FeatureRow(
					icon = Icons.Filled.Schedule,
					title = "Meeting-ready notes",
					subtitle = "Timestamped details arrive the moment you stop recording."
				)
				FeatureRow(
					icon = Icons.Filled.Lock,
					title = "Privacy built in",
					subtitle = "Audio stays on device—only approved text reaches your AI provider."
				)
			}
		}

		Spacer(modifier = Modifier.height(Spacing.large))

		Row(
			modifier = Modifier
				.fillMaxWidth()
				.horizontalScroll(rememberScrollState()),
			horizontalArrangement = Arrangement.spacedBy(Spacing.small)
		) {
			InsightChip(icon = Icons.Filled.Bolt, label = "Realtime transcription")
			InsightChip(icon = Icons.Filled.TaskAlt, label = "Action item detection")
			InsightChip(icon = Icons.Filled.Lock, label = "Enterprise security")
		}

		Spacer(modifier = Modifier.height(Spacing.huge))

		EnhancedRecordButton(
			recordingState = RecordingButtonState.Idle,
			onStartRecording = onRecordClick,
			onStopRecording = {},
			duration = 0L
		)

		Spacer(modifier = Modifier.height(Spacing.large))

		Text(
			text = "Review, copy, and share the transcript immediately after recording completes.",
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			textAlign = TextAlign.Center
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

		Surface(
			modifier = Modifier.fillMaxWidth(),
			shape = RoundedCornerShape(28.dp),
			tonalElevation = 4.dp
		) {
			Column(
				modifier = Modifier.padding(Spacing.large),
				verticalArrangement = Arrangement.spacedBy(Spacing.medium)
			) {
				Text(
					text = "Microphone access required",
					style = MaterialTheme.typography.headlineSmall,
					fontWeight = FontWeight.SemiBold
				)
				Text(
					text = "Instant Notes only listens while you record and stores audio locally. Enable the microphone to capture conversations hands-free.",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}

		Button(
			onClick = onRequestPermission,
			modifier = Modifier.fillMaxWidth()
		) {
			Text("Allow microphone access")
		}

		OutlinedButton(
			onClick = onOpenSettings,
			modifier = Modifier.fillMaxWidth()
		) {
			Text("Open app settings")
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
				text = "Recording in progress",
				style = MaterialTheme.typography.headlineLarge,
				fontWeight = FontWeight.ExtraBold
			)

			RecordingQualityIndicator(
				quality = recordingQuality,
				modifier = Modifier.padding(horizontal = Spacing.medium)
			)
		}

		Surface(
			modifier = Modifier
				.fillMaxWidth()
				.height(140.dp),
			shape = RoundedCornerShape(28.dp),
			tonalElevation = 6.dp
		) {
			Box(modifier = Modifier.padding(Spacing.large)) {
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
		}

		// Enhanced record button with stop functionality
		EnhancedRecordButton(
			recordingState = RecordingButtonState.Recording,
			onStartRecording = {},
			onStopRecording = onStopClick,
			duration = duration
		)

		Text(
			text = "Speak naturally. Pause or tap stop whenever you want to review your notes.",
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

		Surface(
			modifier = Modifier.fillMaxWidth(),
			shape = RoundedCornerShape(28.dp),
			tonalElevation = 4.dp
		) {
			Column(
				modifier = Modifier.padding(Spacing.large),
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
					text = "We’re transcribing and organizing your notes.",
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
			text = "Notes ready to share",
			style = MaterialTheme.typography.headlineLarge,
			fontWeight = FontWeight.SemiBold
		)

		val noteScrollState = rememberScrollState()

		Surface(
			modifier = Modifier
				.fillMaxWidth()
				.heightIn(min = 200.dp, max = 420.dp),
			shape = RoundedCornerShape(24.dp),
			tonalElevation = 6.dp
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

		FilledTonalButton(
			onClick = {
				haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
				onCopy()
			},
			modifier = Modifier
				.fillMaxWidth()
				.semantics {
					contentDescription = "Copy generated notes to clipboard"
				}
		) {
			Text(
				"Copy to clipboard",
				style = ExtendedTypography.buttonText
			)
		}

		FilledTonalButton(
			onClick = {
				haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
				onShare()
			},
			modifier = Modifier
				.fillMaxWidth()
				.semantics {
					contentDescription = "Share generated notes with other apps"
				}
		) {
			Text(
				"Share",
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
				"Start new recording",
				style = ExtendedTypography.buttonText
			)
		}

		Spacer(modifier = Modifier.height(Spacing.small))
	}
	}
}

	@Composable
	private fun FeatureRow(
		icon: ImageVector,
		title: String,
		subtitle: String
	) {
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
			verticalAlignment = Alignment.CenterVertically
		) {
			Surface(
				shape = CircleShape,
				color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
			) {
				Icon(
					imageVector = icon,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.primary,
					modifier = Modifier
						.padding(Spacing.small)
						.size(28.dp)
				)
			}

			Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
				Text(
					text = title,
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.SemiBold
				)
				Text(
					text = subtitle,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}
	}

	@Composable
	private fun InsightChip(
		icon: ImageVector,
		label: String,
		modifier: Modifier = Modifier
	) {
		Surface(
			modifier = modifier,
			shape = RoundedCornerShape(50),
			color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
		) {
			Row(
				modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small),
				horizontalArrangement = Arrangement.spacedBy(Spacing.small),
				verticalAlignment = Alignment.CenterVertically
			) {
				Icon(
					imageVector = icon,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.onSecondaryContainer,
					modifier = Modifier.size(18.dp)
				)
				Text(
					text = label,
					style = MaterialTheme.typography.labelLarge,
					color = MaterialTheme.colorScheme.onSecondaryContainer
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
