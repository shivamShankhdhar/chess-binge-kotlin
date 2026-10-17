package bizora.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Color constants matching React Native IllustrationAssets.tsx
private val SoftBlobBg = Color(0xFFEDF3DF)
private val BrandLine = Color(0xFF5B692D)
private val HeaderBarBg = Color(0xFFFCE9E7)
private val MutedLine = Color(0xFFA89F9A)
private val BarChartGreen1 = Color(0xFF8D9F56)
private val BarChartGreen2 = Color(0xFF6C7D38)

/**
 * 8. Calendar with Clock Badge Illustration
 * Matches React Native CalendarClockIllustration
 * ViewBox: 0 0 110 110
 */
@Composable
fun CalendarClockIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 110.dp
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val scaleFactor = this.size.width / 110f
            withTransform({
                scale(scaleFactor, scaleFactor, Offset.Zero)
            }) {
                // Soft blush background blob (cx=55, cy=55, r=46)
                drawCircle(
                    color = SoftBlobBg,
                    radius = 46f,
                    center = Offset(55f, 55f)
                )

                // Calendar Body (translate 24, 24)
                withTransform({
                    translate(24f, 24f)
                }) {
                    // White calendar card
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(4f, 6f),
                        size = Size(50f, 46f),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Fill
                    )
                    drawRoundRect(
                        color = BrandLine,
                        topLeft = Offset(4f, 6f),
                        size = Size(50f, 46f),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 2.5f)
                    )

                    // Top header bar (fill #FCE9E7)
                    val headerPath = Path().apply {
                        moveTo(4f, 6f)
                        lineTo(54f, 6f)
                        lineTo(54f, 16f)
                        lineTo(4f, 16f)
                        close()
                    }
                    drawPath(headerPath, color = HeaderBarBg, style = Fill)
                    drawLine(
                        color = BrandLine,
                        start = Offset(4f, 16f),
                        end = Offset(54f, 16f),
                        strokeWidth = 2f
                    )

                    // Binder rings (x=14, y=2, w=4, h=8, rx=2; x=40, y=2, w=4, h=8, rx=2)
                    drawRoundRect(
                        color = BrandLine,
                        topLeft = Offset(14f, 2f),
                        size = Size(4f, 8f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                    drawRoundRect(
                        color = BrandLine,
                        topLeft = Offset(40f, 2f),
                        size = Size(4f, 8f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )

                    // Grid dots
                    val dots = listOf(
                        Offset(16f, 24f), Offset(29f, 24f), Offset(42f, 24f),
                        Offset(16f, 34f), Offset(29f, 34f), Offset(42f, 34f)
                    )
                    dots.forEach { dot ->
                        drawCircle(
                            color = BrandLine.copy(alpha = 0.6f),
                            radius = 2f,
                            center = dot
                        )
                    }
                }

                // Clock badge at bottom right (translate 62, 54)
                withTransform({
                    translate(62f, 54f)
                }) {
                    drawCircle(
                        color = BrandLine,
                        radius = 14f,
                        center = Offset(14f, 14f)
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(14f, 7f),
                        end = Offset(14f, 14f),
                        strokeWidth = 2.2f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(14f, 14f),
                        end = Offset(19f, 14f),
                        strokeWidth = 2.2f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

/**
 * 9. Document with Checkmark Badge Illustration
 * Matches React Native DocCheckmarkIllustration
 * ViewBox: 0 0 110 110
 */
@Composable
fun DocCheckmarkIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 110.dp
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val scaleFactor = this.size.width / 110f
            withTransform({
                scale(scaleFactor, scaleFactor, Offset.Zero)
            }) {
                // Soft blush background blob
                drawCircle(
                    color = SoftBlobBg,
                    radius = 46f,
                    center = Offset(55f, 55f)
                )

                // Document Body (translate 30, 22)
                withTransform({
                    translate(30f, 22f)
                }) {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(4f, 4f),
                        size = Size(44f, 56f),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Fill
                    )
                    drawRoundRect(
                        color = BrandLine,
                        topLeft = Offset(4f, 4f),
                        size = Size(44f, 56f),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 2.5f)
                    )

                    // Document text lines
                    drawLine(
                        color = BrandLine,
                        start = Offset(14f, 18f),
                        end = Offset(38f, 18f),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = BrandLine,
                        start = Offset(14f, 28f),
                        end = Offset(38f, 28f),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = BrandLine,
                        start = Offset(14f, 38f),
                        end = Offset(28f, 38f),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )
                }

                // Checkmark badge at bottom right (translate 60, 56)
                withTransform({
                    translate(60f, 56f)
                }) {
                    drawCircle(
                        color = BrandLine,
                        radius = 14f,
                        center = Offset(14f, 14f)
                    )
                    val checkPath = Path().apply {
                        moveTo(8f, 14f)
                        lineTo(12f, 18f)
                        lineTo(20f, 10f)
                    }
                    drawPath(
                        path = checkPath,
                        color = Color.White,
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }
    }
}

/**
 * 10. People Group with Plus Badge Illustration
 * Matches React Native PeopleGroupPlusIllustration
 * ViewBox: 0 0 110 110
 */
@Composable
fun PeopleGroupPlusIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 110.dp
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val scaleFactor = this.size.width / 110f
            withTransform({
                scale(scaleFactor, scaleFactor, Offset.Zero)
            }) {
                // Background circle
                drawCircle(
                    color = SoftBlobBg,
                    radius = 46f,
                    center = Offset(55f, 55f)
                )

                // Avatars (translate 24, 28)
                withTransform({
                    translate(24f, 28f)
                }) {
                    // Left Person
                    drawCircle(
                        color = Color.White,
                        radius = 7f,
                        center = Offset(16f, 14f)
                    )
                    drawCircle(
                        color = BrandLine,
                        radius = 7f,
                        center = Offset(16f, 14f),
                        style = Stroke(width = 2f)
                    )
                    val leftShoulder = Path().apply {
                        moveTo(6f, 32f)
                        cubicTo(6f, 24f, 10f, 21f, 16f, 21f)
                        cubicTo(22f, 21f, 26f, 24f, 26f, 32f)
                    }
                    drawPath(leftShoulder, color = Color.White, style = Fill)
                    drawPath(leftShoulder, color = BrandLine, style = Stroke(width = 2f))

                    // Right Person
                    drawCircle(
                        color = Color.White,
                        radius = 7f,
                        center = Offset(44f, 14f)
                    )
                    drawCircle(
                        color = BrandLine,
                        radius = 7f,
                        center = Offset(44f, 14f),
                        style = Stroke(width = 2f)
                    )
                    val rightShoulder = Path().apply {
                        moveTo(34f, 32f)
                        cubicTo(34f, 24f, 38f, 21f, 44f, 21f)
                        cubicTo(50f, 21f, 54f, 24f, 54f, 32f)
                    }
                    drawPath(rightShoulder, color = Color.White, style = Fill)
                    drawPath(rightShoulder, color = BrandLine, style = Stroke(width = 2f))

                    // Center Main Person
                    drawCircle(
                        color = Color.White,
                        radius = 9f,
                        center = Offset(30f, 12f)
                    )
                    drawCircle(
                        color = BrandLine,
                        radius = 9f,
                        center = Offset(30f, 12f),
                        style = Stroke(width = 2.5f)
                    )
                    val centerShoulder = Path().apply {
                        moveTo(18f, 34f)
                        cubicTo(18f, 25f, 23f, 22f, 30f, 22f)
                        cubicTo(37f, 22f, 42f, 25f, 42f, 34f)
                    }
                    drawPath(centerShoulder, color = Color.White, style = Fill)
                    drawPath(centerShoulder, color = BrandLine, style = Stroke(width = 2.5f))
                }

                // Plus badge at bottom right (translate 64, 56)
                withTransform({
                    translate(64f, 56f)
                }) {
                    drawCircle(
                        color = BrandLine,
                        radius = 13f,
                        center = Offset(13f, 13f)
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(13f, 7f),
                        end = Offset(13f, 19f),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(7f, 13f),
                        end = Offset(19f, 13f),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

/**
 * 11. Document with Magnifying Glass Illustration
 * Matches React Native SearchEmptyDocIllustration
 * ViewBox: 0 0 110 110
 */
@Composable
fun SearchEmptyDocIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 110.dp
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val scaleFactor = this.size.width / 110f
            withTransform({
                scale(scaleFactor, scaleFactor, Offset.Zero)
            }) {
                // Background circle
                drawCircle(
                    color = SoftBlobBg,
                    radius = 46f,
                    center = Offset(55f, 55f)
                )

                // Document paper (translate 28, 22)
                withTransform({
                    translate(28f, 22f)
                }) {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(4f, 4f),
                        size = Size(42f, 54f),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Fill
                    )
                    drawRoundRect(
                        color = BrandLine,
                        topLeft = Offset(4f, 4f),
                        size = Size(42f, 54f),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 2.5f)
                    )
                    drawLine(
                        color = BrandLine,
                        start = Offset(12f, 18f),
                        end = Offset(34f, 18f),
                        strokeWidth = 2.2f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = BrandLine,
                        start = Offset(12f, 28f),
                        end = Offset(26f, 28f),
                        strokeWidth = 2.2f,
                        cap = StrokeCap.Round
                    )
                }

                // Magnifying Glass (translate 52, 46)
                withTransform({
                    translate(52f, 46f)
                }) {
                    drawCircle(
                        color = Color.White,
                        radius = 13f,
                        center = Offset(16f, 16f)
                    )
                    drawCircle(
                        color = BrandLine,
                        radius = 13f,
                        center = Offset(16f, 16f),
                        style = Stroke(width = 3f)
                    )
                    drawLine(
                        color = BrandLine,
                        start = Offset(26f, 26f),
                        end = Offset(38f, 38f),
                        strokeWidth = 3.5f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

/**
 * 12. Envelope with Person+ Badge Illustration
 * Matches React Native EnvelopePersonPlusIllustration
 * ViewBox: 0 0 110 110
 */
@Composable
fun EnvelopePersonPlusIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 110.dp
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val scaleFactor = this.size.width / 110f
            withTransform({
                scale(scaleFactor, scaleFactor, Offset.Zero)
            }) {
                // Background circle
                drawCircle(
                    color = SoftBlobBg,
                    radius = 46f,
                    center = Offset(55f, 55f)
                )

                // Open Envelope (translate 26, 32)
                withTransform({
                    translate(26f, 32f)
                }) {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(4f, 8f),
                        size = Size(52f, 38f),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Fill
                    )
                    drawRoundRect(
                        color = BrandLine,
                        topLeft = Offset(4f, 8f),
                        size = Size(52f, 38f),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 2.5f)
                    )

                    val flapPath = Path().apply {
                        moveTo(5f, 10f)
                        lineTo(30f, 30f)
                        lineTo(55f, 10f)
                    }
                    drawPath(
                        path = flapPath,
                        color = BrandLine,
                        style = Stroke(width = 2f, join = StrokeJoin.Round)
                    )
                }

                // Person+ badge at bottom right (translate 62, 52)
                withTransform({
                    translate(62f, 52f)
                }) {
                    drawCircle(
                        color = BrandLine,
                        radius = 14f,
                        center = Offset(14f, 14f)
                    )
                    // Head
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = Offset(12f, 10f)
                    )
                    // Shoulder
                    val shoulderPath = Path().apply {
                        moveTo(6f, 21f)
                        cubicTo(6f, 17f, 9f, 15f, 12f, 15f)
                        cubicTo(15f, 15f, 18f, 17f, 18f, 21f)
                    }
                    drawPath(
                        path = shoulderPath,
                        color = Color.White,
                        style = Stroke(width = 1.8f)
                    )
                    // Plus sign
                    drawLine(
                        color = Color.White,
                        start = Offset(20f, 10f),
                        end = Offset(20f, 16f),
                        strokeWidth = 1.8f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(17f, 13f),
                        end = Offset(23f, 13f),
                        strokeWidth = 1.8f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

/**
 * 13. Bar Chart with Magnifying Glass Illustration
 * Matches React Native BarChartSearchIllustration
 * ViewBox: 0 0 110 110
 */
@Composable
fun BarChartSearchIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 110.dp
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val scaleFactor = this.size.width / 110f
            withTransform({
                scale(scaleFactor, scaleFactor, Offset.Zero)
            }) {
                // Background circle
                drawCircle(
                    color = SoftBlobBg,
                    radius = 46f,
                    center = Offset(55f, 55f)
                )

                // Bar chart bars (translate 24, 30)
                withTransform({
                    translate(24f, 30f)
                }) {
                    drawRoundRect(
                        color = BarChartGreen1,
                        topLeft = Offset(6f, 28f),
                        size = Size(8f, 22f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                    drawRoundRect(
                        color = BrandLine,
                        topLeft = Offset(18f, 14f),
                        size = Size(8f, 36f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                    drawRoundRect(
                        color = BarChartGreen2.copy(alpha = 0.6f),
                        topLeft = Offset(30f, 6f),
                        size = Size(8f, 44f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                    drawLine(
                        color = BrandLine,
                        start = Offset(2f, 50f),
                        end = Offset(52f, 50f),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )
                }

                // Magnifying Glass (translate 54, 46)
                withTransform({
                    translate(54f, 46f)
                }) {
                    drawCircle(
                        color = Color.White,
                        radius = 12f,
                        center = Offset(14f, 14f)
                    )
                    drawCircle(
                        color = BrandLine,
                        radius = 12f,
                        center = Offset(14f, 14f),
                        style = Stroke(width = 3f)
                    )
                    drawLine(
                        color = BrandLine,
                        start = Offset(23f, 23f),
                        end = Offset(34f, 34f),
                        strokeWidth = 3.5f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

/**
 * Two smartphones (one with checkmark, one with red X).
 * Matches React Native DualPhoneReviewedIllustration
 * ViewBox: 0 0 110 110
 */
@Composable
fun DualPhoneReviewedIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 110.dp
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val scaleFactor = this.size.width / 110f
            withTransform({
                scale(scaleFactor, scaleFactor, Offset.Zero)
            }) {
                // Background circle
                drawCircle(
                    color = SoftBlobBg,
                    radius = 46f,
                    center = Offset(55f, 55f)
                )

                // Left Smartphone (Checkmark) (translate 20, 26)
                withTransform({
                    translate(20f, 26f)
                }) {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(2f, 4f),
                        size = Size(28f, 48f),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Fill
                    )
                    drawRoundRect(
                        color = MutedLine,
                        topLeft = Offset(2f, 4f),
                        size = Size(28f, 48f),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 2f)
                    )
                    drawCircle(
                        color = BrandLine.copy(alpha = 0.3f),
                        radius = 9f,
                        center = Offset(16f, 26f)
                    )
                    val checkPath = Path().apply {
                        moveTo(12f, 26f)
                        lineTo(15f, 29f)
                        lineTo(20f, 23f)
                    }
                    drawPath(
                        path = checkPath,
                        color = BrandLine,
                        style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                // Right Smartphone (Red X) (translate 48, 18)
                withTransform({
                    translate(48f, 18f)
                }) {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(4f, 4f),
                        size = Size(34f, 58f),
                        cornerRadius = CornerRadius(7f, 7f),
                        style = Fill
                    )
                    drawRoundRect(
                        color = BrandLine,
                        topLeft = Offset(4f, 4f),
                        size = Size(34f, 58f),
                        cornerRadius = CornerRadius(7f, 7f),
                        style = Stroke(width = 2.5f)
                    )
                    drawCircle(
                        color = BrandLine,
                        radius = 12f,
                        center = Offset(21f, 30f)
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(16f, 25f),
                        end = Offset(26f, 35f),
                        strokeWidth = 2.2f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(26f, 25f),
                        end = Offset(16f, 35f),
                        strokeWidth = 2.2f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

/**
 * 14. Smartphone with camera lens crossed out by slash.
 * Matches React Native CameraOffIllustration
 */
@Composable
fun CameraOffIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 110.dp
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val scaleFactor = this.size.width / 110f
            withTransform({
                scale(scaleFactor, scaleFactor, Offset.Zero)
            }) {
                drawCircle(color = SoftBlobBg, radius = 46f, center = Offset(55f, 55f))
                withTransform({ translate(35f, 18f) }) {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(4f, 4f),
                        size = Size(34f, 64f),
                        cornerRadius = CornerRadius(8f, 8f),
                        style = Fill
                    )
                    drawRoundRect(
                        color = BrandLine,
                        topLeft = Offset(4f, 4f),
                        size = Size(34f, 64f),
                        cornerRadius = CornerRadius(8f, 8f),
                        style = Stroke(width = 2.5f)
                    )
                    drawLine(color = BrandLine, start = Offset(16f, 9f), end = Offset(26f, 9f), strokeWidth = 2f, cap = StrokeCap.Round)
                    drawRoundRect(color = BrandLine.copy(alpha = 0.8f), topLeft = Offset(11f, 28f), size = Size(20f, 15f), cornerRadius = CornerRadius(3f, 3f))
                    drawCircle(color = Color.White, radius = 4f, center = Offset(21f, 35.5f))
                    drawLine(color = Color(0xFFEF4444), start = Offset(6f, 20f), end = Offset(36f, 52f), strokeWidth = 3f, cap = StrokeCap.Round)
                }
            }
        }
    }
}

/**
 * 15. Invalid QR Code Illustration
 * Matches React Native InvalidQrIllustration
 */
@Composable
fun InvalidQrIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 110.dp
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val scaleFactor = this.size.width / 110f
            withTransform({
                scale(scaleFactor, scaleFactor, Offset.Zero)
            }) {
                drawCircle(color = SoftBlobBg, radius = 46f, center = Offset(55f, 55f))
                withTransform({ translate(24f, 24f) }) {
                    drawRoundRect(color = Color.White, topLeft = Offset(14f, 6f), size = Size(38f, 46f), cornerRadius = CornerRadius(4f, 4f), style = Fill)
                    drawRoundRect(color = BrandLine, topLeft = Offset(14f, 6f), size = Size(38f, 46f), cornerRadius = CornerRadius(4f, 4f), style = Stroke(width = 2f))
                    drawRect(color = BrandLine, topLeft = Offset(20f, 12f), size = Size(8f, 8f))
                    drawRect(color = BrandLine, topLeft = Offset(38f, 12f), size = Size(8f, 8f))
                    drawRect(color = BrandLine, topLeft = Offset(20f, 30f), size = Size(8f, 8f))
                    drawRect(color = BrandLine, topLeft = Offset(32f, 24f), size = Size(4f, 4f))
                    drawRect(color = BrandLine, topLeft = Offset(38f, 30f), size = Size(8f, 8f))
                }
                withTransform({ translate(68f, 20f) }) {
                    drawCircle(color = Color(0xFFEF4444), radius = 12f, center = Offset(12f, 12f))
                    drawLine(color = Color.White, start = Offset(7f, 7f), end = Offset(17f, 17f), strokeWidth = 2.5f, cap = StrokeCap.Round)
                    drawLine(color = Color.White, start = Offset(17f, 7f), end = Offset(7f, 17f), strokeWidth = 2.5f, cap = StrokeCap.Round)
                }
            }
        }
    }
}

/**
 * 16. Closed Signboard Illustration
 * Matches React Native ClosedSignIllustration
 */
@Composable
fun ClosedSignIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 110.dp
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val scaleFactor = this.size.width / 110f
            withTransform({
                scale(scaleFactor, scaleFactor, Offset.Zero)
            }) {
                drawCircle(color = SoftBlobBg, radius = 46f, center = Offset(55f, 55f))
                drawLine(color = BrandLine, start = Offset(30f, 20f), end = Offset(40f, 34f), strokeWidth = 2f)
                drawLine(color = BrandLine, start = Offset(80f, 20f), end = Offset(70f, 34f), strokeWidth = 2f)
                withTransform({ translate(22f, 34f) }) {
                    drawRoundRect(color = BrandLine, topLeft = Offset(4f, 2f), size = Size(60f, 34f), cornerRadius = CornerRadius(6f, 6f), style = Fill)
                    drawRoundRect(color = Color.White.copy(alpha = 0.6f), topLeft = Offset(8f, 6f), size = Size(52f, 26f), cornerRadius = CornerRadius(4f, 4f), style = Stroke(width = 1f))
                }
                withTransform({ translate(66f, 52f) }) {
                    drawCircle(color = Color.White, radius = 14f, center = Offset(14f, 14f))
                    drawCircle(color = BrandLine, radius = 14f, center = Offset(14f, 14f), style = Stroke(width = 2.5f))
                    drawLine(color = BrandLine, start = Offset(14f, 7f), end = Offset(14f, 14f), strokeWidth = 2.2f, cap = StrokeCap.Round)
                    drawLine(color = BrandLine, start = Offset(14f, 14f), end = Offset(19f, 14f), strokeWidth = 2.2f, cap = StrokeCap.Round)
                }
            }
        }
    }
}

/**
 * 17. Paper Plane Offline Illustration
 * Matches React Native PaperPlaneOfflineIllustration
 */
@Composable
fun PaperPlaneOfflineIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 110.dp
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val scaleFactor = this.size.width / 110f
            withTransform({
                scale(scaleFactor, scaleFactor, Offset.Zero)
            }) {
                drawCircle(color = SoftBlobBg, radius = 46f, center = Offset(55f, 55f))
                val planePath = Path().apply {
                    moveTo(25f, 50f)
                    lineTo(70f, 30f)
                    lineTo(45f, 65f)
                    lineTo(40f, 55f)
                    close()
                }
                drawPath(planePath, color = Color.White, style = Fill)
                drawPath(planePath, color = BrandLine, style = Stroke(width = 2f, join = StrokeJoin.Round))
                withTransform({ translate(65f, 55f) }) {
                    drawCircle(color = Color(0xFFEF4444), radius = 11f, center = Offset(11f, 11f))
                    drawLine(color = Color.White, start = Offset(6f, 6f), end = Offset(16f, 16f), strokeWidth = 2.2f, cap = StrokeCap.Round)
                    drawLine(color = Color.White, start = Offset(16f, 6f), end = Offset(6f, 16f), strokeWidth = 2.2f, cap = StrokeCap.Round)
                }
            }
        }
    }
}

/** Matches React Native EmptyHistoryIllustration */
@Composable
fun EmptyHistoryIllustration(modifier: Modifier = Modifier, size: Dp = 100.dp) {
    CalendarClockIllustration(modifier = modifier, size = size)
}

/** Matches React Native EmptyTeamIllustration */
@Composable
fun EmptyTeamIllustration(modifier: Modifier = Modifier, size: Dp = 100.dp) {
    PeopleGroupPlusIllustration(modifier = modifier, size = size)
}

/** Matches React Native EmptyRequestsIllustration */
@Composable
fun EmptyRequestsIllustration(modifier: Modifier = Modifier, size: Dp = 100.dp) {
    DocCheckmarkIllustration(modifier = modifier, size = size)
}

/** Matches React Native ExpiredInviteIllustration */
@Composable
fun ExpiredInviteIllustration(modifier: Modifier = Modifier, size: Dp = 90.dp) {
    DocCheckmarkIllustration(modifier = modifier, size = size)
}

/** Matches React Native OfflineIllustration */
@Composable
fun OfflineIllustration(modifier: Modifier = Modifier, size: Dp = 90.dp) {
    PaperPlaneOfflineIllustration(modifier = modifier, size = size)
}

