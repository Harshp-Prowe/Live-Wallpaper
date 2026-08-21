package com.harsh.motion.wallpaper

import android.content.Context
import android.hardware.display.DisplayManager
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.Display
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Surface
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
import kotlin.math.floor

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
        private var hardwareCanvasUnavailable = false
        // Draw on every Nth vsync. Deriving this from the panel's own rate keeps
        // the result an exact divisor of it; an elapsed-time cutoff instead
        // beats against vsync, so a 16.67ms "60fps" threshold on a 144Hz panel
        // actually lands on every 3rd frame = 48fps, worse than it was aiming
        // for. floor() keeps every common panel at or above 60: 60->60, 90->90,
        // 120->60, 144->72.
        private var frameSkip = 1
        private var vsyncTicks = 0
        private val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!visible) return
                // Re-post first so a skipped vsync still keeps the loop alive.
                choreographer.postFrameCallback(this)

                // Capped, because redrawing a wallpaper at the full 144Hz is
                // heat and battery for motion nobody perceives. Every animation
                // here advances by elapsed dt, so dropping the sampling rate
                // changes how often motion is drawn, never its speed or shape.
                if (++vsyncTicks < frameSkip) return
                vsyncTicks = 0

                val dt = if (lastFrameTime == 0L) 0f else (frameTimeNanos - lastFrameTime) / 1_000_000_000f
                lastFrameTime = frameTimeNanos
                val r = renderer
                if (r != null) {
                    r.setTilt(sensor.tiltX, sensor.tiltY)
                    r.update(dt.coerceIn(0f, 0.1f))
                    drawFrame(r)
                }
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
            frameSkip = frameSkipForDisplay()

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

        private fun frameSkipForDisplay(): Int {
            val hz = runCatching {
                val dm = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                dm.getDisplay(Display.DEFAULT_DISPLAY).refreshRate
            }.getOrDefault(60f)
            if (hz <= 0f) return 1
            return maxOf(1, floor(hz / 60f).toInt())
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

        /**
         * Renders on the GPU wherever possible.
         *
         * [SurfaceHolder.lockCanvas] hands back a *software* canvas, so every
         * full-screen shader pass — aurora, glow, sheen, vignette — was being
         * composited by the CPU on the main thread. The editor preview is an
         * ordinary View, so its onDraw is hardware-accelerated and the identical
         * effects cost a fraction as much there; that mismatch is why the
         * wallpaper felt heavy while the editor felt fine on the same phone.
         * [Surface.lockHardwareCanvas] puts both on the same GPU path.
         */
        private fun drawFrame(r: EffectRenderer) {
            val surface = surfaceHolder.surface
            if (!surface.isValid) return
            if (!hardwareCanvasUnavailable && drawHardware(surface, r)) return
            drawSoftware(r)
        }

        private fun drawHardware(surface: Surface, r: EffectRenderer): Boolean {
            var canvas: android.graphics.Canvas? = null
            return try {
                canvas = surface.lockHardwareCanvas()
                if (canvas == null) false else { r.draw(canvas); true }
            } catch (t: Throwable) {
                // A few OEM builds refuse a hardware canvas on the wallpaper
                // surface. Fall back permanently rather than retrying per frame.
                hardwareCanvasUnavailable = true
                false
            } finally {
                canvas?.let { runCatching { surface.unlockCanvasAndPost(it) } }
            }
        }

        private fun drawSoftware(r: EffectRenderer) {
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
