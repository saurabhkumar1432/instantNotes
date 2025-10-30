package com.voicenotesai.domain.usecase

import com.voicenotesai.domain.portability.DataPortabilityEngine
import com.voicenotesai.domain.portability.ImportData
import com.voicenotesai.domain.portability.ImportResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for importing notes from various formats
 */
@Singleton
class ImportNotesUseCase @Inject constructor(
    private val dataPortabilityEngine: DataPortabilityEngine
) {
    /**
     * Import notes from the provided data
     */
    suspend fun execute(importData: ImportData): ImportResult {
        return dataPortabilityEngine.importNotes(importData)
    }
}