package com.voicenotesai.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicenotesai.domain.error.ErrorAction
import com.voicenotesai.domain.error.ErrorSeverity
import com.voicenotesai.domain.error.RecoverySuggestion
import com.voicenotesai.domain.error.UserMessage
import com.voicenotesai.presentation.theme.ModernSpacing

/**
 * Comprehensive error recovery dialog with guided steps and suggestions.
 */
@Composable
fun ErrorRecoveryDialog(
    userMessage: UserMessage,
    actions: List<ErrorAction>,
    recoverySuggestions: List<RecoverySuggestion>,
    isRecovering: Boolean = false,
    onActionClick: (ErrorAction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ModernSpacing.large),
            verticalArrangement = Arrangement.spacedBy(ModernSpacing.medium)
        ) {
            // Error header with severity indicator
            ErrorHeader(
                userMessage = userMessage,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Recovery suggestions if available
            if (recoverySuggestions.isNotEmpty()) {
                RecoverySuggestionsSection(
                    suggestions = recoverySuggestions,
                    onSuggestionClick = onActionClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Guidance text if available
            userMessage.guidance?.let { guidance ->
                GuidanceSection(
                    guidance = guidance,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Action buttons
            ErrorActionButtons(
                actions = actions,
                isRecovering = isRecovering,
                onActionClick = onActionClick,
                onDismiss = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Error header with severity indicator and message.
 */
@Composable
fun ErrorHeader(
    userMessage: UserMessage,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ModernSpacing.medium),
        verticalAlignment = Alignment.Top
    ) {
        // Severity icon
        val (icon, color) = when (userMessage.severity) {
            ErrorSeverity.INFO -> Icons.Default.Info to MaterialTheme.colorScheme.primary
            ErrorSeverity.WARNING -> Icons.Default.Warning to MaterialTheme.colorScheme.primary
            ErrorSeverity.ERROR -> Icons.Default.Error to MaterialTheme.colorScheme.error
            ErrorSeverity.CRITICAL -> Icons.Default.Error to MaterialTheme.colorScheme.error
        }
        
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        
        // Error message content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(ModernSpacing.small)
        ) {
            Text(
                text = userMessage.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = userMessage.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * Recovery suggestions section with prioritized suggestions.
 */
@Composable
fun RecoverySuggestionsSection(
    suggestions: List<RecoverySuggestion>,
    onSuggestionClick: (ErrorAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(suggestions.size <= 2) }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ModernSpacing.small)
    ) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ModernSpacing.small)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Suggested Solutions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            if (suggestions.size > 2) {
                TextButton(
                    onClick = { expanded = !expanded }
                ) {
                    Text(
                        text = if (expanded) "Show less" else "Show all (${suggestions.size})",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        
        // Suggestions list
        val displayedSuggestions = if (expanded) {
            suggestions.sortedByDescending { it.priority }
        } else {
            suggestions.sortedByDescending { it.priority }.take(2)
        }
        
        displayedSuggestions.forEach { suggestion ->
            RecoverySuggestionCard(
                suggestion = suggestion,
                onClick = { onSuggestionClick(suggestion.action) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Individual recovery suggestion card.
 */
@Composable
fun RecoverySuggestionCard(
    suggestion: RecoverySuggestion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ModernSpacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = suggestion.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = suggestion.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
            
            val actionLabel = when (suggestion.action) {
                is ErrorAction.Retry -> "Try"
                is ErrorAction.NavigateToSettings -> "Fix"
                is ErrorAction.OpenPermissions -> "Allow"
                is ErrorAction.Custom -> "Do"
                else -> "Go"
            }
            
            FilledTonalButton(
                onClick = onClick,
                modifier = Modifier.semantics {
                    contentDescription = "Apply suggestion: ${suggestion.title}"
                }
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

/**
 * Guidance section with helpful information.
 */
@Composable
fun GuidanceSection(
    guidance: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                RoundedCornerShape(8.dp)
            )
            .padding(ModernSpacing.medium)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(ModernSpacing.small)
        ) {
            Icon(
                imageVector = Icons.Default.HelpOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = guidance,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Error action buttons with recovery state handling.
 */
@Composable
fun ErrorActionButtons(
    actions: List<ErrorAction>,
    isRecovering: Boolean,
    onActionClick: (ErrorAction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ModernSpacing.small)
    ) {
        // Primary actions (retry, settings, etc.)
        val primaryActions = actions.filterNot { it is ErrorAction.Dismiss }
        if (primaryActions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ModernSpacing.small)
            ) {
                primaryActions.take(2).forEach { action ->
                    ErrorActionButton(
                        action = action,
                        isRecovering = isRecovering,
                        onClick = { onActionClick(action) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Secondary actions
        val secondaryActions = actions.drop(2).filterNot { it is ErrorAction.Dismiss }
        if (secondaryActions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ModernSpacing.small)
            ) {
                secondaryActions.forEach { action ->
                    OutlinedButton(
                        onClick = { onActionClick(action) },
                        enabled = !isRecovering,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = getActionLabel(action),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
        
        // Dismiss button
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Dismiss",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/**
 * Individual error action button.
 */
@Composable
fun ErrorActionButton(
    action: ErrorAction,
    isRecovering: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (label, icon, isPrimary) = when (action) {
        is ErrorAction.Retry -> Triple("Retry", Icons.Default.Refresh, true)
        is ErrorAction.NavigateToSettings -> Triple("Settings", Icons.Default.Settings, true)
        is ErrorAction.OpenPermissions -> Triple("Allow", Icons.Default.CheckCircle, true)
        is ErrorAction.Custom -> Triple(action.label, Icons.Default.PlayArrow, false)
        else -> Triple(getActionLabel(action), Icons.Default.PlayArrow, false)
    }
    
    if (isPrimary) {
        Button(
            onClick = onClick,
            enabled = !isRecovering,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (isRecovering && action is ErrorAction.Retry) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isRecovering && action is ErrorAction.Retry) "Retrying..." else label,
                style = MaterialTheme.typography.labelMedium
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = !isRecovering,
            modifier = modifier
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/**
 * Recovery progress indicator for ongoing recovery operations.
 */
@Composable
fun RecoveryProgressIndicator(
    isVisible: Boolean,
    message: String = "Attempting to resolve the issue...",
    progress: Float? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ModernSpacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ModernSpacing.medium)
            ) {
                CircularProgressIndicator(
                    progress = progress ?: 0f,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 3.dp
                )
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Recovery in Progress",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Get user-friendly label for error actions.
 */
private fun getActionLabel(action: ErrorAction): String {
    return when (action) {
        is ErrorAction.Retry -> action.label
        is ErrorAction.NavigateToSettings -> action.label
        is ErrorAction.OpenPermissions -> action.label
        is ErrorAction.Dismiss -> action.label
        is ErrorAction.ViewDetails -> action.label
        is ErrorAction.ContactSupport -> action.label
        is ErrorAction.Custom -> action.label
    }
}