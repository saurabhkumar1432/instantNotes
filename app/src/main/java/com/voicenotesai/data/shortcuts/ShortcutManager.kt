package com.voicenotesai.data.shortcuts

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager as AndroidShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import com.voicenotesai.R
import com.voicenotesai.data.notification.QuickCaptureService
import com.voicenotesai.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Android app shortcuts for quick access to key features.
 */
@Singleton
class ShortcutManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val shortcutManager: AndroidShortcutManager? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            context.getSystemService(AndroidShortcutManager::class.java)
        } else {
            null
        }
    }
    
    /**
     * Creates and publishes dynamic shortcuts for the app.
     * Note: We use manifest shortcuts instead of dynamic shortcuts to avoid conflicts.
     */
    fun createShortcuts() {
        // Using manifest shortcuts defined in shortcuts.xml instead of dynamic shortcuts
        // to avoid conflicts with existing shortcut IDs
    }
    
    /**
     * Updates shortcut usage to help Android prioritize frequently used shortcuts.
     */
    fun reportShortcutUsed(shortcutId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            shortcutManager?.reportShortcutUsed(shortcutId)
        }
    }
    
    /**
     * Removes all dynamic shortcuts.
     */
    fun removeAllShortcuts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            shortcutManager?.removeAllDynamicShortcuts()
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun createDynamicShortcuts() {
        val shortcuts = listOf(
            createQuickRecordShortcut(),
            createViewTasksShortcut(),
            createRecentNotesShortcut()
        )
        
        shortcutManager?.dynamicShortcuts = shortcuts
    }
    
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun createQuickRecordShortcut(): ShortcutInfo {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = QuickCaptureService.ACTION_QUICK_RECORD
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        return ShortcutInfo.Builder(context, "quick_record")
            .setShortLabel("Quick Record")
            .setLongLabel("Start Recording Voice Note")
            .setIcon(Icon.createWithResource(context, R.drawable.ic_mic))
            .setIntent(intent)
            .setRank(0)
            .build()
    }
    
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun createViewTasksShortcut(): ShortcutInfo {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = QuickCaptureService.ACTION_VIEW_TASKS
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        return ShortcutInfo.Builder(context, "view_tasks")
            .setShortLabel("View Tasks")
            .setLongLabel("View Pending Tasks")
            .setIcon(Icon.createWithResource(context, R.drawable.ic_tasks))
            .setIntent(intent)
            .setRank(1)
            .build()
    }
    
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun createRecentNotesShortcut(): ShortcutInfo {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = QuickCaptureService.ACTION_VIEW_RECENT_NOTES
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        return ShortcutInfo.Builder(context, "recent_notes")
            .setShortLabel("Recent Notes")
            .setLongLabel("View Recent Voice Notes")
            .setIcon(Icon.createWithResource(context, R.drawable.ic_notes))
            .setIntent(intent)
            .setRank(2)
            .build()
    }
}