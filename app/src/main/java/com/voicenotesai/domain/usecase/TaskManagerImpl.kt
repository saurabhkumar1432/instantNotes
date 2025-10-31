package com.voicenotesai.domain.usecase

import com.voicenotesai.data.repository.AIRepository
import com.voicenotesai.data.repository.SettingsRepository
import com.voicenotesai.data.repository.TaskRepository
import com.voicenotesai.domain.model.Task
import com.voicenotesai.domain.model.TaskExtractionResult
import com.voicenotesai.domain.model.TaskPriority
import com.voicenotesai.domain.model.TaskWithNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TaskManager that handles task extraction, creation, and management.
 * 
 * This implementation provides:
 * - AI-powered task extraction from note content
 * - Manual task creation and management
 * - Task completion tracking with timestamps
 * - Integration with configured AI providers
 */
@Singleton
class TaskManagerImpl @Inject constructor(
    private val taskRepository: TaskRepository,
    private val aiRepository: AIRepository,
    private val settingsRepository: SettingsRepository
) : TaskManager {
    
    companion object {
        private val TASK_EXTRACTION_PROMPT = """
            Analyze the following text and extract any action items, tasks, todos, or commitments.
            
            Look for:
            - Action items (e.g., "Call John tomorrow", "Send email to team")
            - Todo items (e.g., "Buy groceries", "Fix the bug")
            - Deadlines and commitments (e.g., "Submit report by Friday")
            - Follow-up actions (e.g., "Schedule meeting with team")
            - Reminders (e.g., "Remember to pick up dry cleaning")
            
            Return only the task descriptions, one per line, without any additional formatting or numbering.
            If no tasks are found, return "NO_TASKS_FOUND".
            
            Text to analyze:
            {transcription}
        """.trimIndent()
    }
    
    override suspend fun extractTasksFromNote(noteId: String, content: String): TaskExtractionResult {
        return try {
            // Get current AI settings
            val aiSettings = settingsRepository.getSettings().first()
            
            if (aiSettings?.apiKey?.isBlank() != false) {
                return TaskExtractionResult(
                    success = false,
                    error = "AI provider not configured. Please configure AI settings first."
                )
            }
            
            // Prepare the prompt with the note content
            val prompt = TASK_EXTRACTION_PROMPT.replace("{transcription}", content)
            
            // Call AI service to extract tasks
            val result = aiRepository.generateNotes(
                provider = aiSettings!!.provider,
                apiKey = aiSettings.apiKey,
                model = aiSettings.model,
                transcribedText = content,
                promptTemplate = prompt
            )
            
            result.fold(
                onSuccess = { response ->
                    if (response.trim() == "NO_TASKS_FOUND" || response.isBlank()) {
                        TaskExtractionResult(
                            success = true,
                            extractedTasks = emptyList(),
                            confidence = 1.0f
                        )
                    } else {
                        val tasks = response.lines()
                            .map { it.trim() }
                            .filter { it.isNotBlank() && it != "NO_TASKS_FOUND" }
                        
                        TaskExtractionResult(
                            success = true,
                            extractedTasks = tasks,
                            confidence = 0.8f // Default confidence for AI extraction
                        )
                    }
                },
                onFailure = { exception ->
                    TaskExtractionResult(
                        success = false,
                        error = "Failed to extract tasks: ${exception.message}"
                    )
                }
            )
        } catch (e: Exception) {
            TaskExtractionResult(
                success = false,
                error = "Unexpected error during task extraction: ${e.message}"
            )
        }
    }
    
    override suspend fun createTasksFromExtraction(noteId: String, taskTexts: List<String>): Result<List<Task>> {
        return try {
            val tasks = taskTexts.map { taskText ->
                Task(
                    text = taskText,
                    sourceNoteId = noteId,
                    priority = TaskPriority.NORMAL
                )
            }
            
            taskRepository.insertTasks(tasks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun createManualTask(
        text: String,
        dueDate: Long?,
        priority: TaskPriority
    ): Result<Task> {
        return try {
            val task = Task(
                text = text.trim(),
                dueDate = dueDate,
                priority = priority
            )
            
            taskRepository.insertTask(task)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun markTaskComplete(taskId: String): Result<Unit> {
        return taskRepository.markTaskCompleted(taskId)
    }
    
    override suspend fun markTaskIncomplete(taskId: String): Result<Unit> {
        return taskRepository.markTaskIncomplete(taskId)
    }
    
    override suspend fun deleteTask(taskId: String): Result<Unit> {
        return taskRepository.deleteTask(taskId)
    }
    
    override suspend fun getTasksByStatus(completed: Boolean): Flow<List<TaskWithNote>> {
        return taskRepository.getTasksByStatusWithNotes(completed)
    }
    
    override suspend fun getAllTasks(): Flow<List<TaskWithNote>> {
        return taskRepository.getAllTasksWithNotes()
    }
    
    override suspend fun getPendingTasksCount(): Flow<Int> {
        return taskRepository.getPendingTasksCount()
    }
    
    override suspend fun getTasksForNote(noteId: String): Flow<List<Task>> {
        return taskRepository.getTasksForNote(noteId)
    }
    
    override suspend fun updateTask(task: Task): Result<Unit> {
        return taskRepository.updateTask(task)
    }
    
    /**
     * Convenience method to extract tasks and create them in a single operation.
     */
    override suspend fun extractAndCreateTasks(noteId: String, content: String): Result<List<Task>> {
        val extractionResult = extractTasksFromNote(noteId, content)
        
        return if (extractionResult.success && extractionResult.extractedTasks.isNotEmpty()) {
            createTasksFromExtraction(noteId, extractionResult.extractedTasks)
        } else if (extractionResult.success) {
            // No tasks found, return empty list as success
            Result.success(emptyList())
        } else {
            // Extraction failed
            Result.failure(Exception(extractionResult.error ?: "Task extraction failed"))
        }
    }
}