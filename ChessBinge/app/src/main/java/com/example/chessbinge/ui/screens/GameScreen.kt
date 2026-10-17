package com.example.chessbinge.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessbinge.audio.SoundManager
import com.example.chessbinge.model.GameMode
import com.example.chessbinge.model.PieceColor
import com.example.chessbinge.model.TimeControl
import com.example.chessbinge.ui.board.ChessBoardView
import com.example.chessbinge.ui.components.GameActionBar
import com.example.chessbinge.ui.components.GameOverDialog
import com.example.chessbinge.ui.components.MoveHistoryBar
import com.example.chessbinge.ui.components.PlayerProfileBar
import com.example.chessbinge.ui.components.PromotionDialog
import com.example.chessbinge.ui.theme.BgCard
import com.example.chessbinge.ui.theme.BgDark
import com.example.chessbinge.ui.theme.CrimsonGlow
import com.example.chessbinge.ui.theme.CrimsonPrimary
import com.example.chessbinge.ui.theme.TextPrimary
import com.example.chessbinge.ui.theme.TextSecondary
import com.example.chessbinge.ui.theme.TitanOneFontFamily
import com.example.chessbinge.viewmodel.ChessViewModel

@Composable
fun GameScreen(
    viewModel: ChessViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsState()
    val gameMode by viewModel.gameMode.collectAsState()
    val aiDifficulty by viewModel.aiDifficulty.collectAsState()
    val timeControl by viewModel.timeControl.collectAsState()
    val playerColor by viewModel.playerColor.collectAsState()
    val selectedPosition by viewModel.selectedPosition.collectAsState()
    val validMoves by viewModel.validMoves.collectAsState()
    val isFlipped by viewModel.isFlipped.collectAsState()
    val hintMove by viewModel.hintMove.collectAsState()
    val isAIThinking by viewModel.isAIThinking.collectAsState()

    android.util.Log.d("ChessViewModel", "GameScreen compose: timeControl=$timeControl, whiteTime=${gameState.whiteTimeRemainingMs}, blackTime=${gameState.blackTimeRemainingMs}, hasClock=${timeControl != TimeControl.UNTIMED}")

    var showGameOverDialog by remember { mutableStateOf(true) }
    androidx.compose.runtime.LaunchedEffect(gameState.isGameOver) {
        if (!gameState.isGameOver) {
            showGameOverDialog = true
        }
    }
    var isSoundMuted by remember { mutableStateOf(!SoundManager.isSoundEnabled.value) }

    val hasClock = timeControl != TimeControl.UNTIMED
    val isVsAI = gameMode == GameMode.VS_AI

    // Top player info (Opponent)
    val topColor = if (isFlipped) PieceColor.WHITE else PieceColor.BLACK
    val bottomColor = topColor.opposite()

    val topName = if (isVsAI) {
        if (playerColor == topColor) "You" else "AI (${aiDifficulty.displayName})"
    } else {
        if (topColor == PieceColor.WHITE) "White Player" else "Black Player"
    }
    val topIsAI = isVsAI && playerColor != topColor

    val bottomName = if (isVsAI) {
        if (playerColor == bottomColor) "You" else "AI (${aiDifficulty.displayName})"
    } else {
        if (bottomColor == PieceColor.WHITE) "White Player" else "Black Player"
    }

    val topCaptured = if (topColor == PieceColor.WHITE) gameState.capturedBlack else gameState.capturedWhite
    val bottomCaptured = if (bottomColor == PieceColor.WHITE) gameState.capturedBlack else gameState.capturedWhite

    val topMaterial = if (topColor == PieceColor.WHITE) gameState.materialAdvantage else -gameState.materialAdvantage
    val bottomMaterial = -topMaterial

    val topTime = if (topColor == PieceColor.WHITE) gameState.whiteTimeRemainingMs else gameState.blackTimeRemainingMs
    val bottomTime = if (bottomColor == PieceColor.WHITE) gameState.whiteTimeRemainingMs else gameState.blackTimeRemainingMs

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top App Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CHESS BINGE",
                        fontFamily = TitanOneFontFamily,
                        fontSize = 17.sp,
                        color = CrimsonPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF261217))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = gameMode.displayName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonGlow
                        )
                    }
                }

                // AI Thinking indicator
                Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    if (isAIThinking) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = CrimsonGlow,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Opponent Profile Bar
            PlayerProfileBar(
                name = topName,
                subtitle = if (topIsAI) "Engine Elo ~${1200 + aiDifficulty.depth * 400}" else "Opponent",
                color = topColor,
                isCurrentTurn = gameState.currentTurn == topColor && !gameState.isGameOver,
                isAI = topIsAI,
                capturedPieces = topCaptured,
                materialAdvantage = topMaterial,
                timeRemainingMs = topTime,
                hasClock = hasClock
            )

            Spacer(modifier = Modifier.height(10.dp))

            // The Red & Black Chessboard with Coordinate Borders
            ChessBoardView(
                gameState = gameState,
                selectedPosition = selectedPosition,
                validMoves = validMoves,
                isFlipped = isFlipped,
                hintMove = hintMove,
                onSquareClicked = { pos -> viewModel.onSquareClicked(pos) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Player Profile Bar
            PlayerProfileBar(
                name = bottomName,
                subtitle = if (isVsAI) "Challenger" else "Player",
                color = bottomColor,
                isCurrentTurn = gameState.currentTurn == bottomColor && !gameState.isGameOver,
                isAI = false,
                capturedPieces = bottomCaptured,
                materialAdvantage = bottomMaterial,
                timeRemainingMs = bottomTime,
                hasClock = hasClock
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Move History Log
            MoveHistoryBar(moves = gameState.moveHistory)

            Spacer(modifier = Modifier.height(10.dp))

            // Game Action Bar
            GameActionBar(
                canUndo = gameState.moveHistory.isNotEmpty() && !gameState.isGameOver,
                isSoundMuted = isSoundMuted,
                onUndo = { viewModel.undoMove() },
                onHint = { viewModel.requestHint() },
                onFlip = { viewModel.toggleFlip() },
                onResign = { viewModel.resign() },
                onToggleSound = {
                    isSoundMuted = !SoundManager.toggleSound()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Pawn Promotion Modal
        if (gameState.pendingPromotion != null) {
            PromotionDialog(
                color = gameState.currentTurn,
                onPieceSelected = { chosenType -> viewModel.onPromotePiece(chosenType) },
                onDismiss = { /* must choose piece */ }
            )
        }

        // Game Over Dialog
        if (gameState.isGameOver && showGameOverDialog) {
            GameOverDialog(
                gameState = gameState,
                playerColor = playerColor,
                onPlayAgain = {
                    viewModel.startNewGame(gameMode, aiDifficulty, timeControl, playerColor)
                },
                onReviewGame = {
                    showGameOverDialog = false
                },
                onHome = onNavigateBack
            )
        }
    }
}
