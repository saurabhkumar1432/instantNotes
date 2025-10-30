package com.voicenotesai.domain.ai

import com.voicenotesai.domain.model.AudioData

/**
 * Local AI processing engine interface for offline transcription and note generation.
 * Provides fallback processing capabilities when network is unavailable.
 */
interface LocalAIEngine {
    
    /**
     * Initializes local AI models for offline processing.
     * Downloads and sets up models if not already available.
     */
    suspend fun initializeLocalModels(): InitializationResult
    
    /**
     * Processes audio data offline using local models.
     * Provides basic transcription and note generation without network dependency.
     */
    suspend fun processOffline(audioData: AudioData): OfflineProcessingResult
    
    /**
     * Gets the capabilities of currently available local models.
     */
    fun getModelCapabilities(): LocalModelCapabilities
    
    /**
     * Updates local models to newer versions when available.
     */
    suspend fun updateModels(): ModelUpdateResult
    
    /**
     * Gets performance metrics for local processing operations.
     */
    fun getProcessingMetrics(): ProcessingMetrics
    
    /**
     * Checks if local processing is available and ready.
     */
    fun isLocalProcessingAvailable(): Boolean
    
    /**
     * Gets the storage space used by local models.
     */
    suspend fun getModelStorageInfo(): ModelStorageInfo
    
    /**
     * Clears cached models to free up storage space.
     */
    suspend fun clearModelCache(): ClearCacheResult
}

/**
 * Result of local model initialization.
 */
data class InitializationResult(
    val success: Boolean,
    val modelsInitialized: List<LocalModel> = emptyList(),
    val totalSizeMB: Float = 0f,
    val initializationTimeMs: Long = 0,
    val error: String? = null
)

/**
 * Result of offline audio processing.
 */
data class OfflineProcessingResult(
    val success: Boolean,
    val transcription: String = "",
    val confidence: Float = 0f,
    val generatedNotes: String = "",
    val processingTimeMs: Long = 0,
    val modelUsed: LocalModel? = null,
    val error: String? = null
)

/**
 * Capabilities of local AI models.
 */
data class LocalModelCapabilities(
    val transcriptionSupported: Boolean = false,
    val noteGenerationSupported: Boolean = false,
    val supportedLanguages: List<String> = emptyList(),
    val maxAudioDurationSeconds: Int = 0,
    val maxAudioSizeMB: Float = 0f,
    val estimatedAccuracy: Float = 0f,
    val availableModels: List<LocalModel> = emptyList()
)

/**
 * Result of model update operation.
 */
data class ModelUpdateResult(
    val success: Boolean,
    val updatedModels: List<LocalModel> = emptyList(),
    val totalDownloadSizeMB: Float = 0f,
    val updateTimeMs: Long = 0,
    val error: String? = null
)

/**
 * Performance metrics for local processing.
 */
data class ProcessingMetrics(
    val averageTranscriptionTimeMs: Long = 0,
    val averageNoteGenerationTimeMs: Long = 0,
    val totalProcessedAudioMinutes: Float = 0f,
    val averageAccuracy: Float = 0f,
    val memoryUsageMB: Float = 0f,
    val cpuUsagePercent: Float = 0f,
    val batteryImpactPercent: Float = 0f,
    val successRate: Float = 0f
)

/**
 * Information about model storage usage.
 */
data class ModelStorageInfo(
    val totalSizeMB: Float,
    val availableSpaceMB: Float,
    val modelDetails: List<ModelStorageDetail> = emptyList()
)

/**
 * Storage details for individual models.
 */
data class ModelStorageDetail(
    val model: LocalModel,
    val sizeMB: Float,
    val lastUsed: Long,
    val downloadDate: Long
)

/**
 * Result of cache clearing operation.
 */
data class ClearCacheResult(
    val success: Boolean,
    val freedSpaceMB: Float = 0f,
    val modelsRemoved: List<LocalModel> = emptyList(),
    val error: String? = null
)

/**
 * Represents a local AI model.
 */
data class LocalModel(
    val id: String,
    val name: String,
    val type: LocalModelType,
    val version: String,
    val sizeMB: Float,
    val accuracy: Float,
    val supportedLanguages: List<String>,
    val isDownloaded: Boolean = false,
    val isInitialized: Boolean = false,
    val downloadUrl: String? = null,
    val checksum: String? = null
)

/**
 * Types of local AI models.
 */
enum class LocalModelType {
    TRANSCRIPTION,
    NOTE_GENERATION,
    LANGUAGE_DETECTION,
    ENTITY_EXTRACTION,
    SENTIMENT_ANALYSIS
}

/**
 * Configuration for local processing.
 */
data class LocalProcessingConfig(
    val preferredModel: LocalModel? = null,
    val maxProcessingTimeMs: Long = 30000, // 30 seconds default
    val enableNoteGeneration: Boolean = true,
    val fallbackToBasicTranscription: Boolean = true,
    val qualityLevel: ProcessingQuality = ProcessingQuality.BALANCED
)

/**
 * Quality levels for local processing.
 */
enum class ProcessingQuality {
    FAST,      // Lower accuracy, faster processing
    BALANCED,  // Balanced accuracy and speed
    ACCURATE   // Higher accuracy, slower processing
}