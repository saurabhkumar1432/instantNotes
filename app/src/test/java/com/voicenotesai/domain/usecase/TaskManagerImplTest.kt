package com.voicenotesai.domain.usecase

import com.voicenotesai.data.model.AIProvider
import com.voicenotesai.data.model.AISettings
import com.voicenotesai.data.repository.AIRepository
import com.voicenotesai.data.repository.SettingsRepository
import com.voicenotesai.data.repository.TaskRepository
import com.voicenotesai.domain.model.Task
import com.voicenotesai.domain.model.TaskPriority
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TaskManagerImpl.
 * 
 * Tests the core task management functionality including:
 * - AI-powered task extraction
 * - Manual task creation
 * - Task completion tracking
 */
class TaskManagerImplTest {
    
    private lateinit var taskManager: TaskManagerImpl
    private lateinit var taskRepository: TaskRepository
    private lateinit var aiRepository: AIRepository
    private lateinit var settingsRepository: SettingsRepository
    
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
    fun `extractTasksFromNote should return success when AI extracts tasks`() = runTest {
        // Given
        val noteId = "note123"
        val content = "Call John tomorrow and send email to team by Friday"
        val aiSettings = AISettings(
            provider = AIProvider.OpenAI,
            apiKey = "test-key",
            model = "gpt-3.5-turbo"
        )
        val aiResponse = "Call John tomorrow\nSend email to team by Friday"
        
        coEvery { settingsRepository.getAISettings() } returns aiSettings
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
        assertEquals(2, result.extractedTasks.size)
        assertEquals("Call John tomorrow", result.extractedTasks[0])
        assertEquals("Send email to team by Friday", result.extractedTasks[1])
        assertEquals(0.8f, result.confidence)
    }
    
    @Test
    fun `extractTasksFromNote should return empty list when no tasks found`() = runTest {
        // Given
        val noteId = "note123"
        val content = "This is just a regular note with no action items"
        val aiSettings = AISettings(
            provider = AIProvider.OpenAI,
            apiKey = "test-key",
            model = "gpt-3.5-turbo"
        )
        
        coEvery { settingsRepository.getAISettings() } returns aiSettings
        coEvery { 
            aiRepository.generateNotes(
                provider = any(),
                apiKey = any(),
                model = any(),
                transcribedText = any(),
                promptTemplate = any()
            )
        } returns Result.success("NO_TASKS_FOUND")
        
        // When
        val result = taskManager.extractTasksFromNote(noteId, content)
        
        // Then
        assertTrue(result.success)
        assertTrue(result.extractedTasks.isEmpty())
        assertEquals(1.0f, result.confidence)
    }
    
    @Test
    fun `extractTasksFromNote should return error when AI provider not configured`() = runTest {
        // Given
        val noteId = "note123"
        val content = "Call John tomorrow"
        val aiSettings = AISettings(
            provider = AIProvider.OpenAI,
            apiKey = "", // Empty API key
            model = "gpt-3.5-turbo"
        )
        
        coEvery { settingsRepository.getAISettings() } returns aiSettings
        
        // When
        val result = taskManager.extractTasksFromNote(noteId, content)
        
        // Then
        assertFalse(result.success)
        assertEquals("AI provider not configured. Please configure AI settings first.", result.error)
    }
    
    @Test
    fun `createManualTask should create task with correct properties`() = runTest {
        // Given
        val taskText = "Buy groceries"
        val dueDate = System.currentTimeMillis() + 86400000L // Tomorrow
        val priority = TaskPriority.HIGH
        val category = "Personal"
        
        val expectedTask = Task(
            text = taskText,
            dueDate = dueDate,
            priority = priority,
            category = category
        )
        
        coEvery { taskRepository.insertTask(any()) } returns Result.success(expectedTask)
        
        // When
        val result = taskManager.createManualTask(taskText, dueDate, priority, category)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { taskRepository.insertTask(any()) }
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
        coVerify { taskRepository.insertTasks(any()) }
    }
    
    @Test
    fun `markTaskComplete should call repository with correct task ID`() = runTest {
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
    fun `markTaskIncomplete should call repository with correct task ID`() = runTest {
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
    fun `deleteTask should call repository with correct task ID`() = runTest {
        // Given
        val taskId = "task123"
        
        coEvery { taskRepository.deleteTask(taskId) } returns Result.success(Unit)
        
        // When
        val result = taskManager.deleteTask(taskId)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { taskRepository.deleteTask(taskId) }
    }
}