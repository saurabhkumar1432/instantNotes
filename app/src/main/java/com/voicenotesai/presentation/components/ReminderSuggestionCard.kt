package com.voicenotesai.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.voicenotesai.domain.model.DetectedDateTime
import com.voicenotesai.domain.model.DateTimeType
import java.text.SimpleDateFormat
import java.util.*

/**
 * Card component for displaying AI-detected date/time suggestions for reminder creation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSuggestionCard(
    detectedDateTime: DetectedDateTime,
    onCreateReminder: (DetectedDateTime) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with suggestion icon and dismiss button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Reminder suggestion",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Reminder Suggestion",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss suggestion",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Detected text and interpretation
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Original detected text
                Text(
                    text = "Detected: \"${detectedDateTime.text}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Interpreted date/time
                Text(
                    text = "Interpreted as: ${formatDetectedDateTime(detectedDateTime)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Suggested reminder time
                detectedDateTime.suggestedReminderTime?.let { reminderTime ->
                    Text(
                        text = "Suggested reminder: ${formatReminderTime(reminderTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Confidence indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Confidence:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    LinearProgressIndicator(
                        progress = detectedDateTime.confidence,
                        modifier = Modifier
                            .width(60.dp)
                            .height(4.dp),
                        color = when {
                            detectedDateTime.confidence >= 0.8f -> MaterialTheme.colorScheme.primary
                            detectedDateTime.confidence >= 0.6f -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.error
                        },
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "${(detectedDateTime.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action button
            Button(
                onClick = { onCreateReminder(detectedDateTime) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Create Reminder",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

private fun formatDetectedDateTime(detectedDateTime: DetectedDateTime): String {
    val date = Date(detectedDateTime.timestamp)
    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance()
    val detectedCalendar = Calendar.getInstance().apply { timeInMillis = detectedDateTime.timestamp }
    
    return when (detectedDateTime.type) {
        DateTimeType.ABSOLUTE_DATE -> {
            val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
            dateFormat.format(date)
        }
        DateTimeType.RELATIVE_DATE -> {
            val isToday = calendar.get(Calendar.DAY_OF_YEAR) == detectedCalendar.get(Calendar.DAY_OF_YEAR) &&
                    calendar.get(Calendar.YEAR) == detectedCalendar.get(Calendar.YEAR)
            
            val isTomorrow = run {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val tomorrow = calendar.get(Calendar.DAY_OF_YEAR) == detectedCalendar.get(Calendar.DAY_OF_YEAR) &&
                        calendar.get(Calendar.YEAR) == detectedCalendar.get(Calendar.YEAR)
                calendar.add(Calendar.DAY_OF_YEAR, -1) // Reset
                tomorrow
            }
            
            when {
                isToday -> "Today"
                isTomorrow -> "Tomorrow"
                else -> {
                    val dateFormat = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
                    dateFormat.format(date)
                }
            }
        }
        DateTimeType.TIME_ONLY -> {
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            "Today at ${timeFormat.format(date)}"
        }
        DateTimeType.DATETIME -> {
            val dateTimeFormat = SimpleDateFormat("EEEE, MMMM dd 'at' h:mm a", Locale.getDefault())
            dateTimeFormat.format(date)
        }
        DateTimeType.RECURRING -> {
            "Recurring: ${detectedDateTime.text}"
        }
    }
}

private fun formatReminderTime(timestamp: Long): String {
    val date = Date(timestamp)
    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance()
    val reminderCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    
    val isToday = calendar.get(Calendar.DAY_OF_YEAR) == reminderCalendar.get(Calendar.DAY_OF_YEAR) &&
            calendar.get(Calendar.YEAR) == reminderCalendar.get(Calendar.YEAR)
    
    val isTomorrow = run {
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrow = calendar.get(Calendar.DAY_OF_YEAR) == reminderCalendar.get(Calendar.DAY_OF_YEAR) &&
                calendar.get(Calendar.YEAR) == reminderCalendar.get(Calendar.YEAR)
        calendar.add(Calendar.DAY_OF_YEAR, -1) // Reset
        tomorrow
    }
    
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("MMM dd 'at' h:mm a", Locale.getDefault())
    
    return when {
        isToday -> "Today at ${timeFormat.format(date)}"
        isTomorrow -> "Tomorrow at ${timeFormat.format(date)}"
        else -> dateFormat.format(date)
    }
}