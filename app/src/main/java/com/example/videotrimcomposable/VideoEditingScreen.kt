package com.example.videotrimcomposable

import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.videotrimcomposable.VideoTrimmer.VideoTimeline

@Composable
fun VideoEditingScreen() {



    Scaffold(
        topBar = {
             Text(text = "VideoEditingScreen")
        }
    ) { paddingValues ->
        BoxWithConstraints (
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Video(paddingValues)
            VideoTimeline(
                images = images,
                numberOfImages = images.size,
                width = maxWidth-40.dp
            )

        }
    }
}


val images: List<Int> = listOf(
    R.drawable.background,
    R.drawable.background,
    R.drawable.background,
    R.drawable.background,
    R.drawable.background,
    R.drawable.background,
)
