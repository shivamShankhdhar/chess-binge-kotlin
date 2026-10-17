package com.example.chessbinge

import com.example.chessbinge.engine.ChessEngine
import com.example.chessbinge.model.Move
import com.example.chessbinge.model.PieceColor
import com.example.chessbinge.model.PieceType
import com.example.chessbinge.model.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessEngineTest {

    @Test
    fun testInitialState() {
        val state = ChessEngine.createInitialState()
        assertEquals(PieceColor.WHITE, state.currentTurn)
        assertFalse(state.isCheck)
        assertFalse(state.isGameOver)

        // 32 pieces on board
        var count = 0
        for (r in 0..7) {
            for (c in 0..7) {
                if (state.board[r][c] != null) count++
            }
        }
        assertEquals(32, count)

        // Kings in correct positions
        val whiteKing = state.pieceAt(7, 4)
        assertEquals(PieceType.KING, whiteKing?.type)
        assertEquals(PieceColor.WHITE, whiteKing?.color)

        val blackKing = state.pieceAt(0, 4)
        assertEquals(PieceType.KING, blackKing?.type)
        assertEquals(PieceColor.BLACK, blackKing?.color)

        // Legal moves for initial state: 16 pawn moves (8 single + 8 double) + 4 knight moves = 20 legal moves
        val legalMoves = ChessEngine.generateLegalMoves(state)
        assertEquals(20, legalMoves.size)
    }

    @Test
    fun testPawnAdvanceAndSan() {
        var state = ChessEngine.createInitialState()

        // 1. e4 (from e2: row 6, col 4 to e4: row 4, col 4)
        val e2 = Position(6, 4)
        val e4 = Position(4, 4)
        val move = ChessEngine.getLegalMovesForPosition(state, e2).find { it.to == e4 }
        assertNotNull(move)

        state = ChessEngine.makeMove(state, move!!)
        assertEquals("e4", state.lastMove?.san)
        assertEquals(PieceColor.BLACK, state.currentTurn)
        assertEquals(Position(5, 4), state.enPassantTarget) // e3 is en passant target
    }

    @Test
    fun testEnPassantCapture() {
        // Setup position where White pawn is on e5, Black plays d5
        // FEN: rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2
        val fen = "rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2"
        val state = ChessEngine.fromFen(fen)

        val e5 = Position(3, 4)
        val d6 = Position(2, 3)

        val epMove = ChessEngine.getLegalMovesForPosition(state, e5).find { it.to == d6 && it.isEnPassant }
        assertNotNull("En passant move should be generated", epMove)

        val nextState = ChessEngine.makeMove(state, epMove!!)
        assertEquals("exd6", nextState.lastMove?.san)
        // Black pawn at d5 (row 3, col 3) must be removed
        assertNull("Black pawn on d5 should be removed", nextState.pieceAt(3, 3))
        assertEquals(1, nextState.capturedBlack.size)
        assertEquals(PieceType.PAWN, nextState.capturedBlack[0].type)
    }

    @Test
    fun testKingsideCastling() {
        // Position with empty f1 and g1 squares, king and rook unmoved
        // FEN: r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4
        val fen = "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4"
        val state = ChessEngine.fromFen(fen)

        val e1 = Position(7, 4)
        val g1 = Position(7, 6)

        // White king must be able to castle kingside
        val castleMove = ChessEngine.getLegalMovesForPosition(state, e1).find { it.to == g1 && it.isCastling }
        assertNotNull("Kingside castling should be legal", castleMove)

        val nextState = ChessEngine.makeMove(state, castleMove!!)
        assertEquals("O-O", nextState.lastMove?.san)
        assertEquals(PieceType.KING, nextState.pieceAt(7, 6)?.type)
        assertEquals(PieceType.ROOK, nextState.pieceAt(7, 5)?.type) // Rook moved to f1
        assertNull(nextState.pieceAt(7, 7)) // h1 empty
    }

    @Test
    fun testFoolsMate() {
        // 1. f3 e5 2. g4 Qh4# (Fool's Mate)
        var state = ChessEngine.createInitialState()

        // 1. f3
        val f3Move = ChessEngine.getLegalMovesForPosition(state, Position.fromAlgebraic("f2")).find { it.to == Position.fromAlgebraic("f3") }!!
        state = ChessEngine.makeMove(state, f3Move)

        // 1... e5
        val e5Move = ChessEngine.getLegalMovesForPosition(state, Position.fromAlgebraic("e7")).find { it.to == Position.fromAlgebraic("e5") }!!
        state = ChessEngine.makeMove(state, e5Move)

        // 2. g4
        val g4Move = ChessEngine.getLegalMovesForPosition(state, Position.fromAlgebraic("g2")).find { it.to == Position.fromAlgebraic("g4") }!!
        state = ChessEngine.makeMove(state, g4Move)

        // 2... Qh4#
        val qh4Move = ChessEngine.getLegalMovesForPosition(state, Position.fromAlgebraic("d8")).find { it.to == Position.fromAlgebraic("h4") }!!
        state = ChessEngine.makeMove(state, qh4Move)

        assertTrue("White should be in check", state.isCheck)
        assertTrue("Game should be over by checkmate", state.isCheckmate)
        assertEquals(PieceColor.BLACK, state.winner)
        assertEquals("Qh4#", state.lastMove?.san)
    }

    @Test
    fun testScholarsMate() {
        // 1. e4 e5 2. Bc4 Nc6 3. Qh5 Nf6 4. Qxf7#
        var state = ChessEngine.createInitialState()

        fun play(from: String, to: String) {
            val move = ChessEngine.getLegalMovesForPosition(state, Position.fromAlgebraic(from)).find { it.to == Position.fromAlgebraic(to) }!!
            state = ChessEngine.makeMove(state, move)
        }

        play("e2", "e4")
        play("e7", "e5")
        play("f1", "c4")
        play("b8", "c6")
        play("d1", "h5")
        play("g8", "f6")
        play("h5", "f7")

        assertTrue("King should be in check", state.isCheck)
        assertTrue("Game should be checkmate", state.isCheckmate)
        assertEquals(PieceColor.WHITE, state.winner)
        assertEquals("Qxf7#", state.lastMove?.san)
    }

    @Test
    fun testStalemate() {
        // Classic stalemate position: Black king on a8, White king on c7, White queen on b6 -> Black to move, stalemate
        // FEN: k7/2K5/1Q6/8/8/8/8/8 b - - 0 1
        val fen = "k7/2K5/1Q6/8/8/8/8/8 b - - 0 1"
        val state = ChessEngine.fromFen(fen)

        assertFalse("King is not in check", state.isCheck)
        assertTrue("Game is stalemate", state.isStalemate)
        assertTrue("Game is over", state.isGameOver)
        assertNull("No winner in stalemate", state.winner)
    }

    @Test
    fun testPawnPromotion() {
        // White pawn on a7 about to promote to a8
        // FEN: 8/P7/8/8/8/8/k7/4K3 w - - 0 1
        val fen = "8/P7/8/8/8/8/k7/4K3 w - - 0 1"
        val state = ChessEngine.fromFen(fen)

        val a7 = Position.fromAlgebraic("a7")
        val a8 = Position.fromAlgebraic("a8")

        val promoMoves = ChessEngine.getLegalMovesForPosition(state, a7).filter { it.to == a8 }
        assertEquals(4, promoMoves.size) // Queen, Rook, Bishop, Knight

        val queenPromo = promoMoves.find { it.promotedTo == PieceType.QUEEN }!!
        val nextState = ChessEngine.makeMove(state, queenPromo)

        assertEquals("a8=Q+", nextState.lastMove?.san)
        assertEquals(PieceType.QUEEN, nextState.pieceAt(a8)?.type)
    }

    @Test
    fun testInsufficientMaterial() {
        // King vs King
        val kvk = "8/8/8/4k3/8/8/8/4K3 w - - 0 1"
        val stateKvk = ChessEngine.fromFen(kvk)
        assertTrue(ChessEngine.isInsufficientMaterial(stateKvk.board))

        // King + Knight vs King
        val kvkn = "8/8/8/4k3/8/8/5N2/4K3 w - - 0 1"
        val stateKvkn = ChessEngine.fromFen(kvkn)
        assertTrue(ChessEngine.isInsufficientMaterial(stateKvkn.board))

        // King + Pawn vs King (NOT insufficient)
        val kvkp = "8/8/8/4k3/8/8/5P2/4K3 w - - 0 1"
        val stateKvkp = ChessEngine.fromFen(kvkp)
        assertFalse(ChessEngine.isInsufficientMaterial(stateKvkp.board))
    }
}
