package bizora.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import bizora.app.data.model.AttendanceRecord
import bizora.app.data.model.AttendanceStatus
import bizora.app.data.model.EmployeeItem
import bizora.app.ui.components.BizoraPullRefreshBox
import bizora.app.ui.components.ModalCloseButton
import bizora.app.ui.components.UserAvatar
import bizora.app.ui.theme.LocalBizoraColors
import bizora.app.utils.ReportFormat
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private enum class EmployeeDetailTab(val label: String) {
    PROFILE("Profile"),
    ATTENDANCES("Attendances"),
    REPORTS("Reports")
}

@Composable
fun EmployeeDetailsScreen(
    employee: EmployeeItem,
    workplaceName: String,
    workplaceCode: String,
    onBack: () -> Unit,
    onChangePinClick: () -> Unit,
    onDeactivateClick: () -> Unit,
    onExportReportClick: (EmployeeItem, ReportFormat) -> Unit,
    isDemo: Boolean = false,
    records: List<AttendanceRecord> = emptyList(),
    onRefresh: (suspend () -> Unit)? = null
) {
    val colors = LocalBizoraColors.current
    val coroutineScope = rememberCoroutineScope()
    var isPullRefreshing by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(EmployeeDetailTab.PROFILE) }
    var showFormatSelectorModal by remember { mutableStateOf(false) }
    var selectedExportFormat by remember { mutableStateOf(ReportFormat.EXCEL) }

    // Only generate sample attendance records for this employee if it's the demo login
    val attendanceRecords = remember(employee.id, records, isDemo) {
        if (records.isNotEmpty()) {
            records
        } else if (isDemo) {
            val now = LocalDate.now()
            (0..14).map { daysAgo ->
                val date = now.minusDays(daysAgo.toLong())
                val isWeekend = date.dayOfWeek.value >= 6
                val status = when {
                    isWeekend -> AttendanceStatus.NOT_MARKED
                    daysAgo == 3 -> AttendanceStatus.HALF_DAY
                    daysAgo == 7 -> AttendanceStatus.ABSENT
                    else -> AttendanceStatus.PRESENT
                }
                AttendanceRecord(
                    id = "rec-${employee.id}-$daysAgo",
                    attendanceDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    status = status,
                    checkInTime = if (status == AttendanceStatus.PRESENT || status == AttendanceStatus.HALF_DAY) "2026-10-05T09:15:00" else null,
                    checkOutTime = if (status == AttendanceStatus.PRESENT) "2026-10-05T18:15:00" else if (status == AttendanceStatus.HALF_DAY) "2026-10-05T13:30:00" else null
                )
            }
        } else {
            emptyList()
        }
    }

    val presentDays = attendanceRecords.count { it.status == AttendanceStatus.PRESENT }
    val totalWorkDays = attendanceRecords.count { it.status != AttendanceStatus.NOT_MARKED }
    val halfDays = attendanceRecords.count { it.status == AttendanceStatus.HALF_DAY }
    val absentDays = attendanceRecords.count { it.status == AttendanceStatus.ABSENT }
    val attendancePercentage = if (totalWorkDays > 0) ((presentDays + halfDays * 0.5f) / totalWorkDays * 100f).roundToInt() else 0

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Dark/Themed Banner Header matching RN
        Surface(
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to employees",
                        tint = colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Employee details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3-Tab Bar matching RN (Profile, Attendances, Reports)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .background(colors.surfaceMuted, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            EmployeeDetailTab.values().forEach { tab ->
                val isSelected = activeTab == tab
                val icon = when (tab) {
                    EmployeeDetailTab.PROFILE -> Icons.Default.Person
                    EmployeeDetailTab.ATTENDANCES -> Icons.Default.DateRange
                    EmployeeDetailTab.REPORTS -> Icons.Default.Assessment
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) colors.surface else Color.Transparent)
                        .clickable { activeTab = tab }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.label,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) colors.brandPrimary else colors.textSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tab.label,
                            fontSize = 13.5.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) colors.brandPrimary else colors.textSecondary
                        )
                    }
                }
            }
        }

        // Tab Content
        BizoraPullRefreshBox(
            isRefreshing = isPullRefreshing,
            onRefresh = {
                if (onRefresh != null) {
                    isPullRefreshing = true
                    coroutineScope.launch {
                        try {
                            onRefresh()
                        } finally {
                            isPullRefreshing = false
                        }
                    }
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeTab) {
                EmployeeDetailTab.PROFILE -> EmployeeProfileTabContent(
                    employee = employee,
                    onChangePinClick = onChangePinClick,
                    onDeactivateClick = onDeactivateClick
                )
                EmployeeDetailTab.ATTENDANCES -> EmployeeAttendancesTabContent(
                    employee = employee,
                    records = attendanceRecords,
                    presentDays = presentDays,
                    totalDays = totalWorkDays,
                    halfDays = halfDays,
                    absentDays = absentDays,
                    percentage = attendancePercentage
                )
                EmployeeDetailTab.REPORTS -> EmployeeReportsTabContent(
                    employee = employee,
                    records = attendanceRecords,
                    onExportClick = { showFormatSelectorModal = true }
                )
            }
        }

        if (showFormatSelectorModal) {
            EmployeeReportFormatModal(
                employee = employee,
                selectedFormat = selectedExportFormat,
                onFormatSelected = { selectedExportFormat = it },
                onConfirmExport = {
                    showFormatSelectorModal = false
                    onExportReportClick(employee, selectedExportFormat)
                },
                onDismiss = { showFormatSelectorModal = false }
            )
        }
    }
}

// -------------------------------------------------------------------------------------
// 1. PROFILE TAB
// -------------------------------------------------------------------------------------
@Composable
private fun EmployeeProfileTabContent(
    employee: EmployeeItem,
    onChangePinClick: () -> Unit,
    onDeactivateClick: () -> Unit
) {
    val colors = LocalBizoraColors.current
    val todayFormatted = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM"))

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Card 1: Profile Summary Card
        item {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, colors.border),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        name = employee.name,
                        size = 56.dp,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = employee.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = employee.employeeCode,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.brandPrimary
                            )
                            if (!employee.teamName.isNullOrBlank()) {
                                Text(
                                    text = "• ${employee.teamName}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = employee.invitedEmail ?: "Email not provided",
                            fontSize = 12.5.sp,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE6F7ED))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = employee.status.uppercase(),
                            color = Color(0xFF16A34A),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Card 2: Details Table
        item {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, colors.border),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    DetailRow(
                        icon = Icons.Default.Person,
                        label = "Employee ID",
                        value = employee.employeeCode,
                        colors = colors
                    )
                    HorizontalDivider(color = colors.border.copy(alpha = 0.6f))
                    DetailRow(
                        icon = Icons.Default.Groups,
                        label = "Team / Squad",
                        value = employee.teamName ?: "General",
                        colors = colors
                    )
                    HorizontalDivider(color = colors.border.copy(alpha = 0.6f))
                    DetailRow(
                        icon = Icons.Default.Email,
                        label = "Email address",
                        value = employee.invitedEmail ?: "Not provided",
                        colors = colors
                    )
                    HorizontalDivider(color = colors.border.copy(alpha = 0.6f))
                    DetailRow(
                        icon = Icons.Default.Shield,
                        label = "Status",
                        value = employee.status,
                        colors = colors,
                        isPill = true
                    )
                    HorizontalDivider(color = colors.border.copy(alpha = 0.6f))
                    DetailRow(
                        icon = Icons.Default.CalendarMonth,
                        label = "Joined",
                        value = if (!employee.joinedAt.isNullOrBlank()) employee.joinedAt.take(10) else "Active Member",
                        colors = colors
                    )
                }
            }
        }

        // Card 3: Today's Attendance Times Card
        item {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, colors.border),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = colors.brandPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Attendance",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                        }
                        Text(
                            text = todayFormatted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AttendanceCell(
                            label = "CHECK IN",
                            value = "09:15 AM",
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(colors.border)
                        )
                        AttendanceCell(
                            label = "CHECK OUT",
                            value = "06:15 PM",
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(colors.border)
                        )
                        AttendanceCell(
                            label = "HOURS WORKED",
                            value = "9h 00m",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Card 4: Action Buttons Row (Change MPIN & Deactivate Employee)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onChangePinClick,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.2.dp, colors.brandPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.surface),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = colors.brandPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Change MPIN",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.brandPrimary
                    )
                }

                Button(
                    onClick = onDeactivateClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier
                        .weight(1.1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonRemove,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Deactivate",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    colors: bizora.app.ui.theme.BizoraColors,
    isPill: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                fontSize = 13.5.sp,
                color = colors.textSecondary
            )
        }
        if (isPill) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE6F7ED))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = value.uppercase(),
                    color = Color(0xFF16A34A),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Text(
                text = value,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AttendanceCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalBizoraColors.current
    Column(
        modifier = modifier.padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary
        )
    }
}

// -------------------------------------------------------------------------------------
// 2. ATTENDANCES TAB
// -------------------------------------------------------------------------------------
@Composable
private fun EmployeeAttendancesTabContent(
    employee: EmployeeItem,
    records: List<AttendanceRecord>,
    presentDays: Int,
    totalDays: Int,
    halfDays: Int,
    absentDays: Int,
    percentage: Int
) {
    val colors = LocalBizoraColors.current
    val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM yyyy"))

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Attendance Performance Metric Card (matching RN Svg Circle Gauge)
        item {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, colors.border),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.brandPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = null,
                                    tint = colors.brandPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Attendance Performance",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceMuted)
                                .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = colors.brandPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentMonth,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Gauge Row: Left circular arc, Right stats col
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Circular Gauge
                        Box(
                            modifier = Modifier.size(96.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val trackColor = colors.border
                            val strokeColor = if (percentage >= 80) Color(0xFF16A34A) else if (percentage >= 60) Color(0xFFD97706) else Color(0xFFDC2626)

                            Canvas(modifier = Modifier.size(88.dp)) {
                                drawArc(
                                    color = trackColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = strokeColor,
                                    startAngle = -90f,
                                    sweepAngle = (percentage / 100f) * 360f,
                                    useCenter = false,
                                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$percentage%",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "PRESENT",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Stats Column
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "DAYS RATIO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary,
                                    letterSpacing = 0.5.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFE6F7ED))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (percentage >= 80) "Excellent" else "Good",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF16A34A)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$presentDays",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF16A34A)
                                )
                                Text(
                                    text = " / ",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = "$totalDays",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = " days present",
                                    fontSize = 11.5.sp,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Mini horizontal progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(colors.border)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(percentage / 100f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (percentage >= 80) Color(0xFF16A34A) else Color(0xFFD97706))
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Chips Row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MiniChip(label = "$presentDays Present", color = Color(0xFF16A34A), bg = Color(0xFFE6F7ED))
                                if (absentDays > 0) {
                                    MiniChip(label = "$absentDays Absent", color = Color(0xFFDC2626), bg = Color(0xFFFEE2E2))
                                }
                                if (halfDays > 0) {
                                    MiniChip(label = "$halfDays Half-day", color = colors.textSecondary, bg = colors.surfaceMuted)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Full attendance history
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Full attendance history",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceMuted)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${records.size} records",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary
                    )
                }
            }
        }

        // Attendance History List
        items(records) { record ->
            AttendanceHistoryCard(record = record)
        }
    }
}

@Composable
private fun MiniChip(label: String, color: Color, bg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun AttendanceHistoryCard(record: AttendanceRecord) {
    val colors = LocalBizoraColors.current
    val (statusBg, statusFg, statusLabel) = when (record.status) {
        AttendanceStatus.PRESENT -> Triple(Color(0xFFE6F7ED), Color(0xFF16A34A), "PRESENT")
        AttendanceStatus.ABSENT -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), "ABSENT")
        AttendanceStatus.HALF_DAY -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "HALF DAY")
        AttendanceStatus.PENDING -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "PENDING")
        else -> Triple(colors.surfaceMuted, colors.textSecondary, "NOT MARKED")
    }

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.attendanceDate,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (record.checkInTime != null && record.checkOutTime != null) {
                        "In 09:15 AM · Out 06:15 PM · 9h 00m"
                    } else if (record.checkInTime != null) {
                        "In 09:15 AM · Check out pending"
                    } else {
                        "No check-in recorded"
                    },
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = statusLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusFg
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// 3. REPORTS TAB
// -------------------------------------------------------------------------------------
@Composable
private fun EmployeeReportsTabContent(
    employee: EmployeeItem,
    records: List<AttendanceRecord>,
    onExportClick: () -> Unit
) {
    val colors = LocalBizoraColors.current
    val startDateStr = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("d MMM yyyy"))
    val endDateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM yyyy"))

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, colors.border),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Attendance reports",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$startDateStr – $endDateStr",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }

                        IconButton(
                            onClick = { /* Open filters */ },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.brandPrimary.copy(alpha = 0.10f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filter report",
                                tint = colors.brandPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onExportClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.brandPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Export employee report",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "${records.size} matching attendance records",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary
                    )
                }
            }
        }

        items(records.take(8)) { record ->
            AttendanceHistoryCard(record = record)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmployeeReportFormatModal(
    employee: EmployeeItem,
    selectedFormat: ReportFormat,
    onFormatSelected: (ReportFormat) -> Unit,
    onConfirmExport: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalBizoraColors.current

    val formats = listOf(
        Triple(
            ReportFormat.EXCEL,
            "Excel Spreadsheet (.xlsx)",
            "Detailed workbook with punch timestamps & hours"
        ),
        Triple(
            ReportFormat.PDF,
            "PDF Document (.pdf)",
            "Official printable audit report with summary metrics"
        ),
        Triple(
            ReportFormat.CSV,
            "CSV Spreadsheet (.csv)",
            "Plain text data suitable for import to any tool"
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = colors.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = colors.border)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(colors.brandTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = colors.brandPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Choose Export Format",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Attendance for ${employee.name}",
                            fontSize = 12.5.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                ModalCloseButton(onClick = onDismiss)
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Format Selection Cards
            formats.forEach { (format, title, description) ->
                val isSelected = format == selectedFormat
                val formatColor = when (format) {
                    ReportFormat.EXCEL -> Color(0xFF15803D)
                    ReportFormat.PDF -> Color(0xFFDC2626)
                    ReportFormat.CSV -> Color(0xFF0284C7)
                }
                val formatBg = when (format) {
                    ReportFormat.EXCEL -> Color(0xFFDCFCE7)
                    ReportFormat.PDF -> Color(0xFFFEE2E2)
                    ReportFormat.CSV -> Color(0xFFE0F2FE)
                }
                val icon = when (format) {
                    ReportFormat.EXCEL -> Icons.Default.TableChart
                    ReportFormat.PDF -> Icons.Default.PictureAsPdf
                    ReportFormat.CSV -> Icons.Default.Description
                }

                Surface(
                    onClick = { onFormatSelected(format) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) colors.brandTint.copy(alpha = 0.35f) else colors.surfaceMuted,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) colors.brandPrimary else colors.border
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(formatBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = formatColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = description,
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }

                        // Radio Indicator
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (isSelected) 0.dp else 1.5.dp,
                                    color = if (isSelected) Color.Transparent else colors.border,
                                    shape = CircleShape
                                )
                                .background(if (isSelected) colors.brandPrimary else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Button (Olive with pure white text and icon)
            Button(
                onClick = onConfirmExport,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.brandPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generate & Export .${selectedFormat.extension.uppercase()}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
