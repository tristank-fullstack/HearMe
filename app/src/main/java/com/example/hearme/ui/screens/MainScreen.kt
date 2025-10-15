package com.example.hearme.ui.screens



import android.Manifest
import android.util.Log
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Para la flecha
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.lifecycle.viewmodel.compose.viewModel // Para obtener el ViewModel
import com.example.hearme.viewmodels.HomeViewModel // Importa tu ViewModel
import com.example.hearme.viewmodels.HomeUiState // Importa el estado
import androidx.compose.foundation.layout.Column // Para apilar verticalmente
import androidx.compose.foundation.layout.Spacer // Para espaciado
import androidx.compose.foundation.layout.height // Para altura del Spacer
import androidx.compose.foundation.layout.fillMaxWidth // Para ocupar ancho
import androidx.compose.material.icons.Icons // Para el icono de búsqueda
import androidx.compose.material.icons.filled.Search // Icono de búsqueda
import androidx.compose.material3.TextFieldDefaults // Para colores del TextField
import androidx.compose.material3.OutlinedTextField // O TextField, o SearchBar más adelante
import androidx.compose.foundation.clickable // Para hacer click sobre el buscador "falso" inicial
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import kotlinx.coroutines.delay
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color // Importa Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hearme.ui.navigation.BottomNavItem

import androidx.compose.runtime.snapshotFlow

import androidx.navigation.NavController
import com.google.maps.android.compose.Marker as GoogleMapMarker
import com.google.maps.android.compose.MarkerState as GoogleMapMarkerState // Si también usas este directamente

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState

import androidx.compose.material.icons.filled.MyLocation // Icono para botón ubicarme
import androidx.compose.runtime.DisposableEffect // Para inicializar cliente
import androidx.compose.ui.platform.LocalContext // Para obtener contexto
import com.google.accompanist.permissions.* // Para el permiso

import androidx.compose.ui.draw.clip
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.maps.CameraUpdateFactory // Import para mover cámara

import com.example.hearme.ui.theme.HearMeTheme
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.auth.FirebaseAuth // Importa si necesitas info del usuario
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.error
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.hearme.data.components.PlacesCardsList
import com.example.hearme.data.components.ShowImageFromUrl
import com.google.accompanist.permissions.*
import com.example.hearme.viewmodels.MapMarkerInfo
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest


// --- Pantalla Principal que contiene el Scaffold y la Navegación ---
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    auth: FirebaseAuth,
    navController: NavController // <-- ¡Recibe NavController!
) {
    // Estado para recordar qué item está seleccionado en la barra inferior.
    var selectedItemRoute by rememberSaveable { mutableStateOf(BottomNavItem.Inicio.route) }

    Scaffold(
        bottomBar = {
            // Llama al Composable de la barra de navegación inferior
            AppBottomNavigationBar(
                selectedRoute = selectedItemRoute,
                // Cuando un ítem es seleccionado en la barra, actualiza nuestro estado local
                onItemSelected = { newRoute ->
                    selectedItemRoute = newRoute
                    // --- NAVEGACIÓN OPCIONAL DESDE AQUÍ ---
                    // Si quieres que la barra inferior NAVEGUE en el NavController principal
                    // en lugar de solo cambiar el contenido localmente, harías esto:
                    /*
                    navController.navigate(newRoute) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true; restoreState = true
                    }
                    */
                    // PERO si lo haces así, el 'when' de abajo ya no es necesario,
                    // porque el NavHost de MainActivity manejaría qué pantalla mostrar.
                    // MANTENEMOS el cambio de estado local por ahora.
                }
            )
        }
    ) { paddingValues -> // Padding proporcionado por Scaffold

        // Contenido principal que cambia según la selección LOCAL
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // ¡Aplica el padding del Scaffold aquí!
        ) {
            // Usa 'when' para mostrar el contenido correcto basado en la ruta seleccionada LOCALMENTE
            when (selectedItemRoute) {
                BottomNavItem.Inicio.route -> {
                    HomeScreenContent(
                        navController = navController
                    ) // Llama al contenido de Inicio (definido abajo o en otro archivo)
                }
                BottomNavItem.Resenas.route -> {
                    ResenasScreenContent() // Llama al contenido de Reseñas (Placeholder abajo)
                }
                BottomNavItem.Perfil.route -> {
                    // --- ¡LLAMADA CORRECTA A PerfilScreen! ---
                    // Llama a PerfilScreen y le PASA el NavController recibido
                    PerfilScreen(navController = navController)
                    // --- FIN LLAMADA CORRECTA ---
                }
            }
        } // Fin Box contenido principal
    } // Fin Scaffold
} // Fin MainScreen


// --- Composable para la Barra de Navegación Inferior (Sin Cambios) ---
@Composable
fun AppBottomNavigationBar(
    selectedRoute: String,
    onItemSelected: (String) -> Unit
) {
    NavigationBar {
        val navItems = listOf(
            BottomNavItem.Inicio,
            BottomNavItem.Resenas,
            BottomNavItem.Perfil
        )
        navItems.forEach { item ->
            val isSelected = (item.route == selectedRoute)
            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemSelected(item.route) },
                label = { Text(item.title) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = if (isSelected) item.selectedColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedTextColor = item.selectedColor,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedIconColor = item.selectedColor,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}


@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    homeViewModel: HomeViewModel = viewModel(),
    navController: NavController
) {
    val TAG = "HomeScreenContent"
    val uiState = homeViewModel.uiState

    val context = LocalContext.current
    val activity = context as? Activity

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    val defaultInitialLocation = LatLng(40.416775, -3.703790) // Madrid Centro como fallback
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            uiState.userLocation ?: defaultInitialLocation,
            if (uiState.userLocation != null) 15f else 10f
        )
    }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() } // Para mostrar errores

    DisposableEffect(Unit) {
        homeViewModel.initializeLocationClient(context)
        onDispose {
            // Si initializeLocationClient o getCurrentUserLocation iniciaran
            // actualizaciones de ubicación continuas (con requestLocationUpdates),
            // aquí deberías detenerlas. Por ejemplo:
            // homeViewModel.stopLocationUpdates() // Necesitarías implementar esta función en tu ViewModel
            // Si solo obtienes la ubicación una vez por llamada, no se necesita limpieza aquí.
            Log.d(TAG, "DisposableEffect onDispose: HomeScreenContent leaving composition.")
        }
    }

    LaunchedEffect(Unit) {
        activity?.let {
            if (locationPermissionState.status.isGranted) {
                homeViewModel.getCurrentUserLocation(it)
            }
        }
    }

    LaunchedEffect(locationPermissionState.status) {
        if (locationPermissionState.status.isGranted) {
            activity?.let { homeViewModel.getCurrentUserLocation(it) }
        } else {
            if (locationPermissionState.status is PermissionStatus.Denied &&
                !(locationPermissionState.status as PermissionStatus.Denied).shouldShowRationale
            ) {
                Log.w(TAG, "Permiso de ubicación denegado permanentemente.")
                // Considera mostrar un Snackbar persistente o guiar a ajustes.
            }
        }
    }

    LaunchedEffect(uiState.userLocation) {
        if (uiState.userLocation != null && locationPermissionState.status.isGranted) {
            val currentTarget = cameraPositionState.position.target
            val distance = FloatArray(1)
            android.location.Location.distanceBetween(
                currentTarget.latitude, currentTarget.longitude,
                uiState.userLocation.latitude, uiState.userLocation.longitude,
                distance
            )
            if (distance[0] > 200 || cameraPositionState.position.zoom < 14f) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(uiState.userLocation, 15f),
                    1000
                )
            }
        }
    }

    LaunchedEffect(uiState.targetMapLocation) {
        if (uiState.targetMapLocation != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(uiState.targetMapLocation, 15f),
                1000
            )
            homeViewModel.consumeMapTargetLocation()
        }
    }

    LaunchedEffect(uiState.isSearching) {
        if (uiState.isSearching) {
            delay(100)
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting focus: ${e.message}", e)
            }
        } else {
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(cameraPositionState) {
        snapshotFlow { cameraPositionState.isMoving }
            .collectLatest { isMoving ->
                if (!isMoving && uiState.isSearching && !uiState.isLoading && !uiState.isLocationLoading) {
                    val currentMapCenter = cameraPositionState.position.target
                    Log.d(TAG, "Map Idle at: $currentMapCenter. Fetching places for map view.")
                    val radius = when {
                        cameraPositionState.position.zoom > 15f -> 2000
                        cameraPositionState.position.zoom > 12f -> 5000
                        else -> 10000
                    }
                    homeViewModel.loadPlacesForMap(currentMapCenter, radius = radius)
                }
            }
    }

    // Mostrar errores de carga o búsqueda con Snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMsg ->
            snackbarHostState.showSnackbar(
                message = errorMsg,
                actionLabel = "OK",
                duration = SnackbarDuration.Long
            )
            homeViewModel.clearError() // Limpia el error después de mostrarlo
        }
    }


    Column(modifier = Modifier.fillMaxSize()) {
        // --- BARRA SUPERIOR DINÁMICA ---
        if (uiState.isSearching) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.surface),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    homeViewModel.onSearchActiveChange(false)
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = { homeViewModel.onSearchQueryChange(it) },
                    placeholder = { Text("Buscar lugares...") },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (uiState.searchQuery.isNotBlank()) {
                            val currentCenter = cameraPositionState.position.target
                            homeViewModel.performSearch(
                                query = uiState.searchQuery,
                                locationForBias = currentCenter
                            )
                            focusManager.clearFocus()
                        }
                    }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = Color.Gray,
                    ),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .clickable { homeViewModel.onSearchActiveChange(true) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Iniciar Búsqueda",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buscar lugares...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // --- CONTENIDO PRINCIPAL ---
        Box(modifier = Modifier.weight(1f)) { // Box para que el Snackbar se superponga correctamente
            if (!uiState.isSearching) {
                // MODO NO BÚSQUEDA
                if (uiState.userLocation != null) {
                    PlacesCardsList(
                        userLocation = uiState.userLocation,
                        markers = uiState.mapMarkers,
                        navController = navController
                    )
                } else if (locationPermissionState.status.isGranted && uiState.isLocationLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Text("Obteniendo ubicación...", modifier = Modifier.padding(top = 16.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else if (!locationPermissionState.status.isGranted &&
                    locationPermissionState.status.shouldShowRationale ||
                    (locationPermissionState.status is PermissionStatus.Denied && !homeViewModel.uiState.permissionRequestedFirstTime) // Mostrar si se necesita rationale o es la primera vez que se pide (y fue denegado)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("El permiso de ubicación ayuda a encontrar lugares relevantes.", textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Puedes otorgar el permiso para mejorar tu experiencia.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                locationPermissionState.launchPermissionRequest()
                                homeViewModel.setPermissionRequested()
                            }) {
                                Text("Otorgar Permiso de Ubicación")
                            }
                        }
                    }
                } else if (!locationPermissionState.status.isGranted) { // Denegado y no se debe mostrar rationale (o ya se pidió y se denegó)
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "El permiso de ubicación fue denegado. Para usar esta funcionalidad, por favor habilítalo en los ajustes de la aplicación.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                // Guía al usuario a los ajustes de la app.
                                // Necesitas el `activity` para esto.
                                activity?.let {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    val uri = android.net.Uri.fromParts("package", it.packageName, null)
                                    intent.data = uri
                                    it.startActivity(intent)
                                }
                            }) {
                                Text("Abrir Ajustes de la App")
                            }
                        }
                    }
                } else { // Permiso concedido, pero uiState.userLocation es null y no está cargando
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No se pudo obtener la ubicación actual.", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { activity?.let { homeViewModel.getCurrentUserLocation(it) } }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
            } else { // MODO BÚSQUEDA: Mapa
                Box(
                    modifier = Modifier.fillMaxSize() // El .weight(1f) ya no es necesario si el padre es solo este Box
                ) {
                    GoogleMap(
                        modifier = Modifier.matchParentSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            minZoomPreference = 3.5f,
                            maxZoomPreference = 20.0f,
                            isMyLocationEnabled = locationPermissionState.status.isGranted
                        ),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = true,
                            mapToolbarEnabled = false,
                            myLocationButtonEnabled = false
                        ),
                        onMapLoaded = {
                            Log.d(TAG, "Map Loaded.")
                            if (uiState.mapMarkers.isEmpty()) {
                                val initialLoadLocation = uiState.userLocation ?: cameraPositionState.position.target
                                homeViewModel.loadPlacesForMap(initialLoadLocation, radius = 10000)
                            }
                        }
                    ) {
                        val markersToDisplay = uiState.mapMarkers
                        markersToDisplay.forEach { markerData ->
                            Marker(
                                state = MarkerState(position = markerData.position),
                                title = markerData.title,
                                snippet = markerData.snippet,
                                tag = markerData.id,
                                onClick = { clickedMarker ->
                                    Log.d(TAG, "Marker clicked: Title='${clickedMarker.title}', Tag='${clickedMarker.tag}'")
                                    // homeViewModel.onMarkerClicked(clickedMarker.tag as String)
                                    true
                                }
                            )
                        }
                    }

                    if (locationPermissionState.status.isGranted) {
                        FloatingActionButton(
                            onClick = {
                                activity?.let { act -> homeViewModel.getCurrentUserLocation(act) }
                                    ?: Log.e(TAG, "Activity es null, no se puede obtener ubicación actual.")
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(Icons.Filled.MyLocation, contentDescription = "Centrar en mi ubicación")
                        }
                    }

                    if (uiState.isLoading || uiState.isLocationLoading) {
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(8.dp))
                                if (uiState.isLocationLoading) Text("Buscando tu ubicación...", style = MaterialTheme.typography.bodyMedium)
                                else if (uiState.isLoading) Text("Cargando lugares...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                } // Fin Box del Mapa
            }
            // SnackbarHost para mostrar errores
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } // Fin Box contenido principal (con weight)
    } // Fin Column principal
}


// --- Contenidos Placeholder/Reales para cada Pestaña ---
@Composable fun ResenasScreenContent() { PlaceholderScreen("TUS RESEÑAS") }




// --- Composable Auxiliar para Placeholders ---
@Composable
fun PlaceholderScreen(screenName: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Pantalla de $screenName", style = MaterialTheme.typography.headlineMedium)
    }
}

// --- Preview para MainScreen ---
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    HearMeTheme {
        val previewNavController = rememberNavController()
        // val mockAuth: FirebaseAuth = mock() // Necesitarías una librería de mock como Mockito
        // MainScreen(auth = mockAuth, navController = previewNavController)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Preview de MainScreen (Contenido dinámico dentro)")
        }
    }
}