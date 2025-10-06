package com.voicenotesai.presentation.notes

import androidx.lifecycle.viewModelScope
import com.voicenotesai.data.local.entity.Note
import com.voicenotesai.data.repository.NotesRepository
import com.voicenotesai.domain.model.AppError
import com.voicenotesai.presentation.performance.OptimizedViewModel
import com.voicenotesai.presentation.performance.MemoryOptimizer.debounceOptimized
import com.voicenotesai.presentation.performance.MemoryOptimizer.limitItems
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Performance-optimized ViewModel for the notes screen.
 * Extends OptimizedViewModel for better performance characteristics.
 */
@HiltViewModel
class OptimizedNotesViewModel @Inject constructor(
    private val notesRepository: NotesRepository
) : OptimizedViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<AppError?>(null)
    
    // Raw notes flow with memory optimization
    private val notesFlow = notesRepository.getAllNotes()
        .limitItems(maxItems = 1000) // Prevent memory issues with very large note collections
        .catch { error ->
            val appError = AppError.StorageError(error.message ?: "Failed to load notes")
            _error.value = appError
            _isLoading.value = false
            emit(emptyList())
        }

    // Optimized search query with debouncing
    private val debouncedSearchQuery = _searchQuery
        .debounceOptimized(timeoutMillis = 300)

    // Combined UI state with optimal recomposition characteristics
    val uiState: StateFlow<NotesUiState> = combine(
        notesFlow,
        debouncedSearchQuery
    ) { notes, searchQuery ->
        _isLoading.value = false
        _error.value = null
        
        val filteredNotes = if (searchQuery.isBlank()) {
            notes
        } else {
            notes.filter { note ->
                note.content.contains(searchQuery, ignoreCase = true) ||
                        note.transcribedText?.contains(searchQuery, ignoreCase = true) == true
            }
        }
        
        when {
            filteredNotes.isEmpty() && searchQuery.isBlank() -> NotesUiState.Empty
            filteredNotes.isEmpty() && searchQuery.isNotBlank() -> NotesUiState.Empty
            else -> NotesUiState.Success(filteredNotes)
        }
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = NotesUiState.Loading
    )

    // Separate error and loading states for better performance
    val errorState: StateFlow<AppError?> = _error.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    
    val loadingState: StateFlow<Boolean> = _isLoading.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    /**
     * Updates search query with debouncing to prevent excessive filtering.
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Deletes a note with optimized error handling.
     */
    fun deleteNote(noteId: Long) {
        safeLaunch(
            onError = { error ->
                _error.value = AppError.StorageError(error.message ?: "Failed to delete note")
            }
        ) {
            notesRepository.deleteNote(noteId)
        }
    }

    /**
     * Clears any error state.
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Refreshes the notes list.
     */
    fun refresh() {
        _isLoading.value = true
        _error.value = null
        // The flow will automatically emit new data
    }
}

/**
 * Optimized UI state sealed class with stable data structures.
 */
sealed class OptimizedNotesUiState {
    object Loading : OptimizedNotesUiState()
    object Empty : OptimizedNotesUiState()
    data class Success(val notes: List<Note>) : OptimizedNotesUiState()
    data class Error(val error: AppError) : OptimizedNotesUiState()
}