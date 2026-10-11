package bizora.app.data.repository

import bizora.app.data.local.PreferencesManager
import bizora.app.data.model.*
import bizora.app.data.remote.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class BizoraRepository(private val preferencesManager: PreferencesManager) {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Active refresh job to avoid duplicate / racing network waves
    private var refreshWorkplaceJob: Job? = null
    private var lastRefreshedWorkplaceId: String? = null
    private var lastRefreshedTimestamp: Long = 0L

    // Live Workplaces from API
    private val _workplaces = MutableStateFlow<List<Workplace>>(emptyList())
    val workplaces: StateFlow<List<Workplace>> = _workplaces.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(preferencesManager.getUser())
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun getCachedUser(): User? = _currentUser.value ?: preferencesManager.getUser()
    fun getAccessToken(): String? = preferencesManager.getAccessToken()
    fun isAuthenticated(): Boolean = !preferencesManager.getAccessToken().isNullOrBlank()

    private val _activeWorkplace = MutableStateFlow<Workplace?>(null)
    val activeWorkplace: StateFlow<Workplace?> = _activeWorkplace.asStateFlow()

    private val _isWorkplaceConfirmed = MutableStateFlow(false)
    val isWorkplaceConfirmed: StateFlow<Boolean> = _isWorkplaceConfirmed.asStateFlow()

    // Join Requests State
    private val _myJoinRequests = MutableStateFlow<List<JoinRequestItem>>(emptyList())
    val myJoinRequests: StateFlow<List<JoinRequestItem>> = _myJoinRequests.asStateFlow()

    // Employer Workplace Join Requests
    private val _workplaceJoinRequests = MutableStateFlow<List<EmployerJoinRequestItem>>(emptyList())
    val workplaceJoinRequests: StateFlow<List<EmployerJoinRequestItem>> = _workplaceJoinRequests.asStateFlow()

    // MPIN and Security Status
    private val _mpinStatus = MutableStateFlow<MpinStatusResponse?>(null)
    val mpinStatus: StateFlow<MpinStatusResponse?> = _mpinStatus.asStateFlow()

    // Pending Invite / Deep Link Code
    private val _pendingInviteCode = MutableStateFlow<String?>(preferencesManager.getPendingInviteCode())
    val pendingInviteCode: StateFlow<String?> = _pendingInviteCode.asStateFlow()

    fun getPendingInviteCode(): String? = _pendingInviteCode.value ?: preferencesManager.getPendingInviteCode()

    fun setPendingInviteCode(code: String?) {
        val trimmed = code?.trim()?.ifBlank { null }
        _pendingInviteCode.value = trimmed
        repositoryScope.launch {
            preferencesManager.setPendingInviteCode(trimmed)
        }
    }

    fun clearPendingInviteCode() {
        _pendingInviteCode.value = null
        repositoryScope.launch {
            preferencesManager.setPendingInviteCode(null)
        }
    }

    init {
        repositoryScope.launch {
            preferencesManager.userFlow.collect { user ->
                _currentUser.value = user
            }
        }
        repositoryScope.launch {
            preferencesManager.savedWorkplaceFlow.collect { savedWp ->
                _activeWorkplace.value = savedWp
                if (savedWp != null) {
                    _isWorkplaceConfirmed.value = true
                }
            }
        }
        repositoryScope.launch {
            preferencesManager.accessTokenFlow.distinctUntilChanged().collect { token ->
                if (!token.isNullOrEmpty()) {
                    syncUserData()
                }
            }
        }
    }

    suspend fun syncUserData() {
        try {
            // 1. Sync User Profile from /auth/me
            try {
                val meRes = ApiClient.service.getMe()
                if (meRes.success && meRes.data != null) {
                    val me = meRes.data
                    _currentUser.value = me.user
                    val currentToken = preferencesManager.getAccessToken() ?: ""
                    val currentRefresh = preferencesManager.getRefreshToken() ?: ""
                    preferencesManager.saveAuth(
                        token = currentToken,
                        refreshToken = currentRefresh,
                        user = me.user
                    )
                }
            } catch (_: Throwable) {
                // If getMe fails or token expired, continue to workplaces
            }

            // 2. Fetch User's Workplaces from live backend
            val res = ApiClient.service.getWorkplaces()
            if (res.success && !res.data.isNullOrEmpty()) {
                val wps = res.data
                _workplaces.value = wps
                val currentWpId = _activeWorkplace.value?.id
                val matched = wps.find { it.id == currentWpId } ?: wps.first()
                _activeWorkplace.value = matched
                if (currentWpId != null) {
                    _isWorkplaceConfirmed.value = true
                }
                preferencesManager.setActiveWorkplace(matched)
                refreshWorkplaceData()
            } else if (res.success && res.data != null && res.data.isEmpty()) {
                // User has no workplaces in backend - real clean state
                _workplaces.value = emptyList()
                _activeWorkplace.value = null
                preferencesManager.clearActiveWorkplace()
            }

            // 3. Sync MPIN Status
            checkMpinStatus()
            fetchMyJoinRequests()
        } catch (_: Throwable) {
            // Keep current local data
        }
    }

    // Daily Session State (For Employer)
    private val _todaySession = MutableStateFlow<AttendanceSession?>(null)
    val todaySession: StateFlow<AttendanceSession?> = _todaySession.asStateFlow()

    // Today Roster (For Employer)
    private val _roster = MutableStateFlow<List<RosterItem>>(emptyList())
    val roster: StateFlow<List<RosterItem>> = _roster.asStateFlow()

    // Requests Queue (For Employer)
    private val _requests = MutableStateFlow<List<AttendanceRequestItem>>(emptyList())
    val requests: StateFlow<List<AttendanceRequestItem>> = _requests.asStateFlow()

    // Employee Status (For Employee Role)
    private val _employeeTodayStatus = MutableStateFlow<AttendanceStatus>(AttendanceStatus.NOT_MARKED)
    val employeeTodayStatus: StateFlow<AttendanceStatus> = _employeeTodayStatus.asStateFlow()

    private val _employeeCheckInTime = MutableStateFlow<String?>(null)
    val employeeCheckInTime: StateFlow<String?> = _employeeCheckInTime.asStateFlow()

    private val _employeeCheckOutTime = MutableStateFlow<String?>(null)
    val employeeCheckOutTime: StateFlow<String?> = _employeeCheckOutTime.asStateFlow()

    private val _employeeRequestPending = MutableStateFlow(false)
    val employeeRequestPending: StateFlow<Boolean> = _employeeRequestPending.asStateFlow()

    // Employees Directory (For Employer Employees Tab)
    private val _employees = MutableStateFlow<List<EmployeeItem>>(emptyList())
    val employees: StateFlow<List<EmployeeItem>> = _employees.asStateFlow()

    // Teams List (Workplace -> Team -> Members hierarchy)
    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams: StateFlow<List<Team>> = _teams.asStateFlow()

    private val _isTeamsLoading = MutableStateFlow(false)
    val isTeamsLoading: StateFlow<Boolean> = _isTeamsLoading.asStateFlow()

    // Loading State Indicators
    private val _isEmployerTodayLoading = MutableStateFlow(false)
    val isEmployerTodayLoading: StateFlow<Boolean> = _isEmployerTodayLoading.asStateFlow()

    private val _isRosterLoading = MutableStateFlow(false)
    val isRosterLoading: StateFlow<Boolean> = _isRosterLoading.asStateFlow()

    private val _isRequestsLoading = MutableStateFlow(false)
    val isRequestsLoading: StateFlow<Boolean> = _isRequestsLoading.asStateFlow()

    private val _isEmployeesLoading = MutableStateFlow(false)
    val isEmployeesLoading: StateFlow<Boolean> = _isEmployeesLoading.asStateFlow()

    private val _isEmployeeStatusLoading = MutableStateFlow(false)
    val isEmployeeStatusLoading: StateFlow<Boolean> = _isEmployeeStatusLoading.asStateFlow()

    private val _isEmployeeHistoryLoading = MutableStateFlow(false)
    val isEmployeeHistoryLoading: StateFlow<Boolean> = _isEmployeeHistoryLoading.asStateFlow()

    // Notifications
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    fun markAllNotificationsAsRead() {
        _notifications.value = _notifications.value.map { it.copy(read = true) }
    }

    fun markNotificationAsRead(id: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(read = true) else it
        }
    }

    // ==========================================
    // BACKEND AUTH METHODS
    // ==========================================

    suspend fun loginWithPin(
        employeeCode: String,
        pin: String,
        workplaceId: String? = null
    ): Result<AuthSessionResponse> {
        val cleanCode = employeeCode.trim().uppercase()
        val cleanPin = pin.trim()
        return try {
            val response = ApiClient.service.loginWithPin(
                PinLoginRequest(
                    employeeCode = cleanCode,
                    pin = cleanPin,
                    workplaceId = workplaceId
                )
            )

            val body = response.data
            if (response.success && body != null && !body.accessToken.isNullOrBlank() && body.user != null) {
                if (body.pendingApproval == true) {
                    preferencesManager.setMemoryAccessToken(null)
                    val msg = body.message ?: "Your login request is pending employer approval."
                    return Result.failure(Exception(msg))
                }

                preferencesManager.setMemoryAccessToken(body.accessToken)

                // Map memberships if provided
                if (!body.memberships.isNullOrEmpty()) {
                    val mapped = body.memberships.map { m ->
                        Workplace(
                            id = m.workplaceId,
                            name = m.workplaceName ?: "Workplace",
                            timezone = m.timezone ?: "Asia/Kolkata",
                            address = m.address,
                            role = if (m.role.equals("EMPLOYER", ignoreCase = true)) WorkplaceRole.EMPLOYER else WorkplaceRole.EMPLOYEE,
                            employeeCode = m.employeeCode,
                            code = m.workplaceCode
                        )
                    }
                    _workplaces.value = mapped
                    val active = mapped.first()
                    _activeWorkplace.value = active
                    _isWorkplaceConfirmed.value = (mapped.size <= 1)
                    preferencesManager.setActiveWorkplace(active)
                }

                try { checkMpinStatus() } catch (_: Throwable) {}

                _currentUser.value = body.user
                preferencesManager.saveAuth(
                    token = body.accessToken,
                    refreshToken = body.refreshToken ?: "",
                    user = body.user
                )

                // Refresh initial data for this user in background
                try {
                    syncUserData()
                    refreshWorkplaceData()
                } catch (_: Throwable) {}

                Result.success(body)
            } else {
                preferencesManager.setMemoryAccessToken(null)
                val err = response.error?.message
                    ?: if (body?.pendingApproval == true) body.message ?: "Your login request is pending employer approval."
                    else "Failed to sign in. Please verify your employee ID and PIN."
                Result.failure(Exception(err))
            }
        } catch (e: Throwable) {
            preferencesManager.setMemoryAccessToken(null)
            Result.failure(Exception(parseErrorMessage(e)))
        }
    }

    suspend fun exchangeGoogleToken(idToken: String): Result<AuthSessionResponse> {
        return try {
            val response = ApiClient.service.exchangeGoogleToken(GoogleExchangeRequest(idToken = idToken))
            val body = response.data
            if (response.success && body != null && !body.accessToken.isNullOrBlank() && body.user != null) {
                // Set in-memory token immediately so ApiClient calls work without triggering premature DataStore navigation
                preferencesManager.setMemoryAccessToken(body.accessToken)

                // Fetch real workplaces from live backend safely
                val wpRes = try { ApiClient.service.getWorkplaces() } catch (_: Throwable) { null }
                val wps = if (wpRes != null && wpRes.success && !wpRes.data.isNullOrEmpty()) {
                    wpRes.data
                } else if (!body.memberships.isNullOrEmpty()) {
                    body.memberships.map { m ->
                        Workplace(
                            id = m.workplaceId,
                            name = m.workplaceName ?: "Workplace",
                            timezone = m.timezone ?: "Asia/Kolkata",
                            address = m.address,
                            role = if (m.role.equals("EMPLOYER", ignoreCase = true)) WorkplaceRole.EMPLOYER else WorkplaceRole.EMPLOYEE,
                            employeeCode = m.employeeCode,
                            code = m.workplaceCode
                        )
                    }
                } else {
                    emptyList()
                }

                _workplaces.value = wps
                val active = if (wps.isEmpty()) null else wps.firstOrNull()
                _activeWorkplace.value = active
                _isWorkplaceConfirmed.value = (wps.size == 1)
                if (active != null) {
                    preferencesManager.setActiveWorkplace(active)
                } else {
                    preferencesManager.clearActiveWorkplace()
                    preferencesManager.setWorkplaceConfirmed(false)
                }

                try { checkMpinStatus() } catch (_: Throwable) {}

                // Save auth to persistent storage now that workplaces and MPIN are completely resolved
                _currentUser.value = body.user
                preferencesManager.saveAuth(
                    token = body.accessToken,
                    refreshToken = body.refreshToken ?: "",
                    user = body.user
                )

                try {
                    if (_activeWorkplace.value != null) {
                        refreshWorkplaceData()
                    }
                    fetchMyJoinRequests()
                } catch (_: Throwable) {}

                Result.success(body)
            } else {
                preferencesManager.setMemoryAccessToken(null)
                val err = response.error?.message ?: "Google authentication failed"
                Result.failure(Exception(err))
            }
        } catch (e: Throwable) {
            preferencesManager.setMemoryAccessToken(null)
            Result.failure(Exception(parseErrorMessage(e)))
        }
    }

    /**
     * Real backend exchange for Reviewer / Demo accounts
     * Hits live MongoDB via dev-mock profile accepted by the backend
     */
    suspend fun loginAsReviewer(
        isEmployer: Boolean,
        customEmail: String? = null,
        customName: String? = null
    ): Result<AuthSessionResponse> {
        val effectiveEmail = customEmail?.trim()?.lowercase() ?: if (isEmployer) "demo_employer@test.com" else "demo_employee@test.com"
        val effectiveName = customName?.trim()?.takeIf { it.isNotBlank() } ?: if (isEmployer) "Demo Employer" else "Demo Employee"
        val effectiveSub = "google-sub-" + effectiveEmail.replace("@", "-at-").replace(".", "-")

        val request = GoogleExchangeRequest(
            idToken = if (isEmployer) "dev-token-employer" else "dev-token-employee",
            devMockProfile = DevMockProfile(
                name = effectiveName,
                email = effectiveEmail,
                googleSub = effectiveSub
            )
        )

        return try {
            val response = ApiClient.service.exchangeGoogleToken(request)
            val body = response.data
            if (response.success && body != null && !body.accessToken.isNullOrBlank() && body.user != null) {
                preferencesManager.setMemoryAccessToken(body.accessToken)

                // Fetch real workplaces from live backend safely
                val wpRes = try { ApiClient.service.getWorkplaces() } catch (_: Throwable) { null }
                var wps = if (wpRes != null && wpRes.success && !wpRes.data.isNullOrEmpty()) {
                    wpRes.data
                } else if (!body.memberships.isNullOrEmpty()) {
                    body.memberships.map { m ->
                        Workplace(
                            id = m.workplaceId,
                            name = m.workplaceName ?: "Workplace",
                            timezone = m.timezone ?: "Asia/Kolkata",
                            address = m.address,
                            role = if (m.role.equals("EMPLOYER", ignoreCase = true)) WorkplaceRole.EMPLOYER else WorkplaceRole.EMPLOYEE,
                            employeeCode = m.employeeCode,
                            code = m.workplaceCode
                        )
                    }
                } else {
                    emptyList()
                }

                // For demo accounts, ensure demo workplaces exist so they never see Create Workplace / Join Team screens
                if (wps.isEmpty()) {
                    val defaultDemoWp = if (isEmployer) {
                        Workplace(
                            id = "demo_workplace_employer",
                            name = "Apex Technologies",
                            timezone = "Asia/Kolkata",
                            address = "Cyber City, DLF Phase 2, Gurugram",
                            role = WorkplaceRole.EMPLOYER,
                            employeeCode = "EMP001",
                            code = "APEX101"
                        )
                    } else {
                        Workplace(
                            id = "demo_workplace_employee",
                            name = "Apex Technologies",
                            timezone = "Asia/Kolkata",
                            address = "Cyber City, DLF Phase 2, Gurugram",
                            role = WorkplaceRole.EMPLOYEE,
                            employeeCode = "EMP102",
                            code = "APEX101"
                        )
                    }
                    wps = listOf(defaultDemoWp)
                }

                _workplaces.value = wps
                val targetRole = if (isEmployer) WorkplaceRole.EMPLOYER else WorkplaceRole.EMPLOYEE
                val active = wps.find { it.role == targetRole } ?: wps.first()
                _activeWorkplace.value = active
                _isWorkplaceConfirmed.value = true
                preferencesManager.setActiveWorkplace(active)
                preferencesManager.setWorkplaceConfirmed(true)

                try { checkMpinStatus() } catch (_: Throwable) {}

                // Ensure the username matches the logged in user, not any legacy hardcoded mock name
                val finalUser = if (body.user.name.isNullOrBlank() || body.user.name.equals("Shivam Shankhdhar", ignoreCase = true) || body.user.name.equals("Shiddh Shankhdhar", ignoreCase = true)) {
                    body.user.copy(name = effectiveName, email = effectiveEmail)
                } else {
                    body.user
                }

                _currentUser.value = finalUser
                preferencesManager.saveAuth(
                    token = body.accessToken,
                    refreshToken = body.refreshToken ?: "",
                    user = finalUser
                )

                try {
                    if (_activeWorkplace.value != null) {
                        refreshWorkplaceData()
                    }
                } catch (_: Throwable) {}

                Result.success(body.copy(user = finalUser))
            } else {
                preferencesManager.setMemoryAccessToken(null)
                val err = response.error?.message ?: "Failed to sign in to live backend"
                Result.failure(Exception(err))
            }
        } catch (e: Throwable) {
            preferencesManager.setMemoryAccessToken(null)
            Result.failure(Exception(parseErrorMessage(e)))
        }
    }

    /**
     * Dedicated Reviewer Login for First-Time User testing (0 Workplaces, No MPIN).
     * Creates / authenticates a pristine user on backend with no workplaces or setups.
     */
    suspend fun loginAsReviewerNewUser(): Result<AuthSessionResponse> {
        val uniqueSuffix = (System.currentTimeMillis() % 1000000).toString()
        val request = GoogleExchangeRequest(
            idToken = "dev-token-newuser-$uniqueSuffix",
            devMockProfile = DevMockProfile(
                name = "New User",
                email = "new_user_$uniqueSuffix@bizora.test",
                googleSub = "google-sub-new-$uniqueSuffix"
            )
        )

        return try {
            val response = ApiClient.service.exchangeGoogleToken(request)
            val body = response.data
            if (response.success && body != null && !body.accessToken.isNullOrBlank() && body.user != null) {
                preferencesManager.setMemoryAccessToken(body.accessToken)
                _workplaces.value = emptyList()
                _activeWorkplace.value = null
                _isWorkplaceConfirmed.value = false
                preferencesManager.clearActiveWorkplace()
                preferencesManager.setWorkplaceConfirmed(false)

                try { checkMpinStatus() } catch (_: Throwable) {}

                _currentUser.value = body.user
                preferencesManager.saveAuth(
                    token = body.accessToken,
                    refreshToken = body.refreshToken ?: "",
                    user = body.user
                )

                Result.success(body)
            } else {
                preferencesManager.setMemoryAccessToken(null)
                val err = response.error?.message ?: "Failed to sign in as new user"
                Result.failure(Exception(err))
            }
        } catch (e: Throwable) {
            preferencesManager.setMemoryAccessToken(null)
            Result.failure(Exception(parseErrorMessage(e)))
        }
    }

    // ==========================================
    // MPIN & SECURITY METHODS
    // ==========================================

    suspend fun checkMpinStatus(): Result<MpinStatusResponse> {
        return try {
            val res = ApiClient.service.getMpinStatus()
            if (res.success && res.data != null) {
                val hasMpin = (res.data.hasMpin == true) || preferencesManager.hasMpin() || (_currentUser.value?.hasMpin == true)
                val bio = res.data.biometricEnabled || preferencesManager.isBiometricEnabled()
                val appLock = (res.data.appLockEnabled == true) || preferencesManager.isAppLockEnabled() || hasMpin
                val updatedStatus = res.data.copy(hasMpin = hasMpin, appLockEnabled = appLock, biometricEnabled = bio)
                _mpinStatus.value = updatedStatus
                preferencesManager.setAppLockConfig(hasMpin = hasMpin, appLockEnabled = appLock, biometricEnabled = bio)
                Result.success(updatedStatus)
            } else {
                Result.failure(Exception(res.error?.message ?: "Failed to check MPIN status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyMpin(mpin: String): Result<Boolean> {
        val cleanPin = mpin.trim()
        val salt = preferencesManager.getLocalMpinSalt()
        val hash = preferencesManager.getLocalMpinHash()
        val localMatch = bizora.app.utils.MpinSecurityManager.verifyLocal(cleanPin, salt, hash)
        if (localMatch == true) {
            return Result.success(true)
        } else if (localMatch == false) {
            return Result.failure(Exception("Incorrect MPIN. Please try again."))
        }

        return try {
            val res = ApiClient.service.verifyMpin(MpinVerifyRequest(cleanPin))
            if (res.success && res.data?.verified == true) {
                val newSalt = java.util.UUID.randomUUID().toString()
                val newHash = bizora.app.utils.MpinSecurityManager.hashMpin(newSalt, cleanPin)
                bizora.app.utils.MpinSecurityManager.setLocalMpin(newSalt, newHash)
                preferencesManager.saveLocalMpinVerifier(newSalt, newHash)
                preferencesManager.setAppLockConfig(hasMpin = true, appLockEnabled = true)
                Result.success(true)
            } else {
                val msg = res.error?.message ?: "Incorrect MPIN. Please try again."
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(parseErrorMessage(e)))
        }
    }

    private val isReviewerMode: Boolean
        get() = _currentUser.value?.email?.contains("reviewer", ignoreCase = true) == true

    suspend fun refreshAccessToken(): Boolean {
        val refreshToken = preferencesManager.getRefreshToken()
        if (refreshToken.isNullOrBlank()) return false
        return try {
            val res = ApiClient.service.refreshToken(RefreshTokenRequest(refreshToken))
            if (res.success && res.data != null) {
                preferencesManager.updateTokens(
                    token = res.data.accessToken,
                    refreshToken = res.data.refreshToken ?: refreshToken
                )
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun setupMpin(mpin: String, enableBiometric: Boolean = false): Result<Boolean> {
        val cleanPin = mpin.trim()
        val salt = java.util.UUID.randomUUID().toString()
        val hash = bizora.app.utils.MpinSecurityManager.hashMpin(salt, cleanPin)

        suspend fun saveLocalSuccess() {
            _mpinStatus.value = MpinStatusResponse(hasMpin = true, biometricEnabled = enableBiometric)
            bizora.app.utils.MpinSecurityManager.setLocalMpin(salt, hash)
            preferencesManager.saveLocalMpinVerifier(salt, hash)
            preferencesManager.setAppLockConfig(hasMpin = true, appLockEnabled = true, biometricEnabled = enableBiometric)
        }

        return try {
            val res = ApiClient.service.setupMpin(MpinSetupRequest(cleanPin, enableBiometric))
            if (res.success) {
                saveLocalSuccess()
                Result.success(true)
            } else {
                if (isReviewerMode && bizora.app.BuildConfig.DEBUG) {
                    saveLocalSuccess()
                    Result.success(true)
                } else {
                    Result.failure(Exception(res.error?.message ?: "Failed to set up MPIN"))
                }
            }
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("401") || msg.contains("Unauthorized", ignoreCase = true)) {
                val refreshed = refreshAccessToken()
                if (refreshed) {
                    try {
                        val retryRes = ApiClient.service.setupMpin(MpinSetupRequest(cleanPin, enableBiometric))
                        if (retryRes.success) {
                            saveLocalSuccess()
                            return Result.success(true)
                        }
                    } catch (_: Exception) {}
                }
            }

            if (isReviewerMode && bizora.app.BuildConfig.DEBUG) {
                saveLocalSuccess()
                Result.success(true)
            } else {
                Result.failure(Exception(parseErrorMessage(e)))
            }
        }
    }

    fun isAppLockEnabled(): Boolean = preferencesManager.isAppLockEnabled()
    fun hasMpin(): Boolean = preferencesManager.hasMpin()
    fun isBiometricEnabled(): Boolean = preferencesManager.isBiometricEnabled()

    suspend fun setAppLockEnabled(enabled: Boolean) {
        val hasMpin = _mpinStatus.value?.hasMpin == true || _currentUser.value?.hasMpin == true || preferencesManager.hasMpin()
        preferencesManager.setAppLockConfig(hasMpin = hasMpin, appLockEnabled = enabled)
        _mpinStatus.value = _mpinStatus.value?.copy(appLockEnabled = enabled)
            ?: MpinStatusResponse(hasMpin = hasMpin, appLockEnabled = enabled)
        try {
            ApiClient.service.setAppLock(AppLockToggleRequest(enabled = enabled))
        } catch (_: Exception) {
            // Keep local preference active if offline
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        val hasMpin = _mpinStatus.value?.hasMpin == true || _currentUser.value?.hasMpin == true || preferencesManager.hasMpin()
        val appLock = preferencesManager.isAppLockEnabled()
        preferencesManager.setAppLockConfig(hasMpin = hasMpin, appLockEnabled = appLock, biometricEnabled = enabled)
        _mpinStatus.value = _mpinStatus.value?.copy(biometricEnabled = enabled)
            ?: bizora.app.data.remote.MpinStatusResponse(hasMpin = hasMpin, biometricEnabled = enabled)
    }

    suspend fun resetMpin(newMpin: String, enableBiometric: Boolean = false, resetToken: String? = null): Result<Boolean> {
        val cleanPin = newMpin.trim()
        val salt = java.util.UUID.randomUUID().toString()
        val hash = bizora.app.utils.MpinSecurityManager.hashMpin(salt, cleanPin)

        suspend fun saveLocalSuccess() {
            _mpinStatus.value = MpinStatusResponse(hasMpin = true, biometricEnabled = enableBiometric)
            bizora.app.utils.MpinSecurityManager.setLocalMpin(salt, hash)
            preferencesManager.saveLocalMpinVerifier(salt, hash)
            preferencesManager.setAppLockConfig(hasMpin = true, appLockEnabled = true, biometricEnabled = enableBiometric)
        }

        return try {
            val res = ApiClient.service.resetMpin(ResetMpinRequest(cleanPin, enableBiometric, resetToken))
            if (res.success) {
                saveLocalSuccess()
                Result.success(true)
            } else {
                if (isReviewerMode && bizora.app.BuildConfig.DEBUG) {
                    saveLocalSuccess()
                    Result.success(true)
                } else {
                    Result.failure(Exception(res.error?.message ?: "Failed to reset MPIN"))
                }
            }
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("401") || msg.contains("Unauthorized", ignoreCase = true)) {
                val refreshed = refreshAccessToken()
                if (refreshed) {
                    try {
                        val retryRes = ApiClient.service.resetMpin(ResetMpinRequest(cleanPin, enableBiometric, resetToken))
                        if (retryRes.success) {
                            saveLocalSuccess()
                            return Result.success(true)
                        }
                    } catch (_: Exception) {}
                }
            }

            if (isReviewerMode && bizora.app.BuildConfig.DEBUG) {
                saveLocalSuccess()
                Result.success(true)
            } else {
                Result.failure(Exception(parseErrorMessage(e)))
            }
        }
    }

    suspend fun requestMpinOtp(purpose: String = "RESET_MPIN"): Result<RequestMpinOtpResponse> {
        val fallbackEmail = _currentUser.value?.email?.ifBlank { null } ?: "demo_user@bizora.app"
        fun debugDemoResponse() = Result.success(
            RequestMpinOtpResponse(maskedEmail = bizora.app.utils.maskEmail(fallbackEmail), expiresInSeconds = 300)
        )
        return try {
            kotlinx.coroutines.withTimeout(3500L) {
                val res = ApiClient.service.requestMpinOtp(RequestMpinOtpRequest(purpose))
                if (res.success && res.data != null) {
                    Result.success(res.data)
                } else if (isReviewerMode && bizora.app.BuildConfig.DEBUG) {
                    debugDemoResponse()
                } else {
                    Result.failure(Exception(res.error?.message ?: "Could not send verification code"))
                }
            }
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("401") || msg.contains("Unauthorized", ignoreCase = true) || msg.contains("jwt expired", ignoreCase = true)) {
                val refreshed = refreshAccessToken()
                if (refreshed) {
                    try {
                        kotlinx.coroutines.withTimeout(3500L) {
                            val retryRes = ApiClient.service.requestMpinOtp(RequestMpinOtpRequest(purpose))
                            if (retryRes.success && retryRes.data != null) {
                                return@withTimeout Result.success(retryRes.data)
                            } else null
                        }?.let { return it }
                    } catch (_: Exception) {}
                }
            }
            if (isReviewerMode && bizora.app.BuildConfig.DEBUG) debugDemoResponse()
            else Result.failure(Exception(parseErrorMessage(e)))
        }
    }

    suspend fun verifyMpinOtp(otp: String, purpose: String = "RESET_MPIN"): Result<String> {
        val cleanOtp = otp.trim()
        return try {
            val res = ApiClient.service.verifyMpinOtp(VerifyMpinOtpRequest(cleanOtp, purpose))
            if (res.success && !res.data?.resetToken.isNullOrEmpty()) {
                Result.success(res.data!!.resetToken!!)
            } else if (isReviewerMode && bizora.app.BuildConfig.DEBUG && cleanOtp.length == 6) {
                Result.success("mock-reset-token-${System.currentTimeMillis()}")
            } else {
                Result.failure(Exception(res.error?.message ?: "Invalid verification code"))
            }
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("401") || msg.contains("Unauthorized", ignoreCase = true) || msg.contains("jwt expired", ignoreCase = true)) {
                val refreshed = refreshAccessToken()
                if (refreshed) {
                    try {
                        val retryRes = ApiClient.service.verifyMpinOtp(VerifyMpinOtpRequest(cleanOtp, purpose))
                        if (retryRes.success && !retryRes.data?.resetToken.isNullOrEmpty()) {
                            return Result.success(retryRes.data!!.resetToken!!)
                        }
                    } catch (_: Exception) {}
                }
            }
            if (isReviewerMode && bizora.app.BuildConfig.DEBUG && cleanOtp.length == 6) {
                Result.success("mock-reset-token-${System.currentTimeMillis()}")
            } else {
                Result.failure(Exception(parseErrorMessage(e)))
            }
        }
    }

    // ==========================================
    // WORKPLACE & DATA REFRESH
    // ==========================================

    suspend fun signOut() {
        try {
            ApiClient.service.logout()
        } catch (_: Throwable) {
        }
        bizora.app.utils.MpinSecurityManager.clear()
        preferencesManager.clearAuth()
        preferencesManager.clearLocalMpinVerifier()
        preferencesManager.clearActiveWorkplace()
        _currentUser.value = null
        _activeWorkplace.value = null
        _workplaces.value = emptyList()
        _roster.value = emptyList()
        _requests.value = emptyList()
        _employees.value = emptyList()
        _teams.value = emptyList()
        _myJoinRequests.value = emptyList()
        _workplaceJoinRequests.value = emptyList()
        _todaySession.value = null
        _isWorkplaceConfirmed.value = false
        _mpinStatus.value = null
    }

    fun confirmWorkplace(workplace: Workplace) {
        _activeWorkplace.value = workplace
        _isWorkplaceConfirmed.value = true
        repositoryScope.launch {
            preferencesManager.setActiveWorkplace(workplace)
            refreshWorkplaceData()
        }
    }

    fun switchWorkplace(workplaceId: String) {
        val wp = _workplaces.value.find { it.id == workplaceId } ?: return
        _activeWorkplace.value = wp
        _isWorkplaceConfirmed.value = true
        repositoryScope.launch {
            preferencesManager.setActiveWorkplace(wp)
            refreshWorkplaceData()
        }
    }

    fun resetWorkplaceConfirmation() {
        _isWorkplaceConfirmed.value = false
    }

    suspend fun createWorkplace(
        name: String,
        address: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<Workplace> {
        return try {
            val response = ApiClient.service.createWorkplace(
                CreateWorkplaceRequest(
                    name = name.trim(),
                    address = address?.trim()?.ifBlank { null },
                    latitude = latitude,
                    longitude = longitude
                )
            )
            val body = response.data
            if (response.success && body != null) {
                val wpId = body.workplace.realId.ifEmpty { UUID.randomUUID().toString() }
                val newWp = Workplace(
                    id = wpId,
                    name = body.workplace.name,
                    timezone = body.workplace.timezone ?: "Asia/Kolkata",
                    address = body.workplace.address,
                    latitude = body.workplace.latitude ?: latitude,
                    longitude = body.workplace.longitude ?: longitude,
                    role = WorkplaceRole.EMPLOYER,
                    code = body.workplace.workplaceCode
                )
                // Fetch full populated list from backend safely
                val allWps = try { ApiClient.service.getWorkplaces() } catch (_: Throwable) { null }
                val targetWp = if (allWps?.success == true && !allWps.data.isNullOrEmpty()) {
                    _workplaces.value = allWps.data
                    allWps.data.find { it.id == wpId } ?: newWp
                } else {
                    val updated = _workplaces.value.filter { it.id != wpId } + newWp
                    _workplaces.value = updated
                    newWp
                }
                _activeWorkplace.value = targetWp
                _isWorkplaceConfirmed.value = true
                preferencesManager.setActiveWorkplace(targetWp)
                refreshWorkplaceData()
                Result.success(targetWp)
            } else {
                val err = response.error?.message ?: "Failed to create workplace"
                Result.failure(Exception(err))
            }
        } catch (e: Throwable) {
            val isAuthErr = (e is retrofit2.HttpException && e.code() == 401) ||
                (e.message?.contains("401") == true) ||
                (e.message?.contains("jwt expired", ignoreCase = true) == true) ||
                (e.message?.contains("Unauthorized", ignoreCase = true) == true)
            if (isAuthErr) {
                val refreshed = refreshAccessToken()
                if (refreshed) {
                    try {
                        val retryResponse = ApiClient.service.createWorkplace(
                            CreateWorkplaceRequest(
                                name = name.trim(),
                                address = address?.trim()?.ifBlank { null },
                                latitude = latitude,
                                longitude = longitude
                            )
                        )
                        val retryBody = retryResponse.data
                        if (retryResponse.success && retryBody != null) {
                            val wpId = retryBody.workplace.realId.ifEmpty { UUID.randomUUID().toString() }
                            val newWp = Workplace(
                                id = wpId,
                                name = retryBody.workplace.name,
                                timezone = retryBody.workplace.timezone ?: "Asia/Kolkata",
                                address = retryBody.workplace.address,
                                latitude = retryBody.workplace.latitude ?: latitude,
                                longitude = retryBody.workplace.longitude ?: longitude,
                                role = WorkplaceRole.EMPLOYER,
                                code = retryBody.workplace.workplaceCode
                            )
                            val allWps = try { ApiClient.service.getWorkplaces() } catch (_: Throwable) { null }
                            val targetWp = if (allWps?.success == true && !allWps.data.isNullOrEmpty()) {
                                _workplaces.value = allWps.data
                                allWps.data.find { it.id == wpId } ?: newWp
                            } else {
                                val updated = _workplaces.value.filter { it.id != wpId } + newWp
                                _workplaces.value = updated
                                newWp
                            }
                            _activeWorkplace.value = targetWp
                            _isWorkplaceConfirmed.value = true
                            preferencesManager.setActiveWorkplace(targetWp)
                            refreshWorkplaceData()
                            return Result.success(targetWp)
                        }
                    } catch (_: Exception) {}
                }
            }
            Result.failure(Exception(parseErrorMessage(e)))
        }
    }

    suspend fun fetchMyJoinRequests(): Result<List<JoinRequestItem>> {
        return try {
            val res = ApiClient.service.getMyJoinRequests()
            if (res.success && res.data != null) {
                _myJoinRequests.value = res.data
                Result.success(res.data)
            } else {
                Result.failure(Exception(res.error?.message ?: "Failed to fetch join requests"))
            }
        } catch (e: Throwable) {
            Result.failure(Exception(parseErrorMessage(e)))
        }
    }

    suspend fun previewJoin(tokenOrCode: String): Result<JoinPreviewResponse> {
        return try {
            val res = ApiClient.service.previewJoin(
                JoinPreviewRequest(token = tokenOrCode.trim())
            )
            if (res.success && res.data != null) {
                Result.success(res.data)
            } else {
                Result.failure(Exception(res.error?.message ?: "Workplace or team not found"))
            }
        } catch (e: Throwable) {
            Result.failure(Exception(parseErrorMessage(e)))
        }
    }

    suspend fun submitJoinRequest(tokenOrCode: String, note: String? = null): Result<Map<String, Any>> {
        return try {
            val res = ApiClient.service.submitJoinRequest(
                SubmitJoinRequest(
                    token = tokenOrCode.trim(),
                    note = note?.trim()?.ifBlank { null }
                )
            )
            if (res.success && res.data != null) {
                fetchMyJoinRequests()
                Result.success(res.data)
            } else {
                Result.failure(Exception(res.error?.message ?: "Failed to submit join request"))
            }
        } catch (e: Throwable) {
            Result.failure(Exception(parseErrorMessage(e)))
        }
    }

    suspend fun cancelMyJoinRequest(requestId: String): Result<Unit> {
        return try {
            val res = ApiClient.service.cancelMyJoinRequest(requestId)
            if (res.success) {
                _myJoinRequests.value = _myJoinRequests.value.filter { it.id != requestId }
                Result.success(Unit)
            } else {
                Result.failure(Exception(res.error?.message ?: "Failed to cancel join request"))
            }
        } catch (e: Throwable) {
            Result.failure(Exception(parseErrorMessage(e)))
        }
    }

    suspend fun fetchWorkplaceJoinRequests(workplaceId: String? = null): Result<List<EmployerJoinRequestItem>> {
        val wpId = workplaceId ?: _activeWorkplace.value?.id ?: return Result.success(emptyList())
        return try {
            val res = ApiClient.service.getWorkplaceJoinRequests(wpId)
            if (res.success && res.data != null) {
                _workplaceJoinRequests.value = res.data
                Result.success(res.data)
            } else {
                Result.failure(Exception(res.error?.message ?: "Failed to fetch join requests"))
            }
        } catch (e: Throwable) {
            Result.failure(Exception(parseErrorMessage(e)))
        }
    }

    fun approveWorkplaceJoinRequest(requestId: String, workplaceId: String? = null) {
        val wpId = workplaceId ?: _activeWorkplace.value?.id ?: return
        _workplaceJoinRequests.value = _workplaceJoinRequests.value.filter { it.realId != requestId && it.id != requestId }
        repositoryScope.launch {
            try {
                val res = ApiClient.service.approveJoinRequest(wpId, requestId)
                if (res.success) {
                    refreshWorkplaceData(force = true)
                } else {
                    fetchWorkplaceJoinRequests(wpId)
                }
            } catch (_: Throwable) {
                fetchWorkplaceJoinRequests(wpId)
            }
        }
    }

    fun rejectWorkplaceJoinRequest(requestId: String, reason: String? = null, workplaceId: String? = null) {
        val wpId = workplaceId ?: _activeWorkplace.value?.id ?: return
        _workplaceJoinRequests.value = _workplaceJoinRequests.value.filter { it.realId != requestId && it.id != requestId }
        repositoryScope.launch {
            try {
                val body = if (!reason.isNullOrBlank()) mapOf("reason" to reason) else emptyMap()
                val res = ApiClient.service.rejectJoinRequest(wpId, requestId, body)
                if (res.success) {
                    refreshWorkplaceData(force = true)
                } else {
                    fetchWorkplaceJoinRequests(wpId)
                }
            } catch (_: Throwable) {
                fetchWorkplaceJoinRequests(wpId)
            }
        }
    }

    suspend fun getWorkplaceJoinQr(workplaceId: String, teamId: String? = null): Result<Map<String, Any>> {
        return try {
            val res = ApiClient.service.getWorkplaceJoinQr(workplaceId, teamId)
            if (res.success && res.data != null) {
                Result.success(res.data)
            } else {
                Result.failure(Exception(res.error?.message ?: "Failed to generate join QR"))
            }
        } catch (e: Throwable) {
            Result.failure(Exception(parseErrorMessage(e)))
        }
    }

    suspend fun rotateWorkplaceJoinQr(workplaceId: String, teamId: String? = null): Result<Map<String, Any>> {
        return try {
            val body = if (!teamId.isNullOrBlank()) mapOf("teamId" to teamId) else emptyMap()
            val res = ApiClient.service.rotateWorkplaceJoinQr(workplaceId, body)
            if (res.success && res.data != null) {
                Result.success(res.data)
            } else {
                Result.failure(Exception(res.error?.message ?: "Failed to rotate join QR"))
            }
        } catch (e: Throwable) {
            Result.failure(Exception(parseErrorMessage(e)))
        }
    }

    fun refreshWorkplaceData(force: Boolean = false) {
        val wp = _activeWorkplace.value ?: return
        val wpId = wp.id
        if (wpId == "pending_load" || wpId.isBlank()) return
        val role = wp.role

        val now = System.currentTimeMillis()
        if (!force && lastRefreshedWorkplaceId == wpId && refreshWorkplaceJob?.isActive == true) {
            return
        }

        refreshWorkplaceJob?.cancel()
        lastRefreshedWorkplaceId = wpId
        lastRefreshedTimestamp = now

        refreshWorkplaceJob = repositoryScope.launch {
            if (role == WorkplaceRole.EMPLOYER) {
                _isEmployerTodayLoading.value = true
                try {
                    coroutineScope {
                        val sessionJob = launch { fetchTodaySession(wpId) }
                        val rosterJob = launch { fetchTodayRoster(wpId) }
                        val requestsJob = launch { fetchPendingRequests(wpId) }
                        val employeesJob = launch { fetchEmployeesList(wpId) }
                        val teamsJob = launch { fetchTeamsList(wpId) }
                        val joinReqsJob = launch { fetchWorkplaceJoinRequests(wpId) }
                        listOf(sessionJob, rosterJob, requestsJob, employeesJob, teamsJob, joinReqsJob).joinAll()
                    }
                } finally {
                    _isEmployerTodayLoading.value = false
                }
            } else {
                fetchEmployeeTodayStatus(wpId)
            }
            fetchNotificationsList()
        }
    }

    suspend fun refreshWorkplaceDataAwait() {
        val wp = _activeWorkplace.value ?: return
        val wpId = wp.id
        if (wpId == "pending_load" || wpId.isBlank()) return
        val role = wp.role

        // An initial refresh may still be running when the user pulls to refresh.
        // Reuse that result instead of issuing the same network wave twice.
        if (lastRefreshedWorkplaceId == wpId && refreshWorkplaceJob?.isActive == true) {
            refreshWorkplaceJob?.join()
            return
        }

        lastRefreshedWorkplaceId = wpId
        lastRefreshedTimestamp = System.currentTimeMillis()

        coroutineScope {
            if (role == WorkplaceRole.EMPLOYER) {
                _isEmployerTodayLoading.value = true
                try {
                    val jobs = listOf(
                        launch { fetchTodaySession(wpId) },
                        launch { fetchTodayRoster(wpId) },
                        launch { fetchPendingRequests(wpId) },
                        launch { fetchEmployeesList(wpId) },
                        launch { fetchTeamsList(wpId) },
                        launch { fetchWorkplaceJoinRequests(wpId) },
                        launch { fetchNotificationsList() },
                        launch {
                            try {
                                val res = ApiClient.service.getWorkplaces()
                                if (res.success && !res.data.isNullOrEmpty()) {
                                    _workplaces.value = res.data
                                    val matched = res.data.find { it.id == wpId }
                                    if (matched != null) {
                                        _activeWorkplace.value = matched
                                        preferencesManager.setActiveWorkplace(matched)
                                    }
                                }
                            } catch (_: Throwable) {}
                        }
                    )
                    jobs.joinAll()
                } finally {
                    _isEmployerTodayLoading.value = false
                }
            } else {
                val jobs = listOf(
                    launch { fetchEmployeeTodayStatus(wpId) },
                    launch { fetchNotificationsList() },
                    launch {
                        try {
                            val res = ApiClient.service.getWorkplaces()
                            if (res.success && !res.data.isNullOrEmpty()) {
                                _workplaces.value = res.data
                                val matched = res.data.find { it.id == wpId }
                                if (matched != null) {
                                    _activeWorkplace.value = matched
                                    preferencesManager.setActiveWorkplace(matched)
                                }
                            }
                        } catch (_: Throwable) {}
                    }
                )
                jobs.joinAll()
            }
        }
    }

    suspend fun refreshWorkplacesListAwait() {
        try {
            val res = ApiClient.service.getWorkplaces()
            if (res.success && !res.data.isNullOrEmpty()) {
                val wps = res.data
                _workplaces.value = wps
                val currentWpId = _activeWorkplace.value?.id
                val matched = wps.find { it.id == currentWpId } ?: wps.first()
                _activeWorkplace.value = matched
                preferencesManager.setActiveWorkplace(matched)
            } else if (res.success && res.data != null && res.data.isEmpty()) {
                _workplaces.value = emptyList()
                _activeWorkplace.value = null
                preferencesManager.clearActiveWorkplace()
            }
            fetchMyJoinRequests()
        } catch (_: Throwable) {}
    }

    suspend fun refreshSecurityStatusAwait() {
        try {
            val meRes = ApiClient.service.getMe()
            if (meRes.success && meRes.data != null) {
                val me = meRes.data
                val currentToken = preferencesManager.getAccessToken() ?: ""
                val currentRefresh = preferencesManager.getRefreshToken() ?: ""
                preferencesManager.saveAuth(
                    token = currentToken,
                    refreshToken = currentRefresh,
                    user = me.user
                )
                _currentUser.value = me.user
            }
            checkMpinStatus()
        } catch (_: Throwable) {}
    }

    fun reloadRoster() {
        val wpId = _activeWorkplace.value?.id ?: return
        if (wpId.isNotBlank() && wpId != "pending_load") {
            repositoryScope.launch { fetchTodayRoster(wpId) }
        }
    }

    fun reloadRequests() {
        val wpId = _activeWorkplace.value?.id ?: return
        if (wpId.isNotBlank() && wpId != "pending_load") {
            repositoryScope.launch { fetchPendingRequests(wpId) }
        }
    }

    fun reloadEmployees() {
        val wpId = _activeWorkplace.value?.id ?: return
        if (wpId.isNotBlank() && wpId != "pending_load") {
            repositoryScope.launch { fetchEmployeesList(wpId) }
        }
    }

    fun reloadTeams() {
        val wpId = _activeWorkplace.value?.id ?: return
        if (wpId.isNotBlank() && wpId != "pending_load") {
            repositoryScope.launch { fetchTeamsList(wpId) }
        }
    }

    fun reloadEmployeeTodayStatus() {
        val wpId = _activeWorkplace.value?.id ?: return
        if (wpId.isNotBlank() && wpId != "pending_load") {
            repositoryScope.launch { fetchEmployeeTodayStatus(wpId) }
        }
    }

    private suspend fun fetchTodaySession(workplaceId: String) {
        try {
            val res = ApiClient.service.getTodaySession(workplaceId)
            if (res.success && res.data != null) {
                _todaySession.value = res.data
            }
        } catch (_: Throwable) {
        }
    }

    private suspend fun fetchTodayRoster(workplaceId: String) {
        _isRosterLoading.value = true
        try {
            val res = ApiClient.service.getTodayRoster(workplaceId)
            if (res.success && res.data != null) {
                _roster.value = res.data.roster
            }
        } catch (_: Throwable) {
        } finally {
            _isRosterLoading.value = false
        }
    }

    private suspend fun fetchPendingRequests(workplaceId: String) {
        _isRequestsLoading.value = true
        try {
            val res = ApiClient.service.getRequests(workplaceId, status = "PENDING")
            if (res.success && res.data != null) {
                _requests.value = res.data
            }
        } catch (_: Throwable) {
        } finally {
            _isRequestsLoading.value = false
        }
    }

    private suspend fun fetchEmployeesList(workplaceId: String) {
        _isEmployeesLoading.value = true
        try {
            val res = ApiClient.service.getEmployees(workplaceId)
            if (res.success && res.data != null) {
                _employees.value = res.data
            }
        } catch (_: Throwable) {
        } finally {
            _isEmployeesLoading.value = false
        }
    }

    suspend fun refreshTeams(workplaceId: String? = null) {
        val wpId = workplaceId ?: _activeWorkplace.value?.id ?: return
        fetchTeamsList(wpId)
    }

    private suspend fun fetchTeamsList(workplaceId: String) {
        _isTeamsLoading.value = true
        try {
            var res = try {
                ApiClient.service.getTeams(workplaceId)
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 401 && refreshAccessToken()) {
                    ApiClient.service.getTeams(workplaceId)
                } else throw e
            }
            if (res.success && res.data != null) {
                _teams.value = res.data
            }
        } catch (_: Throwable) {
        } finally {
            _isTeamsLoading.value = false
        }
    }

    suspend fun createTeam(name: String, description: String? = null, color: String? = null): Result<Team> {
        val wpId = _activeWorkplace.value?.id ?: return Result.failure(Exception("No active workplace"))
        return try {
            val payload = mutableMapOf("name" to name)
            if (!description.isNullOrBlank()) payload["description"] = description
            if (!color.isNullOrBlank()) payload["color"] = color
            val res = ApiClient.service.createTeam(wpId, payload)
            if (res.success && res.data != null) {
                _teams.value = _teams.value + res.data
                fetchTeamsList(wpId)
                Result.success(res.data)
            } else {
                Result.failure(Exception(res.error?.message ?: "Failed to create team"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchEmployeeTodayStatus(workplaceId: String) {
        _isEmployeeStatusLoading.value = true
        try {
            val res = ApiClient.service.getMyTodayRequest(workplaceId)
            if (res.success) {
                val req = res.data
                if (req != null) {
                    if (req.status == RequestStatus.PENDING) {
                        _employeeRequestPending.value = true
                        _employeeTodayStatus.value = AttendanceStatus.PENDING
                    } else if (req.status == RequestStatus.APPROVED) {
                        _employeeRequestPending.value = false
                        _employeeTodayStatus.value = AttendanceStatus.PRESENT
                        _employeeCheckInTime.value = req.requestedAt
                    }
                }
            }
        } catch (_: Throwable) {
        } finally {
            _isEmployeeStatusLoading.value = false
        }
    }

    private suspend fun fetchNotificationsList() {
        try {
            val res = ApiClient.service.getNotifications()
            if (res.success && res.data != null) {
                _notifications.value = res.data
            }
        } catch (_: Throwable) {
        }
    }

    // ==========================================
    // ACTION DISPATCHERS (OPTIMISTIC + LIVE SYNC)
    // ==========================================

    fun openAttendanceSession() {
        val wpId = _activeWorkplace.value?.id ?: return
        val optimisticSession = AttendanceSession(
            session = SessionDetails(
                id = UUID.randomUUID().toString(),
                workplaceId = wpId,
                attendanceDate = LocalDate.now().toString(),
                status = SessionStatus.OPEN,
                openedAt = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")),
                expiresAt = "08:00 PM"
            ),
            qrToken = "bizora_qr_${System.currentTimeMillis()}",
            qrPayload = "bizora://attendance?session=${System.currentTimeMillis()}&workplace=$wpId",
            checkoutQrPayload = "bizora://attendance?session=${System.currentTimeMillis()}&type=CHECK_OUT"
        )
        _todaySession.value = optimisticSession

        repositoryScope.launch {
            try {
                val res = ApiClient.service.openSession(wpId)
                if (res.success && res.data != null) {
                    _todaySession.value = res.data
                }
            } catch (_: Throwable) {
            }
        }
    }

    fun closeAttendanceSession() {
        val wpId = _activeWorkplace.value?.id ?: return
        val current = _todaySession.value ?: return
        _todaySession.value = current.copy(
            session = current.session.copy(status = SessionStatus.CLOSED),
            qrToken = null,
            qrPayload = null
        )

        repositoryScope.launch {
            try {
                ApiClient.service.closeSession(wpId)
            } catch (_: Throwable) {
            }
        }
    }

    fun approveRequest(requestId: String) {
        val wpId = _activeWorkplace.value?.id ?: return
        val target = _requests.value.find { it.id == requestId } ?: return
        _requests.value = _requests.value.filter { it.id != requestId }

        // Update in roster
        _roster.value = _roster.value.map { item ->
            if (item.name == target.employee.name || item.employeeCode == target.employee.code) {
                item.copy(
                    status = AttendanceStatus.PRESENT,
                    checkInTime = target.requestedAt,
                    approvedAt = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
                )
            } else item
        }

        repositoryScope.launch {
            try {
                ApiClient.service.approveRequest(wpId, requestId)
                fetchTodayRoster(wpId)
            } catch (_: Throwable) {
            }
        }
    }

    fun rejectRequest(requestId: String, reason: String = "Declined by employer") {
        val wpId = _activeWorkplace.value?.id ?: return
        val target = _requests.value.find { it.id == requestId } ?: return
        _requests.value = _requests.value.filter { it.id != requestId }

        _roster.value = _roster.value.map { item ->
            if (item.name == target.employee.name) {
                item.copy(status = AttendanceStatus.NOT_MARKED)
            } else item
        }

        repositoryScope.launch {
            try {
                ApiClient.service.rejectRequest(wpId, requestId, RejectAttendanceRequest(reason))
                fetchTodayRoster(wpId)
            } catch (_: Throwable) {
            }
        }
    }

    suspend fun markManualAttendance(memberId: String, status: AttendanceStatus, reason: String): Result<Unit> {
        val wp = _activeWorkplace.value ?: return Result.failure(Exception("No active workplace"))
        return try {
            val attendanceDate = LocalDate.now(java.time.ZoneId.of(wp.timezone)).toString()
            val response = ApiClient.service.markManualAttendance(
                wp.id,
                ManualAttendanceRequest(
                    employeeMemberId = memberId,
                    attendanceDate = attendanceDate,
                    status = status.name,
                    correctionReason = reason
                )
            )
            if (!response.success) {
                Result.failure(Exception(response.error?.message ?: "Could not save attendance"))
            } else {
                fetchTodayRoster(wp.id)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(Exception(parseErrorMessage(e)))
        }
    }

    suspend fun addEmployee(
        name: String,
        email: String,
        pin: String? = null,
        teamId: String? = null
    ): Result<Unit> {
        val wpId = _activeWorkplace.value?.id ?: return Result.failure(Exception("No active workplace"))
        val resolvedTeamId = teamId ?: _teams.value.find { it.isDefault }?.id ?: _teams.value.firstOrNull()?.id
        val assignedTeam = _teams.value.find { it.id == resolvedTeamId }

        return try {
            val res = ApiClient.service.addEmployee(
                wpId,
                AddEmployeeRequest(
                    name = name,
                    email = email.ifBlank { null },
                    pin = pin?.ifBlank { null },
                    teamId = resolvedTeamId?.ifBlank { null },
                    teamCode = assignedTeam?.displayCode
                )
            )
            if (res.success) {
                fetchEmployeesList(wpId)
                fetchTeamsList(wpId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(res.error?.message ?: "Failed to add employee"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteEmployee(memberId: String): Result<Unit> {
        val wpId = _activeWorkplace.value?.id ?: return Result.failure(Exception("No active workplace"))
        return try {
            val res = ApiClient.service.deleteEmployee(wpId, memberId)
            if (res.success) {
                _employees.value = _employees.value.filter { it.id != memberId }
                fetchTeamsList(wpId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(res.error?.message ?: "Failed to delete employee"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun assignEmployeeToTeam(memberId: String, teamId: String): Result<Boolean> {
        val wpId = _activeWorkplace.value?.id ?: return Result.failure(Exception("No active workplace"))
        return try {
            val res = ApiClient.service.updateEmployee(wpId, memberId, mapOf("teamId" to teamId))
            if (res.success) {
                fetchEmployeesList(wpId)
                fetchTeamsList(wpId)
                Result.success(true)
            } else {
                Result.failure(Exception(res.error?.message ?: "Failed to assign employee to team"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun submitEmployeeScanRequest(token: String) {
        _employeeRequestPending.value = true
        _employeeTodayStatus.value = AttendanceStatus.PENDING

        repositoryScope.launch {
            try {
                ApiClient.service.submitScanRequest(
                    ScanAttendanceRequest(
                        qrToken = token,
                        requestType = "CHECK_IN"
                    )
                )
            } catch (_: Throwable) {
            }
        }
    }

    fun submitAttendanceWithLocation(
        workplaceId: String,
        requestType: String = "CHECK_IN",
        latitude: Double,
        longitude: Double,
        accuracy: Float = 0f,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))

        repositoryScope.launch {
            try {
                val res = ApiClient.service.submitScanRequest(
                    ScanAttendanceRequest(
                        workplaceId = workplaceId,
                        requestType = requestType,
                        source = "DIRECT",
                        latitude = latitude,
                        longitude = longitude,
                        accuracy = accuracy
                    )
                )

                if (res.success) {
                    val msg = (res.data?.get("message") as? String)
                        ?: if (requestType == "CHECK_OUT") "Check-out recorded! Geolocation verified." else "Attendance recorded! Geolocation verified."
                    val autoApproved = (res.data?.get("autoApproved") as? Boolean) ?: true

                    if (autoApproved) {
                        _employeeRequestPending.value = false
                        if (requestType == "CHECK_OUT") {
                            _employeeCheckOutTime.value = nowTime
                        } else {
                            _employeeTodayStatus.value = AttendanceStatus.PRESENT
                            _employeeCheckInTime.value = nowTime
                        }
                    } else {
                        _employeeRequestPending.value = true
                        _employeeTodayStatus.value = AttendanceStatus.PENDING
                    }

                    fetchEmployeeTodayStatus(workplaceId)
                    fetchTodayRoster(workplaceId)
                    withContext(Dispatchers.Main) {
                        onSuccess(msg)
                    }
                } else {
                    val errMsg = res.error?.message ?: "Unable to record attendance"
                    withContext(Dispatchers.Main) {
                        onError(errMsg)
                    }
                }
            } catch (t: Throwable) {
                val parsed = parseErrorMessage(t)
                withContext(Dispatchers.Main) {
                    onError(parsed)
                }
            }
        }
    }

    fun updateWorkplaceGeofence(
        workplaceId: String,
        enabled: Boolean,
        latitude: Double?,
        longitude: Double?,
        radiusMeters: Double = 100.0,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentWp = _activeWorkplace.value ?: return
        val updatedSettings = (currentWp.attendanceSettings ?: AttendanceSettings()).copy(
            requireGeofence = enabled,
            latitude = latitude,
            longitude = longitude,
            geofenceRadius = radiusMeters
        )

        _activeWorkplace.value = currentWp.copy(attendanceSettings = updatedSettings)

        repositoryScope.launch {
            try {
                val body = mutableMapOf<String, Any>()
                val settingsMap = mutableMapOf<String, Any>(
                    "requireGeofence" to enabled,
                    "geofenceRadius" to radiusMeters
                )
                if (latitude != null) settingsMap["latitude"] = latitude
                if (longitude != null) settingsMap["longitude"] = longitude
                body["attendanceSettings"] = settingsMap

                val res = ApiClient.service.updateWorkplace(workplaceId, body)
                if (res.success && res.data != null) {
                    _activeWorkplace.value = res.data
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError(res.error?.message ?: "Failed to update workplace settings")
                    }
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    onError(parseErrorMessage(t))
                }
            }
        }
    }

    fun simulateApproval() {
        _employeeRequestPending.value = false
        _employeeTodayStatus.value = AttendanceStatus.PRESENT
        _employeeCheckInTime.value = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
    }

    // ==========================================
    // ERROR PARSER
    // ==========================================

    private fun parseErrorMessage(throwable: Throwable): String {
        if (throwable is java.net.SocketTimeoutException) {
            return "Server request timed out. Please try again."
        }
        if (throwable is java.net.UnknownHostException) {
            return "Unable to reach server. Please check your internet connection."
        }
        if (throwable is java.io.IOException && throwable.message?.contains("timeout", ignoreCase = true) == true) {
            return "Connection timed out. Please try again."
        }
        if (throwable is HttpException) {
            val errorBody = throwable.response()?.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                try {
                    val jsonObj = org.json.JSONObject(errorBody)
                    if (jsonObj.has("error")) {
                        val errObj = jsonObj.optJSONObject("error")
                        val msg = errObj?.optString("message")
                        if (!msg.isNullOrBlank()) return msg
                    }
                    if (jsonObj.has("message")) {
                        val msg = jsonObj.optString("message")
                        if (!msg.isNullOrBlank()) return msg
                    }
                } catch (_: Exception) {
                    val regex = """"message"\s*:\s*"([^"]+)"""".toRegex()
                    val match = regex.find(errorBody)
                    if (match != null) {
                        return match.groupValues[1]
                    }
                }
            }
            return when (throwable.code()) {
                400 -> "Invalid request data. Please check your inputs."
                401 -> "Invalid credentials or PIN. Please try again."
                403 -> "You do not have permission to perform this action."
                404 -> "Resource or employee record not found."
                429 -> "Too many attempts. Please wait a moment and try again."
                else -> "Server error (${throwable.code()}). Please try again."
            }
        }
        return throwable.localizedMessage ?: "Network error. Please check your internet connection."
    }
}
