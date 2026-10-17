package bizora.app.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bizora.app.data.model.EmployeeItem
import bizora.app.ui.components.ModalCloseButton
import bizora.app.ui.components.StatusChip
import bizora.app.ui.theme.LocalBizoraColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeActionsModal(
    employee: EmployeeItem,
    workplaceName: String,
    workplaceCode: String = "",
    onDismiss: () -> Unit,
    onChangePinClick: () -> Unit,
    onDeactivateClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    val colors = LocalBizoraColors.current
    val context = LocalContext.current
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
            // Header Row: Avatar + Name + Status + Close (X) button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(colors.brandTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = employee.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "EM" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.brandPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = employee.name,
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
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
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = employee.status,
                                    color = when (employee.status) {
                                        "ACTIVE" -> colors.success
                                        "INVITED" -> colors.pending
                                        else -> colors.neutralStatus
                                    },
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${employee.employeeCode} • ${employee.teamName ?: "General"} • ${employee.invitedEmail ?: "No email"}",
                            fontSize = 12.5.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                ModalCloseButton(
                    onClick = { dismissSmoothly() }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Items
            EmployeeActionRow(
                icon = Icons.Default.Key,
                iconTint = colors.brandPrimary,
                iconBg = colors.brandTint,
                title = "Change employee MPIN",
                subtitle = "Set or reset employee's 4–6 digit login PIN",
                onClick = {
                    dismissSmoothly { onChangePinClick() }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            EmployeeActionRow(
                icon = Icons.Outlined.ContentCopy,
                iconTint = colors.textPrimary,
                iconBg = colors.surfaceMuted,
                title = "Copy employee code",
                subtitle = "Copy code ${employee.employeeCode} to clipboard",
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Employee Code", employee.employeeCode))
                    Toast.makeText(context, "Employee code copied.", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            EmployeeActionRow(
                icon = Icons.Default.Share,
                iconTint = colors.textPrimary,
                iconBg = colors.surfaceMuted,
                title = "Share team invite",
                subtitle = "Send join link and details for ${employee.teamName ?: "team"}",
                onClick = {
                    val teamInfo = if (employee.teamName != null) "the ${employee.teamName} team at " else ""
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Hi ${employee.name}, join $teamInfo\"$workplaceName\" on Bizora!\nYour employee code is: ${employee.employeeCode}.\nDownload & request to join: https://bizora.app/join"
                        )
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Team Invite"))
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            EmployeeActionRow(
                icon = Icons.Default.PersonRemove,
                iconTint = colors.textSecondary,
                iconBg = colors.surfaceMuted,
                title = "Deactivate employee",
                subtitle = "Revoke active attendance recording access",
                onClick = {
                    dismissSmoothly { onDeactivateClick() }
                }
            )

            if (onDeleteClick != null) {
                Spacer(modifier = Modifier.height(10.dp))

                EmployeeActionRow(
                    icon = Icons.Default.Delete,
                    iconTint = colors.danger,
                    iconBg = colors.danger.copy(alpha = 0.12f),
                    title = "Delete employee",
                    subtitle = "Remove this employee record completely",
                    onClick = {
                        dismissSmoothly { onDeleteClick() }
                    }
                )
            }
        }
    }
}

@Composable
private fun EmployeeActionRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = LocalBizoraColors.current
    Surface(
        color = colors.surfaceMuted.copy(alpha = 0.5f),
        shape = RoundedCornerShape(14.dp),
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
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
