package com.voicenotesai.data.portability

import com.voicenotesai.data.repository.NotesRepository
import com.voicenotesai.domain.portability.ExportFormat
import com.voicenotesai.domain.portability.ExportResult
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.io.IOException

/**
 * Unit tests for DataPortabilityEngineImpl
 */
class DataPortabilityEngineTest {
    
    private lateinit var notesRepository: NotesRepository
    private lateinit var dataPortabilityEngine: DataPortabilityEngineImpl
    private lateinit var tempDir: File
    
    @Before
    fun setup() {
        notesRepository = mockk(relaxed = true)
        dataPortabilityEngine = DataPortabilityEngineImpl(notesRepository)
        
        // Create temporary directory for test files
        tempDir = File.createTempFile("test", "").apply {
            delete()
            mkdirs()
        }
    }
    
    @Test
    fun `exportNotes with JSON format creates valid file`() = runTest {
        // Given
        val outputFile = File(tempDir, "test_export.json")
        
        // When
        val result = dataPortabilityEngine.exportNotes(ExportFormat.JSON, outputFile)
        
        // Then
        assertTrue("Export should succeed", result is ExportResult.Success)
        assertTrue("Output file should exist", outputFile.exists())
        assertTrue("Output file should not be empty", outputFile.length() > 0)
        
        // Verify JSON structure
        val content = outputFile.readText()
        assertTrue("Should contain metadata", content.contains("metadata"))
        assertTrue("Should contain notes array", content.contains("notes"))
    }
    
    @Test
    fun `exportNotes with CSV format creates valid file`() = runTest {
        // Given
        val outputFile = File(tempDir, "test_export.csv")
        
        // When
        val result = dataPortabilityEngine.exportNotes(ExportFormat.CSV, outputFile)
        
        // Then
        assertTrue("Export should succeed", result is ExportResult.Success)
        assertTrue("Output file should exist", outputFile.exists())
        
        // Verify CSV structure
        val lines = outputFile.readLines()
        assertTrue("Should have header", lines.isNotEmpty())
        assertTrue("Header should contain expected columns", 
            lines.first().contains("ID,Content,Transcribed Text"))
    }
    
    @Test
    fun `exportNotes with Markdown format creates valid file`() = runTest {
        // Given
        val outputFile = File(tempDir, "test_export.md")
        
        // When
        val result = dataPortabilityEngine.exportNotes(ExportFormat.Markdown, outputFile)
        
        // Then
        assertTrue("Export should succeed", result is ExportResult.Success)
        assertTrue("Output file should exist", outputFile.exists())
        
        // Verify Markdown structure
        val content = outputFile.readText()
        assertTrue("Should contain title", content.contains("# Voice Notes Export"))
        assertTrue("Should contain export date", content.contains("**Export Date:**"))
    }
    
    @Test
    fun `exportNotes with PDF format creates valid file`() = runTest {
        // Given
        val outputFile = File(tempDir, "test_export.pdf")
        
        // When
        val result = dataPortabilityEngine.exportNotes(ExportFormat.PDF, outputFile)
        
        // Then
        assertTrue("Export should succeed", result is ExportResult.Success)
        assertTrue("Output file should exist", outputFile.exists())
        assertTrue("PDF file should have content", outputFile.length() > 0)
        
        // Verify it's a PDF file (basic check)
        val header = outputFile.readBytes().take(4)
        assertEquals("Should be PDF file", "%PDF".toByteArray().toList(), header)
    }
    
    @Test
    fun `exportNotes handles permission denied error`() = runTest {
        // Given - Create a file in a directory that doesn't exist to simulate permission error
        val invalidDir = File(tempDir, "nonexistent/readonly.json")
        
        // When
        val result = dataPortabilityEngine.exportNotes(ExportFormat.JSON, invalidDir)
        
        // Then
        assertTrue("Export should fail", result is ExportResult.Failure)
    }
    
    @Test
    fun `createBackup creates valid backup file`() = runTest {
        // Given
        val backupFile = File(tempDir, "test_backup.zip")
        
        // When
        val result = dataPortabilityEngine.createBackup(includeAudio = false, backupFile)
        
        // Then
        assertTrue("Backup should succeed", result is com.voicenotesai.domain.portability.BackupResult.Success)
        assertTrue("Backup file should exist", backupFile.exists())
        assertTrue("Backup file should not be empty", backupFile.length() > 0)
    }
    
    @Test
    fun `validateDataIntegrity detects non-existent file`() = runTest {
        // Given
        val nonExistentFile = File(tempDir, "non_existent.json")
        
        // When
        val result = dataPortabilityEngine.validateDataIntegrity(nonExistentFile)
        
        // Then
        assertTrue("Validation should fail", result is com.voicenotesai.domain.portability.IntegrityCheckResult.Invalid)
        val invalidResult = result as com.voicenotesai.domain.portability.IntegrityCheckResult.Invalid
        assertFalse("Should not be able to proceed", invalidResult.canProceed)
    }
    
    @Test
    fun `validateDataIntegrity detects empty file`() = runTest {
        // Given
        val emptyFile = File(tempDir, "empty.json").apply { createNewFile() }
        
        // When
        val result = dataPortabilityEngine.validateDataIntegrity(emptyFile)
        
        // Then
        assertTrue("Validation should fail", result is com.voicenotesai.domain.portability.IntegrityCheckResult.Invalid)
        val invalidResult = result as com.voicenotesai.domain.portability.IntegrityCheckResult.Invalid
        assertFalse("Should not be able to proceed", invalidResult.canProceed)
    }
}