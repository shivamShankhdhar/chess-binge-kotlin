package bizora.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import bizora.app.data.model.WorkplaceRole
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
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
import bizora.app.data.model.Team
import bizora.app.data.model.User
import bizora.app.data.model.Workplace
import bizora.app.data.repository.BizoraRepository
import bizora.app.ui.components.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import bizora.app.ui.dialogs.AttendanceConfirmationModal
import bizora.app.ui.dialogs.NotificationsModal
import bizora.app.ui.dialogs.WorkplaceJoinQrModal
import bizora.app.ui.dialogs.WorkplaceSwitcherModal
import bizora.app.ui.dialogs.PlayStoreRatingModal
import bizora.app.ui.dialogs.AppUpdateModal
import bizora.app.ui.theme.LocalBizoraColors
import bizora.app.utils.LocationHelper
import bizora.app.utils.UserCoordinates
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

enum class EmployeeTab {
    TODAY,
    HISTORY,
    PROFILE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDashboard(
    repository: BizoraRepository,
    onSignOut: () -> Unit,
    onToggleTheme: () -> Unit = {},
    isDark: Boolean = false,
    onLockAppNow: () -> Unit = {}
) {
    val colors = LocalBizoraColors.current
    val context = LocalContext.current

    val activeWorkplaceNullable by repository.activeWorkplace.collectAsState()
    val activeWorkplace = activeWorkplaceNullable ?: Workplace(id = "", name = "Workplace", timezone = "Asia/Kolkata", role = WorkplaceRole.EMPLOYEE)
    val workplaces by repository.workplaces.collectAsState()
    val currentUser by repository.currentUser.collectAsState()
    val teams by repository.teams.collectAsState()
    val mpinStatus by repository.mpinStatus.collectAsState()
    val todayStatus by repository.employeeTodayStatus.collectAsState()
    val checkInTime by repository.employeeCheckInTime.collectAsState()
    val isPending by repository.employeeRequestPending.collectAsState()
    val notifications by repository.notifications.collectAsState()
    val isEmployeeStatusLoading by repository.isEmployeeStatusLoading.collectAsState()
    val isEmployeeHistoryLoading by repository.isEmployeeHistoryLoading.collectAsState()

    var activeTab by remember { mutableStateOf(EmployeeTab.TODAY) }
    var showProfileScreen by remember { mutableStateOf(false) }
    var showSecuritySettingsScreen by remember { mutableStateOf(false) }
    var isAppLockDismissedForSession by remember { mutableStateOf(false) }
    var isScanningQr by remember { mutableStateOf(false) }
    var isReviewingScan by remember { mutableStateOf(false) }
    var scannedToken by remember { mutableStateOf<String?>(null) }
    var showWorkplaceSwitcher by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var showJoinQrModal by remember { mutableStateOf(false) }
    var selectedDetailItem by remember { mutableStateOf<AttendanceDetailData?>(null) }
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

    // Geolocation Attendance state
    var pendingConfirmation by remember { mutableStateOf<PendingAttendanceConfirmation?>(null) }
    var isSubmittingAttendance by remember { mutableStateOf(false) }
    var targetRequestType by remember { mutableStateOf("CHECK_IN") }
    val coroutineScope = rememberCoroutineScope()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            coroutineScope.launch {
                val coords = LocationHelper.getCurrentLocation(context)
                val wpSettings = activeWorkplace.attendanceSettings
                val dist = if (coords != null && wpSettings?.latitude != null && wpSettings.longitude != null) {
                    LocationHelper.calculateDistanceMeters(
                        coords.latitude,
                        coords.longitude,
                        wpSettings.latitude,
                        wpSettings.longitude
                    )
                } else null

                val now = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
                val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"))

                pendingConfirmation = PendingAttendanceConfirmation(
                    requestType = targetRequestType,
                    date = todayStr,
                    time = now,
                    coordinates = coords,
                    distanceMeters = dist
                )
            }
        } else {
            pendingConfirmation = null
            Toast.makeText(context, "Location permission denied. Geolocation attendance cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    val proceedWithLocationCapture = { reqType: String ->
        targetRequestType = reqType
        if (LocationHelper.hasLocationPermission(context)) {
            coroutineScope.launch {
                val coords = LocationHelper.getCurrentLocation(context)
                val wpSettings = activeWorkplace.attendanceSettings
                val dist = if (coords != null && wpSettings?.latitude != null && wpSettings.longitude != null) {
                    LocationHelper.calculateDistanceMeters(
                        coords.latitude,
                        coords.longitude,
                        wpSettings.latitude,
                        wpSettings.longitude
                    )
                } else null

                val now = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
                val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"))

                pendingConfirmation = PendingAttendanceConfirmation(
                    requestType = reqType,
                    date = todayStr,
                    time = now,
                    coordinates = coords,
                    distanceMeters = dist
                )
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val triggerAttendanceMark = { reqType: String ->
        targetRequestType = reqType
        // Requirement 3: When geolocation feature is clicked, FIRST show reward ad.
        // Only after ad completion, open location request modal to capture GPS coordinates.
        // Fallback: If ad is skipped/cancelled, cancel action entirely and do not open geolocation feature.
        bizora.app.utils.AdMobRewardHelper.showRewardAd(
            context = context,
            onRewardCompleted = {
                proceedWithLocationCapture(reqType)
            },
            onCancelled = {
                pendingConfirmation = null
                Toast.makeText(context, "Attendance cancelled: Reward ad was not completed", Toast.LENGTH_SHORT).show()
            }
        )
    }

    val tabs = remember {
        listOf(
            BottomTabItem(
                key = EmployeeTab.TODAY,
                label = "Dashboard",
                iconRenderer = { _, size, active -> DashboardTabIcon(size = size, active = active) }
            ),
            BottomTabItem(
                key = EmployeeTab.HISTORY,
                label = "History",
                iconRenderer = { _, size, active -> HistoryTabIcon(size = size, active = active) }
            ),
            BottomTabItem(
                key = EmployeeTab.PROFILE,
                label = "Profile",
                iconRenderer = { _, size, active -> ProfileTabIcon(size = size, active = active) }
            )
        )
    }

    if (isScanningQr) {
        CameraQrScanner(
            onQrScanned = { qr ->
                isScanningQr = false
                scannedToken = qr
                isReviewingScan = true
            },
            onClose = { isScanningQr = false }
        )
        return
    }

    if (isReviewingScan) {
        val currentScanned = scannedToken ?: ""
        ReviewAttendanceRequestScreen(
            workplace = activeWorkplace,
            token = currentScanned,
            onBack = { isReviewingScan = false },
            onConfirm = {
                isReviewingScan = false
                if (currentScanned.isNotBlank()) {
                    repository.submitEmployeeScanRequest(currentScanned)
                    Toast.makeText(context, "Attendance request submitted!", Toast.LENGTH_LONG).show()
                }
            }
        )
        return
    }

    Scaffold(
        topBar = {
            if (activeTab == EmployeeTab.TODAY) {
                Surface(
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                                .clickable { showProfileScreen = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                name = currentUser?.name ?: "Employee",
                                size = 44.dp,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentUser?.name ?: "Employee",
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = activeWorkplace.name,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.brandPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    val activeTeam = teams.firstOrNull()
                                    // Team badge
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colors.brandPrimary.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = activeTeam?.name ?: "All Teams",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.brandPrimary
                                        )
                                    }

                                    // Workplace Join QR button
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colors.brandPrimary.copy(alpha = 0.12f))
                                            .border(1.dp, colors.border, RoundedCornerShape(6.dp))
                                            .clickable { showJoinQrModal = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = "Workplace QR",
                                            tint = colors.brandPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Notification Bell Button (44dp circular)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceMuted)
                                .border(1.dp, colors.border, CircleShape)
                                .clickable { showNotifications = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            if (notifications.any { !it.read }) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-8).dp, y = 8.dp)
                                )
                            }
                        }
                    }
                }
            } else if (activeTab == EmployeeTab.HISTORY) {
                Surface(
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Attendance History",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            letterSpacing = (-0.3).sp
                        )
                    }
                }
            } else {
                Surface(
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BizoraMark(size = 24.dp)
                            Text(
                                text = "Bizora",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.brandPrimary
                            )
                        }
                        Surface(
                            color = colors.brandPrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Profile",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.brandPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
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
            HeaderBackgroundArt(modifier = Modifier.align(Alignment.TopEnd))

            when (activeTab) {
                EmployeeTab.TODAY -> {
                    if (isEmployeeStatusLoading && todayStatus == AttendanceStatus.NOT_MARKED && checkInTime == null && !isPending) {
                        EmployeeTodaySkeleton()
                    } else {
                        EmployeeTodayContent(
                            status = todayStatus,
                            checkInTime = checkInTime,
                            isPending = isPending,
                            showAppLockPrompt = (mpinStatus?.hasMpin != true && mpinStatus?.appLockEnabled != true && currentUser?.hasMpin != true && currentUser?.appLockEnabled != true && !repository.hasMpin() && !repository.isAppLockEnabled() && !isAppLockDismissedForSession),
                            onSetupAppLock = { showSecuritySettingsScreen = true },
                            onDismissAppLock = { isAppLockDismissedForSession = true },
                            onScanClick = { isScanningQr = true },
                            onDirectCheckIn = {
                                triggerAttendanceMark("CHECK_IN")
                            },
                            onDirectCheckOut = {
                                triggerAttendanceMark("CHECK_OUT")
                            },
                            onSimulateApproval = { repository.simulateApproval() },
                            onNavigateToHistory = { activeTab = EmployeeTab.HISTORY },
                            onItemClick = { title, stat, time ->
                                selectedDetailItem = AttendanceDetailData(
                                    date = title,
                                    status = stat,
                                    time = time,
                                    workplaceName = activeWorkplace.name
                                )
                            }
                        )
                    }
                }

                EmployeeTab.HISTORY -> {
                    if (isEmployeeHistoryLoading) {
                        EmployeeHistorySkeleton()
                    } else {
                        EmployeeHistoryContent(
                            onItemClick = { date, stat ->
                                selectedDetailItem = AttendanceDetailData(
                                    date = date,
                                    status = stat,
                                    time = if (stat == AttendanceStatus.PRESENT) "09:15 AM - 06:15 PM" else if (stat == AttendanceStatus.HALF_DAY) "09:30 AM - 01:30 PM" else "Not Marked",
                                    workplaceName = activeWorkplace.name
                                )
                            }
                        )
                    }
                }

                EmployeeTab.PROFILE -> EmployeeProfileContent(
                    currentUser = currentUser,
                    workplace = activeWorkplace,
                    teams = teams,
                    onOpenProfileSettings = { showProfileScreen = true },
                    onOpenSecuritySettings = { showSecuritySettingsScreen = true },
                    onSignOut = onSignOut,
                    onShowRating = { showRatingModal = true },
                    onShowUpdate = { showUpdateModal = true }
                )
            }
        }
    }

    if (showNotifications) {
        NotificationsModal(
            notifications = notifications,
            onDismiss = { showNotifications = false }
        )
    }

    if (showJoinQrModal) {
        WorkplaceJoinQrModal(
            workplaceId = activeWorkplace.id,
            workplaceName = activeWorkplace.name,
            repository = repository,
            onDismiss = { showJoinQrModal = false }
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

    pendingConfirmation?.let { pending ->
        AttendanceConfirmationModal(
            workplaceName = activeWorkplace.name,
            workplaceCode = activeWorkplace.code ?: activeWorkplace.id.takeLast(6).uppercase(),
            requestType = pending.requestType,
            currentDate = pending.date,
            currentTime = pending.time,
            distanceMeters = pending.distanceMeters,
            isSubmitting = isSubmittingAttendance,
            onConfirm = {
                isSubmittingAttendance = true
                val lat = pending.coordinates?.latitude ?: 0.0
                val lng = pending.coordinates?.longitude ?: 0.0
                val acc = pending.coordinates?.accuracy ?: 0f

                repository.submitAttendanceWithLocation(
                    workplaceId = activeWorkplace.id,
                    requestType = pending.requestType,
                    latitude = lat,
                    longitude = lng,
                    accuracy = acc,
                    onSuccess = { msg ->
                        isSubmittingAttendance = false
                        pendingConfirmation = null
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    },
                    onError = { err ->
                        isSubmittingAttendance = false
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            },
            onDismiss = { pendingConfirmation = null }
        )
    }

    selectedDetailItem?.let { detail ->
        AttendanceDetailModal(
            detail = detail,
            onDismiss = { selectedDetailItem = null }
        )
    }
}

data class AttendanceDetailData(
    val date: String,
    val status: AttendanceStatus,
    val time: String,
    val workplaceName: String
)

data class PendingAttendanceConfirmation(
    val requestType: String,
    val date: String,
    val time: String,
    val coordinates: UserCoordinates?,
    val distanceMeters: Double?
)

// -------------------------------------------------------------
// TAB 1: EMPLOYEE TODAY
// -------------------------------------------------------------
@Composable
private fun EmployeeTodayContent(
    status: AttendanceStatus,
    checkInTime: String?,
    isPending: Boolean,
    showAppLockPrompt: Boolean = false,
    onSetupAppLock: () -> Unit = {},
    onDismissAppLock: () -> Unit = {},
    onScanClick: () -> Unit,
    onDirectCheckIn: () -> Unit,
    onDirectCheckOut: () -> Unit,
    onSimulateApproval: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onItemClick: (String, AttendanceStatus, String) -> Unit = { _, _, _ -> }
) {
    val colors = LocalBizoraColors.current
    val today = LocalDate.now()
    val todayDateStr = today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))

    val isApproved = status == AttendanceStatus.PRESENT
    val checkInDisplay = checkInTime ?: if (isPending) "Pending" else "--:--"
    val checkOutDisplay = if (isApproved) "06:15 PM" else "--:--"

    // 7-day horizontal weekday strip centered on today
    val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
    val weekDays = remember(today) {
        (0..6).map { startOfWeek.plusDays(it.toLong()) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showAppLockPrompt) {
            item {
                AppLockPromptCard(
                    onSetupClick = onSetupAppLock,
                    onDismissClick = onDismissAppLock
                )
            }
        }

        // ─── 1. Horizontal Calendar Weekdays Strip (matching RN empWeekdaysStrip) ───
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                weekDays.forEach { date ->
                    val isSelected = date == today
                    val dayNum = date.dayOfMonth.toString()
                    val dayName = date.format(DateTimeFormatter.ofPattern("EEE"))
                    Surface(
                        color = if (isSelected) colors.brandPrimary else colors.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = if (isSelected) null else BorderStroke(1.dp, colors.border),
                        shadowElevation = if (isSelected) 3.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (!isSelected) onNavigateToHistory()
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dayNum,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dayName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White.copy(alpha = 0.9f) else colors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // ─── 2. Section: Today Attendance Header ───
        item {
            Text(
                text = "Today Attendance",
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                letterSpacing = (-0.3).sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // ─── 3. 2x2 Rounded Cards Grid (balanced left & right) ───
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Row 1: Check In & Check Out
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card 1: Check In
                    Surface(
                        color = colors.surface,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, colors.border),
                        shadowElevation = 1.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.brandPrimary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = colors.brandPrimary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                Surface(
                                    color = if (isApproved) Color(0xFF16A34A).copy(alpha = 0.12f) else if (isPending) Color(0xFFD97706).copy(alpha = 0.12f) else colors.surfaceMuted,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (isApproved) "On Time" else if (isPending) "Pending" else "Not marked",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isApproved) Color(0xFF16A34A) else if (isPending) Color(0xFFD97706) else colors.textSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Check In",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = checkInDisplay,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                maxLines = 1
                            )
                        }
                    }

                    // Card 2: Check Out
                    Surface(
                        color = colors.surface,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, colors.border),
                        shadowElevation = 1.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF7C3AED).copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        tint = Color(0xFF7C3AED),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                Surface(
                                    color = if (isApproved) Color(0xFF7C3AED).copy(alpha = 0.12f) else colors.surfaceMuted,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (isApproved) "Go Home" else "Locked",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isApproved) Color(0xFF7C3AED) else colors.textSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Check Out",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = checkOutDisplay,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Row 2: Break Time & Total Days
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card 3: Break Time
                    Surface(
                        color = colors.surface,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, colors.border),
                        shadowElevation = 1.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFD97706).copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Coffee,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                Surface(
                                    color = Color(0xFFD97706).copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Recorded",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFD97706),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Break Time",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "00:45 hr",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                maxLines = 1
                            )
                        }
                    }

                    // Card 4: Total Days
                    Surface(
                        color = colors.surface,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, colors.border),
                        shadowElevation = 1.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF16A34A).copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                Surface(
                                    color = Color(0xFF16A34A).copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Working Days",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF16A34A),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Total Days",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "28",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // ─── 4. Section: Your Activity Header ───
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Activity",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "View All",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.brandPrimary,
                    modifier = Modifier.clickable { onNavigateToHistory() }
                )
            }
        }

        // ─── 5. Interactive Action / Swipe-to-Action Card ───
        item {
            Surface(
                color = if (isApproved) Color(0xFF7C3AED) else if (isPending) Color(0xFFD97706) else colors.brandPrimary,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (!isApproved && !isPending) {
                            onDirectCheckIn()
                        } else if (isApproved) {
                            onDirectCheckOut()
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isPending) "Check-In Request Pending" else if (isApproved) "Tap to Check Out" else "Tap to Check In",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isPending) "Awaiting employer review" else if (isApproved) "Recorded at $checkInDisplay" else "Ready to record today's attendance",
                            fontSize = 13.5.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isApproved) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // ─── 6. Secondary Quick Options Row (Scan QR & Attendance History) ───
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick Button 1: Scan QR
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onScanClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = colors.brandPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isApproved) "Scan Check-Out QR" else "Scan Check-In QR",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1
                        )
                    }
                }

                // Quick Button 2: View History
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .weight(0.85f)
                        .clickable { onNavigateToHistory() }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = colors.brandPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "View History",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // ─── 7. Status Card for Today's Attendance ───
        if (isApproved || isPending) {
            item {
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onItemClick("Today", status, "In: $checkInDisplay")
                        }
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(44.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isApproved) Color(0xFF16A34A) else Color(0xFFD97706))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isApproved) "Today's Attendance Confirmed" else "Attendance Request Pending",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "In: $checkInDisplay • Out: $checkOutDisplay",
                                fontSize = 12.5.sp,
                                color = colors.textSecondary
                            )
                        }
                        StatusChip(status = status)
                    }
                }
            }
        }

        // ─── 8. Recent Days Feed ───
        item {
            Text(
                text = "Recent Days",
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                letterSpacing = (-0.3).sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        val pastDays = listOf(
            Triple("Yesterday", AttendanceStatus.PRESENT, "In: 09:12 AM • Out: 06:15 PM"),
            Triple("2 days ago", AttendanceStatus.PRESENT, "In: 09:05 AM • Out: 06:02 PM"),
            Triple("3 days ago", AttendanceStatus.HALF_DAY, "In: 09:30 AM • Out: 01:15 PM"),
            Triple("4 days ago", AttendanceStatus.PRESENT, "In: 09:14 AM • Out: 06:10 PM")
        )

        items(pastDays, key = { it.first }) { (date, stat, timing) ->
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(date, stat, timing) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = date, fontWeight = FontWeight.SemiBold, fontSize = 16.5.sp, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = timing, fontSize = 13.5.sp, color = colors.textSecondary)
                    }
                    StatusChip(status = stat)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: EMPLOYEE HISTORY
// -------------------------------------------------------------
@Composable
private fun EmployeeHistoryContent(
    onItemClick: (String, AttendanceStatus) -> Unit = { _, _ -> }
) {
    val colors = LocalBizoraColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                color = colors.brandTint,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.brandPrimary.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("22", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.brandPrimary)
                        Text("Present", fontSize = 13.5.sp, color = colors.textSecondary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("1", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.danger)
                        Text("Absent", fontSize = 13.5.sp, color = colors.textSecondary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("96%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.success)
                        Text("Monthly Rate", fontSize = 13.5.sp, color = colors.textSecondary)
                    }
                }
            }
        }

        item {
            Text("October 2026 Records", fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, letterSpacing = (-0.3).sp)
        }

        val days = (1..15).map { day ->
            val stat = if (day == 8) AttendanceStatus.ABSENT else if (day == 12) AttendanceStatus.HALF_DAY else AttendanceStatus.PRESENT
            "Oct $day, 2026" to stat
        }

        if (days.isEmpty()) {
            item {
                EmptyState(
                    illustration = { CalendarClockIllustration(size = 110.dp) },
                    title = "No attendance records yet",
                    description = "When you check in and check out, your attendance history logs will appear here."
                )
            }
        } else {
            items(days, key = { it.first }) { (dateStr, stat) ->
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onItemClick(dateStr, stat) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(dateStr, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = colors.textPrimary)
                        StatusChip(status = stat)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: EMPLOYEE PROFILE
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmployeeProfileContent(
    currentUser: User?,
    workplace: Workplace,
    teams: List<Team> = emptyList(),
    onOpenProfileSettings: () -> Unit = {},
    onOpenSecuritySettings: () -> Unit = {},
    onSignOut: () -> Unit,
    onShowRating: () -> Unit = {},
    onShowUpdate: () -> Unit = {}
) {
    val colors = LocalBizoraColors.current
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var biometricEnabled by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    val displayName = currentUser?.name?.takeIf { it.isNotBlank() } ?: "Employee"
    val displayEmail = currentUser?.email?.takeIf { it.isNotBlank() } ?: "No email attached"
    val initials = remember(displayName) {
        val parts = displayName.trim().split(" ").filter { it.isNotBlank() }
        if (parts.size >= 2) {
            "${parts[0].first()}${parts[1].first()}".uppercase()
        } else {
            displayName.take(2).uppercase()
        }
    }
    val wpCode = (workplace.code?.takeIf { it.isNotBlank() } ?: workplace.formattedCode).uppercase()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ─── 1. Profile Identity Hero Card (matching RN profileHeroCard) ───
        item {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, colors.border),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar with Active Indicator
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(colors.brandPrimary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                color = colors.brandPrimary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF16A34A))
                                .border(2.dp, colors.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = displayEmail,
                        fontSize = 13.5.sp,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Badges Row: Role Badge + Workplace Code Badge
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Role Badge
                        Surface(
                            color = colors.brandPrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = colors.brandPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Employee",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.brandPrimary
                                )
                            }
                        }

                        // Team Badge
                        val activeTeam = teams.firstOrNull()
                        Surface(
                            color = colors.brandTint,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, colors.brandPrimary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = colors.brandPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = activeTeam?.name ?: "All Teams",
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

        // ─── 2. Section: Workplace Details ───
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Workplace",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.brandPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = colors.brandPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Workplace", fontSize = 12.sp, color = colors.textSecondary)
                                Text(workplace.name, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = colors.border)

                        val currentTeam = teams.firstOrNull()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Assigned Team", fontSize = 12.sp, color = colors.textSecondary)
                                Text(currentTeam?.name ?: "All Teams", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.brandPrimary)
                            }
                            if (currentTeam != null) {
                                val tCode = currentTeam.displayCode
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(tCode))
                                        Toast.makeText(context, "Team code copied: #$tCode", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy #$tCode", fontSize = 12.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ─── 3. Section: App Preferences & Security ───
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "App Preferences & Security",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )
                // App Lock Card
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSecuritySettings() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.brandPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = colors.brandPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("App Lock & Security", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colors.textPrimary)
                            Text("Manage MPIN, biometric unlock & passcode", fontSize = 12.5.sp, color = colors.textSecondary)
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Preferences & Theme Card
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenProfileSettings() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.brandPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = colors.brandPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Settings & Profile", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colors.textPrimary)
                            Text("Account information, security & profile", fontSize = 12.5.sp, color = colors.textSecondary)
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // ─── 4. Section: About App ───
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "About App",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )
                Surface(
                    color = colors.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFECEFDE)),
                                contentAlignment = Alignment.Center
                            ) {
                                BizoraMark(size = 30.dp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Bizora", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
                                Text("Version 1.0.0 (Production Release)", fontSize = 12.sp, color = colors.textSecondary)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Bizora is an effortless digital workplace attendance tracker designed for speed, reliability, and security.",
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // ─── Development Phase Previews (Rating & Update Modals) ───
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "App Feedback & Updates",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )
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

        // ─── 5. Sign Out Button (matching RN profileLogoutBtn) ───
        item {
            OutlinedButton(
                onClick = { showSignOutConfirm = true },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colors.danger
                ),
                border = BorderStroke(1.dp, colors.danger.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = colors.danger,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.danger)
            }
        }
    }

    if (showSignOutConfirm) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSignOutConfirm = false },
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
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text("Sign out", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Are you sure you want to sign out of this workplace?", fontSize = 14.sp, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showSignOutConfirm = false },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                    Button(
                        onClick = {
                            showSignOutConfirm = false
                            onSignOut()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.danger),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sign Out", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// REVIEW ATTENDANCE REQUEST SCREEN (1:1 with RN review-request)
// -------------------------------------------------------------
@Composable
fun ReviewAttendanceRequestScreen(
    workplace: Workplace,
    token: String,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = LocalBizoraColors.current
    var isSubmitting by remember { mutableStateOf(false) }
    val todayDateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))
    val currentTimeStr = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))

    Scaffold(
        topBar = {
            Surface(
                color = colors.surface,
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Review attendance request",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
            }
        },
        containerColor = colors.canvas
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // QR verified banner
            Surface(
                color = colors.successTint,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.success.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.success),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("QR verified", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
                        Text("Check-in QR is valid and authenticated.", fontSize = 14.sp, color = colors.textSecondary)
                    }
                }
            }

            // Verified items card
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Workplace verified
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.brandTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = colors.brandPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Workplace verified", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = colors.textPrimary)
                            Text(workplace.name, fontSize = 14.sp, color = colors.textSecondary)
                        }
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.success, modifier = Modifier.size(20.dp))
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = colors.border)

                    // Network verification
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.brandTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = colors.brandPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Network verified", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = colors.textPrimary)
                            Text("Connected to authorized workplace network", fontSize = 14.sp, color = colors.textSecondary)
                        }
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.success, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Details card
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Date", fontSize = 14.sp, color = colors.textSecondary)
                            Text(todayDateStr, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = colors.border)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Requested time", fontSize = 14.sp, color = colors.textSecondary)
                            Text(currentTimeStr, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = colors.border)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Business, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Workplace", fontSize = 14.sp, color = colors.textSecondary)
                            Text(workplace.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action button
            Button(
                onClick = {
                    isSubmitting = true
                    onConfirm()
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.brandPrimary,
                    contentColor = colors.textInverse
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = colors.textInverse, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Request attendance", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ATTENDANCE DETAIL MODAL (1:1 with RN approved-detail)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceDetailModal(
    detail: AttendanceDetailData,
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
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attendance Details",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = colors.surfaceMuted,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(detail.date, fontWeight = FontWeight.Bold, fontSize = 16.5.sp, color = colors.textPrimary)
                        StatusChip(status = detail.status)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Workplace: ${detail.workplaceName}", fontSize = 13.5.sp, color = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = colors.brandPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Timing", fontSize = 12.sp, color = colors.textSecondary)
                            Text(detail.time, fontWeight = FontWeight.Medium, fontSize = 14.5.sp, color = colors.textPrimary)
                        }
                    }

                    HorizontalDivider(color = colors.border)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = colors.success, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Verification", fontSize = 12.sp, color = colors.textSecondary)
                            Text("QR Code & Geofence Verified", fontWeight = FontWeight.Medium, fontSize = 14.5.sp, color = colors.textPrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Close", color = colors.textInverse, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
