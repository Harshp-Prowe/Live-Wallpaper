package com.harsh.dual.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/*
 * Brand palette — built around Baby Blue (#89CFF0).
 * Light mode: a deeper azure drives actionable elements (readable white text),
 * Baby Blue tints containers. Dark mode: Baby Blue is the primary accent.
 * Dynamic (wallpaper) color is intentionally disabled for a consistent brand.
 */

// Core brand blues
private val BabyBlue = Color(0xFF89CFF0)
private val Azure = Color(0xFF1C86C9)      // deeper, for light-mode buttons
private val DeepNavy = Color(0xFF06304A)

private val LightColors = lightColorScheme(
    primary = Azure,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDEBFB),
    onPrimaryContainer = DeepNavy,

    secondary = Color(0xFF4E6472),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD3E5F0),
    onSecondaryContainer = Color(0xFF0A1E2A),

    tertiary = Color(0xFF5A5B8E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE2DFFF),
    onTertiaryContainer = Color(0xFF161341),

    background = Color(0xFFF5FAFE),
    onBackground = Color(0xFF101C24),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF101C24),
    surfaceVariant = Color(0xFFDEE8EF),
    onSurfaceVariant = Color(0xFF41525C),

    outline = Color(0xFF71828C),
    outlineVariant = Color(0xFFC1CDD4),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    inverseSurface = Color(0xFF2C3237),
    inverseOnSurface = Color(0xFFEDF1F5),
    inversePrimary = BabyBlue,
)

private val DarkColors = darkColorScheme(
    primary = BabyBlue,
    onPrimary = Color(0xFF00344C),
    primaryContainer = Color(0xFF115270),
    onPrimaryContainer = Color(0xFFC7E7FB),

    secondary = Color(0xFFB6CAD8),
    onSecondary = Color(0xFF20333E),
    secondaryContainer = Color(0xFF374955),
    onSecondaryContainer = Color(0xFFD3E5F0),

    tertiary = Color(0xFFC3C0F5),
    onTertiary = Color(0xFF2B2957),
    tertiaryContainer = Color(0xFF423F6F),
    onTertiaryContainer = Color(0xFFE2DFFF),

    background = Color(0xFF0E1519),
    onBackground = Color(0xFFDFE9F0),
    surface = Color(0xFF151E24),
    onSurface = Color(0xFFDFE9F0),
    surfaceVariant = Color(0xFF2A353C),
    onSurfaceVariant = Color(0xFFBAC7D0),

    outline = Color(0xFF84919A),
    outlineVariant = Color(0xFF3F4A51),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    inverseSurface = Color(0xFFDFE9F0),
    inverseOnSurface = Color(0xFF2C3237),
    inversePrimary = Azure,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun DualTheme(
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
        typography = Typography(),
        content = content,
    )
}
