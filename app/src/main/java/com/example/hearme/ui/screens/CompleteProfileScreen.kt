package com.example.hearme.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hearme.ui.theme.HearMeTheme
import com.example.hearme.viewmodels.CompleteProfileViewModel

@Composable
fun CompleteProfileScreen(
    onProfileCompleteSuccess: () -> Unit,
    viewModel: CompleteProfileViewModel = viewModel()
) {
    val uiState = viewModel.uiState

    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var birthdate by remember { mutableStateOf("") } // Input simple por ahora

    val snackbarHostState = remember { SnackbarHostState() }

    // --- Manejar Botón Atrás del Dispositivo ---
    BackHandler(enabled = true) {
        Log.d("CompleteProfileScreen", "Back button pressed, cancelling registration...")
        viewModel.cancelRegistrationAndLogout()
        // El AuthStateListener/LaunchedEffect en MainActivity se encargará de navegar a Login
    }
    // --- Fin Manejar Botón Atrás ---

    // Efecto para navegar si el perfil se completa con éxito
    LaunchedEffect(key1 = uiState.profileCompleteSuccess) {
        if (uiState.profileCompleteSuccess) {
            onProfileCompleteSuccess() // Navega a la pantalla principal ('main')
        }
    }
    // Efecto para mostrar errores en Snackbar
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            snackbarHostState.showSnackbar(
                message = uiState.error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
        // Puedes añadir una TopAppBar aquí si quieres un botón de cancelar visual
        /*
        topBar = {
             TopAppBar(
                 title = { Text("Completar Perfil") },
                 navigationIcon = {
                      IconButton(onClick = { viewModel.cancelRegistrationAndLogout() }) {
                          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancelar Registro")
                      }
                  }
             )
        }
        */
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Completa tu Perfil", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Nombre de Usuario (único)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.error?.contains("usuario", ignoreCase = true) == true
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Nombre y Apellidos") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = birthdate,
                onValueChange = { birthdate = it },
                label = { Text("Fecha Nacimiento (YYYY-MM-DD)") },
                placeholder = { Text("Ej: 1995-10-25") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { viewModel.saveProfileDetails(username, fullName, birthdate) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                ) {
                    Text("Completar Registro")
                }
            }
        } // Fin Column
    } // Fin Scaffold
} // Fin CompleteProfileScreen


// --- Preview ---
@Preview(showBackground = true)
@Composable
fun CompleteProfileScreenPreview() {
    HearMeTheme {
        // La preview no interactúa con el ViewModel real
        // Muestra un placeholder o una versión sin estado si fuera necesario
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
            Text("Preview Completar Perfil")
        }
        // CompleteProfileScreen(onProfileCompleteSuccess = {}) // Llamada real fallaría sin VM
    }
}