package bizora.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bizora.app.ui.theme.LocalBizoraColors

@Composable
fun MetricTile(
    label: String,
    count: Int,
    tintColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalBizoraColors.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(tintColor)
            .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 12.dp)
    ) {
        Column {
            Text(
                text = count.toString(),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary,
                maxLines = 2,
                lineHeight = 16.sp
            )
        }
    }
}
