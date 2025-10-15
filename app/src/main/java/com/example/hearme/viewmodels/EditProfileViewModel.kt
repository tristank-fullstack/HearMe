package com.example.hearme.viewmodels

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Estado para la pantalla de edición
data class EditProfileUiState(
    val isLoading: Boolean = false, // ¿Cargando datos iniciales?
    val isSaving: Boolean = false,  // ¿Guardando cambios?
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val currentUsername: String = "",
    val currentImageUrl: String? = null,
    val selectedImageUri: Uri? = null // Uri local de la imagen seleccionada
)

class EditProfileViewModel : ViewModel() {

    var uiState by mutableStateOf(EditProfileUiState())
        private set

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance() // Instancia de Storage

    private var originalUsername: String = "" // Para saber si el username cambió

    init {
        loadInitialData()
    }

    // Carga datos actuales al entrar en la pantalla
    private fun loadInitialData() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            uiState = uiState.copy(error = "Usuario no encontrado", isLoading = false)
            return
        }

        uiState = uiState.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val doc = firestore.collection("users").document(userId).get().await()
                if (doc.exists()) {
                    originalUsername = doc.getString("username") ?: ""
                    uiState = uiState.copy(
                        isLoading = false,
                        currentUsername = originalUsername,
                        currentImageUrl = doc.getString("profileImageUrl")
                    )
                } else {
                    uiState = uiState.copy(isLoading = false, error = "Perfil no encontrado.")
                }
            } catch (e: Exception) {
                Log.e("EditProfileVM", "Error cargando datos iniciales", e)
                uiState = uiState.copy(isLoading = false, error = "No se pudo cargar el perfil.")
            }
        }
    }

    // Llamado cuando el usuario selecciona una nueva imagen
    fun onImageSelected(uri: Uri?) {
        Log.d("EditProfileVM", "Imagen seleccionada localmente: $uri")
        uiState = uiState.copy(selectedImageUri = uri)
    }

    // Llamado para guardar los cambios
    fun saveChanges(newUsername: String) {
        val userId = auth.currentUser?.uid ?: return // Necesitamos userId
        val trimmedUsername = newUsername.trim()

        if (trimmedUsername.isBlank()) {
            uiState = uiState.copy(error = "El nombre de usuario no puede estar vacío.")
            return
        }

        uiState = uiState.copy(isSaving = true, error = null, saveSuccess = false)

        viewModelScope.launch {
            var uploadedImageUrl: String? = null // Variable para guardar la URL si se sube nueva imagen
            try {
                var newImageUrl: String? = uiState.currentImageUrl // Por defecto, usa la actual
                val imageUri = uiState.selectedImageUri
                val imageUriToUpload = uiState.selectedImageUri
                if (imageUriToUpload != null) {
                    Log.d("EditProfileVM", "Intentando subir nueva imagen desde $imageUriToUpload")
                    // Define la ruta en Storage (ej: profileImages/userId/profile.jpg)
                    val storageRef = storage.reference.child("profileImages/$userId/profile.jpg")
                    // Sube el archivo
                    storageRef.putFile(imageUriToUpload).await() // Espera a que termine la subida
                    // Obtiene la URL de descarga pública
                    uploadedImageUrl = storageRef.downloadUrl.await().toString()
                    Log.d("EditProfileVM", "Imagen subida exitosamente. URL: $uploadedImageUrl")
                } else {
                    Log.d("EditProfileVM", "No se seleccionó nueva imagen para subir.")
                }

                // 2. Comprobar unicidad del nuevo username (SI ha cambiado)
                if (trimmedUsername != originalUsername) {
                    Log.d("EditProfileVM", "Comprobando unicidad de username: $trimmedUsername")
                    val usernameQuery = firestore.collection("users")
                        .whereEqualTo("username", trimmedUsername)
                        .limit(1).get().await()
                    if (!usernameQuery.isEmpty && usernameQuery.documents[0].id != userId) {
                        throw Exception("El nombre de usuario '$trimmedUsername' ya existe.")
                    }
                    Log.d("EditProfileVM", "Username '$trimmedUsername' es único o es el mismo.")
                }

                // 3. Preparar datos para ACTUALIZAR en Firestore
                Log.d("EditProfileVM", "Preparando actualización de Firestore...")
                val updates = mutableMapOf<String, Any?>()
                // Añade username solo si cambió
                if (trimmedUsername != originalUsername) {
                    updates["username"] = trimmedUsername
                }
                // Añade la URL de la imagen SOLO si se subió una NUEVA
                if (uploadedImageUrl != null) {
                    updates["profileImageUrl"] = uploadedImageUrl
                }

                // 4. Actualizar Firestore SOLO si hay algo que cambiar
                if (updates.isNotEmpty()) {
                    Log.d("EditProfileVM", "Actualizando Firestore con: $updates")
                    firestore.collection("users").document(userId)
                        .set(updates, SetOptions.merge()) // Usa merge para no borrar otros campos
                        .await()
                    Log.d("EditProfileVM", "Firestore actualizado.")
                    // Actualiza el estado local con los nuevos datos guardados
                    uiState = uiState.copy(
                        currentUsername = if(updates.containsKey("username")) trimmedUsername else originalUsername,
                        currentImageUrl = uploadedImageUrl ?: uiState.currentImageUrl, // Actualiza URL si se subió
                        selectedImageUri = null // Limpia la URI local después de subir
                    )
                    originalUsername = trimmedUsername // Actualiza el original para futuras ediciones
                } else {
                    Log.d("EditProfileVM", "No hubo cambios para guardar en Firestore.")
                    uiState = uiState.copy(selectedImageUri = null) // Limpia URI local aunque no se guardó nada nuevo
                }

                // 5. Indicar Éxito
                uiState = uiState.copy(isSaving = false, saveSuccess = true)

            } catch (e: FirebaseFirestoreException) { // Error específico de Firestore
                Log.e("EditProfileVM", "Error Firestore al guardar", e)
                uiState = uiState.copy(isSaving = false, error = "Error al guardar en BD: ${e.message}")
            } catch(e: Exception) { // Captura errores de Storage, unicidad, etc.
                Log.e("EditProfileVM", "Error general al guardar", e)
                uiState = uiState.copy(isSaving = false, error = e.message ?: "Error desconocido.")
            }
        }
    }

    fun clearError() {
        if (uiState.error != null) {
            uiState = uiState.copy(error = null)
        }
    }
}