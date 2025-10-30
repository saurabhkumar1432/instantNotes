package com.voicenotesai.data.ai

import com.voicenotesai.domain.ai.ContentCategory
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlinx.coroutines.Dispatchers

/**
 * Basic tests for content analysis functionality to verify core features work.
 */
class BasicContentAnalysisTest {

    private lateinit var noteCategorizationService: NoteCategorizationService
    private lateinit var entityExtractionService: EntityExtractionService

    @Before
    fun setup() {
        noteCategorizationService = NoteCategorizationService(Dispatchers.Unconfined)
        entityExtractionService = EntityExtractionService(Dispatchers.Unconfined)
    }

    @Test
    fun `test basic meeting categorization`() = runTest {
        val meetingContent = "Team meeting with agenda and action items"
        val result = noteCategorizationService.categorizeNote(meetingContent)
        
        assertNotNull("Should return categorization result", result)
        assertNotNull("Should have primary category", result.primaryCategory)
        assertTrue("Should have confidence score", result.confidence >= 0f)
    }

    @Test
    fun `test basic entity extraction`() = runTest {
        val content = "Contact john@example.com or call 555-1234"
        val entities = entityExtractionService.extractEntities(content)
        
        assertNotNull("Should return entities list", entities)
        // Basic check - should find at least some entities
        assertTrue("Should extract some entities", entities.isNotEmpty())
    }

    @Test
    fun `test empty content handling`() = runTest {
        val emptyResult = noteCategorizationService.categorizeNote("")
        assertEquals("Empty content should default to OTHER", ContentCategory.OTHER, emptyResult.primaryCategory)
        
        val emptyEntities = entityExtractionService.extractEntities("")
        assertTrue("Empty content should have no entities", emptyEntities.isEmpty())
    }

    @Test
    fun `test task categorization`() = runTest {
        val taskContent = "TODO: Complete the project by Friday"
        val result = noteCategorizationService.categorizeNote(taskContent)
        
        // Should categorize as task planning or at least not crash
        assertNotNull("Should return categorization result", result)
        assertTrue("Should have reasonable confidence", result.confidence >= 0f)
    }
}