package com.voicenotesai.data.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.voicenotesai.R
import com.voicenotesai.data.local.dao.ReminderDao
import com.voicenotesai.data.local.entity.ReminderEntity
import com.voicenotesai.domain.model.Reminder
import com.voicenotesai.domain.model.ReminderAction
import com.voicenotesai.domain.model.ReminderType
import com.voicenotesai.domain.model.SnoozeDuration
import com.voicenotesai.domain.notification.NotificationManager
import com.voicenotesai.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of NotificationManager for handling reminder notifications.
 * Manages scheduling, canceling, and handling notification actions.
 */
@Singleton
class NotificationManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderDao: ReminderDao
) : NotificationManager {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager = NotificationManagerCompat.from(context)
    
    companion object {
        private const val REMINDER_CHANNEL_ID = "reminder_notifications"
        private const val QUICK_CAPTURE_CHANNEL_ID = "quick_capture"
        private const val QUICK_CAPTURE_NOTIFICATION_ID = 1001
        private const val REMINDER_REQUEST_CODE_BASE = 2000
        
        // Notification actions
        const val ACTION_MARK_DONE = "com.voicenotesai.ACTION_MARK_DONE"
        const val ACTION_SNOOZE_15MIN = "com.voicenotesai.ACTION_SNOOZE_15MIN"
        const val ACTION_SNOOZE_1HOUR = "com.voicenotesai.ACTION_SNOOZE_1HOUR"
        const val ACTION_SNOOZE_TOMORROW = "com.voicenotesai.ACTION_SNOOZE_TOMORROW"
        const val ACTION_VIEW_NOTE = "com.voicenotesai.ACTION_VIEW_NOTE"
        const val ACTION_VIEW_TASK = "com.voicenotesai.ACTION_VIEW_TASK"
        const val ACTION_QUICK_RECORD = "com.voicenotesai.ACTION_QUICK_RECORD"
        
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_NOTE_ID = "note_id"
        const val EXTRA_TASK_ID = "task_id"
    }
    
    init {
        createNotificationChannels()
    }
    
    override suspend fun scheduleReminder(reminder: Reminder): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Save reminder to database
            val reminderEntity = reminder.toEntity()
            reminderDao.insertReminder(reminderEntity)
            
            // Schedule notification
            scheduleNotification(reminder)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun cancelReminder(reminderId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Cancel scheduled notification
            cancelScheduledNotification(reminderId)
            
            // Remove from database
            reminderDao.deleteReminder(reminderId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun createReminderFromNote(
        noteId: String, 
        triggerTime: Long, 
        title: String, 
        description: String?
    ): Result<Reminder> = withContext(Dispatchers.IO) {
        try {
            val reminder = Reminder(
                title = title,
                description = description,
                triggerTime = triggerTime,
                sourceNoteId = noteId
            )
            
            scheduleReminder(reminder)
            Result.success(reminder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun createReminderFromTask(
        taskId: String, 
        triggerTime: Long, 
        title: String, 
        description: String?
    ): Result<Reminder> = withContext(Dispatchers.IO) {
        try {
            val reminder = Reminder(
                title = title,
                description = description,
                triggerTime = triggerTime,
                sourceTaskId = taskId
            )
            
            scheduleReminder(reminder)
            Result.success(reminder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun showQuickCaptureNotification() {
        if (!hasNotificationPermission()) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_QUICK_RECORD
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, QUICK_CAPTURE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentTitle("Voice Notes")
            .setContentText("Tap to start recording")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        
        notificationManager.notify(QUICK_CAPTURE_NOTIFICATION_ID, notification)
    }
    
    override fun hideQuickCaptureNotification() {
        notificationManager.cancel(QUICK_CAPTURE_NOTIFICATION_ID)
    }
    
    override suspend fun handleReminderAction(reminderId: String, action: ReminderAction): Result<Unit> {
        return when (action) {
            ReminderAction.MARK_DONE -> markReminderCompleted(reminderId)
            ReminderAction.SNOOZE_15MIN -> snoozeReminder(reminderId, SnoozeDuration.FIFTEEN_MINUTES)
            ReminderAction.SNOOZE_1HOUR -> snoozeReminder(reminderId, SnoozeDuration.ONE_HOUR)
            ReminderAction.SNOOZE_TOMORROW -> snoozeReminder(reminderId, SnoozeDuration.TOMORROW)
            ReminderAction.VIEW_NOTE -> {
                // This would typically launch the app to view the note
                // Implementation depends on navigation structure
                Result.success(Unit)
            }
            ReminderAction.VIEW_TASK -> {
                // This would typically launch the app to view the task
                // Implementation depends on navigation structure
                Result.success(Unit)
            }
        }
    }
    
    override suspend fun snoozeReminder(reminderId: String, duration: SnoozeDuration): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val newTriggerTime = System.currentTimeMillis() + duration.milliseconds
            reminderDao.updateReminderTriggerTime(reminderId, newTriggerTime)
            
            // Cancel current notification
            cancelScheduledNotification(reminderId)
            
            // Reschedule with new time
            val reminder = reminderDao.getReminderById(reminderId)?.toDomain()
            reminder?.let {
                scheduleNotification(it.copy(triggerTime = newTriggerTime))
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun markReminderCompleted(reminderId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            reminderDao.markReminderCompleted(reminderId, System.currentTimeMillis())
            cancelScheduledNotification(reminderId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun checkAndTriggerPendingReminders(): Result<List<Reminder>> = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            val pendingReminders = reminderDao.getRemindersToTrigger(currentTime)
            
            pendingReminders.forEach { reminderEntity ->
                showReminderNotification(reminderEntity.toDomain())
            }
            
            Result.success(pendingReminders.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateNotificationSettings(enabled: Boolean): Result<Unit> {
        return try {
            if (!enabled) {
                hideQuickCaptureNotification()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            notificationManager.areNotificationsEnabled()
        }
    }
    
    override suspend fun requestNotificationPermission(): Result<Boolean> {
        // This would typically be handled by the UI layer
        // Return current permission status
        return Result.success(hasNotificationPermission())
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Reminders",
                AndroidNotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for scheduled reminders"
                enableVibration(true)
                enableLights(true)
            }
            
            val quickCaptureChannel = NotificationChannel(
                QUICK_CAPTURE_CHANNEL_ID,
                "Quick Capture",
                AndroidNotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent notification for quick voice recording"
                setShowBadge(false)
            }
            
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager
            manager.createNotificationChannel(reminderChannel)
            manager.createNotificationChannel(quickCaptureChannel)
        }
    }
    
    private fun scheduleNotification(reminder: Reminder) {
        val intent = Intent(context, Context::class.java).apply { // ReminderBroadcastReceiver disabled
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                reminder.triggerTime,
                pendingIntent
            )
        }
    }
    
    private fun cancelScheduledNotification(reminderId: String) {
        val intent = Intent(context, Context::class.java) // ReminderBroadcastReceiver disabled
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
    }
    
    private fun showReminderNotification(reminder: Reminder) {
        if (!hasNotificationPermission()) return
        
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (reminder.sourceNoteId != null) {
                putExtra(EXTRA_NOTE_ID, reminder.sourceNoteId)
            }
            if (reminder.sourceTaskId != null) {
                putExtra(EXTRA_TASK_ID, reminder.sourceTaskId)
            }
        }
        
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create action buttons
        val markDoneIntent = createActionIntent(reminder.id, ACTION_MARK_DONE)
        val snooze15Intent = createActionIntent(reminder.id, ACTION_SNOOZE_15MIN)
        val snooze1HourIntent = createActionIntent(reminder.id, ACTION_SNOOZE_1HOUR)
        
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.title)
            .setContentText(reminder.description ?: "Reminder")
            .setContentIntent(mainPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_check, "Done", markDoneIntent)
            .addAction(R.drawable.ic_snooze, "15min", snooze15Intent)
            .addAction(R.drawable.ic_snooze, "1hr", snooze1HourIntent)
            .build()
        
        notificationManager.notify(reminder.id.hashCode(), notification)
    }
    
    private fun createActionIntent(reminderId: String, action: String): PendingIntent {
        val intent = Intent(context, Context::class.java).apply { // ReminderActionReceiver disabled
            this.action = action
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        
        return PendingIntent.getBroadcast(
            context,
            "$reminderId$action".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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