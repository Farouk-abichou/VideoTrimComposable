package com.example.videotrimcomposable

import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.videotrimcomposable.VideoTrimmer.VideoTimeline

@Composable
fun VideoEditingScreen() {
    Scaffold(
        topBar = {
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                ,
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ){
                Text(text = "VideoEditingScreen")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Video(paddingValues)
            VideoTimeline()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    VideoEditingScreen()
}

