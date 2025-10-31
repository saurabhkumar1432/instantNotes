package com.voicenotesai.data.local.dao

import androidx.room.*
import com.voicenotesai.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Task operations.
 * 
 * Provides efficient database operations for task management with proper indexing
 * and optimized queries for common use cases.
 */
@Dao
interface TaskDao {
    
    /**
     * Inserts a new task into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long
    
    /**
     * Inserts multiple tasks in a single transaction.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)
    
    /**
     * Updates an existing task.
     */
    @Update
    suspend fun updateTask(task: TaskEntity)
    
    /**
     * Deletes a task by ID.
     */
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)
    
    /**
     * Gets a task by ID.
     */
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?
    
    /**
     * Gets all tasks ordered by creation date (newest first).
     */
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>
    
    /**
     * Gets tasks filtered by completion status.
     */
    @Query("SELECT * FROM tasks WHERE isCompleted = :completed ORDER BY createdAt DESC")
    fun getTasksByStatus(completed: Boolean): Flow<List<TaskEntity>>
    
    /**
     * Gets tasks associated with a specific note.
     */
    @Query("SELECT * FROM tasks WHERE sourceNoteId = :noteId ORDER BY createdAt DESC")
    fun getTasksForNote(noteId: Long): Flow<List<TaskEntity>>
    
    /**
     * Gets the count of pending (incomplete) tasks.
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0")
    fun getPendingTasksCount(): Flow<Int>
    
    /**
     * Gets the count of completed tasks.
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1")
    fun getCompletedTasksCount(): Flow<Int>
    
    /**
     * Gets tasks with upcoming due dates (within next 7 days).
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE isCompleted = 0 
        AND dueDate IS NOT NULL 
        AND dueDate BETWEEN :startTime AND :endTime 
        ORDER BY dueDate ASC
    """)
    fun getTasksDueSoon(startTime: Long, endTime: Long): Flow<List<TaskEntity>>
    
    /**
     * Gets overdue tasks (past due date and not completed).
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE isCompleted = 0 
        AND dueDate IS NOT NULL 
        AND dueDate < :currentTime 
        ORDER BY dueDate ASC
    """)
    fun getOverdueTasks(currentTime: Long): Flow<List<TaskEntity>>
    
    /**
     * Marks a task as completed with timestamp.
     */
    @Query("UPDATE tasks SET isCompleted = 1, completedAt = :completedAt WHERE id = :taskId")
    suspend fun markTaskCompleted(taskId: String, completedAt: Long)
    
    /**
     * Marks a task as incomplete, removing completion timestamp.
     */
    @Query("UPDATE tasks SET isCompleted = 0, completedAt = NULL WHERE id = :taskId")
    suspend fun markTaskIncomplete(taskId: String)
    
    /**
     * Gets tasks by priority level.
     */
    @Query("SELECT * FROM tasks WHERE priority = :priority ORDER BY createdAt DESC")
    fun getTasksByPriority(priority: String): Flow<List<TaskEntity>>
    
    /**
     * Searches tasks by text content.
     */
    @Query("SELECT * FROM tasks WHERE text LIKE '%' || :searchQuery || '%' ORDER BY createdAt DESC")
    fun searchTasks(searchQuery: String): Flow<List<TaskEntity>>
    
    /**
     * Gets task statistics for analytics.
     */
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END) as completed,
            SUM(CASE WHEN isCompleted = 0 THEN 1 ELSE 0 END) as pending
        FROM tasks
    """)
    suspend fun getTaskStatistics(): TaskStatistics
    
    /**
     * Deletes all completed tasks older than the specified timestamp.
     */
    @Query("DELETE FROM tasks WHERE isCompleted = 1 AND completedAt < :olderThan")
    suspend fun deleteOldCompletedTasks(olderThan: Long)
}

/**
 * Data class for task statistics.
 */
data class TaskStatistics(
    val total: Int,
    val completed: Int,
    val pending: Int
)