package com.voicenotesai.data.local.dao

import androidx.room.*
import com.voicenotesai.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for reminder operations.
 * Provides efficient queries for reminder management and scheduling.
 */
@Dao
interface ReminderDao {
    
    /**
     * Get all reminders ordered by trigger time.
     */
    @Query("SELECT * FROM reminders ORDER BY triggerTime ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>
    
    /**
     * Get pending reminders (not completed) ordered by trigger time.
     */
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY triggerTime ASC")
    fun getPendingReminders(): Flow<List<ReminderEntity>>
    
    /**
     * Get completed reminders ordered by completion time.
     */
    @Query("SELECT * FROM reminders WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedReminders(): Flow<List<ReminderEntity>>
    
    /**
     * Get reminders that should be triggered (trigger time <= current time and not completed).
     */
    @Query("SELECT * FROM reminders WHERE triggerTime <= :currentTime AND isCompleted = 0 ORDER BY triggerTime ASC")
    suspend fun getRemindersToTrigger(currentTime: Long): List<ReminderEntity>
    
    /**
     * Get reminders for a specific note.
     */
    @Query("SELECT * FROM reminders WHERE sourceNoteId = :noteId ORDER BY triggerTime ASC")
    fun getRemindersForNote(noteId: Long): Flow<List<ReminderEntity>>
    
    /**
     * Get reminders for a specific task.
     */
    @Query("SELECT * FROM reminders WHERE sourceTaskId = :taskId ORDER BY triggerTime ASC")
    fun getRemindersForTask(taskId: String): Flow<List<ReminderEntity>>
    
    /**
     * Get reminder by ID.
     */
    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: String): ReminderEntity?
    
    /**
     * Get count of pending reminders.
     */
    @Query("SELECT COUNT(*) FROM reminders WHERE isCompleted = 0")
    fun getPendingRemindersCount(): Flow<Int>
    
    /**
     * Get reminders within a time range.
     */
    @Query("SELECT * FROM reminders WHERE triggerTime BETWEEN :startTime AND :endTime ORDER BY triggerTime ASC")
    suspend fun getRemindersInRange(startTime: Long, endTime: Long): List<ReminderEntity>
    
    /**
     * Insert a new reminder.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long
    
    /**
     * Insert multiple reminders.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderEntity>)
    
    /**
     * Update an existing reminder.
     */
    @Update
    suspend fun updateReminder(reminder: ReminderEntity)
    
    /**
     * Mark reminder as completed.
     */
    @Query("UPDATE reminders SET isCompleted = 1, completedAt = :completedAt WHERE id = :id")
    suspend fun markReminderCompleted(id: String, completedAt: Long)
    
    /**
     * Mark reminder as incomplete.
     */
    @Query("UPDATE reminders SET isCompleted = 0, completedAt = NULL WHERE id = :id")
    suspend fun markReminderIncomplete(id: String)
    
    /**
     * Update reminder trigger time (for snoozing).
     */
    @Query("UPDATE reminders SET triggerTime = :newTriggerTime WHERE id = :id")
    suspend fun updateReminderTriggerTime(id: String, newTriggerTime: Long)
    
    /**
     * Delete a reminder.
     */
    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: String)
    
    /**
     * Delete all completed reminders older than specified time.
     */
    @Query("DELETE FROM reminders WHERE isCompleted = 1 AND completedAt < :cutoffTime")
    suspend fun deleteOldCompletedReminders(cutoffTime: Long)
    
    /**
     * Delete all reminders for a specific note.
     */
    @Query("DELETE FROM reminders WHERE sourceNoteId = :noteId")
    suspend fun deleteRemindersForNote(noteId: Long)
    
    /**
     * Delete all reminders for a specific task.
     */
    @Query("DELETE FROM reminders WHERE sourceTaskId = :taskId")
    suspend fun deleteRemindersForTask(taskId: String)
}