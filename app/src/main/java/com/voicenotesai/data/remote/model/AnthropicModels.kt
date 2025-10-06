package com.voicenotesai.data.remote.model

import com.google.gson.annotations.SerializedName

data class AnthropicRequest(
    val model: String,
    val messages: List<AnthropicMessage>,
    @SerializedName("max_tokens")
    val maxTokens: Int = 1024,
    val temperature: Double = 0.7
)

data class AnthropicMessage(
    val role: String,
    val content: String
)

data class AnthropicResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<AnthropicContent>,
    val model: String,
    @SerializedName("stop_reason")
    val stopReason: String? = null,
    val usage: AnthropicUsage? = null
)

data class AnthropicContent(
    val type: String,
    val text: String
)

data class AnthropicUsage(
    @SerializedName("input_tokens")
    val inputTokens: Int,
    @SerializedName("output_tokens")
    val outputTokens: Int
)

data class AnthropicErrorResponse(
    val type: String,
    val error: AnthropicError
)

data class AnthropicError(
    val type: String,
    val message: String
)
