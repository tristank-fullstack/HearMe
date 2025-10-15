package com.example.hearme.data.components

import android.util.Log
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.hearme.utils.calculateDistance
import com.example.hearme.viewmodels.MapMarkerInfo
import com.google.android.gms.maps.model.LatLng


@Composable
fun PlacesCardsList(
    userLocation: LatLng, // Este es com.google.android.gms.maps.model.LatLng
    markers: List<MapMarkerInfo>,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    // 1. Depuración: registra el número total de marcadores recibidos
    Log.d("MarkersRaw", "Total markers: ${markers.size}")

    // 2. Muestra para cada marcador su distancia y los tipos retornados
    markers.forEach { marker ->
        // ASUMO que marker.position es com.google.android.gms.maps.model.LatLng
        // Y que userLocation es com.google.android.gms.maps.model.LatLng
        // Y que tu función calculateDistance espera estos tipos (o los que realmente sean)

        // --- CORRECCIÓN AQUÍ ---
        val distance = calculateDistance(userLocation, marker.position) // <--- Coma añadida
        Log.d("DistanceCheck", "${marker.title}: $distance metros, types: ${marker.types}")
    }

    // 3. Filtra todos los marcadores dentro del radio deseado (por ejemplo, 2000 metros = 2 km)
    val distanceThreshold = 2000f
    val markersWithinRadius = markers.filter { marker ->
        // --- LA LLAMADA AQUÍ PARECÍA CORRECTA, PERO VERIFICA LOS TIPOS ---
        calculateDistance(userLocation, marker.position) <= distanceThreshold
    }
    Log.d("MarkersWithinRadius", "Markers within $distanceThreshold m: ${markersWithinRadius.size}")

    // 4. Define la lista de tipos permitidos (todo en minúsculas)
    val allowedTypes = listOf(
        "restaurant",
        "bar",
        "cafe",
        "bakery",
        "meal_takeaway",
        "meal_delivery",
        "grocery_or_supermarket",
        "convenience_store",
        "food",
        "point_of_interest",
        "establishment",
        "local_dining"
    )

    // 5. Define una lista de palabras clave que indiquen establecimientos de comida
    val foodKeywords = listOf("restaurante", "café", "bar", "comida", "dining", "fermín")

    // 6. Filtra por tipo y, si no hay coincidencia, se chequea el título o snippet
    val filteredMarkers = markersWithinRadius.filter { marker ->
        if (marker.types.isEmpty()) {
            true
        } else {
            val typeMatches = marker.types.map { it.lowercase() }
                .any { it in allowedTypes }
            if (typeMatches) {
                true
            } else {
                val titleSnippet = (marker.title + " " + (marker.snippet ?: "")).lowercase()
                foodKeywords.any { keyword -> titleSnippet.contains(keyword) }
            }
        }
    }
    Log.d("MarkersAfterFiltering", "Markers after filtering: ${filteredMarkers.size}")

    // Nota: Tienes un Log.d duplicado aquí, puedes quitar uno si quieres.
    // Log.d("MarkersAfterFiltering", "Markers after filtering: ${filteredMarkers.size}")

    LazyColumn(modifier = modifier) {
        // Deberías iterar sobre filteredMarkers aquí, no sobre los 'markers' originales
        // si el objetivo es mostrar solo los filtrados.
        items(filteredMarkers) { marker -> // <--- CAMBIADO DE 'markers' A 'filteredMarkers'
            PlaceCard(
                marker = marker,
                navController = navController
                // userLocation = userLocation // Podrías necesitar pasar userLocation a PlaceCard si calcula distancia ahí
            )
        }
    }
}