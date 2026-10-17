package com.example.chessbinge.engine

import com.example.chessbinge.model.GameState
import com.example.chessbinge.model.Move
import com.example.chessbinge.model.Piece
import com.example.chessbinge.model.PieceColor
import com.example.chessbinge.model.PieceType
import com.example.chessbinge.model.Position

object ChessEngine {

    fun createInitialState(): GameState {
        val board = Array(8) { Array<Piece?>(8) { null } }

        // Black major pieces (Row 0, Rank 8)
        val blackBackRow = listOf(
            PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
            PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
        )
        for (c in 0..7) {
            board[0][c] = Piece("b_${blackBackRow[c]}_$c", blackBackRow[c], PieceColor.BLACK)
        }
        // Black pawns (Row 1, Rank 7)
        for (c in 0..7) {
            board[1][c] = Piece("b_P_$c", PieceType.PAWN, PieceColor.BLACK)
        }

        // White pawns (Row 6, Rank 2)
        for (c in 0..7) {
            board[6][c] = Piece("w_P_$c", PieceType.PAWN, PieceColor.WHITE)
        }
        // White major pieces (Row 7, Rank 1)
        val whiteBackRow = listOf(
            PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
            PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
        )
        for (c in 0..7) {
            board[7][c] = Piece("w_${whiteBackRow[c]}_$c", whiteBackRow[c], PieceColor.WHITE)
        }

        return GameState(
            board = board,
            currentTurn = PieceColor.WHITE
        )
    }

    /**
     * Parses a FEN (Forsyth–Edwards Notation) string into a GameState.
     */
    fun fromFen(fen: String): GameState {
        val parts = fen.trim().split(" ")
        val board = Array(8) { Array<Piece?>(8) { null } }
        val rows = parts[0].split("/")

        val castlingRights = parts.getOrNull(2) ?: "-"

        for (r in 0..7) {
            var c = 0
            for (char in rows[r]) {
                if (char.isDigit()) {
                    c += char.digitToInt()
                } else {
                    val color = if (char.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
                    val type = when (char.lowercaseChar()) {
                        'p' -> PieceType.PAWN
                        'n' -> PieceType.KNIGHT
                        'b' -> PieceType.BISHOP
                        'r' -> PieceType.ROOK
                        'q' -> PieceType.QUEEN
                        'k' -> PieceType.KING
                        else -> PieceType.PAWN
                    }

                    val hasMoved = when {
                        type == PieceType.KING && color == PieceColor.WHITE && r == 7 && c == 4 ->
                            !castlingRights.contains('K') && !castlingRights.contains('Q')
                        type == PieceType.ROOK && color == PieceColor.WHITE && r == 7 && c == 7 ->
                            !castlingRights.contains('K')
                        type == PieceType.ROOK && color == PieceColor.WHITE && r == 7 && c == 0 ->
                            !castlingRights.contains('Q')
                        type == PieceType.KING && color == PieceColor.BLACK && r == 0 && c == 4 ->
                            !castlingRights.contains('k') && !castlingRights.contains('q')
                        type == PieceType.ROOK && color == PieceColor.BLACK && r == 0 && c == 7 ->
                            !castlingRights.contains('k')
                        type == PieceType.ROOK && color == PieceColor.BLACK && r == 0 && c == 0 ->
                            !castlingRights.contains('q')
                        type == PieceType.PAWN && color == PieceColor.WHITE && r == 6 -> false
                        type == PieceType.PAWN && color == PieceColor.BLACK && r == 1 -> false
                        else -> true
                    }

                    board[r][c] = Piece("${color.name.first()}_${type.name}_${r}_$c", type, color, hasMoved = hasMoved)
                    c++
                }
            }
        }

        val turn = if (parts.size > 1 && parts[1] == "b") PieceColor.BLACK else PieceColor.WHITE
        val enPassant = if (parts.size > 3 && parts[3] != "-") {
            try { Position.fromAlgebraic(parts[3]) } catch (_: Exception) { null }
        } else null

        val halfMoves = parts.getOrNull(4)?.toIntOrNull() ?: 0
        val fullMoves = parts.getOrNull(5)?.toIntOrNull() ?: 1

        val state = GameState(
            board = board,
            currentTurn = turn,
            enPassantTarget = enPassant,
            halfMoveClock = halfMoves,
            fullMoveNumber = fullMoves
        )

        val inCheck = isKingInCheck(board, turn)
        val legalMoves = generateLegalMoves(state)
        val isCheckmate = inCheck && legalMoves.isEmpty()
        val isStalemate = !inCheck && legalMoves.isEmpty()

        return state.copy(
            isCheck = inCheck,
            isCheckmate = isCheckmate,
            isStalemate = isStalemate,
            isGameOver = isCheckmate || isStalemate,
            winner = if (isCheckmate) turn.opposite() else null
        )
    }

    /**
     * Finds king position for specified color.
     */
    fun findKing(board: Array<Array<Piece?>>, color: PieceColor): Position? {
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p != null && p.type == PieceType.KING && p.color == color) {
                    return Position(r, c)
                }
            }
        }
        return null
    }

    /**
     * Determines whether target square is attacked by any enemy piece.
     */
    fun isSquareAttacked(board: Array<Array<Piece?>>, target: Position, byColor: PieceColor): Boolean {
        // 1. Attacked by enemy Pawns
        val pawnDir = if (byColor == PieceColor.WHITE) 1 else -1 // enemy pawn moves in this row direction
        val pawnAttackingRow = target.row + pawnDir
        if (pawnAttackingRow in 0..7) {
            for (dc in listOf(-1, 1)) {
                val pawnCol = target.col + dc
                if (pawnCol in 0..7) {
                    val p = board[pawnAttackingRow][pawnCol]
                    if (p != null && p.color == byColor && p.type == PieceType.PAWN) {
                        return true
                    }
                }
            }
        }

        // 2. Attacked by Knight
        val knightDeltas = listOf(
            -2 to -1, -2 to 1, -1 to -2, -1 to 2,
            1 to -2, 1 to 2, 2 to -1, 2 to 1
        )
        for ((dr, dc) in knightDeltas) {
            val nr = target.row + dr
            val nc = target.col + dc
            if (nr in 0..7 && nc in 0..7) {
                val p = board[nr][nc]
                if (p != null && p.color == byColor && p.type == PieceType.KNIGHT) {
                    return true
                }
            }
        }

        // 3. Attacked by King (adjacent)
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = target.row + dr
                val nc = target.col + dc
                if (nr in 0..7 && nc in 0..7) {
                    val p = board[nr][nc]
                    if (p != null && p.color == byColor && p.type == PieceType.KING) {
                        return true
                    }
                }
            }
        }

        // 4. Attacked along rays by Bishop / Queen (Diagonals)
        val diagDirs = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
        for ((dr, dc) in diagDirs) {
            var step = 1
            while (true) {
                val nr = target.row + dr * step
                val nc = target.col + dc * step
                if (nr !in 0..7 || nc !in 0..7) break
                val p = board[nr][nc]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break // Blocked by piece
                }
                step++
            }
        }

        // 5. Attacked along rays by Rook / Queen (Orthogonals)
        val orthoDirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        for ((dr, dc) in orthoDirs) {
            var step = 1
            while (true) {
                val nr = target.row + dr * step
                val nc = target.col + dc * step
                if (nr !in 0..7 || nc !in 0..7) break
                val p = board[nr][nc]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break // Blocked by piece
                }
                step++
            }
        }

        return false
    }

    fun isKingInCheck(board: Array<Array<Piece?>>, color: PieceColor): Boolean {
        val kingPos = findKing(board, color) ?: return false
        return isSquareAttacked(board, kingPos, color.opposite())
    }

    /**
     * Generates all pseudo-legal moves for the piece at pos.
     */
    fun generateCandidateMoves(state: GameState, pos: Position): List<Move> {
        val piece = state.pieceAt(pos) ?: return emptyList()
        val board = state.board
        val color = piece.color
        val candidates = mutableListOf<Move>()

        when (piece.type) {
            PieceType.PAWN -> {
                val forward = if (color == PieceColor.WHITE) -1 else 1
                val startRow = if (color == PieceColor.WHITE) 6 else 1
                val promoRow = if (color == PieceColor.WHITE) 0 else 7

                // 1 step forward
                val nextR = pos.row + forward
                if (nextR in 0..7 && board[nextR][pos.col] == null) {
                    val to = Position(nextR, pos.col)
                    if (nextR == promoRow) {
                        // Promotion
                        for (pt in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                            candidates.add(Move(from = pos, to = to, piece = piece, promotedTo = pt))
                        }
                    } else {
                        candidates.add(Move(from = pos, to = to, piece = piece))
                    }

                    // 2 steps forward from initial rank
                    val doubleR = pos.row + forward * 2
                    if (pos.row == startRow && board[doubleR][pos.col] == null) {
                        candidates.add(Move(from = pos, to = Position(doubleR, pos.col), piece = piece))
                    }
                }

                // Captures diagonally
                for (dc in listOf(-1, 1)) {
                    val capC = pos.col + dc
                    if (nextR in 0..7 && capC in 0..7) {
                        val targetPiece = board[nextR][capC]
                        val to = Position(nextR, capC)
                        if (targetPiece != null && targetPiece.color != color) {
                            if (nextR == promoRow) {
                                for (pt in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                                    candidates.add(Move(from = pos, to = to, piece = piece, capturedPiece = targetPiece, promotedTo = pt))
                                }
                            } else {
                                candidates.add(Move(from = pos, to = to, piece = piece, capturedPiece = targetPiece))
                            }
                        } else if (state.enPassantTarget == to) {
                            // En Passant capture
                            val capturedPawnPos = Position(pos.row, capC)
                            val enPassantPawn = board[capturedPawnPos.row][capturedPawnPos.col]
                            if (enPassantPawn != null && enPassantPawn.color != color) {
                                candidates.add(
                                    Move(
                                        from = pos,
                                        to = to,
                                        piece = piece,
                                        capturedPiece = enPassantPawn,
                                        isEnPassant = true
                                    )
                                )
                            }
                        }
                    }
                }
            }

            PieceType.KNIGHT -> {
                val jumps = listOf(
                    -2 to -1, -2 to 1, -1 to -2, -1 to 2,
                    1 to -2, 1 to 2, 2 to -1, 2 to 1
                )
                for ((dr, dc) in jumps) {
                    val nr = pos.row + dr
                    val nc = pos.col + dc
                    if (nr in 0..7 && nc in 0..7) {
                        val dest = board[nr][nc]
                        if (dest == null || dest.color != color) {
                            candidates.add(Move(from = pos, to = Position(nr, nc), piece = piece, capturedPiece = dest))
                        }
                    }
                }
            }

            PieceType.BISHOP -> {
                val dirs = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
                addRays(candidates, state, pos, piece, dirs)
            }

            PieceType.ROOK -> {
                val dirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
                addRays(candidates, state, pos, piece, dirs)
            }

            PieceType.QUEEN -> {
                val dirs = listOf(
                    -1 to -1, -1 to 1, 1 to -1, 1 to 1,
                    -1 to 0, 1 to 0, 0 to -1, 0 to 1
                )
                addRays(candidates, state, pos, piece, dirs)
            }

            PieceType.KING -> {
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        if (dr == 0 && dc == 0) continue
                        val nr = pos.row + dr
                        val nc = pos.col + dc
                        if (nr in 0..7 && nc in 0..7) {
                            val dest = board[nr][nc]
                            if (dest == null || dest.color != color) {
                                candidates.add(Move(from = pos, to = Position(nr, nc), piece = piece, capturedPiece = dest))
                            }
                        }
                    }
                }

                // Castling
                if (!piece.hasMoved && !isKingInCheck(board, color)) {
                    val rank = pos.row
                    val oppColor = color.opposite()

                    // Kingside (e1->g1 or e8->g8)
                    val kingsideRook = board[rank][7]
                    if (kingsideRook != null && kingsideRook.type == PieceType.ROOK && !kingsideRook.hasMoved) {
                        // Empty squares between king and rook: col 5, 6
                        if (board[rank][5] == null && board[rank][6] == null) {
                            // Squares king passes through and lands on must not be attacked
                            if (!isSquareAttacked(board, Position(rank, 5), oppColor) &&
                                !isSquareAttacked(board, Position(rank, 6), oppColor)
                            ) {
                                candidates.add(
                                    Move(
                                        from = pos,
                                        to = Position(rank, 6),
                                        piece = piece,
                                        isCastling = true
                                    )
                                )
                            }
                        }
                    }

                    // Queenside (e1->c1 or e8->c8)
                    val queensideRook = board[rank][0]
                    if (queensideRook != null && queensideRook.type == PieceType.ROOK && !queensideRook.hasMoved) {
                        // Empty squares between king and rook: col 1, 2, 3
                        if (board[rank][1] == null && board[rank][2] == null && board[rank][3] == null) {
                            if (!isSquareAttacked(board, Position(rank, 3), oppColor) &&
                                !isSquareAttacked(board, Position(rank, 2), oppColor)
                            ) {
                                candidates.add(
                                    Move(
                                        from = pos,
                                        to = Position(rank, 2),
                                        piece = piece,
                                        isCastling = true
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        return candidates
    }

    private fun addRays(
        candidates: MutableList<Move>,
        state: GameState,
        pos: Position,
        piece: Piece,
        dirs: List<Pair<Int, Int>>
    ) {
        val board = state.board
        val color = piece.color
        for ((dr, dc) in dirs) {
            var step = 1
            while (true) {
                val nr = pos.row + dr * step
                val nc = pos.col + dc * step
                if (nr !in 0..7 || nc !in 0..7) break
                val dest = board[nr][nc]
                if (dest == null) {
                    candidates.add(Move(from = pos, to = Position(nr, nc), piece = piece))
                } else {
                    if (dest.color != color) {
                        candidates.add(Move(from = pos, to = Position(nr, nc), piece = piece, capturedPiece = dest))
                    }
                    break // Ray is blocked
                }
                step++
            }
        }
    }

    /**
     * Executes candidate move on a copy of board and verifies if king is safe.
     */
    fun isMoveLegal(state: GameState, move: Move): Boolean {
        val tempBoard = state.copyBoard()

        // Apply move on tempBoard
        tempBoard[move.from.row][move.from.col] = null

        if (move.isEnPassant) {
            // Remove captured en passant pawn
            val capRow = move.from.row
            val capCol = move.to.col
            tempBoard[capRow][capCol] = null
        } else if (move.isCastling) {
            // Move rook
            val rank = move.from.row
            if (move.to.col == 6) {
                // Kingside
                val rook = tempBoard[rank][7]
                tempBoard[rank][7] = null
                tempBoard[rank][5] = rook?.copyWithMoved()
            } else if (move.to.col == 2) {
                // Queenside
                val rook = tempBoard[rank][0]
                tempBoard[rank][0] = null
                tempBoard[rank][3] = rook?.copyWithMoved()
            }
        }

        val placedPiece = if (move.promotedTo != null) {
            Piece("promo_${move.piece.color}_${move.promotedTo}_${move.to.row}_${move.to.col}", move.promotedTo, move.piece.color, hasMoved = true)
        } else {
            move.piece.copyWithMoved()
        }
        tempBoard[move.to.row][move.to.col] = placedPiece

        // King must not be in check
        return !isKingInCheck(tempBoard, move.piece.color)
    }

    /**
     * Generates all legal moves for a given position.
     */
    fun getLegalMovesForPosition(state: GameState, pos: Position): List<Move> {
        val piece = state.pieceAt(pos) ?: return emptyList()
        if (piece.color != state.currentTurn) return emptyList()

        val candidates = generateCandidateMoves(state, pos)
        return candidates.filter { isMoveLegal(state, it) }
    }

    /**
     * Generates all legal moves for current player in the game state.
     */
    fun generateLegalMoves(state: GameState): List<Move> {
        val legalMoves = mutableListOf<Move>()
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = state.board[r][c]
                if (piece != null && piece.color == state.currentTurn) {
                    legalMoves.addAll(getLegalMovesForPosition(state, Position(r, c)))
                }
            }
        }
        return legalMoves
    }

    /**
     * Applies a legal move to the state and returns new updated GameState with SAN notation.
     */
    fun makeMove(state: GameState, move: Move): GameState {
        val newBoard = state.copyBoard()
        val color = move.piece.color
        val oppColor = color.opposite()

        // 1. Move piece
        newBoard[move.from.row][move.from.col] = null

        var captured = move.capturedPiece
        if (move.isEnPassant) {
            val capRow = move.from.row
            val capCol = move.to.col
            captured = newBoard[capRow][capCol]
            newBoard[capRow][capCol] = null
        } else if (move.isCastling) {
            val rank = move.from.row
            if (move.to.col == 6) {
                val rook = newBoard[rank][7]
                newBoard[rank][7] = null
                newBoard[rank][5] = rook?.copyWithMoved()
            } else if (move.to.col == 2) {
                val rook = newBoard[rank][0]
                newBoard[rank][0] = null
                newBoard[rank][3] = rook?.copyWithMoved()
            }
        }

        val placedPiece = if (move.promotedTo != null) {
            Piece("promo_${color}_${move.promotedTo}_${move.to.row}_${move.to.col}", move.promotedTo, color, hasMoved = true)
        } else {
            move.piece.copyWithMoved()
        }
        newBoard[move.to.row][move.to.col] = placedPiece

        // Track captured pieces
        val newCapturedWhite = state.capturedWhite.toMutableList()
        val newCapturedBlack = state.capturedBlack.toMutableList()
        if (captured != null) {
            if (captured.color == PieceColor.WHITE) newCapturedWhite.add(captured)
            else newCapturedBlack.add(captured)
        }

        // En passant target for next turn
        val newEnPassant = if (move.piece.type == PieceType.PAWN && Math.abs(move.to.row - move.from.row) == 2) {
            Position((move.from.row + move.to.row) / 2, move.from.col)
        } else null

        // 50-move rule halfmove clock
        val resetHalfMove = move.piece.type == PieceType.PAWN || captured != null
        val newHalfMoveClock = if (resetHalfMove) 0 else state.halfMoveClock + 1
        val newFullMove = if (color == PieceColor.BLACK) state.fullMoveNumber + 1 else state.fullMoveNumber

        // Evaluate checks and game over
        val oppInCheck = isKingInCheck(newBoard, oppColor)

        // Build tentative next state to compute opponent's legal moves
        val nextTurnState = state.copy(
            board = newBoard,
            currentTurn = oppColor,
            enPassantTarget = newEnPassant
        )
        val oppLegalMoves = generateLegalMoves(nextTurnState)

        val isCheckmate = oppInCheck && oppLegalMoves.isEmpty()
        val isStalemate = !oppInCheck && oppLegalMoves.isEmpty()
        val isFiftyMoveRule = newHalfMoveClock >= 100
        val isInsufficientMaterial = isInsufficientMaterial(newBoard)

        val isDraw = isStalemate || isFiftyMoveRule || isInsufficientMaterial
        val drawReason = when {
            isStalemate -> "Stalemate"
            isFiftyMoveRule -> "50-Move Rule Draw"
            isInsufficientMaterial -> "Insufficient Material Draw"
            else -> null
        }
        val isGameOver = isCheckmate || isDraw
        val winner = if (isCheckmate) color else null

        // Format SAN string
        val san = formatSan(state, move, isCheckmate, oppInCheck)
        val finalMove = move.copy(san = san, capturedPiece = captured)

        return state.copy(
            board = newBoard,
            currentTurn = oppColor,
            moveHistory = state.moveHistory + finalMove,
            capturedWhite = newCapturedWhite,
            capturedBlack = newCapturedBlack,
            enPassantTarget = newEnPassant,
            halfMoveClock = newHalfMoveClock,
            fullMoveNumber = newFullMove,
            isCheck = oppInCheck,
            isCheckmate = isCheckmate,
            isStalemate = isStalemate,
            isDraw = isDraw,
            drawReason = drawReason,
            isGameOver = isGameOver,
            winner = winner,
            lastMove = finalMove,
            pendingPromotion = null
        )
    }

    private fun formatSan(state: GameState, move: Move, checkmate: Boolean, check: Boolean): String {
        val suffix = when {
            checkmate -> "#"
            check -> "+"
            else -> ""
        }

        if (move.isCastling) {
            val castlingStr = if (move.to.col == 6) "O-O" else "O-O-O"
            return "$castlingStr$suffix"
        }

        val sb = StringBuilder()
        if (move.piece.type == PieceType.PAWN) {
            if (move.capturedPiece != null || move.isEnPassant) {
                sb.append('a' + move.from.col)
                sb.append('x')
            }
            sb.append(move.to.algebraic)
            if (move.promotedTo != null) {
                sb.append('=').append(move.promotedTo.symbol)
            }
        } else {
            sb.append(move.piece.type.symbol)

            // Disambiguation if needed
            val otherCandidates = generateCandidateMoves(state, move.from).filter {
                it.to == move.to && it.piece.type == move.piece.type && it.from != move.from && isMoveLegal(state, it)
            }
            if (otherCandidates.isNotEmpty()) {
                val sameCol = otherCandidates.any { it.from.col == move.from.col }
                val sameRow = otherCandidates.any { it.from.row == move.from.row }
                if (!sameCol) {
                    sb.append('a' + move.from.col)
                } else if (!sameRow) {
                    sb.append(8 - move.from.row)
                } else {
                    sb.append(move.from.algebraic)
                }
            }

            if (move.capturedPiece != null) {
                sb.append('x')
            }
            sb.append(move.to.algebraic)
        }

        sb.append(suffix)
        return sb.toString()
    }

    fun isInsufficientMaterial(board: Array<Array<Piece?>>): Boolean {
        val pieces = mutableListOf<Piece>()
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p != null) pieces.add(p)
            }
        }

        // King vs King
        if (pieces.size == 2 && pieces.all { it.type == PieceType.KING }) return true

        // King + Minor vs King
        if (pieces.size == 3) {
            val minorCount = pieces.count { it.type == PieceType.BISHOP || it.type == PieceType.KNIGHT }
            if (minorCount == 1 && pieces.count { it.type == PieceType.KING } == 2) return true
        }

        // King + Bishop vs King + Bishop on same square color
        if (pieces.size == 4) {
            val bishops = pieces.filter { it.type == PieceType.BISHOP }
            if (bishops.size == 2 && pieces.count { it.type == PieceType.KING } == 2) {
                // Find positions of both bishops
                var b1SquareColor: Int? = null
                var b2SquareColor: Int? = null
                for (r in 0..7) {
                    for (c in 0..7) {
                        val p = board[r][c]
                        if (p?.type == PieceType.BISHOP) {
                            val color = (r + c) % 2
                            if (b1SquareColor == null) b1SquareColor = color
                            else b2SquareColor = color
                        }
                    }
                }
                if (b1SquareColor == b2SquareColor) return true
            }
        }

        return false
    }

    /**
     * Checks if moving pawn to dest requires pawn promotion.
     */
    fun isPromotionMove(piece: Piece, to: Position): Boolean {
        if (piece.type != PieceType.PAWN) return false
        return (piece.color == PieceColor.WHITE && to.row == 0) ||
               (piece.color == PieceColor.BLACK && to.row == 7)
    }
}
