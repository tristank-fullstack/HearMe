package com.example.hearme.data.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.rememberAsyncImagePainter
import com.example.hearme.R

@Composable
fun PlaceImage(
    photoUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    if (photoUrl != null) {
        Image(
            painter = rememberAsyncImagePainter(model = photoUrl),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        // Si no hay URL, mostramos un placeholder del drawable
        Image(
            painter = painterResource(id = R.drawable.placeholder),  // Asegúrate de tener un placeholder en res/drawable
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}