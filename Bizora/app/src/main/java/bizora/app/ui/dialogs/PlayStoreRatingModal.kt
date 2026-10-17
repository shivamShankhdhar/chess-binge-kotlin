package bizora.app.ui.dialogs

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bizora.app.ui.components.BizoraMark
import bizora.app.ui.components.ModalCloseButton
import bizora.app.ui.theme.LocalBizoraColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayStoreRatingModal(
    onDismiss: () -> Unit,
    onRated: (Int) -> Unit = {}
) {
    val colors = LocalBizoraColors.current
    val context = LocalContext.current
    var selectedStars by remember { mutableIntStateOf(5) }

    fun openPlayStore() {
        val packageName = context.packageName
        try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
            context.startActivity(marketIntent)
        } catch (e: ActivityNotFoundException) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
            context.startActivity(webIntent)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = colors.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = colors.border)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                ModalCloseButton(
                    onClick = onDismiss
                )
            }

            BizoraMark(size = 54.dp)

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Enjoying Bizora?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Your rating and feedback help us make workplace attendance faster and better for everyone.",
                fontSize = 13.5.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Star Rating Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..5).forEach { star ->
                    IconButton(
                        onClick = { selectedStars = star },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (star <= selectedStars) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "$star stars",
                            tint = if (star <= selectedStars) Color(0xFFF59E0B) else colors.border,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = when (selectedStars) {
                    5 -> "Great! We love to hear that! ⭐⭐⭐⭐⭐"
                    4 -> "Good experience! ⭐⭐⭐⭐"
                    3 -> "Average ⭐⭐⭐"
                    else -> "We're constantly improving! ⭐"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selectedStars >= 4) Color(0xFF16A34A) else colors.textSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onRated(selectedStars)
                    openPlayStore()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.brandPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Rate on Google Play",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textInverse
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Remind Me Later",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )
            }
        }
    }
}
