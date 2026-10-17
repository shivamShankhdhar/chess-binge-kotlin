package bizora.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bizora.app.data.model.AttendanceStatus
import bizora.app.data.model.EmployeeItem
import bizora.app.data.model.RosterItem
import bizora.app.data.model.Team
import bizora.app.ui.components.BizoraPullRefreshBox
import bizora.app.ui.components.EmptyState
import bizora.app.ui.components.EmptyTeamIllustration
import bizora.app.ui.theme.LocalBizoraColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailsScreen(
    team: Team,
    employees: List<EmployeeItem>,
    roster: List<RosterItem> = emptyList(),
    workplaceName: String,
    workplaceCode: String,
    onBack: () -> Unit,
    onEmployeeClick: (EmployeeItem) -> Unit,
    onAddEmployeeClick: (String) -> Unit,
    onOpenJoinQrClick: (String) -> Unit,
    onRefresh: (suspend () -> Unit)? = null
) {
    BackHandler { onBack() }

    val colors = LocalBizoraColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isPullRefreshing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Parse team color
    val teamColor = remember(team.color) {
        try {
            Color(android.graphics.Color.parseColor(team.color))
        } catch (_: Exception) {
            colors.brandPrimary
        }
    }

    // Resolve team members: match employees by teamId or teamName, or fallback to team.members
    val teamEmployees = remember(team, employees) {
        val matched = employees.filter {
            it.teamId == team.id ||
            it.teamName.equals(team.name, ignoreCase = true) ||
            team.members.any { m -> m.id == it.id || (it.employeeCode.isNotBlank() && m.employeeCode.equals(it.employeeCode, ignoreCase = true)) }
        }
        if (matched.isNotEmpty()) {
            matched
        } else {
            // Map TeamMemberSummary fallback
            team.members.map { tm ->
                EmployeeItem(
                    id = tm.id,
                    name = tm.name,
                    employeeCode = tm.employeeCode ?: "",
                    avatarUrl = tm.avatarUrl,
                    status = tm.status,
                    teamId = team.id,
                    teamName = team.name
                )
            }
        }
    }

    // Filter by search query
    val filteredEmployees = remember(teamEmployees, searchQuery) {
        if (searchQuery.isBlank()) teamEmployees
        else {
            val q = searchQuery.trim().lowercase()
            teamEmployees.filter {
                it.name.lowercase().contains(q) ||
                it.employeeCode.lowercase().contains(q) ||
                (it.invitedEmail != null && it.invitedEmail.lowercase().contains(q))
            }
        }
    }

    // Attendance stats for this team
    val presentCount = remember(teamEmployees, roster) {
        teamEmployees.count { emp ->
            val r = roster.find { it.memberId == emp.id || it.employeeCode.equals(emp.employeeCode, ignoreCase = true) }
            r?.status == AttendanceStatus.PRESENT
        }
    }

    val activeCount = remember(teamEmployees) {
        teamEmployees.count { it.status.equals("ACTIVE", ignoreCase = true) }
    }

    BizoraPullRefreshBox(
        isRefreshing = isPullRefreshing,
        onRefresh = {
            if (onRefresh != null) {
                coroutineScope.launch {
                    isPullRefreshing = true
                    try { onRefresh() } finally { isPullRefreshing = false }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Surface(
            color = colors.canvas,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = onBack,
                            shape = CircleShape,
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(teamColor)
                                )
                                Text(
                                    text = team.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (team.isDefault) {
                                    Surface(
                                        color = colors.brandTint,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "Default",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.brandPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = workplaceName,
                                fontSize = 12.5.sp,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Quick Add button in top right
                    Button(
                        onClick = { onAddEmployeeClick(team.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add Member",
                            tint = colors.textInverse,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Add Member",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textInverse
                        )
                    }
                }

                HorizontalDivider(color = colors.border.copy(alpha = 0.6f))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Team Identity & Code Banner Card
                    item {
                        Surface(
                            color = colors.surface,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, colors.border),
                            shadowElevation = 0.5.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = team.name,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                        if (!team.description.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = team.description,
                                                fontSize = 13.5.sp,
                                                color = colors.textSecondary,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(teamColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Groups,
                                            contentDescription = null,
                                            tint = teamColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Team Code and Share Pill
                                Surface(
                                    color = colors.surfaceMuted,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, colors.border),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "TEAM CODE",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textSecondary,
                                                letterSpacing = 0.6.sp
                                            )
                                            Text(
                                                text = team.displayCode,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = colors.brandPrimary,
                                                letterSpacing = 0.5.sp
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            // Copy button
                                            Surface(
                                                color = colors.surface,
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, colors.border),
                                                modifier = Modifier.clickable {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("Team Code", team.displayCode))
                                                    Toast.makeText(context, "Team code copied: ${team.displayCode}", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = "Copy",
                                                        tint = colors.textPrimary,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = "Copy",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = colors.textPrimary
                                                    )
                                                }
                                            }

                                            // QR Code / Invite Button
                                            Surface(
                                                color = colors.brandTint,
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, colors.brandPrimary.copy(alpha = 0.3f)),
                                                modifier = Modifier.clickable {
                                                    onOpenJoinQrClick(team.id)
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.QrCode2,
                                                        contentDescription = "Team QR",
                                                        tint = colors.brandPrimary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = "Team QR",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.brandPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Metrics Tiles Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TeamStatCard(
                                title = "Total Members",
                                value = "${teamEmployees.size}",
                                subtitle = "In this team",
                                modifier = Modifier.weight(1f)
                            )
                            TeamStatCard(
                                title = "Present Today",
                                value = "$presentCount",
                                subtitle = "${if (teamEmployees.isNotEmpty()) (presentCount * 100 / teamEmployees.size) else 0}% attendance",
                                modifier = Modifier.weight(1f)
                            )
                            TeamStatCard(
                                title = "Active Status",
                                value = "$activeCount",
                                subtitle = "Active members",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Search Bar & Employee Count Header
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Employees in ${team.name} (${teamEmployees.size})",
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Search bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text("Search members by name or ID...", fontSize = 13.5.sp, color = colors.textSecondary)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear",
                                                tint = colors.textSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = colors.surface,
                                    unfocusedContainerColor = colors.surface,
                                    focusedBorderColor = colors.brandPrimary,
                                    unfocusedBorderColor = colors.border
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Employees List or Empty State
                    if (filteredEmployees.isEmpty()) {
                        item {
                            Surface(
                                color = colors.surface,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    EmptyState(
                                        illustration = { EmptyTeamIllustration(size = 80.dp) },
                                        title = if (searchQuery.isNotBlank()) "No Matching Employees" else "No Employees in this Team",
                                        description = if (searchQuery.isNotBlank()) "Try searching for a different name or employee ID." else "Add existing employees or invite new team members to join this team.",
                                        actionLabel = if (searchQuery.isNotBlank()) "Clear Search" else "Add Employee to Team",
                                        onAction = {
                                            if (searchQuery.isNotBlank()) searchQuery = ""
                                            else onAddEmployeeClick(team.id)
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredEmployees, key = { it.id }) { employee ->
                            val rosterItem = roster.find {
                                it.memberId == employee.id || it.employeeCode.equals(employee.employeeCode, ignoreCase = true)
                            }
                            TeamEmployeeRow(
                                employee = employee,
                                attendanceStatus = rosterItem?.status,
                                checkInTime = rosterItem?.checkInTime,
                                onClick = { onEmployeeClick(employee) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamStatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalBizoraColors.current
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.5.sp,
                color = colors.brandPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TeamEmployeeRow(
    employee: EmployeeItem,
    attendanceStatus: AttendanceStatus?,
    checkInTime: String?,
    onClick: () -> Unit
) {
    val colors = LocalBizoraColors.current

    val (badgeText, badgeBg, badgeTextColor) = when (attendanceStatus) {
        AttendanceStatus.PRESENT -> Triple(
            if (checkInTime != null) "In: $checkInTime" else "Present",
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32)
        )
        AttendanceStatus.HALF_DAY -> Triple("Half Day", Color(0xFFFFF8E1), Color(0xFFF57F17))
        AttendanceStatus.ABSENT -> Triple("Absent", Color(0xFFFFEBEE), Color(0xFFC62828))
        AttendanceStatus.LEAVE -> Triple("Leave", Color(0xFFE3F2FD), Color(0xFF1565C0))
        AttendanceStatus.PENDING -> Triple("Pending", Color(0xFFFFF3E0), Color(0xFFE65100))
        else -> Triple(
            if (employee.status.equals("ACTIVE", ignoreCase = true)) "Active" else employee.status.lowercase().replaceFirstChar { it.uppercase() },
            colors.surfaceMuted,
            colors.textSecondary
        )
    }

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Avatar initials circle
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(colors.brandTint),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = employee.name
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .take(2)
                        .joinToString("")
                        .uppercase()

                    Text(
                        text = initials.ifBlank { "E" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.brandPrimary
                    )
                }

                Column {
                    Text(
                        text = employee.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = colors.surfaceMuted,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = employee.employeeCode.ifBlank { "EMP" },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }

                        if (!employee.invitedEmail.isNullOrBlank()) {
                            Text(
                                text = employee.invitedEmail,
                                fontSize = 11.5.sp,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status Badge
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeTextColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.textSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
