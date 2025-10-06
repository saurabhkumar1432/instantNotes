package com.voicenotesai.data.remote.api

import com.voicenotesai.data.remote.model.AnthropicRequest
import com.voicenotesai.data.remote.model.AnthropicResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AnthropicService {
    @POST("v1/messages")
    suspend fun generateMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Body request: AnthropicRequest
    ): Response<AnthropicResponse>
}
