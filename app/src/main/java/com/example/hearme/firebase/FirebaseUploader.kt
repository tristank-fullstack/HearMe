package com.example.hearme.firebase

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.io.File

fun uploadAudioToFirebase(
    file: File,
    onSuccess: (downloadUrl: String) -> Unit,
    onFailure: (exception: Exception) -> Unit
) {
    // Obtiene la instancia de Firebase Storage
    val storage = FirebaseStorage.getInstance()
    // Referencia a la carpeta "reviews" con el nombre del archivo
    val audioRef = storage.reference.child("reviews/${file.name}")

    // Convierte el archivo a URI
    val fileUri = Uri.fromFile(file)

    // Inicia la subida del archivo
    audioRef.putFile(fileUri)
        .addOnSuccessListener {
            // Una vez subida, solicita el URL de descarga
            audioRef.downloadUrl.addOnSuccessListener { uri ->
                onSuccess(uri.toString())
            }
        }
        .addOnFailureListener { exception ->
            onFailure(exception)
        }
}