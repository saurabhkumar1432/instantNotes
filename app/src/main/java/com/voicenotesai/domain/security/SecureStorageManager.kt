package com.voicenotesai.domain.security

/**
 * Secure storage manager interface with audit logging
 * Provides encrypted local storage with secure deletion capabilities
 */
interface SecureStorageManager {
    
    /**
     * Stores data securely with encryption
     * @param key Unique identifier for the data
     * @param value Data to store securely
     * @return Storage operation result
     */
    suspend fun storeSecurely(key: String, value: ByteArray): StorageResult
    
    /**
     * Retrieves securely stored data
     * @param key Unique identifier for the data
     * @return Decrypted data or null if not found
     */
    suspend fun retrieveSecurely(key: String): ByteArray?
    
    /**
     * Deletes securely stored data
     * @param key Unique identifier for the data
     * @return Deletion operation result
     */
    suspend fun deleteSecurely(key: String): DeletionResult
    
    /**
     * Performs secure wipe of data with overwriting
     * @param key Unique identifier for the data
     * @return Secure wipe operation result
     */
    suspend fun secureWipe(key: String): WipeResult
    
    /**
     * Stores text data securely
     * @param key Unique identifier for the data
     * @param value Text to store securely
     * @return Storage operation result
     */
    suspend fun storeText(key: String, value: String): StorageResult
    
    /**
     * Retrieves securely stored text
     * @param key Unique identifier for the data
     * @return Decrypted text or null if not found
     */
    suspend fun retrieveText(key: String): String?
    
    /**
     * Lists all stored keys (for management purposes)
     * @return List of stored keys
     */
    suspend fun listKeys(): List<String>
    
    /**
     * Gets storage statistics
     * @return Storage usage statistics
     */
    suspend fun getStorageStats(): StorageStats
    
    /**
     * Audits access to stored data
     * @param key Data key that was accessed
     * @param operation Type of operation performed
     * @return Audit entry created
     */
    fun auditAccess(key: String, operation: StorageOperation): AuditEntry
    
    /**
     * Gets audit trail for a specific key
     * @param key Data key to get audit trail for
     * @return List of audit entries
     */
    suspend fun getAuditTrail(key: String): List<AuditEntry>
    
    /**
     * Gets complete audit log
     * @param limit Maximum number of entries to return
     * @return List of audit entries
     */
    suspend fun getAuditLog(limit: Int = 100): List<AuditEntry>
    
    /**
     * Clears old audit entries
     * @param olderThan Timestamp before which to clear entries
     * @return Cleanup result
     */
    suspend fun clearOldAuditEntries(olderThan: Long): AuditCleanupResult
}

/**
 * Storage operation result
 */
sealed class StorageResult {
    object Success : StorageResult()
    data class Error(val message: String, val cause: Throwable? = null) : StorageResult()
}

/**
 * Deletion operation result
 */
sealed class DeletionResult {
    object Success : DeletionResult()
    object NotFound : DeletionResult()
    data class Error(val message: String, val cause: Throwable? = null) : DeletionResult()
}

/**
 * Secure wipe operation result
 */
sealed class WipeResult {
    object Success : WipeResult()
    object NotFound : WipeResult()
    data class PartialWipe(val message: String) : WipeResult()
    data class Error(val message: String, val cause: Throwable? = null) : WipeResult()
}

/**
 * Storage usage statistics
 */
data class StorageStats(
    val totalKeys: Int,
    val totalSizeBytes: Long,
    val encryptedSizeBytes: Long,
    val lastAccessTime: Long?,
    val oldestEntryTime: Long?,
    val compressionRatio: Float
)

/**
 * Storage operation types for audit logging
 */
enum class StorageOperation {
    STORE,
    RETRIEVE,
    DELETE,
    SECURE_WIPE,
    LIST_KEYS,
    GET_STATS
}

/**
 * Audit entry for storage operations
 */
data class AuditEntry(
    val id: String,
    val timestamp: Long,
    val operation: StorageOperation,
    val key: String,
    val success: Boolean,
    val errorMessage: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Audit cleanup result
 */
sealed class AuditCleanupResult {
    data class Success(val entriesRemoved: Int) : AuditCleanupResult()
    data class Error(val message: String) : AuditCleanupResult()
}

/**
 * Secure storage configuration
 */
data class SecureStorageConfig(
    val encryptionEnabled: Boolean = true,
    val compressionEnabled: Boolean = true,
    val auditingEnabled: Boolean = true,
    val maxAuditEntries: Int = 10000,
    val auditRetentionDays: Int = 90,
    val secureWipeIterations: Int = 3
)