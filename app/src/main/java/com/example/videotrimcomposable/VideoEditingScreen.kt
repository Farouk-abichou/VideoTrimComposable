package com.example.videotrimcomposable

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterialApi::class)
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

            var sliderValues by remember {
                mutableStateOf(1f..20f) // pass the initial values
            }

            RangeSlider(
                values = sliderValues,
                onValueChange = { sliderValues_ ->
                    sliderValues = sliderValues_
                },
                valueRange = 1f..20f,
                onValueChangeFinished = {
                    // this is called when the user completed selecting the value
                    Log.d(
                        "MainActivity",
                        "First: ${sliderValues.start}, Last: ${sliderValues.endInclusive}"
                    )
                }
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
