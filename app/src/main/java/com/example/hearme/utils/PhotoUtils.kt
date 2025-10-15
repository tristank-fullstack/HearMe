package com.example.hearme.utils

fun getPhotoUrl(photoReference: String, apiKey: String, maxWidth: Int = 400): String {
    return "https://maps.googleapis.com/maps/api/place/photo?maxwidth=$maxWidth&photoreference=$photoReference&key=$apiKey"
}