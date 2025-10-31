package com.voicenotesai.presentation.sharing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.voicenotesai.R

/**
 * Dialog for creating and managing shareable links
 */
@Composable
fun ShareableLinkDialog(
    isVisible: Boolean,
    noteTitle: String,
    onDismiss: () -> Unit,
    onCreateLink: (ShareableLinkOptions) -> Unit,
    generatedLink: String? = null,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedExpiration by remember { mutableStateOf(ExpirationOption.ONE_WEEK) }
    var requirePassword by remember { mutableStateOf(false) }
    var allowDownload by remember { mutableStateOf(true) }
    
    val clipboardManager = LocalClipboardManager.current
    
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.create_shareable_link),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                        }
                    }
                    
                    Text(
                        text = noteTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    if (generatedLink != null) {
                        // Show generated link
                        LinkResultSection(
                            link = generatedLink,
                            onCopyLink = {
                                clipboardManager.setText(AnnotatedString(generatedLink))
                            },
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    } else {
                        // Show configuration options
                        LinkConfigurationSection(
                            selectedExpiration = selectedExpiration,
                            onExpirationChange = { selectedExpiration = it },
                            requirePassword = requirePassword,
                            onPasswordChange = { requirePassword = it },
                            allowDownload = allowDownload,
                            onDownloadChange = { allowDownload = it },
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    
                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel))
                        }
                        
                        if (generatedLink == null) {
                            Button(
                                onClick = {
                                    onCreateLink(
                                        ShareableLinkOptions(
                                            expirationHours = selectedExpiration.hours,
                                            requirePassword = requirePassword,
                                            allowDownload = allowDownload
                                        )
                                    )
                                },
                                enabled = !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(stringResource(R.string.create_link))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Section showing the generated link
 */
@Composable
private fun LinkResultSection(
    link: String,
    onCopyLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.shareable_link_created),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = link,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(onClick = onCopyLink) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.copy_link)
                    )
                }
            }
        }
        
        Text(
            text = stringResource(R.string.link_sharing_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * Section for configuring link options
 */
@Composable
private fun LinkConfigurationSection(
    selectedExpiration: ExpirationOption,
    onExpirationChange: (ExpirationOption) -> Unit,
    requirePassword: Boolean,
    onPasswordChange: (Boolean) -> Unit,
    allowDownload: Boolean,
    onDownloadChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Expiration options
        Text(
            text = stringResource(R.string.link_expiration),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Column(
            modifier = Modifier
                .selectableGroup()
                .padding(bottom = 16.dp)
        ) {
            ExpirationOption.values().forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedExpiration == option,
                            onClick = { onExpirationChange(option) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedExpiration == option,
                        onClick = null
                    )
                    Text(
                        text = stringResource(option.labelRes),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
        
        // Additional options
        Text(
            text = stringResource(R.string.additional_options),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Password protection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = requirePassword,
                onCheckedChange = onPasswordChange
            )
            Column(
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.require_password),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.require_password_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Download permission
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = allowDownload,
                onCheckedChange = onDownloadChange
            )
            Column(
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.allow_download),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.allow_download_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Expiration options for shareable links
 */
enum class ExpirationOption(val hours: Int?, val labelRes: Int) {
    ONE_HOUR(1, R.string.expiration_1_hour),
    ONE_DAY(24, R.string.expiration_1_day),
    ONE_WEEK(24 * 7, R.string.expiration_1_week),
    ONE_MONTH(24 * 30, R.string.expiration_1_month),
    NEVER(null, R.string.expiration_never)
}

/**
 * Options for creating a shareable link
 */
data class ShareableLinkOptions(
    val expirationHours: Int?,
    val requirePassword: Boolean,
    val allowDownload: Boolean
)