package com.voicenotesai.domain.usecase

import com.voicenotesai.domain.portability.DataPortabilityEngine
import com.voicenotesai.domain.portability.ExportFormat
import com.voicenotesai.domain.portability.ExportResult
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for exporting notes to various formats
 */
@Singleton
class ExportNotesUseCase @Inject constructor(
    private val dataPortabilityEngine: DataPortabilityEngine
) {
    /**
     * Export all notes to the specified format and file
     */
    suspend fun execute(format: ExportFormat, outputFile: File): ExportResult {
        return dataPortabilityEngine.exportNotes(format, outputFile)
    }
}