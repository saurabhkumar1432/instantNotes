package com.voicenotesai.data.visualization

import com.voicenotesai.domain.visualization.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.math.sin
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for AudioVisualizationEngineImpl
 * 
 * Requirements addressed:
 * - 1.3: Real-time audio visualization with smooth animations and contextual UI adaptations
 */
class AudioVisualizationEngineImplTest {
    
    private lateinit var audioVisualizationEngine: AudioVisualizationEngineImpl
    
    @Before
    fun setup() {
        audioVisualizationEngine = AudioVisualizationEngineImpl()
    }
    
    @Test
    fun `processAudioStream generates valid visualization data`() {
        // Given
        val audioData = generateTestAudioData(1024, 0.5f)
        
        // When
        val result = audioVisualizationEngine.processAudioStream(audioData)
        
        // Then
        assertNotNull(result)
        assertTrue(result.waveform.amplitudes.isNotEmpty())
        assertTrue(result.audioLevel.rms > 0f)
        assertTrue(result.timestamp > 0L)
    }
    
    @Test
    fun `generateWaveform creates proper waveform data`() {
        // Given
        val samples = generateTestAudioData(512, 0.3f)
        val audioBuffer = AudioBuffer(
            samples = samples,
            sampleRate = 44100,
            channels = 1,
            bufferSize = samples.size
        )
        
        // When
        val waveform = audioVisualizationEngine.generateWaveform(audioBuffer)
        
        // Then
        assertTrue(waveform.amplitudes.isNotEmpty())
        assertTrue(waveform.peaks.isNotEmpty())
        assertTrue(waveform.rms > 0f)
        assertTrue(waveform.maxAmplitude >= waveform.rms)
        assertEquals(waveform.amplitudes.size, waveform.peaks.size)
    }
    
    @Test
    fun `createSpectralAnalysis generates frequency data`() {
        // Given
        val audioData = generateTestAudioData(512, 0.4f)
        
        // When
        val spectral = audioVisualizationEngine.createSpectralAnalysis(audioData)
        
        // Then
        assertTrue(spectral.frequencies.isNotEmpty())
        assertTrue(spectral.magnitudes.isNotEmpty())
        assertEquals(spectral.frequencies.size, spectral.magnitudes.size)
        assertTrue(spectral.dominantFrequency >= 0f)
        assertTrue(spectral.spectralCentroid >= 0f)
    }
    
    @Test
    fun `adaptVisualizationQuality adjusts based on performance`() {
        // Given - Low performance metrics
        val lowPerformance = PerformanceMetrics(
            frameRate = 25f,
            cpuUsage = 85f,
            memoryUsage = 500L,
            batteryLevel = 0.2f,
            thermalState = ThermalState.HOT
        )
        
        // When
        val lowQuality = audioVisualizationEngine.adaptVisualizationQuality(lowPerformance)
        
        // Then
        assertEquals(VisualizationQuality.LOW, lowQuality.visualizationQuality)
        assertEquals(30, lowQuality.updateRate)
        assertEquals(false, lowQuality.enableSpectralAnalysis)
        
        // Given - High performance metrics
        val highPerformance = PerformanceMetrics(
            frameRate = 60f,
            cpuUsage = 20f,
            memoryUsage = 100L,
            batteryLevel = 0.9f,
            thermalState = ThermalState.NORMAL
        )
        
        // When
        val highQuality = audioVisualizationEngine.adaptVisualizationQuality(highPerformance)
        
        // Then
        assertEquals(VisualizationQuality.HIGH, highQuality.visualizationQuality)
        assertEquals(60, highQuality.updateRate)
        assertEquals(true, highQuality.enableSpectralAnalysis)
    }
    
    @Test
    fun `getAudioLevelStream provides real-time updates`() = runTest {
        // Given
        val audioData = generateTestAudioData(256, 0.6f)
        
        // When
        audioVisualizationEngine.processAudioStream(audioData)
        val audioLevel = audioVisualizationEngine.getAudioLevelStream().first()
        
        // Then
        assertTrue(audioLevel.rms > 0f)
        assertTrue(audioLevel.peak >= audioLevel.rms)
        assertTrue(audioLevel.db > -60f) // Should be above silence threshold
    }
    
    @Test
    fun `getUIAdaptations creates appropriate config for recording state`() {
        // Given - Active recording with speech
        val activeAudioState = AudioState(
            isRecording = true,
            duration = 5000L,
            averageLevel = 0.5f,
            peakLevel = 0.8f,
            silenceDuration = 0L,
            speechDetected = true
        )
        
        // When
        val adaptations = audioVisualizationEngine.getUIAdaptations(activeAudioState)
        
        // Then
        assertTrue(adaptations.backgroundIntensity > 0f)
        assertNotNull(adaptations.pulseAnimation)
        assertTrue(adaptations.pulseAnimation!!.enabled)
        assertTrue(adaptations.showLevelMeter)
        assertTrue(adaptations.showWaveform)
    }
    
    @Test
    fun `silent audio produces appropriate audio level`() {
        // Given
        val silentAudioData = FloatArray(512) { 0f }
        
        // When
        val result = audioVisualizationEngine.processAudioStream(silentAudioData)
        
        // Then
        assertTrue(result.audioLevel.isSilent)
        assertEquals(0f, result.audioLevel.rms)
        assertEquals(0f, result.audioLevel.peak)
        assertTrue(result.audioLevel.db <= -60f)
    }
    
    @Test
    fun `clipping audio is detected correctly`() {
        // Given - Audio data with clipping (values near 1.0)
        val clippingAudioData = FloatArray(512) { 0.98f }
        
        // When
        val result = audioVisualizationEngine.processAudioStream(clippingAudioData)
        
        // Then
        assertTrue(result.audioLevel.isClipping)
        assertTrue(result.audioLevel.peak > 0.95f)
    }
    
    private fun generateTestAudioData(size: Int, amplitude: Float): FloatArray {
        return FloatArray(size) { index ->
            // Generate a sine wave with some harmonics
            val t = index.toFloat() / size
            val fundamental = amplitude * sin(2f * Math.PI.toFloat() * 440f * t)
            val harmonic = amplitude * 0.3f * sin(2f * Math.PI.toFloat() * 880f * t)
            fundamental + harmonic
        }
    }
}