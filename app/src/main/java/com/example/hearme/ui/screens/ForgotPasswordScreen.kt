package com.example.hearme.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hearme.ui.theme.HearMeTheme // Usa tu tema
import com.example.hearme.viewmodels.ForgotPasswordUiState // Importa el estado específico
import com.example.hearme.viewmodels.LoginViewModel // Usamos el mismo ViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onEmailSent: () -> Unit,      // Callback para cuando el email se envía (puede volver a login)
    onBackClick: () -> Unit,      // Callback para volver atrás
    loginViewModel: LoginViewModel = viewModel()
) {
    val uiState = loginViewModel.forgotPasswordUiState

    // Efecto para manejar el éxito (podría navegar atrás o mostrar mensaje)
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            Log.d("ForgotPasswordScreen", "Email sent success detected")
            // Opcional: navegar automáticamente atrás después de un delay
            // kotlinx.coroutines.delay(3000) // Espera 3 segundos
            // onEmailSent() // Llama al callback para volver (a Login por ejemplo)
        }
    }

    ForgotPasswordScreenContent(
        uiState = uiState,
        onSendLinkClick = { email ->
            loginViewModel.sendPasswordReset(email)
        },
        onBackClick = onBackClick,
        onMessageDismiss = { loginViewModel.clearForgotPasswordMessages() } // Limpia error o éxito
    )
}

// --- Composable Interno para la UI y Previsualización ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreenContent(
    uiState: ForgotPasswordUiState,
    onSendLinkClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onMessageDismiss: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // Mensaje a mostrar en el Snackbar (error o éxito)
    val messageToShow = uiState.errorMessage ?: uiState.successMessage

    // Efecto para mostrar el Snackbar
    LaunchedEffect(messageToShow) {
        if (messageToShow != null) {
            val result = snackbarHostState.showSnackbar(
                message = messageToShow,
                actionLabel = "Ok",
                duration = SnackbarDuration.Long // Dale tiempo para leer el mensaje de éxito
            )
            // Cuando se cierra el Snackbar, limpiamos el mensaje en el ViewModel
            if (result == SnackbarResult.ActionPerformed || result == SnackbarResult.Dismissed) {
                onMessageDismiss()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Recuperar Contraseña") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // Centra verticalmente
        ) {
            Text(
                "Introduce tu email y te enviaremos un enlace para restablecer tu contraseña.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.errorMessage != null, // Marca error si hay mensaje de error
                enabled = !uiState.isLoading,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Muestra indicador de carga si está en proceso
            if (uiState.isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = { onSendLinkClick(email) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && uiState.successMessage == null // Deshabilita si está cargando o si ya se envió
            ) {
                Text("Enviar Enlace de Recuperación")
            }

            // Opcional: Mostrar el mensaje de éxito directamente en la pantalla también
            /*
            if (uiState.successMessage != null && !uiState.isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.successMessage,
                    color = MaterialTheme.colorScheme.primary, // O un color verde
                    style = MaterialTheme.typography.bodySmall
                )
            }
            */
        }
    }
}

// --- Previews para ForgotPasswordScreenContent ---

@Preview(showBackground = true, name = "Forgot Password Default")
@Composable
fun ForgotPasswordScreenPreviewDefault() {
    HearMeTheme {
        ForgotPasswordScreenContent(
            uiState = ForgotPasswordUiState(),
            onSendLinkClick = {},
            onBackClick = {},
            onMessageDismiss = {}
        )
    }
}

@Preview(showBackground = true, name = "Forgot Password Loading")
@Composable
fun ForgotPasswordScreenPreviewLoading() {
    HearMeTheme {
        ForgotPasswordScreenContent(
            uiState = ForgotPasswordUiState(isLoading = true),
            onSendLinkClick = {},
            onBackClick = {},
            onMessageDismiss = {}
        )
    }
}

@Preview(showBackground = true, name = "Forgot Password Error")
@Composable
fun ForgotPasswordScreenPreviewError() {
    HearMeTheme {
        ForgotPasswordScreenContent(
            uiState = ForgotPasswordUiState(errorMessage = "No se encontró cuenta con este email."),
            onSendLinkClick = {},
            onBackClick = {},
            onMessageDismiss = {}
        )
    }
}

@Preview(showBackground = true, name = "Forgot Password Success")
@Composable
fun ForgotPasswordScreenPreviewSuccess() {
    HearMeTheme {
        ForgotPasswordScreenContent(
            uiState = ForgotPasswordUiState(successMessage = "Email enviado correctamente."),
            onSendLinkClick = {},
            onBackClick = {},
            onMessageDismiss = {}
        )
    }
}