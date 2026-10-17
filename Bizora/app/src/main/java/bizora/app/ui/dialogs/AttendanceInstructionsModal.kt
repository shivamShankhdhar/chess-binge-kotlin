package bizora.app.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bizora.app.ui.components.ModalCloseButton
import bizora.app.ui.theme.LocalBizoraColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceInstructionsModal(
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
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.brandTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = colors.brandPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "How Attendance Works",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Operational guide & daily workflow",
                            fontSize = 12.5.sp,
                            color = colors.textSecondary
                        )
                    }
                }
                ModalCloseButton(
                    onClick = onDismiss
                )
            }

            HorizontalDivider(color = colors.border, modifier = Modifier.padding(bottom = 16.dp))

            // Step 1
            InstructionStepRow(
                stepNumber = "1",
                stepColor = colors.brandPrimary,
                title = "Open Morning Session",
                description = "Start the daily attendance session each morning. A secure, rotating anti-spoof QR code is created specifically for your workplace."
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Step 2
            InstructionStepRow(
                stepNumber = "2",
                stepColor = Color(0xFF10B981),
                title = "Employees Scan at Kiosk",
                description = "Display the QR code on a front-desk tablet, laptop, or mobile screen. Employees scan with their mobile app to instantly record Check-In or Check-Out."
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Step 3
            InstructionStepRow(
                stepNumber = "3",
                stepColor = Color(0xFF8B5CF6),
                title = "Manual Overrides & Corrections",
                description = "If someone forgot their phone, worked remotely, or needs an adjustment, tap Manual Attendance to select the employee, set status, and provide a reason."
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Step 4
            InstructionStepRow(
                stepNumber = "4",
                stepColor = Color(0xFFF59E0B),
                title = "Automated Session Closing",
                description = "Sessions automatically close at your workplace auto-close hour (e.g. 23:00) to lock daily records for reports, or you can end the session manually anytime."
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Security & Verification Chips
            Surface(
                color = colors.surfaceMuted,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SecurityChip(icon = Icons.Default.Shield, label = "Anti-Spoof QR")
                    SecurityChip(icon = Icons.Default.LocationOn, label = "Geofence")
                    SecurityChip(icon = Icons.Default.Wifi, label = "Wi-Fi Sync")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pro Tip Card
            Surface(
                color = Color(0xFFFEF3C7),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(18.dp).padding(top = 1.dp)
                    )
                    Text(
                        text = "Pro Tip: Leave the QR Kiosk screen running on a tablet or reception desk monitor in Guided Access mode for a dedicated, zero-touch attendance terminal.",
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFF92400E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Got it button
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Got it",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = colors.textInverse
                )
            }
        }
    }
}

@Composable
private fun InstructionStepRow(
    stepNumber: String,
    stepColor: Color,
    title: String,
    description: String
) {
    val colors = LocalBizoraColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
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
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Color.White
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun SecurityChip(icon: ImageVector, label: String) {
    val colors = LocalBizoraColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.brandPrimary,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary
        )
    }
}
