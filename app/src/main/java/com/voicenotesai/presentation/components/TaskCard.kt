package com.voicenotesai.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Note
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.voicenotesai.R
import com.voicenotesai.domain.model.EnhancedNote
import com.voicenotesai.domain.model.Task
import com.voicenotesai.domain.model.TaskPriority
// import com.voicenotesai.presentation.accessibility.cardAccessibility
// import com.voicenotesai.presentation.accessibility.toggleAccessibility
import com.voicenotesai.presentation.theme.ModernShapes
import com.voicenotesai.presentation.theme.ModernSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Task card component showing task text, source note, date, and completion status.
 * Supports swipe actions for task completion and deletion.
 */
@Composable
fun TaskCard(
    task: Task,
    sourceNote: EnhancedNote?,
    onToggleComplete: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
    onTaskClick: (() -> Unit)? = null
) {
    val taskStateDesc = if (task.isCompleted) {
        stringResource(R.string.task_completed_content_desc, task.text)
    } else {
        stringResource(R.string.task_pending_content_desc, task.text)
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = ModernShapes.borderWidth,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = ModernShapes.cardCorners
            )
            .let { cardModifier ->
                if (onTaskClick != null) {
                    cardModifier.clickable(onClick = onTaskClick)
                } else {
                    cardModifier
                }
            },
        shape = ModernShapes.cardCorners,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ModernSpacing.cardPadding),
            verticalAlignment = Alignment.Top
        ) {
            // Checkbox for completion status
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete(task.id) },
                modifier = Modifier
                    .padding(end = ModernSpacing.componentGap)

            )
            
            // Task content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Task text with strikethrough if completed
                Text(
                    text = task.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (task.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(ModernSpacing.small))
                
                // Task metadata row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side: Source note and date
                    Column {
                        // Source note indicator
                        if (sourceNote != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Note,
                                    contentDescription = "Source note",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = extractTitle(sourceNote.content).take(30) + if (extractTitle(sourceNote.content).length > 30) "..." else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        // Creation date
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Created",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatDate(task.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Right side: Priority indicator and actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ModernSpacing.small)
                    ) {
                        // Priority indicator
                        if (task.priority != TaskPriority.NORMAL) {
                            PriorityIndicator(priority = task.priority)
                        }
                        
                        // Delete button
                        IconButton(
                            onClick = { onDelete(task.id) },
                            modifier = Modifier
                                .size(32.dp)
                                .semantics {
                                    contentDescription = "Delete task: ${task.text}"
                                    role = Role.Button
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null, // Handled by parent
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                
                // Due date if present
                if (task.dueDate != null) {
                    Spacer(modifier = Modifier.height(ModernSpacing.extraSmall))
                    val isOverdue = task.dueDate < System.currentTimeMillis() && !task.isCompleted
                    Text(
                        text = "Due: ${formatDate(task.dueDate)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Priority indicator component showing a colored dot for task priority.
 */
@Composable
private fun PriorityIndicator(
    priority: TaskPriority,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (priority) {
        TaskPriority.LOW -> MaterialTheme.colorScheme.secondary to "Low"
        TaskPriority.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant to "Normal"
        TaskPriority.HIGH -> MaterialTheme.colorScheme.tertiary to "High"
        TaskPriority.URGENT -> MaterialTheme.colorScheme.error to "Urgent"
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

/**
 * Formats a timestamp to a readable date string.
 */
private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 24 * 60 * 60 * 1000 -> "Today" // Less than 24 hours
        diff < 2 * 24 * 60 * 60 * 1000 -> "Yesterday" // Less than 48 hours
        diff < 7 * 24 * 60 * 60 * 1000 -> { // Less than 7 days
            SimpleDateFormat("EEEE", Locale.getDefault()).format(date)
        }
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
    }
}

/**
 * Extracts a title from note content.
 */
private fun extractTitle(content: String): String {
    return content.lines().firstOrNull()?.take(50) ?: "Untitled Note"
}