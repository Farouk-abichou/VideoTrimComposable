package com.example.videotrimcomposable.VideoTrimmer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun VideoTimeline(
    images: List<Int>,
    numberOfImages : Int
){
    val width =300.dp

    Card (
        modifier = Modifier
            .width(width)
            .height(60.dp),
        shape = RoundedCornerShape(30),
        border = BorderStroke(
            2.dp,
            color = Color.LightGray
        )
    ){
        LazyRow {
            items(count = numberOfImages){item ->
                Image(
                    painterResource(images[item]),
                    contentDescription = item.toString(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .height(60.dp)
                        .width(width/numberOfImages)
                )
            }
        }
    }
}