package com.harsh.motion.wallpaper

import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.harsh.motion.data.WallpaperConfig
import com.harsh.motion.data.WallpaperRepository
import com.harsh.motion.engine.BitmapLoader
import com.harsh.motion.engine.EffectRenderer
import com.harsh.motion.engine.MotionSensor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        // Driven by Choreographer rather than a fixed 33ms Handler post: the old
        // loop drifted against the display's refresh, so frames were repeatedly
        // shown twice or skipped and even a perfectly cheap effect looked
        // stuttery. This lands one frame per vsync instead.
        private val choreographer = Choreographer.getInstance()
        private val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!visible) return
                val dt = if (lastFrameTime == 0L) 0f else (frameTimeNanos - lastFrameTime) / 1_000_000_000f
                lastFrameTime = frameTimeNanos
                val r = renderer
                if (r != null) {
                    r.setTilt(sensor.tiltX, sensor.tiltY)
                    r.update(dt.coerceIn(0f, 0.1f))
                    drawFrame(r)
                }
                choreographer.postFrameCallback(this)
            }
        }

        private var activeConfig: WallpaperConfig? = null
        private val engineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
        private var surfaceWidth = 0
        private var surfaceHeight = 0
        private var loadedPhotoUri: String? = null
        private var loadJob: Job? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
            sensor.onShake = { renderer?.onShake() }

            // Observed, not read once: while this wallpaper is already applied,
            // saving an edit in the app must take effect immediately. Reading
            // the active config only in onCreate meant the engine kept serving
            // the stale one until the app's data was cleared or reinstalled.
            val repo = WallpaperRepository(this@MotionWallpaperService)
            engineScope.launch {
                combine(repo.activeConfigId, repo.configs) { id, all -> all.firstOrNull { it.id == id } }
                    .distinctUntilChanged()
                    .collect { applyConfig(it) }
            }
        }

        private fun applyConfig(config: WallpaperConfig?) {
            activeConfig = config
            if (config == null) {
                renderer?.release()
                renderer = null
                loadedPhotoUri = null
                return
            }
            val existing = renderer
            if (existing == null) renderer = EffectRenderer(config) else existing.updateConfig(config)
            applySizeAndPhoto()
        }

        private fun applySizeAndPhoto(forceReload: Boolean = false) {
            val r = renderer ?: return
            if (surfaceWidth == 0 || surfaceHeight == 0) return

            // The wallpaper surface is often wider than one screen (Android
            // gives room to pan between home-screen pages). Scale/position
            // against the real single-screen resolution, not the raw surface
            // size, or the photo ends up badly over-zoomed and mis-anchored.
            val dm = resources.displayMetrics
            r.setReferenceSize(dm.widthPixels, dm.heightPixels)
            r.setSize(surfaceWidth, surfaceHeight)

            val uriString = activeConfig?.photoUri ?: return
            if (!forceReload && uriString == loadedPhotoUri) return

            loadJob?.cancel()
            loadJob = engineScope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    runCatching {
                        BitmapLoader.decodeScaled(
                            this@MotionWallpaperService, Uri.parse(uriString),
                            maxOf(dm.widthPixels, dm.heightPixels),
                        )
                    }.getOrNull()
                } ?: return@launch
                if (renderer !== r) return@launch
                loadedPhotoUri = uriString
                r.setBitmap(bitmap)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            applySizeAndPhoto(forceReload = true)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                lastFrameTime = 0L
                sensor.start(withShake = true)
                choreographer.postFrameCallback(frameCallback)
            } else {
                sensor.stop()
                choreographer.removeFrameCallback(frameCallback)
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
            // GestureDetector alone only reports discrete taps, so drag and
            // press-and-hold need the raw stream.
            val r = renderer ?: return
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> r.onTouchMove(event.x, event.y)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> r.onTouchUp()
            }
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
            surfaceWidth = 0
            surfaceHeight = 0
            sensor.stop()
            choreographer.removeFrameCallback(frameCallback)
        }

        override fun onDestroy() {
            super.onDestroy()
            visible = false
            sensor.stop()
            choreographer.removeFrameCallback(frameCallback)
            engineScope.cancel()
            // Replaced by another wallpaper (a gallery photo, say) — drop the
            // decoded photo instead of leaving it held in this process.
            renderer?.release()
            renderer = null
            loadedPhotoUri = null
        }
    }
}
