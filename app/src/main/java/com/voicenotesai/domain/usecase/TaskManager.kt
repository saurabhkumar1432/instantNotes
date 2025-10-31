package com.voicenotesai.domain.usecase

import com.voicenotesai.domain.model.Task
import com.voicenotesai.domain.model.TaskExtractionResult
import com.voicenotesai.domain.model.TaskWithNote
import kotlinx.coroutines.flow.Flow

/**
 * Task management interface for handling task extraction, creation, and lifecycle management.
 * 
 * This interface provides comprehensive task management functionality including:
 * - AI-powered task extraction from note content
 * - Manual task creation and management
 * - Task completion tracking with timestamps
 * - Task filtering and organization
 */
interface TaskManager {
    
    /**
     * Extracts action items and tasks from note content using AI processing.
     * 
     * Uses the configured AI provider to analyze note content and identify:
     * - Action items (e.g., "Call John tomorrow")
     * - Todo items (e.g., "Buy groceries")
     * - Deadlines and commitments (e.g., "Submit report by Friday")
     * - Follow-up actions (e.g., "Schedule meeting with team")
     * 
     * @param noteId The ID of the source note
     * @param content The note content to analyze
     * @return TaskExtractionResult containing extracted tasks or error information
     */
    suspend fun extractTasksFromNote(noteId: String, content: String): TaskExtractionResult
    
    /**
     * Creates tasks from extracted task text and associates them with the source note.
     * 
     * @param noteId The ID of the source note
     * @param taskTexts List of task descriptions extracted from the note
     * @return Result containing the list of created tasks or error information
     */
    suspend fun createTasksFromExtraction(noteId: String, taskTexts: List<String>): Result<List<Task>>
    
    /**
     * Creates a manual task without requiring a source note.
     * 
     * Allows users to create tasks directly without voice notes, supporting:
     * - Custom task text
     * - Optional due dates
     * - Priority levels
     * 
     * @param text The task description
     * @param dueDate Optional due date timestamp
     * @param priority Task priority level (default: NORMAL)
     * @return Result containing the created task or error information
     */
    suspend fun createManualTask(
        text: String, 
        dueDate: Long? = null,
        priority: com.voicenotesai.domain.model.TaskPriority = com.voicenotesai.domain.model.TaskPriority.NORMAL
    ): Result<Task>
    
    /**
     * Marks a task as completed with timestamp tracking.
     * 
     * Updates the task status and records the completion timestamp for analytics
     * and progress tracking purposes.
     * 
     * @param taskId The ID of the task to mark as complete
     * @return Result indicating success or failure
     */
    suspend fun markTaskComplete(taskId: String): Result<Unit>
    
    /**
     * Marks a task as incomplete, removing completion timestamp.
     * 
     * Allows users to reopen completed tasks if needed.
     * 
     * @param taskId The ID of the task to mark as incomplete
     * @return Result indicating success or failure
     */
    suspend fun markTaskIncomplete(taskId: String): Result<Unit>
    
    /**
     * Deletes a task permanently.
     * 
     * @param taskId The ID of the task to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteTask(taskId: String): Result<Unit>
    
    /**
     * Retrieves tasks filtered by completion status with associated note information.
     * 
     * Returns a Flow for reactive updates when tasks change.
     * 
     * @param completed Filter for completed (true) or pending (false) tasks
     * @return Flow of tasks with associated note information
     */
    suspend fun getTasksByStatus(completed: Boolean): Flow<List<TaskWithNote>>
    
    /**
     * Retrieves all tasks with associated note information.
     * 
     * @return Flow of all tasks with associated note information
     */
    suspend fun getAllTasks(): Flow<List<TaskWithNote>>
    
    /**
     * Gets the count of pending (incomplete) tasks for dashboard display.
     * 
     * @return Flow of pending task count for reactive UI updates
     */
    suspend fun getPendingTasksCount(): Flow<Int>
    
    /**
     * Gets tasks associated with a specific note.
     * 
     * @param noteId The ID of the source note
     * @return Flow of tasks associated with the note
     */
    suspend fun getTasksForNote(noteId: String): Flow<List<Task>>
    
    /**
     * Updates an existing task with new information.
     * 
     * @param task The updated task information
     * @return Result indicating success or failure
     */
    suspend fun updateTask(task: Task): Result<Unit>
    
    /**
     * Convenience method to extract tasks and create them in a single operation.
     * 
     * @param noteId The ID of the source note
     * @param content The note content to analyze and extract tasks from
     * @return Result containing the list of created tasks or error information
     */
    suspend fun extractAndCreateTasks(noteId: String, content: String): Result<List<Task>>
}