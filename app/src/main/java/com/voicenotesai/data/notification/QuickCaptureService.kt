package com.voicenotesai.data.notification

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.voicenotesai.R
import com.voicenotesai.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service for persistent quick capture notification.
 */
@AndroidEntryPoint
class QuickCaptureService : Service() {
    
    @Inject
    lateinit var notificationManager: com.voicenotesai.domain.notification.NotificationManager
    
    companion object {
        const val ACTION_START_QUICK_CAPTURE = "com.voicenotesai.ACTION_START_QUICK_CAPTURE"
        const val ACTION_STOP_QUICK_CAPTURE = "com.voicenotesai.ACTION_STOP_QUICK_CAPTURE"
        const val ACTION_QUICK_RECORD = "com.voicenotesai.ACTION_QUICK_RECORD"
        const val ACTION_VIEW_RECENT_NOTES = "com.voicenotesai.ACTION_VIEW_RECENT_NOTES"
        const val ACTION_VIEW_TASKS = "com.voicenotesai.ACTION_VIEW_TASKS"
        
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "quick_capture"
        
        fun startService(context: Context) {
            val intent = Intent(context, QuickCaptureService::class.java).apply {
                action = ACTION_START_QUICK_CAPTURE
            }
            context.startForegroundService(intent)
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, QuickCaptureService::class.java).apply {
                action = ACTION_STOP_QUICK_CAPTURE
            }
            context.stopService(intent)
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_QUICK_CAPTURE -> {
                startForeground(NOTIFICATION_ID, createQuickCaptureNotification())
            }
            ACTION_STOP_QUICK_CAPTURE -> {
                stopForeground(true)
                stopSelf()
            }
        }
        
        return START_STICKY
    }
    
    private fun createQuickCaptureNotification(): Notification {
        // Quick Record Action
        val quickRecordIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_QUICK_RECORD
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val quickRecordPendingIntent = PendingIntent.getActivity(
            this,
            0,
            quickRecordIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // View Recent Notes Action
        val recentNotesIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_VIEW_RECENT_NOTES
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val recentNotesPendingIntent = PendingIntent.getActivity(
            this,
            1,
            recentNotesIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // View Tasks Action
        val tasksIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_VIEW_TASKS
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tasksPendingIntent = PendingIntent.getActivity(
            this,
            2,
            tasksIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentTitle("Voice Notes - Quick Capture")
            .setContentText("Tap to start recording or access your notes")
            .setContentIntent(quickRecordPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                R.drawable.ic_mic,
                "Record",
                quickRecordPendingIntent
            )
            .addAction(
                R.drawable.ic_notes,
                "Notes",
                recentNotesPendingIntent
            )
            .addAction(
                R.drawable.ic_tasks,
                "Tasks",
                tasksPendingIntent
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Quick access to voice recording and your notes. Tap Record to start capturing your thoughts instantly.")
            )
            .build()
    }
}