package com.voicenotesai.domain.usecase

import com.voicenotesai.domain.portability.BackupResult
import com.voicenotesai.domain.portability.DataPortabilityEngine
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for creating backups of all notes and audio files
 */
@Singleton
class CreateBackupUseCase @Inject constructor(
    private val dataPortabilityEngine: DataPortabilityEngine
) {
    /**
     * Create a backup with optional audio file inclusion
     */
    suspend fun execute(includeAudio: Boolean = false, outputFile: File): BackupResult {
        return dataPortabilityEngine.createBackup(includeAudio, outputFile)
    }
}