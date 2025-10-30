package com.voicenotesai.data.portability.formatters

import com.voicenotesai.domain.model.EnhancedNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * CSV export formatter for notes
 */
class CsvExportFormatter : ExportFormatter {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    override suspend fun format(notes: List<EnhancedNote>, outputFile: File): FormatResult {
        return withContext(Dispatchers.IO) {
            try {
                FileWriter(outputFile).use { writer ->
                    // Write CSV header
                    writer.write(CSV_HEADER)
                    writer.write("\n")
                    
                    // Write each note as a CSV row
                    notes.forEach { note ->
                        writer.write(note.toCsvRow())
                        writer.write("\n")
                    }
                }
                
                FormatResult.Success(
                    file = outputFile,
                    notesFormatted = notes.size,
                    fileSizeBytes = outputFile.length()
                )
            } catch (e: Exception) {
                FormatResult.Failure(
                    error = "Failed to export to CSV: ${e.message}",
                    cause = e
                )
            }
        }
    }
    
    override fun getFileExtension(): String = "csv"
    
    override fun getMimeType(): String = "text/csv"
    
    companion object {
        private const val CSV_HEADER = "ID,Content,Transcribed Text,Timestamp,Last Modified,Category,Tags,Language,Sentiment,Is Archived,Audio Duration,File Size"
    }
    
    /**
     * Convert EnhancedNote to CSV row
     */
    private fun EnhancedNote.toCsvRow(): String {
        return listOf(
            id.escapeCsv(),
            content.escapeCsv(),
            transcribedText.escapeCsv(),
            dateFormat.format(Date(timestamp)),
            dateFormat.format(Date(lastModified)),
            category.name,
            tags.joinToString(";").escapeCsv(),
            language?.let { "${it.code} (${it.name})" } ?: "",
            sentiment?.let { "${it.label.name} (${String.format("%.2f", it.score)})" } ?: "",
            isArchived.toString(),
            duration?.let { "${it}ms" } ?: "",
            fileSize?.let { "${it} bytes" } ?: ""
        ).joinToString(",")
    }
    
    /**
     * Escape CSV special characters
     */
    private fun String.escapeCsv(): String {
        return if (contains(",") || contains("\"") || contains("\n") || contains("\r")) {
            "\"${replace("\"", "\"\"")}\"" 
        } else {
            this
        }
    }
}