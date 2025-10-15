package com.example.hearme.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star // O podrías usar mic, list, etc.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector


val BottomNavBlue = Color(0xFF648FF8)
val BottomNavPink = Color(0xFFE36B9E)

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val selectedColor: Color
) {
    data object Inicio : BottomNavItem("inicio", "Inicio", Icons.Default.Home, BottomNavBlue)
    data object Resenas : BottomNavItem("resenas", "Tus Reseñas", Icons.Default.Star, BottomNavPink)
    data object Perfil : BottomNavItem("perfil", "Perfil", Icons.Default.Person, BottomNavBlue)

    companion object {
        val entries = listOf(Inicio, Resenas, Perfil)
    }
}