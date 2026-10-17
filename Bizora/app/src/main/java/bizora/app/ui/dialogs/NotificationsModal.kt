package bizora.app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bizora.app.data.model.NotificationItem
import bizora.app.ui.components.DocCheckmarkIllustration
import bizora.app.ui.components.EmptyState
import bizora.app.ui.components.ModalCloseButton
import bizora.app.ui.theme.LocalBizoraColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsModal(
    notifications: List<NotificationItem>,
    onDismiss: () -> Unit,
    onMarkAllAsRead: () -> Unit = {},
    onMarkAsRead: (String) -> Unit = {}
) {
    val colors = LocalBizoraColors.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun dismissSmoothly() {
        coroutineScope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onDismiss()
            }
        }
    }
    var filter by remember { mutableStateOf("ALL") } // "ALL" or "UNREAD"

    val unreadCount = notifications.count { !it.read }
    val displayedNotifications = remember(notifications, filter) {
        if (filter == "UNREAD") notifications.filter { !it.read }
        else notifications
    }

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
                .padding(bottom = 24.dp)
        ) {
            // Header Row (Bell icon + "Notifications" + Unread badge + Mark read button + Close button)
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
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = colors.brandPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Notifications",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    if (unreadCount > 0) {
                        Surface(
                            color = colors.brandTint,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = "$unreadCount new",
                                color = colors.brandPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (unreadCount > 0) {
                        Surface(
                            color = colors.surfaceMuted,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { onMarkAllAsRead() }
                        ) {
                            Text(
                                text = "Mark read",
                                color = colors.brandPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    ModalCloseButton(
                        onClick = { dismissSmoothly() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // All Chip
                val allActive = filter == "ALL"
                Surface(
                    color = if (allActive) colors.brandPrimary else colors.surfaceMuted,
                    shape = RoundedCornerShape(999.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (allActive) colors.brandPrimary else colors.border
                    ),
                    modifier = Modifier.clickable { filter = "ALL" }
                ) {
                    Text(
                        text = "All (${notifications.size})",
                        color = if (allActive) colors.textInverse else colors.textSecondary,
                        fontSize = 14.5.sp,
                        fontWeight = if (allActive) FontWeight.Bold else FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }

                // Unread Chip
                val unreadActive = filter == "UNREAD"
                Surface(
                    color = if (unreadActive) colors.brandPrimary else colors.surfaceMuted,
                    shape = RoundedCornerShape(999.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (unreadActive) colors.brandPrimary else colors.border
                    ),
                    modifier = Modifier.clickable { filter = "UNREAD" }
                ) {
                    Text(
                        text = "Unread ($unreadCount)",
                        color = if (unreadActive) colors.textInverse else colors.textSecondary,
                        fontSize = 14.5.sp,
                        fontWeight = if (unreadActive) FontWeight.Bold else FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Notification List or Empty State
            if (displayedNotifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        illustration = { DocCheckmarkIllustration(size = 90.dp) },
                        title = if (filter == "UNREAD") "No unread notifications" else "No notifications yet",
                        description = if (filter == "UNREAD")
                            "You've read all your recent notifications."
                        else
                            "Check-in updates and workplace announcements will appear here."
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 440.dp)
                ) {
                    items(displayedNotifications, key = { it.id }) { notif ->
                        val (icon, iconColor, iconBg) = when (notif.type.uppercase()) {
                            "REQUEST" -> Triple(Icons.Default.Schedule, Color(0xFFD97706), Color(0xFFFFFBEB))
                            "SYSTEM" -> Triple(Icons.Default.Security, Color(0xFF7C3AED), Color(0xFFFAF5FF))
                            "INFO" -> Triple(Icons.Default.Info, Color(0xFF5B692D), Color(0xFFF4F7EC))
                            else -> Triple(Icons.Default.CheckCircle, Color(0xFF16A34A), Color(0xFFF0FDF4))
                        }

                        Surface(
                            color = if (colors.isDark) colors.surfaceMuted else Color.White,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (!notif.read) colors.brandPrimary.copy(alpha = 0.4f) else colors.border
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMarkAsRead(notif.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (colors.isDark) colors.surface else iconBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = iconColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = notif.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.5.sp,
                                            color = colors.textPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (!notif.read) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF22C55E))
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = notif.body,
                                        fontSize = 14.sp,
                                        color = colors.textSecondary,
                                        lineHeight = 19.sp
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = notif.timestamp,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.brandPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
