package com.voicenotesai.data.portability.formatters

import com.voicenotesai.domain.model.EnhancedNote
import java.io.File

/**
 * Interface for formatting notes into different export formats
 */
interface ExportFormatter {
    /**
     * Format notes and write to the specified file
     */
    suspend fun format(notes: List<EnhancedNote>, outputFile: File): FormatResult
    
    /**
     * Get the file extension for this format
     */
    fun getFileExtension(): String
    
    /**
     * Get the MIME type for this format
     */
    fun getMimeType(): String
}

/**
 * Result of formatting operation
 */
sealed class FormatResult {
    data class Success(
        val file: File,
        val notesFormatted: Int,
        val fileSizeBytes: Long
    ) : FormatResult()
    
    data class Failure(
        val error: String,
        val cause: Throwable? = null
    ) : FormatResult()
}