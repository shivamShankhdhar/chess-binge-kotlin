package bizora.app.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bizora.app.ui.components.ModalCloseButton
import bizora.app.ui.theme.LocalBizoraColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterAttendanceModal(
    selectedStatus: String,
    selectedRange: String,
    onDismiss: () -> Unit,
    onApply: (status: String, range: String) -> Unit,
    onReset: () -> Unit
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

    var draftStatus by remember { mutableStateOf(selectedStatus) }
    var draftRange by remember { mutableStateOf(selectedRange) }

    val statusOptions = listOf("ALL", "PRESENT", "ABSENT", "HALF_DAY", "LEAVE", "PENDING")
    val rangeOptions = listOf("Today", "Last 7 Days", "This Month", "All Time")

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
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter attendance",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )

                ModalCloseButton(
                    onClick = { dismissSmoothly() }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Date Range Section
            Text(
                text = "Date range",
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rangeOptions.take(2).forEach { range ->
                    val isSelected = draftRange.equals(range, ignoreCase = true)
                    Surface(
                        color = if (isSelected) colors.brandTint else colors.surfaceMuted,
                        border = if (isSelected) BorderStroke(1.5.dp, colors.brandPrimary) else BorderStroke(1.dp, colors.border),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { draftRange = range }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = range,
                                fontSize = 14.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colors.brandPrimary else colors.textPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rangeOptions.drop(2).forEach { range ->
                    val isSelected = draftRange.equals(range, ignoreCase = true)
                    Surface(
                        color = if (isSelected) colors.brandTint else colors.surfaceMuted,
                        border = if (isSelected) BorderStroke(1.5.dp, colors.brandPrimary) else BorderStroke(1.dp, colors.border),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { draftRange = range }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = range,
                                fontSize = 14.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colors.brandPrimary else colors.textPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Attendance Status Section
            Text(
                text = "Attendance status",
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Pills grid (3 per row)
            statusOptions.chunked(3).forEach { chunk ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chunk.forEach { status ->
                        val isSelected = draftStatus.equals(status, ignoreCase = true)
                        val displayStatus = when (status) {
                            "ALL" -> "All"
                            "PRESENT" -> "Present"
                            "ABSENT" -> "Absent"
                            "HALF_DAY" -> "Half day"
                            "LEAVE" -> "On leave"
                            "PENDING" -> "Pending"
                            else -> status
                        }
                        Surface(
                            color = if (isSelected) colors.brandTint else colors.surfaceMuted,
                            border = if (isSelected) BorderStroke(1.5.dp, colors.brandPrimary) else BorderStroke(1.dp, colors.border),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { draftStatus = status }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 9.dp)
                            ) {
                                Text(
                                    text = displayStatus,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) colors.brandPrimary else colors.textPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons: Apply filters vs Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        draftStatus = "ALL"
                        draftRange = "Today"
                        dismissSmoothly { onReset() }
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "Reset",
                        color = colors.textPrimary,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = {
                        val s = draftStatus
                        val r = draftRange
                        dismissSmoothly { onApply(s, r) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "Apply filters",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
