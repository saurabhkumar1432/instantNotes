package com.voicenotesai.data.repository

import com.voicenotesai.data.local.dao.TaskDao
import com.voicenotesai.data.local.entity.TaskEntity
import com.voicenotesai.data.repository.NotesRepository
import com.voicenotesai.domain.model.Task
import com.voicenotesai.domain.model.TaskPriority
import com.voicenotesai.domain.model.TaskWithNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TaskRepository that handles task data operations.
 * 
 * Provides data mapping between domain models and database entities,
 * and combines task data with associated note information.
 */
@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val notesRepository: NotesRepository
) : TaskRepository {
    
    override suspend fun insertTask(task: Task): Result<Task> {
        return try {
            val entity = task.toEntity()
            taskDao.insertTask(entity)
            Result.success(task)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun insertTasks(tasks: List<Task>): Result<List<Task>> {
        return try {
            val entities = tasks.map { it.toEntity() }
            taskDao.insertTasks(entities)
            Result.success(tasks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateTask(task: Task): Result<Unit> {
        return try {
            val entity = task.toEntity()
            taskDao.updateTask(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            taskDao.deleteTask(taskId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getTaskById(taskId: String): Task? {
        return try {
            taskDao.getTaskById(taskId)?.toDomainModel()
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getAllTasksWithNotes(): Flow<List<TaskWithNote>> {
        return taskDao.getAllTasks().map { taskEntities ->
            taskEntities.map { taskEntity ->
                val task = taskEntity.toDomainModel()
                // Note: We'll need to handle note fetching asynchronously in the UI layer
                // For now, we'll return TaskWithNote with null note
                TaskWithNote(task, null)
            }
        }
    }
    
    override fun getTasksByStatusWithNotes(completed: Boolean): Flow<List<TaskWithNote>> {
        return taskDao.getTasksByStatus(completed).map { taskEntities ->
            taskEntities.map { taskEntity ->
                val task = taskEntity.toDomainModel()
                // Note: We'll need to handle note fetching asynchronously in the UI layer
                // For now, we'll return TaskWithNote with null note
                TaskWithNote(task, null)
            }
        }
    }
    
    override fun getTasksForNote(noteId: String): Flow<List<Task>> {
        return try {
            val longNoteId = noteId.toLongOrNull() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
            taskDao.getTasksForNote(longNoteId).map { taskEntities ->
                taskEntities.map { it.toDomainModel() }
            }
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }
    
    override fun getPendingTasksCount(): Flow<Int> {
        return taskDao.getPendingTasksCount()
    }
    
    override fun getCompletedTasksCount(): Flow<Int> {
        return taskDao.getCompletedTasksCount()
    }
    
    override suspend fun markTaskCompleted(taskId: String): Result<Unit> {
        return try {
            val completedAt = System.currentTimeMillis()
            taskDao.markTaskCompleted(taskId, completedAt)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun markTaskIncomplete(taskId: String): Result<Unit> {
        return try {
            taskDao.markTaskIncomplete(taskId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getTasksDueSoon(daysAhead: Int): Flow<List<TaskWithNote>> {
        val currentTime = System.currentTimeMillis()
        val endTime = currentTime + (daysAhead * 24 * 60 * 60 * 1000L) // Convert days to milliseconds
        
        return taskDao.getTasksDueSoon(currentTime, endTime).map { taskEntities ->
            taskEntities.map { taskEntity ->
                val task = taskEntity.toDomainModel()
                // Note: We'll need to handle note fetching asynchronously in the UI layer
                // For now, we'll return TaskWithNote with null note
                TaskWithNote(task, null)
            }
        }
    }
    
    override fun getOverdueTasks(): Flow<List<TaskWithNote>> {
        val currentTime = System.currentTimeMillis()
        
        return taskDao.getOverdueTasks(currentTime).map { taskEntities ->
            taskEntities.map { taskEntity ->
                val task = taskEntity.toDomainModel()
                // Note: We'll need to handle note fetching asynchronously in the UI layer
                // For now, we'll return TaskWithNote with null note
                TaskWithNote(task, null)
            }
        }
    }
    
    override fun searchTasks(query: String): Flow<List<TaskWithNote>> {
        return taskDao.searchTasks(query).map { taskEntities ->
            taskEntities.map { taskEntity ->
                val task = taskEntity.toDomainModel()
                // Note: We'll need to handle note fetching asynchronously in the UI layer
                // For now, we'll return TaskWithNote with null note
                TaskWithNote(task, null)
            }
        }
    }
    
    override fun getTasksByPriority(priority: TaskPriority): Flow<List<TaskWithNote>> {
        return taskDao.getTasksByPriority(priority.name).map { taskEntities ->
            taskEntities.map { taskEntity ->
                val task = taskEntity.toDomainModel()
                // Note: We'll need to handle note fetching asynchronously in the UI layer
                // For now, we'll return TaskWithNote with null note
                TaskWithNote(task, null)
            }
        }
    }
    

    
    /**
     * Extension function to convert Task domain model to TaskEntity.
     */
    private fun Task.toEntity(): TaskEntity {
        return TaskEntity(
            id = id,
            text = text,
            isCompleted = isCompleted,
            sourceNoteId = sourceNoteId?.toLongOrNull(),
            createdAt = createdAt,
            completedAt = completedAt,
            dueDate = dueDate,
            priority = priority.name
        )
    }
    
    /**
     * Extension function to convert TaskEntity to Task domain model.
     */
    private fun TaskEntity.toDomainModel(): Task {
        return Task(
            id = id,
            text = text,
            isCompleted = isCompleted,
            sourceNoteId = sourceNoteId?.toString(),
            createdAt = createdAt,
            completedAt = completedAt,
            dueDate = dueDate,
            priority = try {
                TaskPriority.valueOf(priority)
            } catch (e: IllegalArgumentException) {
                TaskPriority.NORMAL
            }
        )
    }
}