package com.voicenotesai.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.voicenotesai.R
import com.voicenotesai.presentation.accessibility.AccessibilityPreferences
import com.voicenotesai.presentation.accessibility.AccessibleBodyText
import com.voicenotesai.presentation.accessibility.AccessibleHeading
import com.voicenotesai.presentation.components.EnhancedListItem
import com.voicenotesai.presentation.components.ResponsiveContent
import com.voicenotesai.presentation.layout.rememberEnhancedLayoutConfig
import com.voicenotesai.presentation.theme.Spacing
import com.voicenotesai.presentation.theme.rememberAdvancedSpacing

/**
 * Enhanced settings screen with typography and accessibility options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTypography: () -> Unit,
    onNavigateToAISettings: () -> Unit,
    typographyViewModel: TypographyViewModel = hiltViewModel()
) {
    val typographyUiState by typographyViewModel.uiState.collectAsState()
    val layoutConfig = rememberEnhancedLayoutConfig()
    val spacing = rememberAdvancedSpacing(typographyUiState.accessibilityPreferences.spacingScale)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AccessibleHeading(
                        text = stringResource(R.string.nav_label_settings),
                        level = 1,
                        accessibilityPreferences = typographyUiState.accessibilityPreferences
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.go_back_description)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        ResponsiveContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            accessibilityPreferences = typographyUiState.accessibilityPreferences
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(spacing.screenMargin),
                verticalArrangement = Arrangement.spacedBy(spacing.sectionSpacing)
            ) {
                // Typography & Accessibility Section
                SettingsCategory(
                    title = stringResource(R.string.typography_accessibility_settings),
                    accessibilityPreferences = typographyUiState.accessibilityPreferences
                ) {
                    EnhancedListItem(
                        title = stringResource(R.string.text_size),
                        subtitle = stringResource(R.string.text_size_description),
                        trailing = typographyUiState.typographySettings.textSizeScale.name,
                        accessibilityPreferences = typographyUiState.accessibilityPreferences,
                        onClick = onNavigateToTypography
                    )
                    
                    EnhancedListItem(
                        title = stringResource(R.string.spacing_layout),
                        subtitle = stringResource(R.string.spacing_description),
                        trailing = typographyUiState.typographySettings.spacingScale.name,
                        accessibilityPreferences = typographyUiState.accessibilityPreferences,
                        onClick = onNavigateToTypography
                    )
                    
                    EnhancedListItem(
                        title = stringResource(R.string.accessibility_options),
                        subtitle = getAccessibilityOptionsSummary(typographyUiState.accessibilityPreferences),
                        accessibilityPreferences = typographyUiState.accessibilityPreferences,
                        onClick = onNavigateToTypography
                    )
                }
                
                // AI Provider Section
                SettingsCategory(
                    title = stringResource(R.string.settings_ai_provider_title),
                    accessibilityPreferences = typographyUiState.accessibilityPreferences
                ) {
                    EnhancedListItem(
                        title = stringResource(R.string.settings_connection_title),
                        subtitle = stringResource(R.string.settings_connection_desc),
                        accessibilityPreferences = typographyUiState.accessibilityPreferences,
                        onClick = onNavigateToAISettings
                    )
                }
            }
        }
    }
}

/**
 * Settings category component with proper typography
 */
@Composable
private fun SettingsCategory(
    title: String,
    accessibilityPreferences: AccessibilityPreferences,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val spacing = rememberAdvancedSpacing(accessibilityPreferences.spacingScale)
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.componentSpacing)
    ) {
        AccessibleHeading(
            text = title,
            level = 2,
            accessibilityPreferences = accessibilityPreferences
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (accessibilityPreferences.highContrastMode) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(spacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(spacing.itemSpacing)
            ) {
                content()
            }
        }
    }
}

/**
 * Get summary of enabled accessibility options
 */
@Composable
private fun getAccessibilityOptionsSummary(preferences: AccessibilityPreferences): String {
    val enabledOptions = mutableListOf<String>()
    
    if (preferences.highContrastMode) {
        enabledOptions.add(stringResource(R.string.high_contrast_mode))
    }
    if (preferences.boldText) {
        enabledOptions.add(stringResource(R.string.bold_text))
    }
    if (preferences.increasedLineSpacing) {
        enabledOptions.add(stringResource(R.string.increased_line_spacing))
    }
    if (preferences.reducedMotion) {
        enabledOptions.add(stringResource(R.string.reduced_motion))
    }
    
    return when {
        enabledOptions.isEmpty() -> "None enabled"
        enabledOptions.size == 1 -> enabledOptions.first()
        enabledOptions.size <= 3 -> enabledOptions.joinToString(", ")
        else -> "${enabledOptions.take(2).joinToString(", ")} +${enabledOptions.size - 2} more"
    }
}