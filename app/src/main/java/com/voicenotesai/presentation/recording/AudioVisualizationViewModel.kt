package com.voicenotesai.presentation.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicenotesai.domain.visualization.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

/**
 * ViewModel for managing audio visualization state and real-time updates.
 * 
 * Requirements addressed:
 * - 1.3: Real-time audio visualization with smooth animations and contextual UI adaptations
 */
@HiltViewModel
class AudioVisualizationViewModel @Inject constructor(
    private val audioVisualizationEngine: AudioVisualizationEngine
) : ViewModel() {
    
    private val _visualizationData = MutableStateFlow<VisualizationData?>(null)
    val visualizationData: StateFlow<VisualizationData?> = _visualizationData.asStateFlow()
    
    private val _audioState = MutableStateFlow(
        AudioState(
            isRecording = false,
            duration = 0L,
            averageLevel = 0f,
            peakLevel = 0f,
            silenceDuration = 0L,
            speechDetected = false
        )
    )
    val audioState: StateFlow<AudioState> = _audioState.asStateFlow()
    
    private val _uiAdaptations = MutableStateFlow(
        UIAdaptationConfig(
            backgroundIntensity = 0f,
            pulseAnimation = null,
            colorScheme = createDefaultColorScheme(),
            showSpectralBars = false,
            showWaveform = true,
            showLevelMeter = false
        )
    )
    val uiAdaptations: StateFlow<UIAdaptationConfig> = _uiAdaptations.asStateFlow()
    
    private val _performanceMetrics = MutableStateFlow(
        PerformanceMetrics(
            frameRate = 60f,
            cpuUsage = 20f,
            memoryUsage = 100L,
            batteryLevel = 1f,
            thermalState = ThermalState.NORMAL
        )
    )
    
    private var recordingJob: Job? = null
    private var recordingStartTime = 0L
    private var lastSpeechTime = 0L
    private val audioLevelHistory = mutableListOf<Float>()
    
    init {
        // Monitor audio levels and update UI adaptations
        viewModelScope.launch {
            audioVisualizationEngine.getAudioLevelStream()
                .collect { audioLevel ->
                    updateAudioState(audioLevel)
                    updateUIAdaptations()
                }
        }
        
        // Monitor performance and adapt quality
        viewModelScope.launch {
            _performanceMetrics.collect { metrics ->
                val qualitySettings = audioVisualizationEngine.adaptVisualizationQuality(metrics)
                // Quality settings are applied internally by the engine
            }
        }
    }
    
    /**
     * Starts audio visualization for recording
     */
    fun startRecording() {
        if (_audioState.value.isRecording) return
        
        recordingStartTime = System.currentTimeMillis()
        lastSpeechTime = recordingStartTime
        audioLevelHistory.clear()
        
        _audioState.value = _audioState.value.copy(
            isRecording = true,
            duration = 0L,
            silenceDuration = 0L
        )
        
        recordingJob = viewModelScope.launch {
            // Simulate real-time audio data processing
            while (_audioState.value.isRecording) {
                val simulatedAudioData = generateSimulatedAudioData()
                val visualizationData = audioVisualizationEngine.processAudioStream(simulatedAudioData)
                
                _visualizationData.value = visualizationData
                
                delay(16) // ~60 FPS updates
            }
        }
    }
    
    /**
     * Stops audio visualization
     */
    fun stopRecording() {
        recordingJob?.cancel()
        recordingJob = null
        
        _audioState.value = _audioState.value.copy(
            isRecording = false,
            speechDetected = false
        )
        
        // Fade out visualization
        viewModelScope.launch {
            delay(500)
            _visualizationData.value = null
        }
    }
    
    /**
     * Updates performance metrics for quality adaptation
     */
    fun updatePerformanceMetrics(
        frameRate: Float,
        cpuUsage: Float,
        memoryUsage: Long,
        batteryLevel: Float,
        thermalState: ThermalState
    ) {
        _performanceMetrics.value = PerformanceMetrics(
            frameRate = frameRate,
            cpuUsage = cpuUsage,
            memoryUsage = memoryUsage,
            batteryLevel = batteryLevel,
            thermalState = thermalState
        )
    }
    
    /**
     * Processes real audio data (to be called from actual audio recording)
     */
    fun processRealAudioData(audioData: FloatArray) {
        if (!_audioState.value.isRecording) return
        
        val visualizationData = audioVisualizationEngine.processAudioStream(audioData)
        _visualizationData.value = visualizationData
    }
    
    private fun updateAudioState(audioLevel: AudioLevel) {
        val currentTime = System.currentTimeMillis()
        val currentState = _audioState.value
        
        if (!currentState.isRecording) return
        
        // Update audio level history
        audioLevelHistory.add(audioLevel.rms)
        if (audioLevelHistory.size > 100) { // Keep last 100 samples
            audioLevelHistory.removeAt(0)
        }
        
        // Calculate average level
        val averageLevel = audioLevelHistory.average().toFloat()
        
        // Detect speech (simple threshold-based detection)
        val speechThreshold = 0.02f
        val speechDetected = audioLevel.rms > speechThreshold
        
        if (speechDetected) {
            lastSpeechTime = currentTime
        }
        
        val silenceDuration = if (speechDetected) 0L else currentTime - lastSpeechTime
        val duration = currentTime - recordingStartTime
        
        _audioState.value = currentState.copy(
            duration = duration,
            averageLevel = averageLevel,
            peakLevel = audioLevel.peak,
            silenceDuration = silenceDuration,
            speechDetected = speechDetected
        )
    }
    
    private fun updateUIAdaptations() {
        val adaptations = audioVisualizationEngine.getUIAdaptations(_audioState.value)
        _uiAdaptations.value = adaptations
    }
    
    private fun generateSimulatedAudioData(): FloatArray {
        // Generate simulated audio data for demonstration
        // In a real implementation, this would come from the microphone
        val bufferSize = 1024
        val audioData = FloatArray(bufferSize)
        
        val currentState = _audioState.value
        val baseAmplitude = if (currentState.isRecording) {
            // Simulate varying audio levels
            0.1f + Random.nextFloat() * 0.3f
        } else {
            0.01f // Very low noise floor
        }
        
        for (i in audioData.indices) {
            // Generate a mix of sine waves to simulate speech-like audio
            val t = i.toFloat() / bufferSize
            val frequency1 = 440f + Random.nextFloat() * 200f // Fundamental
            val frequency2 = frequency1 * 2f // First harmonic
            val frequency3 = frequency1 * 3f // Second harmonic
            
            audioData[i] = baseAmplitude * (
                0.6f * kotlin.math.sin(2f * kotlin.math.PI * frequency1 * t) +
                0.3f * kotlin.math.sin(2f * kotlin.math.PI * frequency2 * t) +
                0.1f * kotlin.math.sin(2f * kotlin.math.PI * frequency3 * t)
            ).toFloat()
            
            // Add some noise
            audioData[i] += (Random.nextFloat() - 0.5f) * 0.02f
        }
        
        return audioData
    }
    
    private fun createDefaultColorScheme(): VisualizationColorScheme {
        return VisualizationColorScheme(
            primaryColor = androidx.compose.ui.graphics.Color(0xFF2196F3),
            secondaryColor = androidx.compose.ui.graphics.Color(0xFF03DAC6),
            accentColor = androidx.compose.ui.graphics.Color(0xFFFF5722),
            backgroundColor = androidx.compose.ui.graphics.Color(0xFF121212),
            gradientColors = listOf(
                androidx.compose.ui.graphics.Color(0xFF2196F3),
                androidx.compose.ui.graphics.Color(0xFF03DAC6),
                androidx.compose.ui.graphics.Color(0xFF9C27B0)
            )
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
    }
}