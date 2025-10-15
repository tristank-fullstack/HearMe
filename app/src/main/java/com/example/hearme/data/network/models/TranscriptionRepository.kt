package com.example.hearme.data.network.models

import com.example.hearme.data.network.RetrofitClient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

object TranscriptionRepository {
    suspend fun transcribeAudio(
        reviewDocumentId: String,
        audioStoragePath: String
    ): TranscribeResponse {
        // Verifica que haya un usuario autenticado
        val currentUser = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("El usuario no está autenticado.")

        val tokenResult = currentUser.getIdToken(false).await()
        val idToken = tokenResult.token ?: throw IllegalStateException("No se pudo obtener el token.")
        // Formatea la cabecera (estándar Bearer)
        val authHeader = "Bearer $idToken"

        // Crea la solicitud
        val request = TranscribeRequest(
            reviewDocumentId = reviewDocumentId,
            audioStoragePath = audioStoragePath,
            languageCode = "es-ES"
        )

        // Llama al endpoint y devuelve la respuesta
        return RetrofitClient.transcriptionApiService.transcribeAudio(request, authHeader)
    }
}