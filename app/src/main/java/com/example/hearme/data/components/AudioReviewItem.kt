package com.example.hearme.data.components


import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.text.font.FontWeight
import androidx.media3.exoplayer.ExoPlayer
import com.example.hearme.data.network.models.AudioReview
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.runtime.rememberCoroutineScope // Importar
import com.example.hearme.firebase.saveReviewForUser // Importar
import com.example.hearme.firebase.unsaveReviewForUser // Importar
import kotlinx.coroutines.launch // Importar
import androidx.compose.material.icons.outlined.BookmarkBorder // Asegurar importación
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.hearme.data.network.models.TranscriptionRepository
import com.example.hearme.data.network.models.getPathFromFirebaseStorageUrl
import com.example.hearme.firebase.deleteAudioReviewPhysically
import com.example.hearme.firebase.likeReview
import com.example.hearme.firebase.unlikeReview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
}


@OptIn(UnstableApi::class)
@Composable
fun AudioReviewItem(
    audioReview: AudioReview,
    modifier: Modifier = Modifier
) {
    val TAG = "AudioReviewItem"
    val currentUser = FirebaseAuth.getInstance().currentUser
    val isMyReview = currentUser?.uid == audioReview.userId
    val coroutineScope = rememberCoroutineScope() // Usar el scope de la composición

    // Log para ver el estado inicial del audioReview
    LaunchedEffect(audioReview) {
        Log.d(TAG, "Composing AudioReviewItem for reviewId: ${audioReview.reviewId}, localName: '${audioReview.localName}', audioUrl: '${audioReview.audioUrl}'")
    }


    var isSaved by remember(audioReview.reviewId, audioReview.savedBy, currentUser?.uid) {
        val saved = currentUser?.uid?.let { uid -> audioReview.savedBy.contains(uid) } ?: false
        mutableStateOf(saved)
    }

    var isLikedByCurrentUser by remember(audioReview.reviewId, audioReview.likedBy, currentUser?.uid) {
        val liked = currentUser?.uid?.let { uid -> audioReview.likedBy.contains(uid) } ?: false
        mutableStateOf(liked)
    }
    val likeCount = audioReview.likedBy.size // Esto se actualizará si audioReview se recompone con nuevos datos

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val exoPlayer = remember(audioReview.audioUrl) {
        ExoPlayer.Builder(context).build().apply {
            if (audioReview.audioUrl.isNotBlank()) {
                try {
                    val mediaItem = MediaItem.fromUri(Uri.parse(audioReview.audioUrl))
                    setMediaItem(mediaItem)
                    prepare()
                } catch (e: Exception) {
                    Log.e(TAG, "Error preparing MediaItem for review ${audioReview.reviewId}, URL: ${audioReview.audioUrl}", e)
                }
            }
            playWhenReady = false
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var currentTime by remember { mutableStateOf("00:00") }
    var totalDuration by remember { mutableStateOf("00:00") }
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "audioReviewProgress")

    // Estado para la transcripción y el estado de carga/error de la transcripción
    var transcriptionText by remember(audioReview.reviewId, audioReview.transcription) { mutableStateOf(audioReview.transcription) }
    var isTranscribing by remember { mutableStateOf(false) }
    var transcriptionError by remember { mutableStateOf<String?>(null) }


    LaunchedEffect(exoPlayer, isPlaying) {
        while (true) {
            if (exoPlayer.playbackState == Player.STATE_READY && exoPlayer.duration > 0) {
                val duration = exoPlayer.duration.coerceAtLeast(1L)
                val totMin = (duration / 1000) / 60
                val totSec = (duration / 1000) % 60
                totalDuration = String.format("%02d:%02d", totMin, totSec)

                if (isPlaying) {
                    val currentPosition = exoPlayer.currentPosition
                    progress = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
                    val curMin = (currentPosition / 1000) / 60
                    val curSec = (currentPosition / 1000) % 60
                    currentTime = String.format("%02d:%02d", curMin, curSec)

                    if (currentPosition >= duration && duration > 0) {
                        exoPlayer.pause()
                        exoPlayer.seekTo(0)
                        isPlaying = false
                        progress = 0f
                        currentTime = "00:00"
                    }
                }
            } else if (exoPlayer.playbackState == Player.STATE_IDLE && audioReview.audioUrl.isBlank()) {
                totalDuration = "--:--"
                currentTime = "--:--"
                progress = 0f
            }
            delay(200)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro de que quieres eliminar esta reseña? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                deleteAudioReviewPhysically(audioReview.reviewId)
                                // Aquí podrías querer notificar a la pantalla principal para que actualice la lista
                            } catch (e: Exception) {
                                Log.e(TAG, "Error en eliminación física de review ${audioReview.reviewId}", e)
                            }
                        }
                        showDeleteConfirmDialog = false
                    }
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 8.dp),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = if (isMyReview) "Tú" else audioReview.userName.ifBlank { "Usuario desconocido" },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    if (audioReview.localName.isNotBlank()) {
                        Text(
                            text = "en: ${audioReview.localName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = {
                            currentUser?.uid?.let { userId ->
                                if (audioReview.reviewId.isNotBlank()) {
                                    val newLikedState = !isLikedByCurrentUser
                                    // Optimistic update
                                    isLikedByCurrentUser = newLikedState
                                    // Actualizar el contador localmente (o esperar a que el modelo se actualice)
                                    // val newLikeCount = if (newLikedState) likeCount + 1 else likeCount -1
                                    // audioReview = audioReview.copy(likedBy = if(newLikedState) audioReview.likedBy + userId else audioReview.likedBy - userId)

                                    coroutineScope.launch {
                                        try {
                                            if (newLikedState) {
                                                likeReview(audioReview.reviewId, userId)
                                            } else {
                                                unlikeReview(audioReview.reviewId, userId)
                                            }
                                            // No es necesario actualizar el estado aquí si el ViewModel observa cambios en Firestore
                                        } catch (e: Exception) {
                                            isLikedByCurrentUser = !newLikedState // Revert on error
                                            Log.e(TAG, "Error like/unlike: ${e.message}", e)
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isLikedByCurrentUser) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isLikedByCurrentUser) "Quitar me gusta" else "Dar me gusta",
                            tint = if (isLikedByCurrentUser) Color.Red else LocalContentColor.current
                        )
                    }
                    Text(
                        text = likeCount.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 2.dp, end = 8.dp)
                    )
                    IconButton(
                        onClick = {
                            currentUser?.uid?.let { userId ->
                                if (audioReview.reviewId.isNotBlank()) {
                                    val newSavedState = !isSaved
                                    isSaved = newSavedState // Optimistic update
                                    coroutineScope.launch {
                                        try {
                                            if (newSavedState) {
                                                saveReviewForUser(audioReview.reviewId, userId)
                                            } else {
                                                unsaveReviewForUser(audioReview.reviewId, userId)
                                            }
                                        } catch (e: Exception) {
                                            isSaved = !newSavedState // Revert on error
                                            Log.e(TAG, "Error save/unsave: ${e.message}", e)
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isSaved) "Quitar de guardados" else "Guardar reseña",
                            tint = if (isSaved) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    if (isMyReview) {
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Eliminar reseña"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    if (audioReview.audioUrl.isNotBlank()) {
                        if (isPlaying) {
                            exoPlayer.pause()
                        } else {
                            if (exoPlayer.playbackState == Player.STATE_IDLE || exoPlayer.playbackState == Player.STATE_ENDED) {
                                if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                    exoPlayer.seekTo(0)
                                }
                                // No es necesario preparar de nuevo si ya se hizo en el remember
                            }
                            exoPlayer.play()
                        }
                        isPlaying = !isPlaying
                    }
                }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Slider(
                    value = animatedProgress,
                    onValueChange = { newPosition ->
                        if (exoPlayer.duration > 0 && (exoPlayer.playbackState == Player.STATE_READY || exoPlayer.playbackState == Player.STATE_BUFFERING)) {
                            progress = newPosition
                            val seekPosition = (newPosition * exoPlayer.duration).toLong()
                            exoPlayer.seekTo(seekPosition)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = totalDuration,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- SECCIÓN DE TRANSCRIPCIÓN ---
            if (isTranscribing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Transcribiendo...", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (transcriptionError != null) {
                Text(
                    text = "Error: $transcriptionError",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (!transcriptionText.isNullOrBlank()) {
                Text(
                    text = "Transcripción:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = transcriptionText!!,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else { // No hay transcripción, no se está transcribiendo y no hay error
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = "Transcribir",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier
                            .clickable {
                                if (audioReview.audioUrl.isBlank()) {
                                    Log.w(TAG, "URL de audio vacía, no se puede transcribir.")
                                    transcriptionError = "URL de audio no disponible."
                                    return@clickable
                                }

                                val rawAudioUrl = audioReview.audioUrl
                                val storagePathToSend = getPathFromFirebaseStorageUrl(rawAudioUrl)

                                if (storagePathToSend != null) {
                                    Log.d(
                                        TAG,
                                        "Botón Transcribir clickeado para review: ${audioReview.reviewId}"
                                    )
                                    Log.d(TAG, "Raw Audio URL: $rawAudioUrl")
                                    Log.d(TAG, "Storage Path to send to API: $storagePathToSend")

                                    isTranscribing = true
                                    transcriptionError = null // Limpiar error anterior

                                    coroutineScope.launch(Dispatchers.IO) { // Usar el scope recordado y cambiar a IO
                                        try {
                                            val response = TranscriptionRepository.transcribeAudio(
                                                reviewDocumentId = audioReview.reviewId,
                                                audioStoragePath = storagePathToSend
                                                // languageCode = "es-ES" // Opcional si tu API tiene un default
                                            )
                                            withContext(Dispatchers.Main) {
                                                Log.d(
                                                    TAG,
                                                    "Transcripción obtenida: ${response.transcription}"
                                                )
                                                transcriptionText = response.transcription
                                                // La UI se actualizará porque transcriptionText es un estado
                                                // Si la API actualiza Firestore, y el ViewModel observa Firestore,
                                                // el audioReview también podría actualizarse desde la fuente de verdad.
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error al transcribir: ${e.message}", e)
                                            withContext(Dispatchers.Main) {
                                                transcriptionError =
                                                    e.localizedMessage ?: "Error desconocido"
                                            }
                                        } finally {
                                            withContext(Dispatchers.Main) {
                                                isTranscribing = false
                                            }
                                        }
                                    }
                                } else {
                                    Log.e(
                                        TAG,
                                        "No se pudo extraer la ruta de Storage de la URL: $rawAudioUrl. No se enviará la solicitud."
                                    )
                                    transcriptionError = "URL de audio inválida."
                                }
                            }
                            .padding(vertical = 4.dp)
                    )
                }
            }
            // --- FIN SECCIÓN DE TRANSCRIPCIÓN ---
        }
    }
}
// Función auxiliar para formatear el tiempo en mm:ss.
fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}