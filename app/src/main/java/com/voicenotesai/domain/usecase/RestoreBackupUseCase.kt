package com.voicenotesai.domain.usecase

import com.voicenotesai.domain.portability.DataPortabilityEngine
import com.voicenotesai.domain.portability.RestoreResult
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for restoring notes from backup files
 */
@Singleton
class RestoreBackupUseCase @Inject constructor(
    private val dataPortabilityEngine: DataPortabilityEngine
) {
    /**
     * Restore notes and audio files from a backup
     */
    suspend fun execute(backupFile: File): RestoreResult {
        return dataPortabilityEngine.restoreBackup(backupFile)
    }
}