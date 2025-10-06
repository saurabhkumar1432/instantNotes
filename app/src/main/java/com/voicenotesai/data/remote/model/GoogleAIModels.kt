package com.voicenotesai.data.remote.model

import com.google.gson.annotations.SerializedName

data class GoogleAIRequest(
    val contents: List<GoogleAIContent>,
    @SerializedName("generationConfig")
    val generationConfig: GoogleAIGenerationConfig? = null
)

data class GoogleAIContent(
    val parts: List<GoogleAIPart>,
    val role: String? = "user"
)

data class GoogleAIPart(
    val text: String
)

data class GoogleAIGenerationConfig(
    val temperature: Double = 0.7,
    @SerializedName("maxOutputTokens")
    val maxOutputTokens: Int = 1024
)

data class GoogleAIResponse(
    val candidates: List<GoogleAICandidate>,
    @SerializedName("usageMetadata")
    val usageMetadata: GoogleAIUsageMetadata? = null
)

data class GoogleAICandidate(
    val content: GoogleAIContent,
    @SerializedName("finishReason")
    val finishReason: String? = null,
    val index: Int
)

data class GoogleAIUsageMetadata(
    @SerializedName("promptTokenCount")
    val promptTokenCount: Int,
    @SerializedName("candidatesTokenCount")
    val candidatesTokenCount: Int,
    @SerializedName("totalTokenCount")
    val totalTokenCount: Int
)

data class GoogleAIErrorResponse(
    val error: GoogleAIError
)

data class GoogleAIError(
    val code: Int,
    val message: String,
    val status: String
)
