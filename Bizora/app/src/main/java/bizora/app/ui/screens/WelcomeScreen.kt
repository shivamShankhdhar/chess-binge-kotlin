package bizora.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bizora.app.R
import bizora.app.ui.components.BizoraMark
import bizora.app.ui.components.FeatureIconScan
import bizora.app.ui.components.FeatureIconShieldLock
import bizora.app.ui.components.FeatureIconTeam
import bizora.app.ui.theme.LocalBizoraColors

private object WelcomeTheme {
    val canvas = Color(0xFFF8FAEF)
    val iconBg = Color(0xFFEDF3E1)
    val iconColor = Color(0xFF4A5832)
    val tagBg = Color(0xFFEDF1DE)
    val tagBorder = Color(0xFFD8DEC8)
    val tagText = Color(0xFF55643B)
    val titleDark = Color(0xFF12160E)
    val titleGreen = Color(0xFF505E35)
    val subtitle = Color(0xFF636C56)
    val itemTitle = Color(0xFF151910)
    val itemDesc = Color(0xFF636B57)
    val divider = Color(0xFFEBEFE2)
    val buttonBg = Color(0xFF596437)
}

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit
) {
    val colors = LocalBizoraColors.current
    val scrollState = rememberScrollState()

    // ── Continuous Breathing & Floating Animations ──
    val infiniteTransition = rememberInfiniteTransition(label = "welcome_ambient")

    // Brand icon gentle breathing
    val brandPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brand_pulse"
    )

    // Illustration floating drift
    val illustrationFloat by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "illustration_float"
    )

    // Background organic accent movement
    val blobOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob_float"
    )

    // Arrow button micro-animation
    val arrowShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_shift"
    )

    // ── Entrance Fade & Slide Transition ──
    val entranceAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entranceAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    Surface(
        color = if (colors.isDark) colors.canvas else WelcomeTheme.canvas,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Ambient decorative background shapes
            if (!colors.isDark) {
                Box(
                    modifier = Modifier
                        .offset(x = 180.dp, y = ((-20) + blobOffset).dp)
                        .size(width = 82.dp, height = 180.dp)
                        .rotate(38f)
                        .clip(RoundedCornerShape(41.dp))
                        .background(Color(0xFFEDF3E0).copy(alpha = 0.65f))
                )
                Box(
                    modifier = Modifier
                        .offset(x = 240.dp, y = (50 - blobOffset).dp)
                        .size(width = 140.dp, height = 180.dp)
                        .clip(RoundedCornerShape(70.dp))
                        .background(Color(0xFFEFF4E3).copy(alpha = 0.5f))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // ── Scrollable Body Area ──
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp)
                        .graphicsLayer {
                            alpha = entranceAnim.value
                            translationY = (1f - entranceAnim.value) * 30f
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Top App Branding: Official App Icon + App Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        BizoraMark(
                            size = 44.dp,
                            modifier = Modifier.scale(brandPulseScale)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Bizora",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.6).sp,
                            color = if (colors.isDark) colors.textPrimary else Color(0xFF1B2210)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Category Badge Pill
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (colors.isDark) colors.surfaceMuted else WelcomeTheme.tagBg)
                            .border(
                                width = 1.dp,
                                color = if (colors.isDark) colors.border else WelcomeTheme.tagBorder,
                                shape = CircleShape
                            )
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "WORKPLACE ATTENDANCE",
                            color = if (colors.isDark) colors.brandPrimary else WelcomeTheme.tagText,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Main Headlines
                    Text(
                        text = "Clock in with a scan.",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (colors.isDark) colors.textPrimary else WelcomeTheme.titleDark,
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp
                    )
                    Text(
                        text = "Carry on with your day.",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (colors.isDark) colors.brandPrimary else WelcomeTheme.titleGreen,
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. Subtitle
                    Text(
                        text = "Simple check-ins. Clear approvals.\nA team that stays in sync.",
                        fontSize = 16.sp,
                        color = if (colors.isDark) colors.textSecondary else WelcomeTheme.subtitle,
                        textAlign = TextAlign.Center,
                        lineHeight = 23.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 5. Welcome Illustration (With smooth floating animation)
                    Image(
                        painter = painterResource(id = R.drawable.welcome_illustration),
                        contentDescription = "Welcome illustration",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(165.dp)
                            .offset(y = illustrationFloat.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 6. Feature List
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Feature 1: QR check-in requests
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (colors.isDark) colors.brandTint else WelcomeTheme.iconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                FeatureIconScan(
                                    size = 22.dp,
                                    color = if (colors.isDark) colors.brandPrimary else WelcomeTheme.iconColor
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "QR check-in requests",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.5.sp,
                                    color = if (colors.isDark) colors.textPrimary else WelcomeTheme.itemTitle
                                )
                                Text(
                                    text = "Scan team QR codes to send your attendance request.",
                                    fontSize = 14.sp,
                                    color = if (colors.isDark) colors.textSecondary else WelcomeTheme.itemDesc,
                                    lineHeight = 19.sp
                                )
                            }
                        }

                        HorizontalDivider(
                            color = if (colors.isDark) colors.border else WelcomeTheme.divider,
                            thickness = 1.dp
                        )

                        // Feature 2: Live approval updates
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (colors.isDark) colors.brandTint else WelcomeTheme.iconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                FeatureIconTeam(
                                    size = 24.dp,
                                    color = if (colors.isDark) colors.brandPrimary else WelcomeTheme.iconColor
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Live approval updates",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.5.sp,
                                    color = if (colors.isDark) colors.textPrimary else WelcomeTheme.itemTitle
                                )
                                Text(
                                    text = "See real-time status from your team or manager.",
                                    fontSize = 14.sp,
                                    color = if (colors.isDark) colors.textSecondary else WelcomeTheme.itemDesc,
                                    lineHeight = 19.sp
                                )
                            }
                        }

                        HorizontalDivider(
                            color = if (colors.isDark) colors.border else WelcomeTheme.divider,
                            thickness = 1.dp
                        )

                        // Feature 3: PIN & biometric app lock
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (colors.isDark) colors.brandTint else WelcomeTheme.iconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                FeatureIconShieldLock(
                                    size = 22.dp,
                                    color = if (colors.isDark) colors.brandPrimary else WelcomeTheme.iconColor
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "PIN & biometric app lock",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.5.sp,
                                    color = if (colors.isDark) colors.textPrimary else WelcomeTheme.itemTitle
                                )
                                Text(
                                    text = "Keep your app private with PIN, Face ID or fingerprint.",
                                    fontSize = 14.sp,
                                    color = if (colors.isDark) colors.textSecondary else WelcomeTheme.itemDesc,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ── 7. Pinned Bottom Action Bar with "Get started" Button ──
                Surface(
                    color = if (colors.isDark) colors.canvas else WelcomeTheme.canvas,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp)
                ) {
                    Button(
                        onClick = onGetStarted,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (colors.isDark) colors.brandPrimary else WelcomeTheme.buttonBg
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text(
                            text = "Get started",
                            color = if (colors.isDark) colors.textInverse else Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = if (colors.isDark) colors.textInverse else Color.White,
                            modifier = Modifier
                                .size(19.dp)
                                .offset(x = arrowShift.dp)
                        )
                    }
                }
            }
        }
    }
}
