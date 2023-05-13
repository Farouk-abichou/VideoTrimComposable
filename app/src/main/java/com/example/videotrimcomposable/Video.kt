package com.example.videotrimcomposable

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource

@Composable
fun Video(
    paddingValues: PaddingValues
){

    Image(
        painter = painterResource(id = R.drawable.ic_launcher_background),
        contentDescription = "Just Pretend like this is a video"
    )
}