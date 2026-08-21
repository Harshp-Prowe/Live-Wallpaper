package com.harsh.motion.data

/**
 * Real, independently-rendered motion behaviors. The many interaction names in
 * the product brief (Tilt Effect, Gyroscope Control, Parallax Layers, Motion
 * Follow, Perspective Shift, Depth Layers, Motion Tracking...) describe the same
 * underlying mechanic — device-tilt-driven image movement — so they map to one
 * real engine, [TILT_PARALLAX], rather than being duplicated as fake variants.
 * Same idea for the touch-based and particle-based groups below. The editor
 * offers exactly one toggle per entry here; listing the synonyms as separate
 * toggles made a single tap appear to select several chips at once.
 */
enum class EffectType(val label: String, val description: String) {
    TILT_PARALLAX(
        "Gyro Parallax",
        "Tilt your phone to shift depth layers — covers Tilt, Gyroscope, Accelerometer Motion, Parallax, Perspective Shift.",
    ),
    FLOATING(
        "Floating Motion",
        "A gentle idle drift and breathing scale — covers Floating, Elastic, Inertia, Smooth Follow, Ambient Animation.",
    ),
    TOUCH_REACTIVE(
        "Touch Ripple",
        "Tap and drag ripple across the photo — covers Touch Reactive, Ripple, Press & Hold, Double Tap, Drag, Swipe, Zoom on Touch.",
    ),
    PARTICLES(
        "Particles",
        "Sparkles, hearts or light orbs drifting over the photo, reacting to touch and tilt.",
    ),
    DYNAMIC_LIGHT(
        "Dynamic Light",
        "A soft light sweep and depth vignette that shifts with tilt — covers Dynamic Lighting, Reflection, Depth Blur.",
    ),
    SHAKE_BURST(
        "Shake Burst",
        "Shake your phone for a burst of particles — covers Shake Effect.",
    ),
    CINEMATIC_ZOOM(
        "Cinematic Zoom",
        "A slow, continuously eased zoom and drift across the photo — alive on its own, with no tilt or touch needed.",
    ),
    AURORA_GLOW(
        "Aurora Glow",
        "Large, soft colour lights drifting over the photo and blended like real light, for a dreamy gradient look.",
    ),
    LIQUID_WAVE(
        "Liquid Wave",
        "The photo itself warps in a slow liquid ripple.",
    ),
}

/**
 * Which motion sensors a set of effects actually needs.
 *
 * Both of these are *fused* sensors: the rotation vector keeps the gyroscope,
 * accelerometer and magnetometer powered, and linear acceleration keeps the gyro
 * and accelerometer powered. Registering either one costs the same battery
 * whether or not anything on screen uses the values, so a wallpaper must only
 * ask for what its own effects read.
 */
val Set<EffectType>.needsTiltSensor: Boolean
    get() = EffectType.TILT_PARALLAX in this ||
        EffectType.DYNAMIC_LIGHT in this ||
        EffectType.AURORA_GLOW in this

val Set<EffectType>.needsShakeSensor: Boolean
    get() = EffectType.SHAKE_BURST in this

enum class ParticleStyle(val label: String) {
    SPARKLE("Sparkle"),
    HEART("Hearts"),
    BOKEH("Light Orbs"),
}

/** A single user-created (or template-derived) live wallpaper configuration. */
data class WallpaperConfig(
    val id: String,
    val name: String,
    val photoUri: String,
    val effects: Set<EffectType> = setOf(
        EffectType.FLOATING,
        EffectType.PARTICLES,
        EffectType.DYNAMIC_LIGHT,
        EffectType.TILT_PARALLAX,
        EffectType.TOUCH_REACTIVE,
        EffectType.CINEMATIC_ZOOM,
        EffectType.AURORA_GLOW,
    ),
    val particleStyle: ParticleStyle = ParticleStyle.SPARKLE,
    val intensity: Float = 0.75f,
    // User-chosen crop/position: scale 1x (fit whole photo) up to 4x (zoomed
    // in); offsetX/offsetY are -1..1 fractions of the available pan range at
    // the current scale, so they translate correctly between the small editor
    // preview and the full-resolution live wallpaper.
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val createdAt: Long = System.currentTimeMillis(),
)

/** A prebuilt starting point — the effect combo is applied to whatever photo the user picks. */
data class WallpaperTemplate(
    val id: String,
    val name: String,
    val description: String,
    val effects: Set<EffectType>,
    val particleStyle: ParticleStyle,
    val previewGradient: Pair<Long, Long>,
)

object Templates {
    val all = listOf(
        WallpaperTemplate(
            "cinematic_depth",
            "Cinematic Depth",
            "Subtle parallax with a soft cinematic light sweep.",
            setOf(EffectType.TILT_PARALLAX, EffectType.DYNAMIC_LIGHT),
            ParticleStyle.BOKEH,
            0xFF232526L to 0xFF414345L,
        ),
        WallpaperTemplate(
            "dreamy_float",
            "Dreamy Float",
            "Slow floating motion with drifting sparkles.",
            setOf(EffectType.FLOATING, EffectType.PARTICLES),
            ParticleStyle.SPARKLE,
            0xFF4568DCL to 0xFFB06AB3L,
        ),
        WallpaperTemplate(
            "interactive_glow",
            "Interactive Glow",
            "Touch the screen for ripples and a warm glow.",
            setOf(EffectType.TOUCH_REACTIVE, EffectType.DYNAMIC_LIGHT),
            ParticleStyle.BOKEH,
            0xFFFF6B35L to 0xFFFFB703L,
        ),
        WallpaperTemplate(
            "heartbeat",
            "Heartbeat",
            "Love theme: floating photo with drifting hearts and a tap-triggered heart burst.",
            setOf(EffectType.FLOATING, EffectType.PARTICLES, EffectType.TOUCH_REACTIVE),
            ParticleStyle.HEART,
            0xFFF72585L to 0xFFFF6B9DL,
        ),
        WallpaperTemplate(
            "us_forever",
            "Us Forever",
            "Love theme: parallax depth with drifting hearts and a warm glow.",
            setOf(EffectType.TILT_PARALLAX, EffectType.PARTICLES, EffectType.DYNAMIC_LIGHT),
            ParticleStyle.HEART,
            0xFFFF758CL to 0xFFFF7EB3L,
        ),
        WallpaperTemplate(
            "together_always",
            "Together Always",
            "Love theme: floating motion, touch ripples, and drifting hearts.",
            setOf(EffectType.FLOATING, EffectType.TOUCH_REACTIVE, EffectType.PARTICLES),
            ParticleStyle.HEART,
            0xFFEE9CA7L to 0xFFFFDDE1L,
        ),
        WallpaperTemplate(
            "aurora_dream",
            "Aurora Dream",
            "Soft colour lights drifting over the photo with slow float and light orbs.",
            setOf(EffectType.AURORA_GLOW, EffectType.FLOATING, EffectType.PARTICLES),
            ParticleStyle.BOKEH,
            0xFF00C9A7L to 0xFF845EC2L,
        ),
        WallpaperTemplate(
            "cinematic_drift",
            "Cinematic Drift",
            "A slow film-like zoom across the photo with parallax depth and a light sweep.",
            setOf(EffectType.CINEMATIC_ZOOM, EffectType.TILT_PARALLAX, EffectType.DYNAMIC_LIGHT),
            ParticleStyle.BOKEH,
            0xFF141E30L to 0xFF243B55L,
        ),
        WallpaperTemplate(
            "liquid_glass",
            "Liquid Glass",
            "The photo ripples like water, with glassy touch ripples and a moving sheen.",
            setOf(EffectType.LIQUID_WAVE, EffectType.TOUCH_REACTIVE, EffectType.DYNAMIC_LIGHT),
            ParticleStyle.SPARKLE,
            0xFF2E3192L to 0xFF1BFFFFL,
        ),
        WallpaperTemplate(
            "neon_pulse",
            "Neon Pulse",
            "Aurora colour over a liquid ripple, reacting to every touch.",
            setOf(EffectType.AURORA_GLOW, EffectType.LIQUID_WAVE, EffectType.TOUCH_REACTIVE),
            ParticleStyle.SPARKLE,
            0xFFFF0099L to 0xFF493240L,
        ),
        WallpaperTemplate(
            "living_portrait",
            "Living Portrait",
            "Everything on: cinematic zoom, aurora light, parallax and touch — the full showcase.",
            setOf(
                EffectType.CINEMATIC_ZOOM,
                EffectType.AURORA_GLOW,
                EffectType.TILT_PARALLAX,
                EffectType.TOUCH_REACTIVE,
                EffectType.PARTICLES,
            ),
            ParticleStyle.BOKEH,
            0xFF232526L to 0xFFFF6B35L,
        ),
    )
}
