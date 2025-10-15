package com.example.hearme.firebase

import androidx.annotation.OptIn
import androidx.compose.animation.core.copy
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.hearme.data.network.models.AudioReview
import com.example.hearme.viewmodels.UserProfile
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions // Importar SetOptions
import kotlinx.coroutines.tasks.await // Para usar await

/**
 * Agrega una reseña de audio a Firestore en la colección "audioReviews".
 */
suspend fun addAudioReview(
    // Mantenemos el nombre, pero ahora es suspend
    audioReview: AudioReview,
    // onSuccess: () -> Unit, // Ya no necesitamos callbacks si es suspend y la UI se actualiza por listeners
    // onFailure: (Exception) -> Unit
): String { // Devuelve el ID de la reseña
    val db = FirebaseFirestore.getInstance()
    val collectionRef = db.collection("audioReviews")

    // Primero, crea un nuevo documento para obtener su ID
    val newReviewRef = collectionRef.document()
    val reviewId = newReviewRef.id

    val reviewWithIdAndTimestamp = audioReview.copy(
        reviewId = reviewId,
        uploadTimestamp = if (audioReview.uploadTimestamp == 0L) System.currentTimeMillis() else audioReview.uploadTimestamp
    )

    // Luego, establece los datos del documento, incluyendo su propio ID
    newReviewRef.set(reviewWithIdAndTimestamp).await()
    // onSuccess() // Llamada ya no es necesaria aquí directamente
    return reviewId
}

@OptIn(UnstableApi::class)
suspend fun getUserProfile(userId: String): UserProfile? {
    if (userId.isBlank()) {
        Log.w("FirestoreHelper", "getUserProfile: userId está vacío.")
        return null
    }
    // ... (resto de la lógica como antes)
    try {
        val documentSnapshot = FirebaseFirestore.getInstance().collection("users")
            .document(userId)
            .get()
            .await()
        if (documentSnapshot.exists()) {
            return documentSnapshot.toObject(UserProfile::class.java)
        } else {
            Log.w("FirestoreHelper", "getUserProfile: No se encontró perfil para UID: $userId")
            return null
        }
    } catch (e: Exception) {
        Log.e("FirestoreHelper", "getUserProfile: Error obteniendo perfil para UID: $userId", e)
        return null
    }
}

suspend fun deleteAudioReviewPhysically(reviewId: String) { // Nombre cambiado para claridad
    if (reviewId.isBlank()) {
        println("Error: reviewId está vacío, no se puede eliminar.")
        throw IllegalArgumentException("Review ID no puede estar vacío.")
    }
    val reviewRef = FirebaseFirestore.getInstance().collection("audioReviews").document(reviewId)
    try {
        reviewRef.delete().await() // <--- CAMBIO PRINCIPAL AQUÍ
        println("Reseña $reviewId ELIMINADA FÍSICAMENTE de Firestore.")
    } catch (e: Exception) {
        println("Error al eliminar físicamente la reseña $reviewId: ${e.message}")
        throw e
    }
}

fun listenToSavedReviewsForUser(
    userId: String,
    onUpdate: (List<AudioReview>) -> Unit,
    onError: (Exception) -> Unit
): ListenerRegistration { // Devuelve el ListenerRegistration para poder quitarlo
    if (userId.isBlank()) {
        onError(IllegalArgumentException("User ID no puede estar vacío para escuchar reseñas guardadas."))
        // Devuelve un ListenerRegistration "vacío" o maneja el error de otra forma
        return ListenerRegistration { }
    }
    val db = FirebaseFirestore.getInstance()
    val query = db.collection("audioReviews")
        .whereArrayContains("savedBy", userId)
    // .orderBy("uploadTimestamp", Query.Direction.DESCENDING) // Opcional

    return query.addSnapshotListener { snapshot, exception ->
        if (exception != null) {
            onError(exception)
            return@addSnapshotListener
        }
        val reviews = snapshot?.documents?.mapNotNull { doc ->
            val review = doc.toObject(AudioReview::class.java)
            review?.copy(reviewId = doc.id)
        }?.filterNot { it.isDeleted } ?: emptyList()
        onUpdate(reviews)
    }
}


@OptIn(UnstableApi::class)
suspend fun likeReview(reviewId: String, userId: String) {
    val TAG = "FirestoreHelperLike"
    Log.d(TAG, "Attempting to like review: $reviewId by user: $userId")
    if (reviewId.isBlank() || userId.isBlank()) {
        Log.w(TAG, "Review ID or User ID is blank. Cannot like review. ReviewID: '$reviewId', UserID: '$userId'")
        throw IllegalArgumentException("Review ID or User ID cannot be blank for liking.")
    }
    val reviewRef = FirebaseFirestore.getInstance().collection("audioReviews").document(reviewId)
    try {
        // Usar FieldValue.arrayUnion para añadir el userId a la lista 'likedBy'
        // Esto es atómico y maneja la no duplicación.
        reviewRef.update("likedBy", FieldValue.arrayUnion(userId)).await()
        Log.d(TAG, "Review $reviewId liked successfully by $userId. 'likedBy' field updated with arrayUnion.")

        // Opcionalmente, si tienes un contador de likes separado y quieres gestionarlo aquí:
        // CUIDADO: Hacer dos escrituras separadas no es atómico.
        // Es mejor si el contador se actualiza mediante una Cloud Function que escucha los cambios en `likedBy`
        // o si confías en likedBy.size en el cliente.
        // Si DEBES hacerlo aquí, considera una transacción o acepta la no atomicidad.
        // Ejemplo (no recomendado sin transacción para dos campos):
        // reviewRef.update("likesCount", FieldValue.increment(1)).await()
        // Log.d(TAG, "likesCount incremented for review $reviewId")

    } catch (e: Exception) {
        Log.e(TAG, "Error liking review $reviewId: ${e.message}", e)
        throw e
    }
}

@OptIn(UnstableApi::class)
suspend fun unlikeReview(reviewId: String, userId: String) {
    val TAG = "FirestoreHelperUnlike"
    Log.d(TAG, "Attempting to unlike review: $reviewId by user: $userId")
    if (reviewId.isBlank() || userId.isBlank()) {
        Log.w(TAG, "Review ID or User ID is blank. Cannot unlike review. ReviewID: '$reviewId', UserID: '$userId'")
        throw IllegalArgumentException("Review ID or User ID cannot be blank for unliking.")
    }
    val reviewRef = FirebaseFirestore.getInstance().collection("audioReviews").document(reviewId)
    try {
        // Usar FieldValue.arrayRemove para quitar el userId de la lista 'likedBy'
        reviewRef.update("likedBy", FieldValue.arrayRemove(userId)).await()
        Log.d(TAG, "Review $reviewId unliked successfully by $userId. 'likedBy' field updated with arrayRemove.")

        // Opcionalmente, decrementar likesCount (misma advertencia que arriba)
        // reviewRef.update("likesCount", FieldValue.increment(-1)).await()
        // Log.d(TAG, "likesCount decremented for review $reviewId")

    } catch (e: Exception) {
        Log.e(TAG, "Error unliking review $reviewId: ${e.message}", e)
        throw e
    }
}


fun listenToLikedReviewsForUser(
    userId: String,
    onUpdate: (List<AudioReview>) -> Unit,
    onError: (Exception) -> Unit
): ListenerRegistration {
    if (userId.isBlank()) {
        onError(IllegalArgumentException("User ID no puede estar vacío para escuchar reseñas likeadas."))
        return ListenerRegistration { } // Devuelve un listener no-op
    }
    val db = FirebaseFirestore.getInstance()
    // Consulta las reseñas donde el array 'likedBy' contiene el userId
    val query = db.collection("audioReviews")
        .whereArrayContains("likedBy", userId)
    // .orderBy("uploadTimestamp", Query.Direction.DESCENDING) // Opcional: si quieres ordenarlas

    return query.addSnapshotListener { snapshot, exception ->
        if (exception != null) {
            onError(exception)
            return@addSnapshotListener
        }
        val reviews = snapshot?.documents?.mapNotNull { doc ->
            val review = doc.toObject(AudioReview::class.java)
            review?.copy(reviewId = doc.id)
        }?.filterNot { it.isDeleted } ?: emptyList() // Importante: seguir filtrando las eliminadas
        onUpdate(reviews)
    }
}

/**
 * Establece un listener para las reseñas de audio de un local (filtrando por localId).
 */
fun listenToAudioReviews(
    localId: String,
    onUpdate: (List<AudioReview>) -> Unit,
    onError: (exception: Exception) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    db.collection("audioReviews")
        .whereEqualTo("localId", localId)
        // .orderBy("uploadTimestamp", Query.Direction.DESCENDING) // Opcional: ordenar por fecha
        .addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                onError(exception)
                return@addSnapshotListener
            }
            val reviews = snapshot?.documents?.mapNotNull { doc ->
                val review = doc.toObject(AudioReview::class.java)
                // Asegurar que el ID del documento se mapea a reviewId,
                // y que savedBy se deserialice correctamente (debería ser automático)
                review?.copy(reviewId = doc.id)
            } ?: emptyList()
            onUpdate(reviews.filterNot { it.isDeleted })
        }
}

// --- NUEVAS FUNCIONES PARA GUARDAR/DESGUARDAR (igual que antes) ---

/**
 * Guarda una reseña para el usuario actual.
 */
suspend fun saveReviewForUser(reviewId: String, userId: String) {
    if (reviewId.isBlank() || userId.isBlank()) {
        println("Error: reviewId o userId están vacíos al intentar guardar.")
        return
    }
    val reviewRef = FirebaseFirestore.getInstance().collection("audioReviews").document(reviewId)
    reviewRef.update("savedBy", FieldValue.arrayUnion(userId)).await()
    println("Review $reviewId guardada por usuario $userId")
}

/**
 * Quita una reseña de los guardados del usuario actual.
 */
suspend fun unsaveReviewForUser(reviewId: String, userId: String) {
    if (reviewId.isBlank() || userId.isBlank()) {
        println("Error: reviewId o userId están vacíos al intentar desguardar.")
        return
    }
    val reviewRef = FirebaseFirestore.getInstance().collection("audioReviews").document(reviewId)
    reviewRef.update("savedBy", FieldValue.arrayRemove(userId)).await()
    println("Review $reviewId quitada de guardados por usuario $userId")
}

/**
 * Obtiene todas las reseñas guardadas por un usuario específico.
 */
suspend fun getSavedReviewsForUser(userId: String): List<AudioReview> {
    if (userId.isBlank()) return emptyList()
    val db = FirebaseFirestore.getInstance()
    val snapshot = db.collection("audioReviews")
        .whereArrayContains("savedBy", userId)
        // .orderBy("uploadTimestamp", Query.Direction.DESCENDING) // Opcional
        .get()
        .await()

    return snapshot.documents.mapNotNull { doc ->
        val review = doc.toObject(AudioReview::class.java)
        review?.copy(reviewId = doc.id)
    }.filterNot { it.isDeleted }
}