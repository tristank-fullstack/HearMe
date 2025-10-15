package com.example.hearme.ui.screens


import android.Manifest
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.hearme.data.components.AudioReviewList
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.hearme.firebase.listenToAudioReviews
import coil.compose.AsyncImage
import com.example.hearme.data.components.AudioReviewList
import com.example.hearme.data.network.models.AudioReview
import com.example.hearme.firebase.StorageHelper
import com.example.hearme.firebase.addAudioReview
import com.example.hearme.firebase.uploadAudioToFirebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.hearme.firebase.addAudioReview // Importar la función (mismo nombre)
import kotlinx.coroutines.launch // Importar launch
import androidx.compose.runtime.rememberCoroutineScope


@Composable
fun ReviewScreen(
    placeId: String,         // ID del local
    localName: String,       // Nombre del bar obtenido de la API (dato a guardar)
    localImage: String,      // No se usa en este fragmento pero lo mantenemos por si acaso
    userName: String         // Nombre del usuario actual (no se usa directamente en la lógica de escucha)
) {
    // Lista mutable para almacenar las reseñas de audio (se actualizan en tiempo real)
    val audioReviews = remember { mutableStateListOf<AudioReview>() }
    val coroutineScope = rememberCoroutineScope()

    // Escucha en tiempo real las reseñas para el local especificado
    LaunchedEffect(placeId) {
        listenToAudioReviews(
            localId = placeId,
            onUpdate = { reviews ->
                // ---- INICIO: LÓGICA DE ORDENACIÓN ----
                // Ordenar las reseñas por la cantidad de likes en orden descendente.
                // Asumimos que AudioReview tiene un campo `likedBy: List<String>`
                val sortedReviews = reviews.sortedByDescending { it.likedBy.size }
                // ---- FIN: LÓGICA DE ORDENACIÓN ----

                audioReviews.clear()
                audioReviews.addAll(sortedReviews) // Añadir las reseñas ordenadas
            },
            onError = { exception ->
                // Usar Log.e para errores es más estándar
                Log.e("ReviewScreen", "Error al cargar las reseñas: ${exception.message}", exception)
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            Text(
                text = "Reseñas de $localName",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))

            // AudioReviewList ahora recibirá la lista ya ordenada.
            // Si AudioReviewList es un LazyColumn directamente aquí, no necesitas pasar `audioReviews`
            // sino que usarías `items(audioReviews) { review -> AudioReviewItem(review) }`
            // Por ahora, asumimos que AudioReviewList maneja internamente la presentación de la lista.
            AudioReviewList(audioReviews = audioReviews)
        }

        Divider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.outline,
            thickness = 1.dp
        )

        AudioRecorderPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            onAudioRecorded = { audioFile ->
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    Log.w("ReviewScreen", "Usuario no autenticado, no se puede subir el audio")
                    return@AudioRecorderPanel
                }

                val db = FirebaseFirestore.getInstance()
                db.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        val profileUserName = document.getString("username") ?: "Usuario desconocido"

                        StorageHelper.uploadAudioToFirebase(
                            file = audioFile,
                            onSuccess = { downloadUrl ->
                                val newReview = AudioReview(
                                    localId = placeId,
                                    userName = profileUserName,
                                    audioUrl = downloadUrl,
                                    userId = currentUser.uid,
                                    localName = localName,
                                    // likedBy se inicializará como lista vacía por defecto en el modelo
                                    // timestamp se inicializará en addAudioReview o por defecto
                                )
                                coroutineScope.launch {
                                    try {
                                        val generatedReviewId = addAudioReview(newReview)
                                        Log.i("ReviewScreen", "Reseña añadida con ID: $generatedReviewId")
                                        // La UI se actualiza a través del listener, que ahora ordenará
                                    } catch (e: Exception) {
                                        Log.e("ReviewScreen", "Error al guardar la reseña en Firestore", e)
                                    }
                                }
                            },
                            onFailure = { exception ->
                                Log.e("ReviewScreen", "Error al subir el audio", exception)
                            }
                        )
                    }
                    .addOnFailureListener { exception ->
                        Log.e("ReviewScreen", "Error al obtener el perfil de usuario", exception)
                    }
            },
            onRecordingCanceled = {
                Log.d("ReviewScreen", "Grabación cancelada")
            }
        )
    }
}