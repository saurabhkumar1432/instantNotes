package com.voicenotesai.data.ai

import com.voicenotesai.domain.model.DateTimeDetectionResult
import com.voicenotesai.domain.model.DetectedDateTime
import com.voicenotesai.domain.model.DateTimeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for detecting dates and times in note content using pattern matching and AI.
 * Provides intelligent suggestions for reminder creation.
 */
@Singleton
class DateTimeDetectionService @Inject constructor() {
    
    private val dateFormats = listOf(
        SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()),
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()),
        SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("MMMM dd", Locale.getDefault()),
        SimpleDateFormat("MMM dd", Locale.getDefault())
    )
    
    private val timeFormats = listOf(
        SimpleDateFormat("h:mm a", Locale.getDefault()),
        SimpleDateFormat("HH:mm", Locale.getDefault()),
        SimpleDateFormat("h a", Locale.getDefault())
    )
    
    private val dateTimeFormats = listOf(
        SimpleDateFormat("MMMM dd, yyyy 'at' h:mm a", Locale.getDefault()),
        SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault()),
        SimpleDateFormat("MM/dd/yyyy h:mm a", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    )
    
    // Regex patterns for different date/time expressions
    private val patterns = mapOf(
        DateTimeType.RELATIVE_DATE to listOf(
            Pattern.compile("\\b(tomorrow|tmrw)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(today)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(next week)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(next month)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(in \\d+ days?)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(in \\d+ weeks?)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(in \\d+ months?)\\b", Pattern.CASE_INSENSITIVE)
        ),
        DateTimeType.RECURRING to listOf(
            Pattern.compile("\\b(every (monday|tuesday|wednesday|thursday|friday|saturday|sunday))\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(every week)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(every month)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(daily|weekly|monthly)\\b", Pattern.CASE_INSENSITIVE)
        )
    )
    
    /**
     * Detect dates and times in the given text content.
     */
    suspend fun detectDateTimes(content: String): DateTimeDetectionResult = withContext(Dispatchers.Default) {
        try {
            val detectedDates = mutableListOf<DetectedDateTime>()
            
            // Detect absolute dates and times
            detectedDates.addAll(detectAbsoluteDates(content))
            detectedDates.addAll(detectTimes(content))
            detectedDates.addAll(detectAbsoluteDateTimes(content))
            
            // Detect relative dates
            detectedDates.addAll(detectRelativeDates(content))
            
            // Detect recurring patterns
            detectedDates.addAll(detectRecurringPatterns(content))
            
            DateTimeDetectionResult(
                success = true,
                detectedDates = detectedDates.distinctBy { it.text },
                confidence = if (detectedDates.isNotEmpty()) 0.8f else 0f
            )
        } catch (e: Exception) {
            DateTimeDetectionResult(
                success = false,
                error = "Failed to detect dates/times: ${e.message}"
            )
        }
    }
    
    private fun detectAbsoluteDates(content: String): List<DetectedDateTime> {
        val detected = mutableListOf<DetectedDateTime>()
        
        dateFormats.forEach { format ->
            val pattern = createPatternFromFormat(format)
            val matcher = pattern.matcher(content)
            
            while (matcher.find()) {
                try {
                    val dateText = matcher.group()
                    val date = format.parse(dateText)
                    date?.let {
                        val calendar = Calendar.getInstance().apply { time = it }
                        // If year is not specified, assume current year
                        if (calendar.get(Calendar.YEAR) == 1970) {
                            calendar.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR))
                        }
                        
                        detected.add(
                            DetectedDateTime(
                                text = dateText,
                                timestamp = calendar.timeInMillis,
                                type = DateTimeType.ABSOLUTE_DATE,
                                confidence = 0.9f,
                                suggestedReminderTime = calendar.timeInMillis - (24 * 60 * 60 * 1000) // 1 day before
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Ignore parsing errors
                }
            }
        }
        
        return detected
    }
    
    private fun detectTimes(content: String): List<DetectedDateTime> {
        val detected = mutableListOf<DetectedDateTime>()
        
        timeFormats.forEach { format ->
            val pattern = createPatternFromFormat(format)
            val matcher = pattern.matcher(content)
            
            while (matcher.find()) {
                try {
                    val timeText = matcher.group()
                    val time = format.parse(timeText)
                    time?.let {
                        val calendar = Calendar.getInstance()
                        val timeCalendar = Calendar.getInstance().apply { this.time = it }
                        
                        calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                        calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        
                        // If time has passed today, assume tomorrow
                        if (calendar.timeInMillis < System.currentTimeMillis()) {
                            calendar.add(Calendar.DAY_OF_MONTH, 1)
                        }
                        
                        detected.add(
                            DetectedDateTime(
                                text = timeText,
                                timestamp = calendar.timeInMillis,
                                type = DateTimeType.TIME_ONLY,
                                confidence = 0.8f,
                                suggestedReminderTime = calendar.timeInMillis - (60 * 60 * 1000) // 1 hour before
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Ignore parsing errors
                }
            }
        }
        
        return detected
    }
    
    private fun detectAbsoluteDateTimes(content: String): List<DetectedDateTime> {
        val detected = mutableListOf<DetectedDateTime>()
        
        dateTimeFormats.forEach { format ->
            val pattern = createPatternFromFormat(format)
            val matcher = pattern.matcher(content)
            
            while (matcher.find()) {
                try {
                    val dateTimeText = matcher.group()
                    val dateTime = format.parse(dateTimeText)
                    dateTime?.let {
                        detected.add(
                            DetectedDateTime(
                                text = dateTimeText,
                                timestamp = it.time,
                                type = DateTimeType.DATETIME,
                                confidence = 0.95f,
                                suggestedReminderTime = it.time - (60 * 60 * 1000) // 1 hour before
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Ignore parsing errors
                }
            }
        }
        
        return detected
    }
    
    private fun detectRelativeDates(content: String): List<DetectedDateTime> {
        val detected = mutableListOf<DetectedDateTime>()
        val calendar = Calendar.getInstance()
        
        patterns[DateTimeType.RELATIVE_DATE]?.forEach { pattern ->
            val matcher = pattern.matcher(content)
            
            while (matcher.find()) {
                val text = matcher.group().lowercase()
                val timestamp = when {
                    text.contains("tomorrow") || text.contains("tmrw") -> {
                        calendar.add(Calendar.DAY_OF_MONTH, 1)
                        calendar.timeInMillis
                    }
                    text.contains("today") -> {
                        calendar.timeInMillis
                    }
                    text.contains("next week") -> {
                        calendar.add(Calendar.WEEK_OF_YEAR, 1)
                        calendar.timeInMillis
                    }
                    text.contains("next month") -> {
                        calendar.add(Calendar.MONTH, 1)
                        calendar.timeInMillis
                    }
                    text.matches(Regex("in \\d+ days?")) -> {
                        val days = Regex("\\d+").find(text)?.value?.toInt() ?: 1
                        calendar.add(Calendar.DAY_OF_MONTH, days)
                        calendar.timeInMillis
                    }
                    text.matches(Regex("in \\d+ weeks?")) -> {
                        val weeks = Regex("\\d+").find(text)?.value?.toInt() ?: 1
                        calendar.add(Calendar.WEEK_OF_YEAR, weeks)
                        calendar.timeInMillis
                    }
                    text.matches(Regex("in \\d+ months?")) -> {
                        val months = Regex("\\d+").find(text)?.value?.toInt() ?: 1
                        calendar.add(Calendar.MONTH, months)
                        calendar.timeInMillis
                    }
                    else -> null
                }
                
                timestamp?.let {
                    detected.add(
                        DetectedDateTime(
                            text = matcher.group(),
                            timestamp = it,
                            type = DateTimeType.RELATIVE_DATE,
                            confidence = 0.85f,
                            suggestedReminderTime = it - (24 * 60 * 60 * 1000) // 1 day before
                        )
                    )
                }
                
                // Reset calendar for next iteration
                calendar.time = Date()
            }
        }
        
        return detected
    }
    
    private fun detectRecurringPatterns(content: String): List<DetectedDateTime> {
        val detected = mutableListOf<DetectedDateTime>()
        
        patterns[DateTimeType.RECURRING]?.forEach { pattern ->
            val matcher = pattern.matcher(content)
            
            while (matcher.find()) {
                val text = matcher.group()
                // For recurring patterns, we'll set the next occurrence
                val calendar = Calendar.getInstance()
                
                val timestamp = when {
                    text.lowercase().contains("every monday") -> {
                        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                        if (calendar.timeInMillis <= System.currentTimeMillis()) {
                            calendar.add(Calendar.WEEK_OF_YEAR, 1)
                        }
                        calendar.timeInMillis
                    }
                    text.lowercase().contains("every tuesday") -> {
                        calendar.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
                        if (calendar.timeInMillis <= System.currentTimeMillis()) {
                            calendar.add(Calendar.WEEK_OF_YEAR, 1)
                        }
                        calendar.timeInMillis
                    }
                    // Add more day patterns as needed
                    text.lowercase().contains("every week") || text.lowercase().contains("weekly") -> {
                        calendar.add(Calendar.WEEK_OF_YEAR, 1)
                        calendar.timeInMillis
                    }
                    text.lowercase().contains("every month") || text.lowercase().contains("monthly") -> {
                        calendar.add(Calendar.MONTH, 1)
                        calendar.timeInMillis
                    }
                    text.lowercase().contains("daily") -> {
                        calendar.add(Calendar.DAY_OF_MONTH, 1)
                        calendar.timeInMillis
                    }
                    else -> null
                }
                
                timestamp?.let {
                    detected.add(
                        DetectedDateTime(
                            text = text,
                            timestamp = it,
                            type = DateTimeType.RECURRING,
                            confidence = 0.8f,
                            suggestedReminderTime = it - (60 * 60 * 1000) // 1 hour before
                        )
                    )
                }
            }
        }
        
        return detected
    }
    
    private fun createPatternFromFormat(format: SimpleDateFormat): Pattern {
        // This is a simplified approach - in a real implementation,
        // you'd want more sophisticated pattern generation
        val patternString = when (format.toPattern()) {
            "MMMM dd, yyyy" -> "\\b[A-Za-z]+ \\d{1,2}, \\d{4}\\b"
            "MMM dd, yyyy" -> "\\b[A-Za-z]{3} \\d{1,2}, \\d{4}\\b"
            "MM/dd/yyyy" -> "\\b\\d{1,2}/\\d{1,2}/\\d{4}\\b"
            "dd/MM/yyyy" -> "\\b\\d{1,2}/\\d{1,2}/\\d{4}\\b"
            "yyyy-MM-dd" -> "\\b\\d{4}-\\d{1,2}-\\d{1,2}\\b"
            "MMMM dd" -> "\\b[A-Za-z]+ \\d{1,2}\\b"
            "MMM dd" -> "\\b[A-Za-z]{3} \\d{1,2}\\b"
            "h:mm a" -> "\\b\\d{1,2}:\\d{2} [AaPp][Mm]\\b"
            "HH:mm" -> "\\b\\d{1,2}:\\d{2}\\b"
            "h a" -> "\\b\\d{1,2} [AaPp][Mm]\\b"
            "MMMM dd, yyyy 'at' h:mm a" -> "\\b[A-Za-z]+ \\d{1,2}, \\d{4} at \\d{1,2}:\\d{2} [AaPp][Mm]\\b"
            "MMM dd, yyyy 'at' h:mm a" -> "\\b[A-Za-z]{3} \\d{1,2}, \\d{4} at \\d{1,2}:\\d{2} [AaPp][Mm]\\b"
            "MM/dd/yyyy h:mm a" -> "\\b\\d{1,2}/\\d{1,2}/\\d{4} \\d{1,2}:\\d{2} [AaPp][Mm]\\b"
            "yyyy-MM-dd HH:mm" -> "\\b\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{2}\\b"
            else -> "\\b\\w+\\b" // Fallback pattern
        }
        
        return Pattern.compile(patternString, Pattern.CASE_INSENSITIVE)
    }
}