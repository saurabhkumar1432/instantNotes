package com.voicenotesai.presentation.notes

import androidx.annotation.StringRes
import com.voicenotesai.R

/**
 * Filter options for notes list
 */
enum class NotesFilter(@StringRes val displayNameRes: Int) {
    ALL(R.string.filter_all),
    RECENT(R.string.filter_recent),
    ARCHIVED(R.string.filter_archived),
    FAVORITES(R.string.filter_favorites),
    WITH_TASKS(R.string.filter_with_tasks)
}