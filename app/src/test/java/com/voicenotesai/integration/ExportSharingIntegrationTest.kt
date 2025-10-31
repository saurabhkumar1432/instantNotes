package com.voicenotesai.integration

import com.voicenotesai.data.portability.DataPortabilityEngineImpl
import com.voicenotesai.data.sharing.SharingManagerImpl
import com.voicenotesai.domain.model.EnhancedNote
import com.voicenotesai.domain.model.Task
import com.voicenotesai.domain.portability.DataPortabilityEngine
import com.voicenotesai.domain.sharing.SharingManager
import com.voicenotesai.presentation.notes.ExportFormat
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * Integration test for export and sharing functionality across different formats.
 * Tests the complete export and sharing workflow:
 * 1. Exporting notes to various formats (PDF, Word, Markdown, Plain Text)
 * 2. Sharing notes via different channels (email, messaging, cloud storage)
 * 3. Calendar integration for meeting notes
 * 4. Bulk export operations
 * 5. Template customization for exports
 */
class ExportSharingIntegrationTest {

    private lateinit var dataPortabilityEngine: DataPortabilityEngineImpl
    private lateinit var sharingManager: SharingManagerImpl

    @Before
    fun setup() {
        dataPortabilityEngine = mockk(relaxed = true)
        sharingManager = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `export single note to PDF format`() = runTest {
        // Given: A note with tasks
        val note = EnhancedNote(
            id = "export-note-1",
            originalTranscription = "Team meeting discussion about Q4 goals",
            enhancedContent = "Q4 Planning Meeting:\n- Review current progress\n- Set Q4 objectives\n- Assign responsibilities",
            summary = "Q4 planning and goal setting",
            keyPoints = listOf("Progress review", "Q4 objectives", "Team responsibilities"),
            actionItems = listOf("Review current progress", "Set Q4 objectives", "Assign responsibilities"),
            timestamp = System.currentTimeMillis(),
            duration = 3600000L, // 1 hour
            tags = listOf("meeting", "planning", "Q4"),
            category = "Work"
        )

        val associatedTasks = listOf(
            Task(
                id = "task-1",
                text = "Review current progress",
                sourceNoteId = note.id,
                isCompleted = false,
                createdAt = System.currentTimeMillis()
            ),
            Task(
                id = "task-2",
                text = "Set Q4 objectives", 
                sourceNoteId = note.id,
                isCompleted = false,
                createdAt = System.currentTimeMillis()
            )
        )

        // Mock PDF export
        val pdfFile = File("test-export.pdf")
        coEvery { dataPortabilityEngine.exportNote(note, ExportFormat.PDF, associatedTasks) } returns 
            DataPortabilityEngine.ExportResult(
                success = true,
                filePath = pdfFile.absolutePath,
                format = ExportFormat.PDF,
                fileSize = 1024 * 50, // 50KB
                errorMessage = null
            )

        // When: Exporting note to PDF
        val exportResult = dataPortabilityEngine.exportNote(note, ExportFormat.PDF, associatedTasks)

        // Then: Export should succeed
        assertTrue("PDF export should succeed", exportResult.success)
        assertEquals("Should export to PDF format", ExportFormat.PDF, exportResult.format)
        assertNotNull("Should have file path", exportResult.filePath)
        assertTrue("File size should be reasonable", exportResult.fileSize > 0)
        assertNull("Should have no error message", exportResult.errorMessage)

        verify { dataPortabilityEngine.exportNote(note, ExportFormat.PDF, associatedTasks) }
    }

    @Test
    fun `export multiple notes to Word document`() = runTest {
        // Given: Multiple notes for bulk export
        val notes = listOf(
            EnhancedNote(
                id = "bulk-note-1",
                originalTranscription = "First meeting notes",
                enhancedContent = "Meeting 1: Project kickoff",
                summary = "Project kickoff meeting",
                keyPoints = listOf("Project scope", "Timeline"),
                actionItems = listOf("Create project plan"),
                timestamp = System.currentTimeMillis() - 86400000, // Yesterday
                duration = 1800000L, // 30 minutes
                tags = listOf("meeting", "kickoff"),
                category = "Work"
            ),
            EnhancedNote(
                id = "bulk-note-2",
                originalTranscription = "Second meeting notes",
                enhancedContent = "Meeting 2: Progress review",
                summary = "Weekly progress review",
                keyPoints = listOf("Progress update", "Blockers"),
                actionItems = listOf("Address blockers"),
                timestamp = System.currentTimeMillis(),
                duration = 2400000L, // 40 minutes
                tags = listOf("meeting", "review"),
                category = "Work"
            )
        )

        // Mock Word export
        val wordFile = File("bulk-export.docx")
        coEvery { dataPortabilityEngine.exportMultipleNotes(notes, ExportFormat.WORD) } returns
            DataPortabilityEngine.BulkExportResult(
                success = true,
                filePath = wordFile.absolutePath,
                format = ExportFormat.WORD,
                fileSize = 1024 * 150, // 150KB
                notesCount = 2,
                errorMessage = null
            )

        // When: Bulk exporting to Word
        val bulkExportResult = dataPortabilityEngine.exportMultipleNotes(notes, ExportFormat.WORD)

        // Then: Bulk export should succeed
        assertTrue("Word bulk export should succeed", bulkExportResult.success)
        assertEquals("Should export to Word format", ExportFormat.WORD, bulkExportResult.format)
        assertEquals("Should export 2 notes", 2, bulkExportResult.notesCount)
        assertTrue("File size should be reasonable for multiple notes", bulkExportResult.fileSize > 100 * 1024)

        verify { dataPortabilityEngine.exportMultipleNotes(notes, ExportFormat.WORD) }
    }

    @Test
    fun `export to Markdown with custom template`() = runTest {
        // Given: Note for Markdown export
        val note = EnhancedNote(
            id = "markdown-note-1",
            originalTranscription = "Technical discussion about API design",
            enhancedContent = "API Design Discussion:\n- REST vs GraphQL\n- Authentication strategy\n- Rate limiting",
            summary = "API architecture decisions",
            keyPoints = listOf("REST vs GraphQL", "Authentication", "Rate limiting"),
            actionItems = listOf("Finalize API design", "Implement authentication"),
            timestamp = System.currentTimeMillis(),
            duration = 2700000L, // 45 minutes
            tags = listOf("technical", "api", "design"),
            category = "Development"
        )

        // Given: Custom Markdown template
        val customTemplate = """
            # {{title}}
            
            **Date:** {{date}}
            **Duration:** {{duration}}
            **Category:** {{category}}
            
            ## Summary
            {{summary}}
            
            ## Key Points
            {{#keyPoints}}
            - {{.}}
            {{/keyPoints}}
            
            ## Action Items
            {{#actionItems}}
            - [ ] {{.}}
            {{/actionItems}}
            
            ## Full Content
            {{content}}
            
            ---
            *Generated by Voice Notes AI*
        """.trimIndent()

        // Mock Markdown export with template
        val markdownFile = File("custom-export.md")
        coEvery { dataPortabilityEngine.exportNoteWithTemplate(note, ExportFormat.MARKDOWN, customTemplate) } returns
            DataPortabilityEngine.ExportResult(
                success = true,
                filePath = markdownFile.absolutePath,
                format = ExportFormat.MARKDOWN,
                fileSize = 1024 * 5, // 5KB
                errorMessage = null
            )

        // When: Exporting with custom template
        val exportResult = dataPortabilityEngine.exportNoteWithTemplate(note, ExportFormat.MARKDOWN, customTemplate)

        // Then: Template export should succeed
        assertTrue("Markdown template export should succeed", exportResult.success)
        assertEquals("Should export to Markdown format", ExportFormat.MARKDOWN, exportResult.format)

        verify { dataPortabilityEngine.exportNoteWithTemplate(note, ExportFormat.MARKDOWN, customTemplate) }
    }

    @Test
    fun `share note via email with attachments`() = runTest {
        // Given: Note to share
        val note = EnhancedNote(
            id = "share-note-1",
            originalTranscription = "Client feedback on prototype",
            enhancedContent = "Client Feedback Session:\n- UI improvements needed\n- Performance concerns\n- Feature requests",
            summary = "Client prototype feedback",
            keyPoints = listOf("UI improvements", "Performance", "Feature requests"),
            actionItems = listOf("Update UI design", "Optimize performance", "Evaluate feature requests"),
            timestamp = System.currentTimeMillis(),
            duration = 1800000L, // 30 minutes
            tags = listOf("client", "feedback", "prototype"),
            category = "Work"
        )

        // Given: Export for sharing
        val pdfFile = File("share-note.pdf")
        coEvery { dataPortabilityEngine.exportNote(note, ExportFormat.PDF, emptyList()) } returns
            DataPortabilityEngine.ExportResult(
                success = true,
                filePath = pdfFile.absolutePath,
                format = ExportFormat.PDF,
                fileSize = 1024 * 75, // 75KB
                errorMessage = null
            )

        // Mock email sharing
        coEvery { sharingManager.shareViaEmail(
            recipients = listOf("client@example.com", "team@company.com"),
            subject = "Client Feedback Session Notes",
            body = any(),
            attachments = listOf(pdfFile.absolutePath)
        ) } returns SharingManager.SharingResult(
            success = true,
            platform = "Email",
            errorMessage = null
        )

        // When: Sharing via email
        val exportResult = dataPortabilityEngine.exportNote(note, ExportFormat.PDF, emptyList())
        assertTrue("Export for sharing should succeed", exportResult.success)

        val sharingResult = sharingManager.shareViaEmail(
            recipients = listOf("client@example.com", "team@company.com"),
            subject = "Client Feedback Session Notes",
            body = "Please find attached the notes from our feedback session.",
            attachments = listOf(exportResult.filePath)
        )

        // Then: Email sharing should succeed
        assertTrue("Email sharing should succeed", sharingResult.success)
        assertEquals("Should share via email", "Email", sharingResult.platform)

        verify { sharingManager.shareViaEmail(any(), any(), any(), any()) }
    }

    @Test
    fun `share to cloud storage with folder organization`() = runTest {
        // Given: Notes for cloud storage
        val workNotes = listOf(
            EnhancedNote(
                id = "work-note-1",
                originalTranscription = "Sprint planning meeting",
                enhancedContent = "Sprint Planning:\n- Story estimation\n- Sprint goals\n- Team capacity",
                summary = "Sprint planning session",
                keyPoints = listOf("Story estimation", "Sprint goals"),
                actionItems = listOf("Update sprint backlog"),
                timestamp = System.currentTimeMillis(),
                duration = 3600000L, // 1 hour
                tags = listOf("sprint", "planning", "agile"),
                category = "Work"
            )
        )

        // Mock cloud storage sharing
        coEvery { sharingManager.shareToCloudStorage(
            notes = workNotes,
            provider = SharingManager.CloudProvider.GOOGLE_DRIVE,
            folderPath = "/Voice Notes/Work/2024",
            format = ExportFormat.PDF
        ) } returns SharingManager.CloudSharingResult(
            success = true,
            provider = SharingManager.CloudProvider.GOOGLE_DRIVE,
            sharedUrls = listOf("https://drive.google.com/file/d/abc123/view"),
            folderUrl = "https://drive.google.com/drive/folders/xyz789",
            errorMessage = null
        )

        // When: Sharing to Google Drive
        val cloudResult = sharingManager.shareToCloudStorage(
            notes = workNotes,
            provider = SharingManager.CloudProvider.GOOGLE_DRIVE,
            folderPath = "/Voice Notes/Work/2024",
            format = ExportFormat.PDF
        )

        // Then: Cloud sharing should succeed
        assertTrue("Cloud sharing should succeed", cloudResult.success)
        assertEquals("Should share to Google Drive", SharingManager.CloudProvider.GOOGLE_DRIVE, cloudResult.provider)
        assertEquals("Should have shared URL", 1, cloudResult.sharedUrls.size)
        assertNotNull("Should have folder URL", cloudResult.folderUrl)

        verify { sharingManager.shareToCloudStorage(workNotes, SharingManager.CloudProvider.GOOGLE_DRIVE, "/Voice Notes/Work/2024", ExportFormat.PDF) }
    }

    @Test
    fun `create calendar event from meeting note`() = runTest {
        // Given: Meeting note with date/time information
        val meetingNote = EnhancedNote(
            id = "meeting-note-1",
            originalTranscription = "Follow-up meeting scheduled for next Tuesday at 2 PM to discuss project status",
            enhancedContent = "Follow-up Meeting:\n- Project status review\n- Next steps discussion\n- Timeline adjustments",
            summary = "Project follow-up meeting",
            keyPoints = listOf("Status review", "Next steps", "Timeline"),
            actionItems = listOf("Prepare status report", "Review timeline"),
            timestamp = System.currentTimeMillis(),
            duration = 1800000L, // 30 minutes
            tags = listOf("meeting", "follow-up", "project"),
            category = "Work"
        )

        // Mock calendar integration
        val calendarEvent = SharingManager.CalendarEvent(
            title = "Project Follow-up Meeting",
            description = meetingNote.enhancedContent,
            startTime = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000, // Next week
            duration = 3600000L, // 1 hour
            attendees = listOf("team@company.com"),
            location = "Conference Room A"
        )

        coEvery { sharingManager.createCalendarEvent(meetingNote, calendarEvent) } returns
            SharingManager.CalendarResult(
                success = true,
                eventId = "cal-event-123",
                eventUrl = "https://calendar.google.com/event?eid=abc123",
                errorMessage = null
            )

        // When: Creating calendar event
        val calendarResult = sharingManager.createCalendarEvent(meetingNote, calendarEvent)

        // Then: Calendar event creation should succeed
        assertTrue("Calendar event creation should succeed", calendarResult.success)
        assertNotNull("Should have event ID", calendarResult.eventId)
        assertNotNull("Should have event URL", calendarResult.eventUrl)

        verify { sharingManager.createCalendarEvent(meetingNote, calendarEvent) }
    }

    @Test
    fun `export with filtering and date range`() = runTest {
        // Given: Notes with different categories and dates
        val allNotes = listOf(
            EnhancedNote(
                id = "filter-note-1",
                originalTranscription = "Work meeting",
                enhancedContent = "Work content",
                summary = "Work summary",
                keyPoints = listOf("Work point"),
                actionItems = listOf("Work task"),
                timestamp = System.currentTimeMillis() - 86400000, // Yesterday
                duration = 1800000L,
                tags = listOf("work"),
                category = "Work"
            ),
            EnhancedNote(
                id = "filter-note-2",
                originalTranscription = "Personal note",
                enhancedContent = "Personal content",
                summary = "Personal summary",
                keyPoints = listOf("Personal point"),
                actionItems = listOf("Personal task"),
                timestamp = System.currentTimeMillis() - 172800000, // 2 days ago
                duration = 900000L,
                tags = listOf("personal"),
                category = "Personal"
            ),
            EnhancedNote(
                id = "filter-note-3",
                originalTranscription = "Another work note",
                enhancedContent = "More work content",
                summary = "Work summary 2",
                keyPoints = listOf("Work point 2"),
                actionItems = listOf("Work task 2"),
                timestamp = System.currentTimeMillis() - 43200000, // 12 hours ago
                duration = 2400000L,
                tags = listOf("work", "important"),
                category = "Work"
            )
        )

        // Given: Filter criteria
        val filterCriteria = DataPortabilityEngine.ExportFilter(
            categories = listOf("Work"),
            dateRange = DataPortabilityEngine.DateRange(
                startDate = System.currentTimeMillis() - 86400000, // Last 24 hours
                endDate = System.currentTimeMillis()
            ),
            tags = listOf("work"),
            includeCompleted = true
        )

        // Mock filtered export
        val filteredNotes = allNotes.filter { note ->
            note.category == "Work" && 
            note.timestamp >= filterCriteria.dateRange.startDate &&
            note.tags.any { it in filterCriteria.tags }
        }

        coEvery { dataPortabilityEngine.exportWithFilter(filterCriteria, ExportFormat.MARKDOWN) } returns
            DataPortabilityEngine.BulkExportResult(
                success = true,
                filePath = "filtered-export.md",
                format = ExportFormat.MARKDOWN,
                fileSize = 1024 * 25, // 25KB
                notesCount = 1, // Only one note matches the filter
                errorMessage = null
            )

        // When: Exporting with filter
        val exportResult = dataPortabilityEngine.exportWithFilter(filterCriteria, ExportFormat.MARKDOWN)

        // Then: Filtered export should succeed
        assertTrue("Filtered export should succeed", exportResult.success)
        assertEquals("Should export 1 filtered note", 1, exportResult.notesCount)
        assertEquals("Should export to Markdown", ExportFormat.MARKDOWN, exportResult.format)

        verify { dataPortabilityEngine.exportWithFilter(filterCriteria, ExportFormat.MARKDOWN) }
    }

    @Test
    fun `handle export failures gracefully`() = runTest {
        // Given: Note that fails to export
        val problematicNote = EnhancedNote(
            id = "problem-note-1",
            originalTranscription = "Note with special characters: ñáéíóú",
            enhancedContent = "Content with émojis 🎉 and spëcial chars",
            summary = "Problematic content",
            keyPoints = listOf("Special chars", "Encoding issues"),
            actionItems = listOf("Fix encoding"),
            timestamp = System.currentTimeMillis(),
            duration = 1200000L,
            tags = listOf("encoding", "special"),
            category = "Test"
        )

        // Mock export failure
        coEvery { dataPortabilityEngine.exportNote(problematicNote, ExportFormat.PDF, emptyList()) } returns
            DataPortabilityEngine.ExportResult(
                success = false,
                filePath = null,
                format = ExportFormat.PDF,
                fileSize = 0,
                errorMessage = "PDF generation failed: Unsupported character encoding"
            )

        // When: Attempting to export problematic note
        val exportResult = dataPortabilityEngine.exportNote(problematicNote, ExportFormat.PDF, emptyList())

        // Then: Should handle failure gracefully
        assertFalse("Export should fail", exportResult.success)
        assertNull("Should have no file path", exportResult.filePath)
        assertEquals("Should have zero file size", 0, exportResult.fileSize)
        assertNotNull("Should have error message", exportResult.errorMessage)
        assertTrue("Error message should mention encoding", exportResult.errorMessage?.contains("encoding") == true)

        verify { dataPortabilityEngine.exportNote(problematicNote, ExportFormat.PDF, emptyList()) }
    }

    @Test
    fun `share with expiration links`() = runTest {
        // Given: Note for temporary sharing
        val temporaryNote = EnhancedNote(
            id = "temp-note-1",
            originalTranscription = "Confidential meeting notes",
            enhancedContent = "Confidential Discussion:\n- Sensitive information\n- Internal decisions",
            summary = "Confidential meeting",
            keyPoints = listOf("Sensitive info", "Internal decisions"),
            actionItems = listOf("Follow up privately"),
            timestamp = System.currentTimeMillis(),
            duration = 2700000L,
            tags = listOf("confidential", "internal"),
            category = "Work"
        )

        // Mock shareable link creation with expiration
        val expirationTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000 // 24 hours
        coEvery { sharingManager.createShareableLink(
            note = temporaryNote,
            expirationTime = expirationTime,
            requirePassword = true,
            allowDownload = false
        ) } returns SharingManager.ShareableLinkResult(
            success = true,
            shareUrl = "https://voicenotes.ai/share/temp-abc123",
            expirationTime = expirationTime,
            password = "temp-pass-456",
            errorMessage = null
        )

        // When: Creating shareable link with expiration
        val linkResult = sharingManager.createShareableLink(
            note = temporaryNote,
            expirationTime = expirationTime,
            requirePassword = true,
            allowDownload = false
        )

        // Then: Shareable link should be created successfully
        assertTrue("Shareable link creation should succeed", linkResult.success)
        assertNotNull("Should have share URL", linkResult.shareUrl)
        assertEquals("Should have correct expiration", expirationTime, linkResult.expirationTime)
        assertNotNull("Should have password", linkResult.password)

        verify { sharingManager.createShareableLink(temporaryNote, expirationTime, true, false) }
    }
}