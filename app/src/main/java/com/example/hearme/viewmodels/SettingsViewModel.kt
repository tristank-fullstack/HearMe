package com.example.hearme.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider // Para reautenticar
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SettingsUiState(
    val showConfirmLogoutDialog: Boolean = false,
    val error: String? = null,
    // Estados para Cambio de Contraseña
    val showReauthenticateDialog: Boolean = false, // Diálogo para pedir pass actual
    val showNewPasswordDialog: Boolean = false,   // Diálogo para pedir nueva pass
    val isReauthenticating: Boolean = false,      // Cargando al reautenticar
    val isUpdatingPassword: Boolean = false,      // Cargando al actualizar pass
    val passwordChangeSuccess: Boolean = false,    // Indicador de éxito

    val showReauthenticateEmailDialog: Boolean = false, // Diálogo para pedir pass ANTES de cambiar email
    val showNewEmailDialog: Boolean = false,          // Diálogo para pedir nuevo email
    val isUpdatingEmail: Boolean = false,             // Cargando al actualizar email
    val emailChangeSuccess: Boolean = false,           // Indicador de éxito cambio email
    val emailVerificationSent: Boolean = false,       // Indicador de que se envió email de verificación al NUEVO email

)

class SettingsViewModel : ViewModel() {

    var uiState by mutableStateOf(SettingsUiState())
        private set

    private val auth = FirebaseAuth.getInstance()

    // --- Logout (sin cambios) ---
    fun requestLogout() { uiState = uiState.copy(showConfirmLogoutDialog = true) }
    fun confirmLogout() { auth.signOut(); uiState = uiState.copy(showConfirmLogoutDialog = false) }
    fun cancelLogout() { uiState = uiState.copy(showConfirmLogoutDialog = false) }
    // --- Fin Logout ---


    // --- INICIO: Lógica Cambio Contraseña ---

    // 1. Iniciar el proceso: Muestra diálogo para pedir contraseña actual
    fun requestPasswordChange() {
        // Resetea estados previos y muestra el primer diálogo
        uiState = uiState.copy(
            showReauthenticateDialog = true,
            showNewPasswordDialog = false,
            error = null,
            passwordChangeSuccess = false
        )
    }

    // 2. Reautenticar con la contraseña actual
    fun reauthenticateWithCurrentPassword(currentPassword: String) {
        val user = auth.currentUser ?: return // No debería pasar si está en Settings
        val email = user.email ?: return // Necesitamos el email

        if (currentPassword.isBlank()) {
            uiState = uiState.copy(error = "Introduce tu contraseña actual.")
            return
        }

        uiState = uiState.copy(isReauthenticating = true, error = null)
        viewModelScope.launch {
            try {
                // Crea la credencial con email y contraseña actual
                val credential = EmailAuthProvider.getCredential(email, currentPassword)
                // Reautentica al usuario
                auth.currentUser?.reauthenticate(credential)?.await()

                Log.d("SettingsViewModel", "Reautenticación exitosa.")
                // Éxito: Oculta diálogo actual, muestra diálogo para nueva contraseña
                uiState = uiState.copy(
                    isReauthenticating = false,
                    showReauthenticateDialog = false,
                    showNewPasswordDialog = true // <-- Abre el siguiente diálogo
                )

            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error de Reautenticación", e)
                val errorMsg = when(e) {
                    is FirebaseAuthInvalidCredentialsException -> "La contraseña actual es incorrecta."
                    else -> "Error al verificar contraseña: ${e.localizedMessage}"
                }
                uiState = uiState.copy(isReauthenticating = false, error = errorMsg)
                // Mantenemos el diálogo de reautenticación abierto para que reintente
            }
        }
    }

    // 3. Actualizar a la nueva contraseña
    fun updatePassword(newPassword: String, confirmPassword: String) {
        if (newPassword.isBlank() || confirmPassword.isBlank()) {
            uiState = uiState.copy(error = "Introduce y confirma la nueva contraseña.")
            return
        }
        if (newPassword.length < 6) {
            uiState = uiState.copy(error = "La nueva contraseña debe tener al menos 6 caracteres.")
            return
        }
        if (newPassword != confirmPassword) {
            uiState = uiState.copy(error = "Las nuevas contraseñas no coinciden.")
            return
        }

        uiState = uiState.copy(isUpdatingPassword = true, error = null)
        viewModelScope.launch {
            try {
                auth.currentUser?.updatePassword(newPassword)?.await()
                Log.d("SettingsViewModel", "Contraseña actualizada exitosamente.")
                uiState = uiState.copy(
                    isUpdatingPassword = false,
                    showNewPasswordDialog = false,
                    passwordChangeSuccess = true
                )

            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error al actualizar contraseña", e)
                val errorMsg = when (e) {
                    is FirebaseAuthWeakPasswordException -> "La nueva contraseña es demasiado débil."
                    // --- ¡NUEVA COMPROBACIÓN! ---
                    // Firebase a menudo lanza FirebaseAuthInvalidCredentialsException
                    // o un FirebaseAuthException genérico si la contraseña es la misma.
                    // Comprobamos el mensaje de error específico si es posible.
                    is FirebaseAuthInvalidCredentialsException -> "Error: ${e.localizedMessage}" // Podría ser este
                    is FirebaseAuthException -> {
                        // Comprueba si el mensaje de error indica que es la misma contraseña
                        if (e.message?.contains(
                                "same as previous",
                                ignoreCase = true
                            ) == true || // Ejemplo de texto posible
                            e.message?.contains("WEAK_PASSWORD", ignoreCase = true) == false
                        ) { // Si no es weak pass, podría ser "same"
                            "La nueva contraseña no puede ser igual a la anterior."
                        } else {
                            // Otro error de Firebase Auth
                            "Error al cambiar contraseña: ${e.localizedMessage}"
                        }
                    }
                    // --- FIN NUEVA COMPROBACIÓN ---
                    else -> "Error al cambiar contraseña: ${e.localizedMessage}"
                }
                uiState = uiState.copy(isUpdatingPassword = false, error = errorMsg)
                // Mantenemos el diálogo abierto para que corrija
            }
        } // Fin launch
    }

    // 4. Cerrar los diálogos de cambio de contraseña
    fun cancelPasswordChange() {
        uiState = uiState.copy(
            showReauthenticateDialog = false,
            showNewPasswordDialog = false,
            error = null, // Limpia errores al cancelar
            isReauthenticating = false, // Resetea loadings
            isUpdatingPassword = false
        )
    }

    // 5. Resetear el flag de éxito (para que el Snackbar/mensaje desaparezca)
    fun resetPasswordChangeSuccess() {
        if (uiState.passwordChangeSuccess) {
            uiState = uiState.copy(passwordChangeSuccess = false)
        }
    }

    // --- FIN: Lógica Cambio Contraseña ---

    // 1. Iniciar proceso: Pide reautenticación (igual que para contraseña)
    fun requestEmailChange() {
        // Mostramos el mismo diálogo de reautenticar, pero al confirmar iremos a otro flujo
        uiState = uiState.copy(
            showReauthenticateEmailDialog = true, // Usamos flag diferente si queremos un diálogo distinto
            showNewEmailDialog = false,
            error = null,
            emailChangeSuccess = false,
            emailVerificationSent = false
        )
    }

    // 2. Reautenticar ANTES de mostrar diálogo de nuevo email
    fun reauthenticateForEmailChange(currentPassword: String) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return

        if (currentPassword.isBlank()) { /* ... (error) ... */ return }

        uiState = uiState.copy(isReauthenticating = true, error = null) // Reutilizamos flag loading
        viewModelScope.launch {
            try {
                val credential = EmailAuthProvider.getCredential(email, currentPassword)
                auth.currentUser?.reauthenticate(credential)?.await()
                Log.d("SettingsViewModel", "Reautenticación para cambio de email exitosa.")
                // Éxito: Oculta diálogo actual, muestra diálogo para nuevo email
                uiState = uiState.copy(
                    isReauthenticating = false,
                    showReauthenticateEmailDialog = false, // Cierra diálogo reauth
                    showNewEmailDialog = true // <-- Abre diálogo nuevo email
                )
            } catch (e: Exception) { // ... (Manejo de error igual que en reauth de contraseña) ...
                Log.e("SettingsViewModel", "Error de Reautenticación para email", e)
                val errorMsg = when(e) { is FirebaseAuthInvalidCredentialsException -> "Error de Reautenticación para email."
                    else -> "Error al verificar el correo: ${e.localizedMessage}"}
                uiState = uiState.copy(isReauthenticating = false, error = errorMsg)
            }
        }
    }

    // 3. Actualizar al nuevo email y enviar verificación
    fun updateEmail(newEmail: String) {
        if (newEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            uiState = uiState.copy(error = "Introduce un email válido.")
            return
        }
        // Comprueba si el nuevo email es igual al actual
        if (newEmail.equals(auth.currentUser?.email, ignoreCase = true)) {
            uiState = uiState.copy(error = "El nuevo email no puede ser igual al actual.")
            return
        }

        uiState = uiState.copy(isUpdatingEmail = true, error = null)
        viewModelScope.launch {
            try {
                // Paso 1: Actualizar email en Firebase Auth
                auth.currentUser?.verifyBeforeUpdateEmail(newEmail)?.await() // Usa verifyBeforeUpdateEmail

                Log.d("SettingsViewModel", "Solicitud de actualización de email a '$newEmail' enviada. Se requiere verificación.")

                // Éxito: Oculta diálogo, indica éxito y que se envió verificación
                // NOTA: El email NO cambia en Auth hasta que se verifica el nuevo.
                // La sesión actual sigue siendo válida con el email antiguo.
                uiState = uiState.copy(
                    isUpdatingEmail = false,
                    showNewEmailDialog = false,
                    emailChangeSuccess = true, // Indica éxito general del proceso
                    emailVerificationSent = true // Indica que se envió email al nuevo correo
                )

            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error al actualizar email", e)
                val errorMsg = when(e) {
                    is FirebaseAuthUserCollisionException -> "El nuevo email ya está en uso por otra cuenta."
                    // Otros errores de Firebase Auth
                    else -> "Error al cambiar email: ${e.localizedMessage}"
                }
                uiState = uiState.copy(isUpdatingEmail = false, error = errorMsg)
                // Mantenemos diálogo de nuevo email abierto
            }
        }
    }

    // 4. Cerrar diálogos de cambio de email
    fun cancelEmailChange() {
        uiState = uiState.copy(
            showReauthenticateEmailDialog = false,
            showNewEmailDialog = false,
            error = null,
            isReauthenticating = false,
            isUpdatingEmail = false
        )
    }

    // 5. Resetear flags de éxito de cambio de email
    fun resetEmailChangeFlags() {
        if (uiState.emailChangeSuccess || uiState.emailVerificationSent) {
            uiState = uiState.copy(emailChangeSuccess = false, emailVerificationSent = false)
        }
    }

    // --- FIN: Lógica Cambio Email ---

    // --- Limpiar Error General ---
    fun clearError() {
        if (uiState.error != null) {
            uiState = uiState.copy(error = null)
        }
    }
}