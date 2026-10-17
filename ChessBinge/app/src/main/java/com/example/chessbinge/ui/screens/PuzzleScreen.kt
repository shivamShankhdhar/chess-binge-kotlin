package com.example.chessbinge.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessbinge.engine.PuzzleRepository
import com.example.chessbinge.model.PieceColor
import com.example.chessbinge.ui.board.ChessBoardView
import com.example.chessbinge.ui.theme.BgCard
import com.example.chessbinge.ui.theme.BgCardBorder
import com.example.chessbinge.ui.theme.BgDark
import com.example.chessbinge.ui.theme.CrimsonDark
import com.example.chessbinge.ui.theme.CrimsonGlow
import com.example.chessbinge.ui.theme.CrimsonPrimary
import com.example.chessbinge.ui.theme.GoldAccent
import com.example.chessbinge.ui.theme.TextPrimary
import com.example.chessbinge.ui.theme.TextSecondary
import com.example.chessbinge.ui.theme.TitanOneFontFamily
import com.example.chessbinge.viewmodel.ChessViewModel

@Composable
fun PuzzleScreen(
    viewModel: ChessViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsState()
    val currentPuzzle by viewModel.currentPuzzle.collectAsState()
    val selectedPosition by viewModel.selectedPosition.collectAsState()
    val validMoves by viewModel.validMoves.collectAsState()
    val isFlipped by viewModel.isFlipped.collectAsState()
    val hintMove by viewModel.hintMove.collectAsState()
    val puzzleSolved by viewModel.puzzleSolved.collectAsState()
    val puzzleFailed by viewModel.puzzleFailed.collectAsState()

    val puzzle = currentPuzzle ?: PuzzleRepository.puzzles.first()
    val currentIndex = PuzzleRepository.puzzles.indexOfFirst { it.id == puzzle.id }.coerceAtLeast(0)

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
            // Header Bar
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

                Text(
                    text = "TACTICS ARENA",
                    fontFamily = TitanOneFontFamily,
                    fontSize = 17.sp,
                    color = CrimsonPrimary,
                    letterSpacing = 0.5.sp
                )

                IconButton(onClick = { viewModel.requestHint() }) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Hint",
                        tint = GoldAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Puzzle Info Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgCard)
                    .border(1.dp, BgCardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = puzzle.title,
                            fontFamily = TitanOneFontFamily,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CrimsonDark)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = puzzle.theme,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF2E2214))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = puzzle.difficulty,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${if (puzzle.playerColor == PieceColor.WHITE) "White" else "Black"} to move: ${puzzle.description}",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // The Chessboard
            ChessBoardView(
                gameState = gameState,
                selectedPosition = selectedPosition,
                validMoves = validMoves,
                isFlipped = isFlipped,
                hintMove = hintMove,
                onSquareClicked = { pos -> viewModel.onSquareClicked(pos) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Success Banner
            AnimatedVisibility(visible = puzzleSolved) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF132B1A))
                        .border(1.dp, Color(0xFF2ECC71), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2ECC71), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "EXCELLENT!", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF2ECC71))
                                Text(text = "Tactical theme mastered.", fontSize = 12.sp, color = TextSecondary)
                            }
                        }

                        Button(
                            onClick = {
                                val nextIdx = (currentIndex + 1) % PuzzleRepository.puzzles.size
                                viewModel.loadPuzzle(PuzzleRepository.puzzles[nextIdx])
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "NEXT", fontWeight = FontWeight.Bold, color = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Failed Banner
            AnimatedVisibility(visible = puzzleFailed && !puzzleSolved) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2E1217))
                        .border(1.dp, CrimsonGlow, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = CrimsonGlow, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "INCORRECT MOVE", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CrimsonGlow)
                                Text(text = "Look for the sharpest continuation.", fontSize = 12.sp, color = TextSecondary)
                            }
                        }

                        Button(
                            onClick = { viewModel.retryPuzzle() },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "RETRY", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Row: Previous, Next, Reset
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        val prevIdx = (currentIndex - 1 + PuzzleRepository.puzzles.size) % PuzzleRepository.puzzles.size
                        viewModel.loadPuzzle(PuzzleRepository.puzzles[prevIdx])
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "Prev Puzzle", color = TextPrimary, fontSize = 12.sp)
                }

                Button(
                    onClick = { viewModel.retryPuzzle() },
                    colors = ButtonDefaults.buttonColors(containerColor = BgCard),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Reset", color = TextPrimary, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        val nextIdx = (currentIndex + 1) % PuzzleRepository.puzzles.size
                        viewModel.loadPuzzle(PuzzleRepository.puzzles[nextIdx])
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "Next Puzzle", color = TextPrimary, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
