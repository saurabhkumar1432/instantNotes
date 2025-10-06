package com.voicenotesai.data.model

/**
 * AI provider settings including API configuration and custom prompt template.
 */
data class AISettings(
    val provider: AIProvider,
    val apiKey: String,
    val model: String,
    val isValidated: Boolean = false,
    val promptTemplate: String = DEFAULT_PROMPT_TEMPLATE
) {
    companion object {
        const val DEFAULT_PROMPT_TEMPLATE = """You are an AI assistant that converts voice transcriptions into well-formatted notes.

Please analyze the following transcribed text and create structured notes with these requirements:
1. Extract key points and organize them clearly
2. Fix any grammatical errors
3. Format with appropriate headings and bullet points
4. Preserve the original meaning and important details
5. Make it easy to scan and understand

Transcribed text:
{transcription}

Please provide the formatted notes:"""
    }
}
