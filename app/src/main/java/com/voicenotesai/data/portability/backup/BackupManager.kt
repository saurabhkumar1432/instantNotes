package com.voicenotesai.data.portability.backup

import com.voicenotesai.domain.model.EnhancedNote
import com.voicenotesai.domain.portability.BackupResult
import com.voicenotesai.domain.portability.RestoreResult
import java.io.File

/**
 * Interface for backup and restore operations
 */
interface BackupManager {
    /**
     * Create a backup of all notes and optionally audio files
     */
    suspend fun createBackup(
        notes: List<EnhancedNote>,
        includeAudio: Boolean,
        outputFile: File
    ): BackupResult
    
    /**
     * Restore notes from a backup file
     */
    suspend fun restoreBackup(backupFile: File): RestoreResult
    
    /**
     * Validate backup file integrity
     */
    suspend fun validateBackup(backupFile: File): BackupValidationResult
}

/**
 * Result of backup validation
 */
sealed class BackupValidationResult {
    data class Valid(
        val notesCount: Int,
        val audioFilesCount: Int,
        val backupDate: Long,
        val version: String
    ) : BackupValidationResult()
    
    data class Invalid(
        val issues: List<String>
    ) : BackupValidationResult()
}