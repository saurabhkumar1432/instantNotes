package com.voicenotesai.presentation.help

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages persistent state for help system using SharedPreferences.
 * Tracks which quick tips have been dismissed and which tours have been completed.
 */
class HelpPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    /**
     * Check if a quick tip has been dismissed for a specific screen.
     */
    fun isQuickTipDismissed(screenKey: String): Boolean {
        return prefs.getBoolean(getQuickTipKey(screenKey), false)
    }
    
    /**
     * Mark a quick tip as dismissed for a specific screen.
     */
    fun dismissQuickTip(screenKey: String) {
        prefs.edit().putBoolean(getQuickTipKey(screenKey), true).apply()
    }
    
    /**
     * Check if a tour has been completed.
     */
    fun isTourCompleted(tourId: String): Boolean {
        return prefs.getBoolean(getTourKey(tourId), false)
    }
    
    /**
     * Mark a tour as completed.
     */
    fun completeTour(tourId: String) {
        prefs.edit().putBoolean(getTourKey(tourId), true).apply()
    }
    
    /**
     * Check if a tour has been skipped.
     */
    fun isTourSkipped(tourId: String): Boolean {
        return prefs.getBoolean(getTourSkipKey(tourId), false)
    }
    
    /**
     * Mark a tour as skipped.
     */
    fun skipTour(tourId: String) {
        prefs.edit().putBoolean(getTourSkipKey(tourId), true).apply()
    }
    
    /**
     * Reset all quick tips (show them again).
     */
    fun resetQuickTips() {
        val editor = prefs.edit()
        prefs.all.keys
            .filter { it.startsWith(QUICK_TIP_PREFIX) }
            .forEach { editor.remove(it) }
        editor.apply()
    }
    
    /**
     * Reset all tours (mark them as not completed).
     */
    fun resetTours() {
        val editor = prefs.edit()
        prefs.all.keys
            .filter { it.startsWith(TOUR_PREFIX) || it.startsWith(TOUR_SKIP_PREFIX) }
            .forEach { editor.remove(it) }
        editor.apply()
    }
    
    /**
     * Clear all help preferences.
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
    
    private fun getQuickTipKey(screenKey: String) = "$QUICK_TIP_PREFIX$screenKey"
    private fun getTourKey(tourId: String) = "$TOUR_PREFIX$tourId"
    private fun getTourSkipKey(tourId: String) = "$TOUR_SKIP_PREFIX$tourId"
    
    companion object {
        private const val PREFS_NAME = "help_preferences"
        private const val QUICK_TIP_PREFIX = "quick_tip_dismissed_"
        private const val TOUR_PREFIX = "tour_completed_"
        private const val TOUR_SKIP_PREFIX = "tour_skipped_"
        
        @Volatile
        private var instance: HelpPreferences? = null
        
        fun getInstance(context: Context): HelpPreferences {
            return instance ?: synchronized(this) {
                instance ?: HelpPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
