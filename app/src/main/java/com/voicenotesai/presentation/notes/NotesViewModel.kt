package com.voicenotesai.presentation.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicenotesai.data.local.entity.Note
import com.voicenotesai.data.repository.NotesRepository
import com.voicenotesai.domain.model.AppError
import com.voicenotesai.domain.model.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the notes screen that handles loading and managing saved notes.
 */
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val notesRepository: NotesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotesUiState>(NotesUiState.Loading)
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    init {
        loadNotes()
    }

    /**
     * Loads all notes from the repository.
     */
    private fun loadNotes() {
        viewModelScope.launch {
            notesRepository.getAllNotes()
                .catch { error ->
                    val appError = AppError.StorageError(error.message ?: "Failed to load notes")
                    _uiState.value = NotesUiState.Error(appError)
                }
                .collect { notes ->
                    _uiState.value = if (notes.isEmpty()) {
                        NotesUiState.Empty
                    } else {
                        NotesUiState.Success(notes)
                    }
                }
        }
    }

    /**
     * Deletes a note by its ID.
     */
    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            try {
                notesRepository.deleteNote(noteId)
                // Notes will be automatically updated via Flow
            } catch (e: Exception) {
                val appError = AppError.StorageError(e.message ?: "Failed to delete note")
                _uiState.value = NotesUiState.Error(appError)
            }
        }
    }

    /**
     * Clears any error messages and reloads notes.
     */
    fun clearError() {
        loadNotes()
    }
}

/**
 * UI state for the notes screen.
 */
sealed class NotesUiState {
    object Loading : NotesUiState()
    object Empty : NotesUiState()
    data class Success(val notes: List<Note>) : NotesUiState()
    data class Error(val error: AppError) : NotesUiState()
}
