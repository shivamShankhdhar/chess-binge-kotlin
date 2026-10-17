package bizora.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = LightBizoraColors.brandPrimary,
    onPrimary = LightBizoraColors.textInverse,
    primaryContainer = LightBizoraColors.brandTint,
    onPrimaryContainer = LightBizoraColors.brandPrimary,
    surface = LightBizoraColors.surface,
    onSurface = LightBizoraColors.textPrimary,
    background = LightBizoraColors.canvas,
    onBackground = LightBizoraColors.textPrimary,
    error = LightBizoraColors.danger,
    onError = LightBizoraColors.textInverse
)

@Composable
fun BizoraTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val bizoraColors = LightBizoraColors
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = bizoraColors.canvas.toArgb()
            window.navigationBarColor = bizoraColors.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
    val fixedDensity = androidx.compose.ui.unit.Density(
        density = currentDensity.density,
        fontScale = 1.0f
    )

    val defaultTextStyle = androidx.compose.ui.text.TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 15.sp,
        lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified,
        lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
            alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
            trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.Both
        ),
        color = androidx.compose.ui.graphics.Color.Unspecified
    )

    CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides fixedDensity,
        LocalBizoraColors provides bizoraColors,
        androidx.compose.material3.LocalTextStyle provides defaultTextStyle
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}