package com.voicenotesai.data.repository

import com.voicenotesai.data.local.dao.NotesDao
import com.voicenotesai.data.local.dao.ReminderDao
import com.voicenotesai.data.local.dao.TaskDao
import com.voicenotesai.data.local.entity.ReminderEntity
import com.voicenotesai.domain.model.EnhancedNote
import com.voicenotesai.domain.model.NoteCategory
import com.voicenotesai.domain.model.Reminder
import com.voicenotesai.domain.model.ReminderType
import com.voicenotesai.domain.model.ReminderWithContext
import com.voicenotesai.domain.model.Task
import com.voicenotesai.domain.model.TaskPriority
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ReminderRepository for managing reminder data operations.
 */
@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val reminderDao: ReminderDao,
    private val notesDao: NotesDao,
    private val taskDao: TaskDao
) : ReminderRepository {
    
    override fun getAllReminders(): Flow<List<Reminder>> {
        return reminderDao.getAllReminders().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getPendingReminders(): Flow<List<Reminder>> {
        return reminderDao.getPendingReminders().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getCompletedReminders(): Flow<List<Reminder>> {
        return reminderDao.getCompletedReminders().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getRemindersForNote(noteId: String): Flow<List<Reminder>> {
        val noteIdLong = noteId.toLongOrNull() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return reminderDao.getRemindersForNote(noteIdLong).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getRemindersForTask(taskId: String): Flow<List<Reminder>> {
        return reminderDao.getRemindersForTask(taskId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getReminderById(id: String): Reminder? {
        return reminderDao.getReminderById(id)?.toDomain()
    }
    
    override fun getRemindersWithContext(): Flow<List<ReminderWithContext>> {
        return combine(
            reminderDao.getAllReminders(),
            notesDao.getAllNotes(),
            taskDao.getAllTasks()
        ) { reminders, notes, tasks ->
            reminders.map { reminder ->
                val sourceNote = reminder.sourceNoteId?.let { noteId ->
                    notes.find { it.id == noteId }?.let { noteEntity ->
                        EnhancedNote(
                            id = noteEntity.id.toString(),
                            content = noteEntity.content,
                            transcribedText = noteEntity.transcribedText ?: "",
                            timestamp = noteEntity.timestamp,
                            lastModified = noteEntity.lastModified,
                            category = NoteCategory.General, // Default category
                            tags = noteEntity.tags.split(",").filter { it.isNotBlank() },
                            isArchived = noteEntity.isArchived,
                            audioFingerprint = noteEntity.audioFingerprint,
                            language = null, // Will be null for now
                            duration = noteEntity.duration
                        )
                    }
                }
                
                val sourceTask = reminder.sourceTaskId?.let { taskId ->
                    tasks.find { it.id == taskId }?.let { taskEntity ->
                        Task(
                            id = taskEntity.id,
                            text = taskEntity.text,
                            isCompleted = taskEntity.isCompleted,
                            sourceNoteId = taskEntity.sourceNoteId?.toString(),
                            createdAt = taskEntity.createdAt,
                            completedAt = taskEntity.completedAt,
                            dueDate = taskEntity.dueDate,
                            priority = TaskPriority.valueOf(taskEntity.priority)
                        )
                    }
                }
                
                ReminderWithContext(
                    reminder = reminder.toDomain(),
                    sourceNote = sourceNote,
                    sourceTask = sourceTask
                )
            }
        }
    }
    
    override fun getPendingRemindersCount(): Flow<Int> {
        return reminderDao.getPendingRemindersCount()
    }
    
    override suspend fun getRemindersToTrigger(currentTime: Long): List<Reminder> {
        return reminderDao.getRemindersToTrigger(currentTime).map { it.toDomain() }
    }
    
    override suspend fun getRemindersInRange(startTime: Long, endTime: Long): List<Reminder> {
        return reminderDao.getRemindersInRange(startTime, endTime).map { it.toDomain() }
    }
    
    override suspend fun insertReminder(reminder: Reminder): Result<Unit> {
        return try {
            reminderDao.insertReminder(reminder.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun insertReminders(reminders: List<Reminder>): Result<Unit> {
        return try {
            reminderDao.insertReminders(reminders.map { it.toEntity() })
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateReminder(reminder: Reminder): Result<Unit> {
        return try {
            reminderDao.updateReminder(reminder.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun markReminderCompleted(id: String): Result<Unit> {
        return try {
            reminderDao.markReminderCompleted(id, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun markReminderIncomplete(id: String): Result<Unit> {
        return try {
            reminderDao.markReminderIncomplete(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateReminderTriggerTime(id: String, newTriggerTime: Long): Result<Unit> {
        return try {
            reminderDao.updateReminderTriggerTime(id, newTriggerTime)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteReminder(id: String): Result<Unit> {
        return try {
            reminderDao.deleteReminder(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteOldCompletedReminders(cutoffTime: Long): Result<Unit> {
        return try {
            reminderDao.deleteOldCompletedReminders(cutoffTime)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteRemindersForNote(noteId: String): Result<Unit> {
        return try {
            val noteIdLong = noteId.toLongOrNull() ?: return Result.failure(IllegalArgumentException("Invalid note ID"))
            reminderDao.deleteRemindersForNote(noteIdLong)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteRemindersForTask(taskId: String): Result<Unit> {
        return try {
            reminderDao.deleteRemindersForTask(taskId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Extension functions for conversion
    private fun Reminder.toEntity(): ReminderEntity {
        return ReminderEntity(
            id = id,
            title = title,
            description = description,
            triggerTime = triggerTime,
            sourceNoteId = sourceNoteId?.toLongOrNull(),
            sourceTaskId = sourceTaskId,
            isCompleted = isCompleted,
            reminderType = reminderType.name,
            repeatInterval = repeatInterval,
            createdAt = createdAt,
            completedAt = completedAt
        )
    }
    
    private fun ReminderEntity.toDomain(): Reminder {
        return Reminder(
            id = id,
            title = title,
            description = description,
            triggerTime = triggerTime,
            sourceNoteId = sourceNoteId?.toString(),
            sourceTaskId = sourceTaskId,
            isCompleted = isCompleted,
            reminderType = ReminderType.valueOf(reminderType),
            repeatInterval = repeatInterval,
            createdAt = createdAt,
            completedAt = completedAt
        )
    }
}