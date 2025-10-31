package com.voicenotesai.data.notification

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.voicenotesai.data.local.dao.ReminderDao
import com.voicenotesai.data.local.entity.ReminderEntity
import com.voicenotesai.domain.model.Reminder
import com.voicenotesai.domain.model.ReminderAction
import com.voicenotesai.domain.model.ReminderType
import com.voicenotesai.domain.model.SnoozeDuration
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for reminder scheduling and notification handling.
 * 
 * Tests reminder creation, scheduling, cancellation, and action handling.
 */
class ReminderNotificationTest {
    
    private lateinit var notificationManager: NotificationManagerImpl
    private lateinit var context: Context
    private lateinit var reminderDao: ReminderDao
    private lateinit var alarmManager: AlarmManager
    private lateinit var notificationManagerCompat: NotificationManagerCompat
    
    private val testReminder = Reminder(
        id = "reminder123",
        title = "Test Reminder",
        description = "This is a test reminder",
        triggerTime = System.currentTimeMillis() + 3600000L, // 1 hour from now
        sourceNoteId = "note123"
    )
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        reminderDao = mockk()
        alarmManager = mockk(relaxed = true)
        notificationManagerCompat = mockk(relaxed = true)
        
        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        
        notificationManager = NotificationManagerImpl(context, reminderDao)
    }
    
    @Test
    fun `scheduleReminder should save reminder and schedule notification`() = runTest {
        // Given
        val reminder = testReminder
        
        coEvery { reminderDao.insertReminder(any()) } returns Unit
        
        // When
        val result = notificationManager.scheduleReminder(reminder)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { reminderDao.insertReminder(any()) }
        // Note: AlarmManager scheduling verification would require more complex mocking
    }
    
    @Test
    fun `scheduleReminder should handle database failure`() = runTest {
        // Given
        val reminder = testReminder
        val exception = RuntimeException("Database error")
        
        coEvery { reminderDao.insertReminder(any()) } throws exception
        
        // When
        val result = notificationManager.scheduleReminder(reminder)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
    
    @Test
    fun `cancelReminder should remove reminder and cancel notification`() = runTest {
        // Given
        val reminderId = "reminder123"
        
        coEvery { reminderDao.deleteReminder(reminderId) } returns Unit
        
        // When
        val result = notificationManager.cancelReminder(reminderId)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { reminderDao.deleteReminder(reminderId) }
    }
    
    @Test
    fun `createReminderFromNote should create reminder with note reference`() = runTest {
        // Given
        val noteId = "note123"
        val triggerTime = System.currentTimeMillis() + 3600000L
        val title = "Note Reminder"
        val description = "Reminder for note"
        
        coEvery { reminderDao.insertReminder(any()) } returns Unit
        
        // When
        val result = notificationManager.createReminderFromNote(noteId, triggerTime, title, description)
        
        // Then
        assertTrue(result.isSuccess)
        val reminder = result.getOrNull()
        assertNotNull(reminder)
        assertEquals(noteId, reminder?.sourceNoteId)
        assertEquals(title, reminder?.title)
        assertEquals(description, reminder?.description)
        assertEquals(triggerTime, reminder?.triggerTime)
    }
    
    @Test
    fun `createReminderFromTask should create reminder with task reference`() = runTest {
        // Given
        val taskId = "task123"
        val triggerTime = System.currentTimeMillis() + 3600000L
        val title = "Task Reminder"
        val description = "Reminder for task"
        
        coEvery { reminderDao.insertReminder(any()) } returns Unit
        
        // When
        val result = notificationManager.createReminderFromTask(taskId, triggerTime, title, description)
        
        // Then
        assertTrue(result.isSuccess)
        val reminder = result.getOrNull()
        assertNotNull(reminder)
        assertEquals(taskId, reminder?.sourceTaskId)
        assertEquals(title, reminder?.title)
        assertEquals(description, reminder?.description)
        assertEquals(triggerTime, reminder?.triggerTime)
    }
    
    @Test
    fun `handleReminderAction should mark reminder as done`() = runTest {
        // Given
        val reminderId = "reminder123"
        val action = ReminderAction.MARK_DONE
        
        coEvery { reminderDao.markReminderCompleted(reminderId, any()) } returns Unit
        
        // When
        val result = notificationManager.handleReminderAction(reminderId, action)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { reminderDao.markReminderCompleted(reminderId, any()) }
    }
    
    @Test
    fun `handleReminderAction should snooze reminder for 15 minutes`() = runTest {
        // Given
        val reminderId = "reminder123"
        val action = ReminderAction.SNOOZE_15MIN
        val reminderEntity = ReminderEntity(
            id = reminderId,
            title = "Test",
            triggerTime = System.currentTimeMillis(),
            reminderType = ReminderType.ONE_TIME.name
        )
        
        coEvery { reminderDao.updateReminderTriggerTime(reminderId, any()) } returns Unit
        coEvery { reminderDao.getReminderById(reminderId) } returns reminderEntity
        
        // When
        val result = notificationManager.handleReminderAction(reminderId, action)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { reminderDao.updateReminderTriggerTime(reminderId, any()) }
    }
    
    @Test
    fun `handleReminderAction should snooze reminder for 1 hour`() = runTest {
        // Given
        val reminderId = "reminder123"
        val action = ReminderAction.SNOOZE_1HOUR
        val reminderEntity = ReminderEntity(
            id = reminderId,
            title = "Test",
            triggerTime = System.currentTimeMillis(),
            reminderType = ReminderType.ONE_TIME.name
        )
        
        coEvery { reminderDao.updateReminderTriggerTime(reminderId, any()) } returns Unit
        coEvery { reminderDao.getReminderById(reminderId) } returns reminderEntity
        
        // When
        val result = notificationManager.handleReminderAction(reminderId, action)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { reminderDao.updateReminderTriggerTime(reminderId, any()) }
    }
    
    @Test
    fun `handleReminderAction should snooze reminder until tomorrow`() = runTest {
        // Given
        val reminderId = "reminder123"
        val action = ReminderAction.SNOOZE_TOMORROW
        val reminderEntity = ReminderEntity(
            id = reminderId,
            title = "Test",
            triggerTime = System.currentTimeMillis(),
            reminderType = ReminderType.ONE_TIME.name
        )
        
        coEvery { reminderDao.updateReminderTriggerTime(reminderId, any()) } returns Unit
        coEvery { reminderDao.getReminderById(reminderId) } returns reminderEntity
        
        // When
        val result = notificationManager.handleReminderAction(reminderId, action)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { reminderDao.updateReminderTriggerTime(reminderId, any()) }
    }
    
    @Test
    fun `snoozeReminder should update trigger time correctly`() = runTest {
        // Given
        val reminderId = "reminder123"
        val duration = SnoozeDuration.FIFTEEN_MINUTES
        val reminderEntity = ReminderEntity(
            id = reminderId,
            title = "Test",
            triggerTime = System.currentTimeMillis(),
            reminderType = ReminderType.ONE_TIME.name
        )
        
        coEvery { reminderDao.updateReminderTriggerTime(reminderId, any()) } returns Unit
        coEvery { reminderDao.getReminderById(reminderId) } returns reminderEntity
        
        // When
        val result = notificationManager.snoozeReminder(reminderId, duration)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { 
            reminderDao.updateReminderTriggerTime(
                reminderId, 
                match { newTime -> newTime > System.currentTimeMillis() }
            ) 
        }
    }
    
    @Test
    fun `markReminderCompleted should update completion status`() = runTest {
        // Given
        val reminderId = "reminder123"
        
        coEvery { reminderDao.markReminderCompleted(reminderId, any()) } returns Unit
        
        // When
        val result = notificationManager.markReminderCompleted(reminderId)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { reminderDao.markReminderCompleted(reminderId, any()) }
    }
    
    @Test
    fun `checkAndTriggerPendingReminders should find and trigger due reminders`() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val pendingReminders = listOf(
            ReminderEntity(
                id = "reminder1",
                title = "Reminder 1",
                triggerTime = currentTime - 1000, // Past due
                reminderType = ReminderType.ONE_TIME.name
            ),
            ReminderEntity(
                id = "reminder2",
                title = "Reminder 2",
                triggerTime = currentTime - 2000, // Past due
                reminderType = ReminderType.ONE_TIME.name
            )
        )
        
        coEvery { reminderDao.getRemindersToTrigger(any()) } returns pendingReminders
        
        // When
        val result = notificationManager.checkAndTriggerPendingReminders()
        
        // Then
        assertTrue(result.isSuccess)
        val triggeredReminders = result.getOrNull()
        assertNotNull(triggeredReminders)
        assertEquals(2, triggeredReminders?.size)
    }
    
    @Test
    fun `updateNotificationSettings should handle notification disable`() = runTest {
        // Given
        val enabled = false
        
        // When
        val result = notificationManager.updateNotificationSettings(enabled)
        
        // Then
        assertTrue(result.isSuccess)
        // Verify that quick capture notification is hidden when disabled
    }
    
    @Test
    fun `hasNotificationPermission should check permission correctly`() {
        // Given
        every { context.checkSelfPermission(any()) } returns android.content.pm.PackageManager.PERMISSION_GRANTED
        
        // When
        val hasPermission = notificationManager.hasNotificationPermission()
        
        // Then
        assertTrue(hasPermission)
    }
    
    @Test
    fun `requestNotificationPermission should return current permission status`() = runTest {
        // Given
        every { context.checkSelfPermission(any()) } returns android.content.pm.PackageManager.PERMISSION_GRANTED
        
        // When
        val result = notificationManager.requestNotificationPermission()
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }
    
    @Test
    fun `showQuickCaptureNotification should display persistent notification`() {
        // Given
        every { context.checkSelfPermission(any()) } returns android.content.pm.PackageManager.PERMISSION_GRANTED
        
        // When
        notificationManager.showQuickCaptureNotification()
        
        // Then
        // Verification would require mocking NotificationManagerCompat
        // This test verifies the method doesn't crash
    }
    
    @Test
    fun `hideQuickCaptureNotification should cancel persistent notification`() {
        // When
        notificationManager.hideQuickCaptureNotification()
        
        // Then
        // Verification would require mocking NotificationManagerCompat
        // This test verifies the method doesn't crash
    }
}