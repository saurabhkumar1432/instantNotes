package com.voicenotesai.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.voicenotesai.data.model.AIProvider
import com.voicenotesai.domain.model.toUserMessage
import com.voicenotesai.presentation.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
	viewModel: SettingsViewModel = hiltViewModel(),
	onNavigateBack: () -> Unit
) {
	val uiState by viewModel.uiState.collectAsState()
	var showApiKey by remember { mutableStateOf(false) }
	var expanded by remember { mutableStateOf(false) }
	val snackbarHostState = remember { SnackbarHostState() }

	LaunchedEffect(uiState.isSaved) {
		if (uiState.isSaved) {
			snackbarHostState.showSnackbar("Settings saved successfully!")
			kotlinx.coroutines.delay(2000)
			viewModel.clearSavedState()
		}
	}

	LaunchedEffect(uiState.error) {
		uiState.error?.let { error ->
			snackbarHostState.showSnackbar(
				message = error.toUserMessage(),
				duration = SnackbarDuration.Long
			)
			viewModel.clearError()
		}
	}

	Scaffold(
		containerColor = Color.Transparent,
		topBar = {
			TopAppBar(
				title = {
					Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
						Text(
							text = "AI provider settings",
							style = MaterialTheme.typography.titleLarge
						)
						Text(
							text = "Connect your AI provider so Instant Notes can generate structured summaries.",
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
						)
					}
				},
				navigationIcon = {
					IconButton(onClick = onNavigateBack) {
						Icon(
							imageVector = Icons.Default.ArrowBack,
							contentDescription = "Back"
						)
					}
				},
				colors = TopAppBarDefaults.topAppBarColors(
					containerColor = Color.Transparent,
					titleContentColor = MaterialTheme.colorScheme.onBackground,
					navigationIconContentColor = MaterialTheme.colorScheme.onBackground
				)
			)
		},
		snackbarHost = { SnackbarHost(snackbarHostState) }
	) { paddingValues ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(paddingValues)
				.verticalScroll(rememberScrollState())
				.padding(horizontal = Spacing.large, vertical = Spacing.extraLarge),
			verticalArrangement = Arrangement.spacedBy(Spacing.large)
		) {
			SettingsSection(
				title = "Connection",
				description = "Select your AI provider and authenticate with your API key."
			) {
				ExposedDropdownMenuBox(
					expanded = expanded,
					onExpandedChange = { expanded = !expanded }
				) {
					OutlinedTextField(
						value = uiState.provider.name.replace("_", " "),
						onValueChange = {},
						readOnly = true,
						label = { Text("AI provider") },
						trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
						modifier = Modifier
							.fillMaxWidth()
							.menuAnchor(),
						colors = TextFieldDefaults.outlinedTextFieldColors(
							containerColor = Color.Transparent
						)
					)
					ExposedDropdownMenu(
						expanded = expanded,
						onDismissRequest = { expanded = false }
					) {
						AIProvider.entries.forEach { provider ->
							DropdownMenuItem(
								text = { Text(provider.name.replace("_", " ")) },
								onClick = {
									viewModel.onProviderChanged(provider)
									expanded = false
								}
							)
						}
					}
				}

				OutlinedTextField(
					value = uiState.apiKey,
					onValueChange = { viewModel.onApiKeyChanged(it) },
					label = { Text("API key") },
					placeholder = { Text("Paste the secret from your provider console") },
					visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
					trailingIcon = {
						TextButton(onClick = { showApiKey = !showApiKey }) {
							Text(if (showApiKey) "Hide" else "Show")
						}
					},
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
					modifier = Modifier.fillMaxWidth(),
					singleLine = true,
					colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = Color.Transparent)
				)

				OutlinedTextField(
					value = uiState.model,
					onValueChange = { viewModel.onModelChanged(it) },
					label = { Text("Model name") },
					placeholder = { Text("Example: gpt-4o, claude-3-opus-20240229") },
					modifier = Modifier.fillMaxWidth(),
					singleLine = true,
					colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = Color.Transparent)
				)
			}

			SettingsSection(
				title = "Model guidance",
				description = "Use one of these production-tested model identifiers for the best transcription and summarization results."
			) {
				ModelRecommendations(provider = uiState.provider)
			}

			if (uiState.validationStatus != ValidationStatus.NONE) {
				ValidationBanner(uiState)
			}

			Button(
				onClick = { viewModel.validateApiKey() },
				modifier = Modifier.fillMaxWidth(),
				enabled = uiState.apiKey.isNotBlank() && uiState.model.isNotBlank() && uiState.validationStatus != ValidationStatus.VALIDATING,
				colors = ButtonDefaults.buttonColors(
					containerColor = MaterialTheme.colorScheme.secondaryContainer,
					contentColor = MaterialTheme.colorScheme.onSecondaryContainer
				)
			) {
				if (uiState.validationStatus == ValidationStatus.VALIDATING) {
					CircularProgressIndicator(
						modifier = Modifier.size(20.dp),
						strokeWidth = 2.dp
					)
					Spacer(modifier = Modifier.width(Spacing.small))
				}
				Text("Validate credentials", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
			}

			Button(
				onClick = { viewModel.saveSettings() },
				modifier = Modifier.fillMaxWidth(),
				enabled = !uiState.isLoading && uiState.validationStatus == ValidationStatus.SUCCESS,
				colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
			) {
				if (uiState.isLoading) {
					CircularProgressIndicator(
						modifier = Modifier.size(20.dp),
						color = MaterialTheme.colorScheme.onPrimary,
						strokeWidth = 2.dp
					)
					Spacer(modifier = Modifier.width(Spacing.small))
				}
				Text("Save configuration", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
			}

			SettingsSection(
				title = "Why we validate",
				description = "We run a quick request before recording so you never lose a note to invalid credentials."
			) {
				Text(
					text = "The check confirms your API key and model are accessible. You can re-run validation any time after updating your provider settings.",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}

			if (uiState.isSaved) {
				Surface(
					modifier = Modifier.fillMaxWidth(),
					shape = RoundedCornerShape(20.dp),
					color = MaterialTheme.colorScheme.primaryContainer,
					tonalElevation = 0.dp
				) {
					Text(
						text = "Settings saved successfully.",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onPrimaryContainer,
						modifier = Modifier.padding(Spacing.medium)
					)
				}
			}
		}
	}
}

@Composable
private fun SettingsSection(
	title: String,
	description: String? = null,
	content: @Composable ColumnScope.() -> Unit
) {
	Surface(
		modifier = Modifier.fillMaxWidth(),
		shape = RoundedCornerShape(24.dp),
		tonalElevation = 4.dp
	) {
		Column(
			modifier = Modifier.padding(Spacing.large),
			verticalArrangement = Arrangement.spacedBy(Spacing.medium),
			content = {
				Text(
					text = title,
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.SemiBold
				)
				description?.let {
					Text(
						text = it,
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
				content()
			}
		)
	}
}

@Composable
private fun ModelRecommendations(provider: AIProvider) {
	val recommendations = when (provider) {
		AIProvider.OPENAI -> listOf("gpt-4", "gpt-4o", "gpt-3.5-turbo")
		AIProvider.ANTHROPIC -> listOf("claude-3-opus-20240229", "claude-3-sonnet-20240229", "claude-3-haiku-20240307")
		AIProvider.GOOGLE_AI -> listOf("gemini-1.5-pro", "gemini-1.5-flash", "gemini-pro")
	}

	Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
		recommendations.forEach { model ->
			Text(
				text = "- $model",
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}
}

@Composable
private fun ValidationBanner(uiState: SettingsUiState) {
	data class ColorSet(
		val borderColor: Color,
		val bgColor: Color,
		val content: Color,
		val iconTint: Color
	)
	
	val colors = when (uiState.validationStatus) {
		ValidationStatus.SUCCESS -> ColorSet(
			MaterialTheme.colorScheme.primary,
			MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
			MaterialTheme.colorScheme.onPrimaryContainer,
			MaterialTheme.colorScheme.primary
		)
		ValidationStatus.FAILED -> ColorSet(
			MaterialTheme.colorScheme.error,
			MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
			MaterialTheme.colorScheme.onErrorContainer,
			MaterialTheme.colorScheme.error
		)
		ValidationStatus.VALIDATING -> ColorSet(
			MaterialTheme.colorScheme.secondary,
			MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
			MaterialTheme.colorScheme.onSurfaceVariant,
			MaterialTheme.colorScheme.secondary
		)
		else -> ColorSet(
			MaterialTheme.colorScheme.outline,
			MaterialTheme.colorScheme.surface,
			MaterialTheme.colorScheme.onSurface,
			MaterialTheme.colorScheme.onSurface
		)
	}

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.border(
				width = 3.dp,
				color = colors.borderColor,
				shape = RoundedCornerShape(20.dp)
			)
			.background(
				color = colors.bgColor,
				shape = RoundedCornerShape(20.dp)
			)
			.padding(Spacing.large),
		verticalArrangement = Arrangement.spacedBy(Spacing.medium)
	) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
		) {
			when (uiState.validationStatus) {
				ValidationStatus.VALIDATING -> {
					CircularProgressIndicator(
						modifier = Modifier.size(28.dp),
						strokeWidth = 3.dp,
						color = colors.iconTint
					)
				}
				ValidationStatus.SUCCESS -> {
					Icon(
						imageVector = Icons.Default.CheckCircle,
						contentDescription = null,
						tint = colors.iconTint,
						modifier = Modifier.size(28.dp)
					)
				}
				ValidationStatus.FAILED -> {
					Icon(
						imageVector = Icons.Default.Close,
						contentDescription = null,
						tint = colors.iconTint,
						modifier = Modifier.size(28.dp)
					)
				}
				else -> {}
			}
			Text(
				text = uiState.validationMessage,
				style = MaterialTheme.typography.bodyLarge,
				color = colors.content,
				fontWeight = FontWeight.Bold
			)
		}
	}
}
