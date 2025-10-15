package com.example.hearme.data.network.models

data class TranscribeRequest(
    val reviewDocumentId: String,
    val audioStoragePath: String,
    val languageCode: String = "es-ES"  // Puedes parametrizarlo si lo deseas
)

data class TranscribeResponse(
    val transcription: String,
    val message: String,
    val status: String
)