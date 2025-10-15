package com.example.hearme.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hearme.ui.theme.HearMeTheme
import com.example.hearme.viewmodels.LoginUiState
import com.example.hearme.viewmodels.LoginViewModel


@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit, // Navega a 'main'
    onNavigateToCompleteProfile: () -> Unit, // ¡NUEVO! Navega a 'complete_profile'
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    loginViewModel: LoginViewModel = viewModel()
) {
    val loginState = loginViewModel.loginUiState // Usa loginUiState

    // Efecto para navegar según el resultado del login
    LaunchedEffect(key1 = loginState.loginSuccess, key2 = loginState.navigationToCompleteProfile) {
        when {
            loginState.loginSuccess -> { // Ir a main
                Log.d("LoginScreen", "Login Success -> Navigating to main")
                onLoginSuccess()
                loginViewModel.resetLoginNavigation()
            }
            loginState.navigationToCompleteProfile -> { // Ir a completar perfil
                Log.d("LoginScreen", "Login OK, but needs profile -> Navigating to complete profile")
                onNavigateToCompleteProfile() // <-- Llama al nuevo callback
                loginViewModel.resetLoginNavigation()
            }
        }
    }

    LoginScreenContent(
        uiState = loginState, // Pasa el estado correcto
        onLoginClick = { email, password -> loginViewModel.loginUser(email, password) },
        onRegisterClick = onRegisterClick,
        onForgotPasswordClick = onForgotPasswordClick,
        onErrorDismiss = { loginViewModel.clearLoginErrorMessage() }
    )
}

// --- Composable Interno para la UI y Previsualización ---
// Este SÍ se puede previsualizar porque recibe el estado directamente
@Composable
fun LoginScreenContent(
    uiState: LoginUiState, // Recibe el estado directamente
    onLoginClick: (String, String) -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    // --- ¡¡AÑADE ESTE PARÁMETRO AQUÍ!! ---
    onErrorDismiss: () -> Unit // Callback para cuando se cierra el Snackbar de error
    // --- FIN AÑADIR ---
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // Efecto para mostrar el Snackbar cuando hay un mensaje de error
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            val result = snackbarHostState.showSnackbar(
                message = uiState.errorMessage,
                actionLabel = "Ok", // Etiqueta para cerrar
                duration = SnackbarDuration.Indefinite // O Short/Long
            )
            // Si el Snackbar se cierra (por acción o timeout), llamamos al callback
            if (result == SnackbarResult.ActionPerformed || result == SnackbarResult.Dismissed) {
                onErrorDismiss() // <-- ¡Llamamos al nuevo callback!
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (uiState.isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text("Iniciar Sesión", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.errorMessage != null && uiState.errorMessage.contains("email", ignoreCase = true), // Marca error si el mensaje lo menciona
                enabled = !uiState.isLoading,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.errorMessage != null && uiState.errorMessage.contains("contraseña", ignoreCase = true), // Marca error si el mensaje lo menciona
                enabled = !uiState.isLoading,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onForgotPasswordClick,
                modifier = Modifier.align(Alignment.End),
                enabled = !uiState.isLoading
            ) {
                Text("He olvidado mi contraseña")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onLoginClick(email, password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text("Login")
            }
            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = onRegisterClick,
                enabled = !uiState.isLoading
            ) {
                Text("¿No tienes cuenta? Regístrate")
            }
        }
    }
}


// --- ¡PREVISUALIZACIÓN CORREGIDA! ---
@Preview(showBackground = true, name = "Login Screen Default")
@Composable
fun LoginScreenPreviewDefault() {
    HearMeTheme {
        LoginScreenContent(
            uiState = LoginUiState(), // Usa LoginUiState
            onLoginClick = { _, _ -> },
            onRegisterClick = { },
            onForgotPasswordClick = { },
            onErrorDismiss = { }
        )
    }
}

        @Preview(showBackground = true, name = "Login Screen Loading")
        @Composable
        fun LoginScreenPreviewLoading() {
            HearMeTheme {
                LoginScreenContent(
                    uiState = LoginUiState(isLoading = true), // Usa LoginUiState y activa isLoading
                    onLoginClick = { _, _ -> },
                    onRegisterClick = { },
                    onForgotPasswordClick = { },
                    onErrorDismiss = { }
                )
            }
        }

        @Preview(showBackground = true, name = "Login Screen Error")
        @Composable
        fun LoginScreenPreviewError() {
            HearMeTheme {
                LoginScreenContent(
                    // Usa LoginUiState y asigna un mensaje de error
                    uiState = LoginUiState(errorMessage = "Contraseña incorrecta."),
                    onLoginClick = { _, _ -> },
                    onRegisterClick = { },
                    onForgotPasswordClick = { },
                    onErrorDismiss = { }
                )
            }
        }

        // Podrías añadir una preview para el estado de "Navegar a Completar Perfil" si quisieras
        @Preview(showBackground = true, name = "Login Screen Needs Profile")
        @Composable
        fun LoginScreenPreviewNeedsProfile() {
            HearMeTheme {
                LoginScreenContent(
                    // Usa LoginUiState y activa el flag correspondiente
                    uiState = LoginUiState(navigationToCompleteProfile = true),
                    onLoginClick = { _, _ -> },
                    onRegisterClick = { },
                    onForgotPasswordClick = { },
                    onErrorDismiss = { }
                )
            }
        }