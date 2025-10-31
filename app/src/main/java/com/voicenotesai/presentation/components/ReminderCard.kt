package com.voicenotesai.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.voicenotesai.domain.model.Reminder
import com.voicenotesai.domain.model.ReminderType
import com.voicenotesai.domain.model.ReminderWithContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Card component for displaying reminder information with actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderCard(
    reminderWithContext: ReminderWithContext,
    onToggleComplete: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onViewSource: (String?, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val reminder = reminderWithContext.reminder
    val isOverdue = reminder.triggerTime < System.currentTimeMillis() && !reminder.isCompleted
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                reminder.isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                isOverdue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with title and completion status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (reminder.isCompleted) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Completion checkbox
                Checkbox(
                    checked = reminder.isCompleted,
                    onCheckedChange = { onToggleComplete(reminder.id) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            
            // Description if available
            reminder.description?.let { description ->
                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (reminder.isCompleted) 0.5f else 0.7f
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alpha(if (reminder.isCompleted) 0.6f else 1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Time and type information
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Trigger time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Time",
                        tint = when {
                            reminder.isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            isOverdue -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = formatReminderTime(reminder.triggerTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            reminder.isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            isOverdue -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        }
                    )
                    
                    // Overdue indicator
                    if (isOverdue) {
                        Text(
                            text = "• Overdue",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Reminder type indicator
                if (reminder.reminderType != ReminderType.ONE_TIME) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Recurring",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = reminder.reminderType.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            
            // Source information
            if (reminderWithContext.sourceNote != null || reminderWithContext.sourceTask != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                reminderWithContext.sourceNote?.let { note ->
                    SourceChip(
                        text = "Note: ${note.content.take(30)}...",
                        onClick = { onViewSource(note.id, null) }
                    )
                }
                
                reminderWithContext.sourceTask?.let { task ->
                    SourceChip(
                        text = "Task: ${task.text.take(30)}...",
                        onClick = { onViewSource(null, task.id) }
                    )
                }
            }
            
            // Action buttons
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit button
                IconButton(
                    onClick = { onEdit(reminder.id) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit reminder",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                // Delete button
                IconButton(
                    onClick = { onDelete(reminder.id) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete reminder",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        border = AssistChipDefaults.assistChipBorder(
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = modifier
    )
}

private fun formatReminderTime(timestamp: Long): String {
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
    
    val isYesterday = run {
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = calendar.get(Calendar.DAY_OF_YEAR) == reminderCalendar.get(Calendar.DAY_OF_YEAR) &&
                calendar.get(Calendar.YEAR) == reminderCalendar.get(Calendar.YEAR)
        calendar.add(Calendar.DAY_OF_YEAR, 1) // Reset
        yesterday
    }
    
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    val fullDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    return when {
        isToday -> "Today ${timeFormat.format(Date(timestamp))}"
        isTomorrow -> "Tomorrow ${timeFormat.format(Date(timestamp))}"
        isYesterday -> "Yesterday ${timeFormat.format(Date(timestamp))}"
        timestamp > now -> "${dateFormat.format(Date(timestamp))} ${timeFormat.format(Date(timestamp))}"
        else -> fullDateFormat.format(Date(timestamp))
    }
}