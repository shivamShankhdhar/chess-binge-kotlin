package com.example.chessbinge.engine

import com.example.chessbinge.model.PieceColor
import com.example.chessbinge.model.Puzzle

object PuzzleRepository {

    val puzzles: List<Puzzle> = listOf(
        Puzzle(
            id = "puz_1",
            title = "Scholar's Nemesis",
            theme = "Mate in 1",
            difficulty = "Easy",
            playerColor = PieceColor.WHITE,
            fen = "r1bqkb1r/pppp1ppp/2n5/4p3/2B1n3/5Q2/PPPP1PPP/RNB1K1NR w KQkq - 0 1",
            solutionMoves = listOf("f3" to "f7"),
            description = "White delivers an unstoppable blow on the f7 weakness."
        ),
        Puzzle(
            id = "puz_2",
            title = "Back-Rank Annihilation",
            theme = "Back Rank Mate",
            difficulty = "Easy",
            playerColor = PieceColor.WHITE,
            fen = "3r2k1/5ppp/8/8/8/8/4QPPP/6K1 w - - 0 1",
            solutionMoves = listOf("e2" to "e8", "d8" to "e8"),
            description = "The enemy king is trapped behind its pawns! Sacrifice the queen to clear the rank."
        ),
        Puzzle(
            id = "puz_3",
            title = "Royal Knight Fork",
            theme = "Fork",
            difficulty = "Medium",
            playerColor = PieceColor.WHITE,
            fen = "r1b1k2r/pp3ppp/2n5/1B1p4/3N4/8/PPP2PPP/R3K2R w KQkq - 0 1",
            solutionMoves = listOf("d4" to "c6"),
            description = "Exploit the pinned piece to gain devastating material."
        ),
        Puzzle(
            id = "puz_4",
            title = "Smothered Mate Legacy",
            theme = "Smothered Mate",
            difficulty = "Hard",
            playerColor = PieceColor.WHITE,
            fen = "6k1/5ppp/8/8/8/5N2/5PPP/4Q1K1 w - - 0 1",
            solutionMoves = listOf("e1" to "e8"),
            description = "Deliver checkmate on the back rank."
        ),
        Puzzle(
            id = "puz_5",
            title = "Anastasia's Trap",
            theme = "Checkmate Attack",
            difficulty = "Medium",
            playerColor = PieceColor.WHITE,
            fen = "5rk1/1p3ppp/p7/3N4/8/8/PPP2PPP/4R1K1 w - - 0 1",
            solutionMoves = listOf("d5" to "e7"),
            description = "Infiltrate with the Knight to paralyze the enemy king."
        ),
        Puzzle(
            id = "puz_6",
            title = "Queen Infiltration",
            theme = "Mate in 1",
            difficulty = "Easy",
            playerColor = PieceColor.WHITE,
            fen = "rnbqkbnr/ppppp2p/5p2/6p1/4P3/3P4/PPP2PPP/RNBQKBNR w KQkq - 0 1",
            solutionMoves = listOf("d1" to "h5"),
            description = "Fool's mate pattern! Punish Black for opening the king's diagonal."
        ),
        Puzzle(
            id = "puz_7",
            title = "Rook Lift Decapitation",
            theme = "Mate in 2",
            difficulty = "Medium",
            playerColor = PieceColor.WHITE,
            fen = "r4rk1/5ppp/8/8/8/1R6/5PPP/5RK1 w - - 0 1",
            solutionMoves = listOf("b3" to "b8"),
            description = "Double rooks on the back rank overwhelm the defender."
        ),
        Puzzle(
            id = "puz_8",
            title = "Greek Gift Strike",
            theme = "Sacrifice & Mate",
            difficulty = "Hard",
            playerColor = PieceColor.WHITE,
            fen = "r1bq1rk1/ppp2ppp/2n5/3pP3/1b1P4/2NB1N2/PP3PPP/R1BQK2R w KQ - 0 1",
            solutionMoves = listOf("d3" to "h7"),
            description = "The classic bishop sacrifice against the h7 square breaks Black's castled fortress."
        )
    )
}
