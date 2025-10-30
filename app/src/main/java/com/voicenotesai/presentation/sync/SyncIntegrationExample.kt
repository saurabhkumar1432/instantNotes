package com.voicenotesai.presentation.sync

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicenotesai.domain.sync.*

/**
 * Example screen showing how to integrate the sync system with the existing app.
 * This demonstrates the sync status indicators, conflict resolution, and progress tracking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncIntegrationExample(
    modifier: Modifier = Modifier,
    viewModel: SyncViewModel = hiltViewModel()
) {
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val conflicts by viewModel.conflicts.collectAsStateWithLifecycle()
    val currentConflict by viewModel.currentConflict.collectAsStateWithLifecycle()
    val pendingOperations by viewModel.pendingOperations.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Show conflict resolution dialog when needed
    currentConflict?.let { conflict ->
        ConflictResolutionDialog(
            conflict = conflict,
            onResolve = { resolution ->
                viewModel.resolveConflict(conflict.id, resolution)
            },
            onDismiss = {
                viewModel.dismissCurrentConflict()
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App bar with sync status
        TopAppBar(
            title = { Text("Sync Management") },
            actions = {
                CompactSyncStatusIndicator(
                    syncStatus = syncStatus,
                    syncProgress = syncProgress,
                    onClick = { viewModel.refreshData() }
                )
            }
        )

        // Main sync status indicator
        SyncStatusIndicator(
            syncStatus = syncStatus,
            syncProgress = syncProgress,
            onRetryClick = { viewModel.retryFailedOperations() },
            onCancelClick = { viewModel.cancelSync() },
            showDetails = true
        )

        // Error display
        uiState.error?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(onClick = { viewModel.clearError() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss error",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Sync actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.startSync() },
                enabled = syncStatus != SyncStatus.SYNCING && !uiState.isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Sync")
            }
            
            OutlinedButton(
                onClick = { viewModel.forceFullSync() },
                enabled = syncStatus != SyncStatus.SYNCING && !uiState.isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Full Sync")
            }
        }

        // Conflicts summary
        if (conflicts.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        
                        Text(
                            text = "${conflicts.size} Conflicts Need Resolution",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    
                    Button(
                        onClick = { viewModel.showNextConflict() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Text("Resolve Conflicts")
                    }
                }
            }
        }

        // Pending operations list
        if (pendingOperations.isNotEmpty()) {
            Text(
                text = "Pending Operations (${pendingOperations.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pendingOperations.take(5)) { operation ->
                    PendingOperationItem(
                        operation = operation,
                        onRetry = { 
                            viewModel.syncItem(operation.itemId, operation.itemType)
                        }
                    )
                }
                
                if (pendingOperations.size > 5) {
                    item {
                        Text(
                            text = "... and ${pendingOperations.size - 5} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }

        // Loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

/**
 * Individual pending operation item.
 */
@Composable
private fun PendingOperationItem(
    operation: SyncOperation,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${operation.itemType.name} - ${operation.operation.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "Status: ${operation.status.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = getOperationStatusColor(operation.status)
                )
                
                if (operation.error != null) {
                    Text(
                        text = "Error: ${operation.error}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            if (operation.status == SyncOperationStatus.Failed) {
                IconButton(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry operation",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Gets color for operation status.
 */
@Composable
private fun getOperationStatusColor(status: SyncOperationStatus): androidx.compose.ui.graphics.Color {
    return when (status) {
        SyncOperationStatus.Pending -> MaterialTheme.colorScheme.onSurfaceVariant
        SyncOperationStatus.InProgress -> MaterialTheme.colorScheme.primary
        SyncOperationStatus.Completed -> MaterialTheme.colorScheme.primary
        SyncOperationStatus.Failed -> MaterialTheme.colorScheme.error
        SyncOperationStatus.Cancelled -> MaterialTheme.colorScheme.outline
        SyncOperationStatus.ConflictDetected -> MaterialTheme.colorScheme.tertiary
        SyncOperationStatus.Retrying -> MaterialTheme.colorScheme.secondary
    }
}