package com.voicenotesai.data.portability.parsers

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.voicenotesai.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader

/**
 * JSON import parser for notes
 */
class JsonImportParser : ImportParser {
    
    private val gson = Gson()
    
    override suspend fun parse(file: File): ParseResult {
        return withContext(Dispatchers.IO) {
            try {
                val importData = FileReader(file).use { reader ->
                    gson.fromJson(reader, JsonImportData::class.java)
                }
                
                val warnings = mutableListOf<String>()
                
                // Validate version compatibility
                if (importData.metadata?.version != "1.0") {
                    warnings.add("Import data version ${importData.metadata?.version} may not be fully compatible")
                }
                
                val notes = importData.notes?.mapNotNull { jsonNote ->
                    try {
                        jsonNote.toEnhancedNote()
                    } catch (e: Exception) {
                        warnings.add("Failed to parse note ${jsonNote.id}: ${e.message}")
                        null
                    }
                } ?: emptyList()
                
                ParseResult.Success(
                    notes = notes,
                    warnings = warnings
                )
            } catch (e: JsonSyntaxException) {
                ParseResult.Failure(
                    error = "Invalid JSON format: ${e.message}",
                    cause = e
                )
            } catch (e: Exception) {
                ParseResult.Failure(
                    error = "Failed to parse JSON file: ${e.message}",
                    cause = e
                )
            }
        }
    }
    
    override suspend fun validateFormat(file: File): ValidationResult {
        return withContext(Dispatchers.IO) {
            try {
                val content = file.readText()
                
                // Basic JSON validation
                if (!content.trim().startsWith("{") || !content.trim().endsWith("}")) {
                    return@withContext ValidationResult.Invalid(listOf("File does not appear to be valid JSON"))
                }
                
                // Try to parse as JSON
                val importData = gson.fromJson(content, JsonImportData::class.java)
                
                val issues = mutableListOf<String>()
                
                // Validate required fields
                if (importData.notes == null) {
                    issues.add("Missing 'notes' field")
                }
                
                if (importData.metadata == null) {
                    issues.add("Missing 'metadata' field")
                }
                
                // Validate notes structure
                importData.notes?.forEachIndexed { index, note ->
                    if (note.id.isNullOrBlank()) {
                        issues.add("Note at index $index missing ID")
                    }
                    if (note.content.isNullOrBlank()) {
                        issues.add("Note at index $index missing content")
                    }
                    if (note.timestamp == null || note.timestamp <= 0) {
                        issues.add("Note at index $index has invalid timestamp")
                    }
                }
                
                if (issues.isEmpty()) {
                    ValidationResult.Valid
                } else {
                    ValidationResult.Invalid(issues)
                }
            } catch (e: JsonSyntaxException) {
                ValidationResult.Invalid(listOf("Invalid JSON syntax: ${e.message}"))
            } catch (e: Exception) {
                ValidationResult.Invalid(listOf("Failed to validate file: ${e.message}"))
            }
        }
    }
    
    /**
     * Data classes for JSON import
     */
    private data class JsonImportData(
        val metadata: JsonMetadata?,
        val notes: List<JsonNote>?
    )
    
    private data class JsonMetadata(
        val version: String?,
        val exportDate: Long?,
        val totalNotes: Int?,
        val appVersion: String?
    )
    
    private data class JsonNote(
        val id: String?,
        val content: String?,
        val transcribedText: String?,
        val timestamp: Long?,
        val lastModified: Long?,
        val category: String?,
        val tags: List<String>?,
        val entities: List<JsonEntity>?,
        val sentiment: JsonSentiment?,
        val language: JsonLanguage?,
        val audioFingerprint: String?,
        val isArchived: Boolean?,
        val accessLevel: String?,
        val audioFilePath: String?,
        val duration: Long?,
        val fileSize: Long?
    )
    
    private data class JsonEntity(
        val text: String?,
        val type: String?,
        val confidence: Float?,
        val startIndex: Int?,
        val endIndex: Int?
    )
    
    private data class JsonSentiment(
        val score: Float?,
        val confidence: Float?,
        val label: String?
    )
    
    private data class JsonLanguage(
        val code: String?,
        val name: String?,
        val confidence: Float?
    )
    
    /**
     * Convert JsonNote to EnhancedNote
     */
    private fun JsonNote.toEnhancedNote(): EnhancedNote {
        return EnhancedNote(
            id = id ?: throw IllegalArgumentException("Note ID is required"),
            content = content ?: throw IllegalArgumentException("Note content is required"),
            transcribedText = transcribedText ?: "",
            timestamp = timestamp ?: throw IllegalArgumentException("Note timestamp is required"),
            lastModified = lastModified ?: timestamp ?: System.currentTimeMillis(),
            category = category?.let { 
                try { 
                    NoteCategory.valueOf(it) 
                } catch (e: IllegalArgumentException) { 
                    NoteCategory.General 
                }
            } ?: NoteCategory.General,
            tags = tags ?: emptyList(),
            entities = entities?.mapNotNull { entity ->
                try {
                    ExtractedEntity(
                        text = entity.text ?: return@mapNotNull null,
                        type = EntityType.valueOf(entity.type ?: return@mapNotNull null),
                        confidence = entity.confidence ?: 0f,
                        startIndex = entity.startIndex ?: 0,
                        endIndex = entity.endIndex ?: 0
                    )
                } catch (e: IllegalArgumentException) {
                    null
                }
            } ?: emptyList(),
            sentiment = sentiment?.let { s ->
                try {
                    SentimentScore(
                        score = s.score ?: 0f,
                        confidence = s.confidence ?: 0f,
                        label = SentimentLabel.valueOf(s.label ?: "Neutral")
                    )
                } catch (e: IllegalArgumentException) {
                    null
                }
            },
            language = language?.let { l ->
                DetectedLanguage(
                    code = l.code ?: "",
                    name = l.name ?: "",
                    confidence = l.confidence ?: 0f
                )
            },
            audioFingerprint = audioFingerprint,
            isArchived = isArchived ?: false,
            accessLevel = AccessLevel(
                level = try {
                    SecurityLevel.valueOf(accessLevel ?: "Private")
                } catch (e: IllegalArgumentException) {
                    SecurityLevel.Internal
                },
                permissions = setOf(Permission.Read, Permission.Write)
            ),
            audioFilePath = audioFilePath,
            duration = duration,
            fileSize = fileSize
        )
    }
}