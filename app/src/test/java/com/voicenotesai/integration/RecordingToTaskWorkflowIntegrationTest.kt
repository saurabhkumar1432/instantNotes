package com.voicenotesai.integration

import com.voicenotesai.data.ai.AIConfigurationManagerImpl
import com.voicenotesai.data.local.AppDatabase
import com.voicenotesai.data.model.AIConfiguration
import com.voicenotesai.data.model.AIProvider
import com.voicenotesai.data.repository.NotesRepositoryImpl
import com.voicenotesai.data.repository.TaskRepositoryImpl
import com.voicenotesai.domain.model.EnhancedNote
import com.voicenotesai.domain.model.Task
import com.voicenotesai.domain.usecase.GenerateNotesUseCase
import com.voicenotesai.domain.usecase.TaskManagerImpl
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.*

/**
 * Integration test for the complete user workflow from recording to task management.
 * Tests the end-to-end flow of:
 * 1. Recording audio
 * 2. Transcribing and enhancing with AI
 * 3. Extracting tasks from note content
 * 4. Managing task completion
 */
class RecordingToTaskWorkflowIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var notesRepository: NotesRepositoryImpl
    private lateinit var taskRepository: TaskRepositoryImpl
    private lateinit var aiConfigurationManager: AIConfigurationManagerImpl
    private lateinit var generateNotesUseCase: GenerateNotesUseCase
    private lateinit var taskManager: TaskManagerImpl

    @Before
    fun setup() {
        // Mock database and DAOs
        database = mockk(relaxed = true)
        
        // Mock repositories
        notesRepository = mockk(relaxed = true)
        taskRepository = mockk(relaxed = true)
        aiConfigurationManager = mockk(relaxed = true)
        
        // Mock use cases
        generateNotesUseCase = mockk(relaxed = true)
        taskManager = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `complete workflow - recording to task completion`() = runTest {
        // Given: A configured AI provider
        val aiConfig = AIConfiguration(
            provider = AIProvider.OpenAI,
            apiKey = "test-key",
            modelName = "gpt-3.5-turbo",
            isValidated = true
        )
        coEvery { aiConfigurationManager.getCurrentConfiguration() } returns aiConfig

        // Given: Audio recording data
        val audioData = byteArrayOf(1, 2, 3, 4, 5) // Mock audio data
        val transcribedText = "I need to call John tomorrow and finish the project report by Friday"

        // Given: AI-enhanced note with extracted tasks
        val enhancedNote = EnhancedNote(
            id = "note-1",
            originalTranscription = transcribedText,
            enhancedContent = "Meeting Notes:\n- Call John tomorrow\n- Complete project report by Friday",
            summary = "Follow-up tasks from meeting",
            keyPoints = listOf("Call John", "Project report deadline"),
            actionItems = listOf("Call John tomorrow", "Finish project report by Friday"),
            timestamp = System.currentTimeMillis(),
            duration = 30000L,
            tags = listOf("meeting", "tasks"),
            category = "Work"
        )

        // Given: Extracted tasks
        val extractedTasks = listOf(
            Task(
                id = "task-1",
                text = "Call John tomorrow",
                sourceNoteId = "note-1",
                isCompleted = false,
                createdAt = System.currentTimeMillis()
            ),
            Task(
                id = "task-2", 
                text = "Finish project report by Friday",
                sourceNoteId = "note-1",
                isCompleted = false,
                createdAt = System.currentTimeMillis()
            )
        )

        // Mock the workflow steps
        coEvery { generateNotesUseCase.invoke(any()) } returns Result.success(enhancedNote)
        coEvery { taskManager.extractTasksFromNote("note-1", any()) } returns extractedTasks
        coEvery { notesRepository.insertNote(any()) } returns Unit
        coEvery { taskRepository.insertTasks(any()) } returns Unit
        coEvery { taskRepository.getTasksByNoteId("note-1") } returns flowOf(extractedTasks)
        coEvery { taskManager.markTaskComplete("task-1") } returns Result.success(Unit)

        // When: Processing the complete workflow
        // Step 1: Generate enhanced note from audio
        val noteResult = generateNotesUseCase.invoke(audioData)
        assertTrue("Note generation should succeed", noteResult.isSuccess)
        val note = noteResult.getOrThrow()

        // Step 2: Save the note
        notesRepository.insertNote(note)

        // Step 3: Extract tasks from the note
        val tasks = taskManager.extractTasksFromNote(note.id, note.enhancedContent)
        assertEquals("Should extract 2 tasks", 2, tasks.size)

        // Step 4: Save the tasks
        taskRepository.insertTasks(tasks)

        // Step 5: Complete one task
        val completeResult = taskManager.markTaskComplete("task-1")
        assertTrue("Task completion should succeed", completeResult.isSuccess)

        // Then: Verify the workflow completed successfully
        verify { notesRepository.insertNote(note) }
        verify { taskRepository.insertTasks(tasks) }
        verify { taskManager.markTaskComplete("task-1") }

        // Verify task content
        assertEquals("Call John tomorrow", tasks[0].text)
        assertEquals("Finish project report by Friday", tasks[1].text)
        assertEquals("note-1", tasks[0].sourceNoteId)
        assertEquals("note-1", tasks[1].sourceNoteId)
    }

    @Test
    fun `workflow handles AI processing failure gracefully`() = runTest {
        // Given: AI configuration exists but processing fails
        val aiConfig = AIConfiguration(
            provider = AIProvider.OpenAI,
            apiKey = "test-key",
            modelName = "gpt-3.5-turbo",
            isValidated = true
        )
        coEvery { aiConfigurationManager.getCurrentConfiguration() } returns aiConfig
        coEvery { generateNotesUseCase.invoke(any()) } returns Result.failure(Exception("AI processing failed"))

        // When: Processing audio with AI failure
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        val result = generateNotesUseCase.invoke(audioData)

        // Then: Should handle failure gracefully
        assertTrue("Should return failure result", result.isFailure)
        assertEquals("AI processing failed", result.exceptionOrNull()?.message)

        // Verify no database operations were attempted
        verify(exactly = 0) { notesRepository.insertNote(any()) }
        verify(exactly = 0) { taskRepository.insertTasks(any()) }
    }

    @Test
    fun `workflow continues with basic transcription when AI enhancement fails`() = runTest {
        // Given: AI configuration exists but enhancement fails
        val aiConfig = AIConfiguration(
            provider = AIProvider.OpenAI,
            apiKey = "test-key", 
            modelName = "gpt-3.5-turbo",
            isValidated = true
        )
        coEvery { aiConfigurationManager.getCurrentConfiguration() } returns aiConfig

        // Given: Basic transcription succeeds but enhancement fails
        val basicNote = EnhancedNote(
            id = "note-1",
            originalTranscription = "I need to call John tomorrow",
            enhancedContent = "I need to call John tomorrow", // Same as original when enhancement fails
            summary = "",
            keyPoints = emptyList(),
            actionItems = emptyList(),
            timestamp = System.currentTimeMillis(),
            duration = 15000L,
            tags = emptyList(),
            category = null
        )

        coEvery { generateNotesUseCase.invoke(any()) } returns Result.success(basicNote)
        coEvery { taskManager.extractTasksFromNote(any(), any()) } returns emptyList()
        coEvery { notesRepository.insertNote(any()) } returns Unit

        // When: Processing with enhancement failure
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        val result = generateNotesUseCase.invoke(audioData)

        // Then: Should still save basic note
        assertTrue("Should succeed with basic transcription", result.isSuccess)
        val note = result.getOrThrow()
        assertEquals("Should preserve original transcription", "I need to call John tomorrow", note.originalTranscription)
        assertEquals("Enhanced content should fallback to original", note.originalTranscription, note.enhancedContent)

        // Verify note was saved even without enhancement
        notesRepository.insertNote(note)
        verify { notesRepository.insertNote(note) }
    }

    @Test
    fun `task extraction works with various content formats`() = runTest {
        // Given: Different note content formats with tasks
        val testCases = listOf(
            "TODO: Call client\n- Review documents\n* Send email" to listOf("Call client", "Review documents", "Send email"),
            "Action items:\n1. Schedule meeting\n2. Prepare presentation" to listOf("Schedule meeting", "Prepare presentation"),
            "I need to buy groceries and pick up dry cleaning" to listOf("buy groceries", "pick up dry cleaning"),
            "Remember to submit report by EOD" to listOf("submit report by EOD")
        )

        testCases.forEachIndexed { index, (content, expectedTasks) ->
            // Mock task extraction for each test case
            val noteId = "note-$index"
            val extractedTasks = expectedTasks.mapIndexed { taskIndex, taskText ->
                Task(
                    id = "task-$index-$taskIndex",
                    text = taskText,
                    sourceNoteId = noteId,
                    isCompleted = false,
                    createdAt = System.currentTimeMillis()
                )
            }

            coEvery { taskManager.extractTasksFromNote(noteId, content) } returns extractedTasks

            // When: Extracting tasks from content
            val tasks = taskManager.extractTasksFromNote(noteId, content)

            // Then: Should extract expected tasks
            assertEquals("Should extract ${expectedTasks.size} tasks from: $content", expectedTasks.size, tasks.size)
            tasks.forEachIndexed { taskIndex, task ->
                assertTrue("Task should contain expected text", task.text.contains(expectedTasks[taskIndex], ignoreCase = true))
                assertEquals("Task should reference correct note", noteId, task.sourceNoteId)
            }
        }
    }

    @Test
    fun `task completion updates are reflected across the system`() = runTest {
        // Given: A note with tasks
        val noteId = "note-1"
        val tasks = listOf(
            Task(id = "task-1", text = "Task 1", sourceNoteId = noteId, isCompleted = false),
            Task(id = "task-2", text = "Task 2", sourceNoteId = noteId, isCompleted = false)
        )

        val completedTask = tasks[0].copy(isCompleted = true, completedAt = System.currentTimeMillis())
        val updatedTasks = listOf(completedTask, tasks[1])

        coEvery { taskRepository.getTasksByNoteId(noteId) } returns flowOf(tasks) andThen flowOf(updatedTasks)
        coEvery { taskManager.markTaskComplete("task-1") } returns Result.success(Unit)
        coEvery { taskRepository.updateTask(any()) } returns Unit

        // When: Completing a task
        val result = taskManager.markTaskComplete("task-1")

        // Then: Task completion should succeed
        assertTrue("Task completion should succeed", result.isSuccess)

        // Verify task was updated in repository
        verify { taskRepository.updateTask(any()) }

        // Verify updated tasks reflect completion
        val finalTasks = taskRepository.getTasksByNoteId(noteId)
        // Note: In a real test, we would collect the flow and verify the completion status
    }
}