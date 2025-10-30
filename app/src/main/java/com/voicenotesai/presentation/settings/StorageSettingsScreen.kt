package com.voicenotesai.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.voicenotesai.domain.storage.*
import kotlin.math.roundToInt

/**
 * Storage settings screen with smart caching and cleanup configuration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StorageSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val storageMetrics by viewModel.storageMetrics.collectAsState()
    val storageAnalysis by viewModel.storageAnalysis.collectAsState()

    // Handle error display
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // In a real app, you might show a snackbar or dialog
            // For now, we'll just clear the error after showing it
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Storage Management",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Storage Overview Card
            item {
                StorageOverviewCard(
                    storageMetrics = storageMetrics,
                    storageAnalysis = storageAnalysis,
                    onOptimizeClick = { viewModel.optimizeStorage() },
                    onAutomaticCleanupClick = { viewModel.performAutomaticCleanup() },
                    isOptimizing = uiState.isOptimizing
                )
            }

            // Automatic Cleanup Settings
            item {
                AutomaticCleanupCard(
                    settings = uiState.settings,
                    onAutomaticCleanupToggle = viewModel::updateAutomaticCleanup,
                    onCleanupFrequencyChange = viewModel::updateCleanupFrequency,
                    onLowStorageModeToggle = viewModel::updateLowStorageMode,
                    onLowStorageThresholdChange = viewModel::updateLowStorageThreshold
                )
            }

            // Cache Management Settings
            item {
                CacheManagementCard(
                    settings = uiState.settings,
                    onMaxCacheSizeChange = viewModel::updateMaxCacheSize,
                    onCompressionLevelChange = viewModel::updateCompressionLevel
                )
            }

            // Data Retention Settings
            item {
                DataRetentionCard(
                    settings = uiState.settings,
                    onMaxAudioRetentionChange = viewModel::updateMaxAudioRetention,
                    onArchiveOldNotesToggle = viewModel::updateArchiveOldNotes,
                    onArchiveThresholdChange = viewModel::updateArchiveThreshold,
                    onMaxTempFileAgeChange = viewModel::updateMaxTempFileAge
                )
            }

            // Performance Optimization Settings
            item {
                PerformanceOptimizationCard(
                    settings = uiState.settings,
                    onBatteryOptimizationToggle = viewModel::updateBatteryOptimization,
                    onThermalOptimizationToggle = viewModel::updateThermalOptimization,
                    onDeleteEmptyFoldersToggle = viewModel::updateDeleteEmptyFolders,
                    onOptimizeDbOnStartupToggle = viewModel::updateOptimizeDatabaseOnStartup
                )
            }

            // Storage Recommendations
            storageAnalysis?.let { analysis ->
                if (analysis.recommendations.isNotEmpty()) {
                    item {
                        StorageRecommendationsCard(
                            recommendations = analysis.recommendations,
                            onRecommendationAction = { recommendation ->
                                when (recommendation.action) {
                                    is RecommendationAction.AutomaticCleanup -> viewModel.optimizeStorage()
                                    is RecommendationAction.CompactDatabase -> viewModel.compactDatabase()
                                    is RecommendationAction.ArchiveData -> {
                                        val days = recommendation.action.olderThan.inWholeDays.toInt()
                                        viewModel.archiveOldNotes(days)
                                    }
                                    else -> { /* Handle other actions */ }
                                }
                            }
                        )
                    }
                }
            }

            // Manual Actions
            item {
                ManualActionsCard(
                    onCleanupTempFiles = { viewModel.cleanupTempFiles() },
                    onCompactDatabase = { viewModel.compactDatabase() },
                    onArchiveOldNotes = { viewModel.archiveOldNotes(90) },
                    isOptimizing = uiState.isOptimizing
                )
            }
        }
    }
}

@Composable
private fun StorageOverviewCard(
    storageMetrics: StorageMetrics?,
    storageAnalysis: StorageAnalysis?,
    onOptimizeClick: () -> Unit,
    onAutomaticCleanupClick: () -> Unit,
    isOptimizing: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Storage Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            storageMetrics?.let { metrics ->
                // Storage usage bar
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Used: ${formatBytes(metrics.usedSpace)}")
                        Text("Available: ${formatBytes(metrics.availableSpace)}")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = metrics.usagePercentage / 100f,
                        modifier = Modifier.fillMaxWidth(),
                        color = when (metrics.storageHealth) {
                            StorageHealth.EXCELLENT -> Color.Green
                            StorageHealth.GOOD -> Color.Blue
                            StorageHealth.WARNING -> Color.Yellow
                            StorageHealth.CRITICAL -> Color.Red
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Storage Health: ${metrics.storageHealth.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (metrics.storageHealth) {
                            StorageHealth.EXCELLENT -> Color.Green
                            StorageHealth.GOOD -> Color.Blue
                            StorageHealth.WARNING -> Color.Yellow
                            StorageHealth.CRITICAL -> Color.Red
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Storage breakdown
                storageAnalysis?.let { analysis ->
                    Text("Storage Breakdown:", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    StorageBreakdownItem("Database", analysis.databaseSize)
                    StorageBreakdownItem("Cache", analysis.cacheSize)
                    StorageBreakdownItem("Audio Files", analysis.audioFilesSize)
                    StorageBreakdownItem("Temporary Files", analysis.tempFilesSize)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOptimizeClick,
                    enabled = !isOptimizing,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isOptimizing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Optimize Storage")
                    }
                }
                
                OutlinedButton(
                    onClick = onAutomaticCleanupClick,
                    enabled = !isOptimizing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Auto Cleanup")
                }
            }
        }
    }
}

@Composable
private fun StorageBreakdownItem(label: String, size: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(formatBytes(size), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AutomaticCleanupCard(
    settings: StorageManagementSettings,
    onAutomaticCleanupToggle: (Boolean) -> Unit,
    onCleanupFrequencyChange: (CleanupFrequency) -> Unit,
    onLowStorageModeToggle: (Boolean) -> Unit,
    onLowStorageThresholdChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Automatic Cleanup",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Enable automatic cleanup
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Automatic Cleanup")
                Switch(
                    checked = settings.enableAutomaticCleanup,
                    onCheckedChange = onAutomaticCleanupToggle
                )
            }

            if (settings.enableAutomaticCleanup) {
                Spacer(modifier = Modifier.height(12.dp))

                // Cleanup frequency
                Text("Cleanup Frequency", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                CleanupFrequency.values().forEach { frequency ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.cleanupFrequency == frequency,
                            onClick = { onCleanupFrequencyChange(frequency) }
                        )
                        Text(frequency.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Low storage mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Low Storage Mode")
                    Text(
                        "Automatically clean when storage is low",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.enableLowStorageMode,
                    onCheckedChange = onLowStorageModeToggle
                )
            }

            if (settings.enableLowStorageMode) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Threshold: ${(settings.lowStorageThreshold * 100).roundToInt()}%")
                Slider(
                    value = settings.lowStorageThreshold * 100,
                    onValueChange = onLowStorageThresholdChange,
                    valueRange = 50f..95f,
                    steps = 8
                )
            }
        }
    }
}

@Composable
private fun CacheManagementCard(
    settings: StorageManagementSettings,
    onMaxCacheSizeChange: (Int) -> Unit,
    onCompressionLevelChange: (CompressionLevel) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Cache Management",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Max cache size
            Text("Maximum Cache Size: ${settings.maxCacheSize / (1024 * 1024)}MB")
            Slider(
                value = (settings.maxCacheSize / (1024 * 1024)).toFloat(),
                onValueChange = { onMaxCacheSizeChange(it.toInt()) },
                valueRange = 50f..500f,
                steps = 17
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Compression level
            Text("Compression Level", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            CompressionLevel.values().forEach { level ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settings.compressionLevel == level,
                        onClick = { onCompressionLevelChange(level) }
                    )
                    Text(level.name.lowercase().replaceFirstChar { it.uppercase() })
                }
            }
        }
    }
}

@Composable
private fun DataRetentionCard(
    settings: StorageManagementSettings,
    onMaxAudioRetentionChange: (Int) -> Unit,
    onArchiveOldNotesToggle: (Boolean) -> Unit,
    onArchiveThresholdChange: (Int) -> Unit,
    onMaxTempFileAgeChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Data Retention",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Audio retention
            Text("Audio File Retention: ${settings.maxAudioRetention.inWholeDays} days")
            Slider(
                value = settings.maxAudioRetention.inWholeDays.toFloat(),
                onValueChange = { onMaxAudioRetentionChange(it.toInt()) },
                valueRange = 7f..365f,
                steps = 50
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Archive old notes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Archive Old Notes")
                    Text(
                        "Automatically archive notes older than threshold",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.archiveOldNotes,
                    onCheckedChange = onArchiveOldNotesToggle
                )
            }

            if (settings.archiveOldNotes) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Archive Threshold: ${settings.archiveThreshold.inWholeDays} days")
                Slider(
                    value = settings.archiveThreshold.inWholeDays.toFloat(),
                    onValueChange = { onArchiveThresholdChange(it.toInt()) },
                    valueRange = 30f..365f,
                    steps = 33
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Temp file age
            Text("Temporary File Age: ${settings.maxTempFileAge.inWholeDays} days")
            Slider(
                value = settings.maxTempFileAge.inWholeDays.toFloat(),
                onValueChange = { onMaxTempFileAgeChange(it.toInt()) },
                valueRange = 1f..30f,
                steps = 28
            )
        }
    }
}

@Composable
private fun PerformanceOptimizationCard(
    settings: StorageManagementSettings,
    onBatteryOptimizationToggle: (Boolean) -> Unit,
    onThermalOptimizationToggle: (Boolean) -> Unit,
    onDeleteEmptyFoldersToggle: (Boolean) -> Unit,
    onOptimizeDbOnStartupToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Performance Optimization",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingsToggleItem(
                title = "Battery Optimization",
                description = "Skip cleanup when battery is low",
                checked = settings.enableBatteryOptimization,
                onCheckedChange = onBatteryOptimizationToggle
            )

            SettingsToggleItem(
                title = "Thermal Optimization",
                description = "Skip cleanup when device is hot",
                checked = settings.enableThermalOptimization,
                onCheckedChange = onThermalOptimizationToggle
            )

            SettingsToggleItem(
                title = "Delete Empty Folders",
                description = "Remove empty directories during cleanup",
                checked = settings.deleteEmptyFolders,
                onCheckedChange = onDeleteEmptyFoldersToggle
            )

            SettingsToggleItem(
                title = "Optimize Database on Startup",
                description = "Compact database when app starts",
                checked = settings.optimizeDatabaseOnStartup,
                onCheckedChange = onOptimizeDbOnStartupToggle
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun StorageRecommendationsCard(
    recommendations: List<StorageRecommendation>,
    onRecommendationAction: (StorageRecommendation) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Storage Recommendations",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            recommendations.forEach { recommendation ->
                RecommendationItem(
                    recommendation = recommendation,
                    onActionClick = { onRecommendationAction(recommendation) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun RecommendationItem(
    recommendation: StorageRecommendation,
    onActionClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (recommendation.priority) {
                StorageRecommendationPriority.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                StorageRecommendationPriority.HIGH -> MaterialTheme.colorScheme.warningContainer
                StorageRecommendationPriority.MEDIUM -> MaterialTheme.colorScheme.primaryContainer
                StorageRecommendationPriority.LOW -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        recommendation.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        recommendation.description,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (recommendation.potentialSavings > 0) {
                        Text(
                            "Potential savings: ${formatBytes(recommendation.potentialSavings)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                if (recommendation.action !is RecommendationAction.ManualReview) {
                    TextButton(onClick = onActionClick) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualActionsCard(
    onCleanupTempFiles: () -> Unit,
    onCompactDatabase: () -> Unit,
    onArchiveOldNotes: () -> Unit,
    isOptimizing: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Manual Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onCleanupTempFiles,
                enabled = !isOptimizing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CleaningServices, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clean Temporary Files")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onCompactDatabase,
                enabled = !isOptimizing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Storage, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compact Database")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onArchiveOldNotes,
                enabled = !isOptimizing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Archive, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Archive Old Notes (90+ days)")
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024 * 1024)}GB"
        bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
        bytes >= 1024 -> "${bytes / 1024}KB"
        else -> "${bytes}B"
    }
}

// Extension property for warning container color (if not available in Material3)
private val ColorScheme.warningContainer: Color
    get() = Color(0xFFFFF3CD) // Light yellow for warning

private val ColorScheme.onWarningContainer: Color
    get() = Color(0xFF856404) // Dark yellow for text on warning