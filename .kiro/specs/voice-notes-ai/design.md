# Design Document

## Overview

This Android application is built using modern Android development practices with Kotlin, Jetpack Compose for UI, and follows the MVVM (Model-View-ViewModel) architecture pattern. The app consists of three main screens: a main recording screen, a notes history screen, and a settings screen. Notes are persisted locally and displayed in a list for easy access. The design emphasizes simplicity, security, and seamless user experience.

### Technology Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Architecture:** MVVM with Repository pattern
- **Dependency Injection:** Hilt
- **Async Operations:** Kotlin Coroutines + Flow
- **Storage:** 
  - Room Database for notes persistence
  - DataStore (encrypted) for settings
- **Speech Recognition:** Android SpeechRecognizer API
- **HTTP Client:** Retrofit + OkHttp for AI API calls
- **Permissions:** Accompanist Permissions library

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Presentation Layer                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐      │
│  │ MainActivity │  │ NotesScreen  │  │  SettingsScreen  │      │
│  │ (Compose UI) │  │ (Compose UI) │  │  (Compose UI)    │      │
│  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘      │
│         │                 │                    │                 │
│  ┌──────▼───────┐  ┌──────▼───────┐  ┌────────▼─────────┐      │
│  │ MainViewModel│  │NotesViewModel│  │ SettingsViewModel│      │
│  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘      │
└─────────┼──────────────────┼─────────────────────┼──────────────┘
          │                  │                     │
┌─────────▼──────────────────▼─────────────────────▼──────────────┐
│                     Domain Layer                                 │
│  ┌──────────────────┐  ┌──────────────────┐                     │
│  │ VoiceRecorder    │  │  AIService       │                     │
│  │ UseCase          │  │  UseCase         │                     │
│  └────────┬─────────┘  └────────┬─────────┘                     │
└───────────┼──────────────────────┼───────────────────────────────┘
            │                      │
┌───────────▼──────────────────────▼───────────────────────────────┐
│                      Data Layer                                   │
│  ┌──────────────────┐  ┌──────────────────┐  ┌────────────────┐ │
│  │ AudioRepository  │  │ SettingsRepository│  │NotesRepository │ │
│  └────────┬─────────┘  └────────┬─────────┘  └────────┬───────┘ │
│           │                     │                      │          │
│  ┌────────▼─────────┐  ┌────────▼─────────┐  ┌────────▼───────┐ │
│  │ SpeechRecognizer │  │ DataStore        │  │ Room Database  │ │
│  │ (Android API)    │  │ (Encrypted)      │  │                │ │
│  └──────────────────┘  └──────────────────┘  └────────────────┘ │
│                                                                   │
│  ┌──────────────────────────────────────┐                        │
│  │  AIApiService (Retrofit)             │                        │
│  │  - OpenAI API                        │                        │
│  │  - Anthropic API                     │                        │
│  │  - Google AI API                     │                        │
│  └──────────────────────────────────────┘                        │
└───────────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. Presentation Layer

#### MainActivity
- Single activity hosting Compose navigation
- Manages navigation between Main and Settings screens
- Handles system-level permissions

#### MainScreen (Composable)
```kotlin
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToNotes: () -> Unit
)
```
- Displays record button with animation
- Shows recording state (idle, recording, processing)
- Displays generated notes after recording
- Provides copy and share actions
- Shows error messages
- Provides navigation to notes history

#### NotesScreen (Composable)
```kotlin
@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    onNavigateBack: () -> Unit,
    onNoteClick: (Note) -> Unit
)
```
- Displays list of saved notes in reverse chronological order
- Shows note preview (timestamp, first few lines)
- Allows viewing full note details
- Provides delete action for individual notes
- Supports copy and share for each note
- Shows empty state when no notes exist

#### SettingsScreen (Composable)
```kotlin
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
)
```
- AI provider dropdown (OpenAI, Anthropic, Google AI)
- API key input field (masked)
- Model name input field
- Save button with validation

#### MainViewModel
```kotlin
class MainViewModel @Inject constructor(
    private val recordVoiceUseCase: RecordVoiceUseCase,
    private val generateNotesUseCase: GenerateNotesUseCase,
    private val notesRepository: NotesRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel()
```
- Manages UI state (RecordingState, NotesState)
- Handles recording lifecycle
- Triggers AI note generation
- Saves generated notes to database
- Exposes StateFlow for UI observation

#### NotesViewModel
```kotlin
class NotesViewModel @Inject constructor(
    private val notesRepository: NotesRepository
) : ViewModel()
```
- Loads and displays saved notes
- Handles note deletion
- Provides search/filter functionality
- Exposes Flow of notes list

#### SettingsViewModel
```kotlin
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel()
```
- Manages settings form state
- Validates and saves settings
- Loads existing settings

### 2. Domain Layer

#### RecordVoiceUseCase
```kotlin
class RecordVoiceUseCase @Inject constructor(
    private val audioRepository: AudioRepository
) {
    suspend operator fun invoke(): Flow<RecordingResult>
}
```
- Encapsulates voice recording logic
- Returns Flow of recording states and results

#### GenerateNotesUseCase
```kotlin
class GenerateNotesUseCase @Inject constructor(
    private val aiRepository: AIRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(transcribedText: String): Result<String>
}
```
- Retrieves AI settings
- Calls appropriate AI service
- Returns formatted bullet-point notes

### 3. Data Layer

#### AudioRepository
```kotlin
interface AudioRepository {
    suspend fun startRecording(): Flow<RecordingState>
    suspend fun stopRecording(): Result<String> // Returns transcribed text
    fun hasPermission(): Boolean
}

class AudioRepositoryImpl @Inject constructor(
    private val context: Context
) : AudioRepository
```
- Wraps Android SpeechRecognizer API
- Manages recording state
- Converts speech to text

#### SettingsRepository
```kotlin
interface SettingsRepository {
    suspend fun saveSettings(settings: AISettings)
    fun getSettings(): Flow<AISettings?>
    suspend fun hasValidSettings(): Boolean
}

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository
```
- Stores/retrieves settings using encrypted DataStore
- Validates settings completeness

#### NotesRepository
```kotlin
interface NotesRepository {
    suspend fun saveNote(note: Note): Long
    fun getAllNotes(): Flow<List<Note>>
    suspend fun getNoteById(id: Long): Note?
    suspend fun deleteNote(id: Long)
    suspend fun deleteAllNotes()
}

class NotesRepositoryImpl @Inject constructor(
    private val notesDao: NotesDao
) : NotesRepository
```
- Manages CRUD operations for notes
- Uses Room database for persistence
- Returns Flow for reactive updates

#### AIRepository
```kotlin
interface AIRepository {
    suspend fun generateNotes(
        provider: AIProvider,
        apiKey: String,
        model: String,
        transcribedText: String
    ): Result<String>
}

class AIRepositoryImpl @Inject constructor(
    private val openAIService: OpenAIService,
    private val anthropicService: AnthropicService,
    private val googleAIService: GoogleAIService
) : AIRepository
```
- Routes requests to appropriate AI service
- Handles API communication
- Formats prompts for bullet-point generation

#### AI Service Interfaces
```kotlin
interface OpenAIService {
    @POST("v1/chat/completions")
    suspend fun generateCompletion(@Body request: OpenAIRequest): OpenAIResponse
}

interface AnthropicService {
    @POST("v1/messages")
    suspend fun generateMessage(@Body request: AnthropicRequest): AnthropicResponse
}

interface GoogleAIService {
    @POST("v1/models/{model}:generateContent")
    suspend fun generateContent(@Path("model") model: String, @Body request: GoogleAIRequest): GoogleAIResponse
}
```

## Data Models

### Note Entity
```kotlin
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val transcribedText: String? = null
)

@Dao
interface NotesDao {
    @Insert
    suspend fun insert(note: Note): Long
    
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>
    
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?
    
    @Delete
    suspend fun delete(note: Note)
    
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM notes")
    suspend fun deleteAll()
}

@Database(entities = [Note::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notesDao(): NotesDao
}
```

### AISettings
```kotlin
data class AISettings(
    val provider: AIProvider,
    val apiKey: String,
    val model: String
)

enum class AIProvider {
    OPENAI,
    ANTHROPIC,
    GOOGLE_AI
}
```

### UI States
```kotlin
sealed class RecordingState {
    object Idle : RecordingState()
    data class Recording(val duration: Long) : RecordingState()
    data class Processing(val progress: String) : RecordingState()
    data class Success(val notes: String) : RecordingState()
    data class Error(val message: String) : RecordingState()
}

data class SettingsUiState(
    val provider: AIProvider = AIProvider.OPENAI,
    val apiKey: String = "",
    val model: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSaved: Boolean = false
)
```

### API Request/Response Models
```kotlin
// OpenAI
data class OpenAIRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.7
)

data class Message(
    val role: String,
    val content: String
)

// Similar models for Anthropic and Google AI
```

## Error Handling

### Error Categories

1. **Permission Errors**
   - User denies microphone permission
   - Action: Show dialog explaining need, provide button to open app settings

2. **Recording Errors**
   - SpeechRecognizer fails or unavailable
   - No speech detected
   - Action: Display user-friendly error message, allow retry

3. **Network Errors**
   - No internet connection
   - API timeout
   - Action: Show error with retry option

4. **API Errors**
   - Invalid API key (401)
   - Rate limit exceeded (429)
   - Invalid request (400)
   - Action: Display specific error message, guide user to check settings

5. **Configuration Errors**
   - Missing settings
   - Action: Prompt user to configure settings

### Error Handling Strategy

```kotlin
sealed class AppError {
    data class PermissionDenied(val permission: String) : AppError()
    data class RecordingFailed(val reason: String) : AppError()
    data class NetworkError(val message: String) : AppError()
    data class APIError(val code: Int, val message: String) : AppError()
    object SettingsNotConfigured : AppError()
}

fun AppError.toUserMessage(): String {
    return when (this) {
        is PermissionDenied -> "Microphone permission is required to record audio"
        is RecordingFailed -> "Recording failed: $reason"
        is NetworkError -> "Network error: $message. Please check your connection"
        is APIError -> when (code) {
            401 -> "Invalid API key. Please check your settings"
            429 -> "Rate limit exceeded. Please try again later"
            else -> "API error: $message"
        }
        is SettingsNotConfigured -> "Please configure AI settings before recording"
    }
}
```

## Testing Strategy

### Unit Tests

1. **ViewModels**
   - Test state transitions
   - Test error handling
   - Mock repositories

2. **Use Cases**
   - Test business logic
   - Test error scenarios
   - Mock repositories

3. **Repositories**
   - Test data operations
   - Test API calls with mock responses
   - Test DataStore operations

### Integration Tests

1. **Repository + DataStore**
   - Test settings persistence
   - Test encryption

2. **Repository + API Services**
   - Test API integration with mock server
   - Test different AI providers

### UI Tests

1. **Main Screen**
   - Test recording flow
   - Test permission handling
   - Test notes display

2. **Settings Screen**
   - Test form validation
   - Test settings save/load

### Test Tools
- JUnit 5 for unit tests
- MockK for mocking
- Turbine for Flow testing
- Compose Testing for UI tests

## Security Considerations

1. **API Key Storage**
   - Use EncryptedSharedPreferences or encrypted DataStore
   - Never log API keys
   - Clear keys on app uninstall

2. **Network Security**
   - Use HTTPS only
   - Implement certificate pinning for production
   - Add network security config

3. **Audio Privacy**
   - Delete audio files immediately after transcription
   - Don't cache raw audio
   - Only send transcribed text to AI services

4. **Permissions**
   - Request permissions at runtime
   - Explain permission usage clearly
   - Handle permission denial gracefully

## Performance Considerations

1. **Audio Processing**
   - Use background thread for recording
   - Stream audio data efficiently
   - Limit recording duration (5 minutes max)

2. **API Calls**
   - Implement timeout (30 seconds)
   - Show progress indicators
   - Cancel ongoing requests on screen exit

3. **UI Responsiveness**
   - Use Kotlin Coroutines for async operations
   - Debounce user inputs in settings
   - Lazy loading for notes display

## Navigation Structure

```
MainActivity
├── MainScreen (default)
│   ├── [Settings Icon] → SettingsScreen
│   └── [Notes Icon] → NotesScreen
├── NotesScreen
│   ├── [Back Button] → MainScreen
│   └── [Note Item Click] → NoteDetailScreen
├── NoteDetailScreen
│   └── [Back Button] → NotesScreen
└── SettingsScreen
    └── [Back Button] → MainScreen
```

Navigation using Compose Navigation with four destinations:
- MainScreen: Recording interface
- NotesScreen: List of saved notes
- NoteDetailScreen: Full note view with actions
- SettingsScreen: AI configuration
