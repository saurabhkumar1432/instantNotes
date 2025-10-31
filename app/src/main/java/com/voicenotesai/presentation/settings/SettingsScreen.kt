package com.voicenotesai.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.stringResource
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.voicenotesai.data.model.AIProviderType
import com.voicenotesai.domain.model.toUserMessage
import com.voicenotesai.presentation.components.GradientHeader
import com.voicenotesai.presentation.components.SettingItem
import com.voicenotesai.presentation.components.toLocalizedMessage
import com.voicenotesai.presentation.theme.ModernSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
	viewModel: SettingsViewModel = hiltViewModel(),
	onNavigateBack: () -> Unit
) {
	val uiState by viewModel.uiState.collectAsState()
	var showApiKey by remember { mutableStateOf(false) }
	var showCustomHeaders by remember { mutableStateOf(false) }
	val snackbarHostState = remember { SnackbarHostState() }

	// Handle success/error messages
	LaunchedEffect(uiState.isSaved) {
		if (uiState.isSaved) {
			snackbarHostState.showSnackbar("Settings saved successfully")
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
		containerColor = MaterialTheme.colorScheme.background,
		snackbarHost = { SnackbarHost(snackbarHostState) }
	) { paddingValues ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(paddingValues)
				.verticalScroll(rememberScrollState())
		) {
			// Gradient Header
			GradientHeader(
				title = "Settings",
				showUserAvatar = false,
				actions = {
					IconButton(onClick = onNavigateBack) {
						Icon(
							imageVector = Icons.Default.ArrowBack,
							contentDescription = "Back",
							tint = MaterialTheme.colorScheme.onPrimary
						)
					}
				}
			)
			
			Spacer(modifier = Modifier.height(ModernSpacing.sectionSpacing))
			
			// AI Models Section
			SettingsGroup(
				title = "AI MODELS",
				modifier = Modifier.padding(horizontal = ModernSpacing.screenPadding)
			) {
				AIProviderConfiguration(
					uiState = uiState,
					onProviderChanged = viewModel::onProviderChanged,
					onApiKeyChanged = viewModel::onApiKeyChanged,
					onBaseUrlChanged = viewModel::onBaseUrlChanged,
					onModelChanged = viewModel::onModelChanged,
					onCustomHeadersChanged = viewModel::onCustomHeadersChanged,
					onDiscoverModels = viewModel::discoverModels,
					onValidateConfiguration = viewModel::validateConfiguration,
					onSaveConfiguration = viewModel::saveConfiguration,
					showApiKey = showApiKey,
					onToggleApiKeyVisibility = { showApiKey = !showApiKey },
					showCustomHeaders = showCustomHeaders,
					onToggleCustomHeaders = { showCustomHeaders = !showCustomHeaders }
				)
			}
			
			Spacer(modifier = Modifier.height(ModernSpacing.sectionSpacing))
			
			// Account Section
			SettingsGroup(
				title = "ACCOUNT",
				modifier = Modifier.padding(horizontal = ModernSpacing.screenPadding)
			) {
				SettingItem(
					icon = Icons.Default.CheckCircle,
					label = "Sync Status",
					value = "Connected",
					onClick = { /* Handle sync settings */ }
				)
			}
			
			Spacer(modifier = Modifier.height(ModernSpacing.sectionSpacing))
			
			// Security Section
			SettingsGroup(
				title = "SECURITY",
				modifier = Modifier.padding(horizontal = ModernSpacing.screenPadding)
			) {
				SettingItem(
					icon = Icons.Default.CheckCircle,
					label = "Biometric Lock",
					showToggle = true,
					toggleValue = false,
					onToggleChange = { /* Handle biometric toggle */ },
					onClick = { }
				)
			}
			
			Spacer(modifier = Modifier.height(ModernSpacing.sectionSpacing))
			
			// Notifications Section
			SettingsGroup(
				title = "NOTIFICATIONS",
				modifier = Modifier.padding(horizontal = ModernSpacing.screenPadding)
			) {
				SettingItem(
					icon = Icons.Default.CheckCircle,
					label = "Push Notifications",
					showToggle = true,
					toggleValue = true,
					onToggleChange = { /* Handle notifications toggle */ },
					onClick = { }
				)
			}
			
			Spacer(modifier = Modifier.height(ModernSpacing.sectionSpacing))
			
			// Appearance Section
			SettingsGroup(
				title = "APPEARANCE",
				modifier = Modifier.padding(horizontal = ModernSpacing.screenPadding)
			) {
				SettingItem(
					icon = Icons.Default.CheckCircle,
					label = "Dark Mode",
					showToggle = true,
					toggleValue = false,
					onToggleChange = { /* Handle dark mode toggle */ },
					onClick = { }
				)
			}
			
			Spacer(modifier = Modifier.height(ModernSpacing.sectionSpacing))
			
			// Support Section
			SettingsGroup(
				title = "SUPPORT",
				modifier = Modifier.padding(horizontal = ModernSpacing.screenPadding)
			) {
				SettingItem(
					icon = Icons.Default.CheckCircle,
					label = "Help & FAQ",
					onClick = { /* Handle help */ }
				)
				
				SettingItem(
					icon = Icons.Default.CheckCircle,
					label = "Contact Support",
					onClick = { /* Handle contact */ }
				)
			}
			
			Spacer(modifier = Modifier.height(ModernSpacing.sectionSpacing))
			
			// App Version
			Text(
				text = "Version 1.0.0",
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier
					.fillMaxWidth()
					.padding(ModernSpacing.screenPadding),
				textAlign = androidx.compose.ui.text.style.TextAlign.Center
			)
			
			Spacer(modifier = Modifier.height(ModernSpacing.sectionSpacing))
		}
	}
}

@Composable
private fun SettingsGroup(
	title: String,
	modifier: Modifier = Modifier,
	content: @Composable ColumnScope.() -> Unit
) {
	Column(modifier = modifier) {
		Text(
			text = title,
			style = MaterialTheme.typography.labelSmall.copy(
				fontWeight = FontWeight.SemiBold
			),
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier.padding(
				horizontal = ModernSpacing.cardPadding,
				vertical = ModernSpacing.componentGap
			)
		)
		
		Card(
			modifier = Modifier.fillMaxWidth(),
			colors = CardDefaults.cardColors(
				containerColor = MaterialTheme.colorScheme.surface
			),
			elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
			shape = RoundedCornerShape(12.dp)
		) {
			Column(
				modifier = Modifier.padding(ModernSpacing.cardPadding),
				verticalArrangement = Arrangement.spacedBy(ModernSpacing.componentGap),
				content = content
			)
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AIProviderConfiguration(
	uiState: SettingsUiState,
	onProviderChanged: (AIProviderType) -> Unit,
	onApiKeyChanged: (String) -> Unit,
	onBaseUrlChanged: (String) -> Unit,
	onModelChanged: (String) -> Unit,
	onCustomHeadersChanged: (Map<String, String>) -> Unit,
	onDiscoverModels: () -> Unit,
	onValidateConfiguration: () -> Unit,
	onSaveConfiguration: () -> Unit,
	showApiKey: Boolean,
	onToggleApiKeyVisibility: () -> Unit,
	showCustomHeaders: Boolean,
	onToggleCustomHeaders: () -> Unit
) {
	var providerExpanded by remember { mutableStateOf(false) }
	
	Column(
		verticalArrangement = Arrangement.spacedBy(ModernSpacing.componentGap)
	) {
		// Provider Selection
		ExposedDropdownMenuBox(
			expanded = providerExpanded,
			onExpandedChange = { providerExpanded = !providerExpanded }
		) {
			OutlinedTextField(
				value = uiState.provider.getDisplayName(),
				onValueChange = {},
				readOnly = true,
				label = { Text("AI Provider") },
				trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
				modifier = Modifier
					.fillMaxWidth()
					.menuAnchor(),
				colors = TextFieldDefaults.outlinedTextFieldColors(
					containerColor = Color.Transparent
				)
			)
			ExposedDropdownMenu(
				expanded = providerExpanded,
				onDismissRequest = { providerExpanded = false }
			) {
				AIProviderType.getAllBuiltInProviders().forEach { provider ->
					DropdownMenuItem(
						text = { Text(provider.getDisplayName()) },
						onClick = {
							onProviderChanged(provider)
							providerExpanded = false
						}
					)
				}
				DropdownMenuItem(
					text = { Text("Custom") },
					onClick = {
						onProviderChanged(AIProviderType.Custom("Custom"))
						providerExpanded = false
					}
				)
			}
		}
		
		// API Key (if required)
		if (uiState.provider.requiresApiKey()) {
			OutlinedTextField(
				value = uiState.apiKey,
				onValueChange = onApiKeyChanged,
				label = { Text("API Key") },
				placeholder = { Text("Enter your API key") },
				visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
				trailingIcon = {
					IconButton(onClick = onToggleApiKeyVisibility) {
						Icon(
							imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
							contentDescription = if (showApiKey) "Hide API key" else "Show API key"
						)
					}
				},
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
				colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = Color.Transparent)
			)
		}
		
		// Base URL (for local providers and custom)
		if (uiState.provider is AIProviderType.Ollama || 
			uiState.provider is AIProviderType.LMStudio || 
			uiState.provider is AIProviderType.Custom) {
			OutlinedTextField(
				value = uiState.baseUrl,
				onValueChange = onBaseUrlChanged,
				label = { Text("Base URL") },
				placeholder = { Text(uiState.provider.getDefaultBaseUrl() ?: "Enter base URL") },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
				colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = Color.Transparent)
			)
		}
		
		// Model Selection
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(ModernSpacing.componentGap)
		) {
			OutlinedTextField(
				value = uiState.modelName,
				onValueChange = onModelChanged,
				label = { Text("Model") },
				placeholder = { Text("Enter model name (e.g., gpt-4, claude-3-sonnet, gemini-pro)") },
				modifier = Modifier.weight(1f),
				colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = Color.Transparent)
			)
			
			// Model Discovery Button (for local providers)
			if (uiState.provider is AIProviderType.Ollama || uiState.provider is AIProviderType.LMStudio) {
				IconButton(
					onClick = onDiscoverModels,
					enabled = uiState.baseUrl.isNotBlank() && !uiState.isDiscoveringModels
				) {
					if (uiState.isDiscoveringModels) {
						CircularProgressIndicator(
							modifier = Modifier.size(20.dp),
							strokeWidth = 2.dp
						)
					} else {
						Icon(
							imageVector = Icons.Default.Refresh,
							contentDescription = "Discover models"
						)
					}
				}
			}
		}
		
		// Custom Headers (for Custom provider)
		if (uiState.provider is AIProviderType.Custom) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					text = "Custom Headers",
					style = MaterialTheme.typography.bodyMedium
				)
				TextButton(onClick = onToggleCustomHeaders) {
					Text(if (showCustomHeaders) "Hide" else "Show")
				}
			}
			
			if (showCustomHeaders) {
				CustomHeadersEditor(
					headers = uiState.customHeaders,
					onHeadersChanged = onCustomHeadersChanged
				)
			}
		}
		
		// Validation Status
		if (uiState.validationStatus != ValidationStatus.NONE) {
			ValidationStatusCard(uiState = uiState)
		}
		
		// Validation Button
		Button(
			onClick = onValidateConfiguration,
			modifier = Modifier.fillMaxWidth(),
			enabled = uiState.isConfigurationComplete() && uiState.validationStatus != ValidationStatus.VALIDATING,
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
				Spacer(modifier = Modifier.width(8.dp))
			}
			Text(
				text = when (uiState.validationStatus) {
					ValidationStatus.VALIDATING -> "Validating..."
					ValidationStatus.SUCCESS -> "Validated ✓"
					ValidationStatus.FAILED -> "Retry Validation"
					else -> "Validate Configuration"
				},
				fontWeight = FontWeight.SemiBold
			)
		}
		
		// Save Button
		Button(
			onClick = onSaveConfiguration,
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
				Spacer(modifier = Modifier.width(8.dp))
			}
			Text(
				text = "Save Configuration",
				fontWeight = FontWeight.SemiBold
			)
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomHeadersEditor(
	headers: Map<String, String>,
	onHeadersChanged: (Map<String, String>) -> Unit
) {
	var newHeaderKey by remember { mutableStateOf("") }
	var newHeaderValue by remember { mutableStateOf("") }
	
	Column(
		verticalArrangement = Arrangement.spacedBy(ModernSpacing.componentGap)
	) {
		// Existing headers
		headers.forEach { (key, value) ->
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					text = "$key: $value",
					modifier = Modifier.weight(1f),
					style = MaterialTheme.typography.bodySmall
				)
				IconButton(
					onClick = {
						val newHeaders = headers.toMutableMap()
						newHeaders.remove(key)
						onHeadersChanged(newHeaders)
					}
				) {
					Icon(
						imageVector = Icons.Default.Delete,
						contentDescription = "Remove header",
						tint = MaterialTheme.colorScheme.error
					)
				}
			}
		}
		
		// Add new header
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(8.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			OutlinedTextField(
				value = newHeaderKey,
				onValueChange = { newHeaderKey = it },
				label = { Text("Header Name") },
				modifier = Modifier.weight(1f),
				singleLine = true,
				colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = Color.Transparent)
			)
			OutlinedTextField(
				value = newHeaderValue,
				onValueChange = { newHeaderValue = it },
				label = { Text("Header Value") },
				modifier = Modifier.weight(1f),
				singleLine = true,
				colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = Color.Transparent)
			)
			IconButton(
				onClick = {
					if (newHeaderKey.isNotBlank() && newHeaderValue.isNotBlank()) {
						val newHeaders = headers.toMutableMap()
						newHeaders[newHeaderKey] = newHeaderValue
						onHeadersChanged(newHeaders)
						newHeaderKey = ""
						newHeaderValue = ""
					}
				},
				enabled = newHeaderKey.isNotBlank() && newHeaderValue.isNotBlank()
			) {
				Icon(
					imageVector = Icons.Default.Add,
					contentDescription = "Add header"
				)
			}
		}
	}
}

@Composable
private fun ValidationStatusCard(uiState: SettingsUiState) {
	val colors = when (uiState.validationStatus) {
		ValidationStatus.SUCCESS -> Triple(
			MaterialTheme.colorScheme.primary,
			MaterialTheme.colorScheme.primaryContainer,
			MaterialTheme.colorScheme.onPrimaryContainer
		)
		ValidationStatus.FAILED -> Triple(
			MaterialTheme.colorScheme.error,
			MaterialTheme.colorScheme.errorContainer,
			MaterialTheme.colorScheme.onErrorContainer
		)
		ValidationStatus.VALIDATING -> Triple(
			MaterialTheme.colorScheme.secondary,
			MaterialTheme.colorScheme.secondaryContainer,
			MaterialTheme.colorScheme.onSecondaryContainer
		)
		else -> Triple(
			MaterialTheme.colorScheme.outline,
			MaterialTheme.colorScheme.surface,
			MaterialTheme.colorScheme.onSurface
		)
	}
	
	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(containerColor = colors.second),
		border = BorderStroke(1.dp, colors.first)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(ModernSpacing.cardPadding),
			horizontalArrangement = Arrangement.spacedBy(ModernSpacing.componentGap),
			verticalAlignment = Alignment.CenterVertically
		) {
			when (uiState.validationStatus) {
				ValidationStatus.VALIDATING -> {
					CircularProgressIndicator(
						modifier = Modifier.size(24.dp),
						strokeWidth = 2.dp,
						color = colors.first
					)
				}
				ValidationStatus.SUCCESS -> {
					Icon(
						imageVector = Icons.Default.CheckCircle,
						contentDescription = null,
						tint = colors.first,
						modifier = Modifier.size(24.dp)
					)
				}
				ValidationStatus.FAILED -> {
					Icon(
						imageVector = Icons.Default.Close,
						contentDescription = null,
						tint = colors.first,
						modifier = Modifier.size(24.dp)
					)
				}
				else -> {}
			}
			
			Text(
				text = uiState.validationMessage,
				style = MaterialTheme.typography.bodyMedium,
				color = colors.third,
				fontWeight = FontWeight.Medium
			)
		}
	}
}


