package com.voicenotesai.data.portability.formatters

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.properties.TextAlignment
import com.voicenotesai.domain.model.EnhancedNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * PDF export formatter for notes
 */
class PdfExportFormatter : ExportFormatter {
    
    private val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    
    override suspend fun format(notes: List<EnhancedNote>, outputFile: File): FormatResult {
        return withContext(Dispatchers.IO) {
            try {
                val pdfWriter = PdfWriter(outputFile)
                val pdfDocument = PdfDocument(pdfWriter)
                val document = Document(pdfDocument)
                
                // Document title
                document.add(
                    Paragraph("Voice Notes Export")
                        .setFontSize(24f)
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(20f)
                )
                
                // Export metadata
                document.add(
                    Paragraph()
                        .add(Text("Export Date: ").setBold())
                        .add(Text(dateFormat.format(Date())))
                        .setMarginBottom(5f)
                )
                
                document.add(
                    Paragraph()
                        .add(Text("Total Notes: ").setBold())
                        .add(Text(notes.size.toString()))
                        .setMarginBottom(20f)
                )
                
                // Group notes by category
                val notesByCategory = notes.groupBy { it.category }
                
                notesByCategory.forEach { (category, categoryNotes) ->
                    // Category header
                    document.add(
                        Paragraph("${category.name} Notes")
                            .setFontSize(18f)
                            .setBold()
                            .setMarginTop(20f)
                            .setMarginBottom(10f)
                    )
                    
                    // Notes in category
                    categoryNotes.sortedByDescending { it.timestamp }.forEach { note ->
                        document.add(note.toPdfContent())
                    }
                }
                
                document.close()
                
                FormatResult.Success(
                    file = outputFile,
                    notesFormatted = notes.size,
                    fileSizeBytes = outputFile.length()
                )
            } catch (e: Exception) {
                FormatResult.Failure(
                    error = "Failed to export to PDF: ${e.message}",
                    cause = e
                )
            }
        }
    }
    
    override fun getFileExtension(): String = "pdf"
    
    override fun getMimeType(): String = "application/pdf"
    
    /**
     * Convert EnhancedNote to PDF content
     */
    private fun EnhancedNote.toPdfContent(): Paragraph {
        val container = Paragraph()
            .setMarginBottom(15f)
            .setMarginTop(10f)
        
        // Note header with timestamp
        container.add(
            Paragraph("Note from ${dateFormat.format(Date(timestamp))}")
                .setFontSize(14f)
                .setBold()
                .setMarginBottom(5f)
        )
        
        // Metadata
        val metadata = mutableListOf<String>()
        metadata.add("Category: ${category.name}")
        if (tags.isNotEmpty()) {
            metadata.add("Tags: ${tags.joinToString(", ")}")
        }
        if (language != null) {
            metadata.add("Language: ${language.name}")
        }
        if (sentiment != null) {
            metadata.add("Sentiment: ${sentiment.label.name} (${String.format("%.1f", sentiment.score)})")
        }
        if (duration != null) {
            metadata.add("Duration: ${formatDuration(duration)}")
        }
        
        container.add(
            Paragraph(metadata.joinToString(" | "))
                .setFontSize(10f)
                .setItalic()
                .setMarginBottom(8f)
        )
        
        // Main content
        container.add(
            Paragraph(content)
                .setFontSize(12f)
                .setMarginBottom(5f)
        )
        
        // Transcribed text if different
        if (transcribedText.isNotEmpty() && transcribedText != content) {
            container.add(
                Paragraph()
                    .add(Text("Transcription: ").setBold().setFontSize(10f))
                    .add(Text(transcribedText).setFontSize(10f))
                    .setMarginBottom(5f)
            )
        }
        
        // Entities
        if (entities.isNotEmpty()) {
            val entitiesByType = entities.groupBy { it.type }
            val entitiesText = entitiesByType.map { (type, typeEntities) ->
                "${type.name}: ${typeEntities.joinToString(", ") { it.text }}"
            }.joinToString(" | ")
            
            container.add(
                Paragraph()
                    .add(Text("Extracted: ").setBold().setFontSize(9f))
                    .add(Text(entitiesText).setFontSize(9f))
                    .setMarginBottom(5f)
            )
        }
        
        // Footer
        container.add(
            Paragraph("ID: $id | Last Modified: ${dateFormat.format(Date(lastModified))}")
                .setFontSize(8f)
                .setItalic()
        )
        
        return container
    }
    
    /**
     * Format duration in milliseconds to human readable format
     */
    private fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        
        return if (minutes > 0) {
            "${minutes}m ${remainingSeconds}s"
        } else {
            "${remainingSeconds}s"
        }
    }
}