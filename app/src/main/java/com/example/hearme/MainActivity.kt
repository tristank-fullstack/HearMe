package com.example.hearme

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseUser // Importa FirebaseUser
import com.example.hearme.ui.theme.HearMeTheme // ¡Usa el nombre de tu tema! (Parece ser HearmeTheme)
import kotlinx.coroutines.channels.awaitClose // Importa awaitClose
import kotlinx.coroutines.flow.Flow // Importa Flow
import kotlinx.coroutines.flow.callbackFlow // Importa callbackFlow

import android.provider.Settings

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hierarchy

import com.example.hearme.ui.screens.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.hearme.ui.navigation.BottomNavItem
import com.example.hearme.ui.screens.CompleteProfileScreen
import com.example.hearme.ui.screens.EditProfileScreen
import com.example.hearme.ui.screens.LoginScreen
import com.example.hearme.ui.screens.ForgotPasswordScreen
import com.example.hearme.ui.screens.HomeScreenContent
import com.example.hearme.ui.screens.MainScreen
import com.example.hearme.ui.screens.PerfilScreen
import com.example.hearme.ui.screens.RegisterScreen
import com.example.hearme.ui.screens.ResenasScreenContent
import com.example.hearme.viewmodels.ProfileViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.firebase.auth.FirebaseAuth



import android.Manifest
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star


import androidx.compose.material3.*

import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.android.gms.maps.MapsInitializer
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory


class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(applicationContext)

        // Configura App Check usando el proveedor de Play Integrity (opcional: solo si usas App Check)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        // Obtén la instancia de FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // *** Agrega este bloque para imprimir el token en Logcat ***
        val currentUser = auth.currentUser
        if (currentUser != null) {
            currentUser.getIdToken(false).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result?.token
                    Log.d("FirebaseToken", "Token obtenido: $token")
                } else {
                    Log.e("FirebaseToken", "Error al obtener el token", task.exception)
                }
            }
        } else {
            Log.d("FirebaseToken", "No hay usuario autenticado")
        }

        // Inicializa Google Maps
        MapsInitializer.initialize(applicationContext)
        setContent {
            HearMeTheme {
                MainAppWithPermissionCheck(auth)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainAppWithPermissionCheck(auth: FirebaseAuth) {
    val navController = rememberNavController()
    val profileViewModel: ProfileViewModel = viewModel()
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    var permissionRequested by remember { mutableStateOf(false) }
    val currentUser by remember(auth) { auth.authStateFlow() }.collectAsState(initial = auth.currentUser)

    val startDestination = remember(currentUser, locationPermissionState.status.isGranted) {
        when {
            currentUser == null -> "login"
            locationPermissionState.status.isGranted -> BottomNavItem.Inicio.route
            else -> "permission_check"
        }
    }

    LaunchedEffect(key1 = currentUser, key2 = locationPermissionState.status, key3 = permissionRequested) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        Log.d("MainApp", "Effect: User=${currentUser?.uid}, Perm=${locationPermissionState.status}, Route=$currentRoute, Requested=$permissionRequested")
        val authRoutes = setOf("login", "register", "forgot_password", "complete_profile")

        if (currentUser != null) {
            if (!locationPermissionState.status.isGranted) {
                if (currentRoute != "permission_denied" && currentRoute != "permission_check") {
                    if (!permissionRequested) {
                        locationPermissionState.launchPermissionRequest(); permissionRequested = true
                    } else if (currentRoute != "permission_denied") {
                        navController.navigate("permission_denied") { popUpTo(0) { inclusive = true } }
                    }
                } else if (currentRoute == "permission_check" && permissionRequested && !locationPermissionState.status.isGranted) {
                    navController.navigate("permission_denied") { popUpTo(0) { inclusive = true } }
                } else if (currentRoute == "permission_check" && !permissionRequested) {
                    locationPermissionState.launchPermissionRequest(); permissionRequested = true
                }
            } else { // Permiso concedido
                val isComplete = profileViewModel.isCurrentUserProfileComplete()
                when (isComplete) {
                    true -> if (currentRoute in authRoutes || currentRoute == "permission_check" || currentRoute == "permission_denied") {
                        navController.navigate("inicio") { popUpTo(0) { inclusive = true } } // <-- Usa string "inicio"
                    }
                    false -> if (currentRoute != "complete_profile") {
                        navController.navigate("complete_profile") { popUpTo(0) { inclusive = true } }
                    }
                    null -> if (currentRoute != "login") {
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    }
                }
                }
            // --- FIN OBTENER TOKEN ---
        } else { // No hay usuario (firebaseUser es null)
            // Lógica para navegar a "login" si estábamos en una pantalla protegida
            val mainAppRoutes = setOf("inicio", "resenas", "perfil", "settings", "edit_profile", "permission_check", "permission_denied")
            if (currentRoute != null && currentRoute in mainAppRoutes) {
                Log.d("MainApp", "User logged out, navigating to login from $currentRoute")
                navController.navigate("login") { popUpTo(0) { inclusive = true } }
            }
        }
    }

    // --- Renderizado Condicional ---
    if (currentUser == null || locationPermissionState.status.isGranted) {
        MainScaffoldWithNavHost(navController = navController, auth = auth, startDestination = startDestination)
    } else {
        PermissionDeniedScreen(
            permissionState = locationPermissionState,
            onRetry = { locationPermissionState.launchPermissionRequest() }
        )
    }
}

// --- Scaffold Principal y NavHost ---
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScaffoldWithNavHost(
    navController: NavHostController,
    auth: FirebaseAuth, // auth se pasa por si alguna pantalla lo necesita directamente
    startDestination: String
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Determina si se debe mostrar la barra inferior basándose en la ruta actual
    // Las rutas de la barra inferior están definidas en BottomNavItem
    val bottomBarRoutes = remember { setOf("inicio", "resenas", "perfil") }
    val shouldShowBottomBar = currentDestination?.hierarchy?.any { it.route in bottomBarRoutes } == true
    var currentBarName by remember { mutableStateOf("") }

// Simulación de obtener el nombre desde la API (esto puede ser un efecto o callback)
    LaunchedEffect(Unit) {
        // Aquí harías tu llamada a la API de Google y asignarías el resultado.
        // Por ejemplo, supongamos que la API te devuelve "El Bar Real" después de un tiempo.
        kotlinx.coroutines.delay(1000) // simula tiempo de respuesta
        currentBarName = "El Bar Real" // aquí se asigna dinámicamente el valor obtenido
    }

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) { // Muestra la barra solo en las rutas principales
                AppBottomNavigationBar(navController = navController)
            }
        }
        ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Destinos de Autenticación
            composable("login") { LoginScreen(onLoginSuccess = {}, onNavigateToCompleteProfile = { navController.navigate("complete_profile"){ popUpTo("login"){inclusive=true} } }, onRegisterClick = { navController.navigate("register") }, onForgotPasswordClick = { navController.navigate("forgot_password") }) }
            composable("register") { RegisterScreen(onAuthSuccess = {navController.navigate("complete_profile"){ popUpTo("register"){inclusive=true} }}, onBackClick = {navController.popBackStack()} )}
            composable("forgot_password") { ForgotPasswordScreen(onEmailSent = {navController.popBackStack()}, onBackClick = {navController.popBackStack()} )}
            composable("complete_profile") { CompleteProfileScreen(onProfileCompleteSuccess = { navController.navigate(BottomNavItem.Inicio.route){ popUpTo("login"){inclusive=true} }})}


            composable(
                route = "resenas/{placeId}?name={localName}&localImage={localImage}",
                arguments = listOf(
                    navArgument("placeId") { type = NavType.StringType },
                    navArgument("localName") { type = NavType.StringType; defaultValue = ""; nullable = true },
                    navArgument("localImage") { type = NavType.StringType; defaultValue = ""; nullable = true }
                )
            ) { backStackEntry ->
                val placeId = backStackEntry.arguments?.getString("placeId") ?: ""
                val localName = backStackEntry.arguments?.getString("localName") ?: ""
                val localImage = backStackEntry.arguments?.getString("localImage") ?: ""
                val userName = backStackEntry.arguments?.getString("userName") ?: ""
                android.util.Log.d("NavHost", "localImage recibido: $localImage")
                ReviewScreen(
                    placeId = placeId,
                    localName = localName,
                    localImage = localImage,
                    userName = userName
                )
            }
            // Destinos Principales de la Barra Inferior
            composable(BottomNavItem.Inicio.route) { HomeScreenContent(
                navController = navController
            ) }


            composable(BottomNavItem.Resenas.route) { MyReviewsScreen(defaultBarName = currentBarName)}
            composable(BottomNavItem.Perfil.route) { PerfilScreen(navController = navController) }

            // Destinos Secundarios (Navegados desde Perfil)
            composable("settings") {
                Log.d("NavHost", "Composing SettingsScreen route") // Log para ver si llega aquí
                SettingsScreen(navController = navController)
            }
            composable("edit_profile") {
                EditProfileScreen(onNavigateBack = { navController.popBackStack() })
            }

            // Destinos de Permisos
            composable("permission_check") { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center){ CircularProgressIndicator() } }
            composable("permission_denied") {
                // Este es un fallback, el principal se renderiza fuera del NavHost
                val tempPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
                PermissionDeniedScreen(permissionState = tempPermissionState, onRetry = { tempPermissionState.launchPermissionRequest() })
            }
        } // Fin NavHost
    } // Fin Scaffold
}


// --- Pantalla de Permiso Denegado (Sin cambios) ---
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionDeniedScreen(permissionState: PermissionState, onRetry: () -> Unit) {
    // ... (código igual que antes)
    val context = LocalContext.current
    val isPermanentlyDenied = !permissionState.status.shouldShowRationale
    val appSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { Log.d("PermissionDeniedScreen", "Returned from app settings.") }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text( "Permiso de Ubicación Requerido", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            if (isPermanentlyDenied) "Has denegado permanentemente el permiso de ubicación. La aplicación lo necesita para funcionar. Por favor, habilítalo manualmente en los ajustes."
            else "La aplicación necesita permiso de ubicación para mostrar el mapa y sitios cercanos. Por favor, concede el permiso.",
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            if (isPermanentlyDenied) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", context.packageName, null)
                appSettingsLauncher.launch(intent)
            } else {
                onRetry()
            }
        }) {
            Text(if (isPermanentlyDenied) "Abrir Ajustes" else "Conceder Permiso")
        }
    }
}


// --- Barra de Navegación Inferior (Sin cambios, usando BottomNavItem.entries) ---
@Composable
fun AppBottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        // Define los items localmente usando Triple(ruta: String, título: String, icono: ImageVector)
        val navItemsData = remember {
            listOf(
                Triple("inicio", "Inicio", Icons.Default.Home),
                Triple("resenas", "Tus Reseñas", Icons.Default.Star),
                Triple("perfil", "Perfil", Icons.Default.Person)
            )
        }
        val blue = Color(0xFF648FF8); val pink = Color(0xFFE36B9E); val defaultColor = MaterialTheme.colorScheme.onSurfaceVariant

        navItemsData.forEach { (route, title, icon) -> // Itera sobre la lista local
            val isSelected = currentDestination?.hierarchy?.any { it.route == route } == true // Compara con string

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        navController.navigate(route) { // Navega usando string 'route'
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    }
                },
                label = { Text(title) },
                icon = {
                    val itemColor = when(route) { "inicio" -> blue; "resenas" -> pink; "perfil" -> blue; else -> defaultColor }
                    Icon(icon, contentDescription = title, tint = if (isSelected) itemColor else defaultColor)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedTextColor = when(route){"inicio"->blue;"resenas"->pink;"perfil"->blue;else->defaultColor},
                    unselectedTextColor = defaultColor,
                    selectedIconColor = when(route){"inicio"->blue;"resenas"->pink;"perfil"->blue;else->defaultColor},
                    unselectedIconColor = defaultColor,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

// --- Función de Extensión Auth Flow (Sin Cambios) ---
fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser).isSuccess }
    addAuthStateListener(listener); awaitClose { removeAuthStateListener(listener) }
}
// --- Placeholder Screen ---
@Composable
fun PlaceholderScreen(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Pantalla $name (TODO)") }
}