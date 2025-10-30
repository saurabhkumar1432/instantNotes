package com.voicenotesai.data.visualization

import androidx.compose.ui.graphics.Color
import com.voicenotesai.domain.visualization.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Implementation of AudioVisualizationEngine providing real-time audio visualization
 * with waveform generation, spectral analysis, and contextual UI adaptations.
 * 
 * Requirements addressed:
 * - 1.3: Real-time audio visualization with smooth animations and contextual UI adaptations
 */
@Singleton
class AudioVisualizationEngineImpl @Inject constructor() : AudioVisualizationEngine {
    
    private val _audioLevelFlow = MutableStateFlow(AudioLevel.SILENT)
    private var currentQualitySettings = QualitySettings(
        visualizationQuality = VisualizationQuality.HIGH,
        updateRate = 60,
        maxDataPoints = 256,
        enableSpectralAnalysis = true,
        enableAdvancedEffects = true
    )
    
    // Circular buffer for waveform history
    private val waveformHistory = mutableListOf<Float>()
    private val maxHistorySize = 512
    
    // FFT processing variables
    private var fftBuffer = FloatArray(512)
    private var window = FloatArray(512)
    
    init {
        // Initialize Hamming window for FFT
        initializeWindow()
    }
    
    override fun processAudioStream(audioData: FloatArray): VisualizationData {
        val audioBuffer = AudioBuffer(
            samples = audioData,
            sampleRate = 44100, // Standard sample rate
            channels = 1,
            bufferSize = audioData.size
        )
        
        val waveform = generateWaveform(audioBuffer)
        val spectral = if (currentQualitySettings.enableSpectralAnalysis) {
            createSpectralAnalysis(audioData)
        } else {
            SpectralData(
                frequencies = emptyList(),
                magnitudes = emptyList(),
                dominantFrequency = 0f,
                spectralCentroid = 0f,
                spectralRolloff = 0f,
                spectralFlux = 0f
            )
        }
        
        val audioLevel = calculateAudioLevel(audioData)
        _audioLevelFlow.value = audioLevel
        
        return VisualizationData(
            waveform = waveform,
            spectral = spectral,
            audioLevel = audioLevel
        )
    }
    
    override fun generateWaveform(audioBuffer: AudioBuffer): WaveformData {
        val samples = audioBuffer.samples
        val downsampleFactor = maxOf(1, samples.size / currentQualitySettings.maxDataPoints)
        
        val amplitudes = mutableListOf<Float>()
        val peaks = mutableListOf<Float>()
        
        // Downsample and calculate amplitudes
        for (i in samples.indices step downsampleFactor) {
            val endIndex = minOf(i + downsampleFactor, samples.size)
            val chunk = samples.sliceArray(i until endIndex)
            
            val rms = sqrt(chunk.map { it * it }.average()).toFloat()
            val peak = chunk.maxOfOrNull { abs(it) } ?: 0f
            
            amplitudes.add(rms)
            peaks.add(peak)
        }
        
        // Update waveform history
        amplitudes.forEach { amplitude ->
            waveformHistory.add(amplitude)
            if (waveformHistory.size > maxHistorySize) {
                waveformHistory.removeAt(0)
            }
        }
        
        val overallRms = sqrt(samples.map { it * it }.average()).toFloat()
        val maxAmplitude = samples.maxOfOrNull { abs(it) } ?: 0f
        val minAmplitude = samples.minOfOrNull { abs(it) } ?: 0f
        
        return WaveformData(
            amplitudes = amplitudes,
            peaks = peaks,
            rms = overallRms,
            maxAmplitude = maxAmplitude,
            minAmplitude = minAmplitude,
            duration = (samples.size * 1000L) / audioBuffer.sampleRate
        )
    }
    
    override fun createSpectralAnalysis(fftData: FloatArray): SpectralData {
        val fftSize = minOf(fftData.size, fftBuffer.size)
        
        // Apply windowing function
        for (i in 0 until fftSize) {
            fftBuffer[i] = fftData[i] * window[i]
        }
        
        // Perform FFT (simplified implementation)
        val fftResult = performFFT(fftBuffer.sliceArray(0 until fftSize))
        
        // Calculate frequency bins
        val sampleRate = 44100f
        val frequencies = mutableListOf<Float>()
        val magnitudes = mutableListOf<Float>()
        
        for (i in 0 until fftResult.size / 2) {
            val frequency = (i * sampleRate) / fftResult.size
            val magnitude = sqrt(fftResult[i * 2].pow(2) + fftResult[i * 2 + 1].pow(2))
            
            frequencies.add(frequency)
            magnitudes.add(magnitude)
        }
        
        // Calculate spectral features
        val dominantFrequency = findDominantFrequency(frequencies, magnitudes)
        val spectralCentroid = calculateSpectralCentroid(frequencies, magnitudes)
        val spectralRolloff = calculateSpectralRolloff(frequencies, magnitudes)
        val spectralFlux = calculateSpectralFlux(magnitudes)
        
        return SpectralData(
            frequencies = frequencies,
            magnitudes = magnitudes,
            dominantFrequency = dominantFrequency,
            spectralCentroid = spectralCentroid,
            spectralRolloff = spectralRolloff,
            spectralFlux = spectralFlux
        )
    }
    
    override fun adaptVisualizationQuality(performance: PerformanceMetrics): QualitySettings {
        val quality = when {
            performance.frameRate < 30f || performance.cpuUsage > 80f -> VisualizationQuality.LOW
            performance.frameRate < 50f || performance.cpuUsage > 60f -> VisualizationQuality.MEDIUM
            else -> VisualizationQuality.HIGH
        }
        
        currentQualitySettings = when (quality) {
            VisualizationQuality.LOW -> QualitySettings(
                visualizationQuality = quality,
                updateRate = 30,
                maxDataPoints = 64,
                enableSpectralAnalysis = false,
                enableAdvancedEffects = false
            )
            VisualizationQuality.MEDIUM -> QualitySettings(
                visualizationQuality = quality,
                updateRate = 45,
                maxDataPoints = 128,
                enableSpectralAnalysis = true,
                enableAdvancedEffects = false
            )
            VisualizationQuality.HIGH -> QualitySettings(
                visualizationQuality = quality,
                updateRate = 60,
                maxDataPoints = 256,
                enableSpectralAnalysis = true,
                enableAdvancedEffects = true
            )
        }
        
        return currentQualitySettings
    }
    
    override fun getAudioLevelStream(): Flow<AudioLevel> {
        return _audioLevelFlow.asStateFlow()
    }
    
    override fun getUIAdaptations(audioState: AudioState): UIAdaptationConfig {
        val intensity = when {
            audioState.peakLevel > 0.8f -> 1.0f
            audioState.peakLevel > 0.5f -> 0.7f
            audioState.peakLevel > 0.2f -> 0.4f
            else -> 0.1f
        }
        
        val pulseConfig = if (audioState.isRecording && audioState.speechDetected) {
            PulseConfig(
                intensity = intensity,
                frequency = 1.0f + (audioState.averageLevel * 2f),
                enabled = true
            )
        } else null
        
        val colorScheme = createColorScheme(audioState)
        
        return UIAdaptationConfig(
            backgroundIntensity = intensity * 0.3f,
            pulseAnimation = pulseConfig,
            colorScheme = colorScheme,
            showSpectralBars = currentQualitySettings.enableSpectralAnalysis && audioState.speechDetected,
            showWaveform = true,
            showLevelMeter = audioState.isRecording
        )
    }
    
    private fun calculateAudioLevel(audioData: FloatArray): AudioLevel {
        if (audioData.isEmpty()) return AudioLevel.SILENT
        
        val rms = sqrt(audioData.map { it * it }.average()).toFloat()
        val peak = audioData.maxOfOrNull { abs(it) } ?: 0f
        val db = if (rms > 0f) 20f * log10(rms) else -60f
        val isClipping = peak > 0.95f
        val isSilent = rms < 0.01f
        
        return AudioLevel(rms, peak, db, isClipping, isSilent)
    }
    
    private fun initializeWindow() {
        // Hamming window
        for (i in window.indices) {
            window[i] = (0.54 - 0.46 * cos(2.0 * PI * i / (window.size - 1))).toFloat()
        }
    }
    
    private fun performFFT(input: FloatArray): FloatArray {
        // Simplified FFT implementation for demonstration
        // In a production app, you'd use a more efficient FFT library like JTransforms
        val n = input.size
        val output = FloatArray(n * 2) // Complex output (real, imaginary pairs)
        
        for (k in 0 until n) {
            var realSum = 0.0
            var imagSum = 0.0
            
            for (j in 0 until n) {
                val angle = -2.0 * PI * k * j / n
                realSum += input[j] * cos(angle)
                imagSum += input[j] * sin(angle)
            }
            
            output[k * 2] = realSum.toFloat()
            output[k * 2 + 1] = imagSum.toFloat()
        }
        
        return output
    }
    
    private fun findDominantFrequency(frequencies: List<Float>, magnitudes: List<Float>): Float {
        if (magnitudes.isEmpty()) return 0f
        
        val maxIndex = magnitudes.indices.maxByOrNull { magnitudes[it] } ?: 0
        return frequencies.getOrElse(maxIndex) { 0f }
    }
    
    private fun calculateSpectralCentroid(frequencies: List<Float>, magnitudes: List<Float>): Float {
        if (frequencies.isEmpty() || magnitudes.isEmpty()) return 0f
        
        val weightedSum = frequencies.zip(magnitudes) { freq, mag -> freq * mag }.sum()
        val totalMagnitude = magnitudes.sum()
        
        return if (totalMagnitude > 0f) weightedSum / totalMagnitude else 0f
    }
    
    private fun calculateSpectralRolloff(frequencies: List<Float>, magnitudes: List<Float>): Float {
        if (frequencies.isEmpty() || magnitudes.isEmpty()) return 0f
        
        val totalEnergy = magnitudes.sum()
        val threshold = totalEnergy * 0.85f // 85% rolloff point
        
        var cumulativeEnergy = 0f
        for (i in magnitudes.indices) {
            cumulativeEnergy += magnitudes[i]
            if (cumulativeEnergy >= threshold) {
                return frequencies.getOrElse(i) { 0f }
            }
        }
        
        return frequencies.lastOrNull() ?: 0f
    }
    
    private fun calculateSpectralFlux(magnitudes: List<Float>): Float {
        // Simplified spectral flux calculation
        if (magnitudes.size < 2) return 0f
        
        var flux = 0f
        for (i in 1 until magnitudes.size) {
            val diff = magnitudes[i] - magnitudes[i - 1]
            if (diff > 0) flux += diff
        }
        
        return flux / magnitudes.size
    }
    
    private fun createColorScheme(audioState: AudioState): VisualizationColorScheme {
        val baseHue = when {
            audioState.peakLevel > 0.8f -> 0f // Red for high levels
            audioState.peakLevel > 0.5f -> 60f // Yellow for medium levels
            audioState.speechDetected -> 120f // Green for speech
            else -> 240f // Blue for low/no audio
        }
        
        val saturation = 0.7f + (audioState.averageLevel * 0.3f)
        val brightness = 0.8f
        
        return VisualizationColorScheme(
            primaryColor = Color.hsv(baseHue, saturation, brightness),
            secondaryColor = Color.hsv((baseHue + 30f) % 360f, saturation * 0.8f, brightness),
            accentColor = Color.hsv((baseHue + 60f) % 360f, saturation, brightness * 0.9f),
            backgroundColor = Color.hsv(baseHue, saturation * 0.2f, 0.1f),
            gradientColors = listOf(
                Color.hsv(baseHue, saturation, brightness),
                Color.hsv((baseHue + 20f) % 360f, saturation * 0.9f, brightness * 0.8f),
                Color.hsv((baseHue + 40f) % 360f, saturation * 0.7f, brightness * 0.6f)
            )
        )
    }
}