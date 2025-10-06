package com.voicenotesai.data.repository

import kotlinx.coroutines.flow.Flow

sealed class RecordingState {
    object Idle : RecordingState()
    data class Recording(val duration: Long) : RecordingState()
    object Processing : RecordingState()
    data class Success(val transcribedText: String) : RecordingState()
    data class Error(val message: String) : RecordingState()
}

sealed class RecordingResult {
    data class Success(val transcribedText: String) : RecordingResult()
    data class Error(val message: String) : RecordingResult()
}

interface AudioRepository {
    suspend fun startRecording(): Flow<RecordingState>
    suspend fun stopRecording(): Result<String>
    fun hasPermission(): Boolean
    fun isRecording(): Boolean
    fun release()
}
