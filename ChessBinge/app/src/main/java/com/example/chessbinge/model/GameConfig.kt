package com.example.chessbinge.model

data class Move(
    val from: Position,
    val to: Position,
    val piece: Piece,
    val capturedPiece: Piece? = null,
    val isEnPassant: Boolean = false,
    val isCastling: Boolean = false,
    val promotedTo: PieceType? = null,
    val san: String = ""
)

enum class GameMode(val displayName: String, val subtitle: String) {
    VS_AI("Play vs AI", "Challenge our tactical chess engine"),
    PASS_AND_PLAY("Pass & Play", "2 Players sharing one screen"),
    TACTICS_PUZZLE("Tactics & Puzzles", "Sharpen your tactical vision")
}

enum class AIDifficulty(val displayName: String, val depth: Int, val description: String) {
    EASY("Novice", 1, "Casual play with fun blunders"),
    MEDIUM("Tactician", 2, "Strong positional awareness"),
    HARD("Grandmaster", 3, "Deep alpha-beta tactical evaluation")
}

enum class TimeControl(val displayName: String, val initialSeconds: Int, val incrementSeconds: Int) {
    UNTIMED("No Clock", 0, 0),
    BLITZ_3("3 min | +2s", 180, 2),
    BLITZ_5("5 min", 300, 0),
    RAPID_10("10 min | +5s", 600, 5),
    CLASSICAL_15("15 min | +10s", 900, 10)
}

data class GameStats(
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val puzzlesSolved: Int = 0
)
