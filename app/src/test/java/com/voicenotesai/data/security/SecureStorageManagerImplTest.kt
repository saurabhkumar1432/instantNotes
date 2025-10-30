package com.voicenotesai.data.security

import com.voicenotesai.domain.security.*
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for SecureStorageManager interface contract
 * These tests focus on the interface behavior rather than implementation details
 */
class SecureStorageManagerImplTest {

    @Test
    fun `StorageResult Success should be created correctly`() {
        val result = StorageResult.Success
        assertTrue(result is StorageResult.Success)
    }

    @Test
    fun `StorageResult Error should contain message and cause`() {
        val message = "Test error"
        val cause = RuntimeException("Test cause")
        val result = StorageResult.Error(message, cause)
        
        assertTrue(result is StorageResult.Error)
        assertEquals(message, result.message)
        assertEquals(cause, result.cause)
    }

    @Test
    fun `DeletionResult Success should be created correctly`() {
        val result = DeletionResult.Success
        assertTrue(result is DeletionResult.Success)
    }

    @Test
    fun `DeletionResult NotFound should be created correctly`() {
        val result = DeletionResult.NotFound
        assertTrue(result is DeletionResult.NotFound)
    }

    @Test
    fun `DeletionResult Error should contain message and cause`() {
        val message = "Deletion failed"
        val cause = RuntimeException("Test cause")
        val result = DeletionResult.Error(message, cause)
        
        assertTrue(result is DeletionResult.Error)
        assertEquals(message, result.message)
        assertEquals(cause, result.cause)
    }

    @Test
    fun `WipeResult Success should be created correctly`() {
        val result = WipeResult.Success
        assertTrue(result is WipeResult.Success)
    }

    @Test
    fun `WipeResult NotFound should be created correctly`() {
        val result = WipeResult.NotFound
        assertTrue(result is WipeResult.NotFound)
    }

    @Test
    fun `WipeResult PartialWipe should contain message`() {
        val message = "Partial wipe completed"
        val result = WipeResult.PartialWipe(message)
        
        assertTrue(result is WipeResult.PartialWipe)
        assertEquals(message, result.message)
    }

    @Test
    fun `WipeResult Error should contain message and cause`() {
        val message = "Wipe failed"
        val cause = RuntimeException("Test cause")
        val result = WipeResult.Error(message, cause)
        
        assertTrue(result is WipeResult.Error)
        assertEquals(message, result.message)
        assertEquals(cause, result.cause)
    }

    @Test
    fun `StorageStats should be created with correct values`() {
        val stats = StorageStats(
            totalKeys = 10,
            totalSizeBytes = 1000L,
            encryptedSizeBytes = 800L,
            lastAccessTime = 1234567890L,
            oldestEntryTime = 1234567800L,
            compressionRatio = 0.8f
        )
        
        assertEquals(10, stats.totalKeys)
        assertEquals(1000L, stats.totalSizeBytes)
        assertEquals(800L, stats.encryptedSizeBytes)
        assertEquals(1234567890L, stats.lastAccessTime)
        assertEquals(1234567800L, stats.oldestEntryTime)
        assertEquals(0.8f, stats.compressionRatio, 0.01f)
    }

    @Test
    fun `StorageOperation enum should have all required values`() {
        val operations = StorageOperation.values()
        
        assertTrue(operations.contains(StorageOperation.STORE))
        assertTrue(operations.contains(StorageOperation.RETRIEVE))
        assertTrue(operations.contains(StorageOperation.DELETE))
        assertTrue(operations.contains(StorageOperation.SECURE_WIPE))
        assertTrue(operations.contains(StorageOperation.LIST_KEYS))
        assertTrue(operations.contains(StorageOperation.GET_STATS))
    }

    @Test
    fun `AuditEntry should be created with correct values`() {
        val id = "test-id"
        val timestamp = System.currentTimeMillis()
        val operation = StorageOperation.STORE
        val key = "test-key"
        val success = true
        val errorMessage = "test error"
        val metadata = mapOf("key1" to "value1", "key2" to "value2")
        
        val auditEntry = AuditEntry(
            id = id,
            timestamp = timestamp,
            operation = operation,
            key = key,
            success = success,
            errorMessage = errorMessage,
            metadata = metadata
        )
        
        assertEquals(id, auditEntry.id)
        assertEquals(timestamp, auditEntry.timestamp)
        assertEquals(operation, auditEntry.operation)
        assertEquals(key, auditEntry.key)
        assertEquals(success, auditEntry.success)
        assertEquals(errorMessage, auditEntry.errorMessage)
        assertEquals(metadata, auditEntry.metadata)
    }

    @Test
    fun `AuditCleanupResult Success should contain entries removed count`() {
        val entriesRemoved = 5
        val result = AuditCleanupResult.Success(entriesRemoved)
        
        assertTrue(result is AuditCleanupResult.Success)
        assertEquals(entriesRemoved, result.entriesRemoved)
    }

    @Test
    fun `AuditCleanupResult Error should contain message`() {
        val message = "Cleanup failed"
        val result = AuditCleanupResult.Error(message)
        
        assertTrue(result is AuditCleanupResult.Error)
        assertEquals(message, result.message)
    }

    @Test
    fun `SecureStorageConfig should have correct default values`() {
        val config = SecureStorageConfig()
        
        assertTrue(config.encryptionEnabled)
        assertTrue(config.compressionEnabled)
        assertTrue(config.auditingEnabled)
        assertEquals(10000, config.maxAuditEntries)
        assertEquals(90, config.auditRetentionDays)
        assertEquals(3, config.secureWipeIterations)
    }

    @Test
    fun `SecureStorageConfig should allow custom values`() {
        val config = SecureStorageConfig(
            encryptionEnabled = false,
            compressionEnabled = false,
            auditingEnabled = false,
            maxAuditEntries = 5000,
            auditRetentionDays = 30,
            secureWipeIterations = 5
        )
        
        assertFalse(config.encryptionEnabled)
        assertFalse(config.compressionEnabled)
        assertFalse(config.auditingEnabled)
        assertEquals(5000, config.maxAuditEntries)
        assertEquals(30, config.auditRetentionDays)
        assertEquals(5, config.secureWipeIterations)
    }
}