package com.voicenotesai.domain.usecase

import com.voicenotesai.data.ai.DateTimeDetectionService
import com.voicenotesai.data.repository.ReminderRepository
import com.voicenotesai.domain.model.DateTimeDetectionResult
import com.voicenotesai.domain.model.Reminder
import com.voicenotesai.domain.model.ReminderType
import com.voicenotesai.domain.model.ReminderWithContext
import com.voicenotesai.domain.notification.NotificationManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for managing reminders and smart date/time detection.
 * Handles reminder creation, scheduling, and AI-powered date detection.
 */
@Singleton
class ReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val notificationManager: NotificationManager,
    private val dateTimeDetectionService: DateTimeDetectionService
) {
    
    /**
     * Detect dates and times in note content and suggest reminders.
     */
    suspend fun detectAndSuggestReminders(noteId: String, content: String): DateTimeDetectionResult {
        return dateTimeDetectionService.detectDateTimes(content)
    }
    
    /**
     * Create a reminder from detected date/time in note content.
     */
    suspend fun createReminderFromDetection(
        noteId: String,
        title: String,
        description: String?,
        triggerTime: Long,
        reminderType: ReminderType = ReminderType.ONE_TIME
    ): Result<Reminder> {
        val reminder = Reminder(
            title = title,
            description = description,
            triggerTime = triggerTime,
            sourceNoteId = noteId,
            reminderType = reminderType
        )
        
        return reminderRepository.insertReminder(reminder).fold(
            onSuccess = {
                notificationManager.scheduleReminder(reminder).fold(
                    onSuccess = { Result.success(reminder) },
                    onFailure = { error ->
                        // Rollback database insertion if notification scheduling fails
                        reminderRepository.deleteReminder(reminder.id)
                        Result.failure(error)
                    }
                )
            },
            onFailure = { Result.failure(it) }
        )
    }
    
    /**
     * Create a manual reminder for a note.
     */
    suspend fun createReminderForNote(
        noteId: String,
        title: String,
        description: String?,
        triggerTime: Long,
        reminderType: ReminderType = ReminderType.ONE_TIME
    ): Result<Reminder> {
        return notificationManager.createReminderFromNote(noteId, triggerTime, title, description)
    }
    
    /**
     * Create a manual reminder for a task.
     */
    suspend fun createReminderForTask(
        taskId: String,
        title: String,
        description: String?,
        triggerTime: Long,
        reminderType: ReminderType = ReminderType.ONE_TIME
    ): Result<Reminder> {
        return notificationManager.createReminderFromTask(taskId, triggerTime, title, description)
    }
    
    /**
     * Get all reminders with context information.
     */
    fun getRemindersWithContext(): Flow<List<ReminderWithContext>> {
        return reminderRepository.getRemindersWithContext()
    }
    
    /**
     * Get pending reminders.
     */
    fun getPendingReminders(): Flow<List<Reminder>> {
        return reminderRepository.getPendingReminders()
    }
    
    /**
     * Get completed reminders.
     */
    fun getCompletedReminders(): Flow<List<Reminder>> {
        return reminderRepository.getCompletedReminders()
    }
    
    /**
     * Get reminders for a specific note.
     */
    fun getRemindersForNote(noteId: String): Flow<List<Reminder>> {
        return reminderRepository.getRemindersForNote(noteId)
    }
    
    /**
     * Get reminders for a specific task.
     */
    fun getRemindersForTask(taskId: String): Flow<List<Reminder>> {
        return reminderRepository.getRemindersForTask(taskId)
    }
    
    /**
     * Get count of pending reminders.
     */
    fun getPendingRemindersCount(): Flow<Int> {
        return reminderRepository.getPendingRemindersCount()
    }
    
    /**
     * Mark a reminder as completed.
     */
    suspend fun markReminderCompleted(reminderId: String): Result<Unit> {
        return notificationManager.markReminderCompleted(reminderId)
    }
    
    /**
     * Snooze a reminder.
     */
    suspend fun snoozeReminder(reminderId: String, snoozeMinutes: Int): Result<Unit> {
        val newTriggerTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        return reminderRepository.updateReminderTriggerTime(reminderId, newTriggerTime).fold(
            onSuccess = {
                // Reschedule notification
                val reminder = reminderRepository.getReminderById(reminderId)
                reminder?.let {
                    notificationManager.scheduleReminder(it.copy(triggerTime = newTriggerTime))
                } ?: Result.success(Unit)
            },
            onFailure = { Result.failure(it) }
        )
    }
    
    /**
     * Update a reminder.
     */
    suspend fun updateReminder(reminder: Reminder): Result<Unit> {
        return reminderRepository.updateReminder(reminder).fold(
            onSuccess = {
                // Reschedule notification with updated details
                notificationManager.scheduleReminder(reminder)
            },
            onFailure = { Result.failure(it) }
        )
    }
    
    /**
     * Delete a reminder.
     */
    suspend fun deleteReminder(reminderId: String): Result<Unit> {
        return notificationManager.cancelReminder(reminderId)
    }
    
    /**
     * Check and trigger pending reminders.
     */
    suspend fun checkAndTriggerPendingReminders(): Result<List<Reminder>> {
        return notificationManager.checkAndTriggerPendingReminders()
    }
    
    /**
     * Clean up old completed reminders.
     */
    suspend fun cleanupOldReminders(daysOld: Int = 30): Result<Unit> {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        return reminderRepository.deleteOldCompletedReminders(cutoffTime)
    }
    
    /**
     * Enable or disable quick capture notification.
     */
    fun setQuickCaptureEnabled(enabled: Boolean) {
        if (enabled) {
            notificationManager.showQuickCaptureNotification()
        } else {
            notificationManager.hideQuickCaptureNotification()
        }
    }
    
    /**
     * Check if notification permissions are granted.
     */
    fun hasNotificationPermission(): Boolean {
        return notificationManager.hasNotificationPermission()
    }
    
    /**
     * Request notification permissions.
     */
    suspend fun requestNotificationPermission(): Result<Boolean> {
        return notificationManager.requestNotificationPermission()
    }
}