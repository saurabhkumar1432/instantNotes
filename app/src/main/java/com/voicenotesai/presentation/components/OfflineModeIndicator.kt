package com.voicenotesai.presentation.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import com.voicenotesai.presentation.theme.ModernSpacing

/**
 * Offline mode indicator showing current connectivity status and available functionality.
 */
@Composable
fun OfflineModeIndicator(
    isOffline: Boolean,
    pendingOperations: Int = 0,
    onRetrySync: (() -> Unit)? = null,
    onViewOfflineFeatures: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOffline,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ModernSpacing.medium),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ModernSpacing.large),
                verticalArrangement = Arrangement.spacedBy(ModernSpacing.small)
            ) {
                // Header with offline icon and status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ModernSpacing.small)
                    ) {
                        OfflineStatusIcon(
                            isOffline = true,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Offline Mode",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (pendingOperations > 0) {
                        PendingOperationsBadge(count = pendingOperations)
                    }
                }
                
                // Offline capabilities description
                Text(
                    text = "You can still record notes and use basic features. Changes will sync when connection is restored.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    lineHeight = 16.sp
                )
                
                // Available offline features
                OfflineFeaturesList()
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ModernSpacing.small, Alignment.End)
                ) {
                    onViewOfflineFeatures?.let {
                        TextButton(onClick = it) {
                            Text(
                                "View Features",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    
                    onRetrySync?.let {
                        TextButton(onClick = it) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Retry Sync",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact offline status indicator for headers and toolbars.
 */
@Composable
fun CompactOfflineIndicator(
    isOffline: Boolean,
    pendingOperations: Int = 0,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOffline,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val contentDesc = if (pendingOperations > 0) {
            "Offline mode active with $pendingOperations pending operations"
        } else {
            "Offline mode active"
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .semantics {
                    contentDescription = contentDesc
                }
                .let { mod ->
                    onClick?.let { mod.clickable { it() } } ?: mod
                }
        ) {
            OfflineStatusIcon(
                isOffline = true,
                modifier = Modifier.size(14.dp)
            )
            
            Text(
                text = "Offline",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            
            if (pendingOperations > 0) {
                PendingOperationsBadge(
                    count = pendingOperations,
                    size = 12.dp
                )
            }
        }
    }
}

/**
 * Icon indicating offline/online status.
 */
@Composable
fun OfflineStatusIcon(
    isOffline: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val icon: ImageVector = if (isOffline) {
        Icons.Default.WifiOff
    } else {
        Icons.Default.Wifi
    }
    
    Icon(
        imageVector = icon,
        contentDescription = if (isOffline) "Offline" else "Online",
        tint = tint,
        modifier = modifier
    )
}

/**
 * Badge showing number of pending operations.
 */
@Composable
fun PendingOperationsBadge(
    count: Int,
    size: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    if (count > 0) {
        Box(
            modifier = modifier
                .size(size)
                .background(
                    MaterialTheme.colorScheme.primary,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (count > 99) "99+" else count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = (size.value * 0.6).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * List of available offline features.
 */
@Composable
private fun OfflineFeaturesList() {
    val offlineFeatures = listOf(
        "Record voice notes",
        "View existing notes",
        "Basic transcription",
        "Local search",
        "Export notes"
    )
    
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.padding(0.dp)
        ) {
            Text(
                text = if (expanded) "Hide available features" else "Show available features",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(start = ModernSpacing.small),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                offlineFeatures.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ModernSpacing.small)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                )
                        )
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sync status indicator showing sync progress and status.
 */
@Composable
fun SyncStatusIndicator(
    isSyncing: Boolean,
    lastSyncTime: String? = null,
    syncError: String? = null,
    onRetrySync: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ModernSpacing.small),
        modifier = modifier
    ) {
        val (icon, color, text) = when {
            isSyncing -> Triple(
                Icons.Default.Sync,
                MaterialTheme.colorScheme.primary,
                "Syncing..."
            )
            syncError != null -> Triple(
                Icons.Default.SyncDisabled,
                MaterialTheme.colorScheme.error,
                "Sync failed"
            )
            lastSyncTime != null -> Triple(
                Icons.Default.Sync,
                MaterialTheme.colorScheme.onSurfaceVariant,
                "Last sync: $lastSyncTime"
            )
            else -> Triple(
                Icons.Default.CloudOff,
                MaterialTheme.colorScheme.onSurfaceVariant,
                "Not synced"
            )
        }
        
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
        
        if (syncError != null && onRetrySync != null) {
            TextButton(
                onClick = onRetrySync,
                modifier = Modifier.padding(0.dp)
            ) {
                Text(
                    "Retry",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}