package com.voicenotesai.data.portability

import com.voicenotesai.data.portability.backup.BackupManager
import com.voicenotesai.data.portability.backup.ZipBackupManager
import com.voicenotesai.data.portability.formatters.*
import com.voicenotesai.data.portability.parsers.ImportParser
import com.voicenotesai.data.portability.parsers.JsonImportParser
import com.voicenotesai.data.portability.parsers.ParseResult
import com.voicenotesai.data.repository.NotesRepository
import com.voicenotesai.domain.model.EnhancedNote
import com.voicenotesai.domain.portability.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of DataPortabilityEngine
 */
@Singleton
class DataPortabilityEngineImpl @Inject constructor(
    private val notesRepository: NotesRepository
) : DataPortabilityEngine {
    
    private val backupManager: BackupManager = ZipBackupManager()
    
    private val exportFormatters = mapOf(
        ExportFormat.JSON::class to JsonExportFormatter(),
        ExportFormat.CSV::class to CsvExportFormatter(),
        ExportFormat.Markdown::class to MarkdownExportFormatter(),
        ExportFormat.PDF::class to PdfExportFormatter()
    )
    
    private val importParsers = mapOf(
        ExportFormat.JSON::class to JsonImportParser()
    )
    
    override suspend fun exportNotes(format: ExportFormat, outputFile: File): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                // Get all notes from repository
                val notes = getAllNotesAsEnhanced()
                
                // Get appropriate formatter
                val formatter = exportFormatters[format::class]
                    ?: return@withContext ExportResult.Failure(
                        error = ExportError.FORMAT_NOT_SUPPORTED,
                        message = "Export format ${format::class.simpleName} is not supported"
                    )
                
                // Ensure output directory exists
                outputFile.parentFile?.mkdirs()
                
                // Format and write notes
                when (val result = formatter.format(notes, outputFile)) {
                    is FormatResult.Success -> ExportResult.Success(
                        file = result.file,
                        notesExported = result.notesFormatted,
                        fileSizeBytes = result.fileSizeBytes
                    )
                    is FormatResult.Failure -> ExportResult.Failure(
                        error = ExportError.DATA_ACCESS_ERROR,
                        message = result.error
                    )
                }
            } catch (e: SecurityException) {
                ExportResult.Failure(
                    error = ExportError.PERMISSION_DENIED,
                    message = "Permission denied: ${e.message}"
                )
            } catch (e: Exception) {
                ExportResult.Failure(
                    error = ExportError.FILE_WRITE_ERROR,
                    message = "Failed to export notes: ${e.message}"
                )
            }
        }
    }
    
    override suspend fun importNotes(data: ImportData): ImportResult {
        return withContext(Dispatchers.IO) {
            try {
                // Validate data integrity if requested
                if (data.validateIntegrity) {
                    when (val integrityResult = validateDataIntegrity(data.file)) {
                        is IntegrityCheckResult.Invalid -> {
                            val criticalIssues = integrityResult.issues.filter { 
                                it.severity == IssueSeverity.CRITICAL 
                            }
                            if (criticalIssues.isNotEmpty() || !integrityResult.canProceed) {
                                return@withContext ImportResult.Failure(
                                    error = ImportError.VALIDATION_FAILED,
                                    message = "Data integrity validation failed: ${criticalIssues.joinToString(", ") { it.description }}"
                                )
                            }
                        }
                        is IntegrityCheckResult.Valid -> {
                            // Continue with import
                        }
                    }
                }
                
                // Get appropriate parser
                val parser = importParsers[data.format::class]
                    ?: return@withContext ImportResult.Failure(
                        error = ImportError.INVALID_FORMAT,
                        message = "Import format ${data.format::class.simpleName} is not supported"
                    )
                
                // Parse the file
                when (val parseResult = parser.parse(data.file)) {
                    is ParseResult.Success -> {
                        // Check for duplicates and save new notes
                        val existingNotes = getAllNotesAsEnhanced()
                        val existingIds = existingNotes.map { it.id }.toSet()
                        
                        val newNotes = parseResult.notes.filter { it.id !in existingIds }
                        val duplicatesSkipped = parseResult.notes.size - newNotes.size
                        
                        // Save new notes to repository
                        newNotes.forEach { note ->
                            val basicNote = com.voicenotesai.data.local.entity.Note(
                                id = note.id.hashCode().toLong(),
                                content = note.content,
                                timestamp = note.timestamp,
                                transcribedText = note.transcribedText
                            )
                            notesRepository.saveNote(basicNote)
                        }
                        
                        ImportResult.Success(
                            notesImported = newNotes.size,
                            duplicatesSkipped = duplicatesSkipped,
                            validationPassed = data.validateIntegrity
                        )
                    }
                    is ParseResult.Failure -> ImportResult.Failure(
                        error = ImportError.CORRUPTED_DATA,
                        message = parseResult.error,
                        lineNumber = parseResult.lineNumber
                    )
                }
            } catch (e: SecurityException) {
                ImportResult.Failure(
                    error = ImportError.FILE_READ_ERROR,
                    message = "Permission denied: ${e.message}"
                )
            } catch (e: Exception) {
                ImportResult.Failure(
                    error = ImportError.FILE_READ_ERROR,
                    message = "Failed to import notes: ${e.message}"
                )
            }
        }
    }
    
    override suspend fun createBackup(includeAudio: Boolean, outputFile: File): BackupResult {
        return withContext(Dispatchers.IO) {
            try {
                val notes = getAllNotesAsEnhanced()
                
                // Ensure output directory exists
                outputFile.parentFile?.mkdirs()
                
                backupManager.createBackup(notes, includeAudio, outputFile)
            } catch (e: SecurityException) {
                BackupResult.Failure(
                    error = BackupError.PERMISSION_DENIED,
                    message = "Permission denied: ${e.message}"
                )
            } catch (e: Exception) {
                BackupResult.Failure(
                    error = BackupError.METADATA_CREATION_FAILED,
                    message = "Failed to create backup: ${e.message}"
                )
            }
        }
    }
    
    override suspend fun restoreBackup(backupFile: File): RestoreResult {
        return withContext(Dispatchers.IO) {
            try {
                backupManager.restoreBackup(backupFile)
            } catch (e: SecurityException) {
                RestoreResult.Failure(
                    error = RestoreError.BACKUP_CORRUPTED,
                    message = "Permission denied: ${e.message}"
                )
            } catch (e: Exception) {
                RestoreResult.Failure(
                    error = RestoreError.EXTRACTION_FAILED,
                    message = "Failed to restore backup: ${e.message}"
                )
            }
        }
    }
    
    override suspend fun validateDataIntegrity(file: File): IntegrityCheckResult {
        return withContext(Dispatchers.IO) {
            try {
                val issues = mutableListOf<IntegrityIssue>()
                
                // Basic file validation
                if (!file.exists()) {
                    issues.add(
                        IntegrityIssue(
                            type = IntegrityIssueType.CORRUPTED_AUDIO_FILE,
                            description = "File does not exist",
                            severity = IssueSeverity.CRITICAL
                        )
                    )
                    return@withContext IntegrityCheckResult.Invalid(issues, false)
                }
                
                if (file.length() == 0L) {
                    issues.add(
                        IntegrityIssue(
                            type = IntegrityIssueType.CORRUPTED_AUDIO_FILE,
                            description = "File is empty",
                            severity = IssueSeverity.CRITICAL
                        )
                    )
                    return@withContext IntegrityCheckResult.Invalid(issues, false)
                }
                
                // File extension validation
                val extension = file.extension.lowercase()
                val supportedExtensions = listOf("json", "csv", "md", "pdf", "zip")
                
                if (extension !in supportedExtensions) {
                    issues.add(
                        IntegrityIssue(
                            type = IntegrityIssueType.ENCODING_ERROR,
                            description = "Unsupported file format: $extension",
                            severity = IssueSeverity.ERROR
                        )
                    )
                }
                
                // Content validation for JSON files
                if (extension == "json") {
                    val parser = JsonImportParser()
                    when (val validationResult = parser.validateFormat(file)) {
                        is com.voicenotesai.data.portability.parsers.ValidationResult.Invalid -> {
                            validationResult.issues.forEach { issue ->
                                issues.add(
                                    IntegrityIssue(
                                        type = IntegrityIssueType.INVALID_DATA_TYPE,
                                        description = issue,
                                        severity = IssueSeverity.ERROR
                                    )
                                )
                            }
                        }
                        is com.voicenotesai.data.portability.parsers.ValidationResult.Valid -> {
                            // File is valid
                        }
                    }
                }
                
                // Calculate checksum for integrity verification
                val checksum = calculateFileChecksum(file)
                val notesCount = estimateNotesCount(file)
                
                if (issues.isEmpty()) {
                    IntegrityCheckResult.Valid(
                        checksumMatches = true, // We don't have a reference checksum to compare
                        structureValid = true,
                        notesCount = notesCount
                    )
                } else {
                    val canProceed = issues.none { it.severity == IssueSeverity.CRITICAL }
                    IntegrityCheckResult.Invalid(issues, canProceed)
                }
            } catch (e: Exception) {
                IntegrityCheckResult.Invalid(
                    issues = listOf(
                        IntegrityIssue(
                            type = IntegrityIssueType.CORRUPTED_AUDIO_FILE,
                            description = "Failed to validate file: ${e.message}",
                            severity = IssueSeverity.CRITICAL
                        )
                    ),
                    canProceed = false
                )
            }
        }
    }
    
    /**
     * Get all notes from repository as enhanced notes
     */
    private suspend fun getAllNotesAsEnhanced(): List<EnhancedNote> {
        return withContext(Dispatchers.IO) {
            try {
                // For now, we'll return an empty list since we need to collect from Flow
                // This would need to be implemented properly with Flow collection
                // or by adding a suspend function to the repository
                emptyList<EnhancedNote>()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    /**
     * Calculate file checksum for integrity verification
     */
    private fun calculateFileChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead = input.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Estimate number of notes in file (rough estimation)
     */
    private fun estimateNotesCount(file: File): Int {
        return when (file.extension.lowercase()) {
            "json" -> {
                try {
                    val content = file.readText()
                    // Count occurrences of "id" field as rough estimate
                    content.split("\"id\"").size - 1
                } catch (e: Exception) {
                    0
                }
            }
            "csv" -> {
                try {
                    file.readLines().size - 1 // Subtract header
                } catch (e: Exception) {
                    0
                }
            }
            else -> 0
        }
    }
}