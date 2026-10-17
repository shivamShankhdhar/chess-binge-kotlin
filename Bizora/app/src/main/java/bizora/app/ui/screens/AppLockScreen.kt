package bizora.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import bizora.app.data.repository.BizoraRepository
import bizora.app.ui.components.*
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

    AppLockContent(
        uiState = uiState,
        pinLength = pin.length,
        firstName = firstName.ifBlank { "there" },
        lockMode = lockMode,
        isVerifying = isVerifying,
        resetErrorMessage = resetErrorMessage,
        cooldownSeconds = cooldownSeconds,
        shakeOffset = shakeOffset.value,
        onDigit = { onDigitPress(it) },
        onBackspace = { onBackspace() },
        onForgotMpin = { handleForgotMpin() },
        onRetry = {
            if (lastFailedPin.length == maxDigits) verifyMpinAttempt(lastFailedPin)
            else {
                pin = ""
                uiState = LockUiState.READY
            }
        },
        onCancelReset = {
            lockMode = AppLockMode.UNLOCK
            pin = ""
            resetNewPin = ""
            resetConfirmPin = ""
            resetToken = null
            resetErrorMessage = null
            uiState = LockUiState.READY
        },
        onBiometric = if (lockMode == AppLockMode.UNLOCK && hasBiometrics) ({ triggerBiometrics() }) else null
    )

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

@Composable
internal fun AppLockContent(
    uiState: LockUiState,
    pinLength: Int,
    firstName: String,
    lockMode: AppLockMode = AppLockMode.UNLOCK,
    isVerifying: Boolean = false,
    resetErrorMessage: String? = null,
    cooldownSeconds: Int = 0,
    shakeOffset: Float = 0f,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onForgotMpin: () -> Unit,
    onRetry: () -> Unit,
    onCancelReset: () -> Unit,
    onBiometric: (() -> Unit)? = null,
    adContent: @Composable () -> Unit = { MpinAdSlot() }
) {
    val locked = uiState == LockUiState.TEMPORARILY_LOCKED
    val resetting = lockMode != AppLockMode.UNLOCK
    val disabled = isVerifying || cooldownSeconds > 0 || uiState in listOf(
        LockUiState.CHECKING, LockUiState.UNLOCKED, LockUiState.TEMPORARILY_LOCKED, LockUiState.CONNECTION_ERROR
    )
    MpinPage {
        val compact = LocalCompactMpin.current
        MpinHeader(onBack = if (resetting && !isVerifying) onCancelReset else null)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(if (compact) 12.dp else 24.dp))
            MpinHeading(
                title = when (lockMode) {
                    AppLockMode.UNLOCK -> "Hi, $firstName"
                    AppLockMode.RESET_NEW -> "Set new MPIN"
                    AppLockMode.RESET_CONFIRM -> "Confirm your MPIN"
                },
                subtitle = when (lockMode) {
                    AppLockMode.UNLOCK -> "Enter your MPIN to continue"
                    AppLockMode.RESET_NEW -> "Create a 4-digit MPIN"
                    AppLockMode.RESET_CONFIRM -> "Enter the same 4 digits again"
                }
            )
            Spacer(Modifier.height(if (compact) 14.dp else 24.dp))
            MpinInputCard(locked = locked) {
                MpinSlots(
                    filled = if (uiState == LockUiState.UNLOCKED) 4 else pinLength,
                    error = uiState == LockUiState.WRONG_MPIN || (resetting && resetErrorMessage != null && pinLength == 4),
                    locked = locked,
                    focus = !disabled,
                    modifier = Modifier.graphicsLayer { translationX = shakeOffset }
                )
                Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (compact) 32.dp else 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (resetting) {
                        when {
                            isVerifying -> MpinMessage("Enabling app lock...", loading = true)
                            resetErrorMessage != null -> MpinMessage(resetErrorMessage, color = MpinStyle.Error, icon = Icons.Default.Error)
                            else -> MpinLink("Cancel reset", onCancelReset)
                        }
                    } else {
                        when (uiState) {
                            LockUiState.READY, LockUiState.ENTERING -> MpinLink("Forgot MPIN?", onForgotMpin)
                            LockUiState.CHECKING -> {
                                MpinMessage("Checking MPIN...", loading = true)
                            }
                            LockUiState.WRONG_MPIN -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    MpinMessage("Incorrect MPIN. Please try again.", color = MpinStyle.Error, icon = Icons.Default.Error)
                                    Spacer(Modifier.height(4.dp))
                                    MpinLink("Forgot MPIN?", onForgotMpin)
                                }
                            }
                            LockUiState.UNLOCKED -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    MpinMessage("MPIN verified", icon = Icons.Default.CheckCircle)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Opening your workspace...", fontSize = 12.sp, color = MpinStyle.Secondary)
                                }
                            }
                            LockUiState.TEMPORARILY_LOCKED -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(Icons.Default.Lock, null, tint = MpinStyle.Amber, modifier = Modifier.size(24.dp))
                                        Column {
                                            Text("Too many incorrect attempts", color = Color(0xFF8F4100), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            Spacer(Modifier.height(2.dp))
                                            Text("Try again in %02d:%02d".format(cooldownSeconds / 60, cooldownSeconds % 60), color = MpinStyle.Secondary, fontSize = 12.sp)
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    MpinLink("Forgot MPIN?", onForgotMpin)
                                }
                            }
                            LockUiState.CONNECTION_ERROR -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    MpinMessage("Unable to verify MPIN", color = MpinStyle.Amber, icon = Icons.Default.Error)
                                    Spacer(Modifier.height(3.dp))
                                    Text("Check your connection and try again.", color = MpinStyle.Secondary, fontSize = 11.5.sp)
                                    Spacer(Modifier.height(8.dp))
                                    MpinButton("Retry", onRetry)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f).heightIn(min = 12.dp))
        MpinKeypadPanel(
            onDigit = onDigit,
            onBackspace = onBackspace,
            disabled = disabled,
            trustText = "Your workspace, securely connected.",
            onBiometric = onBiometric,
            adContent = adContent
        )
    }
}
