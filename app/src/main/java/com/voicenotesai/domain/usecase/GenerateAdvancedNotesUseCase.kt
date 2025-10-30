package com.voicenotesai.domain.usecase

import com.voicenotesai.domain.ai.AIProcessingEngine
import com.voicenotesai.domain.ai.NoteFormat
import com.voicenotesai.domain.ai.NoteGenerationResult
import javax.inject.Inject

/**
 * Use case for generating advanced formatted notes using AI processing engine.
 */
class GenerateAdvancedNotesUseCase @Inject constructor(
    private val aiProcessingEngine: AIProcessingEngine
) {
    
    /**
     * Generates formatted notes from transcript using the specified format.
     */
    suspend operator fun invoke(
        transcript: String,
        format: NoteFormat
    ): NoteGenerationResult {
        return aiProcessingEngine.generateNotes(transcript, format)
    }
    
    /**
     * Generates notes in multiple formats for comparison.
     */
    suspend fun generateMultipleFormats(
        transcript: String,
        formats: List<NoteFormat>
    ): Map<NoteFormat, NoteGenerationResult> {
        val results = mutableMapOf<NoteFormat, NoteGenerationResult>()
        
        formats.forEach { format ->
            val result = aiProcessingEngine.generateNotes(transcript, format)
            results[format] = result
        }
        
        return results
    }
}