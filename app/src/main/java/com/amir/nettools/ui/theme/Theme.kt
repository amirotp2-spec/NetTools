package com.amir.nettools.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val NtBlue = Color(0xFF3B82F6)
val NtGreen = Color(0xFF22C55E)
val NtRed = Color(0xFFEF4444)
val NtAmber = Color(0xFFF59E0B)

private val DarkScheme = darkColorScheme(
    primary = NtBlue,
    onPrimary = Color.White,
    background = Color(0xFF0A0B0E),
    onBackground = Color.White,
    surface = Color(0xFF0A0B0E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF17181D),
    onSurfaceVariant = Color(0xFF9CA3AF),
    surfaceContainer = Color(0xFF17181D),
    outline = Color(0xFF2A2C33),
    error = NtRed,
    errorContainer = Color(0xFF3A1518),
    onErrorContainer = Color(0xFFFFB4AB)
)

private val LightScheme = lightColorScheme(
    primary = NtBlue,
    onPrimary = Color.White,
    background = Color(0xFFF5F6F8),
    onBackground = Color(0xFF0A0B0E),
    surface = Color(0xFFF5F6F8),
    onSurface = Color(0xFF0A0B0E),
    surfaceVariant = Color.White,
    onSurfaceVariant = Color(0xFF5B6270),
    surfaceContainer = Color.White,
    outline = Color(0xFFD4D7DD),
    error = NtRed,
    errorContainer = Color(0xFFFDE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

/** themeMode: 0 = system, 1 = dark, 2 = light */
@Composable
fun NetToolsTheme(themeMode: Int, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        1 -> true
        2 -> false
        else -> isSystemInDarkTheme()
    }
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, content = content)
}
