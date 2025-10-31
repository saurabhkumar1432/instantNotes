package com.voicenotesai.domain.notification

import com.voicenotesai.domain.model.Reminder
import com.voicenotesai.domain.model.ReminderAction
import com.voicenotesai.domain.model.SnoozeDuration

/**
 * Interface for managing notifications and reminders.
 * Handles scheduling, canceling, and managing reminder notifications.
 */
interface NotificationManager {
    
    /**
     * Schedule a reminder notification.
     */
    suspend fun scheduleReminder(reminder: Reminder): Result<Unit>
    
    /**
     * Cancel a scheduled reminder notification.
     */
    suspend fun cancelReminder(reminderId: String): Result<Unit>
    
    /**
     * Create and schedule a reminder from a note.
     */
    suspend fun createReminderFromNote(noteId: String, triggerTime: Long, title: String, description: String? = null): Result<Reminder>
    
    /**
     * Create and schedule a reminder from a task.
     */
    suspend fun createReminderFromTask(taskId: String, triggerTime: Long, title: String, description: String? = null): Result<Reminder>
    
    /**
     * Show persistent notification for quick capture.
     */
    fun showQuickCaptureNotification()
    
    /**
     * Hide persistent notification for quick capture.
     */
    fun hideQuickCaptureNotification()
    
    /**
     * Handle reminder action from notification (Mark Done, Snooze, View Note).
     */
    suspend fun handleReminderAction(reminderId: String, action: ReminderAction): Result<Unit>
    
    /**
     * Snooze a reminder for the specified duration.
     */
    suspend fun snoozeReminder(reminderId: String, duration: SnoozeDuration): Result<Unit>
    
    /**
     * Mark a reminder as completed.
     */
    suspend fun markReminderCompleted(reminderId: String): Result<Unit>
    
    /**
     * Check and trigger any pending reminders.
     */
    suspend fun checkAndTriggerPendingReminders(): Result<List<Reminder>>
    
    /**
     * Update notification settings and permissions.
     */
    suspend fun updateNotificationSettings(enabled: Boolean): Result<Unit>
    
    /**
     * Check if notification permissions are granted.
     */
    fun hasNotificationPermission(): Boolean
    
    /**
     * Request notification permissions from the user.
     */
    suspend fun requestNotificationPermission(): Result<Boolean>
}