package com.example.game.gl

import android.opengl.GLSurfaceView
import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.game.GameEngine
import kotlin.math.abs

@Composable
fun SubwayGameSurface(
    engine: GameEngine,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val glView = remember {
        var downX = 0f
        var downY = 0f
        var gestureHandled = false
        val swipeThresholdPx = 36f

        object : GLSurfaceView(context) {
            override fun onTouchEvent(event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.x
                        downY = event.y
                        gestureHandled = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!gestureHandled) {
                            val dx = event.x - downX
                            val dy = event.y - downY
                            if (abs(dx) > swipeThresholdPx || abs(dy) > swipeThresholdPx) {
                                gestureHandled = true
                                if (abs(dx) > abs(dy)) {
                                    if (dx > 0f) engine.swipeRight() else engine.swipeLeft()
                                } else {
                                    if (dy < 0f) engine.swipeUp() else engine.swipeDown()
                                }
                            }
                        }
                        return true
                    }
                }
                return super.onTouchEvent(event)
            }
        }.apply {
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            preserveEGLContextOnPause = true
            setRenderer(SubwayGlRenderer(engine))
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    DisposableEffect(lifecycleOwner, glView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glView.onResume()
                Lifecycle.Event.ON_PAUSE -> {
                    engine.pauseGame()
                    glView.onPause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            glView.onPause()
        }
    }

    AndroidView(
        factory = { glView },
        modifier = modifier.fillMaxSize()
    )
}
