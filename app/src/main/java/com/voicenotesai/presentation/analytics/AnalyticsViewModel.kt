package com.voicenotesai.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicenotesai.data.local.entity.Note
import com.voicenotesai.data.repository.NotesRepository
import com.voicenotesai.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * ViewModel for Analytics Screen
 * Manages data fetching and processing for analytics display
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val notesRepository: NotesRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    // Selected time period (Week, Month, Year)
    private val _selectedPeriod = MutableStateFlow(TimePeriod.WEEK)
    val selectedPeriod: StateFlow<TimePeriod> = _selectedPeriod.asStateFlow()

    // All notes from repository
    private val allNotes: Flow<List<Note>> = notesRepository.getAllNotes()
    
    // All tasks from repository
    private val allTasks = taskRepository.getAllTasksWithNotes()

    // Analytics data based on selected period
    val analyticsData: StateFlow<AnalyticsData> = combine(
        allNotes,
        allTasks,
        selectedPeriod
    ) { notes, tasks, period ->
        calculateAnalytics(notes, tasks, period)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsData()
    )

    fun selectPeriod(period: TimePeriod) {
        _selectedPeriod.value = period
    }

    private fun calculateAnalytics(notes: List<Note>, tasks: List<com.voicenotesai.domain.model.TaskWithNote>, period: TimePeriod): AnalyticsData {
        val periodStartTime = getPeriodStartTime(period)
        val filteredNotes = notes.filter { it.timestamp >= periodStartTime }
        val filteredTasks = tasks.filter { it.task.createdAt >= periodStartTime }

        // Calculate total notes
        val totalNotes = filteredNotes.size

        // Calculate completed tasks
        val completedTasks = filteredTasks.count { it.task.isCompleted }

        // Calculate total duration
        val totalDuration = filteredNotes.sumOf { it.duration }

        // Calculate average note length (word count)
        val averageNoteLength = if (totalNotes > 0) filteredNotes.sumOf { it.wordCount } / totalNotes else 0

        // Calculate daily note counts for chart
        val dailyCounts = calculateDailyCounts(filteredNotes, period)

        // Extract top tags
        val topTags = extractTopTags(filteredNotes)

        // Calculate trends (compare with previous period)
        val previousPeriodStart = getPeriodStartTime(period, offset = -1)
        val previousPeriodNotes = notes.filter { 
            it.timestamp >= previousPeriodStart && it.timestamp < periodStartTime 
        }
        val previousPeriodTasks = tasks.filter { 
            it.task.createdAt >= previousPeriodStart && it.task.createdAt < periodStartTime 
        }
        
        val notesTrend = calculateTrend(filteredNotes.size, previousPeriodNotes.size)
        val tasksTrend = calculateTrend(completedTasks, previousPeriodTasks.count { it.task.isCompleted })
        val previousAvgLength = if (previousPeriodNotes.isNotEmpty()) 
            previousPeriodNotes.sumOf { it.wordCount } / previousPeriodNotes.size 
        else 0
        val lengthTrend = calculateTrend(averageNoteLength, previousAvgLength)

        return AnalyticsData(
            totalNotes = totalNotes,
            completedTasks = completedTasks,
            totalDuration = totalDuration,
            averageNoteLength = averageNoteLength,
            dailyCounts = dailyCounts,
            topTags = topTags,
            notesTrend = notesTrend,
            tasksTrend = tasksTrend,
            lengthTrend = lengthTrend
        )
    }

    private fun getPeriodStartTime(period: TimePeriod, offset: Int = 0): Long {
        val calendar = Calendar.getInstance()
        return when (period) {
            TimePeriod.WEEK -> {
                calendar.add(Calendar.WEEK_OF_YEAR, offset)
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            TimePeriod.MONTH -> {
                calendar.add(Calendar.MONTH, offset)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            TimePeriod.YEAR -> {
                calendar.add(Calendar.YEAR, offset)
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
        }
    }

    private fun calculateDailyCounts(notes: List<Note>, period: TimePeriod): List<DailyCount> {
        val daysToShow = when (period) {
            TimePeriod.WEEK -> 7
            TimePeriod.MONTH -> 30
            TimePeriod.YEAR -> 12 // Show months instead of days for year
        }

        val calendar = Calendar.getInstance()
        val counts = mutableMapOf<String, Int>()

        // Initialize all days/months with 0
        for (i in 0 until daysToShow) {
            val label = when (period) {
                TimePeriod.WEEK -> calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, java.util.Locale.getDefault()) ?: ""
                TimePeriod.MONTH -> "${calendar.get(Calendar.DAY_OF_MONTH)}"
                TimePeriod.YEAR -> calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, java.util.Locale.getDefault()) ?: ""
            }
            counts[label] = 0
            calendar.add(when (period) {
                TimePeriod.YEAR -> Calendar.MONTH
                else -> Calendar.DAY_OF_MONTH
            }, -1)
        }

        // Count notes per day/month
        notes.forEach { note ->
            val noteCalendar = Calendar.getInstance().apply { timeInMillis = note.timestamp }
            val label = when (period) {
                TimePeriod.WEEK -> noteCalendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, java.util.Locale.getDefault()) ?: ""
                TimePeriod.MONTH -> "${noteCalendar.get(Calendar.DAY_OF_MONTH)}"
                TimePeriod.YEAR -> noteCalendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, java.util.Locale.getDefault()) ?: ""
            }
            counts[label] = (counts[label] ?: 0) + 1
        }

        return counts.map { (label, count) ->
            DailyCount(label = label, count = count)
        }.sortedBy { it.label }
    }

    private fun extractTopTags(notes: List<Note>): List<TagCount> {
        val tagCounts = mutableMapOf<String, Int>()
        
        notes.forEach { note ->
            // Parse tags from the tags field (comma-separated)
            val tags = note.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
            
            tags.forEach { tag ->
                tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
            }
        }

        return tagCounts
            .map { (tag, count) -> TagCount(tag = tag, count = count) }
            .sortedByDescending { it.count }
            .take(5) // Top 5 tags
    }

    private fun calculateTrend(current: Int, previous: Int): Float {
        if (previous == 0) return if (current > 0) 100f else 0f
        return ((current - previous).toFloat() / previous) * 100f
    }
}

/**
 * Time period options for analytics
 */
enum class TimePeriod {
    WEEK,
    MONTH,
    YEAR
}

/**
 * Analytics data container
 */
data class AnalyticsData(
    val totalNotes: Int = 0,
    val completedTasks: Int = 0,
    val totalDuration: Long = 0L,
    val averageNoteLength: Int = 0,
    val dailyCounts: List<DailyCount> = emptyList(),
    val topTags: List<TagCount> = emptyList(),
    val notesTrend: Float = 0f,
    val tasksTrend: Float = 0f,
    val lengthTrend: Float = 0f
)

/**
 * Daily/Monthly count for chart
 */
data class DailyCount(
    val label: String,
    val count: Int
)

/**
 * Tag usage count
 */
data class TagCount(
    val tag: String,
    val count: Int
)
