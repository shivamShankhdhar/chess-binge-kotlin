package bizora.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
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
import bizora.app.data.model.*
import bizora.app.data.repository.BizoraRepository
import bizora.app.ui.components.*
import bizora.app.ui.dialogs.*
import bizora.app.ui.theme.LocalBizoraColors
import bizora.app.utils.ExportedReport
import bizora.app.utils.ReportExporter
import bizora.app.utils.AdMobInterstitialHelper
import bizora.app.utils.LocationHelper
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class EmployerTab {
    TODAY,
    ATTENDANCE,
    EMPLOYEES,
    WORKPLACE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerDashboard(
    repository: BizoraRepository,
    onSignOut: () -> Unit = {},
    onToggleTheme: () -> Unit = {}
) {
    val colors = LocalBizoraColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var activeTab by remember { mutableStateOf(EmployerTab.TODAY) }

    // Active workplace & state from repository
    val activeWorkplaceNullable by repository.activeWorkplace.collectAsState()
    val activeWorkplace = activeWorkplaceNullable ?: Workplace(id = "", name = "Workplace", timezone = "Asia/Kolkata", role = WorkplaceRole.EMPLOYER)
    val workplaces by repository.workplaces.collectAsState()
    val todaySession by repository.todaySession.collectAsState()
    val roster by repository.roster.collectAsState()
    val requests by repository.requests.collectAsState()
    val employees by repository.employees.collectAsState()
    val teams by repository.teams.collectAsState()
    val notifications by repository.notifications.collectAsState()
    val isEmployerTodayLoading by repository.isEmployerTodayLoading.collectAsState()
    val isRosterLoading by repository.isRosterLoading.collectAsState()
    val isRequestsLoading by repository.isRequestsLoading.collectAsState()
    val isEmployeesLoading by repository.isEmployeesLoading.collectAsState()

    var showWorkplaceSwitcher by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var showAddEmployeeModal by remember { mutableStateOf(false) }
    var preselectedTeamIdForAdd by remember { mutableStateOf<String?>(null) }
    var showCreateTeamModal by remember { mutableStateOf(false) }
    var employeesActiveSubTab by remember { mutableStateOf("Teams") }
    var employeesSelectedTeamFilter by remember { mutableStateOf<String?>(null) }
    var showCloseConfirmDialog by remember { mutableStateOf(false) }
    var showAttendanceQrModal by remember { mutableStateOf(false) }
    var showHowItWorksModal by remember { mutableStateOf(false) }
    var rejectTargetId by remember { mutableStateOf<String?>(null) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var manualMarkItem by remember { mutableStateOf<RosterItem?>(null) }
    var showJoinQrModal by remember { mutableStateOf(false) }
    var showEmployeePinModal by remember { mutableStateOf(false) }
    var pinEmployeeName by remember { mutableStateOf("") }
    var selectedEmployeeForActions by remember { mutableStateOf<EmployeeItem?>(null) }
    var viewingEmployeeDetails by remember { mutableStateOf<EmployeeItem?>(null) }
    var exportedReportForEmployeeDetails by remember { mutableStateOf<ExportedReport?>(null) }
    var showFilterAttendanceModal by remember { mutableStateOf(false) }
    var filterAttendanceStatus by remember { mutableStateOf("ALL") }
    var filterAttendanceRange by remember { mutableStateOf("Today") }
    var showRatingModal by remember { mutableStateOf(false) }
    var showUpdateModal by remember { mutableStateOf(false) }

    // Dedicated Employee Details Full Screen (matching React Native employee-detail screen)
    if (viewingEmployeeDetails != null) {
        val emp = viewingEmployeeDetails!!
        val cleanWorkplaceCode = activeWorkplace.formattedCode
        EmployeeDetailsScreen(
            employee = emp,
            workplaceName = activeWorkplace.name,
            workplaceCode = cleanWorkplaceCode,
            onBack = { viewingEmployeeDetails = null },
            onChangePinClick = {
                pinEmployeeName = emp.name
                showEmployeePinModal = true
            },
            onDeactivateClick = {
                val name = emp.name
                viewingEmployeeDetails = null
                Toast.makeText(context, "$name has been deactivated", Toast.LENGTH_SHORT).show()
            },
            onExportReportClick = { _ ->
                AdMobInterstitialHelper.showAdThenExecute(context) {
                    exportedReportForEmployeeDetails = ReportExporter.exportIndividualEmployeeReport(
                        context = context,
                        workplaceName = activeWorkplace.name,
                        employeeName = emp.name,
                        employeeCode = emp.employeeCode,
                        rosterHistory = roster
                    )
                }
            }
        )

        // Modal: Change Employee MPIN (Bottom Sheet)
        if (showEmployeePinModal) {
            EmployeePinModal(
                employeeName = pinEmployeeName,
                onDismiss = { showEmployeePinModal = false },
                onSavePin = { pin ->
                    showEmployeePinModal = false
                    Toast.makeText(context, "MPIN for $pinEmployeeName updated successfully", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Modal: Report Export Bottom Sheet
        if (exportedReportForEmployeeDetails != null) {
            ReportExportModal(
                report = exportedReportForEmployeeDetails!!,
                onDismiss = { exportedReportForEmployeeDetails = null }
            )
        }

        return
    }

    // Bottom Navigation Tabs
    val tabs = remember {
        listOf(
            BottomTabItem(
                key = EmployerTab.TODAY,
                label = "Today",
                iconRenderer = { color, size, active -> HomeTabIcon(color = color, size = size, active = active) },
                badgeCount = 0
            ),
            BottomTabItem(
                key = EmployerTab.ATTENDANCE,
                label = "Attendance",
                iconRenderer = { color, size, active -> AttendanceTabIcon(color = color, size = size, active = active) }
            ),
            BottomTabItem(
                key = EmployerTab.EMPLOYEES,
                label = "Employees",
                iconRenderer = { color, size, active -> EmployeesTabIcon(color = color, size = size, active = active) }
            ),
            BottomTabItem(
                key = EmployerTab.WORKPLACE,
                label = "Workplace",
                iconRenderer = { color, size, active -> WorkplaceTabIcon(color = color, size = size, active = active) }
            )
        )
    }

    Scaffold(
        topBar = {
            when (activeTab) {
                EmployerTab.TODAY -> {
                    val user by repository.currentUser.collectAsState(null)
                    val effectiveName = user?.name ?: "Demo Employer"
                    val effectiveEmail = user?.email ?: "demo_employer@test.com"
                    val initials = effectiveName.split(" ")
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .take(2)
                        .joinToString("")
                        .ifEmpty { "DE" }

                    HomeHeaderOptions(
                        userName = effectiveName,
                        userEmail = effectiveEmail,
                        initials = initials,
                        avatarUrl = user?.avatarUrl,
                        notificationCount = notifications.count { !it.read },
                        onNotificationsClick = { showNotifications = true },
                        onProfileClick = { activeTab = EmployerTab.WORKPLACE },
                        onSearchClick = { activeTab = EmployerTab.EMPLOYEES }
                    )
                }
                EmployerTab.ATTENDANCE -> {
                    AttendanceTopBar(
                        onHelpClick = { showHowItWorksModal = true }
                    )
                }
                EmployerTab.EMPLOYEES -> {
                    EmployeesTopBar(
                        onAddClick = {
                            preselectedTeamIdForAdd = null
                            showAddEmployeeModal = true
                        }
                    )
                }
                EmployerTab.WORKPLACE -> {
                    WorkplaceTopBar(
                        workplaceName = activeWorkplace.name,
                        onSignOutClick = { showSignOutConfirm = true }
                    )
                }
            }
        },
        bottomBar = {
            BizoraBottomBar(
                tabs = tabs,
                activeTab = activeTab,
                onTabSelected = { activeTab = it }
            )
        },
        floatingActionButton = {
            if (activeTab == EmployerTab.EMPLOYEES) {
                FloatingActionButton(
                    onClick = {
                        preselectedTeamIdForAdd = null
                        showAddEmployeeModal = true
                    },
                    containerColor = colors.brandPrimary,
                    contentColor = colors.textInverse,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Employee", modifier = Modifier.size(28.dp))
                }
            }
        },
        containerColor = colors.canvas
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (activeTab != EmployerTab.TODAY) {
                HeaderBackgroundArt(modifier = Modifier.align(Alignment.TopEnd))
            }

            when (activeTab) {
                EmployerTab.TODAY -> {
                    if (isEmployerTodayLoading && roster.isEmpty()) {
                        EmployerTodaySkeleton()
                    } else {
                        TodayScreenView(
                            workplace = activeWorkplace,
                            session = todaySession,
                            roster = roster,
                            requests = requests,
                            teams = teams,
                            totalEmployeesCount = employees.size,
                            onOpenQrModal = { showAttendanceQrModal = true },
                            onOpenJoinQr = {
                                preselectedTeamIdForAdd = null
                                showJoinQrModal = true
                            },
                            onOpenJoinQrForTeam = { teamId ->
                                preselectedTeamIdForAdd = teamId
                                showJoinQrModal = true
                            },
                            onSwitchWorkplace = { showWorkplaceSwitcher = true },
                            onManualMark = { manualMarkItem = it },
                            onApproveRequest = { repository.approveRequest(it) },
                            onRejectRequest = { rejectTargetId = it },
                            onNavigateToAttendance = { activeTab = EmployerTab.ATTENDANCE },
                            onNavigateToEmployees = {
                                employeesActiveSubTab = "All"
                                employeesSelectedTeamFilter = null
                                activeTab = EmployerTab.EMPLOYEES
                            },
                            onNavigateToTeamsDirectory = {
                                employeesActiveSubTab = "Teams"
                                employeesSelectedTeamFilter = null
                                activeTab = EmployerTab.EMPLOYEES
                            },
                            onNavigateToTeamRoster = { teamId ->
                                employeesActiveSubTab = "All"
                                employeesSelectedTeamFilter = teamId
                                activeTab = EmployerTab.EMPLOYEES
                            },
                            onNavigateToWorkplace = { activeTab = EmployerTab.WORKPLACE },
                            onAddEmployee = { teamId ->
                                preselectedTeamIdForAdd = teamId
                                showAddEmployeeModal = true
                            },
                            onCreateTeam = { showCreateTeamModal = true },
                            onEmployeeClick = { viewingEmployeeDetails = it }
                        )
                    }
                }

                EmployerTab.ATTENDANCE -> AttendanceScreenView(
                    workplaceName = activeWorkplace.name,
                    session = todaySession,
                    roster = roster,
                    requests = requests,
                    filterAttendanceStatus = filterAttendanceStatus,
                    isRosterLoading = isRosterLoading,
                    isRequestsLoading = isRequestsLoading,
                    onOpenSession = { repository.openAttendanceSession() },
                    onCloseSession = { showCloseConfirmDialog = true },
                    onOpenQrModal = { showAttendanceQrModal = true },
                    onApproveRequest = { repository.approveRequest(it) },
                    onRejectRequest = { rejectTargetId = it },
                    onManualMark = { manualMarkItem = it },
                    onOpenHelp = { showHowItWorksModal = true },
                    onOpenFilterAttendance = { showFilterAttendanceModal = true },
                    onEmployeeClick = { viewingEmployeeDetails = it }
                )

                EmployerTab.EMPLOYEES -> EmployeesScreenView(
                    employees = employees,
                    teams = teams,
                    isEmployeesLoading = isEmployeesLoading,
                    initialSubTab = employeesActiveSubTab,
                    initialSelectedTeamId = employeesSelectedTeamFilter,
                    onAddEmployeeClick = { teamId ->
                        preselectedTeamIdForAdd = teamId
                        showAddEmployeeModal = true
                    },
                    onCreateTeamClick = { showCreateTeamModal = true },
                    onOpenJoinQr = { teamId ->
                        preselectedTeamIdForAdd = teamId
                        showJoinQrModal = true
                    },
                    onEmployeeClick = { viewingEmployeeDetails = it },
                    onCopyInviteLink = { code ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Invite Link", "https://bizora.app/join?code=$code"))
                        Toast.makeText(context, "Invite link copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                )

                EmployerTab.WORKPLACE -> WorkplaceScreenView(
                    workplace = activeWorkplace,
                    repository = repository,
                    onSwitchWorkplace = { showWorkplaceSwitcher = true },
                    onOpenJoinQr = { showJoinQrModal = true },
                    onShowRating = { showRatingModal = true },
                    onShowUpdate = { showUpdateModal = true },
                    onSignOut = { showSignOutConfirm = true }
                )
            }
        }
    }

    // Modal: Workplace Switcher
    if (showWorkplaceSwitcher) {
        WorkplaceSwitcherModal(
            workplaces = workplaces,
            activeWorkplaceId = activeWorkplace.id,
            onSelectWorkplace = {
                repository.switchWorkplace(it.id)
                showWorkplaceSwitcher = false
            },
            onDismiss = { showWorkplaceSwitcher = false },
            onCreateNewWorkplace = {
                showWorkplaceSwitcher = false
                Toast.makeText(context, "Workplace creation flow", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal: Notifications
    if (showNotifications) {
        NotificationsModal(
            notifications = notifications,
            onDismiss = { showNotifications = false },
            onMarkAllAsRead = { repository.markAllNotificationsAsRead() },
            onMarkAsRead = { repository.markNotificationAsRead(it) }
        )
    }

    // Modal: Manual Attendance Record
    if (manualMarkItem != null) {
        ManualAttendanceDialog(
            item = manualMarkItem,
            allEmployees = roster,
            onDismiss = { manualMarkItem = null },
            onSave = { memberId, status, reason ->
                repository.markManualAttendance(memberId, status, reason)
                manualMarkItem = null
            }
        )
    }

    // Modal: Add Employee
    if (showAddEmployeeModal) {
        AddEmployeeDialog(
            teams = teams,
            initialTeamId = preselectedTeamIdForAdd,
            onDismiss = {
                showAddEmployeeModal = false
                preselectedTeamIdForAdd = null
            },
            onCreateTeamClick = {
                showAddEmployeeModal = false
                showCreateTeamModal = true
            },
            onAdd = { name, code, email, teamId ->
                repository.addEmployee(name, code, email, teamId = teamId)
                showAddEmployeeModal = false
                preselectedTeamIdForAdd = null
            }
        )
    }

    // Modal: Create Team
    if (showCreateTeamModal) {
        CreateTeamDialog(
            onDismiss = { showCreateTeamModal = false },
            onCreateTeam = { name, description, color ->
                coroutineScope.launch {
                    val res = repository.createTeam(name, description, color)
                    if (res.isSuccess) {
                        showCreateTeamModal = false
                        Toast.makeText(context, "Team '$name' created successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Failed to create team", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Bottom Sheet: Close Attendance Session Confirm
    if (showCloseConfirmDialog) {
        CloseSessionConfirmBottomSheet(
            onDismiss = { showCloseConfirmDialog = false },
            onConfirm = { repository.closeAttendanceSession() }
        )
    }

    // Modal: Live Attendance QR & Session Manager
    if (showAttendanceQrModal) {
        AttendanceQrModal(
            session = todaySession,
            isOpen = todaySession?.session?.status == SessionStatus.OPEN,
            onOpenSession = { repository.openAttendanceSession() },
            onCloseSession = {
                showAttendanceQrModal = false
                showCloseConfirmDialog = true
            },
            onDismiss = { showAttendanceQrModal = false }
        )
    }

    // Modal: How Attendance Works
    if (showHowItWorksModal) {
        HowAttendanceWorksDialog(onDismiss = { showHowItWorksModal = false })
    }

    // Bottom Sheet: Reject Attendance Request with Reason
    if (rejectTargetId != null) {
        RejectRequestDialog(
            requestId = rejectTargetId!!,
            onDismiss = { rejectTargetId = null },
            onConfirm = { reason ->
                repository.rejectRequest(rejectTargetId!!, reason)
                rejectTargetId = null
            }
        )
    }

    // Bottom Sheet: Sign Out Confirmation
    if (showSignOutConfirm) {
        SignOutConfirmBottomSheet(
            onDismiss = { showSignOutConfirm = false },
            onConfirm = {
                showSignOutConfirm = false
                onSignOut()
            }
        )
    }

    // Modal: Workplace Join QR & Link (Bottom Sheet)
    if (showJoinQrModal) {
        WorkplaceJoinQrModal(
            workplaceId = activeWorkplace.id,
            workplaceName = activeWorkplace.name,
            workplaceCode = activeWorkplace.formattedCode,
            teams = teams,
            initialTeamId = preselectedTeamIdForAdd,
            onCreateTeam = { showCreateTeamModal = true },
            onDismiss = { showJoinQrModal = false }
        )
    }

    // Modal: Employee Actions (Bottom Sheet)
    if (selectedEmployeeForActions != null) {
        val emp = selectedEmployeeForActions!!
        val cleanWorkplaceCode = activeWorkplace.formattedCode
        EmployeeActionsModal(
            employee = emp,
            workplaceName = activeWorkplace.name,
            workplaceCode = cleanWorkplaceCode,
            onDismiss = { selectedEmployeeForActions = null },
            onChangePinClick = {
                pinEmployeeName = emp.name
                showEmployeePinModal = true
            },
            onDeactivateClick = {
                selectedEmployeeForActions = null
                Toast.makeText(context, "Employee deactivated", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal: Change Employee MPIN (Bottom Sheet)
    if (showEmployeePinModal) {
        EmployeePinModal(
            employeeName = pinEmployeeName,
            onDismiss = { showEmployeePinModal = false },
            onSavePin = { pin ->
                showEmployeePinModal = false
            }
        )
    }

    // Modal: Filter Attendance (Bottom Sheet)
    if (showFilterAttendanceModal) {
        FilterAttendanceModal(
            selectedStatus = filterAttendanceStatus,
            selectedRange = filterAttendanceRange,
            onDismiss = { showFilterAttendanceModal = false },
            onApply = { status, range ->
                filterAttendanceStatus = status
                filterAttendanceRange = range
            },
            onReset = {
                filterAttendanceStatus = "ALL"
                filterAttendanceRange = "Today"
            }
        )
    }

    if (showRatingModal) {
        PlayStoreRatingModal(
            onDismiss = { showRatingModal = false }
        )
    }

    if (showUpdateModal) {
        AppUpdateModal(
            versionName = "1.1.0",
            onDismiss = { showUpdateModal = false }
        )
    }
}

// =========================================================================
// 1. TODAY SCREEN (MATCHING REACT NATIVE DASHBOARD EXACTLY)
// =========================================================================

@Composable
fun TodayScreenView(
    workplace: Workplace,
    session: AttendanceSession?,
    roster: List<RosterItem>,
    requests: List<AttendanceRequestItem>,
    teams: List<Team> = emptyList(),
    totalEmployeesCount: Int,
    onOpenQrModal: () -> Unit,
    onOpenJoinQr: () -> Unit = {},
    onOpenJoinQrForTeam: (String?) -> Unit = {},
    onSwitchWorkplace: () -> Unit,
    onManualMark: (RosterItem) -> Unit,
    onApproveRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToEmployees: () -> Unit,
    onNavigateToTeamsDirectory: () -> Unit = onNavigateToEmployees,
    onNavigateToTeamRoster: (String) -> Unit = { onNavigateToEmployees() },
    onNavigateToWorkplace: () -> Unit,
    onAddEmployee: (String?) -> Unit,
    onCreateTeam: () -> Unit = {},
    onEmployeeClick: (EmployeeItem) -> Unit = {}
) {
    val colors = LocalBizoraColors.current

    val presentCount = roster.count { it.status == AttendanceStatus.PRESENT }
    val pendingCount = requests.size
    val effectiveEmployeesCount = if (totalEmployeesCount > 0) totalEmployeesCount else roster.size

    val hour = java.time.LocalTime.now().hour
    val isMorning = hour < 12
    val attendanceActionText = if (isMorning) "Check in" else "Check out"
    val attendanceSubtitle = if (isMorning) "Record your check-in" else "Record your checkout"

    val currentDateFormatted = remember {
        java.time.LocalDate.now().format(
            java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM", java.util.Locale.ENGLISH)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. ACTIVE WORKPLACE CARD (Matches reference design)
        item {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF334155) else Color(0xFFE9ECE0)),
                shadowElevation = 0.5.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSwitchWorkplace() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Dark Olive Green Icon Box with Building
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF4B5927)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apartment,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = workplace.name,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF22C55E), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "ACTIVE WORKPLACE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp,
                                color = if (colors.isDark) Color(0xFF86EFAC) else Color(0xFF4B5927)
                            )
                        }
                    }

                    // Circular Dropdown Arrow
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (colors.isDark) Color(0xFF1E293B) else Color(0xFFEFF2E7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Switch Workplace",
                            tint = if (colors.isDark) Color(0xFFF1F5F9) else Color(0xFF4B5927),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. TODAY'S OVERVIEW SECTION
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Section Title Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's overview",
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = currentDateFormatted,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                }

                // 2x2 Quadrant Grid Card
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF334155) else Color(0xFFECEFE5)),
                    shadowElevation = 0.5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Top Row: Present + Pending
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Top-Left: Present
                            TodayOverviewQuadrantItem(
                                iconBg = Color(0xFFEBF2DF),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.People,
                                        contentDescription = null,
                                        tint = Color(0xFF4B5927),
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                value = presentCount.toString(),
                                label = "Present",
                                onClick = onNavigateToAttendance,
                                modifier = Modifier.weight(1f)
                            )

                            // Vertical Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight(0.72f)
                                    .background(if (colors.isDark) Color(0xFF334155) else Color(0xFFEFF2EA))
                            )

                            // Top-Right: Pending
                            TodayOverviewQuadrantItem(
                                iconBg = Color(0xFFFEF3C7),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                value = pendingCount.toString(),
                                label = "Pending",
                                onClick = onNavigateToAttendance,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Horizontal Divider
                        HorizontalDivider(
                            color = if (colors.isDark) Color(0xFF334155) else Color(0xFFEFF2EA),
                            thickness = 1.dp
                        )

                        // Bottom Row: Teams + Staff
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Bottom-Left: Teams
                            TodayOverviewQuadrantItem(
                                iconBg = Color(0xFFE0E7FF),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Groups,
                                        contentDescription = null,
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                value = (if (teams.size > 1) teams.size else 2).toString(),
                                label = "Teams",
                                onClick = onNavigateToTeamsDirectory,
                                modifier = Modifier.weight(1f)
                            )

                            // Vertical Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight(0.72f)
                                    .background(if (colors.isDark) Color(0xFF334155) else Color(0xFFEFF2EA))
                            )

                            // Bottom-Right: Staff
                            TodayOverviewQuadrantItem(
                                iconBg = Color(0xFFDCFCE7),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                value = (if (effectiveEmployeesCount > 0) effectiveEmployeesCount else 1).toString(),
                                label = "Staff",
                                onClick = onNavigateToEmployees,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 3. YOUR ATTENDANCE BANNER CARD
        item {
            Surface(
                color = if (colors.isDark) Color(0xFF1E293B) else Color(0xFFEEF3E6),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF334155) else Color(0xFFE0E7D6)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (colors.isDark) Color(0xFF334155) else Color(0xFFDEE5D3)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = if (colors.isDark) Color(0xFFF1F5F9) else Color(0xFF3D4A1E),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Your attendance",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (colors.isDark) Color(0xFFF1F5F9) else Color(0xFF19240A)
                        )
                        Text(
                            text = attendanceSubtitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = if (colors.isDark) Color(0xFF94A3B8) else Color(0xFF5D6B47)
                        )
                    }

                    Surface(
                        color = Color(0xFF3D4A1E),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable { onOpenQrModal() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = attendanceActionText,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 4. QUICK ACTIONS SECTION
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Quick actions",
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // 2x2 Grid of Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row 1: Invite employee + View attendance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TodayQuickActionCard(
                            icon = Icons.Outlined.PersonAdd,
                            title = "Invite employee",
                            subtitle = "Add team members",
                            onClick = onOpenJoinQr,
                            modifier = Modifier.weight(1f)
                        )
                        TodayQuickActionCard(
                            icon = Icons.Outlined.CalendarMonth,
                            title = "View attendance",
                            subtitle = "Check team activity",
                            onClick = onNavigateToAttendance,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Approvals + Manage teams
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TodayQuickActionCard(
                            icon = Icons.Outlined.AssignmentTurnedIn,
                            title = "Approvals",
                            subtitle = if (pendingCount > 0) "$pendingCount pending" else "0 pending",
                            subtitleColor = if (pendingCount > 0) Color(0xFFD97706) else Color(0xFFB45309),
                            onClick = onNavigateToAttendance,
                            modifier = Modifier.weight(1f)
                        )
                        TodayQuickActionCard(
                            icon = Icons.Outlined.Groups,
                            title = "Manage teams",
                            subtitle = "Organize your teams",
                            onClick = onNavigateToTeamsDirectory,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 5. TEAM OVERVIEW SECTION
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header with "Team overview" and "View all >"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Team overview",
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onNavigateToTeamsDirectory() }
                            .padding(vertical = 2.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = "View all",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (colors.isDark) Color(0xFF86EFAC) else Color(0xFF4B5927)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = if (colors.isDark) Color(0xFF86EFAC) else Color(0xFF4B5927),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Unified Card for teams
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF334155) else Color(0xFFECEFE5)),
                    shadowElevation = 0.5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val displayTeams = remember(teams) {
                        if (teams.size >= 2) {
                            teams.take(4)
                        } else if (teams.size == 1) {
                            listOf(
                                teams[0],
                                Team(id = "engineering", workplaceId = workplace.id, name = "Engineering", memberCount = 1)
                            )
                        } else {
                            listOf(
                                Team(id = "general", workplaceId = workplace.id, name = "General", memberCount = 1, isDefault = true),
                                Team(id = "engineering", workplaceId = workplace.id, name = "Engineering", memberCount = 1)
                            )
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        displayTeams.forEachIndexed { index, team ->
                            val dotColor = when (index % 4) {
                                0 -> Color(0xFF556930) // Olive
                                1 -> Color(0xFF2563EB) // Blue
                                2 -> Color(0xFF7C3AED) // Purple
                                else -> Color(0xFF0D9488) // Teal
                            }
                            val count = if (team.memberCount > 0) team.memberCount else 1
                            val memberText = if (count == 1) "1 member" else "$count members"
                            val subtitleText = if (team.isDefault || team.name.equals("General", ignoreCase = true)) {
                                "Default team • $memberText"
                            } else {
                                memberText
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToTeamRoster(team.id) }
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(dotColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = team.name,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = subtitleText,
                                        fontSize = 12.sp,
                                        color = colors.textSecondary,
                                        modifier = Modifier.padding(top = 1.dp)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFF9CA3AF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (index < displayTeams.size - 1) {
                                HorizontalDivider(
                                    color = if (colors.isDark) Color(0xFF334155) else Color(0xFFF1F3EE),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. NO APPROVALS WAITING BANNER
        item {
            Surface(
                color = if (colors.isDark) Color(0xFF1E293B) else Color(0xFFE5EEDC),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF334155) else Color(0xFFD8E4CD)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4B5927)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "No approvals waiting",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (colors.isDark) Color(0xFFF1F5F9) else Color(0xFF1B240D)
                        )
                        Text(
                            text = "You're all caught up.",
                            fontSize = 12.sp,
                            color = if (colors.isDark) Color(0xFF94A3B8) else Color(0xFF52613B)
                        )
                    }
                }
            }
        }

        // 7. ADVERTISEMENT BANNER
        item {
            Surface(
                color = if (colors.isDark) Color(0xFF1E293B) else Color(0xFFE8ECEF),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF334155) else Color(0xFFDCE2E7)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Advertisement",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (colors.isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                        )
                        Text(
                            text = "This is a 320x50 test ad.",
                            fontSize = 10.5.sp,
                            color = if (colors.isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayOverviewQuadrantItem(
    iconBg: Color,
    icon: @Composable () -> Unit,
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalBizoraColors.current
    Row(
        modifier = modifier
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                lineHeight = 28.sp
            )
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun TodayQuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitleColor: Color? = null
) {
    val colors = LocalBizoraColors.current
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF334155) else Color(0xFFE8ECE1)),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(35.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (colors.isDark) Color(0xFF334155) else Color(0xFFEFF4E8)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (colors.isDark) Color(0xFF86EFAC) else Color(0xFF4B5927),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = subtitleColor ?: colors.textSecondary,
                    fontWeight = if (subtitleColor != null) FontWeight.Medium else FontWeight.Normal,
                    letterSpacing = (-0.2).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

// Bottom Sheet: Close Attendance Session Confirm
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloseSessionConfirmBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = LocalBizoraColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.danger.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = colors.danger,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Close Attendance Session?",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceMuted)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Closing the session will disable check-in QR codes for employees. Today's attendance log will be finalized for reporting.",
                fontSize = 13.5.sp,
                color = colors.textSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger),
                    modifier = Modifier.weight(1.2f).height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close Session", color = colors.textInverse, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// Bottom Sheet: Sign Out Confirmation
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignOutConfirmBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = LocalBizoraColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.danger.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = colors.danger,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Sign out of Bizora?",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceMuted)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Are you sure you want to sign out? You will need to sign in again to access and manage your workplace.",
                fontSize = 13.5.sp,
                color = colors.textSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        onDismiss()
                        onConfirm()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger),
                    modifier = Modifier.weight(1.2f).height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sign Out", color = colors.textInverse, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// Top Bar: Attendance Tab
@Composable
fun AttendanceTopBar(onHelpClick: () -> Unit) {
    val colors = LocalBizoraColors.current
    Surface(
        color = colors.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Attendance",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    letterSpacing = (-0.4).sp
                )
                Text(
                    text = "Mark attendance, live QR & manual entry.",
                    fontSize = 14.5.sp,
                    color = colors.textSecondary
                )
            }
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.clickable { onHelpClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = colors.brandPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Help",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }
            }
        }
    }
}

// Top Bar: Employees Tab
@Composable
fun EmployeesTopBar(onAddClick: () -> Unit) {
    val colors = LocalBizoraColors.current
    Surface(
        color = colors.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Employees",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    letterSpacing = (-0.4).sp
                )
                Text(
                    text = "Manage staff, join links and access.",
                    fontSize = 14.5.sp,
                    color = colors.textSecondary
                )
            }
            Surface(
                color = colors.brandTint,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, colors.brandPrimary),
                modifier = Modifier.clickable { onAddClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = colors.brandPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Add",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.brandPrimary
                    )
                }
            }
        }
    }
}

// Top Bar: Workplace Tab
@Composable
fun WorkplaceTopBar(
    workplaceName: String,
    onSignOutClick: () -> Unit
) {
    val colors = LocalBizoraColors.current
    Surface(
        color = colors.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Workplace",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    letterSpacing = (-0.4).sp
                )
                Text(
                    text = "Settings, Wi-Fi and invite code.",
                    fontSize = 14.5.sp,
                    color = colors.textSecondary
                )
            }
            Surface(
                color = colors.danger.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, colors.danger.copy(alpha = 0.3f)),
                modifier = Modifier.clickable { onSignOutClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        tint = colors.danger,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Sign Out",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.danger
                    )
                }
            }
        }
    }
}

// Modal: Live Attendance QR & Session Manager
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceQrModal(
    session: AttendanceSession?,
    isOpen: Boolean,
    onOpenSession: () -> Unit,
    onCloseSession: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalBizoraColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var qrTab by remember { mutableStateOf("checkin") }

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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.brandTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = colors.brandPrimary, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("Today's Attendance QR", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text(
                            text = if (isOpen) "Session open • Employees can scan" else "Session closed",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceMuted)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isOpen && session?.qrPayload != null) {
                // Check-In / Check-Out tab toggle with generous height and equal balancing
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surfaceMuted)
                        .border(1.dp, colors.border.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isCheckIn = qrTab == "checkin"
                    val checkInBg by animateColorAsState(
                        targetValue = if (isCheckIn) colors.brandPrimary else Color.Transparent,
                        label = "checkInBg"
                    )
                    val checkInContentColor by animateColorAsState(
                        targetValue = if (isCheckIn) Color.White else colors.textPrimary,
                        label = "checkInContentColor"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(checkInBg)
                            .clickable { qrTab = "checkin" },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Login,
                                contentDescription = null,
                                tint = if (isCheckIn) Color.White else colors.brandPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Check-In",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = checkInContentColor
                            )
                        }
                    }

                    val isCheckOut = qrTab == "checkout"
                    val checkOutBg by animateColorAsState(
                        targetValue = if (isCheckOut) Color(0xFFD97706) else Color.Transparent,
                        label = "checkOutBg"
                    )
                    val checkOutContentColor by animateColorAsState(
                        targetValue = if (isCheckOut) Color.White else colors.textPrimary,
                        label = "checkOutContentColor"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(checkOutBg)
                            .clickable { qrTab = "checkout" },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = null,
                                tint = if (isCheckOut) Color.White else Color(0xFFD97706),
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Check-Out",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = checkOutContentColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Active Mode Pill Indicator
                Surface(
                    color = if (qrTab == "checkout") Color(0xFFFEF3C7) else colors.brandTint,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, if (qrTab == "checkout") Color(0xFFFDE68A) else colors.brandPrimary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (qrTab == "checkout") Color(0xFFD97706) else colors.brandPrimary)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = if (qrTab == "checkout") "Displaying Check-Out QR" else "Displaying Check-In QR",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (qrTab == "checkout") Color(0xFF92400E) else colors.brandPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val rawPayload = session.qrPayload
                val currentPayload = if (qrTab == "checkout") {
                    if (rawPayload.contains("action=checkin")) rawPayload.replace("action=checkin", "action=checkout")
                    else if (rawPayload.contains("?")) "$rawPayload&action=checkout"
                    else "$rawPayload?action=checkout"
                } else {
                    rawPayload
                }

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        QrCodeView(content = currentPayload, size = 195.dp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (qrTab == "checkout")
                        "Employees scan this to check out · Recorded instantly, no approval needed"
                    else
                        "Employees scan this to check in · Recorded instantly, no approval needed",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Opened at ${session.session.openedAt} • Auto-closes 08:00 PM",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onCloseSession,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.danger),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("End Session", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(colors.brandTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = colors.brandPrimary, modifier = Modifier.size(36.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Start Today's Attendance",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = "Generate live dynamic QR codes for team check-in and check-out.",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )

                // Security Badges
                Surface(
                    color = colors.surfaceMuted,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SecurityBadgeChip(icon = Icons.Default.Shield, text = "Anti-Spoof QR")
                        SecurityBadgeChip(icon = Icons.Default.Place, text = "Geofenced")
                        SecurityBadgeChip(icon = Icons.Default.Wifi, text = "Wi-Fi Sync")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onOpenSession,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = colors.textInverse, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Attendance Session", fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, color = colors.textInverse)
                }
            }
        }
    }
}

// Modal: How Attendance Works
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowAttendanceWorksDialog(
    onDismiss: () -> Unit
) {
    val colors = LocalBizoraColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = colors.brandPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            "How Attendance Works",
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            "Operational guide & daily workflow",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceMuted)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 1
            HowItWorksStepItem(
                stepNumber = "1",
                stepColor = colors.brandPrimary,
                title = "Open Morning Session",
                description = "Start the daily attendance session each morning. A secure, rotating anti-spoof QR code is created specifically for your workplace."
            )

            // Step 2
            HowItWorksStepItem(
                stepNumber = "2",
                stepColor = Color(0xFF10B981),
                title = "Employees Scan at Kiosk",
                description = "Display the QR code on a front-desk tablet, laptop, or mobile screen. Employees scan with their mobile app to instantly record Check-In or Check-Out."
            )

            // Step 3
            HowItWorksStepItem(
                stepNumber = "3",
                stepColor = Color(0xFF8B5CF6),
                title = "Manual Overrides & Corrections",
                description = "If someone forgot their phone, worked remotely, or needs an adjustment, tap Manual Attendance to select the employee, set status, and provide a reason."
            )

            // Step 4
            HowItWorksStepItem(
                stepNumber = "4",
                stepColor = Color(0xFFF59E0B),
                title = "Automated Session Closing",
                description = "Sessions automatically close at your workplace auto-close hour (e.g. 23:00) to lock daily records for reports, or you can end the session manually anytime."
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Security & Verification Chips
            Surface(
                color = colors.surfaceMuted,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SecurityBadgeChip(icon = Icons.Default.Shield, text = "Anti-Spoof QR")
                    SecurityBadgeChip(icon = Icons.Default.Place, text = "Geofenced")
                    SecurityBadgeChip(icon = Icons.Default.Wifi, text = "Wi-Fi Sync")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pro Tip Box
            Surface(
                color = Color(0xFFFEF3C7),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Text(
                        text = "Pro Tip: Leave the QR Kiosk screen running on an iPad or reception desk monitor in Guided Access mode for a dedicated, zero-touch attendance terminal.",
                        fontSize = 12.sp,
                        color = Color(0xFF92400E),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Got it button
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text("Got it", fontWeight = FontWeight.SemiBold, color = colors.textInverse, fontSize = 14.5.sp)
            }
        }
    }
}

@Composable
private fun HowItWorksStepItem(
    stepNumber: String,
    stepColor: Color,
    title: String,
    description: String
) {
    val colors = LocalBizoraColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(stepColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = colors.textSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun SecurityBadgeChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    val colors = LocalBizoraColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = colors.brandPrimary, modifier = Modifier.size(13.dp))
        Text(text, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
    }
}

// Bottom Sheet: Reject Attendance Request
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RejectRequestDialog(
    requestId: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val colors = LocalBizoraColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var reason by remember { mutableStateOf("Declined by employer") }

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
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.danger.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = null,
                            tint = colors.danger,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Reject Attendance Request",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.5.sp,
                        color = colors.textPrimary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceMuted)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Provide an audit reason for rejecting this attendance request. The employee will see this reason in their app.",
                fontSize = 13.5.sp,
                color = colors.textSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                placeholder = { Text("Reason for rejection...", fontSize = 13.5.sp) },
                shape = RoundedCornerShape(12.dp),
                minLines = 2,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.danger,
                    unfocusedBorderColor = colors.border
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { onConfirm(reason.trim()) },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger),
                    modifier = Modifier.weight(1.3f).height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reject Request", color = colors.textInverse, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}


// =========================================================================
// 2. ATTENDANCE SCREEN (WITH 4 INTERNAL TABS: Mark, Requests, Daily, Reports)
// =========================================================================

@Composable
fun AttendanceScreenView(
    workplaceName: String = "Workplace",
    session: AttendanceSession?,
    roster: List<RosterItem>,
    requests: List<AttendanceRequestItem>,
    filterAttendanceStatus: String = "ALL",
    isRosterLoading: Boolean = false,
    isRequestsLoading: Boolean = false,
    onOpenSession: () -> Unit,
    onCloseSession: () -> Unit,
    onOpenQrModal: () -> Unit,
    onApproveRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit,
    onManualMark: (RosterItem) -> Unit,
    onOpenHelp: () -> Unit = {},
    onOpenFilterAttendance: () -> Unit = {},
    onEmployeeClick: (EmployeeItem) -> Unit = {}
) {
    val colors = LocalBizoraColors.current
    val context = LocalContext.current
    var subTab by remember { mutableStateOf("Mark") }
    val internalTabs = listOf("Mark", "Requests", "Daily", "Reports")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Internal Segmented Tabs Bar with Badge
        SegmentedSubTabs(
            tabs = internalTabs,
            selectedTab = subTab,
            onTabSelected = { subTab = it },
            badges = mapOf("Requests" to requests.size),
            modifier = Modifier.padding(bottom = 14.dp)
        )

        // Subtab Content
        when (subTab) {
            "Mark" -> AttendanceMarkSubTab(
                session = session,
                roster = roster,
                onOpenQrModal = onOpenQrModal,
                onManualMark = onManualMark,
                onEmployeeClick = onEmployeeClick
            )

            "Requests" -> {
                if (isRequestsLoading && requests.isEmpty()) {
                    EmployerRequestsSkeleton()
                } else {
                    AttendanceRequestsSubTab(
                        requests = requests,
                        onApprove = onApproveRequest,
                        onReject = onRejectRequest
                    )
                }
            }

            "Daily" -> {
                if (isRosterLoading && roster.isEmpty()) {
                    EmployerDailySkeleton()
                } else {
                    AttendanceDailySubTab(
                        roster = roster,
                        filterAttendanceStatus = filterAttendanceStatus,
                        onOpenFilterAttendance = onOpenFilterAttendance,
                        onManualMark = onManualMark,
                        onEmployeeClick = onEmployeeClick
                    )
                }
            }

            "Reports" -> {
                if (isRosterLoading && roster.isEmpty()) {
                    EmployerReportsSkeleton()
                } else {
                    AttendanceReportsSubTab(
                        roster = roster,
                        workplaceName = workplaceName
                    )
                }
            }
        }
    }
}

// Subtab: Mark (Matches rn_screen_3_attendance_mark.png)
@Composable
fun AttendanceMarkSubTab(
    session: AttendanceSession?,
    roster: List<RosterItem>,
    onOpenQrModal: () -> Unit,
    onManualMark: (RosterItem) -> Unit,
    onEmployeeClick: (EmployeeItem) -> Unit = {}
) {
    val colors = LocalBizoraColors.current
    val isOpen = session?.session?.status == SessionStatus.OPEN

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Option 1: Manual Attendance Action Card
        item {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (roster.isNotEmpty()) onManualMark(roster.first())
                    }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.brandTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.EditCalendar, contentDescription = null, tint = colors.brandPrimary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Manual Attendance", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text("Record, override, or adjust attendance for any team member", fontSize = 12.5.sp, color = colors.textSecondary)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(22.dp))
                }
            }
        }

        // Option 2: Live Attendance Session Card
        item {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenQrModal() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isOpen) Color(0xFFDCFCE7) else colors.brandTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = if (isOpen) Color(0xFF16A34A) else colors.brandPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isOpen) "Attendance Session" else "Open Attendance Session",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = if (isOpen) Color(0xFFDCFCE7) else colors.surfaceMuted,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isOpen) "Active" else "Inactive",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isOpen) Color(0xFF16A34A) else colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Front-desk live QR code session for workforce check-ins",
                            fontSize = 12.5.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(22.dp))
                }
            }
        }

        // Section: Direct Quick-Edit List
        item {
            Text(
                text = "Quick Member Attendance",
                fontSize = 16.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        items(roster) { item ->
            RosterCard(
                item = item,
                onManualEdit = { onManualMark(item) },
                onClick = {
                    onEmployeeClick(
                        EmployeeItem(
                            id = item.memberId,
                            name = item.name,
                            employeeCode = item.employeeCode,
                            avatarUrl = item.avatarUrl,
                            status = "ACTIVE"
                        )
                    )
                }
            )
        }
    }
}

// Subtab: Requests
@Composable
fun AttendanceRequestsSubTab(
    requests: List<AttendanceRequestItem>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    val colors = LocalBizoraColors.current
    var statusFilter by remember { mutableStateOf("PENDING") }

    val filtered = remember(requests, statusFilter) {
        when (statusFilter) {
            "PENDING" -> requests.filter { it.status == RequestStatus.PENDING }
            "APPROVED" -> requests.filter { it.status == RequestStatus.APPROVED }
            else -> requests.filter { it.status == RequestStatus.REJECTED }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Status Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("PENDING" to "Pending", "APPROVED" to "Approved", "REJECTED" to "Rejected").forEach { (code, label) ->
                val isSelected = statusFilter == code
                val count = when (code) {
                    "PENDING" -> requests.count { it.status == RequestStatus.PENDING }
                    "APPROVED" -> requests.count { it.status == RequestStatus.APPROVED }
                    else -> requests.count { it.status == RequestStatus.REJECTED }
                }

                Surface(
                    color = if (isSelected) colors.brandPrimary else colors.surface,
                    shape = RoundedCornerShape(999.dp),
                    border = if (isSelected) null else BorderStroke(1.dp, colors.border),
                    modifier = Modifier.clickable { statusFilter = code }
                ) {
                    Text(
                        text = "$label ($count)",
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) colors.textInverse else colors.textPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when (statusFilter) {
                    "PENDING" -> {
                        EmptyState(
                            illustration = { DualPhoneReviewedIllustration(size = 110.dp) },
                            title = "You're all caught up",
                            description = "No pending check-in or check-out requests waiting for your review."
                        )
                    }
                    "APPROVED" -> {
                        EmptyState(
                            illustration = { DocCheckmarkIllustration(size = 100.dp) },
                            title = "No approved requests today",
                            description = "Approved attendance requests for today will appear here."
                        )
                    }
                    else -> {
                        EmptyState(
                            illustration = { DocCheckmarkIllustration(size = 100.dp) },
                            title = "No rejected requests today",
                            description = "Rejected attendance requests for today will appear here."
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(filtered) { request ->
                    AttendanceRequestCard(
                        request = request,
                        onApprove = { onApprove(request.id) },
                        onReject = { onReject(request.id) }
                    )
                }
            }
        }
    }
}

// Subtab: Daily
@Composable
fun AttendanceDailySubTab(
    roster: List<RosterItem>,
    filterAttendanceStatus: String = "ALL",
    onOpenFilterAttendance: () -> Unit = {},
    onManualMark: (RosterItem) -> Unit,
    onEmployeeClick: (EmployeeItem) -> Unit = {}
) {
    val colors = LocalBizoraColors.current
    var statusFilter by remember { mutableStateOf(filterAttendanceStatus) }

    LaunchedEffect(filterAttendanceStatus) {
        statusFilter = filterAttendanceStatus
    }

    val filtered = remember(roster, statusFilter) {
        if (statusFilter == "ALL") roster
        else roster.filter { it.status.name.equals(statusFilter, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Date & Status Header
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = colors.brandPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy")),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${filtered.size} of ${roster.size} shown",
                        fontSize = 12.5.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onOpenFilterAttendance,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceMuted)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = colors.brandPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }

        // Filter Pills: All, Present, Pending, Not Marked, Absent
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ALL" to "All", "PRESENT" to "Present", "NOT_MARKED" to "Not Marked", "ABSENT" to "Absent").forEach { (code, label) ->
                val isSelected = statusFilter == code
                Surface(
                    color = if (isSelected) colors.brandPrimary else colors.surface,
                    shape = RoundedCornerShape(999.dp),
                    border = if (isSelected) null else BorderStroke(1.dp, colors.border),
                    modifier = Modifier.clickable { statusFilter = code }
                ) {
                    Text(
                        text = label,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) colors.textInverse else colors.textPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (roster.isEmpty()) {
                    EmptyState(
                        illustration = { CalendarClockIllustration(size = 90.dp) },
                        title = "No attendance records",
                        description = "No attendance records found for today."
                    )
                } else {
                    EmptyState(
                        illustration = { SearchEmptyDocIllustration(size = 90.dp) },
                        title = "No matching employees",
                        description = "No employees currently match the selected filter.",
                        actionLabel = "Clear filter",
                        onAction = { statusFilter = "ALL" }
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(filtered) { member ->
                    RosterCard(
                        item = member,
                        onManualEdit = { onManualMark(member) },
                        onClick = {
                            onEmployeeClick(
                                EmployeeItem(
                                    id = member.memberId,
                                    name = member.name,
                                    employeeCode = member.employeeCode,
                                    avatarUrl = member.avatarUrl,
                                    status = "ACTIVE"
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

// Subtab: Reports
@Composable
fun AttendanceReportsSubTab(
    roster: List<RosterItem>,
    workplaceName: String = "Workplace"
) {
    val colors = LocalBizoraColors.current
    val context = LocalContext.current
    var selectedRange by remember { mutableStateOf("This month") }
    var exportedReport by remember { mutableStateOf<ExportedReport?>(null) }
    var isGenerating by remember { mutableStateOf(false) }

    val total = roster.size
    val present = roster.count { it.status == AttendanceStatus.PRESENT }
    val absent = roster.count { it.status == AttendanceStatus.ABSENT }
    val pending = roster.count { it.status == AttendanceStatus.PENDING }
    val rate = if (total > 0) ((present.toFloat() / total.toFloat()) * 100).toInt() else 0

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Analytics Hero Banner
        item {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.brandTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Insights, contentDescription = null, tint = colors.brandPrimary, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Attendance Analytics & Insights", fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            Text("Real-time workforce attendance metrics and logs.", fontSize = 13.sp, color = colors.textSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4-Card Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCounterCard(title = "Present", count = present, color = colors.success, bg = colors.successTint, modifier = Modifier.weight(1f))
                        MetricCounterCard(title = "Attendance Rate", countStr = "$rate%", color = colors.brandPrimary, bg = colors.brandTint, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCounterCard(title = "Absent", count = absent, color = colors.danger, bg = colors.dangerTint, modifier = Modifier.weight(1f))
                        MetricCounterCard(title = "Pending Approvals", count = pending, color = colors.pending, bg = colors.pendingTint, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Export Report Options
        item {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Export Timesheets & Reports", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Text("Generate real formatted audit logs and save directly to your device storage in Excel, PDF, or CSV.", fontSize = 13.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp, bottom = 14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                AdMobInterstitialHelper.showAdThenExecute(context) {
                                    try {
                                        isGenerating = true
                                        val report = ReportExporter.exportToExcel(
                                            context = context,
                                            workplaceName = workplaceName,
                                            roster = roster
                                        )
                                        exportedReport = report
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error generating Excel: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isGenerating = false
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, tint = colors.brandPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Excel", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = colors.brandPrimary)
                        }

                        Button(
                            onClick = {
                                AdMobInterstitialHelper.showAdThenExecute(context) {
                                    try {
                                        isGenerating = true
                                        val report = ReportExporter.exportToPdf(
                                            context = context,
                                            workplaceName = workplaceName,
                                            roster = roster
                                        )
                                        exportedReport = report
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error generating PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isGenerating = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = colors.textInverse, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textInverse)
                        }

                        OutlinedButton(
                            onClick = {
                                AdMobInterstitialHelper.showAdThenExecute(context) {
                                    try {
                                        isGenerating = true
                                        val report = ReportExporter.exportToCsv(
                                            context = context,
                                            workplaceName = workplaceName,
                                            roster = roster
                                        )
                                        exportedReport = report
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error generating CSV: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isGenerating = false
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CSV", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0284C7))
                        }
                    }
                }
            }
        }

        if (roster.isEmpty()) {
            item {
                EmptyState(
                    illustration = { BarChartSearchIllustration(size = 110.dp) },
                    title = "No records for this period",
                    description = "There are no attendance records within the selected date range."
                )
            }
        }
    }

    // Modal: Report Export Bottom Sheet
    if (exportedReport != null) {
        ReportExportModal(
            report = exportedReport,
            onDismiss = { exportedReport = null }
        )
    }
}

// =========================================================================
// 3. EMPLOYEES SCREEN (WITH 5 INTERNAL TABS: Teams, All, Active, Invited, Requests)
// =========================================================================

@Composable
fun TeamDirectoryCard(
    team: Team,
    members: List<EmployeeItem>,
    onAddMember: () -> Unit,
    onViewMembers: () -> Unit
) {
    val colors = LocalBizoraColors.current
    val context = LocalContext.current
    val teamColor = try {
        Color(android.graphics.Color.parseColor(team.color))
    } catch (_: Exception) {
        colors.brandPrimary
    }

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Color Pill + Team Name + Code Pill + Default Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(teamColor)
                    )
                    Text(
                        text = team.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Team Code Pill
                    Surface(
                        color = colors.surfaceMuted,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.8.dp, colors.border),
                        modifier = Modifier.clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Team Code", team.displayCode))
                            Toast.makeText(context, "Team code copied: ${team.displayCode}", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "#${team.displayCode}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.brandPrimary,
                                letterSpacing = 0.5.sp
                            )
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy code",
                                tint = colors.brandPrimary,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }

                    if (team.isDefault) {
                        Surface(
                            color = colors.brandTint,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Default",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.brandPrimary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            if (!team.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = team.description,
                    fontSize = 13.5.sp,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Member Avatars & Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((-6).dp)
                ) {
                    val previewMembers = members.take(4)
                    previewMembers.forEach { member ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .border(2.dp, colors.surface, CircleShape)
                                .background(colors.surfaceMuted),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = member.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.brandPrimary
                            )
                        }
                    }

                    if (members.size > 4) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .border(2.dp, colors.surface, CircleShape)
                                .background(colors.brandTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+${members.size - 4}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.brandPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${members.size} ${if (members.size == 1) "member" else "members"}",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Add Member to this team
                Button(
                    onClick = onAddMember,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = colors.textInverse, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Member", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textInverse)
                }

                // View members
                OutlinedButton(
                    onClick = onViewMembers,
                    border = BorderStroke(1.dp, colors.border),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("View Roster", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                }
            }
        }
    }
}

@Composable
fun EmployeesScreenView(
    employees: List<EmployeeItem>,
    teams: List<Team> = emptyList(),
    isEmployeesLoading: Boolean = false,
    initialSubTab: String = "Teams",
    initialSelectedTeamId: String? = null,
    onAddEmployeeClick: (String?) -> Unit,
    onCreateTeamClick: () -> Unit = {},
    onOpenJoinQr: (String?) -> Unit = {},
    onEmployeeClick: (EmployeeItem) -> Unit = {},
    onCopyInviteLink: (String) -> Unit
) {
    val colors = LocalBizoraColors.current
    var subTab by remember(initialSubTab) { mutableStateOf(initialSubTab) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTeamId by remember(initialSelectedTeamId) { mutableStateOf(initialSelectedTeamId) }
    val internalTabs = listOf("Teams", "All", "Active", "Invited", "Requests")

    if (isEmployeesLoading && employees.isEmpty()) {
        EmployerEmployeesSkeleton()
        return
    }

    // Filter employees by subtab and selected team
    val filtered = remember(employees, subTab, searchQuery, selectedTeamId) {
        val byTab = when (subTab) {
            "Active" -> employees.filter { it.status.equals("ACTIVE", ignoreCase = true) }
            "Invited" -> employees.filter { it.status.equals("INVITED", ignoreCase = true) }
            "Requests" -> emptyList() // Render join requests in dedicated subtab view
            else -> employees
        }
        val byTeam = if (selectedTeamId == null) byTab else {
            val selTeam = teams.find { it.id == selectedTeamId }
            byTab.filter { emp ->
                emp.teamId == selectedTeamId || (selTeam != null && emp.teamName.equals(selTeam.name, ignoreCase = true))
            }
        }
        if (searchQuery.isBlank()) byTeam
        else byTeam.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.employeeCode.contains(searchQuery, ignoreCase = true) ||
            (it.invitedEmail?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Internal Segmented Tabs Bar
        SegmentedSubTabs(
            tabs = internalTabs,
            selectedTab = subTab,
            onTabSelected = { subTab = it },
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (subTab == "Teams") {
            // Header for Teams
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Workplace Teams (${teams.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Button(
                    onClick = onCreateTeamClick,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = colors.textInverse, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Team", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textInverse)
                }
            }

            if (teams.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        illustration = { EmptyTeamIllustration(size = 90.dp) },
                        title = "No Teams Created Yet",
                        description = "Create teams (squads, departments or shifts) to group and organize your employees.",
                        actionLabel = "Create First Team",
                        onAction = onCreateTeamClick
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(teams) { team ->
                        val teamMembers = employees.filter {
                            it.teamId == team.id || it.teamName.equals(team.name, ignoreCase = true)
                        }
                        TeamDirectoryCard(
                            team = team,
                            members = teamMembers,
                            onAddMember = { onAddEmployeeClick(team.id) },
                            onViewMembers = {
                                selectedTeamId = team.id
                                subTab = "All"
                            }
                        )
                    }
                }
            }
        } else if (subTab == "Requests") {
            // Join Requests Empty / List View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    illustration = { DocCheckmarkIllustration(size = 90.dp) },
                    title = "No Pending Join Requests",
                    description = "When staff scan your workplace QR code to join, pending requests appear here."
                )
            }
        } else {
            // Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search employees by name, code or email...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(22.dp)) },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedBorderColor = colors.brandPrimary,
                    unfocusedBorderColor = colors.border
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            )

            // Hierarchy Row: Teams Filter Chips (Workplace -> Team -> Team Members)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "All Teams" Chip
                    item {
                        val isAllSelected = selectedTeamId == null
                        Surface(
                            color = if (isAllSelected) colors.brandPrimary else colors.surface,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, if (isAllSelected) colors.brandPrimary else colors.border),
                            modifier = Modifier.clickable { selectedTeamId = null }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "All Teams",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isAllSelected) colors.textInverse else colors.textPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isAllSelected) colors.textInverse.copy(alpha = 0.2f) else colors.surfaceMuted)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${employees.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAllSelected) colors.textInverse else colors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Individual Team Chips
                    items(teams) { team ->
                        val isSelected = selectedTeamId == team.id
                        val teamCount = employees.count {
                            it.teamId == team.id || it.teamName.equals(team.name, ignoreCase = true)
                        }
                        val teamColor = try {
                            Color(android.graphics.Color.parseColor(team.color))
                        } catch (_: Exception) {
                            colors.brandPrimary
                        }

                        Surface(
                            color = if (isSelected) colors.brandPrimary else colors.surface,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, if (isSelected) colors.brandPrimary else colors.border),
                            modifier = Modifier.clickable { selectedTeamId = team.id }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) colors.textInverse else teamColor)
                                )
                                Text(
                                    text = team.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) colors.textInverse else colors.textPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isSelected) colors.textInverse.copy(alpha = 0.2f) else colors.surfaceMuted)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$teamCount",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) colors.textInverse else colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // "+ Team" Action Button
                Surface(
                    color = colors.brandTint.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, colors.brandPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable { onCreateTeamClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New Team",
                            tint = colors.brandPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Team",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.brandPrimary
                        )
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Workplace Join QR & Link Card
                item {
                    Surface(
                        color = colors.brandTint.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, colors.brandPrimary.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenJoinQr(selectedTeamId) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.brandPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Team Invite QR & Link", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                                Text("Display scannable team QR or share direct invite link", fontSize = 12.sp, color = colors.textSecondary)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.brandPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        if (selectedTeamId != null) {
                            EmptyState(
                                illustration = { EmptyTeamIllustration(size = 90.dp) },
                                title = "No members in this team",
                                description = "Add an employee email to invite them into this team.",
                                actionLabel = "Add employee",
                                onAction = { onAddEmployeeClick(selectedTeamId) }
                            )
                        } else if (searchQuery.isNotBlank()) {
                            EmptyState(
                                illustration = { SearchEmptyDocIllustration(size = 90.dp) },
                                title = "No matching employees",
                                description = "Try another name, email, or employee code.",
                                actionLabel = "Clear search",
                                onAction = { searchQuery = "" }
                            )
                        } else if (subTab == "Invited") {
                            EmptyState(
                                illustration = { EnvelopePersonPlusIllustration(size = 90.dp) },
                                title = "No invitations yet",
                                description = "Add an employee email to invite them to this workplace.",
                                actionLabel = "Add employee",
                                onAction = { onAddEmployeeClick(null) }
                            )
                        } else {
                            EmptyState(
                                illustration = { PeopleGroupPlusIllustration(size = 90.dp) },
                                title = "No employees yet",
                                description = "Add an employee email to invite them to this workplace.",
                                actionLabel = "Add employee",
                                onAction = { onAddEmployeeClick(null) },
                                secondaryLabel = "Share link",
                                onSecondaryAction = { onOpenJoinQr(null) }
                            )
                        }
                    }
                } else {
                    items(filtered) { employee ->
                        EmployeeCard(
                            employee = employee,
                            onClick = { onEmployeeClick(employee) },
                            onCopyInvite = { onCopyInviteLink(employee.employeeCode) }
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 4. WORKPLACE SCREEN (WITH 2 INTERNAL TABS: Overview, Invite)
// =========================================================================

@Composable
fun WorkplaceScreenView(
    workplace: Workplace,
    repository: BizoraRepository,
    onSwitchWorkplace: () -> Unit,
    onOpenJoinQr: () -> Unit = {},
    onShowRating: () -> Unit = {},
    onShowUpdate: () -> Unit = {},
    onSignOut: () -> Unit
) {
    val colors = LocalBizoraColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var subTab by remember { mutableStateOf("Overview") }
    val internalTabs = listOf("Overview", "Invite")

    var requireWifi by remember { mutableStateOf(workplace.attendanceSettings?.requireWifi ?: false) }
    var allowHistory by remember { mutableStateOf(workplace.attendanceSettings?.allowEmployeeViewHistory ?: true) }
    var requireGeofence by remember(workplace.attendanceSettings) {
        mutableStateOf(workplace.attendanceSettings?.requireGeofence ?: false)
    }
    var latText by remember(workplace.attendanceSettings) {
        mutableStateOf(workplace.attendanceSettings?.latitude?.toString() ?: "")
    }
    var lngText by remember(workplace.attendanceSettings) {
        mutableStateOf(workplace.attendanceSettings?.longitude?.toString() ?: "")
    }
    var radiusMeters by remember(workplace.attendanceSettings) {
        mutableStateOf(workplace.attendanceSettings?.geofenceRadius ?: 100.0)
    }
    var isSavingSettings by remember { mutableStateOf(false) }
    var isLocating by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Internal Segmented Tabs Bar
        SegmentedSubTabs(
            tabs = internalTabs,
            selectedTab = subTab,
            onTabSelected = { subTab = it },
            modifier = Modifier.padding(bottom = 14.dp)
        )

        when (subTab) {
            "Overview" -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Workplace Info Card
                    item {
                        Surface(
                            color = colors.surface,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Workplace Details", fontWeight = FontWeight.SemiBold, fontSize = 16.5.sp, color = colors.textPrimary)
                                Spacer(modifier = Modifier.height(12.dp))

                                DetailRowItem(label = "Timezone", value = workplace.timezone)
                                DetailRowItem(label = "Address", value = workplace.address ?: "Not set")
                                DetailRowItem(label = "Office Wi-Fi SSID", value = workplace.wifiSsid ?: "Any network")
                                DetailRowItem(label = "Workplace Code", value = (workplace.code?.takeIf { it.isNotBlank() } ?: workplace.formattedCode).uppercase())
                            }
                        }
                    }

                    // Security & Verification Settings Card (Wi-Fi, History, Geofence)
                    item {
                        Surface(
                            color = colors.surface,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Attendance Settings", fontWeight = FontWeight.SemiBold, fontSize = 16.5.sp, color = colors.textPrimary)
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Enforce Wi-Fi Verification", fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, color = colors.textPrimary)
                                        Text("Flag requests submitted away from office network", fontSize = 12.5.sp, color = colors.textSecondary)
                                    }
                                    Switch(checked = requireWifi, onCheckedChange = { requireWifi = it })
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = colors.border)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Allow History Visibility", fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, color = colors.textPrimary)
                                        Text("Employees can inspect their attendance history", fontSize = 12.5.sp, color = colors.textSecondary)
                                    }
                                    Switch(checked = allowHistory, onCheckedChange = { allowHistory = it })
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = colors.border)

                                // Geofence Verification Settings
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Enforce Geolocation Verification", fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, color = colors.textPrimary)
                                        Text("Employees within workplace location can mark attendance instantly without QR scan or admin approval", fontSize = 12.sp, color = colors.textSecondary)
                                    }
                                    Switch(checked = requireGeofence, onCheckedChange = { requireGeofence = it })
                                }

                                if (requireGeofence) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        color = colors.surfaceMuted,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Office Coordinates", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = colors.textPrimary)
                                                OutlinedButton(
                                                    onClick = {
                                                        bizora.app.utils.AdMobRewardHelper.showRewardAd(
                                                            context = context,
                                                            onRewardCompleted = {
                                                                if (!LocationHelper.hasLocationPermission(context)) {
                                                                    Toast.makeText(context, "Location permission required. Please enable in Settings.", Toast.LENGTH_SHORT).show()
                                                                } else {
                                                                    isLocating = true
                                                                    coroutineScope.launch {
                                                                        val loc = LocationHelper.getCurrentLocation(context)
                                                                        isLocating = false
                                                                        if (loc != null) {
                                                                            latText = loc.latitude.toString()
                                                                            lngText = loc.longitude.toString()
                                                                            Toast.makeText(context, "Current GPS location captured!", Toast.LENGTH_SHORT).show()
                                                                        } else {
                                                                            Toast.makeText(context, "Could not acquire GPS fix. Please ensure GPS is ON.", Toast.LENGTH_SHORT).show()
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            onCancelled = {
                                                                Toast.makeText(context, "Location capture cancelled: Reward ad was not completed", Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(if (isLocating) "Locating..." else "Use GPS", fontSize = 11.5.sp)
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = latText,
                                                    onValueChange = { latText = it },
                                                    label = { Text("Latitude", fontSize = 11.sp) },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true
                                                )
                                                OutlinedTextField(
                                                    value = lngText,
                                                    onValueChange = { lngText = it },
                                                    label = { Text("Longitude", fontSize = 11.sp) },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true
                                                )
                                            }

                                            // Radius Selection
                                            Column {
                                                Text("Geofence Radius: ${radiusMeters.toInt()} meters", fontSize = 12.5.sp, color = colors.textSecondary)
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    listOf(50.0, 100.0, 200.0, 500.0).forEach { r ->
                                                        val isSelected = radiusMeters == r
                                                        FilterChip(
                                                            selected = isSelected,
                                                            onClick = { radiusMeters = r },
                                                            label = { Text("${r.toInt()}m", fontSize = 11.5.sp) }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        isSavingSettings = true
                                        val latVal = latText.toDoubleOrNull()
                                        val lngVal = lngText.toDoubleOrNull()
                                        repository.updateWorkplaceGeofence(
                                            workplaceId = workplace.id,
                                            enabled = requireGeofence,
                                            latitude = latVal,
                                            longitude = lngVal,
                                            radiusMeters = radiusMeters,
                                            onSuccess = {
                                                isSavingSettings = false
                                                Toast.makeText(context, "Attendance settings updated successfully!", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { err ->
                                                isSavingSettings = false
                                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    },
                                    enabled = !isSavingSettings,
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(42.dp)
                                ) {
                                    Text(if (isSavingSettings) "Saving..." else "Save Attendance Settings", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                                }
                            }
                        }
                    }

                    // Developer Testing Modals Preview Card
                    item {
                        Surface(
                            color = colors.surface,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Developer Preview & Testing", fontWeight = FontWeight.SemiBold, fontSize = 16.5.sp, color = colors.textPrimary)
                                Text("Test standard Play Store Rating and App Update dialogs", fontSize = 12.5.sp, color = colors.textSecondary)
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onShowRating,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Rate App", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    OutlinedButton(
                                        onClick = onShowUpdate,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) {
                                        Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = colors.brandPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Check Updates", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    // Actions
                    item {
                        OutlinedButton(
                            onClick = onSwitchWorkplace,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Switch Workplace", fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                        }
                    }

                    item {
                        Button(
                            onClick = onSignOut,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.danger),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = colors.textInverse, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sign Out", fontWeight = FontWeight.SemiBold, color = colors.textInverse, fontSize = 14.5.sp)
                        }
                    }
                }
            }

            "Invite" -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Surface(
                            color = colors.surface,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Workplace Join QR", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                                Text("Scan this QR code with the Bizora mobile app to join this workplace.", fontSize = 13.5.sp, color = colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))

                                QrCodeView(
                                    content = "bizora://join?workplace=${workplace.id}",
                                    size = 200.dp
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Surface(
                                    color = colors.surfaceMuted,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val cleanCode = (workplace.code?.takeIf { it.isNotBlank() } ?: workplace.formattedCode).uppercase()
                                        Column {
                                            Text("Workplace Code", fontSize = 11.5.sp, color = colors.textSecondary, fontWeight = FontWeight.SemiBold)
                                            Text(cleanCode, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, letterSpacing = 2.sp)
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("Workplace Code", cleanCode))
                                                    Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = colors.brandPrimary, modifier = Modifier.size(22.dp))
                                            }

                                            IconButton(
                                                onClick = {
                                                    val sendIntent = Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(Intent.EXTRA_TEXT, "Join my workplace ${workplace.name} on Bizora: https://bizora.app/join?code=$cleanCode")
                                                        type = "text/plain"
                                                    }
                                                    context.startActivity(Intent.createChooser(sendIntent, "Share Workplace Invite"))
                                                }
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = "Share", tint = colors.brandPrimary, modifier = Modifier.size(22.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// REUSABLE SUBCOMPONENTS
// =========================================================================

@Composable
fun MetricCounterCard(
    title: String,
    count: Int? = null,
    countStr: String? = null,
    color: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalBizoraColors.current
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = countStr ?: (count ?: 0).toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RosterCard(item: RosterItem, onManualEdit: () -> Unit, onClick: (() -> Unit)? = null) {
    val colors = LocalBizoraColors.current
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceMuted),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = colors.brandPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, fontWeight = FontWeight.SemiBold, fontSize = 15.5.sp, color = colors.textPrimary)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${item.employeeCode} • ${item.checkInTime ?: "Not checked in"}",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    if (!item.teamName.isNullOrBlank()) {
                        Surface(
                            color = colors.brandTint,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = item.teamName!!,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.brandPrimary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
            StatusPill(status = item.status)
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onManualEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun EmployeeCard(
    employee: EmployeeItem,
    onClick: () -> Unit = {},
    onCopyInvite: () -> Unit
) {
    val colors = LocalBizoraColors.current
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceMuted),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = employee.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = colors.brandPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = employee.name, fontWeight = FontWeight.SemiBold, fontSize = 15.5.sp, color = colors.textPrimary)
                Text(
                    text = "${employee.employeeCode} • ${employee.invitedEmail ?: "PIN only"}",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
                if (!employee.teamName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Team: ${employee.teamName}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.brandPrimary
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        when (employee.status) {
                            "ACTIVE" -> colors.successTint
                            "INVITED" -> colors.pendingTint
                            else -> colors.neutralTint
                        }
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = employee.status,
                    color = when (employee.status) {
                        "ACTIVE" -> colors.success
                        "INVITED" -> colors.pending
                        else -> colors.neutralStatus
                    },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (employee.status == "INVITED") {
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onCopyInvite, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Link, contentDescription = "Copy Link", tint = colors.brandPrimary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun StatusPill(status: AttendanceStatus) {
    val colors = LocalBizoraColors.current
    val bg: Color
    val textColor: Color
    when (status) {
        AttendanceStatus.PRESENT -> {
            bg = colors.successTint
            textColor = colors.success
        }
        AttendanceStatus.PENDING -> {
            bg = colors.pendingTint
            textColor = colors.pending
        }
        AttendanceStatus.ABSENT -> {
            bg = colors.dangerTint
            textColor = colors.danger
        }
        AttendanceStatus.HALF_DAY -> {
            bg = colors.pendingTint
            textColor = colors.pending
        }
        AttendanceStatus.LEAVE -> {
            bg = colors.brandTint
            textColor = colors.brandPrimary
        }
        AttendanceStatus.NOT_MARKED -> {
            bg = colors.neutralTint
            textColor = colors.neutralStatus
        }
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = status.name.replace("_", " "),
            color = textColor,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun DetailRowItem(label: String, value: String) {
    val colors = LocalBizoraColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = colors.textSecondary)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
    }
}
