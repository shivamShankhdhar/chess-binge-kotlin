package com.example.chessbinge.ui.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.chessbinge.model.Piece
import com.example.chessbinge.model.PieceColor
import com.example.chessbinge.model.PieceType
import com.example.chessbinge.ui.theme.BlackPieceDetail
import com.example.chessbinge.ui.theme.BlackPieceFill
import com.example.chessbinge.ui.theme.BlackPieceStroke
import com.example.chessbinge.ui.theme.WhitePieceFill
import com.example.chessbinge.ui.theme.WhitePieceShade
import com.example.chessbinge.ui.theme.WhitePieceStroke

object ChessPieces {

    fun drawPiece(
        drawScope: DrawScope,
        piece: Piece,
        topLeft: Offset,
        size: Size
    ) {
        val w = size.width
        val h = size.height
        val isWhite = piece.color == PieceColor.WHITE

        val fillColor = if (isWhite) WhitePieceFill else BlackPieceFill
        val strokeColor = if (isWhite) WhitePieceStroke else BlackPieceStroke
        val accentColor = if (isWhite) WhitePieceShade else BlackPieceDetail

        when (piece.type) {
            PieceType.PAWN -> drawPawn(drawScope, topLeft, w, h, fillColor, strokeColor, accentColor, isWhite)
            PieceType.KNIGHT -> drawKnight(drawScope, topLeft, w, h, fillColor, strokeColor, accentColor, isWhite)
            PieceType.BISHOP -> drawBishop(drawScope, topLeft, w, h, fillColor, strokeColor, accentColor, isWhite)
            PieceType.ROOK -> drawRook(drawScope, topLeft, w, h, fillColor, strokeColor, accentColor, isWhite)
            PieceType.QUEEN -> drawQueen(drawScope, topLeft, w, h, fillColor, strokeColor, accentColor, isWhite)
            PieceType.KING -> drawKing(drawScope, topLeft, w, h, fillColor, strokeColor, accentColor, isWhite)
        }
    }

    private fun drawPawn(
        scope: DrawScope,
        tl: Offset,
        w: Float,
        h: Float,
        fill: Color,
        stroke: Color,
        accent: Color,
        isWhite: Boolean
    ) {
        val cx = tl.x + w / 2f
        val strokeWidth = (w * 0.045f).coerceAtLeast(1.5f)

        // Base
        val basePath = Path().apply {
            moveTo(cx - w * 0.28f, tl.y + h * 0.88f)
            lineTo(cx + w * 0.28f, tl.y + h * 0.88f)
            lineTo(cx + w * 0.22f, tl.y + h * 0.78f)
            lineTo(cx - w * 0.22f, tl.y + h * 0.78f)
            close()
        }
        scope.drawPath(basePath, fill)
        scope.drawPath(basePath, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))

        // Body
        val bodyPath = Path().apply {
            moveTo(cx - w * 0.16f, tl.y + h * 0.78f)
            cubicTo(
                cx - w * 0.18f, tl.y + h * 0.65f,
                cx - w * 0.10f, tl.y + h * 0.50f,
                cx - w * 0.10f, tl.y + h * 0.45f
            )
            lineTo(cx + w * 0.10f, tl.y + h * 0.45f)
            cubicTo(
                cx + w * 0.10f, tl.y + h * 0.50f,
                cx + w * 0.18f, tl.y + h * 0.65f,
                cx + w * 0.16f, tl.y + h * 0.78f
            )
            close()
        }
        scope.drawPath(bodyPath, fill)
        scope.drawPath(bodyPath, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))

        // Neck collar ring
        val collarPath = Path().apply {
            addOval(Rect(cx - w * 0.14f, tl.y + h * 0.40f, cx + w * 0.14f, tl.y + h * 0.46f))
        }
        scope.drawPath(collarPath, accent)
        scope.drawPath(collarPath, stroke, style = Stroke(strokeWidth))

        // Head sphere
        val headRadius = w * 0.16f
        val headCenter = Offset(cx, tl.y + h * 0.28f)
        scope.drawCircle(fill, headRadius, headCenter)
        scope.drawCircle(stroke, headRadius, headCenter, style = Stroke(strokeWidth))

        // Highlight
        if (isWhite) {
            scope.drawCircle(Color.White.copy(alpha = 0.8f), headRadius * 0.35f, Offset(cx - headRadius * 0.35f, tl.y + h * 0.23f))
        }
    }

    private fun drawKnight(
        scope: DrawScope,
        tl: Offset,
        w: Float,
        h: Float,
        fill: Color,
        stroke: Color,
        accent: Color,
        isWhite: Boolean
    ) {
        val cx = tl.x + w / 2f
        val strokeWidth = (w * 0.045f).coerceAtLeast(1.5f)

        // Base
        val basePath = Path().apply {
            moveTo(cx - w * 0.30f, tl.y + h * 0.88f)
            lineTo(cx + w * 0.30f, tl.y + h * 0.88f)
            lineTo(cx + w * 0.24f, tl.y + h * 0.80f)
            lineTo(cx - w * 0.24f, tl.y + h * 0.80f)
            close()
        }
        scope.drawPath(basePath, fill)
        scope.drawPath(basePath, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))

        // Horse Head Silhouette
        val horsePath = Path().apply {
            moveTo(cx - w * 0.22f, tl.y + h * 0.80f)
            // Chest
            cubicTo(
                cx - w * 0.26f, tl.y + h * 0.65f,
                cx - w * 0.28f, tl.y + h * 0.50f,
                cx - w * 0.20f, tl.y + h * 0.38f
            )
            // Muzzle / Snout
            lineTo(cx - w * 0.32f, tl.y + h * 0.42f)
            lineTo(cx - w * 0.34f, tl.y + h * 0.34f)
            lineTo(cx - w * 0.18f, tl.y + h * 0.24f)
            // Ears
            lineTo(cx - w * 0.12f, tl.y + h * 0.14f)
            lineTo(cx - w * 0.04f, tl.y + h * 0.22f)
            lineTo(cx + w * 0.04f, tl.y + h * 0.16f)
            lineTo(cx + w * 0.08f, tl.y + h * 0.24f)
            // Mane back
            cubicTo(
                cx + w * 0.24f, tl.y + h * 0.38f,
                cx + w * 0.26f, tl.y + h * 0.60f,
                cx + w * 0.22f, tl.y + h * 0.80f
            )
            close()
        }
        scope.drawPath(horsePath, fill)
        scope.drawPath(horsePath, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))

        // Eye
        val eyePos = Offset(cx - w * 0.14f, tl.y + h * 0.30f)
        scope.drawCircle(stroke, w * 0.035f, eyePos)

        // Mane details
        val manePath = Path().apply {
            moveTo(cx + w * 0.04f, tl.y + h * 0.34f)
            lineTo(cx + w * 0.16f, tl.y + h * 0.42f)
            moveTo(cx + w * 0.08f, tl.y + h * 0.48f)
            lineTo(cx + w * 0.18f, tl.y + h * 0.56f)
            moveTo(cx + w * 0.10f, tl.y + h * 0.62f)
            lineTo(cx + w * 0.19f, tl.y + h * 0.70f)
        }
        scope.drawPath(manePath, stroke, style = Stroke(strokeWidth * 0.85f, cap = StrokeCap.Round))
    }

    private fun drawBishop(
        scope: DrawScope,
        tl: Offset,
        w: Float,
        h: Float,
        fill: Color,
        stroke: Color,
        accent: Color,
        isWhite: Boolean
    ) {
        val cx = tl.x + w / 2f
        val strokeWidth = (w * 0.045f).coerceAtLeast(1.5f)

        // Base
        val basePath = Path().apply {
            moveTo(cx - w * 0.28f, tl.y + h * 0.88f)
            lineTo(cx + w * 0.28f, tl.y + h * 0.88f)
            lineTo(cx + w * 0.22f, tl.y + h * 0.78f)
            lineTo(cx - w * 0.22f, tl.y + h * 0.78f)
            close()
        }
        scope.drawPath(basePath, fill)
        scope.drawPath(basePath, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))

        // Body
        val bodyPath = Path().apply {
            moveTo(cx - w * 0.18f, tl.y + h * 0.78f)
            cubicTo(
                cx - w * 0.18f, tl.y + h * 0.65f,
                cx - w * 0.12f, tl.y + h * 0.55f,
                cx - w * 0.14f, tl.y + h * 0.48f
            )
            lineTo(cx + w * 0.14f, tl.y + h * 0.48f)
            cubicTo(
                cx + w * 0.12f, tl.y + h * 0.55f,
                cx + w * 0.18f, tl.y + h * 0.65f,
                cx + w * 0.18f, tl.y + h * 0.78f
            )
            close()
        }
        scope.drawPath(bodyPath, fill)
        scope.drawPath(bodyPath, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))

        // Collar ring
        val ringPath = Path().apply {
            addOval(Rect(cx - w * 0.16f, tl.y + h * 0.45f, cx + w * 0.16f, tl.y + h * 0.50f))
        }
        scope.drawPath(ringPath, accent)
        scope.drawPath(ringPath, stroke, style = Stroke(strokeWidth))

        // Mitre head (oval pointed head)
        val mitrePath = Path().apply {
            moveTo(cx - w * 0.16f, tl.y + h * 0.44f)
            cubicTo(
                cx - w * 0.20f, tl.y + h * 0.32f,
                cx - w * 0.08f, tl.y + h * 0.20f,
                cx, tl.y + h * 0.16f
            )
            cubicTo(
                cx + w * 0.08f, tl.y + h * 0.20f,
                cx + w * 0.20f, tl.y + h * 0.32f,
                cx + w * 0.16f, tl.y + h * 0.44f
            )
            close()
        }
        scope.drawPath(mitrePath, fill)
        scope.drawPath(mitrePath, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))

        // Top cross / orb
        scope.drawCircle(accent, w * 0.045f, Offset(cx, tl.y + h * 0.15f))
        scope.drawCircle(stroke, w * 0.045f, Offset(cx, tl.y + h * 0.15f), style = Stroke(strokeWidth * 0.7f))

        // Mitre cut slash
        val cutPath = Path().apply {
            moveTo(cx - w * 0.02f, tl.y + h * 0.22f)
            lineTo(cx + w * 0.12f, tl.y + h * 0.32f)
        }
        scope.drawPath(cutPath, stroke, style = Stroke(strokeWidth * 1.1f, cap = StrokeCap.Round))
    }

    private fun drawRook(
        scope: DrawScope,
        tl: Offset,
        w: Float,
        h: Float,
        fill: Color,
        stroke: Color,
        accent: Color,
        isWhite: Boolean
    ) {
        val cx = tl.x + w / 2f
        val strokeWidth = (w * 0.045f).coerceAtLeast(1.5f)

        // Base
        val basePath = Path().apply {
            moveTo(cx - w * 0.30f, tl.y + h * 0.88f)
            lineTo(cx + w * 0.30f, tl.y + h * 0.88f)
            lineTo(cx + w * 0.24f, tl.y + h * 0.78f)
            lineTo(cx - w * 0.24f, tl.y + h * 0.78f)
            close()
        }
        scope.drawPath(basePath, fill)
        scope.drawPath(basePath, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))

        // Tower Body
        val towerPath = Path().apply {
            moveTo(cx - w * 0.20f, tl.y + h * 0.78f)
            lineTo(cx - w * 0.16f, tl.y + h * 0.40f)
            lineTo(cx + w * 0.16f, tl.y + h * 0.40f)
            lineTo(cx + w * 0.20f, tl.y + h * 0.78f)
            close()
        }
        scope.drawPath(towerPath, fill)
        scope.drawPath(towerPath, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))

        // Ledge cornice
        val ledgePath = Path().apply {
            addRect(Rect(cx - w * 0.25f, tl.y + h * 0.34f, cx + w * 0.25f, tl.y + h * 0.40f))
        }
        scope.drawPath(ledgePath, accent)
        scope.drawPath(ledgePath, stroke, style = Stroke(strokeWidth))

        // Battlements (3 Crenellations)
        val castleTopPath = Path().apply {
            moveTo(cx - w * 0.25f, tl.y + h * 0.34f)
            lineTo(cx - w * 0.25f, tl.y + h * 0.18f)
            lineTo(cx - w * 0.15f, tl.y + h * 0.18f)
            lineTo(cx - w * 0.15f, tl.y + h * 0.25f)
            lineTo(cx - w * 0.05f, tl.y + h * 0.25f)
            lineTo(cx - w * 0.05f, tl.y + h * 0.18f)
            lineTo(cx + w * 0.05f, tl.y + h * 0.18f)
            lineTo(cx + w * 0.05f, tl.y + h * 0.25f)
            lineTo(cx + w * 0.15f, tl.y + h * 0.25f)
            lineTo(cx + w * 0.15f, tl.y + h * 0.18f)
            lineTo(cx + w * 0.25f, tl.y + h * 0.18f)
            lineTo(cx + w * 0.25f, tl.y + h * 0.34f)
            close()
        }
        scope.drawPath(castleTopPath, fill)
        scope.drawPath(castleTopPath, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Miter))
    }

    private fun drawQueen(
        scope: DrawScope,
        tl: Offset,
        w: Float,
        h: Float,
        fill: Color,
        stroke: Color,
        accent: Color,
        isWhite: Boolean
    ) {
        val cx = tl.x + w / 2f
        val strokeWidth = (w * 0.045f).coerceAtLeast(1.5f)

        // Base
        val basePath = Path().apply {
            moveTo(cx - w * 0.32f, tl.y + h * 0.88f)
            lineTo(cx + w * 0.32f, tl.y + h * 0.88f)
            lineTo(cx + w * 0.24f, tl.y + h * 0.78f)
            lineTo(cx - w * 0.24f, tl.y + h * 0.78f)
            close()
        }
        scope.drawPath(basePath, fill)
        scope.drawPath(basePath, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))

        // Body / Robe
        val bodyPath = Path().apply {
            moveTo(cx - w * 0.20f, tl.y + h * 0.78f)
            cubicTo(
                cx - w * 0.18f, tl.y + h * 0.62f,
                cx - w * 0.10f, tl.y + h * 0.50f,
                cx - w * 0.15f, tl.y + h * 0.44f
            )
            lineTo(cx + w * 0.15f, tl.y + h * 0.44f)
            cubicTo(
                cx + w * 0.10f, tl.y + h * 0.50f,
                cx + w * 0.18f, tl.y + h * 0.62f,
                cx + w * 0.20f, tl.y + h * 0.78f
            )
            close()
        }
        scope.drawPath(bodyPath, fill)
        scope.drawPath(bodyPath, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))

        // Diadem band
        val bandPath = Path().apply {
            addOval(Rect(cx - w * 0.18f, tl.y + h * 0.40f, cx + w * 0.18f, tl.y + h * 0.46f))
        }
        scope.drawPath(bandPath, accent)
        scope.drawPath(bandPath, stroke, style = Stroke(strokeWidth))

        // Crown Spikes (5 points)
        val crownPath = Path().apply {
            moveTo(cx - w * 0.16f, tl.y + h * 0.42f)
            lineTo(cx - w * 0.28f, tl.y + h * 0.22f) // left outer
            lineTo(cx - w * 0.12f, tl.y + h * 0.32f)
            lineTo(cx - w * 0.14f, tl.y + h * 0.18f) // left inner
            lineTo(cx, tl.y + h * 0.28f)
            lineTo(cx, tl.y + h * 0.14f)             // center high
            lineTo(cx, tl.y + h * 0.28f)
            lineTo(cx + w * 0.14f, tl.y + h * 0.18f) // right inner
            lineTo(cx + w * 0.12f, tl.y + h * 0.32f)
            lineTo(cx + w * 0.28f, tl.y + h * 0.22f) // right outer
            lineTo(cx + w * 0.16f, tl.y + h * 0.42f)
            close()
        }
        scope.drawPath(crownPath, fill)
        scope.drawPath(crownPath, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))

        // Crown Pearls
        val pearlRadius = w * 0.038f
        scope.drawCircle(accent, pearlRadius, Offset(cx - w * 0.28f, tl.y + h * 0.22f))
        scope.drawCircle(accent, pearlRadius, Offset(cx - w * 0.14f, tl.y + h * 0.18f))
        scope.drawCircle(accent, pearlRadius * 1.2f, Offset(cx, tl.y + h * 0.14f))
        scope.drawCircle(accent, pearlRadius, Offset(cx + w * 0.14f, tl.y + h * 0.18f))
        scope.drawCircle(accent, pearlRadius, Offset(cx + w * 0.28f, tl.y + h * 0.22f))

        scope.drawCircle(stroke, pearlRadius, Offset(cx - w * 0.28f, tl.y + h * 0.22f), style = Stroke(strokeWidth * 0.7f))
        scope.drawCircle(stroke, pearlRadius, Offset(cx - w * 0.14f, tl.y + h * 0.18f), style = Stroke(strokeWidth * 0.7f))
        scope.drawCircle(stroke, pearlRadius * 1.2f, Offset(cx, tl.y + h * 0.14f), style = Stroke(strokeWidth * 0.7f))
        scope.drawCircle(stroke, pearlRadius, Offset(cx + w * 0.14f, tl.y + h * 0.18f), style = Stroke(strokeWidth * 0.7f))
        scope.drawCircle(stroke, pearlRadius, Offset(cx + w * 0.28f, tl.y + h * 0.22f), style = Stroke(strokeWidth * 0.7f))
    }

    private fun drawKing(
        scope: DrawScope,
        tl: Offset,
        w: Float,
        h: Float,
        fill: Color,
        stroke: Color,
        accent: Color,
        isWhite: Boolean
    ) {
        val cx = tl.x + w / 2f
        val strokeWidth = (w * 0.045f).coerceAtLeast(1.5f)

        // Base
        val basePath = Path().apply {
            moveTo(cx - w * 0.32f, tl.y + h * 0.88f)
            lineTo(cx + w * 0.32f, tl.y + h * 0.88f)
            lineTo(cx + w * 0.24f, tl.y + h * 0.78f)
            lineTo(cx - w * 0.24f, tl.y + h * 0.78f)
            close()
        }
        scope.drawPath(basePath, fill)
        scope.drawPath(basePath, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))

        // Body
        val bodyPath = Path().apply {
            moveTo(cx - w * 0.20f, tl.y + h * 0.78f)
            cubicTo(
                cx - w * 0.22f, tl.y + h * 0.62f,
                cx - w * 0.12f, tl.y + h * 0.50f,
                cx - w * 0.18f, tl.y + h * 0.44f
            )
            lineTo(cx + w * 0.18f, tl.y + h * 0.44f)
            cubicTo(
                cx + w * 0.12f, tl.y + h * 0.50f,
                cx + w * 0.22f, tl.y + h * 0.62f,
                cx + w * 0.20f, tl.y + h * 0.78f
            )
            close()
        }
        scope.drawPath(bodyPath, fill)
        scope.drawPath(bodyPath, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))

        // Royal Circlet
        val circletPath = Path().apply {
            addOval(Rect(cx - w * 0.20f, tl.y + h * 0.40f, cx + w * 0.20f, tl.y + h * 0.46f))
        }
        scope.drawPath(circletPath, accent)
        scope.drawPath(circletPath, stroke, style = Stroke(strokeWidth))

        // King's Arched Crown Dome
        val crownPath = Path().apply {
            moveTo(cx - w * 0.18f, tl.y + h * 0.42f)
            cubicTo(
                cx - w * 0.24f, tl.y + h * 0.30f,
                cx - w * 0.12f, tl.y + h * 0.22f,
                cx, tl.y + h * 0.22f
            )
            cubicTo(
                cx + w * 0.12f, tl.y + h * 0.22f,
                cx + w * 0.24f, tl.y + h * 0.30f,
                cx + w * 0.18f, tl.y + h * 0.42f
            )
            close()
        }
        scope.drawPath(crownPath, fill)
        scope.drawPath(crownPath, stroke, style = Stroke(strokeWidth, join = StrokeJoin.Round))

        // Center Rib
        val ribPath = Path().apply {
            moveTo(cx, tl.y + h * 0.22f)
            lineTo(cx, tl.y + h * 0.42f)
        }
        scope.drawPath(ribPath, stroke, style = Stroke(strokeWidth * 0.8f))

        // King Cross Finial
        val crossH = h * 0.12f
        val crossW = w * 0.14f
        val crossTopY = tl.y + h * 0.10f

        // Vertical bar
        val vBar = Path().apply {
            moveTo(cx, crossTopY)
            lineTo(cx, crossTopY + crossH)
        }
        scope.drawPath(vBar, accent, style = Stroke(strokeWidth * 1.5f, cap = StrokeCap.Square))
        scope.drawPath(vBar, stroke, style = Stroke(strokeWidth * 0.9f, cap = StrokeCap.Square))

        // Horizontal bar
        val hBar = Path().apply {
            moveTo(cx - crossW / 2f, crossTopY + crossH * 0.35f)
            lineTo(cx + crossW / 2f, crossTopY + crossH * 0.35f)
        }
        scope.drawPath(hBar, accent, style = Stroke(strokeWidth * 1.5f, cap = StrokeCap.Square))
        scope.drawPath(hBar, stroke, style = Stroke(strokeWidth * 0.9f, cap = StrokeCap.Square))
    }
}
