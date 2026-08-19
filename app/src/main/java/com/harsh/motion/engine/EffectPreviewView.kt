package com.harsh.motion.engine

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.harsh.motion.data.WallpaperConfig

/**
 * In-app editor preview. Uses the exact same [EffectRenderer] as the live
 * wallpaper so what the user sees while editing is what they get once applied.
 *
 * Battery: the render loop and sensor both start in [onAttachedToWindow] and
 * stop in [onDetachedFromWindow] — nothing runs while the editor isn't visible.
 */
@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class EffectPreviewView(context: Context, private var config: WallpaperConfig) : View(context) {

    private val renderer = EffectRenderer(config)
    private val sensor = MotionSensor(context)
    private val handler = Handler(Looper.getMainLooper())
    private var lastFrameTime = 0L
    private var running = false
    private var errorText: String? = null
    private val errorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
    }

    /** Called on the main thread if the photo fails to decode, with the real reason. */
    var onLoadFailed: ((String) -> Unit)? = null

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

    private val frameRunnable = object : Runnable {
        override fun run() {
            val now = System.nanoTime()
            val dt = if (lastFrameTime == 0L) 0f else (now - lastFrameTime) / 1_000_000_000f
            lastFrameTime = now
            renderer.setTilt(sensor.tiltX, sensor.tiltY)
            renderer.update(dt.coerceIn(0f, 0.1f), sensor.tiltX, sensor.tiltY)
            invalidate()
            if (running) handler.postDelayed(this, FRAME_INTERVAL_MS)
        }
    }

    init {
        sensor.onShake = { renderer.onShake() }
        setOnTouchListener { _, event -> gestures.onTouchEvent(event); true }
    }

    fun updateConfig(newConfig: WallpaperConfig) {
        config = newConfig
        renderer.updateConfig(newConfig)
        loadPhoto()
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        running = true
        lastFrameTime = 0L
        sensor.start(withShake = true)
        handler.post(frameRunnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        running = false
        sensor.stop()
        handler.removeCallbacks(frameRunnable)
    }

    companion object {
        // ~30fps: smooth enough for a wallpaper preview while halving the work
        // (and battery draw) of a naive 60fps loop.
        private const val FRAME_INTERVAL_MS = 33L
    }
}
