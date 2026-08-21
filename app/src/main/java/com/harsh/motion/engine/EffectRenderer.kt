package com.harsh.motion.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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
    private val rippleRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    // A cool, glassy cyan-white wash (instead of flat white) so the ripple
    // reads as premium "liquid glass" rather than a generic touch-feedback
    // circle; a thin bright rim (drawn separately, see rippleRimPaint) gives
    // it a crisp edge instead of an all-over soft blob.
    private val rippleGradient = RadialGradient(
        0f, 0f, 1f,
        intArrayOf(
            Color.argb(0, 210, 240, 255),
            Color.argb(70, 200, 235, 255),
            Color.argb(150, 225, 245, 255),
            Color.argb(0, 210, 240, 255),
        ),
        floatArrayOf(0f, 0.7f, 0.9f, 1f),
        Shader.TileMode.CLAMP,
    )
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    // Built once in normalised -1..1 space, then positioned per particle by
    // canvas transform. See [buildUnitHeart].
    private val heartPath = Path()
    private var heartPathBuilt = false
    private val shaderMatrix = Matrix()
    private var lightShader: RadialGradient? = null

    // Slow diagonal "glass sheen" sweep — the same trick premium app icons
    // and hero banners use for a moving specular highlight — layered on top
    // of the warm radial glow so Dynamic Light reads as a polished reflection
    // instead of a static color wash.
    private val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sheenGradient = LinearGradient(
        -1f, 0f, 1f, 0f,
        intArrayOf(Color.argb(0, 255, 255, 255), Color.argb(90, 255, 255, 255), Color.argb(0, 255, 255, 255)),
        floatArrayOf(0f, 0.5f, 1f),
        Shader.TileMode.CLAMP,
    )
    private val sheenMatrix = Matrix()

    // A subtle cinematic vignette (dark, soft-edged corners) — cheap to add,
    // and it's the single biggest "does this look professional" cue: it
    // frames the photo instead of letting effects float over a flat rectangle.
    private var vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var vignetteShader: RadialGradient? = null

    // Aurora: a few big soft colour lights that drift independently. Built once
    // per reference size; SCREEN-blended so they read as light falling on the
    // photo rather than paint sitting on top of it.
    private val auroraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
    }
    private var auroraShaders: List<RadialGradient> = emptyList()
    private val auroraMatrix = Matrix()
    // Kept so the draw can bound each light to its own extent (see draw()).
    private var auroraRadius = 0f
    private var lightRadius = 0f

    // Liquid wave: a reusable vertex grid for drawBitmapMesh. Allocated once per
    // bitmap and refilled in place each frame, so the effect costs no
    // per-frame allocation.
    private var meshVerts: FloatArray? = null
    private var meshBitmapWidth = 0
    private var meshBitmapHeight = 0

    private var floatPhase = 0f
    private var homeOffsetX = 0f // -1..1, from launcher swipe (onOffsetsChanged)

    // Touch interaction. The aliases promised Drag Interaction, Swipe Motion,
    // Press & Hold and Zoom on Touch, but only the ripple was ever implemented.
    // These drive both: the photo eases toward wherever the finger is (drag /
    // swipe) and eases into a zoom while it stays held (press & hold).
    private var touching = false
    private var touchX = 0f
    private var touchY = 0f
    private var touchPullX = 0f // current eased pull, in -1..1 of the screen
    private var touchPullY = 0f
    private var holdZoom = 0f // 0 = released, 1 = fully held

    private data class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, var size: Float, var phase: Float)
    private data class Ripple(var x: Float, var y: Float, var radius: Float, var alpha: Int, var maxRadius: Float)

    private val particles = mutableListOf<Particle>()
    private val ripples = mutableListOf<Ripple>()

    fun updateConfig(newConfig: WallpaperConfig) {
        val previous = config
        config = newConfig
        // Only the effect set and intensity change the particle field. Reseeding
        // on every call restarted the animation on unrelated edits (a slider
        // drag recomposes many times a second), which read as stutter.
        if (previous.effects != newConfig.effects || previous.intensity != newConfig.intensity) {
            seedParticles()
        }
    }

    fun setBitmap(bmp: Bitmap) {
        val previousFg = bitmap
        val previousBg = bgBitmap
        bitmap = bmp
        // A tiny downscaled copy, upscaled back on draw with bilinear filtering,
        // gives a cheap blur-like look for the edge-to-edge background layer —
        // built once per photo, not per frame.
        bgBitmap = runCatching {
            val w = (bmp.width / 10).coerceAtLeast(1)
            val h = (bmp.height / 10).coerceAtLeast(1)
            Bitmap.createScaledBitmap(bmp, w, h, true)
        }.getOrNull()
        if (previousFg !== bmp) previousFg?.recycle()
        if (previousBg !== bgBitmap) previousBg?.recycle()
    }

    /** Frees the decoded photo. Call when the renderer's surface goes away for
     *  good, so a swapped-out or replaced wallpaper doesn't keep the bitmaps
     *  alive in the wallpaper service process. */
    fun release() {
        bitmap?.recycle()
        bgBitmap?.recycle()
        bitmap = null
        bgBitmap = null
        particles.clear()
        ripples.clear()
    }

    fun setSize(w: Int, h: Int) {
        if (w == width && h == height) return
        width = w
        height = h
        // Only fall back to the canvas size when no explicit single-screen
        // reference has been set (editor preview case).
        if (refWidth == 0 || refHeight == 0) setReferenceSize(w, h)
        seedParticles()
        vignetteShader = RadialGradient(
            w / 2f, h / 2f, kotlin.math.hypot(w / 2f, h / 2f) * 1.05f,
            intArrayOf(Color.argb(0, 0, 0, 0), Color.argb(0, 0, 0, 0), Color.argb(140, 0, 0, 0)),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP,
        )
    }

    /** The single-screen size to scale/position against — pass the device's
     *  real display resolution here for the live wallpaper (see
     *  [com.harsh.motion.wallpaper.MotionWallpaperService]); leave unset for
     *  the editor preview, where the canvas already IS one screen. */
    fun setReferenceSize(w: Int, h: Int) {
        if (w <= 0 || h <= 0 || (w == refWidth && h == refHeight)) return
        refWidth = w
        refHeight = h
        // Warm gold-white glow (not flat white) so it reads as a light
        // reflection rather than a washed-out haze, with a soft falloff.
        lightRadius = min(w, h) * 0.65f
        lightShader = RadialGradient(
            0f, 0f, lightRadius,
            intArrayOf(
                Color.argb(210, 255, 250, 225),
                Color.argb(110, 255, 232, 180),
                Color.argb(0, 255, 220, 160),
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        // Blue / violet / magenta — the app logo's own monogram gradient, so the
        // wallpaper emits the same light the interface is built from. Each is a
        // full soft radial falloff to transparent, so overlapping them mixes
        // into the gradient-mesh look rather than showing hard edges.
        auroraRadius = min(w, h) * 0.75f
        auroraShaders = listOf(
            intArrayOf(Color.argb(200, 74, 155, 232), Color.argb(90, 40, 110, 200), Color.argb(0, 20, 70, 160)),
            intArrayOf(Color.argb(200, 155, 107, 239), Color.argb(90, 120, 70, 210), Color.argb(0, 80, 40, 170)),
            intArrayOf(Color.argb(200, 208, 107, 224), Color.argb(90, 175, 70, 200), Color.argb(0, 130, 40, 160)),
        ).map { colors ->
            RadialGradient(
                0f, 0f, auroraRadius,
                colors, floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
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
        // Denser and larger than before — the old range (6-24 tiny dots) read as
        // near-invisible noise on a real screen rather than a deliberate effect.
        val count = (18 + config.intensity * 24).toInt().coerceIn(14, 42)
        repeat(count) {
            particles += Particle(
                x = Random.nextFloat() * width,
                y = Random.nextFloat() * height,
                vx = (Random.nextFloat() - 0.5f) * 8f,
                vy = -Random.nextFloat() * 14f - 6f,
                size = Random.nextFloat() * 16f + 10f,
                phase = Random.nextFloat() * 6.28f,
            )
        }
    }

    /** Advance physics by [dt] seconds. Cheap — safe to call every frame. */
    fun update(dt: Float) {
        floatPhase += dt

        // Ease toward the sensor reading instead of snapping to it. Raw
        // accelerometer values jitter with every small hand tremor, and feeding
        // them straight into position is what made tilt motion look nervous.
        // This is a frame-rate-independent exponential approach, so the feel is
        // identical at 30, 60 or 120fps.
        val follow = 1f - kotlin.math.exp(-dt * TILT_FOLLOW_RATE)
        smoothedTiltX += (targetTiltX - smoothedTiltX) * follow
        smoothedTiltY += (targetTiltY - smoothedTiltY) * follow

        // Touch pull and press-and-hold zoom, both eased the same way so a drag
        // glides with the finger and springs back on release instead of snapping.
        val touchActive = touching && EffectType.TOUCH_REACTIVE in config.effects
        val wantPullX = if (touchActive && width > 0) (touchX / width - 0.5f) * 2f else 0f
        val wantPullY = if (touchActive && height > 0) (touchY / height - 0.5f) * 2f else 0f
        val touchFollow = 1f - kotlin.math.exp(-dt * TOUCH_FOLLOW_RATE)
        touchPullX += (wantPullX - touchPullX) * touchFollow
        touchPullY += (wantPullY - touchPullY) * touchFollow
        holdZoom += ((if (touchActive) 1f else 0f) - holdZoom) * touchFollow

        // Particles shove away from the finger, so touching the wallpaper feels
        // like it displaces something rather than only painting a ripple.
        if (touchActive && particles.isNotEmpty()) {
            val reach = min(width, height) * 0.28f
            for (p in particles) {
                val ox = p.x - touchX
                val oy = p.y - touchY
                val dist = kotlin.math.hypot(ox, oy)
                if (dist in 0.01f..reach) {
                    val push = (1f - dist / reach) * 220f * dt
                    p.x += ox / dist * push
                    p.y += oy / dist * push
                }
            }
        }
        // Not gated on the PARTICLES effect specifically: a Shake Burst or
        // Double Tap burst can add particles even when ambient Particles
        // isn't separately enabled, and those still need to animate/decay.
        if (particles.isNotEmpty()) {
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

    fun onTouchDown(x: Float, y: Float) {
        touching = true
        touchX = x
        touchY = y
        addRipple(x, y, 1f)
    }

    /** Finger moved while down — drives the drag/swipe follow. */
    fun onTouchMove(x: Float, y: Float) {
        touching = true
        touchX = x
        touchY = y
    }

    /** Finger lifted — the pull and hold-zoom ease back to rest. */
    fun onTouchUp() {
        touching = false
    }
    fun onDoubleTap(x: Float, y: Float) {
        // Double Tap gets its own distinct identity — a burst on top of the
        // ripple — whenever touch effects are on, not only when Particles is
        // separately selected too.
        if (EffectType.TOUCH_REACTIVE !in config.effects) return
        addRipple(x, y, 1.6f)
        burst(x, y, 10)
    }
    fun onShake() {
        // Was checking PARTICLES instead of SHAKE_BURST, so Shake Effect did
        // nothing unless Particles was also separately enabled. Fixed.
        if (EffectType.SHAKE_BURST in config.effects) burst(width / 2f, height / 2f, 16)
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
        while (particles.size > 70) particles.removeAt(0)
    }

    fun draw(canvas: Canvas) {
        val bmp = bitmap ?: run { canvas.drawColor(Color.BLACK); return }
        val intensity = config.intensity

        var dx = 0f
        var dy = 0f
        var breathe = 1f
        if (EffectType.TILT_PARALLAX in config.effects) {
            dx += smoothedTiltX * 70f * intensity
            dy += smoothedTiltY * 50f * intensity
        }
        dx += homeOffsetX * 60f * intensity
        if (EffectType.FLOATING in config.effects) {
            // Two overlapping frequencies per axis instead of one clean sine —
            // reads as organic ambient drift rather than a metronome tick, and
            // the amplitude is large enough to actually notice at a glance.
            dx += (kotlin.math.sin(floatPhase * 0.6f) * 34f + kotlin.math.sin(floatPhase * 0.23f) * 16f) * intensity
            dy += (kotlin.math.cos(floatPhase * 0.5f) * 26f + kotlin.math.cos(floatPhase * 0.31f) * 12f) * intensity
            breathe += kotlin.math.sin(floatPhase * 0.5f) * 0.06f * intensity
        }
        if (EffectType.CINEMATIC_ZOOM in config.effects) {
            // A smoothstep-eased triangle wave, so the zoom slows to a stop at
            // each end and reverses without a visible kink — the difference
            // between "cinematic" and "sliding back and forth".
            val cycle = kotlin.math.abs((floatPhase * 0.035f) % 2f - 1f)
            val eased = cycle * cycle * (3f - 2f * cycle)
            breathe += eased * 0.18f * intensity
            dx += kotlin.math.sin(floatPhase * 0.045f) * refWidth * 0.06f * intensity
            dy += kotlin.math.cos(floatPhase * 0.037f) * refHeight * 0.06f * intensity
        }
        // Drag/swipe follow and press-and-hold zoom.
        dx += touchPullX * min(width, height) * 0.06f * intensity
        dy += touchPullY * min(width, height) * 0.06f * intensity
        breathe += holdZoom * 0.10f * intensity

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
        if (EffectType.LIQUID_WAVE in config.effects) {
            drawLiquidMesh(canvas, bmp, intensity)
        } else {
            canvas.drawBitmap(bmp, 0f, 0f, bitmapPaint)
        }
        canvas.restore()

        if (EffectType.AURORA_GLOW in config.effects && auroraShaders.isNotEmpty()) {
            // Each light drifts on its own slow Lissajous path and breathes at
            // its own rate, so the blobs keep re-mixing instead of moving as a
            // rigid group. Nudged by tilt too, so the colour responds to the phone.
            auroraShaders.forEachIndexed { index, shader ->
                val phase = floatPhase * (0.075f + index * 0.023f) + index * 2.1f
                val pulse = 1f + kotlin.math.sin(floatPhase * 0.31f + index) * 0.24f
                val cx = width / 2f + kotlin.math.sin(phase) * width * 0.42f +
                    smoothedTiltX * width * 0.12f
                val cy = height / 2f + kotlin.math.cos(phase * 0.83f) * height * 0.34f +
                    smoothedTiltY * height * 0.12f
                auroraMatrix.reset()
                auroraMatrix.postScale(pulse, pulse)
                auroraMatrix.postTranslate(cx, cy)
                shader.setLocalMatrix(auroraMatrix)
                auroraPaint.shader = shader
                auroraPaint.alpha = (110f + 90f * intensity).toInt().coerceIn(40, 235)
                // A circle bounded to the gradient's own radius, not a
                // full-screen rect. The gradient is fully transparent past that
                // radius, so painting the whole screen three times over was
                // compositing millions of pixels that could not change anything.
                canvas.drawCircle(cx, cy, auroraRadius * pulse, auroraPaint)
            }
        }

        if (EffectType.DYNAMIC_LIGHT in config.effects) {
            lightShader?.let { shader ->
                // A slow autonomous drift on top of the tilt response keeps the
                // glow gently alive even when the phone is sitting still.
                val driftX = kotlin.math.sin(floatPhase * 0.15f) * refWidth * 0.18f
                val driftY = kotlin.math.cos(floatPhase * 0.12f) * refHeight * 0.18f
                // A slow breathing pulse on the glow's own size makes the sweep
                // read as a living light source rather than a fixed overlay.
                val pulse = 1f + kotlin.math.sin(floatPhase * 0.35f) * 0.18f
                val glowX = width / 2f + smoothedTiltX * refWidth * 0.4f + driftX
                val glowY = height / 2f + smoothedTiltY * refHeight * 0.4f + driftY
                shaderMatrix.reset()
                shaderMatrix.postScale(pulse, pulse)
                shaderMatrix.postTranslate(glowX, glowY)
                shader.setLocalMatrix(shaderMatrix)
                lightPaint.shader = shader
                // Bounded to the glow's radius, as with the aurora above.
                canvas.drawCircle(glowX, glowY, lightRadius * pulse, lightPaint)

                // Diagonal specular sheen, swept slowly left-to-right and
                // wrapping — the moving-highlight look modern glass/metal UI
                // uses, layered over the static warm glow above.
                canvas.save()
                // Clip in unrotated space first: the band below is deliberately
                // oversized (3x width, 2.2x height) so it still covers the
                // screen once rotated, which meant rasterising ~6.6 screens of
                // shader every frame. The clip cuts that to one screen.
                canvas.clipRect(0f, 0f, width.toFloat(), height.toFloat())
                canvas.rotate(-18f, width / 2f, height / 2f)
                val bandHalfWidth = width * 0.16f
                val period = width * 2.6f
                val sweepCenter = (floatPhase * 55f) % period - period / 2f
                sheenMatrix.reset()
                sheenMatrix.setScale(bandHalfWidth, 1f)
                sheenMatrix.postTranslate(width / 2f + sweepCenter, 0f)
                sheenGradient.setLocalMatrix(sheenMatrix)
                sheenPaint.shader = sheenGradient
                canvas.drawRect(-width.toFloat(), -height * 0.6f, width * 2f, height * 1.6f, sheenPaint)
                canvas.restore()
            }
        }

        if (particles.isNotEmpty()) {
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
                // Crisp bright rim on top of the soft glass wash — reads as a
                // deliberate ring, not a fuzzy blob.
                rippleRimPaint.color = Color.argb((r.alpha * 0.8f).toInt(), 235, 248, 255)
                rippleRimPaint.strokeWidth = 2.5f / r.radius
                canvas.drawCircle(0f, 0f, 0.9f, rippleRimPaint)
                canvas.restore()
            }
        }

        vignetteShader?.let { shader ->
            vignettePaint.shader = shader
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
        }
    }

    /**
     * Draws the photo through a warped vertex grid, giving a slow liquid ripple.
     * Two crossed sine waves per axis at different frequencies keep it from
     * reading as a regular grid pulse. Vertices are in bitmap pixel space, so
     * the caller's existing translate/scale still applies unchanged.
     */
    private fun drawLiquidMesh(canvas: Canvas, bmp: Bitmap, intensity: Float) {
        val vertexCount = (MESH_COLS + 1) * (MESH_ROWS + 1) * 2
        var verts = meshVerts
        if (verts == null || verts.size != vertexCount ||
            meshBitmapWidth != bmp.width || meshBitmapHeight != bmp.height
        ) {
            verts = FloatArray(vertexCount)
            meshVerts = verts
            meshBitmapWidth = bmp.width
            meshBitmapHeight = bmp.height
        }

        // Amplitude scales with the photo, so the ripple looks the same on any
        // resolution rather than vanishing on large images.
        val amp = min(bmp.width, bmp.height) * 0.018f * intensity
        var i = 0
        for (row in 0..MESH_ROWS) {
            val v = row / MESH_ROWS.toFloat()
            val y = v * bmp.height
            for (col in 0..MESH_COLS) {
                val u = col / MESH_COLS.toFloat()
                val x = u * bmp.width
                verts[i++] = x + kotlin.math.sin(floatPhase * 1.1f + v * 7.5f + u * 2.3f) * amp
                verts[i++] = y + kotlin.math.cos(floatPhase * 0.9f + u * 6.5f + v * 1.7f) * amp
            }
        }
        canvas.drawBitmapMesh(bmp, MESH_COLS, MESH_ROWS, verts, 0, null, 0, bitmapPaint)
    }

    private fun drawParticle(canvas: Canvas, p: Particle) {
        when (config.particleStyle) {
            ParticleStyle.SPARKLE -> {
                // A per-particle twinkle (alpha shimmer) instead of a flat dot,
                // plus a faint halo behind the core so it reads as a glint of
                // light rather than a plain white dot.
                val twinkle = (kotlin.math.sin(floatPhase * 3f + p.phase) * 0.5f + 0.5f)
                val core = p.size / 3f * (0.7f + twinkle * 0.4f)
                particlePaint.color = Color.argb((40 + twinkle * 60f).toInt(), 255, 255, 255)
                canvas.drawCircle(p.x, p.y, core * 2.4f, particlePaint)
                particlePaint.color = Color.argb((150 + twinkle * 105f).toInt(), 255, 255, 255)
                canvas.drawCircle(p.x, p.y, core, particlePaint)
            }
            ParticleStyle.BOKEH -> {
                val twinkle = (kotlin.math.sin(floatPhase * 1.4f + p.phase) * 0.5f + 0.5f)
                particlePaint.color = Color.argb(60, 255, 220, 180)
                canvas.drawCircle(p.x, p.y, p.size * 1.7f, particlePaint)
                particlePaint.color = Color.argb((110 + twinkle * 90f).toInt(), 255, 225, 170)
                canvas.drawCircle(p.x, p.y, p.size, particlePaint)
            }
            ParticleStyle.HEART -> {
                val twinkle = (kotlin.math.sin(floatPhase * 1.6f + p.phase) * 0.5f + 0.5f)
                // A slow tumble plus a fixed per-particle lean. Hearts standing
                // perfectly upright in a grid is what makes this kind of effect
                // look like clip art.
                val lean = kotlin.math.sin(floatPhase * 0.45f + p.phase) * 9f +
                    (p.phase - 3.14f) * 3.5f
                // Each heart sits somewhere between a soft rose and the brand
                // magenta, so a drift of them reads as one family of light
                // instead of a single flat pink.
                val mix = kotlin.math.sin(p.phase * 1.7f) * 0.5f + 0.5f
                val cr = (255 + (208 - 255) * mix).toInt()
                val cg = (111 + (107 - 111) * mix).toInt()
                val cb = (168 + (224 - 168) * mix).toInt()

                // Glow follows the silhouette, in a few concentric steps so it
                // fades out. Drawing a circle behind a heart gave a pink disc
                // with a heart floating in it; a single low-alpha silhouette
                // instead gave a hard-edged halo that read as a drop shadow.
                particlePaint.style = Paint.Style.FILL
                val glowScale = 0.7f + twinkle * 0.5f
                for (g in HEART_GLOW_SCALE.indices) {
                    particlePaint.color = Color.argb(
                        (HEART_GLOW_ALPHA[g] * glowScale).toInt().coerceIn(0, 255),
                        cr, cg, cb,
                    )
                    drawHeartShape(canvas, p.x, p.y, p.size * HEART_GLOW_SCALE[g], lean)
                }

                // About a third are drawn as outlines. The mix keeps a dense
                // drift feeling light rather than a wall of solid shapes.
                val outlined = ((p.phase * 97f).toInt() % 3) == 0
                particlePaint.color = Color.argb((185 + twinkle * 60f).toInt(), cr, cg, cb)
                val half = p.size * 1.05f
                if (outlined) {
                    particlePaint.style = Paint.Style.STROKE
                    // The canvas is scaled by `half` inside drawHeartShape, and
                    // that scales stroke width too — so set it in unit space.
                    particlePaint.strokeWidth = (p.size * 0.16f) / half
                }
                drawHeartShape(canvas, p.x, p.y, half, lean)
                // Shared paint: hand it back the way the other styles expect it.
                particlePaint.style = Paint.Style.FILL
            }
        }
    }

    /**
     * Draws the cached unit heart at [cx], [cy] with half-extent [half],
     * rotated by [rotationDeg]. The path is built once in normalised
     * coordinates and positioned by canvas transform, so nothing is
     * re-tessellated per particle per frame and rotation is free.
     */
    private fun drawHeartShape(canvas: Canvas, cx: Float, cy: Float, half: Float, rotationDeg: Float) {
        if (!heartPathBuilt) buildUnitHeart()
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(rotationDeg)
        canvas.scale(half, half)
        canvas.drawPath(heartPath, particlePaint)
        canvas.restore()
    }

    /**
     * A properly proportioned heart in a -1..1 box: a full cubic per lobe wall
     * plus a short cusp curve at the top, which is what lets the lobes actually
     * round over. The previous two-cubic version drove both outer walls from the
     * same height as the centre cusp, so the lobes stayed deflated and the whole
     * shape read as a wide pinched blob.
     *
     * The cusp depth (-0.70) is tuned for the size these are actually drawn at.
     * A shallower notch disappears entirely below ~15px and the top reads as a
     * dome; a deeper one splits the lobes into two separate bumps.
     */
    private fun buildUnitHeart() {
        heartPath.reset()
        heartPath.moveTo(0f, 1f)                                        // bottom point
        heartPath.cubicTo(0f, 1f, -1f, 0.278f, -1f, -0.389f)            // left outer wall
        heartPath.cubicTo(-1f, -0.730f, -0.730f, -1f, -0.389f, -1f)     // left lobe crown
        heartPath.cubicTo(-0.196f, -1f, -0.020f, -0.920f, 0f, -0.700f)  // into the cusp
        heartPath.cubicTo(0.020f, -0.920f, 0.196f, -1f, 0.389f, -1f)    // out of the cusp
        heartPath.cubicTo(0.730f, -1f, 1f, -0.730f, 1f, -0.389f)        // right lobe crown
        heartPath.cubicTo(1f, 0.278f, 0f, 1f, 0f, 1f)                   // right outer wall
        heartPath.close()
        heartPathBuilt = true
    }

    // Tilt is read from MotionSensor by the caller each frame via these setters,
    // avoiding a hard dependency on the sensor class from the renderer. The raw
    // reading is only a target — [update] eases toward it (see TILT_FOLLOW_RATE).
    private var targetTiltX = 0f
    private var targetTiltY = 0f
    private var smoothedTiltX = 0f
    private var smoothedTiltY = 0f
    fun setTilt(x: Float, y: Float) { targetTiltX = x; targetTiltY = y }

    private companion object {
        // Higher follows the sensor more tightly; lower glides more. ~6/s keeps
        // the response immediate to a real tilt while filtering hand tremor.
        const val TILT_FOLLOW_RATE = 6f
        // How fast the photo eases toward, and springs back from, a touch.
        const val TOUCH_FOLLOW_RATE = 7f
        // Liquid wave grid. Denser looks smoother; this is fine to draw every
        // frame because the vertex array is reused rather than reallocated.
        const val MESH_COLS = 16
        const val MESH_ROWS = 24

        // Heart glow falloff: concentric silhouettes, widest and faintest first.
        // Three steps is enough to read as soft light; more just costs fills.
        val HEART_GLOW_SCALE = floatArrayOf(1.90f, 1.52f, 1.22f)
        val HEART_GLOW_ALPHA = floatArrayOf(11f, 17f, 23f)
    }
}
