package com.voicenotesai.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicenotesai.data.repository.NotesRepository
import com.voicenotesai.data.repository.TaskRepository
import com.voicenotesai.domain.model.EnhancedNote
import com.voicenotesai.domain.model.Task
import com.voicenotesai.domain.model.TaskFilter
import com.voicenotesai.domain.model.TaskPriority
import com.voicenotesai.domain.model.TaskWithNote
import com.voicenotesai.domain.model.toEnhancedNote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Tasks screen handling task management operations.
 */
@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val notesRepository: NotesRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()
    
    private val _selectedFilter = MutableStateFlow(TaskFilter.ALL)
    val selectedFilter: StateFlow<TaskFilter> = _selectedFilter.asStateFlow()
    
    private val _showAddTaskDialog = MutableStateFlow(false)
    val showAddTaskDialog: StateFlow<Boolean> = _showAddTaskDialog.asStateFlow()
    
    private val _newTaskText = MutableStateFlow("")
    val newTaskText: StateFlow<String> = _newTaskText.asStateFlow()
    
    init {
        loadTasks()
    }
    
    /**
     * Loads tasks based on the current filter and combines with note data.
     */
    private fun loadTasks() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                combine(
                    taskRepository.getAllTasksWithNotes(),
                    _selectedFilter
                ) { tasksWithNotes, filter ->
                    // Filter tasks based on selected filter
                    val filteredTasks = when (filter) {
                        TaskFilter.ALL -> tasksWithNotes
                        TaskFilter.PENDING -> tasksWithNotes.filter { !it.task.isCompleted }
                        TaskFilter.COMPLETED -> tasksWithNotes.filter { it.task.isCompleted }
                    }
                    
                    // Enhance with note data
                    val enhancedTasks = filteredTasks.map { taskWithNote ->
                        val note = if (taskWithNote.task.sourceNoteId != null) {
                            try {
                                notesRepository.getNoteById(taskWithNote.task.sourceNoteId.toLong())?.toEnhancedNote()
                            } catch (e: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                        TaskWithNote(taskWithNote.task, note)
                    }
                    
                    // Sort by creation date (newest first), but put incomplete tasks first
                    val sortedTasks = enhancedTasks.sortedWith(
                        compareBy<TaskWithNote> { it.task.isCompleted }
                            .thenByDescending { it.task.createdAt }
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        tasks = sortedTasks,
                        isLoading = false,
                        error = null
                    )
                }.collect { }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load tasks: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Updates the selected filter and reloads tasks.
     */
    fun setFilter(filter: TaskFilter) {
        _selectedFilter.value = filter
    }
    
    /**
     * Toggles the completion status of a task.
     */
    fun toggleTaskComplete(taskId: String) {
        viewModelScope.launch {
            try {
                val task = _uiState.value.tasks.find { it.task.id == taskId }?.task
                if (task != null) {
                    if (task.isCompleted) {
                        taskRepository.markTaskIncomplete(taskId)
                    } else {
                        taskRepository.markTaskCompleted(taskId)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update task: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Deletes a task.
     */
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                taskRepository.deleteTask(taskId).fold(
                    onSuccess = {
                        // Task deleted successfully, UI will update automatically via Flow
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to delete task: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete task: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Shows the add task dialog.
     */
    fun showAddTaskDialog() {
        _showAddTaskDialog.value = true
        _newTaskText.value = ""
    }
    
    /**
     * Hides the add task dialog.
     */
    fun hideAddTaskDialog() {
        _showAddTaskDialog.value = false
        _newTaskText.value = ""
    }
    
    /**
     * Updates the new task text.
     */
    fun updateNewTaskText(text: String) {
        _newTaskText.value = text
    }
    
    /**
     * Creates a new manual task.
     */
    fun createTask() {
        val taskText = _newTaskText.value.trim()
        if (taskText.isBlank()) return
        
        viewModelScope.launch {
            try {
                val newTask = Task(
                    text = taskText,
                    priority = TaskPriority.NORMAL
                )
                
                taskRepository.insertTask(newTask).fold(
                    onSuccess = {
                        hideAddTaskDialog()
                        // UI will update automatically via Flow
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to create task: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to create task: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Clears the current error message.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Gets the count of tasks for each filter type.
     */
    fun getFilterCounts(): FilterCounts {
        val tasks = _uiState.value.tasks
        return FilterCounts(
            all = tasks.size,
            pending = tasks.count { !it.task.isCompleted },
            completed = tasks.count { it.task.isCompleted }
        )
    }
}

/**
 * UI state for the Tasks screen.
 */
data class TasksUiState(
    val tasks: List<TaskWithNote> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Filter counts for displaying badges on filter chips.
 */
data class FilterCounts(
    val all: Int,
    val pending: Int,
    val completed: Int
) {
    fun getCount(filter: TaskFilter): Int = when (filter) {
        TaskFilter.ALL -> all
        TaskFilter.PENDING -> pending
        TaskFilter.COMPLETED -> completed
    }
}