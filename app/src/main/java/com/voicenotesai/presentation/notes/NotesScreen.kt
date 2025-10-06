package com.voicenotesai.presentation.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.voicenotesai.data.local.entity.Note
import com.voicenotesai.domain.model.toUserMessage
// import com.voicenotesai.presentation.notes.components.BatchDeleteDialog
// import com.voicenotesai.presentation.notes.components.EmptySearchState
// import com.voicenotesai.presentation.notes.components.NotesExportDialog
// import com.voicenotesai.presentation.notes.components.NotesFilterRow
import com.voicenotesai.presentation.notes.components.NotesSearchBar
// import com.voicenotesai.presentation.notes.components.NotesSelectionToolbar
// import com.voicenotesai.presentation.notes.components.NotesSortDropdown
import com.voicenotesai.presentation.theme.ExtendedTypography
import com.voicenotesai.presentation.theme.Spacing
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
    var searchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            val haptic = LocalHapticFeedback.current
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
                        Text(
                            text = "Notes",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Review and manage every transcript you've captured.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNavigateBack()
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Go back to recording screen"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        },
		snackbarHost = { SnackbarHost(snackbarHostState) }
	) { paddingValues ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(paddingValues)
		) {
			// Simple search bar
			NotesSearchBar(
				searchQuery = searchQuery,
				onSearchQueryChange = { searchQuery = it },
				modifier = Modifier
					.padding(Spacing.medium)
			)            // Notes content with basic filtering
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (val state = uiState) {
                    is NotesUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is NotesUiState.Empty -> {
                        if (searchQuery.isNotEmpty()) {
                            EmptySearchState(
                                searchQuery = searchQuery,
                                onClearSearch = { searchQuery = "" },
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            EmptyState(modifier = Modifier.align(Alignment.Center))
                        }
                    }
                    is NotesUiState.Success -> {
                        val filteredNotes = if (searchQuery.isNotEmpty()) {
                            state.notes.filter { note ->
                                note.content.contains(searchQuery, ignoreCase = true) ||
                                        note.transcribedText?.contains(searchQuery, ignoreCase = true) == true
                            }
                        } else {
                            state.notes
                        }
                        
                        if (filteredNotes.isEmpty() && searchQuery.isNotEmpty()) {
                            EmptySearchState(
                                searchQuery = searchQuery,
                                onClearSearch = { searchQuery = "" },
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            NotesList(
                                notes = filteredNotes,
                                onNoteClick = onNoteClick,
                                onDeleteClick = { note -> noteToDelete = note }
                            )
                        }
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.large),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    Text(
                        text = "Everything in one place",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Use search to jump back to a conversation or open a note to review the full transcript.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val timestamp = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(note.timestamp))
    val preview = note.content.take(150).replace("\n", " ")
    Card(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Note from $timestamp. Preview: $preview"
            }
    ) {
        Column(
            modifier = Modifier.padding(Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = formatTimestamp(note.timestamp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDeleteClick()
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .semantics {
                            contentDescription = "Delete note from $timestamp"
                        },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = note.content,
                style = ExtendedTypography.noteContent,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EmptySearchState(
    searchQuery: String,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                text = "🔍 No matches found",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "No notes found for \"$searchQuery\". Try different keywords or clear the search.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            
            TextButton(onClick = onClearSearch) {
                Text("Clear search")
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .padding(horizontal = Spacing.extraLarge),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(Spacing.huge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                text = "No notes yet",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Record from the home screen to create your first AI-generated summary.",
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
        title = { Text("Delete note permanently?") },
        text = { Text("This will permanently remove the note. This action cannot be undone.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete note")
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
