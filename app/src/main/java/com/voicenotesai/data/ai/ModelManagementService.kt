package com.voicenotesai.data.ai

import android.content.Context
import com.voicenotesai.domain.ai.*
import com.voicenotesai.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing local AI model downloads, updates, and storage.
 * Handles model lifecycle including download, verification, and cleanup.
 */
@Singleton
class ModelManagementService @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    companion object {
        private const val MODELS_DIR = "local_ai_models"
        private const val TEMP_DIR = "temp_models"
        private const val DOWNLOAD_TIMEOUT_MS = 300000L // 5 minutes
        private const val CHUNK_SIZE = 8192
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    private val modelsDir: File by lazy {
        File(context.filesDir, MODELS_DIR).apply { mkdirs() }
    }

    private val tempDir: File by lazy {
        File(context.cacheDir, TEMP_DIR).apply { mkdirs() }
    }

    private val _downloadProgress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val downloadProgress: Flow<Map<String, DownloadProgress>> = _downloadProgress.asStateFlow()

    /**
     * Downloads a model from the specified URL.
     */
    suspend fun downloadModel(model: LocalModel): ModelDownloadResult = withContext(ioDispatcher) {
        try {
            if (model.downloadUrl == null) {
                return@withContext ModelDownloadResult(
                    success = false,
                    model = model,
                    error = "No download URL provided for model ${model.name}"
                )
            }

            // Check if model already exists
            val modelFile = File(modelsDir, "${model.id}.bin")
            if (modelFile.exists() && verifyModelIntegrity(modelFile, model.checksum)) {
                return@withContext ModelDownloadResult(
                    success = true,
                    model = model.copy(isDownloaded = true),
                    downloadSizeMB = modelFile.length() / (1024 * 1024f),
                    downloadTimeMs = 0
                )
            }

            val startTime = System.currentTimeMillis()
            updateDownloadProgress(model.id, DownloadProgress(0f, DownloadStatus.STARTING))

            // Download to temporary file first
            val tempFile = File(tempDir, "${model.id}_temp.bin")
            val downloadResult = downloadFile(model.downloadUrl, tempFile, model.id)

            if (!downloadResult.success) {
                updateDownloadProgress(model.id, DownloadProgress(0f, DownloadStatus.FAILED))
                return@withContext ModelDownloadResult(
                    success = false,
                    model = model,
                    error = downloadResult.error
                )
            }

            // Verify downloaded file
            if (model.checksum != null && !verifyModelIntegrity(tempFile, model.checksum)) {
                tempFile.delete()
                updateDownloadProgress(model.id, DownloadProgress(0f, DownloadStatus.FAILED))
                return@withContext ModelDownloadResult(
                    success = false,
                    model = model,
                    error = "Model integrity verification failed"
                )
            }

            // Move to final location
            if (!tempFile.renameTo(modelFile)) {
                tempFile.delete()
                updateDownloadProgress(model.id, DownloadProgress(0f, DownloadStatus.FAILED))
                return@withContext ModelDownloadResult(
                    success = false,
                    model = model,
                    error = "Failed to move model to final location"
                )
            }

            val downloadTime = System.currentTimeMillis() - startTime
            val downloadSize = modelFile.length() / (1024 * 1024f)

            updateDownloadProgress(model.id, DownloadProgress(100f, DownloadStatus.COMPLETED))

            ModelDownloadResult(
                success = true,
                model = model.copy(isDownloaded = true),
                downloadSizeMB = downloadSize,
                downloadTimeMs = downloadTime
            )

        } catch (e: Exception) {
            updateDownloadProgress(model.id, DownloadProgress(0f, DownloadStatus.FAILED))
            ModelDownloadResult(
                success = false,
                model = model,
                error = "Download failed: ${e.message}"
            )
        }
    }

    /**
     * Downloads multiple models concurrently.
     */
    suspend fun downloadModels(models: List<LocalModel>): BatchDownloadResult = withContext(ioDispatcher) {
        val results = mutableListOf<ModelDownloadResult>()
        var totalDownloadSize = 0f
        val startTime = System.currentTimeMillis()

        models.forEach { model ->
            val result = downloadModel(model)
            results.add(result)
            if (result.success) {
                totalDownloadSize += result.downloadSizeMB
            }
        }

        val downloadTime = System.currentTimeMillis() - startTime
        val successfulDownloads = results.count { it.success }

        BatchDownloadResult(
            success = successfulDownloads > 0,
            results = results,
            totalDownloadSizeMB = totalDownloadSize,
            totalDownloadTimeMs = downloadTime,
            successfulDownloads = successfulDownloads,
            failedDownloads = results.size - successfulDownloads
        )
    }

    /**
     * Checks for model updates and downloads newer versions.
     */
    suspend fun updateModels(currentModels: List<LocalModel>): ModelUpdateResult = withContext(ioDispatcher) {
        try {
            val updatedModels = mutableListOf<LocalModel>()
            var totalDownloadSize = 0f
            val startTime = System.currentTimeMillis()

            // Check each model for updates (in real implementation, this would check a remote registry)
            currentModels.forEach { model ->
                val latestVersion = checkForModelUpdate(model)
                if (latestVersion != null && latestVersion.version != model.version) {
                    val downloadResult = downloadModel(latestVersion)
                    if (downloadResult.success) {
                        // Remove old version
                        val oldModelFile = File(modelsDir, "${model.id}.bin")
                        oldModelFile.delete()
                        
                        updatedModels.add(downloadResult.model)
                        totalDownloadSize += downloadResult.downloadSizeMB
                    }
                }
            }

            val updateTime = System.currentTimeMillis() - startTime

            ModelUpdateResult(
                success = true,
                updatedModels = updatedModels,
                totalDownloadSizeMB = totalDownloadSize,
                updateTimeMs = updateTime
            )

        } catch (e: Exception) {
            ModelUpdateResult(
                success = false,
                error = "Model update failed: ${e.message}"
            )
        }
    }

    /**
     * Deletes a specific model from storage.
     */
    suspend fun deleteModel(modelId: String): ModelDeletionResult = withContext(ioDispatcher) {
        try {
            val modelFile = File(modelsDir, "$modelId.bin")
            val sizeMB = if (modelFile.exists()) modelFile.length() / (1024 * 1024f) else 0f
            
            val deleted = if (modelFile.exists()) modelFile.delete() else true
            
            ModelDeletionResult(
                success = deleted,
                modelId = modelId,
                freedSpaceMB = if (deleted) sizeMB else 0f,
                error = if (!deleted) "Failed to delete model file" else null
            )

        } catch (e: Exception) {
            ModelDeletionResult(
                success = false,
                modelId = modelId,
                error = "Model deletion failed: ${e.message}"
            )
        }
    }

    /**
     * Gets information about all downloaded models.
     */
    suspend fun getDownloadedModelsInfo(): List<ModelInfo> = withContext(ioDispatcher) {
        val modelInfos = mutableListOf<ModelInfo>()
        
        modelsDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(".bin")) {
                val modelId = file.name.removeSuffix(".bin")
                val sizeMB = file.length() / (1024 * 1024f)
                
                modelInfos.add(
                    ModelInfo(
                        modelId = modelId,
                        sizeMB = sizeMB,
                        downloadDate = file.lastModified(),
                        lastAccessed = file.lastModified(),
                        isValid = file.exists() && file.length() > 0
                    )
                )
            }
        }
        
        modelInfos
    }

    /**
     * Cleans up old or unused models to free space.
     */
    suspend fun cleanupModels(maxAgeDays: Int = 30): ModelCleanupResult = withContext(ioDispatcher) {
        try {
            val currentTime = System.currentTimeMillis()
            val maxAgeMs = maxAgeDays * 24 * 60 * 60 * 1000L
            
            val cleanedModels = mutableListOf<String>()
            var freedSpace = 0f
            
            modelsDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".bin")) {
                    val age = currentTime - file.lastModified()
                    if (age > maxAgeMs) {
                        val sizeMB = file.length() / (1024 * 1024f)
                        if (file.delete()) {
                            cleanedModels.add(file.name.removeSuffix(".bin"))
                            freedSpace += sizeMB
                        }
                    }
                }
            }
            
            ModelCleanupResult(
                success = true,
                cleanedModels = cleanedModels,
                freedSpaceMB = freedSpace
            )

        } catch (e: Exception) {
            ModelCleanupResult(
                success = false,
                error = "Model cleanup failed: ${e.message}"
            )
        }
    }

    /**
     * Downloads a file from URL with progress tracking.
     */
    private suspend fun downloadFile(
        url: String,
        destinationFile: File,
        modelId: String
    ): FileDownloadResult = withContext(ioDispatcher) {
        var connection: HttpURLConnection? = null
        var retryCount = 0
        
        while (retryCount < MAX_RETRY_ATTEMPTS) {
            try {
                connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = DOWNLOAD_TIMEOUT_MS.toInt()
                connection.readTimeout = DOWNLOAD_TIMEOUT_MS.toInt()
                
                val fileSize = connection.contentLength
                if (fileSize <= 0) {
                    return@withContext FileDownloadResult(
                        success = false,
                        error = "Invalid file size from server"
                    )
                }
                
                updateDownloadProgress(modelId, DownloadProgress(0f, DownloadStatus.DOWNLOADING))
                
                connection.inputStream.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        val buffer = ByteArray(CHUNK_SIZE)
                        var totalBytesRead = 0L
                        var bytesRead: Int
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            
                            val progress = (totalBytesRead.toFloat() / fileSize) * 100f
                            updateDownloadProgress(modelId, DownloadProgress(progress, DownloadStatus.DOWNLOADING))
                        }
                    }
                }
                
                return@withContext FileDownloadResult(success = true)
                
            } catch (e: Exception) {
                retryCount++
                if (retryCount >= MAX_RETRY_ATTEMPTS) {
                    return@withContext FileDownloadResult(
                        success = false,
                        error = "Download failed after $MAX_RETRY_ATTEMPTS attempts: ${e.message}"
                    )
                }
                
                // Wait before retry
                kotlinx.coroutines.delay((1000 * retryCount).toLong())
            } finally {
                connection?.disconnect()
            }
        }
        
        FileDownloadResult(success = false, error = "Unexpected download failure")
    }

    /**
     * Verifies model file integrity using checksum.
     */
    private suspend fun verifyModelIntegrity(file: File, expectedChecksum: String?): Boolean = withContext(ioDispatcher) {
        if (expectedChecksum == null) return@withContext true
        
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(CHUNK_SIZE)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            
            val calculatedChecksum = digest.digest().joinToString("") { "%02x".format(it) }
            return@withContext calculatedChecksum.equals(expectedChecksum, ignoreCase = true)
            
        } catch (e: Exception) {
            return@withContext false
        }
    }

    /**
     * Checks for model updates (simulated - in real implementation would check remote registry).
     */
    private suspend fun checkForModelUpdate(model: LocalModel): LocalModel? = withContext(ioDispatcher) {
        // Simulate checking for updates
        kotlinx.coroutines.delay(100)
        
        // Return null if no update available (simulated)
        return@withContext null
    }

    /**
     * Updates download progress for a specific model.
     */
    private fun updateDownloadProgress(modelId: String, progress: DownloadProgress) {
        val currentProgress = _downloadProgress.value.toMutableMap()
        currentProgress[modelId] = progress
        _downloadProgress.value = currentProgress
    }
}

/**
 * Download progress information.
 */
data class DownloadProgress(
    val progressPercent: Float,
    val status: DownloadStatus,
    val downloadedMB: Float = 0f,
    val totalMB: Float = 0f,
    val speedMBps: Float = 0f
)

/**
 * Download status enumeration.
 */
enum class DownloadStatus {
    STARTING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Result of model download operation.
 */
data class ModelDownloadResult(
    val success: Boolean,
    val model: LocalModel,
    val downloadSizeMB: Float = 0f,
    val downloadTimeMs: Long = 0,
    val error: String? = null
)

/**
 * Result of batch download operation.
 */
data class BatchDownloadResult(
    val success: Boolean,
    val results: List<ModelDownloadResult>,
    val totalDownloadSizeMB: Float,
    val totalDownloadTimeMs: Long,
    val successfulDownloads: Int,
    val failedDownloads: Int
)

/**
 * Result of model deletion operation.
 */
data class ModelDeletionResult(
    val success: Boolean,
    val modelId: String,
    val freedSpaceMB: Float = 0f,
    val error: String? = null
)

/**
 * Information about a downloaded model.
 */
data class ModelInfo(
    val modelId: String,
    val sizeMB: Float,
    val downloadDate: Long,
    val lastAccessed: Long,
    val isValid: Boolean
)

/**
 * Result of model cleanup operation.
 */
data class ModelCleanupResult(
    val success: Boolean,
    val cleanedModels: List<String> = emptyList(),
    val freedSpaceMB: Float = 0f,
    val error: String? = null
)

/**
 * Result of file download operation.
 */
private data class FileDownloadResult(
    val success: Boolean,
    val error: String? = null
)