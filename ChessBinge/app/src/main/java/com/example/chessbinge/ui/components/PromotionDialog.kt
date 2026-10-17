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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.chessbinge.model.Piece
import com.example.chessbinge.model.PieceColor
import com.example.chessbinge.model.PieceType
import com.example.chessbinge.ui.board.ChessPieces
import com.example.chessbinge.ui.theme.BgCard
import com.example.chessbinge.ui.theme.BoardBorderWood
import com.example.chessbinge.ui.theme.CrimsonDark
import com.example.chessbinge.ui.theme.CrimsonGlow
import com.example.chessbinge.ui.theme.CrimsonPrimary
import com.example.chessbinge.ui.theme.GoldAccent
import com.example.chessbinge.ui.theme.TextPrimary
import com.example.chessbinge.ui.theme.TextSecondary
import com.example.chessbinge.ui.theme.TitanOneFontFamily

@Composable
fun PromotionDialog(
    color: PieceColor,
    onPieceSelected: (PieceType) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(BgCard, Color(0xFF1F1215), BoardBorderWood)
                    )
                )
                .border(2.dp, CrimsonPrimary, RoundedCornerShape(20.dp))
                .shadow(24.dp)
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "PROMOTE PAWN",
                    fontFamily = TitanOneFontFamily,
                    fontSize = 20.sp,
                    color = GoldAccent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select a piece to promote your pawn",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val promotionOptions = listOf(
                        PieceType.QUEEN to "Queen",
                        PieceType.ROOK to "Rook",
                        PieceType.BISHOP to "Bishop",
                        PieceType.KNIGHT to "Knight"
                    )

                    for ((type, name) in promotionOptions) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF241A1E))
                                .border(1.dp, CrimsonGlow.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable { onPieceSelected(type) }
                                .padding(vertical = 12.dp, horizontal = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp)
                            ) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                                    val dummyPiece = Piece("promo_${type}", type, color)
                                    ChessPieces.drawPiece(this, dummyPiece, androidx.compose.ui.geometry.Offset.Zero, size)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
