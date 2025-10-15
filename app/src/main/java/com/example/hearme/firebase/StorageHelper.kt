package com.example.hearme.firebase

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.io.File

object StorageHelper {
    fun uploadAudioToFirebase(
        file: File,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Se sube el archivo en la carpeta "audio/"
        val storageRef = FirebaseStorage.getInstance().reference.child("audio/${file.name}")

        println("🚀 Intentando subir audio: ${file.name} (Tamaño: ${file.length()} bytes)")

        val uploadTask = storageRef.putFile(Uri.fromFile(file))
        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                println("🎤 Audio subido correctamente: ${uri.toString()}")
                onSuccess(uri.toString())
            }.addOnFailureListener { exception ->
                println("❌ Error obteniendo URL de descarga: ${exception.message}")
                onFailure(exception)
            }
        }.addOnFailureListener { e ->
            println("❌ Error al subir audio: ${e.message}")
            onFailure(e)
        }
    }
}