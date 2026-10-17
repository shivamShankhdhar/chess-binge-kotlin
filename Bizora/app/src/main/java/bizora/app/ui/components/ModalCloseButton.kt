package bizora.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bizora.app.ui.theme.LocalBizoraColors

/**
 * Reusable, high-visibility, weighted close button for all dialogs and bottom sheets.
 * Ensures the close button never hides, has adequate touch target, and high contrast.
 */
@Composable
fun ModalCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalBizoraColors.current
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = colors.surfaceMuted,
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.8f)),
        modifier = modifier.size(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
