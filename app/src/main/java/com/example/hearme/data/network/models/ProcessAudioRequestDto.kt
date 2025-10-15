package com.example.hearme.data.network.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProcessAudioRequestDto(
    @Json(name = "reviewDocumentId") val reviewDocumentId: String,
    @Json(name = "audioStoragePath") val audioStoragePath: String,
    @Json(name = "languageCode") val languageCode: String = "es-ES" // Por defecto español
)