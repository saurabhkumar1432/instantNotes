package com.voicenotesai.domain.usecase

import com.voicenotesai.data.repository.AudioRepository
import com.voicenotesai.data.repository.RecordingState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case that orchestrates the voice recording flow.
 * Returns a Flow of recording states to provide real-time updates to the UI.
 */
class RecordVoiceUseCase @Inject constructor(
    private val audioRepository: AudioRepository
) {
    /**
     * Starts the voice recording process and returns a Flow of recording states.
     * 
     * @return Flow<RecordingState> emitting states: Idle, Recording, Processing, Success, or Error
     */
    suspend operator fun invoke(): Flow<RecordingState> {
        return audioRepository.startRecording()
    }

    /**
     * Stops the current recording and returns the transcribed text.
     * 
     * @return Result<String> containing the transcribed text or an error
     */
    suspend fun stopRecording(): Result<String> {
        return audioRepository.stopRecording()
    }

    /**
     * Checks if the app has microphone permission.
     * 
     * @return true if permission is granted, false otherwise
     */
    fun hasPermission(): Boolean {
        return audioRepository.hasPermission()
    }

    /**
     * Checks if recording is currently in progress.
     * 
     * @return true if recording, false otherwise
     */
    fun isRecording(): Boolean {
        return audioRepository.isRecording()
    }

    /**
     * Cleans up audio resources. Should be called when the use case is no longer needed.
     */
    fun cleanup() {
        audioRepository.release()
    }
}
