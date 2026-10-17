package bizora.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import bizora.app.data.local.PreferencesManager
import bizora.app.data.model.Workplace
import bizora.app.data.model.WorkplaceRole
import bizora.app.data.repository.BizoraRepository
import bizora.app.ui.screens.AppLockScreen
import bizora.app.ui.screens.OnboardingScreen
import bizora.app.ui.screens.OnboardingStep
import bizora.app.ui.screens.EmployeeDashboard
import bizora.app.ui.screens.EmployerDashboard
import bizora.app.ui.screens.MpinSetupScreen
import bizora.app.ui.screens.MpinUnlockScreen
import bizora.app.ui.screens.PinLoginScreen
import bizora.app.ui.screens.ReviewerLoginScreen
import bizora.app.ui.screens.SignInScreen
import bizora.app.ui.screens.WelcomeScreen
import bizora.app.ui.screens.WorkplaceSelectScreen
import bizora.app.ui.theme.BizoraTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.UUID

class MainActivity : FragmentActivity() {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var repository: BizoraRepository
    private var activeNavController: NavController? = null
    private var pendingAuthToken: String? = null
    private val _isAuthenticating = MutableStateFlow(false)
    private val _authError = MutableStateFlow<String?>(null)
    private var isMpinUnlocked = false
    private var isAwaitingOAuthReturn = false
    private var hasPausedForOAuth = false

    companion object {
        const val GOOGLE_CLIENT_ID = "667573150359-4cff25jgf98hqq00pdrnojslk4bri5sb.apps.googleusercontent.com"
        const val GOOGLE_CALLBACK_URL = "https://www.attendance-tracker.shivamshankhdhar.online/api/v1/auth/google/callback"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as BizoraApplication
        preferencesManager = app.preferencesManager
        repository = app.repository

        handleAuthIntent(intent)

        setContent {
            val startupState by preferencesManager.startupStateFlow.collectAsState(initial = null)
            val isAuthenticating by _isAuthenticating.collectAsState()
            val authError by _authError.collectAsState()

            val currentUser by repository.currentUser.collectAsState()
            val workplacesList by repository.workplaces.collectAsState()
            val activeWp by repository.activeWorkplace.collectAsState()

            BizoraTheme(darkTheme = false) {
                val state = startupState
                if (state == null || !state.isReady) {
                    Surface(
                        color = Color(0xFFF8FAEF),
                        modifier = Modifier.fillMaxSize()
                    ) {}
                    return@BizoraTheme
                }

                val currentActiveWp = workplacesList.firstOrNull { it.id == activeWp?.id }
                    ?: workplacesList.firstOrNull()

                // Fix the initial route for this Activity; later transitions use explicit navigation.
                val startDest = remember(state.isReady) {
                    initialAuthDestination(state.hasSeenWelcome, state.accessToken, state.hasMpin)
                }

                val navController = rememberNavController()
                activeNavController = navController
                val coroutineScope = rememberCoroutineScope()
                val pendingInviteCode by repository.pendingInviteCode.collectAsState()

                LaunchedEffect(navController) {
                    pendingAuthToken?.let { token ->
                        pendingAuthToken = null
                        exchangeTokenAndNavigate(token)
                    }
                }

                LaunchedEffect(currentActiveWp?.id, currentActiveWp?.role) {
                    if (isAuthenticating) return@LaunchedEffect
                    val currentRoute = navController.currentBackStackEntry?.destination?.route ?: return@LaunchedEffect
                    if (currentRoute == "employee_dashboard" || currentRoute == "employer_dashboard") {
                        val workplace = currentActiveWp ?: return@LaunchedEffect
                        val isEmployer = workplace.role == WorkplaceRole.EMPLOYER
                        if (currentRoute == "employee_dashboard" && isEmployer) {
                            navController.navigate("employer_dashboard") {
                                popUpTo("employee_dashboard") { inclusive = true }
                            }
                        } else if (currentRoute == "employer_dashboard" && !isEmployer) {
                            navController.navigate("employee_dashboard") {
                                popUpTo("employer_dashboard") { inclusive = true }
                            }
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = startDest
                ) {
                    composable("welcome") {
                        WelcomeScreen(
                            onGetStarted = {
                                coroutineScope.launch {
                                    preferencesManager.setHasSeenWelcome(true)
                                }
                                navController.navigate("sign_in") {
                                    popUpTo("welcome") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("onboarding") {
                        val inviteCode = pendingInviteCode ?: repository.getPendingInviteCode()
                        OnboardingScreen(
                            repository = repository,
                            initialStep = if (!inviteCode.isNullOrBlank()) OnboardingStep.TEAM_DETAILS else OnboardingStep.WELCOME_CHOOSER,
                            initialInviteCode = inviteCode,
                            onWorkplaceCreated = { wp: Workplace ->
                                navController.navigate("employer_dashboard") {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onWorkplaceJoined = { wp: Workplace ->
                                repository.clearPendingInviteCode()
                                navController.navigate("employee_dashboard") {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onSignOut = {
                                coroutineScope.launch {
                                    repository.signOut()
                                    preferencesManager.setHasSeenWelcome(true)
                                    isMpinUnlocked = false
                                    navController.navigate("sign_in") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable("create_workplace") {
                        OnboardingScreen(
                            repository = repository,
                            initialStep = OnboardingStep.CREATE_WORKPLACE,
                            onBack = {
                                if (!navController.popBackStack()) {
                                    val wp = repository.activeWorkplace.value ?: repository.workplaces.value.firstOrNull()
                                    if (wp != null) {
                                        if (wp.role == WorkplaceRole.EMPLOYER) {
                                            navController.navigate("employer_dashboard") {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        } else {
                                            navController.navigate("employee_dashboard") {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        }
                                    } else {
                                        navController.navigate("workplace_select") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                }
                            },
                            onWorkplaceCreated = { wp: Workplace ->
                                navController.navigate("employer_dashboard") {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onWorkplaceJoined = { wp: Workplace ->
                                navController.navigate("employee_dashboard") {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onSignOut = {
                                coroutineScope.launch {
                                    repository.signOut()
                                    preferencesManager.setHasSeenWelcome(true)
                                    isMpinUnlocked = false
                                    navController.navigate("sign_in") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable("sign_in") {
                        SignInScreen(
                            pendingInviteCode = pendingInviteCode,
                            repository = repository,
                            isLoading = isAuthenticating,
                            errorMessage = authError,
                            onDismissError = { _authError.value = null },
                            onCancelLoading = {
                                isAwaitingOAuthReturn = false
                                hasPausedForOAuth = false
                                _isAuthenticating.value = false
                            },
                            onGoogleSignIn = {
                                launchGoogleOAuth()
                            },
                            onPinLoginClick = {
                                navController.navigate("pin_login")
                            },
                            onContinueAsEmployer = {
                                coroutineScope.launch {
                                    _isAuthenticating.value = true
                                    _authError.value = null
                                    try {
                                        val result = repository.loginAsReviewer(
                                            isEmployer = true,
                                            customEmail = "demo_employer@test.com",
                                            customName = "Demo Employer"
                                        )
                                        if (result.isSuccess) {
                                            navigateAfterAuth()
                                        } else {
                                            val err = result.exceptionOrNull()?.message ?: "Login failed"
                                            _authError.value = err
                                            Toast.makeText(this@MainActivity, err, Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        val err = e.localizedMessage ?: "Login failed"
                                        _authError.value = err
                                        Toast.makeText(this@MainActivity, err, Toast.LENGTH_LONG).show()
                                    } finally {
                                        _isAuthenticating.value = false
                                    }
                                }
                            },
                            onReviewerLoginClick = {
                                navController.navigate("reviewer_login")
                            },
                            onTokenExchange = { manualToken ->
                                lifecycleScope.launch {
                                    exchangeTokenAndNavigate(manualToken)
                                }
                            }
                        )
                    }

                    composable("reviewer_login") {
                        ReviewerLoginScreen(
                            repository = repository,
                            onBack = { navController.popBackStack() },
                            onLoginEmployer = {
                                navigateAfterAuth(isFreshSignIn = true)
                            },
                            onLoginEmployee = {
                                isMpinUnlocked = true
                                val activeWp = repository.activeWorkplace.value
                                val workplacesList = repository.workplaces.value.ifEmpty { listOfNotNull(activeWp) }
                                val firstWp = workplacesList.firstOrNull()
                                if (firstWp != null) {
                                    repository.confirmWorkplace(firstWp)
                                    navController.navigate("employee_dashboard") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } else {
                                    proceedToWorkplaceOrDashboard(navController)
                                }
                            },
                            onLoginNewUser = {
                                navigateAfterAuth(isFreshSignIn = true)
                            }
                        )
                    }

                    composable("pin_login") {
                        PinLoginScreen(
                            repository = repository,
                            onBack = { navController.popBackStack() },
                            onLoginSuccess = {
                                isMpinUnlocked = true
                                val activeWp = repository.activeWorkplace.value
                                val workplacesList = repository.workplaces.value.ifEmpty { listOfNotNull(activeWp) }
                                val firstWp = workplacesList.firstOrNull()
                                if (firstWp != null) {
                                    repository.confirmWorkplace(firstWp)
                                    val dest = if (firstWp.role == WorkplaceRole.EMPLOYER) "employer_dashboard" else "employee_dashboard"
                                    navController.navigate(dest) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } else {
                                    proceedToWorkplaceOrDashboard(navController)
                                }
                            }
                        )
                    }

                    composable("mpin_setup") {
                        MpinSetupScreen(
                            repository = repository,
                            allowDismiss = true,
                            showSetupLater = true,
                            onSuccess = {
                                isMpinUnlocked = true
                                val nav = activeNavController ?: return@MpinSetupScreen
                                proceedToWorkplaceOrDashboard(nav)
                            },
                            onDismiss = {
                                val token = preferencesManager.getAccessToken()
                                if (token.isNullOrBlank()) {
                                    navController.navigate("sign_in") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } else {
                                    isMpinUnlocked = true
                                    val nav = activeNavController ?: return@MpinSetupScreen
                                    proceedToWorkplaceOrDashboard(nav)
                                }
                            }
                        )
                    }

                    composable("mpin_unlock") {
                        AppLockScreen(
                            repository = repository,
                            userName = currentUser?.name ?: "",
                            onUnlockSuccess = {
                                isMpinUnlocked = true
                                val nav = activeNavController ?: return@AppLockScreen
                                proceedToWorkplaceOrDashboard(nav)
                            }
                        )
                    }

                    composable("workplace_select") {
                        WorkplaceSelectScreen(
                            user = currentUser,
                            workplaces = workplacesList.ifEmpty { listOfNotNull(activeWp) },
                            selectedWorkplaceId = activeWp?.id ?: "",
                            onSelectWorkplace = {
                                // Selection is managed locally by WorkplaceSelectScreen;
                                // active workplace is officially confirmed upon tapping Continue
                            },
                            onEnterWorkplace = { wp ->
                                repository.confirmWorkplace(wp)
                                val dest = if (wp.role == WorkplaceRole.EMPLOYER) "employer_dashboard" else "employee_dashboard"
                                navController.navigate(dest) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onCreateNewWorkplace = {
                                navController.navigate("create_workplace")
                            },
                            onRefresh = {
                                repository.refreshWorkplacesListAwait()
                            },
                            onSignOut = {
                                coroutineScope.launch {
                                    repository.signOut()
                                    preferencesManager.setHasSeenWelcome(true)
                                    isMpinUnlocked = false
                                    navController.navigate("sign_in") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable("employer_dashboard") {
                        EmployerDashboard(
                            repository = repository,
                            isDark = false,
                            onLockAppNow = {
                                isMpinUnlocked = false
                                navController.navigate("mpin_unlock") {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onSignOut = {
                                coroutineScope.launch {
                                    repository.signOut()
                                    preferencesManager.setHasSeenWelcome(true)
                                    isMpinUnlocked = false
                                }
                                navController.navigate("sign_in") {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onToggleTheme = {},
                            onCreateNewWorkplace = {
                                navController.navigate("create_workplace")
                            }
                        )
                    }

                    composable("employee_dashboard") {
                        EmployeeDashboard(
                            repository = repository,
                            isDark = false,
                            onLockAppNow = {
                                isMpinUnlocked = false
                                navController.navigate("mpin_unlock") {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onSignOut = {
                                coroutineScope.launch {
                                    repository.signOut()
                                    preferencesManager.setHasSeenWelcome(true)
                                    isMpinUnlocked = false
                                }
                                navController.navigate("sign_in") {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onToggleTheme = {}
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (isAwaitingOAuthReturn) {
            hasPausedForOAuth = true
        }
        val app = application as? BizoraApplication
        val hasMpin = app?.preferencesManager?.hasMpin() == true
        if (hasMpin && !app.preferencesManager.getAccessToken().isNullOrEmpty()) {
            isMpinUnlocked = false
        }
    }

    override fun onResume() {
        super.onResume()
        // If the user left to the browser to sign in and returns without an auth token,
        // cancel the loading state so the spinner does not keep spinning indefinitely.
        if (isAwaitingOAuthReturn && hasPausedForOAuth) {
            isAwaitingOAuthReturn = false
            hasPausedForOAuth = false
            _isAuthenticating.value = false
        }
        val app = application as? BizoraApplication
        val hasMpin = (app?.preferencesManager?.hasMpin() == true) ||
            (app?.preferencesManager?.isAppLockEnabled() == true) ||
            (repository.currentUser.value?.hasMpin == true) ||
            (repository.currentUser.value?.appLockEnabled == true) ||
            (repository.mpinStatus.value?.hasMpin == true) ||
            (repository.mpinStatus.value?.appLockEnabled == true)
        if (hasMpin && !app?.preferencesManager?.getAccessToken().isNullOrEmpty() && !isMpinUnlocked) {
            val nav = activeNavController
            val currentRoute = nav?.currentBackStackEntry?.destination?.route
            if (currentRoute != null && currentRoute != "mpin_unlock" && currentRoute != "mpin_setup" && currentRoute != "sign_in" && currentRoute != "welcome") {
                nav.navigate("mpin_unlock") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    private fun proceedToWorkplaceOrDashboard(nav: NavController) {
        val token = preferencesManager.getAccessToken()
        if (token.isNullOrBlank()) {
            android.util.Log.e("MainActivity", "Blocked proceedToWorkplaceOrDashboard: No access token found!")
            nav.navigate("sign_in") {
                popUpTo(0) { inclusive = true }
            }
            return
        }
        val pendingInvite = repository.getPendingInviteCode()
        if (!pendingInvite.isNullOrBlank()) {
            nav.navigate("onboarding") {
                popUpTo(0) { inclusive = true }
            }
            return
        }
        val activeWp = repository.activeWorkplace.value
        val isConfirmed = repository.isWorkplaceConfirmed.value || preferencesManager.isWorkplaceConfirmed()
        val workplacesList = repository.workplaces.value.ifEmpty { listOfNotNull(activeWp) }
        val dest = resolvePostMpinDestination(
            workplaces = workplacesList,
            activeWorkplace = activeWp,
            isWorkplaceConfirmed = isConfirmed
        )
        if (dest == "employee_dashboard") {
            (workplacesList.firstOrNull { it.id == activeWp?.id } ?: workplacesList.firstOrNull())
                ?.let(repository::confirmWorkplace)
        }
        nav.navigate(dest) {
            popUpTo(0) { inclusive = true }
        }
    }

    private fun navigateAfterAuth(isFreshSignIn: Boolean = true) {
        val nav = activeNavController ?: return
        val token = preferencesManager.getAccessToken()
        if (token.isNullOrBlank()) {
            android.util.Log.e("MainActivity", "Blocked navigateAfterAuth: No access token found!")
            _authError.value = "Sign in was not completed. Please try again."
            return
        }
        val pendingInvite = repository.getPendingInviteCode()
        if (!pendingInvite.isNullOrBlank()) {
            isMpinUnlocked = true
            nav.navigate("onboarding") {
                popUpTo(0) { inclusive = true }
            }
            return
        }
        if (isFreshSignIn) {
            val activeWp = repository.activeWorkplace.value
            if (activeWp?.role != WorkplaceRole.EMPLOYEE) {
                repository.resetWorkplaceConfirmation()
            }
        }
        val app = application as? BizoraApplication
        val mpin = repository.mpinStatus.value
        val user = repository.currentUser.value
        val hasMpin = (mpin?.hasMpin == true) ||
            (mpin?.appLockEnabled == true) ||
            (user?.hasMpin == true) ||
            (user?.appLockEnabled == true) ||
            (app?.preferencesManager?.hasMpin() == true) ||
            (app?.preferencesManager?.isAppLockEnabled() == true)
        isMpinUnlocked = false
        nav.navigate(postSignInDestination(hasMpin)) {
            popUpTo(0) { inclusive = true }
        }
    }

    private fun launchGoogleOAuth() {
        try {
            _authError.value = null
            _isAuthenticating.value = true
            isAwaitingOAuthReturn = true
            hasPausedForOAuth = false
            val returnUrl = "bizora://auth"
            val nonce = UUID.randomUUID().toString()
            val authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                    "client_id=" + URLEncoder.encode(GOOGLE_CLIENT_ID, "UTF-8") +
                    "&redirect_uri=" + URLEncoder.encode(GOOGLE_CALLBACK_URL, "UTF-8") +
                    "&response_type=id_token" +
                    "&scope=" + URLEncoder.encode("openid email profile", "UTF-8") +
                    "&prompt=select_account" +
                    "&nonce=" + URLEncoder.encode(nonce, "UTF-8") +
                    "&state=" + URLEncoder.encode(returnUrl, "UTF-8")

            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(browserIntent)
            Toast.makeText(this, "Opening Google Sign-In...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            isAwaitingOAuthReturn = false
            hasPausedForOAuth = false
            _isAuthenticating.value = false
            _authError.value = "Could not open browser: ${e.localizedMessage}"
            Toast.makeText(this, "Could not open browser: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun extractInviteCode(uri: Uri): String? {
        val teamCode = uri.getQueryParameter("teamCode")
        val token = uri.getQueryParameter("token") ?: uri.getQueryParameter("code")
        if (!teamCode.isNullOrBlank() && !token.isNullOrBlank()) {
            return uri.toString()
        }

        val fromQuery = uri.getQueryParameter("teamCode")
            ?: uri.getQueryParameter("code")
            ?: uri.getQueryParameter("token")
            ?: uri.getQueryParameter("invite")
        if (!fromQuery.isNullOrBlank()) {
            return fromQuery.trim().replace("^#+".toRegex(), "")
        }

        val pathSegments = uri.pathSegments
        if (pathSegments.isNotEmpty()) {
            val last = pathSegments.last().trim()
            if (last.isNotEmpty() && !last.equals("join", ignoreCase = true)) {
                return last.replace("^#+".toRegex(), "")
            }
        }

        val fragment = uri.fragment
        if (!fragment.isNullOrBlank()) {
            val regex = Regex("(?:code|teamCode|token|invite)=([^&\\s#]+)", RegexOption.IGNORE_CASE)
            val match = regex.find(fragment)?.groupValues?.get(1)?.trim()
            if (!match.isNullOrBlank()) return match.replace("^#+".toRegex(), "")
        }

        return null
    }

    private fun isInviteUri(uri: Uri): Boolean {
        if (uri.scheme == "bizora") {
            val host = uri.host ?: ""
            val path = uri.path ?: ""
            if (host.contains("join", ignoreCase = true) || path.contains("join", ignoreCase = true)) return true
            if (uri.getQueryParameter("teamCode") != null || uri.getQueryParameter("code") != null || uri.getQueryParameter("token") != null) return true
        } else if (uri.scheme in listOf("http", "https")) {
            val host = uri.host ?: ""
            val path = uri.path ?: ""
            if ((host.contains("bizora.app", ignoreCase = true) || host.contains("bizora.shivamshankhdhar.online", ignoreCase = true)) &&
                path.contains("join", ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun handleAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return

        // 1. Check for invite link deep link
        if (isInviteUri(uri)) {
            intent.data = null
            val inviteCode = extractInviteCode(uri)
            if (!inviteCode.isNullOrBlank()) {
                repository.setPendingInviteCode(inviteCode)
                val nav = activeNavController
                if (nav != null && repository.isAuthenticated()) {
                    nav.navigate("onboarding") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            return
        }

        // 2. Otherwise handle OAuth return
        if (uri.scheme == "bizora") {
            intent.data = null
            isAwaitingOAuthReturn = false
            hasPausedForOAuth = false

            val error = uri.getQueryParameter("error") ?: uri.getQueryParameter("error_description")
            if (!error.isNullOrEmpty()) {
                _isAuthenticating.value = false
                if (error.contains("denied", ignoreCase = true) || error.contains("cancel", ignoreCase = true)) {
                    _authError.value = null
                } else {
                    _authError.value = "Google sign-in was cancelled or failed: $error"
                }
                return
            }

            var idToken = uri.getQueryParameter("id_token")
            if (idToken.isNullOrEmpty()) {
                val fragment = uri.fragment ?: ""
                for (param in fragment.split("&")) {
                    if (param.startsWith("id_token=")) {
                        idToken = param.substring("id_token=".length)
                        break
                    }
                }
            }

            if (!idToken.isNullOrEmpty()) {
                _isAuthenticating.value = true
                _authError.value = null
                if (activeNavController != null) {
                    lifecycleScope.launch {
                        exchangeTokenAndNavigate(idToken!!)
                    }
                } else {
                    pendingAuthToken = idToken
                }
            } else {
                _isAuthenticating.value = false
                _authError.value = null
            }
        }
    }

    private suspend fun exchangeTokenAndNavigate(idToken: String) {
        _isAuthenticating.value = true
        _authError.value = null
        try {
            val result = repository.exchangeGoogleToken(idToken)
            if (result.isSuccess) {
                Toast.makeText(this, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                val activeWp = repository.activeWorkplace.value
                val wps = repository.workplaces.value
                if (wps.size == 1 && wps.first().role == WorkplaceRole.EMPLOYEE) {
                    repository.confirmWorkplace(wps.first())
                    isMpinUnlocked = true
                    val nav = activeNavController ?: return
                    nav.navigate("employee_dashboard") {
                        popUpTo(0) { inclusive = true }
                    }
                } else {
                    navigateAfterAuth(isFreshSignIn = true)
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Authentication failed"
                _authError.value = errorMsg
                Toast.makeText(this, "Sign in failed: $errorMsg", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Sign in failed"
            _authError.value = errorMsg
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
        } finally {
            _isAuthenticating.value = false
            isAwaitingOAuthReturn = false
            hasPausedForOAuth = false
        }
    }
}
