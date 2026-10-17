package com.example.chessbinge.ui.board

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.example.chessbinge.R
import com.example.chessbinge.engine.ChessEngine
import com.example.chessbinge.model.GameState
import com.example.chessbinge.model.Move
import com.example.chessbinge.model.PieceColor
import com.example.chessbinge.model.PieceType
import com.example.chessbinge.model.Position
import com.example.chessbinge.ui.theme.BgDark
import com.example.chessbinge.ui.theme.BoardBorderBevel
import com.example.chessbinge.ui.theme.BoardBorderWood
import com.example.chessbinge.ui.theme.BoardCoordinateMuted
import com.example.chessbinge.ui.theme.BoardCoordinateText
import com.example.chessbinge.ui.theme.BoardSquareDark
import com.example.chessbinge.ui.theme.BoardSquareLight
import com.example.chessbinge.ui.theme.CaptureRing
import com.example.chessbinge.ui.theme.CrimsonGlow
import com.example.chessbinge.ui.theme.GoldAccent
import com.example.chessbinge.ui.theme.HintMoveGlow
import com.example.chessbinge.ui.theme.KingInCheckGlow
import com.example.chessbinge.ui.theme.LastMoveHighlight
import com.example.chessbinge.ui.theme.SelectedSquareGlow
import com.example.chessbinge.ui.theme.ValidMoveDot

@Composable
fun ChessBoardView(
    gameState: GameState,
    selectedPosition: Position?,
    validMoves: List<Move>,
    isFlipped: Boolean,
    hintMove: Move? = null,
    onSquareClicked: (Position) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val typeface = remember {
        try {
            ResourcesCompat.getFont(context, R.font.fredoka) ?: Typeface.DEFAULT_BOLD
        } catch (_: Exception) {
            Typeface.DEFAULT_BOLD
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(elevation = 16.dp, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(gameState, isFlipped, selectedPosition) {
                    detectTapGestures { offset ->
                        val borderW = size.width * 0.055f
                        val borderH = size.height * 0.055f
                        val boardW = size.width - borderW * 2f
                        val boardH = size.height - borderH * 2f
                        val sqW = boardW / 8f
                        val sqH = boardH / 8f

                        if (offset.x >= borderW && offset.x <= size.width - borderW &&
                            offset.y >= borderH && offset.y <= size.height - borderH
                        ) {
                            val colFrac = ((offset.x - borderW) / sqW).toInt().coerceIn(0, 7)
                            val rowFrac = ((offset.y - borderH) / sqH).toInt().coerceIn(0, 7)

                            val boardRow = if (isFlipped) 7 - rowFrac else rowFrac
                            val boardCol = if (isFlipped) 7 - colFrac else colFrac

                            onSquareClicked(Position(boardRow, boardCol))
                        }
                    }
                }
        ) {
            val totalW = size.width
            val totalH = size.height

            // 1. Calculate Border & Board dimensions
            val borderWidth = totalW * 0.055f
            val borderHeight = totalH * 0.055f
            val gridW = totalW - borderWidth * 2f
            val gridH = totalH - borderHeight * 2f
            val sqW = gridW / 8f
            val sqH = gridH / 8f

            // 2. Outer Beveled Wood & Carbon Border Frame
            val outerCornerRadius = totalW * 0.035f
            val outerFramePath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(0f, 0f, totalW, totalH),
                        cornerRadius = CornerRadius(outerCornerRadius, outerCornerRadius)
                    )
                )
            }
            drawPath(
                path = outerFramePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BoardBorderWood,
                        Color(0xFF1B1B22),
                        BoardBorderWood
                    )
                )
            )

            // Outer frame thin metallic bevel stroke
            drawPath(
                path = outerFramePath,
                color = BoardBorderBevel,
                style = Stroke(width = totalW * 0.008f)
            )

            // Inner border rim around squares
            val innerBoardRect = Rect(
                borderWidth - 1f,
                borderHeight - 1f,
                totalW - borderWidth + 1f,
                totalH - borderHeight + 1f
            )
            drawRect(
                color = BoardBorderBevel,
                topLeft = Offset(innerBoardRect.left, innerBoardRect.top),
                size = Size(innerBoardRect.width, innerBoardRect.height),
                style = Stroke(width = totalW * 0.006f)
            )

            // 3. Draw Premium Metallic Gold Coordinates (Numbers on left/right borders, Letters on top/bottom)
            val textPaint = Paint().apply {
                isAntiAlias = true
                this.typeface = typeface
                textSize = borderWidth * 0.52f
                color = BoardCoordinateText.toArgb()
                textAlign = Paint.Align.CENTER
                setShadowLayer(4f, 0f, 1f, Color.Black.toArgb())
            }

            // Rank Numbers (Vertical borders: Left & Right)
            for (displayRow in 0..7) {
                val actualRow = if (isFlipped) 7 - displayRow else displayRow
                val rankNumber = (8 - actualRow).toString()
                val cy = borderHeight + displayRow * sqH + sqH / 2f + textPaint.textSize / 3f

                // Left border number
                val leftX = borderWidth * 0.5f
                drawContext.canvas.nativeCanvas.drawText(rankNumber, leftX, cy, textPaint)

                // Right border number
                val rightX = totalW - borderWidth * 0.5f
                drawContext.canvas.nativeCanvas.drawText(rankNumber, rightX, cy, textPaint)
            }

            // File Letters (Horizontal borders: Top & Bottom)
            for (displayCol in 0..7) {
                val actualCol = if (isFlipped) 7 - displayCol else displayCol
                val fileLetter = ('A' + actualCol).toString()
                val cx = borderWidth + displayCol * sqW + sqW / 2f

                // Top border letter
                val topY = borderHeight * 0.65f
                drawContext.canvas.nativeCanvas.drawText(fileLetter, cx, topY, textPaint)

                // Bottom border letter
                val bottomY = totalH - borderHeight * 0.35f
                drawContext.canvas.nativeCanvas.drawText(fileLetter, cx, bottomY, textPaint)
            }

            // Corner decorative rivets (Tournament Board Brass Accents)
            val rivetRadius = borderWidth * 0.22f
            val cornerOffsets = listOf(
                Offset(borderWidth * 0.5f, borderHeight * 0.5f),
                Offset(totalW - borderWidth * 0.5f, borderHeight * 0.5f),
                Offset(borderWidth * 0.5f, totalH - borderHeight * 0.5f),
                Offset(totalW - borderWidth * 0.5f, totalH - borderHeight * 0.5f)
            )
            for (pt in cornerOffsets) {
                drawCircle(BoardCoordinateMuted, rivetRadius, pt)
                drawCircle(BoardCoordinateText, rivetRadius * 0.6f, pt)
                drawCircle(Color.Black.copy(alpha = 0.5f), rivetRadius * 0.25f, pt)
            }

            // 4. Draw 8x8 Chessboard Squares (Red & Black Theme)
            for (displayRow in 0..7) {
                for (displayCol in 0..7) {
                    val actualRow = if (isFlipped) 7 - displayRow else displayRow
                    val actualCol = if (isFlipped) 7 - displayCol else displayCol
                    val isLightSquare = (actualRow + actualCol) % 2 == 0

                    val sqX = borderWidth + displayCol * sqW
                    val sqY = borderHeight + displayRow * sqH
                    val sqTopLeft = Offset(sqX, sqY)
                    val sqSize = Size(sqW, sqH)

                    // Draw base square
                    if (isLightSquare) {
                        // Velvet Crimson Red Square with subtle luxury gradient
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF9E1D2A),
                                    BoardSquareLight,
                                    Color(0xFF70131C)
                                ),
                                startY = sqY,
                                endY = sqY + sqH
                            ),
                            topLeft = sqTopLeft,
                            size = sqSize
                        )
                    } else {
                        // Obsidian Charcoal Black Square
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF22232C),
                                    BoardSquareDark,
                                    Color(0xFF121317)
                                ),
                                startY = sqY,
                                endY = sqY + sqH
                            ),
                            topLeft = sqTopLeft,
                            size = sqSize
                        )
                    }

                    // Last move highlight
                    val lastMove = gameState.lastMove
                    if (lastMove != null && (lastMove.from == Position(actualRow, actualCol) || lastMove.to == Position(actualRow, actualCol))) {
                        drawRect(
                            color = LastMoveHighlight,
                            topLeft = sqTopLeft,
                            size = sqSize
                        )
                    }

                    // Selected square highlight
                    if (selectedPosition == Position(actualRow, actualCol)) {
                        drawRect(
                            color = SelectedSquareGlow,
                            topLeft = sqTopLeft,
                            size = sqSize
                        )
                        drawRect(
                            color = GoldAccent,
                            topLeft = sqTopLeft,
                            size = sqSize,
                            style = Stroke(width = sqW * 0.06f)
                        )
                    }

                    // Check highlight for the King in check
                    if (gameState.isCheck) {
                        val piece = gameState.board[actualRow][actualCol]
                        if (piece != null && piece.type == PieceType.KING && piece.color == gameState.currentTurn) {
                            drawCircle(
                                color = KingInCheckGlow.copy(alpha = pulseAlpha),
                                radius = sqW * 0.45f,
                                center = Offset(sqX + sqW / 2f, sqY + sqH / 2f)
                            )
                        }
                    }

                    // Hint Move Highlight
                    if (hintMove != null && (hintMove.from == Position(actualRow, actualCol) || hintMove.to == Position(actualRow, actualCol))) {
                        drawRect(
                            color = HintMoveGlow.copy(alpha = 0.35f),
                            topLeft = sqTopLeft,
                            size = sqSize
                        )
                    }

                    // 5. Draw Piece if present
                    val piece = gameState.board[actualRow][actualCol]
                    if (piece != null) {
                        val piecePadding = sqW * 0.08f
                        val pieceSize = Size(sqW - piecePadding * 2f, sqH - piecePadding * 2f)
                        val pieceTopLeft = Offset(sqX + piecePadding, sqY + piecePadding)

                        ChessPieces.drawPiece(this, piece, pieceTopLeft, pieceSize)
                    }

                    // 6. Valid Move Indicators
                    val targetMove = validMoves.find { it.to == Position(actualRow, actualCol) }
                    if (targetMove != null) {
                        val center = Offset(sqX + sqW / 2f, sqY + sqH / 2f)
                        if (targetMove.capturedPiece != null || targetMove.isEnPassant) {
                            // Target capture ring around enemy piece
                            drawCircle(
                                color = CaptureRing,
                                radius = sqW * 0.42f,
                                center = center,
                                style = Stroke(width = sqW * 0.09f)
                            )
                        } else {
                            // Subtle luminous crimson dot for empty square move
                            drawCircle(
                                color = ValidMoveDot,
                                radius = sqW * 0.17f,
                                center = center
                            )
                        }
                    }
                }
            }

            // Draw hint arrow if present
            if (hintMove != null) {
                val fromDispRow = if (isFlipped) 7 - hintMove.from.row else hintMove.from.row
                val fromDispCol = if (isFlipped) 7 - hintMove.from.col else hintMove.from.col
                val toDispRow = if (isFlipped) 7 - hintMove.to.row else hintMove.to.row
                val toDispCol = if (isFlipped) 7 - hintMove.to.col else hintMove.to.col

                val start = Offset(
                    borderWidth + fromDispCol * sqW + sqW / 2f,
                    borderHeight + fromDispRow * sqH + sqH / 2f
                )
                val end = Offset(
                    borderWidth + toDispCol * sqW + sqW / 2f,
                    borderHeight + toDispRow * sqH + sqH / 2f
                )

                drawLine(
                    color = HintMoveGlow,
                    start = start,
                    end = end,
                    strokeWidth = sqW * 0.08f,
                    cap = StrokeCap.Round
                )
                drawCircle(HintMoveGlow, sqW * 0.12f, end)
            }
        }
    }
}
