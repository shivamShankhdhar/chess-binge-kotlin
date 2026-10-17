package com.example.chessbinge.model

data class GameState(
    val board: Array<Array<Piece?>> = Array(8) { Array(8) { null } },
    val currentTurn: PieceColor = PieceColor.WHITE,
    val moveHistory: List<Move> = emptyList(),
    val capturedWhite: List<Piece> = emptyList(), // White pieces lost
    val capturedBlack: List<Piece> = emptyList(), // Black pieces lost
    val enPassantTarget: Position? = null,
    val halfMoveClock: Int = 0,
    val fullMoveNumber: Int = 1,
    val isCheck: Boolean = false,
    val isCheckmate: Boolean = false,
    val isStalemate: Boolean = false,
    val isDraw: Boolean = false,
    val drawReason: String? = null,
    val isGameOver: Boolean = false,
    val winner: PieceColor? = null,
    val lastMove: Move? = null,
    val whiteTimeRemainingMs: Long = 0L,
    val blackTimeRemainingMs: Long = 0L,
    val pendingPromotion: Pair<Position, Position>? = null // (from, to) awaiting piece selection
) {
    fun pieceAt(pos: Position): Piece? = board[pos.row][pos.col]
    fun pieceAt(row: Int, col: Int): Piece? = if (row in 0..7 && col in 0..7) board[row][col] else null

    fun copyBoard(): Array<Array<Piece?>> = Array(8) { r -> Array(8) { c -> board[r][c] } }

    /** Material advantage difference for White (+ is White advantage, - is Black advantage) */
    val materialAdvantage: Int
        get() {
            var score = 0
            for (r in 0..7) {
                for (c in 0..7) {
                    val p = board[r][c] ?: continue
                    val v = p.type.value
                    if (p.color == PieceColor.WHITE) score += v else score -= v
                }
            }
            return score / 100 // Normalized in pawns
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameState
        if (!board.contentDeepEquals(other.board)) return false
        if (currentTurn != other.currentTurn) return false
        if (moveHistory != other.moveHistory) return false
        if (enPassantTarget != other.enPassantTarget) return false
        if (isCheck != other.isCheck) return false
        if (isGameOver != other.isGameOver) return false
        if (winner != other.winner) return false
        if (whiteTimeRemainingMs != other.whiteTimeRemainingMs) return false
        if (blackTimeRemainingMs != other.blackTimeRemainingMs) return false
        if (pendingPromotion != other.pendingPromotion) return false
        if (capturedWhite != other.capturedWhite) return false
        if (capturedBlack != other.capturedBlack) return false
        return true
    }

    override fun hashCode(): Int {
        var result = board.contentDeepHashCode()
        result = 31 * result + currentTurn.hashCode()
        result = 31 * result + moveHistory.hashCode()
        result = 31 * result + whiteTimeRemainingMs.hashCode()
        result = 31 * result + blackTimeRemainingMs.hashCode()
        return result
    }
}

data class Puzzle(
    val id: String,
    val title: String,
    val theme: String,
    val difficulty: String,
    val playerColor: PieceColor,
    val fen: String,
    val solutionMoves: List<Pair<String, String>>, // list of (fromAlg, toAlg)
    val description: String
)
