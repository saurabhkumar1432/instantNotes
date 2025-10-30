package com.voicenotesai.data.ai

import com.voicenotesai.domain.ai.ContentCategory
import com.voicenotesai.domain.ai.EntityType
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlinx.coroutines.Dispatchers

/**
 * Integration tests for content analysis and entity extraction functionality.
 * Tests the automatic categorization, sentiment analysis, and entity extraction features.
 */
class ContentAnalysisIntegrationTest {

    private lateinit var noteCategorizationService: NoteCategorizationService
    private lateinit var enhancedSentimentAnalysisService: EnhancedSentimentAnalysisService
    private lateinit var contentAnalysisService: ContentAnalysisService
    private lateinit var entityExtractionService: EntityExtractionService

    @Before
    fun setup() {
        noteCategorizationService = NoteCategorizationService(Dispatchers.Unconfined)
        enhancedSentimentAnalysisService = EnhancedSentimentAnalysisService(Dispatchers.Unconfined)
        contentAnalysisService = ContentAnalysisService(
            noteCategorizationService,
            enhancedSentimentAnalysisService,
            Dispatchers.Unconfined
        )
        entityExtractionService = EntityExtractionService(Dispatchers.Unconfined)
    }

    @Test
    fun `test meeting note categorization`() = runTest {
        val meetingContent = """
            Team meeting agenda for project review.
            Attendees: John, Sarah, Mike
            Discussion points:
            1. Project timeline and milestones
            2. Budget allocation decisions
            3. Action items for next sprint
            Follow up meeting scheduled for next week.
        """.trimIndent()

        val categorization = noteCategorizationService.categorizeNote(meetingContent)

        assertEquals(ContentCategory.MEETING, categorization.primaryCategory)
        assertTrue("Confidence should be high for clear meeting content", categorization.confidence > 0.5f)
        assertTrue("Should have secondary categories", categorization.secondaryCategories.isNotEmpty())
    }

    @Test
    fun `test task planning note categorization`() = runTest {
        val taskContent = """
            TODO list for this week:
            - Complete project proposal (high priority)
            - Schedule client meeting (due Friday)
            - Review budget documents (urgent)
            - Prepare presentation slides
            Remember to follow up on pending tasks.
        """.trimIndent()

        val categorization = noteCategorizationService.categorizeNote(taskContent)

        assertEquals(ContentCategory.TASK_PLANNING, categorization.primaryCategory)
        assertTrue("Should detect task-related content", categorization.confidence > 0.5f)
    }

    @Test
    fun `test brainstorming note categorization`() = runTest {
        val brainstormingContent = """
            Brainstorming session for new product ideas.
            Creative concepts to explore:
            - Innovation in mobile apps
            - Alternative solutions for user engagement
            - Breakthrough approaches to data visualization
            Let's think outside the box and generate more possibilities.
        """.trimIndent()

        val categorization = noteCategorizationService.categorizeNote(brainstormingContent)

        assertEquals(ContentCategory.BRAINSTORMING, categorization.primaryCategory)
        assertTrue("Should detect brainstorming content", categorization.confidence > 0.5f)
    }

    @Test
    fun `test personal reflection categorization`() = runTest {
        val personalContent = """
            Personal reflection on today's experiences.
            I feel grateful for the learning opportunities.
            This journey has taught me valuable insights about growth and resilience.
            I believe this perspective will help me in future challenges.
        """.trimIndent()

        val categorization = noteCategorizationService.categorizeNote(personalContent)

        assertEquals(ContentCategory.PERSONAL_REFLECTION, categorization.primaryCategory)
        assertTrue("Should detect personal reflection", categorization.confidence > 0.5f)
    }

    @Test
    fun `test enhanced sentiment analysis with positive content`() = runTest {
        val positiveContent = """
            I'm absolutely thrilled about this amazing opportunity!
            The team is fantastic and I love working on this project.
            Everything is going perfectly and I feel incredibly grateful.
        """.trimIndent()

        val sentimentResult = enhancedSentimentAnalysisService.analyzeEnhancedSentiment(positiveContent)

        assertTrue("Should detect positive sentiment", sentimentResult.overallSentiment.overall > 0.5f)
        assertTrue("Should have high positive score", sentimentResult.overallSentiment.positive > 0.3f)
        assertTrue("Should have emotional phrases", sentimentResult.emotionalPhrases.isNotEmpty())
        assertEquals("Should detect joy as dominant emotion", EmotionCategory.JOY, sentimentResult.dominantEmotion)
    }

    @Test
    fun `test enhanced sentiment analysis with negative content`() = runTest {
        val negativeContent = """
            I'm really frustrated with this terrible situation.
            Everything is going wrong and I feel disappointed.
            This is a horrible experience and I'm quite angry about it.
        """.trimIndent()

        val sentimentResult = enhancedSentimentAnalysisService.analyzeEnhancedSentiment(negativeContent)

        assertTrue("Should detect negative sentiment", sentimentResult.overallSentiment.overall < -0.3f)
        assertTrue("Should have high negative score", sentimentResult.overallSentiment.negative > 0.3f)
        assertTrue("Should have emotional phrases", sentimentResult.emotionalPhrases.isNotEmpty())
        assertTrue("Should detect negative emotion", 
            sentimentResult.dominantEmotion in listOf(EmotionCategory.ANGER, EmotionCategory.SADNESS, EmotionCategory.NEGATIVE_OTHER))
    }

    @Test
    fun `test entity extraction for various entity types`() = runTest {
        val contentWithEntities = """
            Meeting with John Smith at Google Inc. on January 15, 2024 at 2:30 PM.
            Contact him at john.smith@google.com or call (555) 123-4567.
            Visit the website https://www.google.com for more information.
            TODO: Send proposal by Friday, prepare $10,000 budget analysis.
            Address: 123 Main Street, San Francisco, CA.
        """.trimIndent()

        val entities = entityExtractionService.extractEntities(contentWithEntities)

        // Check for different entity types
        val entityTypes = entities.map { it.type }.toSet()
        
        assertTrue("Should extract person names", entityTypes.contains(EntityType.PERSON))
        assertTrue("Should extract organizations", entityTypes.contains(EntityType.ORGANIZATION))
        assertTrue("Should extract dates", entityTypes.contains(EntityType.DATE))
        assertTrue("Should extract times", entityTypes.contains(EntityType.TIME))
        assertTrue("Should extract emails", entityTypes.contains(EntityType.EMAIL))
        assertTrue("Should extract phone numbers", entityTypes.contains(EntityType.PHONE_NUMBER))
        assertTrue("Should extract URLs", entityTypes.contains(EntityType.URL))
        assertTrue("Should extract tasks", entityTypes.contains(EntityType.TASK))
        assertTrue("Should extract money amounts", entityTypes.contains(EntityType.MONEY))
        assertTrue("Should extract locations", entityTypes.contains(EntityType.LOCATION))

        // Verify specific entities
        val personEntities = entities.filter { it.type == EntityType.PERSON }
        assertTrue("Should find person name", personEntities.any { it.text.contains("John Smith") })

        val emailEntities = entities.filter { it.type == EntityType.EMAIL }
        assertTrue("Should find email", emailEntities.any { it.text.contains("john.smith@google.com") })

        val moneyEntities = entities.filter { it.type == EntityType.MONEY }
        assertTrue("Should find money amount", moneyEntities.any { it.text.contains("10,000") })
    }

    @Test
    fun `test tag suggestions based on categorization`() = runTest {
        val meetingContent = """
            Urgent team meeting about project priorities.
            Important decisions need to be made today.
            Action items and next steps discussion.
        """.trimIndent()

        val categorization = noteCategorizationService.categorizeNote(meetingContent)
        val tags = noteCategorizationService.suggestTags(meetingContent, "", categorization)

        assertTrue("Should suggest meeting-related tags", tags.contains("meeting"))
        assertTrue("Should suggest urgency tags", tags.contains("urgent"))
        assertTrue("Should suggest importance tags", tags.contains("important"))
        assertTrue("Should suggest action items tags", tags.contains("action-items"))
        assertTrue("Should have time-based tags", tags.any { it in listOf("morning", "afternoon", "evening", "night") })
    }

    @Test
    fun `test comprehensive content analysis integration`() = runTest {
        val complexContent = """
            Research meeting notes from January 10th, 2024.
            
            Attendees: Dr. Sarah Johnson (sarah.johnson@university.edu), Prof. Mike Chen
            
            Discussion topics:
            1. Data analysis methodology - very promising results!
            2. Budget allocation: $50,000 for equipment
            3. Timeline: Complete by March 15th, 2024
            
            Key findings:
            - Innovation in machine learning approaches
            - Breakthrough in data processing (95% accuracy achieved)
            - Collaboration opportunities with Google Research
            
            Action items:
            - TODO: Prepare research proposal (due next Friday)
            - Schedule follow-up meeting with stakeholders
            - Contact john.doe@company.com for partnership discussion
            
            Personal reflection: I feel excited about these developments!
            This research could have significant impact on the field.
            
            Next steps: Visit lab at 123 Research Drive, call (555) 987-6543
            Website: https://research.university.edu/project
        """.trimIndent()

        // Test categorization
        val categorization = noteCategorizationService.categorizeNote(complexContent)
        assertTrue("Should categorize as meeting or research", 
            categorization.primaryCategory in listOf(ContentCategory.MEETING, ContentCategory.RESEARCH_NOTES))

        // Test sentiment analysis
        val sentiment = enhancedSentimentAnalysisService.analyzeEnhancedSentiment(complexContent)
        assertTrue("Should detect positive sentiment", sentiment.overallSentiment.overall > 0.0f)

        // Test entity extraction
        val entities = entityExtractionService.extractEntities(complexContent)
        val entityTypes = entities.map { it.type }.toSet()
        
        assertTrue("Should extract multiple entity types", entityTypes.size >= 5)
        assertTrue("Should extract dates", entityTypes.contains(EntityType.DATE))
        assertTrue("Should extract emails", entityTypes.contains(EntityType.EMAIL))
        assertTrue("Should extract money", entityTypes.contains(EntityType.MONEY))
        assertTrue("Should extract phone numbers", entityTypes.contains(EntityType.PHONE_NUMBER))
        assertTrue("Should extract URLs", entityTypes.contains(EntityType.URL))

        // Test tag suggestions
        val tags = noteCategorizationService.suggestTags(complexContent, "", categorization)
        assertTrue("Should suggest relevant tags", tags.isNotEmpty())
        assertTrue("Should have at least 3 tags", tags.size >= 3)
    }

    @Test
    fun `test edge cases and error handling`() = runTest {
        // Test empty content
        val emptyCategorization = noteCategorizationService.categorizeNote("")
        assertEquals("Empty content should default to OTHER", ContentCategory.OTHER, emptyCategorization.primaryCategory)

        val emptySentiment = enhancedSentimentAnalysisService.analyzeEnhancedSentiment("")
        assertEquals("Empty content should have neutral sentiment", 0.0f, emptySentiment.overallSentiment.overall, 0.1f)

        val emptyEntities = entityExtractionService.extractEntities("")
        assertTrue("Empty content should have no entities", emptyEntities.isEmpty())

        // Test very short content
        val shortContent = "OK"
        val shortCategorization = noteCategorizationService.categorizeNote(shortContent)
        assertNotNull("Should handle short content", shortCategorization)

        // Test content with special characters
        val specialContent = "Meeting @#$%^&*() with John!!! Email: test@test.com"
        val specialEntities = entityExtractionService.extractEntities(specialContent)
        assertTrue("Should extract entities despite special characters", 
            specialEntities.any { it.type == EntityType.EMAIL })
    }
}