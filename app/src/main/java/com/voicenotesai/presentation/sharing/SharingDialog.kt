package com.voicenotesai.presentation.sharing

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.voicenotesai.R
import com.voicenotesai.domain.sharing.ShareFormat
import com.voicenotesai.domain.sharing.TargetApp

/**
 * Dialog for sharing notes with various options
 */
@Composable
fun SharingDialog(
    isVisible: Boolean,
    noteTitle: String,
    onDismiss: () -> Unit,
    onShareAsText: () -> Unit,
    onShareAsFile: (ShareFormat) -> Unit,
    onShareToApp: (TargetApp, ShareFormat) -> Unit,
    onCreateShareableLink: (Int?) -> Unit,
    onCreateCalendarEvent: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Header
                    Text(
                        text = stringResource(R.string.share_note_title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = noteTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        // Quick share as text
                        item {
                            SharingOption(
                                icon = Icons.Default.Share,
                                title = stringResource(R.string.share_as_text),
                                subtitle = stringResource(R.string.share_as_text_description),
                                onClick = onShareAsText
                            )
                        }
                        
                        // Export formats
                        item {
                            Text(
                                text = stringResource(R.string.export_formats),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        
                        items(getExportFormats()) { format ->
                            SharingOption(
                                icon = format.icon,
                                title = stringResource(format.titleRes),
                                subtitle = stringResource(format.descriptionRes),
                                onClick = { onShareAsFile(format.shareFormat) }
                            )
                        }
                        
                        // Direct app sharing
                        item {
                            Text(
                                text = stringResource(R.string.share_to_apps),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                        }
                        
                        items(getTargetApps()) { app ->
                            SharingOption(
                                icon = app.icon,
                                title = stringResource(app.titleRes),
                                subtitle = stringResource(app.descriptionRes),
                                onClick = { onShareToApp(app.targetApp, ShareFormat.PLAIN_TEXT) }
                            )
                        }
                        
                        // Advanced options
                        item {
                            Text(
                                text = stringResource(R.string.advanced_sharing),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                        }
                        
                        item {
                            SharingOption(
                                icon = Icons.Default.Link,
                                title = stringResource(R.string.create_shareable_link),
                                subtitle = stringResource(R.string.create_shareable_link_description),
                                onClick = { onCreateShareableLink(null) }
                            )
                        }
                        
                        item {
                            SharingOption(
                                icon = Icons.Default.Event,
                                title = stringResource(R.string.create_calendar_event),
                                subtitle = stringResource(R.string.create_calendar_event_description),
                                onClick = onCreateCalendarEvent
                            )
                        }
                    }
                    
                    // Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual sharing option item
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SharingOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Data class for export format options
 */
private data class ExportFormatOption(
    val shareFormat: ShareFormat,
    val icon: ImageVector,
    val titleRes: Int,
    val descriptionRes: Int
)

/**
 * Data class for target app options
 */
private data class TargetAppOption(
    val targetApp: TargetApp,
    val icon: ImageVector,
    val titleRes: Int,
    val descriptionRes: Int
)

/**
 * Get available export formats
 */
private fun getExportFormats(): List<ExportFormatOption> = listOf(
    ExportFormatOption(
        shareFormat = ShareFormat.PDF,
        icon = Icons.Default.PictureAsPdf,
        titleRes = R.string.export_format_pdf,
        descriptionRes = R.string.export_format_pdf_description
    ),
    ExportFormatOption(
        shareFormat = ShareFormat.RTF,
        icon = Icons.Default.Description,
        titleRes = R.string.export_format_word,
        descriptionRes = R.string.export_format_word_description
    ),
    ExportFormatOption(
        shareFormat = ShareFormat.MARKDOWN,
        icon = Icons.Default.Code,
        titleRes = R.string.export_format_markdown,
        descriptionRes = R.string.export_format_markdown_description
    ),
    ExportFormatOption(
        shareFormat = ShareFormat.PLAIN_TEXT,
        icon = Icons.Default.TextFields,
        titleRes = R.string.export_format_text,
        descriptionRes = R.string.export_format_text_description
    )
)

/**
 * Get available target apps
 */
private fun getTargetApps(): List<TargetAppOption> = listOf(
    TargetAppOption(
        targetApp = TargetApp.EMAIL,
        icon = Icons.Default.Email,
        titleRes = R.string.share_to_email,
        descriptionRes = R.string.share_to_email_description
    ),
    TargetAppOption(
        targetApp = TargetApp.WHATSAPP,
        icon = Icons.Default.Chat,
        titleRes = R.string.share_to_whatsapp,
        descriptionRes = R.string.share_to_whatsapp_description
    ),
    TargetAppOption(
        targetApp = TargetApp.GOOGLE_DRIVE,
        icon = Icons.Default.CloudUpload,
        titleRes = R.string.share_to_drive,
        descriptionRes = R.string.share_to_drive_description
    ),
    TargetAppOption(
        targetApp = TargetApp.SLACK,
        icon = Icons.Default.Work,
        titleRes = R.string.share_to_slack,
        descriptionRes = R.string.share_to_slack_description
    )
)