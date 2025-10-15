package com.example.hearme.utils

import android.location.Location
import com.google.android.gms.maps.model.LatLng // SDK de Google Maps

fun calculateDistance(from: LatLng, to: LatLng): Float { // Ambos son del SDK de Maps
    val results = FloatArray(1)
    Location.distanceBetween(
        from.latitude,
        from.longitude,
        to.latitude,
        to.longitude,
        results
    )
    return results[0]
}