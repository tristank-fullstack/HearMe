package com.example.hearme.data.network.models
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    @Json(name = "results") val results: List<GeocodingResult>?,
    @Json(name = "status") val status: String
)

@JsonClass(generateAdapter = true)
data class GeocodingResult(
    @Json(name = "geometry") val geometry: Geometry?,
    @Json(name = "formatted_address") val formattedAddress: String?,
    @Json(name = "place_id") val placeId: String? // Útil para asociar con Places API
)

@JsonClass(generateAdapter = true)
data class Geometry(
    @Json(name = "location") val location: Location?
)

@JsonClass(generateAdapter = true)
data class Location(
    @Json(name = "lat") val lat: Double?,
    @Json(name = "lng") val lng: Double?
)

