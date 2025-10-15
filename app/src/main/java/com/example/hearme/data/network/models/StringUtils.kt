package com.example.hearme.data.network.models
import android.net.Uri
import java.net.URLDecoder

fun getPathFromFirebaseStorageUrl(url: String): String? {
    try {
        val uri = Uri.parse(url)
        if ("firebasestorage.googleapis.com" == uri.host) {
            // La ruta después de /o/ es lo que queremos, decodificada
            // Ejemplo: /v0/b/BUCKET/o/audio%2Ffile.wav -> audio/file.wav
            val path = uri.path ?: return null
            val objectSegmentMarker = "/o/"
            val objectSegmentStartIndex = path.indexOf(objectSegmentMarker)

            if (objectSegmentStartIndex != -1) {
                var encodedPath = path.substring(objectSegmentStartIndex + objectSegmentMarker.length)
                // La ruta puede tener parámetros como ?alt=media&token=...
                val queryParamStartIndex = encodedPath.indexOf('?')
                if (queryParamStartIndex != -1) {
                    encodedPath = encodedPath.substring(0, queryParamStartIndex)
                }
                return URLDecoder.decode(encodedPath, "UTF-8")
            }
        }
    } catch (e: Exception) {
        // Log.e("Utils", "Error parsing Firebase Storage URL: $url", e)
        return null // O devuelve la URL original si prefieres un fallback que siga fallando igual
    }
    return null // O la URL original
}