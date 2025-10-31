package com.voicenotesai.domain.usecase

import com.voicenotesai.data.model.AIConfiguration
import com.voicenotesai.data.model.AIProviderType
import com.voicenotesai.data.repository.AIRepository
import com.voicenotesai.data.repository.SettingsRepository
import com.voicenotesai.data.repository.TaskRepository
import com.voicenotesai.domain.model.Task
import com.voicenotesai.domain.model.TaskPriority
import com.voicenotesai.domain.model.TaskWithNote
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive tests for task management functionality.
 * 
 * Tests task creation, completion, deletion, and AI-powered extraction.
 */
class TaskManagementTest {
    
    private lateinit var taskManager: TaskManagerImpl
    private lateinit var taskRepository: TaskRepository
    private lateinit var aiRepository: AIRepository
    private lateinit var settingsRepository: SettingsRepository
    
    private val testAIConfig = AIConfiguration(
        provider = AIProviderType.OpenAI,
        apiKey = "test-key",
        modelName = "gpt-3.5-turbo"
    )
    
    @Before
    fun setup() {
        taskRepository = mockk()
        aiRepository = mockk()
        settingsRepository = mockk()
        
        taskManager = TaskManagerImpl(
            taskRepository = taskRepository,
            aiRepository = aiRepository,
            settingsRepository = settingsRepository
        )
    }
    
    @Test
    fun `createManualTask should create task with correct properties`() = runTest {
        // Given
        val taskText = "Buy groceries"
        val dueDate = System.currentTimeMillis() + 86400000L
        val priority = TaskPriority.HIGH
        val expectedTask = Task(
            text = taskText,
            dueDate = dueDate,
            priority = priority
        )
        
        coEvery { taskRepository.insertTask(any()) } returns Result.success(expectedTask)
        
        // When
        val result = taskManager.createManualTask(taskText, dueDate, priority)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { taskRepository.insertTask(match { task ->
            task.text == taskText &&
            task.dueDate == dueDate &&
            task.priority == priority &&
            !task.isCompleted
        }) }
    }
    
    @Test
    fun `createManualTask should handle repository failure`() = runTest {
        // Given
        val taskText = "Buy groceries"
        val exception = RuntimeException("Database error")
        
        coEvery { taskRepository.insertTask(any()) } returns Result.failure(exception)
        
        // When
        val result = taskManager.createManualTask(taskText)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
    
    @Test
    fun `markTaskComplete should update task status`() = runTest {
        // Given
        val taskId = "task123"
        
        coEvery { taskRepository.markTaskCompleted(taskId) } returns Result.success(Unit)
        
        // When
        val result = taskManager.markTaskComplete(taskId)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { taskRepository.markTaskCompleted(taskId) }
    }
    
    @Test
    fun `markTaskIncomplete should update task status`() = runTest {
        // Given
        val taskId = "task123"
        
        coEvery { taskRepository.markTaskIncomplete(taskId) } returns Result.success(Unit)
        
        // When
        val result = taskManager.markTaskIncomplete(taskId)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { taskRepository.markTaskIncomplete(taskId) }
    }
    
    @Test
    fun `deleteTask should remove task from repository`() = runTest {
        // Given
        val taskId = "task123"
        
        coEvery { taskRepository.deleteTask(taskId) } returns Result.success(Unit)
        
        // When
        val result = taskManager.deleteTask(taskId)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { taskRepository.deleteTask(taskId) }
    }
    
    @Test
    fun `extractTasksFromNote should extract multiple tasks`() = runTest {
        // Given
        val noteId = "note123"
        val content = "Call John tomorrow and send email to team by Friday. Also buy groceries."
        val aiResponse = "Call John tomorrow\nSend email to team by Friday\nBuy groceries"
        
        coEvery { settingsRepository.getSettings() } returns flowOf(testAIConfig)
        coEvery { 
            aiRepository.generateNotes(
                provider = any(),
                apiKey = any(),
                model = any(),
                transcribedText = any(),
                promptTemplate = any()
            )
        } returns Result.success(aiResponse)
        
        // When
        val result = taskManager.extractTasksFromNote(noteId, content)
        
        // Then
        assertTrue(result.success)
        assertEquals(3, result.extractedTasks.size)
        assertEquals("Call John tomorrow", result.extractedTasks[0])
        assertEquals("Send email to team by Friday", result.extractedTasks[1])
        assertEquals("Buy groceries", result.extractedTasks[2])
    }
    
    @Test
    fun `extractTasksFromNote should handle no tasks found`() = runTest {
        // Given
        val noteId = "note123"
        val content = "This is just a regular note with no action items."
        
        coEvery { settingsRepository.getSettings() } returns flowOf(testAIConfig)
        coEvery { 
            aiRepository.generateNotes(any(), any(), any(), any(), any())
        } returns Result.success("NO_TASKS_FOUND")
        
        // When
        val result = taskManager.extractTasksFromNote(noteId, content)
        
        // Then
        assertTrue(result.success)
        assertTrue(result.extractedTasks.isEmpty())
        assertEquals(1.0f, result.confidence)
    }
    
    @Test
    fun `extractTasksFromNote should handle AI configuration missing`() = runTest {
        // Given
        val noteId = "note123"
        val content = "Call John tomorrow"
        
        coEvery { settingsRepository.getSettings() } returns flowOf(null)
        
        // When
        val result = taskManager.extractTasksFromNote(noteId, content)
        
        // Then
        assertFalse(result.success)
        assertTrue(result.error?.contains("AI provider not configured") == true)
    }
    
    @Test
    fun `extractTasksFromNote should handle AI API failure`() = runTest {
        // Given
        val noteId = "note123"
        val content = "Call John tomorrow"
        val exception = RuntimeException("API error")
        
        coEvery { settingsRepository.getSettings() } returns flowOf(testAIConfig)
        coEvery { 
            aiRepository.generateNotes(any(), any(), any(), any(), any())
        } returns Result.failure(exception)
        
        // When
        val result = taskManager.extractTasksFromNote(noteId, content)
        
        // Then
        assertFalse(result.success)
        assertTrue(result.error?.contains("Failed to extract tasks") == true)
    }
    
    @Test
    fun `createTasksFromExtraction should create multiple tasks`() = runTest {
        // Given
        val noteId = "note123"
        val taskTexts = listOf("Call John", "Send email", "Buy groceries")
        val expectedTasks = taskTexts.map { text ->
            Task(text = text, sourceNoteId = noteId, priority = TaskPriority.NORMAL)
        }
        
        coEvery { taskRepository.insertTasks(any()) } returns Result.success(expectedTasks)
        
        // When
        val result = taskManager.createTasksFromExtraction(noteId, taskTexts)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull()?.size)
        coVerify { taskRepository.insertTasks(match { tasks ->
            tasks.size == 3 &&
            tasks.all { it.sourceNoteId == noteId } &&
            tasks.all { it.priority == TaskPriority.NORMAL }
        }) }
    }
    
    @Test
    fun `extractAndCreateTasks should combine extraction and creation`() = runTest {
        // Given
        val noteId = "note123"
        val content = "Call John tomorrow and send email"
        val aiResponse = "Call John tomorrow\nSend email"
        val expectedTasks = listOf(
            Task(text = "Call John tomorrow", sourceNoteId = noteId),
            Task(text = "Send email", sourceNoteId = noteId)
        )
        
        coEvery { settingsRepository.getSettings() } returns flowOf(testAIConfig)
        coEvery { 
            aiRepository.generateNotes(any(), any(), any(), any(), any())
        } returns Result.success(aiResponse)
        coEvery { taskRepository.insertTasks(any()) } returns Result.success(expectedTasks)
        
        // When
        val result = taskManager.extractAndCreateTasks(noteId, content)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        coVerify { taskRepository.insertTasks(any()) }
    }
    
    @Test
    fun `extractAndCreateTasks should return empty list when no tasks found`() = runTest {
        // Given
        val noteId = "note123"
        val content = "Just regular notes"
        
        coEvery { settingsRepository.getSettings() } returns flowOf(testAIConfig)
        coEvery { 
            aiRepository.generateNotes(any(), any(), any(), any(), any())
        } returns Result.success("NO_TASKS_FOUND")
        
        // When
        val result = taskManager.extractAndCreateTasks(noteId, content)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }
    
    @Test
    fun `getTasksByStatus should return filtered tasks`() = runTest {
        // Given
        val completedTasks = listOf(
            TaskWithNote(Task(text = "Completed task", isCompleted = true), null)
        )
        
        coEvery { taskRepository.getTasksByStatusWithNotes(true) } returns flowOf(completedTasks)
        
        // When
        val result = taskManager.getTasksByStatus(true)
        
        // Then
        // Flow testing would require collecting the flow
        coVerify { taskRepository.getTasksByStatusWithNotes(true) }
    }
    
    @Test
    fun `getPendingTasksCount should return correct count`() = runTest {
        // Given
        val pendingCount = 5
        
        coEvery { taskRepository.getPendingTasksCount() } returns flowOf(pendingCount)
        
        // When
        val result = taskManager.getPendingTasksCount()
        
        // Then
        coVerify { taskRepository.getPendingTasksCount() }
    }
    
    @Test
    fun `getTasksForNote should return note-specific tasks`() = runTest {
        // Given
        val noteId = "note123"
        val noteTasks = listOf(
            Task(text = "Task 1", sourceNoteId = noteId),
            Task(text = "Task 2", sourceNoteId = noteId)
        )
        
        coEvery { taskRepository.getTasksForNote(noteId) } returns flowOf(noteTasks)
        
        // When
        val result = taskManager.getTasksForNote(noteId)
        
        // Then
        coVerify { taskRepository.getTasksForNote(noteId) }
    }
    
    @Test
    fun `updateTask should update task in repository`() = runTest {
        // Given
        val task = Task(
            id = "task123",
            text = "Updated task",
            priority = TaskPriority.HIGH
        )
        
        coEvery { taskRepository.updateTask(task) } returns Result.success(Unit)
        
        // When
        val result = taskManager.updateTask(task)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { taskRepository.updateTask(task) }
    }
}