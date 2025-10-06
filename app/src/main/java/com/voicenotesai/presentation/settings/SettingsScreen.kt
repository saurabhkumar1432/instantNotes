package com.voicenotesai.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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

    // Show success message
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            snackbarHostState.showSnackbar("Settings saved successfully!")
            kotlinx.coroutines.delay(2000)
            viewModel.clearSavedState()
        }
    }

    // Show error message
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
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            // Header
            Text(
                text = "Configure your AI provider settings",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Provider Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = uiState.provider.name.replace("_", " "),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("AI Provider") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
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

            // API Key Input
            OutlinedTextField(
                value = uiState.apiKey,
                onValueChange = { viewModel.onApiKeyChanged(it) },
                label = { Text("API Key") },
                placeholder = { Text("Enter your API key") },
                visualTransformation = if (showApiKey) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Text(if (showApiKey) "Hide" else "Show")
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Model Name Input
            OutlinedTextField(
                value = uiState.model,
                onValueChange = { viewModel.onModelChanged(it) },
                label = { Text("Model Name") },
                placeholder = { Text("e.g., gpt-4, claude-3-opus-20240229") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Helper text based on provider
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    Text(
                        text = "Model Examples:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = when (uiState.provider) {
                            AIProvider.OPENAI -> "• gpt-4\n• gpt-3.5-turbo"
                            AIProvider.ANTHROPIC -> "• claude-3-opus-20240229\n• claude-3-sonnet-20240229"
                            AIProvider.GOOGLE_AI -> "• gemini-pro\n• gemini-1.5-pro"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            // Validation Section
            if (uiState.validationStatus != ValidationStatus.NONE) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (uiState.validationStatus) {
                            ValidationStatus.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
                            ValidationStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                            ValidationStatus.VALIDATING -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.medium),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (uiState.validationStatus) {
                            ValidationStatus.VALIDATING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            ValidationStatus.SUCCESS -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            ValidationStatus.FAILED -> {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            else -> {}
                        }
                        
                        Text(
                            text = uiState.validationMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = when (uiState.validationStatus) {
                                ValidationStatus.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
                                ValidationStatus.FAILED -> MaterialTheme.colorScheme.onErrorContainer
                                ValidationStatus.VALIDATING -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.small))
            }
            
            // Validate Button
            OutlinedButton(
                onClick = { viewModel.validateApiKey() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.apiKey.isNotBlank() && 
                         uiState.model.isNotBlank() && 
                         uiState.validationStatus != ValidationStatus.VALIDATING
            ) {
                if (uiState.validationStatus == ValidationStatus.VALIDATING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Spacing.large),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(Spacing.small))
                }
                Text("Test API Key & Model")
            }
            
            Spacer(modifier = Modifier.height(Spacing.small))

            // Save Button
            Button(
                onClick = { viewModel.saveSettings() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && uiState.validationStatus == ValidationStatus.SUCCESS
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Spacing.large),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save Settings")
                }
            }
            
            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    Text(
                        text = "ℹ️ Important",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "You must test and validate your API key and model before you can start using the app. This ensures your credentials are correct and the selected model is accessible.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Success message
            if (uiState.isSaved) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "Settings saved successfully!",
                        modifier = Modifier.padding(Spacing.medium),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
