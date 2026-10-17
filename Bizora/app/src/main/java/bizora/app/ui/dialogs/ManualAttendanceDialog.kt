package bizora.app.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bizora.app.data.model.AttendanceStatus
import bizora.app.data.model.RosterItem
import bizora.app.ui.components.ModalCloseButton
import bizora.app.ui.components.StatusChip
import bizora.app.ui.theme.LocalBizoraColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private data class StatusUiConfig(
    val label: String,
    val icon: ImageVector,
    val activeBg: Color,
    val activeBorder: Color,
    val activeTint: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualAttendanceDialog(
    item: RosterItem? = null,
    allEmployees: List<RosterItem> = emptyList(),
    onDismiss: () -> Unit,
    onSave: suspend (memberId: String, status: AttendanceStatus, reason: String) -> Result<Unit>
) {
    val colors = LocalBizoraColors.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    val effectiveEmployees = remember(item, allEmployees) {
        if (allEmployees.isNotEmpty()) allEmployees
        else if (item != null) listOf(item)
        else emptyList()
    }

    var selectedEmployee by remember(item, effectiveEmployees) {
        mutableStateOf(item ?: effectiveEmployees.firstOrNull())
    }
    var selectedStatus by remember {
        mutableStateOf(item?.status ?: AttendanceStatus.PRESENT)
    }
    var reason by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val todayFormatted = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))
    }

    val statusOptions = listOf(
        AttendanceStatus.PRESENT,
        AttendanceStatus.ABSENT,
        AttendanceStatus.HALF_DAY,
        AttendanceStatus.LEAVE
    )

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
            // Header Row (Icon + Title + Subtitle + Close button)
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
                        Icon(
                            Icons.Default.PersonSearch,
                            contentDescription = null,
                            tint = colors.brandPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Manual Attendance",
                            fontSize = 18.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Record or adjust attendance entry",
                            fontSize = 14.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                ModalCloseButton(
                    onClick = { dismissSmoothly() }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Employee Selector (Horizontal scroll if multiple)
            Text(
                text = "Select Employee *",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (effectiveEmployees.isEmpty()) {
                Surface(
                    color = colors.surfaceMuted,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "No employees are available yet. Add or invite an employee before recording attendance.",
                            color = colors.textSecondary,
                            fontSize = 13.5.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(effectiveEmployees, key = { it.memberId }) { emp ->
                    val isSelected = selectedEmployee?.memberId == emp.memberId
                    Surface(
                        color = if (isSelected) colors.brandTint else colors.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) colors.brandPrimary else colors.border
                        ),
                        modifier = Modifier.clickable { selectedEmployee = emp }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) colors.brandPrimary else colors.surfaceMuted),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emp.name.take(2).uppercase(),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) colors.textInverse else colors.textPrimary
                                )
                            }

                            Column {
                                Text(
                                    text = emp.name,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) colors.brandPrimary else colors.textPrimary
                                )
                                Text(
                                    text = emp.employeeCode,
                                    fontSize = 12.5.sp,
                                    color = colors.textSecondary
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = colors.brandPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Date Display
            Text(
                text = "Date *",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                color = colors.surfaceMuted,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = colors.brandPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = todayFormatted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Status 4 Pills
            Text(
                text = "Status *",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statusOptions.forEach { status ->
                    val isSelected = selectedStatus == status
                    val cfg = when (status) {
                        AttendanceStatus.PRESENT -> StatusUiConfig(
                            label = "Present",
                            icon = Icons.Default.CheckCircle,
                            activeBg = if (colors.isDark) Color(0xFF1E2F16) else Color(0xFFECF2DE),
                            activeBorder = if (colors.isDark) Color(0xFF385226) else Color(0xFF40501E),
                            activeTint = if (colors.isDark) Color(0xFFA7D980) else Color(0xFF40501E)
                        )
                        AttendanceStatus.ABSENT -> StatusUiConfig(
                            label = "Absent",
                            icon = Icons.Default.Cancel,
                            activeBg = if (colors.isDark) Color(0xFF381A1A) else Color(0xFFFDF0ED),
                            activeBorder = if (colors.isDark) Color(0xFF5E2727) else Color(0xFFDC2626),
                            activeTint = if (colors.isDark) Color(0xFFF87171) else Color(0xFFDC2626)
                        )
                        AttendanceStatus.HALF_DAY -> StatusUiConfig(
                            label = "Half day",
                            icon = Icons.Default.Schedule,
                            activeBg = if (colors.isDark) Color(0xFF2B1D42) else Color(0xFFF2EAFD),
                            activeBorder = if (colors.isDark) Color(0xFF4A346E) else Color(0xFF7839EE),
                            activeTint = if (colors.isDark) Color(0xFFC084FC) else Color(0xFF7839EE)
                        )
                        AttendanceStatus.LEAVE -> StatusUiConfig(
                            label = "On leave",
                            icon = Icons.Default.Person,
                            activeBg = if (colors.isDark) Color(0xFF14293D) else Color(0xFFE5F3FF),
                            activeBorder = if (colors.isDark) Color(0xFF1E40AF) else Color(0xFF026AA2),
                            activeTint = if (colors.isDark) Color(0xFF60A5FA) else Color(0xFF026AA2)
                        )
                        else -> StatusUiConfig(
                            label = "Other",
                            icon = Icons.Default.RadioButtonUnchecked,
                            activeBg = colors.surfaceMuted,
                            activeBorder = colors.border,
                            activeTint = colors.textSecondary
                        )
                    }

                    Surface(
                        color = if (isSelected) cfg.activeBg else colors.surfaceMuted,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) cfg.activeBorder else colors.border
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedStatus = status }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = cfg.icon,
                                contentDescription = null,
                                tint = if (isSelected) cfg.activeTint else colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = cfg.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) cfg.activeTint else colors.textPrimary,
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Reason Textarea
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reason *",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Text(
                    text = "${reason.length}/200",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = reason,
                onValueChange = {
                    if (it.length <= 200) {
                        reason = it
                        if (it.isNotBlank()) errorText = null
                    }
                },
                placeholder = {
                    Text(
                        "Enter reason (e.g. forgot to scan, onsite meeting)...",
                        fontSize = 14.5.sp,
                        color = colors.textSecondary
                    )
                },
                isError = errorText != null,
                minLines = 3,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.brandPrimary,
                    unfocusedBorderColor = colors.border,
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface
                )
            )

            if (errorText != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(errorText!!, color = colors.danger, fontSize = 13.5.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Record Primary Button
            Button(
                onClick = {
                    val targetEmp = selectedEmployee
                    if (targetEmp == null) {
                        errorText = "Please select an employee"
                    } else if (reason.trim().length < 3) {
                        errorText = "Enter an audit reason of at least 3 characters"
                    } else {
                        val mId = targetEmp.memberId
                        val s = selectedStatus
                        val r = reason.trim()
                        isSubmitting = true
                        errorText = null
                        coroutineScope.launch {
                            val result = onSave(mId, s, r)
                            isSubmitting = false
                            if (result.isSuccess) {
                                dismissSmoothly()
                            } else {
                                errorText = result.exceptionOrNull()?.message ?: "Could not save attendance"
                            }
                        }
                    }
                },
                enabled = !isSubmitting && effectiveEmployees.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.brandPrimary,
                    contentColor = colors.textInverse
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Save record",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = colors.textInverse
                )
            }
        }
    }
}
