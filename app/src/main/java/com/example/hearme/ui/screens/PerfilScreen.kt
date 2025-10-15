package com.example.hearme.ui.screens

import android.R.attr.contentDescription
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hearme.viewmodels.ProfileViewModel
import com.example.hearme.viewmodels.UserProfile
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import com.example.hearme.data.components.AudioReviewItem
import com.example.hearme.data.network.models.AudioReview
import com.example.hearme.viewmodels.ProfileUiState

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
// import androidx.compose.foundation.background // No lo usamos directamente en el último ejemplo, pero podría ser útil
import androidx.compose.foundation.border
// import androidx.compose.foundation.clickable // No lo usamos para el icono de ajustes en el último ejemplo, pero podría ser para otros elementos
import androidx.compose.foundation.layout.Arrangement // Si usas Arrangement en algún Row o Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings // Para el icono de Ajustes
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton // Para el botón del icono de Ajustes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
// import androidx.compose.material3.TopAppBarDefaults // Si necesitas personalizar colores de TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue // Para `by viewModel...collectAsState()`
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue // Para `by remember { mutableStateOf(...) }`
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color // Para el color del texto del nombre
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext // Para Coil ImageRequest.Builder
import androidx.compose.ui.semantics.error
// import androidx.compose.ui.res.painterResource // Si usaras painterResource para algún icono local
import androidx.compose.ui.text.font.FontWeight
// import androidx.compose.ui.unit.sp // Si defines tamaños de texto en sp directamente
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter // Para Coil
import coil.request.ImageRequest
import com.example.hearme.R


@OptIn(ExperimentalMaterial3Api::class) // Necesario para Scaffold
@Composable
fun PerfilScreen( // Mantén los parámetros que ya tenías
    navController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    // Recoge el UiState del ViewModel como un State
    val uiState by viewModel.userProfileUiState.collectAsState()

    // El ViewModel se encarga de cargar el perfil en su init si es necesario,
    // o puedes tener una función específica si quieres recargarlo en algún evento.

    Scaffold(
        // topBar = { /* TopAppBar eliminada o comentada */ } // <--- CAMBIO PRINCIPAL AQUÍ
        // Ya no definimos un topBar aquí.
    ) { paddingValues -> // paddingValues ahora solo considerará los insets del sistema (barra de estado, etc.)
        // y el bottomBar si tuvieras uno.
        HandleProfileState(
            modifier = Modifier.padding(paddingValues), // Pasamos el padding del Scaffold
            navController = navController,
            uiState = uiState,
            viewModel = viewModel
        )
    }
}

@Composable
private fun HandleProfileState(
    modifier: Modifier = Modifier,
    navController: NavController,
    uiState: ProfileUiState,
    viewModel: ProfileViewModel // Para llamar a las funciones de carga de listas
) {
    Box(
        modifier = modifier.fillMaxSize(), // Aplicar padding del Scaffold aquí
        contentAlignment = Alignment.Center
    ) {
        when {
            // Mostrar carga inicial del perfil solo si no hay datos de perfil aún
            uiState.isLoadingProfile && uiState.userProfile == null -> {
                CircularProgressIndicator()
            }
            // Mostrar error inicial del perfil solo si no hay datos de perfil
            uiState.profileError != null && uiState.userProfile == null -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Text(text = "Error: ${uiState.profileError}", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadUserProfile() }) { Text("Reintentar") } // Asume que tienes esta función en VM
                }
            }
            uiState.userProfile != null -> {
                // El perfil está cargado (o se está recargando pero ya tenemos datos)
                ProfileContent(
                    navController = navController,
                    userProfile = uiState.userProfile,
                    uiState = uiState, // Pasar el UiState completo para las listas
                    onLoadSavedReviews = { viewModel.listenForSavedReviews() },
                    onLoadLikedReviews = { viewModel.listenForLikedReviews() }
                )
            }
            // Caso por defecto (puede ser un estado inicial antes de que comience cualquier carga)
            else -> {
                Text("Cargando perfil...", modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
fun ProfileContent(
    navController: NavController,
    userProfile: UserProfile,
    uiState: ProfileUiState, // Recibe el UiState completo para acceder a las listas
    onLoadSavedReviews: () -> Unit,
    onLoadLikedReviews: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("like") } // "like" o "save"

    // Efecto para cargar datos cuando la pestaña cambia o si el listener no está activo
    LaunchedEffect(selectedTab, uiState.isListeningToSavedReviews, uiState.isListeningToLikedReviews) {
        Log.d("PerfilScreen", "Selected tab: $selectedTab, isListeningSaved: ${uiState.isListeningToSavedReviews}, isListeningLiked: ${uiState.isListeningToLikedReviews}")
        if (selectedTab == "save" && !uiState.isListeningToSavedReviews) {
            Log.d("PerfilScreen", "Pestaña 'Guardados' - Iniciando listener.")
            onLoadSavedReviews()
        } else if (selectedTab == "like" && !uiState.isListeningToLikedReviews) {
            Log.d("PerfilScreen", "Pestaña 'Me Gusta' - Iniciando listener.")
            onLoadLikedReviews()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp) // Padding general para el contenido de ProfileContent
    ) {
        // --- Card Superior (Adaptado de tu código) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) { // Box para superponer el IconButton de Ajustes
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val imageModifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant) // Fondo si la imagen es transparente o placeholder
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)

                        if (!userProfile.profileImageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(userProfile.profileImageUrl)
                                    .crossfade(true)
                                    // No especificamos .placeholder() o .error() aquí si no queremos usar drawables
                                    // AsyncImage tiene sus propios parámetros para esto.
                                    .build(),
                                contentDescription = "Foto de perfil",
                                modifier = imageModifier,
                                contentScale = ContentScale.Crop,
                                // Painter para el estado de placeholder (mientras carga)
                                placeholder = rememberVectorPainter(image = Icons.Default.Person),
                                // Painter para el estado de error (si falla la carga de la URL)
                                error = rememberVectorPainter(image = Icons.Default.Person),
                                // Painter si el 'model' es null (ya lo manejamos con el if, pero es bueno saberlo)
                                // fallback = rememberVectorPainter(image = Icons.Default.Person)
                            )
                        } else {
                            // Si profileImageUrl es null o blank, muestra el icono de persona directamente
                            Image(
                                painter = rememberVectorPainter(image = Icons.Default.Person),
                                contentDescription = "Foto de perfil por defecto",
                                modifier = imageModifier,
                                contentScale = ContentScale.Fit // O Inside, para que el icono no se recorte si no llena el círculo
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = userProfile.username,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f) // Para que el nombre tome el espacio y el botón de editar se alinee bien
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Botón Editar Perfil
                    Button(
                        onClick = {
                            // TODO: Asegúrate que "edit_profile" está en tu AppScreens
                            // navController.navigate(AppScreens.EditProfileScreen.route)
                            navController.navigate("edit_profile")
                            Log.d("PerfilScreen", "Navegar a Editar Perfil")
                        },
                        modifier = Modifier.align(Alignment.End) // Alinea el botón a la derecha
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Editar Perfil")
                    }
                }
                // Icono de Ajustes (superpuesto en la esquina superior derecha del Card)
                IconButton(
                    onClick = {
                        // TODO: Asegúrate que "settings" está en tu AppScreens
                        // navController.navigate(AppScreens.SettingsScreen.route)
                        navController.navigate("settings")
                        Log.d("PerfilScreen", "Navegar a Ajustes")
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd) // Alinea el icono en la esquina superior derecha del Box padre
                        .padding(top = 4.dp, end = 4.dp) // Pequeño padding para no pegarlo al borde
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Configuración")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Pestañas Like/Guardar (de tu código) ---
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { selectedTab = "like" }, modifier = Modifier.weight(1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Reseñas con Like",
                        tint = if (selectedTab == "like") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        "Me gusta",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selectedTab == "like") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = { selectedTab = "save" }, modifier = Modifier.weight(1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Reseñas Guardadas",
                        tint = if (selectedTab == "save") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        "Guardados",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selectedTab == "save") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        // Indicadores de Pestaña
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier
                .weight(1f)
                .height(3.dp)
                .background(if (selectedTab == "like") MaterialTheme.colorScheme.primary else Color.Transparent))
            Box(modifier = Modifier
                .weight(1f)
                .height(3.dp)
                .background(if (selectedTab == "save") MaterialTheme.colorScheme.primary else Color.Transparent))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Contenido Condicional de Pestañas ---
        Box(modifier = Modifier.weight(1f)) { // Permite que esta sección tome el espacio restante para el scroll
            when (selectedTab) {
                "like" -> {
                    DisplayReviewList(
                        reviews = uiState.likedReviews,
                        isLoading = uiState.isLoadingLikedReviews,
                        error = uiState.likedReviewsError,
                        emptyMessage = "No te ha gustado ninguna reseña aún.",
                        isListening = uiState.isListeningToLikedReviews,
                        onRetry = { onLoadLikedReviews() } // Para reintentar si hay error
                    )
                }
                "save" -> {
                    DisplayReviewList(
                        reviews = uiState.savedReviews,
                        isLoading = uiState.isLoadingSavedReviews,
                        error = uiState.savedReviewsError,
                        emptyMessage = "No tienes reseñas guardadas.",
                        isListening = uiState.isListeningToSavedReviews,
                        onRetry = { onLoadSavedReviews() } // Para reintentar si hay error
                    )
                }
            }
        }
    }
}

@Composable
private fun DisplayReviewList(
    reviews: List<AudioReview>,
    isLoading: Boolean,
    error: String?,
    emptyMessage: String,
    isListening: Boolean,
    onRetry: () -> Unit // Callback para el botón de reintentar
) {
    when {
        // Mostrar carga si está cargando Y (no está escuchando O no hay reseñas aún)
        isLoading && (!isListening || reviews.isEmpty()) -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        error != null -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Error al cargar: $error", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                Button(onClick = onRetry) { Text("Reintentar") }
            }
        }
        reviews.isEmpty() && isListening -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(emptyMessage, style = MaterialTheme.typography.bodyLarge)
            }
        }
        // Si no está escuchando, no está cargando y no hay datos (podría ser el estado inicial antes de que se active el listener)
        reviews.isEmpty() && !isLoading && !isListening -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(emptyMessage, style = MaterialTheme.typography.bodyLarge) // O un mensaje como "Selecciona para cargar..."
            }
        }
        reviews.isNotEmpty() -> {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(reviews, key = { it.reviewId }) { review ->
                    AudioReviewItem(audioReview = review) // Sigue pasando el objeto review completo
                    Divider()
                }
            }
        }
        // Podría haber un caso donde no está cargando, no hay error, no está escuchando, pero la lista está vacía.
        // El caso de arriba (reviews.isEmpty() && !isLoading && !isListening) ya lo cubre.
    }
}