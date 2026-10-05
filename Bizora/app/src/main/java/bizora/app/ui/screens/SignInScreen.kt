package bizora.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bizora.app.R
import bizora.app.ui.components.BizoraEditorialMonogram
import bizora.app.ui.components.EmployeeBadgeIcon
import bizora.app.ui.components.GoogleLogo
import bizora.app.ui.theme.LocalBizoraColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismissError: () -> Unit = {},
    onCancelLoading: () -> Unit = {},
    onGoogleSignIn: () -> Unit,
    onPinLoginClick: () -> Unit,
    onContinueAsEmployer: () -> Unit,
    onReviewerLoginClick: () -> Unit = {},
    onTokenExchange: (String) -> Unit = {}
) {
    val colors = LocalBizoraColors.current
    val scrollState = rememberScrollState()
    var showTokenDialog by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf("") }

    // Intercept hardware Back button to cancel loading immediately
    if (isLoading) {
        BackHandler {
            onCancelLoading()
        }
    }

    val canvasBackground = if (colors.isDark) Color(0xFF141A10) else Color(0xFFFAFBF7)
    val darkOliveHeader = if (colors.isDark) Color(0xFF263319) else Color(0xFF384723)

    // Gentle curved angled boundary between olive header and canvas
    val angledHeaderShape = remember {
        GenericShape { size, _ ->
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            quadraticTo(
                size.width * 0.45f, size.height - 12f,
                0f, size.height - 34f
            )
            close()
        }
    }

    Surface(
        color = canvasBackground,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // TOP SECTION: Dark Olive Editorial Header with Illustration
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(angledHeaderShape)
                        .background(darkOliveHeader)
                ) {
                    // Concentric Circular Rings in Upper-Right
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                    ) {
                        val centerX = size.width * 0.88f
                        val centerY = -size.height * 0.05f
                        val ringColor = Color.White.copy(alpha = 0.08f)
                        val stroke = Stroke(width = 1.6f, cap = StrokeCap.Round)

                        drawCircle(color = ringColor, radius = 100.dp.toPx(), center = Offset(centerX, centerY), style = stroke)
                        drawCircle(color = ringColor, radius = 160.dp.toPx(), center = Offset(centerX, centerY), style = stroke)
                        drawCircle(color = ringColor, radius = 230.dp.toPx(), center = Offset(centerX, centerY), style = stroke)
                        drawCircle(color = ringColor, radius = 310.dp.toPx(), center = Offset(centerX, centerY), style = stroke)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 16.dp)
                    ) {
                        // Monogram Logo Mark
                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            BizoraEditorialMonogram(size = 38.dp)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Headline
                        Text(
                            text = "Built for your\neveryday business.",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 38.sp,
                            letterSpacing = (-0.6).sp,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Subtitle
                        Text(
                            text = "Less paperwork. More progress.",
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFFD2DEC0),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Hero Coworkers Editorial Illustration
                        Image(
                            painter = painterResource(R.drawable.signin_hero_editorial),
                            contentDescription = "Built for your everyday business",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(270.dp)
                        )
                    }
                }

                // BOTTOM SECTION: Off-white Canvas with Actions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 22.dp, bottom = 24.dp)
                ) {
                    // Headline
                    Text(
                        text = "One place. Everything connected.",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (colors.isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Subtitle
                    Text(
                        text = "Manage teams, attendance, orders and payroll.",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (colors.isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
                    )

                    // Error message banner if present
                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            color = if (colors.isDark) Color(0xFF3B1D1D) else Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (colors.isDark) Color(0xFF7F1D1D) else Color(0xFFFCA5A5)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (colors.isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = errorMessage,
                                    color = if (colors.isDark) Color(0xFFFECACA) else Color(0xFF991B1B),
                                    fontSize = 13.5.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = onDismissError,
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = if (colors.isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Primary Button: Continue with Google
                    Surface(
                        color = if (colors.isDark) Color(0xFF22291E) else Color.White,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.2.dp,
                            if (colors.isDark) Color(0xFF384333) else Color(0xFFD1D5DB)
                        ),
                        shadowElevation = 0.5.dp,
                        onClick = onGoogleSignIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            GoogleLogo(size = 22.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Continue with Google",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (colors.isDark) Color.White else Color(0xFF111827)
                            )
                        }
                    }

                    // Helper text below Google button
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sign in or create an account in one step.",
                        fontSize = 12.5.sp,
                        color = if (colors.isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Divider with Employee access label
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = if (colors.isDark) Color(0xFF374133) else Color(0xFFE5E7EB)
                        )
                        Text(
                            text = "Employee access",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (colors.isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280),
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = if (colors.isDark) Color(0xFF374133) else Color(0xFFE5E7EB)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))

                    // Secondary Action: Use employee ID + PIN
                    Surface(
                        color = if (colors.isDark) Color(0xFF28351F) else Color(0xFFEEF3E6),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (colors.isDark) Color(0xFF384A2C) else Color(0xFFE2EAD6)
                        ),
                        onClick = onPinLoginClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            EmployeeBadgeIcon(
                                size = 24.dp,
                                color = if (colors.isDark) Color(0xFFD2DEC0) else Color(0xFF283618)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Use employee ID + PIN",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (colors.isDark) Color.White else Color(0xFF243015)
                            )
                        }
                    }

                    // Helper text below Employee button
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Use the credentials shared by your employer.",
                        fontSize = 12.5.sp,
                        color = if (colors.isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Legal Footer: Terms & Privacy Policy
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "By continuing, you agree to our",
                            fontSize = 12.sp,
                            color = if (colors.isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Terms of Service",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (colors.isDark) Color(0xFFE5E7EB) else Color(0xFF374151)
                            )
                            Text(
                                text = "  •  ",
                                fontSize = 12.sp,
                                color = if (colors.isDark) Color(0xFF6B7280) else Color(0xFF9CA3AF)
                            )
                            Text(
                                text = "Privacy Policy",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (colors.isDark) Color(0xFFE5E7EB) else Color(0xFF374151)
                            )
                        }
                    }

                    // Unobtrusive Developer / Reviewer access bar
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Employer Demo",
                            fontSize = 11.sp,
                            color = colors.textSecondary.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(onClick = onContinueAsEmployer)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = colors.textSecondary.copy(alpha = 0.35f),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Text(
                            text = "Demo Access",
                            fontSize = 11.sp,
                            color = colors.textSecondary.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(onClick = onReviewerLoginClick)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = colors.textSecondary.copy(alpha = 0.35f),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Text(
                            text = "Token",
                            fontSize = 11.sp,
                            color = colors.textSecondary.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { showTokenDialog = true }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Bottom sheet for manual Google ID token exchange
            if (showTokenDialog) {
                val tokenSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { showTokenDialog = false },
                    sheetState = tokenSheetState,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    containerColor = colors.surface,
                    dragHandle = {
                        Box(
                            modifier = Modifier
                                .padding(top = 10.dp, bottom = 6.dp)
                                .width(36.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(colors.border)
                        )
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Google ID Token Exchange",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "If Google Sign-In finished in the browser, paste the token from the confirmation screen below to log in:",
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            label = { Text("ID Token") },
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showTokenDialog = false },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", color = colors.textSecondary)
                            }
                            Button(
                                onClick = {
                                    if (tokenInput.isNotBlank()) {
                                        showTokenDialog = false
                                        onTokenExchange(tokenInput.trim())
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Text("Exchange & Sign In", color = colors.textInverse, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Bottom sheet for credentials checking & workplace loading
            if (isLoading) {
                val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = onCancelLoading,
                    sheetState = loadingSheetState,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    containerColor = colors.surface,
                    dragHandle = {
                        Box(
                            modifier = Modifier
                                .padding(top = 10.dp, bottom = 8.dp)
                                .width(40.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(colors.border)
                        )
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = onCancelLoading,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel and close",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(colors.brandPrimary.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = colors.brandPrimary,
                                strokeWidth = 3.5.dp,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Connecting to Workplace",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 19.sp,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Verifying Google credentials and connecting to your workplace...",
                            fontSize = 13.5.sp,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp
                        )
                        Spacer(modifier = Modifier.height(22.dp))
                        OutlinedButton(
                            onClick = onCancelLoading,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Cancel Sign-In",
                                color = colors.textSecondary,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}
