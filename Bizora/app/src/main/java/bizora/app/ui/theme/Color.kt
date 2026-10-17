package bizora.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class BizoraColors(
    val brandPrimary: Color,
    val brandPressed: Color,
    val brandLight: Color,
    val brandTint: Color,
    val canvas: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textInverse: Color,
    val success: Color,
    val successTint: Color,
    val pending: Color,
    val pendingTint: Color,
    val danger: Color,
    val dangerTint: Color,
    val neutralStatus: Color,
    val neutralTint: Color,
    val isDark: Boolean
)

val LightBizoraColors = BizoraColors(
    brandPrimary = Color(0xFF5B692D),   // Primary CTA, selected tab icon
    brandPressed = Color(0xFF465320),   // Pressed button state
    brandLight = Color(0xFF7A8B47),     // Outline accent
    brandTint = Color(0xFFEDF3DF),      // Active tab pill container
    canvas = Color(0xFFFFFFFF),         // Clean crisp white canvas
    surface = Color(0xFFFFFFFF),        // Card surface
    surfaceMuted = Color(0xFFF4F5F7),   // Secondary pills / search bar
    border = Color(0xFFE5E7EB),         // Borders & dividers
    textPrimary = Color(0xFF1B2210),    // Dark olive charcoal header & body
    textSecondary = Color(0xFF6A755A),  // Supporting labels
    textInverse = Color(0xFFFFFFFF),    // Text on filled primary button
    success = Color(0xFF40501E),        // Present status
    successTint = Color(0xFFECF2DE),    // Present status chip/tile
    pending = Color(0xFF8C6314),        // Pending request amber
    pendingTint = Color(0xFFFBF4D7),    // Pending chip/tile
    danger = Color(0xFFBA3B2A),         // Rejected / absent
    dangerTint = Color(0xFFFDEAE7),     // Rejected chip/tile
    neutralStatus = Color(0xFF606C52),  // Not marked
    neutralTint = Color(0xFFF3F4F6),    // Not marked chip/tile
    isDark = false
)

// Dark theme completely removed - DarkBizoraColors aliases to LightBizoraColors
val DarkBizoraColors = LightBizoraColors

val LocalBizoraColors = staticCompositionLocalOf { LightBizoraColors }