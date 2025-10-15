package com.example.hearme.data.network.api
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import com.example.hearme.data.network.models.GeocodingResponse
import com.example.hearme.data.network.models.NearbySearchResponse

interface GoogleApiService {

    // Geocoding API (ya la tenías)
    @GET("maps/api/geocode/json")
    suspend fun geocode(
        @Query("address") address: String,
        @Query("language") language: String = "es",
        @Query("region") region: String = "es"
        // La API Key se añade por interceptor
    ): Response<GeocodingResponse>

    // --- ¡NUEVO! Nearby Search API ---
    @GET("maps/api/place/nearbysearch/json")
    suspend fun nearbySearch(
        @Query("location") location: String, // Formato "lat,lng"
        @Query("radius") radius: Int,        // En metros
        @Query("type") type: String,         // Ej: "restaurant|bar|night_club"
        @Query("language") language: String = "es"
        // La API Key se añade por interceptor
    ): Response<NearbySearchResponse> // Necesitaremos DTOs para esta respuesta


    @GET("maps/api/place/textsearch/json")
    suspend fun textSearch(
        @Query("query") query: String,
        @Query("type") type: String? = null, // El type es opcional aquí
        @Query("location") location: String? = null, // "lat,lng" opcional para sesgar resultados
        @Query("radius") radius: Int? = null,        // Opcional con location
        @Query("language") language: String = "es"
    ): Response<NearbySearchResponse> // Puede reutilizar el DTO de NearbySearch si la estructura es similar

}