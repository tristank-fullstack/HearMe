package com.example.hearme.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.launch
import com.google.firebase.Timestamp
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Date

data class CompleteProfileUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val profileCompleteSuccess: Boolean = false
)

class CompleteProfileViewModel : ViewModel() {

    var uiState by mutableStateOf(CompleteProfileUiState())
        private set

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Función para guardar detalles del perfil
    fun saveProfileDetails(username: String, fullName: String, birthdate: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            uiState = uiState.copy(error = "Usuario no autenticado.")
            return
        }

        if (username.isBlank() || fullName.isBlank() || birthdate.isBlank()) {
            uiState = uiState.copy(error = "Todos los campos son obligatorios.")
            return
        }

        uiState = uiState.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // Comprobación de Unicidad (igual que antes)
                val usernameQuery = firestore.collection("users")
                    .whereEqualTo("username", username.trim())
                    .limit(1).get().await()
                // ¡OJO! Comprobar si el encontrado NO es el usuario actual
                if (!usernameQuery.isEmpty && usernameQuery.documents[0].id != currentUser.uid) {
                    throw Exception("El nombre de usuario '${username.trim()}' ya está en uso.")
                }

                val userProfileUpdates = hashMapOf<String, Any>( // Usamos Any para el booleano
                    "username" to username.trim(),
                    "fullName" to fullName.trim(),
                    "birthdate" to birthdate,
                    "isProfileComplete" to true // <-- ¡Marcamos como completo!
                    // No necesitamos actualizar uid, email, createdAt
                    // profileImageUrl se actualizará en EditProfile
                )


                firestore.collection("users").document(currentUser.uid)
                    .set(userProfileUpdates, SetOptions.merge()) // Usa Set con Merge
                    .await()

                Log.d("CompleteProfileVM", "Perfil guardado exitosamente para UID: ${currentUser.uid}")
                uiState = uiState.copy(isLoading = false, profileCompleteSuccess = true)

            } catch (e: FirebaseFirestoreException) {
                Log.e("CompleteProfileVM", "Error Firestore al guardar perfil", e)
                uiState = uiState.copy(isLoading = false, error = "Error al guardar datos: ${e.message}")
            } catch (e: Exception) {
                Log.e("CompleteProfileVM", "Error al guardar perfil", e)
                uiState = uiState.copy(isLoading = false, error = e.message ?: "Error desconocido")
            }
        }
    }

    // Función para cancelar y cerrar sesión
    fun cancelRegistrationAndLogout() {
        Log.w("CompleteProfileVM", "Cancelando registro y cerrando sesión...")
        val user = auth.currentUser
        viewModelScope.launch {
            try {
                auth.signOut()
                Log.d("CompleteProfileVM", "Sesión cerrada para usuario ${user?.uid}")
            } catch (e: Exception) {
                Log.e("CompleteProfileVM", "Error al intentar cerrar sesión durante cancelación", e)
            }
        }
    }

    // Función para limpiar el error
    fun clearError() {
        if (uiState.error != null) {
            uiState = uiState.copy(error = null)
        }
    }
}