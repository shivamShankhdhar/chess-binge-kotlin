package com.example.chessbinge.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chessbinge.audio.ChessSound
import com.example.chessbinge.audio.SoundManager
import com.example.chessbinge.engine.ChessAI
import com.example.chessbinge.engine.ChessEngine
import com.example.chessbinge.engine.PuzzleRepository
import com.example.chessbinge.model.AIDifficulty
import com.example.chessbinge.model.GameMode
import com.example.chessbinge.model.GameState
import com.example.chessbinge.model.GameStats
import com.example.chessbinge.model.Move
import com.example.chessbinge.model.PieceColor
import com.example.chessbinge.model.PieceType
import com.example.chessbinge.model.Position
import com.example.chessbinge.model.Puzzle
import com.example.chessbinge.model.TimeControl
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChessViewModel(application: Application) : AndroidViewModel(application) {

    private val _gameState = MutableStateFlow(ChessEngine.createInitialState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _gameMode = MutableStateFlow(GameMode.VS_AI)
    val gameMode: StateFlow<GameMode> = _gameMode.asStateFlow()

    private val _aiDifficulty = MutableStateFlow(AIDifficulty.MEDIUM)
    val aiDifficulty: StateFlow<AIDifficulty> = _aiDifficulty.asStateFlow()

    private val _timeControl = MutableStateFlow(TimeControl.UNTIMED)
    val timeControl: StateFlow<TimeControl> = _timeControl.asStateFlow()

    private val _playerColor = MutableStateFlow(PieceColor.WHITE)
    val playerColor: StateFlow<PieceColor> = _playerColor.asStateFlow()

    private val _selectedPosition = MutableStateFlow<Position?>(null)
    val selectedPosition: StateFlow<Position?> = _selectedPosition.asStateFlow()

    private val _validMoves = MutableStateFlow<List<Move>>(emptyList())
    val validMoves: StateFlow<List<Move>> = _validMoves.asStateFlow()

    private val _isFlipped = MutableStateFlow(false)
    val isFlipped: StateFlow<Boolean> = _isFlipped.asStateFlow()

    private val _hintMove = MutableStateFlow<Move?>(null)
    val hintMove: StateFlow<Move?> = _hintMove.asStateFlow()

    private val _isAIThinking = MutableStateFlow(false)
    val isAIThinking: StateFlow<Boolean> = _isAIThinking.asStateFlow()

    private val _currentPuzzle = MutableStateFlow<Puzzle?>(null)
    val currentPuzzle: StateFlow<Puzzle?> = _currentPuzzle.asStateFlow()

    private val _puzzleStepIndex = MutableStateFlow(0)
    val puzzleStepIndex: StateFlow<Int> = _puzzleStepIndex.asStateFlow()

    private val _puzzleSolved = MutableStateFlow(false)
    val puzzleSolved: StateFlow<Boolean> = _puzzleSolved.asStateFlow()

    private val _puzzleFailed = MutableStateFlow(false)
    val puzzleFailed: StateFlow<Boolean> = _puzzleFailed.asStateFlow()

    private val _stats = MutableStateFlow(GameStats())
    val stats: StateFlow<GameStats> = _stats.asStateFlow()

    private var clockJob: Job? = null
    private var aiJob: Job? = null

    init {
        SoundManager.initialize(application)
    }

    fun startNewGame(
        mode: GameMode,
        difficulty: AIDifficulty = _aiDifficulty.value,
        time: TimeControl = _timeControl.value,
        color: PieceColor = _playerColor.value
    ) {
        clockJob?.cancel()
        aiJob?.cancel()

        _gameMode.value = mode
        _aiDifficulty.value = difficulty
        _timeControl.value = time
        _playerColor.value = color
        _selectedPosition.value = null
        _validMoves.value = emptyList()
        _hintMove.value = null
        _isAIThinking.value = false
        _currentPuzzle.value = null

        _isFlipped.value = (mode == GameMode.VS_AI && color == PieceColor.BLACK)

        val initialMs = time.initialSeconds * 1000L
        android.util.Log.d("ChessViewModel", "startNewGame called: mode=$mode, time=$time, initialSeconds=${time.initialSeconds}, initialMs=$initialMs, color=$color")
        _gameState.value = ChessEngine.createInitialState().copy(
            whiteTimeRemainingMs = initialMs,
            blackTimeRemainingMs = initialMs
        )
        android.util.Log.d("ChessViewModel", "startNewGame state set: whiteTimeRemainingMs=${_gameState.value.whiteTimeRemainingMs}")

        SoundManager.play(ChessSound.GAME_START)
        startClockIfNeeded()

        // If playing vs AI and user is Black, AI moves first
        if (mode == GameMode.VS_AI && color == PieceColor.BLACK) {
            triggerAIMove()
        }
    }

    fun loadPuzzle(puzzle: Puzzle) {
        clockJob?.cancel()
        aiJob?.cancel()

        _gameMode.value = GameMode.TACTICS_PUZZLE
        _currentPuzzle.value = puzzle
        _puzzleStepIndex.value = 0
        _puzzleSolved.value = false
        _puzzleFailed.value = false
        _selectedPosition.value = null
        _validMoves.value = emptyList()
        _hintMove.value = null
        _isAIThinking.value = false
        _playerColor.value = puzzle.playerColor
        _isFlipped.value = (puzzle.playerColor == PieceColor.BLACK)

        _gameState.value = ChessEngine.fromFen(puzzle.fen)
        SoundManager.play(ChessSound.GAME_START)
    }

    fun onSquareClicked(pos: Position) {
        val state = _gameState.value
        if (state.isGameOver) return

        // In AI mode, ignore touches when AI is thinking
        if (_gameMode.value == GameMode.VS_AI && state.currentTurn != _playerColor.value) return

        val sel = _selectedPosition.value

        if (sel == null) {
            // Pick a piece belonging to the current player
            val piece = state.pieceAt(pos)
            if (piece != null && piece.color == state.currentTurn) {
                _selectedPosition.value = pos
                _validMoves.value = ChessEngine.getLegalMovesForPosition(state, pos)
                SoundManager.play(ChessSound.CLICK)
            }
        } else {
            // Already selected
            val targetMove = _validMoves.value.find { it.to == pos }
            if (targetMove != null) {
                // If promotion required
                if (ChessEngine.isPromotionMove(targetMove.piece, pos)) {
                    _gameState.value = state.copy(pendingPromotion = sel to pos)
                } else {
                    executeMove(targetMove)
                }
            } else {
                // Check if user tapped another of their own pieces
                val piece = state.pieceAt(pos)
                if (piece != null && piece.color == state.currentTurn) {
                    _selectedPosition.value = pos
                    _validMoves.value = ChessEngine.getLegalMovesForPosition(state, pos)
                    SoundManager.play(ChessSound.CLICK)
                } else {
                    // Deselect
                    _selectedPosition.value = null
                    _validMoves.value = emptyList()
                }
            }
        }
    }

    fun onPromotePiece(promotedType: PieceType) {
        val state = _gameState.value
        val pending = state.pendingPromotion ?: return
        val matchingMove = _validMoves.value.find {
            it.from == pending.first && it.to == pending.second && it.promotedTo == promotedType
        }
        if (matchingMove != null) {
            executeMove(matchingMove)
        } else {
            _gameState.value = state.copy(pendingPromotion = null)
        }
    }

    private fun executeMove(move: Move) {
        val prevState = _gameState.value
        val newState = ChessEngine.makeMove(prevState, move)

        // Add increment if clock is active
        val incrementMs = _timeControl.value.incrementSeconds * 1000L
        val updatedClockState = if (_timeControl.value != TimeControl.UNTIMED) {
            if (move.piece.color == PieceColor.WHITE) {
                newState.copy(whiteTimeRemainingMs = newState.whiteTimeRemainingMs + incrementMs)
            } else {
                newState.copy(blackTimeRemainingMs = newState.blackTimeRemainingMs + incrementMs)
            }
        } else newState

        _gameState.value = updatedClockState
        _selectedPosition.value = null
        _validMoves.value = emptyList()
        _hintMove.value = null

        // Play appropriate sound
        when {
            updatedClockState.isCheckmate -> {
                if (updatedClockState.winner == _playerColor.value) {
                    SoundManager.play(ChessSound.VICTORY)
                    updateStats(win = true)
                } else {
                    SoundManager.play(ChessSound.DEFEAT)
                    updateStats(loss = true)
                }
            }
            updatedClockState.isDraw -> {
                SoundManager.play(ChessSound.DEFEAT)
                updateStats(draw = true)
            }
            updatedClockState.isCheck -> SoundManager.play(ChessSound.CHECK)
            move.capturedPiece != null || move.isEnPassant -> SoundManager.play(ChessSound.CAPTURE)
            else -> SoundManager.play(ChessSound.MOVE)
        }

        // Check Puzzle Logic
        if (_gameMode.value == GameMode.TACTICS_PUZZLE) {
            handlePuzzleStep(move)
            return
        }

        // If Pass & Play: Optionally rotate board perspective
        if (_gameMode.value == GameMode.PASS_AND_PLAY) {
            // Kept stable or can be toggled by user
        }

        // Trigger AI move if game vs AI is ongoing
        if (_gameMode.value == GameMode.VS_AI && !updatedClockState.isGameOver && updatedClockState.currentTurn != _playerColor.value) {
            triggerAIMove()
        }
    }

    private fun handlePuzzleStep(move: Move) {
        val puzzle = _currentPuzzle.value ?: return
        val currentStep = _puzzleStepIndex.value
        val solution = puzzle.solutionMoves

        if (currentStep < solution.size) {
            val expected = solution[currentStep]
            val fromExpected = Position.fromAlgebraic(expected.first)
            val toExpected = Position.fromAlgebraic(expected.second)

            if (move.from == fromExpected && move.to == toExpected) {
                val nextStep = currentStep + 1
                _puzzleStepIndex.value = nextStep

                if (nextStep >= solution.size) {
                    // Solved!
                    _puzzleSolved.value = true
                    SoundManager.play(ChessSound.SUCCESS)
                    _stats.value = _stats.value.copy(puzzlesSolved = _stats.value.puzzlesSolved + 1)
                } else {
                    // Opponent counter move in puzzle
                    viewModelScope.launch {
                        delay(500)
                        val oppExpected = solution[nextStep]
                        val oppFrom = Position.fromAlgebraic(oppExpected.first)
                        val oppTo = Position.fromAlgebraic(oppExpected.second)
                        val oppMove = ChessEngine.getLegalMovesForPosition(_gameState.value, oppFrom).find { it.to == oppTo }
                        if (oppMove != null) {
                            _gameState.value = ChessEngine.makeMove(_gameState.value, oppMove)
                            SoundManager.play(ChessSound.MOVE)
                            _puzzleStepIndex.value = nextStep + 1
                        }
                    }
                }
            } else {
                // Incorrect move in puzzle
                _puzzleFailed.value = true
                SoundManager.play(ChessSound.DEFEAT)
            }
        }
    }

    fun retryPuzzle() {
        _currentPuzzle.value?.let { loadPuzzle(it) }
    }

    private fun triggerAIMove() {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _isAIThinking.value = true
            // Humanized delay
            delay((450..850).random().toLong())

            val bestMove = ChessAI.getBestMove(_gameState.value, _aiDifficulty.value)
            _isAIThinking.value = false

            if (bestMove != null && !_gameState.value.isGameOver) {
                executeMove(bestMove)
            }
        }
    }

    fun undoMove() {
        val state = _gameState.value
        val history = state.moveHistory
        if (history.isEmpty()) return

        clockJob?.cancel()
        aiJob?.cancel()
        _isAIThinking.value = false

        val stepsToUndo = if (_gameMode.value == GameMode.VS_AI && history.size >= 2) 2 else 1
        val newHistory = history.dropLast(stepsToUndo)

        // Reconstruct game state by replaying moves
        var rebuiltState = ChessEngine.createInitialState().copy(
            whiteTimeRemainingMs = state.whiteTimeRemainingMs,
            blackTimeRemainingMs = state.blackTimeRemainingMs
        )
        for (m in newHistory) {
            rebuiltState = ChessEngine.makeMove(rebuiltState, m)
        }

        _gameState.value = rebuiltState
        _selectedPosition.value = null
        _validMoves.value = emptyList()
        _hintMove.value = null

        SoundManager.play(ChessSound.CLICK)
        startClockIfNeeded()
    }

    fun requestHint() {
        if (_gameState.value.isGameOver) return
        viewModelScope.launch {
            val hint = ChessAI.getBestMove(_gameState.value, AIDifficulty.HARD)
            _hintMove.value = hint
            SoundManager.play(ChessSound.CLICK)
            delay(4000)
            _hintMove.value = null
        }
    }

    fun toggleFlip() {
        _isFlipped.value = !_isFlipped.value
        SoundManager.play(ChessSound.CLICK)
    }

    fun resign() {
        val state = _gameState.value
        if (state.isGameOver) return

        val winner = state.currentTurn.opposite()
        _gameState.value = state.copy(
            isGameOver = true,
            winner = winner,
            drawReason = "Resignation"
        )
        clockJob?.cancel()
        aiJob?.cancel()

        if (winner == _playerColor.value) {
            SoundManager.play(ChessSound.VICTORY)
            updateStats(win = true)
        } else {
            SoundManager.play(ChessSound.DEFEAT)
            updateStats(loss = true)
        }
    }

    private fun startClockIfNeeded() {
        if (_timeControl.value == TimeControl.UNTIMED) return
        val initialState = _gameState.value
        if (initialState.whiteTimeRemainingMs <= 0L || initialState.blackTimeRemainingMs <= 0L) return

        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            while (true) {
                delay(100)
                val state = _gameState.value
                if (state.isGameOver) break

                if (state.currentTurn == PieceColor.WHITE) {
                    val remaining = state.whiteTimeRemainingMs - 100
                    if (remaining <= 0) {
                        _gameState.value = state.copy(
                            whiteTimeRemainingMs = 0,
                            isGameOver = true,
                            winner = PieceColor.BLACK,
                            drawReason = "White ran out of time"
                        )
                        SoundManager.play(ChessSound.DEFEAT)
                        break
                    } else {
                        _gameState.value = state.copy(whiteTimeRemainingMs = remaining)
                    }
                } else {
                    val remaining = state.blackTimeRemainingMs - 100
                    if (remaining <= 0) {
                        _gameState.value = state.copy(
                            blackTimeRemainingMs = 0,
                            isGameOver = true,
                            winner = PieceColor.WHITE,
                            drawReason = "Black ran out of time"
                        )
                        SoundManager.play(ChessSound.VICTORY)
                        break
                    } else {
                        _gameState.value = state.copy(blackTimeRemainingMs = remaining)
                    }
                }
            }
        }
    }

    private fun updateStats(win: Boolean = false, loss: Boolean = false, draw: Boolean = false) {
        val cur = _stats.value
        _stats.value = cur.copy(
            gamesPlayed = cur.gamesPlayed + 1,
            wins = if (win) cur.wins + 1 else cur.wins,
            losses = if (loss) cur.losses + 1 else cur.losses,
            draws = if (draw) cur.draws + 1 else cur.draws
        )
    }

    override fun onCleared() {
        super.onCleared()
        clockJob?.cancel()
        aiJob?.cancel()
        SoundManager.release()
    }
}
