package com.voicenotesai.presentation.notes.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.voicenotesai.presentation.animations.bouncyClickable
import com.voicenotesai.presentation.notes.ExportFormat
import com.voicenotesai.presentation.notes.ExportStats
import com.voicenotesai.presentation.notes.NotesFilter
import com.voicenotesai.presentation.notes.NotesSortType
import com.voicenotesai.presentation.theme.ExtendedTypography
import com.voicenotesai.presentation.theme.Spacing

/**
 * Search bar component for notes with real-time search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    placeholderRes: Int = com.voicenotesai.R.string.search_placeholder,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val searchBarDesc = stringResource(id = com.voicenotesai.R.string.search_bar_content_description)
    
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
    placeholder = { Text(stringResource(id = placeholderRes)) },
        leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(id = com.voicenotesai.R.string.search_icon_description)
                )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                    IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSearchQueryChange("")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(id = com.voicenotesai.R.string.clear_search)
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                // Search is performed in real-time, no action needed
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = searchBarDesc
            }
    )
}

/**
 * Filter chips row for note filtering options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesFilterRow(
    selectedFilter: NotesFilter,
    onFilterChange: (NotesFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
                items(NotesFilter.values()) { filter ->
                val label = stringResource(id = filter.displayNameRes)
                val filterDesc = stringResource(id = com.voicenotesai.R.string.filter_notes_by_format, label)

                FilterChip(
                selected = selectedFilter == filter,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onFilterChange(filter)
                },
                label = {
                    Text(
                        text = stringResource(id = filter.displayNameRes),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.semantics {
                    contentDescription = filterDesc
                }
            )
        }
    }
}

/**
 * Sort dropdown for note sorting options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesSortDropdown(
    selectedSort: NotesSortType,
    onSortChange: (NotesSortType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

        ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
            val selectedSortLabel = stringResource(id = selectedSort.displayNameRes)
            val sortDesc = stringResource(id = com.voicenotesai.R.string.sort_by) + " " + selectedSortLabel

            OutlinedTextField(
            value = stringResource(id = selectedSort.displayNameRes),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(id = com.voicenotesai.R.string.sort_by)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .semantics {
                    contentDescription = sortDesc
                }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            NotesSortType.values().forEach { sortType ->
                DropdownMenuItem(
                    text = { Text(stringResource(id = sortType.displayNameRes)) },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSortChange(sortType)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Selection toolbar that appears when notes are selected.
 */
@Composable
fun NotesSelectionToolbar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    AnimatedVisibility(
        visible = selectedCount > 0,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selection info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    // Selection count indicator
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = selectedCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    Text(
                        text = stringResource(id = com.voicenotesai.R.string.selected_of_total_format, selectedCount, totalCount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedCount < totalCount) {
                        val selectAllDesc = stringResource(id = com.voicenotesai.R.string.select_all_desc)
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectAll()
                            },
                            modifier = Modifier.semantics {
                                contentDescription = selectAllDesc
                            }
                        ) {
                            Text(stringResource(id = com.voicenotesai.R.string.select_all))
                        }
                    }
                    
                    val deselectAllDesc = stringResource(id = com.voicenotesai.R.string.deselect_all_desc)

                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDeselectAll()
                        },
                            modifier = Modifier.semantics {
                                contentDescription = deselectAllDesc
                            }
                    ) {
                        Text(stringResource(id = com.voicenotesai.R.string.deselect))
                    }
                    
                    val exportSelectedDesc = stringResource(id = com.voicenotesai.R.string.export_selected_desc)

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onExport()
                        },
                        modifier = Modifier.semantics {
                            contentDescription = exportSelectedDesc
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null
                        )
                    }
                    
                    val deleteSelectedDesc = stringResource(id = com.voicenotesai.R.string.delete_selected_desc)

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDeleteSelected()
                        },
                        modifier = Modifier.semantics {
                            contentDescription = deleteSelectedDesc
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * Export dialog for choosing export format and viewing statistics.
 */
@Composable
fun NotesExportDialog(
    isVisible: Boolean,
    exportStats: ExportStats,
    onDismiss: () -> Unit,
    onExport: (ExportFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(id = com.voicenotesai.R.string.export_notes_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    // Export statistics
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(Spacing.medium),
                            verticalArrangement = Arrangement.spacedBy(Spacing.small)
                        ) {
                            Text(
                                text = stringResource(id = com.voicenotesai.R.string.export_statistics),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                                Text(
                                    text = stringResource(id = com.voicenotesai.R.string.export_stats_notes_count, exportStats.noteCount),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = stringResource(id = com.voicenotesai.R.string.export_stats_words_count, exportStats.totalWords),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = stringResource(id = com.voicenotesai.R.string.export_stats_chars_count, exportStats.totalCharacters),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                        }
                    }
                    
                    // Format selection
                    Text(
                        text = stringResource(id = com.voicenotesai.R.string.choose_export_format),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        ExportFormat.values().forEach { format ->
                            ExportFormatOption(
                                format = format,
                                onSelect = {
                                    onExport(format)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = com.voicenotesai.R.string.cancel))
                }
            },
            modifier = modifier
        )
    }
}

/**
 * Export format option button.
 */
@Composable
private fun ExportFormatOption(
    format: ExportFormat,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    val description = when (format) {
        ExportFormat.TEXT -> stringResource(id = com.voicenotesai.R.string.export_desc_text)
        ExportFormat.MARKDOWN -> stringResource(id = com.voicenotesai.R.string.export_desc_markdown)
        ExportFormat.JSON -> stringResource(id = com.voicenotesai.R.string.export_desc_json)
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelect()
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(id = format.displayNameRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = format.fileExtension.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Batch delete confirmation dialog.
 */
@Composable
fun BatchDeleteDialog(
    isVisible: Boolean,
    noteCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = stringResource(id = com.voicenotesai.R.string.delete_notes_count_title, noteCount),
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = stringResource(id = com.voicenotesai.R.string.delete_notes_count_text, noteCount),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(id = com.voicenotesai.R.string.delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = com.voicenotesai.R.string.cancel))
                }
            },
            modifier = modifier
        )
    }
}

/**
 * Empty search results state.
 */
@Composable
fun EmptySearchState(
    searchQuery: String,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = stringResource(id = com.voicenotesai.R.string.no_notes_found),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = stringResource(id = com.voicenotesai.R.string.no_notes_match_search_format, searchQuery),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        val clearSearchDesc = stringResource(id = com.voicenotesai.R.string.clear_search) + " to see all notes"

        OutlinedButton(
            onClick = onClearSearch,
            modifier = Modifier.semantics {
                contentDescription = clearSearchDesc
            }
        ) {
            Text(stringResource(id = com.voicenotesai.R.string.clear_search))
        }
    }
}