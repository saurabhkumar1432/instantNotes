package com.voicenotesai.data.remote.api

import com.voicenotesai.data.remote.model.OpenAIRequest
import com.voicenotesai.data.remote.model.OpenAIResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAIService {
    @POST("v1/chat/completions")
    suspend fun generateCompletion(
        @Header("Authorization") authorization: String,
        @Body request: OpenAIRequest
    ): Response<OpenAIResponse>
}
