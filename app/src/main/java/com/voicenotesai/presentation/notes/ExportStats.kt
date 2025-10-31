package com.voicenotesai.presentation.notes

/**
 * Statistics for export operations
 */
data class ExportStats(
    val noteCount: Int,
    val totalWords: Int,
    val totalCharacters: Int,
    val totalDuration: Long = 0L,
    val averageWordsPerNote: Int = if (noteCount > 0) totalWords / noteCount else 0
)