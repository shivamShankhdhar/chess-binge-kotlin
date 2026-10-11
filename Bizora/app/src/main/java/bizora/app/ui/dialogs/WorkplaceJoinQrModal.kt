package bizora.app.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import bizora.app.data.model.Team
import bizora.app.data.repository.BizoraRepository
import bizora.app.ui.components.ModalCloseButton
import bizora.app.ui.components.QrCodeView
import bizora.app.ui.theme.LocalBizoraColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkplaceJoinQrModal(
    workplaceId: String,
    workplaceName: String,
    workplaceCode: String = workplaceId,
    teams: List<Team> = emptyList(),
    initialTeamId: String? = null,
    repository: BizoraRepository? = null,
    onCreateTeam: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val colors = LocalBizoraColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun dismissSmoothly(action: (() -> Unit)? = null) {
        coroutineScope.launch {
            try {
                sheetState.hide()
            } finally {
                onDismiss()
                action?.invoke()
            }
        }
    }

    val cleanCode = remember(workplaceCode, workplaceId) {
        val raw = workplaceCode.ifBlank { workplaceId }
        if (raw.startsWith("BWP-", ignoreCase = true)) raw.uppercase()
        else {
            val clean = (if (raw.startsWith("wp-", ignoreCase = true)) raw.removePrefix("wp-") else raw).uppercase()
            if (clean.length >= 7) "BWP-${clean.take(4)}-${clean.drop(4).take(3)}" else clean
        }
    }

    val defaultTeamCode = remember(cleanCode) {
        val clean = cleanCode.replace("BWP-", "").replace("-", "")
        if (clean.length >= 6) "TM${clean.take(6)}" else "TM${clean.padEnd(6, '0')}"
    }

    val displayTeams = remember(teams, cleanCode) {
        if (teams.isNotEmpty()) teams
        else listOf(
            Team(
                id = "default",
                workplaceId = workplaceId,
                name = "General",
                teamCode = defaultTeamCode,
                color = "#5B692D",
                isDefault = true
            )
        )
    }

    var selectedTeamId by remember(initialTeamId, displayTeams) {
        mutableStateOf(initialTeamId ?: displayTeams.find { it.isDefault }?.id ?: displayTeams.firstOrNull()?.id)
    }
    val selectedTeam = remember(selectedTeamId, displayTeams) {
        displayTeams.find { it.id == selectedTeamId } ?: displayTeams.firstOrNull()
    }

    val effectiveCode = selectedTeam?.displayCode ?: cleanCode
    val teamCode = effectiveCode

    var dynamicJoinLink by remember { mutableStateOf<String?>(null) }
    var dynamicQrPayload by remember { mutableStateOf<String?>(null) }
    var dynamicToken by remember { mutableStateOf<String?>(null) }
    var dynamicExpiresAt by remember { mutableStateOf<String?>(null) }
    var isLoadingQr by remember { mutableStateOf(false) }
    var isRotatingQr by remember { mutableStateOf(false) }

    val fallbackLink = if (selectedTeam != null) {
        "https://www.bizora.shivamshankhdhar.online/join?teamCode=$effectiveCode&token=$cleanCode"
    } else {
        "https://www.bizora.shivamshankhdhar.online/join?code=$cleanCode"
    }

    val joinLink = dynamicJoinLink ?: fallbackLink

    var selectedMode by remember { mutableStateOf<String?>("LINK") } // "LINK" or "QR", default to link/QR mode like RN
    var isLinkCopied by remember { mutableStateOf(false) }
    var isCodeCopied by remember { mutableStateOf(false) }
    var rotateCounter by remember { mutableIntStateOf(1) }

    LaunchedEffect(initialTeamId) {
        if (initialTeamId != null) {
            selectedTeamId = initialTeamId
        }
    }

    LaunchedEffect(selectedTeamId, workplaceId) {
        isLinkCopied = false
        isCodeCopied = false
        if (repository != null) {
            isLoadingQr = true
            val targetTeamId = selectedTeam?.id?.takeIf { it != "default" }
            val res = repository.getWorkplaceJoinQr(workplaceId, targetTeamId)
            if (res.isSuccess) {
                val data = res.getOrNull()
                dynamicJoinLink = data?.get("joinLink") as? String
                dynamicQrPayload = data?.get("qrPayload") as? String
                dynamicToken = data?.get("qrToken") as? String
                dynamicExpiresAt = data?.get("expiresAt") as? String
            }
            isLoadingQr = false
        }
    }

    fun rotateQrAndLink() {
        coroutineScope.launch {
            isRotatingQr = true
            if (repository != null) {
                val targetTeamId = selectedTeam?.id?.takeIf { it != "default" }
                val res = repository.rotateWorkplaceJoinQr(workplaceId, targetTeamId)
                if (res.isSuccess) {
                    val data = res.getOrNull()
                    dynamicJoinLink = data?.get("joinLink") as? String
                    dynamicQrPayload = data?.get("qrPayload") as? String
                    dynamicToken = data?.get("qrToken") as? String
                    dynamicExpiresAt = data?.get("expiresAt") as? String
                    rotateCounter++
                    Toast.makeText(context, "New single-use invite link generated! Valid for 24h.", Toast.LENGTH_SHORT).show()
                } else {
                    rotateCounter++
                    Toast.makeText(context, "Invite link updated", Toast.LENGTH_SHORT).show()
                }
            } else {
                rotateCounter++
                Toast.makeText(context, "Invite link updated", Toast.LENGTH_SHORT).show()
            }
            isRotatingQr = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row: "Invite to Team" + Close (X) button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.brandTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = colors.brandPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Invite to Team",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }

                ModalCloseButton(
                    onClick = { dismissSmoothly() }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // TARGET TEAM SELECTOR STRIP
            if (displayTeams.isNotEmpty()) {
                Text(
                    text = "TARGET TEAM FOR NEW EMPLOYEES",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(displayTeams) { team ->
                        val isSelected = selectedTeamId == team.id
                        val teamColor = try {
                            Color(android.graphics.Color.parseColor(team.color))
                        } catch (_: Exception) {
                            colors.brandPrimary
                        }
                        Surface(
                            color = if (isSelected) teamColor.copy(alpha = 0.15f) else colors.surfaceMuted,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) teamColor else colors.border),
                            modifier = Modifier.clickable { selectedTeamId = team.id }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(teamColor))
                                Text(
                                    text = team.name,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) colors.textPrimary else colors.textSecondary
                                )
                                Text(
                                    text = "(${team.memberCount})",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }

                    item {
                        Surface(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, colors.brandPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable {
                                dismissSmoothly { onCreateTeam() }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = colors.brandPrimary, modifier = Modifier.size(13.dp))
                                Text("Team", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.brandPrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Auto-assignment explanation banner + Team Code pill
                Surface(
                    color = colors.brandTint.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = colors.brandPrimary, modifier = Modifier.size(15.dp))
                            Text(
                                text = if (selectedTeam != null) "Employees will join ${selectedTeam.name} team." else "Employees join the default team.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.brandPrimary
                            )
                        }
                        if (selectedTeam != null) {
                            Surface(
                                color = colors.surface,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.8.dp, colors.brandPrimary.copy(alpha = 0.5f)),
                                modifier = Modifier.clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("${selectedTeam.name} Team Code", selectedTeam.displayCode))
                                    Toast.makeText(context, "${selectedTeam.name} team code copied: ${selectedTeam.displayCode}", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "#${selectedTeam.displayCode}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.brandPrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = colors.brandPrimary, modifier = Modifier.size(10.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (selectedMode == null) {
                // Starting Mode Selection Screen (2 Cards)
                // Intro Banner
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Grow your team",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Choose how you'd like to invite members. You review each request before they join.",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card 1: Invite via link (with "Quickest" badge)
                Surface(
                    color = colors.surfaceMuted.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMode = "LINK" }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.brandTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = colors.brandPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (selectedTeam != null) "Invite to ${selectedTeam.name} link" else "Invite via link",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colors.brandPrimary)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Quickest",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (selectedTeam != null) "Share a direct joining link for ${selectedTeam.name} team." else "Share a direct joining link via WhatsApp, SMS, or email.",
                                fontSize = 12.5.sp,
                                color = colors.textSecondary,
                                lineHeight = 17.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Card 2: Generate QR code
                Surface(
                    color = colors.surfaceMuted.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMode = "QR" }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.brandTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = colors.brandPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Generate QR code",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Display a scannable QR code on screen or print it for in-person joining.",
                                fontSize = 12.5.sp,
                                color = colors.textSecondary,
                                lineHeight = 17.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                // Active Mode View (Workplace Card + Mode Selector Tabs + Content)
                Surface(
                    color = colors.surfaceMuted.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.brandTint),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = colors.brandPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = workplaceName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Direct invitation",
                                    fontSize = 11.5.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.brandTint)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = colors.brandPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Direct Join · No Approval",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.brandPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Selector Tabs: [Invite link] vs [Join QR code]
                Surface(
                    color = colors.surfaceMuted,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedMode == "LINK") colors.surface else Color.Transparent)
                                .clickable { selectedMode = "LINK" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = if (selectedMode == "LINK") colors.brandPrimary else colors.textSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Invite link",
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedMode == "LINK") FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedMode == "LINK") colors.textPrimary else colors.textSecondary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedMode == "QR") colors.surface else Color.Transparent)
                                .clickable { selectedMode = "QR" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    tint = if (selectedMode == "QR") colors.brandPrimary else colors.textSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Join QR code",
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedMode == "QR") FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedMode == "QR") colors.textPrimary else colors.textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedMode == "LINK") {
                    // Link View
                    Text(
                        text = if (selectedTeam != null) "${selectedTeam.name} team invite link" else "Workplace invite link",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        color = colors.surfaceMuted,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = joinLink,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary,
                                maxLines = 2,
                                softWrap = true,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Bizora Invite Link", joinLink))
                                    isLinkCopied = true
                                    val copyMsg = if (selectedTeam != null) "${selectedTeam.name} team invite link copied." else "Workplace invite link copied."
                                    Toast.makeText(context, copyMsg, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLinkCopied) Icons.Default.Check else Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = if (isLinkCopied) colors.brandPrimary else colors.textSecondary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Bizora Invite Link", joinLink))
                                isLinkCopied = true
                                val copyMsg = if (selectedTeam != null) "${selectedTeam.name} team invite link copied." else "Workplace invite link copied."
                                Toast.makeText(context, copyMsg, Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = if (isLinkCopied) Icons.Default.Check else Icons.Outlined.ContentCopy,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isLinkCopied) "Copied!" else "Copy Link",
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = {
                                val targetTeamText = if (selectedTeam != null) "the ${selectedTeam.name} team at \"$workplaceName\"" else "\"$workplaceName\""
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Join $targetTeamText on Bizora directly with this invite link (valid for 24 hours):\n\n$joinLink\n\nSign in to join directly with no manager approval required!"
                                    )
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Team Invitation"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Share Invitation",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        color = colors.brandTint.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = colors.brandPrimary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 1.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Anyone joining with this link or QR code joins directly with NO approval needed. The link is valid for 24 hours and will automatically rotate/delete once an employee joins to prevent unauthorized reuse.",
                                fontSize = 12.sp,
                                color = colors.textPrimary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                } else {
                    // QR Code View
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(modifier = Modifier.padding(16.dp)) {
                                val qrPayload = dynamicQrPayload ?: joinLink
                                QrCodeView(
                                    content = qrPayload,
                                    size = 180.dp
                                )
                            }
                        }

                        if (selectedTeam != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = colors.brandTint,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val teamColor = try {
                                        Color(android.graphics.Color.parseColor(selectedTeam.color))
                                    } catch (_: Exception) {
                                        colors.brandPrimary
                                    }
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(teamColor))
                                    Text(
                                        text = "Assigns directly to: ${selectedTeam.name} team",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.brandPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Primary: Team Code Card (or Workplace Code if no team selected)
                        Surface(
                            color = colors.brandTint.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.brandPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (selectedTeam != null) "${selectedTeam.name} Team Code" else "Workplace Code",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.brandPrimary
                                        )
                                        if (selectedTeam != null) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(colors.brandPrimary.copy(alpha = 0.15f))
                                                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                            ) {
                                                Text(
                                                    text = "DIRECT JOIN",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.brandPrimary
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "# $effectiveCode",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.brandPrimary,
                                        letterSpacing = 1.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Team Code", effectiveCode))
                                        val copyLabel = if (selectedTeam != null) "${selectedTeam.name} team code" else "Workplace code"
                                        Toast.makeText(context, "$copyLabel copied: $effectiveCode", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, colors.brandPrimary.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = null,
                                        tint = colors.brandPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", fontSize = 12.sp, color = colors.brandPrimary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        if (selectedTeam != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            // Secondary reference row for Workplace Code
                            Surface(
                                color = colors.surfaceMuted,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.8.dp, colors.border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Workplace Code: #$cleanCode",
                                        fontSize = 11.5.sp,
                                        color = colors.textSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Copy",
                                        fontSize = 11.5.sp,
                                        color = colors.brandPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Workplace Code", cleanCode))
                                            Toast.makeText(context, "Workplace code copied: $cleanCode", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {                            OutlinedButton(
                                onClick = { rotateQrAndLink() },
                                enabled = !isRotatingQr,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isRotatingQr) "Rotating..." else "Rotate Link/QR",
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }

                            Button(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        val targetTeamText = if (selectedTeam != null) "the ${selectedTeam.name} team at \"$workplaceName\"" else "\"$workplaceName\""
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Join $targetTeamText on Bizora directly with this invite link (valid for 24 hours):\n\n$joinLink\n\nSign in to join directly with no manager approval required!"
                                        )
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Team Invitation"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Share Code",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Switch / Back to Options Link
                TextButton(
                    onClick = { selectedMode = null },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "← Back to invitation options",
                        fontSize = 12.5.sp,
                        color = colors.brandPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
