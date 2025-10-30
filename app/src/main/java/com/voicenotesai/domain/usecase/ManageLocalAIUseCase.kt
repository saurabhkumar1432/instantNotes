package com.voicenotesai.domain.usecase

import com.voicenotesai.data.ai.FallbackProcessingService
import com.voicenotesai.data.ai.ModelManagementService
import com.voicenotesai.domain.ai.*
import com.voicenotesai.domain.model.AppError
import com.voicenotesai.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for managing local AI processing capabilities.
 * Handles initialization, model management, and fallback processing configuration.
 */
class ManageLocalAIUseCase @Inject constructor(
    private val localAIEngine: LocalAIEngine,
    private val fallbackProcessingService: FallbackProcessingService,
    private val modelManagementService: ModelManagementService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    /**
     * Initializes local AI processing capabilities.
     */
    suspend fun initializeLocalProcessing(): Result<LocalProcessingStatus> = withContext(ioDispatcher) {
        try {
            // Initialize fallback processing
            val fallbackResult = fallbackProcessingService.initializeFallback()
            
            if (!fallbackResult.success) {
                return@withContext Result.failure(
                    Exception("Failed to initialize fallback processing: ${fallbackResult.error}")
                )
            }

            // Initialize local models
            val initResult = localAIEngine.initializeLocalModels()
            
            if (!initResult.success) {
                return@withContext Result.failure(
                    Exception("Failed to initialize local models: ${initResult.error}")
                )
            }

            val status = LocalProcessingStatus(
                isAvailable = localAIEngine.isLocalProcessingAvailable(),
                capabilities = localAIEngine.getModelCapabilities(),
                fallbackCapabilities = fallbackResult.capabilities,
                initializedModels = initResult.modelsInitialized,
                totalModelSizeMB = initResult.totalSizeMB,
                initializationTimeMs = initResult.initializationTimeMs
            )

            Result.success(status)

        } catch (e: Exception) {
            Result.failure(Exception("Local AI initialization failed: ${e.message}"))
        }
    }

    /**
     * Downloads and installs local AI models.
     */
    suspend fun downloadModels(models: List<LocalModel>): Result<ModelDownloadStatus> = withContext(ioDispatcher) {
        try {
            val downloadResult = modelManagementService.downloadModels(models)
            
            val status = ModelDownloadStatus(
                success = downloadResult.success,
                downloadedModels = downloadResult.results.filter { it.success }.map { it.model },
                failedModels = downloadResult.results.filter { !it.success }.map { it.model },
                totalDownloadSizeMB = downloadResult.totalDownloadSizeMB,
                totalDownloadTimeMs = downloadResult.totalDownloadTimeMs,
                errors = downloadResult.results.filter { !it.success }.mapNotNull { it.error }
            )

            if (downloadResult.success) {
                Result.success(status)
            } else {
                Result.failure(Exception("Model download failed"))
            }

        } catch (e: Exception) {
            Result.failure(Exception("Model download failed: ${e.message}"))
        }
    }

    /**
     * Updates existing local models to newer versions.
     */
    suspend fun updateModels(): Result<ModelUpdateStatus> = withContext(ioDispatcher) {
        try {
            // Get current models
            val storageInfo = localAIEngine.getModelStorageInfo()
            val currentModels = storageInfo.modelDetails.map { it.model }
            
            // Update models
            val updateResult = modelManagementService.updateModels(currentModels)
            
            val status = ModelUpdateStatus(
                success = updateResult.success,
                updatedModels = updateResult.updatedModels,
                totalUpdateSizeMB = updateResult.totalDownloadSizeMB,
                updateTimeMs = updateResult.updateTimeMs,
                error = updateResult.error
            )

            if (updateResult.success) {
                Result.success(status)
            } else {
                Result.failure(Exception("Model update failed: ${updateResult.error}"))
            }

        } catch (e: Exception) {
            Result.failure(Exception("Model update failed: ${e.message}"))
        }
    }

    /**
     * Gets current local processing status and capabilities.
     */
    suspend fun getProcessingStatus(): Result<LocalProcessingStatus> = withContext(ioDispatcher) {
        try {
            val capabilities = localAIEngine.getModelCapabilities()
            val fallbackCapabilities = fallbackProcessingService.getFallbackCapabilities()
            val storageInfo = localAIEngine.getModelStorageInfo()
            val metrics = localAIEngine.getProcessingMetrics()

            val status = LocalProcessingStatus(
                isAvailable = localAIEngine.isLocalProcessingAvailable(),
                capabilities = capabilities,
                fallbackCapabilities = fallbackCapabilities,
                initializedModels = capabilities.availableModels,
                totalModelSizeMB = storageInfo.totalSizeMB,
                availableSpaceMB = storageInfo.availableSpaceMB,
                processingMetrics = metrics
            )

            Result.success(status)

        } catch (e: Exception) {
            Result.failure(Exception("Failed to get processing status: ${e.message}"))
        }
    }

    /**
     * Manages model storage by cleaning up old or unused models.
     */
    suspend fun cleanupModels(maxAgeDays: Int = 30): Result<ModelCleanupStatus> = withContext(ioDispatcher) {
        try {
            val cleanupResult = modelManagementService.cleanupModels(maxAgeDays)
            
            val status = ModelCleanupStatus(
                success = cleanupResult.success,
                cleanedModels = cleanupResult.cleanedModels,
                freedSpaceMB = cleanupResult.freedSpaceMB,
                error = cleanupResult.error
            )

            if (cleanupResult.success) {
                Result.success(status)
            } else {
                Result.failure(Exception("Model cleanup failed: ${cleanupResult.error}"))
            }

        } catch (e: Exception) {
            Result.failure(Exception("Model cleanup failed: ${e.message}"))
        }
    }

    /**
     * Deletes a specific model to free up space.
     */
    suspend fun deleteModel(modelId: String): Result<ModelDeletionStatus> = withContext(ioDispatcher) {
        try {
            val deletionResult = modelManagementService.deleteModel(modelId)
            
            val status = ModelDeletionStatus(
                success = deletionResult.success,
                modelId = deletionResult.modelId,
                freedSpaceMB = deletionResult.freedSpaceMB,
                error = deletionResult.error
            )

            if (deletionResult.success) {
                Result.success(status)
            } else {
                Result.failure(Exception("Model deletion failed: ${deletionResult.error}"))
            }

        } catch (e: Exception) {
            Result.failure(Exception("Model deletion failed: ${e.message}"))
        }
    }

    /**
     * Observes model download progress.
     */
    fun observeDownloadProgress(): Flow<Map<String, DownloadProgressInfo>> {
        return modelManagementService.downloadProgress.map { progressMap ->
            progressMap.mapValues { (_, progress) ->
                DownloadProgressInfo(
                    progressPercent = progress.progressPercent,
                    status = when (progress.status) {
                        com.voicenotesai.data.ai.DownloadStatus.STARTING -> DownloadProgressStatus.STARTING
                        com.voicenotesai.data.ai.DownloadStatus.DOWNLOADING -> DownloadProgressStatus.DOWNLOADING
                        com.voicenotesai.data.ai.DownloadStatus.COMPLETED -> DownloadProgressStatus.COMPLETED
                        com.voicenotesai.data.ai.DownloadStatus.FAILED -> DownloadProgressStatus.FAILED
                        com.voicenotesai.data.ai.DownloadStatus.CANCELLED -> DownloadProgressStatus.CANCELLED
                    },
                    downloadedMB = progress.downloadedMB,
                    totalMB = progress.totalMB,
                    speedMBps = progress.speedMBps
                )
            }
        }
    }

    /**
     * Checks if fallback processing is available.
     */
    fun isFallbackAvailable(): Boolean {
        return fallbackProcessingService.isFallbackAvailable()
    }
}

/**
 * Status of local AI processing capabilities.
 */
data class LocalProcessingStatus(
    val isAvailable: Boolean,
    val capabilities: LocalModelCapabilities,
    val fallbackCapabilities: com.voicenotesai.data.ai.FallbackCapabilities? = null,
    val initializedModels: List<LocalModel>,
    val totalModelSizeMB: Float,
    val availableSpaceMB: Float = 0f,
    val initializationTimeMs: Long = 0,
    val processingMetrics: ProcessingMetrics? = null
)

/**
 * Status of model download operation.
 */
data class ModelDownloadStatus(
    val success: Boolean,
    val downloadedModels: List<LocalModel>,
    val failedModels: List<LocalModel>,
    val totalDownloadSizeMB: Float,
    val totalDownloadTimeMs: Long,
    val errors: List<String>
)

/**
 * Status of model update operation.
 */
data class ModelUpdateStatus(
    val success: Boolean,
    val updatedModels: List<LocalModel>,
    val totalUpdateSizeMB: Float,
    val updateTimeMs: Long,
    val error: String?
)

/**
 * Status of model cleanup operation.
 */
data class ModelCleanupStatus(
    val success: Boolean,
    val cleanedModels: List<String>,
    val freedSpaceMB: Float,
    val error: String?
)

/**
 * Status of model deletion operation.
 */
data class ModelDeletionStatus(
    val success: Boolean,
    val modelId: String,
    val freedSpaceMB: Float,
    val error: String?
)

/**
 * Download progress information for UI.
 */
data class DownloadProgressInfo(
    val progressPercent: Float,
    val status: DownloadProgressStatus,
    val downloadedMB: Float,
    val totalMB: Float,
    val speedMBps: Float
)

/**
 * Download progress status for UI.
 */
enum class DownloadProgressStatus {
    STARTING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}