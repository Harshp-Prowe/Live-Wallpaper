package com.harsh.motion.engine

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.view.Choreographer
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.harsh.motion.data.WallpaperConfig
import com.harsh.motion.data.needsShakeSensor
import com.harsh.motion.data.needsTiltSensor
import kotlin.math.floor

/**
 * In-app editor preview. Uses the exact same [EffectRenderer] as the live
 * wallpaper so what the user sees while editing is what they get once applied.
 *
 * Battery: the render loop and sensors run only while this view is actually
 * visible on screen (see [onVisibilityAggregated]), and only the sensors the
 * chosen effects read are powered at all.
 */
@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class EffectPreviewView(context: Context, private var config: WallpaperConfig) : View(context) {

    private val renderer = EffectRenderer(config)
    private val sensor = MotionSensor(context)
    private val choreographer = Choreographer.getInstance()
    private var frameSkip = 1
    private var vsyncTicks = 0
    private var lastFrameTime = 0L
    private var running = false
    private var errorText: String? = null
    private val errorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
    }

    /** Called on the main thread if the photo fails to decode, with the real reason. */
    var onLoadFailed: ((String) -> Unit)? = null

    // Pinch-zoom / drag-to-reposition live in the dedicated PhotoAdjustScreen
    // instead of here, so this animated preview's touch is unambiguous: it
    // always previews the chosen effects (tap ripple, double-tap burst) — the
    // exact same touch behavior the real live wallpaper will have.
    private val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            renderer.onTouchDown(e.x, e.y)
            return true
        }
        override fun onDoubleTap(e: MotionEvent): Boolean {
            renderer.onDoubleTap(e.x, e.y)
            return true
        }
    })

    // Vsync-aligned, matching the live wallpaper exactly, so the editor preview
    // is a faithful representation of the real thing rather than a slightly
    // different-feeling approximation.
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            choreographer.postFrameCallback(this)
            // Same vsync-skip cap as the live wallpaper, so the preview is an
            // honest representation of it rather than running smoother than the
            // real thing ever will. See MotionWallpaperService for why this
            // counts vsyncs instead of comparing elapsed time.
            if (++vsyncTicks < frameSkip) return
            vsyncTicks = 0
            val dt = if (lastFrameTime == 0L) 0f else (frameTimeNanos - lastFrameTime) / 1_000_000_000f
            lastFrameTime = frameTimeNanos
            renderer.setTilt(sensor.tiltX, sensor.tiltY)
            renderer.update(dt.coerceIn(0f, 0.1f))
            invalidate()
        }
    }

    init {
        sensor.onShake = { renderer.onShake() }
        setOnTouchListener { _, event ->
            gestures.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> renderer.onTouchMove(event.x, event.y)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> renderer.onTouchUp()
            }
            true
        }
    }

    fun updateConfig(newConfig: WallpaperConfig) {
        val photoChanged = newConfig.photoUri != config.photoUri
        val sensorsChanged = newConfig.effects.needsTiltSensor != config.effects.needsTiltSensor ||
            newConfig.effects.needsShakeSensor != config.effects.needsShakeSensor
        config = newConfig
        if (sensorsChanged && running) {
            sensor.stop()
            startSensorsForConfig()
        }
        renderer.updateConfig(newConfig)
        // Re-decoding from disk on every recomposition (e.g. during a live
        // pinch/drag gesture, which recomposes many times per second) would be
        // wasteful and slow — only reload when the photo itself changed.
        if (photoChanged) loadPhoto()
    }

    private fun loadPhoto() {
        if (width == 0 || height == 0 || config.photoUri.isBlank()) return
        errorText = null
        runCatching {
            BitmapLoader.decodeScaled(context, Uri.parse(config.photoUri), maxOf(width, height))
        }.onSuccess { renderer.setBitmap(it) }
            .onFailure { e ->
                val msg = "Couldn't load photo (uri scheme=${Uri.parse(config.photoUri).scheme}): " +
                    "${e.javaClass.simpleName}: ${e.message}"
                errorText = msg
                onLoadFailed?.invoke(msg)
            }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        renderer.setSize(w, h)
        loadPhoto()
    }

    override fun onDraw(canvas: Canvas) {
        renderer.draw(canvas)
        errorText?.let { msg ->
            canvas.drawText(msg, 24f, height / 2f, errorPaint)
        }
    }

    /**
     * Gated on *aggregate* visibility, not attachment. A View stays attached
     * while its Activity is merely stopped, so keying off onDetachedFromWindow
     * left the render loop posted and two fused motion sensors registered the
     * whole time the app sat in the background.
     */
    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible) resumeRendering() else pauseRendering()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isShown) resumeRendering()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pauseRendering()
    }

    private fun resumeRendering() {
        if (running) return
        running = true
        lastFrameTime = 0L
        val hz = display?.refreshRate ?: 60f
        frameSkip = if (hz > 0f) maxOf(1, floor(hz / 60f).toInt()) else 1
        startSensorsForConfig()
        choreographer.postFrameCallback(frameCallback)
    }

    private fun pauseRendering() {
        if (!running) return
        running = false
        sensor.stop()
        choreographer.removeFrameCallback(frameCallback)
    }

    /** Only the sensors the chosen effects actually read — see [MotionSensor.start]. */
    private fun startSensorsForConfig() {
        sensor.start(
            withTilt = config.effects.needsTiltSensor,
            withShake = config.effects.needsShakeSensor,
        )
    }

}
