package com.voicenotesai.domain.security

/**
 * Biometric authentication manager interface
 * Provides fingerprint and face unlock capabilities with secure storage
 */
interface BiometricAuthManager {
    
    /**
     * Authenticates user using available biometric methods
     * @return Authentication result with details
     */
    suspend fun authenticateUser(): AuthenticationResult
    
    /**
     * Sets up biometric authentication for the user
     * @return Setup result with configuration details
     */
    suspend fun setupBiometricAuth(): SetupResult
    
    /**
     * Checks if biometric authentication is available on device
     * @return Biometric capability information
     */
    fun isBiometricAvailable(): BiometricCapability
    
    /**
     * Encrypts data using biometric-protected key
     * @param data Data to encrypt
     * @return Biometric encrypted data
     */
    suspend fun encryptWithBiometric(data: ByteArray): BiometricEncryptedData
    
    /**
     * Decrypts data using biometric authentication
     * @param encryptedData Biometric encrypted data
     * @return Decrypted data after successful authentication
     */
    suspend fun decryptWithBiometric(encryptedData: BiometricEncryptedData): ByteArray
    
    /**
     * Enables or disables biometric authentication
     * @param enabled Whether to enable biometric auth
     * @return Configuration result
     */
    suspend fun setBiometricEnabled(enabled: Boolean): ConfigurationResult
    
    /**
     * Gets current biometric authentication status
     * @return Current biometric status
     */
    fun getBiometricStatus(): BiometricStatus
    
    /**
     * Sets up fallback authentication methods
     * @param methods List of fallback methods to configure
     * @return Fallback setup result
     */
    suspend fun setupFallbackMethods(methods: List<FallbackMethod>): FallbackSetupResult
}

/**
 * Authentication result from biometric verification
 */
sealed class AuthenticationResult {
    object Success : AuthenticationResult()
    object UserCancelled : AuthenticationResult()
    object AuthenticationFailed : AuthenticationResult()
    object BiometricNotAvailable : AuthenticationResult()
    object BiometricNotEnrolled : AuthenticationResult()
    object TooManyAttempts : AuthenticationResult()
    data class Error(val message: String, val errorCode: Int) : AuthenticationResult()
}

/**
 * Setup result for biometric authentication
 */
sealed class SetupResult {
    object Success : SetupResult()
    object BiometricNotSupported : SetupResult()
    object BiometricNotEnrolled : SetupResult()
    object PermissionDenied : SetupResult()
    data class Error(val message: String) : SetupResult()
}

/**
 * Biometric capability information
 */
data class BiometricCapability(
    val isSupported: Boolean,
    val isEnrolled: Boolean,
    val availableTypes: List<BiometricType>,
    val securityLevel: BiometricSecurityLevel,
    val canAuthenticate: Boolean
)

enum class BiometricType {
    FINGERPRINT,
    FACE,
    IRIS,
    VOICE
}

enum class BiometricSecurityLevel {
    NONE,
    WEAK,
    STRONG
}

/**
 * Biometric encrypted data container
 */
data class BiometricEncryptedData(
    val encryptedBytes: ByteArray,
    val keyAlias: String,
    val iv: ByteArray,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as BiometricEncryptedData
        
        if (!encryptedBytes.contentEquals(other.encryptedBytes)) return false
        if (keyAlias != other.keyAlias) return false
        if (!iv.contentEquals(other.iv)) return false
        if (timestamp != other.timestamp) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = encryptedBytes.contentHashCode()
        result = 31 * result + keyAlias.hashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * Configuration result for biometric settings
 */
sealed class ConfigurationResult {
    object Success : ConfigurationResult()
    data class Error(val message: String) : ConfigurationResult()
}

/**
 * Current biometric authentication status
 */
data class BiometricStatus(
    val isEnabled: Boolean,
    val isConfigured: Boolean,
    val lastAuthenticationTime: Long?,
    val failedAttempts: Int,
    val isLocked: Boolean,
    val lockoutEndTime: Long?
)

/**
 * Fallback authentication methods
 */
enum class FallbackMethod {
    PIN,
    PASSWORD,
    PATTERN,
    SECURITY_QUESTIONS
}

/**
 * Fallback setup result
 */
sealed class FallbackSetupResult {
    object Success : FallbackSetupResult()
    data class PartialSuccess(val configuredMethods: List<FallbackMethod>) : FallbackSetupResult()
    data class Error(val message: String, val failedMethods: List<FallbackMethod>) : FallbackSetupResult()
}