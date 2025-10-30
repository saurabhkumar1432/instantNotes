package com.voicenotesai.domain.security

import javax.crypto.SecretKey

/**
 * Core encryption service interface for AES-256-GCM encryption
 * Provides hardware-backed security using Android Keystore
 */
interface EncryptionService {
    
    /**
     * Encrypts audio data using AES-256-GCM
     * @param audioData Raw audio bytes to encrypt
     * @return Encrypted data with metadata
     */
    suspend fun encryptAudio(audioData: ByteArray): EncryptedData
    
    /**
     * Decrypts audio data
     * @param encryptedData Encrypted data with metadata
     * @return Decrypted audio bytes
     */
    suspend fun decryptAudio(encryptedData: EncryptedData): ByteArray
    
    /**
     * Encrypts text content
     * @param text Plain text to encrypt
     * @return Encrypted text with metadata
     */
    suspend fun encryptText(text: String): EncryptedText
    
    /**
     * Decrypts text content
     * @param encryptedText Encrypted text with metadata
     * @return Decrypted plain text
     */
    suspend fun decryptText(encryptedText: EncryptedText): String
    
    /**
     * Generates a new secure key in Android Keystore
     * @param keyAlias Unique identifier for the key
     * @return Result of key generation
     */
    suspend fun generateSecureKey(keyAlias: String): KeyGenerationResult
    
    /**
     * Rotates existing encryption keys
     * @param oldKeyAlias Current key alias
     * @param newKeyAlias New key alias
     * @return Result of key rotation
     */
    suspend fun rotateKeys(oldKeyAlias: String, newKeyAlias: String): KeyRotationResult
    
    /**
     * Validates hardware-backed security availability
     * @return Hardware security validation result
     */
    fun validateHardwareSecurity(): HardwareSecurityValidation
    
    /**
     * Gets encryption configuration
     * @return Current encryption configuration
     */
    fun getEncryptionConfig(): EncryptionConfig
}

/**
 * Encrypted data container with metadata
 */
data class EncryptedData(
    val encryptedBytes: ByteArray,
    val metadata: EncryptionMetadata
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as EncryptedData
        
        if (!encryptedBytes.contentEquals(other.encryptedBytes)) return false
        if (metadata != other.metadata) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = encryptedBytes.contentHashCode()
        result = 31 * result + metadata.hashCode()
        return result
    }
}

/**
 * Encrypted text container with metadata
 */
data class EncryptedText(
    val encryptedContent: String,
    val metadata: EncryptionMetadata
)

/**
 * Encryption metadata for secure operations
 */
data class EncryptionMetadata(
    val algorithm: String,
    val keyAlias: String,
    val iv: ByteArray,
    val authTag: ByteArray? = null,
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as EncryptionMetadata
        
        if (algorithm != other.algorithm) return false
        if (keyAlias != other.keyAlias) return false
        if (!iv.contentEquals(other.iv)) return false
        if (authTag != null) {
            if (other.authTag == null) return false
            if (!authTag.contentEquals(other.authTag)) return false
        } else if (other.authTag != null) return false
        if (version != other.version) return false
        if (timestamp != other.timestamp) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = algorithm.hashCode()
        result = 31 * result + keyAlias.hashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + (authTag?.contentHashCode() ?: 0)
        result = 31 * result + version
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * Encryption configuration
 */
data class EncryptionConfig(
    val algorithm: EncryptionAlgorithm = EncryptionAlgorithm.AES_256_GCM,
    val keyDerivation: KeyDerivationFunction = KeyDerivationFunction.PBKDF2,
    val saltLength: Int = 32,
    val iterations: Int = 100000,
    val requireHardwareBacking: Boolean = true
)

enum class EncryptionAlgorithm(val value: String) {
    AES_256_GCM("AES/GCM/NoPadding")
}

enum class KeyDerivationFunction {
    PBKDF2
}

/**
 * Key generation result
 */
sealed class KeyGenerationResult {
    object Success : KeyGenerationResult()
    data class Error(val message: String, val cause: Throwable? = null) : KeyGenerationResult()
}

/**
 * Key rotation result
 */
sealed class KeyRotationResult {
    object Success : KeyRotationResult()
    data class Error(val message: String, val cause: Throwable? = null) : KeyRotationResult()
}

/**
 * Hardware security validation result
 */
data class HardwareSecurityValidation(
    val isHardwareBacked: Boolean,
    val hasSecureHardware: Boolean,
    val supportedAlgorithms: List<String>,
    val securityLevel: SecurityLevel
)

enum class SecurityLevel {
    SOFTWARE,
    TRUSTED_ENVIRONMENT,
    STRONGBOX
}