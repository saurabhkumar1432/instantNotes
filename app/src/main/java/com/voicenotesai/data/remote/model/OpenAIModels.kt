package com.voicenotesai.data.remote.model

import com.google.gson.annotations.SerializedName

data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val temperature: Double = 0.7,
    @SerializedName("max_tokens")
    val maxTokens: Int? = null
)

data class OpenAIMessage(
    val role: String,
    val content: String
)

data class OpenAIResponse(
    val id: String,
    val choices: List<OpenAIChoice>,
    val usage: OpenAIUsage? = null
)

data class OpenAIChoice(
    val index: Int,
    val message: OpenAIMessage,
    @SerializedName("finish_reason")
    val finishReason: String
)

data class OpenAIUsage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    @SerializedName("total_tokens")
    val totalTokens: Int
)

data class OpenAIErrorResponse(
    val error: OpenAIError
)

data class OpenAIError(
    val message: String,
    val type: String,
    val code: String? = null
)
