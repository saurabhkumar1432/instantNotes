package com.voicenotesai.data.repository

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

class AudioRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioRepository {

    @Volatile
    private var speechRecognizer: SpeechRecognizer? = null
    @Volatile
    private var isCurrentlyRecording = false
    private var recordingStartTime: Long = 0
    private val maxRecordingDuration = 5 * 60 * 1000L // 5 minutes in milliseconds
    private var hasReceivedSpeech = false
    @Volatile
    private var stopRequested = false
    private val accumulatedText = StringBuilder() // Accumulate partial results

    override fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun isRecording(): Boolean = isCurrentlyRecording

    override suspend fun startRecording(): Flow<RecordingState> = callbackFlow {
        if (!hasPermission()) {
            trySend(RecordingState.Error("Microphone permission not granted"))
            close()
            return@callbackFlow
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(RecordingState.Error("Speech recognition is not available on this device"))
            close()
            return@callbackFlow
        }

        val producerScope = this
        var durationJob: Job? = null

        // Clean up any existing recognizer
        cleanup()
        stopRequested = false
        hasReceivedSpeech = false
        accumulatedText.clear()
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        
        val recognitionListener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isCurrentlyRecording = true
                recordingStartTime = System.currentTimeMillis()
                hasReceivedSpeech = false
                trySend(RecordingState.Recording(0))

                durationJob?.cancel()
                durationJob = producerScope.launch {
                    while (isCurrentlyRecording && !stopRequested) {
                        val duration = System.currentTimeMillis() - recordingStartTime

                        // Check for max duration
                        if (duration >= maxRecordingDuration) {
                            try {
                                speechRecognizer?.stopListening()
                                trySend(RecordingState.Error("Maximum recording duration of 5 minutes reached"))
                            } catch (e: Exception) {
                                // Ignore
                            }
                            break
                        }

                        trySend(RecordingState.Recording(duration))
                        kotlinx.coroutines.delay(1000)
                    }
                }
            }

            override fun onBeginningOfSpeech() {
                // Speech input has begun
                hasReceivedSpeech = true
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Intentionally empty - we use a separate coroutine for duration updates
                // to avoid glitching from the high frequency of this callback
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Audio buffer received
            }

            override fun onEndOfSpeech() {
                isCurrentlyRecording = false
                durationJob?.cancel()
                durationJob = null
                trySend(RecordingState.Processing)
            }

            override fun onError(error: Int) {
                isCurrentlyRecording = false
                durationJob?.cancel()
                durationJob = null
                val hadSpeech = hasReceivedSpeech
                hasReceivedSpeech = false
                
                // Map error codes to user-friendly messages with context
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check your microphone and try again."
                    SpeechRecognizer.ERROR_CLIENT -> "Recording was cancelled. Please try again."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required to record audio."
                    SpeechRecognizer.ERROR_NETWORK -> "Network error. Speech recognition requires an active internet connection."
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout. Please check your internet connection and try again."
                    SpeechRecognizer.ERROR_NO_MATCH -> {
                        if (hadSpeech) {
                            "Speech was detected but couldn't be understood clearly. Please speak clearly and try again."
                        } else {
                            "No speech detected. Please ensure you're speaking clearly into the microphone and try again."
                        }
                    }
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognition service is currently busy. Please wait a moment and try again."
                    SpeechRecognizer.ERROR_SERVER -> "Speech recognition server error. Please try again in a moment."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        if (hadSpeech) {
                            "Speech input timed out after detecting initial speech. Please try again."
                        } else {
                            "No speech detected within the time limit. Please start speaking immediately after tapping the microphone."
                        }
                    }
                    else -> "An unexpected error occurred (Error code: $error). Please restart the app and try again."
                }
                
                // CRITICAL: Always send error and close, even if send fails
                try {
                    trySend(RecordingState.Error(errorMessage))
                } finally {
                    // Close the flow regardless of whether send succeeded
                    close()
                }
            }

            override fun onResults(results: Bundle?) {
                isCurrentlyRecording = false
                durationJob?.cancel()
                durationJob = null
                hasReceivedSpeech = false
                
                // CRITICAL: Use try-finally to ensure Flow always closes
                try {
                    // Defensive null checks and validation
                    if (results == null) {
                        trySend(RecordingState.Error("Recording failed. No results received. Please try again."))
                        return
                    }
                    
                    val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (matches.isNullOrEmpty()) {
                        trySend(RecordingState.Error("No speech detected. Please ensure you're speaking clearly and try again."))
                        return
                    }
                    
                    // Get first match with null safety
                    val transcribedText = matches.firstOrNull()?.trim() ?: ""
                    
                    if (transcribedText.isBlank()) {
                        trySend(RecordingState.Error("No speech detected. Please speak clearly into the microphone and try again."))
                    } else {
                        // Success - we have valid transcribed text
                        trySend(RecordingState.Success(transcribedText))
                    }
                } catch (e: Exception) {
                    // Handle any unexpected errors in result processing
                    trySend(RecordingState.Error("Failed to process recording results. Please try again."))
                } finally {
                    // CRITICAL: Always close the flow, even if trySend failed
                    close()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Keep recording alive by handling partial results
                // This helps prevent early termination on pauses
                hasReceivedSpeech = true
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Reserved for future events
            }
        }

        speechRecognizer?.setRecognitionListener(recognitionListener)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) // Enable to keep recognition alive
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Realistic timeouts that Android actually respects (5 seconds is practical limit)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 0L)
            // Prefer online for better recognition quality
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isCurrentlyRecording = false
            cleanup()
            trySend(RecordingState.Error("Failed to start recording: ${e.message}"))
            close()
            return@callbackFlow
        }

        awaitClose {
            // Cancel duration updates first
            durationJob?.cancel()
            durationJob = null
            // This is called when Flow is cancelled (e.g., navigation away, config change)
            cleanup()
        }
    }

    override suspend fun stopRecording(): Result<String> {
        return try {
            if (!isCurrentlyRecording) {
                Result.failure(IllegalStateException("Not currently recording"))
            } else {
                // Set flag first to prevent error callbacks
                stopRequested = true
                
                // Give the recognizer a moment to finish processing
                kotlinx.coroutines.delay(100)
                
                // Now stop the recognizer
                try {
                    speechRecognizer?.stopListening()
                } catch (e: Exception) {
                    // If stopListening fails, cancel instead
                    speechRecognizer?.cancel()
                }
                
                Result.success("Stop recording triggered")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanup() {
        try {
            if (isCurrentlyRecording) {
                speechRecognizer?.cancel()
            }
            speechRecognizer?.destroy()
            stopRequested = false
        } catch (e: Exception) {
            // Ignore cleanup errors
        } finally {
            speechRecognizer = null
            isCurrentlyRecording = false
            hasReceivedSpeech = false
            recordingStartTime = 0
            accumulatedText.clear()
        }
    }

    /**
     * Cleans up all resources. Should be called when the repository is no longer needed.
     * Note: SpeechRecognizer API processes audio in memory and doesn't create temporary files.
     * This method ensures proper cleanup of the recognizer instance.
     */
    override fun release() {
        cleanup()
    }
}
