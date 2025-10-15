package com.example.hearme.data.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hearme.data.network.models.AudioReview
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import kotlinx.coroutines.delay


@Composable
fun MisResenasItem(
    audioReview: AudioReview,
    modifier: Modifier = Modifier
) {
    // Configuración del ExoPlayer para reproducir el audio de la reseña
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(audioReview.audioUrl)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = false
        }
    }
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var currentTime by remember { mutableStateOf("00:00") }
    var totalDuration by remember { mutableStateOf("00:00") }
    val animatedProgress by animateFloatAsState(targetValue = progress)

    // Actualización del progreso y tiempos cada 50ms
    LaunchedEffect(exoPlayer, isPlaying) {
        while (true) {
            val duration = exoPlayer.duration.coerceAtLeast(1L)
            val totMin = (duration / 1000) / 60
            val totSec = (duration / 1000) % 60
            totalDuration = String.format("%02d:%02d", totMin, totSec)

            if (isPlaying) {
                val curPos = exoPlayer.currentPosition.toFloat() / duration.toFloat()
                progress = curPos.coerceIn(0f, 1f)
                val curMin = (exoPlayer.currentPosition / 1000) / 60
                val curSec = (exoPlayer.currentPosition / 1000) % 60
                currentTime = String.format("%02d:%02d", curMin, curSec)

                if (exoPlayer.currentPosition >= duration) {
                    exoPlayer.pause()
                    exoPlayer.seekTo(0)
                    isPlaying = false
                    progress = 0f
                    currentTime = "00:00"
                }
            }
            delay(50)
        }
    }
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    // Diseño del item: contenedor con borde, padding y controles de audio
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 8.dp),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Fila superior: muestra "TU" a la izquierda y "ver" a la derecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "TU",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "ver",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable { /* Acción pendiente: puedes navegar a la vista de detalle */ }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Fila inferior: controles de audio (play/pause, tiempo, slider y duración)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    if (isPlaying) {
                        exoPlayer.pause()
                        isPlaying = false
                    } else {
                        exoPlayer.play()
                        isPlaying = true
                    }
                }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Slider(
                    value = animatedProgress,
                    onValueChange = { newPosition ->
                        progress = newPosition
                        val seekPosition = (newPosition * exoPlayer.duration).toLong()
                        exoPlayer.seekTo(seekPosition)
                    },
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = totalDuration,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}