# Integration Tests for Voice Notes AI

This directory contains comprehensive integration tests for the Voice Notes AI application, covering all major user workflows and system integrations as specified in the requirements.

## Overview

The integration tests validate end-to-end functionality across the entire application stack, ensuring that all components work together correctly. These tests complement unit tests by focusing on integration scenarios and cross-component interactions.

## Test Structure

### Test Classes

1. **RecordingToTaskWorkflowIntegrationTest.kt**
   - Tests complete user workflow from audio recording to task management
   - Validates AI processing pipeline and task extraction
   - Covers error handling and fallback scenarios

2. **AIProviderSwitchingIntegrationTest.kt**
   - Tests multi-provider AI configuration system
   - Validates provider switching and configuration persistence
   - Covers local AI integration (Ollama, LM Studio)

3. **OfflineFunctionalityIntegrationTest.kt**
   - Tests offline-first architecture and sync behavior
   - Validates offline recording and local storage
   - Covers conflict resolution and retry mechanisms

4. **ExportSharingIntegrationTest.kt**
   - Tests export functionality across multiple formats
   - Validates sharing integrations (email, cloud storage, calendar)
   - Covers bulk operations and template customization

5. **IntegrationTestSuite.kt**
   - Comprehensive test suite that runs all integration tests
   - Contains documentation and execution guidelines
   - Defines performance benchmarks and test data

6. **AndroidIntegrationTest.kt** (in androidTest directory)
   - Android-specific integration tests using Compose UI testing
   - Tests real Android components, navigation, and permissions
   - Validates UI integration and accessibility features

7. **IntegrationTestRunner.kt**
   - Validates test structure and coverage
   - Ensures proper test organization and documentation
   - Verifies requirement coverage completeness

## Test Coverage

### 1. Complete User Workflows (RecordingToTaskWorkflowIntegrationTest)

**Scenarios Tested:**
- ✅ End-to-end recording to task completion workflow
- ✅ AI processing failure handling with graceful degradation
- ✅ Basic transcription fallback when AI enhancement fails
- ✅ Task extraction from various content formats
- ✅ Task completion updates across the system

**Key Test Cases:**
```kotlin
@Test
fun `complete workflow - recording to task completion`()

@Test
fun `workflow handles AI processing failure gracefully`()

@Test
fun `task extraction works with various content formats`()
```

### 2. AI Provider Configuration (AIProviderSwitchingIntegrationTest)

**Scenarios Tested:**
- ✅ Switching between cloud providers (OpenAI, Anthropic, Google AI)
- ✅ Local AI provider setup (Ollama, LM Studio)
- ✅ Custom provider configuration with headers
- ✅ Configuration validation and error handling
- ✅ Persistence across app restarts
- ✅ Network connectivity issues during validation

**Key Test Cases:**
```kotlin
@Test
fun `switching from OpenAI to Anthropic persists configuration`()

@Test
fun `switching to local AI provider (Ollama) with custom endpoint`()

@Test
fun `custom provider configuration with headers`()
```

### 3. Offline Functionality (OfflineFunctionalityIntegrationTest)

**Scenarios Tested:**
- ✅ Offline recording and local storage
- ✅ Sync queue management and processing
- ✅ Local AI processing with Ollama
- ✅ Conflict resolution between local and remote changes
- ✅ Storage management with limited space
- ✅ Retry logic with exponential backoff

**Key Test Cases:**
```kotlin
@Test
fun `offline recording stores locally and queues for sync`()

@Test
fun `sync handles conflicts between local and remote changes`()

@Test
fun `local AI processing works offline with Ollama`()
```

### 4. Export and Sharing (ExportSharingIntegrationTest)

**Scenarios Tested:**
- ✅ Single note export to PDF, Word, Markdown, Plain Text
- ✅ Bulk export operations
- ✅ Custom template support
- ✅ Email sharing with attachments
- ✅ Cloud storage integration
- ✅ Calendar event creation from meeting notes
- ✅ Temporary sharing with expiration links

**Key Test Cases:**
```kotlin
@Test
fun `export single note to PDF format`()

@Test
fun `share note via email with attachments`()

@Test
fun `create calendar event from meeting note`()
```

## Running the Tests

### Prerequisites

1. **Dependencies**: All required testing dependencies are included in `build.gradle.kts`:
   - JUnit 4 for test framework
   - MockK for mocking
   - Kotlinx Coroutines Test for async testing
   - Turbine for Flow testing

2. **Test Environment**: Tests use mocking to isolate integration logic from external dependencies

### Execution Commands

#### Run All Integration Tests
```bash
./gradlew test --tests "com.voicenotesai.integration.*"
```

#### Run Individual Test Classes
```bash
# Recording workflow tests
./gradlew test --tests "com.voicenotesai.integration.RecordingToTaskWorkflowIntegrationTest"

# AI provider switching tests
./gradlew test --tests "com.voicenotesai.integration.AIProviderSwitchingIntegrationTest"

# Offline functionality tests
./gradlew test --tests "com.voicenotesai.integration.OfflineFunctionalityIntegrationTest"

# Export and sharing tests
./gradlew test --tests "com.voicenotesai.integration.ExportSharingIntegrationTest"
```

#### Run Test Suite
```bash
./gradlew test --tests "com.voicenotesai.integration.IntegrationTestSuite"
```

#### Run Android Integration Tests
```bash
./gradlew connectedAndroidTest --tests "com.voicenotesai.integration.AndroidIntegrationTest"
```

#### Validate Test Structure
```bash
./gradlew test --tests "com.voicenotesai.integration.IntegrationTestRunner"
```

## Test Architecture

### Mocking Strategy

All integration tests use MockK for mocking external dependencies:

```kotlin
@Before
fun setup() {
    // Mock repositories and managers
    notesRepository = mockk(relaxed = true)
    taskRepository = mockk(relaxed = true)
    aiConfigurationManager = mockk(relaxed = true)
    
    // Configure mock behaviors
    coEvery { notesRepository.insertNote(any()) } returns Unit
    coEvery { taskRepository.getTasksByNoteId(any()) } returns flowOf(emptyList())
}

@After
fun tearDown() {
    clearAllMocks()
}
```

### Async Testing

All async operations use `runTest` from kotlinx-coroutines-test:

```kotlin
@Test
fun `async integration test`() = runTest {
    // Test async operations
    val result = someAsyncOperation()
    assertTrue("Async operation should succeed", result.isSuccess)
}
```

### Flow Testing

Flow-based operations are tested using appropriate collection methods:

```kotlin
@Test
fun `flow integration test`() = runTest {
    // Mock flow data
    coEvery { repository.getDataFlow() } returns flowOf(testData)
    
    // Test flow operations
    val flow = repository.getDataFlow()
    // Collect and verify flow emissions
}
```

## Performance Benchmarks

Expected execution times for integration tests:

- **Recording Workflow Tests**: < 5 seconds
- **AI Provider Switching Tests**: < 3 seconds  
- **Offline Functionality Tests**: < 4 seconds
- **Export Sharing Tests**: < 6 seconds
- **Full Test Suite**: < 20 seconds

## Test Data

### Sample Data Constants

```kotlin
object TestData {
    const val SAMPLE_AUDIO_SIZE_BYTES = 1024 * 100 // 100KB
    const val SAMPLE_TRANSCRIPTION = "This is a sample transcription for testing purposes"
    const val SAMPLE_ENHANCED_CONTENT = "Enhanced content with AI processing"
    const val SAMPLE_DURATION_MS = 30000L // 30 seconds
    
    val SAMPLE_TAGS = listOf("test", "integration", "sample")
    val SAMPLE_CATEGORIES = listOf("Work", "Personal", "Meeting", "Idea")
}
```

### Mock Configurations

```kotlin
// AI Configuration
val testAIConfig = AIConfiguration(
    provider = AIProvider.OpenAI,
    apiKey = "test-api-key",
    modelName = "gpt-3.5-turbo",
    isValidated = true
)

// Sample Note
val testNote = EnhancedNote(
    id = "test-note-1",
    originalTranscription = "Test transcription",
    enhancedContent = "Enhanced test content",
    summary = "Test summary",
    keyPoints = listOf("Test point 1", "Test point 2"),
    actionItems = listOf("Test task 1", "Test task 2"),
    timestamp = System.currentTimeMillis(),
    duration = 30000L,
    tags = listOf("test"),
    category = "Test"
)
```

## Error Scenarios

### Tested Error Conditions

1. **Network Failures**
   - API connection timeouts
   - Invalid API keys
   - Service unavailability

2. **Storage Issues**
   - Insufficient disk space
   - File permission errors
   - Database corruption

3. **AI Processing Failures**
   - Model unavailability
   - Processing timeouts
   - Invalid responses

4. **Sync Conflicts**
   - Concurrent modifications
   - Network interruptions
   - Data inconsistencies

### Error Handling Validation

```kotlin
@Test
fun `handles network failure gracefully`() = runTest {
    // Mock network failure
    coEvery { apiService.processAudio(any()) } throws NetworkException("Connection failed")
    
    // Verify graceful handling
    val result = processAudioUseCase.invoke(audioData)
    assertTrue("Should handle network failure", result.isFailure)
    assertEquals("Should provide user-friendly message", "Network error occurred", result.errorMessage)
}
```

## Continuous Integration

### CI/CD Integration

These integration tests are designed to run in CI/CD pipelines:

1. **Fast Execution**: All tests complete within performance benchmarks
2. **Isolated Dependencies**: No external service dependencies
3. **Deterministic Results**: Consistent results across environments
4. **Comprehensive Coverage**: All major workflows validated

### GitHub Actions Example

```yaml
name: Integration Tests
on: [push, pull_request]

jobs:
  integration-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run Integration Tests
        run: ./gradlew test --tests "com.voicenotesai.integration.*"
```

## Troubleshooting

### Common Issues

1. **Mock Configuration Errors**
   ```kotlin
   // Ensure all mocks are properly configured
   coEvery { mockRepository.method(any()) } returns expectedResult
   ```

2. **Async Test Failures**
   ```kotlin
   // Use runTest for coroutine testing
   @Test
   fun `async test`() = runTest {
       // Test async operations
   }
   ```

3. **Flow Collection Issues**
   ```kotlin
   // Properly collect flows in tests
   val results = flow.toList()
   assertEquals(expectedSize, results.size)
   ```

### Debug Tips

1. **Enable Verbose Logging**
   ```bash
   ./gradlew test --tests "com.voicenotesai.integration.*" --info
   ```

2. **Run Single Test Method**
   ```bash
   ./gradlew test --tests "*.RecordingToTaskWorkflowIntegrationTest.complete workflow*"
   ```

3. **Check Mock Interactions**
   ```kotlin
   verify(exactly = 1) { mockRepository.method(any()) }
   ```

## Contributing

### Adding New Integration Tests

1. **Follow Naming Conventions**
   - Use descriptive test method names with backticks
   - Group related tests in the same class
   - Use `@Test` annotation for all test methods

2. **Maintain Test Structure**
   - Include `@Before` setup method
   - Include `@After` teardown method
   - Use proper mocking and isolation

3. **Document Test Purpose**
   - Add class-level documentation
   - Explain test scenarios and expected outcomes
   - Include requirements traceability

4. **Validate Test Coverage**
   - Ensure new tests cover both happy path and error scenarios
   - Update test suite documentation
   - Verify performance benchmarks are met

### Code Review Checklist

- [ ] Test methods have descriptive names
- [ ] Proper mocking and isolation used
- [ ] Both success and failure scenarios covered
- [ ] Async operations properly tested with `runTest`
- [ ] Mock interactions verified with `verify`
- [ ] Test documentation updated
- [ ] Performance benchmarks considered

## Conclusion

These integration tests provide comprehensive coverage of the Voice Notes AI application's major workflows and integrations. They ensure that all components work together correctly and that the system handles both normal operations and error conditions gracefully.

The tests are designed to be:
- **Fast**: Complete execution within performance benchmarks
- **Reliable**: Consistent results across different environments
- **Maintainable**: Clear structure and comprehensive documentation
- **Comprehensive**: Coverage of all major integration scenarios

Regular execution of these tests helps maintain system quality and prevents regressions as the application evolves.