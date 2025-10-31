package com.voicenotesai.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.voicenotesai.R
import com.voicenotesai.data.notification.QuickCaptureService
import com.voicenotesai.presentation.MainActivity

/**
 * Home screen widget for quick voice note capture and recent notes access.
 */
class VoiceNotesWidget : AppWidgetProvider() {
    
    companion object {
        const val ACTION_QUICK_RECORD = "com.voicenotesai.widget.ACTION_QUICK_RECORD"
        const val ACTION_VIEW_NOTES = "com.voicenotesai.widget.ACTION_VIEW_NOTES"
        const val ACTION_VIEW_TASKS = "com.voicenotesai.widget.ACTION_VIEW_TASKS"
    }
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Update all widget instances
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_QUICK_RECORD -> {
                // Launch app with quick record action
                val recordIntent = Intent(context, MainActivity::class.java).apply {
                    action = QuickCaptureService.ACTION_QUICK_RECORD
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(recordIntent)
            }
            ACTION_VIEW_NOTES -> {
                // Launch app to view notes
                val notesIntent = Intent(context, MainActivity::class.java).apply {
                    action = QuickCaptureService.ACTION_VIEW_RECENT_NOTES
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(notesIntent)
            }
            ACTION_VIEW_TASKS -> {
                // Launch app to view tasks
                val tasksIntent = Intent(context, MainActivity::class.java).apply {
                    action = QuickCaptureService.ACTION_VIEW_TASKS
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(tasksIntent)
            }
        }
    }
    
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_voice_notes)
        
        // Set up quick record button
        val quickRecordIntent = Intent(context, VoiceNotesWidget::class.java).apply {
            action = ACTION_QUICK_RECORD
        }
        val quickRecordPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            quickRecordIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_quick_record_button, quickRecordPendingIntent)
        
        // Set up view notes button
        val viewNotesIntent = Intent(context, VoiceNotesWidget::class.java).apply {
            action = ACTION_VIEW_NOTES
        }
        val viewNotesPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            viewNotesIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_view_notes_button, viewNotesPendingIntent)
        
        // Set up view tasks button
        val viewTasksIntent = Intent(context, VoiceNotesWidget::class.java).apply {
            action = ACTION_VIEW_TASKS
        }
        val viewTasksPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            viewTasksIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_view_tasks_button, viewTasksPendingIntent)
        
        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}