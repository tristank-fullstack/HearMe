package com.example.hearme.data.network.models

import androidx.annotation.Keep

import java.util.Date

@Keep
data class AudioReview(
    val reviewId: String = "",
    val userId: String = "",
    val localId: String = "",
    val userName: String = "",
    val audioTitle: String = "",
    val audioUrl: String = "",
    val coverImageUrl: String? = null,
    val transcription: String? = null,
    val rating: Float = 0f,
    val comment: String? = null,

    val createdAt: Date? = null,
    val uploadTimestamp: Long = 0L,

    val localName: String = "", // Nombre del local/bar de la reseña
    val timestamp: Long = 0L, // <--- CAMPO IMPORTANTE PARA LA FECHA

    // --- CAMPO IMPORTANTE ---
    val isDeleted: Boolean = false, // Para borrado lógico

    val isPrivate: Boolean = false,
    val likedBy: List<String> = emptyList(),
    val savedBy: List<String> = emptyList(),
    val userProfileImageUrl: String? = null,
    val tags: List<String> = emptyList(),
    val category: String = ""
) {
    // Constructor sin argumentos necesario para la deserialización de Firestore
    constructor() : this(
        reviewId = "",
        userId = "",
        userName = "",
        audioTitle = "",
        audioUrl = "",
        coverImageUrl = null,
        transcription = null,
        rating = 0f,
        comment = null,
        createdAt = null,
        uploadTimestamp = 0L,
        isDeleted = false, // <--- VALOR POR DEFECTO PARA EL CONSTRUCTOR
        isPrivate = false,
        likedBy = emptyList(),
        savedBy = emptyList(),
        userProfileImageUrl = null,
        tags = emptyList(),
        localName = "",
        localId = "",
        category = "",
        timestamp = 0L
    )
}