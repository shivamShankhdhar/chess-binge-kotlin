package com.example.chessbinge.engine

import com.example.chessbinge.model.AIDifficulty
import com.example.chessbinge.model.GameState
import com.example.chessbinge.model.Move
import com.example.chessbinge.model.PieceColor
import com.example.chessbinge.model.PieceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

object ChessAI {

    // Piece-Square Tables (from White's perspective; flipped for Black)
    private val PAWN_TABLE = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
         5,  5, 10, 25, 25, 10,  5,  5,
         0,  0,  0, 20, 20,  0,  0,  0,
         5, -5,-10,  0,  0,-10, -5,  5,
         5, 10, 10,-20,-20, 10, 10,  5,
         0,  0,  0,  0,  0,  0,  0,  0
    )

    private val KNIGHT_TABLE = intArrayOf(
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    )

    private val BISHOP_TABLE = intArrayOf(
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    )

    private val ROOK_TABLE = intArrayOf(
         0,  0,  0,  0,  0,  0,  0,  0,
         5, 10, 10, 10, 10, 10, 10,  5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
         0,  0,  0,  5,  5,  0,  0,  0
    )

    private val QUEEN_TABLE = intArrayOf(
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
         -5,  0,  5,  5,  5,  5,  0, -5,
          0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    )

    private val KING_MIDDLE_TABLE = intArrayOf(
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
         20, 20,  0,  0,  0,  0, 20, 20,
         20, 30, 10,  0,  0, 10, 30, 20
    )

    suspend fun getBestMove(state: GameState, difficulty: AIDifficulty): Move? = withContext(Dispatchers.Default) {
        val legalMoves = ChessEngine.generateLegalMoves(state)
        if (legalMoves.isEmpty()) return@withContext null

        when (difficulty) {
            AIDifficulty.EASY -> {
                // Easy: 70% chance heuristic / 30% random blunder
                if (Random.nextFloat() < 0.3f) {
                    legalMoves.random()
                } else {
                    // Slight preference for captures and checks
                    legalMoves.maxByOrNull { move ->
                        var score = 0
                        if (move.capturedPiece != null) score += move.capturedPiece.type.value
                        if (move.promotedTo != null) score += 800
                        score += Random.nextInt(50)
                        score
                    } ?: legalMoves.random()
                }
            }

            AIDifficulty.MEDIUM -> {
                // Medium: Depth 2 Minimax
                findMinimaxMove(state, depth = 2, isHard = false)
            }

            AIDifficulty.HARD -> {
                // Hard: Depth 3 Minimax with move ordering
                findMinimaxMove(state, depth = 3, isHard = true)
            }
        }
    }

    private fun findMinimaxMove(state: GameState, depth: Int, isHard: Boolean): Move? {
        val moves = ChessEngine.generateLegalMoves(state)
        if (moves.isEmpty()) return null

        val isMaximizing = state.currentTurn == PieceColor.WHITE
        var bestMove: Move? = null
        var bestScore = if (isMaximizing) -1_000_000 else 1_000_000
        var alpha = -1_000_000
        var beta = 1_000_000

        // Order moves (captures first)
        val orderedMoves = moves.sortedByDescending { move ->
            var score = 0
            if (move.capturedPiece != null) {
                score += 1000 + move.capturedPiece.type.value - move.piece.type.value / 10
            }
            if (move.promotedTo != null) score += 900
            score
        }

        for (move in orderedMoves) {
            val nextState = ChessEngine.makeMove(state, move)
            val score = minimax(nextState, depth - 1, alpha, beta, !isMaximizing)

            if (isMaximizing) {
                if (score > bestScore || (score == bestScore && Random.nextBoolean())) {
                    bestScore = score
                    bestMove = move
                }
                alpha = maxOf(alpha, bestScore)
            } else {
                if (score < bestScore || (score == bestScore && Random.nextBoolean())) {
                    bestScore = score
                    bestMove = move
                }
                beta = minOf(beta, bestScore)
            }
            if (beta <= alpha) break
        }

        return bestMove ?: moves.first()
    }

    private fun minimax(
        state: GameState,
        depth: Int,
        alphaInit: Int,
        betaInit: Int,
        isMaximizing: Boolean
    ): Int {
        if (state.isGameOver) {
            if (state.isCheckmate) {
                return if (state.winner == PieceColor.WHITE) 500_000 + depth * 1000 else -500_000 - depth * 1000
            }
            return 0 // Draw
        }

        if (depth == 0) {
            return evaluateBoard(state)
        }

        var alpha = alphaInit
        var beta = betaInit
        val moves = ChessEngine.generateLegalMoves(state)
        if (moves.isEmpty()) {
            return evaluateBoard(state)
        }

        if (isMaximizing) {
            var maxEval = -1_000_000
            for (move in moves) {
                val nextState = ChessEngine.makeMove(state, move)
                val eval = minimax(nextState, depth - 1, alpha, beta, false)
                maxEval = maxOf(maxEval, eval)
                alpha = maxOf(alpha, eval)
                if (beta <= alpha) break
            }
            return maxEval
        } else {
            var minEval = 1_000_000
            for (move in moves) {
                val nextState = ChessEngine.makeMove(state, move)
                val eval = minimax(nextState, depth - 1, alpha, beta, true)
                minEval = minOf(minEval, eval)
                beta = minOf(beta, eval)
                if (beta <= alpha) break
            }
            return minEval
        }
    }

    fun evaluateBoard(state: GameState): Int {
        var score = 0
        val board = state.board

        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c] ?: continue
                val isWhite = piece.color == PieceColor.WHITE
                val value = piece.type.value
                val tableIndex = if (isWhite) r * 8 + c else (7 - r) * 8 + c

                val posBonus = when (piece.type) {
                    PieceType.PAWN -> PAWN_TABLE[tableIndex]
                    PieceType.KNIGHT -> KNIGHT_TABLE[tableIndex]
                    PieceType.BISHOP -> BISHOP_TABLE[tableIndex]
                    PieceType.ROOK -> ROOK_TABLE[tableIndex]
                    PieceType.QUEEN -> QUEEN_TABLE[tableIndex]
                    PieceType.KING -> KING_MIDDLE_TABLE[tableIndex]
                }

                val totalPieceScore = value + posBonus
                if (isWhite) score += totalPieceScore else score -= totalPieceScore
            }
        }

        return score
    }
}
