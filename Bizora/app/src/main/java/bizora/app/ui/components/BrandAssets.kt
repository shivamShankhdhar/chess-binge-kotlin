package bizora.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bizora.app.R
import bizora.app.ui.theme.LocalBizoraColors

/** Shared app icon badge for the sign-in screens. */
@Composable
fun BizoraSignInIcon(modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Box(
        modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.28f)).background(Color(0xFFECEFDE)),
        contentAlignment = Alignment.Center
    ) {
        BizoraEditorialMonogram(size = size * 0.72f)
    }
}

/**
 * Bizora Editorial Monogram (used on primary sign in and employee sign in header)
 * Features official brand logo emblem.
 */
@Composable
fun BizoraEditorialMonogram(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    amberColor: Color = Color(0xFFF2994A),
    primaryColor: Color = Color.White
) {
    Image(
        painter = painterResource(id = R.drawable.ic_bizora_logo),
        contentDescription = "Bizora Logo",
        modifier = modifier.size(size)
    )
}

/**
 * EmployeeBadgeIcon (Card with person silhouette & lines)
 */
@Composable
fun EmployeeBadgeIcon(
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    color: Color = Color(0xFF283618)
) {
    Canvas(modifier = modifier.size(size, size * 0.72f)) {
        val s = this.size.width / 28f

        scale(s, s, pivot = Offset.Zero) {
            // Outer badge card
            drawRoundRect(
                color = color,
                topLeft = Offset(2f, 2f),
                size = Size(24f, 16f),
                cornerRadius = CornerRadius(3.5f, 3.5f),
                style = Stroke(width = 2.2f)
            )
            // Person head
            drawCircle(
                color = color,
                radius = 2.2f,
                center = Offset(8f, 7.5f)
            )
            // Person torso arc
            val body = Path().apply {
                moveTo(4.5f, 13.5f)
                cubicTo(4.5f, 11f, 6.2f, 10f, 8f, 10f)
                cubicTo(9.8f, 10f, 11.5f, 11f, 11.5f, 13.5f)
            }
            drawPath(body, color = color, style = Stroke(width = 2f, cap = StrokeCap.Round))
            // Horizontal detail lines on right
            drawLine(
                color = color,
                start = Offset(14f, 7.5f),
                end = Offset(21f, 7.5f),
                strokeWidth = 2.2f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(14f, 11.5f),
                end = Offset(19f, 11.5f),
                strokeWidth = 2.2f,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Bizora Brand Mark / Logo
 * Official dual-tone forward-pointing arrow emblem (olive green & warm amber).
 */
@Composable
fun BizoraMark(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: Color = LocalBizoraColors.current.brandPrimary
) {
    Image(
        painter = painterResource(id = R.drawable.ic_bizora_logo),
        contentDescription = "Bizora Logo",
        modifier = modifier.size(size)
    )
}

/**
 * Google Logo Vector (4-color official SVG path)
 * ViewBox: 0 0 24 24
 */
@Composable
fun GoogleLogo(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width / 24f

        scale(s, s, pivot = Offset.Zero) {
            // Blue path
            val bluePath = Path().apply {
                moveTo(22.56f, 12.25f)
                cubicTo(22.56f, 11.47f, 22.49f, 10.72f, 22.36f, 10.0f)
                lineTo(12.0f, 10.0f)
                lineTo(12.0f, 14.26f)
                lineTo(17.92f, 14.26f)
                cubicTo(17.66f, 15.63f, 16.88f, 16.79f, 15.71f, 17.57f)
                lineTo(15.71f, 20.34f)
                lineTo(19.28f, 20.34f)
                cubicTo(21.36f, 18.42f, 22.56f, 15.6f, 22.56f, 12.25f)
                close()
            }
            drawPath(bluePath, color = Color(0xFF4285F4))

            // Green path
            val greenPath = Path().apply {
                moveTo(12.0f, 23.0f)
                cubicTo(14.97f, 23.0f, 17.46f, 22.02f, 19.28f, 20.34f)
                lineTo(15.71f, 17.57f)
                cubicTo(14.73f, 18.23f, 13.48f, 18.63f, 12.0f, 18.63f)
                cubicTo(9.14f, 18.63f, 6.71f, 16.7f, 5.84f, 14.1f)
                lineTo(2.18f, 14.1f)
                lineTo(2.18f, 16.94f)
                cubicTo(3.99f, 20.53f, 7.7f, 23.0f, 12.0f, 23.0f)
                close()
            }
            drawPath(greenPath, color = Color(0xFF34A853))

            // Yellow path
            val yellowPath = Path().apply {
                moveTo(5.84f, 14.09f)
                cubicTo(5.62f, 13.43f, 5.49f, 12.73f, 5.49f, 12.0f)
                cubicTo(5.49f, 11.27f, 5.62f, 10.57f, 5.84f, 9.91f)
                lineTo(5.84f, 7.06f)
                lineTo(2.18f, 7.06f)
                cubicTo(1.43f, 8.55f, 1.0f, 10.22f, 1.0f, 12.0f)
                cubicTo(1.0f, 13.78f, 1.43f, 15.45f, 2.18f, 16.94f)
                lineTo(5.03f, 14.72f)
                close()
            }
            drawPath(yellowPath, color = Color(0xFFFBBC05))

            // Red path
            val redPath = Path().apply {
                moveTo(12.0f, 5.38f)
                cubicTo(13.62f, 5.38f, 15.06f, 5.94f, 16.21f, 7.02f)
                lineTo(19.36f, 3.87f)
                cubicTo(17.45f, 2.09f, 14.97f, 1.0f, 12.0f, 1.0f)
                cubicTo(7.7f, 1.0f, 3.99f, 3.47f, 2.18f, 7.06f)
                lineTo(5.84f, 9.9f)
                cubicTo(6.71f, 7.3f, 9.14f, 5.38f, 12.0f, 5.38f)
                close()
            }
            drawPath(redPath, color = Color(0xFFEA4335))
        }
    }
}

/**
 * 10-dots phone keypad pattern (3x3 grid + 1 centered bottom dot)
 * ViewBox: 0 0 22 26
 */
@Composable
fun DialpadKeypadIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = LocalBizoraColors.current.brandPrimary
) {
    Canvas(modifier = modifier.size(size, size * 26f / 22f)) {
        val s = this.size.width / 22f
        scale(s, s, pivot = Offset.Zero) {
            val r = 2.2f
            // Row 1
            drawCircle(color, r, Offset(4f, 4f))
            drawCircle(color, r, Offset(11f, 4f))
            drawCircle(color, r, Offset(18f, 4f))
            // Row 2
            drawCircle(color, r, Offset(4f, 10.5f))
            drawCircle(color, r, Offset(11f, 10.5f))
            drawCircle(color, r, Offset(18f, 10.5f))
            // Row 3
            drawCircle(color, r, Offset(4f, 17f))
            drawCircle(color, r, Offset(11f, 17f))
            drawCircle(color, r, Offset(18f, 17f))
            // Row 4 (center)
            drawCircle(color, r, Offset(11f, 23.5f))
        }
    }
}

/**
 * ManageTeamsIcon: 3-person silhouette icon
 * ViewBox: 0 0 28 28
 */
@Composable
fun ManageTeamsIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Color(0xFF2A5222)
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width / 28f
        scale(s, s, pivot = Offset.Zero) {
            // Left person
            drawCircle(color, 3.2f, Offset(7f, 10f))
            val leftBody = Path().apply {
                moveTo(2.5f, 21f)
                cubicTo(2.5f, 17.5f, 4.5f, 15.5f, 7f, 15.5f)
                cubicTo(8.2f, 15.5f, 9.3f, 16f, 10.1f, 16.8f)
                cubicTo(9.4f, 18f, 9f, 19.4f, 9f, 21f)
                close()
            }
            drawPath(leftBody, color)

            // Right person
            drawCircle(color, 3.2f, Offset(21f, 10f))
            val rightBody = Path().apply {
                moveTo(25.5f, 21f)
                cubicTo(25.5f, 17.5f, 23.5f, 15.5f, 21f, 15.5f)
                cubicTo(19.8f, 15.5f, 18.7f, 16f, 17.9f, 16.8f)
                cubicTo(18.6f, 18f, 19f, 19.4f, 19f, 21f)
                close()
            }
            drawPath(rightBody, color)

            // Center prominent person
            drawCircle(color, 4.2f, Offset(14f, 8f))
            val centerBody = Path().apply {
                moveTo(7.5f, 21.5f)
                cubicTo(7.5f, 17.2f, 10.4f, 15f, 14f, 15f)
                cubicTo(17.6f, 15f, 20.5f, 17.2f, 20.5f, 21.5f)
                close()
            }
            drawPath(centerBody, color)
        }
    }
}

/**
 * TrackAttendanceIcon: Clock with hands
 * ViewBox: 0 0 24 24
 */
@Composable
fun TrackAttendanceIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Color(0xFF18495A)
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width / 24f
        scale(s, s, pivot = Offset.Zero) {
            drawCircle(
                color = color,
                radius = 9.5f,
                center = Offset(12f, 12f),
                style = Stroke(width = 2.5f)
            )
            val hands = Path().apply {
                moveTo(12f, 7.2f)
                lineTo(12f, 12f)
                lineTo(15.5f, 14.5f)
            }
            drawPath(
                path = hands,
                color = color,
                style = Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

/**
 * StayOrganizedIcon: Rising 3-bar chart
 * ViewBox: 0 0 24 24
 */
@Composable
fun StayOrganizedIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Color(0xFF8A5420)
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width / 24f
        scale(s, s, pivot = Offset.Zero) {
            drawRoundRect(
                color = color,
                topLeft = Offset(4f, 14f),
                size = Size(3.4f, 6f),
                cornerRadius = CornerRadius(1.7f, 1.7f)
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(10.3f, 9f),
                size = Size(3.4f, 11f),
                cornerRadius = CornerRadius(1.7f, 1.7f)
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(16.6f, 4f),
                size = Size(3.4f, 16f),
                cornerRadius = CornerRadius(1.7f, 1.7f)
            )
        }
    }
}

/**
 * Modern Workplace & Attendance Vector Illustration (100% SVG / Canvas)
 * Scalable vector art depicting a smart workplace check-in storefront with glass facade,
 * attendance terminal beacon, scan beam, and verification status.
 * ViewBox: 0 0 340 190
 */
@Composable
fun SignInStorefrontVector(
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    brandColor: Color = LocalBizoraColors.current.brandPrimary
) {
    Canvas(modifier = modifier) {
        val s = this.size.width / 340f
        scale(s, s, pivot = Offset.Zero) {
            // Ground shadow / foundation ellipse
            drawOval(
                color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFDCE6CF).copy(alpha = 0.8f),
                topLeft = Offset(20f, 155f),
                size = Size(300f, 28f)
            )

            // Main Building Back Wall
            drawRoundRect(
                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5E9),
                topLeft = Offset(50f, 45f),
                size = Size(240f, 120f),
                cornerRadius = CornerRadius(16f, 16f)
            )

            // Glass curtain panels (Left and Right)
            val glassColor = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFD6E3C7).copy(alpha = 0.6f)
            drawRoundRect(
                color = glassColor,
                topLeft = Offset(62f, 60f),
                size = Size(58f, 95f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawRoundRect(
                color = glassColor,
                topLeft = Offset(220f, 60f),
                size = Size(58f, 95f),
                cornerRadius = CornerRadius(8f, 8f)
            )

            // Architectural Canopy Roof / Modern Portico
            val canopyPath = Path().apply {
                moveTo(38f, 48f)
                lineTo(302f, 48f)
                lineTo(290f, 32f)
                lineTo(50f, 32f)
                close()
            }
            drawPath(canopyPath, color = brandColor)

            // Canopy Accent Stripe
            drawRoundRect(
                color = Color(0xFFE5A93C),
                topLeft = Offset(38f, 47f),
                size = Size(264f, 4.5f),
                cornerRadius = CornerRadius(2f, 2f)
            )

            // Center Entrance Glass Doors
            drawRoundRect(
                color = if (isDark) Color(0xFF0F172A) else Color(0xFFE8EFE0),
                topLeft = Offset(132f, 68f),
                size = Size(76f, 97f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            // Door Glass Panels
            drawRoundRect(
                color = if (isDark) Color(0xFF38BDF8).copy(alpha = 0.15f) else Color(0xFFBCE3F7).copy(alpha = 0.35f),
                topLeft = Offset(136f, 72f),
                size = Size(32f, 88f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = if (isDark) Color(0xFF38BDF8).copy(alpha = 0.15f) else Color(0xFFBCE3F7).copy(alpha = 0.35f),
                topLeft = Offset(172f, 72f),
                size = Size(32f, 88f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            // Door Handles
            drawRoundRect(
                color = brandColor,
                topLeft = Offset(164f, 108f),
                size = Size(3f, 18f),
                cornerRadius = CornerRadius(1.5f, 1.5f)
            )
            drawRoundRect(
                color = brandColor,
                topLeft = Offset(173f, 108f),
                size = Size(3f, 18f),
                cornerRadius = CornerRadius(1.5f, 1.5f)
            )

            // Digital Attendance Beacon / Clock Tower over Entrance
            drawRoundRect(
                color = brandColor,
                topLeft = Offset(145f, 16f),
                size = Size(50f, 22f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            // Clock Face Screen
            drawRoundRect(
                color = if (isDark) Color(0xFF0F172A) else Color.White,
                topLeft = Offset(150f, 20f),
                size = Size(40f, 14f),
                cornerRadius = CornerRadius(5f, 5f)
            )
            // Small Clock Icon / Pip inside beacon
            drawCircle(
                color = Color(0xFF16A34A),
                radius = 3.5f,
                center = Offset(160f, 27f)
            )
            drawRoundRect(
                color = brandColor,
                topLeft = Offset(167f, 25f),
                size = Size(16f, 3.5f),
                cornerRadius = CornerRadius(1.7f, 1.7f)
            )

            // Smart Attendance Terminal Pedestal (Foreground Left)
            drawRoundRect(
                color = if (isDark) Color(0xFF334155) else Color(0xFF64748B),
                topLeft = Offset(88f, 100f),
                size = Size(16f, 65f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            // Terminal Screen Head
            drawRoundRect(
                color = brandColor,
                topLeft = Offset(82f, 85f),
                size = Size(28f, 22f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = if (isDark) Color(0xFF0F172A) else Color.White,
                topLeft = Offset(85f, 88f),
                size = Size(22f, 16f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            // Terminal Scanner Beam
            val beamPath = Path().apply {
                moveTo(96f, 104f)
                lineTo(78f, 142f)
                lineTo(114f, 142f)
                close()
            }
            drawPath(
                beamPath,
                color = Color(0xFF16A34A).copy(alpha = 0.18f)
            )

            // Floating Verified Attendance Badge (Right side)
            drawRoundRect(
                color = if (isDark) Color(0xFF1E293B) else Color.White,
                topLeft = Offset(230f, 22f),
                size = Size(78f, 34f),
                cornerRadius = CornerRadius(10f, 10f)
            )
            drawRoundRect(
                color = Color(0xFF16A34A).copy(alpha = 0.15f),
                topLeft = Offset(230f, 22f),
                size = Size(78f, 34f),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(width = 1.5f)
            )
            // Green check circle inside badge
            drawCircle(
                color = Color(0xFF16A34A),
                radius = 8f,
                center = Offset(245f, 39f)
            )
            // Checkmark lines
            val checkPath = Path().apply {
                moveTo(241.5f, 39f)
                lineTo(244f, 41.5f)
                lineTo(248.5f, 36.5f)
            }
            drawPath(
                checkPath,
                color = Color.White,
                style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            // Badge text bars
            drawRoundRect(
                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A),
                topLeft = Offset(258f, 32f),
                size = Size(38f, 4.5f),
                cornerRadius = CornerRadius(2f, 2f)
            )
            drawRoundRect(
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                topLeft = Offset(258f, 41f),
                size = Size(26f, 3.5f),
                cornerRadius = CornerRadius(1.7f, 1.7f)
            )
        }
    }
}
