package com.example.chessbinge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessbinge.model.Move
import com.example.chessbinge.ui.theme.BgCard
import com.example.chessbinge.ui.theme.BgCardBorder
import com.example.chessbinge.ui.theme.CrimsonPrimary
import com.example.chessbinge.ui.theme.TextPrimary
import com.example.chessbinge.ui.theme.TextSecondary

@Composable
fun MoveHistoryBar(
    moves: List<Move>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Auto-scroll to latest move
    LaunchedEffect(moves.size) {
        if (moves.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BgCard)
            .border(1.dp, BgCardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        if (moves.isEmpty()) {
            Text(
                text = "Game started. White to move.",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.horizontalScroll(scrollState)
            ) {
                val pairs = moves.chunked(2)
                pairs.forEachIndexed { index, pair ->
                    val moveNum = index + 1
                    val isLastPair = index == pairs.size - 1

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$moveNum.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(4.dp))

                        // White move
                        val whiteMove = pair[0]
                        val isWhiteCurrent = isLastPair && pair.size == 1
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isWhiteCurrent) CrimsonPrimary else Color.Transparent)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = whiteMove.san,
                                fontSize = 12.sp,
                                fontWeight = if (isWhiteCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isWhiteCurrent) TextPrimary else TextPrimary.copy(alpha = 0.9f),
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Black move (if made)
                        if (pair.size > 1) {
                            val blackMove = pair[1]
                            val isBlackCurrent = isLastPair
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isBlackCurrent) CrimsonPrimary else Color.Transparent)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = blackMove.san,
                                    fontSize = 12.sp,
                                    fontWeight = if (isBlackCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isBlackCurrent) TextPrimary else TextPrimary.copy(alpha = 0.9f),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))
                    }
                }
            }
        }
    }
}
