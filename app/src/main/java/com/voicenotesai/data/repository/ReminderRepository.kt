package com.voicenotesai.data.repository

import com.voicenotesai.domain.model.Reminder
import com.voicenotesai.domain.model.ReminderWithContext
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for reminder data operations.
 */
interface ReminderRepository {
    
    /**
     * Get all reminders ordered by trigger time.
     */
    fun getAllReminders(): Flow<List<Reminder>>
    
    /**
     * Get pending reminders (not completed) ordered by trigger time.
     */
    fun getPendingReminders(): Flow<List<Reminder>>
    
    /**
     * Get completed reminders ordered by completion time.
     */
    fun getCompletedReminders(): Flow<List<Reminder>>
    
    /**
     * Get reminders for a specific note.
     */
    fun getRemindersForNote(noteId: String): Flow<List<Reminder>>
    
    /**
     * Get reminders for a specific task.
     */
    fun getRemindersForTask(taskId: String): Flow<List<Reminder>>
    
    /**
     * Get reminder by ID.
     */
    suspend fun getReminderById(id: String): Reminder?
    
    /**
     * Get reminders with context (note/task information).
     */
    fun getRemindersWithContext(): Flow<List<ReminderWithContext>>
    
    /**
     * Get count of pending reminders.
     */
    fun getPendingRemindersCount(): Flow<Int>
    
    /**
     * Get reminders that should be triggered.
     */
    suspend fun getRemindersToTrigger(currentTime: Long): List<Reminder>
    
    /**
     * Get reminders within a time range.
     */
    suspend fun getRemindersInRange(startTime: Long, endTime: Long): List<Reminder>
    
    /**
     * Insert a new reminder.
     */
    suspend fun insertReminder(reminder: Reminder): Result<Unit>
    
    /**
     * Insert multiple reminders.
     */
    suspend fun insertReminders(reminders: List<Reminder>): Result<Unit>
    
    /**
     * Update an existing reminder.
     */
    suspend fun updateReminder(reminder: Reminder): Result<Unit>
    
    /**
     * Mark reminder as completed.
     */
    suspend fun markReminderCompleted(id: String): Result<Unit>
    
    /**
     * Mark reminder as incomplete.
     */
    suspend fun markReminderIncomplete(id: String): Result<Unit>
    
    /**
     * Update reminder trigger time (for snoozing).
     */
    suspend fun updateReminderTriggerTime(id: String, newTriggerTime: Long): Result<Unit>
    
    /**
     * Delete a reminder.
     */
    suspend fun deleteReminder(id: String): Result<Unit>
    
    /**
     * Delete all completed reminders older than specified time.
     */
    suspend fun deleteOldCompletedReminders(cutoffTime: Long): Result<Unit>
    
    /**
     * Delete all reminders for a specific note.
     */
    suspend fun deleteRemindersForNote(noteId: String): Result<Unit>
    
    /**
     * Delete all reminders for a specific task.
     */
    suspend fun deleteRemindersForTask(taskId: String): Result<Unit>
}