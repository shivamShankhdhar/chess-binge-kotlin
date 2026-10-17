package com.example.chessbinge.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ChessBingeColorScheme = darkColorScheme(
    primary = CrimsonPrimary,
    onPrimary = TextPrimary,
    primaryContainer = CrimsonDark,
    onPrimaryContainer = TextPrimary,
    secondary = CrimsonGlow,
    onSecondary = TextPrimary,
    tertiary = GoldAccent,
    onTertiary = BgDark,
    background = BgDark,
    onBackground = TextPrimary,
    surface = BgCard,
    onSurface = TextPrimary,
    surfaceVariant = BgCardHover,
    onSurfaceVariant = TextSecondary,
    outline = BgCardBorder
)

@Composable
fun ChessBingeTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = BgDark.toArgb()
                window.navigationBarColor = BgDark.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = ChessBingeColorScheme,
        typography = Typography,
        content = content
    )
}