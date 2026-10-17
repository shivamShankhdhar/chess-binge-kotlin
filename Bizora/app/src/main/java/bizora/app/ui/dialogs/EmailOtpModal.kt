package bizora.app.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bizora.app.ui.components.MpinKeypad
import bizora.app.ui.theme.LocalBizoraColors
import bizora.app.utils.maskEmail
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Email OTP Modal with Custom Numeric Keyboard & 3-* Masked Email:
 * - Shows exactly 3 asterisks (***) in between the email username ("hide the gmail only not much")
 * - Dedicated tactile custom on-screen keyboard (MpinKeypad) directly in modal to fill OTPs
 * - 6-Digit OTP boxes (44x50dp, 12dp radius, activeCursorPip, emptyDot, filled character)
 * - Header badge circle with email lock icon (56x56dp, #EBF1DE background, #D8E2C4 border)
 * - "SECURITY VERIFICATION" pill badge
 * - Auto-verify upon 6th digit
 * - Resend cooldown countdown with refresh icon
 * - Cancel text button at bottom
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailOtpModal(
    maskedEmail: String,
    purpose: String = "RESET_MPIN",
    onVerifySuccess: (resetToken: String) -> Unit,
    onRequestOtp: suspend () -> Result<String>, // returns masked email or error
    onVerifyOtp: suspend (otp: String) -> Result<String>, // returns resetToken
    onDismiss: () -> Unit
) {
    val colors = LocalBizoraColors.current
    var otp by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var cooldownSeconds by remember { mutableIntStateOf(30) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val displayEmail = remember(maskedEmail) {
        val raw = maskedEmail.ifBlank { "demo_user@bizora.app" }
        maskEmail(raw)
    }

    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1000)
            cooldownSeconds -= 1
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.canvas,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(40.dp)
                    .height(4.5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFCAD5B8))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close button in top-right (32x32dp circular, #EBEFE0 background)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEBEFE0))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Header Badge Container
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEBF1DE))
                    .border(1.5.dp, Color(0xFFD8E2C4), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.MarkEmailRead,
                    contentDescription = null,
                    tint = colors.brandPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pill Badge: "SECURITY VERIFICATION"
            Surface(
                color = colors.brandTint,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFD8E2C4))
            ) {
                Text(
                    text = "SECURITY VERIFICATION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.brandPrimary,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title & Subtitle
            Text(
                text = if (purpose == "RESET_MPIN") "Reset MPIN Verification" else "Authorize MPIN Change",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.3).sp
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "We sent a 6-digit verification code to",
                fontSize = 13.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            // 3-* Masked Email Highlight
            Text(
                text = displayEmail,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.brandPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Error Message (Clean red text, NO background)
            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.danger,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(18.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 6-digit OTP Input Display Boxes
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 6) {
                    val char = otp.getOrNull(i)?.toString() ?: ""
                    val isFocused = i == otp.length && otp.length < 6
                    val isFilled = char.isNotEmpty()
                    val isError = errorMessage != null && isFilled

                    val boxBg = when {
                        isError -> colors.dangerTint
                        isFocused -> if (colors.isDark) Color(0xFF1E2E1E) else Color(0xFFF9FBF2)
                        else -> if (colors.isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
                    }

                    val boxBorder = when {
                        isError -> colors.danger
                        isFocused -> colors.brandPrimary
                        isFilled -> colors.brandPrimary
                        else -> Color(0xFFD8E2C4)
                    }

                    val borderWidth = if (isFocused || isFilled || isError) 2.dp else 1.5.dp

                    Box(
                        modifier = Modifier
                            .size(44.dp, 50.dp)
                            .scale(if (isFocused) 1.05f else 1f)
                            .shadow(
                                elevation = if (isFocused) 3.dp else 1.5.dp,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .background(boxBg)
                            .border(borderWidth, boxBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFilled) {
                            Text(
                                text = char,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.textPrimary
                            )
                        } else if (isFocused) {
                            // Active cursor pip
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(colors.brandPrimary)
                            )
                        } else {
                            // Empty dot
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFCAD5B8))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Verifying status indicator or Resend Section
            Box(
                modifier = Modifier
                    .height(26.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (isVerifying) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = colors.brandPrimary,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Verifying code...",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.brandPrimary
                        )
                    }
                } else if (isResending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = colors.brandPrimary,
                        strokeWidth = 2.dp
                    )
                } else if (cooldownSeconds > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Resend code in ",
                            fontSize = 12.5.sp,
                            color = colors.textSecondary
                        )
                        Text(
                            text = "${cooldownSeconds}s",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.brandPrimary
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                isResending = true
                                errorMessage = null
                                otp = ""
                                coroutineScope.launch {
                                    val res = onRequestOtp()
                                    isResending = false
                                    if (res.isSuccess) {
                                        cooldownSeconds = 30
                                    } else {
                                        errorMessage = res.exceptionOrNull()?.message ?: "Failed to resend code. Please try again."
                                    }
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = colors.brandPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Resend verification code",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.brandPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Custom Tactile Numeric Keypad to fill OTPs
            MpinKeypad(
                onDigit = { digit ->
                    if (otp.length < 6 && !isVerifying) {
                        val nextOtp = otp + digit
                        otp = nextOtp
                        errorMessage = null

                        if (nextOtp.length == 6) {
                            isVerifying = true
                            coroutineScope.launch {
                                val result = onVerifyOtp(nextOtp)
                                isVerifying = false
                                if (result.isSuccess) {
                                    val token = result.getOrNull() ?: ""
                                    onVerifySuccess(token)
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message
                                        ?: "Incorrect verification code. Please check your email."
                                    otp = ""
                                }
                            }
                        }
                    }
                },
                onBackspace = {
                    if (otp.isNotEmpty() && !isVerifying) {
                        otp = otp.dropLast(1)
                        errorMessage = null
                    }
                },
                disabled = isVerifying,
                showBiometric = false,
                showTopBorder = true,
                keySize = 56.dp,
                digitFontSize = 21.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Cancel Button
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )
            }
        }
    }
}
