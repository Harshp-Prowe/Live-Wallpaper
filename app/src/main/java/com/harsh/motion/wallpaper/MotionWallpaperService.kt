package com.harsh.motion.wallpaper

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.harsh.motion.data.WallpaperConfig
import com.harsh.motion.data.WallpaperRepository
import com.harsh.motion.engine.BitmapLoader
import com.harsh.motion.engine.EffectRenderer
import com.harsh.motion.engine.MotionSensor
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

/**
 * The real live wallpaper. Renders the user's chosen photo with the selected
 * motion effects using the shared [EffectRenderer] — the same engine the
 * in-app editor previews, so what you design is what you get.
 *
 * Battery discipline: the sensor and the draw loop run ONLY while
 * [Engine.onVisibilityChanged] reports visible (i.e. the wallpaper is actually
 * on screen, not just installed). Both are torn down immediately otherwise.
 */
class MotionWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = MotionEngine()

    private inner class MotionEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private val sensor = MotionSensor(this@MotionWallpaperService)
        private var renderer: EffectRenderer? = null
        private var lastFrameTime = 0L
        private var visible = false

        private val gestures = GestureDetector(
            this@MotionWallpaperService,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean {
                    renderer?.onTouchDown(e.x, e.y)
                    return true
                }
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    renderer?.onDoubleTap(e.x, e.y)
                    return true
                }
            },
        )

        private val frameRunnable = object : Runnable {
            override fun run() {
                if (!visible) return
                val now = System.nanoTime()
                val dt = if (lastFrameTime == 0L) 0f else (now - lastFrameTime) / 1_000_000_000f
                lastFrameTime = now
                val r = renderer
                if (r != null) {
                    r.setTilt(sensor.tiltX, sensor.tiltY)
                    r.update(dt.coerceIn(0f, 0.1f), sensor.tiltX, sensor.tiltY)
                    drawFrame(r)
                }
                handler.postDelayed(this, FRAME_INTERVAL_MS)
            }
        }

        private var activeConfig: WallpaperConfig? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
            val config = loadActiveConfig()
            activeConfig = config
            if (config != null) {
                renderer = EffectRenderer(config)
                sensor.onShake = { renderer?.onShake() }
            }
        }

        private fun loadActiveConfig(): WallpaperConfig? = runBlocking {
            val repo = WallpaperRepository(this@MotionWallpaperService)
            val id = repo.activeConfigId.firstOrNull() ?: return@runBlocking null
            repo.configs.firstOrNull()?.firstOrNull { it.id == id }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            val r = renderer ?: return
            val uriString = activeConfig?.photoUri ?: return
            r.setSize(width, height)
            BitmapLoader.decodeScaled(this@MotionWallpaperService, Uri.parse(uriString), maxOf(width, height))
                ?.let { r.setBitmap(it) }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                lastFrameTime = 0L
                sensor.start(withShake = true)
                handler.post(frameRunnable)
            } else {
                sensor.stop()
                handler.removeCallbacks(frameRunnable)
            }
        }

        override fun onOffsetsChanged(
            xOffset: Float, yOffset: Float,
            xOffsetStep: Float, yOffsetStep: Float,
            xPixelOffset: Int, yPixelOffset: Int,
        ) {
            renderer?.setHomeOffset((xOffset - 0.5f) * 2f)
        }

        override fun onTouchEvent(event: MotionEvent) {
            gestures.onTouchEvent(event)
        }

        private fun drawFrame(r: EffectRenderer) {
            val holder = surfaceHolder
            var canvas: android.graphics.Canvas? = null
            try {
                canvas = holder.lockCanvas()
                canvas?.let { r.draw(it) }
            } finally {
                canvas?.let { runCatching { holder.unlockCanvasAndPost(it) } }
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
            sensor.stop()
            handler.removeCallbacks(frameRunnable)
        }

        companion object {
            private const val FRAME_INTERVAL_MS = 33L
        }
    }
}
