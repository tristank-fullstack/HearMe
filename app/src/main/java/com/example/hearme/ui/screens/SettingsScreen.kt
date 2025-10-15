package com.example.hearme.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.hearme.ui.theme.HearMeTheme
import com.example.hearme.viewmodels.SettingsViewModel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField // Para los diálogos
import androidx.compose.material3.TextButton // Para enlace "Olvidé contraseña"
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.LaunchedEffect // Para Snackbar de éxito
import androidx.compose.material3.SnackbarDuration // Para Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.text.input.KeyboardType
import androidx.media3.common.util.UnstableApi

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class) // Para Scaffold y TopAppBar
@Composable
fun SettingsScreen(
    navController: NavController, // Recibe NavController para volver atrás
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() } // Para mostrar éxito/error

    // Estados locales para los campos de contraseña en los diálogos
    var currentPasswordInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var newEmailInput by remember { mutableStateOf("") } // Para cambio de email

    // --- Diálogo Confirmar Cerrar Sesión ---
    if (uiState.showConfirmLogoutDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelLogout() },
            title = { Text("Cerrar Sesión") },
            text = { Text("¿Estás seguro de que quieres cerrar la sesión actual?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Cerrar Sesión") }
            },
            dismissButton = {
                Button(onClick = { viewModel.cancelLogout() }) { Text("Cancelar") }
            }
        )
    }
    // --- Fin Diálogo Logout ---

    // --- Diálogo para Reautenticar (Contraseña Actual) ---
    if (uiState.showReauthenticateDialog || uiState.showReauthenticateEmailDialog) {
        AlertDialog(
            onDismissRequest = {
                if(uiState.showReauthenticateDialog) viewModel.cancelPasswordChange()
                else viewModel.cancelEmailChange()
            },
            title = { Text("Verificar Contraseña") },
            text = {
                Column {
                    Text(
                        if (uiState.showReauthenticateEmailDialog) {
                            "Para cambiar tu email, primero introduce tu contraseña actual."
                        } else {
                            "Para cambiar tu contraseña, primero introduce tu contraseña actual."
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = currentPasswordInput,
                        onValueChange = { currentPasswordInput = it },
                        label = { Text("Contraseña Actual") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = uiState.error != null && (uiState.showReauthenticateDialog || uiState.showReauthenticateEmailDialog)
                    )
                    TextButton(
                        onClick = {
                            viewModel.cancelPasswordChange() // Cierra diálogo de pass
                            viewModel.cancelEmailChange()    // Cierra diálogo de email
                            Log.w("SettingsScreen", "TODO: Navegar a flujo Olvidé Contraseña global")
                            // Aquí podrías usar navController.navigate("forgot_password") si está definido globalmente
                            // o mostrar un Snackbar indicando usar la opción de la pantalla de Login.
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("¿Olvidaste tu contraseña?")
                    }
                    if (uiState.error != null && (uiState.showReauthenticateDialog || uiState.showReauthenticateEmailDialog)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(uiState.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if(uiState.showReauthenticateDialog) {
                            viewModel.reauthenticateWithCurrentPassword(currentPasswordInput)
                        } else {
                            viewModel.reauthenticateForEmailChange(currentPasswordInput)
                        }
                    },
                    enabled = !uiState.isReauthenticating
                ) {
                    if (uiState.isReauthenticating) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else { Text("Verificar") }
                }
            },
            dismissButton = {
                Button(onClick = {
                    if(uiState.showReauthenticateDialog) viewModel.cancelPasswordChange()
                    else viewModel.cancelEmailChange()
                }) { Text("Cancelar") }
            }
        )
    }
    // --- Fin Diálogo Reautenticar ---

    // --- Diálogo para Nueva Contraseña ---
    if (uiState.showNewPasswordDialog) {
        LaunchedEffect(Unit) { newPasswordInput = ""; confirmPasswordInput = "" } // Limpia campos al abrir
        AlertDialog(
            onDismissRequest = { viewModel.cancelPasswordChange() },
            title = { Text("Nueva Contraseña") },
            text = {
                Column {
                    Text("Introduce tu nueva contraseña.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = newPasswordInput, onValueChange = { newPasswordInput = it }, label = { Text("Nueva Contraseña") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, isError = uiState.error != null)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = confirmPasswordInput, onValueChange = { confirmPasswordInput = it }, label = { Text("Confirmar Nueva Contraseña") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, isError = uiState.error != null)
                    if (uiState.error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(uiState.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.updatePassword(newPasswordInput, confirmPasswordInput) }, enabled = !uiState.isUpdatingPassword) {
                    if (uiState.isUpdatingPassword) { CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp) } else { Text("Actualizar") }
                }
            },
            dismissButton = { Button(onClick = { viewModel.cancelPasswordChange() }) { Text("Cancelar") } }
        )
    }
    // --- Fin Diálogo Nueva Contraseña ---

    // --- Diálogo para Nuevo Email ---
    if (uiState.showNewEmailDialog) {
        LaunchedEffect(Unit) { newEmailInput = "" } // Limpia campo al abrir
        AlertDialog(
            onDismissRequest = { viewModel.cancelEmailChange() },
            title = { Text("Cambiar Email") },
            text = {
                Column {
                    Text("Introduce tu nuevo email. Te enviaremos un enlace de verificación.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = newEmailInput, onValueChange = { newEmailInput = it }, label = { Text("Nuevo Email") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), isError = uiState.error != null)
                    if (uiState.error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(uiState.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.updateEmail(newEmailInput) }, enabled = !uiState.isUpdatingEmail) {
                    if (uiState.isUpdatingEmail) { CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp) } else { Text("Enviar Verificación") }
                }
            },
            dismissButton = { Button(onClick = { viewModel.cancelEmailChange() }) { Text("Cancelar") } }
        )
    }
    // --- Fin Diálogo Nuevo Email ---

    // --- Efecto para Snackbar de Éxito Cambio Contraseña ---
    LaunchedEffect(uiState.passwordChangeSuccess) {
        if (uiState.passwordChangeSuccess) {
            snackbarHostState.showSnackbar(message = "Contraseña actualizada correctamente.", duration = SnackbarDuration.Short)
            viewModel.resetPasswordChangeSuccess()
        }
    }
    // --- Efecto para Snackbar de Éxito Envío Verificación Email ---
    LaunchedEffect(uiState.emailVerificationSent) {
        if (uiState.emailVerificationSent) {
            snackbarHostState.showSnackbar(message = "Se envió un correo de verificación a tu nueva dirección.", duration = SnackbarDuration.Long)
            viewModel.resetEmailChangeFlags()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp) ,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Botón Cambiar Contraseña
            Button(
                onClick = {
                    currentPasswordInput = "" // Limpia campo
                    viewModel.requestPasswordChange() // Inicia flujo
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cambiar Contraseña")
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Botón Cambiar Email
            Button(
                onClick = {
                    currentPasswordInput = "" // Limpia campo para reautenticación
                    viewModel.requestEmailChange() // Inicia flujo
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cambiar Email")
            }
            // El botón "Cambiar de Cuenta" lo hemos eliminado como pediste

            Spacer(modifier = Modifier.height(16.dp))

            // Botón Cerrar Sesión
            OutlinedButton(
                onClick = { viewModel.requestLogout() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Cerrar Sesión")
            }

            // Muestra errores generales del ViewModel si los hubiera (y no se muestran en diálogos)
            if (uiState.error != null && !uiState.showReauthenticateDialog && !uiState.showNewPasswordDialog && !uiState.showNewEmailDialog) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(uiState.error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        } // Fin Column
    } // Fin Scaffold
} // Fin SettingsScreen



// --- Preview (Simple) ---
@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    HearMeTheme {
        // Necesitaríamos un NavController falso para una preview completa
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
            Text("Preview Configuración")
        }
        // SettingsScreen(navController = rememberNavController()) // Llamada real para preview
    }
}