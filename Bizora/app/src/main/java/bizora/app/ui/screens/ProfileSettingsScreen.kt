package bizora.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bizora.app.data.model.User
import bizora.app.data.model.Workplace
import bizora.app.data.repository.BizoraRepository
import bizora.app.ui.components.BizoraMark
import bizora.app.ui.components.UserAvatar
import bizora.app.ui.theme.LocalBizoraColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    repository: BizoraRepository,
    onBack: () -> Unit,
    onOpenSecuritySettings: () -> Unit,
    onToggleTheme: () -> Unit = {},
    isDark: Boolean = false,
    onSignOut: () -> Unit
) {
    val colors = LocalBizoraColors.current
    var showSignOutDialog by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    // Sign out confirmation dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = {
                Text(
                    text = "Sign out?",
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out? You will need to sign in again to access your workplaces.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.danger,
                        contentColor = Color.White
                    )
                ) {
                    Text("Sign out", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        containerColor = colors.canvas,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Settings",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            text = "Your account, preferences and app information",
                            fontSize = 14.sp,
                            color = colors.textSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        }
    ) { paddingValues ->
        SettingsScreenView(
            repository = repository,
            onOpenSecuritySettings = onOpenSecuritySettings,
            onSignOutClick = { showSignOutDialog = true },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun SettingsScreenView(
    repository: BizoraRepository,
    onOpenSecuritySettings: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalBizoraColors.current
    val context = LocalContext.current
    val currentUser by repository.currentUser.collectAsState()
    val workplaces by repository.workplaces.collectAsState()
    val mpinStatus by repository.mpinStatus.collectAsState()

    val hasMpin = currentUser?.hasMpin == true ||
        currentUser?.appLockEnabled == true ||
        mpinStatus?.hasMpin == true ||
        mpinStatus?.appLockEnabled == true ||
        repository.hasMpin() ||
        repository.isAppLockEnabled()
    val activeWorkplacesCount = workplaces.size

    var showAboutDialog by remember { mutableStateOf(false) }

    // About Bizora dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BizoraMark(size = 32.dp)
                    Text("About Bizora", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Bizora is an enterprise-grade smart attendance tracker and workplace management system featuring geofenced QR check-in, rotating anti-spoof codes, team management, and automated payroll reporting.",
                        fontSize = 13.5.sp,
                        color = colors.textSecondary,
                        lineHeight = 19.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version 1.0.0 (Android Native)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.brandPrimary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.brandPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Close", color = Color.White)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ─── Profile Card ───
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                UserAvatar(
                    name = currentUser?.name ?: "User",
                    size = 54.dp,
                    fontSize = 20.sp,
                    avatarUrl = currentUser?.avatarUrl
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentUser?.name ?: "User",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentUser?.email ?: "Signed in",
                        fontSize = 14.sp,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // ─── Section: Account & Profile ───
        SettingsSection(title = "Account & Profile") {
            SettingsRow(
                icon = Icons.Default.Person,
                label = "Full name",
                value = currentUser?.name ?: "Not provided"
            )

            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

            SettingsRow(
                icon = Icons.Default.Email,
                label = "Email address",
                value = currentUser?.email ?: "Not provided",
                actionIcon = Icons.Default.ContentCopy,
                onActionClick = {
                    val email = currentUser?.email
                    if (!email.isNullOrBlank()) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Email", email))
                        Toast.makeText(context, "Email address copied to clipboard.", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

            SettingsRow(
                icon = Icons.Default.Business,
                label = "Linked workplaces",
                value = "$activeWorkplacesCount active ${if (activeWorkplacesCount == 1) "workplace" else "workplaces"}"
            )
        }

        // ─── Section: Security & App Lock ───
        SettingsSection(title = "Security") {
            SettingsRow(
                icon = Icons.Default.Shield,
                label = "Security & app lock",
                value = if (hasMpin) "Protected (MPIN active)" else "MPIN, face and fingerprint",
                showChevron = true,
                onClick = onOpenSecuritySettings
            )
        }

        // ─── Section: About ───
        SettingsSection(title = "About") {
            SettingsRow(
                icon = Icons.Default.Info,
                label = "About Bizora",
                value = "Features, security overview & website",
                showChevron = true,
                onClick = { showAboutDialog = true }
            )

            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

            SettingsRow(
                icon = Icons.Default.Public,
                label = "Official website",
                value = "www.bizora.shivamshankhdhar.online",
                actionIcon = Icons.Default.OpenInNew,
                onActionClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.bizora.shivamshankhdhar.online"))
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
            )

            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

            SettingsRow(
                icon = Icons.Default.Lock,
                label = "Privacy policy",
                value = "AdMob disclosure, data usage & rights",
                actionIcon = Icons.Default.OpenInNew,
                onActionClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.bizora.shivamshankhdhar.online/privacy"))
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
            )

            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

            SettingsRow(
                icon = Icons.Default.Share,
                label = "Share app",
                value = "Share download link with your team",
                showChevron = true,
                onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Check out Bizora — smart attendance tracking, QR check-in & workplace management!\n\nWebsite: https://www.bizora.shivamshankhdhar.online"
                        )
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Bizora"))
                }
            )

            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

            SettingsRow(
                icon = Icons.Default.Code,
                label = "App version",
                value = "1.0.0"
            )
        }

        // ─── Section: Session ───
        SettingsSection(title = "Session") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSignOutClick() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.danger.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        tint = colors.danger,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sign out",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.danger
                    )
                    Text(
                        text = "Log out from your Bizora account",
                        fontSize = 13.5.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalBizoraColors.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 4.dp)
        )
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    label: String,
    value: String,
    actionIcon: ImageVector? = null,
    onActionClick: (() -> Unit)? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val colors = LocalBizoraColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceMuted),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.brandPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = value,
                    fontSize = 13.5.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (actionIcon != null) {
            IconButton(
                onClick = { onActionClick?.invoke() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = null,
                    tint = colors.brandPrimary,
                    modifier = Modifier.size(17.dp)
                )
            }
        } else if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
