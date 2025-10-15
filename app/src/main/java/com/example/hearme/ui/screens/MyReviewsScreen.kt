package com.example.hearme.ui.screens

import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.hearme.data.network.models.AudioReview
import com.example.hearme.data.network.models.AudioReview as AppAudioReview // Alias para claridad

// Modelo para la reseña (cada review)
data class AudioReview(
    val reviewId: String = "",
    val userName: String = "",
    val audioUrl: String = "",
    val userId: String = "",
    val localName: String = "" // Aquí se guarda el nombre específico del bar para esa reseña
)



// Composable que muestra cada review individual.
// Se usa ExoPlayer para reproducir el audio y se visualiza el nombre del local.
@Composable
fun MyReviewItem(review: AppAudioReview) { // Usamos el alias AppAudioReview que apunta a tu modelo
    val context = LocalContext.current
    val TAG = "MyReviewItemOriginal" // Tag para diferenciar logs si es necesario

    // Configuramos ExoPlayer con la URL del audio de la review.
    // Usamos androidx.media3.exoplayer.ExoPlayer
    val exoPlayer = remember(review.audioUrl) {
        ExoPlayer.Builder(context).build().apply {
            if (review.audioUrl.isNotBlank()) { // Solo intentar si la URL no está vacía
                try {
                    // Usar androidx.media3.common.MediaItem
                    setMediaItem(MediaItem.fromUri(Uri.parse(review.audioUrl)))
                    prepare()
                } catch (e: Exception) {
                    // Loguear el error si la preparación falla (ej. URL inválida)
                    Log.e(TAG, "Error preparing MediaItem for review ${review.reviewId}, URL: ${review.audioUrl}", e)
                }
            } else {
                Log.w(TAG, "Audio URL is blank for review ${review.reviewId}. ExoPlayer not preparing media.")
            }
            playWhenReady = false
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var currentTime by remember { mutableStateOf("00:00") }
    var totalDuration by remember { mutableStateOf("00:00") }
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "myReviewProgress")


    LaunchedEffect(exoPlayer, isPlaying) {
        while (true) {
            // Comprobar que ExoPlayer está listo y tiene duración
            // Usar androidx.media3.common.Player.STATE_READY
            if (exoPlayer.playbackState == androidx.media3.common.Player.STATE_READY && exoPlayer.duration > 0) {
                val duration = exoPlayer.duration.coerceAtLeast(1L)
                val totMin = (duration / 1000) / 60
                val totSec = (duration / 1000) % 60
                totalDuration = String.format("%02d:%02d", totMin, totSec)

                if (isPlaying) {
                    val currentPosition = exoPlayer.currentPosition
                    // Asegurar que no haya división por cero si la duración es 0
                    progress = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
                    val curMin = (currentPosition / 1000) / 60
                    val curSec = (currentPosition / 1000) % 60
                    currentTime = String.format("%02d:%02d", curMin, curSec)

                    // Comprobar si el audio ha terminado
                    if (currentPosition >= duration && duration > 0) {
                        exoPlayer.pause()
                        exoPlayer.seekTo(0) // Reiniciar al principio
                        isPlaying = false
                        progress = 0f
                        currentTime = "00:00" // Resetear tiempo
                    }
                }
            } else if (exoPlayer.playbackState == androidx.media3.common.Player.STATE_IDLE && review.audioUrl.isBlank()) {
                // Si la URL está en blanco y el player está inactivo, mostrar tiempos por defecto
                totalDuration = "--:--"
                currentTime = "--:--"
                progress = 0f
            }
            kotlinx.coroutines.delay(200) // Un delay un poco mayor puede ser suficiente y más eficiente
        }
    }

    DisposableEffect(Unit) { // Usar Unit si el efecto no depende de una clave que cambie
        onDispose {
            exoPlayer.release()
        }
    }

    // ========= INICIO DE LOS CAMBIOS DE DISEÑO =========
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 8.dp), // Padding exterior del Surface
        shape = MaterialTheme.shapes.medium, // Bordes redondeados
        tonalElevation = 2.dp, // Sombra sutil
        color = MaterialTheme.colorScheme.surfaceVariant // Color de fondo del Surface
    ) {
        Column(modifier = Modifier.padding(12.dp)) { // Padding interior del Column
            // Fila para información de la reseña (Nombre Local y Nombre Usuario)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Columna para Nombre del Local y Nombre del Usuario
                Column(
                    modifier = Modifier
                        .weight(1f) // Ocupar espacio disponible a la izquierda
                        .padding(end = 8.dp) // Espacio por si se añaden elementos a la derecha
                ) {
                    // Se muestra el nombre del local.
                    Text(
                        text = if (review.localName.isNotBlank()) review.localName else "Sin lugar asignado",
                        style = MaterialTheme.typography.titleLarge.copy( // Estilo para el nombre del local
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface // Color del texto
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Muestra el nombre del usuario (o "Tú" si está vacío).
                    // Para "Mis Reseñas", es probable que userName sea el del usuario o simplemente "Tú".
                    Text(
                        text = "Publicado por: ${if (review.userName.isNotBlank()) review.userName else "Tú"}",
                        style = MaterialTheme.typography.bodyMedium, // Estilo para el nombre de usuario
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // No hay botones de acción (Like, Guardar, Eliminar) aquí como en el otro AudioReviewItem,
                // pero se mantiene la estructura por si se quisieran añadir en el futuro.
            }

            Spacer(modifier = Modifier.height(12.dp)) // Espacio antes de los controles de audio

            // Controles para reproducir/pausar el audio (diseño similar al otro item)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (review.audioUrl.isNotBlank()) { // Solo permitir acción si hay URL
                        if (isPlaying) {
                            exoPlayer.pause()
                        } else {
                            // Si el reproductor está en IDLE o ENDED, y se presiona play:
                            // La preparación ya ocurrió en el `remember` si la URL era válida.
                            // Si terminó, se podría querer reiniciar.
                            if (exoPlayer.playbackState == androidx.media3.common.Player.STATE_ENDED) {
                                exoPlayer.seekTo(0) // Vuelve al inicio
                            }
                            exoPlayer.play()
                        }
                        isPlaying = !isPlaying
                    }
                }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        tint = MaterialTheme.colorScheme.primary, // Color primario para el botón
                        modifier = Modifier.size(36.dp) // Tamaño del icono
                    )
                }
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.bodySmall, // Estilo para los tiempos
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Slider(
                    value = animatedProgress,
                    onValueChange = { newValue ->
                        // Permitir búsqueda solo si ExoPlayer está listo y hay duración
                        if (exoPlayer.duration > 0 && (exoPlayer.playbackState == androidx.media3.common.Player.STATE_READY || exoPlayer.playbackState == androidx.media3.common.Player.STATE_BUFFERING)) {
                            progress = newValue
                            val seekPosition = (newValue * exoPlayer.duration).toLong()
                            exoPlayer.seekTo(seekPosition)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                Text(
                    text = totalDuration,
                    style = MaterialTheme.typography.bodySmall, // Estilo para los tiempos
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
    // ========= FIN DE LOS CAMBIOS DE DISEÑO =========
}


// Pantalla que muestra la lista de reseñas del usuario.
// Se recuperan las reviews desde la colección "audioReviews" de Firestore filtrando por userId.
// Cada documento debe incluir el campo "localName". Si no existe, se usa el valor de defaultBarName.
@Composable
fun MyReviewsScreen(defaultBarName: String) {
    val reviewsState = remember { mutableStateOf<List<AppAudioReview>>(emptyList()) } // Usar AppAudioReview
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val TAG_SCREEN = "MyReviewsScreenOriginal" // Tag para diferenciar logs

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            firestore.collection("audioReviews")
                .whereEqualTo("userId", user.uid)
                // Opcional: Podrías añadir un .orderBy("timestamp", Query.Direction.DESCENDING)
                // si tienes un campo timestamp y quieres ordenar las reseñas.
                .get()
                .addOnSuccessListener { snapshot ->
                    val list = snapshot.documents.mapNotNull { doc ->
                        val reviewId = doc.id
                        val audioUrl = doc.getString("audioUrl") ?: ""
                        // El userName en tus reseñas podría ser el displayName del usuario al momento de crear la reseña.
                        // O podría ser un campo que llenas explícitamente.
                        val profileUserName = doc.getString("userName") ?: user.displayName ?: "" // Usar displayName como fallback
                        val localNameFromReview = doc.getString("localName") ?: defaultBarName

                        if (audioUrl.isNotEmpty()) {
                            // Asegúrate que el constructor de AppAudioReview coincida con los campos que lees
                            AppAudioReview( // Usar el constructor de AppAudioReview
                                reviewId = reviewId,
                                userName = profileUserName,
                                audioUrl = audioUrl,
                                userId = user.uid,
                                localName = localNameFromReview
                                // Si AppAudioReview tiene más campos (ej. likedBy, savedBy, timestamp),
                                // asegúrate de que o bien tienen valores por defecto o los mapeas aquí si es necesario.
                                // Para MyReviewItem, solo se usan los campos listados arriba.
                            )
                        } else {
                            Log.w(TAG_SCREEN, "Review $reviewId skipped, audioUrl is empty.")
                            null
                        }
                    }
                    reviewsState.value = list
                    if (list.isEmpty()) {
                        Log.d(TAG_SCREEN, "No reviews found for user ${user.uid}")
                    } else {
                        Log.d(TAG_SCREEN, "Fetched ${list.size} reviews for user ${user.uid}")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG_SCREEN, "Error retrieving reviews for user ${user.uid}", exception)
                }
        } ?: run {
            // Si el usuario es null (ej. no logueado o se deslogueó)
            Log.d(TAG_SCREEN, "Current user is null, not fetching reviews.")
            reviewsState.value = emptyList() // Limpiar la lista
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 8.dp) // Padding general para la lista
    ) {
        if (reviewsState.value.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillParentMaxSize() // Para que el Column ocupe todo el espacio de LazyColumn
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        // Mensaje más amigable
                        text = "Aún no has publicado ninguna reseña.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            // Añadir key para mejor rendimiento y manejo de estado en LazyColumn
            items(reviewsState.value, key = { it.reviewId }) { review ->
                MyReviewItem(review = review)
                // Opcional: Añadir un Divider para separar visualmente los items
                // Divider(modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }
}