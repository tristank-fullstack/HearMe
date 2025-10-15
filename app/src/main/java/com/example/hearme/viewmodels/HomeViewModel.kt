package com.example.hearme.viewmodels

import android.Manifest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.hearme.data.network.RetrofitClient // Importa tu cliente
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import android.location.Location
import com.example.hearme.data.network.models.PlaceResult

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.compose.ui.semantics.error
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewModelScope
import com.example.hearme.utils.getPhotoUrl
import com.google.android.gms.location.* // Importa todo de location
import kotlinx.coroutines.tasks.await

data class HomeUiState(
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val isLoading: Boolean = false,       // <-- NUEVO: Para mostrar progreso
    val searchError: String? = null,      // <-- NUEVO: Para mostrar errores
    val targetMapLocation: LatLng? = null, // <-- NUEVO: Coordenadas a donde mover el mapa
    val userLocation: LatLng? = null,     // ¡NUEVO! Ubicación actual del usuario
    val isLocationLoading: Boolean = false, // ¡NUEVO! Loading específico de ubicación
    val locationError: String? = null,     // ¡NUEVO! Error específico de ubicación
    val mapMarkers: List<MapMarkerInfo> = emptyList(),
    val error: String? = null,
    val permissionRequestedFirstTime: Boolean = false // Para la lógica de UI de permisos
)
data class MapMarkerInfo(
    val id: String,         // Place ID de Google
    val title: String,
    val snippet: String?,
    val position: LatLng,
    val photoReference: String? = null,
    val types: List<String> = emptyList(),
    val imageUrl: String? = null  // Agregamos este campo para la URL de la foto
)


class HomeViewModel : ViewModel() {

    var uiState by mutableStateOf(HomeUiState())
        private set

    private val googleApiService = RetrofitClient.googleApiService
    private var searchJob: Job? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Tipos de lugares por defecto para nearbySearch cuando el mapa se mueve
    // Puedes hacerlo más configurable si es necesario
    private val defaultPlaceTypesForMapMove = "restaurant|cafe|bar|point_of_interest|establishment"
    private val TAG = "HomeViewModel"

    fun initializeLocationClient(context: Context) {
        if (!::fusedLocationClient.isInitialized) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            Log.d(TAG, "FusedLocationProviderClient inicializado.")
        }
    }

    /**
     * Carga lugares cercanos a la ubicación dada, típicamente llamada cuando el mapa se mueve
     * o después de obtener la ubicación del usuario.
     */
    fun loadPlacesForMap(center: LatLng, radius: Int, types: String = defaultPlaceTypesForMapMove) {
        Log.d(TAG, "loadPlacesForMap: Cargando lugares ($types) cerca de: $center con radio de $radius metros")
        uiState = uiState.copy(isLoading = true, error = null) // Limpia error anterior

        viewModelScope.launch {
            try {
                val locationStr = "${center.latitude},${center.longitude}"
                val response = googleApiService.nearbySearch(
                    location = locationStr,
                    radius = radius,
                    type = types
                )

                if (response.isSuccessful && response.body() != null) {
                    val places = response.body()!!.results
                    if (!places.isNullOrEmpty()) {
                        val newMarkers = places.mapNotNull { place ->
                            place.geometry?.location?.let { loc ->
                                MapMarkerInfo(
                                    id = place.placeId,
                                    position = LatLng(loc.lat, loc.lng),
                                    title = place.name,
                                    snippet = place.vicinity ?: place.types?.firstOrNull(),
                                    photoReference = place.photos?.firstOrNull()?.photoReference
                                )
                            }
                        }
                        // Fusionar con marcadores existentes o reemplazar.
                        // Aquí se fusionan para una experiencia de mapa más continua.
                        // Si prefieres reemplazar: uiState.copy(mapMarkers = newMarkers)
                        val currentMarkersMap = uiState.mapMarkers.associateBy { it.id }.toMutableMap()
                        newMarkers.forEach { currentMarkersMap[it.id] = it }

                        uiState = uiState.copy(isLoading = false, mapMarkers = currentMarkersMap.values.toList())
                        Log.d(TAG, "loadPlacesForMap: ${newMarkers.size} lugares cargados y fusionados.")
                    } else {
                        Log.w(TAG, "loadPlacesForMap: NearbySearch OK pero 'results' es null o vacío. Status: ${response.body()!!.status}")
                        // No necesariamente un error para el usuario si es ZERO_RESULTS, simplemente no hay marcadores que añadir.
                        // Si es otro status, podría ser un error.
                        if (response.body()!!.status != "OK" && response.body()!!.status != "ZERO_RESULTS") {
                            uiState = uiState.copy(
                                isLoading = false,
                                error = "No se encontraron lugares (${response.body()!!.status})."
                            )
                        } else {
                            uiState = uiState.copy(isLoading = false) // Simplemente no hay resultados, no es un error de app
                        }
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    val apiStatus = response.body()?.status
                    val apiErrorMessage = response.body()?.errorMessage
                    val errorMsg = "Error en NearbySearch: ${response.code()} - $errorBody - API Status: $apiStatus - API Msg: $apiErrorMessage"
                    Log.e(TAG, "loadPlacesForMap: $errorMsg")
                    uiState = uiState.copy(isLoading = false, error = "Error al buscar lugares cercanos: ${response.code()}")
                }
            } catch (e: HttpException) {
                Log.e(TAG, "loadPlacesForMap: HTTP Error", e)
                uiState = uiState.copy(isLoading = false, error = "Error del servidor (${e.code()}). Inténtalo más tarde.")
            } catch (e: IOException) {
                Log.e(TAG, "loadPlacesForMap: Network Error", e)
                uiState = uiState.copy(isLoading = false, error = "Error de red al buscar lugares.")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.w(TAG, "loadPlacesForMap Job was cancelled.")
                    uiState = uiState.copy(isLoading = false)
                    throw e
                }
                Log.e(TAG, "loadPlacesForMap: Excepción desconocida", e)
                uiState = uiState.copy(isLoading = false, error = "Error desconocido al buscar lugares.")
            }
        }
    }

    fun onSearchActiveChange(isActive: Boolean) {
        Log.d(TAG, "onSearchActiveChange: $isActive")
        val newQuery = if (!isActive) "" else uiState.searchQuery
        // Solo actualizar si realmente hay un cambio para evitar recomposiciones innecesarias
        if (uiState.isSearching != isActive || (!isActive && uiState.searchQuery.isNotEmpty())) {
            uiState = uiState.copy(
                isSearching = isActive,
                searchQuery = newQuery, // Limpiar query si se desactiva la búsqueda
                error = null,
                targetMapLocation = null
            )
            if (!isActive) {
                searchJob?.cancel()
                // Considera si quieres limpiar los marcadores del mapa cuando se sale del modo búsqueda
                // uiState = uiState.copy(mapMarkers = emptyList())
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        if (uiState.searchQuery != query) {
            uiState = uiState.copy(searchQuery = query, error = null)
        }
    }

    /**
     * Realiza una búsqueda de texto usando Google Places API (Text Search).
     * Los resultados se sesgan hacia `locationForBias` si se proporciona.
     */
    fun performSearch(query: String, locationForBias: LatLng? = null) {
        if (query.isBlank()) {
            uiState = uiState.copy(error = "Introduce algo para buscar", isLoading = false)
            return
        }
        Log.d(TAG, "performSearch: Query='$query', BiasLocation='$locationForBias'")

        searchJob?.cancel() // Cancela cualquier búsqueda anterior en progreso
        searchJob = viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null, mapMarkers = emptyList()) // Limpia marcadores para nueva búsqueda
            try {
                val locationStr = locationForBias?.let { "${it.latitude},${it.longitude}" }
                // Un radio grande si se usa locationForBias, ya que es solo un sesgo
                // La API Text Search no usa 'radius' de la misma forma que Nearby Search si 'rankby' no es distance.
                // Es más un área de sesgo.
                val radiusForBias = if (locationStr != null) 50000 else null // 50km

                val response = googleApiService.textSearch(
                    query = query,
                    location = locationStr,
                    radius = radiusForBias
                    // apiKey = "TU_API_KEY" // Si no usas interceptor
                )

                if (response.isSuccessful && response.body() != null) {
                    val places = response.body()!!.results
                    if (!places.isNullOrEmpty()) {
                        val newMarkers = places.mapNotNull { place ->
                            place.geometry?.location?.let { loc ->
                                MapMarkerInfo(
                                    id = place.placeId,
                                    position = LatLng(loc.lat, loc.lng),
                                    title = place.name,
                                    snippet = place.vicinity ?: place.types?.firstOrNull(),
                                    photoReference = place.photos?.firstOrNull()?.photoReference
                                )
                            }
                        }
                        uiState = uiState.copy(
                            isLoading = false,
                            mapMarkers = newMarkers,
                            // Mueve la cámara al primer resultado de la búsqueda
                            targetMapLocation = newMarkers.firstOrNull()?.position
                        )
                        Log.d(TAG, "performSearch: ${newMarkers.size} resultados encontrados.")
                    } else {
                        Log.w(TAG, "performSearch: TextSearch OK pero 'results' es null o vacío. Status: ${response.body()!!.status}")
                        uiState = uiState.copy(isLoading = false, error = "No se encontraron resultados para '$query'.")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    val apiStatus = response.body()?.status
                    val apiErrorMessage = response.body()?.errorMessage
                    val errorMsg = "Error en TextSearch: ${response.code()} - $errorBody - API Status: $apiStatus - API Msg: $apiErrorMessage"
                    Log.e(TAG, "performSearch: $errorMsg")
                    uiState = uiState.copy(isLoading = false, error = "Error en la búsqueda: ${response.code()}")
                }
            } catch (e: HttpException) {
                Log.e(TAG, "performSearch: HTTP Error", e)
                uiState = uiState.copy(isLoading = false, error = "Error del servidor (${e.code()}). Inténtalo más tarde.")
            } catch (e: IOException) {
                Log.e(TAG, "performSearch: Network Error", e)
                uiState = uiState.copy(isLoading = false, error = "Error de red al realizar la búsqueda.")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.w(TAG, "performSearch Job was cancelled.")
                    uiState = uiState.copy(isLoading = false) // Solo quita el loading
                    throw e
                }
                Log.e(TAG, "performSearch: Excepción desconocida", e)
                uiState = uiState.copy(isLoading = false, error = "Error desconocido al realizar la búsqueda.")
            }
        }
    }


    @SuppressLint("MissingPermission") // El permiso se verifica en la UI antes de llamar
    fun getCurrentUserLocation(activity: Activity) {
        if (!::fusedLocationClient.isInitialized) {
            Log.w(TAG, "getCurrentUserLocation: FusedLocationProviderClient no inicializado. Llamando a initializeLocationClient.")
            initializeLocationClient(activity.applicationContext) // Asegurar que esté inicializado
        }

        if (uiState.isLocationLoading) {
            Log.d(TAG, "getCurrentUserLocation: Solicitud de ubicación ya en progreso.")
            return
        }

        uiState = uiState.copy(isLocationLoading = true, error = null)
        Log.d(TAG, "getCurrentUserLocation: Solicitando ubicación actual...")

        viewModelScope.launch {
            try {
                // No es necesario crear el cliente aquí si ya está inicializado.
                // val client = LocationServices.getFusedLocationProviderClient(activity)

                val locationRequest = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .build()

                val location: Location? = fusedLocationClient
                    .getCurrentLocation(locationRequest, null)
                    .await()

                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    Log.d(TAG, "getCurrentUserLocation: Ubicación obtenida: $userLatLng, Precisión: ${location.accuracy}m")
                    uiState = uiState.copy(
                        isLocationLoading = false,
                        userLocation = userLatLng,
                        targetMapLocation = userLatLng // Mueve la cámara a la ubicación del usuario
                    )
                    // Cargar lugares alrededor de la ubicación del usuario con un radio por defecto
                    loadPlacesForMap(userLatLng, radius = 2000) // Radio de 2km por ejemplo
                } else {
                    Log.w(TAG, "getCurrentUserLocation: getCurrentLocation devolvió null.")
                    uiState = uiState.copy(isLocationLoading = false, error = "No se pudo obtener la ubicación actual (null).")
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "getCurrentUserLocation: Error de Seguridad", e)
                uiState = uiState.copy(isLocationLoading = false, error = "Permiso de ubicación denegado.")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.w(TAG, "getCurrentUserLocation Job was cancelled.")
                    uiState = uiState.copy(isLocationLoading = false)
                    throw e
                }
                Log.e(TAG, "getCurrentUserLocation: Error obteniendo ubicación", e)
                uiState = uiState.copy(isLocationLoading = false, error = "No se pudo obtener la ubicación (¿Servicios desactivados?).")
            }
        }
    }

    /**
     * Limpia el mensaje de error de la UI.
     */
    fun clearError() {
        if (uiState.error != null) {
            uiState = uiState.copy(error = null)
        }
    }

    /**
     * Marca que el permiso de ubicación ha sido solicitado al menos una vez.
     */
    fun setPermissionRequested() {
        if (!uiState.permissionRequestedFirstTime) {
            uiState = uiState.copy(permissionRequestedFirstTime = true)
        }
    }

    /**
     * Limpia la ubicación objetivo del mapa después de que la cámara se haya movido.
     */
    fun consumeMapTargetLocation() {
        if (uiState.targetMapLocation != null) {
            uiState = uiState.copy(targetMapLocation = null)
            Log.d(TAG, "Target map location consumed.")
        }
    }

    // Si tuvieras actualizaciones de ubicación continuas, aquí iría la función para detenerlas.
    // fun stopLocationUpdates() {
    //     if (::fusedLocationClient.isInitialized && locationCallback != null) {
    //         fusedLocationClient.removeLocationUpdates(locationCallback!!)
    //         Log.d(TAG, "Actualizaciones de ubicación detenidas.")
    //     }
    // }
}
