package com.example.hearme.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore // Importa Firestore
import com.google.firebase.firestore.SetOptions // Para merge
import com.google.firebase.Timestamp
import androidx.lifecycle.viewModelScope // Importante para coroutines
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException // Importa esta
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.delay // Para simular carga
import kotlinx.coroutines.launch // Para lanzar coroutines

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginSuccess: Boolean = false, // Va a 'main'
    val navigationToCompleteProfile: Boolean = false // ¡NUEVO! Indica que debe ir a completar perfil
)

data class RegisterUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val authSuccessNeedsProfileCompletion: Boolean = false
)

data class ForgotPasswordUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)


//-------------------------------------------------------------------
// VIEWMODEL PRINCIPAL DE AUTENTICACIÓN
//-------------------------------------------------------------------

class LoginViewModel : ViewModel() {

    // --- Propiedades de Estado ---
    var loginUiState by mutableStateOf(LoginUiState())
        private set
    var registerUiState by mutableStateOf(RegisterUiState())
        private set
    var forgotPasswordUiState by mutableStateOf(ForgotPasswordUiState())
        private set

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance() // Añade instancia Firestore

    //--------------------------------------------------
    // LÓGICA DE LOGIN
    //--------------------------------------------------
    fun loginUser(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            loginUiState = loginUiState.copy(errorMessage = "Email y contraseña no pueden estar vacíos")
            return
        }
        viewModelScope.launch {
            loginUiState = loginUiState.copy(isLoading = true, errorMessage = null, loginSuccess = false, navigationToCompleteProfile = false)
            try {
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val userId = authResult.user?.uid
                if (userId == null) {
                    throw Exception("Error al obtener ID de usuario tras login.")
                }

                Log.d("LoginViewModel", "Firebase Login OK para UID: $userId. Comprobando perfil...")

                // --- ¡NUEVA COMPROBACIÓN EN FIRESTORE! ---
                val profileDoc = firestore.collection("users").document(userId).get().await()
                val isComplete = profileDoc.exists() && profileDoc.getBoolean("isProfileComplete") == true

                if (isComplete) {
                    Log.d("LoginViewModel", "Perfil completo. Indicando navegación a main.")
                    loginUiState = loginUiState.copy(isLoading = false, loginSuccess = true)

                    // --- ¡AQUÍ PUEDES OBTENER EL TOKEN! ---
                    val currentUser = firebaseAuth.currentUser // O authResult.user
                    currentUser?.getIdToken(true)
                        ?.addOnSuccessListener { result ->
                            val idToken = result.token
                            Log.d("FIREBASE_ID_TOKEN", "ID Token: $idToken")
                        }?.addOnFailureListener { exception ->
                            Log.e("FIREBASE_ID_TOKEN", "Error al obtener ID Token", exception)
                        }
                    // --- FIN OBTENER TOKEN ---

                }else {
                    Log.w("LoginViewModel", "Perfil INCOMPLETO o no existe. Navegando a completar perfil.")
                    loginUiState = loginUiState.copy(isLoading = false, navigationToCompleteProfile = true) // Navega a complete_profile
                }
            } catch (e: Exception) {
                val errorMsg = when (e) {
                    is FirebaseAuthInvalidUserException -> "No existe cuenta con este email."
                    is FirebaseAuthInvalidCredentialsException -> "Contraseña incorrecta."
                    else -> "Error de autenticación: ${e.localizedMessage ?: "Error desconocido"}"
                }
                Log.e("LoginViewModel", "Firebase Login Error", e)
                loginUiState = loginUiState.copy(isLoading = false, errorMessage = errorMsg)
            }
        }
    }

    fun clearLoginErrorMessage() {
        // Limpia el mensaje de error de login si existe
        if (loginUiState.errorMessage != null) {
            loginUiState = loginUiState.copy(errorMessage = null)
            Log.d("LoginViewModel", "Login error message cleared.") // Log opcional
        }
    }
    fun resetLoginNavigation() { // Renombrado para más claridad
        if (loginUiState.loginSuccess || loginUiState.navigationToCompleteProfile) {
            loginUiState = loginUiState.copy(loginSuccess = false, navigationToCompleteProfile = false)
        }
    }
    // --- Fin Lógica Login ---

    //--------------------------------------------------
    // LÓGICA DE REGISTRO
    //--------------------------------------------------
    fun registerUser(email: String, password: String, confirmPassword: String) {
        // Validaciones iniciales
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            registerUiState = registerUiState.copy(errorMessage = "Introduce un email válido.")
            return
        }
        if (password.isBlank() || confirmPassword.isBlank()) {
            registerUiState = registerUiState.copy(errorMessage = "Las contraseñas no pueden estar vacías.")
            return
        }
        if (password.length < 6) { // Validación mínima de Firebase
            registerUiState = registerUiState.copy(errorMessage = "La contraseña debe tener al menos 6 caracteres.")
            return
        }
        if (password != confirmPassword) {
            registerUiState = registerUiState.copy(errorMessage = "Las contraseñas no coinciden.")
            return
        }

        viewModelScope.launch {
            registerUiState = registerUiState.copy(isLoading = true, errorMessage = null, authSuccessNeedsProfileCompletion = false)
            var userId: String? = null // Para guardar el UID si Auth tiene éxito
            try {
                Log.d("LoginViewModel", "Attempting Firebase User Creation for: $email")
                val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                userId = authResult.user?.uid
                if (userId == null) {
                    throw Exception("Error al obtener UID tras crear usuario.")
                }
                Log.d("LoginViewModel", "Firebase User Creation Success! UID: $userId. Creando doc en Firestore...")

                // --- ¡NUEVO! Crear documento INCOMPLETO en Firestore ---
                val initialProfileData = hashMapOf(
                    "uid" to userId,
                    "email" to email, // Guardamos email
                    "createdAt" to Timestamp.now(),
                    "isProfileComplete" to false // <-- Marcador clave
                    // NO guardamos username, fullName, birthdate aún
                )
                firestore.collection("users").document(userId)
                    .set(initialProfileData) // Crea el documento inicial
                    .await()
                Log.d("LoginViewModel", "Documento inicial en Firestore creado para $userId.")
                // --- Fin Crear Documento ---

                // Indica éxito para navegar a completar perfil
                registerUiState = registerUiState.copy(
                    isLoading = false,
                    authSuccessNeedsProfileCompletion = true
                )
                // --------------------------------------------------------------------

            } catch (e: Exception) {
                Log.e("LoginViewModel", "Firebase User Creation Error", e)
                if (userId != null && e !is FirebaseAuthUserCollisionException) {
                    // Podríamos añadir lógica para borrar auth user si firestore falló
                    Log.w("LoginViewModel", "Firestore failed after user creation. User $userId might be orphaned in Auth.")
                }
                val errorMsg = when (e) {
                    is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con este email."
                    is FirebaseAuthWeakPasswordException -> "La contraseña es demasiado débil (mínimo 6 caracteres)."
                    is FirebaseAuthException -> "Error de registro: ${e.localizedMessage}" // Error específico de Firebase Auth
                    else -> "Error inesperado: ${e.localizedMessage ?: "Error desconocido"}" // Otro error
                }
                // Actualiza estado de error en el registro
                registerUiState = registerUiState.copy(isLoading = false, errorMessage = errorMsg)
            }
        }
    }

    fun clearRegisterErrorMessage() {
        // Limpia el mensaje de error de registro si existe
        if (registerUiState.errorMessage != null) {
            registerUiState = registerUiState.copy(errorMessage = null)
        }
    }

    fun resetAuthSuccessNeedsProfileCompletion() {
        // Resetea el estado después de haber navegado a la pantalla de completar perfil
        if (registerUiState.authSuccessNeedsProfileCompletion) {
            registerUiState = registerUiState.copy(authSuccessNeedsProfileCompletion = false)
            Log.d("LoginViewModel", "Reset authSuccessNeedsProfileCompletion flag")
        }
    }
    // --- Fin Lógica Registro ---


    //--------------------------------------------------
    // LÓGICA OLVIDÉ CONTRASEÑA
    //--------------------------------------------------
    fun sendPasswordReset(email: String) {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            forgotPasswordUiState = forgotPasswordUiState.copy(errorMessage = "Introduce un email válido.", successMessage = null)
            return
        }
        viewModelScope.launch {
            forgotPasswordUiState = forgotPasswordUiState.copy(isLoading = true, errorMessage = null, successMessage = null)
            try {
                Log.d("LoginViewModel", "Attempting to send password reset email to: $email")
                firebaseAuth.sendPasswordResetEmail(email).await()
                Log.d("LoginViewModel", "Password reset email sent successfully to: $email")
                // Éxito: Muestra mensaje de éxito
                forgotPasswordUiState = forgotPasswordUiState.copy(
                    isLoading = false,
                    successMessage = "Se ha enviado un enlace para restablecer tu contraseña a $email."
                )
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error sending password reset email", e)
                val errorMsg = when (e) {
                    is FirebaseAuthInvalidUserException -> "No se encontró ninguna cuenta asociada a este email."
                    is FirebaseAuthException -> "Error al enviar el email: ${e.localizedMessage}" // Error específico de Firebase Auth
                    else -> "Error inesperado: ${e.localizedMessage ?: "Error desconocido"}" // Otro error
                }
                // Actualiza estado de error en olvidó contraseña
                forgotPasswordUiState = forgotPasswordUiState.copy(isLoading = false, errorMessage = errorMsg, successMessage = null)
            }
        }
    }

    fun clearForgotPasswordMessages() {
        // Limpia mensajes (error o éxito) de la pantalla Olvidé Contraseña
        if (forgotPasswordUiState.errorMessage != null || forgotPasswordUiState.successMessage != null) {
            forgotPasswordUiState = forgotPasswordUiState.copy(errorMessage = null, successMessage = null)
        }
    }
    // --- Fin Lógica Olvidé Contraseña ---

} // --- FIN DE LA CLASE LoginViewModel ---