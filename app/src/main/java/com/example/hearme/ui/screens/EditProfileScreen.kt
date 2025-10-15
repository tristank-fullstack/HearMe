package com.example.hearme.ui.screens

import android.net.Uri // Importa Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult // Importa Launcher
import androidx.activity.result.PickVisualMediaRequest // Importa Contrato Visual Media
import androidx.activity.result.contract.ActivityResultContracts // Importa Contrato base
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.provider.Settings

import android.Manifest // Importa Manifest
import android.content.Intent
import android.os.Build // Importa Build para comprobar versión de Android
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import com.google.accompanist.permissions.*

import coil.compose.rememberAsyncImagePainter
import com.example.hearme.ui.theme.HearMeTheme
import com.example.hearme.viewmodels.EditProfileViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditProfileViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    var username by remember(uiState.currentUsername) { mutableStateOf(uiState.currentUsername) }

    // --- Gestión de Permisos ---
    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionState = rememberPermissionState(permission = storagePermission)
    // ---------------------------

    // --- Launcher Imagen ---
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? -> viewModel.onImageSelected(uri) }
    )
    // --------------------------------------

    // Efecto para mostrar errores
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            snackbarHostState.showSnackbar(
                message = uiState.error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    // Efecto para volver atrás si el guardado fue exitoso
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            // Podrías mostrar un Toast o Snackbar de éxito aquí si quieres
            onNavigateBack() // Vuelve a la pantalla anterior (Perfil)
        }
    }



    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
                TopAppBar(
                    title = { Text("Editar Perfil") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    },
                actions = {
                    // Botón Guardar (solo activo si no está guardando)
                    Button(
                        onClick = { viewModel.saveChanges(username) }, // Pasa el username local
                        enabled = !uiState.isSaving && !uiState.isLoading // Deshabilita si carga o guarda
                    ) { Text("Guardar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                // --- Selector de Imagen ---
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable {
                            if (permissionState.status.isGranted) {
                                // Si ya tiene permiso, lanza el picker
                                Log.d("EditProfileScreen", "Permission granted. Launching picker.")
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            } else {
                                // Si NO tiene permiso (sea cual sea la razón), PIDE PERMISO
                                Log.d("EditProfileScreen", "Permission not granted. Requesting permission.")
                                permissionState.launchPermissionRequest()
                                // Después de que el usuario responda al diálogo de permiso,
                                // si lo concede, tendrá que volver a tocar la imagen
                                // para lanzar el picker (porque este clickable ya terminó).
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        // --- LÓGICA DE PINTOR ACTUALIZADA ---
                        // Prioridad: Imagen local > Imagen actual URL > Icono por defecto
                        painter = when {
                            uiState.selectedImageUri != null -> rememberAsyncImagePainter(model = uiState.selectedImageUri)
                            !uiState.currentImageUrl.isNullOrBlank() -> rememberAsyncImagePainter(model = uiState.currentImageUrl)
                            else -> rememberVectorPainter(image = Icons.Default.Person)
                        },
                        contentDescription = "Seleccionar foto de perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop // Recorta para ajustar al círculo
                    )
                    // Podrías añadir un icono de cámara superpuesto si quieres
                }
                Text("Toca para cambiar foto", style = MaterialTheme.typography.bodySmall)


                Spacer(modifier = Modifier.height(32.dp))

                // --- Campo Username ---
                OutlinedTextField(
                    value = username, // Usa el estado local
                    onValueChange = { username = it }, // Actualiza estado local
                    label = { Text("Nombre de Usuario (único)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = uiState.error?.contains("usuario", ignoreCase = true) == true,
                    enabled = !uiState.isSaving // Deshabilita mientras guarda
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Indicador de progreso si está guardando
                if (uiState.isSaving) {
                    CircularProgressIndicator()
                }
            }
        } // Fin Column principal
    } // Fin Scaffold
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionStatusInfo(status: PermissionStatus, isPermanentlyDenied: Boolean) {
    val text = when {
        status.isGranted -> "Permiso de Galería: Concedido"
        isPermanentlyDenied -> "Permiso de Galería: Denegado permanentemente." // Usa el flag calculado
        status.shouldShowRationale -> "Permiso de Galería: Necesario para elegir foto."
        else -> "" // No muestra nada si aún no se ha interactuado (o primera vez)
    }
    if (text.isNotEmpty()) {
        Text(text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
fun EditProfileScreenPreview() {
    HearMeTheme {
        // La preview necesitaría un ViewModel de prueba o datos falsos
        // Por simplicidad, mostramos un placeholder
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Preview Editar Perfil")
        }
        // O podrías intentar llamar a EditProfileScreen con un VM dummy si lo creas
        // EditProfileScreen(onNavigateBack = {})
    }
}