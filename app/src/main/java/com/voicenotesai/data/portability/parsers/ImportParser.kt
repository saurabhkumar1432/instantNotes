package com.voicenotesai.data.portability.parsers

import com.voicenotesai.domain.model.EnhancedNote
import java.io.File

/**
 * Interface for parsing imported data into notes
 */
interface ImportParser {
    /**
     * Parse the file and return a list of notes
     */
    suspend fun parse(file: File): ParseResult
    
    /**
     * Validate the file format before parsing
     */
    suspend fun validateFormat(file: File): ValidationResult
}

/**
 * Result of parsing operation
 */
sealed class ParseResult {
    data class Success(
        val notes: List<EnhancedNote>,
        val warnings: List<String> = emptyList()
    ) : ParseResult()
    
    data class Failure(
        val error: String,
        val lineNumber: Int? = null,
        val cause: Throwable? = null
    ) : ParseResult()
}

/**
 * Result of format validation
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    
    data class Invalid(
        val issues: List<String>
    ) : ValidationResult()
}