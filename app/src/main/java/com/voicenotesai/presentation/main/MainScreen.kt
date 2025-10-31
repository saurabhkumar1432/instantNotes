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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.stringResource
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
import com.voicenotesai.presentation.components.toLocalizedMessage
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
import com.voicenotesai.presentation.components.GradientHeader
import com.voicenotesai.presentation.components.StatsCard
import com.voicenotesai.presentation.components.NoteCard
import com.voicenotesai.presentation.components.WaveformVisualizer
import com.voicenotesai.presentation.theme.ExtendedTypography
import com.voicenotesai.presentation.theme.Spacing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.background
import com.voicenotesai.data.local.entity.Note
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
	viewModel: MainViewModel = hiltViewModel(),
	onNavigateToSettings: () -> Unit,
	onNavigateToNotes: () -> Unit,
	onNavigateToAnalytics: () -> Unit = {},
	onNavigateToTasks: () -> Unit = {}
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
							text = stringResource(id = com.voicenotesai.R.string.instant_notes_title),
							style = MaterialTheme.typography.titleLarge,
							fontWeight = FontWeight.SemiBold
						)
						Text(
							text = stringResource(id = com.voicenotesai.R.string.instant_notes_subtitle),
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
					val viewSavedNotesDesc = stringResource(id = com.voicenotesai.R.string.view_saved_notes)
					IconButton(
						onClick = {
							haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
							onNavigateToNotes()
						},
						modifier = Modifier
							.semantics {
									contentDescription = viewSavedNotesDesc
								}
					) {
						Icon(
							imageVector = Icons.Default.List,
							contentDescription = null // Already provided by parent
						)
					}
					val openAISettingsDesc = stringResource(id = com.voicenotesai.R.string.open_ai_settings)
					IconButton(
						onClick = {
							haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
							onNavigateToSettings()
						},
						modifier = Modifier
							.semantics {
									contentDescription = openAISettingsDesc
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
						onNavigateToAnalytics = onNavigateToAnalytics,
						onNavigateToTasks = onNavigateToTasks,
						onNavigateToNotes = onNavigateToNotes,
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
						},
						onNavigateToAnalytics = onNavigateToAnalytics,
						onNavigateToTasks = onNavigateToTasks,
						onNavigateToNotes = onNavigateToNotes
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
					val localized = error.toLocalizedMessage()
					val errorMessage = stringResource(id = localized.resId, *localized.args)
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
			title = { Text(stringResource(id = com.voicenotesai.R.string.permission_dialog_title)) },
			text = {
				Text(stringResource(id = com.voicenotesai.R.string.permission_dialog_text))
			},
			confirmButton = {
				TextButton(
					onClick = {
						showPermissionRationale = false
						microphonePermissionState.launchPermissionRequest()
					}
				) {
					Text(stringResource(id = com.voicenotesai.R.string.allow_access))
				}
			},
			dismissButton = {
				TextButton(onClick = { showPermissionRationale = false }) {
					Text(stringResource(id = com.voicenotesai.R.string.not_now))
				}
			}
		)
	}

	if (showPermissionDeniedDialog) {
		AlertDialog(
			onDismissRequest = { showPermissionDeniedDialog = false },
			title = { Text(stringResource(id = com.voicenotesai.R.string.permission_denied_title)) },
			text = {
				Text(stringResource(id = com.voicenotesai.R.string.permission_denied_text))
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
					Text(stringResource(id = com.voicenotesai.R.string.open_settings))
				}
			},
			dismissButton = {
				TextButton(onClick = { showPermissionDeniedDialog = false }) {
					Text(stringResource(id = com.voicenotesai.R.string.cancel))
				}
			}
		)
	}
}

@Composable
private fun IdleContent(
	onRecordClick: () -> Unit,
	modifier: Modifier = Modifier,
	viewModel: MainViewModel = hiltViewModel(),
	onNavigateToAnalytics: () -> Unit = {},
	onNavigateToTasks: () -> Unit = {},
	onNavigateToNotes: () -> Unit = {}
) {
	val notes by viewModel.getAllRecentNotes().collectAsState(initial = emptyList())
	val pendingTasksCount by viewModel.getPendingTasksCount().collectAsState(initial = 0)
	var searchQuery by remember { mutableStateOf("") }
	
	Scaffold(
		containerColor = MaterialTheme.colorScheme.background,
		floatingActionButton = {
			FloatingActionButton(
				onClick = onRecordClick,
				containerColor = MaterialTheme.colorScheme.primary,
				modifier = Modifier.size(64.dp)
			) {
				Icon(
					imageVector = Icons.Default.Add,
					contentDescription = stringResource(id = com.voicenotesai.R.string.start_voice_recording_description),
					modifier = Modifier.size(32.dp)
				)
			}
		}
	) { paddingValues ->
		Column(
			modifier = modifier
				.fillMaxSize()
				.padding(paddingValues)
		) {
			// Gradient Header with Search
			GradientHeader(
				title = stringResource(id = com.voicenotesai.R.string.instant_notes_title),
				showUserAvatar = true,
				userInitials = "JD", // TODO: Get from user profile
				showSearch = true,
				searchQuery = searchQuery,
				onSearchQueryChange = { searchQuery = it },
				searchPlaceholder = stringResource(id = com.voicenotesai.R.string.search_notes_placeholder),
				actions = {
					IconButton(onClick = onNavigateToAnalytics) {
						Icon(
							imageVector = Icons.Default.BarChart,
							contentDescription = "Analytics",
							tint = MaterialTheme.colorScheme.onPrimary
						)
					}
				}
			)
			
			// Stats Cards
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(16.dp),
				horizontalArrangement = Arrangement.spacedBy(12.dp)
			) {
				StatsCard(
					value = notes.size.toString(),
					label = "Notes",
					modifier = Modifier
						.weight(1f)
						.clickable { onNavigateToNotes() }
				)
				StatsCard(
					value = pendingTasksCount.toString(),
					label = "Tasks",
					valueColor = MaterialTheme.colorScheme.tertiary,
					modifier = Modifier
						.weight(1f)
						.clickable { onNavigateToTasks() }
				)
				StatsCard(
					value = getThisWeekCount(notes).toString(),
					label = "This Week",
					valueColor = MaterialTheme.colorScheme.secondary,
					modifier = Modifier.weight(1f)
				)
			}
			
			// Recent Notes Section
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f)
					.padding(horizontal = 16.dp)
			) {
				Text(
					text = stringResource(id = com.voicenotesai.R.string.recent_notes_header),
					style = MaterialTheme.typography.labelSmall.copy(
						fontWeight = FontWeight.SemiBold
					),
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.padding(bottom = 12.dp)
				)
				
				if (notes.isEmpty()) {
					// Empty state
					Box(
						modifier = Modifier
							.fillMaxSize()
							.padding(vertical = 48.dp),
						contentAlignment = Alignment.Center
					) {
						Column(
							horizontalAlignment = Alignment.CenterHorizontally,
							verticalArrangement = Arrangement.spacedBy(16.dp)
						) {
							Icon(
								imageVector = Icons.Default.Description,
								contentDescription = null,
								modifier = Modifier.size(64.dp),
								tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
							)
							Text(
								text = stringResource(id = com.voicenotesai.R.string.no_notes_yet),
								style = MaterialTheme.typography.bodyLarge,
								color = MaterialTheme.colorScheme.onSurfaceVariant
							)
							Text(
								text = stringResource(id = com.voicenotesai.R.string.tap_plus_to_record),
								style = MaterialTheme.typography.bodyMedium,
								color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
							)
						}
					}
				} else {
					LazyColumn(
						verticalArrangement = Arrangement.spacedBy(12.dp)
					) {
						items(notes.filter { note ->
							if (searchQuery.isBlank()) true
							else note.content.contains(searchQuery, ignoreCase = true) ||
								(note.transcribedText?.contains(searchQuery, ignoreCase = true) == true)
						}) { note ->
							NoteCard(
								title = extractTitle(note.content),
								preview = note.content,
								duration = formatTimeAgo(note.timestamp),
								tags = extractTags(note.content),
								onClick = { /* TODO: Navigate to note detail */ },
								onMoreClick = { /* TODO: Show options menu */ }
							)
						}
					}
				}
			}
		}
	}
}

// Helper functions for stats and formatting
private fun calculateTotalDuration(notes: List<Note>): String {
	val totalMs: Long = notes.sumOf { note ->
		note.duration
	}
	val hours = totalMs / 3600000
	val minutes = (totalMs % 3600000) / 60000
	return if (hours > 0) "${hours}h ${minutes}m" else if (minutes > 0) "${minutes}m" else "0m"
}

private fun getThisWeekCount(notes: List<Note>): Int {
	val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
	return notes.count { it.timestamp >= weekAgo }
}

private fun extractTitle(content: String): String {
	return content.lines().firstOrNull()?.take(50) ?: "Untitled Note"
}

private fun formatTimeAgo(timestampMs: Long): String {
	val now = System.currentTimeMillis()
	val diff = now - timestampMs
	
	return when {
		diff < 60000 -> "Just now"
		diff < 3600000 -> "${diff / 60000}m ago"
		diff < 86400000 -> "${diff / 3600000}h ago"
		diff < 604800000 -> "${diff / 86400000}d ago"
		else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestampMs))
	}
}

private fun extractTags(content: String): List<String> {
	// For now, return simple tags based on content analysis
	// In a real app, you might parse the tags field from the Note entity
	val tags = mutableListOf<String>()
	
	// Add tags based on content analysis
	when {
		content.contains("meeting", ignoreCase = true) -> tags.add("meeting")
		content.contains("idea", ignoreCase = true) -> tags.add("idea")
		content.contains("task", ignoreCase = true) -> tags.add("task")
		content.contains("reminder", ignoreCase = true) -> tags.add("reminder")
	}
	
	// Add work/personal tags based on keywords
	when {
		content.contains("work", ignoreCase = true) || 
		content.contains("project", ignoreCase = true) -> tags.add("work")
		content.contains("personal", ignoreCase = true) || 
		content.contains("home", ignoreCase = true) -> tags.add("personal")
	}
	
	return tags.take(3) // Limit to 3 tags
}

// Keep the old IdleContent for fallback
@Composable
private fun OldIdleContent(
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
			text = stringResource(id = com.voicenotesai.R.string.capture_headline),
			style = MaterialTheme.typography.headlineLarge,
			fontWeight = FontWeight.SemiBold,
			textAlign = TextAlign.Center
		)

		Spacer(modifier = Modifier.height(Spacing.small))

		Text(
			text = stringResource(id = com.voicenotesai.R.string.capture_description),
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
					title = stringResource(id = com.voicenotesai.R.string.feature_structured_title),
					subtitle = stringResource(id = com.voicenotesai.R.string.feature_structured_sub)
				)
				FeatureRow(
					icon = Icons.Filled.Schedule,
					title = stringResource(id = com.voicenotesai.R.string.feature_meeting_title),
					subtitle = stringResource(id = com.voicenotesai.R.string.feature_meeting_sub)
				)
				FeatureRow(
					icon = Icons.Filled.Lock,
					title = stringResource(id = com.voicenotesai.R.string.feature_privacy_title),
					subtitle = stringResource(id = com.voicenotesai.R.string.feature_privacy_sub)
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
			InsightChip(icon = Icons.Filled.Bolt, label = stringResource(id = com.voicenotesai.R.string.insight_realtime))
			InsightChip(icon = Icons.Filled.TaskAlt, label = stringResource(id = com.voicenotesai.R.string.insight_actions))
			InsightChip(icon = Icons.Filled.Lock, label = stringResource(id = com.voicenotesai.R.string.insight_enterprise))
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
			text = stringResource(id = com.voicenotesai.R.string.review_share_description),
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
					text = stringResource(id = com.voicenotesai.R.string.microphone_access_required),
					style = MaterialTheme.typography.headlineSmall,
					fontWeight = FontWeight.SemiBold
				)
				Text(
					text = stringResource(id = com.voicenotesai.R.string.microphone_access_desc),
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}

		Button(
			onClick = onRequestPermission,
			modifier = Modifier.fillMaxWidth()
		) {
			Text(stringResource(id = com.voicenotesai.R.string.allow_microphone_button))
		}

		OutlinedButton(
			onClick = onOpenSettings,
			modifier = Modifier.fillMaxWidth()
		) {
			Text(stringResource(id = com.voicenotesai.R.string.open_app_settings))
		}

		Spacer(modifier = Modifier.height(Spacing.extraLarge))
	}
}

@Composable
private fun RecordingContent(
	duration: Long,
	onStopClick: () -> Unit
) {
	// Pulsing animation for recording indicator
	val infiniteTransition = rememberInfiniteTransition(label = "pulse")
	val pulseAlpha by infiniteTransition.animateFloat(
		initialValue = 0.3f,
		targetValue = 1f,
		animationSpec = infiniteRepeatable(
			animation = tween(800, easing = FastOutSlowInEasing),
			repeatMode = RepeatMode.Reverse
		),
		label = "pulseAlpha"
	)

	Column(
		modifier = Modifier.fillMaxSize(),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		// Gradient Header
		GradientHeader(
			title = stringResource(id = com.voicenotesai.R.string.new_recording_title)
		)
		
		Spacer(modifier = Modifier.weight(1f))

		// Large timer display (60sp monospace)
		Text(
			text = formatDuration(duration),
			style = MaterialTheme.typography.displayLarge.copy(
				fontSize = 60.sp,
				fontFamily = FontFamily.Monospace,
				fontWeight = FontWeight.Bold
			),
			color = MaterialTheme.colorScheme.primary
		)

		Spacer(modifier = Modifier.height(32.dp))

		// Waveform visualization with 20 gradient bars
		WaveformVisualizer(
			isActive = true,
			barCount = 20,
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp)
		)

		Spacer(modifier = Modifier.height(48.dp))

		// Pulsing red dot with "Recording..." text
		Row(
			horizontalArrangement = Arrangement.Center,
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier.padding(bottom = 24.dp)
		) {
			Box(
				modifier = Modifier
					.size(12.dp)
					.alpha(pulseAlpha)
					.background(
						color = MaterialTheme.colorScheme.error,
						shape = CircleShape
					)
			)
			
			Spacer(modifier = Modifier.width(12.dp))
			
			Text(
				text = stringResource(id = com.voicenotesai.R.string.recording_in_progress),
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.error.copy(alpha = pulseAlpha),
				fontWeight = FontWeight.SemiBold
			)
		}
		
		Spacer(modifier = Modifier.weight(1f))

		// Single 64dp red stop button
		Button(
			onClick = onStopClick,
			modifier = Modifier.size(64.dp),
			shape = CircleShape,
			colors = ButtonDefaults.buttonColors(
				containerColor = MaterialTheme.colorScheme.error,
				contentColor = MaterialTheme.colorScheme.onError
			),
			contentPadding = PaddingValues(0.dp)
		) {
			Icon(
				imageVector = Icons.Default.Stop,
				contentDescription = "Stop Recording",
				modifier = Modifier.size(32.dp)
			)
		}

		Spacer(modifier = Modifier.height(48.dp))
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
					text = stringResource(id = com.voicenotesai.R.string.processing_subtext),
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
			text = stringResource(id = com.voicenotesai.R.string.notes_ready),
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

	val copyGeneratedDesc = stringResource(id = com.voicenotesai.R.string.copy_generated_notes_description)
	FilledTonalButton(
			onClick = {
				haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
				onCopy()
			},
				modifier = Modifier
				.fillMaxWidth()
				.semantics {
					contentDescription = copyGeneratedDesc
				}
		) {
			Text(
				stringResource(id = com.voicenotesai.R.string.copy_to_clipboard),
				style = ExtendedTypography.buttonText
			)
		}

	val shareGeneratedDesc = stringResource(id = com.voicenotesai.R.string.share_generated_notes_description)
	FilledTonalButton(
			onClick = {
				haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
				onShare()
			},
				modifier = Modifier
				.fillMaxWidth()
				.semantics {
					contentDescription = shareGeneratedDesc
				}
		) {
			Text(
				stringResource(id = com.voicenotesai.R.string.share),
				style = ExtendedTypography.buttonText
			)
		}

	val startVoiceDesc = stringResource(id = com.voicenotesai.R.string.start_voice_recording_description)
	Button(
			onClick = {
				haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
				onNewRecording()
			},
				modifier = Modifier
				.fillMaxWidth()
				.semantics {
					contentDescription = startVoiceDesc
				}
		) {
			Text(
				stringResource(id = com.voicenotesai.R.string.start_new_recording),
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
						contentDescription = title, // Describe the icon by its feature title for assistive tech
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
					contentDescription = label, // Describe the insight icon with the chip label
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
	val label = context.getString(com.voicenotesai.R.string.generated_notes_label)
	val clip = ClipData.newPlainText(label, text)
	clipboard.setPrimaryClip(clip)
}

private fun shareNotes(context: Context, notes: String) {
	val intent = Intent(Intent.ACTION_SEND).apply {
		type = "text/plain"
		putExtra(Intent.EXTRA_TEXT, notes)
		putExtra(Intent.EXTRA_SUBJECT, context.getString(com.voicenotesai.R.string.share_subject))
	}
	val chooserTitle = context.getString(com.voicenotesai.R.string.share_chooser_title)
	context.startActivity(Intent.createChooser(intent, chooserTitle))
}
