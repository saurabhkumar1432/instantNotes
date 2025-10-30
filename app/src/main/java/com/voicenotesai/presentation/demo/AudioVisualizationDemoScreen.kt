package com.voicenotesai.presentation.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicenotesai.R
import com.voicenotesai.presentation.components.AudioVisualizationDisplay
import com.voicenotesai.presentation.components.RecordingIndicator
import com.voicenotesai.presentation.recording.AudioVisualizationViewModel
import com.voicenotesai.domain.visualization.ThermalState

/**
 * Demo screen showcasing the real-time audio visualization capabilities.
 * 
 * Requirements addressed:
 * - 1.3: Real-time audio visualization with smooth animations and contextual UI adaptations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioVisualizationDemoScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AudioVisualizationViewModel = hiltViewModel()
) {
    val visualizationData by viewModel.visualizationData.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val uiAdaptations by viewModel.uiAdaptations.collectAsStateWithLifecycle()
    
    var performanceMode by remember { mutableStateOf(PerformanceMode.HIGH) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.audio_visualization_demo_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = stringResource(R.string.close_demo)
                )
            }
        }
        
        // Main visualization display
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.real_time_visualization),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                AudioVisualizationDisplay(
                    visualizationData = visualizationData,
                    audioState = audioState,
                    uiAdaptations = uiAdaptations,
                    modifier = Modifier.fillMaxWidth(),
                    height = 160.dp
                )
                
                // Recording controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (audioState.isRecording) {
                                viewModel.stopRecording()
                            } else {
                                viewModel.startRecording()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (audioState.isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (audioState.isRecording) {
                                stringResource(R.string.stop_recording)
                            } else {
                                stringResource(R.string.start_recording)
                            }
                        )
                    }
                    
                    RecordingIndicator(
                        isRecording = audioState.isRecording,
                        audioLevel = visualizationData?.audioLevel ?: com.voicenotesai.domain.visualization.AudioLevel.SILENT
                    )
                }
            }
        }
        
        // Audio state information
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.audio_state_info),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                AudioStateInfoRow(
                    label = stringResource(R.string.recording_status),
                    value = if (audioState.isRecording) {
                        stringResource(R.string.recording_active)
                    } else {
                        stringResource(R.string.recording_inactive)
                    }
                )
                
                AudioStateInfoRow(
                    label = stringResource(R.string.duration),
                    value = formatDuration(audioState.duration)
                )
                
                AudioStateInfoRow(
                    label = stringResource(R.string.average_level),
                    value = String.format("%.2f", audioState.averageLevel)
                )
                
                AudioStateInfoRow(
                    label = stringResource(R.string.peak_level),
                    value = String.format("%.2f", audioState.peakLevel)
                )
                
                AudioStateInfoRow(
                    label = stringResource(R.string.speech_detected),
                    value = if (audioState.speechDetected) {
                        stringResource(R.string.yes)
                    } else {
                        stringResource(R.string.no)
                    }
                )
            }
        }
        
        // Visualization data information
        visualizationData?.let { data ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.visualization_data),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    AudioStateInfoRow(
                        label = stringResource(R.string.waveform_points),
                        value = data.waveform.amplitudes.size.toString()
                    )
                    
                    AudioStateInfoRow(
                        label = stringResource(R.string.rms_level),
                        value = String.format("%.3f", data.waveform.rms)
                    )
                    
                    AudioStateInfoRow(
                        label = stringResource(R.string.max_amplitude),
                        value = String.format("%.3f", data.waveform.maxAmplitude)
                    )
                    
                    AudioStateInfoRow(
                        label = stringResource(R.string.spectral_frequencies),
                        value = data.spectral.frequencies.size.toString()
                    )
                    
                    AudioStateInfoRow(
                        label = stringResource(R.string.dominant_frequency),
                        value = String.format("%.1f Hz", data.spectral.dominantFrequency)
                    )
                }
            }
        }
        
        // Performance controls
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.performance_settings),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = stringResource(R.string.performance_mode),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PerformanceMode.values().forEach { mode ->
                        FilterChip(
                            onClick = { 
                                performanceMode = mode
                                updatePerformanceMetrics(viewModel, mode)
                            },
                            label = { Text(mode.displayName) },
                            selected = performanceMode == mode
                        )
                    }
                }
                
                Text(
                    text = stringResource(R.string.performance_description, performanceMode.displayName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // UI adaptations information
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.ui_adaptations),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                AudioStateInfoRow(
                    label = stringResource(R.string.background_intensity),
                    value = String.format("%.2f", uiAdaptations.backgroundIntensity)
                )
                
                AudioStateInfoRow(
                    label = stringResource(R.string.pulse_animation),
                    value = if (uiAdaptations.pulseAnimation?.enabled == true) {
                        stringResource(R.string.enabled)
                    } else {
                        stringResource(R.string.disabled)
                    }
                )
                
                AudioStateInfoRow(
                    label = stringResource(R.string.show_waveform),
                    value = if (uiAdaptations.showWaveform) {
                        stringResource(R.string.yes)
                    } else {
                        stringResource(R.string.no)
                    }
                )
                
                AudioStateInfoRow(
                    label = stringResource(R.string.show_level_meter),
                    value = if (uiAdaptations.showLevelMeter) {
                        stringResource(R.string.yes)
                    } else {
                        stringResource(R.string.no)
                    }
                )
            }
        }
    }
}

@Composable
private fun AudioStateInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private enum class PerformanceMode(
    val displayName: String,
    val frameRate: Float,
    val cpuUsage: Float,
    val thermalState: ThermalState
) {
    HIGH("High Performance", 60f, 20f, ThermalState.NORMAL),
    MEDIUM("Balanced", 45f, 40f, ThermalState.WARM),
    LOW("Battery Saver", 30f, 70f, ThermalState.HOT)
}

private fun updatePerformanceMetrics(
    viewModel: AudioVisualizationViewModel,
    mode: PerformanceMode
) {
    viewModel.updatePerformanceMetrics(
        frameRate = mode.frameRate,
        cpuUsage = mode.cpuUsage,
        memoryUsage = 150L,
        batteryLevel = 0.8f,
        thermalState = mode.thermalState
    )
}

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}