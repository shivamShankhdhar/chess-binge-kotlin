package bizora.app.ui.components

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bizora.app.ui.theme.LocalBizoraColors

/**
 * 3x4 Tactile Keypad matching React Native's MpinKeypad.tsx:
 * - 62x62dp circular keys with subtle elevation and borders
 * - Top 1.5dp divider line
 * - Optional Biometric key (Fingerprint / Face ID / "Face / Touch")
 * - Backspace key
 * - Haptic feedback on tap
 */
@Composable
fun MpinKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    showBiometric: Boolean = false,
    biometricLabel: String = "Biometric",
    hasFaceId: Boolean = false,
    hasFingerprint: Boolean = false,
    onBiometricPress: (() -> Unit)? = null,
    disabled: Boolean = false,
    showTopBorder: Boolean = true,
    keySize: androidx.compose.ui.unit.Dp = 62.dp,
    digitFontSize: androidx.compose.ui.unit.TextUnit = 23.sp,
    modifier: Modifier = Modifier
) {
    val colors = LocalBizoraColors.current
    val view = LocalView.current
    val context = LocalContext.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getSystemService(Vibrator::class.java)
        } else null
    }

    fun triggerTapHaptic() {
        try {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(25)
            }
        } catch (_: Exception) {}
    }

    fun handlePress(digit: String) {
        if (disabled) return
        triggerTapHaptic()
        onDigit(digit)
    }

    fun handleBackspacePress() {
        if (disabled) return
        triggerTapHaptic()
        onBackspace()
    }

    fun handleBiometricClick() {
        if (disabled) return
        triggerTapHaptic()
        onBiometricPress?.invoke()
    }

    Column(
        modifier = modifier
            .width(280.dp)
            .padding(horizontal = 4.dp)
            .padding(top = 8.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showTopBorder) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(colors.border)
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Row 1: 1, 2, 3
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeyButton(digit = "1", disabled = disabled, keySize = keySize, digitFontSize = digitFontSize, onClick = { handlePress("1") })
            KeyButton(digit = "2", disabled = disabled, keySize = keySize, digitFontSize = digitFontSize, onClick = { handlePress("2") })
            KeyButton(digit = "3", disabled = disabled, keySize = keySize, digitFontSize = digitFontSize, onClick = { handlePress("3") })
        }

        // Row 2: 4, 5, 6
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeyButton(digit = "4", disabled = disabled, keySize = keySize, digitFontSize = digitFontSize, onClick = { handlePress("4") })
            KeyButton(digit = "5", disabled = disabled, keySize = keySize, digitFontSize = digitFontSize, onClick = { handlePress("5") })
            KeyButton(digit = "6", disabled = disabled, keySize = keySize, digitFontSize = digitFontSize, onClick = { handlePress("6") })
        }

        // Row 3: 7, 8, 9
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeyButton(digit = "7", disabled = disabled, keySize = keySize, digitFontSize = digitFontSize, onClick = { handlePress("7") })
            KeyButton(digit = "8", disabled = disabled, keySize = keySize, digitFontSize = digitFontSize, onClick = { handlePress("8") })
            KeyButton(digit = "9", disabled = disabled, keySize = keySize, digitFontSize = digitFontSize, onClick = { handlePress("9") })
        }

        // Row 4: Biometric / Spacer, 0, Backspace
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBiometric) {
                Box(
                    modifier = Modifier
                        .size(keySize)
                        .clip(CircleShape)
                        .background(colors.brandTint)
                        .border(1.dp, colors.border, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = !disabled,
                            onClick = { handleBiometricClick() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (hasFaceId && hasFingerprint) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    tint = colors.brandPrimary,
                                    modifier = Modifier.size(17.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = colors.brandPrimary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        } else if (hasFaceId) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Face ID",
                                tint = colors.brandPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Fingerprint",
                                tint = colors.brandPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        val displayLabel = when {
                            hasFaceId && hasFingerprint -> "Face / Touch"
                            biometricLabel.isNotBlank() && biometricLabel != "None" -> biometricLabel
                            else -> "Biometric"
                        }

                        Text(
                            text = displayLabel,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.brandPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 1.5.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(keySize))
            }

            KeyButton(digit = "0", disabled = disabled, keySize = keySize, digitFontSize = digitFontSize, onClick = { handlePress("0") })

            // Backspace Button
            Box(
                modifier = Modifier
                    .size(keySize)
                    .shadow(elevation = 2.dp, shape = CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(1.dp, colors.border, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !disabled,
                        onClick = { handleBackspacePress() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun KeyButton(
    digit: String,
    disabled: Boolean,
    keySize: androidx.compose.ui.unit.Dp = 62.dp,
    digitFontSize: androidx.compose.ui.unit.TextUnit = 23.sp,
    onClick: () -> Unit
) {
    val colors = LocalBizoraColors.current
    Box(
        modifier = Modifier
            .size(keySize)
            .shadow(
                elevation = 2.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = Color(0x151B2210),
                spotColor = Color(0x151B2210)
            )
            .clip(CircleShape)
            .background(colors.surface)
            .border(1.dp, colors.border, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !disabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit,
            fontSize = digitFontSize,
            fontWeight = FontWeight.SemiBold,
            color = if (disabled) colors.textPrimary.copy(alpha = 0.5f) else colors.textPrimary,
            textAlign = TextAlign.Center
        )
    }
}
