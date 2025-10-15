package com.example.hearme.data.network.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProcessAudioResponseDto(
    @Json(name = "status") val status: String,
    @Json(name = "message") val message: String?,
    @Json(name = "transcription") val transcription: String?
)