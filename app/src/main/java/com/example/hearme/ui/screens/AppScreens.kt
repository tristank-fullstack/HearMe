package com.example.hearme.ui.screens

sealed class AppScreens(val route: String) {
    // Pantallas de Autenticación
    object SplashScreen : AppScreens("splash_screen")
    object LoginScreen : AppScreens("login_screen")
    object SignUpScreen : AppScreens("signup_screen")
    object CompleteProfileScreen : AppScreens("complete_profile_screen")

    // Pantallas Principales de la App (después de iniciar sesión)
    object HomeScreen : AppScreens("home_screen") // Asumiendo que tienes una pantalla principal
    object RecordScreen : AppScreens("record_screen") // Asumiendo para grabar audio
    object PerfilScreen : AppScreens("perfil_screen") // Tu pantalla de perfil actual

    // Puedes añadir más pantallas según las necesites
    // object SettingsScreen : AppScreens("settings_screen")
    // object AudioDetailScreen : AppScreens("audio_detail_screen/{audioId}") {
    //    fun createRoute(audioId: String) = "audio_detail_screen/$audioId"
    // }

    // Puedes añadir más rutas aquí a medida que tu aplicación crezca.
    // Por ejemplo, si tienes una pantalla para editar el perfil:
    // object EditProfileScreen : AppScreens("edit_profile_screen")
}