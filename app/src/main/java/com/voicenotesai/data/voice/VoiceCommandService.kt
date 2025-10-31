package com.voicenotesai.data.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.voicenotesai.domain.model.VoiceCommand
import com.voicenotesai.domain.model.VoiceCommandResult
import com.voicenotesai.domain.usecase.VoiceCommandUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for handling voice command recognition and processing.
 */
@Singleton
class VoiceCommandService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val voiceCommandUseCase: VoiceCommandUseCase
) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private val commandResultChannel = Channel<VoiceCommandResult>(Channel.BUFFERED)
    private val recognizedCommandChannel = Channel<VoiceCommand>(Channel.BUFFERED)
    
    val commandResults: Flow<VoiceCommandResult> = commandResultChannel.receiveAsFlow()
    val recognizedCommands: Flow<VoiceCommand> = recognizedCommandChannel.receiveAsFlow()
    
    private var isListening = false
    
    /**
     * Starts listening for voice commands.
     */
    fun startListening() {
        if (isListening) return
        
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            commandResultChannel.trySend(VoiceCommandResult.Error("Speech recognition not available"))
            return
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createRecognitionListener())
        }
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
        
        speechRecognizer?.startListening(intent)
        isListening = true
    }
    
    /**
     * Stops listening for voice commands.
     */
    fun stopListening() {
        if (!isListening) return
        
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }
    
    /**
     * Processes a text phrase as if it were a voice command.
     */
    suspend fun processTextCommand(phrase: String): VoiceCommandResult {
        val result = voiceCommandUseCase.processCommand(phrase)
        
        // If command was recognized, also emit it to the recognized commands flow
        val command = VoiceCommand.fromPhrase(phrase)
        if (command != null && result is VoiceCommandResult.Success) {
            recognizedCommandChannel.trySend(command)
        }
        
        return result
    }
    
    /**
     * Gets all available voice commands.
     */
    fun getAvailableCommands(): Map<VoiceCommand, List<String>> {
        return voiceCommandUseCase.getAvailableCommands()
    }
    
    /**
     * Checks if voice recognition is available on the device.
     */
    fun isVoiceRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }
    
    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                // Speech recognizer is ready
            }
            
            override fun onBeginningOfSpeech() {
                // User started speaking
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Audio level changed
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Audio buffer received
            }
            
            override fun onEndOfSpeech() {
                // User stopped speaking
            }
            
            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech input matched"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error"
                }
                
                commandResultChannel.trySend(VoiceCommandResult.Error(errorMessage))
                isListening = false
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    processRecognizedSpeech(matches)
                }
                isListening = false
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                // Handle partial results if needed
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                // Handle other events
            }
        }
    }
    
    private fun processRecognizedSpeech(matches: List<String>) {
        for (match in matches) {
            val command = VoiceCommand.fromPhrase(match)
            if (command != null) {
                recognizedCommandChannel.trySend(command)
                commandResultChannel.trySend(VoiceCommandResult.Success)
                return
            }
        }
        
        // No command recognized
        commandResultChannel.trySend(VoiceCommandResult.NotRecognized)
    }
    
    fun cleanup() {
        stopListening()
        commandResultChannel.close()
        recognizedCommandChannel.close()
    }
}