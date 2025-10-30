package com.voicenotesai.data.portability.backup

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.voicenotesai.domain.model.EnhancedNote
import com.voicenotesai.domain.portability.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * ZIP-based backup manager implementation
 */
class ZipBackupManager : BackupManager {
    
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create()
    
    override suspend fun createBackup(
        notes: List<EnhancedNote>,
        includeAudio: Boolean,
        outputFile: File
    ): BackupResult {
        return withContext(Dispatchers.IO) {
            try {
                val audioFilesIncluded = mutableListOf<String>()
                
                ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
                    // Create backup metadata
                    val metadata = BackupMetadata(
                        version = "1.0",
                        createdAt = System.currentTimeMillis(),
                        notesCount = notes.size,
                        includesAudio = includeAudio,
                        appVersion = "1.0.0"
                    )
                    
                    // Add metadata file
                    zipOut.putNextEntry(ZipEntry("metadata.json"))
                    zipOut.write(gson.toJson(metadata).toByteArray())
                    zipOut.closeEntry()
                    
                    // Add notes data
                    zipOut.putNextEntry(ZipEntry("notes.json"))
                    val notesData = NotesBackupData(
                        notes = notes.map { it.toBackupNote() }
                    )
                    zipOut.write(gson.toJson(notesData).toByteArray())
                    zipOut.closeEntry()
                    
                    // Add audio files if requested
                    if (includeAudio) {
                        notes.forEach { note ->
                            note.audioFilePath?.let { audioPath ->
                                val audioFile = File(audioPath)
                                if (audioFile.exists()) {
                                    try {
                                        zipOut.putNextEntry(ZipEntry("audio/${note.id}.${audioFile.extension}"))
                                        FileInputStream(audioFile).use { audioInput ->
                                            audioInput.copyTo(zipOut)
                                        }
                                        zipOut.closeEntry()
                                        audioFilesIncluded.add(note.id)
                                    } catch (e: Exception) {
                                        // Log error but continue with backup
                                    }
                                }
                            }
                        }
                    }
                    
                    // Add checksum file
                    val checksum = calculateBackupChecksum(notes, audioFilesIncluded)
                    zipOut.putNextEntry(ZipEntry("checksum.txt"))
                    zipOut.write(checksum.toByteArray())
                    zipOut.closeEntry()
                }
                
                BackupResult.Success(
                    backupFile = outputFile,
                    notesBackedUp = notes.size,
                    audioFilesIncluded = audioFilesIncluded.size,
                    totalSizeBytes = outputFile.length()
                )
            } catch (e: Exception) {
                BackupResult.Failure(
                    error = BackupError.COMPRESSION_FAILED,
                    message = "Failed to create backup: ${e.message}"
                )
            }
        }
    }
    
    override suspend fun restoreBackup(backupFile: File): RestoreResult {
        return withContext(Dispatchers.IO) {
            try {
                var metadata: BackupMetadata? = null
                var notesData: NotesBackupData? = null
                val audioFiles = mutableMapOf<String, ByteArray>()
                var checksum: String? = null
                
                // Extract backup contents
                ZipInputStream(FileInputStream(backupFile)).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        when {
                            entry.name == "metadata.json" -> {
                                metadata = gson.fromJson(zipIn.reader(), BackupMetadata::class.java)
                            }
                            entry.name == "notes.json" -> {
                                notesData = gson.fromJson(zipIn.reader(), NotesBackupData::class.java)
                            }
                            entry.name.startsWith("audio/") -> {
                                val noteId = entry.name.substringAfter("audio/").substringBefore(".")
                                audioFiles[noteId] = zipIn.readBytes()
                            }
                            entry.name == "checksum.txt" -> {
                                checksum = zipIn.reader().readText()
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
                
                // Validate backup
                if (metadata == null || notesData == null) {
                    return@withContext RestoreResult.Failure(
                        error = RestoreError.BACKUP_CORRUPTED,
                        message = "Backup file is missing required components"
                    )
                }
                
                // Validate checksum if present
                val notesDataSafe = notesData
                if (notesDataSafe == null) {
                    return@withContext RestoreResult.Failure(
                        error = RestoreError.BACKUP_CORRUPTED,
                        message = "Backup file is missing notes data"
                    )
                }
                
                val validationPassed = checksum?.let { expectedChecksum ->
                    val actualChecksum = calculateBackupChecksum(
                        notesDataSafe.notes.map { it.toEnhancedNote() },
                        audioFiles.keys.toList()
                    )
                    actualChecksum == expectedChecksum
                } ?: true
                
                RestoreResult.Success(
                    notesRestored = notesDataSafe.notes.size,
                    audioFilesRestored = audioFiles.size,
                    validationPassed = validationPassed
                )
            } catch (e: Exception) {
                RestoreResult.Failure(
                    error = RestoreError.EXTRACTION_FAILED,
                    message = "Failed to restore backup: ${e.message}"
                )
            }
        }
    }
    
    override suspend fun validateBackup(backupFile: File): BackupValidationResult {
        return withContext(Dispatchers.IO) {
            try {
                var metadata: BackupMetadata? = null
                var notesCount = 0
                var audioFilesCount = 0
                
                ZipInputStream(FileInputStream(backupFile)).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        when {
                            entry.name == "metadata.json" -> {
                                metadata = gson.fromJson(zipIn.reader(), BackupMetadata::class.java)
                            }
                            entry.name == "notes.json" -> {
                                val notesData = gson.fromJson(zipIn.reader(), NotesBackupData::class.java)
                                notesCount = notesData.notes.size
                            }
                            entry.name.startsWith("audio/") -> {
                                audioFilesCount++
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
                
                val metadataSafe = metadata
                if (metadataSafe == null) {
                    BackupValidationResult.Invalid(listOf("Missing backup metadata"))
                } else {
                    BackupValidationResult.Valid(
                        notesCount = notesCount,
                        audioFilesCount = audioFilesCount,
                        backupDate = metadataSafe.createdAt,
                        version = metadataSafe.version
                    )
                }
            } catch (e: Exception) {
                BackupValidationResult.Invalid(listOf("Failed to validate backup: ${e.message}"))
            }
        }
    }
    
    /**
     * Calculate checksum for backup validation
     */
    private fun calculateBackupChecksum(notes: List<EnhancedNote>, audioFileIds: List<String>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        
        // Add notes data to checksum
        notes.sortedBy { it.id }.forEach { note ->
            digest.update(note.id.toByteArray())
            digest.update(note.content.toByteArray())
            digest.update(note.timestamp.toString().toByteArray())
        }
        
        // Add audio file IDs to checksum
        audioFileIds.sorted().forEach { audioId ->
            digest.update(audioId.toByteArray())
        }
        
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Data classes for backup
     */
    private data class BackupMetadata(
        val version: String,
        val createdAt: Long,
        val notesCount: Int,
        val includesAudio: Boolean,
        val appVersion: String
    )
    
    private data class NotesBackupData(
        val notes: List<BackupNote>
    )
    
    private data class BackupNote(
        val id: String,
        val content: String,
        val transcribedText: String,
        val timestamp: Long,
        val lastModified: Long,
        val category: String,
        val tags: List<String>,
        val entities: List<BackupEntity>,
        val sentiment: BackupSentiment?,
        val language: BackupLanguage?,
        val audioFingerprint: String?,
        val isArchived: Boolean,
        val accessLevel: String,
        val audioFilePath: String?,
        val duration: Long?,
        val fileSize: Long?
    )
    
    private data class BackupEntity(
        val text: String,
        val type: String,
        val confidence: Float,
        val startIndex: Int,
        val endIndex: Int
    )
    
    private data class BackupSentiment(
        val score: Float,
        val confidence: Float,
        val label: String
    )
    
    private data class BackupLanguage(
        val code: String,
        val name: String,
        val confidence: Float
    )
    
    /**
     * Extension functions for conversion
     */
    private fun EnhancedNote.toBackupNote(): BackupNote {
        return BackupNote(
            id = id,
            content = content,
            transcribedText = transcribedText,
            timestamp = timestamp,
            lastModified = lastModified,
            category = category.name,
            tags = tags,
            entities = entities.map { entity ->
                BackupEntity(
                    text = entity.text,
                    type = entity.type.name,
                    confidence = entity.confidence,
                    startIndex = entity.startIndex,
                    endIndex = entity.endIndex
                )
            },
            sentiment = sentiment?.let {
                BackupSentiment(
                    score = it.score,
                    confidence = it.confidence,
                    label = it.label.name
                )
            },
            language = language?.let {
                BackupLanguage(
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
    
    private fun BackupNote.toEnhancedNote(): EnhancedNote {
        return EnhancedNote(
            id = id,
            content = content,
            transcribedText = transcribedText,
            timestamp = timestamp,
            lastModified = lastModified,
            category = try { 
                com.voicenotesai.domain.model.NoteCategory.valueOf(category) 
            } catch (e: IllegalArgumentException) { 
                com.voicenotesai.domain.model.NoteCategory.General 
            },
            tags = tags,
            entities = entities.map { entity ->
                com.voicenotesai.domain.model.ExtractedEntity(
                    text = entity.text,
                    type = try {
                        com.voicenotesai.domain.model.EntityType.valueOf(entity.type)
                    } catch (e: IllegalArgumentException) {
                        com.voicenotesai.domain.model.EntityType.Person
                    },
                    confidence = entity.confidence,
                    startIndex = entity.startIndex,
                    endIndex = entity.endIndex
                )
            },
            sentiment = sentiment?.let {
                com.voicenotesai.domain.model.SentimentScore(
                    score = it.score,
                    confidence = it.confidence,
                    label = try {
                        com.voicenotesai.domain.model.SentimentLabel.valueOf(it.label)
                    } catch (e: IllegalArgumentException) {
                        com.voicenotesai.domain.model.SentimentLabel.Neutral
                    }
                )
            },
            language = language?.let {
                com.voicenotesai.domain.model.DetectedLanguage(
                    code = it.code,
                    name = it.name,
                    confidence = it.confidence
                )
            },
            audioFingerprint = audioFingerprint,
            isArchived = isArchived,
            accessLevel = com.voicenotesai.domain.model.AccessLevel(
                level = try {
                    com.voicenotesai.domain.model.SecurityLevel.valueOf(accessLevel)
                } catch (e: IllegalArgumentException) {
                    com.voicenotesai.domain.model.SecurityLevel.Internal
                },
                permissions = setOf(
                    com.voicenotesai.domain.model.Permission.Read,
                    com.voicenotesai.domain.model.Permission.Write
                )
            ),
            audioFilePath = audioFilePath,
            duration = duration,
            fileSize = fileSize
        )
    }
}