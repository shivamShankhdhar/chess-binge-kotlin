package bizora.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bizora.app.data.repository.BizoraRepository
import bizora.app.ui.components.BizoraSignInIcon
import bizora.app.ui.theme.LocalBizoraColors
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
    val colors = LocalBizoraColors.current
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
    val currentPinLength = if (isStep1) initialPin.length else confirmPin.length

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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ── Top Bar: [←] Back button and [B] Bizora ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (allowDismiss || uiState != SetupLockUiState.CREATE_EMPTY) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.surface)
                                .border(1.dp, colors.border, CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { handleBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(38.dp))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BizoraSignInIcon(size = 26.dp)
                        Text(
                            text = "Bizora",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            letterSpacing = (-0.3).sp
                        )
                    }

                    if (allowDismiss && showSetupLater && uiState != SetupLockUiState.SAVING && uiState != SetupLockUiState.ENABLED_SUCCESS) {
                        Text(
                            text = "Set up later",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E5924),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onDismiss() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.size(38.dp))
                    }
                }

                // ── Main Content Area ──
                if (uiState == SetupLockUiState.ENABLED_SUCCESS) {
                    // State 7: App lock enabled (Large checkmark + Done)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF2E4F24),
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        Text(
                            text = "App lock is on",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            letterSpacing = (-0.4).sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Use your MPIN to unlock Bizora.",
                            fontSize = 14.5.sp,
                            color = colors.textSecondary
                        )
                        }

                        Button(
                            onClick = onSuccess,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF264E24)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Done",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                } else {
                    // States 1-6 & 8: Steps 1 & 2 with Card + Keypad
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        Text(
                            text = if (isStep1) "Step 1 of 2" else "Step 2 of 2",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (isStep1) "Set up app lock" else "Confirm your MPIN",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            letterSpacing = (-0.4).sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (isStep1) "Create a 4-digit MPIN" else "Enter the same 4 digits again",
                            fontSize = 14.sp,
                            color = colors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // ── Rounded Card with 4 Input Boxes ──
                        Surface(
                            color = if (colors.isDark) Color(0xFF1B2218) else Color.White,
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF2A3326) else Color(0xFFE5E9DE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                SetupMpinBoxesRow(
                                    pinLength = currentPinLength,
                                    isMismatch = uiState == SetupLockUiState.MISMATCH,
                                    isFullFilled = uiState == SetupLockUiState.SAVING || uiState == SetupLockUiState.SAVE_ERROR,
                                    modifier = Modifier.graphicsLayer {
                                        translationX = shakeOffset.value
                                    }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Contextual message below boxes
                                when (uiState) {
                                    SetupLockUiState.CREATE_EMPTY, SetupLockUiState.CREATE_ENTERING -> {
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }

                                    SetupLockUiState.CONFIRM_EMPTY, SetupLockUiState.CONFIRM_ENTERING -> {
                                        Text(
                                            text = "Change MPIN",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF2E4F24),
                                            modifier = Modifier
                                                .clickable {
                                                    initialPin = ""
                                                    confirmPin = ""
                                                    uiState = SetupLockUiState.CREATE_EMPTY
                                                }
                                                .padding(vertical = 4.dp, horizontal = 8.dp)
                                        )
                                    }

                                    SetupLockUiState.MISMATCH -> {
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
                                                    text = "MPINs don't match. Try again.",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFDC2626)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Change MPIN",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF2E4F24),
                                                modifier = Modifier
                                                    .clickable {
                                                        initialPin = ""
                                                        confirmPin = ""
                                                        uiState = SetupLockUiState.CREATE_EMPTY
                                                    }
                                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                                            )
                                        }
                                    }

                                    SetupLockUiState.SAVING -> {
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
                                                text = "Enabling app lock...",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF2E4F24)
                                            )
                                        }
                                    }

                                    SetupLockUiState.SAVE_ERROR -> {
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
                                                    text = "Couldn't enable app lock",
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFD97706)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Please try again.",
                                                fontSize = 12.sp,
                                                color = colors.textSecondary
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Change MPIN",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF2E4F24),
                                                modifier = Modifier
                                                    .clickable {
                                                        initialPin = ""
                                                        confirmPin = ""
                                                        uiState = SetupLockUiState.CREATE_EMPTY
                                                    }
                                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = { saveMpin(initialPin) },
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

                                    SetupLockUiState.ENABLED_SUCCESS -> {}
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // ─── BOTTOM SECTION: Distinct White Container with Rounded Top Corners ───
                    val keypadDisabled = uiState == SetupLockUiState.SAVING ||
                            uiState == SetupLockUiState.SAVE_ERROR

                    Surface(
                        color = if (colors.isDark) Color(0xFF1B2218) else Color.White,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF2A3326) else Color(0xFFE8EDE1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(top = 4.dp, bottom = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 3x4 Clean Numeric Keypad
                            SetupNumericKeypad(
                                onDigit = { onDigitPress(it) },
                                onBackspace = { onBackspace() },
                                disabled = keypadDisabled,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Trust Badge: "Keep your MPIN private."
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
                                    text = "Keep your MPIN private.",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary.copy(alpha = 0.85f)
                                )
                            }

                        }
                    }
                }
            }
        }
    }
}

/**
 * 4 Rounded rectangular MPIN input boxes matching the provided setup mockup:
 * - Green border on the active box
 * - Solid dark green dots on filled boxes
 * - Light red background & red dots on mismatch error
 */
@Composable
private fun SetupMpinBoxesRow(
    pinLength: Int,
    isMismatch: Boolean,
    isFullFilled: Boolean,
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
            val isFilled = isFullFilled || i < pinLength
            val isFocus = !isFullFilled && !isMismatch && i == pinLength

            val boxBg = when {
                isMismatch -> Color(0xFFFDE8E8)
                isFocus -> if (colors.isDark) Color(0xFF1B2218) else Color.White
                colors.isDark -> Color(0xFF252A22)
                else -> Color(0xFFF0F4EB)
            }

            val boxBorder = when {
                isMismatch -> BorderStroke(1.dp, Color(0xFFFCA5A5))
                isFocus -> BorderStroke(1.5.dp, Color(0xFF2E5924))
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
                if (isMismatch) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDC2626))
                    )
                } else if (isFilled) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E4F24))
                    )
                }
            }
        }
    }
}

/**
 * 3x4 Clean Numeric Keypad matching the setup mockup design:
 * - Rounded soft-tinted key backgrounds (88x56dp, RoundedCornerShape(16.dp))
 * - Normal/Medium digits, clean Backspace key
 * - Subtle touch ripple feedback
 * - Dimmed alpha when disabled
 */
@Composable
private fun SetupNumericKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    disabled: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalBizoraColors.current
    val keypadRows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
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
