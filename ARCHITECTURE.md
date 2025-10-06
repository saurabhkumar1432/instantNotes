# Voice Notes AI - Architecture Documentation

## Overview
Voice Notes AI is an Android application that converts voice recordings into structured, AI-generated notes using multiple AI providers (OpenAI, Anthropic Claude, Google AI).

## Architecture Pattern
The application follows **Clean Architecture** principles with **MVVM (Model-View-ViewModel)** pattern for the presentation layer.

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   Compose    │──│  ViewModel   │──│  Use Cases   │     │
│  │   UI Screens │  │              │  │              │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────┴────────────────────────────────┐
│                       Domain Layer                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  Use Cases   │  │    Models    │  │  Repository  │     │
│  │              │  │              │  │  Interfaces  │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────┴────────────────────────────────┐
│                        Data Layer                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Repositories │  │ Local (Room) │  │ Remote (API) │     │
│  │              │  │   DataStore  │  │   Retrofit   │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

## Layer Responsibilities

### 1. Presentation Layer (`presentation/`)
**Technology**: Jetpack Compose, Material3, ViewModels

**Responsibilities**:
- Render UI using Compose
- Handle user interactions
- Observe ViewModel state
- Display errors and loading states
- Navigate between screens

**Key Components**:
- `MainScreen` - Voice recording interface
- `SettingsScreen` - AI provider configuration
- `NotesScreen` - List of saved notes
- `NoteDetailScreen` - Individual note viewer
- `MainViewModel` - Manages recording and note generation flow
- `SettingsViewModel` - Manages AI settings with validation
- `NotesViewModel` - Manages notes list

**State Management**:
- Uses `StateFlow` for reactive state updates
- SavedStateHandle for configuration change persistence
- Unidirectional data flow (UDF)

### 2. Domain Layer (`domain/`)
**Responsibilities**:
- Define business logic
- Define repository interfaces
- Define error models
- Provide use cases for specific operations

**Key Components**:
- `GenerateNotesUseCase` - Orchestrates AI note generation
- `RecordVoiceUseCase` - Handles voice recording logic
- `AppError` - Sealed class for all error types
- Repository interfaces (AIRepository, NotesRepository, etc.)

**Design Patterns**:
- Use Case pattern for business logic encapsulation
- Repository pattern for data abstraction
- Sealed classes for type-safe error handling

### 3. Data Layer (`data/`)
**Responsibilities**:
- Implement repository interfaces
- Handle local database operations (Room)
- Make network API calls (Retrofit)
- Persist settings (DataStore)
- Manage encryption (Security Crypto)

**Sub-layers**:

#### Local Storage
- **Room Database**: Stores notes with timestamps
- **DataStore**: Encrypted storage for API keys and settings
- **Security Crypto**: Encrypts sensitive data

#### Remote APIs
- **Retrofit Services**: OpenAI, Anthropic, Google AI
- **OkHttp**: Network interceptor, timeouts
- **Gson**: JSON serialization

#### Repositories
- `AIRepositoryImpl` - AI API integration with retry logic
- `AudioRepositoryImpl` - SpeechRecognizer integration
- `NotesRepositoryImpl` - Note CRUD operations
- `SettingsRepositoryImpl` - Settings persistence

## Dependency Injection
**Framework**: Hilt (Dagger)

**Modules**:
- `DatabaseModule` - Provides Room database and DAOs
- `NetworkModule` - Provides Retrofit services, OkHttp client
- `RepositoryModule` - Binds repository implementations
- `DispatcherModule` - Provides coroutine dispatchers
- `DataStoreModule` - Provides DataStore instance

**Scopes**:
- `@Singleton` - Application-wide instances (Database, Network, Repositories)
- `@ViewModelScoped` - ViewModel-specific instances (Use Cases)

## Threading Model
**Framework**: Kotlin Coroutines

**Dispatchers**:
- `IoDispatcher` - Network and disk operations (AI calls, DB queries)
- `DefaultDispatcher` - CPU-intensive work (JSON parsing)
- `MainDispatcher` - UI updates (StateFlow emissions)

**Concurrency Patterns**:
- `Mutex` - Request deduplication, rate limiting
- `@Volatile` - Thread-safe state in AudioRepository
- `Flow` - Reactive data streams for database queries
- `StateFlow` - UI state management

## Key Features Implementation

### 1. Voice Recording
**Flow**:
1. User grants microphone permission
2. `MainViewModel` calls `RecordVoiceUseCase`
3. `AudioRepositoryImpl` uses Android's `SpeechRecognizer`
4. Partial results update UI in real-time
5. Final transcription returned to ViewModel

**Tech Stack**:
- `SpeechRecognizer` API (Android built-in)
- `callbackFlow` for converting callbacks to Flow
- `MutableStateFlow` for stop signaling

### 2. AI Note Generation
**Flow**:
1. User stops recording
2. Transcribed text passed to `GenerateNotesUseCase`
3. Input validation (max 40K characters)
4. Retrieve settings from DataStore
5. `AIRepositoryImpl` calls appropriate provider
6. Exponential backoff retry on failures
7. Generated notes saved to Room database

**Features**:
- **Request Deduplication**: Prevents duplicate API calls using Mutex
- **Rate Limiting**: Enforces 1-second minimum between requests
- **Retry Logic**: 3 retries with exponential backoff for 5xx errors
- **Custom Prompts**: User-configurable prompt templates
- **Multi-provider**: Supports OpenAI, Anthropic, Google AI

### 3. Settings Validation
**Flow**:
1. User enters API key and model
2. `SettingsViewModel` validates format
3. Makes test API call (10-20 tokens)
4. Only saves if validation succeeds
5. Blocks app usage until validated

**Validation**:
- Format validation (provider-specific)
- Live API test with minimal token usage
- Validation status persisted in DataStore

### 4. Note Management
**Flow**:
1. Notes stored in Room database
2. `NotesViewModel` observes Flow from DAO
3. Automatic UI updates on data changes
4. Delete with confirmation dialog
5. Share via Android intent

## Error Handling Strategy

### Error Types (Sealed Class)
```kotlin
sealed class AppError {
    data class NetworkError(message: String)
    data class ApiError(message: String)
    data class RecordingError(errorCode: Int)
    data class StorageError(message: String)
    data class NoSpeechDetected
    data class PermissionDenied
}
```

### Error Flow
1. Repository returns `Result<T>` (success or failure)
2. Use Case propagates or transforms error
3. ViewModel maps to AppError
4. UI displays user-friendly message
5. Provides action guidance (retry, settings, etc.)

### Error Extensions
- `toUserMessage()` - Human-readable error text
- `getActionGuidance()` - Suggested user action
- `canRetry()` - Whether retry is possible
- `shouldNavigateToSettings()` - Navigate to settings

## Security Considerations

### API Key Storage
- Encrypted using `EncryptedSharedPreferences`
- Android Security Crypto library
- Never logged or exposed in UI

### Network Security
- HTTPS only for all API calls
- Certificate pinning (planned for production)
- Logging disabled in release builds
- API keys passed in headers only

### ProGuard Rules
- Comprehensive rules for:
  - Hilt/Dagger
  - Retrofit/OkHttp
  - Room/DataStore
  - Jetpack Compose
  - Gson serialization

## Testing Strategy

### Unit Tests (Planned)
- Repository implementations with MockWebServer
- Use Cases with mocked repositories
- ViewModel logic with TestDispatcher
- Error handling and retry logic

### Integration Tests (Planned)
- Room database migrations
- DataStore encryption/decryption
- End-to-end recording → AI → save flow

### UI Tests (Planned)
- Compose UI testing framework
- Permission flow
- Recording flow
- Settings validation flow

## Performance Optimizations

### Memory Management
- Proper SpeechRecognizer cleanup
- Flow cancellation handling
- ViewModel scoping (cleared on navigation)

### Network Optimization
- Request deduplication
- Rate limiting
- Connection pooling (OkHttp)
- Timeout configuration (30s)

### Database Optimization
- Flow-based queries (reactive)
- Indexed timestamp column
- Efficient DELETE operations

### UI Performance
- LazyColumn for notes list
- State hoisting
- Minimal recompositions
- Debounced state updates

## Build Configuration

### Gradle Modules
- `app` - Main application module
- Potential future modules: `core`, `data`, `domain`, `presentation`

### Build Types
- `debug` - Development with logging, no minification
- `release` - Production with ProGuard, optimizations

### Dependencies
- Jetpack Compose BOM 2023.10.01
- Hilt 2.48
- Room 2.6.1
- Retrofit 2.9.0
- Kotlin Coroutines 1.7.3

## Future Enhancements

### Architecture Improvements
1. **Multi-module Architecture**: Split into feature modules
2. **Coordinator Pattern**: Separate MainViewModel responsibilities
3. **Offline Support**: Queue AI requests, sync when online
4. **Paging3**: Paginate notes list for large datasets

### Features
1. **Search**: Full-text search across notes
2. **Export**: PDF, Markdown, TXT export
3. **Batch Operations**: Multi-select, bulk delete/export
4. **Analytics**: Firebase Analytics for usage tracking
5. **Dark Theme**: Complete dark mode support

### Infrastructure
1. **CI/CD**: Automated builds and tests
2. **Crashlytics**: Production crash reporting
3. **Performance Monitoring**: Firebase Performance
4. **A/B Testing**: Feature flags

## References
- [Android Architecture Guide](https://developer.android.com/topic/architecture)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
