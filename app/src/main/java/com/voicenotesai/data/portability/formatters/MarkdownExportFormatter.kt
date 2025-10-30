package com.voicenotesai.data.portability.formatters

import com.voicenotesai.domain.model.EnhancedNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Markdown export formatter for notes
 */
class MarkdownExportFormatter : ExportFormatter {
    
    private val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    
    override suspend fun format(notes: List<EnhancedNote>, outputFile: File): FormatResult {
        return withContext(Dispatchers.IO) {
            try {
                FileWriter(outputFile).use { writer ->
                    // Write document header
                    writer.write("# Voice Notes Export\n\n")
                    writer.write("**Export Date:** ${dateFormat.format(Date())}\n")
                    writer.write("**Total Notes:** ${notes.size}\n\n")
                    writer.write("---\n\n")
                    
                    // Group notes by category
                    val notesByCategory = notes.groupBy { it.category }
                    
                    notesByCategory.forEach { (category, categoryNotes) ->
                        writer.write("## ${category.name} Notes\n\n")
                        
                        categoryNotes.sortedByDescending { it.timestamp }.forEach { note ->
                            writer.write(note.toMarkdown())
                            writer.write("\n---\n\n")
                        }
                    }
                }
                
                FormatResult.Success(
                    file = outputFile,
                    notesFormatted = notes.size,
                    fileSizeBytes = outputFile.length()
                )
            } catch (e: Exception) {
                FormatResult.Failure(
                    error = "Failed to export to Markdown: ${e.message}",
                    cause = e
                )
            }
        }
    }
    
    override fun getFileExtension(): String = "md"
    
    override fun getMimeType(): String = "text/markdown"
    
    /**
     * Convert EnhancedNote to Markdown format
     */
    private fun EnhancedNote.toMarkdown(): String {
        val builder = StringBuilder()
        
        // Title with timestamp
        builder.append("### Note from ${dateFormat.format(Date(timestamp))}\n\n")
        
        // Metadata section
        builder.append("**Category:** ${category.name}\n")
        if (tags.isNotEmpty()) {
            builder.append("**Tags:** ${tags.joinToString(", ") { "`$it`" }}\n")
        }
        if (language != null) {
            builder.append("**Language:** ${language.name} (${language.code})\n")
        }
        if (sentiment != null) {
            builder.append("**Sentiment:** ${sentiment.label.name} (${String.format("%.1f", sentiment.score)})\n")
        }
        if (duration != null) {
            builder.append("**Duration:** ${formatDuration(duration)}\n")
        }
        builder.append("\n")
        
        // Main content
        builder.append("#### Content\n\n")
        builder.append("${content}\n\n")
        
        // Transcribed text if different from content
        if (transcribedText.isNotEmpty() && transcribedText != content) {
            builder.append("#### Transcription\n\n")
            builder.append("${transcribedText}\n\n")
        }
        
        // Entities if present
        if (entities.isNotEmpty()) {
            builder.append("#### Extracted Information\n\n")
            val entitiesByType = entities.groupBy { it.type }
            entitiesByType.forEach { (type, typeEntities) ->
                builder.append("- **${type.name}:** ${typeEntities.joinToString(", ") { it.text }}\n")
            }
            builder.append("\n")
        }
        
        // Footer with metadata
        builder.append("<small>")
        builder.append("*Note ID: `${id}` | ")
        builder.append("Last Modified: ${dateFormat.format(Date(lastModified))}*")
        if (isArchived) {
            builder.append(" | **Archived**")
        }
        builder.append("</small>\n")
        
        return builder.toString()
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