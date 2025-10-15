package com.example.hearme.ui.screens
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Icono para volver atrás
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hearme.ui.theme.HearMeTheme // Usa tu tema
import com.example.hearme.viewmodels.LoginViewModel // Usamos el mismo ViewModel
import com.example.hearme.viewmodels.RegisterUiState // Importa el estado de registro


@OptIn(ExperimentalMaterial3Api::class) // Necesario para TopAppBar experimental
@Composable
fun RegisterScreen(
    // --- ¡¡AÑADE ESTE PARÁMETRO AQUÍ!! ---
    onAuthSuccess: () -> Unit, // Callback cuando Firebase Auth tiene éxito
    // --- FIN AÑADIR ---
    onBackClick: () -> Unit,       // Callback para volver atrás (este ya debería estar)
    loginViewModel: LoginViewModel = viewModel()
) {
    val uiState = loginViewModel.registerUiState

    // --- ¡¡MODIFICA ESTE LaunchedEffect!! ---
    // Antes observaba registerSuccess, ahora authSuccessNeedsProfileCompletion
    LaunchedEffect(uiState.authSuccessNeedsProfileCompletion) { // <-- Observa el nuevo estado
        if (uiState.authSuccessNeedsProfileCompletion) {
            Log.d("RegisterScreen", "Auth Success detected, calling onAuthSuccess")
            onAuthSuccess() // <-- Llama al nuevo callback
            loginViewModel.resetAuthSuccessNeedsProfileCompletion() // Resetea el estado
        }
    }
    // --- FIN MODIFICACIÓN ---
    RegisterScreenContent(
        uiState = uiState,
        onRegisterClick = { email, password, confirmPassword ->
            loginViewModel.registerUser(email, password, confirmPassword)
        },
        onBackClick = onBackClick,
        onErrorDismiss = { loginViewModel.clearRegisterErrorMessage() }
    )
}

// --- Composable Interno para la UI y Previsualización ---
@OptIn(ExperimentalMaterial3Api::class) // Necesario para TopAppBar experimental
@Composable
fun RegisterScreenContent(
    uiState: RegisterUiState,
    onRegisterClick: (String, String, String) -> Unit,
    onBackClick: () -> Unit,
    onErrorDismiss: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // Efecto para mostrar el Snackbar cuando hay error
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            val result = snackbarHostState.showSnackbar(
                message = uiState.errorMessage,
                actionLabel = "Ok",
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed || result == SnackbarResult.Dismissed) {
                onErrorDismiss()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Crear Cuenta") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { // Botón para volver
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
                .padding(paddingValues) // Padding del Scaffold (incluye TopAppBar)
                .padding(16.dp), // Padding adicional
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (uiState.isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Text("Crear Cuenta", style = MaterialTheme.typography.headlineMedium) // Título ya está en TopAppBar
            // Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.errorMessage?.contains("email", ignoreCase = true) == true,
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
                isError = uiState.errorMessage?.contains("contraseña", ignoreCase = true) == true &&
                        !uiState.errorMessage.contains("coinciden", ignoreCase = true), // Marca error si es sobre la contraseña, no sobre la coincidencia
                enabled = !uiState.isLoading,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirmar Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.errorMessage?.contains("coinciden", ignoreCase = true) == true, // Marca error si no coinciden
                enabled = !uiState.isLoading,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onRegisterClick(email, password, confirmPassword) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text("Registrarse")
            }
        }
    }
}

// --- Previews para RegisterScreenContent ---

@Preview(showBackground = true, name = "Register Screen Default")
@Composable
fun RegisterScreenPreviewDefault() {
    HearMeTheme {
        RegisterScreenContent(
            uiState = RegisterUiState(),
            onRegisterClick = { _, _, _ -> },
            onBackClick = {},
            onErrorDismiss = {}
        )
    }
}

@Preview(showBackground = true, name = "Register Screen Loading")
@Composable
fun RegisterScreenPreviewLoading() {
    HearMeTheme {
        RegisterScreenContent(
            uiState = RegisterUiState(isLoading = true),
            onRegisterClick = { _, _, _ -> },
            onBackClick = {},
            onErrorDismiss = {}
        )
    }
}

@Preview(showBackground = true, name = "Register Screen Error")
@Composable
fun RegisterScreenPreviewError() {
    HearMeTheme {
        RegisterScreenContent(
            uiState = RegisterUiState(errorMessage = "Las contraseñas no coinciden."),
            onRegisterClick = { _, _, _ -> },
            onBackClick = {},
            onErrorDismiss = {}
        )
    }
}