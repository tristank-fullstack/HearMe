package com.example.hearme.data.network.api

import com.example.hearme.data.network.models.TranscribeRequest
import com.example.hearme.data.network.models.TranscribeResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface TranscriptionApiService {
    @POST("api/reviews/process-audio")
    suspend fun transcribeAudio(
        @Body request: TranscribeRequest,
        @Header("Authorization") authHeader: String
    ): TranscribeResponse
}
