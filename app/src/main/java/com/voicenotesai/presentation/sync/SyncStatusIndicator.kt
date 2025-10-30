package com.voicenotesai.presentation.sync

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicenotesai.domain.sync.SyncStatus
import com.voicenotesai.domain.sync.SyncProgress

/**
 * Composable that displays sync status with animated indicators and progress tracking.
 */
@Composable
fun SyncStatusIndicator(
    syncStatus: SyncStatus,
    syncProgress: SyncProgress,
    modifier: Modifier = Modifier,
    onRetryClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    showDetails: Boolean = false
) {
    val statusColor = getSyncStatusColor(syncStatus)
    val statusIcon = getSyncStatusIcon(syncStatus)
    val statusText = getSyncStatusText(syncStatus)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Main status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Animated status icon
                    AnimatedSyncIcon(
                        icon = statusIcon,
                        color = statusColor,
                        isAnimating = syncStatus == SyncStatus.SYNCING
                    )
                    
                    Column {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (syncStatus == SyncStatus.SYNCING && syncProgress.totalOperations > 0) {
                            Text(
                                text = "${syncProgress.completedOperations}/${syncProgress.totalOperations} operations",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (syncStatus) {
                        SyncStatus.SYNCING -> {
                            IconButton(
                                onClick = onCancelClick,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel sync",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        SyncStatus.FAILED, SyncStatus.PARTIAL_SUCCESS -> {
                            IconButton(
                                onClick = onRetryClick,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry sync",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
            
            // Progress bar for syncing
            if (syncStatus == SyncStatus.SYNCING && syncProgress.totalOperations > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Column {
                    LinearProgressIndicator(
                        progress = syncProgress.progressPercentage / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = statusColor,
                        trackColor = statusColor.copy(alpha = 0.3f)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(syncProgress.progressPercentage).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        syncProgress.estimatedTimeRemainingMs?.let { timeRemaining ->
                            Text(
                                text = formatTimeRemaining(timeRemaining),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Detailed information
            if (showDetails && (syncProgress.failedOperations > 0 || syncProgress.conflictedOperations > 0)) {
                Spacer(modifier = Modifier.height(12.dp))
                
                SyncDetailsSection(
                    progress = syncProgress,
                    status = syncStatus
                )
            }
        }
    }
}

/**
 * Animated sync icon that rotates when syncing.
 */
@Composable
private fun AnimatedSyncIcon(
    icon: ImageVector,
    color: Color,
    isAnimating: Boolean,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isAnimating) 360f else 0f,
        animationSpec = if (isAnimating) {
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        } else {
            tween(300)
        },
        label = "sync_icon_rotation"
    )

    Box(
        modifier = modifier
            .size(40.dp)
            .background(
                color = color.copy(alpha = 0.1f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(20.dp)
                .rotate(rotation),
            tint = color
        )
    }
}

/**
 * Detailed sync information section.
 */
@Composable
private fun SyncDetailsSection(
    progress: SyncProgress,
    status: SyncStatus,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Divider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            thickness = 1.dp
        )
        
        Text(
            text = "Sync Details",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SyncDetailItem(
                label = "Completed",
                value = progress.completedOperations.toString(),
                color = MaterialTheme.colorScheme.primary
            )
            
            if (progress.failedOperations > 0) {
                SyncDetailItem(
                    label = "Failed",
                    value = progress.failedOperations.toString(),
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            if (progress.conflictedOperations > 0) {
                SyncDetailItem(
                    label = "Conflicts",
                    value = progress.conflictedOperations.toString(),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        
        if (progress.throughputItemsPerSecond > 0) {
            Text(
                text = "Speed: ${String.format("%.1f", progress.throughputItemsPerSecond)} items/sec",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Individual sync detail item.
 */
@Composable
private fun SyncDetailItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Compact sync status indicator for use in app bars or small spaces.
 */
@Composable
fun CompactSyncStatusIndicator(
    syncStatus: SyncStatus,
    syncProgress: SyncProgress,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val statusColor = getSyncStatusColor(syncStatus)
    val statusIcon = getSyncStatusIcon(syncStatus)

    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Box {
            AnimatedSyncIcon(
                icon = statusIcon,
                color = statusColor,
                isAnimating = syncStatus == SyncStatus.SYNCING,
                modifier = Modifier.size(24.dp)
            )
            
            // Progress indicator overlay for syncing
            if (syncStatus == SyncStatus.SYNCING && syncProgress.totalOperations > 0) {
                CircularProgressIndicator(
                    progress = syncProgress.progressPercentage / 100f,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center),
                    strokeWidth = 2.dp,
                    color = statusColor,
                    trackColor = statusColor.copy(alpha = 0.3f)
                )
            }
        }
    }
}

/**
 * Gets the appropriate color for sync status.
 */
@Composable
private fun getSyncStatusColor(status: SyncStatus): Color {
    return when (status) {
        SyncStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
        SyncStatus.SYNCING -> MaterialTheme.colorScheme.primary
        SyncStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        SyncStatus.FAILED -> MaterialTheme.colorScheme.error
        SyncStatus.CANCELLED -> MaterialTheme.colorScheme.outline
        SyncStatus.CONFLICTS_PENDING -> MaterialTheme.colorScheme.tertiary
        SyncStatus.PARTIAL_SUCCESS -> MaterialTheme.colorScheme.secondary
    }
}

/**
 * Gets the appropriate icon for sync status.
 */
private fun getSyncStatusIcon(status: SyncStatus): ImageVector {
    return when (status) {
        SyncStatus.IDLE -> Icons.Default.CloudOff
        SyncStatus.SYNCING -> Icons.Default.Sync
        SyncStatus.COMPLETED -> Icons.Default.CloudDone
        SyncStatus.FAILED -> Icons.Default.CloudOff
        SyncStatus.CANCELLED -> Icons.Default.Cancel
        SyncStatus.CONFLICTS_PENDING -> Icons.Default.Warning
        SyncStatus.PARTIAL_SUCCESS -> Icons.Default.CloudQueue
    }
}

/**
 * Gets human-readable text for sync status.
 */
private fun getSyncStatusText(status: SyncStatus): String {
    return when (status) {
        SyncStatus.IDLE -> "Ready to sync"
        SyncStatus.SYNCING -> "Syncing..."
        SyncStatus.COMPLETED -> "Sync completed"
        SyncStatus.FAILED -> "Sync failed"
        SyncStatus.CANCELLED -> "Sync cancelled"
        SyncStatus.CONFLICTS_PENDING -> "Conflicts need resolution"
        SyncStatus.PARTIAL_SUCCESS -> "Sync partially completed"
    }
}

/**
 * Formats time remaining in a human-readable format.
 */
private fun formatTimeRemaining(timeMs: Long): String {
    val seconds = timeMs / 1000
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}