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
        const val DEFAULT_PROMPT_TEMPLATE = """Convert the following voice transcription into well-formatted notes. Follow these rules:

1. Extract and organize key points clearly
2. Fix any grammatical errors from speech-to-text
3. Use headings, bullet points, and numbered lists where appropriate
4. Preserve all important details and meaning
5. Make it scannable and easy to read

IMPORTANT: Respond ONLY with the formatted notes. Do NOT include any introductory text like "Here are your notes" or "Based on your transcription". Just provide the notes directly.

Transcription:
{transcription}"""
    }
}
