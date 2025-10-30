package com.voicenotesai.domain.visualization

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.Flow

/**
 * Audio visualization engine providing real-time waveform generation,
 * spectral analysis, and contextual UI adaptations during recording.
 * 
 * Requirements addressed:
 * - 1.3: Real-time audio visualization with smooth animations and contextual UI adaptations
 */
interface AudioVisualizationEngine {
    /**
     * Processes audio stream data to generate visualization data
     */
    fun processAudioStream(audioData: FloatArray): VisualizationData
    
    /**
     * Generates waveform data from audio buffer
     */
    fun generateWaveform(audioBuffer: AudioBuffer): WaveformData
    
    /**
     * Creates spectral analysis from FFT data
     */
    fun createSpectralAnalysis(fftData: FloatArray): SpectralData
    
    /**
     * Adapts visualization quality based on performance metrics
     */
    fun adaptVisualizationQuality(performance: PerformanceMetrics): QualitySettings
    
    /**
     * Provides real-time audio level monitoring
     */
    fun getAudioLevelStream(): Flow<AudioLevel>
    
    /**
     * Gets contextual UI adaptation suggestions based on audio state
     */
    fun getUIAdaptations(audioState: AudioState): UIAdaptationConfig
}

/**
 * Represents processed audio data for visualization
 */
@Stable
data class VisualizationData(
    val waveform: WaveformData,
    val spectral: SpectralData,
    val audioLevel: AudioLevel,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Audio buffer containing raw audio samples
 */
@Stable
data class AudioBuffer(
    val samples: FloatArray,
    val sampleRate: Int,
    val channels: Int,
    val bufferSize: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AudioBuffer

        if (!samples.contentEquals(other.samples)) return false
        if (sampleRate != other.sampleRate) return false
        if (channels != other.channels) return false
        if (bufferSize != other.bufferSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = samples.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + channels
        result = 31 * result + bufferSize
        return result
    }
}

/**
 * Waveform visualization data
 */
@Stable
data class WaveformData(
    val amplitudes: List<Float>,
    val peaks: List<Float>,
    val rms: Float,
    val maxAmplitude: Float,
    val minAmplitude: Float,
    val duration: Long
)

/**
 * Spectral analysis data for frequency visualization
 */
@Stable
data class SpectralData(
    val frequencies: List<Float>,
    val magnitudes: List<Float>,
    val dominantFrequency: Float,
    val spectralCentroid: Float,
    val spectralRolloff: Float,
    val spectralFlux: Float
)

/**
 * Real-time audio level information
 */
@Stable
data class AudioLevel(
    val rms: Float,
    val peak: Float,
    val db: Float,
    val isClipping: Boolean,
    val isSilent: Boolean
) {
    companion object {
        val SILENT = AudioLevel(0f, 0f, -60f, false, true)
        
        fun fromRMS(rms: Float): AudioLevel {
            val peak = rms * 1.414f // Approximate peak from RMS
            val db = if (rms > 0f) 20f * kotlin.math.log10(rms) else -60f
            val isClipping = peak > 0.95f
            val isSilent = rms < 0.01f
            
            return AudioLevel(rms, peak, db, isClipping, isSilent)
        }
    }
}

/**
 * Performance metrics for visualization adaptation
 */
@Stable
data class PerformanceMetrics(
    val frameRate: Float,
    val cpuUsage: Float,
    val memoryUsage: Long,
    val batteryLevel: Float,
    val thermalState: ThermalState
)

/**
 * Thermal states for performance monitoring
 */
enum class ThermalState {
    NORMAL,
    WARM,
    HOT,
    CRITICAL
}

/**
 * Quality settings for visualization adaptation
 */
@Stable
data class QualitySettings(
    val visualizationQuality: VisualizationQuality,
    val updateRate: Int, // Updates per second
    val maxDataPoints: Int,
    val enableSpectralAnalysis: Boolean,
    val enableAdvancedEffects: Boolean
)

/**
 * Visualization quality levels
 */
enum class VisualizationQuality {
    LOW,    // Basic waveform only
    MEDIUM, // Waveform with basic spectral
    HIGH    // Full spectral analysis with effects
}

/**
 * Current audio recording state
 */
@Stable
data class AudioState(
    val isRecording: Boolean,
    val duration: Long,
    val averageLevel: Float,
    val peakLevel: Float,
    val silenceDuration: Long,
    val speechDetected: Boolean
)

/**
 * UI adaptation configuration based on audio state
 */
@Stable
data class UIAdaptationConfig(
    val backgroundIntensity: Float,
    val pulseAnimation: PulseConfig?,
    val colorScheme: VisualizationColorScheme,
    val showSpectralBars: Boolean,
    val showWaveform: Boolean,
    val showLevelMeter: Boolean
)

/**
 * Pulse animation configuration
 */
@Stable
data class PulseConfig(
    val intensity: Float,
    val frequency: Float,
    val enabled: Boolean
)

/**
 * Color scheme for visualization
 */
@Stable
data class VisualizationColorScheme(
    val primaryColor: androidx.compose.ui.graphics.Color,
    val secondaryColor: androidx.compose.ui.graphics.Color,
    val accentColor: androidx.compose.ui.graphics.Color,
    val backgroundColor: androidx.compose.ui.graphics.Color,
    val gradientColors: List<androidx.compose.ui.graphics.Color>
)