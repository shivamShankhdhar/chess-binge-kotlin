package bizora.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import bizora.app.data.repository.BizoraRepository
import bizora.app.ui.components.AdBanner
import bizora.app.ui.components.BizoraSignInIcon
import bizora.app.ui.dialogs.EmailOtpModal
import bizora.app.ui.theme.LocalBizoraColors
import bizora.app.utils.BiometricHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AppLockMode {
    UNLOCK,
    RESET_NEW,
    RESET_CONFIRM
}

enum class LockUiState {
    READY,              // State 1: Ready - Enter your MPIN
    ENTERING,           // State 2: Entering - digits entered
    CHECKING,           // State 3: Checking - Verifying MPIN
    WRONG_MPIN,         // State 4: Wrong MPIN - Error message
    UNLOCKED,           // State 5: Unlocked - MPIN verified
    TEMPORARILY_LOCKED, // State 6: Temporarily locked - Too many attempts
    CONNECTION_ERROR    // State 7: Connection error - Network issue
}

/**
 * Modern App Lock MPIN screen supporting all 7 states:
 * 1. Ready - Enter your MPIN
 * 2. Entering - Digits entered with active focus box
 * 3. Checking - Verifying MPIN with spinner
 * 4. Wrong MPIN - Red slots with error message and shake animation
 * 5. Unlocked - Green verified checkmark and auto-unlock
 * 6. Temporarily locked - Cooldown timer on too many attempts
 * 7. Connection error - Network retry prompt
 * Uses AdMob banner at the bottom and supports in-screen MPIN reset via OTP.
 */
@Composable
fun AppLockScreen(
    repository: BizoraRepository,
    userName: String = "User",
    onUnlockSuccess: () -> Unit
) {
    val colors = LocalBizoraColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var lockMode by remember { mutableStateOf(AppLockMode.UNLOCK) }
    var uiState by remember { mutableStateOf(LockUiState.READY) }
    var pin by remember { mutableStateOf("") }
    var lastFailedPin by remember { mutableStateOf("") }
    var failedAttempts by remember { mutableIntStateOf(0) }
    var cooldownSeconds by remember { mutableIntStateOf(0) }

    var resetNewPin by remember { mutableStateOf("") }
    var resetConfirmPin by remember { mutableStateOf("") }
    var resetToken by remember { mutableStateOf<String?>(null) }
    var resetErrorMessage by remember { mutableStateOf<String?>(null) }

    var isVerifying by remember { mutableStateOf(false) }
    var isSendingOtp by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showEmailOtpModal by remember { mutableStateOf(false) }
    var maskedEmail by remember { mutableStateOf("") }

    val currentUser by repository.currentUser.collectAsState()
    val maxDigits = 4
    val hasBiometrics = remember { BiometricHelper.isBiometricAvailable(context) }

    val displayName = currentUser?.name?.takeIf { it.isNotBlank() } ?: userName
    val firstName = displayName.split(" ").firstOrNull()?.trim() ?: displayName

    // Shake animation for error state
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(uiState) {
        if (uiState == LockUiState.WRONG_MPIN) {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 360
                    -12f at 60 using LinearEasing
                    12f at 120 using LinearEasing
                    -8f at 180 using LinearEasing
                    8f at 240 using LinearEasing
                    -4f at 300 using LinearEasing
                    0f at 360 using LinearEasing
                }
            )
        }
    }

    // Cooldown countdown timer
    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1000)
            cooldownSeconds -= 1
            if (cooldownSeconds == 0) {
                uiState = LockUiState.READY
                failedAttempts = 0
            }
        }
    }

    fun triggerBiometrics() {
        val fragmentActivity = context as? FragmentActivity
        if (fragmentActivity != null && hasBiometrics && lockMode == AppLockMode.UNLOCK && uiState != LockUiState.TEMPORARILY_LOCKED) {
            BiometricHelper.showBiometricPrompt(
                activity = fragmentActivity,
                title = "Unlock Bizora",
                subtitle = "Authenticate using Face ID or Fingerprint",
                negativeButtonText = "Use MPIN",
                onSuccess = {
                    uiState = LockUiState.UNLOCKED
                    coroutineScope.launch {
                        delay(400)
                        onUnlockSuccess()
                    }
                },
                onError = { /* Ignored if cancelled */ }
            )
        }
    }

    // Auto-prompt biometrics once on launch
    LaunchedEffect(Unit) {
        if (hasBiometrics && lockMode == AppLockMode.UNLOCK) {
            delay(120)
            triggerBiometrics()
        }
    }

    fun verifyMpinAttempt(enteredPin: String) {
        isVerifying = true
        uiState = LockUiState.CHECKING
        coroutineScope.launch {
            val result = repository.verifyMpin(enteredPin)
            isVerifying = false
            if (result.isSuccess) {
                uiState = LockUiState.UNLOCKED
                delay(500)
                onUnlockSuccess()
            } else {
                val err = result.exceptionOrNull()?.message ?: ""
                val isConnectionError = err.contains("connect", ignoreCase = true) ||
                        err.contains("network", ignoreCase = true) ||
                        err.contains("offline", ignoreCase = true) ||
                        err.contains("timeout", ignoreCase = true)

                if (isConnectionError) {
                    lastFailedPin = enteredPin
                    uiState = LockUiState.CONNECTION_ERROR
                } else {
                    failedAttempts += 1
                    if (failedAttempts >= 5) {
                        cooldownSeconds = 30
                        uiState = LockUiState.TEMPORARILY_LOCKED
                        pin = ""
                    } else {
                        uiState = LockUiState.WRONG_MPIN
                    }
                }
            }
        }
    }

    fun onDigitPress(digit: String) {
        if (isVerifying || cooldownSeconds > 0) return

        // If previously in error state, start fresh with the new digit
        if (uiState == LockUiState.WRONG_MPIN || uiState == LockUiState.CONNECTION_ERROR) {
            pin = digit
            uiState = LockUiState.ENTERING
            return
        }

        // 1. UNLOCK MODE
        if (lockMode == AppLockMode.UNLOCK) {
            if (pin.length >= maxDigits) return
            val next = pin + digit
            pin = next
            uiState = if (next.length == maxDigits) LockUiState.CHECKING else LockUiState.ENTERING

            if (next.length == maxDigits) {
                verifyMpinAttempt(next)
            }
            return
        }

        // 2. RESET NEW PIN MODE
        if (lockMode == AppLockMode.RESET_NEW) {
            if (pin.length >= maxDigits) return
            val next = pin + digit
            pin = next
            if (next.length == maxDigits) {
                resetNewPin = next
                coroutineScope.launch {
                    delay(200)
                    lockMode = AppLockMode.RESET_CONFIRM
                    pin = ""
                }
            }
            return
        }

        // 3. RESET CONFIRM PIN MODE
        if (lockMode == AppLockMode.RESET_CONFIRM) {
            if (pin.length >= maxDigits) return
            val next = pin + digit
            pin = next

            if (next.length == maxDigits) {
                resetConfirmPin = next
                if (next != resetNewPin) {
                    resetErrorMessage = "MPIN does not match. Please try again."
                    coroutineScope.launch {
                        delay(1200)
                        lockMode = AppLockMode.RESET_NEW
                        pin = ""
                        resetNewPin = ""
                        resetConfirmPin = ""
                        resetErrorMessage = null
                    }
                    return
                }

                isVerifying = true
                coroutineScope.launch {
                    val res = repository.resetMpin(
                        newMpin = next,
                        enableBiometric = hasBiometrics,
                        resetToken = resetToken
                    )
                    isVerifying = false
                    if (res.isSuccess) {
                        Toast.makeText(context, "MPIN Reset Successfully", Toast.LENGTH_SHORT).show()
                        resetToken = null
                        lockMode = AppLockMode.UNLOCK
                        onUnlockSuccess()
                    } else {
                        resetErrorMessage = res.exceptionOrNull()?.message ?: "Failed to reset MPIN"
                        lockMode = AppLockMode.RESET_NEW
                        pin = ""
                        resetNewPin = ""
                        resetConfirmPin = ""
                    }
                }
            }
        }
    }

    fun onBackspace() {
        if (isVerifying || cooldownSeconds > 0) return

        if (uiState == LockUiState.WRONG_MPIN || uiState == LockUiState.CONNECTION_ERROR) {
            pin = ""
            uiState = LockUiState.READY
            return
        }

        if (pin.isNotEmpty()) {
            val next = pin.dropLast(1)
            pin = next
            uiState = if (next.isEmpty()) LockUiState.READY else LockUiState.ENTERING
        } else if (pin.isEmpty() && lockMode == AppLockMode.RESET_CONFIRM) {
            lockMode = AppLockMode.RESET_NEW
            pin = resetNewPin
        }
    }

    fun handleForgotMpin() {
        if (isSendingOtp) return
        showResetConfirmDialog = true
    }

    fun proceedToRequestOtp() {
        showResetConfirmDialog = false
        isSendingOtp = true
        coroutineScope.launch {
            val res = repository.requestMpinOtp("RESET_MPIN")
            isSendingOtp = false
            if (res.isSuccess) {
                maskedEmail = res.getOrNull()?.maskedEmail ?: bizora.app.utils.maskEmail(currentUser?.email)
                showEmailOtpModal = true
            } else {
                Toast.makeText(context, res.exceptionOrNull()?.message ?: "Failed to send code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Surface(
        color = colors.canvas,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ─── TOP SECTION: Header & Floating MPIN Card (Cream Background) ───
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // ─── Header: [B] Bizora logo, "Hi, Shivam", "Enter your MPIN to continue" ───
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BizoraSignInIcon(size = 26.dp)
                        Text(
                            text = "Bizora",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            letterSpacing = (-0.3).sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = when (lockMode) {
                            AppLockMode.UNLOCK -> "Hi, $firstName"
                            AppLockMode.RESET_NEW -> "Set New MPIN"
                            AppLockMode.RESET_CONFIRM -> "Confirm New MPIN"
                        },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        letterSpacing = (-0.4).sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when (lockMode) {
                            AppLockMode.UNLOCK -> "Enter your MPIN to continue"
                            AppLockMode.RESET_NEW -> "Enter a 4-digit MPIN to unlock Bizora"
                            AppLockMode.RESET_CONFIRM -> "Re-enter your new MPIN"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ─── MPIN Card Container with 4 rounded input boxes & state message ───
                Surface(
                    color = if (colors.isDark) Color(0xFF1E241B) else Color.White,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF2A3326) else Color(0xFFE8EDE1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 4 Rounded Rectangular Input Boxes with shake animation on error
                        MpinBoxesRow(
                            pinLength = pin.length,
                            uiState = uiState,
                            maxDigits = maxDigits,
                            modifier = Modifier.graphicsLayer {
                                translationX = shakeOffset.value
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Contextual feedback according to the 7 states
                        when (uiState) {
                            LockUiState.READY, LockUiState.ENTERING -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "Forgot MPIN?",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2E5924),
                                        modifier = Modifier
                                            .clickable { handleForgotMpin() }
                                            .padding(vertical = 4.dp, horizontal = 2.dp)
                                    )
                                }
                            }

                            LockUiState.CHECKING -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color(0xFF2E4F24),
                                        strokeWidth = 2.2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Checking MPIN...",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2E4F24)
                                    )
                                }
                            }

                            LockUiState.WRONG_MPIN -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Incorrect MPIN. Please try again.",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            text = "Forgot MPIN?",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF2E4F24),
                                            modifier = Modifier
                                                .clickable { handleForgotMpin() }
                                                .padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            LockUiState.UNLOCKED -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF2E4F24),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "MPIN verified",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Opening your workspace...",
                                        fontSize = 12.5.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            LockUiState.TEMPORARILY_LOCKED -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color(0xFFEA580C),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Too many incorrect attempts",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFC2410C)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Try again in %02d:%02d".format(cooldownSeconds / 60, cooldownSeconds % 60),
                                                fontSize = 12.sp,
                                                color = colors.textSecondary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            text = "Forgot MPIN?",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF2E4F24),
                                            modifier = Modifier
                                                .clickable { handleForgotMpin() }
                                                .padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            LockUiState.CONNECTION_ERROR -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Unable to verify MPIN",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD97706)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Check your connection and try again.",
                                        fontSize = 12.sp,
                                        color = colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = {
                                            if (lastFailedPin.length == maxDigits) {
                                                verifyMpinAttempt(lastFailedPin)
                                            } else {
                                                pin = ""
                                                uiState = LockUiState.READY
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF264E24)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                    ) {
                                        Text(
                                            text = "Retry",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (lockMode != AppLockMode.UNLOCK) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                lockMode = AppLockMode.UNLOCK
                                pin = ""
                                resetNewPin = ""
                                resetConfirmPin = ""
                                resetToken = null
                                resetErrorMessage = null
                                uiState = LockUiState.READY
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Cancel & Return to Lock",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            // ─── BOTTOM SECTION: Distinct White Container with Rounded Top Corners ───
            val keypadDisabled = isVerifying ||
                    cooldownSeconds > 0 ||
                    uiState == LockUiState.CHECKING ||
                    uiState == LockUiState.UNLOCKED ||
                    uiState == LockUiState.TEMPORARILY_LOCKED ||
                    uiState == LockUiState.CONNECTION_ERROR

            Surface(
                color = if (colors.isDark) Color(0xFF1B2218) else Color.White,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF2A3326) else Color(0xFFE8EDE1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 3x4 Clean Numeric Keypad
                    CleanNumericKeypad(
                        onDigit = { onDigitPress(it) },
                        onBackspace = { onBackspace() },
                        showBiometric = lockMode == AppLockMode.UNLOCK && hasBiometrics,
                        onBiometricPress = { triggerBiometrics() },
                        disabled = keypadDisabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Trust Badge: Green lock + "Your workspace, securely connected."
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF2E4F24),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Your workspace, securely connected.",
                            fontSize = 12.sp,
                            color = colors.textSecondary.copy(alpha = 0.85f)
                        )
                    }

                    // Sticky Real Google AdMob Banner at bottom
                    AdBanner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    )
                }
            }
        }

        // ─── Confirmation Alert Dialog: Reset MPIN ───
        if (showResetConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showResetConfirmDialog = false },
                title = {
                    Text(
                        text = "Reset 4-Digit MPIN?",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to reset your MPIN? A 6-digit verification code will be sent to your registered email to authorize the reset.",
                        fontSize = 13.5.sp,
                        color = colors.textSecondary,
                        lineHeight = 19.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showResetConfirmDialog = false
                            proceedToRequestOtp()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF264E24))
                    ) {
                        Text("Send Code", fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirmDialog = false }) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                },
                containerColor = colors.surface,
                shape = RoundedCornerShape(18.dp)
            )
        }

        // ─── Email OTP Modal ───
        if (showEmailOtpModal) {
            EmailOtpModal(
                maskedEmail = maskedEmail,
                purpose = "RESET_MPIN",
                onVerifySuccess = { token ->
                    resetToken = token
                    showEmailOtpModal = false
                    lockMode = AppLockMode.RESET_NEW
                    pin = ""
                    resetNewPin = ""
                    resetConfirmPin = ""
                    resetErrorMessage = null
                    uiState = LockUiState.READY
                },
                onRequestOtp = {
                    val res = repository.requestMpinOtp("RESET_MPIN")
                    if (res.isSuccess) {
                        val em = res.getOrNull()?.maskedEmail ?: bizora.app.utils.maskEmail(currentUser?.email)
                        maskedEmail = em
                        Result.success(em)
                    } else {
                        Result.failure(res.exceptionOrNull() ?: Exception("Failed to send OTP"))
                    }
                },
                onVerifyOtp = { otpCode ->
                    repository.verifyMpinOtp(otpCode, "RESET_MPIN")
                },
                onDismiss = { showEmailOtpModal = false }
            )
        }
    }
}

/**
 * 4 Rounded rectangular MPIN input boxes matching the provided mockup designs:
 * - Green border on the active box
 * - Solid dark green dots on filled boxes
 * - Light red background & red dots on error
 * - Gray dots on temporary locked
 */
@Composable
private fun MpinBoxesRow(
    pinLength: Int,
    uiState: LockUiState,
    maxDigits: Int = 4,
    modifier: Modifier = Modifier
) {
    val colors = LocalBizoraColors.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until maxDigits) {
            val isFilled = i < pinLength
            val isFocus = i == pinLength && uiState in listOf(LockUiState.READY, LockUiState.ENTERING)
            val isWrong = uiState == LockUiState.WRONG_MPIN
            val isLocked = uiState == LockUiState.TEMPORARILY_LOCKED

            val boxBg = when {
                isWrong -> Color(0xFFFDE8E8)
                isLocked -> Color(0xFFF3F4F6)
                colors.isDark -> Color(0xFF252A22)
                else -> Color(0xFFEFF2EB)
            }

            val boxBorder = when {
                isWrong -> BorderStroke(1.dp, Color(0xFFFCA5A5))
                isFocus -> BorderStroke(1.5.dp, Color(0xFF2E4F24))
                else -> null
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .then(
                        if (boxBorder != null) Modifier.border(boxBorder, RoundedCornerShape(7.dp))
                        else Modifier
                    )
                    .background(boxBg),
                contentAlignment = Alignment.Center
            ) {
                if (isWrong) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDC2626))
                    )
                } else if (isLocked) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF9CA3AF))
                    )
                } else if (isFilled) {
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E4F24))
                    )
                }
            }
        }
    }
}

/**
 * 3x4 Clean Numeric Keypad matching the mockup design:
 * - Rounded soft-tinted key backgrounds (88x56dp, RoundedCornerShape(16.dp))
 * - Medium/Normal digits, clean Backspace key
 * - Subtle touch ripple feedback
 * - Dimmed alpha when disabled
 */
@Composable
private fun CleanNumericKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    showBiometric: Boolean,
    onBiometricPress: () -> Unit,
    disabled: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalBizoraColors.current
    val keypadRows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(if (showBiometric) "BIO" else "", "0", "DEL")
    )

    Column(
        modifier = modifier.graphicsLayer {
            alpha = if (disabled) 0.35f else 1f
        },
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        keypadRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { item ->
                    if (item.isEmpty()) {
                        Spacer(modifier = Modifier.size(52.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (colors.isDark) Color(0xFF22281F) else Color(0xFFF3F5EB))
                                .clickable(
                                    enabled = !disabled,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = androidx.compose.material3.ripple()
                                ) {
                                    when (item) {
                                        "DEL" -> onBackspace()
                                        "BIO" -> onBiometricPress()
                                        else -> onDigit(item)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when (item) {
                                "DEL" -> {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Backspace",
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                "BIO" -> {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Biometric",
                                        tint = Color(0xFF2E4F24),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                else -> {
                                    Text(
                                        text = item,
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
