package com.voicenotesai.presentation.notes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import com.voicenotesai.presentation.sharing.SharingDialog
import com.voicenotesai.presentation.sharing.SharingViewModel
import com.voicenotesai.presentation.sharing.ShareableLinkDialog
import com.voicenotesai.presentation.sharing.ShareableLinkOptions
import com.voicenotesai.presentation.sharing.CalendarEventDialog
import com.voicenotesai.domain.model.EnhancedNote
import com.voicenotesai.domain.sharing.ShareFormat
import com.voicenotesai.domain.sharing.TargetApp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicenotesai.data.local.entity.Note
import com.voicenotesai.data.repository.NotesRepository
import com.voicenotesai.domain.model.AppError
import com.voicenotesai.domain.model.Task
import com.voicenotesai.domain.usecase.TaskManager
import com.voicenotesai.presentation.components.toLocalizedMessage
import com.voicenotesai.presentation.theme.Spacing
import com.voicenotesai.presentation.theme.glassLayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Screen that displays the full content of a single note with actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Long,
    viewModel: NoteDetailViewModel = hiltViewModel(),
    sharingViewModel: SharingViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSharingDialog by remember { mutableStateOf(false) }
    var showShareableLinkDialog by remember { mutableStateOf(false) }
    var showCalendarEventDialog by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<Note?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<AppError?>(null) }
    
    val sharingUiState by sharingViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }

    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }

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
                    tasks = state.tasks
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

    // Precompute strings that will be used from non-composable scopes (LaunchedEffect's block and coroutine callbacks)
    val retryLabel = stringResource(id = com.voicenotesai.R.string.retry)
    val localizedError = error?.toLocalizedMessage()
    val errorMessage = localizedError?.let { stringResource(id = it.resId, *it.args) }
    val copiedToClipboard = stringResource(id = com.voicenotesai.R.string.copied_to_clipboard)
    val copyFailedTryShare = stringResource(id = com.voicenotesai.R.string.copy_failed_try_share)

    LaunchedEffect(error) {
        error?.let {
            // Use precomputed strings inside the launched effect (non-composable lambda)
            val message = errorMessage ?: ""
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = retryLabel,
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.loadNote(noteId)
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
                        Text(
                            text = stringResource(id = com.voicenotesai.R.string.note_preview_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = stringResource(id = com.voicenotesai.R.string.note_preview_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(id = com.voicenotesai.R.string.go_back_description)
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
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                note != null -> {
                    NoteContent(
                        note = note!!,
                        tasks = tasks,
                        onCopy = {
                            val success = copyToClipboard(context, note!!.content)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = if (success) copiedToClipboard else copyFailedTryShare
                                )
                            }
                        },
                        onShare = { showSharingDialog = true },
                        onDelete = { showDeleteDialog = true },
                        onToggleTaskCompletion = { taskId ->
                            viewModel.toggleTaskCompletion(taskId)
                        },
                        onExtractTasks = {
                            viewModel.extractTasksFromNote(note!!.id, note!!.content)
                        }
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(Spacing.extraLarge)
                            .border(
                                width = 3.dp,
                                color = MaterialTheme.colorScheme.error,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(Spacing.large),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                    ) {
                        Text(
                            text = stringResource(id = com.voicenotesai.R.string.note_not_found_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = stringResource(id = com.voicenotesai.R.string.note_not_found_desc),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (showDeleteDialog) {
            DeleteConfirmationDialog(
                onConfirm = {
                    viewModel.deleteNote(noteId)
                    showDeleteDialog = false
                },
                onDismiss = { showDeleteDialog = false }
            )
        }
        
        // Sharing dialogs
        note?.let { currentNote ->
            val enhancedNote = EnhancedNote(
                id = currentNote.id.toString(),
                content = currentNote.content,
                transcribedText = currentNote.transcribedText ?: "",
                timestamp = currentNote.timestamp,
                lastModified = currentNote.timestamp,
                duration = null,
                tags = emptyList(),
                category = com.voicenotesai.domain.model.NoteCategory.General,
                entities = emptyList(),
                sentiment = null,
                language = null,
                isArchived = false
            )
            
            SharingDialog(
                isVisible = showSharingDialog,
                noteTitle = currentNote.content.lines().firstOrNull()?.take(50) ?: "Untitled Note",
                onDismiss = { showSharingDialog = false },
                onShareAsText = {
                    sharingViewModel.shareNoteAsText(enhancedNote)
                    showSharingDialog = false
                },
                onShareAsFile = { format ->
                    sharingViewModel.shareNoteAsFile(enhancedNote, format)
                    showSharingDialog = false
                },
                onShareToApp = { targetApp, format ->
                    sharingViewModel.shareToApp(enhancedNote, targetApp, format)
                    showSharingDialog = false
                },
                onCreateShareableLink = { expirationHours ->
                    showSharingDialog = false
                    showShareableLinkDialog = true
                },
                onCreateCalendarEvent = {
                    showSharingDialog = false
                    showCalendarEventDialog = true
                }
            )
            
            ShareableLinkDialog(
                isVisible = showShareableLinkDialog,
                noteTitle = currentNote.content.lines().firstOrNull()?.take(50) ?: "Untitled Note",
                onDismiss = { 
                    showShareableLinkDialog = false
                    sharingViewModel.clearGeneratedLink()
                },
                onCreateLink = { options ->
                    sharingViewModel.createShareableLink(currentNote.id.toString(), options)
                },
                generatedLink = sharingUiState.generatedLink,
                isLoading = sharingUiState.isLoading
            )
            
            CalendarEventDialog(
                isVisible = showCalendarEventDialog,
                noteTitle = currentNote.content.lines().firstOrNull()?.take(50) ?: "Untitled Note",
                noteContent = currentNote.content,
                suggestedEvent = sharingViewModel.extractCalendarEvent(enhancedNote),
                onDismiss = { 
                    showCalendarEventDialog = false
                    sharingViewModel.clearCalendarIntent()
                },
                onCreateEvent = { eventDetails ->
                    sharingViewModel.createCalendarEvent(enhancedNote, eventDetails)
                    showCalendarEventDialog = false
                },
                isLoading = sharingUiState.isLoading
            )
        }
        
        // Handle sharing results
        LaunchedEffect(sharingUiState.calendarIntent) {
            sharingUiState.calendarIntent?.let { intent ->
                try {
                    context.startActivity(intent)
                    sharingViewModel.clearCalendarIntent()
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to open calendar app")
                }
            }
        }
        
        LaunchedEffect(sharingUiState.error) {
            sharingUiState.error?.let { errorMessage ->
                snackbarHostState.showSnackbar(errorMessage)
                sharingViewModel.clearError()
            }
        }
    }
}

@Composable
private fun NoteContent(
    note: Note,
    tasks: List<Task>,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onToggleTaskCompletion: (String) -> Unit,
    onExtractTasks: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.large, vertical = Spacing.extraLarge),
        verticalArrangement = Arrangement.spacedBy(Spacing.large)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
                Text(
                    text = formatTimestamp(note.timestamp),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "✨ AI-powered summary",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium
                )
            }
        }

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
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(Spacing.large)
        ) {
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                lineHeight = 26.sp
            )
        }

        // Action Items Section
        if (tasks.isNotEmpty() || true) { // Always show section to allow task extraction
            ActionItemsSection(
                tasks = tasks,
                onToggleTaskCompletion = onToggleTaskCompletion,
                onExtractTasks = onExtractTasks
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            Button(
                onClick = onCopy,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(stringResource(id = com.voicenotesai.R.string.copy_to_clipboard_label), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            }
            Button(
                onClick = onShare,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Text(stringResource(id = com.voicenotesai.R.string.share_externally_label), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            }
            Button(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Text(stringResource(id = com.voicenotesai.R.string.delete_forever_label), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/**
 * Action Items section showing extracted tasks with checkboxes for completion.
 */
@Composable
private fun ActionItemsSection(
    tasks: List<Task>,
    onToggleTaskCompletion: (String) -> Unit,
    onExtractTasks: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        // Section header with extract button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = "Action Items",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            
            if (tasks.isEmpty()) {
                OutlinedButton(
                    onClick = onExtractTasks,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(Spacing.small))
                    Text("Extract Tasks")
                }
            }
        }
        
        if (tasks.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(Spacing.large)
            ) {
                Text(
                    text = "No action items found. Tap 'Extract Tasks' to analyze this note for tasks and todos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            // Tasks list
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(Spacing.medium)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    tasks.forEach { task ->
                        TaskItem(
                            task = task,
                            onToggleCompletion = { onToggleTaskCompletion(task.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual task item with checkbox and strikethrough for completed tasks.
 */
@Composable
private fun TaskItem(
    task: Task,
    onToggleCompletion: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.Top
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggleCompletion() },
            modifier = Modifier.padding(end = Spacing.small)
        )
        
        Text(
            text = task.text,
            style = MaterialTheme.typography.bodyMedium.copy(
                textDecoration = if (task.isCompleted) {
                    androidx.compose.ui.text.style.TextDecoration.LineThrough
                } else {
                    androidx.compose.ui.text.style.TextDecoration.None
                }
            ),
            color = if (task.isCompleted) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActionButton(
    onClick: () -> Unit,
    label: String,
    leadingIcon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        leadingIcon?.let {
            Icon(imageVector = it, contentDescription = null)
            Spacer(modifier = Modifier.width(Spacing.small))
        }
        Text(label)
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
        title = { Text(stringResource(id = com.voicenotesai.R.string.delete_note_title)) },
        text = { Text(stringResource(id = com.voicenotesai.R.string.delete_note_confirm_text)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(id = com.voicenotesai.R.string.delete_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = com.voicenotesai.R.string.cancel))
            }
        }
    )
}

/**
 * Copies text to clipboard with error handling.
 */
private fun copyToClipboard(context: Context, text: String): Boolean {
    return try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null) {
            val clip = ClipData.newPlainText(context.getString(com.voicenotesai.R.string.note_clip_label), text)
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
    context.startActivity(Intent.createChooser(intent, context.getString(com.voicenotesai.R.string.share_chooser_title)))
}

/**
 * Formats timestamp to readable date/time string.
 */
private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEEE, MMM dd yyyy · hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * ViewModel for the note detail screen.
 */
@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val notesRepository: NotesRepository,
    private val taskManager: TaskManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<NoteDetailUiState>(NoteDetailUiState.Loading)
    val uiState: StateFlow<NoteDetailUiState> = _uiState

    fun loadNote(noteId: Long) {
        _uiState.value = NoteDetailUiState.Loading
        viewModelScope.launch {
            try {
                val note = notesRepository.getNoteById(noteId)
                if (note != null) {
                    // Load tasks associated with this note
                    taskManager.getTasksForNote(noteId.toString()).collect { tasks ->
                        _uiState.value = NoteDetailUiState.Success(note, tasks)
                    }
                } else {
                    _uiState.value = NoteDetailUiState.Error(AppError.StorageError("Note not found"))
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

    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is NoteDetailUiState.Success) {
                    val task = currentState.tasks.find { it.id == taskId }
                    if (task != null) {
                        if (task.isCompleted) {
                            taskManager.markTaskIncomplete(taskId)
                        } else {
                            taskManager.markTaskComplete(taskId)
                        }
                        // The UI will automatically update through the Flow in loadNote
                    }
                }
            } catch (e: Exception) {
                // Handle error silently or show a snackbar
                val error = AppError.StorageError(e.message ?: "Failed to update task")
                _uiState.value = NoteDetailUiState.Error(error)
            }
        }
    }

    fun extractTasksFromNote(noteId: Long, content: String) {
        viewModelScope.launch {
            try {
                taskManager.extractAndCreateTasks(noteId.toString(), content)
                // Tasks will be automatically updated through the Flow
            } catch (e: Exception) {
                // Handle error silently or show a snackbar
            }
        }
    }
}

/**
 * UI state for the note detail screen.
 */
sealed class NoteDetailUiState {
    object Loading : NoteDetailUiState()
    data class Success(val note: Note, val tasks: List<Task> = emptyList()) : NoteDetailUiState()
    data class Error(val error: AppError) : NoteDetailUiState()
    object Deleted : NoteDetailUiState()
}
