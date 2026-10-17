package bizora.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bizora.app.data.model.AttendanceStatus
import bizora.app.ui.theme.LocalBizoraColors

enum class ChipSize { SM, MD }

@Composable
fun StatusChip(
    status: AttendanceStatus,
    modifier: Modifier = Modifier,
    size: ChipSize = ChipSize.MD,
    customLabel: String? = null,
    showIcon: Boolean = true
) {
    StatusChip(
        statusString = status.name,
        modifier = modifier,
        size = size,
        customLabel = customLabel,
        showIcon = showIcon
    )
}

@Composable
fun StatusChip(
    statusString: String,
    modifier: Modifier = Modifier,
    size: ChipSize = ChipSize.MD,
    customLabel: String? = null,
    showIcon: Boolean = true
) {
    val colors = LocalBizoraColors.current
    val isDark = colors.isDark

    val (bg, textColor, border, defaultLabel, icon) = when (statusString.uppercase()) {
        "PRESENT", "APPROVED" -> Quint(
            if (isDark) Color(0xFF1E2F16) else Color(0xFFECF2DE),
            if (isDark) Color(0xFFA7D980) else Color(0xFF40501E),
            if (isDark) Color(0xFF385226) else Color(0xFFD2DEC0),
            if (statusString.uppercase() == "APPROVED") "Approved" else "Present",
            Icons.Default.CheckCircle
        )
        "PENDING" -> Quint(
            if (isDark) Color(0xFF332711) else Color(0xFFFBF4D7),
            if (isDark) Color(0xFFFACC15) else Color(0xFF8C6314),
            if (isDark) Color(0xFF594518) else Color(0xFFF2E5B8),
            "Pending",
            Icons.Default.Schedule
        )
        "ABSENT" -> Quint(
            if (isDark) Color(0xFF262626) else Color(0xFFF1F5E9),
            if (isDark) Color(0xFFCBD5E1) else Color(0xFF5A6844),
            if (isDark) Color(0xFF3F3F46) else Color(0xFFDCE4CD),
            "Absent",
            Icons.Default.PersonOff
        )
        "HALF_DAY" -> Quint(
            if (isDark) Color(0xFF2B1D42) else Color(0xFFF2EAFD),
            if (isDark) Color(0xFFC084FC) else Color(0xFF7839EE),
            if (isDark) Color(0xFF4A346E) else Color(0xFFDDD6FE),
            "Half day",
            Icons.Default.Schedule
        )
        "LEAVE" -> Quint(
            if (isDark) Color(0xFF14293D) else Color(0xFFE5F3FF),
            if (isDark) Color(0xFF60A5FA) else Color(0xFF026AA2),
            if (isDark) Color(0xFF1E40AF) else Color(0xFFB9E6FE),
            "On leave",
            Icons.Default.Person
        )
        "ACTIVE" -> Quint(
            if (isDark) Color(0xFF1E2F16) else Color(0xFFECF2DE),
            if (isDark) Color(0xFFA7D980) else Color(0xFF40501E),
            if (isDark) Color(0xFF385226) else Color(0xFFD2DEC0),
            "Active",
            Icons.Default.CheckCircle
        )
        "INVITED" -> Quint(
            if (isDark) Color(0xFF332711) else Color(0xFFFBF4D7),
            if (isDark) Color(0xFFFACC15) else Color(0xFF8C6314),
            if (isDark) Color(0xFF594518) else Color(0xFFF2E5B8),
            "Invite pending",
            Icons.Default.Schedule
        )
        "REJECTED" -> Quint(
            if (isDark) Color(0xFF381A1A) else Color(0xFFFDF0ED),
            if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
            if (isDark) Color(0xFF5E2727) else Color(0xFFF5C6C0),
            "Rejected",
            Icons.Default.Cancel
        )
        else -> Quint(
            if (isDark) Color(0xFF262626) else Color(0xFFF1F5E9),
            if (isDark) Color(0xFF94A3B8) else Color(0xFF5A6844),
            if (isDark) Color(0xFF3F3F46) else Color(0xFFE2E8D5),
            "Not marked",
            Icons.Default.RadioButtonUnchecked
        )
    }

    val label = customLabel ?: defaultLabel
    val isSmall = size == ChipSize.SM

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .padding(
                horizontal = if (isSmall) 10.dp else 12.dp,
                vertical = if (isSmall) 4.dp else 6.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (showIcon) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(if (isSmall) 13.dp else 15.dp)
            )
        }
        Text(
            text = label,
            color = textColor,
            fontSize = if (isSmall) 13.sp else 14.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.1.sp
        )
    }
}

private data class Quint<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
