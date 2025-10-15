package com.example.hearme.data.network.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NearbySearchResponse(
    @Json(name = "results") val results: List<PlaceResult>?,
    @Json(name = "status") val status: String,
    @Json(name = "next_page_token") val nextPageToken: String? = null, // Para paginación
    @Json(name = "error_message") val errorMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class PlaceResult(
    @Json(name = "place_id") val placeId: String,
    @Json(name = "name") val name: String,
    @Json(name = "geometry") val geometry: PlaceGeometry?,
    @Json(name = "vicinity") val vicinity: String?, // Dirección formateada simple
    @Json(name = "types") val types: List<String>?,
    @Json(name = "rating") val rating: Double? = null,
    @Json(name = "user_ratings_total") val userRatingsTotal: Int? = null,
    @Json(name = "photos") val photos: List<PlacePhoto>? = null
    // Puedes añadir más campos que te interesen de la respuesta
)

@JsonClass(generateAdapter = true)
data class PlaceGeometry(
    @Json(name = "location") val location: PlaceLocation?
)

@JsonClass(generateAdapter = true)
data class PlaceLocation(
    @Json(name = "lat") val lat: Double,
    @Json(name = "lng") val lng: Double
)

@JsonClass(generateAdapter = true)
data class PlacePhoto(
    @Json(name = "photo_reference") val photoReference: String,
    @Json(name = "height") val height: Int,
    @Json(name = "width") val width: Int
    // No incluye la URL directamente, se construye con photo_reference y API Key
)
data class Photo(
    val photo_reference: String,  // Aquí viene la referencia para solicitar la imagen
    val height: Int? = null,
    val width: Int? = null,
    val html_attributions: List<String>? = null
)
