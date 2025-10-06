# Voice Notes AI

An Android application that converts voice input into AI-generated bullet-point notes.

## Features

- Voice recording with speech-to-text conversion
- AI-powered note generation (OpenAI, Anthropic, Google AI)
- Local notes storage and management
- Secure API key storage
- Material Design 3 UI with Jetpack Compose

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose with Material 3
- **Architecture:** MVVM with Repository pattern
- **Dependency Injection:** Hilt
- **Database:** Room
- **Settings Storage:** DataStore (encrypted)
- **Networking:** Retrofit + OkHttp
- **Async:** Coroutines + Flow

## Project Structure

```
app/src/main/java/com/voicenotesai/
├── data/           # Data layer (repositories, database, API)
├── domain/         # Domain layer (use cases, business logic)
└── presentation/   # Presentation layer (UI, ViewModels)
    └── theme/      # Compose theme configuration
```

## Setup

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle dependencies
4. Run the app on an emulator or physical device

## Requirements

- Android Studio Hedgehog or later
- Android SDK 26 (Android 8.0) or higher
- Kotlin 1.9.20

## Configuration

Configure your AI provider settings in the app:
- Select AI provider (OpenAI, Anthropic, or Google AI)
- Enter your API key
- Specify the model name

## Permissions

- **RECORD_AUDIO:** Required for voice recording
- **INTERNET:** Required for AI API calls

## License

This project is for educational purposes.
