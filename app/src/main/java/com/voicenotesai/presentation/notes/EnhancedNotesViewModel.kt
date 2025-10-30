package com.voicenotesai.presentation.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicenotesai.data.local.entity.Note
import com.voicenotesai.data.repository.NotesRepository
import com.voicenotesai.domain.model.AppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.annotation.StringRes
import javax.inject.Inject

/**
 * Enhanced ViewModel for the notes screen with search, filtering, and batch operations.
 */
@HiltViewModel
class EnhancedNotesViewModel @Inject constructor(
    private val notesRepository: NotesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotesUiState>(NotesUiState.Loading)
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow(NotesFilter.ALL)
    val filterType: StateFlow<NotesFilter> = _filterType.asStateFlow()

    private val _sortType = MutableStateFlow(NotesSortType.NEWEST_FIRST)
    val sortType: StateFlow<NotesSortType> = _sortType.asStateFlow()

    private val _selectedNotes = MutableStateFlow<Set<Long>>(emptySet())
    val selectedNotes: StateFlow<Set<Long>> = _selectedNotes.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _showExportDialog = MutableStateFlow(false)
    val showExportDialog: StateFlow<Boolean> = _showExportDialog.asStateFlow()

    init {
        loadNotes()
    }

    /**
     * Loads and processes notes with search, filter, and sort applied.
     */
    private fun loadNotes() {
        viewModelScope.launch {
            combine(
                notesRepository.getAllNotes(),
                _searchQuery.debounce(300).distinctUntilChanged(),
                _filterType,
                _sortType
            ) { notes, query, filter, sort ->
                processNotes(notes, query, filter, sort)
            }
                .catch { error ->
                    val appError = AppError.StorageError(error.message ?: "Failed to load notes")
                    _uiState.value = NotesUiState.Error(appError)
                }
                .collect { processedNotes ->
                    _uiState.value = if (processedNotes.isEmpty()) {
                        NotesUiState.Empty
                    } else {
                        NotesUiState.Success(processedNotes)
                    }
                }
        }
    }

    /**
     * Processes notes with search, filtering, and sorting.
     */
    private fun processNotes(
        notes: List<Note>,
        searchQuery: String,
        filter: NotesFilter,
        sort: NotesSortType
    ): List<Note> {
        var processedNotes = notes

        // Apply search filter
        if (searchQuery.isNotBlank()) {
            processedNotes = processedNotes.filter { note ->
                note.content.contains(searchQuery, ignoreCase = true) ||
                        note.transcribedText?.contains(searchQuery, ignoreCase = true) == true
            }
        }

        // Apply date/content filters
        processedNotes = when (filter) {
            NotesFilter.ALL -> processedNotes
            NotesFilter.TODAY -> processedNotes.filter { isFromToday(it.timestamp) }
            NotesFilter.THIS_WEEK -> processedNotes.filter { isFromThisWeek(it.timestamp) }
            NotesFilter.THIS_MONTH -> processedNotes.filter { isFromThisMonth(it.timestamp) }
            NotesFilter.LONG_NOTES -> processedNotes.filter { it.content.length > 500 }
            NotesFilter.SHORT_NOTES -> processedNotes.filter { it.content.length <= 500 }
        }

        // Apply sorting
        processedNotes = when (sort) {
            NotesSortType.NEWEST_FIRST -> processedNotes.sortedByDescending { it.timestamp }
            NotesSortType.OLDEST_FIRST -> processedNotes.sortedBy { it.timestamp }
            NotesSortType.SHORTEST_FIRST -> processedNotes.sortedBy { it.content.length }
            NotesSortType.LONGEST_FIRST -> processedNotes.sortedByDescending { it.content.length }
            NotesSortType.ALPHABETICAL -> processedNotes.sortedBy { it.content.take(50).lowercase() }
        }

        return processedNotes
    }

    /**
     * Updates the search query.
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Updates the filter type.
     */
    fun updateFilter(filter: NotesFilter) {
        _filterType.value = filter
    }

    /**
     * Updates the sort type.
     */
    fun updateSort(sort: NotesSortType) {
        _sortType.value = sort
    }

    /**
     * Toggles selection mode.
     */
    fun toggleSelectionMode() {
        _isSelectionMode.value = !_isSelectionMode.value
        if (!_isSelectionMode.value) {
            _selectedNotes.value = emptySet()
        }
    }

    /**
     * Toggles selection of a note.
     */
    fun toggleNoteSelection(noteId: Long) {
        val currentSelection = _selectedNotes.value.toMutableSet()
        if (currentSelection.contains(noteId)) {
            currentSelection.remove(noteId)
        } else {
            currentSelection.add(noteId)
        }
        _selectedNotes.value = currentSelection

        // Exit selection mode if no notes are selected
        if (currentSelection.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    /**
     * Selects all visible notes.
     */
    fun selectAllNotes() {
        val currentState = _uiState.value
        if (currentState is NotesUiState.Success) {
            _selectedNotes.value = currentState.notes.map { it.id }.toSet()
        }
    }

    /**
     * Deselects all notes.
     */
    fun deselectAllNotes() {
        _selectedNotes.value = emptySet()
        _isSelectionMode.value = false
    }

    /**
     * Deletes a single note.
     */
    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            try {
                notesRepository.deleteNote(noteId)
                // Remove from selection if it was selected
                val currentSelection = _selectedNotes.value.toMutableSet()
                currentSelection.remove(noteId)
                _selectedNotes.value = currentSelection
            } catch (e: Exception) {
                val appError = AppError.StorageError(e.message ?: "Failed to delete note")
                _uiState.value = NotesUiState.Error(appError)
            }
        }
    }

    /**
     * Deletes selected notes in batch.
     */
    fun deleteSelectedNotes() {
        viewModelScope.launch {
            try {
                val selectedIds = _selectedNotes.value
                selectedIds.forEach { noteId ->
                    notesRepository.deleteNote(noteId)
                }
                _selectedNotes.value = emptySet()
                _isSelectionMode.value = false
            } catch (e: Exception) {
                val appError = AppError.StorageError(e.message ?: "Failed to delete selected notes")
                _uiState.value = NotesUiState.Error(appError)
            }
        }
    }

    /**
     * Shows the export dialog.
     */
    fun showExportDialog() {
        _showExportDialog.value = true
    }

    /**
     * Hides the export dialog.
     */
    fun hideExportDialog() {
        _showExportDialog.value = false
    }

    /**
     * Exports notes in the specified format.
     */
    fun exportNotes(format: ExportFormat): String {
        val currentState = _uiState.value
        if (currentState !is NotesUiState.Success) return ""

        val notesToExport = if (_selectedNotes.value.isNotEmpty()) {
            currentState.notes.filter { _selectedNotes.value.contains(it.id) }
        } else {
            currentState.notes
        }

        return when (format) {
            ExportFormat.TEXT -> exportAsText(notesToExport)
            ExportFormat.MARKDOWN -> exportAsMarkdown(notesToExport)
            ExportFormat.JSON -> exportAsJson(notesToExport)
        }
    }

    /**
     * Gets export statistics.
     */
    fun getExportStats(): ExportStats {
        val currentState = _uiState.value
        if (currentState !is NotesUiState.Success) {
            return ExportStats(0, 0, 0)
        }

        val notesToExport = if (_selectedNotes.value.isNotEmpty()) {
            currentState.notes.filter { _selectedNotes.value.contains(it.id) }
        } else {
            currentState.notes
        }

        val totalWords = notesToExport.sumOf { it.content.split("\\s+".toRegex()).size }
        val totalCharacters = notesToExport.sumOf { it.content.length }

        return ExportStats(
            noteCount = notesToExport.size,
            totalWords = totalWords,
            totalCharacters = totalCharacters
        )
    }

    /**
     * Clears any error messages and reloads notes.
     */
    fun clearError() {
        loadNotes()
    }

    // Private helper methods

    private fun isFromToday(timestamp: Long): Boolean {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return timestamp >= today.timeInMillis
    }

    private fun isFromThisWeek(timestamp: Long): Boolean {
        val weekStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return timestamp >= weekStart.timeInMillis
    }

    private fun isFromThisMonth(timestamp: Long): Boolean {
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return timestamp >= monthStart.timeInMillis
    }

    private fun exportAsText(notes: List<Note>): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        
        return buildString {
            appendLine("Voice Notes Export")
            appendLine("=================")
            appendLine("Exported on: ${dateFormat.format(Date())}")
            appendLine("Total notes: ${notes.size}")
            appendLine()

            notes.forEachIndexed { index, note ->
                appendLine("Note ${index + 1}")
                appendLine("Date: ${dateFormat.format(Date(note.timestamp))}")
                appendLine("---")
                appendLine(note.content)
                appendLine()
                appendLine("=" + "=".repeat(49))
                appendLine()
            }
        }
    }

    private fun exportAsMarkdown(notes: List<Note>): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        
        return buildString {
            appendLine("# Voice Notes Export")
            appendLine()
            appendLine("**Exported on:** ${dateFormat.format(Date())}")
            appendLine("**Total notes:** ${notes.size}")
            appendLine()

            notes.forEachIndexed { index, note ->
                appendLine("## Note ${index + 1}")
                appendLine()
                appendLine("**Date:** ${dateFormat.format(Date(note.timestamp))}")
                appendLine()
                appendLine(note.content)
                appendLine()
                appendLine("---")
                appendLine()
            }
        }
    }

    private fun exportAsJson(notes: List<Note>): String {
        // Simple JSON export without external dependencies
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        
        return buildString {
            appendLine("{")
            appendLine("  \"exportDate\": \"${dateFormat.format(Date())}\",")
            appendLine("  \"totalNotes\": ${notes.size},")
            appendLine("  \"notes\": [")
            
            notes.forEachIndexed { index, note ->
                appendLine("    {")
                appendLine("      \"id\": ${note.id},")
                appendLine("      \"content\": \"${note.content.replace("\"", "\\\"").replace("\n", "\\n")}\",")
                appendLine("      \"timestamp\": ${note.timestamp},")
                appendLine("      \"date\": \"${dateFormat.format(Date(note.timestamp))}\",")
                appendLine("      \"transcribedText\": ${if (note.transcribedText != null) "\"${note.transcribedText.replace("\"", "\\\"").replace("\n", "\\n")}\"" else "null"}")
                append("    }")
                if (index < notes.size - 1) appendLine(",")
                else appendLine()
            }
            
            appendLine("  ]")
            appendLine("}")
        }
    }
}

/**
 * Filter options for notes.
 */
enum class NotesFilter(@StringRes val displayNameRes: Int) {
    ALL(com.voicenotesai.R.string.filter_all_notes),
    TODAY(com.voicenotesai.R.string.filter_today),
    THIS_WEEK(com.voicenotesai.R.string.filter_this_week),
    THIS_MONTH(com.voicenotesai.R.string.filter_this_month),
    LONG_NOTES(com.voicenotesai.R.string.filter_long_notes),
    SHORT_NOTES(com.voicenotesai.R.string.filter_short_notes)
}

/**
 * Sort options for notes.
 */
enum class NotesSortType(@StringRes val displayNameRes: Int) {
    NEWEST_FIRST(com.voicenotesai.R.string.sort_newest_first),
    OLDEST_FIRST(com.voicenotesai.R.string.sort_oldest_first),
    SHORTEST_FIRST(com.voicenotesai.R.string.sort_shortest_first),
    LONGEST_FIRST(com.voicenotesai.R.string.sort_longest_first),
    ALPHABETICAL(com.voicenotesai.R.string.sort_alphabetical)
}

/**
 * Export format options.
 */
enum class ExportFormat(@StringRes val displayNameRes: Int, val fileExtension: String) {
    TEXT(com.voicenotesai.R.string.export_plain_text, "txt"),
    MARKDOWN(com.voicenotesai.R.string.export_markdown, "md"),
    JSON(com.voicenotesai.R.string.export_json, "json")
}

/**
 * Export statistics data class.
 */
data class ExportStats(
    val noteCount: Int,
    val totalWords: Int,
    val totalCharacters: Int
)