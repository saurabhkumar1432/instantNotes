package com.voicenotesai.presentation.tasks

import com.voicenotesai.data.repository.NotesRepository
import com.voicenotesai.data.repository.TaskRepository
import com.voicenotesai.domain.model.EnhancedNote
import com.voicenotesai.domain.model.Task
import com.voicenotesai.domain.model.TaskFilter
import com.voicenotesai.domain.model.TaskPriority
import com.voicenotesai.domain.model.TaskWithNote
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for TasksViewModel to verify task management functionality.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {

    private lateinit var taskRepository: TaskRepository
    private lateinit var notesRepository: NotesRepository
    private lateinit var viewModel: TasksViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    private val sampleTask = Task(
        id = "task1",
        text = "Sample task",
        isCompleted = false,
        sourceNoteId = "1",
        createdAt = System.currentTimeMillis(),
        priority = TaskPriority.NORMAL
    )

    private val sampleNote = EnhancedNote(
        id = 1L,
        title = "Sample Note",
        content = "Sample note content",
        transcribedText = "Sample transcribed text",
        timestamp = System.currentTimeMillis(),
        duration = 60000L,
        tags = listOf("sample"),
        category = "work"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        taskRepository = mockk(relaxed = true)
        notesRepository = mockk(relaxed = true)
        
        // Mock default responses
        every { taskRepository.getAllTasksWithNotes() } returns flowOf(
            listOf(TaskWithNote(sampleTask, null))
        )
        coEvery { notesRepository.getNoteById(1L) } returns sampleNote
        coEvery { taskRepository.markTaskCompleted(any()) } returns Result.success(Unit)
        coEvery { taskRepository.markTaskIncomplete(any()) } returns Result.success(Unit)
        coEvery { taskRepository.deleteTask(any()) } returns Result.success(Unit)
        coEvery { taskRepository.insertTask(any()) } returns Result.success(sampleTask)

        viewModel = TasksViewModel(taskRepository, notesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads tasks correctly`() = runTest {
        // Given - ViewModel is initialized in setup()

        // When - Initial state is loaded
        val uiState = viewModel.uiState.value

        // Then
        assertFalse(uiState.isLoading)
        assertEquals(1, uiState.tasks.size)
        assertEquals(sampleTask.id, uiState.tasks.first().task.id)
        verify { taskRepository.getAllTasksWithNotes() }
    }

    @Test
    fun `setFilter updates selected filter`() = runTest {
        // Given
        val newFilter = TaskFilter.COMPLETED

        // When
        viewModel.setFilter(newFilter)

        // Then
        assertEquals(newFilter, viewModel.selectedFilter.value)
    }

    @Test
    fun `toggleTaskComplete calls repository correctly for incomplete task`() = runTest {
        // Given
        val taskId = "task1"

        // When
        viewModel.toggleTaskComplete(taskId)

        // Then
        coVerify { taskRepository.markTaskCompleted(taskId) }
    }

    @Test
    fun `toggleTaskComplete calls repository correctly for completed task`() = runTest {
        // Given
        val completedTask = sampleTask.copy(isCompleted = true)
        every { taskRepository.getAllTasksWithNotes() } returns flowOf(
            listOf(TaskWithNote(completedTask, null))
        )
        viewModel = TasksViewModel(taskRepository, notesRepository)
        val taskId = "task1"

        // When
        viewModel.toggleTaskComplete(taskId)

        // Then
        coVerify { taskRepository.markTaskIncomplete(taskId) }
    }

    @Test
    fun `deleteTask calls repository correctly`() = runTest {
        // Given
        val taskId = "task1"

        // When
        viewModel.deleteTask(taskId)

        // Then
        coVerify { taskRepository.deleteTask(taskId) }
    }

    @Test
    fun `showAddTaskDialog updates dialog state`() = runTest {
        // When
        viewModel.showAddTaskDialog()

        // Then
        assertTrue(viewModel.showAddTaskDialog.value)
        assertEquals("", viewModel.newTaskText.value)
    }

    @Test
    fun `hideAddTaskDialog updates dialog state`() = runTest {
        // Given
        viewModel.showAddTaskDialog()
        viewModel.updateNewTaskText("Some text")

        // When
        viewModel.hideAddTaskDialog()

        // Then
        assertFalse(viewModel.showAddTaskDialog.value)
        assertEquals("", viewModel.newTaskText.value)
    }

    @Test
    fun `updateNewTaskText updates text correctly`() = runTest {
        // Given
        val newText = "New task text"

        // When
        viewModel.updateNewTaskText(newText)

        // Then
        assertEquals(newText, viewModel.newTaskText.value)
    }

    @Test
    fun `createTask creates task and hides dialog`() = runTest {
        // Given
        val taskText = "New task"
        viewModel.showAddTaskDialog()
        viewModel.updateNewTaskText(taskText)

        // When
        viewModel.createTask()

        // Then
        coVerify { 
            taskRepository.insertTask(
                match { it.text == taskText && it.priority == TaskPriority.NORMAL }
            )
        }
        assertFalse(viewModel.showAddTaskDialog.value)
    }

    @Test
    fun `createTask does nothing with blank text`() = runTest {
        // Given
        viewModel.showAddTaskDialog()
        viewModel.updateNewTaskText("   ") // Blank text

        // When
        viewModel.createTask()

        // Then
        coVerify(exactly = 0) { taskRepository.insertTask(any()) }
        assertTrue(viewModel.showAddTaskDialog.value) // Dialog should remain open
    }

    @Test
    fun `getFilterCounts returns correct counts`() = runTest {
        // Given
        val completedTask = sampleTask.copy(id = "task2", isCompleted = true)
        every { taskRepository.getAllTasksWithNotes() } returns flowOf(
            listOf(
                TaskWithNote(sampleTask, null),
                TaskWithNote(completedTask, null)
            )
        )
        viewModel = TasksViewModel(taskRepository, notesRepository)

        // When
        val filterCounts = viewModel.getFilterCounts()

        // Then
        assertEquals(2, filterCounts.all)
        assertEquals(1, filterCounts.pending)
        assertEquals(1, filterCounts.completed)
    }

    @Test
    fun `clearError resets error state`() = runTest {
        // Given - Set an error state first
        coEvery { taskRepository.deleteTask(any()) } returns Result.failure(RuntimeException("Test error"))
        viewModel.deleteTask("task1")
        assertTrue(viewModel.uiState.value.error != null)

        // When
        viewModel.clearError()

        // Then
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `error handling works correctly for failed operations`() = runTest {
        // Given
        val errorMessage = "Delete failed"
        coEvery { taskRepository.deleteTask(any()) } returns Result.failure(RuntimeException(errorMessage))

        // When
        viewModel.deleteTask("task1")

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.error?.contains("Delete failed") == true)
    }
}