package com.voicenotesai.presentation.notes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicenotesai.data.local.entity.Note
import com.voicenotesai.data.repository.NotesRepository
import com.voicenotesai.domain.model.AppError
import com.voicenotesai.domain.model.toUserMessage
import com.voicenotesai.presentation.theme.Spacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Screen that displays the full content of a single note with actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Long,
    viewModel: NoteDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<Note?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<AppError?>(null) }

    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }

    LaunchedEffect(viewModel) {
        viewModel.uiState.collect { state ->
            when (state) {
                is NoteDetailUiState.Loading -> {
                    isLoading = true
                    error = null
                }
                is NoteDetailUiState.Success -> {
                    isLoading = false
                    note = state.note
                    error = null
                }
                is NoteDetailUiState.Error -> {
                    isLoading = false
                    error = state.error
                }
                is NoteDetailUiState.Deleted -> {
                    onNavigateBack()
                }
            }
        }
    }
    
    // Show error in snackbar
    LaunchedEffect(error) {
        error?.let {
            val result = snackbarHostState.showSnackbar(
                message = it.toUserMessage(),
                actionLabel = "Retry",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.loadNote(noteId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Note Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    note?.let { currentNote ->
                        IconButton(
                            onClick = {
                                val success = copyToClipboard(context, currentNote.content)
                                scope.launch {
                                    val message = if (success) {
                                        "Copied to clipboard"
                                    } else {
                                        "Failed to copy. Please try share instead."
                                    }
                                    snackbarHostState.showSnackbar(message)
                                }
                            }
                        ) {
                            Text("Copy")
                        }
                        IconButton(
                            onClick = {
                                shareNote(context, currentNote.content)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share"
                            )
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                note != null -> {
                    NoteContent(note = note!!)
                }
                error != null -> {
                    // Error is shown in snackbar, show empty state
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(Spacing.extraLarge),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Note not found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Delete confirmation dialog
        if (showDeleteDialog) {
            DeleteConfirmationDialog(
                onConfirm = {
                    viewModel.deleteNote(noteId)
                    showDeleteDialog = false
                },
                onDismiss = {
                    showDeleteDialog = false
                }
            )
        }
    }
}

/**
 * Displays the note content.
 */
@Composable
private fun NoteContent(note: Note) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.medium)
    ) {
        Text(
            text = formatTimestamp(note.timestamp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.medium))
        Text(
            text = note.content,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}



/**
 * Delete confirmation dialog.
 */
@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Note") },
        text = { Text("Are you sure you want to delete this note? This action cannot be undone.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
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

/**
 * Copies text to clipboard with error handling.
 * 
 * @param context Android context
 * @param text Text to copy
 * @return true if successful, false otherwise
 */
private fun copyToClipboard(context: Context, text: String): Boolean {
    return try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null) {
            val clip = ClipData.newPlainText("Note", text)
            clipboard.setPrimaryClip(clip)
            true
        } else {
            false
        }
    } catch (e: Exception) {
        // ClipboardManager might throw SecurityException on some Android versions
        e.printStackTrace()
        false
    }
}

/**
 * Shares note content via other apps.
 */
private fun shareNote(context: Context, content: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, content)
    }
    context.startActivity(Intent.createChooser(intent, "Share Note"))
}

/**
 * Formats timestamp to readable date/time string.
 */
private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEEE, MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * ViewModel for the note detail screen.
 */
@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val notesRepository: NotesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NoteDetailUiState>(NoteDetailUiState.Loading)
    val uiState: StateFlow<NoteDetailUiState> = _uiState

    fun loadNote(noteId: Long) {
        _uiState.value = NoteDetailUiState.Loading
        viewModelScope.launch {
            try {
                val note = notesRepository.getNoteById(noteId)
                _uiState.value = if (note != null) {
                    NoteDetailUiState.Success(note)
                } else {
                    NoteDetailUiState.Error(AppError.StorageError("Note not found"))
                }
            } catch (e: Exception) {
                val error = AppError.StorageError(e.message ?: "Failed to load note")
                _uiState.value = NoteDetailUiState.Error(error)
            }
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            try {
                notesRepository.deleteNote(noteId)
                _uiState.value = NoteDetailUiState.Deleted
            } catch (e: Exception) {
                val error = AppError.StorageError(e.message ?: "Failed to delete note")
                _uiState.value = NoteDetailUiState.Error(error)
            }
        }
    }
}

/**
 * UI state for the note detail screen.
 */
sealed class NoteDetailUiState {
    object Loading : NoteDetailUiState()
    data class Success(val note: Note) : NoteDetailUiState()
    data class Error(val error: AppError) : NoteDetailUiState()
    object Deleted : NoteDetailUiState()
}
