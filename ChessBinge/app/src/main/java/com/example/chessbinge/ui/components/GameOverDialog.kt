package com.example.chessbinge.ui.components

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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.chessbinge.model.GameState
import com.example.chessbinge.model.PieceColor
import com.example.chessbinge.ui.theme.BgCard
import com.example.chessbinge.ui.theme.BoardBorderWood
import com.example.chessbinge.ui.theme.CrimsonGlow
import com.example.chessbinge.ui.theme.CrimsonPrimary
import com.example.chessbinge.ui.theme.GoldAccent
import com.example.chessbinge.ui.theme.TextPrimary
import com.example.chessbinge.ui.theme.TextSecondary
import com.example.chessbinge.ui.theme.TitanOneFontFamily

@Composable
fun GameOverDialog(
    gameState: GameState,
    playerColor: PieceColor,
    onPlayAgain: () -> Unit,
    onReviewGame: () -> Unit,
    onHome: () -> Unit
) {
    val isWin = gameState.winner == playerColor
    val isLoss = gameState.winner != null && gameState.winner != playerColor
    val isDraw = gameState.isDraw

    val title = when {
        isWin -> "VICTORY!"
        isLoss -> "DEFEAT"
        else -> "DRAW"
    }

    val subtitle = when {
        gameState.isCheckmate -> "Checkmate! ${gameState.winner?.name} reigns supreme."
        gameState.isStalemate -> "Stalemate! No legal moves available."
        gameState.drawReason != null -> gameState.drawReason
        gameState.winner != null -> "${gameState.winner.name} won by resignation or timeout."
        else -> "Game concluded."
    }

    Dialog(onDismissRequest = { /* Modal */ }) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF261217),
                            BgCard,
                            BoardBorderWood
                        )
                    )
                )
                .border(2.dp, if (isWin) GoldAccent else CrimsonPrimary, RoundedCornerShape(24.dp))
                .shadow(28.dp)
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Trophy / Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    if (isWin) GoldAccent.copy(alpha = 0.3f) else CrimsonGlow.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(
                            2.dp,
                            if (isWin) GoldAccent else CrimsonPrimary,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Game Result",
                        tint = if (isWin) GoldAccent else CrimsonGlow,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    fontFamily = TitanOneFontFamily,
                    fontSize = 28.sp,
                    color = if (isWin) GoldAccent else if (isLoss) CrimsonGlow else TextPrimary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = subtitle ?: "",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Play Again Button
                Button(
                    onClick = onPlayAgain,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonPrimary,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PLAY AGAIN",
                        fontFamily = TitanOneFontFamily,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Review Moves Button
                OutlinedButton(
                    onClick = onReviewGame,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(
                        text = "REVIEW MOVES",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Home Button
                TextButton(
                    onClick = onHome,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Back to Menu",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
