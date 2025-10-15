package com.example.hearme.viewmodels

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.copy
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hearme.firebase.uploadAudioToFirebase
import com.example.hearme.firebase.listenToSavedReviewsForUser
import com.example.hearme.firebase.getSavedReviewsForUser
import com.example.hearme.firebase.saveReviewForUser
import com.example.hearme.firebase.addAudioReview
import com.example.hearme.firebase.deleteAudioReviewPhysically
import com.example.hearme.firebase.likeReview
import com.example.hearme.firebase.unlikeReview
import com.example.hearme.firebase.listenToLikedReviewsForUser
import com.example.hearme.firebase.unsaveReviewForUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.IconButton
import com.example.hearme.data.network.RetrofitClient   // Para acceder al servicio
import com.example.hearme.data.network.models.ProcessAudioRequestDto // Tu DTO
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import com.example.hearme.data.network.models.AudioReview // Asegúrate de importar tu modelo
import com.example.hearme.firebase.getSavedReviewsForUser // Importar
import com.example.hearme.firebase.getUserProfile
import com.example.hearme.firebase.listenToSavedReviewsForUser
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.firestore.ListenerRegistration // Para manejar el listener
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


// Data class UserProfile (Tu definición)
data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val name: String = "", // Usando 'name' como en PerfilScreen
    val birthdate: String = "",
    val email: String = "",
    val profileImageUrl: String? = null
) {
    val age: Int?
        get() {
            if (birthdate.length == 10 && birthdate.count { it == '-' } == 2) {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    sdf.isLenient = false
                    val birthDateParsed = sdf.parse(birthdate)
                    if (birthDateParsed != null) {
                        val today = Calendar.getInstance()
                        val birth = Calendar.getInstance().apply { time = birthDateParsed }
                        if (birth.after(today)) return null
                        var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
                        if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
                            age--
                        }
                        return if (age >= 0) age else null
                    }
                } catch (e: Exception) {
                    Log.e("UserProfile", "Error parsing birthdate '$birthdate'", e)
                }
            }
            return null
        }
}

// Estado de la UI para la pantalla de Perfil
data class ProfileUiState(
    val userProfile: UserProfile? = null,
    val isLoadingProfile: Boolean = true,
    val profileError: String? = null,

    val savedReviews: List<AudioReview> = emptyList(),
    val isLoadingSavedReviews: Boolean = false,
    val savedReviewsError: String? = null,
    val isListeningToSavedReviews: Boolean = false,

    val likedReviews: List<AudioReview> = emptyList(),
    val isLoadingLikedReviews: Boolean = false,
    val likedReviewsError: String? = null,
    val isListeningToLikedReviews: Boolean = false
)

class ProfileViewModel : ViewModel() {

    private val _userProfileUiState = MutableStateFlow(ProfileUiState())
    val userProfileUiState: StateFlow<ProfileUiState> = _userProfileUiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()

    private var savedReviewsListenerReg: ListenerRegistration? = null
    private var likedReviewsListenerReg: ListenerRegistration? = null

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        _userProfileUiState.update { it.copy(isLoadingProfile = true, profileError = null) }
        val currentFirebaseUser = auth.currentUser
        if (currentFirebaseUser == null) {
            _userProfileUiState.update { it.copy(isLoadingProfile = false, profileError = "Usuario no autenticado.") }
            return
        }

        viewModelScope.launch {
            try {
                Log.d("ProfileViewModel", "Cargando perfil para UID: ${currentFirebaseUser.uid}")
                // Llamada directa a la función de nivel superior
                val profile = getUserProfile(currentFirebaseUser.uid)
                if (profile != null) {
                    _userProfileUiState.update { it.copy(userProfile = profile, isLoadingProfile = false) }
                    Log.d("ProfileViewModel", "Perfil cargado exitosamente desde Helper: ${profile.username}")
                } else {
                    _userProfileUiState.update { it.copy(isLoadingProfile = false, profileError = "Perfil no encontrado.") }
                    Log.w("ProfileViewModel", "Helper no devolvió perfil para UID: ${currentFirebaseUser.uid}")
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error al cargar perfil con Helper", e)
                _userProfileUiState.update { it.copy(isLoadingProfile = false, profileError = "Error al cargar el perfil: ${e.message}") }
            }
        }
    }

    fun listenForSavedReviews() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _userProfileUiState.update {
                it.copy(
                    isLoadingSavedReviews = false,
                    savedReviews = emptyList(),
                    savedReviewsError = "Usuario no autenticado.",
                    isListeningToSavedReviews = false
                )
            }
            return
        }

        if (_userProfileUiState.value.isListeningToSavedReviews && savedReviewsListenerReg != null) {
            Log.d("ProfileViewModel", "Ya se está escuchando las reseñas GUARDADAS.")
            return
        }

        stopListeningToSavedReviews()

        _userProfileUiState.update { it.copy(isLoadingSavedReviews = true, savedReviewsError = null, isListeningToSavedReviews = false) }
        Log.d("ProfileViewModel", "Iniciando listener para reseñas GUARDADAS por UID: $userId")

        // Llamada directa a la función de nivel superior
        savedReviewsListenerReg = listenToSavedReviewsForUser(
            userId = userId,
            onUpdate = { reviews ->
                Log.d("ProfileViewModel", "Reseñas GUARDADAS actualizadas: ${reviews.size} encontradas.")
                _userProfileUiState.update {
                    it.copy(
                        isLoadingSavedReviews = false,
                        savedReviews = reviews,
                        isListeningToSavedReviews = true
                    )
                }
            },
            onError = { error ->
                Log.e("ProfileViewModel", "Error escuchando reseñas GUARDADAS", error)
                _userProfileUiState.update {
                    it.copy(
                        isLoadingSavedReviews = false,
                        savedReviewsError = "Error al escuchar guardados: ${error.message}",
                        isListeningToSavedReviews = false
                    )
                }
            }
        )
    }

    private fun stopListeningToSavedReviews() {
        savedReviewsListenerReg?.remove()
        savedReviewsListenerReg = null
        if (_userProfileUiState.value.isListeningToSavedReviews) {
            _userProfileUiState.update { it.copy(isListeningToSavedReviews = false, isLoadingSavedReviews = false, savedReviews = emptyList()) }
        }
        Log.d("ProfileViewModel", "Listener de reseñas GUARDADAS detenido.")
    }

    fun listenForLikedReviews() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _userProfileUiState.update {
                it.copy(
                    isLoadingLikedReviews = false,
                    likedReviews = emptyList(),
                    likedReviewsError = "Usuario no autenticado.",
                    isListeningToLikedReviews = false
                )
            }
            return
        }

        if (_userProfileUiState.value.isListeningToLikedReviews && likedReviewsListenerReg != null) {
            Log.d("ProfileViewModel", "Ya se está escuchando las reseñas LIKEADAS.")
            return
        }

        stopListeningToLikedReviews()

        _userProfileUiState.update { it.copy(isLoadingLikedReviews = true, likedReviewsError = null, isListeningToLikedReviews = false) }
        Log.d("ProfileViewModel", "Iniciando listener para reseñas LIKEADAS por UID: $userId")

        // Llamada directa a la función de nivel superior
        likedReviewsListenerReg = listenToLikedReviewsForUser(
            userId = userId,
            onUpdate = { reviews ->
                Log.d("ProfileViewModel", "Reseñas LIKEADAS actualizadas: ${reviews.size} encontradas.")
                _userProfileUiState.update {
                    it.copy(
                        isLoadingLikedReviews = false,
                        likedReviews = reviews,
                        isListeningToLikedReviews = true
                    )
                }
            },
            onError = { error ->
                Log.e("ProfileViewModel", "Error escuchando reseñas LIKEADAS", error)
                _userProfileUiState.update {
                    it.copy(
                        isLoadingLikedReviews = false,
                        likedReviewsError = "Error al escuchar likes: ${error.message}",
                        isListeningToLikedReviews = false
                    )
                }
            }
        )
    }

    private fun stopListeningToLikedReviews() {
        likedReviewsListenerReg?.remove()
        likedReviewsListenerReg = null
        if (_userProfileUiState.value.isListeningToLikedReviews) {
            _userProfileUiState.update { it.copy(isListeningToLikedReviews = false, isLoadingLikedReviews = false, likedReviews = emptyList()) }
        }
        Log.d("ProfileViewModel", "Listener de reseñas LIKEADAS detenido.")
    }

    fun logout() {
        Log.d("ProfileViewModel", "Cerrando sesión del usuario...")
        stopListeningToSavedReviews()
        stopListeningToLikedReviews()
        auth.signOut()
        _userProfileUiState.value = ProfileUiState(isLoadingProfile = false) // Resetea el estado
    }

    suspend fun isCurrentUserProfileComplete(): Boolean? {
        val userId = auth.currentUser?.uid ?: return null
        return try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance() // Firestore directo aquí
            val doc = firestore.collection("users").document(userId).get().await()
            doc.exists() && doc.getBoolean("isProfileComplete") == true
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Error comprobando isProfileComplete para $userId", e)
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListeningToSavedReviews()
        stopListeningToLikedReviews()
        Log.d("ProfileViewModel", "ViewModel onCleared, todos los listeners detenidos.")
    }
}