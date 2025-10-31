package com.voicenotesai.presentation.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.voicenotesai.domain.model.TaskFilter
import com.voicenotesai.presentation.components.GradientHeader
import com.voicenotesai.presentation.components.TaskCard
import com.voicenotesai.presentation.theme.ModernSpacing

/**
 * Tasks screen showing all tasks with filtering and management capabilities.
 * Features gradient header, filter chips, task list, and floating action button for task creation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val showAddTaskDialog by viewModel.showAddTaskDialog.collectAsState()
    val newTaskText by viewModel.newTaskText.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show error messages in snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddTaskDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add task",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Gradient Header
            GradientHeader(
                title = "Tasks",
                actions = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
            
            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ModernSpacing.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(ModernSpacing.small),
                contentPadding = PaddingValues(vertical = ModernSpacing.componentGap)
            ) {
                items(TaskFilter.values()) { filter ->
                    val filterCounts = viewModel.getFilterCounts()
                    val count = filterCounts.getCount(filter)
                    
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = {
                            Text(
                                text = "${filter.displayName} ($count)",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }
            
            // Tasks List
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = ModernSpacing.screenPadding)
            ) {
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    
                    uiState.tasks.isEmpty() -> {
                        EmptyTasksState(
                            filter = selectedFilter,
                            onAddTask = { viewModel.showAddTaskDialog() },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(ModernSpacing.componentGap),
                            contentPadding = PaddingValues(vertical = ModernSpacing.componentGap)
                        ) {
                            items(
                                items = uiState.tasks,
                                key = { it.task.id }
                            ) { taskWithNote ->
                                TaskCard(
                                    task = taskWithNote.task,
                                    sourceNote = taskWithNote.sourceNote,
                                    onToggleComplete = { taskId ->
                                        viewModel.toggleTaskComplete(taskId)
                                    },
                                    onDelete = { taskId ->
                                        viewModel.deleteTask(taskId)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Add Task Dialog
    if (showAddTaskDialog) {
        AddTaskDialog(
            taskText = newTaskText,
            onTaskTextChange = { viewModel.updateNewTaskText(it) },
            onConfirm = { viewModel.createTask() },
            onDismiss = { viewModel.hideAddTaskDialog() }
        )
    }
}

/**
 * Empty state component shown when there are no tasks.
 */
@Composable
private fun EmptyTasksState(
    filter: TaskFilter,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(ModernSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when (filter) {
                TaskFilter.ALL -> "No tasks yet"
                TaskFilter.PENDING -> "No pending tasks"
                TaskFilter.COMPLETED -> "No completed tasks"
            },
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(ModernSpacing.small))
        
        Text(
            text = when (filter) {
                TaskFilter.ALL -> "Create your first task or record a note with action items"
                TaskFilter.PENDING -> "All tasks are completed! Great job!"
                TaskFilter.COMPLETED -> "Complete some tasks to see them here"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (filter == TaskFilter.ALL) {
            Spacer(modifier = Modifier.height(ModernSpacing.medium))
            
            Button(
                onClick = onAddTask,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Add Task")
            }
        }
    }
}

/**
 * Dialog for adding a new manual task.
 */
@Composable
private fun AddTaskDialog(
    taskText: String,
    onTaskTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add New Task",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter a description for your task:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(ModernSpacing.componentGap))
                
                OutlinedTextField(
                    value = taskText,
                    onValueChange = onTaskTextChange,
                    placeholder = {
                        Text("e.g., Call the dentist to schedule appointment")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = taskText.trim().isNotBlank()
            ) {
                Text("Add Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}