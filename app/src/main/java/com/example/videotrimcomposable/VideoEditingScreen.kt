package com.example.videotrimcomposable

import android.provider.MediaStore.Video
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun VideoEditingScreen() {
    Scaffold(
        topBar = {
            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ){
                Text(text = "VideoEditingScreen")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Video(paddingValues)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    VideoEditingScreen()
}

