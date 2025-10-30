package com.voicenotesai.presentation.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.voicenotesai.presentation.theme.ExtendedTypography
import com.voicenotesai.presentation.theme.Spacing

@OptIn(ExperimentalPermissionsApi::class)
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun OnboardingScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToMain: () -> Unit,
    onStartFirstRecording: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    
    val microphonePermissionState = rememberPermissionState(
        permission = Manifest.permission.RECORD_AUDIO
    )
    
    // Update permission status when it changes
    LaunchedEffect(microphonePermissionState.status) {
        viewModel.updatePermissionStatus(microphonePermissionState.status.isGranted)
    }
    
    // Handle completion
    LaunchedEffect(uiState.currentStep) {
        if (uiState.currentStep == OnboardingStep.Completed) {
            onNavigateToMain()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            OnboardingTopBar(
                currentStep = uiState.currentStep,
                canGoBack = OnboardingConfig.getPreviousStep(uiState.currentStep) != null,
                onBackClick = { viewModel.previousStep() },
                onSkipClick = { viewModel.showSkipDialog() }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.large)
            ) {
                // Progress indicator
                if (OnboardingConfig.getPageForStep(uiState.currentStep)?.showProgress == true) {
                    ProgressIndicator(
                        currentStep = uiState.currentStep,
                        modifier = Modifier.padding(vertical = Spacing.medium)
                    )
                }
                
                // Main content with page transitions
                AnimatedContent(
                    targetState = uiState.currentStep,
                    transitionSpec = {
                        val slideDirection = if (OnboardingConfig.getStepIndex(targetState) > 
                            OnboardingConfig.getStepIndex(initialState)) 1 else -1
                        
                        slideInHorizontally(
                            initialOffsetX = { it * slideDirection },
                            animationSpec = tween(300)
                        ) + fadeIn(animationSpec = tween(300)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { -it * slideDirection },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    label = "onboarding-page-transition"
                ) { step ->
                    OnboardingConfig.getPageForStep(step)?.let { page ->
                        OnboardingPageContent(
                            page = page,
                            uiState = uiState,
                            onPrimaryAction = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                handlePrimaryAction(
                                    step = step,
                                    viewModel = viewModel,
                                    microphonePermissionState = microphonePermissionState,
                                    onNavigateToSettings = onNavigateToSettings,
                                    onStartFirstRecording = onStartFirstRecording
                                )
                            },
                            onSecondaryAction = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.nextStep()
                            }
                        )
                    }
                }
            }
            
            // Skip confirmation dialog
            if (uiState.showSkipDialog) {
                SkipConfirmationDialog(
                    onConfirm = { viewModel.skipOnboarding() },
                    onDismiss = { viewModel.hideSkipDialog() }
                )
            }
            
            // Error message
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(Spacing.medium),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(Spacing.medium),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
private fun OnboardingTopBar(
    currentStep: OnboardingStep,
    canGoBack: Boolean,
    onBackClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    TopAppBar(
        title = { },
        navigationIcon = {
            if (canGoBack) {
                val goBackDesc = stringResource(id = com.voicenotesai.R.string.go_back_description)
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBackClick()
                    },
                    modifier = Modifier.semantics {
                        contentDescription = goBackDesc
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null
                    )
                }
            }
        },
        actions = {
            if (currentStep != OnboardingStep.Welcome) {
                val skipOnboardingDesc = stringResource(id = com.voicenotesai.R.string.skip_onboarding_description)
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSkipClick()
                    },
                    modifier = Modifier.semantics {
                        contentDescription = skipOnboardingDesc
                    }
                ) {
                    Text(
                        stringResource(id = com.voicenotesai.R.string.skip),
                        style = ExtendedTypography.buttonText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
private fun ProgressIndicator(
    currentStep: OnboardingStep,
    modifier: Modifier = Modifier
) {
    val currentIndex = OnboardingConfig.getStepIndex(currentStep)
    val progress by animateFloatAsState(
        targetValue = (currentIndex.toFloat() / OnboardingConfig.totalSteps),
        animationSpec = tween(durationMillis = 300),
        label = "progress-animation"
    )
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = com.voicenotesai.R.string.progress_step_format, currentIndex + 1, OnboardingConfig.totalSteps),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${((progress * 100).toInt())}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(Spacing.small))
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    uiState: OnboardingUiState,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = Spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(Spacing.extraLarge))
        
        // Icon (if provided)
        page.icon?.let { icon ->
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .padding(Spacing.large),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.extraLarge))
        }
        
        // Title
        Text(
            text = stringResource(id = page.titleRes),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(Spacing.medium))
        
        // Subtitle
        Text(
            text = stringResource(id = page.subtitleRes),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(Spacing.large))
        
        // Description
        Text(
            text = stringResource(id = page.descriptionRes),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
        )
        
        Spacer(modifier = Modifier.height(Spacing.huge))
        
        // Action buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            val primaryButtonDesc = stringResource(id = page.primaryButtonTextRes) + " button"
            Button(
                onClick = onPrimaryAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = primaryButtonDesc
                    },
                enabled = uiState.canProceed && !uiState.isLoading
            ) {
                Text(
                    text = stringResource(id = page.primaryButtonTextRes),
                    style = ExtendedTypography.buttonText
                )
            }
            
            page.secondaryButtonTextRes?.let { secondaryRes ->
                val secondaryButtonDesc = stringResource(id = secondaryRes) + " button"
                OutlinedButton(
                    onClick = onSecondaryAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = secondaryButtonDesc
                        }
                ) {
                    Text(
                        text = stringResource(id = secondaryRes),
                        style = ExtendedTypography.buttonText
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.extraLarge))
    }
}

@Composable
private fun SkipConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(id = com.voicenotesai.R.string.skip_setup_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                stringResource(id = com.voicenotesai.R.string.skip_setup_text)
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(id = com.voicenotesai.R.string.skip))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = com.voicenotesai.R.string.continue_setup))
            }
        }
    )
}

@OptIn(ExperimentalPermissionsApi::class)
private fun handlePrimaryAction(
    step: OnboardingStep,
    viewModel: OnboardingViewModel,
    microphonePermissionState: com.google.accompanist.permissions.PermissionState,
    onNavigateToSettings: () -> Unit,
    onStartFirstRecording: () -> Unit
) {
    when (step) {
        OnboardingStep.Welcome -> {
            viewModel.nextStep()
        }
        OnboardingStep.PermissionEducation -> {
            // Handle permission request
            if (microphonePermissionState.status.isGranted) {
                viewModel.nextStep()
            } else {
                microphonePermissionState.launchPermissionRequest()
            }
        }
        OnboardingStep.AIProviderSetup -> {
            onNavigateToSettings()
        }
        OnboardingStep.FirstRecording -> {
            onStartFirstRecording()
            viewModel.completeOnboarding()
        }
        OnboardingStep.Completed -> {
            // Should not reach here
        }
    }
}