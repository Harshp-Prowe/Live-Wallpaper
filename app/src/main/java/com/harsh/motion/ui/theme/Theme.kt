package com.harsh.motion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Dark-first, because a wallpaper tool should get out of the way of the
 * imagery. The ground is indigo-tinted rather than neutral black — tinted glass
 * has a cast, and it keeps the three brand lights (see [Brand]) from looking
 * like stickers on a grey card.
 *
 * Light mode is the same identity inverted: a cool violet-white, deliberately
 * not the warm cream this palette would default to.
 */

// The same logo gradient, darkened for legible contrast on light surfaces.
private val BlueInk = Color(0xFF0C56B3)
private val VioletInk = Color(0xFF652892)
private val MagentaInk = Color(0xFF9E32B8)

private val DarkColors = darkColorScheme(
    primary = Brand.Blue,
    onPrimary = Brand.Ink,
    primaryContainer = Color(0xFF10305C),
    onPrimaryContainer = Color(0xFFCFE4FF),

    secondary = Brand.Violet,
    onSecondary = Brand.Ink,
    secondaryContainer = Color(0xFF33265C),
    onSecondaryContainer = Color(0xFFE2D3FF),

    tertiary = Brand.Magenta,
    onTertiary = Brand.Ink,
    tertiaryContainer = Color(0xFF5A2545),
    onTertiaryContainer = Color(0xFFFFD6E8),

    background = Brand.Ink,
    onBackground = Brand.Mist,
    surface = Brand.Slate,
    onSurface = Brand.Mist,
    surfaceVariant = Brand.Haze,
    onSurfaceVariant = Brand.Fog,

    outline = Brand.Line,
    outlineVariant = Color(0xFF231F33),

    error = Color(0xFFFF8A8A),
    onError = Color(0xFF3A0006),
    errorContainer = Color(0xFF6E1420),
    onErrorContainer = Color(0xFFFFDAD9),

    inverseSurface = Brand.Mist,
    inverseOnSurface = Brand.Ink,
    inversePrimary = BlueInk,
)

private val LightColors = lightColorScheme(
    primary = BlueInk,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E6FF),
    onPrimaryContainer = Color(0xFF06183A),

    secondary = VioletInk,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9DEFF),
    onSecondaryContainer = Color(0xFF230A5B),

    tertiary = MagentaInk,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF7DAFF),
    onTertiaryContainer = Color(0xFF2E0A38),

    background = Color(0xFFF7F5FF),
    onBackground = Color(0xFF15131F),
    surface = Color.White,
    onSurface = Color(0xFF15131F),
    surfaceVariant = Color(0xFFEDE9F7),
    onSurfaceVariant = Color(0xFF554F6B),

    outline = Color(0xFFCFC8E2),
    outlineVariant = Color(0xFFE3DEF2),

    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    inverseSurface = Color(0xFF15131F),
    inverseOnSurface = Color(0xFFF3F0FF),
    inversePrimary = Brand.Blue,
)

/*
 * Two voices, both from families already on the device — there is no font file
 * to ship and no network call to make.
 *
 * Display/body: sans, but with tracking pulled tight at large sizes and real
 * weight contrast, so headings read as set type rather than scaled-up Roboto.
 *
 * labelSmall is the utility voice: monospace, wide tracking, used for eyebrows
 * and numeric readouts. It is the vernacular of camera and darkroom equipment,
 * which is what this app is about — light moving across a surface.
 */
private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 31.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.6.sp,
    ),
)

// Generous, consistent rounding — glass panels, not Material default boxes.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun MotionTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
