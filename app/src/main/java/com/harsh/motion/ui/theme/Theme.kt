package com.harsh.motion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/*
 * Brand palette — warm sunset (orange -> pink). A consistent brand look is used
 * on every device; dynamic wallpaper colors are intentionally not used.
 */

private val Sunset = Color(0xFFFF6B35)     // orange, light-mode primary
private val Rose = Color(0xFFF72585)       // pink accent
private val SunsetGlow = Color(0xFFFF9166) // dark-mode primary (softer orange)

private val LightColors = lightColorScheme(
    primary = Sunset,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0D1),
    onPrimaryContainer = Color(0xFF3A1200),

    secondary = Rose,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD6E8),
    onSecondaryContainer = Color(0xFF3A0020),

    tertiary = Color(0xFFFFB703),
    onTertiary = Color(0xFF3A2800),
    tertiaryContainer = Color(0xFFFFEBB0),
    onTertiaryContainer = Color(0xFF2A1D00),

    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF271A15),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF271A15),
    surfaceVariant = Color(0xFFF2E0DA),
    onSurfaceVariant = Color(0xFF534039),

    outline = Color(0xFF85736C),
    outlineVariant = Color(0xFFD7C2BA),

    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    inverseSurface = Color(0xFF3B2E29),
    inverseOnSurface = Color(0xFFFFEDE6),
    inversePrimary = SunsetGlow,
)

private val DarkColors = darkColorScheme(
    primary = SunsetGlow,
    onPrimary = Color(0xFF4A1D00),
    primaryContainer = Color(0xFF6A3210),
    onPrimaryContainer = Color(0xFFFFDBC8),

    secondary = Color(0xFFFF8FC0),
    onSecondary = Color(0xFF4A0026),
    secondaryContainer = Color(0xFF6A0F42),
    onSecondaryContainer = Color(0xFFFFD6E8),

    tertiary = Color(0xFFFFCC66),
    onTertiary = Color(0xFF3A2800),
    tertiaryContainer = Color(0xFF574000),
    onTertiaryContainer = Color(0xFFFFEBB0),

    background = Color(0xFF191110),
    onBackground = Color(0xFFF1DFD9),
    surface = Color(0xFF221715),
    onSurface = Color(0xFFF1DFD9),
    surfaceVariant = Color(0xFF534039),
    onSurfaceVariant = Color(0xFFD7C2BA),

    outline = Color(0xFF9F8C84),
    outlineVariant = Color(0xFF534039),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    inverseSurface = Color(0xFFF1DFD9),
    inverseOnSurface = Color(0xFF3B2E29),
    inversePrimary = Sunset,
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
        typography = Typography(),
        content = content,
    )
}
