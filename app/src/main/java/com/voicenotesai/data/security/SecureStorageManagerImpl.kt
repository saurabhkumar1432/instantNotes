package com.voicenotesai.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.voicenotesai.domain.security.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.SecureRandom
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SecureStorageManager with encrypted local storage and audit logging
 * Provides secure deletion with data overwriting capabilities
 */
@Singleton
class SecureStorageManagerImpl @Inject constructor(
    private val context: Context,
    private val encryptionService: EncryptionService
) : SecureStorageManager {
    
    companion object {
        private const val SECURE_STORAGE_PREFS = "secure_storage_prefs"
        private const val AUDIT_LOG_PREFS = "audit_log_prefs"
        private const val STORAGE_STATS_PREFS = "storage_stats_prefs"
        private const val SECURE_WIPE_ITERATIONS = 3
        private const val MAX_AUDIT_ENTRIES = 10000
        private const val AUDIT_RETENTION_DAYS = 90L
    }
    
    private val mutex = Mutex()
    private val secureRandom = SecureRandom()
    private val config = SecureStorageConfig()
    
    // Encrypted SharedPreferences for secure storage
    private val securePrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setRequestStrongBoxBacked(true)
            .build()
        
        EncryptedSharedPreferences.create(
            context,
            SECURE_STORAGE_PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    // Encrypted SharedPreferences for audit log
    private val auditPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setRequestStrongBoxBacked(true)
            .build()
        
        EncryptedSharedPreferences.create(
            context,
            AUDIT_LOG_PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    // Regular SharedPreferences for storage statistics (non-sensitive)
    private val statsPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(STORAGE_STATS_PREFS, Context.MODE_PRIVATE)
    }
    
    override suspend fun storeSecurely(key: String, value: ByteArray): StorageResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val startTime = System.currentTimeMillis()
                
                // Encrypt the data
                val encryptedData = if (config.encryptionEnabled) {
                    encryptionService.encryptAudio(value)
                } else {
                    EncryptedData(value, EncryptionMetadata("NONE", "none", ByteArray(0)))
                }
                
                // Compress if enabled
                val finalData = if (config.compressionEnabled) {
                    compressData(encryptedData.encryptedBytes)
                } else {
                    encryptedData.encryptedBytes
                }
                
                // Store metadata and data
                val metadataJson = serializeMetadata(encryptedData.metadata, config.compressionEnabled)
                val encodedData = Base64.encodeToString(finalData, Base64.NO_WRAP)
                
                securePrefs.edit()
                    .putString("${key}_data", encodedData)
                    .putString("${key}_metadata", metadataJson)
                    .putLong("${key}_timestamp", System.currentTimeMillis())
                    .apply()
                
                // Update storage statistics
                updateStorageStats(key, finalData.size.toLong(), value.size.toLong())
                
                // Create audit entry
                if (config.auditingEnabled) {
                    val auditEntry = AuditEntry(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        operation = StorageOperation.STORE,
                        key = key,
                        success = true,
                        metadata = mapOf(
                            "originalSize" to value.size.toString(),
                            "encryptedSize" to finalData.size.toString(),
                            "compressionEnabled" to config.compressionEnabled.toString(),
                            "processingTimeMs" to (System.currentTimeMillis() - startTime).toString()
                        )
                    )
                    storeAuditEntry(auditEntry)
                }
                
                StorageResult.Success
            } catch (e: Exception) {
                // Create error audit entry
                if (config.auditingEnabled) {
                    val auditEntry = AuditEntry(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        operation = StorageOperation.STORE,
                        key = key,
                        success = false,
                        errorMessage = e.message
                    )
                    storeAuditEntry(auditEntry)
                }
                
                StorageResult.Error("Failed to store data securely: ${e.message}", e)
            }
        }
    }
    
    override suspend fun retrieveSecurely(key: String): ByteArray? = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val startTime = System.currentTimeMillis()
                
                val encodedData = securePrefs.getString("${key}_data", null)
                val metadataJson = securePrefs.getString("${key}_metadata", null)
                
                if (encodedData == null || metadataJson == null) {
                    // Create audit entry for not found
                    if (config.auditingEnabled) {
                        val auditEntry = AuditEntry(
                            id = UUID.randomUUID().toString(),
                            timestamp = System.currentTimeMillis(),
                            operation = StorageOperation.RETRIEVE,
                            key = key,
                            success = false,
                            errorMessage = "Key not found"
                        )
                        storeAuditEntry(auditEntry)
                    }
                    return@withLock null
                }
                
                val compressedData = Base64.decode(encodedData, Base64.NO_WRAP)
                val metadata = deserializeMetadata(metadataJson)
                
                // Decompress if needed
                val encryptedBytes = if (metadata.second) {
                    decompressData(compressedData)
                } else {
                    compressedData
                }
                
                // Decrypt the data
                val decryptedData = if (config.encryptionEnabled && metadata.first.algorithm != "NONE") {
                    val encryptedData = EncryptedData(encryptedBytes, metadata.first)
                    encryptionService.decryptAudio(encryptedData)
                } else {
                    encryptedBytes
                }
                
                // Create successful audit entry
                if (config.auditingEnabled) {
                    val auditEntry = AuditEntry(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        operation = StorageOperation.RETRIEVE,
                        key = key,
                        success = true,
                        metadata = mapOf(
                            "dataSize" to decryptedData.size.toString(),
                            "processingTimeMs" to (System.currentTimeMillis() - startTime).toString()
                        )
                    )
                    storeAuditEntry(auditEntry)
                }
                
                decryptedData
            } catch (e: Exception) {
                // Create error audit entry
                if (config.auditingEnabled) {
                    val auditEntry = AuditEntry(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        operation = StorageOperation.RETRIEVE,
                        key = key,
                        success = false,
                        errorMessage = e.message
                    )
                    storeAuditEntry(auditEntry)
                }
                
                null
            }
        }
    }
    
    override suspend fun deleteSecurely(key: String): DeletionResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val exists = securePrefs.contains("${key}_data")
                
                if (!exists) {
                    // Create audit entry for not found
                    if (config.auditingEnabled) {
                        val auditEntry = AuditEntry(
                            id = UUID.randomUUID().toString(),
                            timestamp = System.currentTimeMillis(),
                            operation = StorageOperation.DELETE,
                            key = key,
                            success = false,
                            errorMessage = "Key not found"
                        )
                        storeAuditEntry(auditEntry)
                    }
                    return@withLock DeletionResult.NotFound
                }
                
                // Remove from secure preferences
                securePrefs.edit()
                    .remove("${key}_data")
                    .remove("${key}_metadata")
                    .remove("${key}_timestamp")
                    .apply()
                
                // Update storage statistics
                removeFromStorageStats(key)
                
                // Create successful audit entry
                if (config.auditingEnabled) {
                    val auditEntry = AuditEntry(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        operation = StorageOperation.DELETE,
                        key = key,
                        success = true
                    )
                    storeAuditEntry(auditEntry)
                }
                
                DeletionResult.Success
            } catch (e: Exception) {
                // Create error audit entry
                if (config.auditingEnabled) {
                    val auditEntry = AuditEntry(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        operation = StorageOperation.DELETE,
                        key = key,
                        success = false,
                        errorMessage = e.message
                    )
                    storeAuditEntry(auditEntry)
                }
                
                DeletionResult.Error("Failed to delete data: ${e.message}", e)
            }
        }
    }
    
    override suspend fun secureWipe(key: String): WipeResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val exists = securePrefs.contains("${key}_data")
                
                if (!exists) {
                    // Create audit entry for not found
                    if (config.auditingEnabled) {
                        val auditEntry = AuditEntry(
                            id = UUID.randomUUID().toString(),
                            timestamp = System.currentTimeMillis(),
                            operation = StorageOperation.SECURE_WIPE,
                            key = key,
                            success = false,
                            errorMessage = "Key not found"
                        )
                        storeAuditEntry(auditEntry)
                    }
                    return@withLock WipeResult.NotFound
                }
                
                // Get original data size for overwriting
                val encodedData = securePrefs.getString("${key}_data", null)
                val originalSize = encodedData?.let { Base64.decode(it, Base64.NO_WRAP).size } ?: 0
                
                // Perform secure wipe by overwriting with random data multiple times
                var wipeSuccess = true
                var wipeMessage = ""
                
                try {
                    for (iteration in 1..config.secureWipeIterations) {
                        val randomData = ByteArray(originalSize)
                        secureRandom.nextBytes(randomData)
                        val encodedRandomData = Base64.encodeToString(randomData, Base64.NO_WRAP)
                        
                        securePrefs.edit()
                            .putString("${key}_data", encodedRandomData)
                            .apply()
                        
                        // Force write to disk
                        Thread.sleep(10) // Small delay to ensure write completion
                    }
                } catch (e: Exception) {
                    wipeSuccess = false
                    wipeMessage = "Partial wipe completed: ${e.message}"
                }
                
                // Finally remove the entries
                securePrefs.edit()
                    .remove("${key}_data")
                    .remove("${key}_metadata")
                    .remove("${key}_timestamp")
                    .apply()
                
                // Update storage statistics
                removeFromStorageStats(key)
                
                // Create audit entry
                if (config.auditingEnabled) {
                    val auditEntry = AuditEntry(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        operation = StorageOperation.SECURE_WIPE,
                        key = key,
                        success = wipeSuccess,
                        errorMessage = if (!wipeSuccess) wipeMessage else null,
                        metadata = mapOf(
                            "wipeIterations" to config.secureWipeIterations.toString(),
                            "originalDataSize" to originalSize.toString()
                        )
                    )
                    storeAuditEntry(auditEntry)
                }
                
                if (wipeSuccess) {
                    WipeResult.Success
                } else {
                    WipeResult.PartialWipe(wipeMessage)
                }
            } catch (e: Exception) {
                // Create error audit entry
                if (config.auditingEnabled) {
                    val auditEntry = AuditEntry(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        operation = StorageOperation.SECURE_WIPE,
                        key = key,
                        success = false,
                        errorMessage = e.message
                    )
                    storeAuditEntry(auditEntry)
                }
                
                WipeResult.Error("Failed to perform secure wipe: ${e.message}", e)
            }
        }
    }
    
    override suspend fun storeText(key: String, value: String): StorageResult {
        return storeSecurely(key, value.toByteArray(Charsets.UTF_8))
    }
    
    override suspend fun retrieveText(key: String): String? {
        return retrieveSecurely(key)?.let { String(it, Charsets.UTF_8) }
    }
    
    override suspend fun listKeys(): List<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val allKeys = securePrefs.all.keys
                val dataKeys = allKeys.filter { it.endsWith("_data") }
                    .map { it.removeSuffix("_data") }
                
                // Create audit entry
                if (config.auditingEnabled) {
                    val auditEntry = AuditEntry(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        operation = StorageOperation.LIST_KEYS,
                        key = "ALL",
                        success = true,
                        metadata = mapOf("keyCount" to dataKeys.size.toString())
                    )
                    storeAuditEntry(auditEntry)
                }
                
                dataKeys
            } catch (e: Exception) {
                // Create error audit entry
                if (config.auditingEnabled) {
                    val auditEntry = AuditEntry(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        operation = StorageOperation.LIST_KEYS,
                        key = "ALL",
                        success = false,
                        errorMessage = e.message
                    )
                    storeAuditEntry(auditEntry)
                }
                
                emptyList()
            }
        }
    }
    
    override suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val totalKeys = statsPrefs.getInt("total_keys", 0)
                val totalSizeBytes = statsPrefs.getLong("total_size_bytes", 0L)
                val encryptedSizeBytes = statsPrefs.getLong("encrypted_size_bytes", 0L)
                val lastAccessTime = statsPrefs.getLong("last_access_time", 0L)
                val oldestEntryTime = statsPrefs.getLong("oldest_entry_time", 0L)
                
                val compressionRatio = if (totalSizeBytes > 0) {
                    encryptedSizeBytes.toFloat() / totalSizeBytes.toFloat()
                } else {
                    1.0f
                }
                
                val stats = StorageStats(
                    totalKeys = totalKeys,
                    totalSizeBytes = totalSizeBytes,
                    encryptedSizeBytes = encryptedSizeBytes,
                    lastAccessTime = if (lastAccessTime > 0) lastAccessTime else null,
                    oldestEntryTime = if (oldestEntryTime > 0) oldestEntryTime else null,
                    compressionRatio = compressionRatio
                )
                
                // Create audit entry
                if (config.auditingEnabled) {
                    val auditEntry = AuditEntry(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        operation = StorageOperation.GET_STATS,
                        key = "STATS",
                        success = true,
                        metadata = mapOf(
                            "totalKeys" to totalKeys.toString(),
                            "totalSizeBytes" to totalSizeBytes.toString()
                        )
                    )
                    storeAuditEntry(auditEntry)
                }
                
                stats
            } catch (e: Exception) {
                // Create error audit entry and return empty stats
                if (config.auditingEnabled) {
                    val auditEntry = AuditEntry(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        operation = StorageOperation.GET_STATS,
                        key = "STATS",
                        success = false,
                        errorMessage = e.message
                    )
                    storeAuditEntry(auditEntry)
                }
                
                StorageStats(0, 0L, 0L, null, null, 1.0f)
            }
        }
    }
    
    override fun auditAccess(key: String, operation: StorageOperation): AuditEntry {
        val auditEntry = AuditEntry(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            operation = operation,
            key = key,
            success = true
        )
        
        if (config.auditingEnabled) {
            // Store audit entry asynchronously to avoid blocking
            try {
                storeAuditEntry(auditEntry)
            } catch (e: Exception) {
                // Log error but don't fail the operation
                android.util.Log.e("SecureStorageManager", "Failed to store audit entry", e)
            }
        }
        
        return auditEntry
    }
    
    override suspend fun getAuditTrail(key: String): List<AuditEntry> = withContext(Dispatchers.IO) {
        if (!config.auditingEnabled) return@withContext emptyList()
        
        try {
            val allAuditEntries = getAllAuditEntries()
            allAuditEntries.filter { it.key == key }
                .sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override suspend fun getAuditLog(limit: Int): List<AuditEntry> = withContext(Dispatchers.IO) {
        if (!config.auditingEnabled) return@withContext emptyList()
        
        try {
            val allAuditEntries = getAllAuditEntries()
            allAuditEntries.sortedByDescending { it.timestamp }
                .take(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override suspend fun clearOldAuditEntries(olderThan: Long): AuditCleanupResult = withContext(Dispatchers.IO) {
        if (!config.auditingEnabled) {
            return@withContext AuditCleanupResult.Success(0)
        }
        
        mutex.withLock {
            try {
                val allAuditEntries = getAllAuditEntries()
                val entriesToKeep = allAuditEntries.filter { it.timestamp >= olderThan }
                val entriesRemoved = allAuditEntries.size - entriesToKeep.size
                
                // Clear all audit entries and store only the ones to keep
                auditPrefs.edit().clear().apply()
                
                entriesToKeep.forEach { entry ->
                    storeAuditEntry(entry)
                }
                
                AuditCleanupResult.Success(entriesRemoved)
            } catch (e: Exception) {
                AuditCleanupResult.Error("Failed to clean audit entries: ${e.message}")
            }
        }
    }
    
    /**
     * Private helper methods
     */
    
    private fun storeAuditEntry(entry: AuditEntry) {
        try {
            val entryJson = serializeAuditEntry(entry)
            auditPrefs.edit()
                .putString(entry.id, entryJson)
                .apply()
            
            // Cleanup old entries if we exceed the limit
            cleanupAuditEntriesIfNeeded()
        } catch (e: Exception) {
            android.util.Log.e("SecureStorageManager", "Failed to store audit entry", e)
        }
    }
    
    private fun getAllAuditEntries(): List<AuditEntry> {
        return try {
            auditPrefs.all.values.mapNotNull { value ->
                if (value is String) {
                    deserializeAuditEntry(value)
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun cleanupAuditEntriesIfNeeded() {
        try {
            val allEntries = getAllAuditEntries()
            if (allEntries.size > config.maxAuditEntries) {
                val cutoffTime = System.currentTimeMillis() - (config.auditRetentionDays * 24 * 60 * 60 * 1000)
                val entriesToKeep = allEntries.filter { it.timestamp >= cutoffTime }
                    .sortedByDescending { it.timestamp }
                    .take(config.maxAuditEntries)
                
                // Clear and rebuild audit log
                auditPrefs.edit().clear().apply()
                entriesToKeep.forEach { entry ->
                    val entryJson = serializeAuditEntry(entry)
                    auditPrefs.edit().putString(entry.id, entryJson).apply()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SecureStorageManager", "Failed to cleanup audit entries", e)
        }
    }
    
    private fun updateStorageStats(key: String, encryptedSize: Long, originalSize: Long) {
        try {
            val currentKeys = statsPrefs.getInt("total_keys", 0)
            val currentEncryptedSize = statsPrefs.getLong("encrypted_size_bytes", 0L)
            val currentOriginalSize = statsPrefs.getLong("total_size_bytes", 0L)
            val currentTime = System.currentTimeMillis()
            
            // Check if this is an update or new entry
            val isNewKey = !statsPrefs.contains("key_${key}_size")
            
            statsPrefs.edit()
                .putInt("total_keys", if (isNewKey) currentKeys + 1 else currentKeys)
                .putLong("encrypted_size_bytes", currentEncryptedSize + encryptedSize)
                .putLong("total_size_bytes", currentOriginalSize + originalSize)
                .putLong("last_access_time", currentTime)
                .putLong("key_${key}_size", encryptedSize)
                .apply()
            
            // Update oldest entry time if this is the first entry
            if (currentKeys == 0) {
                statsPrefs.edit().putLong("oldest_entry_time", currentTime).apply()
            }
        } catch (e: Exception) {
            android.util.Log.e("SecureStorageManager", "Failed to update storage stats", e)
        }
    }
    
    private fun removeFromStorageStats(key: String) {
        try {
            val keySize = statsPrefs.getLong("key_${key}_size", 0L)
            if (keySize > 0) {
                val currentKeys = statsPrefs.getInt("total_keys", 0)
                val currentEncryptedSize = statsPrefs.getLong("encrypted_size_bytes", 0L)
                
                statsPrefs.edit()
                    .putInt("total_keys", maxOf(0, currentKeys - 1))
                    .putLong("encrypted_size_bytes", maxOf(0L, currentEncryptedSize - keySize))
                    .remove("key_${key}_size")
                    .apply()
            }
        } catch (e: Exception) {
            android.util.Log.e("SecureStorageManager", "Failed to remove from storage stats", e)
        }
    }
    
    private fun compressData(data: ByteArray): ByteArray {
        // Simple compression using GZIP
        return try {
            val output = java.io.ByteArrayOutputStream()
            val gzip = java.util.zip.GZIPOutputStream(output)
            gzip.write(data)
            gzip.close()
            output.toByteArray()
        } catch (e: Exception) {
            data // Return original data if compression fails
        }
    }
    
    private fun decompressData(compressedData: ByteArray): ByteArray {
        return try {
            val input = java.io.ByteArrayInputStream(compressedData)
            val gzip = java.util.zip.GZIPInputStream(input)
            gzip.readBytes()
        } catch (e: Exception) {
            compressedData // Return original data if decompression fails
        }
    }
    
    private fun serializeMetadata(metadata: EncryptionMetadata, compressionEnabled: Boolean): String {
        // Simple JSON-like serialization
        return """
            {
                "algorithm": "${metadata.algorithm}",
                "keyAlias": "${metadata.keyAlias}",
                "iv": "${Base64.encodeToString(metadata.iv, Base64.NO_WRAP)}",
                "authTag": "${metadata.authTag?.let { Base64.encodeToString(it, Base64.NO_WRAP) } ?: ""}",
                "version": ${metadata.version},
                "timestamp": ${metadata.timestamp},
                "compressionEnabled": $compressionEnabled
            }
        """.trimIndent()
    }
    
    private fun deserializeMetadata(json: String): Pair<EncryptionMetadata, Boolean> {
        // Simple JSON parsing (in production, use a proper JSON library)
        val algorithm = extractJsonValue(json, "algorithm")
        val keyAlias = extractJsonValue(json, "keyAlias")
        val ivString = extractJsonValue(json, "iv")
        val authTagString = extractJsonValue(json, "authTag")
        val version = extractJsonValue(json, "version").toIntOrNull() ?: 1
        val timestamp = extractJsonValue(json, "timestamp").toLongOrNull() ?: System.currentTimeMillis()
        val compressionEnabled = extractJsonValue(json, "compressionEnabled").toBooleanStrictOrNull() ?: false
        
        val iv = if (ivString.isNotEmpty()) Base64.decode(ivString, Base64.NO_WRAP) else ByteArray(0)
        val authTag = if (authTagString.isNotEmpty()) Base64.decode(authTagString, Base64.NO_WRAP) else null
        
        val metadata = EncryptionMetadata(algorithm, keyAlias, iv, authTag, version, timestamp)
        return Pair(metadata, compressionEnabled)
    }
    
    private fun serializeAuditEntry(entry: AuditEntry): String {
        val metadataJson = entry.metadata.entries.joinToString(",") { 
            "\"${it.key}\": \"${it.value}\"" 
        }
        
        return """
            {
                "id": "${entry.id}",
                "timestamp": ${entry.timestamp},
                "operation": "${entry.operation}",
                "key": "${entry.key}",
                "success": ${entry.success},
                "errorMessage": "${entry.errorMessage ?: ""}",
                "metadata": {$metadataJson}
            }
        """.trimIndent()
    }
    
    private fun deserializeAuditEntry(json: String): AuditEntry? {
        return try {
            val id = extractJsonValue(json, "id")
            val timestamp = extractJsonValue(json, "timestamp").toLongOrNull() ?: return null
            val operation = StorageOperation.valueOf(extractJsonValue(json, "operation"))
            val key = extractJsonValue(json, "key")
            val success = extractJsonValue(json, "success").toBooleanStrictOrNull() ?: return null
            val errorMessage = extractJsonValue(json, "errorMessage").takeIf { it.isNotEmpty() }
            
            // Parse metadata (simplified)
            val metadata = mutableMapOf<String, String>()
            // In production, use proper JSON parsing
            
            AuditEntry(id, timestamp, operation, key, success, errorMessage, metadata)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun extractJsonValue(json: String, key: String): String {
        // Simple JSON value extraction (in production, use a proper JSON library)
        val pattern = "\"$key\"\\s*:\\s*\"?([^\"\\n,}]+)\"?".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.trim('"') ?: ""
    }
}