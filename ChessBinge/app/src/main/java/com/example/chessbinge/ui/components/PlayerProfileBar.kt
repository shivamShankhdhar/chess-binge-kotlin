package com.example.chessbinge.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessbinge.model.Piece
import com.example.chessbinge.model.PieceColor
import com.example.chessbinge.model.PieceType
import com.example.chessbinge.ui.theme.BgCard
import com.example.chessbinge.ui.theme.BgCardBorder
import com.example.chessbinge.ui.theme.CrimsonGlow
import com.example.chessbinge.ui.theme.CrimsonPrimary
import com.example.chessbinge.ui.theme.GoldAccent
import com.example.chessbinge.ui.theme.TextPrimary
import com.example.chessbinge.ui.theme.TextSecondary

@Composable
fun PlayerProfileBar(
    name: String,
    subtitle: String,
    color: PieceColor,
    isCurrentTurn: Boolean,
    isAI: Boolean = false,
    capturedPieces: List<Piece>,
    materialAdvantage: Int, // difference from perspective of this player
    timeRemainingMs: Long,
    hasClock: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isCurrentTurn) CrimsonPrimary else BgCardBorder,
        animationSpec = tween(300),
        label = "borderTurn"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (isCurrentTurn) {
                        listOf(Color(0xFF261014), BgCard, Color(0xFF1E1418))
                    } else {
                        listOf(BgCard, BgCard)
                    }
                )
            )
            .border(
                width = if (isCurrentTurn) 1.8.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left: Avatar + Name + Captures
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Avatar badge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (color == PieceColor.WHITE) Color(0xFFE9ECEF) else Color(0xFF16171B))
                        .border(
                            1.5.dp,
                            if (color == PieceColor.WHITE) Color(0xFFADB5BD) else CrimsonPrimary,
                            CircleShape
                        )
                ) {
                    if (isAI) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "AI",
                            tint = if (color == PieceColor.WHITE) Color(0xFF212529) else CrimsonGlow,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Text(
                            text = if (color == PieceColor.WHITE) "♔" else "♚",
                            fontSize = 24.sp,
                            color = if (color == PieceColor.WHITE) Color(0xFF212529) else CrimsonGlow
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        if (isCurrentTurn) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(CrimsonGlow)
                            )
                        }
                    }

                    // Captured pieces & material score
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if (capturedPieces.isNotEmpty()) {
                            val pieceSymbols = capturedPieces.joinToString("") { piece ->
                                when (piece.type) {
                                    PieceType.PAWN -> "p"
                                    PieceType.KNIGHT -> "n"
                                    PieceType.BISHOP -> "b"
                                    PieceType.ROOK -> "r"
                                    PieceType.QUEEN -> "q"
                                    PieceType.KING -> ""
                                }
                            }
                            Text(
                                text = pieceSymbols,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        } else {
                            Text(
                                text = subtitle,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        if (materialAdvantage > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(GoldAccent.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "+$materialAdvantage",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent
                                )
                            }
                        }
                    }
                }
            }

            // Right: Digital Clock
            if (hasClock) {
                val totalSeconds = (timeRemainingMs / 1000).coerceAtLeast(0)
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                val formattedTime = "%02d:%02d".format(minutes, seconds)
                val isLowTime = totalSeconds <= 30 && isCurrentTurn

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isLowTime) CrimsonPrimary.copy(alpha = 0.25f)
                            else Color(0xFF0F1014)
                        )
                        .border(
                            1.dp,
                            if (isLowTime) CrimsonGlow else if (isCurrentTurn) CrimsonPrimary.copy(alpha = 0.5f) else BgCardBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isLowTime) CrimsonGlow else TextPrimary
                    )
                }
            }
        }
    }
}
