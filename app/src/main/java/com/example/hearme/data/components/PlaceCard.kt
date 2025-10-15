package com.example.hearme.data.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import coil.compose.AsyncImage
import com.example.hearme.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.hearme.viewmodels.MapMarkerInfo


@Composable
fun PlaceCard(
    marker: MapMarkerInfo,
    navController: NavController,
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Mostrar la imagen de Google Maps si existe una photoReference
            marker.photoReference?.let { photoRef ->
                val imageUrl =
                    "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=$photoRef&key=AIzaSyAEu4xz1YOueLEzIFfEgNC5dU6EX-HNkUM"

                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Imagen de ${marker.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    placeholder = painterResource(id = R.drawable.placeholder),
                    error = painterResource(id = R.drawable.error_image)
                )
            } ?: run {
                // Si no hay photoReference, se muestra una imagen por defecto
                Image(
                    painter = painterResource(id = R.drawable.placeholder),
                    contentDescription = "Sin imagen",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = marker.title,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                marker.snippet?.let { snippetText ->
                    Text(
                        text = snippetText,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val encodedName = Uri.encode(marker.title)
                    val encodedImage = Uri.encode(marker.imageUrl)
                    android.util.Log.d("PlaceCard", "URL enviada: $encodedImage")
                    navController.navigate("resenas/${marker.id}?name=$encodedName&localImage=$encodedImage")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = "Reseñar")
            }
        }
    }
}