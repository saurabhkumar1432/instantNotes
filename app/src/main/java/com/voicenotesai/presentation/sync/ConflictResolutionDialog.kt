package com.voicenotesai.presentation.sync

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.voicenotesai.domain.sync.*

/**
 * Dialog for resolving sync conflicts with side-by-side comparison and resolution options.
 */
@Composable
fun ConflictResolutionDialog(
    conflict: SyncConflict,
    onResolve: (ConflictResolution) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedResolution by remember { mutableStateOf<ConflictResolution?>(null) }
    var showMergeEditor by remember { mutableStateOf(false) }
    var mergedContent by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                ConflictDialogHeader(
                    conflict = conflict,
                    onDismiss = onDismiss
                )
                
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                
                // Content comparison
                if (showMergeEditor) {
                    MergeEditor(
                        localContent = conflict.localVersion.data.toString(),
                        remoteContent = conflict.remoteVersion.data.toString(),
                        mergedContent = mergedContent,
                        onMergedContentChange = { mergedContent = it },
                        onBack = { showMergeEditor = false },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    ConflictComparison(
                        conflict = conflict,
                        selectedResolution = selectedResolution,
                        onResolutionSelected = { selectedResolution = it },
                        onShowMergeEditor = { 
                            showMergeEditor = true
                            mergedContent = suggestMergedContent(conflict)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                
                // Action buttons
                ConflictDialogActions(
                    selectedResolution = selectedResolution,
                    mergedContent = mergedContent,
                    showMergeEditor = showMergeEditor,
                    onResolve = { resolution ->
                        val finalResolution = if (showMergeEditor) {
                            ConflictResolution.UseMerged(mergedContent)
                        } else {
                            resolution
                        }
                        onResolve(finalResolution)
                    },
                    onCancel = onDismiss
                )
            }
        }
    }
}

/**
 * Header section of the conflict resolution dialog.
 */
@Composable
private fun ConflictDialogHeader(
    conflict: SyncConflict,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Sync Conflict",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = getConflictDescription(conflict),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close dialog",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Side-by-side comparison of conflicting versions.
 */
@Composable
private fun ConflictComparison(
    conflict: SyncConflict,
    selectedResolution: ConflictResolution?,
    onResolutionSelected: (ConflictResolution) -> Unit,
    onShowMergeEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Resolution options
        Text(
            text = "Choose how to resolve this conflict:",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ResolutionOption(
                    title = "Keep Local Version",
                    description = "Use your local changes and discard remote changes",
                    icon = Icons.Default.PhoneAndroid,
                    isSelected = selectedResolution is ConflictResolution.UseLocal,
                    onClick = { onResolutionSelected(ConflictResolution.UseLocal) }
                )
            }
            
            item {
                ResolutionOption(
                    title = "Keep Remote Version",
                    description = "Use remote changes and discard your local changes",
                    icon = Icons.Default.Cloud,
                    isSelected = selectedResolution is ConflictResolution.UseRemote,
                    onClick = { onResolutionSelected(ConflictResolution.UseRemote) }
                )
            }
            
            item {
                ResolutionOption(
                    title = "Merge Changes",
                    description = "Combine both versions manually",
                    icon = Icons.Default.MergeType,
                    isSelected = false,
                    onClick = onShowMergeEditor
                )
            }
            
            item {
                ResolutionOption(
                    title = "Skip This Item",
                    description = "Don't sync this item for now",
                    icon = Icons.Default.SkipNext,
                    isSelected = selectedResolution is ConflictResolution.Skip,
                    onClick = { onResolutionSelected(ConflictResolution.Skip) }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Version comparison
                Text(
                    text = "Version Comparison",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Local version
                    VersionCard(
                        title = "Local Version",
                        version = conflict.localVersion,
                        modifier = Modifier.weight(1f),
                        isSelected = selectedResolution is ConflictResolution.UseLocal
                    )
                    
                    // Remote version
                    VersionCard(
                        title = "Remote Version",
                        version = conflict.remoteVersion,
                        modifier = Modifier.weight(1f),
                        isSelected = selectedResolution is ConflictResolution.UseRemote
                    )
                }
            }
        }
    }
}

/**
 * Resolution option item.
 */
@Composable
private fun ResolutionOption(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }
    
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Card displaying version information.
 */
@Composable
private fun VersionCard(
    title: String,
    version: ConflictVersion,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Card(
        modifier = modifier
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "Version: ${version.version}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "Modified: ${formatTimestamp(version.timestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Content preview
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = version.data.toString().take(100) + if (version.data.toString().length > 100) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(8.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Merge editor for manual conflict resolution.
 */
@Composable
private fun MergeEditor(
    localContent: String,
    remoteContent: String,
    mergedContent: String,
    onMergedContentChange: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with back button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to options"
                )
            }
            
            Text(
                text = "Merge Changes",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Source versions (read-only)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Local version
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Local Version",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = localContent,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(12.dp)
                            .heightIn(max = 120.dp)
                    )
                }
            }
            
            // Remote version
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Remote Version",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = remoteContent,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(12.dp)
                            .heightIn(max = 120.dp)
                    )
                }
            }
        }
        
        // Merged content editor
        Column {
            Text(
                text = "Merged Content",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            OutlinedTextField(
                value = mergedContent,
                onValueChange = onMergedContentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 200.dp),
                placeholder = {
                    Text("Edit the merged content here...")
                },
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

/**
 * Action buttons for the conflict resolution dialog.
 */
@Composable
private fun ConflictDialogActions(
    selectedResolution: ConflictResolution?,
    mergedContent: String,
    showMergeEditor: Boolean,
    onResolve: (ConflictResolution) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
    ) {
        OutlinedButton(
            onClick = onCancel
        ) {
            Text("Cancel")
        }
        
        Button(
            onClick = {
                if (showMergeEditor) {
                    onResolve(ConflictResolution.UseMerged(mergedContent))
                } else {
                    selectedResolution?.let { onResolve(it) }
                }
            },
            enabled = if (showMergeEditor) {
                mergedContent.isNotBlank()
            } else {
                selectedResolution != null
            }
        ) {
            Text("Resolve Conflict")
        }
    }
}

/**
 * Gets a human-readable description of the conflict.
 */
private fun getConflictDescription(conflict: SyncConflict): String {
    return when (conflict.conflictType) {
        ConflictType.CONCURRENT_MODIFICATION -> "This ${conflict.itemType.name.lowercase()} was modified both locally and remotely"
        ConflictType.DELETE_MODIFY_CONFLICT -> "This ${conflict.itemType.name.lowercase()} was deleted remotely but modified locally"
        ConflictType.SCHEMA_MISMATCH -> "The data format has changed and needs to be updated"
        ConflictType.PERMISSION_DENIED -> "You don't have permission to sync this ${conflict.itemType.name.lowercase()}"
        ConflictType.DATA_CORRUPTION -> "The data appears to be corrupted and needs attention"
        ConflictType.VERSION_MISMATCH -> "Version conflict detected for this ${conflict.itemType.name.lowercase()}"
    }
}

/**
 * Suggests merged content based on local and remote versions.
 */
private fun suggestMergedContent(conflict: SyncConflict): String {
    val localContent = conflict.localVersion.data.toString()
    val remoteContent = conflict.remoteVersion.data.toString()
    
    // Simple merge suggestion - in practice, this would be more sophisticated
    return if (conflict.localVersion.timestamp > conflict.remoteVersion.timestamp) {
        localContent
    } else {
        remoteContent
    }
}

/**
 * Formats timestamp for display.
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000} minutes ago"
        diff < 86400000 -> "${diff / 3600000} hours ago"
        else -> "${diff / 86400000} days ago"
    }
}