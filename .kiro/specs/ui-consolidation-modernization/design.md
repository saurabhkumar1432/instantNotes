# Design Document

## Overview

This design document outlines the technical approach for consolidating and modernizing the Voice Notes AI application's user interface. The design focuses on eliminating duplicate components, implementing modern Material You design patterns, and creating a cohesive user experience that aligns with the mobile mockup designs while maintaining excellent performance and accessibility.

## Architecture

### High-Level Architecture

The modernized application follows a clean, consolidated architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
├─────────────────────────────────────────────────────────────┤
│  Unified Components │  Single ViewModels │  Modern Theming  │
├─────────────────────────────────────────────────────────────┤
│                     Domain Layer                            │
├─────────────────────────────────────────────────────────────┤
│  Task Management │  AI Processing │  Reminder System       │
├─────────────────────────────────────────────────────────────┤
│                      Data Layer                             │
├─────────────────────────────────────────────────────────────┤
│  Consolidated Repos │  Local Storage │  Multi-Provider AI  │
├─────────────────────────────────────────────────────────────┤
│                 Infrastructure Layer                        │
├─────────────────────────────────────────────────────────────┤
│  Network │  Database │  Notifications │  Widget Support    │
└─────────────────────────────────────────────────────────────┘
```

### Core Design Principles

1. **Component Consolidation**: Single source of truth for each UI component
2. **Modern Material You**: Gradient headers, dynamic theming, consistent spacing
3. **Task-Centric Design**: Built-in task management and reminder system
4. **Multi-Provider AI**: Flexible AI configuration supporting local and cloud models
5. **Offline-First**: Full functionality without internet connection
6. **Performance Optimized**: 60fps animations, efficient memory usage

## Components and Interfaces

### 1. Consolidated UI Component System

#### Core Components (Keep Only These)
```kotlin
// Primary components to maintain
interface GradientHeader {
    fun render(
        title: String,
        showUserAvatar: Boolean = false,
        userInitials: String = "JD",
        showSearch: Boolean = false,
        searchQuery: String = "",
        onSearchQueryChange: (String) -> Unit = {},
        actions: @Composable () -> Unit = {}
    )
}

interface StatsCard {
    fun render(
        value: String,
        label: String,
        valueColor: Color = MaterialTheme.colorScheme.primary,
        modifier: Modifier = Modifier
    )
}

interface NoteCard {
    fun render(
        title: String,
        preview: String,
        duration: String,
        tags: List<String>,
        hasActionItems: Boolean = false,
        onClick: () -> Unit,
        onMoreClick: () -> Unit,
        modifier: Modifier = Modifier
    )
}

interface WaveformVisualizer {
    fun render(
        isActive: Boolean,
        barCount: Int = 20,
        audioLevels: List<Float> = emptyList(),
        modifier: Modifier = Modifier
    )
}

interface SettingItem {
    fun render(
        icon: ImageVector,
        label: String,
        value: String? = null,
        showToggle: Boolean = false,
        toggleValue: Boolean = false,
        onToggleChange: (Boolean) -> Unit = {},
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    )
}
```

#### Components to Remove
- ModernSettingsScreen.kt
- EnhancedSettingsScreen.kt
- TypographyDemoScreen.kt
- AudioVisualizationDemoScreen.kt
- EnhancedNotesViewModel.kt
- OptimizedNotesViewModel.kt

### 2. Modern Visual Design System

#### Material You Theme Implementation
```kotlin
data class ModernColorScheme(
    val primary: Color,           // #6366F1 (Indigo)
    val tertiary: Color,          // #8B5CF6 (Purple) 
    val secondary: Color,         // #10B981 (Green)
    val surface: Color,           // Card backgrounds
    val background: Color,        // Screen backgrounds
    val outline: Color,           // Border colors (20% opacity)
    val onSurface: Color,         // Primary text
    val onSurfaceVariant: Color,  // Secondary text
    val onPrimary: Color          // Text on gradients
)

object ModernSpacing {
    val screenPadding = 16.dp
    val sectionSpacing = 16.dp
    val componentGap = 12.dp
    val cardPadding = 16.dp
    val smallCardPadding = 12.dp
}

object ModernShapes {
    val cardCorners = RoundedCornerShape(12.dp)
    val chipCorners = RoundedCornerShape(4.dp)
    val borderWidth = 1.dp
}
```

#### Gradient System
```kotlin
object GradientSystem {
    fun headerGradient(colorScheme: ColorScheme) = Brush.horizontalGradient(
        colors = listOf(
            colorScheme.primary,
            colorScheme.tertiary
        )
    )
    
    fun waveformGradient(colorScheme: ColorScheme) = Brush.verticalGradient(
        colors = listOf(
            colorScheme.primary,
            colorScheme.tertiary
        )
    )
}
```

### 3. Task Management System

#### Task Data Models
```kotlin
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isCompleted: Boolean = false,
    val sourceNoteId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val dueDate: Long? = null,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val category: String? = null
)

enum class TaskPriority {
    LOW, NORMAL, HIGH, URGENT
}

data class TaskWithNote(
    @Embedded val task: Task,
    @Relation(
        parentColumn = "sourceNoteId",
        entityColumn = "id"
    )
    val sourceNote: Note?
)
```

#### Task Management Interface
```kotlin
interface TaskManager {
    suspend fun extractTasksFromNote(noteId: String, content: String): List<Task>
    suspend fun createManualTask(text: String, dueDate: Long? = null): Task
    suspend fun markTaskComplete(taskId: String): Result<Unit>
    suspend fun markTaskIncomplete(taskId: String): Result<Unit>
    suspend fun deleteTask(taskId: String): Result<Unit>
    suspend fun getTasksByStatus(completed: Boolean): Flow<List<TaskWithNote>>
    suspend fun getPendingTasksCount(): Flow<Int>
}
```

### 4. Multi-Provider AI Configuration

#### AI Provider Models
```kotlin
sealed class AIProvider {
    object OpenAI : AIProvider()
    object Anthropic : AIProvider()
    object GoogleAI : AIProvider()
    object OpenRouter : AIProvider()
    object Ollama : AIProvider()
    object LMStudio : AIProvider()
    data class Custom(val name: String) : AIProvider()
}

data class AIConfiguration(
    val provider: AIProvider,
    val apiKey: String? = null,
    val baseUrl: String? = null,
    val modelName: String,
    val customHeaders: Map<String, String> = emptyMap(),
    val isValidated: Boolean = false,
    val lastValidated: Long? = null
)

data class AIModel(
    val id: String,
    val name: String,
    val provider: AIProvider,
    val capabilities: Set<AICapability>
)

enum class AICapability {
    TEXT_GENERATION,
    TASK_EXTRACTION,
    SUMMARIZATION,
    CATEGORIZATION
}
```

#### AI Configuration Manager
```kotlin
interface AIConfigurationManager {
    suspend fun saveConfiguration(config: AIConfiguration): Result<Unit>
    suspend fun getCurrentConfiguration(): AIConfiguration?
    suspend fun validateConfiguration(config: AIConfiguration): ValidationResult
    suspend fun getAvailableModels(provider: AIProvider): List<AIModel>
    suspend fun testConnection(config: AIConfiguration): ConnectionResult
}

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val supportedCapabilities: Set<AICapability> = emptySet()
)
```

### 5. Reminder and Notification System

#### Reminder Models
```kotlin
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val triggerTime: Long,
    val sourceNoteId: String? = null,
    val sourceTaskId: String? = null,
    val isCompleted: Boolean = false,
    val reminderType: ReminderType = ReminderType.ONE_TIME,
    val repeatInterval: Long? = null
)

enum class ReminderType {
    ONE_TIME,
    DAILY,
    WEEKLY,
    MONTHLY
}
```

#### Notification Manager
```kotlin
interface NotificationManager {
    suspend fun scheduleReminder(reminder: Reminder): Result<Unit>
    suspend fun cancelReminder(reminderId: String): Result<Unit>
    suspend fun createReminderFromNote(noteId: String, triggerTime: Long): Result<Reminder>
    suspend fun createReminderFromTask(taskId: String, triggerTime: Long): Result<Reminder>
    fun showQuickCaptureNotification()
    fun hideQuickCaptureNotification()
}
```

### 6. Screen Architecture

#### Home Screen Design
```kotlin
@Composable
fun ModernHomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToRecording: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToRecording,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Default.Add, "Start Recording")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues)
        ) {
            // Gradient Header with Search
            item {
                GradientHeader(
                    title = "Voice Notes",
                    showUserAvatar = true,
                    showSearch = true,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, "Settings")
                        }
                    }
                )
            }
            
            // Stats Grid
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatsCard(
                        value = uiState.totalNotes.toString(),
                        label = "Notes",
                        modifier = Modifier.weight(1f)
                    )
                    StatsCard(
                        value = uiState.pendingTasks.toString(),
                        label = "Tasks",
                        valueColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                    StatsCard(
                        value = uiState.thisWeekNotes.toString(),
                        label = "This Week",
                        valueColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Recent Notes
            item {
                Text(
                    text = "Recent Notes",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            items(uiState.filteredNotes(searchQuery)) { note ->
                NoteCard(
                    title = note.extractTitle(),
                    preview = note.content,
                    duration = note.formatDuration(),
                    tags = note.tags,
                    hasActionItems = note.hasActionItems,
                    onClick = { /* Navigate to detail */ },
                    onMoreClick = { /* Show menu */ },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}
```

#### Recording Screen Design
```kotlin
@Composable
fun ModernRecordingScreen(
    viewModel: RecordingViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GradientHeader(title = "New Recording")
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Timer Display
        Text(
            text = uiState.formattedDuration,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 60.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Waveform
        WaveformVisualizer(
            isActive = uiState.isRecording,
            audioLevels = uiState.audioLevels,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Recording Status
        if (uiState.isRecording) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PulsingDot()
                Text("Recording...")
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Stop Button
        Button(
            onClick = { viewModel.stopRecording() },
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Stop, "Stop Recording")
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}
```

#### Tasks Screen Design
```kotlin
@Composable
fun TasksScreen(
    viewModel: TasksViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedFilter by remember { mutableStateOf(TaskFilter.ALL) }
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddTaskDialog() }
            ) {
                Icon(Icons.Default.Add, "Add Task")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues)
        ) {
            item {
                GradientHeader(
                    title = "Tasks",
                    actions = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    }
                )
            }
            
            // Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskFilter.values().forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.displayName) }
                        )
                    }
                }
            }
            
            // Tasks List
            items(uiState.getFilteredTasks(selectedFilter)) { taskWithNote ->
                TaskCard(
                    task = taskWithNote.task,
                    sourceNote = taskWithNote.sourceNote,
                    onToggleComplete = { viewModel.toggleTaskComplete(it) },
                    onDelete = { viewModel.deleteTask(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}
```

## Data Models

### Enhanced Note Model
```kotlin
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val content: String,
    val transcribedText: String,
    val timestamp: Long,
    val lastModified: Long = timestamp,
    val duration: Long? = null,
    val tags: List<String> = emptyList(),
    val category: String? = null,
    val hasActionItems: Boolean = false,
    val isArchived: Boolean = false
) {
    fun extractTitle(): String = content.lines().firstOrNull()?.take(50) ?: "Untitled Note"
    
    fun formatDuration(): String {
        if (duration == null) return "0:00"
        val minutes = (duration / 60000).toInt()
        val seconds = ((duration % 60000) / 1000).toInt()
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}
```

### Home Screen State
```kotlin
data class HomeUiState(
    val notes: List<Note> = emptyList(),
    val totalNotes: Int = 0,
    val pendingTasks: Int = 0,
    val thisWeekNotes: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    fun filteredNotes(query: String): List<Note> {
        if (query.isBlank()) return notes.take(10)
        return notes.filter { 
            it.content.contains(query, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }.take(10)
    }
}
```

## Error Handling

### Consolidated Error System
```kotlin
sealed class AppError {
    data class NetworkError(val message: String) : AppError()
    data class AIProviderError(val provider: AIProvider, val message: String) : AppError()
    data class RecordingError(val message: String) : AppError()
    data class StorageError(val message: String) : AppError()
    data class ValidationError(val field: String, val message: String) : AppError()
    data class TaskError(val message: String) : AppError()
    data class ReminderError(val message: String) : AppError()
}

interface ErrorHandler {
    fun handleError(error: AppError): UserMessage
    fun canRecover(error: AppError): Boolean
    suspend fun attemptRecovery(error: AppError): Result<Unit>
}
```

## Testing Strategy

### Component Testing
```kotlin
class GradientHeaderTest {
    @Test
    fun `displays title correctly`()
    
    @Test
    fun `shows search bar when enabled`()
    
    @Test
    fun `handles search query changes`()
}

class TaskManagerTest {
    @Test
    fun `extracts tasks from note content`()
    
    @Test
    fun `marks tasks as complete`()
    
    @Test
    fun `creates manual tasks`()
}

class AIConfigurationTest {
    @Test
    fun `validates OpenAI configuration`()
    
    @Test
    fun `validates local model configuration`()
    
    @Test
    fun `handles connection failures gracefully`()
}
```

### Integration Testing
- Home screen with real data
- Recording flow end-to-end
- Task creation and management
- AI provider switching
- Reminder scheduling

### Performance Testing
- 60fps animation verification
- Memory usage monitoring
- Large dataset handling (1000+ notes)
- Startup time optimization

This design provides a solid foundation for implementing the consolidated, modern Voice Notes AI application while maintaining excellent performance and user experience.