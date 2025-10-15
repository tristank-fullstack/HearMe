package com.example.hearme.data.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding

@Composable
fun MyReviewItem(
    audioUrl: String // acá podrías pasar otros campos si los necesitaras
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Fila superior: "TU" a la izquierda y "ver" a la derecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "TU",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ver",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        // Acción pendiente: aquí podrías navegar a un detalle o lo que necesites
                    }
                )
            }
            // Aquí puedes colocar los controles de audio o un simple placeholder con la URL
            Text(
                text = "Audio: $audioUrl",
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}