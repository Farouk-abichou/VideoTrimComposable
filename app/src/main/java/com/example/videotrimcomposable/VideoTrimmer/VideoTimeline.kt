package com.example.videotrimcomposable.VideoTrimmer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.videotrimcomposable.R

@Composable
fun VideoTimeline(){
    val width =300.dp
    val numberOfImages =10
    Card (
        modifier = Modifier
            .width(width)
            .height(60.dp)
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp)
    ){
        LazyRow {
            items(count = numberOfImages){item ->
                Image(
                    painterResource(R.drawable.background),
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