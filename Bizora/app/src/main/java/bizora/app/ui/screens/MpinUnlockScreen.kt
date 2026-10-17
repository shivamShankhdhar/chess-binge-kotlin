package bizora.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bizora.app.data.repository.BizoraRepository
import bizora.app.ui.components.BizoraMark
import bizora.app.ui.components.HeaderBackgroundArt
import bizora.app.ui.components.MpinDots
import bizora.app.ui.components.MpinKeypad
import bizora.app.ui.theme.LocalBizoraColors
import kotlinx.coroutines.launch

@Composable
fun MpinUnlockScreen(
    repository: BizoraRepository,
    onSuccess: () -> Unit,
    onSignOut: () -> Unit
) {
    val colors = LocalBizoraColors.current
    val coroutineScope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val maxDigits = 4

    fun onDigitPress(digit: String) {
        if (isVerifying || pin.length >= maxDigits) return
        errorMessage = null
        val nextPin = pin + digit
        pin = nextPin

        if (nextPin.length == maxDigits) {
            isVerifying = true
            coroutineScope.launch {
                val result = repository.verifyMpin(nextPin)
                isVerifying = false
                if (result.isSuccess) {
                    onSuccess()
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Invalid MPIN. Please try again."
                    pin = ""
                }
            }
        }
    }

    fun onBackspace() {
        if (pin.isNotEmpty() && !isVerifying) {
            pin = pin.dropLast(1)
            errorMessage = null
        }
    }

    Surface(
        color = colors.canvas,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            HeaderBackgroundArt(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(200.dp, 160.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Section: Header & Sign Out
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BizoraMark(size = 32.dp)

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSignOut() }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign out",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sign out",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textSecondary
                        )
                    }
                }

                // Middle Section: Icon, Title, Pin Dots & Error
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(colors.brandTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = colors.brandPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Security PIN",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Enter your 4-digit MPIN to unlock Bizora",
                        fontSize = 14.sp,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Error / Verifying state
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .padding(bottom = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                color = colors.brandPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (errorMessage != null) {
                            Surface(
                                color = colors.dangerTint,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.danger.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = errorMessage ?: "",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.danger,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // 4 MPIN Slots
                    MpinDots(
                        filledCount = pin.length,
                        total = maxDigits,
                        isError = errorMessage != null
                    )
                }

                // Bottom Section: Custom Numeric Keypad
                MpinKeypad(
                    onDigit = { onDigitPress(it) },
                    onBackspace = { onBackspace() },
                    showBiometric = false,
                    disabled = isVerifying,
                    showTopBorder = true,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}
