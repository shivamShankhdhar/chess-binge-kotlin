package bizora.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bizora.app.R
import bizora.app.data.repository.BizoraRepository
import bizora.app.ui.components.BizoraEditorialMonogram
import bizora.app.ui.theme.LocalBizoraColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinLoginScreen(
    repository: BizoraRepository? = null,
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val colors = LocalBizoraColors.current
    val coroutineScope = rememberCoroutineScope()
    var employeeId by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    var isEmployeeIdFocused by remember { mutableStateOf(false) }
    var isPinFocused by remember { mutableStateOf(false) }

    val canvasColor = if (colors.isDark) Color(0xFF141A10) else Color(0xFFFAFBF7)
    val cardBackground = if (colors.isDark) Color(0xFF1F2818) else Color.White
    val cardBorder = if (colors.isDark) Color(0xFF334027) else Color(0xFFE5EBE0)
    val inputBorderDefault = if (colors.isDark) Color(0xFF38472C) else Color(0xFFD1D5DB)
    val inputBorderFocused = if (colors.isDark) Color(0xFF8BAA58) else Color(0xFF4A5C28)
    val oliveBrand = if (colors.isDark) Color(0xFFA6C475) else Color(0xFF364522)
    val oliveButton = if (colors.isDark) Color(0xFF556A30) else Color(0xFF4A5C28)

    Surface(
        color = canvasColor,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Top Navigation: Back to sign-in
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBack)
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = oliveBrand,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Back to sign-in",
                    color = oliveBrand,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Header Section: Left Branding & Copy + Right Character Illustration
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Left Column
                Column(
                    modifier = Modifier
                        .weight(1.15f)
                        .padding(end = 4.dp)
                ) {
                    // Bizora Brand Badge + Name
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF3D4E26)),
                            contentAlignment = Alignment.Center
                        ) {
                            BizoraEditorialMonogram(
                                size = 20.dp,
                                primaryColor = Color.White,
                                amberColor = Color(0xFFF2994A)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Bizora",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = oliveBrand,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Employee sign-in",
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (colors.isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
                        letterSpacing = (-0.6).sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Use the ID and PIN shared by your employer.",
                        fontSize = 14.5.sp,
                        color = if (colors.isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280),
                        lineHeight = 21.sp
                    )
                }

                // Right Column: Employee Badge Character
                Box(
                    modifier = Modifier
                        .weight(0.85f)
                        .height(170.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Box(
                        modifier = Modifier
                            .width(145.dp)
                            .height(165.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 72.dp,
                                    topEnd = 72.dp,
                                    bottomStart = 16.dp,
                                    bottomEnd = 16.dp
                                )
                            )
                            .background(Color(0xFFF2EFE8))
                    ) {
                        Image(
                            painter = painterResource(R.drawable.pin_employee_character),
                            contentDescription = "Employee with ID badge",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Form Card
            Surface(
                color = cardBackground,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, cardBorder),
                shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Employee ID Label
                    Text(
                        text = "Employee ID",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (colors.isDark) Color(0xFFF3F4F6) else Color(0xFF111827)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Employee ID Input Field with Person Icon + Vertical Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (colors.isDark) Color(0xFF182013) else Color(0xFFFAFBFA))
                            .border(
                                width = 1.2.dp,
                                color = if (isEmployeeIdFocused) inputBorderFocused else inputBorderDefault,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = if (isEmployeeIdFocused) inputBorderFocused else Color(0xFF6B7280),
                                modifier = Modifier.size(20.dp)
                            )

                            // Subtle vertical separator
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(if (colors.isDark) Color(0xFF334027) else Color(0xFFE5E7EB))
                            )

                            Box(modifier = Modifier.weight(1f)) {
                                if (employeeId.isEmpty()) {
                                    Text(
                                        text = "e.g. EMP001",
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 15.sp
                                    )
                                }
                                BasicTextField(
                                    value = employeeId,
                                    onValueChange = {
                                        employeeId = it.uppercase()
                                        errorMessage = null
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = if (colors.isDark) Color.White else Color(0xFF111827),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = SolidColor(inputBorderFocused),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Characters,
                                        imeAction = ImeAction.Next
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { isEmployeeIdFocused = it.isFocused }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // PIN Label
                    Text(
                        text = "PIN",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (colors.isDark) Color(0xFFF3F4F6) else Color(0xFF111827)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // PIN Input Field with Lock Icon + Vertical Divider + Eye Toggle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (colors.isDark) Color(0xFF182013) else Color(0xFFFAFBFA))
                            .border(
                                width = 1.2.dp,
                                color = if (isPinFocused) inputBorderFocused else inputBorderDefault,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = if (isPinFocused) inputBorderFocused else Color(0xFF6B7280),
                                modifier = Modifier.size(20.dp)
                            )

                            // Subtle vertical separator
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(if (colors.isDark) Color(0xFF334027) else Color(0xFFE5E7EB))
                            )

                            Box(modifier = Modifier.weight(1f)) {
                                if (pin.isEmpty()) {
                                    Text(
                                        text = "Enter 4–6 digit PIN",
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 15.sp
                                    )
                                }
                                BasicTextField(
                                    value = pin,
                                    onValueChange = {
                                        val digitsOnly = it.filter { ch -> ch.isDigit() }
                                        if (digitsOnly.length <= 6) {
                                            pin = digitsOnly
                                            errorMessage = null
                                        }
                                    },
                                    singleLine = true,
                                    visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                                    textStyle = TextStyle(
                                        color = if (colors.isDark) Color.White else Color(0xFF111827),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = SolidColor(inputBorderFocused),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.NumberPassword,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (employeeId.isBlank() || pin.length < 4) {
                                                errorMessage = "Enter your employee ID and a 4–6 digit PIN."
                                            } else {
                                                onLoginSuccess()
                                            }
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { isPinFocused = it.isFocused }
                                )
                            }

                            // Eye visibility toggle
                            IconButton(
                                onClick = { showPin = !showPin },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (showPin) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (showPin) "Hide PIN" else "Show PIN",
                                    tint = Color(0xFF4B5563),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Error Message
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = colors.dangerTint,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = errorMessage!!,
                                    color = colors.danger,
                                    fontSize = 13.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap here to preview employee dashboard →",
                                    color = oliveBrand,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { onLoginSuccess() }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Deep Olive Submit Button: Sign in ->
                    Button(
                        onClick = {
                            if (employeeId.isBlank() || pin.length < 4) {
                                errorMessage = "Enter your employee ID and a 4–6 digit PIN."
                            } else if (repository != null) {
                                coroutineScope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    try {
                                        val result = repository.loginWithPin(employeeId, pin)
                                        if (result.isSuccess) {
                                            onLoginSuccess()
                                        } else {
                                            errorMessage = result.exceptionOrNull()?.message ?: "Login failed. Check your credentials."
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = e.localizedMessage ?: "Login failed"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            } else {
                                onLoginSuccess()
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = oliveButton,
                            contentColor = Color.White,
                            disabledContainerColor = oliveButton.copy(alpha = 0.6f),
                            disabledContentColor = Color.White.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Sign in",
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Forgot your PIN? link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Forgot your PIN?",
                            color = oliveBrand,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { showHelpDialog = true }
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notice Banner: Your employer may need to approve your first sign-in
            Surface(
                color = if (colors.isDark) Color(0xFF28351F) else Color(0xFFEEF3E6),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    0.8.dp,
                    if (colors.isDark) Color(0xFF384A2C) else Color(0xFFE2EAD6)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = oliveButton,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Your employer may need to approve your first sign-in.",
                        color = if (colors.isDark) Color(0xFFD2DEC0) else Color(0xFF374151),
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Footer: Secure access to your workplace
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Secure access to your workplace",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (showHelpDialog) {
        val helpSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showHelpDialog = false },
            sheetState = helpSheetState,
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
                    text = "Forgot your PIN?",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Ask your employer or workplace admin to reset your PIN from their Employees management tab in the Employer dashboard.",
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showHelpDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = oliveButton),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Got it", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
