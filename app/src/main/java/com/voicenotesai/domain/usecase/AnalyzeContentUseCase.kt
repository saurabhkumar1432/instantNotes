package com.voicenotesai.domain.usecase

import com.voicenotesai.data.ai.ContentAnalysisService
import com.voicenotesai.data.ai.EntityExtractionService
import com.voicenotesai.data.ai.EnhancedSentimentResult
import com.voicenotesai.data.ai.NoteCategorization
import com.voicenotesai.domain.ai.AIProcessingEngine
import com.voicenotesai.domain.ai.ContentAnalysis
import com.voicenotesai.domain.ai.EntityExtractionResult
import javax.inject.Inject

/**
 * Enhanced use case for comprehensive content analysis and entity extraction.
 * Provides automatic categorization, sentiment analysis, and entity extraction.
 */
class AnalyzeContentUseCase @Inject constructor(
    private val aiProcessingEngine: AIProcessingEngine,
    private val contentAnalysisService: ContentAnalysisService,
    private val entityExtractionService: EntityExtractionService
) {
    
    /**
     * Analyzes the content for sentiment, themes, and categories using AI processing engine.
     */
    suspend fun analyzeContent(content: String): ContentAnalysis {
        return aiProcessingEngine.analyzeContent(content)
    }
    
    /**
     * Extracts entities from the given text using AI processing engine.
     */
    suspend fun extractEntities(text: String): EntityExtractionResult {
        return aiProcessingEngine.extractEntities(text)
    }

    /**
     * Performs comprehensive content analysis including categorization and enhanced sentiment.
     */
    suspend fun analyzeContentComprehensive(
        content: String,
        transcribedText: String = ""
    ): ComprehensiveAnalysisResult {
        // Perform all analysis operations in parallel for better performance
        val fullText = "$content $transcribedText"
        
        // Basic content analysis
        val contentAnalysis = aiProcessingEngine.analyzeContent(fullText)
        
        // Enhanced categorization
        val categorization = contentAnalysisService.categorizeNoteDetailed(content, transcribedText)
        
        // Enhanced sentiment analysis
        val enhancedSentiment = contentAnalysisService.analyzeEnhancedSentiment(fullText)
        
        // Entity extraction
        val entities = entityExtractionService.extractEntities(fullText)
        
        // Tag suggestions
        val suggestedTags = contentAnalysisService.suggestTags(content, transcribedText)
        
        return ComprehensiveAnalysisResult(
            contentAnalysis = contentAnalysis,
            categorization = categorization,
            enhancedSentiment = enhancedSentiment,
            entities = entities,
            suggestedTags = suggestedTags
        )
    }

    /**
     * Automatically categorizes a note based on its content.
     */
    suspend fun categorizeNote(content: String, transcribedText: String = ""): NoteCategorization {
        return contentAnalysisService.categorizeNoteDetailed(content, transcribedText)
    }

    /**
     * Performs enhanced sentiment analysis with emotional insights.
     */
    suspend fun analyzeEnhancedSentiment(content: String): EnhancedSentimentResult {
        return contentAnalysisService.analyzeEnhancedSentiment(content)
    }

    /**
     * Suggests tags based on content analysis and categorization.
     */
    suspend fun suggestTags(content: String, transcribedText: String = ""): List<String> {
        return contentAnalysisService.suggestTags(content, transcribedText)
    }

    /**
     * Extracts entities directly using the entity extraction service.
     */
    suspend fun extractEntitiesDetailed(text: String): List<com.voicenotesai.domain.ai.ExtractedEntity> {
        return entityExtractionService.extractEntities(text)
    }
}

/**
 * Comprehensive analysis result containing all analysis outputs.
 */
data class ComprehensiveAnalysisResult(
    val contentAnalysis: ContentAnalysis,
    val categorization: NoteCategorization,
    val enhancedSentiment: EnhancedSentimentResult,
    val entities: List<com.voicenotesai.domain.ai.ExtractedEntity>,
    val suggestedTags: List<String>
)