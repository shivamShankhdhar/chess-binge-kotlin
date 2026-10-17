package bizora.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import bizora.app.data.repository.BizoraRepository
import bizora.app.ui.components.AdBanner
import bizora.app.ui.components.BizoraPullRefreshBox
import bizora.app.ui.components.HeaderBackgroundArt
import bizora.app.ui.components.MpinDots
import bizora.app.ui.components.MpinKeypad
import bizora.app.ui.dialogs.EmailOtpModal
import bizora.app.ui.theme.LocalBizoraColors
import bizora.app.utils.AdMobInterstitialHelper
import bizora.app.utils.BiometricHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Security & App Lock Screen with 1:1 design, inputs, bottom sheet modals,
 * and token parity with React Native's SecuritySettingsScreen.tsx.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    repository: BizoraRepository,
    onBack: () -> Unit,
    onLockAppNow: () -> Unit
) {
    val colors = LocalBizoraColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentUser by repository.currentUser.collectAsState()
    val mpinStatus by repository.mpinStatus.collectAsState()

    val hasBiometrics = remember { BiometricHelper.isBiometricAvailable(context) }
    val hasMpin = currentUser?.hasMpin == true ||
        currentUser?.appLockEnabled == true ||
        mpinStatus?.hasMpin == true ||
        mpinStatus?.appLockEnabled == true ||
        repository.hasMpin() ||
        repository.isAppLockEnabled()

    var isAppLockActive by remember(hasMpin) {
        mutableStateOf(hasMpin && (repository.isAppLockEnabled() || repository.hasMpin()))
    }
    var isBiometricEnabled by remember {
        mutableStateOf(mpinStatus?.biometricEnabled == true)
    }
    var isPullRefreshing by remember { mutableStateOf(false) }

    // Modals state
    var showDisableConfirmModal by remember { mutableStateOf(false) }
    var isDisabling by remember { mutableStateOf(false) }

    var showChooseLockTypeModal by remember { mutableStateOf(false) }
    var pendingLockType by remember { mutableStateOf("MPIN") } // "MPIN", "FACE_ID", "FINGERPRINT"

    var showEnableVerifyModal by remember { mutableStateOf(false) }
    var enableVerifyPin by remember { mutableStateOf("") }
    var enableVerifyError by remember { mutableStateOf<String?>(null) }
    var isVerifyingToEnable by remember { mutableStateOf(false) }

    var showMpinModal by remember { mutableStateOf(false) }
    var modalMode by remember { mutableStateOf("CHANGE") } // "CHANGE" or "RESET"
    var changeStep by remember { mutableStateOf("NEW") } // "CURRENT", "NEW", "CONFIRM"
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var changeError by remember { mutableStateOf<String?>(null) }
    var isChanging by remember { mutableStateOf(false) }
    var resetToken by remember { mutableStateOf<String?>(null) }

    // Email OTP state
    var showEmailOtpModal by remember { mutableStateOf(false) }
    var otpMaskedEmail by remember { mutableStateOf("") }
    var otpPurpose by remember { mutableStateOf("CHANGE_MPIN") } // "CHANGE_MPIN" or "RESET_MPIN"
    var isRequestingOtp by remember { mutableStateOf(false) }

    // Success bottom sheet
    var successModal by remember {
        mutableStateOf<Triple<Boolean, String, String>>(Triple(false, "", ""))
    }

    // Preload interstitial ad when visiting security settings
    LaunchedEffect(Unit) {
        AdMobInterstitialHelper.preload(context)
    }

    BackHandler { onBack() }

    fun openChangeMpinModal() {
        if (!hasMpin) {
            AdMobInterstitialHelper.showAdThenExecute(context) {
                showChooseLockTypeModal = true
            }
            return
        }
        if (isRequestingOtp) return
        isRequestingOtp = true
        otpPurpose = "CHANGE_MPIN"
        coroutineScope.launch {
            val res = repository.requestMpinOtp("CHANGE_MPIN")
            isRequestingOtp = false
            if (res.isSuccess) {
                otpMaskedEmail = res.getOrNull()?.maskedEmail ?: bizora.app.utils.maskEmail(currentUser?.email)
                showEmailOtpModal = true
            } else {
                Toast.makeText(
                    context,
                    res.exceptionOrNull()?.message ?: "Failed to send verification code to your email.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun openResetMpinModal() {
        if (isRequestingOtp) return
        isRequestingOtp = true
        otpPurpose = "RESET_MPIN"
        coroutineScope.launch {
            val res = repository.requestMpinOtp("RESET_MPIN")
            isRequestingOtp = false
            if (res.isSuccess) {
                otpMaskedEmail = res.getOrNull()?.maskedEmail ?: bizora.app.utils.maskEmail(currentUser?.email)
                showEmailOtpModal = true
            } else {
                Toast.makeText(
                    context,
                    res.exceptionOrNull()?.message ?: "Failed to send verification code to your email.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun handleAppLockMasterToggle(enable: Boolean) {
        if (!enable) {
            showDisableConfirmModal = true
        } else {
            // Show interstitial ad first to enable app lock settings, then open the options
            AdMobInterstitialHelper.showAdThenExecute(context) {
                if (hasMpin) {
                    enableVerifyPin = ""
                    enableVerifyError = null
                    showEnableVerifyModal = true
                } else {
                    showChooseLockTypeModal = true
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
    ) {
        HeaderBackgroundArt(modifier = Modifier.align(Alignment.TopEnd))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ─── Header (matching React Native Header) ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back button (38x38dp circle, surface bg, border 1dp)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .border(1.dp, colors.border, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onBack
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Security & App Lock",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                // Placeholder for balance
                Spacer(modifier = Modifier.size(38.dp))
            }

            // ─── Content ───
            BizoraPullRefreshBox(
                isRefreshing = isPullRefreshing,
                onRefresh = {
                    isPullRefreshing = true
                    coroutineScope.launch {
                        try {
                            repository.refreshSecurityStatusAwait()
                        } finally {
                            isPullRefreshing = false
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                // ─── Compact Hero Section: Master App Lock Status & Switch ───
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, colors.border),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isAppLockActive) Color(0xFFEBF1DE) else colors.surfaceMuted)
                                    .border(1.dp, if (isAppLockActive) Color(0xFFD8E2C4) else colors.border, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAppLockActive) Icons.Filled.Shield else Icons.Outlined.Shield,
                                    contentDescription = null,
                                    tint = if (isAppLockActive) colors.brandPrimary else colors.textSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = if (isAppLockActive) "App Lock Active" else "App Lock Disabled",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    letterSpacing = (-0.2).sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isAppLockActive) "Protected with 4-digit MPIN" else "Enable to protect your workplace",
                                    fontSize = 12.5.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Switch(
                            checked = isAppLockActive,
                            onCheckedChange = { handleAppLockMasterToggle(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.brandPrimary,
                                uncheckedThumbColor = Color(0xFF64748B),
                                uncheckedTrackColor = if (colors.isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                            )
                        )
                    }
                }

                // ─── Section: 3 Authentication Options ───
                Text(
                    text = "AUTHENTICATION METHODS (3 OPTIONS)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textSecondary,
                    letterSpacing = 0.8.sp
                )

                if (!isAppLockActive) {
                    // Disabled Notice Box
                    Surface(
                        color = colors.surface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { handleAppLockMasterToggle(true) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Turn on App Lock above to configure MPIN, Face ID, and Fingerprint.",
                                fontSize = 12.5.sp,
                                color = colors.textSecondary,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                // Grouped Card containing Option 1, 2, 3
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, colors.border),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // ── OPTION 1: 4-DIGIT MPIN ──
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.brandTint),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = colors.brandPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "1. 4-Digit MPIN",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )

                                        // Mini Badge
                                        Surface(
                                            color = if (hasMpin) Color(0xFFEAF7EE) else Color(0xFFF1F5F9),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(
                                                0.8.dp,
                                                if (hasMpin) Color(0xFFC7E9D1) else Color(0xFFE2E8F0)
                                            )
                                        ) {
                                            Text(
                                                text = if (hasMpin) "CONFIGURED" else "NOT SET",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (hasMpin) Color(0xFF15803D) else Color(0xFF64748B),
                                                letterSpacing = 0.5.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = if (hasMpin) "Primary passcode fallback for app lock" else "Set up a 4-digit PIN for passcode unlock",
                                        fontSize = 12.5.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            if (hasMpin) {
                                // Side-by-side action buttons: [Change MPIN] and [Reset]
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Change MPIN Button
                                    Surface(
                                        color = if (isAppLockActive) Color(0xFFEBF1DE) else Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, if (isAppLockActive) Color(0xFFD8E2C4) else Color(0xFFE2E8F0)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clickable(
                                                enabled = isAppLockActive && !isRequestingOtp,
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = { openChangeMpinModal() }
                                            )
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            if (isRequestingOtp && otpPurpose == "CHANGE_MPIN") {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    color = colors.brandPrimary,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = null,
                                                    tint = if (isAppLockActive) colors.brandPrimary else colors.textSecondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Change MPIN",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isAppLockActive) colors.brandPrimary else colors.textSecondary
                                                )
                                            }
                                        }
                                    }

                                    // Reset Button
                                    Surface(
                                        color = Color(0xFFF8FAFC),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier
                                            .weight(0.7f)
                                            .height(42.dp)
                                            .clickable(
                                                enabled = isAppLockActive && !isRequestingOtp,
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = { openResetMpinModal() }
                                            )
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            if (isRequestingOtp && otpPurpose == "RESET_MPIN") {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    color = colors.brandPrimary,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Outlined.LockReset,
                                                    contentDescription = null,
                                                    tint = colors.textSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = "Reset",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Full width setup button
                                Button(
                                    onClick = {
                                        AdMobInterstitialHelper.showAdThenExecute(context) {
                                            showChooseLockTypeModal = true
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.brandPrimary,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                ) {
                                    Text(
                                        text = "Set Up App Lock (Choose Method)",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

                        // ── OPTION 2: FACE ID ──
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.brandTint),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Face,
                                        contentDescription = null,
                                        tint = colors.brandPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "2. Face ID",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAppLockActive) colors.textPrimary else colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (!hasBiometrics) {
                                            "Not available or not enrolled on device"
                                        } else {
                                            "Unlock automatically with Face ID"
                                        },
                                        fontSize = 12.5.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            Switch(
                                checked = isAppLockActive && isBiometricEnabled && hasBiometrics,
                                enabled = isAppLockActive && hasBiometrics,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        val activity = context as? FragmentActivity
                                        if (activity != null) {
                                            BiometricHelper.showBiometricPrompt(
                                                activity = activity,
                                                title = "Confirm Face ID",
                                                subtitle = "Authenticate to enable Face ID unlock",
                                                onSuccess = {
                                                    isBiometricEnabled = true
                                                    coroutineScope.launch {
                                                        repository.setBiometricEnabled(true)
                                                    }
                                                },
                                                onError = { err ->
                                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    } else {
                                        isBiometricEnabled = false
                                        coroutineScope.launch {
                                            repository.setBiometricEnabled(false)
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = colors.brandPrimary,
                                    uncheckedThumbColor = Color(0xFF64748B),
                                    uncheckedTrackColor = if (colors.isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                                )
                            )
                        }

                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

                        // ── OPTION 3: FINGERPRINT ──
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFFAF7EE))
                                        .border(1.dp, Color(0xFFE8E1CE), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = colors.brandPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "3. Fingerprint",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAppLockActive) colors.textPrimary else colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (!hasBiometrics) {
                                            "Sensor not available or not enrolled"
                                        } else {
                                            "Touch device sensor for fingerprint unlock"
                                        },
                                        fontSize = 12.5.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            Switch(
                                checked = isAppLockActive && isBiometricEnabled && hasBiometrics,
                                enabled = isAppLockActive && hasBiometrics,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        val activity = context as? FragmentActivity
                                        if (activity != null) {
                                            BiometricHelper.showBiometricPrompt(
                                                activity = activity,
                                                title = "Confirm Fingerprint",
                                                subtitle = "Touch sensor to enable fingerprint unlock",
                                                onSuccess = {
                                                    isBiometricEnabled = true
                                                    coroutineScope.launch {
                                                        repository.setBiometricEnabled(true)
                                                    }
                                                },
                                                onError = { err ->
                                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    } else {
                                        isBiometricEnabled = false
                                        coroutineScope.launch {
                                            repository.setBiometricEnabled(false)
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = colors.brandPrimary,
                                    uncheckedThumbColor = Color(0xFF64748B),
                                    uncheckedTrackColor = if (colors.isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                                )
                            )
                        }
                    }
                }

                // ─── Section: TEST APP LOCK ───
                if (hasMpin) {
                    Text(
                        text = "TEST APP LOCK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textSecondary,
                        letterSpacing = 0.8.sp
                    )

                    Surface(
                        color = colors.surface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = isAppLockActive,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onLockAppNow
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.brandTint),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = colors.brandPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Lock App Now",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAppLockActive) colors.textPrimary else colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isAppLockActive) "Test lock screen and configured unlock methods" else "Enable App Lock above to test",
                                        fontSize = 12.5.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
            }

            // Sticky AdBanner at the bottom of Security & App Lock screen
            AdBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            )
        }
    }

    // ─────────────────────────────────────────────────────────────
    // BOTTOM SHEETS & MODALS (MATCHING REACT NATIVE 1:1)
    // ─────────────────────────────────────────────────────────────

    // 1. Confirm Disable App Lock Bottom Sheet
    if (showDisableConfirmModal) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showDisableConfirmModal = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = colors.canvas,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 10.dp)
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
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Red shield-off circle
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEE2E2))
                        .border(1.5.dp, Color(0xFFFECACA), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    color = Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "DISABLE APP LOCK",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFDC2626),
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Disable App Lock?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Bizora will no longer lock when you switch apps or lock your phone. Your attendance records and sensitive workplace details will be accessible without an MPIN or biometrics.",
                    fontSize = 13.5.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Destructive Disable Button
                Button(
                    onClick = {
                        isDisabling = true
                        coroutineScope.launch {
                            repository.setAppLockEnabled(false)
                            isDisabling = false
                            isAppLockActive = false
                            showDisableConfirmModal = false
                            successModal = Triple(
                                true,
                                "App Lock Disabled",
                                "App lock has been turned off. You can re-enable it at any time from this screen."
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isDisabling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Disable App Lock",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = { showDisableConfirmModal = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Keep App Lock Enabled",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }

    // 2. Choose App Lock Type Bottom Sheet
    if (showChooseLockTypeModal) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showChooseLockTypeModal = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = colors.canvas,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 10.dp)
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
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Shield-key circle
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEBF1DE))
                        .border(1.5.dp, Color(0xFFD8E2C4), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = colors.brandPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    color = colors.brandTint,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFD8E2C4))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = colors.brandPrimary,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "CHOOSE LOCK METHOD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.brandPrimary,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Select App Lock Type",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Choose how you want to authenticate when opening Bizora.",
                    fontSize = 13.5.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Option Card 1: 4-Digit MPIN
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, colors.border),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                pendingLockType = "MPIN"
                                showChooseLockTypeModal = false
                                modalMode = "CHANGE"
                                changeStep = "NEW"
                                currentPin = ""
                                newPin = ""
                                confirmPin = ""
                                changeError = null
                                resetToken = null
                                showMpinModal = true
                            }
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.brandTint),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = colors.brandPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "4-Digit MPIN",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Surface(
                                        color = colors.surfaceMuted,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(0.8.dp, colors.border)
                                    ) {
                                        Text(
                                            text = "STANDARD",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = colors.textSecondary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Fast, secure 4-digit numeric passcode to unlock",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Option Card 2: Face ID
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, colors.border),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                pendingLockType = "FACE_ID"
                                showChooseLockTypeModal = false
                                modalMode = "CHANGE"
                                changeStep = "NEW"
                                currentPin = ""
                                newPin = ""
                                confirmPin = ""
                                changeError = null
                                resetToken = null
                                showMpinModal = true
                            }
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.brandTint),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    tint = colors.brandPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Face ID",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Surface(
                                        color = Color(0xFFEAF7EE),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(0.8.dp, Color(0xFFC7E9D1))
                                    ) {
                                        Text(
                                            text = "FACE UNLOCK",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF15803D),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Unlock instantly using facial recognition",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Option Card 3: Fingerprint
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, colors.border),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                pendingLockType = "FINGERPRINT"
                                showChooseLockTypeModal = false
                                modalMode = "CHANGE"
                                changeStep = "NEW"
                                currentPin = ""
                                newPin = ""
                                confirmPin = ""
                                changeError = null
                                resetToken = null
                                showMpinModal = true
                            }
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFAF7EE))
                                    .border(1.dp, Color(0xFFE8E1CE), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = colors.brandPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Fingerprint",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Surface(
                                        color = Color(0xFFFEF3C7),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(0.8.dp, Color(0xFFFDE68A))
                                    ) {
                                        Text(
                                            text = "TOUCH SENSOR",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFB45309),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Unlock instantly with your touch sensor",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(
                    onClick = { showChooseLockTypeModal = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }

    // 3. Verify MPIN to Enable App Lock Full-Covered Screen
    if (showEnableVerifyModal) {
        BackHandler {
            showEnableVerifyModal = false
            enableVerifyPin = ""
            enableVerifyError = null
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.canvas
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
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Header Row with Close button on right
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Spacer(modifier = Modifier.size(38.dp))
                                Text(
                                    text = "Verify MPIN to Enable",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                IconButton(
                                    onClick = {
                                        showEnableVerifyModal = false
                                        enableVerifyPin = ""
                                        enableVerifyError = null
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(colors.surface)
                                        .border(1.dp, colors.border, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Enter your 4-digit security MPIN to turn on App Lock.",
                                fontSize = 14.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Error box or loading indicator
                            Box(
                                modifier = Modifier.height(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isVerifyingToEnable) {
                                    CircularProgressIndicator(
                                        color = colors.brandPrimary,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else if (enableVerifyError != null) {
                                    Text(
                                        text = enableVerifyError ?: "",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.danger,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // MpinDots
                            MpinDots(
                                filledCount = enableVerifyPin.length,
                                total = 4,
                                isError = enableVerifyError != null
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // "Forgot MPIN? Reset here" pill button
                            Surface(
                                color = Color(0xFFEBF1DE),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFD8E2C4)),
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        showEnableVerifyModal = false
                                        enableVerifyPin = ""
                                        enableVerifyError = null
                                        openResetMpinModal()
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.LockReset,
                                        contentDescription = null,
                                        tint = colors.brandPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Forgot MPIN? Reset here",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.brandPrimary
                                    )
                                }
                            }
                        }

                        // Keypad
                        MpinKeypad(
                            onDigit = { digit ->
                                if (!isVerifyingToEnable && enableVerifyPin.length < 4) {
                                    enableVerifyError = null
                                    val next = enableVerifyPin + digit
                                    enableVerifyPin = next
                                    if (next.length == 4) {
                                        isVerifyingToEnable = true
                                        coroutineScope.launch {
                                            val res = repository.verifyMpin(next)
                                            val ok = res.isSuccess && res.getOrDefault(false)
                                            isVerifyingToEnable = false
                                            if (ok) {
                                                repository.setAppLockEnabled(true)
                                                isAppLockActive = true
                                                showEnableVerifyModal = false
                                                enableVerifyPin = ""
                                                enableVerifyError = null
                                                successModal = Triple(
                                                    true,
                                                    "App Lock Enabled",
                                                    "Your 4-digit MPIN and biometrics are now actively protecting your account."
                                                )
                                            } else {
                                                enableVerifyError = "Incorrect MPIN. Please try again."
                                                enableVerifyPin = ""
                                            }
                                        }
                                    }
                                }
                            },
                            onBackspace = {
                                if (!isVerifyingToEnable && enableVerifyPin.isNotEmpty()) {
                                    enableVerifyError = null
                                    enableVerifyPin = enableVerifyPin.dropLast(1)
                                }
                            },
                            showBiometric = false,
                            showTopBorder = true,
                            keySize = 54.dp,
                            digitFontSize = 20.sp
                        )
                    }

                    // Banner Ad at the bottom in MPIN screen
                    AdBanner(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    // 4. Change / Reset / Setup MPIN Screen (Exact 8-State Setup UI)
    if (showMpinModal) {
        MpinSetupScreen(
            repository = repository,
            allowDismiss = true,
            onSuccess = {
                showMpinModal = false
                isAppLockActive = true
                coroutineScope.launch {
                    repository.refreshSecurityStatusAwait()
                }
            },
            onDismiss = {
                showMpinModal = false
            }
        )
    }

    // 5. Email OTP Modal (matching React Native EmailOtpModal.tsx 1:1)
    if (showEmailOtpModal) {
        EmailOtpModal(
            maskedEmail = otpMaskedEmail,
            purpose = otpPurpose,
            onVerifySuccess = { token ->
                resetToken = token
                showEmailOtpModal = false
                modalMode = if (otpPurpose == "CHANGE_MPIN") "CHANGE" else "RESET"
                changeStep = "NEW"
                currentPin = ""
                newPin = ""
                confirmPin = ""
                changeError = null
                showMpinModal = true
            },
            onRequestOtp = {
                val res = repository.requestMpinOtp(otpPurpose)
                if (res.isSuccess) {
                    val em = res.getOrNull()?.maskedEmail ?: bizora.app.utils.maskEmail(currentUser?.email)
                    otpMaskedEmail = em
                    Result.success(em)
                } else {
                    Result.failure(res.exceptionOrNull() ?: Exception("Failed to send OTP"))
                }
            },
            onVerifyOtp = { otpCode ->
                repository.verifyMpinOtp(otpCode, otpPurpose)
            },
            onDismiss = {
                showEmailOtpModal = false
                resetToken = null
            }
        )
    }

    // 6. Success Bottom Sheet Modal
    if (successModal.first) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { successModal = Triple(false, "", "") },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = colors.canvas,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 10.dp)
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
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Shield check icon circle (68x68dp, #EAF7EE bg, #C7E9D1 border)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEAF7EE))
                        .border(1.5.dp, Color(0xFFC7E9D1), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF15803D),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    color = Color(0xFFEAF7EE),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFC7E9D1))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "SECURITY ACTIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF15803D),
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = successModal.second,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = successModal.third,
                    fontSize = 13.5.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { successModal = Triple(false, "", "") },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.brandPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Done",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
