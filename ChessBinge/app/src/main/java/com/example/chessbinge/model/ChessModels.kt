package com.example.chessbinge.model

enum class PieceColor {
    WHITE,
    BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(val symbol: String, val value: Int) {
    PAWN("P", 100),
    KNIGHT("N", 320),
    BISHOP("B", 330),
    ROOK("R", 500),
    QUEEN("Q", 900),
    KING("K", 20000)
}

data class Position(val row: Int, val col: Int) {
    init {
        require(row in 0..7 && col in 0..7) { "Invalid board position: ($row, $col)" }
    }

    /**
     * Algebraic coordinate: col 0->a ... 7->h; row 0->8 ... 7->1
     */
    val algebraic: String
        get() = "${('a' + col)}${8 - row}"

    companion object {
        fun fromAlgebraic(s: String): Position {
            require(s.length == 2) { "Invalid algebraic notation: $s" }
            val col = s[0].lowercaseChar() - 'a'
            val rank = s[1].digitToInt()
            val row = 8 - rank
            return Position(row, col)
        }
    }
}

data class Piece(
    val id: String,
    val type: PieceType,
    val color: PieceColor,
    val hasMoved: Boolean = false
) {
    fun copyWithMoved(): Piece = copy(hasMoved = true)
}
