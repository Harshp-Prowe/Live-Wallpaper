package com.harsh.dual.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Brand = Color(0xFF6C4CF1)
private val BrandLight = Color(0xFFB9A8FF)

private val DarkColors = darkColorScheme(
    primary = BrandLight,
    onPrimary = Color(0xFF23105E),
    primaryContainer = Color(0xFF4A2FC0),
    onPrimaryContainer = Color(0xFFE7DEFF),
    background = Color(0xFF121016),
    surface = Color(0xFF1B1922),
    surfaceVariant = Color(0xFF2A2733),
)

private val LightColors = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7DEFF),
    onPrimaryContainer = Color(0xFF23105E),
    background = Color(0xFFF7F5FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFECE8F5),
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun DualTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content,
    )
}
