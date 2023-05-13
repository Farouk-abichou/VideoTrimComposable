package com.example.videotrimcomposable.VideoTrimmer

interface Callback {
    fun onSeek(position: Float, seekMillis: Long)
    fun onSeekStart(position: Float, seekMillis: Long)
    fun onStopSeek(position: Float, seekMillis: Long)
    fun onLeftProgress(leftPos: Float, seekMillis: Long)
    fun onRightProgress(rightPos: Float, seekMillis: Long)
}