package com.voicenotesai.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicenotesai.presentation.theme.ModernSpacing

/**
 * Data class representing storage usage information.
 */
data class StorageInfo(
    val totalSpace: Long,
    val usedSpace: Long,
    val availableSpace: Long,
    val notesSize: Long,
    val audioSize: Long,
    val cacheSize: Long,
    val tempSize: Long
) {
    val usagePercentage: Float get() = (usedSpace.toFloat() / totalSpace.toFloat()).coerceIn(0f, 1f)
    val isLowSpace: Boolean get() = usagePercentage > 0.9f
    val isCriticalSpace: Boolean get() = usagePercentage > 0.95f
}

/**
 * Storage usage card showing current storage status and cleanup options.
 */
@Composable
fun StorageUsageCard(
    storageInfo: StorageInfo,
    isOptimizing: Boolean = false,
    onOptimizeStorage: () -> Unit,
    onManageStorage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (storageInfo.isCriticalSpace) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            } else if (storageInfo.isLowSpace) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ModernSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ModernSpacing.medium)
        ) {
            // Header with storage icon and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ModernSpacing.small)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = if (storageInfo.isCriticalSpace) {
                            MaterialTheme.colorScheme.error
                        } else if (storageInfo.isLowSpace) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Storage Usage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                if (storageInfo.isLowSpace) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Low storage warning",
                        tint = if (storageInfo.isCriticalSpace) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Storage usage progress bar
            StorageProgressBar(
                storageInfo = storageInfo,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Storage breakdown
            StorageBreakdown(storageInfo = storageInfo)
            
            // Warning message for low storage
            if (storageInfo.isLowSpace) {
                StorageWarningMessage(
                    isCritical = storageInfo.isCriticalSpace,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ModernSpacing.small, Alignment.End)
            ) {
                OutlinedButton(
                    onClick = onManageStorage,
                    enabled = !isOptimizing
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manage")
                }
                
                FilledTonalButton(
                    onClick = onOptimizeStorage,
                    enabled = !isOptimizing,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (storageInfo.isLowSpace) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    )
                ) {
                    if (isOptimizing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isOptimizing) "Optimizing..." else "Optimize")
                }
            }
        }
    }
}

/**
 * Storage progress bar with usage visualization.
 */
@Composable
fun StorageProgressBar(
    storageInfo: StorageInfo,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = storageInfo.usagePercentage,
        label = "storage_progress"
    )
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ModernSpacing.small)
    ) {
        // Usage text
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${formatBytes(storageInfo.usedSpace)} used",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${formatBytes(storageInfo.totalSpace)} total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Progress bar
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = when {
                storageInfo.isCriticalSpace -> MaterialTheme.colorScheme.error
                storageInfo.isLowSpace -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        
        // Percentage text
        Text(
            text = "${(storageInfo.usagePercentage * 100).toInt()}% used",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

/**
 * Breakdown of storage usage by category.
 */
@Composable
fun StorageBreakdown(
    storageInfo: StorageInfo,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ModernSpacing.small)
    ) {
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.padding(0.dp)
        ) {
            Text(
                text = if (expanded) "Hide breakdown" else "Show breakdown",
                style = MaterialTheme.typography.labelSmall
            )
        }
        
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(ModernSpacing.small)
            ) {
                StorageBreakdownItem(
                    label = "Notes",
                    size = storageInfo.notesSize,
                    color = MaterialTheme.colorScheme.primary
                )
                StorageBreakdownItem(
                    label = "Audio files",
                    size = storageInfo.audioSize,
                    color = MaterialTheme.colorScheme.secondary
                )
                StorageBreakdownItem(
                    label = "Cache",
                    size = storageInfo.cacheSize,
                    color = MaterialTheme.colorScheme.tertiary
                )
                if (storageInfo.tempSize > 0) {
                    StorageBreakdownItem(
                        label = "Temporary files",
                        size = storageInfo.tempSize,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/**
 * Individual storage breakdown item.
 */
@Composable
fun StorageBreakdownItem(
    label: String,
    size: Long,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ModernSpacing.small)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = formatBytes(size),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Warning message for low storage situations.
 */
@Composable
fun StorageWarningMessage(
    isCritical: Boolean,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, message) = if (isCritical) {
        Triple(
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.error,
            "Critical: Storage is almost full. Some features may not work properly."
        )
    } else {
        Triple(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.primary,
            "Warning: Storage is running low. Consider cleaning up old files."
        )
    }
    
    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .border(
                1.dp,
                textColor.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .padding(ModernSpacing.medium)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ModernSpacing.small)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Storage cleanup options dialog content.
 */
@Composable
fun StorageCleanupOptions(
    onCleanCache: () -> Unit,
    onCleanTempFiles: () -> Unit,
    onArchiveOldNotes: () -> Unit,
    onCompactDatabase: () -> Unit,
    isProcessing: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ModernSpacing.medium)
    ) {
        Text(
            text = "Storage Cleanup Options",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Text(
            text = "Choose which cleanup operations to perform:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        CleanupOptionItem(
            title = "Clear Cache",
            description = "Remove cached data and thumbnails",
            icon = Icons.Default.CleaningServices,
            onClick = onCleanCache,
            enabled = !isProcessing
        )
        
        CleanupOptionItem(
            title = "Clean Temporary Files",
            description = "Remove temporary recording and processing files",
            icon = Icons.Default.Delete,
            onClick = onCleanTempFiles,
            enabled = !isProcessing
        )
        
        CleanupOptionItem(
            title = "Archive Old Notes",
            description = "Move notes older than 6 months to archive",
            icon = Icons.Default.Folder,
            onClick = onArchiveOldNotes,
            enabled = !isProcessing
        )
        
        CleanupOptionItem(
            title = "Compact Database",
            description = "Optimize database to free up space",
            icon = Icons.Default.Storage,
            onClick = onCompactDatabase,
            enabled = !isProcessing
        )
    }
}

/**
 * Individual cleanup option item.
 */
@Composable
fun CleanupOptionItem(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ModernSpacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ModernSpacing.medium),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                    modifier = Modifier.size(24.dp)
                )
                
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                }
            }
            
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.semantics {
                    contentDescription = "Perform $title cleanup"
                }
            ) {
                Text("Clean")
            }
        }
    }
}

/**
 * Format bytes to human-readable string.
 */
private fun formatBytes(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unitIndex = 0
    
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    
    return "%.1f %s".format(size, units[unitIndex])
}