package com.harsh.motion.data

/**
 * Real, independently-rendered motion behaviors. The many interaction names in
 * the product brief (Tilt Effect, Gyroscope Control, Parallax Layers, Motion
 * Follow, Perspective Shift, Depth Layers, Motion Tracking...) describe the same
 * underlying mechanic — device-tilt-driven image movement — so they map to one
 * real engine, [TILT_PARALLAX], rather than being duplicated as fake variants.
 * Same idea for the touch-based and particle-based groups below.
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
}

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
    val effects: Set<EffectType> = setOf(EffectType.TILT_PARALLAX),
    val particleStyle: ParticleStyle = ParticleStyle.SPARKLE,
    val intensity: Float = 0.6f,
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
    )
}
