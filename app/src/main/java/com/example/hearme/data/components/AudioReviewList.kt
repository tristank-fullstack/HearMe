package com.example.hearme.data.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import com.example.hearme.data.network.models.AudioReview

@Composable
fun AudioReviewList(
    audioReviews: List<AudioReview>
) {
    LazyColumn {
        items(audioReviews) { review ->
            AudioReviewItem(audioReview = review)
        }
    }
}