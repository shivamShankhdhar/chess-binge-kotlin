package bizora.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bizora.app.ui.theme.LocalBizoraColors

/**
 * HeaderBackgroundArt:
 * Soft flowing organic sage waves in top-right
 * Floating hollow rounded square outline
 * Drifting soft filled squircles
 * Adapts between Light and Dark themes.
 * ViewBox: 0 0 220 180
 */
@Composable
fun HeaderBackgroundArt(
    modifier: Modifier = Modifier
) {
    val colors = LocalBizoraColors.current
    val isDark = colors.isDark

    val wave1 = if (isDark) Color(0xFF141822) else Color(0xFFEDF2E1)
    val wave2 = if (isDark) Color(0xFF10141C) else Color(0xFFE4ECD5)
    val strokeColor = if (isDark) Color(0xFF222938) else Color(0xFFDCE4CD)
    val dot1 = if (isDark) Color(0xFF171D28) else Color(0xFFDFE6CF)
    val dot2 = if (isDark) Color(0xFF1A212E) else Color(0xFFE2E8D3)
    val dot3 = if (isDark) Color(0xFF1E2635) else Color(0xFFE6ECD8)
    val dot4 = if (isDark) Color(0xFF222C3D) else Color(0xFFEAF0DC)

    Canvas(modifier = modifier.size(220.dp, 180.dp)) {
        val sX = this.size.width / 220f
        val sY = this.size.height / 180f

        scale(sX, sY, pivot = Offset.Zero) {
            // Wave 1: M60 0 C95 45 130 80 155 110 C180 140 200 165 220 180 L220 0 Z
            val path1 = Path().apply {
                moveTo(60f, 0f)
                cubicTo(95f, 45f, 130f, 80f, 155f, 110f)
                cubicTo(180f, 140f, 200f, 165f, 220f, 180f)
                lineTo(220f, 0f)
                close()
            }
            drawPath(path1, wave1.copy(alpha = if (isDark) 0.95f else 0.85f))

            // Wave 2: M110 0 C140 40 170 75 190 105 C205 125 214 140 220 150 L220 0 Z
            val path2 = Path().apply {
                moveTo(110f, 0f)
                cubicTo(140f, 40f, 170f, 75f, 190f, 105f)
                cubicTo(205f, 125f, 214f, 140f, 220f, 150f)
                lineTo(220f, 0f)
                close()
            }
            drawPath(path2, wave2.copy(alpha = if (isDark) 0.85f else 0.7f))

            // Small filled floating squircles drifting down
            drawRoundRect(dot1, Offset(104f, 66f), Size(16f, 16f), CornerRadius(5f, 5f))
            drawRoundRect(dot2, Offset(130f, 96f), Size(18f, 18f), CornerRadius(5f, 5f))
            drawRoundRect(dot3, Offset(164f, 122f), Size(15f, 15f), CornerRadius(4f, 4f))
            drawRoundRect(dot4, Offset(188f, 148f), Size(12f, 12f), CornerRadius(3f, 3f))
        }
    }
}

/**
 * CardWatermarkArt:
 * Translucent rounded squares watermark for dark cards.
 */
@Composable
fun CardWatermarkArt(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        // Top-left decorative rounded squares
        Box(
            modifier = Modifier
                .offset(x = (-6).dp, y = (-10).dp)
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.14f))
        )
        Box(
            modifier = Modifier
                .offset(x = 16.dp, y = 12.dp)
                .size(22.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color.White.copy(alpha = 0.10f))
        )
        Box(
            modifier = Modifier
                .offset(x = 6.dp, y = 32.dp)
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.08f))
        )

        // Bottom-right decorative rounded squares
        Box(
            modifier = Modifier
                .offset(x = 42.dp, y = 38.dp)
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color.White.copy(alpha = 0.14f))
        )
    }
}

/**
 * TopAttendanceMark:
 * Custom badge mark from WelcomeScreen:
 * Squircle background rx=12, 4 corner brackets, inner squares and dots.
 * ViewBox: 0 0 46 46
 */
@Composable
fun TopAttendanceMark(
    modifier: Modifier = Modifier,
    size: Dp = 42.dp
) {
    val iconBg = Color(0xFFEDF3E1)
    val iconColor = Color(0xFF4A5832)

    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width / 46f
        scale(s, s, pivot = Offset.Zero) {
            // Squircle base
            drawRoundRect(
                color = iconBg,
                topLeft = Offset(5f, 5f),
                size = Size(36f, 36f),
                cornerRadius = CornerRadius(12f, 12f)
            )

            val strokeW = 2.8f
            // Bracket 1 (top-left): V15 C12 13.3 13.3 12 15 12 H20
            val b1 = Path().apply {
                moveTo(12f, 20f)
                lineTo(12f, 15f)
                cubicTo(12f, 13.3f, 13.3f, 12f, 15f, 12f)
                lineTo(20f, 12f)
            }
            drawPath(b1, iconColor, style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

            // Bracket 2 (top-right): M26 12 H31 C32.7 12 34 13.3 34 15 V20
            val b2 = Path().apply {
                moveTo(26f, 12f)
                lineTo(31f, 12f)
                cubicTo(32.7f, 12f, 34f, 13.3f, 34f, 15f)
                lineTo(34f, 20f)
            }
            drawPath(b2, iconColor, style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

            // Bracket 3 (bottom-left): M12 26 V31 C12 32.7 13.3 34 15 34 H20
            val b3 = Path().apply {
                moveTo(12f, 26f)
                lineTo(12f, 31f)
                cubicTo(12f, 32.7f, 13.3f, 34f, 15f, 34f)
                lineTo(20f, 34f)
            }
            drawPath(b3, iconColor, style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

            // Bracket 4 (bottom-right): M26 34 H31 C32.7 34 34 32.7 34 31 V26
            val b4 = Path().apply {
                moveTo(26f, 34f)
                lineTo(31f, 34f)
                cubicTo(32.7f, 34f, 34f, 32.7f, 34f, 31f)
                lineTo(34f, 26f)
            }
            drawPath(b4, iconColor, style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

            // Inner square 1: (17.5, 17.5) w=4.5, h=4.5, rx=1
            drawRoundRect(
                color = iconColor,
                topLeft = Offset(17.5f, 17.5f),
                size = Size(4.5f, 4.5f),
                cornerRadius = CornerRadius(1f, 1f),
                style = Stroke(1.8f)
            )

            // Inner square 2: (24, 17.5) w=4.5, h=4.5, rx=1
            drawRoundRect(
                color = iconColor,
                topLeft = Offset(24f, 17.5f),
                size = Size(4.5f, 4.5f),
                cornerRadius = CornerRadius(1f, 1f),
                style = Stroke(1.8f)
            )

            // Inner square 3: (17.5, 24) w=4.5, h=4.5, rx=1
            drawRoundRect(
                color = iconColor,
                topLeft = Offset(17.5f, 24f),
                size = Size(4.5f, 4.5f),
                cornerRadius = CornerRadius(1f, 1f),
                style = Stroke(1.8f)
            )

            // Inner filled dot 1: (24, 24) w=2.2, h=2.2, rx=0.5
            drawRoundRect(
                color = iconColor,
                topLeft = Offset(24f, 24f),
                size = Size(2.2f, 2.2f),
                cornerRadius = CornerRadius(0.5f, 0.5f)
            )

            // Inner filled dot 2: (26.8, 26.8) w=1.7, h=1.7, rx=0.4
            drawRoundRect(
                color = iconColor,
                topLeft = Offset(26.8f, 26.8f),
                size = Size(1.7f, 1.7f),
                cornerRadius = CornerRadius(0.4f, 0.4f)
            )
        }
    }
}

/**
 * FeatureIconScan:
 * ViewBox: 0 0 24 24
 */
@Composable
fun FeatureIconScan(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    color: Color = Color(0xFF4A5832)
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width / 24f
        scale(s, s, pivot = Offset.Zero) {
            val strokeW = 2.2f
            // 4 corners
            val c1 = Path().apply { moveTo(2.5f, 7.5f); lineTo(2.5f, 5f); cubicTo(2.5f, 3.6f, 3.6f, 2.5f, 5f, 2.5f); lineTo(7.5f, 2.5f) }
            drawPath(c1, color, style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

            val c2 = Path().apply { moveTo(16.5f, 2.5f); lineTo(19f, 2.5f); cubicTo(20.4f, 2.5f, 21.5f, 3.6f, 21.5f, 5f); lineTo(21.5f, 7.5f) }
            drawPath(c2, color, style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

            val c3 = Path().apply { moveTo(2.5f, 16.5f); lineTo(2.5f, 19f); cubicTo(2.5f, 20.4f, 3.6f, 21.5f, 5f, 21.5f); lineTo(7.5f, 21.5f) }
            drawPath(c3, color, style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

            val c4 = Path().apply { moveTo(16.5f, 21.5f); lineTo(19f, 21.5f); cubicTo(20.4f, 21.5f, 21.5f, 20.4f, 21.5f, 19f); lineTo(21.5f, 16.5f) }
            drawPath(c4, color, style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

            // Scan lines
            drawLine(color, Offset(7f, 8f), Offset(10f, 8f), 2f, StrokeCap.Round)
            drawLine(color, Offset(13.5f, 8f), Offset(17f, 8f), 2f, StrokeCap.Round)
            drawLine(color, Offset(7f, 12f), Offset(17f, 12f), 2.2f, StrokeCap.Round)
            drawLine(color, Offset(7f, 16f), Offset(11.5f, 16f), 2f, StrokeCap.Round)
        }
    }
}

/**
 * FeatureIconTeam:
 * ViewBox: 0 0 28 28
 */
@Composable
fun FeatureIconTeam(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Color(0xFF4A5832)
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width / 28f
        scale(s, s, pivot = Offset.Zero) {
            val strokeW = 2.2f
            drawCircle(color, 3.4f, Offset(14f, 9.5f), style = Stroke(strokeW))

            val centerArc = Path().apply {
                moveTo(7.5f, 22.5f)
                cubicTo(7.5f, 18.5f, 10.4f, 15.5f, 14f, 15.5f)
                cubicTo(17.6f, 15.5f, 20.5f, 18.5f, 20.5f, 22.5f)
            }
            drawPath(centerArc, color, style = Stroke(strokeW, cap = StrokeCap.Round))

            val leftHead = Path().apply {
                moveTo(6f, 10.8f)
                cubicTo(5.2f, 11.5f, 4.8f, 12.6f, 4.8f, 13.8f)
                cubicTo(4.8f, 15f, 5.5f, 16f, 6.5f, 16.5f)
            }
            drawPath(leftHead, color, style = Stroke(2f, cap = StrokeCap.Round))

            val leftBody = Path().apply {
                moveTo(3f, 22f)
                cubicTo(3f, 19.5f, 4.5f, 17.5f, 6.5f, 16.8f)
            }
            drawPath(leftBody, color, style = Stroke(2f, cap = StrokeCap.Round))

            val rightHead = Path().apply {
                moveTo(22f, 10.8f)
                cubicTo(22.8f, 11.5f, 23.2f, 12.6f, 23.2f, 13.8f)
                cubicTo(23.2f, 15f, 22.5f, 16f, 21.5f, 16.5f)
            }
            drawPath(rightHead, color, style = Stroke(2f, cap = StrokeCap.Round))

            val rightBody = Path().apply {
                moveTo(25f, 22f)
                cubicTo(25f, 19.5f, 23.5f, 17.5f, 21.5f, 16.8f)
            }
            drawPath(rightBody, color, style = Stroke(2f, cap = StrokeCap.Round))
        }
    }
}

/**
 * FeatureIconShieldLock:
 * ViewBox: 0 0 24 24
 */
@Composable
fun FeatureIconShieldLock(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    color: Color = Color(0xFF4A5832)
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width / 24f
        scale(s, s, pivot = Offset.Zero) {
            val strokeW = 2.1f
            // Shield outline: M12 2.5C12 2.5 15.5 4.5 19.5 5.2V11.5C19.5 16.2 16.2 20.3 12 21.5C7.8 20.3 4.5 16.2 4.5 11.5V5.2C8.5 4.5 12 2.5 12 2.5Z
            val shield = Path().apply {
                moveTo(12f, 2.5f)
                lineTo(19.5f, 5.2f)
                lineTo(19.5f, 11.5f)
                cubicTo(19.5f, 16.2f, 16.2f, 20.3f, 12f, 21.5f)
                cubicTo(7.8f, 20.3f, 4.5f, 16.2f, 4.5f, 11.5f)
                lineTo(4.5f, 5.2f)
                close()
            }
            drawPath(shield, color, style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

            // Lock body: x=9.5 y=11 w=5 h=4.5 rx=1.2
            drawRoundRect(
                color = color,
                topLeft = Offset(9.5f, 11f),
                size = Size(5f, 4.5f),
                cornerRadius = CornerRadius(1.2f, 1.2f)
            )

            // Lock shackle: M10.4 11V9.4C10.4 8.5 11.1 7.8 12 7.8C12.9 7.8 13.6 8.5 13.6 9.4V11
            val shackle = Path().apply {
                moveTo(10.4f, 11f)
                lineTo(10.4f, 9.4f)
                cubicTo(10.4f, 8.5f, 11.1f, 7.8f, 12f, 7.8f)
                cubicTo(12.9f, 7.8f, 13.6f, 8.5f, 13.6f, 9.4f)
                lineTo(13.6f, 11f)
            }
            drawPath(shackle, color, style = Stroke(1.5f, cap = StrokeCap.Round))
        }
    }
}
