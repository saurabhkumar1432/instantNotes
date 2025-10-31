package com.voicenotesai.presentation.notes

import androidx.annotation.StringRes
import com.voicenotesai.R

/**
 * Sort options for notes list
 */
enum class NotesSortType(@StringRes val displayNameRes: Int) {
    DATE_NEWEST(R.string.sort_date_newest),
    DATE_OLDEST(R.string.sort_date_oldest),
    TITLE_A_Z(R.string.sort_title_a_z),
    TITLE_Z_A(R.string.sort_title_z_a),
    DURATION_LONGEST(R.string.sort_duration_longest),
    DURATION_SHORTEST(R.string.sort_duration_shortest)
}