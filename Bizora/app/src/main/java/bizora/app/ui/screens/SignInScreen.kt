package bizora.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bizora.app.R
import bizora.app.data.repository.BizoraRepository
import bizora.app.ui.components.BizoraSignInIcon
import bizora.app.ui.components.EmployeeBadgeIcon
import bizora.app.ui.components.GoogleLogo
import bizora.app.ui.theme.LocalBizoraColors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    pendingInviteCode: String? = null,
    repository: BizoraRepository? = null,
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
    var showTokenDialog by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf("") }
    var invitedTeamName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pendingInviteCode) {
        if (!pendingInviteCode.isNullOrBlank() && repository != null) {
            try {
                val res = repository.previewJoin(pendingInviteCode)
                if (res.isSuccess) {
                    val preview = res.getOrNull()
                    invitedTeamName = preview?.targetTeam?.name ?: preview?.workplaceName
                } else {
                    val uri = try { android.net.Uri.parse(pendingInviteCode) } catch (_: Exception) { null }
                    val qTeam = uri?.getQueryParameter("team") ?: uri?.getQueryParameter("teamCode")
                    if (!qTeam.isNullOrBlank()) {
                        invitedTeamName = qTeam
                    }
                }
            } catch (_: Exception) {}
        } else {
            invitedTeamName = null
        }
    }

    // Intercept hardware Back button to cancel loading immediately
    if (isLoading) {
        BackHandler {
            onCancelLoading()
        }
    }

    Surface(color = Color(0xFFFBFAF6), modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            EditorialLoginLayout(
                errorMessage = errorMessage,
                invitedTeamName = invitedTeamName,
                onDismissError = onDismissError,
                onGoogleSignIn = onGoogleSignIn,
                onPinLoginClick = onPinLoginClick,
                onContinueAsEmployer = onContinueAsEmployer,
                onReviewerLoginClick = onReviewerLoginClick,
                onOpenTokenDialog = { showTokenDialog = true }
            )

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

@Composable
private fun EditorialLoginLayout(
    errorMessage: String?,
    invitedTeamName: String? = null,
    onDismissError: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onPinLoginClick: () -> Unit,
    onContinueAsEmployer: () -> Unit,
    onReviewerLoginClick: () -> Unit,
    onOpenTokenDialog: () -> Unit
) {
    val reference = ImageBitmap.imageResource(R.drawable.signin_reference)
    val olive = Color(0xFF3D4C25)
    val ink = Color(0xFF17230F)
    val secondary = Color(0xFF74777C)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 26.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BizoraSignInIcon()
            Spacer(Modifier.width(5.dp))
            Text("Bizora", fontSize = 27.sp, fontWeight = FontWeight.SemiBold, color = ink)
        }

        Spacer(Modifier.height(22.dp))
        Text(
            text = "Good business\nstarts here.",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp),
            fontSize = 41.sp,
            lineHeight = 40.sp,
            letterSpacing = (-1.6).sp,
            fontWeight = FontWeight.Bold,
            color = ink
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "One place for your team\nand daily operations.",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 27.dp),
            fontSize = 18.5.sp,
            lineHeight = 22.sp,
            color = secondary
        )
        Spacer(Modifier.height(2.dp))

        // Draw only the artwork region of the supplied design, at the screen's actual width.
        Canvas(Modifier.fillMaxWidth().aspectRatio(853f / 600f)) {
            drawImage(
                image = reference,
                srcOffset = IntOffset(0, 520),
                srcSize = IntSize(853, 600),
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
            )
        }

        if (!errorMessage.isNullOrBlank()) {
            Surface(
                color = Color(0xFFFDE8E7),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 6.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(errorMessage, modifier = Modifier.weight(1f), color = Color(0xFF9B2828), fontSize = 13.sp)
                    IconButton(onClick = onDismissError, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss error")
                    }
                }
            }
        }

        if (!invitedTeamName.isNullOrBlank()) {
            Spacer(Modifier.height(14.dp))
            Surface(
                color = Color(0xFFF1F5E8),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFD6DEC3)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = olive.copy(alpha = 0.12f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = olive,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sign in first to join the team $invitedTeamName",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp,
                            color = ink,
                            lineHeight = 19.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "You'll be directly added to the team after sign in.",
                            fontSize = 12.sp,
                            color = olive.copy(alpha = 0.85f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onGoogleSignIn,
            colors = ButtonDefaults.buttonColors(containerColor = olive, contentColor = Color.White),
            shape = RoundedCornerShape(11.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp).height(56.dp)
        ) {
            Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(34.dp)) {
                Box(contentAlignment = Alignment.Center) { GoogleLogo(size = 21.dp) }
            }
            Spacer(Modifier.width(18.dp))
            Text("Continue with Google", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(10.dp))
        Text("Sign in or create an account in one step.", fontSize = 12.5.sp, color = secondary)

        Spacer(Modifier.height(31.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 29.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(Modifier.weight(1f), color = Color(0xFFAEB1B2))
            Text("Employee access", modifier = Modifier.padding(horizontal = 14.dp), fontSize = 12.sp, color = secondary)
            HorizontalDivider(Modifier.weight(1f), color = Color(0xFFAEB1B2))
        }
        Spacer(Modifier.height(25.dp))

        OutlinedButton(
            onClick = onPinLoginClick,
            border = BorderStroke(1.dp, olive),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFBFAF6), contentColor = ink),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp).height(53.dp)
        ) {
            EmployeeBadgeIcon(size = 29.dp, color = olive)
            Spacer(Modifier.width(14.dp))
            Text("Use employee ID + PIN", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        Text("Use the credentials shared by your employer.", fontSize = 12.5.sp, color = secondary)

        Spacer(Modifier.height(35.dp))
        Text("By continuing, you agree to our", fontSize = 11.5.sp, color = secondary)
        Spacer(Modifier.height(5.dp))
        Text("Terms of Service   ·   Privacy Policy", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = olive)

        Row(
            modifier = Modifier.padding(top = 24.dp, bottom = 22.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Employer Demo", modifier = Modifier.clickable(onClick = onContinueAsEmployer).padding(5.dp), fontSize = 11.sp, color = secondary)
            Text("·", color = secondary)
            Text("Demo Access", modifier = Modifier.clickable(onClick = onReviewerLoginClick).padding(5.dp), fontSize = 11.sp, color = secondary)
            Text("·", color = secondary)
            Text("Token", modifier = Modifier.clickable(onClick = onOpenTokenDialog).padding(5.dp), fontSize = 11.sp, color = secondary)
        }
    }
}
