package com.voicenotesai.data.repository

import com.voicenotesai.domain.model.Task
import com.voicenotesai.domain.model.TaskWithNote
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for task data operations.
 * 
 * Provides an abstraction layer between the domain layer and data sources,
 * handling data mapping and caching strategies.
 */
interface TaskRepository {
    
    /**
     * Inserts a new task into the database.
     */
    suspend fun insertTask(task: Task): Result<Task>
    
    /**
     * Inserts multiple tasks in a single transaction.
     */
    suspend fun insertTasks(tasks: List<Task>): Result<List<Task>>
    
    /**
     * Updates an existing task.
     */
    suspend fun updateTask(task: Task): Result<Unit>
    
    /**
     * Deletes a task by ID.
     */
    suspend fun deleteTask(taskId: String): Result<Unit>
    
    /**
     * Gets a task by ID.
     */
    suspend fun getTaskById(taskId: String): Task?
    
    /**
     * Gets all tasks with associated note information.
     */
    fun getAllTasksWithNotes(): Flow<List<TaskWithNote>>
    
    /**
     * Gets tasks filtered by completion status with associated note information.
     */
    fun getTasksByStatusWithNotes(completed: Boolean): Flow<List<TaskWithNote>>
    
    /**
     * Gets tasks associated with a specific note.
     */
    fun getTasksForNote(noteId: String): Flow<List<Task>>
    
    /**
     * Gets the count of pending (incomplete) tasks.
     */
    fun getPendingTasksCount(): Flow<Int>
    
    /**
     * Gets the count of completed tasks.
     */
    fun getCompletedTasksCount(): Flow<Int>
    
    /**
     * Marks a task as completed with timestamp.
     */
    suspend fun markTaskCompleted(taskId: String): Result<Unit>
    
    /**
     * Marks a task as incomplete, removing completion timestamp.
     */
    suspend fun markTaskIncomplete(taskId: String): Result<Unit>
    
    /**
     * Gets tasks with upcoming due dates.
     */
    fun getTasksDueSoon(daysAhead: Int = 7): Flow<List<TaskWithNote>>
    
    /**
     * Gets overdue tasks.
     */
    fun getOverdueTasks(): Flow<List<TaskWithNote>>
    
    /**
     * Searches tasks by text content.
     */
    fun searchTasks(query: String): Flow<List<TaskWithNote>>
    
    /**
     * Gets tasks by priority level.
     */
    fun getTasksByPriority(priority: com.voicenotesai.domain.model.TaskPriority): Flow<List<TaskWithNote>>
    

}