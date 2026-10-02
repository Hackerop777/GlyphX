package com.glyphx.app.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = VoidBlack,
    secondary = NeonCyan,
    onSecondary = VoidBlack,
    tertiary = NeonElectricBlue,
    background = VoidBlack,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = VoidBlack
)

private val LightColorScheme = lightColorScheme(
    primary = NeonPurple,
    onPrimary = TextPrimary,
    secondary = NeonCyan,
    onSecondary = TextPrimary,
    tertiary = NeonElectricBlue,
    background = VoidBlack, // Keep dark futuristic aesthetic by default
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = VoidBlack
)

@Composable
fun GlyphXTheme(
    darkTheme: Boolean = true, // Default to deep futuristic dark
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = VoidBlack.toArgb()
                window.navigationBarColor = VoidBlack.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
