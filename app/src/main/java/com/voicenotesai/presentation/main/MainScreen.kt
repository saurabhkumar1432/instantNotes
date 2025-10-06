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
import com.voicenotesai.presentation.theme.Spacing
import com.voicenotesai.presentation.theme.glassLayer

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
					IconButton(onClick = onNavigateToNotes) {
						Icon(
							imageVector = Icons.Default.List,
							contentDescription = "View all saved notes"
						)
					}
					IconButton(onClick = onNavigateToSettings) {
						Icon(
							imageVector = Icons.Default.Settings,
							contentDescription = "Open settings"
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
						}
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
	onRecordClick: () -> Unit
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

		RecordButton(isRecording = false, onClick = onRecordClick)

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
				text = "Something went wrong",
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
	onStopClick: () -> Unit
) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.verticalScroll(rememberScrollState()),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Spacer(modifier = Modifier.height(Spacing.large))

		Text(
			text = "🎤 Listening...",
			style = MaterialTheme.typography.headlineLarge,
			fontWeight = FontWeight.ExtraBold
		)

		Spacer(modifier = Modifier.height(Spacing.small))

		Text(
			text = formatDuration(duration),
			style = MaterialTheme.typography.displayMedium,
			color = MaterialTheme.colorScheme.primary,
			fontWeight = FontWeight.Bold
		)

		Spacer(modifier = Modifier.height(Spacing.large))

		Box(
			modifier = Modifier
				.fillMaxWidth()
				.height(160.dp)
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
				.padding(horizontal = Spacing.large, vertical = Spacing.large)
		) {
			WaveformIndicator(
				modifier = Modifier
					.fillMaxWidth()
					.fillMaxHeight()
			)
		}

		Spacer(modifier = Modifier.height(Spacing.huge))

		RecordButton(isRecording = true, onClick = onStopClick)

		Spacer(modifier = Modifier.height(Spacing.large))

		Text(
			text = "Done? Tap to stop and let the magic happen ✨",
			style = MaterialTheme.typography.bodyLarge,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			fontWeight = FontWeight.Medium,
			textAlign = TextAlign.Center
		)

		Spacer(modifier = Modifier.height(Spacing.extraLarge))
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
		CircularProgressIndicator(
			modifier = Modifier.size(72.dp),
			color = MaterialTheme.colorScheme.primary,
			trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
		)

		Spacer(modifier = Modifier.height(Spacing.large))

		Box(
			modifier = Modifier
				.fillMaxWidth()
				.glassLayer(RoundedCornerShape(28.dp))
				.padding(Spacing.large)
		) {
			Text(
				text = message,
				style = MaterialTheme.typography.bodyLarge,
				textAlign = TextAlign.Center
			)
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
				style = MaterialTheme.typography.bodyLarge,
				color = MaterialTheme.colorScheme.onSurface,
				modifier = Modifier
					.fillMaxWidth()
					.verticalScroll(noteScrollState)
					.padding(Spacing.large),
				fontWeight = FontWeight.Medium
			)
		}

		Button(
			onClick = onCopy,
			modifier = Modifier.fillMaxWidth(),
			colors = ButtonDefaults.buttonColors(
				containerColor = MaterialTheme.colorScheme.secondaryContainer,
				contentColor = MaterialTheme.colorScheme.onSecondaryContainer
			)
		) {
			Text("📋 Copy to clipboard", fontWeight = FontWeight.Bold)
		}

		Button(
			onClick = onShare,
			modifier = Modifier.fillMaxWidth(),
			colors = ButtonDefaults.buttonColors(
				containerColor = MaterialTheme.colorScheme.tertiaryContainer,
				contentColor = MaterialTheme.colorScheme.onTertiaryContainer
			)
		) {
			Text("📤 Share with friends", fontWeight = FontWeight.Bold)
		}

		Button(
			onClick = onNewRecording,
			modifier = Modifier.fillMaxWidth()
		) {
			Text("🎙️ Record another banger", fontWeight = FontWeight.Bold)
		}

		Spacer(modifier = Modifier.height(Spacing.small))
	}
}

@Composable
private fun RecordButton(
	isRecording: Boolean,
	onClick: () -> Unit,
	enabled: Boolean = true
) {
	val infiniteTransition = rememberInfiniteTransition(label = "record-pulse")

	val scale by infiniteTransition.animateFloat(
		initialValue = 1f,
		targetValue = if (isRecording) 1.12f else 1f,
		animationSpec = infiniteRepeatable(
			animation = tween(820, easing = FastOutSlowInEasing),
			repeatMode = RepeatMode.Reverse
		),
		label = "record-scale"
	)

	val rippleAlpha by infiniteTransition.animateFloat(
		initialValue = if (isRecording) 0.4f else 0.15f,
		targetValue = 0f,
		animationSpec = infiniteRepeatable(
			animation = tween(1400, easing = LinearEasing),
			repeatMode = RepeatMode.Restart
		),
		label = "record-alpha"
	)

	val rippleScale by infiniteTransition.animateFloat(
		initialValue = 1f,
		targetValue = if (isRecording) 1.45f else 1.1f,
		animationSpec = infiniteRepeatable(
			animation = tween(1400, easing = FastOutSlowInEasing),
			repeatMode = RepeatMode.Restart
		),
		label = "record-ripple"
	)

	val buttonColor = if (isRecording) {
		MaterialTheme.colorScheme.error
	} else {
		MaterialTheme.colorScheme.primary
	}

	Box(
		contentAlignment = Alignment.Center,
		modifier = Modifier.size(180.dp)
	) {
		Box(
			modifier = Modifier
				.size(160.dp)
				.background(
					brush = Brush.radialGradient(
						colors = listOf(buttonColor.copy(alpha = rippleAlpha), Color.Transparent)
					),
					shape = CircleShape
				)
				.scale(rippleScale)
				.blur(30.dp)
		)

		FilledIconButton(
			onClick = onClick,
			enabled = enabled,
			modifier = Modifier
				.size(132.dp)
				.scale(scale),
			colors = IconButtonDefaults.filledIconButtonColors(
				containerColor = buttonColor
			),
			shape = CircleShape
		) {
			Box(
				modifier = Modifier
					.size(46.dp)
					.background(
						color = Color.White,
						shape = if (isRecording) RoundedCornerShape(8.dp) else CircleShape
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
	val barCount = 5
	val barBrush = Brush.verticalGradient(
		colors = listOf(
			MaterialTheme.colorScheme.primary,
			MaterialTheme.colorScheme.secondary,
			MaterialTheme.colorScheme.primary
		)
	)

	val barHeights = (0 until barCount).map { index ->
		infiniteTransition.animateFloat(
			initialValue = 0.3f,
			targetValue = 1f,
			animationSpec = infiniteRepeatable(
				animation = tween(
					durationMillis = 560 + (index * 120),
					easing = FastOutSlowInEasing
				),
				repeatMode = RepeatMode.Reverse,
				initialStartOffset = StartOffset(index * 90)
			),
			label = "waveform-bar-$index"
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
						brush = barBrush,
						shape = RoundedCornerShape(Spacing.small)
					)
			)
		}
	}
}
