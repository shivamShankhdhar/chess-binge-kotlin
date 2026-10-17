package bizora.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
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
import bizora.app.data.repository.BizoraRepository
import bizora.app.ui.components.*
import bizora.app.utils.BiometricHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SetupLockUiState {
    CREATE_EMPTY,      // State 1: Step 1 of 2 - Create MPIN (0 digits)
    CREATE_ENTERING,   // State 2: Step 1 of 2 - Create MPIN (1-3 digits)
    CONFIRM_EMPTY,     // State 3: Step 2 of 2 - Confirm MPIN (0 digits)
    CONFIRM_ENTERING,  // State 4: Step 2 of 2 - Confirm MPIN (1-3 digits)
    MISMATCH,          // State 5: MPIN mismatch error
    SAVING,            // State 6: Saving app lock with spinner
    ENABLED_SUCCESS,   // State 7: App lock is on (check icon + Done)
    SAVE_ERROR         // State 8: Couldn't save error with Retry
}

/**
 * Dedicated MPIN & App Lock Setup Screen matching the exact 8 states:
 * 1. Create MPIN — Empty
 * 2. Create MPIN — Entering
 * 3. Confirm MPIN — Empty
 * 4. Confirm MPIN — Entering
 * 5. MPIN mismatch
 * 6. Saving app lock
 * 7. App lock enabled
 * 8. Couldn't save
 * Uses Google AdMob banner at bottom and supports seamless cancellation / back.
 */
@Composable
fun MpinSetupScreen(
    repository: BizoraRepository,
    allowDismiss: Boolean = true,
    showSetupLater: Boolean = false,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var uiState by remember { mutableStateOf(SetupLockUiState.CREATE_EMPTY) }
    var initialPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    val maxDigits = 4
    val hasBiometrics = remember { BiometricHelper.isBiometricAvailable(context) }

    // Shake animation for mismatch error
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(uiState) {
        if (uiState == SetupLockUiState.MISMATCH) {
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

    fun handleBack() {
        when (uiState) {
            SetupLockUiState.CONFIRM_EMPTY,
            SetupLockUiState.CONFIRM_ENTERING,
            SetupLockUiState.MISMATCH,
            SetupLockUiState.SAVE_ERROR -> {
                // Return to Step 1
                initialPin = ""
                confirmPin = ""
                uiState = SetupLockUiState.CREATE_EMPTY
            }
            SetupLockUiState.SAVING -> {
                // Do not interrupt saving
            }
            SetupLockUiState.ENABLED_SUCCESS -> {
                onSuccess()
            }
            else -> {
                if (allowDismiss) onDismiss()
            }
        }
    }

    BackHandler(enabled = allowDismiss || uiState != SetupLockUiState.CREATE_EMPTY) {
        handleBack()
    }

    fun saveMpin(pinToSave: String) {
        uiState = SetupLockUiState.SAVING
        coroutineScope.launch {
            val result = repository.setupMpin(pinToSave, enableBiometric = hasBiometrics)
            if (result.isSuccess) {
                repository.setAppLockEnabled(true)
                uiState = SetupLockUiState.ENABLED_SUCCESS
            } else {
                uiState = SetupLockUiState.SAVE_ERROR
            }
        }
    }

    fun onDigitPress(digit: String) {
        if (uiState == SetupLockUiState.SAVING || uiState == SetupLockUiState.ENABLED_SUCCESS) return

        // If previously in error state, start fresh with the new digit in confirm step
        if (uiState == SetupLockUiState.MISMATCH || uiState == SetupLockUiState.SAVE_ERROR) {
            confirmPin = digit
            uiState = SetupLockUiState.CONFIRM_ENTERING
            return
        }

        // STEP 1: CREATE MPIN
        if (uiState == SetupLockUiState.CREATE_EMPTY || uiState == SetupLockUiState.CREATE_ENTERING) {
            if (initialPin.length < maxDigits) {
                val next = initialPin + digit
                initialPin = next
                if (next.length == maxDigits) {
                    uiState = SetupLockUiState.CREATE_ENTERING
                    coroutineScope.launch {
                        delay(180)
                        confirmPin = ""
                        uiState = SetupLockUiState.CONFIRM_EMPTY
                    }
                } else {
                    uiState = SetupLockUiState.CREATE_ENTERING
                }
            }
            return
        }

        // STEP 2: CONFIRM MPIN
        if (uiState == SetupLockUiState.CONFIRM_EMPTY || uiState == SetupLockUiState.CONFIRM_ENTERING) {
            if (confirmPin.length < maxDigits) {
                val next = confirmPin + digit
                confirmPin = next
                if (next.length == maxDigits) {
                    if (next == initialPin) {
                        saveMpin(next)
                    } else {
                        uiState = SetupLockUiState.MISMATCH
                    }
                } else {
                    uiState = SetupLockUiState.CONFIRM_ENTERING
                }
            }
        }
    }

    fun onBackspace() {
        if (uiState == SetupLockUiState.SAVING || uiState == SetupLockUiState.ENABLED_SUCCESS) return

        if (uiState == SetupLockUiState.MISMATCH || uiState == SetupLockUiState.SAVE_ERROR) {
            confirmPin = ""
            uiState = SetupLockUiState.CONFIRM_EMPTY
            return
        }

        // Step 1
        if (uiState == SetupLockUiState.CREATE_ENTERING) {
            val next = initialPin.dropLast(1)
            initialPin = next
            uiState = if (next.isEmpty()) SetupLockUiState.CREATE_EMPTY else SetupLockUiState.CREATE_ENTERING
            return
        }

        // Step 2
        if (uiState == SetupLockUiState.CONFIRM_ENTERING) {
            val next = confirmPin.dropLast(1)
            confirmPin = next
            uiState = if (next.isEmpty()) SetupLockUiState.CONFIRM_EMPTY else SetupLockUiState.CONFIRM_ENTERING
            return
        } else if (uiState == SetupLockUiState.CONFIRM_EMPTY) {
            // Tapping backspace on empty confirm goes back to Step 1
            initialPin = ""
            confirmPin = ""
            uiState = SetupLockUiState.CREATE_EMPTY
        }
    }

    val isStep1 = uiState == SetupLockUiState.CREATE_EMPTY || uiState == SetupLockUiState.CREATE_ENTERING
    MpinSetupContent(
        uiState = uiState,
        pinLength = if (isStep1) initialPin.length else confirmPin.length,
        showBack = false,
        showSetupLater = allowDismiss && showSetupLater,
        shakeOffset = shakeOffset.value,
        onBack = { handleBack() },
        onSetupLater = onDismiss,
        onDigit = { onDigitPress(it) },
        onBackspace = { onBackspace() },
        onChangeMpin = {
            initialPin = ""
            confirmPin = ""
            uiState = SetupLockUiState.CREATE_EMPTY
        },
        onRetry = { saveMpin(initialPin) },
        onDone = onSuccess
    )
}

/** Presentation is separate from saving so every reference state can be previewed. */
@Composable
internal fun MpinSetupContent(
    uiState: SetupLockUiState,
    pinLength: Int,
    showBack: Boolean = false,
    showSetupLater: Boolean = false,
    shakeOffset: Float = 0f,
    onBack: () -> Unit,
    onSetupLater: () -> Unit,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onChangeMpin: () -> Unit,
    onRetry: () -> Unit,
    onDone: () -> Unit,
    adContent: @Composable () -> Unit = { MpinAdSlot() }
) {
    val isStep1 = uiState == SetupLockUiState.CREATE_EMPTY || uiState == SetupLockUiState.CREATE_ENTERING
    val saving = uiState == SetupLockUiState.SAVING
    val success = uiState == SetupLockUiState.ENABLED_SUCCESS
    val mismatch = uiState == SetupLockUiState.MISMATCH
    val saveError = uiState == SetupLockUiState.SAVE_ERROR

    MpinPage {
        val compact = LocalCompactMpin.current
        MpinHeader(
            onBack = if (showBack && !saving) onBack else null,
            onSetupLater = if (showSetupLater && !saving && !success) onSetupLater else null
        )
        if (success) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(88.dp).background(Color(0xFFEAF0DC), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MpinStyle.Green, modifier = Modifier.size(48.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("App lock is on", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MpinStyle.Ink)
                    Spacer(Modifier.height(8.dp))
                    Text("Use your MPIN to unlock Bizora.", fontSize = 15.sp, color = MpinStyle.Secondary)
                }
            }
            MpinButton("Done", onDone, Modifier.padding(horizontal = 24.dp).padding(bottom = 16.dp))
            MpinTrustFooter(text = null, adContent = adContent)
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(if (compact) 12.dp else 24.dp))
                MpinHeading(
                    title = if (isStep1) "Set up app lock" else "Confirm your MPIN",
                    subtitle = if (isStep1) "Create a 4-digit MPIN" else "Enter the same 4 digits again",
                    step = if (isStep1) "Step 1 of 2" else "Step 2 of 2"
                )
                Spacer(Modifier.height(if (compact) 14.dp else 24.dp))
                MpinInputCard {
                    MpinSlots(
                        filled = pinLength,
                        error = mismatch,
                        focus = !saving && !saveError,
                        modifier = Modifier.graphicsLayer { translationX = shakeOffset }
                    )
                    Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = if (compact) 32.dp else 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isStep1 -> {
                                // Spacing preserved so keypad placement remains rock-solid
                            }
                            saving -> {
                                MpinMessage("Enabling app lock...", loading = true)
                            }
                            saveError -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    MpinMessage("Couldn't enable app lock", color = MpinStyle.Amber, icon = Icons.Default.Error)
                                    Spacer(Modifier.height(2.dp))
                                    Text("Please try again.", fontSize = 12.sp, color = MpinStyle.Secondary)
                                    Spacer(Modifier.height(4.dp))
                                    MpinLink("Change MPIN", onChangeMpin)
                                    Spacer(Modifier.height(8.dp))
                                    MpinButton("Retry", onRetry)
                                }
                            }
                            else -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (mismatch) {
                                        MpinMessage("MPINs don't match. Try again.", color = MpinStyle.Error, icon = Icons.Default.Error)
                                        Spacer(Modifier.height(4.dp))
                                    }
                                    MpinLink("Change MPIN", onChangeMpin)
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
                disabled = saving || saveError,
                trustText = "Keep your MPIN private.",
                adContent = adContent
            )
        }
    }
}
