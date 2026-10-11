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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bizora.app.data.model.*
import bizora.app.data.remote.EmployerJoinRequestItem
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
    WORKPLACE,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerDashboard(
    repository: BizoraRepository,
    onSignOut: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
    isDark: Boolean = false,
    onLockAppNow: () -> Unit = {},
    onCreateNewWorkplace: () -> Unit = {}
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
    val workplaceJoinRequests by repository.workplaceJoinRequests.collectAsState()
    val employees by repository.employees.collectAsState()
    val teams by repository.teams.collectAsState()
    val visibleTeams = remember(teams) {
        teams.filterNot { team ->
            team.isDefault && team.name.equals("General", ignoreCase = true) &&
                team.members.none { it.role.equals("EMPLOYEE", ignoreCase = true) }
        }
    }
    val notifications by repository.notifications.collectAsState()
    val currentUser by repository.currentUser.collectAsState()
    val mpinStatus by repository.mpinStatus.collectAsState()
    val isEmployerTodayLoading by repository.isEmployerTodayLoading.collectAsState()
    val isRosterLoading by repository.isRosterLoading.collectAsState()
    val isRequestsLoading by repository.isRequestsLoading.collectAsState()
    val isEmployeesLoading by repository.isEmployeesLoading.collectAsState()

    var showProfileScreen by remember { mutableStateOf(false) }
    var showSecuritySettingsScreen by remember { mutableStateOf(false) }
    var isAppLockDismissedForSession by remember { mutableStateOf(false) }
    var showWorkplaceSwitcher by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var showAddEmployeeModal by remember { mutableStateOf(false) }
    var showManualAttendanceDialog by remember { mutableStateOf(false) }
    var preselectedTeamIdForAdd by remember { mutableStateOf<String?>(null) }
    var showCreateTeamModal by remember { mutableStateOf(false) }
    var employeesActiveSubTab by remember { mutableStateOf("All") }
    var workplaceActiveSubTab by remember { mutableStateOf("Overview") }
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
    var isPullRefreshing by remember { mutableStateOf(false) }

    if (showSecuritySettingsScreen) {
        SecuritySettingsScreen(
            repository = repository,
            onBack = { showSecuritySettingsScreen = false },
            onLockAppNow = onLockAppNow
        )
        return
    }

    if (showProfileScreen) {
        ProfileSettingsScreen(
            repository = repository,
            onBack = { showProfileScreen = false },
            onOpenSecuritySettings = { showSecuritySettingsScreen = true },
            onToggleTheme = onToggleTheme,
            isDark = isDark,
            onSignOut = onSignOut
        )
        return
    }

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
            onExportReportClick = { _, format ->
                AdMobInterstitialHelper.showAdThenExecute(context) {
                    exportedReportForEmployeeDetails = ReportExporter.exportIndividualEmployeeReport(
                        context = context,
                        workplaceName = activeWorkplace.name,
                        employeeName = emp.name,
                        employeeCode = emp.employeeCode,
                        rosterHistory = roster,
                        format = format
                    )
                }
            },
            onRefresh = {
                repository.refreshWorkplaceDataAwait()
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

    if (showAddEmployeeModal) {
        InviteMemberScreen(
            workplaceName = activeWorkplace.name,
            teams = visibleTeams,
            initialTeamId = preselectedTeamIdForAdd,
            onBack = {
                showAddEmployeeModal = false
                preselectedTeamIdForAdd = null
            },
            onCreateTeam = {
                showCreateTeamModal = true
            },
            onInvite = { name, email, teamId ->
                repository.addEmployee(name, email = email, teamId = teamId)
            }
        )
        if (showCreateTeamModal) {
            CreateTeamDialog(
                onDismiss = { showCreateTeamModal = false },
                onCreateTeam = { name, description, color ->
                    repository.createTeam(name, description, color).map { }
                }
            )
        }
        return
    }

    // Bottom Navigation Tabs
    val tabs = remember {
        listOf(
            BottomTabItem(
                key = EmployerTab.TODAY,
                label = "Home",
                iconRenderer = { _, size, active -> HomeTabIcon(size = size, active = active) },
                badgeCount = 0
            ),
            BottomTabItem(
                key = EmployerTab.ATTENDANCE,
                label = "Attendance",
                iconRenderer = { _, size, active -> AttendanceTabIcon(size = size, active = active) }
            ),
            BottomTabItem(
                key = EmployerTab.WORKPLACE,
                label = "Workplace",
                iconRenderer = { _, size, active -> WorkplaceTabIcon(size = size, active = active) }
            ),
            BottomTabItem(
                key = EmployerTab.SETTINGS,
                label = "Settings",
                iconRenderer = { _, size, active -> SettingsTabIcon(size = size, active = active) }
            )
        )
    }

    Scaffold(
        topBar = {
            when (activeTab) {
                EmployerTab.TODAY -> {
                    val user by repository.currentUser.collectAsState(null)
                    val effectiveName = user?.name?.takeIf { it.isNotBlank() } ?: "Employer"
                    val effectiveEmail = user?.email?.takeIf { it.isNotBlank() } ?: "employer@bizora.app"
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
                        onProfileClick = { activeTab = EmployerTab.SETTINGS },
                        onSearchClick = {
                            workplaceActiveSubTab = "Teams"
                            activeTab = EmployerTab.WORKPLACE
                        }
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
                        workplaceName = activeWorkplace.name
                    )
                }
                EmployerTab.SETTINGS -> {
                    SettingsTopBar(
                        onSignOutClick = { showSignOutConfirm = true }
                    )
                }
            }
        },
        bottomBar = {
            Column {
                AdBanner()
                BizoraBottomBar(
                    tabs = tabs,
                    activeTab = activeTab,
                    onTabSelected = { activeTab = it }
                )
            }
        },
        containerColor = colors.canvas
    ) { padding ->
        BizoraPullRefreshBox(
            isRefreshing = isPullRefreshing,
            onRefresh = {
                isPullRefreshing = true
                coroutineScope.launch {
                    try {
                        repository.refreshWorkplaceDataAwait()
                    } finally {
                        isPullRefreshing = false
                    }
                }
            },
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
                            joinRequests = workplaceJoinRequests,
                            teams = visibleTeams,
                            totalEmployeesCount = employees.size,
                            showAppLockPrompt = (mpinStatus?.hasMpin != true && mpinStatus?.appLockEnabled != true && currentUser?.hasMpin != true && currentUser?.appLockEnabled != true && !repository.hasMpin() && !repository.isAppLockEnabled() && !isAppLockDismissedForSession),
                            onSetupAppLock = { showSecuritySettingsScreen = true },
                            onDismissAppLock = { isAppLockDismissedForSession = true },
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
                            onManualMark = {
                                manualMarkItem = it
                                showManualAttendanceDialog = true
                            },
                            onApproveRequest = { repository.approveRequest(it) },
                            onRejectRequest = { rejectTargetId = it },
                            onApproveJoinRequest = { repository.approveWorkplaceJoinRequest(it) },
                            onRejectJoinRequest = { repository.rejectWorkplaceJoinRequest(it) },
                            onNavigateToJoinRequests = {
                                employeesActiveSubTab = "Requests"
                                activeTab = EmployerTab.EMPLOYEES
                            },
                            onNavigateToAttendance = { activeTab = EmployerTab.ATTENDANCE },
                            onNavigateToEmployees = {
                                workplaceActiveSubTab = "Teams"
                                activeTab = EmployerTab.WORKPLACE
                            },
                            onNavigateToTeamsDirectory = {
                                workplaceActiveSubTab = "Teams"
                                activeTab = EmployerTab.WORKPLACE
                            },
                            onNavigateToTeamRoster = { teamId ->
                                workplaceActiveSubTab = "Teams"
                                activeTab = EmployerTab.WORKPLACE
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
                    onManualMark = {
                        manualMarkItem = it
                        showManualAttendanceDialog = true
                    },
                    onOpenHelp = { showHowItWorksModal = true },
                    onOpenFilterAttendance = { showFilterAttendanceModal = true },
                    onEmployeeClick = { viewingEmployeeDetails = it }
                )

                EmployerTab.EMPLOYEES -> EmployeesScreenView(
                    employees = employees,
                    teams = visibleTeams,
                    joinRequests = workplaceJoinRequests,
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
                    },
                    onApproveJoinRequest = { repository.approveWorkplaceJoinRequest(it) },
                    onRejectJoinRequest = { repository.rejectWorkplaceJoinRequest(it) }
                )

                EmployerTab.WORKPLACE -> WorkplaceScreenView(
                    workplace = activeWorkplace,
                    repository = repository,
                    teams = visibleTeams,
                    employees = employees,
                    initialSubTab = workplaceActiveSubTab,
                    onCreateTeamClick = { showCreateTeamModal = true },
                    onAddEmployeeClick = { teamId ->
                        preselectedTeamIdForAdd = teamId
                        showAddEmployeeModal = true
                    },
                    onViewTeamMembers = { teamId ->
                        workplaceActiveSubTab = "Teams"
                        activeTab = EmployerTab.WORKPLACE
                    },
                    onSwitchWorkplace = { showWorkplaceSwitcher = true },
                    onOpenJoinQr = {
                        preselectedTeamIdForAdd = null
                        showJoinQrModal = true
                    },
                    onOpenJoinQrForTeam = { teamId ->
                        preselectedTeamIdForAdd = teamId
                        showJoinQrModal = true
                    },
                    onShowRating = { showRatingModal = true },
                    onShowUpdate = { showUpdateModal = true },
                    onOpenProfile = { activeTab = EmployerTab.SETTINGS },
                    onOpenSecurity = { showSecuritySettingsScreen = true },
                    onSignOut = { showSignOutConfirm = true }
                )

                EmployerTab.SETTINGS -> SettingsScreenView(
                    repository = repository,
                    onOpenSecuritySettings = { showSecuritySettingsScreen = true },
                    onSignOutClick = { showSignOutConfirm = true }
                )
            }
        }
    }

    // Modal: Workplace Switcher
    if (showWorkplaceSwitcher) {
        WorkplaceSwitcherModal(
            workplaces = workplaces,
            activeWorkplaceId = activeWorkplace.id,
            currentActiveWorkplace = activeWorkplace,
            onSelectWorkplace = {
                repository.switchWorkplace(it.id)
                showWorkplaceSwitcher = false
            },
            onDismiss = { showWorkplaceSwitcher = false },
            onCreateNewWorkplace = {
                showWorkplaceSwitcher = false
                onCreateNewWorkplace()
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
    if (showManualAttendanceDialog) {
        ManualAttendanceDialog(
            item = manualMarkItem,
            allEmployees = if (roster.isNotEmpty()) roster else employees.map {
                RosterItem(
                    memberId = it.id,
                    name = it.name,
                    employeeCode = it.employeeCode,
                    avatarUrl = it.avatarUrl
                )
            },
            onDismiss = {
                showManualAttendanceDialog = false
                manualMarkItem = null
            },
            onSave = { memberId, status, reason ->
                repository.markManualAttendance(memberId, status, reason)
            }
        )
    }

    // Modal: Create Team
    if (showCreateTeamModal) {
        CreateTeamDialog(
            onDismiss = { showCreateTeamModal = false },
            onCreateTeam = { name, description, color ->
                val res = repository.createTeam(name, description, color)
                if (res.isSuccess) {
                    Toast.makeText(context, "Team '$name' created successfully!", Toast.LENGTH_SHORT).show()
                }
                res.map { }
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
            teams = visibleTeams,
            initialTeamId = preselectedTeamIdForAdd,
            repository = repository,
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
    joinRequests: List<EmployerJoinRequestItem> = emptyList(),
    teams: List<Team> = emptyList(),
    totalEmployeesCount: Int,
    showAppLockPrompt: Boolean = false,
    onSetupAppLock: () -> Unit = {},
    onDismissAppLock: () -> Unit = {},
    onOpenQrModal: () -> Unit,
    onOpenJoinQr: () -> Unit = {},
    onOpenJoinQrForTeam: (String?) -> Unit = {},
    onSwitchWorkplace: () -> Unit,
    onManualMark: (RosterItem?) -> Unit,
    onApproveRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit,
    onApproveJoinRequest: (String) -> Unit = {},
    onRejectJoinRequest: (String) -> Unit = {},
    onNavigateToJoinRequests: () -> Unit = {},
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
    val pendingAttendanceCount = requests.size
    val pendingJoinCount = joinRequests.count { it.status.equals("PENDING", ignoreCase = true) }
    val totalApprovalsCount = pendingAttendanceCount + pendingJoinCount
    val effectiveEmployeesCount = if (totalEmployeesCount > 0) totalEmployeesCount else roster.size

    val currentDateFormatted = remember {
        java.time.LocalDate.now().format(
            java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM", java.util.Locale.ENGLISH)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas),
        contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 0.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. ACTIVE WORKPLACE (Full-width light olive banner matching reference design)
        item {
            val bannerBg = if (colors.isDark) Color(0xFF222C18) else Color(0xFFEFF5E4)
            val borderLine = if (colors.isDark) Color(0xFF2E3A20) else Color(0xFFE2ECCF)
            val iconBg = Color(0xFF36451C)
            val textGreen = if (colors.isDark) Color(0xFF86EFAC) else Color(0xFF36451C)
            val chevronBg = if (colors.isDark) Color(0xFF2A361E) else Color(0xFFDCE8C8)

            Surface(
                color = bannerBg,
                shape = RectangleShape,
                border = BorderStroke(1.dp, borderLine),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSwitchWorkplace() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Workplace Icon
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = "Workplace",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = workplace.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            color = if (colors.isDark) Color(0xFF2C391D) else Color(0xFFDCE8C8),
                            shape = CircleShape,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "ACTIVE WORKPLACE",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp,
                                color = if (colors.isDark) Color(0xFF9AE6B4) else Color(0xFF36451C),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                            )
                        }
                    }

                    // Circular Dropdown Arrow
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(chevronBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Switch Workplace",
                            tint = textGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (showAppLockPrompt) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    AppLockPromptCard(
                        onSetupClick = onSetupAppLock,
                        onDismissClick = onDismissAppLock
                    )
                }
            }
        }

        // 2. TODAY'S SNAPSHOT CARD
        item {
            val dayOfWeekStr = remember {
                java.time.LocalDate.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("EEE", java.util.Locale.ENGLISH)
                ).uppercase()
            }
            val dayOfMonthStr = remember {
                java.time.LocalDate.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("dd", java.util.Locale.ENGLISH)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Today’s snapshot",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF334155) else Color(0xFFECEFE5)),
                    shadowElevation = 0.5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Top Row: Date Pill + Status info + "Open QR" button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Date Pill (e.g. TUE 06)
                            Box(
                                modifier = Modifier
                                    .size(width = 52.dp, height = 54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (colors.isDark) Color(0xFF25331C) else Color(0xFFEFF5E4)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dayOfWeekStr,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (colors.isDark) Color(0xFF86EFAC) else Color(0xFF4B5927),
                                        lineHeight = 14.sp
                                    )
                                    Text(
                                        text = dayOfMonthStr,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (colors.isDark) Color(0xFFF1F5F9) else Color(0xFF1B2210),
                                        lineHeight = 22.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Separator line
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(38.dp)
                                    .background(if (colors.isDark) Color(0xFF334155) else Color(0xFFEFF2EA))
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Team Status
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = if (colors.isDark) Color(0xFF86EFAC) else Color(0xFF36451C),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Your team, today",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = (-0.1).sp,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (presentCount == 0) "Waiting for the first check-in." else "$presentCount checked in today.",
                                        fontSize = 13.5.sp,
                                        color = colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Open QR Pill Button
                            Surface(
                                onClick = onOpenQrModal,
                                color = if (colors.isDark) Color(0xFF25331C) else Color(0xFFEFF5E4),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF3D4F28) else Color(0xFFD6DEC3))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.QrCodeScanner,
                                        contentDescription = null,
                                        tint = if (colors.isDark) Color(0xFF86EFAC) else Color(0xFF36451C),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Open QR",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (colors.isDark) Color(0xFF86EFAC) else Color(0xFF36451C)
                                    )
                                }
                            }
                        }

                        // Divider
                        HorizontalDivider(
                            color = if (colors.isDark) Color(0xFF334155) else Color(0xFFEFF2EA),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 14.dp)
                        )

                        // Bottom Row: Present & Pending requests
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Present Stat
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onNavigateToAttendance() },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(if (colors.isDark) Color(0xFF25331C) else Color(0xFFEFF5E4)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(modifier = Modifier.size(24.dp)) {
                                        Icon(
                                            imageVector = Icons.Outlined.Person,
                                            contentDescription = null,
                                            tint = if (colors.isDark) Color(0xFF86EFAC) else Color(0xFF36451C),
                                            modifier = Modifier
                                                .size(22.dp)
                                                .align(Alignment.CenterStart)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .align(Alignment.BottomEnd)
                                                .clip(CircleShape)
                                                .background(if (colors.isDark) Color(0xFF86EFAC) else Color(0xFF36451C)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = if (colors.isDark) Color(0xFF1B240D) else Color.White,
                                                modifier = Modifier.size(7.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = presentCount.toString(),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                        lineHeight = 26.sp
                                    )
                                    Text(
                                        text = "Present",
                                        fontSize = 13.5.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            // Vertical Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(44.dp)
                                    .background(if (colors.isDark) Color(0xFF334155) else Color(0xFFEFF2EA))
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            // Pending requests Stat
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        if (pendingJoinCount > 0) {
                                            onNavigateToJoinRequests()
                                        } else {
                                            onNavigateToAttendance()
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(if (colors.isDark) Color(0xFF3D2A14) else Color(0xFFFEF3C7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Assignment,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = totalApprovalsCount.toString(),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                        lineHeight = 26.sp
                                    )
                                    Text(
                                        text = "Pending requests",
                                        fontSize = 13.5.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. QUICK ACTIONS SECTION
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Quick actions",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // 2x2 Grid of Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row 1: Invite + Attendances
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TodayQuickActionCard(
                            icon = Icons.Outlined.PersonAdd,
                            title = "Invite",
                            subtitle = "Add team members",
                            onClick = onOpenJoinQr,
                            modifier = Modifier.weight(1f)
                        )
                        TodayQuickActionCard(
                            icon = Icons.Outlined.CalendarMonth,
                            title = "Attendances",
                            subtitle = "Check team activity",
                            onClick = onNavigateToAttendance,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Approvals + Teams
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TodayQuickActionCard(
                            icon = Icons.Outlined.FactCheck,
                            title = "Approvals",
                            subtitle = if (totalApprovalsCount > 0) "$totalApprovalsCount pending" else "0 pending",
                            subtitleColor = Color(0xFFEA580C),
                            onClick = {
                                if (pendingJoinCount > 0) {
                                    onNavigateToJoinRequests()
                                } else {
                                    onNavigateToAttendance()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        TodayQuickActionCard(
                            icon = Icons.Outlined.Groups,
                            title = "Teams",
                            subtitle = "Organize your teams",
                            onClick = onNavigateToTeamsDirectory,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3.5 PENDING JOIN REQUESTS (if any)
        if (pendingJoinCount > 0) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Pending Approvals",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = if (colors.isDark) Color(0xFF3D2A14) else Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "$pendingJoinCount",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "View all",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (colors.isDark) Color(0xFF86EFAC) else Color(0xFF36451C),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onNavigateToJoinRequests() }
                                .padding(vertical = 2.dp, horizontal = 4.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        joinRequests.filter { it.status.equals("PENDING", ignoreCase = true) }
                            .take(3)
                            .forEach { req ->
                                WorkplaceJoinRequestCard(
                                    request = req,
                                    onApprove = { onApproveJoinRequest(req.realId) },
                                    onReject = { onRejectJoinRequest(req.realId) }
                                )
                            }
                    }
                }
            }
        }

        // 4. WORKPLACE TEAMS SECTION
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Header with "Workplace teams" and "View all >"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Workplace teams",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
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
                            color = if (colors.isDark) Color(0xFF86EFAC) else Color(0xFF36451C)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = if (colors.isDark) Color(0xFF86EFAC) else Color(0xFF36451C),
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
                    val displayTeams = teams.take(4)

                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (displayTeams.isEmpty()) {
                            Text(
                                text = "No teams yet. Create your first team to get started.",
                                fontSize = 14.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                        displayTeams.forEachIndexed { index, team ->
                            val dotColor = when (index % 4) {
                                0 -> Color(0xFF36451C) // Olive green
                                1 -> Color(0xFF64748B) // Slate gray
                                2 -> Color(0xFF2563EB) // Blue
                                else -> Color(0xFF0D9488) // Teal
                            }
                            val count = team.members.count { it.role.equals("EMPLOYEE", ignoreCase = true) }
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
                                        fontSize = 16.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = subtitleText,
                                        fontSize = 14.sp,
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
        onClick = onClick,
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF334155) else Color(0xFFE8ECE1)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (colors.isDark) Color(0xFF25331C) else Color(0xFFEFF5E4)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (colors.isDark) Color(0xFF86EFAC) else Color(0xFF36451C),
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    color = subtitleColor ?: colors.textSecondary,
                    fontWeight = if (subtitleColor != null) FontWeight.SemiBold else FontWeight.Normal,
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
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "Mark attendance, live QR & manual entry.",
                    fontSize = 14.sp,
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
                        fontSize = 13.5.sp,
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
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "Manage staff, join links and access.",
                    fontSize = 14.sp,
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
                        fontSize = 13.5.sp,
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
    workplaceName: String
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
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "Teams, Wi-Fi and invite.",
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }
        }
    }
}

// Top Bar: Settings Tab
@Composable
fun SettingsTopBar(
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
                    text = "Settings",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "Account, preferences and security.",
                    fontSize = 14.sp,
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

/**
 * Viewfinder accent with 4 rounded corner brackets framing the QR code
 */
@Composable
private fun QrViewfinder(
    bracketColor: Color,
    modifier: Modifier = Modifier,
    bracketLength: androidx.compose.ui.unit.Dp = 26.dp,
    bracketWidth: androidx.compose.ui.unit.Dp = 3.5.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 10.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .drawBehind {
                val strokePx = bracketWidth.toPx()
                val lengthPx = bracketLength.toPx()
                val radiusPx = cornerRadius.toPx()
                val stroke = Stroke(width = strokePx, cap = StrokeCap.Round)

                val w = size.width
                val h = size.height

                // Top-Left bracket
                val topLeftPath = Path().apply {
                    moveTo(0f, lengthPx)
                    lineTo(0f, radiusPx)
                    quadraticTo(0f, 0f, radiusPx, 0f)
                    lineTo(lengthPx, 0f)
                }
                drawPath(topLeftPath, color = bracketColor, style = stroke)

                // Top-Right bracket
                val topRightPath = Path().apply {
                    moveTo(w - lengthPx, 0f)
                    lineTo(w - radiusPx, 0f)
                    quadraticTo(w, 0f, w, radiusPx)
                    lineTo(w, lengthPx)
                }
                drawPath(topRightPath, color = bracketColor, style = stroke)

                // Bottom-Left bracket
                val bottomLeftPath = Path().apply {
                    moveTo(0f, h - lengthPx)
                    lineTo(0f, h - radiusPx)
                    quadraticTo(0f, h, radiusPx, h)
                    lineTo(lengthPx, h)
                }
                drawPath(bottomLeftPath, color = bracketColor, style = stroke)

                // Bottom-Right bracket
                val bottomRightPath = Path().apply {
                    moveTo(w - lengthPx, h)
                    lineTo(w - radiusPx, h)
                    quadraticTo(w, h, w, h - radiusPx)
                    lineTo(w, h - lengthPx)
                }
                drawPath(bottomRightPath, color = bracketColor, style = stroke)
            }
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
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
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = if (colors.isDark) Color(0xFF141912) else Color(0xFFF8F8F1),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD5D5D0))
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Attendance QR",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isOpen) "Employees can scan now" else "Session closed",
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (colors.isDark) Color(0xFF22281F) else Color(0xFFF0F0EB))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = if (colors.isDark) colors.textPrimary else Color(0xFF5E6455),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (isOpen && session != null) {
                // Check-In / Check-Out Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (colors.isDark) Color(0xFF1E241B) else Color(0xFFF3F3ED))
                        .border(
                            1.dp,
                            if (colors.isDark) Color(0xFF2E382A) else Color(0xFFE5E7DC),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isCheckIn = qrTab == "checkin"
                    val checkInBg = if (isCheckIn) Color(0xFF5E7128) else Color.Transparent
                    val checkInContentColor = if (isCheckIn) Color.White else colors.textPrimary
                    val checkInIconTint = if (isCheckIn) Color.White else Color(0xFF5E7128)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
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
                                tint = checkInIconTint,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Check-In",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = checkInContentColor
                            )
                        }
                    }

                    val isCheckOut = qrTab == "checkout"
                    val checkOutBg = if (isCheckOut) Color(0xFFDE7007) else Color.Transparent
                    val checkOutContentColor = if (isCheckOut) Color.White else colors.textPrimary
                    val checkOutIconTint = if (isCheckOut) Color.White else Color(0xFFDE7007)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
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
                                tint = checkOutIconTint,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Check-Out",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = checkOutContentColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val rawPayload = session.qrPayload ?: "bizora://attendance?session_id=${session.session.id}&action=checkin"
                val currentPayload = if (qrTab == "checkout") {
                    session.checkoutQrPayload ?: (
                        if (rawPayload.contains("action=checkin")) rawPayload.replace("action=checkin", "action=checkout")
                        else if (rawPayload.contains("?")) "$rawPayload&action=checkout"
                        else "$rawPayload?action=checkout"
                    )
                } else {
                    rawPayload
                }

                // Main White Card containing "Session live", QR Viewfinder, Title & Subtitle
                Surface(
                    color = if (colors.isDark) Color(0xFF1E241B) else Color.White,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF2E382A) else Color(0xFFE8EDE1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // "Session live" pill badge
                        val isCheckIn = qrTab == "checkin"
                        val badgeBg = if (isCheckIn) {
                            if (colors.isDark) Color(0xFF25301E) else Color(0xFFEEF1DF)
                        } else {
                            if (colors.isDark) Color(0xFF382613) else Color(0xFFFEF4E1)
                        }
                        val badgeBorder = if (isCheckIn) {
                            if (colors.isDark) Color(0xFF384A2C) else Color(0xFFDEE3C8)
                        } else {
                            if (colors.isDark) Color(0xFF5A3C1C) else Color(0xFFFDE68A)
                        }
                        val badgeDotColor = if (isCheckIn) Color(0xFF5E7128) else Color(0xFFDE7007)
                        val badgeTextColor = if (isCheckIn) {
                            if (colors.isDark) Color(0xFFB5C99A) else Color(0xFF3B4A1A)
                        } else {
                            if (colors.isDark) Color(0xFFFCD34D) else Color(0xFF92400E)
                        }

                        Surface(
                            color = badgeBg,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, badgeBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(badgeDotColor)
                                )
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = "Session live",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = badgeTextColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // QR Viewfinder with colored corner brackets
                        val bracketColor = if (isCheckIn) Color(0xFF5E7128) else Color(0xFFDE7007)

                        QrViewfinder(
                            bracketColor = bracketColor,
                            bracketLength = 26.dp,
                            bracketWidth = 3.5.dp,
                            cornerRadius = 10.dp
                        ) {
                            QrCodeView(
                                content = currentPayload,
                                size = 210.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (isCheckIn) "Scan to check in" else "Scan to check out",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "Attendance is recorded instantly.",
                            fontSize = 13.5.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Time Information Card: Opened & Auto-closes
                val openedDisplay = session.session.openedAt.trim().removePrefix("0").ifEmpty { "7:09 PM" }
                val closesDisplay = session.session.expiresAt.trim().removePrefix("0").ifEmpty { "8:00 PM" }

                Surface(
                    color = if (colors.isDark) Color(0xFF1F241C) else Color(0xFFF5F5EE),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF2E382A) else Color(0xFFE8EDE1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Opened
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = if (colors.isDark) colors.textSecondary else Color(0xFF5E6455),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Opened",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = openedDisplay,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            }
                        }

                        // Vertical Divider
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .width(1.dp)
                                .background(if (colors.isDark) Color(0xFF2E382A) else Color(0xFFE0E4D5))
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Right: Auto-closes
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = if (colors.isDark) colors.textSecondary else Color(0xFF5E6455),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Auto-closes",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = closesDisplay,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "No approval needed.",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    onClick = onCloseSession,
                    color = Color(0xFFFEE7E7),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "End session",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(colors.brandTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = colors.brandPrimary,
                        modifier = Modifier.size(36.dp)
                    )
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E7128)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Open Attendance Session",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp,
                        color = Color.White
                    )
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
    onManualMark: (RosterItem?) -> Unit,
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
    onManualMark: (RosterItem?) -> Unit,
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
                        onManualMark(roster.firstOrNull())
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
                        Text(
                            text = if (isOpen) "Attendance Session" else "Open Attendance Session",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = if (isOpen) Color(0xFFDCFCE7) else (if (colors.isDark) Color(0xFF262C22) else Color(0xFFF1F5E9)),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (isOpen) Color(0xFF86EFAC) else (if (colors.isDark) Color(0xFF374230) else Color(0xFFE2E8D8)))
                            ) {
                                Text(
                                    text = if (isOpen) "Active" else "Inactive",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isOpen) Color(0xFF16A34A) else colors.textSecondary,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "Front-desk live QR code session",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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

        items(roster, key = { it.memberId }) { item ->
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
                items(filtered, key = { it.id }) { request ->
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
                items(filtered, key = { it.memberId }) { member ->
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
    onViewMembers: () -> Unit,
    onInvite: (() -> Unit)? = null
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Add Member to this team
                Button(
                    onClick = onAddMember,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = colors.textInverse, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textInverse)
                }

                // View members
                OutlinedButton(
                    onClick = onViewMembers,
                    border = BorderStroke(1.dp, colors.border),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Roster", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                }

                if (onInvite != null) {
                    OutlinedButton(
                        onClick = onInvite,
                        border = BorderStroke(1.dp, colors.brandPrimary.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.brandTint.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = "Team Invite", tint = colors.brandPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Invite", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.brandPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun EmployeesScreenView(
    employees: List<EmployeeItem>,
    teams: List<Team> = emptyList(),
    joinRequests: List<EmployerJoinRequestItem> = emptyList(),
    isEmployeesLoading: Boolean = false,
    initialSubTab: String = "All",
    initialSelectedTeamId: String? = null,
    onAddEmployeeClick: (String?) -> Unit,
    onCreateTeamClick: () -> Unit = {},
    onOpenJoinQr: (String?) -> Unit = {},
    onEmployeeClick: (EmployeeItem) -> Unit = {},
    onCopyInviteLink: (String) -> Unit,
    onApproveJoinRequest: (String) -> Unit = {},
    onRejectJoinRequest: (String) -> Unit = {}
) {
    val colors = LocalBizoraColors.current
    var subTab by remember(initialSubTab) { mutableStateOf(initialSubTab) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTeamId by remember(initialSelectedTeamId) { mutableStateOf(initialSelectedTeamId) }
    val internalTabs = listOf("All", "Active", "Invited", "Requests")

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
        val invitedCount = employees.count { it.status.equals("INVITED", ignoreCase = true) }
        val pendingRequests = remember(joinRequests) {
            joinRequests.filter { it.status.equals("PENDING", ignoreCase = true) }
        }

        // Internal Segmented Tabs Bar
        SegmentedSubTabs(
            tabs = internalTabs,
            selectedTab = subTab,
            onTabSelected = { subTab = it },
            badges = buildMap {
                if (invitedCount > 0) put("Invited", invitedCount)
                if (pendingRequests.isNotEmpty()) put("Requests", pendingRequests.size)
            },
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (subTab == "Requests") {
            if (pendingRequests.isEmpty()) {
                // Join Requests Empty State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        illustration = { DocCheckmarkIllustration(size = 90.dp) },
                        title = "No Pending Join Requests",
                        description = "When staff scan your team QR code or use a share link to join, pending requests appear here for approval."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
                ) {
                    items(pendingRequests, key = { it.realId }) { req ->
                        WorkplaceJoinRequestCard(
                            request = req,
                            onApprove = { onApproveJoinRequest(req.realId) },
                            onReject = { onRejectJoinRequest(req.realId) }
                        )
                    }
                }
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
                    items(teams, key = { it.id }) { team ->
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
                // Email Invitations Info Banner for Invited Tab
                if (subTab == "Invited") {
                    item {
                        Surface(
                            color = colors.brandTint.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.brandPrimary.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = colors.brandPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Direct Email Invitations: Employees invited by email join automatically without needing employer approval when they sign in. (QR code and link requests require approval under Requests)",
                                    fontSize = 12.sp,
                                    color = colors.textPrimary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

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
                                Text("Display scannable team QR or share direct invite link (requires approval)", fontSize = 12.sp, color = colors.textSecondary)
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
                                title = "No email invitations yet",
                                description = "Invite employees directly by email. They will automatically join their assigned team upon signing in without requiring your approval.",
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
                    items(filtered, key = { it.id }) { employee ->
                        EmployeeCard(
                            employee = employee,
                            teams = teams,
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
// 4. WORKPLACE SCREEN (WITH 3 INTERNAL TABS: Overview, Teams, Invite)
// =========================================================================

@Composable
fun WorkplaceScreenView(
    workplace: Workplace,
    repository: BizoraRepository,
    teams: List<Team> = emptyList(),
    employees: List<EmployeeItem> = emptyList(),
    initialSubTab: String = "Overview",
    onCreateTeamClick: () -> Unit = {},
    onAddEmployeeClick: (String?) -> Unit = {},
    onViewTeamMembers: (String) -> Unit = {},
    onSwitchWorkplace: () -> Unit,
    onOpenJoinQr: () -> Unit = {},
    onOpenJoinQrForTeam: (String?) -> Unit = {},
    onShowRating: () -> Unit = {},
    onShowUpdate: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenSecurity: () -> Unit = {},
    onSignOut: () -> Unit
) {
    val colors = LocalBizoraColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var subTab by remember(initialSubTab) { mutableStateOf(initialSubTab) }
    val internalTabs = listOf("Overview", "Teams", "Invite")

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
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.brandPrimary,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(42.dp)
                                ) {
                                    Text(
                                        text = if (isSavingSettings) "Saving..." else "Save Attendance Settings",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                        color = Color.White
                                    )
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

                    // Settings & Security Card
                    item {
                        Surface(
                            color = colors.surface,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Settings & Security", fontWeight = FontWeight.SemiBold, fontSize = 16.5.sp, color = colors.textPrimary)
                                Text("Configure app lock, biometrics & profile", fontSize = 12.5.sp, color = colors.textSecondary)
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onOpenProfile,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = null, tint = colors.brandPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Settings", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    OutlinedButton(
                                        onClick = onOpenSecurity,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = colors.brandPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("App Lock", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
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

            "Teams" -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Workplace Teams (${teams.size})",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Squads, shifts & departments",
                                fontSize = 12.5.sp,
                                color = colors.textSecondary
                            )
                        }
                        Button(
                            onClick = onCreateTeamClick,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
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
                                description = "Create teams (squads, departments or shifts) to group and organize your workplace members.",
                                actionLabel = "Create First Team",
                                onAction = onCreateTeamClick
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(teams, key = { it.id }) { team ->
                                val teamMembers = employees.filter {
                                    it.teamId == team.id || it.teamName.equals(team.name, ignoreCase = true)
                                }
                                TeamDirectoryCard(
                                    team = team,
                                    members = teamMembers,
                                    onAddMember = { onAddEmployeeClick(team.id) },
                                    onViewMembers = { onViewTeamMembers(team.id) },
                                    onInvite = { onOpenJoinQrForTeam(team.id) }
                                )
                            }
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
                                Text("Team Join QR", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                                Text("Scan this QR code with the Bizora mobile app to join this team.", fontSize = 13.5.sp, color = colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))

                                val activeTeam = teams.firstOrNull()
                                val displayTeamCode = activeTeam?.displayCode ?: ""
                                val inviteToken = workplace.formattedCode

                                QrCodeView(
                                    content = "bizora://join?teamCode=$displayTeamCode&token=$inviteToken",
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
                                        Column {
                                            Text(if (activeTeam != null) "Team Code (${activeTeam.name})" else "Team Code", fontSize = 11.5.sp, color = colors.textSecondary, fontWeight = FontWeight.SemiBold)
                                            Text("#$displayTeamCode", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, letterSpacing = 2.sp)
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("Team Code", displayTeamCode))
                                                    Toast.makeText(context, "Team code copied: #$displayTeamCode", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = colors.brandPrimary, modifier = Modifier.size(22.dp))
                                            }

                                            IconButton(
                                                onClick = {
                                                    val sendIntent = Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(Intent.EXTRA_TEXT, "Join our team at ${workplace.name} on Bizora: https://bizora.app/join?teamCode=$displayTeamCode&token=$inviteToken")
                                                        type = "text/plain"
                                                    }
                                                    context.startActivity(Intent.createChooser(sendIntent, "Share Team Invite"))
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
    teams: List<Team> = emptyList(),
    onClick: () -> Unit = {},
    onCopyInvite: () -> Unit
) {
    val colors = LocalBizoraColors.current
    val resolvedTeamName = employee.teamName.takeIf { !it.isNullOrBlank() }
        ?: teams.find { it.id == employee.teamId }?.name
        ?: "General"
    val resolvedTeamColor = teams.find {
        it.name.equals(resolvedTeamName, ignoreCase = true) || it.id == employee.teamId
    }?.let {
        runCatching { Color(android.graphics.Color.parseColor(it.color)) }.getOrNull()
    } ?: colors.brandPrimary

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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    Text(
                        text = employee.employeeCode,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                    Text(text = "•", fontSize = 12.sp, color = colors.border)
                    // Aesthetic Team Tag Chip
                    Surface(
                        color = resolvedTeamColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(resolvedTeamColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = resolvedTeamName,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = resolvedTeamColor
                            )
                        }
                    }
                }

                if (!employee.invitedEmail.isNullOrBlank()) {
                    Text(
                        text = employee.invitedEmail!!,
                        fontSize = 12.sp,
                        color = colors.textSecondary.copy(alpha = 0.8f),
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp)
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
