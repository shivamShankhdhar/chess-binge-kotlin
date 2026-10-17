package bizora.app.ui.dialogs

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bizora.app.ui.components.ModalCloseButton
import bizora.app.ui.theme.LocalBizoraColors
import bizora.app.utils.ExportedReport
import bizora.app.utils.ReportExporter
import bizora.app.utils.ReportFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportExportModal(
    report: ExportedReport?,
    onDismiss: () -> Unit
) {
    if (report == null) return
    val colors = LocalBizoraColors.current
    val context = LocalContext.current

    var savedPath by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Launcher for system file picker to save anywhere (Downloads, Drive, Documents, etc.)
    val saveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(report.format.mimeType)
    ) { uri ->
        if (uri != null) {
            val success = ReportExporter.saveToUri(context, report, uri)
            if (success) {
                savedPath = "Custom folder selection"
                Toast.makeText(context, "Saved file to device successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to save file to selected location.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val formatColor = when (report.format) {
        ReportFormat.EXCEL -> Color(0xFF15803D)
        ReportFormat.PDF -> Color(0xFFDC2626)
        ReportFormat.CSV -> Color(0xFF0284C7)
    }

    val formatTint = when (report.format) {
        ReportFormat.EXCEL -> Color(0xFFDCFCE7)
        ReportFormat.PDF -> Color(0xFFFEE2E2)
        ReportFormat.CSV -> Color(0xFFE0F2FE)
    }

    val formatIcon = when (report.format) {
        ReportFormat.EXCEL -> Icons.Default.TableChart
        ReportFormat.PDF -> Icons.Default.PictureAsPdf
        ReportFormat.CSV -> Icons.Default.Description
    }

    val formatLabel = when (report.format) {
        ReportFormat.EXCEL -> "Excel Report Ready"
        ReportFormat.PDF -> "PDF Report Ready"
        ReportFormat.CSV -> "CSV Report Ready"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = colors.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = colors.border
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(formatTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = formatIcon,
                            contentDescription = null,
                            tint = formatColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = formatLabel,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Generated real .${report.format.extension} file",
                            fontSize = 12.5.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                ModalCloseButton(
                    onClick = onDismiss
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // File Details Card
            Surface(
                color = colors.surfaceMuted,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = formatColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = report.filename,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(formatColor)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = report.format.extension.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    HorizontalDivider(color = colors.border)

                    // Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("FILE SIZE", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                            Text(report.fileSizeFormatted, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        }
                        Column {
                            Text("RECORDS", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                            Text("${report.recordCount} Employees", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        }
                        Column {
                            Text("FORMAT", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                            Text(report.format.displayName, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary Action: Save to Device
                Button(
                    onClick = {
                        isSaving = true
                        val res = ReportExporter.saveToDevice(context, report)
                        res.onSuccess { path ->
                            savedPath = path
                            Toast.makeText(context, "Saved to $path", Toast.LENGTH_LONG).show()
                        }.onFailure { err ->
                            Toast.makeText(context, "Could not save to Downloads: ${err.message}", Toast.LENGTH_SHORT).show()
                        }
                        isSaving = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.brandPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (savedPath != null) "Save Again to Device" else "Save to Device",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                // Saved Confirmation Card
                if (savedPath != null) {
                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Saved to Device Storage",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF14532D)
                                )
                                Text(
                                    text = savedPath ?: "",
                                    fontSize = 12.sp,
                                    color = Color(0xFF166534)
                                )
                            }
                        }
                    }
                }

                // Row: Share & Open File
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            ReportExporter.shareReport(context, report)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colors.textPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = colors.brandPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Share",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            ReportExporter.openReport(context, report)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colors.textPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = colors.brandPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Open File",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Secondary Action: Choose custom destination folder (Save As...)
                TextButton(
                    onClick = {
                        saveAsLauncher.launch(report.filename)
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Choose specific folder (Save As...)",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
