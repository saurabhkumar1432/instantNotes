package com.voicenotesai.data.remote.api

import com.voicenotesai.data.remote.model.GoogleAIRequest
import com.voicenotesai.data.remote.model.GoogleAIResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GoogleAIService {
    @POST("v1/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GoogleAIRequest
    ): Response<GoogleAIResponse>
}
