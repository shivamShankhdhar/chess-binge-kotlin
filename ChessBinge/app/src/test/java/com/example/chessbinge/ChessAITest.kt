package com.example.chessbinge

import com.example.chessbinge.engine.ChessAI
import com.example.chessbinge.engine.ChessEngine
import com.example.chessbinge.model.AIDifficulty
import com.example.chessbinge.model.PieceColor
import com.example.chessbinge.model.Position
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessAITest {

    @Test
    fun testAIFindsMateInOne() = runBlocking {
        // Scholar's Mate setup: White queen on h5, bishop on c4, black king on e8 with f7 unprotected
        // FEN: r1bqkb1r/pppp1ppp/2n5/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 0 1
        val fen = "r1bqkb1r/pppp1ppp/2n5/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 0 1"
        val state = ChessEngine.fromFen(fen)

        val bestMove = ChessAI.getBestMove(state, AIDifficulty.HARD)
        assertNotNull(bestMove)

        // Best move should be Qxf7# (h5 to f7)
        assertEquals(Position.fromAlgebraic("h5"), bestMove!!.from)
        assertEquals(Position.fromAlgebraic("f7"), bestMove.to)

        val nextState = ChessEngine.makeMove(state, bestMove)
        assertTrue("Move should result in checkmate", nextState.isCheckmate)
        assertEquals(PieceColor.WHITE, nextState.winner)
    }

    @Test
    fun testAICapturesHangingQueen() = runBlocking {
        // Black queen on e4 hanging with white rook on e1
        // FEN: 4k3/8/8/8/4q3/8/8/4R1K1 w - - 0 1
        val fen = "4k3/8/8/8/4q3/8/8/4R1K1 w - - 0 1"
        val state = ChessEngine.fromFen(fen)

        val bestMove = ChessAI.getBestMove(state, AIDifficulty.HARD)
        assertNotNull(bestMove)
        assertEquals(Position.fromAlgebraic("e1"), bestMove!!.from)
        assertEquals(Position.fromAlgebraic("e4"), bestMove.to)
    }
}
