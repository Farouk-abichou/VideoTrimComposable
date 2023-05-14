package com.example.videotrimcomposable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.videotrimcomposable.ui.theme.VideoTrimComposableTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VideoTrimComposableTheme {
                VideoEditingScreen()
            }
            }
        }
}

