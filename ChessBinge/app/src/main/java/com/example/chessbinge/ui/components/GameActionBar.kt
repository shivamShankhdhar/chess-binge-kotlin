package com.example.chessbinge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessbinge.ui.theme.BgCard
import com.example.chessbinge.ui.theme.BgCardBorder
import com.example.chessbinge.ui.theme.CrimsonGlow
import com.example.chessbinge.ui.theme.CrimsonPrimary
import com.example.chessbinge.ui.theme.GoldAccent
import com.example.chessbinge.ui.theme.TextPrimary
import com.example.chessbinge.ui.theme.TextSecondary

@Composable
fun GameActionBar(
    canUndo: Boolean,
    isSoundMuted: Boolean,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onFlip: () -> Unit,
    onResign: () -> Unit,
    onToggleSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .border(1.dp, BgCardBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        ActionButtonItem(
            icon = Icons.AutoMirrored.Filled.Undo,
            label = "Undo",
            enabled = canUndo,
            onClick = onUndo
        )
        ActionButtonItem(
            icon = Icons.Default.Lightbulb,
            label = "Hint",
            accentColor = GoldAccent,
            onClick = onHint
        )
        ActionButtonItem(
            icon = Icons.Default.SwapVert,
            label = "Flip",
            onClick = onFlip
        )
        ActionButtonItem(
            icon = if (isSoundMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
            label = if (isSoundMuted) "Unmute" else "Mute",
            onClick = onToggleSound
        )
        ActionButtonItem(
            icon = Icons.Default.Flag,
            label = "Resign",
            accentColor = CrimsonGlow,
            onClick = onResign
        )
    }
}

@Composable
private fun ActionButtonItem(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    accentColor: Color = TextPrimary,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.4f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accentColor.copy(alpha = alpha),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary.copy(alpha = alpha)
        )
    }
}
