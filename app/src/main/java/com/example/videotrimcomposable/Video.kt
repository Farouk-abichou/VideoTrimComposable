package com.example.videotrimcomposable

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

@Composable
fun Video(
    paddingValues: PaddingValues
){
    Image(
        painter = painterResource(id = R.drawable.background),
        contentDescription = "Just Pretend like this is a video",
        contentScale = ContentScale.Crop
    )


}