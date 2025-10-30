package com.voicenotesai.domain.model

import java.io.File

/**
 * Represents audio data for processing.
 */
data class AudioData(
    val data: ByteArray,
    val format: AudioFormat,
    val sampleRate: Int,
    val channels: Int,
    val durationMs: Long,
    val file: File? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AudioData

        if (!data.contentEquals(other.data)) return false
        if (format != other.format) return false
        if (sampleRate != other.sampleRate) return false
        if (channels != other.channels) return false
        if (durationMs != other.durationMs) return false
        if (file != other.file) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + sampleRate
        result = 31 * result + channels
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + (file?.hashCode() ?: 0)
        return result
    }
}

/**
 * Supported audio formats.
 */
enum class AudioFormat {
    WAV,
    MP3,
    AAC,
    FLAC,
    OGG,
    M4A,
    WEBM
}

