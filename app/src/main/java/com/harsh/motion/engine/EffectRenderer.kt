package com.harsh.motion.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.harsh.motion.data.EffectType
import com.harsh.motion.data.ParticleStyle
import com.harsh.motion.data.WallpaperConfig
import kotlin.math.min
import kotlin.random.Random

/**
 * Shared, allocation-light rendering + physics core, used by both the live
 * wallpaper engine and the in-app editor preview so behavior is identical.
 *
 * Performance/battery notes: Paint/Shader objects are created once and reused;
 * shader movement uses `setLocalMatrix` instead of recreating shaders; particle
 * count is capped; nothing here allocates per frame beyond primitive math.
 */
class EffectRenderer(private var config: WallpaperConfig) {

    private var width = 0
    private var height = 0
    // The actual canvas/surface can be wider than one screen — Android gives
    // live wallpapers extra width so they can pan smoothly between home-screen
    // pages. Scaling/positioning must use the single-screen reference size, or
    // the photo ends up scaled to the whole multi-page canvas: badly
    // over-zoomed and anchored toward one corner. Defaults to the canvas size
    // (correct for the in-app editor preview, which is never multi-page).
    private var refWidth = 0
    private var refHeight = 0
    private var bitmap: Bitmap? = null
    private var bgBitmap: Bitmap? = null
    private val scrimPaint = Paint().apply { color = Color.argb(120, 0, 0, 0) }

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rippleGradient = RadialGradient(
        0f, 0f, 1f,
        intArrayOf(Color.argb(0, 255, 255, 255), Color.argb(200, 255, 255, 255), Color.argb(0, 255, 255, 255)),
        floatArrayOf(0f, 0.85f, 1f),
        Shader.TileMode.CLAMP,
    )
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val heartPath = Path()
    private val shaderMatrix = Matrix()
    private var lightShader: RadialGradient? = null

    private var floatPhase = 0f
    private var homeOffsetX = 0f // -1..1, from launcher swipe (onOffsetsChanged)

    private data class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, var size: Float, var phase: Float)
    private data class Ripple(var x: Float, var y: Float, var radius: Float, var alpha: Int, var maxRadius: Float)

    private val particles = mutableListOf<Particle>()
    private val ripples = mutableListOf<Ripple>()

    fun updateConfig(newConfig: WallpaperConfig) {
        config = newConfig
        seedParticles()
    }

    fun setBitmap(bmp: Bitmap) {
        bitmap = bmp
        // A tiny downscaled copy, upscaled back on draw with bilinear filtering,
        // gives a cheap blur-like look for the edge-to-edge background layer —
        // built once per photo, not per frame.
        bgBitmap = runCatching {
            val w = (bmp.width / 10).coerceAtLeast(1)
            val h = (bmp.height / 10).coerceAtLeast(1)
            Bitmap.createScaledBitmap(bmp, w, h, true)
        }.getOrNull()
    }

    fun setSize(w: Int, h: Int) {
        if (w == width && h == height) return
        width = w
        height = h
        // Only fall back to the canvas size when no explicit single-screen
        // reference has been set (editor preview case).
        if (refWidth == 0 || refHeight == 0) setReferenceSize(w, h)
        seedParticles()
    }

    /** The single-screen size to scale/position against — pass the device's
     *  real display resolution here for the live wallpaper (see
     *  [com.harsh.motion.wallpaper.MotionWallpaperService]); leave unset for
     *  the editor preview, where the canvas already IS one screen. */
    fun setReferenceSize(w: Int, h: Int) {
        if (w <= 0 || h <= 0 || (w == refWidth && h == refHeight)) return
        refWidth = w
        refHeight = h
        lightShader = RadialGradient(
            0f, 0f, min(w, h) * 0.6f,
            intArrayOf(Color.argb(90, 255, 255, 255), Color.argb(0, 255, 255, 255)),
            null, Shader.TileMode.CLAMP,
        )
    }

    fun setHomeOffset(x: Float) { homeOffsetX = x }

    /** Pixels available to pan the foreground photo at its current zoom, on
     *  each axis, given the single-screen reference size — used both to
     *  render and to convert the editor's drag gesture into a
     *  resolution-independent offset fraction. */
    fun maxPan(): Pair<Float, Float> {
        val bmp = bitmap ?: return 0f to 0f
        if (refWidth == 0 || refHeight == 0) return 0f to 0f
        val fgScale = minOf(refWidth.toFloat() / bmp.width, refHeight.toFloat() / bmp.height) * config.scale
        val maxX = maxOf(0f, (bmp.width * fgScale - refWidth) / 2f)
        val maxY = maxOf(0f, (bmp.height * fgScale - refHeight) / 2f)
        return maxX to maxY
    }

    private fun seedParticles() {
        particles.clear()
        if (EffectType.PARTICLES !in config.effects || width == 0 || height == 0) return
        val count = (10 + config.intensity * 14).toInt().coerceIn(6, 24)
        repeat(count) {
            particles += Particle(
                x = Random.nextFloat() * width,
                y = Random.nextFloat() * height,
                vx = (Random.nextFloat() - 0.5f) * 6f,
                vy = -Random.nextFloat() * 10f - 4f,
                size = Random.nextFloat() * 10f + 6f,
                phase = Random.nextFloat() * 6.28f,
            )
        }
    }

    /** Advance physics by [dt] seconds. Cheap — safe to call every frame. */
    fun update(dt: Float, tiltX: Float, tiltY: Float) {
        floatPhase += dt
        if (EffectType.PARTICLES in config.effects) {
            for (p in particles) {
                p.y += p.vy * dt
                p.x += p.vx * dt + kotlin.math.sin(floatPhase + p.phase) * 4f * dt
                if (p.y < -20f) {
                    p.y = height + 20f
                    p.x = Random.nextFloat() * width
                }
                if (p.x < -20f) p.x = width + 20f
                if (p.x > width + 20f) p.x = -20f
            }
        }
        if (ripples.isNotEmpty()) {
            val it = ripples.iterator()
            while (it.hasNext()) {
                val r = it.next()
                r.radius += dt * r.maxRadius * 1.6f
                r.alpha = (r.alpha - dt * 340f).toInt().coerceAtLeast(0)
                if (r.alpha <= 0 || r.radius > r.maxRadius) it.remove()
            }
        }
    }

    fun onTouchDown(x: Float, y: Float) = addRipple(x, y, 1f)
    fun onDoubleTap(x: Float, y: Float) {
        addRipple(x, y, 1.6f)
        if (EffectType.PARTICLES in config.effects) burst(x, y, 8)
    }
    fun onShake() {
        if (EffectType.PARTICLES in config.effects) burst(width / 2f, height / 2f, 14)
    }

    private fun addRipple(x: Float, y: Float, scale: Float) {
        if (EffectType.TOUCH_REACTIVE !in config.effects) return
        ripples += Ripple(x, y, 0f, 200, min(width, height) * 0.5f * scale)
        if (ripples.size > 6) ripples.removeAt(0)
    }

    private fun burst(x: Float, y: Float, count: Int) {
        repeat(count) {
            val angle = Random.nextFloat() * 6.28f
            particles += Particle(
                x = x, y = y,
                vx = kotlin.math.cos(angle) * 40f,
                vy = kotlin.math.sin(angle) * 40f - 20f,
                size = Random.nextFloat() * 8f + 6f,
                phase = Random.nextFloat() * 6.28f,
            )
        }
        while (particles.size > 40) particles.removeAt(0)
    }

    fun draw(canvas: Canvas) {
        val bmp = bitmap ?: run { canvas.drawColor(Color.BLACK); return }
        val intensity = config.intensity

        var dx = 0f
        var dy = 0f
        var breathe = 1f
        if (EffectType.TILT_PARALLAX in config.effects) {
            dx += lastTiltX * 70f * intensity
            dy += lastTiltY * 50f * intensity
        }
        dx += homeOffsetX * 60f * intensity
        if (EffectType.FLOATING in config.effects) {
            dx += kotlin.math.sin(floatPhase * 0.6f) * 20f * intensity
            dy += kotlin.math.cos(floatPhase * 0.5f) * 16f * intensity
            breathe += kotlin.math.sin(floatPhase * 0.5f) * 0.035f * intensity
        }

        canvas.drawColor(Color.BLACK)

        // Background layer: a softly blurred copy that COVERS the whole screen
        // (cropped, like any wallpaper background) so there are never black
        // bars — it moves at a slower rate than the foreground for a real
        // sense of depth (this is what makes "3D Parallax"/"Depth Layers" real).
        bgBitmap?.let { bg ->
            canvas.save()
            val bgCoverScale = maxOf(width.toFloat() / bg.width, height.toFloat() / bg.height) * 1.15f
            canvas.translate(width / 2f + dx * 0.4f, height / 2f + dy * 0.4f)
            canvas.scale(bgCoverScale, bgCoverScale)
            canvas.translate(-bg.width / 2f, -bg.height / 2f)
            canvas.drawBitmap(bg, 0f, 0f, bitmapPaint)
            canvas.restore()
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
        }

        // Foreground layer: the photo at the user's chosen crop/zoom (defaults
        // to showing the WHOLE photo — see [WallpaperConfig.scale]).
        val (maxPanX, maxPanY) = maxPan()
        dx += config.offsetX.coerceIn(-1f, 1f) * maxPanX
        dy += config.offsetY.coerceIn(-1f, 1f) * maxPanY

        canvas.save()
        val fgScale = minOf(refWidth.toFloat() / bmp.width, refHeight.toFloat() / bmp.height) * config.scale * breathe
        canvas.translate(width / 2f + dx, height / 2f + dy)
        canvas.scale(fgScale, fgScale)
        canvas.translate(-bmp.width / 2f, -bmp.height / 2f)
        canvas.drawBitmap(bmp, 0f, 0f, bitmapPaint)
        canvas.restore()

        if (EffectType.DYNAMIC_LIGHT in config.effects) {
            lightShader?.let { shader ->
                // A slow autonomous drift on top of the tilt response keeps the
                // glow gently alive even when the phone is sitting still.
                val driftX = kotlin.math.sin(floatPhase * 0.15f) * refWidth * 0.12f
                val driftY = kotlin.math.cos(floatPhase * 0.12f) * refHeight * 0.12f
                shaderMatrix.reset()
                shaderMatrix.postTranslate(
                    width / 2f + lastTiltX * refWidth * 0.4f + driftX,
                    height / 2f + lastTiltY * refHeight * 0.4f + driftY,
                )
                shader.setLocalMatrix(shaderMatrix)
                lightPaint.shader = shader
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), lightPaint)
            }
        }

        if (EffectType.PARTICLES in config.effects) {
            for (p in particles) drawParticle(canvas, p)
        }

        if (ripples.isNotEmpty()) {
            for (r in ripples) {
                canvas.save()
                canvas.translate(r.x, r.y)
                canvas.scale(r.radius, r.radius)
                ripplePaint.shader = rippleGradient
                ripplePaint.alpha = r.alpha
                canvas.drawCircle(0f, 0f, 1f, ripplePaint)
                canvas.restore()
            }
        }
    }

    private fun drawParticle(canvas: Canvas, p: Particle) {
        when (config.particleStyle) {
            ParticleStyle.SPARKLE -> {
                // A per-particle twinkle (alpha shimmer) instead of a flat dot.
                val twinkle = (kotlin.math.sin(floatPhase * 3f + p.phase) * 0.5f + 0.5f)
                particlePaint.color = Color.argb((120 + twinkle * 135f).toInt(), 255, 255, 255)
                canvas.drawCircle(p.x, p.y, p.size / 3f * (0.7f + twinkle * 0.4f), particlePaint)
            }
            ParticleStyle.BOKEH -> {
                particlePaint.color = Color.argb(120, 255, 220, 180)
                canvas.drawCircle(p.x, p.y, p.size, particlePaint)
            }
            ParticleStyle.HEART -> {
                particlePaint.color = Color.argb(220, 247, 37, 133)
                drawHeart(canvas, p.x, p.y, p.size)
            }
        }
    }

    private fun drawHeart(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        heartPath.reset()
        val s = size / 10f
        heartPath.moveTo(cx, cy + 3 * s)
        heartPath.cubicTo(cx - 6 * s, cy - 3 * s, cx - 2 * s, cy - 7 * s, cx, cy - 3 * s)
        heartPath.cubicTo(cx + 2 * s, cy - 7 * s, cx + 6 * s, cy - 3 * s, cx, cy + 3 * s)
        heartPath.close()
        canvas.drawPath(heartPath, particlePaint)
    }

    // Tilt is read from MotionSensor by the caller each frame via these setters,
    // avoiding a hard dependency on the sensor class from the renderer.
    private var lastTiltX = 0f
    private var lastTiltY = 0f
    fun setTilt(x: Float, y: Float) { lastTiltX = x; lastTiltY = y }
}
