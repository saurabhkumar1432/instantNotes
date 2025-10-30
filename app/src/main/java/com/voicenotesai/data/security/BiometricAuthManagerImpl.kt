package com.voicenotesai.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.voicenotesai.domain.security.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Implementation of BiometricAuthManager using AndroidX Biometric library
 * Provides fingerprint and face unlock with secure storage
 */
@Singleton
class BiometricAuthManagerImpl @Inject constructor(
    private val context: Context
) : BiometricAuthManager {
    
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val BIOMETRIC_KEY_ALIAS = "voice_notes_biometric_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
        private const val KEY_SIZE = 256
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 30 * 60 * 1000L // 30 minutes
    }
    
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    }
    
    private val biometricManager: BiometricManager by lazy {
        BiometricManager.from(context)
    }
    
    private val secureRandom = SecureRandom()
    
    // Simple in-memory storage for demo - in production, use encrypted SharedPreferences
    private var biometricStatus = BiometricStatus(
        isEnabled = false,
        isConfigured = false,
        lastAuthenticationTime = null,
        failedAttempts = 0,
        isLocked = false,
        lockoutEndTime = null
    )
    
    override suspend fun authenticateUser(): AuthenticationResult = withContext(Dispatchers.Main) {
        // Check if biometric is available
        val capability = isBiometricAvailable()
        if (!capability.canAuthenticate) {
            return@withContext when {
                !capability.isSupported -> AuthenticationResult.BiometricNotAvailable
                !capability.isEnrolled -> AuthenticationResult.BiometricNotEnrolled
                else -> AuthenticationResult.BiometricNotAvailable
            }
        }
        
        // Check if locked due to too many attempts
        if (biometricStatus.isLocked) {
            val currentTime = System.currentTimeMillis()
            if (biometricStatus.lockoutEndTime != null && currentTime < biometricStatus.lockoutEndTime!!) {
                return@withContext AuthenticationResult.TooManyAttempts
            } else {
                // Unlock if lockout period has passed
                biometricStatus = biometricStatus.copy(
                    isLocked = false,
                    lockoutEndTime = null,
                    failedAttempts = 0
                )
            }
        }
        
        return@withContext suspendCancellableCoroutine { continuation ->
            val activity = context as? FragmentActivity
                ?: return@suspendCancellableCoroutine continuation.resume(
                    AuthenticationResult.Error("Context is not a FragmentActivity", -1)
                )
            
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        val result = when (errorCode) {
                            BiometricPrompt.ERROR_USER_CANCELED -> AuthenticationResult.UserCancelled
                            BiometricPrompt.ERROR_CANCELED -> AuthenticationResult.UserCancelled
                            BiometricPrompt.ERROR_LOCKOUT,
                            BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                                handleFailedAttempt()
                                AuthenticationResult.TooManyAttempts
                            }
                            else -> AuthenticationResult.Error(errString.toString(), errorCode)
                        }
                        continuation.resume(result)
                    }
                    
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        // Reset failed attempts on success
                        biometricStatus = biometricStatus.copy(
                            lastAuthenticationTime = System.currentTimeMillis(),
                            failedAttempts = 0,
                            isLocked = false,
                            lockoutEndTime = null
                        )
                        continuation.resume(AuthenticationResult.Success)
                    }
                    
                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        handleFailedAttempt()
                        continuation.resume(AuthenticationResult.AuthenticationFailed)
                    }
                }
            )
            
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Use your biometric credential to access Voice Notes")
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()
            
            biometricPrompt.authenticate(promptInfo)
        }
    }
    
    override suspend fun setupBiometricAuth(): SetupResult = withContext(Dispatchers.IO) {
        try {
            val capability = isBiometricAvailable()
            
            when {
                !capability.isSupported -> SetupResult.BiometricNotSupported
                !capability.isEnrolled -> SetupResult.BiometricNotEnrolled
                else -> {
                    // Generate biometric-protected key
                    generateBiometricKey()
                    biometricStatus = biometricStatus.copy(
                        isEnabled = true,
                        isConfigured = true
                    )
                    SetupResult.Success
                }
            }
        } catch (e: Exception) {
            SetupResult.Error("Failed to setup biometric authentication: ${e.message}")
        }
    }
    
    override fun isBiometricAvailable(): BiometricCapability {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
        val canAuthenticate = biometricManager.canAuthenticate(authenticators)
        
        val isSupported = canAuthenticate != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
        val isEnrolled = canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
        
        val availableTypes = mutableListOf<BiometricType>()
        if (isSupported) {
            // In a real implementation, you would check specific biometric types
            availableTypes.add(BiometricType.FINGERPRINT)
            // Add other types based on device capabilities
        }
        
        return BiometricCapability(
            isSupported = isSupported,
            isEnrolled = isEnrolled,
            availableTypes = availableTypes,
            securityLevel = if (isSupported) BiometricSecurityLevel.STRONG else BiometricSecurityLevel.NONE,
            canAuthenticate = canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
        )
    }
    
    override suspend fun encryptWithBiometric(data: ByteArray): BiometricEncryptedData = withContext(Dispatchers.IO) {
        if (!keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
            generateBiometricKey()
        }
        
        val secretKey = keyStore.getKey(BIOMETRIC_KEY_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance(TRANSFORMATION)
        
        // Generate random IV
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)
        
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        
        val encryptedBytes = cipher.doFinal(data)
        
        BiometricEncryptedData(
            encryptedBytes = encryptedBytes,
            keyAlias = BIOMETRIC_KEY_ALIAS,
            iv = iv
        )
    }
    
    override suspend fun decryptWithBiometric(encryptedData: BiometricEncryptedData): ByteArray = withContext(Dispatchers.IO) {
        // First authenticate user
        val authResult = authenticateUser()
        if (authResult != AuthenticationResult.Success) {
            throw SecurityException("Biometric authentication failed: $authResult")
        }
        
        val secretKey = keyStore.getKey(encryptedData.keyAlias, null) as SecretKey
            ?: throw SecurityException("Biometric key not found: ${encryptedData.keyAlias}")
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, encryptedData.iv)
        
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        
        return@withContext cipher.doFinal(encryptedData.encryptedBytes)
    }
    
    override suspend fun setBiometricEnabled(enabled: Boolean): ConfigurationResult = withContext(Dispatchers.IO) {
        try {
            if (enabled && !isBiometricAvailable().canAuthenticate) {
                return@withContext ConfigurationResult.Error("Biometric authentication not available")
            }
            
            biometricStatus = biometricStatus.copy(isEnabled = enabled)
            ConfigurationResult.Success
        } catch (e: Exception) {
            ConfigurationResult.Error("Failed to configure biometric authentication: ${e.message}")
        }
    }
    
    override fun getBiometricStatus(): BiometricStatus = biometricStatus
    
    override suspend fun setupFallbackMethods(methods: List<FallbackMethod>): FallbackSetupResult = withContext(Dispatchers.IO) {
        try {
            // In a real implementation, this would configure actual fallback methods
            // For now, we'll just simulate success
            val configuredMethods = methods.filter { method ->
                when (method) {
                    FallbackMethod.PIN,
                    FallbackMethod.PASSWORD,
                    FallbackMethod.PATTERN -> true
                    FallbackMethod.SECURITY_QUESTIONS -> false // Not implemented in this demo
                }
            }
            
            if (configuredMethods.size == methods.size) {
                FallbackSetupResult.Success
            } else {
                val failedMethods = methods - configuredMethods.toSet()
                FallbackSetupResult.PartialSuccess(configuredMethods)
            }
        } catch (e: Exception) {
            FallbackSetupResult.Error("Failed to setup fallback methods: ${e.message}", methods)
        }
    }
    
    /**
     * Generates a biometric-protected key in Android Keystore
     */
    private fun generateBiometricKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            BIOMETRIC_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationValidityDurationSeconds(-1) // Require auth for every use
            .setRandomizedEncryptionRequired(true)
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }
    
    /**
     * Handles failed authentication attempts
     */
    private fun handleFailedAttempt() {
        val newFailedAttempts = biometricStatus.failedAttempts + 1
        val isLocked = newFailedAttempts >= MAX_FAILED_ATTEMPTS
        val lockoutEndTime = if (isLocked) {
            System.currentTimeMillis() + LOCKOUT_DURATION_MS
        } else null
        
        biometricStatus = biometricStatus.copy(
            failedAttempts = newFailedAttempts,
            isLocked = isLocked,
            lockoutEndTime = lockoutEndTime
        )
    }
}