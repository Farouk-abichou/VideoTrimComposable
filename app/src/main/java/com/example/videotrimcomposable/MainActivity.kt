package com.example.videotrimcomposable

import android.os.Bundle
import android.os.Handler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import com.example.videotrimcomposable.ui.theme.VideoTrimComposableTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context= LocalContext.current
            VideoTrimComposableTheme {

                val trimView = TrimView(context)
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colors.background
//                ) {
//                    VideoEditingScreen()
//                }

                trimView.max = 100
                trimView.trim = 50
                trimView.minTrim = 20
                trimView.maxTrim = 80

                trimView.onTrimChangeListener = object : TrimView.TrimChangeListener() {
                    override fun onLeftEdgeChanged(trimStart: Int, trim: Int) {
                        trimView.progress = 0
                    }

                    override fun onRightEdgeChanged(trimStart: Int, trim: Int) {
                        trimView.progress = 0
                    }

                    override fun onRangeChanged(trimStart: Int, trim: Int) {
                        trimView.progress = 0
                    }
                }
                val handler = Handler()
                fun timer() {
                    trimView.progress++
                    handler.postDelayed({
                        timer()
                    }, 500)
                }
                timer()
            }

            }
        }
}

