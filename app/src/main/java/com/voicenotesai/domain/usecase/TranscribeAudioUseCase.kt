package com.voicenotesai.domain.usecase

import com.voicenotesai.domain.ai.AIProcessingEngine
import com.voicenotesai.domain.ai.TranscriptionConfig
import com.voicenotesai.domain.ai.TranscriptionResult
import com.voicenotesai.domain.model.AudioData
import javax.inject.Inject

/**
 * Use case for transcribing audio to text using AI processing engine.
 */
class TranscribeAudioUseCase @Inject constructor(
    private val aiProcessingEngine: AIProcessingEngine
) {
    
    /**
     * Transcribes the given audio data using the specified configuration.
     */
    suspend operator fun invoke(
        audioData: AudioData,
        config: TranscriptionConfig
    ): TranscriptionResult {
        return aiProcessingEngine.transcribeAudio(audioData, config)
    }
}