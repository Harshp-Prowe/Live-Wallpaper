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
    private var bitmap: Bitmap? = null

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.WHITE
    }
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
    }

    fun setSize(w: Int, h: Int) {
        if (w == width && h == height) return
        width = w
        height = h
        seedParticles()
        lightShader = if (w > 0 && h > 0) {
            RadialGradient(
                0f, 0f, min(w, h) * 0.6f,
                intArrayOf(Color.argb(90, 255, 255, 255), Color.argb(0, 255, 255, 255)),
                null, Shader.TileMode.CLAMP,
            )
        } else null
    }

    fun setHomeOffset(x: Float) { homeOffsetX = x }

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
        if (EffectType.TILT_PARALLAX in config.effects) {
            dx += lastTiltX * 26f * intensity
            dy += lastTiltY * 18f * intensity
        }
        dx += homeOffsetX * 40f * intensity
        if (EffectType.FLOATING in config.effects) {
            dx += kotlin.math.sin(floatPhase * 0.6f) * 6f * intensity
            dy += kotlin.math.cos(floatPhase * 0.5f) * 5f * intensity
        }

        canvas.save()
        // Slight overscale so tilt/float never reveal an edge.
        val scale = 1.08f
        canvas.translate(width / 2f + dx, height / 2f + dy)
        canvas.scale(scale, scale)
        canvas.translate(-width / 2f, -height / 2f)
        canvas.drawBitmap(bmp, 0f, 0f, bitmapPaint)
        canvas.restore()

        if (EffectType.DYNAMIC_LIGHT in config.effects) {
            lightShader?.let { shader ->
                shaderMatrix.reset()
                shaderMatrix.postTranslate(
                    width / 2f + lastTiltX * width * 0.4f,
                    height / 2f + lastTiltY * height * 0.4f,
                )
                shader.setLocalMatrix(shaderMatrix)
                lightPaint.shader = shader
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), lightPaint)
            }
        }

        if (EffectType.PARTICLES in config.effects) {
            for (p in particles) drawParticle(canvas, p.x, p.y, p.size)
        }

        if (ripples.isNotEmpty()) {
            for (r in ripples) {
                ripplePaint.alpha = r.alpha
                canvas.drawCircle(r.x, r.y, r.radius, ripplePaint)
            }
        }
    }

    private fun drawParticle(canvas: Canvas, x: Float, y: Float, size: Float) {
        when (config.particleStyle) {
            ParticleStyle.SPARKLE -> {
                particlePaint.color = Color.argb(210, 255, 255, 255)
                canvas.drawCircle(x, y, size / 3f, particlePaint)
            }
            ParticleStyle.BOKEH -> {
                particlePaint.color = Color.argb(120, 255, 220, 180)
                canvas.drawCircle(x, y, size, particlePaint)
            }
            ParticleStyle.HEART -> {
                particlePaint.color = Color.argb(220, 247, 37, 133)
                drawHeart(canvas, x, y, size)
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
