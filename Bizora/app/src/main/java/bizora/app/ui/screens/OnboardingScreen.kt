package bizora.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import bizora.app.R
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import bizora.app.data.model.Workplace
import bizora.app.data.model.WorkplaceRole
import bizora.app.data.remote.JoinPreviewResponse
import bizora.app.data.remote.JoinRequestItem
import bizora.app.data.repository.BizoraRepository
import bizora.app.ui.components.AdBanner
import bizora.app.ui.components.CameraQrScanner
import bizora.app.ui.components.UserAvatar
import bizora.app.utils.LocationHelper
import kotlinx.coroutines.launch

private fun extractJoinCode(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return ""

    // 0. If it contains both teamCode and token (e.g. from invite link or QR code), keep the full URL/query so backend can verify direct join token
    val urlRegex = Regex("(?:https?://|bizora://)[^\\s\"']+", RegexOption.IGNORE_CASE)
    val urlMatch = urlRegex.find(trimmed)?.value
    if (!urlMatch.isNullOrBlank()) {
        try {
            val uri = android.net.Uri.parse(urlMatch)
            if (uri.getQueryParameter("teamCode") != null && (uri.getQueryParameter("token") != null || uri.getQueryParameter("code") != null)) {
                return urlMatch
            }
            val param = uri.getQueryParameter("teamCode")
                ?: uri.getQueryParameter("code")
                ?: uri.getQueryParameter("token")
                ?: uri.getQueryParameter("invite")
            if (!param.isNullOrBlank()) return param.replace("^#+".toRegex(), "").trim()

            val lastSeg = uri.lastPathSegment?.trim()
            if (!lastSeg.isNullOrBlank() && !lastSeg.equals("join", ignoreCase = true)) {
                return lastSeg.replace("^#+".toRegex(), "").trim()
            }
        } catch (_: Exception) {}
        return urlMatch
    }

    if (trimmed.contains("teamCode", ignoreCase = true) && (trimmed.contains("token", ignoreCase = true) || trimmed.contains("code", ignoreCase = true))) {
        return trimmed
    }

    // 1. Try query parameters like ?teamCode=... or ?code=... anywhere in text
    val queryRegex = Regex("(?:teamCode|code|token|invite)=([^&\\s#\"']+)", RegexOption.IGNORE_CASE)
    val queryMatch = queryRegex.find(trimmed)?.groupValues?.get(1)?.trim()
    if (!queryMatch.isNullOrBlank()) {
        return queryMatch.replace("^#+".toRegex(), "").trim()
    }

    // 3. Look for standard Workplace Code format (e.g. BWP-1234-567)
    val bwpRegex = Regex("\\b(BWP-[0-9]{4}-[0-9]{3})\\b", RegexOption.IGNORE_CASE)
    val bwpMatch = bwpRegex.find(trimmed)?.groupValues?.get(1)
    if (!bwpMatch.isNullOrBlank()) return bwpMatch.uppercase()

    // 4. Look for Team Code format (e.g. TM123456 or #TM123456)
    val tmRegex = Regex("#?([A-Za-z]{2}[0-9A-Za-z]{5,8})\\b")
    val tmMatch = tmRegex.find(trimmed)?.groupValues?.get(1)
    if (!tmMatch.isNullOrBlank()) return tmMatch.uppercase()

    // 5. Default cleanup: remove quotes, leading '#'
    return trimmed.replace("^#+".toRegex(), "").replace("^[\"']|[\"']$".toRegex(), "").trim()
}

enum class OnboardingStep {
    WELCOME_CHOOSER,
    TEAM_DETAILS,
    CREATE_WORKPLACE,
    REQUEST_PENDING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    repository: BizoraRepository,
    initialStep: OnboardingStep = OnboardingStep.WELCOME_CHOOSER,
    initialInviteCode: String? = null,
    onBack: (() -> Unit)? = null,
    onWorkplaceCreated: (Workplace) -> Unit,
    onWorkplaceJoined: (Workplace) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by repository.currentUser.collectAsState()
    val workplaces by repository.workplaces.collectAsState()
    val activeWorkplace by repository.activeWorkplace.collectAsState()
    val myJoinRequests by repository.myJoinRequests.collectAsState()

    val hasExistingWorkplace = activeWorkplace != null || workplaces.isNotEmpty()

    val effectiveInitialStep = remember(initialInviteCode, initialStep) {
        if (!initialInviteCode.isNullOrBlank()) OnboardingStep.TEAM_DETAILS else initialStep
    }
    var currentStep by remember { mutableStateOf(effectiveInitialStep) }
    var showJoinModal by remember { mutableStateOf(false) }
    var showHelpModal by remember { mutableStateOf(false) }
    var showJoinRequestsModal by remember { mutableStateOf(false) }
    var isScanningQr by remember { mutableStateOf(false) }
    var isSubmittingJoinCode by remember { mutableStateOf(false) }

    var teamPreviewData by remember { mutableStateOf<JoinPreviewResponse?>(null) }
    var isLoadingPreview by remember { mutableStateOf(!initialInviteCode.isNullOrBlank()) }
    var previewErrorMessage by remember { mutableStateOf<String?>(null) }
    var activeInviteCode by remember { mutableStateOf(initialInviteCode?.let { extractJoinCode(it) }) }

    val appIconBitmap = remember(context) {
        try {
            val drawable = context.packageManager.getApplicationIcon(context.packageName)
            val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
            val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
            val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            drawable.setBounds(0, 0, w, h)
            drawable.draw(canvas)
            bmp.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    BackHandler {
        if (currentStep == OnboardingStep.CREATE_WORKPLACE) {
            if (onBack != null) {
                onBack()
                return@BackHandler
            }
            if (initialStep == OnboardingStep.CREATE_WORKPLACE && hasExistingWorkplace) {
                val wp = activeWorkplace ?: workplaces.firstOrNull()
                if (wp != null) {
                    repository.confirmWorkplace(wp)
                    if (wp.role == WorkplaceRole.EMPLOYER) {
                        onWorkplaceCreated(wp)
                    } else {
                        onWorkplaceJoined(wp)
                    }
                    return@BackHandler
                }
            }
            currentStep = OnboardingStep.WELCOME_CHOOSER
        } else if (currentStep == OnboardingStep.TEAM_DETAILS) {
            repository.clearPendingInviteCode()
            currentStep = OnboardingStep.WELCOME_CHOOSER
        } else if (currentStep != OnboardingStep.WELCOME_CHOOSER) {
            currentStep = OnboardingStep.WELCOME_CHOOSER
        } else if (hasExistingWorkplace) {
            val wp = activeWorkplace ?: workplaces.firstOrNull()
            if (wp != null) {
                repository.confirmWorkplace(wp)
                if (wp.role == WorkplaceRole.EMPLOYER) {
                    onWorkplaceCreated(wp)
                } else {
                    onWorkplaceJoined(wp)
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isScanningQr = true
        } else {
            Toast.makeText(context, "Camera permission is required to scan team QR code", Toast.LENGTH_SHORT).show()
        }
    }

    fun startQrScan() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            isScanningQr = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Create workplace form
    var wpName by remember { mutableStateOf("") }
    var wpAddress by remember { mutableStateOf("") }
    var isCreatingWorkplace by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }
    var createdWorkplace by remember { mutableStateOf<Workplace?>(null) }

    // Map & Location state
    var selectedLatitude by remember { mutableStateOf<Double?>(12.9756) }
    var selectedLongitude by remember { mutableStateOf<Double?>(77.6066) }
    var selectedAddressText by remember { mutableStateOf("MG Road, Bengaluru, Karnataka") }
    var addressSearchQuery by remember { mutableStateOf("") }
    var isLocating by remember { mutableStateOf(false) }
    var isGeocoding by remember { mutableStateOf(false) }
    var mapWebView by remember { mutableStateOf<android.webkit.WebView?>(null) }
    val focusRequesterAddress = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            coroutineScope.launch {
                isLocating = true
                val coords = LocationHelper.getCurrentLocation(context)
                isLocating = false
                if (coords != null) {
                    selectedLatitude = coords.latitude
                    selectedLongitude = coords.longitude
                    mapWebView?.evaluateJavascript("if (window.updatePosition) { window.updatePosition(${coords.latitude}, ${coords.longitude}); }", null)
                    isGeocoding = true
                    val addr = LocationHelper.getAddressFromCoordinates(context, coords.latitude, coords.longitude)
                    isGeocoding = false
                    if (!addr.isNullOrBlank()) {
                        selectedAddressText = addr
                        addressSearchQuery = addr
                        wpAddress = addr
                    }
                }
            }
        } else {
            Toast.makeText(context, "Location permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    fun fetchCurrentLocation() {
        if (!LocationHelper.hasLocationPermission(context)) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }
        coroutineScope.launch {
            isLocating = true
            val coords = LocationHelper.getCurrentLocation(context)
            isLocating = false
            if (coords != null) {
                selectedLatitude = coords.latitude
                selectedLongitude = coords.longitude
                mapWebView?.evaluateJavascript("if (window.updatePosition) { window.updatePosition(${coords.latitude}, ${coords.longitude}); }", null)
                isGeocoding = true
                val addr = LocationHelper.getAddressFromCoordinates(context, coords.latitude, coords.longitude)
                isGeocoding = false
                if (!addr.isNullOrBlank()) {
                    selectedAddressText = addr
                    addressSearchQuery = addr
                    wpAddress = addr
                }
            } else {
                Toast.makeText(context, "Could not determine location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun searchLocation(query: String) {
        if (query.isBlank()) return
        coroutineScope.launch {
            isGeocoding = true
            val coords = LocationHelper.getCoordinatesFromAddress(context, query)
            isGeocoding = false
            if (coords != null) {
                selectedLatitude = coords.latitude
                selectedLongitude = coords.longitude
                mapWebView?.evaluateJavascript("if (window.updatePosition) { window.updatePosition(${coords.latitude}, ${coords.longitude}); }", null)
                val addr = LocationHelper.getAddressFromCoordinates(context, coords.latitude, coords.longitude) ?: query
                selectedAddressText = addr
                wpAddress = addr
            } else {
                Toast.makeText(context, "Address not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Pending request state
    val pendingRequest = remember(myJoinRequests) {
        myJoinRequests.find { it.status.equals("PENDING", ignoreCase = true) }
    }
    var isCheckingStatus by remember { mutableStateOf(false) }
    var isCancellingRequest by remember { mutableStateOf(false) }

    // If a pending join request exists, automatically show the pending status step
    LaunchedEffect(pendingRequest) {
        if (pendingRequest != null && currentStep == OnboardingStep.WELCOME_CHOOSER && initialInviteCode.isNullOrBlank()) {
            currentStep = OnboardingStep.REQUEST_PENDING
        }
    }

    // Auto-poll while waiting on REQUEST_PENDING step so employer approval is immediately detected
    LaunchedEffect(currentStep, pendingRequest?.id) {
        if (currentStep == OnboardingStep.REQUEST_PENDING && pendingRequest != null) {
            while (true) {
                kotlinx.coroutines.delay(4000)
                val reqsRes = repository.fetchMyJoinRequests()
                val reqs = reqsRes.getOrNull()
                val cur = reqs?.find { it.id == pendingRequest.id }
                if (cur == null || cur.status.equals("APPROVED", ignoreCase = true)) {
                    repository.refreshWorkplacesListAwait()
                    repository.syncUserData()
                    val joinedWp = repository.workplaces.value.find { it.id == pendingRequest.workplaceId }
                        ?: repository.workplaces.value.firstOrNull()
                        ?: repository.activeWorkplace.value
                    if (joinedWp != null) {
                        Toast.makeText(context, "Your request was approved! Welcome!", Toast.LENGTH_LONG).show()
                        repository.confirmWorkplace(joinedWp)
                        onWorkplaceJoined(joinedWp)
                        break
                    }
                } else if (cur.status.equals("REJECTED", ignoreCase = true)) {
                    Toast.makeText(context, "Your request was declined: ${cur.rejectionReason ?: "By manager"}", Toast.LENGTH_LONG).show()
                    currentStep = OnboardingStep.WELCOME_CHOOSER
                    break
                }
            }
        }
    }

    fun processJoinCode(rawInput: String) {
        val code = extractJoinCode(rawInput)
        if (code.isBlank()) {
            Toast.makeText(context, "Please enter or scan a valid team code", Toast.LENGTH_SHORT).show()
            return
        }

        coroutineScope.launch {
            isSubmittingJoinCode = true
            try {
                val res = repository.submitJoinRequest(code)
                if (res.isSuccess) {
                    val data = res.getOrNull()
                    val status = data?.get("status") as? String
                    val autoApproved = (data?.get("autoApproved") as? Boolean) == true ||
                            status.equals("APPROVED", ignoreCase = true) ||
                            (data?.get("alreadyMember") as? Boolean) == true
                    val wpName = (data?.get("workplaceName") as? String) ?: "the team"
                    val wpId = data?.get("workplaceId") as? String

                    if (autoApproved) {
                        Toast.makeText(context, "Joined $wpName successfully!", Toast.LENGTH_LONG).show()
                        repository.refreshWorkplacesListAwait()
                        repository.syncUserData()
                        val joinedWp = repository.workplaces.value.find { it.id == wpId }
                            ?: repository.workplaces.value.firstOrNull()
                            ?: repository.activeWorkplace.value
                        if (joinedWp != null) {
                            repository.confirmWorkplace(joinedWp)
                            onWorkplaceJoined(joinedWp)
                        } else {
                            currentStep = OnboardingStep.WELCOME_CHOOSER
                        }
                    } else {
                        Toast.makeText(context, "Join request submitted! Awaiting manager approval.", Toast.LENGTH_LONG).show()
                        currentStep = OnboardingStep.REQUEST_PENDING
                        repository.fetchMyJoinRequests()
                    }
                } else {
                    val err = res.exceptionOrNull()?.message ?: "Unable to join team"
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                }
            } finally {
                isSubmittingJoinCode = false
            }
        }
    }

    fun confirmJoinTeam(code: String) {
        val cleanCode = extractJoinCode(code)
        if (cleanCode.isBlank()) {
            Toast.makeText(context, "Please provide a valid team code", Toast.LENGTH_SHORT).show()
            return
        }

        coroutineScope.launch {
            isSubmittingJoinCode = true
            try {
                val res = repository.submitJoinRequest(cleanCode)
                if (res.isSuccess) {
                    val data = res.getOrNull()
                    val status = data?.get("status") as? String
                    val autoApproved = (data?.get("autoApproved") as? Boolean) == true ||
                            status.equals("APPROVED", ignoreCase = true) ||
                            (data?.get("alreadyMember") as? Boolean) == true
                    val wpName = (data?.get("workplaceName") as? String)
                        ?: teamPreviewData?.workplaceName
                        ?: "the team"
                    val wpId = (data?.get("workplaceId") as? String) ?: teamPreviewData?.workplaceId

                    repository.clearPendingInviteCode()

                    if (autoApproved) {
                        Toast.makeText(context, "Joined $wpName successfully!", Toast.LENGTH_LONG).show()
                        repository.refreshWorkplacesListAwait()
                        repository.syncUserData()
                        val joinedWp = repository.workplaces.value.find { it.id == wpId }
                            ?: repository.workplaces.value.firstOrNull()
                            ?: repository.activeWorkplace.value
                        if (joinedWp != null) {
                            repository.confirmWorkplace(joinedWp)
                            onWorkplaceJoined(joinedWp)
                        } else {
                            currentStep = OnboardingStep.WELCOME_CHOOSER
                        }
                    } else {
                        Toast.makeText(context, "Join request submitted! Awaiting manager approval.", Toast.LENGTH_LONG).show()
                        currentStep = OnboardingStep.REQUEST_PENDING
                        repository.fetchMyJoinRequests()
                    }
                } else {
                    val err = res.exceptionOrNull()?.message ?: "Unable to join team"
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, e.localizedMessage ?: "Error joining team", Toast.LENGTH_LONG).show()
            } finally {
                isSubmittingJoinCode = false
            }
        }
    }

    fun loadTeamPreview(rawCode: String) {
        val cleanCode = extractJoinCode(rawCode)
        if (cleanCode.isBlank()) return
        activeInviteCode = cleanCode
        currentStep = OnboardingStep.TEAM_DETAILS
        isLoadingPreview = true
        previewErrorMessage = null
        coroutineScope.launch {
            try {
                val res = repository.previewJoin(cleanCode)
                if (res.isSuccess) {
                    teamPreviewData = res.getOrNull()
                    previewErrorMessage = null
                } else {
                    val err = res.exceptionOrNull()?.message ?: "Could not find workplace or team"
                    previewErrorMessage = err
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "Failed to load team preview"
                previewErrorMessage = err
                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            } finally {
                isLoadingPreview = false
            }
        }
    }

    LaunchedEffect(initialInviteCode) {
        if (!initialInviteCode.isNullOrBlank()) {
            loadTeamPreview(initialInviteCode)
        }
    }

    if (isScanningQr) {
        CameraQrScanner(
            onQrScanned = { rawQr ->
                isScanningQr = false
                loadTeamPreview(rawQr)
            },
            onClose = { isScanningQr = false }
        )
        return
    }


    LaunchedEffect(Unit) {
        if (!repository.isAuthenticated()) {
            onSignOut()
            return@LaunchedEffect
        }
        repository.fetchMyJoinRequests()
        repository.syncUserData()
    }

    val displayName = remember(currentUser) {
        val directName = currentUser?.name?.trim()?.takeIf { it.isNotBlank() }
        if (directName != null) return@remember directName

        val cachedName = repository.getCachedUser()?.name?.trim()?.takeIf { it.isNotBlank() }
        if (cachedName != null) return@remember cachedName

        val email = currentUser?.email?.trim()?.takeIf { it.isNotBlank() }
            ?: repository.getCachedUser()?.email?.trim()?.takeIf { it.isNotBlank() }
        if (email != null) {
            val localPart = email.substringBefore("@")
            val formatted = localPart
                .replace(".", " ")
                .replace("_", " ")
                .replace("-", " ")
                .split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
            if (formatted.isNotBlank()) return@remember formatted
        }

        "User"
    }

    val firstName = remember(displayName) {
        if (displayName.equals("User", ignoreCase = true)) {
            ""
        } else {
            displayName.split(" ").firstOrNull { it.isNotBlank() } ?: displayName
        }
    }

    val brandGreen = Color(0xFF5B692D)
    val brandLightGreen = Color(0xFFF1F5E8)
    val textPrimary = Color(0xFF17202A)
    val textSecondary = Color(0xFF64748B)

    createdWorkplace?.let { workplace ->
        WorkplaceCreatedScreen(workplace = workplace, onContinue = { onWorkplaceCreated(workplace) })
        return
    }

    Scaffold(
        containerColor = Color(0xFFFAFAF7),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep == OnboardingStep.CREATE_WORKPLACE || currentStep == OnboardingStep.TEAM_DETAILS) {
                    // Back button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF1E9))
                            .clickable {
                                if (currentStep == OnboardingStep.TEAM_DETAILS) {
                                    repository.clearPendingInviteCode()
                                    currentStep = OnboardingStep.WELCOME_CHOOSER
                                } else if (onBack != null) {
                                    onBack()
                                } else if (initialStep == OnboardingStep.CREATE_WORKPLACE && hasExistingWorkplace) {
                                    val wp = activeWorkplace ?: workplaces.firstOrNull()
                                    if (wp != null) {
                                        repository.confirmWorkplace(wp)
                                        if (wp.role == WorkplaceRole.EMPLOYER) {
                                            onWorkplaceCreated(wp)
                                        } else {
                                            onWorkplaceJoined(wp)
                                        }
                                    } else {
                                        currentStep = OnboardingStep.WELCOME_CHOOSER
                                    }
                                } else {
                                    currentStep = OnboardingStep.WELCOME_CHOOSER
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1E281C),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Bizora Brand Center
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (appIconBitmap != null) {
                            Image(
                                bitmap = appIconBitmap,
                                contentDescription = "Bizora Logo",
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(7.dp))
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.splash_icon),
                                contentDescription = "Bizora Logo",
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(7.dp))
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bizora",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E281C),
                            letterSpacing = (-0.5).sp
                        )
                    }

                    // Help button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.2.dp, Color(0xFFD6DBCE), CircleShape)
                            .clickable { showHelpModal = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = "Help",
                            tint = Color(0xFF1E281C),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasExistingWorkplace && currentStep != OnboardingStep.WELCOME_CHOOSER) {
                            IconButton(
                                onClick = {
                                    val wp = activeWorkplace ?: workplaces.firstOrNull()
                                    if (wp != null) {
                                        repository.confirmWorkplace(wp)
                                        if (wp.role == WorkplaceRole.EMPLOYER) {
                                            onWorkplaceCreated(wp)
                                        } else {
                                            onWorkplaceJoined(wp)
                                        }
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Workplace",
                                    tint = textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        // App Icon Logo
                        if (appIconBitmap != null) {
                            Image(
                                bitmap = appIconBitmap,
                                contentDescription = "Bizora Logo",
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.splash_icon),
                                contentDescription = "Bizora Logo",
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Bizora",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                    }

                    // Help Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showHelpModal = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = "Help",
                            tint = Color(0xFF111827),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Help",
                            color = Color(0xFF111827),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp
                        )
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AdBanner()
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentStep) {
                OnboardingStep.WELCOME_CHOOSER -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Hero Artwork
                        Image(
                            painter = painterResource(id = R.drawable.workspace_hero_illustration),
                            contentDescription = "Workspace Hero",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. Greeting & Titles
                        Text(
                            text = if (firstName.isNotBlank()) "Welcome, $firstName" else "Welcome",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF555B4E)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Your next chapter\nstarts here.",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827),
                            textAlign = TextAlign.Center,
                            lineHeight = 34.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Create your workspace or join your team.",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        )

                        if (myJoinRequests.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                onClick = { showJoinRequestsModal = true },
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFF1F5E8),
                                border = BorderStroke(1.dp, Color(0xFFDEE5CE))
                            ) {
                                Text(
                                    text = "My join requests (${myJoinRequests.size})",
                                    color = Color(0xFF36451C),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 3. Option Card 1: Create a workplace
                        Surface(
                            onClick = { currentStep = OnboardingStep.CREATE_WORKPLACE },
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFEFF4E9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_onboarding_storefront),
                                        contentDescription = "Storefront",
                                        tint = Color(0xFF2C431A),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Create a workplace",
                                        fontSize = 17.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF111827)
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "For business owners and employers",
                                        fontSize = 13.5.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF384922)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Navigate",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4. Option Card 2: Join a workplace
                        Surface(
                            onClick = { showJoinModal = true },
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFEFF4E9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_onboarding_team),
                                        contentDescription = "Team",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Join a workplace",
                                        fontSize = 17.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF111827)
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "For employees with an invite",
                                        fontSize = 13.5.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF384922)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Navigate",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 5. Quick Join Row (Scan QR & Code or link)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Scan QR button
                            Surface(
                                onClick = { startQrScan() },
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_onboarding_qr),
                                        contentDescription = "Scan QR",
                                        tint = Color(0xFF111827),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Scan QR",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF111827)
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Navigate",
                                        tint = Color(0xFF111827),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Code or link button
                            Surface(
                                onClick = { showJoinModal = true },
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_onboarding_link),
                                        contentDescription = "Code or link",
                                        tint = Color(0xFF111827),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Code or link",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF111827)
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Navigate",
                                        tint = Color(0xFF111827),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 6. Review notice
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Info",
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Join requests are reviewed by your employer.",
                                fontSize = 12.5.sp,
                                color = Color(0xFF6B7280)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        // 7. Google avatar and user name directly / Sign out row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val userAvatarUrl = currentUser?.avatarUrl?.takeIf { it.isNotBlank() }
                                ?: repository.getCachedUser()?.avatarUrl?.takeIf { it.isNotBlank() }

                            UserAvatar(
                                name = displayName,
                                avatarUrl = userAvatarUrl,
                                size = 38.dp,
                                fontSize = 15.sp
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = displayName,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF111827),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = "Sign out",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF111827),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onSignOut() }
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                OnboardingStep.TEAM_DETAILS -> {
                    TeamDetailsScreenView(
                        preview = teamPreviewData,
                        isLoading = isLoadingPreview,
                        errorMessage = previewErrorMessage,
                        activeInviteCode = activeInviteCode,
                        displayName = displayName,
                        currentUserAvatarUrl = currentUser?.avatarUrl ?: repository.getCachedUser()?.avatarUrl,
                        currentUserEmail = currentUser?.email ?: repository.getCachedUser()?.email,
                        isSubmittingJoinCode = isSubmittingJoinCode,
                        brandGreen = brandGreen,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onRetry = {
                            activeInviteCode?.let { loadTeamPreview(it) }
                        },
                        onJoinDirectly = {
                            confirmJoinTeam(activeInviteCode ?: teamPreviewData?.targetTeam?.teamCode ?: teamPreviewData?.workplaceCode ?: "")
                        },
                        onGoToDashboard = {
                            repository.clearPendingInviteCode()
                            val wpId = teamPreviewData?.workplaceId
                            val existing = repository.workplaces.value.find { it.id == wpId } ?: repository.activeWorkplace.value
                            if (existing != null) {
                                repository.confirmWorkplace(existing)
                                onWorkplaceJoined(existing)
                            } else {
                                coroutineScope.launch {
                                    repository.refreshWorkplacesListAwait()
                                    repository.syncUserData()
                                    val wp = repository.workplaces.value.find { it.id == wpId } ?: repository.activeWorkplace.value
                                    if (wp != null) {
                                        repository.confirmWorkplace(wp)
                                        onWorkplaceJoined(wp)
                                    }
                                }
                            }
                        },
                        onBackToOptions = {
                            repository.clearPendingInviteCode()
                            currentStep = OnboardingStep.WELCOME_CHOOSER
                        }
                    )
                }

                OnboardingStep.CREATE_WORKPLACE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        // Header: Rounded square storefront icon + title & subtitle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFEFF5E4)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_onboarding_storefront),
                                    contentDescription = null,
                                    tint = Color(0xFF36451C),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Create your workplace",
                                    fontSize = 23.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF172013),
                                    letterSpacing = (-0.3).sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Name your business and choose its location.",
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Workplace name *
                        Row {
                            Text(
                                text = "Workplace name ",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1F2937)
                            )
                            Text(
                                text = "*",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFD6DBCE), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_onboarding_storefront),
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(Color(0xFFE5E7EB))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            BasicTextField(
                                value = wpName,
                                onValueChange = {
                                    wpName = it
                                    createError = null
                                },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    color = Color(0xFF1F2937),
                                    fontWeight = FontWeight.Normal
                                ),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { innerTextField ->
                                    if (wpName.isEmpty()) {
                                        Text(
                                            text = "Sharma General Store",
                                            fontSize = 15.sp,
                                            color = Color(0xFF9CA3AF)
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Workplace address (Optional)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Workplace address",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1F2937)
                            )
                            Text(
                                text = "Optional",
                                fontSize = 13.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFD6DBCE), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(Color(0xFFE5E7EB))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            BasicTextField(
                                value = addressSearchQuery,
                                onValueChange = {
                                    addressSearchQuery = it
                                    wpAddress = it
                                },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    color = Color(0xFF1F2937),
                                    fontWeight = FontWeight.Normal
                                ),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Search
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        searchLocation(addressSearchQuery)
                                        keyboardController?.hide()
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequesterAddress),
                                decorationBox = { innerTextField ->
                                    if (addressSearchQuery.isEmpty()) {
                                        Text(
                                            text = "MG Road, Bengaluru",
                                            fontSize = 15.sp,
                                            color = Color(0xFF9CA3AF)
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            if (addressSearchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        addressSearchQuery = ""
                                        wpAddress = ""
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color(0xFF6B7280),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Location actions: Use current location | Enter manually
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { fetchCurrentLocation() }
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = Color(0xFF4B5927),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Use current location",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4B5927)
                                )
                            }
                            Text(
                                text = "Enter manually",
                                fontSize = 13.sp,
                                color = Color(0xFF4B5927),
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { focusRequesterAddress.requestFocus() }
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Google Map Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    android.webkit.WebView(ctx).apply {
                                        mapWebView = this
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = false
                                        settings.allowFileAccess = true
                                        settings.allowContentAccess = false
                                        settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                                        android.util.Log.i("MapWebView", "Initializing WebView for map")
                                        webChromeClient = object : android.webkit.WebChromeClient() {
                                            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                                android.util.Log.i("MapWebView", "Console: ${consoleMessage?.message()} [line ${consoleMessage?.lineNumber()}]")
                                                return true
                                            }
                                        }
                                        webViewClient = object : android.webkit.WebViewClient() {
                                            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                                android.util.Log.i("MapWebView", "Page finished loading: $url")
                                                view?.evaluateJavascript("typeof L") { result ->
                                                    android.util.Log.i("MapWebView", "typeof L: $result")
                                                }
                                            }
                                            override fun onReceivedError(view: android.webkit.WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                                                android.util.Log.e("MapWebView", "Error ($errorCode): $description at $failingUrl")
                                            }
                                        }
                                        setOnTouchListener { v, event ->
                                            when (event.action) {
                                                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                                    v.parent?.requestDisallowInterceptTouchEvent(true)
                                                }
                                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                                    v.parent?.requestDisallowInterceptTouchEvent(false)
                                                }
                                            }
                                            false
                                        }
                                        addJavascriptInterface(
                                            object {
                                                @android.webkit.JavascriptInterface
                                                fun onMapMoved(lat: Double, lng: Double) {
                                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                        selectedLatitude = lat
                                                        selectedLongitude = lng
                                                        coroutineScope.launch {
                                                            val addr = LocationHelper.getAddressFromCoordinates(context, lat, lng)
                                                            if (!addr.isNullOrBlank()) {
                                                                selectedAddressText = addr
                                                                wpAddress = addr
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            "AndroidBridge"
                                        )
                                        layoutParams = android.view.ViewGroup.LayoutParams(
                                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                        val lat = selectedLatitude ?: 12.9756
                                        val lng = selectedLongitude ?: 77.6066
                                        android.util.Log.i("MapWebView", "Building map HTML for lat=$lat, lng=$lng")
                                        val html = """
                                            <!DOCTYPE html>
                                            <html>
                                            <head>
                                              <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                                              <link rel="stylesheet" href="leaflet/leaflet.css" />
                                              <script src="leaflet/leaflet.js"></script>
                                              <style>
                                                * { margin:0; padding:0; box-sizing:border-box; }
                                                html, body { width:100vw; height:100vh; overflow:hidden; background:#e5e3df; margin:0; padding:0; }
                                                #map { width:100vw; height:100vh; background:#e5e3df; position:absolute; top:0; left:0; }
                                                .leaflet-control-container { display:none !important; }
                                              </style>
                                            </head>
                                            <body>
                                              <div id="map"></div>
                                              <script>
                                                try {
                                                  console.log("Map container size before init: " + document.getElementById('map').clientWidth + "x" + document.getElementById('map').clientHeight);
                                                  var map = L.map('map', {
                                                    zoomControl: false,
                                                    attributionControl: false,
                                                    center: [$lat, $lng],
                                                    zoom: 16
                                                  });
                                                  var googleTiles = L.tileLayer('https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', {
                                                    maxZoom: 20
                                                  });
                                                  var osmTiles = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                                    maxZoom: 19
                                                  });
                                                  googleTiles.addTo(map);
                                                  googleTiles.on('load', function() {
                                                    console.log("Google tiles loaded successfully");
                                                  });
                                                  googleTiles.on('tileerror', function() {
                                                    console.warn("Google tile error, fallback to OSM");
                                                    if (!map.hasLayer(osmTiles)) {
                                                      osmTiles.addTo(map);
                                                    }
                                                  });
                                                  map.on('moveend', function() {
                                                    var c = map.getCenter();
                                                    if (window.AndroidBridge) {
                                                      window.AndroidBridge.onMapMoved(c.lat, c.lng);
                                                    }
                                                  });
                                                  window.updatePosition = function(lat, lng) {
                                                    map.setView([lat, lng], 16, { animate: true });
                                                  };
                                                  setTimeout(function() {
                                                    map.invalidateSize(true);
                                                    console.log("InvalidateSize 100ms done: " + document.getElementById('map').clientWidth + "x" + document.getElementById('map').clientHeight);
                                                  }, 100);
                                                  setTimeout(function() {
                                                    map.invalidateSize(true);
                                                    console.log("InvalidateSize 500ms done");
                                                  }, 500);
                                                  console.log("Leaflet map initialized successfully");
                                                } catch(err) {
                                                  console.error("Map init error: " + err.message);
                                                }
                                              </script>
                                            </body>
                                            </html>
                                        """.trimIndent()
                                        loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                onRelease = { webView ->
                                    mapWebView = null
                                    webView.stopLoading()
                                    webView.removeJavascriptInterface("AndroidBridge")
                                    webView.destroy()
                                }
                            )

                            // Centered Red Pin
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                MapPinMarker(modifier = Modifier.offset(y = (-18).dp))
                            }

                            // Floating Re-Center Button
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 26.dp, end = 12.dp),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    shadowElevation = 4.dp,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable { fetchCurrentLocation() }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isLocating) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = Color(0xFF4B5927)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.MyLocation,
                                                contentDescription = "My Location",
                                                tint = Color(0xFF374151),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Bottom Google watermark & attribution strip
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(Color(0xD9FFFFFF))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GoogleWatermark()
                                Text(
                                    text = "Map data ©2024 Google  Terms of Use",
                                    fontSize = 8.5.sp,
                                    color = Color(0xFF4B5563)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Selected address Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Selected address",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF6B7280)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = selectedAddressText.ifBlank { "MG Road, Bengaluru, Karnataka" },
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1F2937),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "Change",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4B5927),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { focusRequesterAddress.requestFocus() }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Footer hint row: Check the pin location before continuing. | Skip location for now
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Check the pin location before continuing.",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                            Text(
                                text = "Skip location for now",
                                fontSize = 12.sp,
                                color = Color(0xFF4B5927),
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier
                                    .clickable {
                                        selectedLatitude = null
                                        selectedLongitude = null
                                        Toast.makeText(context, "Location skipped", Toast.LENGTH_SHORT).show()
                                    }
                            )
                        }

                        if (!createError.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = createError!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Primary Button: Create workplace ->
                        Button(
                            onClick = {
                                if (wpName.trim().length < 2) {
                                    createError = "Please enter a workplace name with at least 2 characters."
                                    return@Button
                                }
                                coroutineScope.launch {
                                    isCreatingWorkplace = true
                                    createError = null
                                    val addrToSave = selectedAddressText.ifBlank { wpAddress.trim().ifBlank { null } }
                                    val res = repository.createWorkplace(
                                        name = wpName,
                                        address = addrToSave,
                                        latitude = selectedLatitude,
                                        longitude = selectedLongitude
                                    )
                                    isCreatingWorkplace = false
                                    if (res.isSuccess) {
                                        createdWorkplace = res.getOrThrow()
                                    } else {
                                        createError = res.exceptionOrNull()?.message ?: "Failed to create workplace"
                                    }
                                }
                            },
                            enabled = wpName.trim().length >= 2 && !isCreatingWorkplace,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF38461E),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFBAC4A4),
                                disabledContentColor = Color.White.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            if (isCreatingWorkplace) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Create workplace",
                                        fontSize = 16.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Invite your team after setup.",
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                OnboardingStep.REQUEST_PENDING -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Pending Clock",
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(46.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Request Pending",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Your request to join a team is being reviewed by the admin.",
                            fontSize = 14.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Pending Status Card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFD97706))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "AWAITING APPROVAL",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD97706),
                                        letterSpacing = 1.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = pendingRequest?.workplaceName ?: "Workplace",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )

                                if (!pendingRequest?.address.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = pendingRequest!!.address!!,
                                        fontSize = 13.sp,
                                        color = textSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Your request was submitted and is awaiting employer approval. Check back later or refresh below.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF475569),
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                isCheckingStatus = true
                                                val reqs = repository.fetchMyJoinRequests()
                                                repository.syncUserData()
                                                isCheckingStatus = false
                                                val updatedPending = reqs.getOrNull()?.find { it.id == pendingRequest?.id }
                                                if (updatedPending == null || updatedPending.status == "APPROVED") {
                                                    Toast.makeText(context, "Your request was approved! Welcome!", Toast.LENGTH_LONG).show()
                                                    val wp = repository.workplaces.value.find { it.id == pendingRequest?.workplaceId }
                                                    if (wp != null) {
                                                        onWorkplaceJoined(wp)
                                                    } else {
                                                        currentStep = OnboardingStep.WELCOME_CHOOSER
                                                    }
                                                } else if (updatedPending.status == "REJECTED") {
                                                    Toast.makeText(context, "Your request was declined: ${updatedPending.rejectionReason ?: "By manager"}", Toast.LENGTH_LONG).show()
                                                    currentStep = OnboardingStep.WELCOME_CHOOSER
                                                } else {
                                                    Toast.makeText(context, "Request is still awaiting approval.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        enabled = !isCheckingStatus,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = brandGreen,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (isCheckingStatus) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Refresh",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Check Status",
                                                fontSize = 13.sp,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            if (pendingRequest != null) {
                                                coroutineScope.launch {
                                                    isCancellingRequest = true
                                                    val res = repository.cancelMyJoinRequest(pendingRequest.realId)
                                                    isCancellingRequest = false
                                                    if (res.isSuccess) {
                                                        Toast.makeText(context, "Join request cancelled.", Toast.LENGTH_SHORT).show()
                                                        currentStep = OnboardingStep.WELCOME_CHOOSER
                                                    } else {
                                                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Failed to cancel", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isCancellingRequest,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64748B))
                                    ) {
                                        Text("Cancel", fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Info Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF1F8E9),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFDCEDC8), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = "Info",
                                    tint = Color(0xFF33691E),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "What happens next?",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF33691E)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Once the workplace admin approves your request, the app will grant you access automatically.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF558B2F),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        TextButton(
                            onClick = { currentStep = OnboardingStep.WELCOME_CHOOSER }
                        ) {
                            Text(
                                text = "Back to options",
                                color = brandGreen,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal 1: Join Team Bottom Sheet
    if (showJoinModal) {
        JoinTeamBottomSheet(
            onDismiss = { showJoinModal = false },
            onSubmitCode = { code ->
                showJoinModal = false
                loadTeamPreview(code)
            },
            onStartScan = {
                showJoinModal = false
                startQrScan()
            },
            onViewMyRequests = {
                showJoinModal = false
                showJoinRequestsModal = true
            }
        )
    }

    // Joining in-flight loading dialog
    if (isSubmittingJoinCode) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = null,
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = brandGreen,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Joining team...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Validating invitation & setting up access",
                            fontSize = 12.5.sp,
                            color = textSecondary
                        )
                    }
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal 2: Help Modal
    if (showHelpModal) {
        AlertDialog(
            onDismissRequest = { showHelpModal = false },
            confirmButton = {
                TextButton(onClick = { showHelpModal = false }) {
                    Text("Got it", color = brandGreen, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text("How Bizora Works", fontWeight = FontWeight.Bold, color = textPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "• Business Owners (Employers): Tap 'Create a workplace' to register your business, invite your team, and track daily attendance.",
                        fontSize = 14.sp,
                        color = textSecondary,
                        lineHeight = 19.sp
                    )
                    Text(
                        text = "• Staff & Employees: Tap 'Join a team' and enter the team code (e.g. TM960520), scan the team QR code, or paste the link shared by your employer to request access.",
                        fontSize = 14.sp,
                        color = textSecondary,
                        lineHeight = 19.sp
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal 3: My Join Requests Bottom Sheet
    if (showJoinRequestsModal) {
        ModalBottomSheet(
            onDismissRequest = { showJoinRequestsModal = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "My Join Requests",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                if (myJoinRequests.isEmpty()) {
                    Text(
                        text = "You have no active workplace join requests.",
                        color = textSecondary,
                        fontSize = 14.sp
                    )
                } else {
                    myJoinRequests.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.workplaceName ?: "Workplace",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Status: ${item.status}",
                                        fontSize = 12.sp,
                                        color = when (item.status) {
                                            "APPROVED" -> Color(0xFF16A34A)
                                            "REJECTED" -> Color(0xFFDC2626)
                                            else -> Color(0xFFD97706)
                                        },
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                if (item.status == "PENDING") {
                                    TextButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository.cancelMyJoinRequest(item.realId)
                                            }
                                        }
                                    ) {
                                        Text("Cancel", color = Color(0xFFDC2626), fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinTeamBottomSheet(
    onDismiss: () -> Unit,
    onSubmitCode: (String) -> Unit,
    onStartScan: () -> Unit,
    onViewMyRequests: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var inputCode by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(0) } // 0: Code, 1: Scan, 2: Link

    fun dismissSmoothly(action: (() -> Unit)? = null) {
        coroutineScope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onDismiss()
                action?.invoke()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Join a team",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF17202A)
                )
                IconButton(onClick = { dismissSmoothly() }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Text(
                text = "Enter your team code (e.g. TM960520), scan the team QR code, or paste the team invite link.",
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Method Selector Tabs: Enter Code | Scan QR | Paste Link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(4.dp)
            ) {
                listOf("Enter Code", "Scan QR", "Paste Link").forEachIndexed { index, label ->
                    val isSelected = selectedMethod == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .clickable {
                                selectedMethod = index
                                if (index == 1) {
                                    dismissSmoothly { onStartScan() }
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFF5B692D) else Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (selectedMethod == 1) {
                // QR Scanner Card
                Surface(
                    color = Color(0xFFF8F9FA),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dismissSmoothly { onStartScan() } }
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF5B692D).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = Color(0xFF5B692D),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Scan Team QR Code",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF17202A)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Point your camera at your employer's team QR code to request direct team access.",
                            fontSize = 12.5.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { dismissSmoothly { onStartScan() } },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF5B692D),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Scanner", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = Color.White)
                        }
                    }
                }
            } else {
                // Input
                OutlinedTextField(
                    value = inputCode,
                    onValueChange = {
                        inputCode = if (selectedMethod == 0) it.uppercase() else it
                    },
                    placeholder = {
                        Text(
                            if (selectedMethod == 0) "e.g. TM960520 or BWP-1234-567" else "https://... or invite link",
                            color = Color(0xFFA89F9A),
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF5B692D),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = if (selectedMethod == 0) KeyboardCapitalization.Characters else KeyboardCapitalization.None,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (inputCode.isNotBlank()) onSubmitCode(extractJoinCode(inputCode))
                        }
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (inputCode.isNotBlank()) onSubmitCode(extractJoinCode(inputCode))
                    },
                    enabled = inputCode.trim().length >= 4,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5B692D),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFBAC4A4),
                        disabledContentColor = Color.White.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Request to Join Team",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TextButton(
                onClick = onViewMyRequests,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "View my team join requests",
                    color = Color(0xFF5B692D),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun TeamDetailsScreenView(
    preview: JoinPreviewResponse?,
    isLoading: Boolean,
    errorMessage: String?,
    activeInviteCode: String?,
    displayName: String,
    currentUserAvatarUrl: String?,
    currentUserEmail: String?,
    isSubmittingJoinCode: Boolean,
    brandGreen: Color,
    textPrimary: Color,
    textSecondary: Color,
    onRetry: () -> Unit,
    onJoinDirectly: () -> Unit,
    onGoToDashboard: () -> Unit,
    onBackToOptions: () -> Unit
) {
    if (isLoading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5E8)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = brandGreen,
                    modifier = Modifier.size(38.dp),
                    strokeWidth = 3.5.dp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Loading Team Details",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Fetching organization and invitation information...",
                fontSize = 14.sp,
                color = textSecondary,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    if (errorMessage != null && preview == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEE2E2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Unable to Load Team",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                fontSize = 14.sp,
                color = textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))
            if (!activeInviteCode.isNullOrBlank()) {
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = brandGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(text = "Retry", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            OutlinedButton(
                onClick = onBackToOptions,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Go to Workplace Options",
                    color = brandGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
        return
    }

    if (preview == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = brandGreen)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // 1. Group Icon Badge
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(brandGreen.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = brandGreen,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Team Name & Workplace
        val teamName = preview.targetTeam?.name ?: preview.workplaceName
        Text(
            text = teamName,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            textAlign = TextAlign.Center
        )

        if (preview.targetTeam != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "at ${preview.workplaceName}",
                fontSize = 15.sp,
                color = textSecondary,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Code & Member Count Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val displayCode = preview.targetTeam?.teamCode ?: preview.workplaceCode
            if (!displayCode.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5E8))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "#$displayCode",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = brandGreen
                    )
                }
            }

            val members = preview.activeMembersCount ?: preview.memberCount ?: 0
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "$members member${if (members == 1) "" else "s"}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Workplace Details Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!preview.ownerName.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Manager / Admin",
                                fontSize = 11.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = preview.ownerName,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }

                if (!preview.address.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Workplace Location",
                                fontSize = 11.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = preview.address,
                                fontSize = 13.5.sp,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Direct Join Verified",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF16A34A)
                        )
                        Text(
                            text = "You can mark attendance immediately after joining",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. "You're joining as" User Info Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    name = displayName,
                    size = 44.dp,
                    avatarUrl = currentUserAvatarUrl
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "You'll join as",
                        fontSize = 11.5.sp,
                        color = textSecondary
                    )
                    Text(
                        text = displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    if (!currentUserEmail.isNullOrBlank()) {
                        Text(
                            text = currentUserEmail,
                            fontSize = 12.sp,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 6. Action Button
        if (preview.alreadyMember) {
            Button(
                onClick = onGoToDashboard,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = brandGreen,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = "Go to Attendance Dashboard",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Button(
                onClick = onJoinDirectly,
                enabled = !isSubmittingJoinCode,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = brandGreen,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                if (isSubmittingJoinCode) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Joining...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Join Team Directly",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = onBackToOptions,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Not this team? Browse other options",
                color = textSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MapPinMarker(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(32.dp, 40.dp)) {
            val w = size.width
            val h = size.height * 0.88f
            val path = Path().apply {
                moveTo(w / 2f, h)
                cubicTo(w * 0.12f, h * 0.65f, 0f, h * 0.45f, 0f, h * 0.35f)
                cubicTo(0f, h * 0.15f, w * 0.22f, 0f, w / 2f, 0f)
                cubicTo(w * 0.78f, 0f, w, h * 0.15f, w, h * 0.35f)
                cubicTo(w, h * 0.45f, w * 0.88f, h * 0.65f, w / 2f, h)
                close()
            }
            drawPath(path, color = Color(0xFFEA4335))

            drawCircle(
                color = Color(0xFF8E1B10),
                radius = w * 0.16f,
                center = Offset(w / 2f, h * 0.35f)
            )
        }
        Canvas(modifier = Modifier.size(14.dp, 4.dp)) {
            drawOval(color = Color(0x33000000))
        }
    }
}

@Composable
private fun GoogleWatermark(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Color(0xFF4285F4), fontWeight = FontWeight.Bold)) { append("G") }
                withStyle(SpanStyle(color = Color(0xFFEA4335), fontWeight = FontWeight.Bold)) { append("o") }
                withStyle(SpanStyle(color = Color(0xFFFBBC05), fontWeight = FontWeight.Bold)) { append("o") }
                withStyle(SpanStyle(color = Color(0xFF4285F4), fontWeight = FontWeight.Bold)) { append("g") }
                withStyle(SpanStyle(color = Color(0xFF34A853), fontWeight = FontWeight.Bold)) { append("l") }
                withStyle(SpanStyle(color = Color(0xFFEA4335), fontWeight = FontWeight.Bold)) { append("e") }
            },
            fontSize = 12.sp,
            letterSpacing = (-0.4).sp
        )
    }
}
