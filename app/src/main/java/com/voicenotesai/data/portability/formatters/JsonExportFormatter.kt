package com.voicenotesai.data.portability.formatters

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.voicenotesai.domain.model.EnhancedNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * JSON export formatter for notes
 */
class JsonExportFormatter : ExportFormatter {
    
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create()
    
    override suspend fun format(notes: List<EnhancedNote>, outputFile: File): FormatResult {
        return withContext(Dispatchers.IO) {
            try {
                val exportData = JsonExportData(
                    metadata = ExportMetadata(
                        version = "1.0",
                        exportDate = System.currentTimeMillis(),
                        totalNotes = notes.size,
                        appVersion = "1.0.0"
                    ),
                    notes = notes.map { it.toJsonNote() }
                )
                
                FileWriter(outputFile).use { writer ->
                    gson.toJson(exportData, writer)
                }
                
                FormatResult.Success(
                    file = outputFile,
                    notesFormatted = notes.size,
                    fileSizeBytes = outputFile.length()
                )
            } catch (e: Exception) {
                FormatResult.Failure(
                    error = "Failed to export to JSON: ${e.message}",
                    cause = e
                )
            }
        }
    }
    
    override fun getFileExtension(): String = "json"
    
    override fun getMimeType(): String = "application/json"
    
    /**
     * Data structure for JSON export
     */
    private data class JsonExportData(
        val metadata: ExportMetadata,
        val notes: List<JsonNote>
    )
    
    private data class ExportMetadata(
        val version: String,
        val exportDate: Long,
        val totalNotes: Int,
        val appVersion: String
    )
    
    private data class JsonNote(
        val id: String,
        val content: String,
        val transcribedText: String,
        val timestamp: Long,
        val lastModified: Long,
        val category: String,
        val tags: List<String>,
        val entities: List<JsonEntity>,
        val sentiment: JsonSentiment?,
        val language: JsonLanguage?,
        val audioFingerprint: String?,
        val isArchived: Boolean,
        val accessLevel: String,
        val audioFilePath: String?,
        val duration: Long?,
        val fileSize: Long?
    )
    
    private data class JsonEntity(
        val text: String,
        val type: String,
        val confidence: Float,
        val startIndex: Int,
        val endIndex: Int
    )
    
    private data class JsonSentiment(
        val score: Float,
        val confidence: Float,
        val label: String
    )
    
    private data class JsonLanguage(
        val code: String,
        val name: String,
        val confidence: Float
    )
    
    /**
     * Convert EnhancedNote to JsonNote
     */
    private fun EnhancedNote.toJsonNote(): JsonNote {
        return JsonNote(
            id = id,
            content = content,
            transcribedText = transcribedText,
            timestamp = timestamp,
            lastModified = lastModified,
            category = category.name,
            tags = tags,
            entities = entities.map { entity ->
                JsonEntity(
                    text = entity.text,
                    type = entity.type.name,
                    confidence = entity.confidence,
                    startIndex = entity.startIndex,
                    endIndex = entity.endIndex
                )
            },
            sentiment = sentiment?.let { 
                JsonSentiment(
                    score = it.score,
                    confidence = it.confidence,
                    label = it.label.name
                )
            },
            language = language?.let {
                JsonLanguage(
                    code = it.code,
                    name = it.name,
                    confidence = it.confidence
                )
            },
            audioFingerprint = audioFingerprint,
            isArchived = isArchived,
            accessLevel = accessLevel.level.name,
            audioFilePath = audioFilePath,
            duration = duration,
            fileSize = fileSize
        )
    }
}