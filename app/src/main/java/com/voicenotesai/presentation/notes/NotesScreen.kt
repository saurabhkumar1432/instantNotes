package com.voicenotesai.presentation.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.voicenotesai.data.local.entity.Note
import com.voicenotesai.domain.model.toUserMessage
import com.voicenotesai.presentation.theme.Spacing
import com.voicenotesai.presentation.theme.glassLayer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main notes screen that displays a list of saved notes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NotesViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNoteClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
                        Text(
                            text = "Saved drops",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Swipe through every riff the AI turned into notes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is NotesUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is NotesUiState.Empty -> {
                    EmptyState(modifier = Modifier.align(Alignment.Center))
                }
                is NotesUiState.Success -> {
                    NotesList(
                        notes = state.notes,
                        onNoteClick = onNoteClick,
                        onDeleteClick = { note -> noteToDelete = note }
                    )
                }
                is NotesUiState.Error -> {
                    LaunchedEffect(state.error) {
                        val result = snackbarHostState.showSnackbar(
                            message = state.error.toUserMessage(),
                            actionLabel = "Retry",
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.clearError()
                        }
                    }
                    EmptyState(modifier = Modifier.align(Alignment.Center))
                }
            }
        }

        noteToDelete?.let { note ->
            DeleteConfirmationDialog(
                onConfirm = {
                    viewModel.deleteNote(note.id)
                    noteToDelete = null
                },
                onDismiss = { noteToDelete = null }
            )
        }
    }
}

@Composable
private fun NotesList(
    notes: List<Note>,
    onNoteClick: (Long) -> Unit,
    onDeleteClick: (Note) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.large,
            end = Spacing.large,
            top = Spacing.large,
            bottom = Spacing.huge
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
		item {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.border(
						width = 3.dp,
						brush = Brush.linearGradient(
							colors = listOf(
								MaterialTheme.colorScheme.primary,
								MaterialTheme.colorScheme.tertiary
							)
						),
						shape = RoundedCornerShape(24.dp)
					)
					.background(
						color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
						shape = RoundedCornerShape(24.dp)
					)
					.padding(Spacing.large)
			) {
				Column(
					verticalArrangement = Arrangement.spacedBy(Spacing.small)
				) {
					Text(
						text = "✨ Your masterpieces",
						style = MaterialTheme.typography.headlineSmall,
						fontWeight = FontWeight.ExtraBold,
						color = MaterialTheme.colorScheme.onPrimaryContainer
					)
					Text(
						text = "Every note is AI-polished and ready to reference whenever inspiration strikes again 🚀",
						style = MaterialTheme.typography.bodyLarge,
						color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
						fontWeight = FontWeight.Medium
					)
				}
			}
		}
		
		items(notes, key = { it.id }) { note ->
            NoteCard(
                note = note,
                onClick = { onNoteClick(note.id) },
                onDeleteClick = { onDeleteClick(note) }
            )
        }
    }
}

@Composable
private fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 3.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.primary
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
            .padding(Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
			Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
				Box(
					modifier = Modifier
						.background(
							brush = Brush.linearGradient(
								listOf(
									MaterialTheme.colorScheme.primary,
									MaterialTheme.colorScheme.secondary
								)
							),
							shape = RoundedCornerShape(12.dp)
						)
						.padding(horizontal = 14.dp, vertical = 8.dp)
				) {
					Text(
						text = formatTimestamp(note.timestamp),
						style = MaterialTheme.typography.labelLarge,
						color = Color.White,
						fontWeight = FontWeight.Bold
					)
				}
			}
			
			FilledTonalIconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(42.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete note",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Text(
            text = note.content,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(horizontal = Spacing.extraLarge)
            .border(
                width = 3.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(28.dp)
            )
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(Spacing.huge)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                text = "💀 Nothing here yet",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Hit that record button on the home screen to capture your first genius idea 💡",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete note") },
        text = { Text("This note will vanish forever. Continue?") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
