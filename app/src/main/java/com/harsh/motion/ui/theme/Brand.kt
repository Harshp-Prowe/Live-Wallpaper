package com.harsh.motion.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Brand surface: dark, indigo-tinted glass with light moving under it.
 *
 * The accent colours are not decorative — they are the same three lights the
 * wallpaper engine actually renders (see the aurora shaders in
 * [com.harsh.motion.engine.EffectRenderer]), so the interface is made of the
 * same material as the thing it produces.
 */
object Brand {
    // Sampled from the app logo's card, which falls off from a faintly indigo
    // near-black toward the corners.
    val Ink = Color(0xFF090B11)     // deepest ground
    val Slate = Color(0xFF11141C)   // raised surface
    val Haze = Color(0xFF191D28)    // inset / card
    val Line = Color(0xFF272C3B)    // hairline

    // The logo monogram's gradient, brightened to stay legible as UI accents.
    // Blue -> violet -> magenta, left to right, exactly as the H flows into the F.
    val Blue = Color(0xFF4A9BE8)
    val Violet = Color(0xFF9B6BEF)
    val Magenta = Color(0xFFD06BE0)

    val Mist = Color(0xFFEAECF5)    // primary text
    val Fog = Color(0xFF929AB4)     // secondary text

    /** The signature: accent is always this three-stop light, never a flat fill. */
    val aurora = listOf(Blue, Violet, Magenta)

    fun auroraBrush(): Brush = Brush.linearGradient(aurora)
}

/**
 * The primary action, and the only filled-gradient element in the app. Keeping
 * the brand light to exactly one control per screen is what stops it becoming
 * decoration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuroraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    shape: Shape = RoundedCornerShape(50),
) {
    Surface(onClick = onClick, shape = shape, color = Color.Transparent, modifier = modifier) {
        Row(
            Modifier
                .background(Brand.auroraBrush())
                .padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
    }
}

/**
 * A slow ambient bloom of the three brand lights, drifting behind content.
 *
 * Deliberately the only ambient motion in the app, and it uses the same
 * unhurried easing as the wallpaper engine's own light drift — the interface
 * breathes the way the product does. Everything else stays still.
 */
@Composable
fun AuroraBloom(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            // 18s is slow enough to read as atmosphere rather than animation.
            animation = tween(durationMillis = 18_000, easing = LinearEasing),
        ),
        label = "auroraPhase",
    )

    Box(
        modifier.drawBehind {
            val turn = phase * 2f * Math.PI.toFloat()
            Brand.aurora.forEachIndexed { index, color ->
                val a = turn * (0.6f + index * 0.18f) + index * 2.1f
                val cx = size.width * (0.5f + 0.34f * kotlin.math.sin(a))
                val cy = size.height * (0.42f + 0.40f * kotlin.math.cos(a * 0.82f))
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.40f * intensity),
                            color.copy(alpha = 0.13f * intensity),
                            Color.Transparent,
                        ),
                        center = Offset(cx, cy),
                        radius = size.minDimension * 0.95f,
                    ),
                )
            }
        }.fillMaxSize(),
    )
}
